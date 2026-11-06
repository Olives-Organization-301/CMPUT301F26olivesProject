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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Calendar
import java.util.Date
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.code.classes.PreviewCampsite
import kotlinx.coroutines.selects.select


/** # Search Screen
 *  Implementation of the Entrants' Main Screen as seen in the Figma.
 *
 *  Prototype was gained from this conversation with Claude Sonnet 5.0:
 *  https://claude.ai/chat/fa52c57a-03b0-486b-b988-87d2f36ff18a
 *  Prompt: "How do I Correctly pad this Main screen? How do I make the Cards?"
 *
 */
@Composable
fun AdminMainSearchScreen(navController: NavController) {
    var searchString by remember { mutableStateOf("") }
    var selectedTop by remember { mutableStateOf("Logs")}
    val format = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    // TODO: DELETE this once we connect it to a Database
    val previewCampsites = remember { mutableStateListOf(PreviewCampsite(name = "Campsite1", location = "Location1",startMillis = null, endMillis = null)) }

    // Column Wrapping the Search/Card Screens and Menu bar
    Column(modifier = Modifier.fillMaxSize()) {
        TopTabs(selected = selectedTop, onSelect = { selectedTop = it } )

        Box(modifier = Modifier.weight(1f)){
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ){
                TextField(
                    value = searchString,
                    onValueChange = { searchString = it },
                    placeholder = { Text("Search for a campsite")},
                    leadingIcon = { Icon(Icons.Default.Menu, contentDescription = "Sidebar")},
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ){
                    Button(onClick = { }, modifier = Modifier.weight(1f)) { Text("Campsite Owner View")}
                    Button(onClick = { }, modifier = Modifier.weight(1f)) { Text("Entrant View")}
                }
                Text(
                    "Current View : Admin",
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ){
                    items(previewCampsites) { preview ->
                        CampsiteCard(preview, format)
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF3EDF7))
                .navigationBarsPadding()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("My Account", fontSize = 12.sp)
        }

    }



}

@Composable
fun TopTabs(selected: String, onSelect: (String) -> Unit ){
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ){
        listOf("User Database", "Campsites", "Images", "Logs").forEach { tab ->
            Button(
                onClick = { onSelect(tab) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ){ Text(tab, maxLines = 1, fontSize = 12.sp)}
        }
    }
}

@Composable
fun CampsiteCard(preview: PreviewCampsite, format: SimpleDateFormat){
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        shape = RoundedCornerShape(24.dp)
    ){
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ){
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(Color.DarkGray, RoundedCornerShape(16.dp))
            )
            Spacer(Modifier.width(12.dp))
            Column{
                Text(preview.name)
                Text(preview.location)
                Text("Entry: " + (preview.startMillis?.let { format.format(Date(it)) } ?: "Not set"))
                Text("End: " + (preview.endMillis?.let { format.format(Date(it)) } ?: "Not set"))
            }
        }
    }
}







