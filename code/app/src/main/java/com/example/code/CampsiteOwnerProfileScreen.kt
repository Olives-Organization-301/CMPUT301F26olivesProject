package com.example.code

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.code.classes.AppState
import com.example.code.classes.CampsiteOwner
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CampsiteOwnerProfileScreen(
    navController: NavController,
    appState: AppState
) {
    val context = LocalContext.current
    var owner by remember { mutableStateOf<CampsiteOwner?>(null) }
    var ownedCampsites by remember { mutableStateOf<List<com.example.code.classes.Campsite>>(emptyList()) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showPolicyDialog by remember { mutableStateOf(false) }
    var refreshTick by remember { mutableStateOf(0) }

    androidx.compose.runtime.LaunchedEffect(appState.userID, refreshTick) {
        val id = appState.userID
        if (!id.isNullOrBlank()) {
            appState.owners.searchCampsiteOwner(id) { found ->
                owner = found
                found?.viewCampsites(appState.campsites) { campsites ->
                    ownedCampsites = campsites
                }
            }
        }
    }

    val outline = MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        Spacer(Modifier.height(48.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            ProfilePicture(
                profileImage = owner?.profileImage.orEmpty(),
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Text(owner?.name?.ifBlank { "No name set" } ?: "Loading…", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("@${owner?.username ?: ""}", color = Color.Gray, fontSize = 14.sp)
                Text(owner?.dateOfBirth?.ifBlank { "No date of birth" } ?: "", fontSize = 12.sp)
                Text(owner?.email ?: "", fontSize = 12.sp)
                Text(owner?.phoneNumber?.ifBlank { "No phone number" } ?: "", fontSize = 12.sp)
            }

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
        Text(owner?.description?.ifBlank { "No description yet." } ?: "")

        Spacer(Modifier.height(24.dp))

        Text("My Campsites", fontSize = 22.sp, fontWeight = FontWeight.Medium)

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(ownedCampsites) { campsite ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            appState.viewingCampsiteID = campsite.campsiteID
                            navController.navigate("campsiteDetails")
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProfilePicture(
                        profileImage = campsite.thumbnailPhoto().orEmpty(),
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )

                    Text(
                        text = campsite.getName(),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                    )

                    Text("ℹ️", fontSize = 18.sp)
                    Text("›", fontSize = 24.sp, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { showPolicyDialog = true },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) { Text("Policy", fontSize = 12.sp, maxLines = 1) }

            Button(
                onClick = { showEditDialog = true },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) { Text("Edit Profile", fontSize = 12.sp, maxLines = 1) }

            Button(
                onClick = {
                    appState.userID = null
                    appState.role = null
                    DeviceIdentity.clearEntrantId(context)
                    navController.navigate("loginScreen") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) { Text("Log Out", fontSize = 12.sp, maxLines = 1, color = Color.White) }
        }
    }

    val current = owner
    if (showEditDialog && current != null) {
        EditCampsiteOwnerProfileDialog(
            owner = current,
            onDismiss = { showEditDialog = false },
            onSave = { name, username, dob, email, phone, description, profileImage, wantsNotifications ->
                current.name = name
                current.username = username
                current.dateOfBirth = dob
                current.email = email
                current.phoneNumber = phone
                current.description = description
                if (profileImage != current.profileImage) {
                    if (profileImage.isBlank()) {
                        deleteCampsiteOwnerImagesFromFirestore(current.getUserID())
                    } else {
                        saveCampsiteOwnerImageToFirestore(context, current.getUserID(), username, email, profileImage)
                    }
                }
                current.profileImage = profileImage
                current.wantsNotifications = wantsNotifications
                appState.owners.updateCampsiteOwner(current)
                refreshTick++
                showEditDialog = false
            },
            onDeleteProfile = {
                val id = current.getUserID()
                appState.owners.deleteCampsiteOwner(current)
                deleteCampsiteOwnerImagesFromFirestore(id)
                showEditDialog = false
                appState.userID = null
                appState.role = null
                DeviceIdentity.clearEntrantId(context)
                navController.navigate("loginScreen") {
                    popUpTo(0) { inclusive = true }
                }
            }
        )
    }

    if (showPolicyDialog) {
        PolicyDialog(onDismiss = { showPolicyDialog = false })
    }
}

@Composable
fun EditCampsiteOwnerProfileDialog(
    owner: CampsiteOwner,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        username: String,
        dateOfBirth: String,
        email: String,
        phoneNumber: String,
        description: String,
        profileImage: String,
        wantsNotifications: Boolean
    ) -> Unit,
    onDeleteProfile: () -> Unit
) {
    var name by remember { mutableStateOf(owner.name) }
    var username by remember { mutableStateOf(owner.username) }
    var dateOfBirth by remember { mutableStateOf(owner.dateOfBirth) }
    var email by remember { mutableStateOf(owner.email) }
    var phoneNumber by remember { mutableStateOf(owner.phoneNumber) }
    var description by remember { mutableStateOf(owner.description) }
    var profileImage by remember { mutableStateOf(owner.profileImage) }
    var wantsNotifications by remember { mutableStateOf(owner.wantsNotifications) }

    val (processingPhoto, takePhoto, openPhotoPicker) = rememberPhotoPicker { profileImage = it }

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

                Spacer(Modifier.height(8.dp))

                Text("Profile Picture", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProfilePicture(
                        profileImage = profileImage,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                    )

                    FlowRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = takePhoto,
                            enabled = !processingPhoto,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Take a Picture", fontSize = 11.sp, maxLines = 1)
                        }

                        Button(
                            onClick = openPhotoPicker,
                            enabled = !processingPhoto,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Choose from Gallery", fontSize = 11.sp, maxLines = 1)
                        }

                        if (profileImage.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    profileImage = ""
                                },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Remove", fontSize = 11.sp, maxLines = 1)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))

                Text("Preset Avatars (Select to Preview):", fontSize = 12.sp, color = Color.Gray)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .border(
                                width = if (profileImage == TEST_PFP_PROFILE) 3.dp else 1.dp,
                                color = if (profileImage == TEST_PFP_PROFILE) MaterialTheme.colorScheme.primary else Color.Gray,
                                shape = CircleShape
                            )
                            .clickable {
                                profileImage = TEST_PFP_PROFILE
                            }
                    ) {
                        Image(
                            painter = painterResource(R.drawable.default_profile_image),
                            contentDescription = "Test Profile Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .border(
                                width = if (profileImage == TEST_PFP_GIFT) 3.dp else 1.dp,
                                color = if (profileImage == TEST_PFP_GIFT) MaterialTheme.colorScheme.primary else Color.Gray,
                                shape = CircleShape
                            )
                            .clickable {
                                profileImage = TEST_PFP_GIFT
                            }
                    ) {
                        Image(
                            painter = painterResource(R.drawable.default_gift_image),
                            contentDescription = "Test Gift Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

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
                enabled = !processingPhoto,
                onClick = {
                    onSave(
                        name,
                        username,
                        dateOfBirth,
                        email,
                        phoneNumber,
                        description,
                        profileImage,
                        wantsNotifications
                    )
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

fun saveCampsiteOwnerImageToFirestore(
    context: Context,
    ownerID: String,
    username: String,
    email: String,
    profileImage: String
) {
    if (profileImage.isBlank() || ownerID.isBlank()) return
    try {
        val deviceId = DeviceIdentity.getOrCreate(context)
        val db = com.google.firebase.Firebase.firestore
        db.collection("images")
            .whereEqualTo("campsiteownerID", ownerID)
            .get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents.filterNot { it.contains("campsiteID") }) {
                    db.collection("images").document(doc.id).delete()
                }
                val docId = java.util.UUID.randomUUID().toString()
                val imageDoc = mapOf(
                    "campsiteownerID" to ownerID,
                    "deviceIdentity" to deviceId,
                    "username" to username,
                    "email" to email,
                    "profileImage" to profileImage,
                    "imageData" to profileImage,
                    "timestamp" to System.currentTimeMillis()
                )
                db.collection("images").document(docId).set(imageDoc)
            }
            .addOnFailureListener {
                val docId = java.util.UUID.randomUUID().toString()
                val imageDoc = mapOf(
                    "campsiteownerID" to ownerID,
                    "deviceIdentity" to deviceId,
                    "username" to username,
                    "email" to email,
                    "profileImage" to profileImage,
                    "imageData" to profileImage,
                    "timestamp" to System.currentTimeMillis()
                )
                db.collection("images").document(docId).set(imageDoc)
            }
    } catch (e: Exception) {
        android.util.Log.e("OwnerImageRecord", "Failed saving owner image to images collection", e)
    }
}

fun deleteCampsiteOwnerImagesFromFirestore(ownerID: String) {
    if (ownerID.isBlank()) return
    try {
        val db = com.google.firebase.Firebase.firestore
        db.collection("images")
            .whereEqualTo("campsiteownerID", ownerID)
            .get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents.filterNot { it.contains("campsiteID") }) {
                    db.collection("images").document(doc.id).delete()
                }
            }
    } catch (e: Exception) {
        android.util.Log.e("OwnerImageDelete", "Failed deleting images for owner $ownerID from Firestore", e)
    }
}
