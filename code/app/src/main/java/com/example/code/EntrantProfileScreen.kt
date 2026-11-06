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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

// Conversation: https://claude.ai/share/b857df9c-812a-4adb-b696-38a5266149f7
@Composable
fun ProfileScreen(navController: NavController) {
    val campsitePlaceholder = remember { mutableStateListOf("Campsite1", "Campsite2", "Campsite3") }
    var showEditDialog by remember { mutableStateOf(false) }

    val outline = MaterialTheme.colorScheme.onSurface

    // Organize Entire UI
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {

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
                Text("Profile Name")
                Text("Username")
                Text("Date of Birth")
                Text("Email")
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
        Text(
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor " +
                    "incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis " +
                    "nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat."
        )

        Spacer(Modifier.height(24.dp))

        Text("Recent Campsites", fontSize = 22.sp, fontWeight = FontWeight.Medium)

        // Previous Campsites
        // weight(1f) lets the list take the leftover space so the buttons stay pinned at the bottom
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(campsitePlaceholder) { campsite ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { /* TODO: navigate to campsite details */ }
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
                onClick = {},
                modifier = Modifier.weight(1f)
            ) { Text("View User Policy") }

            Button(
                onClick = { showEditDialog = true },
                modifier = Modifier.weight(1f)
            ) { Text("Edit User Profile") }
        }
    }

    // Edit User popup (Delete Profile lives here now)
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit User Profile") },
            text = {
                Column {
                    // TODO: editable fields (name, username, about me, ...)
                    Text("Profile fields go here.")

                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = { /* TODO: confirm + delete profile */ },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Delete Profile") }
                }
            },
            confirmButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("Cancel") }
            }
        )
    }
}