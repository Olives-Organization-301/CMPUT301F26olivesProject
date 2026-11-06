package com.example.code

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import com.example.code.classes.Campsite
import com.example.code.ui.theme.CodeTheme
import android.util.Log

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


