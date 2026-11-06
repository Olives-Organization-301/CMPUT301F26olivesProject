package com.example.code.classes

/*
WAITLIST implemented in order to facilitate the lottery function.
Users/entrants will be allowed to join this waitlist for a period of
one week until it expires (IMPLEMENTED), at which point 5 (or less,
depending on waitlist size) random users on the list will be selected,
on a first-come first-serve basis, to reserve their spots.

They will be notified by the system and are free to turn down these
requests at will.

The owner can spectate the waiting list as it fills up (or not), right
until the deadline for waiting list closure. (IMPLEMENTED) Optionally
they may also be able to see the status of each selected member of the
waiting list - whether they have signed on or not.

THIS CODE WAS IMPLEMENTED BY GOOGLE GEMINI 11-06-2026, WITH THE PROMPT
"This button allows a campsite owner to place their campsite into a
lottery system for a certain period of time. Such a lottery will allow
for customers/entrants to join a "waiting list" for one week. The owner
can spectate this waiting list until its expiry."
 */
data class WaitlistEntrant( // contents of the waitingList
    val id: String,
    val name: String,
    val email: String,
    val joinedMillis: Long,
)

class PreviewCampsite(
    val name: String,
    val location: String,
    val startMillis: Long?,
    val endMillis: Long?,
    val waitingList: List<WaitlistEntrant> = emptyList(),
)
// waitingList for the lottery system