package com.example.code

import android.content.Context
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Calendar
import java.util.Date
import androidx.compose.material3.Surface
import androidx.compose.ui.window.DialogProperties


/** # Search Screen
 *  Implementation of the Entrants' Main Screen as seen in the Figma.
 *
 *  Prototype was gained from this conversation with Claude Sonnet 5.0:
 *  https://claude.ai/chat/fa52c57a-03b0-486b-b988-87d2f36ff18a
 *  Prompt: "How do I Correctly pad this Main screen? How do I make the Cards?"
 *
 */
@Composable
fun campsiteOwnerMainScreen(navController: NavController) {
    var searchString by remember { mutableStateOf("") }
    var showAdd by remember {mutableStateOf(false)}
    // TODO: DELETE this once we connect it to a Database
    val campsitePlaceholder = remember { listOf("Campsite1", "Campsite2") }

    // Column Wrapping the Search/Card Screens and Menu bar
    Column(modifier = Modifier.fillMaxSize()) {

        Box(modifier = Modifier.weight(1f)) {

            // Padded area for search screen and campsites: search field + list
            Column(
                modifier = Modifier
                    .fillMaxSize()
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

                                Spacer(Modifier.width(8.dp))

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Button(onClick = { }) { Text("  Edit  ", maxLines = 1) }
                                    Button(onClick = { }) { Text("Lottery", maxLines = 1) }
                                }

                            }
                        }
                    }
                }
            }
            FloatingActionButton(
                onClick = { showAdd = true},
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {Icon(Icons.Filled.Add, contentDescription = "Add campsite") }
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
            Button(onClick = { }, modifier = Modifier.weight(1f)) { Text("QR Code", maxLines = 1) }
            Button(onClick = { }, modifier = Modifier.weight(1f)) { Text("Account", maxLines = 1) }
        }
    }
    if(showAdd){
        AddCampsite(onDismiss = {showAdd = false})
    }
}

/*
Implementation of owner screen as outlined in the figma. Search screen was used as reference.
AddCampsite, pickdate, and parts of campsiteOwnerMainScreen UI were developed using claude 3.5 in
this chat: https://claude.ai/share/140477f9-42aa-4b16-8189-dad57b7d8afb
 */
@Composable
fun AddCampsite(onDismiss: () -> Unit){
    var name by remember {mutableStateOf("")}
    var location by remember {mutableStateOf("")}
    var startMillis by remember { mutableStateOf<Long?>(null)}
    var endMillis by remember { mutableStateOf<Long?>(null)}
    val context = LocalContext.current
    val format = remember { SimpleDateFormat("MMM d, yyyy",Locale.getDefault()) }

    val startText = startMillis?.let {format.format(Date(it))} ?:"Not set"
    val endText = startMillis?.let {format.format(Date(it))} ?:"Not set"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ){
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                    Text(
                        text = "Add Campsite",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { onDismiss() }) {
                        Text("Save")
                    }
                }

                Column(
                    modifier = Modifier
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Campsite name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Location")
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Address or place name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Lottery dates")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { pickDate(context, null) { startMillis = it } },
                            modifier = Modifier.weight(1f)
                        ) { Text("Entry: $startText", maxLines = 1) }

                        OutlinedButton(
                            onClick = { pickDate(context, startMillis) { endMillis = it } },
                            modifier = Modifier.weight(1f)
                        ) { Text("End: $endText", maxLines = 1) }
                    }
                }
            }
        }
    }

}

fun pickDate(context: Context, minMillis: Long?, onPicked: (Long) -> Unit){
    val cal = Calendar.getInstance()
    val dialog = android.app.DatePickerDialog(
        context,
        {_, year, month, day ->
            val picked = Calendar.getInstance().apply {set(year, month, day, 0, 0, 0)}
            onPicked(picked.timeInMillis)
        },
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH)
    )
    minMillis?.let {dialog.datePicker.minDate = it }
    dialog.show()
}

