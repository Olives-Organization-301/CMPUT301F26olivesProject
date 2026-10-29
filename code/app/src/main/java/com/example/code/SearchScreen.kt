package com.example.code

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

/** # Search Screen
 *  Implementation of the Entrants' Main Screen as seen in the Figma.
 *
 *  Prototype was gained from this conversation with Claude Sonnet 5.0:
 *  https://claude.ai/chat/fa52c57a-03b0-486b-b988-87d2f36ff18a
 *  Prompt: "How do I Correctly pad this Main screen? How do I make the Cards?"
 *
 */
@Composable
fun SearchScreen(navController: NavController) {
    var searchString by remember { mutableStateOf("") }
    // TODO: DELETE this once we connect it to a Database
    val campsitePlaceholder = remember { listOf("Campsite1", "Campsite2", "Campsite3") }

    // Column Wrapping the Search/Card Screens and
    Column(modifier = Modifier.fillMaxSize()) {

        // Padded area for search screen and campsites: search field + list
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Search Bar
            // TODO: Add Sidebar button and Search Button on Search Button
            TextField(
                value = searchString,
                onValueChange = { searchString = it },
                label = { Text("Search for a campsite") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            )

            // The Cards of Campsites
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(campsitePlaceholder) { campsite ->
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
                            Column {
                                Text(campsite)
                                Text("Location")
                                Text("Lottery Date Entry")
                                Text("Lottery Date End")
                            }
                        }
                    }
                }
            }
        }

        // Menu Bar
        // TODO: Turn Buttons into Icons + Text and Remove Backgrounds for buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.LightGray)
                .navigationBarsPadding()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = { }, modifier = Modifier.weight(1f)) { Text("List", maxLines = 1) }
            Button(onClick = { }, modifier = Modifier.weight(1f)) { Text("Alerts", maxLines = 1) }
            Button(onClick = { }, modifier = Modifier.weight(1f)) { Text("Account", maxLines = 1) }
        }
    }
}