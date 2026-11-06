package com.example.code

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun EntrantAlertsScreen(navController: NavController){
    //TODO: Flesh out notifications
    val alerts = remember { mutableListOf<String>("Sky is blue", "I am tired", "Its so over") }
    Column(
        modifier = Modifier.fillMaxSize().padding(15.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Alerts",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "✕",
                fontSize = 24.sp,
                modifier = Modifier.clickable { navController.popBackStack() }
            )
        }
        LazyColumn() {
            itemsIndexed(alerts){
                index, alert -> AlertRow(index.toString(), alert)
            }
        }
    }
}

@Composable
fun AlertRow(
    name: String,
    body: String
){
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Column() {
            Text(
            text = name,
            fontSize = 30.sp,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            text = body,
            fontSize = 15.sp,
        )

        }
    }
}