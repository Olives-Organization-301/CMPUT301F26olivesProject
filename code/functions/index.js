const { initializeApp } = require("firebase-admin/app");
const { FieldValue, getFirestore } = require("firebase-admin/firestore");
const { error } = require("firebase-functions/logger");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const { drawClosedLottery, lotteryCutoff, deliverDrawMessages } = require("./lottery");

initializeApp();

async function forEachPage(query, processDocument) {
  let page = query.limit(100);
  while (true) {
    const snapshot = await page.get();
    for (const document of snapshot.docs) {
      try {
        await processDocument(document);
      } catch (cause) {
        error("Lottery processing failed", { campsiteID: document.id, cause: String(cause) });
      }
    }
    if (snapshot.size < 100) return;
    page = query.startAfter(snapshot.docs.at(-1)).limit(100);
  }
}

/**
 * US 02.05.01. Draw on the server even when the owner has closed the app.
 * Codex prompts: "work on user stories 2.04.07, 2.04.06, 2.05.01, how would the idea be implemented first"
 * and "work on it then, and make the test cases, and then document the code with the necessary prompts"
 */
exports.drawClosedLotteries = onSchedule("* * * * *", async () => {
  const db = getFirestore();
  const now = new Date();
  const cutoff = lotteryCutoff(now);

  for (const name of ["campsites", "lotteries"]) {
    const listings = db.collection(name);
    await forEachPage(listings.where("lotteryEndDate", "<=", cutoff).orderBy("lotteryEndDate"), async document => {
      const listing = document.data();
      if (listing.lotteryID || listing.lotteryDrawnAt != null ||
          Object.keys(listing.locationWinners ?? {}).length > 0 || listing.finalList?.length > 0) return;
      await drawClosedLottery(db, document.ref, cutoff, now);
    });

    // Stable message IDs let the next invocation finish an interrupted delivery.
    await forEachPage(listings.where("lotteryNotificationsPending", "==", true), document =>
      deliverDrawMessages(db, document, anchorID => FieldValue.arrayRemove(anchorID)));
  }
});
