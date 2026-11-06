package com.example.code

import android.os.Build
import androidx.annotation.RequiresApi
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
import com.example.code.classes.Campsite
import java.time.LocalDateTime

/** # Search Screen
 *  Implementation of the Entrants' Main Screen as seen in the Figma.
 *
 *  Prototype was gained from this conversation with Claude Sonnet 5.0:
 *  https://claude.ai/chat/fa52c57a-03b0-486b-b988-87d2f36ff18a
 *  https://claude.ai/chat/e0b977ee-8243-4d21-b3ca-40ef221146a9 (The Searching stuff)
 *  Prompt: "How do I Correctly pad this Main screen? How do I make the Cards?"
 *
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MainSearchScreen(navController: NavController, campsiteDatabase: MutableList<Campsite>) {
    var searchString by remember { mutableStateOf("") }

    // TODO: Add an actual filter
    val fromDate: LocalDateTime? = LocalDateTime.parse("2024-02-15T16:48:59")
    val selectedCountry by remember { mutableStateOf("") }

    val visibleCampsites = remember(campsiteDatabase.toList(), searchString) {
        campsiteDatabase
            .asSequence() // Android-style feature
            .filter { campsite ->
                (selectedCountry == "" || campsite.getCoordinates().getCountry() == selectedCountry) &&
                        (fromDate == null || campsite.getStartDate() >= fromDate)
            }
            // Sort by Levenshtein Distance
            .map { it to levenshteinDist(searchString, it.getName()) }
            .sortedBy { (_, dist) -> dist }
            .take(100) // Show 100 campsites
            .map { (campsite, _) -> campsite }
            .toList() // Turn it back to campsite
    }

    // Column Wrapping the Search/Card Screens and Menu bar
    Column(modifier = Modifier.fillMaxSize()) {

        // To avoid clipping it into the top of the screen
        Spacer(Modifier.height(48.dp))

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
                items(visibleCampsites) { campsite ->
                    /*
                    * Card Button is used to Turn have the Card UI that we desire
                    * Each Campsite has a little box dedicated to itself
                    * It's first organized into a Row (Horizontally)
                    * Before the Campsite data is listed in its size
                     */
                    Card(
                        onClick = {
                            navController.navigate("campsiteDetails")
                        },
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
                                Text(campsite.getName())
                                Text(campsite.getCoordinates().getCountry())
                                Text(campsite.getStartDate().toString())
                                Text(campsite.getEndDate().toString())
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
            Button(onClick = {
                navController.navigate("reservationsListScreen")
            },
                modifier = Modifier.weight(1f)) {
                Text("List", maxLines = 1)
            }
            Button(onClick = {
                navController.navigate("entrantAlerts")
            },
                modifier = Modifier.weight(1f)) {
                Text("Alerts", maxLines = 1)
            }
            Button(onClick = {
                navController.navigate("profileScreen")
            },
                modifier = Modifier.weight(1f)) {
                Text("Account", maxLines = 1)
            }
        }
    }
}

fun levenshteinDist(a: String, b: String): Int {
    val aLength = a.length + 1
    val bLength = b.length + 1
    val levArr = Array(aLength) { IntArray(bLength) }
    val cost: (Char, Char) -> Int = { x, y -> if (x == y) 0 else 1 }

    for (i in 0 until aLength) { levArr[i][0] = i }
    for (j in 0 until bLength) { levArr[0][j] = j }

    for (i in 1 until aLength) {
        for (j in 1 until bLength) {
            levArr[i][j] = minOf(
                levArr[i - 1][j] + 1,
                levArr[i][j - 1] + 1,
                levArr[i - 1][j - 1] + cost(a[i - 1], b[j - 1])
            )
        }
    }

    return levArr[a.length][b.length]
}