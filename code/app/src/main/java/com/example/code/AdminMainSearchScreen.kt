package com.example.code


import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.sp
import com.example.code.classes.AppState
import com.example.code.classes.Campsite

data class AdminThemeColors(
    val gradient: List<Color>,
    val title: Color,
    val subtitle: Color,
    val footer: Color,
    val primaryButton: Color,
    val primaryText: Color,
    val secondaryButton: Color,
    val secondaryText: Color
)

private val AdminLightColors = AdminThemeColors(
    gradient = listOf(Color(0xFFFFF8EE), Color(0xFFFDE8CB)),
    title = Color(0xFF2B1B17),
    subtitle = Color(0xFF7A6A62),
    footer = Color(0xFF1A1A1A),
    primaryButton = Color(0xFF45238A),
    primaryText = Color.White,
    secondaryButton = Color(0xFF6B49B0),
    secondaryText = Color.White
)

private val AdminDarkColors = AdminThemeColors(
    gradient = listOf(Color(0xFF101828), Color(0xFF1F2B45)),
    title = Color.White,
    subtitle = Color(0xFF9AA5BD),
    footer = Color.White,
    primaryButton = Color(0xFFFFB84D),
    primaryText = Color(0xFF1A1A1A),
    secondaryButton = Color(0xFFFFE7BF),
    secondaryText = Color(0xFF1A1A1A)
)

data class Profile(val id: String, val name: String, val role: String)
data class PendingRemoval(val message: String, val onConfirm: () -> Unit)

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AdminMainSearchScreen(
    navController: NavController,
    appState: AppState,
    darkTheme: Boolean = isSystemInDarkTheme()
) {
    var searchString by remember { mutableStateOf("") }
    var selectedTop by remember { mutableStateOf("Campsites")}
    var pendingRemoval by remember {mutableStateOf<PendingRemoval?>(null)}

    val colors = if (darkTheme) AdminDarkColors else AdminLightColors

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

    LaunchedEffect(selectedTop) {
        if (selectedTop == "Campsites") {
            campsiteDb.fetchAllCampsites { _ -> }
        } else if (selectedTop == "User Database") {
            entrantDb.fetchAllEntrants { _ -> }
            ownerDb.fetchAllOwners { _ -> }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.horizontalGradient(colors.gradient))
            .systemBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopTabs(selected = selectedTop, onSelect = { selectedTop = it }, colors = colors)

            Box(modifier = Modifier.weight(1f)){
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // search bar
                    TextField(
                        value = searchString,
                        onValueChange = { searchString = it },
                        placeholder = { Text("Search for a campsite", color = colors.subtitle) },
                        leadingIcon = { Icon(Icons.Default.Menu, contentDescription = "Sidebar", tint = colors.title) },
                        shape = RoundedCornerShape(28.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = colors.title.copy(alpha = 0.06f),
                            unfocusedContainerColor = colors.title.copy(alpha = 0.04f),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = colors.title,
                            unfocusedTextColor = colors.title
                        ),
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
                            colors = colors,
                            modifier = Modifier.weight(1f)
                        )

                        "Campsites" -> CampsitesTab(
                            campsites = campsiteDb.campsites.filter {it.getName().contains(query, ignoreCase = true)},
                            onRemove = { campsite ->
                                askRemove("Remove the campsite \"${campsite.getName()}\"?"){
                                    campsiteDb.deleteCampsite(campsite)
                                }
                            },
                            colors = colors,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
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
fun TopTabs(selected: String, onSelect: (String) -> Unit, colors: AdminThemeColors){
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ){
        listOf("User Database", "Campsites").forEach { tab ->
            val isSelected = selected == tab
            Button(
                onClick = { onSelect(tab) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) colors.primaryButton else colors.secondaryButton.copy(alpha = 0.3f),
                    contentColor = if (isSelected) colors.primaryText else colors.title
                ),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ){
                Text(tab, maxLines = 1, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun RemoveButton(text: String = "Remove", onClick: () -> Unit, colors: AdminThemeColors){
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.primaryButton,
            contentColor = colors.primaryText
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
    ){
        Text(text, maxLines = 1, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
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
fun EmptyState(text: String, colors: AdminThemeColors){
    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center){
        Text(text, color = colors.subtitle, fontSize = 14.sp)
    }
}

@Composable
fun UserDatabaseTab(
    profiles: List<Profile>,
    onRemoveProfile: (Profile) -> Unit,
    onRemoveOwner: (Profile) -> Unit,
    colors: AdminThemeColors,
    modifier: Modifier = Modifier
){
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        if(profiles.isEmpty()) item {EmptyState("No profiles found", colors)}
        items(profiles) { profile ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colors.title.copy(alpha = 0.05f)
                )
            ){
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Spacer(Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(1f)){
                        Text(profile.name, fontWeight = FontWeight.Bold, color = colors.title, fontSize = 16.sp)
                        Text(profile.role, fontSize = 12.sp, color = colors.subtitle)
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.End
                    ){
                        RemoveButton("Remove", onClick = { onRemoveProfile(profile) }, colors = colors)
                        if(profile.role == "Campsite Owner"){
                            RemoveButton("Policy Violation", onClick = { onRemoveOwner(profile) }, colors = colors)
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
    colors: AdminThemeColors,
    modifier: Modifier = Modifier
){
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ){
        if(campsites.isEmpty()) item {EmptyState("No campsites found", colors)}
        items(campsites){ campsite ->
            CampsiteCard(campsite, onRemove = { onRemove(campsite)}, colors = colors)
        }
    }
}

@Composable
fun CampsiteCard(campsite: Campsite, onRemove: () -> Unit, colors: AdminThemeColors){
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(135.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFB0B0B0))
    ){
        val thumbnail = campsiteThumbnailRes(campsite)
        CampsiteImageBackground(
            imageRes = thumbnail,
            fallback = Color(0xFFB0B0B0),
            modifier = Modifier.fillMaxSize()
        ) {
            CompositionLocalProvider(
                LocalContentColor provides if (thumbnail != null) Color.White else Color.Black
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(campsite.getName(), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        val sub = campsite.getMainCoordinates()?.getCity() ?: ""
                        val country = campsite.getMainCoordinates()?.getCountry() ?: ""
                        val locText = if (sub.isNotBlank()) "$sub, $country" else country
                        if (locText.isNotBlank()) {
                            Text(locText, fontSize = 12.sp, color = Color.White.copy(alpha = 0.9f))
                        }
                        if (campsite.getOwnerID().isNotBlank()) {
                            Text("Owner ID: ${campsite.getOwnerID()}", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                        }
                        Text("Price: \$${campsite.getPrice()} | Max Occupancy: ${campsite.getTotalOccupancy()}", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                    Spacer(Modifier.width(8.dp))
                    RemoveButton("Remove", onClick = onRemove, colors = colors)
                }
            }
        }
    }
}
