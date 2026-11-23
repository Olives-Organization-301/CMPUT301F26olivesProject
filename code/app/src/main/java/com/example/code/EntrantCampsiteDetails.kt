package com.example.code

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.code.classes.AppState
import com.example.code.classes.Campsite
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.code.classes.CampsiteOwner
import com.example.code.classes.Entrant

// TODO: DELETE these colours. We want colour choices to be in ui.Theme
private val ScreenBackground = Color(0xFFFDF8F3)
private val ButtonPurple = Color(0xFF6750A4)
private val ImagePlaceholder = Color(0xFFE6DFD8)

/** Campsite Details Screen
 * Show the Details of the Screen
 * I Used Sonnet 5.0 in order to make the basic GUI Structure
 * Prompt: "Can you make the GUI based on this screen?"
 * Conversation: https://claude.ai/share/2b8f9dba-e685-463b-b161-acccbf698b8a
 * Date: Nov 13, 2026
 * I used Sonnet 5.0 in order to integrate the backend
 * Prompt: "Can you integrate the backend with the GUI?"
 * Conversation: https://claude.ai/share/fbdd45b9-ed51-42b6-a072-85a9fb6f5d32
 * Date: Nov 13, 2026
 * I used Opus 5.5 In order add popups to indicate joining/leaving a list
 * Pormpt: "Can you add a few dialogue boxes for joining/leaving a lottery?"
 * Conversation: https://claude.ai/share/ae294592-b4fe-497b-86a9-a45c32b331e2
 * Date: Nov 20, 2026
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CampsiteDetailsScreen(
    navController: NavController,
    appState: AppState,
) {

    val entrant by produceState<Entrant?>(
        initialValue = null,
        key1 = appState.userID
    ) {
        val id = appState.userID
        if (id == null) {
            value = null
        } else {
            appState.entrants.searchEntrants(id) { found ->
                value = found
            }
        }
    }

    val chosenCampsite by produceState<Campsite?>(
        initialValue = null,
        key1 = appState.viewingCampsiteID
    ) {
        val id = appState.viewingCampsiteID
        if (id == null) {
            value = null
        } else {
            appState.campsites.searchCampsite(id) { found ->
                value = found
            }
        }
    }

    val campsite = chosenCampsite

    // Whether this entrant is already on the campsite's waiting list
    var isJoined by remember(campsite, entrant) {
        val id = appState.userID
        mutableStateOf(
            campsite != null && id != null && (
                campsite.entrantInList(id) ||
                    entrant?.getCampsiteLists()?.get("Waiting_List")?.contains(campsite.campsiteID) == true
                )
        )
    }
    var isSaving by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var joinSucceeded by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var leaveSucceeded by remember { mutableStateOf(false) }

    val owner by produceState<CampsiteOwner?>(
        initialValue = null,
        key1 = campsite?.getOwnerID()
    ) {
        val ownerID = campsite?.getOwnerID()
        if (ownerID == null) {
            value = null
        } else {
            appState.owners.searchCampsiteOwner(ownerID) { found ->
                value = found
            }
        }
    }

    // Entire Interface
    // Wrap the Join Buttons
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
    ) {

        // To avoid clipping it into the top of the screen
        Spacer(Modifier.height(48.dp))

        // Header: centered title + exit button on the right
        // This box controls the campsite Title and the X Button on the far-left.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp)
        ) {
            // Header title
            Text(
                text = campsite?.getName() ?: "Loading...",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
            // Use IconButton in order to Make the X Icon
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .border(2.dp, Color.Black, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✕", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        /* TODO: I want a future Column to add reviews (Or should it be in a separate screen?)
        *   in the description as well.
        */

        // Scrollable description area (takes all space between header and buttons)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {

            // Image: centered, rectangular
            // TODO: swap this Box for an Image(painter = ..., contentDescription = ...)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ImagePlaceholder)
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Owner + dates
            Text(owner?.username ?: "", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(
                "Lottery Begin Date: ${campsite?.getStartDate()?.toLocalDate() ?: ""}",
                fontWeight = FontWeight.Medium,
                fontSize = 18.sp
            )
            Text(
                "Lottery End Date: ${campsite?.getEndDate()?.toLocalDate() ?: ""}",
                fontWeight = FontWeight.Medium,
                fontSize = 18.sp
            )
            Text(
                "Rating: ${campsite?.getRating()?.let { "$it/5" } ?: ""}",
                fontWeight = FontWeight.Medium,
                fontSize = 18.sp
            )
            Text(
                "Max Occupancy: ${campsite?.getMaxOccupancy() ?: ""}",
                fontWeight = FontWeight.Medium,
                fontSize = 18.sp
            )
            Text(
                "Location: ${campsite?.getCoordinates()?.getCountry() ?: ""}",
                fontWeight = FontWeight.Medium,
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Details
            Text("Details:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                text = campsite?.getDescription() ?: "",
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Join / Leave Waiting List
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val userID = appState.userID
            val currentEntrant = entrant

            if (campsite != null && userID != null && isJoined) {
                Button(
                    enabled = !isSaving,
                    onClick = {
                        isSaving = true
                        appState.campsites.removeFromWaitingList(campsite, userID) { ok ->
                            isSaving = false
                            leaveSucceeded = ok
                            if (ok) isJoined = false
                            showLeaveDialog = true
                        }
                        if (currentEntrant != null) {
                            appState.entrants.removeEntrantCampsite(
                                currentEntrant, "Waiting_List", campsite.campsiteID
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = ButtonPurple)
                ) {
                    Box(modifier = Modifier.size(18.dp).background(Color.White, CircleShape))
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Leave Waiting List", fontSize = 14.sp, textAlign = TextAlign.Center)
                }
            } else {
                Button(
                    enabled = campsite != null && currentEntrant != null && userID != null && !isSaving,
                    onClick = {
                        if (campsite != null && userID != null && currentEntrant != null) {
                            isSaving = true
                            // 1. campsite doc: waitingList += entrantID
                            appState.campsites.addToWaitingList(campsite, userID) { campsiteOk ->
                                // 2. entrant doc: campsiteLists.Waiting_List += campsiteID
                                appState.entrants.addEntrantCampsite(
                                    currentEntrant, "Waiting_List", campsite.campsiteID
                                ) { entrantOk ->
                                    isSaving = false
                                    joinSucceeded = campsiteOk && entrantOk
                                    if (joinSucceeded) isJoined = true
                                    showJoinDialog = true
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = ButtonPurple)
                ) {
                    Box(modifier = Modifier.size(18.dp).background(Color.White, CircleShape))
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        if (isSaving) "Joining…" else "Join Waiting List",
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    // Confirmation popup after pressing "Join Waiting List"
    if (showJoinDialog) {
        JoinWaitingListDialog(
            success = joinSucceeded,
            campsiteName = campsite?.getName(),
            onDismiss = { showJoinDialog = false }
        )
    }

    // Confirmation popup after pressing "Leave Waiting List"
    if (showLeaveDialog) {
        LeaveWaitingListDialog(
            success = leaveSucceeded,
            campsiteName = campsite?.getName(),
            onDismiss = { showLeaveDialog = false }
        )
    }
}

/** Popup shown after an entrant tries to join a campsite's waiting list.
 * @param success: whether both database writes succeeded
 * @param campsiteName: name shown in the message (falls back to "this campsite")
 * @param onDismiss: called when OK is pressed or the dialog is dismissed
 * I used Opus 5.5 In order add popups to indicate joining/leaving a list
 * Pormpt: "Can you add a few dialogue boxes for joining/leaving a lottery?"
 * Conversation: https://claude.ai/share/ae294592-b4fe-497b-86a9-a45c32b331e2
 * Date: Nov 20, 2026
 */
@Composable
fun JoinWaitingListDialog(
    success: Boolean,
    campsiteName: String?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (success) "You're on the waiting list!" else "Couldn't join") },
        text = {
            Text(
                if (success)
                    "You've joined the waiting list for ${campsiteName ?: "this campsite"}. " +
                        "You'll be notified if you're selected in the lottery."
                else
                    "Something went wrong while joining the waiting list. Please try again."
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        }
    )
}

/** Popup shown after an entrant tries to leave a campsite's waiting list.
 * @param success: whether removing the entrant from the waiting list succeeded
 * @param campsiteName: name shown in the message (falls back to "this campsite")
 * @param onDismiss: called when OK is pressed or the dialog is dismissed
 * I used Opus 5.5 In order add popups to indicate joining/leaving a list
 * Pormpt: "Can you add a few dialogue boxes for joining/leaving a lottery?"
 * Conversation: https://claude.ai/share/ae294592-b4fe-497b-86a9-a45c32b331e2
 * Date: Nov 20, 2026
 */
@Composable
fun LeaveWaitingListDialog(
    success: Boolean,
    campsiteName: String?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (success) "You've left the waiting list" else "Couldn't leave") },
        text = {
            Text(
                if (success)
                    "You've been removed from the waiting list for ${campsiteName ?: "this campsite"}. " +
                        "You can join again at any time before the lottery closes."
                else
                    "Something went wrong while leaving the waiting list. Please try again."
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        }
    )
}
