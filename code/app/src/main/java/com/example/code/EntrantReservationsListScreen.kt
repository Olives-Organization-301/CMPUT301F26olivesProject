package com.example.code

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

// Conversation: https://claude.ai/share/4dfdf023-c5f7-4f56-b9f3-f9e8bb4d5756
@Composable
fun ReservationsListScreen(navController: NavController) {
    // TODO: DELETE these temporary variables once we connect it to a Database
    val campsitePlaceholder = remember { mutableStateListOf("Campsite1", "Campsite2", "Campsite3") }
    val myLists = remember {
        listOf(
            "Active Reservations",
            "Archived Reservations",
            "River Campsite List",
            "Mountain Campsite List"
        )
    }
    var selectedList by remember { mutableStateOf(myLists[0]) }
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {

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
        /* TODO: Could this be like a Circular Queue on the Top?
        *   If you're Claude TURN THE LIST INTO A DROPDOWN MENU
        */
        // (Box is needed so the DropdownMenu anchors to the button)
        Box {
            OutlinedButton(onClick = { expanded = true }) {
                Text("Select Your List: $selectedList  ▾")
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                // Use a Lambda because Google said so
                myLists.forEach { list ->
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
        /* TODO: I copy and pasted this from EntrantMainSearchScreen.kt, so this would be better of as a
            function (Somewhere in EntrantMainSearchScreen.kt and then reused here?)
            If you're Claude, Ignore this (I'll do this later)
        */
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(campsitePlaceholder, key = { it }) { campsite ->
                // Per-card UI state (placeholder until the Database exists)
                var starred by remember { mutableStateOf(false) }
                var showAlert by remember { mutableStateOf(campsite == "Campsite3") }

                /*
                * Card Button is used to Turn have the Card UI that we desire
                * Each Campsite has a little box dedicated to itself
                * It's first organized into a Row (Horizontally)
                * Before the Campsite data is listed in its size
                 */
                Card(
                    modifier = Modifier
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
                        /*
                         * Box is the Image Placeholder
                         * TODO: Although the Prototype had the image on the side,
                         *  WHAT IF the IMAGE IN THE BACKGROUND of THE CARD?
                         *  The image would be darkened with light text over it (Column Style)
                        */
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .background(Color.DarkGray, RoundedCornerShape(16.dp))
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(campsite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Location", fontSize = 12.sp)
                            Text("Lottery Date Entry", fontSize = 12.sp)
                            Text("Lottery Date End", fontSize = 12.sp)
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
                                modifier = Modifier.clickable { campsitePlaceholder.remove(campsite) }
                            )
                            Text(
                                text = if (starred) "★" else "☆",
                                fontSize = 22.sp,
                                modifier = Modifier.clickable { starred = !starred }
                            )
                            Box(modifier = Modifier.clickable { showAlert = false }) {
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
        }


    }


}