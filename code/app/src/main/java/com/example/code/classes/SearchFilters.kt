package com.example.code.classes

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.code.classes.Campsite
import java.time.LocalDateTime

/** Search Filters uses a Data Class in order to handle what filters a user would want to use.
 * Conversation: https://claude.ai/share/1fc95a02-f5f0-415b-8055-adf22f799619
 * Usage: EntrantsCampsiteDetails.kt
 */
@RequiresApi(Build.VERSION_CODES.O)
data class SearchFilters(
    val fromDate: LocalDateTime? = null,
    val country: String = "",
    val minGuests: Int? = null,
    val maxGuests: Int? = null
) {
    // Search filter has been modified in order to accomodate for country filtering, and Peak Occupancy change.
    fun matches(campsite: Campsite): Boolean =
        (country.isBlank() ||
                campsite.getCoordinates().any { it.getCountry().equals(country, ignoreCase = true) }) &&
                (fromDate?.let { f -> campsite.getStartDate()?.let { it >= f } ?: true } ?: true) &&
                (minGuests?.let { campsite.getPeakOccupancy() >= it } ?: true) &&
                (maxGuests?.let { campsite.getPeakOccupancy() <= it } ?: true)
}