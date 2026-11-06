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


    private val firstNames = listOf(
        "Alice", "Ben", "Carla", "Daniel", "Emma", "Felix", "Grace", "Hugo", "Iris", "Jack",
        "Kira", "Liam", "Maya", "Noah", "Olivia", "Paul", "Quinn", "Rosa", "Sam", "Tara"
    )
    private val lastNames = listOf(
        "Smith", "Johnson", "Brown", "Lee", "Garcia", "Miller", "Davis", "Wilson", "Moore", "Taylor"
    )
    private val campsiteNames = listOf(
        "Pine Hollow", "Lakeside Retreat", "Cedar Ridge", "Maple Grove", "Eagle Point",
        "Willow Creek", "Sunset Meadow", "Birch Bend", "Aspen Flats", "Riverbend"
    )
    private val locations = listOf(
        "Banff, AB", "Jasper, AB", "Canmore, AB", "Lake Louise, AB",
        "Waterton, AB", "Drumheller, AB", "Grande Cache, AB"
    )

    private fun randomName() = "${firstNames.random()} ${lastNames.random()}"

    private fun makeEmail(name: String, id: Int) =
        "${name.lowercase().replace(" ", ".")}$id@example.com"

    private fun randomPassword(length: Int = 10): String {
        val chars = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return (1..length).map { chars.random() }.joinToString("")
    }

    // Returns a 10-digit number with a Edmonton (780) area code that hasn't been used yet
    private fun uniquePhone(used: MutableSet<String>): String {
        while (true) {
            val phone = "780" + Random.nextInt(1000000, 9999999)
            if (used.add(phone)) return phone
        }
    }
}



