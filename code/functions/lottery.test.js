const assert = require("node:assert/strict");
const { test } = require("node:test");
const { drawClosedLottery, drawResults, lotteryCutoff, resultMessages, deliverDrawMessages } = require("./lottery");

const now = new Date("2026-11-27T06:00:00.000Z");
const cutoff = lotteryCutoff(now);
const listing = {
  lotteryEndDate: "2026-11-27T00:00",
  ownerID: "owner",
  name: "Pine Camp",
  waitingList: ["a", "b", "a"],
  activeLotteryList: ["c"],
  maxOccupancy: [1, 1],
};

test("uses Alberta time and draws at the exact closing time", () => {
  assert.equal(cutoff, "2026-11-27T00:00:00.000000000");
  assert.equal(drawResults(listing, lotteryCutoff(new Date(now.getTime() - 1)), now), null);
  assert.notEqual(drawResults(listing, cutoff, now), null);
  assert.equal(lotteryCutoff(new Date("2026-07-01T06:00:00Z")), "2026-07-01T00:00:00.000000000");
});

test("keeps the existing independent location draws and produces stable result messages", () => {
  const result = drawResults(listing, cutoff, now, () => 0);
  assert.deepEqual(result.locationWinners, { 0: ["a"], 1: ["a"] });
  assert.deepEqual(result.finalList, ["a"]);
  assert.deepEqual(result.lotteryDrawEntrants, ["a", "b", "c"]);
  const drawn = { ...listing, ...result };
  const messages = resultMessages("pine", drawn);
  assert.deepEqual(messages.map(({ entrantID, locationIndex, type }) => [entrantID, locationIndex, type]), [
    ["a", 0, "lottery_won"], ["a", 1, "lottery_won"],
    ["b", -1, "lottery_lost"], ["c", -1, "lottery_lost"],
  ]);
  assert.equal(new Set(messages.map(message => message.messageID)).size, 4);
  assert.deepEqual(resultMessages("pine", { ...drawn, waitingList: [] }), messages);
});

test("records a completed draw for empty waitlists and respects legacy draw markers", () => {
  const empty = { ...listing, waitingList: [], activeLotteryList: [] };
  const result = drawResults(empty, cutoff, now);
  assert.equal(result.lotteryDrawnAt, now.getTime());
  assert.equal(result.lotteryNotificationsPending, false);
  assert.equal(drawResults({ ...empty, ...result }, cutoff, now), null);
  assert.equal(drawResults({ ...listing, finalList: ["a"] }, cutoff, now), null);
  assert.equal(drawResults({ ...listing, locationWinners: { 0: [] } }, cutoff, now), null);
});

test("two overlapping jobs only commit one draw against the same listing", async () => {
  let state = { ...listing };
  let queue = Promise.resolve();
  const db = {
    runTransaction(work) {
      const result = queue.then(() => work({
        get: async () => ({ exists: true, data: () => ({ ...state }) }),
        update: (_, values) => { state = { ...state, ...values }; },
      }));
      queue = result;
      return result;
    },
  };
  const results = await Promise.all([
    drawClosedLottery(db, "pine", cutoff, now),
    drawClosedLottery(db, "pine", cutoff, now),
  ]);
  assert.deepEqual(results, [true, false]);
  assert.equal(state.lotteryNotificationsPending, true);
});

test("an interrupted notification batch retries without duplicating messages", async () => {
  const entrants = Array.from({ length: 401 }, (_, index) => `entrant-${index}`);
  const largeListing = { ...listing, waitingList: entrants, activeLotteryList: [], maxOccupancy: [1] };
  let state = { ...largeListing, ...drawResults(largeListing, cutoff, now, () => 0) };
  const writes = new Map();
  let commits = 0;
  const db = {
    collection() { return { doc: id => id }; },
    batch() {
      const batchWrites = new Map();
      return {
        set(id, message) { batchWrites.set(id, message); },
        async commit() {
          if (++commits === 2) throw new Error("interrupted");
          for (const [id, message] of batchWrites) writes.set(id, message);
        },
      };
    },
  };
  const document = {
    id: "pine",
    data: () => state,
    ref: { update: async values => { state = { ...state, ...values }; } },
  };
  await assert.rejects(deliverDrawMessages(db, document), /interrupted/);
  assert.equal(state.lotteryNotificationsPending, true);
  assert.equal(writes.size, 400);
  await deliverDrawMessages(db, document);
  assert.equal(state.lotteryNotificationsPending, false);
  assert.equal(writes.size, 401);
});

test("a shared listing draws once and sends one result per entrant across both sites", () => {
  const shared = {
    ...listing,
    campsiteIDs: ["pine", "cedar"],
    winnerCount: 2,
  };
  const result = drawResults(shared, cutoff, now, () => 0);
  assert.deepEqual(result.finalList, ["a", "b"]);
  assert.equal(result.locationWinners, undefined);
  assert.equal(drawResults({ ...shared, ...result }, cutoff, now), null);
  assert.equal(drawResults({ ...listing, lotteryID: "shared" }, cutoff, now), null);

  const messages = resultMessages("shared", { ...shared, ...result });
  assert.deepEqual(messages.map(m => [m.entrantID, m.type]), [
    ["a", "lottery_won"], ["b", "lottery_won"], ["c", "lottery_lost"],
  ]);
  assert.ok(messages.every(m => m.lotteryID === "shared" && m.campsiteID === "pine" && m.locationIndex === -1));
  assert.equal(new Set(messages.map(m => m.messageID)).size, 3);
});

test("shared result delivery removes the losing entry and keeps the winner's entry", async () => {
  const shared = { ...listing, campsiteIDs: ["pine", "cedar"], winnerCount: 1 };
  let state = { ...shared, ...drawResults(shared, cutoff, now, () => 0) };
  const removed = [];
  const messages = new Map();
  const db = {
    collection: name => ({ doc: id => `${name}/${id}` }),
    runTransaction: work => work({
      getAll: async (...refs) => refs.map(ref => ({ ref, exists: ref !== "Entrants/c" })),
      update: (ref, changes) => removed.push([ref, changes]),
      set: (ref, message) => messages.set(ref, message),
    }),
  };
  await deliverDrawMessages(db, {
    id: "shared",
    data: () => state,
    ref: { update: async values => { state = { ...state, ...values }; } },
  }, anchor => ({ remove: anchor }));

  assert.deepEqual(removed, [["Entrants/b", {
    "campsiteLists.Active_Campsites": { remove: "pine" },
    "campsiteLists.Waiting_List": { remove: "pine" },
  }]]);
  assert.equal(messages.size, 3);
  assert.equal(state.lotteryNotificationsPending, false);
});
