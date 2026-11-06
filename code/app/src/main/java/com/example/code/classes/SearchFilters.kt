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
    fun matches(campsite: Campsite): Boolean =
        (country.isBlank() ||
                campsite.getCoordinates().getCountry().equals(country, ignoreCase = true)) &&
                (fromDate?.let { f -> campsite.getStartDate()?.let { it >= f } ?: true } ?: true) &&
                (minGuests?.let { campsite.getMaxOccupancy() >= it } ?: true) &&
                (maxGuests?.let { campsite.getMaxOccupancy() <= it } ?: true)
}