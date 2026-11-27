package com.example.code

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.code.classes.AppState
import java.time.LocalDate

/** US 01.04.02 and 02.11.01. A shared winner picks one campsite and agrees to its own rules. */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
internal fun SharedLotteryReservationDialog(
    appState: AppState,
    lottery: SharedLottery,
    entrantID: String,
    onDismiss: () -> Unit,
) {
    val availableCampsites = appState.campsites.campsites
        .filter { it.campsiteID in lottery.campsiteIDs && it.sharedLotteryID == lottery.id }
        .associateBy { it.campsiteID }
    var choice by remember(lottery.id) { mutableStateOf<Pair<String, Int>?>(null) }
    var pendingDates by remember(lottery.id) { mutableStateOf<List<LocalDate>?>(null) }
    var saving by remember(lottery.id) { mutableStateOf(false) }
    var failed by remember(lottery.id) { mutableStateOf(false) }
    val decline = {
        if (!saving) {
            saving = true
            failed = false
            appState.campsites.sharedLotteries.decline(lottery.id, entrantID).addOnCompleteListener {
                saving = false
                failed = !it.isSuccessful || it.result != true
                if (!failed) onDismiss()
            }
        }
    }
    val selected = choice
    val campsite = selected?.first?.let(availableCampsites::get)

    if (selected == null || campsite == null) {
        AlertDialog(
            onDismissRequest = { if (!saving) onDismiss() },
            title = { Text("Choose a campsite") },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (failed) item { Text("Could not decline the reservation. Try again.") }
                    items(lottery.campsiteIDs, key = { it }) { id ->
                        val site = availableCampsites[id]
                        if (site != null) {
                            Column {
                                Text(site.getName())
                                if (site.getVacationStartTimes().isEmpty()) {
                                    Text("Stay dates unavailable")
                                } else {
                                    val count = maxOf(1, site.getCoordinates().size, site.getMaxOccupancy().size)
                                    repeat(count) { location ->
                                        Button(
                                            modifier = Modifier.testTag("sharedReservation_${id}_$location"),
                                            enabled = !saving,
                                            onClick = {
                                                failed = false
                                                choice = id to location
                                            }
                                        ) { Text("Choose location ${location + 1}") }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = !saving, onClick = onDismiss) { Text("Close") }
            },
            dismissButton = {
                TextButton(enabled = !saving, onClick = decline) { Text("Decline reservation") }
            }
        )
    } else {
        val location = selected.second
        ReserveDatesDialog(
            campsite = campsite,
            title = "${campsite.getName()} · Location ${location + 1}",
            initiallySelected = emptySet(),
            onDismiss = {
                if (!saving) {
                    failed = false
                    choice = null
                }
            },
            onConfirm = { pendingDates = it },
            onDecline = decline,
            saving = saving,
            saveFailed = failed,
            unavailableDays = lottery.bookedDates(campsite.campsiteID, location)
                .mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.toSet(),
            earliestDate = lotteryNow().toLocalDate(),
        )
        pendingDates?.let { dates ->
            // Keep the displayed rules fixed until this dialog is dismissed. The transaction
            // checks this same text against the campsite's current rules before confirming.
            val rules = remember(campsite.campsiteID, dates) { campsite.getRules().toList() }
            CampsiteRulesDialog(
                campsiteName = campsite.getName(),
                rules = rules,
                onDecline = { pendingDates = null },
                onAgree = {
                    if (!saving) {
                        pendingDates = null
                        saving = true
                        failed = false
                        appState.campsites.sharedLotteries.confirm(
                            lottery.id, entrantID, campsite.campsiteID, location, dates, rules
                        ).addOnCompleteListener {
                            saving = false
                            failed = !it.isSuccessful || it.result != true
                            if (!failed) onDismiss()
                        }
                    }
                }
            )
        }
    }
}
