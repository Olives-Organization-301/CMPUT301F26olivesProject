const { createHash, randomInt } = require("node:crypto");

// Match Android, including on runtimes with older time zone data.
const clockOptions = {
  year: "numeric", month: "2-digit", day: "2-digit",
  hour: "2-digit", minute: "2-digit", second: "2-digit",
  fractionalSecondDigits: 3, hourCycle: "h23",
};
const historicClock = new Intl.DateTimeFormat("en-CA", { ...clockOptions, timeZone: "America/Edmonton" });
const albertaClock = new Intl.DateTimeFormat("en-CA", { ...clockOptions, timeZone: "Etc/GMT+6" });
const albertaTimeEffective = Date.parse("2026-11-01T06:00:00Z");

function lotteryCutoff(now) {
  const clock = now.getTime() < albertaTimeEffective ? historicClock : albertaClock;
  const parts = Object.fromEntries(clock.formatToParts(now).map(({ type, value }) => [type, value]));
  return `${parts.year}-${parts.month}-${parts.day}T${parts.hour}:${parts.minute}:${parts.second}.${parts.fractionalSecond}000000`;
}

function entrantsFrom(...lists) {
  return [...new Set(lists.flatMap(list => Array.isArray(list) ? list : [])
    .filter(value => typeof value === "string" && value.length > 0))];
}

function drawResults(listing, cutoff, now, pickIndex = randomInt) {
  // US 02.04.02: Member campsites defer to their shared lottery's single draw.
  if (listing.lotteryID || typeof listing.lotteryEndDate !== "string" || listing.lotteryEndDate.length === 0 ||
      listing.lotteryEndDate > cutoff || listing.lotteryDrawnAt != null ||
      Object.keys(listing.locationWinners ?? {}).length > 0 ||
      entrantsFrom(listing.finalList).length > 0) return null;

  const entrants = entrantsFrom(listing.waitingList, listing.activeLotteryList);
  if (Array.isArray(listing.campsiteIDs) && listing.campsiteIDs.length > 1) {
    const count = Number.isSafeInteger(listing.winnerCount) && listing.winnerCount > 0 ? listing.winnerCount : 5;
    return {
      finalList: sample(entrants, count, pickIndex),
      lotteryDrawnAt: now.getTime(),
      lotteryNotificationsPending: entrants.length > 0,
      lotteryDrawEntrants: entrants,
    };
  }
  const storedOccupancy = Array.isArray(listing.maxOccupancy) ? listing.maxOccupancy : [listing.maxOccupancy];
  const parsedOccupancy = storedOccupancy.map(value => typeof value === "string" && value.trim() ? Number(value) : value)
    .filter(Number.isSafeInteger);
  const occupancy = parsedOccupancy.length ? parsedOccupancy : [4];
  const locationWinners = Object.fromEntries(occupancy.map((capacity, index) =>
    [String(index), sample(entrants, capacity, pickIndex)]));

  return {
    locationWinners,
    finalList: entrantsFrom(...Object.values(locationWinners)),
    lotteryDrawnAt: now.getTime(),
    lotteryNotificationsPending: entrants.length > 0,
    lotteryDrawEntrants: entrants,
  };
}

function sample(entrants, capacity, pickIndex) {
  const candidates = [...entrants];
  const count = Math.min(Math.max(capacity, 0), candidates.length);
  for (let i = 0; i < count; i++) {
    const selected = i + pickIndex(candidates.length - i);
    [candidates[i], candidates[selected]] = [candidates[selected], candidates[i]];
  }
  return candidates.slice(0, count);
}

function resultMessages(listingID, listing) {
  const shared = Array.isArray(listing.campsiteIDs) && listing.campsiteIDs.length > 1;
  const campsiteID = shared ? listing.campsiteIDs[0] : listingID;
  const campsiteName = listing.name ?? listing.Name ?? listing.campsiteName ?? campsiteID;
  const ownerID = String(listing.ownerID ?? listing.OwnerID ?? listing.ownerId ?? listing.owner_id ?? "");
  const sharedWinners = new Set(shared ? entrantsFrom(listing.finalList) : []);
  const locationsByEntrant = new Map();
  for (const [index, winners] of Object.entries(listing.locationWinners ?? {})) {
    for (const entrant of winners) {
      const locations = locationsByEntrant.get(entrant) ?? [];
      locations.push(Number(index));
      locationsByEntrant.set(entrant, locations);
    }
  }
  return entrantsFrom(listing.lotteryDrawEntrants).flatMap(entrantID => {
    const locations = locationsByEntrant.get(entrantID) ?? [-1];
    return locations.map(locationIndex => {
      const won = shared ? sharedWinners.has(entrantID) : locationIndex >= 0;
      const messageID = "lottery-" + createHash("sha256")
        .update(JSON.stringify([shared ? `shared/${listingID}` : campsiteID, listing.lotteryDrawnAt, entrantID, locationIndex])).digest("hex");
      return {
        messageID, campsiteID, campsiteName, ownerID, entrantID, locationIndex,
        sender: campsiteName, fromOwner: true, automated: true, stayDates: [],
        type: won ? "lottery_won" : "lottery_lost",
        sentAt: listing.lotteryDrawnAt + Math.max(0, locationIndex),
        timestamp: new Date(listing.lotteryDrawnAt).toISOString(),
        ...(shared ? { lotteryID: listingID } : {}),
        text: won && shared
          ? "You won a place in a shared lottery. Choose one campsite and your stay dates."
          : won
          ? `Congratulations! You won a place in location ${locationIndex + 1} at ${campsiteName}. Use the button below to select your dates.`
          : `You were not selected in the lottery for ${campsiteName}.`,
      };
    });
  });
}

// Re-read inside the transaction so concurrent jobs and late listing edits cannot redraw it.
async function drawClosedLottery(db, ref, cutoff, now) {
  return db.runTransaction(async transaction => {
    const snapshot = await transaction.get(ref);
    if (!snapshot.exists) return false;
    const result = drawResults(snapshot.data(), cutoff, now);
    if (!result) return false;
    transaction.update(ref, result);
    return true;
  });
}

// Stable message IDs allow the next run to replay a partially delivered draw.
async function deliverDrawMessages(db, document, removeEntry) {
  const messages = resultMessages(document.id, document.data());
  const messageCollection = db.collection("messages");
  const size = messages[0]?.lotteryID ? 200 : 400;
  for (let i = 0; i < messages.length; i += size) {
    const block = messages.slice(i, i + size);
    const losses = block.filter(message => message.lotteryID && message.type === "lottery_lost");
    if (losses.length && removeEntry) {
      await db.runTransaction(async transaction => {
        const profiles = db.collection("Entrants");
        const snapshots = await transaction.getAll(...losses.map(message => profiles.doc(message.entrantID)));
        const anchor = losses[0].campsiteID;
        for (const snapshot of snapshots) {
          if (snapshot.exists) {
            transaction.update(snapshot.ref, {
              "campsiteLists.Active_Campsites": removeEntry(anchor),
              "campsiteLists.Waiting_List": removeEntry(anchor),
            });
          }
        }
        for (const message of block) {
          transaction.set(messageCollection.doc(message.messageID), message);
        }
      });
      continue;
    }
    const batch = db.batch();
    for (const message of block) {
      batch.set(messageCollection.doc(message.messageID), message);
    }
    await batch.commit();
  }
  await document.ref.update({ lotteryNotificationsPending: false, lotteryDrawEntrants: [] });
}

module.exports = { drawResults, resultMessages, drawClosedLottery, lotteryCutoff, deliverDrawMessages };
