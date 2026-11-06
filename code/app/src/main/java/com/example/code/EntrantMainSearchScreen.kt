package com.example.code

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.code.classes.AppState
import com.example.code.classes.Campsite
import java.time.LocalDateTime
import com.example.code.classes.SearchFilters

/** # Search Screen
 *  Implementation of the Entrants' Main Screen as seen in the Figma.
 *
 *  Prototype was gained from this conversation with Claude Sonnet 5.0:
 *  Prompt: "Can you Do the GUI Based on this screenshot?"
 *  Prompt: "How do I Correctly pad this Main screen? How do I make the Cards?"
 *  https://claude.ai/share/65a871a8-c3f3-421e-ad84-506b32425362
 *  Claude Sonnet 5.0 was used for searching and filtering:
 *  Prompt: "How Can you make a searching function? How do I check Levenshtein's Distance?"
 *  https://claude.ai/share/327fdf39-7b02-40b7-9f26-c5f66c048f9b (The Searching stuff)
 *
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MainSearchScreen(
    navController: NavController,
    appState: AppState,
    onUpdateCampsiteId: (String) -> Unit
) {
    val campsiteDatabase: List<Campsite> = appState.campsites.campsites

    // Search for a Database
    var searchString by remember { mutableStateOf("") }

    // Filter settings as a Dataclass (See SearchFilters.kt in classes package)
    var filters by remember { mutableStateOf(SearchFilters()) }
    // For filter pop-up screen
    var enterSettings by remember { mutableStateOf(false) }

    // Campsites that filter and search
    // Uses Levenshtein distance to sort, and filters it based on SearchFilter Dataclass
    val visibleCampsites = remember(campsiteDatabase.toList(), searchString, filters) {
        campsiteDatabase
            .asSequence()
            .filter { filters.matches(it) }
            .map { it to levenshteinDist(searchString, it.getName()) }
            .sortedBy { (_, dist) -> dist }
            .take(100)
            .map { (campsite, _) -> campsite }
            .toList()
    }

    // Column Wrapping the Search/Card Screens and Menu bar
    Column(modifier = Modifier.fillMaxSize()) {

        // Filter Settings Pop-up screen
        if (enterSettings) {
            FilterSettingsDialog(
                initial = filters,
                onSave = { filters = it; enterSettings = false },
                onClear = { filters = SearchFilters(); enterSettings = false },
                onDismiss = { enterSettings = false }
            )
        }
        // Padded area for search screen and campsites: search field + list
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // To avoid clipping it into the top of the screen
            Spacer(Modifier.height(48.dp))

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

            // Extra Row Icon buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(onClick = { enterSettings = true }) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .border(2.dp, Color.Black, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⚙", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // The Cards of Campsites
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(visibleCampsites) { campsite ->
                    /* Card Gives a small space for Campsite
                    * Stores Campsite Information
                     */
                    Card(
                        onClick = {
                            onUpdateCampsiteId(campsite.campsiteID)
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
                            // TODO: Make Image the Background, put info on the very beginning
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

/**
 * Uses Levenshtein Distance to sort the campsite
 * Based on String Similarity
 * Prompt: "How do I use Levenshtein's Distance to "
 * Claude Convo: https://claude.ai/share/327fdf39-7b02-40b7-9f26-c5f66c048f9b
 */
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

/**
 * Main Popup ui that allows the user to change the filter
 * Can change Date, country, min and max guest number
 * Prompt: "How can I handle search filtering?"
 * Claude convo: https://claude.ai/share/1fc95a02-f5f0-415b-8055-adf22f799619
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun FilterSettingsDialog(
    initial: SearchFilters,
    onSave: (SearchFilters) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    var dateText by remember { mutableStateOf(initial.fromDate?.toString() ?: "") }
    var country by remember { mutableStateOf(initial.country) }
    var minText by remember { mutableStateOf(initial.minGuests?.toString() ?: "") }
    var maxText by remember { mutableStateOf(initial.maxGuests?.toString() ?: "") }

    val parsedDate = runCatching { LocalDateTime.parse(dateText) }.getOrNull()
    val min = minText.toIntOrNull()
    val max = maxText.toIntOrNull()

    val dateInvalid = dateText.isNotBlank() && parsedDate == null
    val rangeInvalid = min != null && max != null && min > max

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Filter Settings") },
        text = {
            Column {
                Text("Start date (ISO, UTC):")
                TextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("e.g. 2026-11-05T00:00") },
                    isError = dateInvalid,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                )

                Spacer(Modifier.height(24.dp))

                Text("Country:")
                TextField(
                    value = country,
                    onValueChange = { country = it },
                    label = { Text("e.g. Canada") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                )

                Spacer(Modifier.height(24.dp))

                Text("Guests:")
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextField(
                        value = minText,
                        onValueChange = { minText = it.filter(Char::isDigit).take(4) },
                        label = { Text("Min") },
                        isError = rangeInvalid,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    TextField(
                        value = maxText,
                        onValueChange = { maxText = it.filter(Char::isDigit).take(4) },
                        label = { Text("Max") },
                        isError = rangeInvalid,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !dateInvalid && !rangeInvalid,
                onClick = { onSave(SearchFilters(parsedDate, country.trim(), min, max)) }
            ) { Text("Save") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onClear) { Text("Clear") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}
