# Automatic lottery draw

The Android **Menu → Lottery** editor saves the registration dates and optional
waitlist capacity in one Firestore transaction. A blank capacity means unlimited.
Registration and listing changes check the latest campsite document in their
transactions, so a join cannot take a spot already filled by another entrant.

The Android app checks for closed lotteries when an owner or a registered
entrant has the app open. It checks immediately and then every 15 seconds.
An owner processes their own lotteries, and an entrant can process lotteries
they entered. The draw waits for a participating app and a Firestore connection.
This path uses the project's existing Firestore plan. The Cloud Functions
source in this folder is retained for reference.

The app rechecks the close time and draw status in a transaction, then records
results even for an empty waitlist. Single-campsite lotteries use the existing
per-location occupancy limits for the random draw.
Result messages use stable IDs and a pending marker so an interrupted delivery
can resume during the next run. Existing campsite documents without a capacity
remain unlimited. Previously drawn lotteries keep their existing results.
Offset-free registration dates are interpreted in Alberta time, UTC−06:00
from November 2026.

## Registration and shared lotteries

Registration includes the opening instant and excludes the closing instant.
Owners set the window in **Menu → Lottery**. The **Available stay dates** in
**Add/Edit Campsite** supply the campsite calendar used after a win.

For a shared lottery, select two or more unentered campsites you own and set a
future opening date. One document in `lotteries` holds the campsite IDs,
registration window, waitlist, winner count and optional capacity. Membership
locks when registration opens. The first campsite anchors the entry in the
existing entrant list and holds the result conversation.

The app draws each shared lottery once and skips its member campsite
documents. Each entrant receives one result. A winner chooses one campsite,
location and available date or set of dates, then agrees to that site's rules.
The confirmation transaction checks eligibility, the current rules and date
availability against the same lottery document used for other claims. It
records the booked campsite for reservation lists and history. Cancellation
or owner rejection releases the dates. Single-campsite winners still select
dates for their drawn location and must also acknowledge the current campsite
rules before their reservation is saved.

Prompts and implementation notes are in `SharedLottery.kt`, `Lottery.kt`,
and `LotteryDrawRepository.kt`.

## Verification

Run `npm --prefix functions test` from the directory containing `firebase.json`.
The legacy server tests use in-memory Firestore stand-ins. Android coverage lives in
`ForegroundLotteryDrawTest`, `CampsiteLifecycleTest`, `EntrantCampsiteDetailsTest`,
and `SharedLotteryTest`.

The opt-in integration test in `ForegroundLotteryDrawTest` uses the local
Firestore emulator with project ID `demo-olives-lottery`. Run
`adb reverse tcp:8188 tcp:8188` and pass `firestoreEmulatorHost=127.0.0.1` and
`firestoreEmulatorPort=8188` to the Android instrumentation runner. The test
uses an isolated Firebase app and never reads from the live project.
