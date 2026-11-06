package com.example.code

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.code.classes.Campsite
import com.example.code.ui.theme.CodeTheme
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID
import kotlin.random.Random

@RequiresApi(Build.VERSION_CODES.O)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CodeTheme {
                val campsiteDatabase = remember { mutableStateListOf<Campsite>() }
                Log.d("Firestore", "seed function started")
                CampsiteNavController(campsiteDatabase)
            }
        }
    }
}


