# Firestore reviewer demo

The demo records use the prefix `olives_reviewer_20261126`. Log in as **Entrant** with `olives.entrant01@example.com` through `olives.entrant20@example.com`, or as **Owner** with `olives.owner1@example.com` and `olives.owner2@example.com`. These are synthetic accounts for the app's email-based login. The fixture includes profile and campsite JPEGs, location capacities, lottery and stay dates, and message threads.

| Campsite | Reviewer walkthrough |
| --- | --- |
| **Demo 1 - Thursday Draw** | Owner 1. One location, capacity 3. Entrants 01–05 start on the waiting list. The lottery opens Thursday, December 3, 2026 and closes Friday, December 4, 2026 on the device's local calendar. The draw starts unset. Use My Reservations to check Waiting List before Thursday and Active Campsites during the window. |
| **Demo 2 - One Entrant Two Locations** | Owner 1. Capacities 1 and 2. Entrant 06 won both locations and gets a separate result message for each. |
| **Demo 3 - Twenty Entrants Two Locations** | Owner 1. Capacities 10 and 10. Each location has ten winners. Entrants 02, 11, and 17 won both locations in the saved draw. |
| **Demo 4 - QR Entry** | Owner 2. Generate its code under QR Code. Capture it as an image and open it from the entrant's QR picker. The scanner should open this specific campsite by ID and the entrant can use the normal Join button. Entrant 20 is the dedicated QR test account. Its entry is already saved, so use another demo entrant to repeat the Join step. |
| **Demo 5 - Completed Stay and Reviews** | Owner 2. Five participants with saved outcomes. Entrant 01 opted out of notifications, won and confirmed, and has the campsite on the seeded Past list. Entrant 02 was rejected. Entrant 03 won and has a reservation. Entrant 04 won and declined. Entrant 05 was rejected. The owner and entrants 01 and 02 have two-way chat history. |

## Verification

Firestore server reads confirmed 91 records. Ten focused emulator tests passed, including a rotated QR, an unknown campsite code, Waiting-to-Active movement, reservation confirmation and decline, owner messaging, and Android notifications for wins, losses, and an opted-out win. The notification tests use test entrants and Android's real notification manager. A separate live test scanned an owner-generated QR bitmap, entered Demo 4 as entrant 20, and read the entry back from both the campsite and entrant documents.

## Lottery, stays, and reviews

- The owner dashboard draws a lottery at its saved close time while it is open. If the dashboard is closed, it catches up the next time the owner opens it. The owner's Lottery dialog shows the actual participants and results by location.
- A confirmed stay moves to Past Campsites after the last selected day across all confirmed locations. The app reconciles overdue stays when the entrant signs in or keeps the app open. Older reservations without saved dates remain in Reserved.
- On Demo 5, open **Reviews** as a demo entrant, select a rating, and post. Sign in as another entrant to reply. Reopen the tab to see both entries again.

## Reproduction

The instrumented fixture writes only when explicitly passed `writeReviewerFirestore=true`. It checks the server for the existing owner fixture before writing so a rerun can verify the records without resetting reviewer activity.

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.example.code.ReviewerFirestoreSeed' '-Pandroid.testInstrumentationRunnerArguments.writeReviewerFirestore=true'
```

Run the separate live QR check after seeding. It verifies or creates entrant 20's QR entry for Demo 4.

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.example.code.ReviewerQrFirestoreTest' '-Pandroid.testInstrumentationRunnerArguments.writeReviewerFirestore=true'
```


Codex prompt summary — “Seed five reviewer campsites with synthetic profiles, photos, messages, and lottery edge cases. Verify Firestore reads, generate an owner QR code that can actually be scanned, and test lottery entry from that code.”
