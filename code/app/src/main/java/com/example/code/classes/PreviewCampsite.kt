package com.example.code.classes

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
    val startMillisRegis: Long?,
    val endMillisRegis: Long?,
    val waitingList: List<WaitlistEntrant> = emptyList(),
)
