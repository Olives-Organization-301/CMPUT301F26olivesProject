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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.runtime.setValue
import com.example.code.classes.CampsiteOwner

// TODO: DELETE these colours. We want colour choices to be in ui.Theme
private val ScreenBackground = Color(0xFFFDF8F3)
private val ButtonPurple = Color(0xFF6750A4)
private val ImagePlaceholder = Color(0xFFE6DFD8)

// Conversation: https://claude.ai/chat/073b54ab-b55c-4752-a1ab-e436a65fdb6a
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CampsiteDetailsScreen(
    navController: NavController,
    appState: AppState,
) {

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
            Text(campsite?.getOwnerID() ?: "", fontWeight = FontWeight.Bold, fontSize = 18.sp)
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

        // Join Waiting List or Active Lottery
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (campsite != null && appState.userID != null
                && campsite.entrantInList(appState.userID!!)
            ) {
                Button(
                    onClick = {
                        campsite.removeEntrant(appState.userID!!)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = ButtonPurple)
                ) {
                    Box(modifier = Modifier.size(18.dp).background(Color.White, CircleShape))
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Leave\nWaiting List", fontSize = 14.sp, textAlign = TextAlign.Center)
                }
            } else {
                Button(
                    onClick = {
                        if (appState.userID != null && campsite != null) {
                            campsite.addEntrant(appState.userID!!)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = ButtonPurple)
                ) {
                    Box(modifier = Modifier.size(18.dp).background(Color.White, CircleShape))
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Join\nWaiting List", fontSize = 14.sp, textAlign = TextAlign.Center)
                }
                Button(
                    onClick = {
                        if (appState.userID != null && campsite != null) {
                            campsite.addActiveLotteryEntrant(appState.userID!!)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = ButtonPurple)
                ) {
                    Box(modifier = Modifier.size(18.dp).background(Color.White, CircleShape))
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Join\nActive Lottery", fontSize = 14.sp, textAlign = TextAlign.Center)
                }
            }
        }
    }
}