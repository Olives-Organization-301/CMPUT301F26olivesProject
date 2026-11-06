package com.example.code

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.code.classes.AppState
import com.example.code.classes.Campsite
import com.example.code.classes.Entrant
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.KeyboardType

// Conversation: https://claude.ai/share/b857df9c-812a-4adb-b696-38a5266149f7
@Composable
fun ProfileScreen(
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

    val recentCampsites = entrant?.getCampsiteLists()?.get("Past_Campsites") ?: emptyList()
    var showEditDialog by remember { mutableStateOf(false) }
    var showPolicyDialog by remember { mutableStateOf(false) }
    var refreshTick by remember { mutableStateOf(0) }
    val tick = refreshTick   // read so the screen recomposes after a save

    val outline = MaterialTheme.colorScheme.onSurface

    // Organize Entire UI
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {

        // To avoid clipping it into the top of the screen
        Spacer(Modifier.height(48.dp))

        // Profile Basic Details
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Profile Picture (placeholder silhouette built from plain shapes, no icon dependency)
            // Swap this Box for an Image / AsyncImage once users have real photos.
            Box(
                modifier = Modifier.size(80.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                /* TODO: Claude ass Attempt at making a cute little profile picture
                *   In the future, replace this with an image Box or something
                */
                // Head
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .border(4.dp, outline, CircleShape)
                )
                // Shoulders
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .size(width = 80.dp, height = 36.dp)
                        .border(
                            4.dp,
                            outline,
                            RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                        )
                )
            }

            // Details
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Text(entrant?.name?.ifBlank { "No name set" } ?: "Loading…")
                Text("@${entrant?.username ?: ""}")
                Text(entrant?.dateOfBirth?.ifBlank { "No date of birth" } ?: "")
                Text(entrant?.email ?: "")
                Text(entrant?.phoneNumber?.ifBlank { "No phone number" } ?: "")
            }

            // Cancel Box
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .border(3.dp, outline, CircleShape)
                    .clickable { navController.popBackStack() },
                contentAlignment = Alignment.Center
            ) {
                Text("✕", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }

        Spacer(Modifier.height(16.dp))

        Text("About Me", fontWeight = FontWeight.Bold)
        Text(entrant?.description?.ifBlank { "No description yet." } ?: "")

        Spacer(Modifier.height(24.dp))

        Text("Recent Campsites", fontSize = 22.sp, fontWeight = FontWeight.Medium)

        // Previous Campsites
        // weight(1f) lets the list take the leftover space so the buttons stay pinned at the bottom
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(recentCampsites) { campsite ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            appState.viewingCampsiteID = campsite
                            navController.navigate("campsiteDetails")
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Image of Campsite
                    // Placeholder box; replace with Image(painterResource(...)) or AsyncImage(...)
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )

                    Text(
                        text = campsite,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                    )

                    // Details icon ("click me for more info!"), emoji so no dependencies
                    Text("ℹ️", fontSize = 18.sp)

                    // Chevron
                    Text(
                        text = "›",
                        fontSize = 24.sp,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { showPolicyDialog = true },
                modifier = Modifier.weight(1f)
            ) { Text("View User Policy") }

            Button(
                onClick = { showEditDialog = true },
                modifier = Modifier.weight(1f)
            ) { Text("Edit User Profile") }
        }
    }

    // Edit User popup (Delete Profile lives here now)
    val current = entrant
    if (showEditDialog && current != null) {
        EditProfileDialog(
            entrant = current,
            onDismiss = { showEditDialog = false },
            onSave = { name, username, dob, email, phone, description ->
                current.name = name
                current.username = username
                current.dateOfBirth = dob
                current.email = email
                current.phoneNumber = phone
                current.description = description
                appState.entrants.updateEntrant(current)
                refreshTick++
                showEditDialog = false
            },
            onDeleteProfile = {
                appState.entrants.deleteEntrant(entrant!!)
                showEditDialog = false
                appState.userID = null
                navController.navigate("loginScreen")
            }
        )
    }

    if (showPolicyDialog) {
        PolicyDialog(onDismiss = { showPolicyDialog = false })
    }
}

@Composable
fun PolicyDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("User Policy") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PolicySection(
                    title = "1. Accounts",
                    body = "You must provide accurate information when creating your profile. " +
                            "You are responsible for all activity under your account and for " +
                            "keeping your login details private."
                )
                PolicySection(
                    title = "2. Reservations",
                    body = "Campsite reservations are held for you once confirmed. If you cannot " +
                            "attend, cancel at least 48 hours in advance so the spot can be " +
                            "offered to someone on the waiting list. Repeated no-shows may " +
                            "limit your ability to book."
                )
                PolicySection(
                    title = "3. Respect the Campsite",
                    body = "Leave no trace: pack out all garbage, follow posted fire restrictions, " +
                            "and keep noise down between 10 PM and 7 AM. Damage to campsite " +
                            "property may result in fees or account suspension."
                )
                PolicySection(
                    title = "4. Conduct",
                    body = "Harassment, discrimination, or unsafe behaviour toward other campers " +
                            "or staff is not tolerated and may result in removal from the platform."
                )
                PolicySection(
                    title = "5. Your Data",
                    body = "We store your name, contact details, and campsite history to run the " +
                            "service. We do not sell your personal information. You can edit or " +
                            "delete your profile at any time from your profile page."
                )
                PolicySection(
                    title = "6. Changes",
                    body = "This policy may be updated from time to time. Continued use of the " +
                            "app means you accept the current version."
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun PolicySection(title: String, body: String) {
    Column {
        Text(title, fontWeight = FontWeight.Bold)
        Text(body, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun EditProfileDialog(
    entrant: Entrant,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        username: String,
        dateOfBirth: String,
        email: String,
        phoneNumber: String,
        description: String
    ) -> Unit,
    onDeleteProfile: () -> Unit
) {
    // Draft state: edits only get committed when Save is pressed
    var name by remember { mutableStateOf(entrant.name) }
    var username by remember { mutableStateOf(entrant.username) }
    var dateOfBirth by remember { mutableStateOf(entrant.dateOfBirth) }
    var email by remember { mutableStateOf(entrant.email) }
    var phoneNumber by remember { mutableStateOf(entrant.phoneNumber) }
    var description by remember { mutableStateOf(entrant.description) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit User Profile") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dateOfBirth,
                    onValueChange = { dateOfBirth = it },
                    label = { Text("Date of Birth") },
                    placeholder = { Text("YYYY-MM-DD") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("About Me") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = onDeleteProfile,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Delete Profile") }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(
                    name,
                    username,
                    dateOfBirth,
                    email,
                    phoneNumber,
                    description
                ) }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}