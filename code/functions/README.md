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

For a shared lottery, select two or more unentered campsites and set a future
opening date. A different owner's campsite appears as an option only after its
owner selects **Allow another owner to include this campsite in a shared lottery**
in that campsite's **Menu → Lottery** dialog. The offer can be withdrawn while
the campsite is unassigned. The owner of the first campsite coordinates the
lottery; other participating owners can view its settings and entrants. One
document in `lotteries` holds the campsite IDs, participating owner IDs,
registration window, waitlist, winner count and optional capacity. The save
transaction rechecks each campsite's ownership, offer and current membership.
Membership locks when registration opens. The first campsite anchors the entry
in the existing entrant list and holds the result conversation.

The app draws each shared lottery once and skips its member campsite
documents. Each entrant receives one result. A winner chooses one campsite,
location and available date or set of dates, then agrees to that site's rules.
The confirmation transaction checks eligibility, the current rules and date
availability against the same lottery document used for other claims. It
records the booked campsite for reservation lists and history. Cancellation
or owner rejection releases the dates. Booking and cancellation messages use
the selected campsite's owner, so the second owner sees those reservations.
Each owner also sees registered shared-lottery entrants in **Messages** for
their own campsite. Chat threads remain separate per campsite: a message to
the first owner is not copied into the second owner's thread. The second owner
can start a campsite thread with a registered entrant, who can then reply.
Single-campsite winners still select
dates for their drawn location and must also acknowledge the current campsite
rules before their reservation is saved.

The prompts "work on 2.04.08", "so when entrant registers, for campsite owner
messages no entrant messages appear, fix that", "can owner B see messages from
the entrants on that campsite collab", and "make all test cases for this,
stress test it to the limit, and with all the other test cases from before all
together so the app works" are documented beside the relevant code and tests.
Earlier lottery prompts and implementation notes remain in `SharedLottery.kt`,
`Lottery.kt`, and `LotteryDrawRepository.kt`.

## Verification

Run `npm --prefix functions test` from the directory containing `firebase.json`.
The legacy server tests use in-memory Firestore stand-ins. Android coverage lives in
`ForegroundLotteryDrawTest`, `CampsiteLifecycleTest`, `EntrantCampsiteDetailsTest`,
and `SharedLotteryTest`.

Run `node scripts/run-reservation-rules-test.js all` from `code/` with an
Android emulator connected. It starts the local Firestore emulator, sets up
ADB reverse, and runs the Android instrumentation suite in fresh groups. A
single 150-test instrumentation session exhausted the AVD during verification;
the grouped run completed. `ForegroundLotteryDrawTest` checks opt-in and
cross-owner booking routing and races 30 registrations against a 10-person
capacity. `CampsiteOwnerMessagesTest` checks both owners' inboxes, separate
message threads, entrant replies, and a 1,000-entrant roster. Emulator tests
use an isolated Firebase app with project ID `demo-olives-lottery`. The
read-only live owner inbox audit requires `readOwnerReservations=true`. Tests
that seed or change live reviewer data remain opt-in with
`writeReviewerFirestore=true`.

For `ReservationRulesFirestoreTest`, run `./gradlew reservationRulesFirestoreTest`
from `code/` with an Android emulator connected. This task starts the cached
Firestore emulator, sets up ADB reverse and the runner arguments, runs that
test, and removes the connection and temporary emulator files afterward.
