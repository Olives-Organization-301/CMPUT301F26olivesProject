package com.example.code

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.code.classes.AppState
import com.example.code.classes.Campsite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AdminImagesScreen(navController: NavController, appState: AppState) {
    // appState.campsites is a guess: whatever AppState calls its DatabaseCampsites
    val db = appState.campsites
    var toRemove by remember { mutableStateOf<Campsite?>(null) }

    // Live list; only campsites that actually have an uploaded photo
    val withPhotos = db.campsites.filter { !it.detailsPhoto().isNullOrBlank() }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Spacer(Modifier.height(48.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Uploaded images", fontSize = 24.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text("✕", fontSize = 24.sp, modifier = Modifier.clickable { navController.popBackStack() })
        }
        Spacer(Modifier.height(12.dp))

        if (withPhotos.isEmpty()) {
            Text("No uploaded images")
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(withPhotos, key = { it.campsiteID }) { campsite ->
                    val photo = campsite.detailsPhoto().orEmpty()
                    // Decode off the main thread; the photos are 1280px wide
                    val bitmap by produceState<ImageBitmap?>(null, photo) {
                        value = withContext(Dispatchers.Default) { decodeBase64ToImageBitmap(photo) }
                    }
                    Card {
                        Column {
                            bitmap?.let {
                                Image(
                                    bitmap = it,
                                    contentDescription = campsite.getName(),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxWidth().height(120.dp)
                                )
                            }
                            Text(campsite.getName(), modifier = Modifier.padding(8.dp), maxLines = 1)
                            TextButton(onClick = { toRemove = campsite }) { Text("Remove") }
                        }
                    }
                }
            }
        }
    }

    toRemove?.let { campsite ->
        AlertDialog(
            onDismissRequest = { toRemove = null },
            title = { Text("Remove image?") },
            text = { Text("The photo for \"${campsite.getName()}\" will be replaced with the default image.") },
            confirmButton = {
                Button(onClick = {
                    db.removeCampsiteImage(campsite.campsiteID)
                    toRemove = null
                }) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { toRemove = null }) { Text("Cancel") } }
        )
    }
}