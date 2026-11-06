package com.example.code

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.code.classes.AppState
import com.example.code.classes.Campsite
import com.example.code.classes.Entrant
import java.time.format.DateTimeFormatter
import java.util.Locale


// Conversation: https://claude.ai/share/4dfdf023-c5f7-4f56-b9f3-f9e8bb4d5756
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ReservationsListScreen(
    navController: NavController,
    appState: AppState
) {

    val entrant by produceState<Entrant?>(
        initialValue = null,
        key1 = appState.userID
    ) {
        val id = appState.userID
        if (id == null) {
            value = null
        } else {
            appState.entrants.searchEntrant(id) { found ->
                value = found
            }
        }
    }

    val campsites: Map<String, MutableList<String>> = entrant?.getCampsiteLists() ?: emptyMap()

    var selectedList by remember { mutableStateOf("Active_Campsites") }
    var expanded by remember { mutableStateOf(false) }

    // UI-observable copy of the currently selected list
    val displayedCampsites = remember(entrant, selectedList) {
        mutableStateListOf<String>().apply {
            addAll(campsites[selectedList] ?: emptyList())
        }
    }

    val campsiteCache = remember { mutableStateMapOf<String, Campsite>() }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.getDefault()) }

    // Launch a Coroutine at the start of the app in order to
    LaunchedEffect(displayedCampsites.toList()) {
        displayedCampsites
            .filter { it !in campsiteCache }
            .forEach { id ->
                appState.campsites.searchCampsite(id) { found ->
                    if (found != null) campsiteCache[id] = found
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {

        // To avoid clipping it into the top of the screen
        Spacer(Modifier.height(48.dp))

        // Header: title + close button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedList,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "✕",
                fontSize = 24.sp,
                modifier = Modifier.clickable { navController.popBackStack() }
            )
        }

        Spacer(Modifier.height(8.dp))

        // Select Screens list
        // (Box is needed so the DropdownMenu anchors to the button)
        Box {
            OutlinedButton(onClick = { expanded = true }) {
                Text("Select Your List: $selectedList  ▾")
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                // Use Lambda to iterate
                campsites.keys.forEach { list ->
                    DropdownMenuItem(
                        text = { Text(list) },
                        onClick = {
                            // TODO: Actually Switch between the Campsites
                            selectedList = list
                            expanded = false // Close menu after selection
                        }
                    )

                }
            }
        }


        // The Cards of Campsites
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(displayedCampsites, key = { it }) { campsite ->
                CampsiteCard(
                    campsiteID = campsite,
                    campsiteData = campsiteCache[campsite],
                    dateFormatter = dateFormatter,
                    onRemove = {
                        entrant?.let {
                            appState.entrants.removeEntrantCampsite(it, selectedList, campsite)
                        }
                        displayedCampsites.remove(campsite)
                    }
                )
            }
        }


    }


}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CampsiteCard(
    campsiteID: String,
    campsiteData: Campsite?,
    dateFormatter: DateTimeFormatter,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Per-card UI state (placeholder until the Database exists)
    var starred by remember { mutableStateOf(false) }
    var showAlert by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFB0B0B0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // TODO: Put the image in the background
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(Color.DarkGray, RoundedCornerShape(16.dp))
            )
            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = campsiteData?.getName() ?: campsiteID,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = campsiteData?.getCoordinates()?.getCountry() ?: "Loading…",
                    fontSize = 12.sp
                )
                Text(
                    text = "Lottery Entry: ${campsiteData?.getStartDate()?.format(dateFormatter) ?: "—"}",
                    fontSize = 12.sp
                )
                Text(
                    text = "Lottery End: ${campsiteData?.getEndDate()?.format(dateFormatter) ?: "—"}",
                    fontSize = 12.sp
                )
            }

            // Action icons (remove / favorite / notifications)
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "✕",
                    fontSize = 22.sp,
                    modifier = Modifier.clickable { onRemove() }
                )
                Text(
                    text = if (starred) "★" else "☆",
                    fontSize = 22.sp,
                    modifier = Modifier.clickable { starred = !starred }
                )
                Box(modifier = Modifier.clickable { showAlert = !showAlert }) {
                    Text("🔔", fontSize = 22.sp)
                    if (showAlert) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .align(Alignment.TopEnd)
                                .background(Color.Red, CircleShape)
                        )
                    }
                }
            }
        }
    }
}