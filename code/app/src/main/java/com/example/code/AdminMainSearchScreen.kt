package com.example.code


import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.sp
import com.example.code.classes.AppState
import com.example.code.classes.Campsite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** # Search Screen
 *  Implementation of the Admins' Main Screen as seen in the Figma.
 *
 *  Prototype was gained from this conversation with Claude Sonnet 5.0:
 *  https://claude.ai/chat/fa52c57a-03b0-486b-b988-87d2f36ff18a
 *  Prompt: "I want to change this UI code to make it look like this--how do I do that?"
 *  Images attacthed: Figma write ups and beta code done in production
 */
// https://claude.ai/share/cdce4c2a-2c1a-4c36-895d-ad4fb811582b
// one of the prompts used : now when User Database is clicked, I want both collections "Entrants" and
// "CampsiteOwners" displayed as how the logic for displaying collection "campsites" were in Admin

data class Profile(val id: String, val name: String, val role: String)
data class PendingRemoval(val message: String, val onConfirm: () -> Unit)

// From Gabe; I edited Campsite Details in order to change the dates of the campsites, so it requires this API (Sorry)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AdminMainSearchScreen(navController: NavController, appState: AppState) {
    var searchString by remember { mutableStateOf("") }
    var selectedTop by remember { mutableStateOf("Campsites")}
    var pendingRemoval by remember {mutableStateOf<PendingRemoval?>(null)}

    val campsiteDb = appState.campsites
    val entrantDb = appState.entrants
    val ownerDb = appState.owners

    val profiles = ownerDb.campsiteOwners.map { Profile(it.getUserID(), it.username.ifBlank { it.email.ifBlank { it.getUserID() } }, "Campsite Owner") } +
            entrantDb.entrants.map { Profile(it.getUserID(), it.username.ifBlank { it.email.ifBlank { it.getUserID() } }, "Entrant") }

    fun deleteProfile(profile: Profile){
        if(profile.role == "Campsite Owner"){
            ownerDb.campsiteOwners.firstOrNull {it.getUserID() == profile.id}
                ?.let {ownerDb.deleteCampsiteOwner(it)}
        } else {
            entrantDb.entrants.firstOrNull{it.getUserID() == profile.id}
                ?.let{entrantDb.deleteEntrant(it)}
        }
    }

    val askRemove: (String, () -> Unit) -> Unit = { message, action ->
        pendingRemoval = PendingRemoval(message, action)
    }

    androidx.compose.runtime.LaunchedEffect(selectedTop) {
        if (selectedTop == "Campsites" || selectedTop == "Images") {
            campsiteDb.fetchAllCampsites { _ -> }
        } else if (selectedTop == "User Database") {
            entrantDb.fetchAllEntrants { _ -> }
            ownerDb.fetchAllOwners { _ -> }
        }
    }

    // Column Wrapping the Search/Card Screens and Menu bar
    Column(modifier = Modifier.fillMaxSize()) {
        TopTabs(selected = selectedTop, onSelect = { selectedTop = it } )

        Box(modifier = Modifier.weight(1f)){
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                //search bar
                TextField(
                    value = searchString,
                    onValueChange = { searchString = it },
                    placeholder = { Text("Search for a campsite") },
                    leadingIcon = { Icon(Icons.Default.Menu, contentDescription = "Sidebar") },
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                val query = searchString.trim()

                when (selectedTop) {
                    "User Database" -> UserDatabaseTab(
                        profiles = profiles.filter { it.name.contains(query, ignoreCase = true) },
                        onRemoveProfile = { profile ->
                            askRemove("Remove \"${profile.name}\" for violating app policy?") {
                                deleteProfile(profile)
                            }
                        },
                        onRemoveOwner = { profile ->
                            askRemove("Remove \"${profile.name}\" for violating app policy?") {
                                deleteProfile(profile)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    "Campsites" -> CampsitesTab(
                        campsites = campsiteDb.campsites.filter {it.getName().contains(query, ignoreCase = true)},
                        onRemove = { campsite ->
                            askRemove("Remove the campsite \"${campsite.getName()}\"?"){
                                campsiteDb.deleteCampsite(campsite)
                            }
                        },
                        modifier = Modifier.weight(1f)

                    )

                    "Images" -> ImagesTab(
                        campsites = campsiteDb.campsites.filter {it.getName().contains(query, ignoreCase = true)},
                        onRemove = { campsite ->
                            askRemove("Remove the photo for \"${campsite.getName()}\" ?"){
                                campsiteDb.removeCampsiteImage(campsite.campsiteID)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )

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
            Text("", fontSize = 12.sp)
        }

    }

    pendingRemoval?.let { pending ->
        ConfirmRemoveDialog(
            message = pending.message,
            onConfirm = pending.onConfirm,
            onDismiss = { pendingRemoval = null }
        )
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
        listOf("User Database", "Campsites", "Images").forEach { tab ->
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
fun RemoveButton(text: String = "Remove", onClick: () -> Unit){
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 12.dp),
    ){ Text(text, maxLines = 1, fontSize = 12.sp)}
}

@Composable
fun ConfirmRemoveDialog(message: String, onConfirm: () -> Unit, onDismiss: () -> Unit){
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {Text("Confirm removal")},
        text = { Text(message)},
        confirmButton = {
            TextButton(onClick = {
                onConfirm()
                onDismiss()
            }) { Text("Remove", color = Color.Red)}
        },
        dismissButton = { TextButton(onClick = onDismiss){ Text("Cancel")} }
    )
}

@Composable
fun EmptyState(text: String){
    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center){
        Text(text)
    }
}

@Composable
fun UserDatabaseTab(
    profiles: List<Profile>,
    onRemoveProfile: (Profile) -> Unit,
    onRemoveOwner: (Profile) -> Unit,
    modifier: Modifier = Modifier
){
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        if(profiles.isEmpty()) item {EmptyState("No profiles found")}
        items(profiles) { profile ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ){
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)){
                        Text(profile.name)
                        Text(profile.role, fontSize = 12.sp)
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.End
                    ){
                        RemoveButton("Remove"){ onRemoveProfile(profile)}
                        if(profile.role == "Campsite Owner"){
                            RemoveButton("Policy Violation") { onRemoveOwner(profile)}
                        }
                    }
                }
            }

        }
    }
}

@Composable
fun CampsitesTab(
    campsites: List<Campsite>,
    onRemove: (Campsite) -> Unit,
    modifier: Modifier = Modifier
){
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ){
        if(campsites.isEmpty()) item {EmptyState("No campsites found")}
        items(campsites){ campsite ->
            CampsiteCard(campsite, onRemove = { onRemove(campsite)})
        }
    }
}

@Composable
fun ImagesTab(
    campsites: List<Campsite>,
    onRemove: (Campsite) -> Unit,
    modifier: Modifier = Modifier
) {
    val withPhotos = campsites.filter { !it.detailsPhoto().isNullOrBlank() }
    if (withPhotos.isEmpty()) {
        EmptyState("No uploaded images")
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        items(withPhotos, key = { it.campsiteID }) { campsite ->
            val photo = campsite.detailsPhoto().orEmpty()
            // Decode off the main thread; the photos are 1280px wide
            val bitmap by produceState<ImageBitmap?>(null, photo) {
                value = withContext(Dispatchers.Default) { decodeBase64ToImageBitmap(photo) }
            }
            Card(shape = RoundedCornerShape(16.dp)) {
                Column {
                    bitmap?.let {
                        Image(
                            bitmap = it,
                            contentDescription = campsite.getName(),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().height(120.dp)
                        )
                    }
                    Text(
                        campsite.getName(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    Box(Modifier.padding(8.dp)) {
                        RemoveButton(onClick = { onRemove(campsite) })
                    }
                }
            }
        }
    }
}

@Composable
fun CampsiteCard(campsite: Campsite,  onRemove: () -> Unit){
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp),
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
                    .size(80.dp)
                    .background(Color.DarkGray, RoundedCornerShape(16.dp))
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)){
                Text(campsite.getName(), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                val sub = campsite.getMainCoordinates()?.getCity() ?: "" // I Changed getfirstSubdivision to get City
                val country = campsite.getMainCoordinates()?.getCountry() ?: "" // Also get country now
                val locText = if (sub.isNotBlank()) "$sub, $country" else country
                Text(locText, fontSize = 12.sp, color = Color.Gray)
                if (campsite.getOwnerID().isNotBlank()) {
                    Text("Owner ID: ${campsite.getOwnerID()}", fontSize = 11.sp, color = Color.DarkGray)
                }
                // Since I Changed Max Occupancy as how many spots there were in a day, total occupancy should just be the sum of it
                Text("Price: \$${campsite.getPrice()} | Max Occupancy: ${campsite.getTotalOccupancy()}", fontSize = 11.sp, color = Color.DarkGray)
            }
            RemoveButton(onClick = onRemove)
        }
    }
}







