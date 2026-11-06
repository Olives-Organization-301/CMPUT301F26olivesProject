package com.example.code

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button

//https://claude.ai/share/1c03451a-fc15-4299-b3cf-86965896ee5a. Prompt: Duplicate the figma drawing as best as you can.
data class  ChatMessage(
    val sender: String,
    val timestamp: String,
    val text: String,
    val fromOwner: Boolean
)

private val ChatBackground = Color(0xFFD4D0CF)
private val BubbleColor = Color(0xFF2B2B2B)
private val SubtitleColor = Color(0xFF666666)
@Composable
fun CampsiteChatScreen(
    navController: NavController,
    campsiteName: String = "Campsite Name",
    ownerName: String = "Owner Name",
    entrantName: String = "Entrant Name",
    messages: List<ChatMessage> = listOf(ChatMessage("Entrant Name", "Date, Time", "[Message Txt]", false),
        ChatMessage("Owner Name", "Date, Time", "[Message Txt]", true),
        ChatMessage("Entrant Name", "Date, Time", "[Message Txt]", false))

){
    Column(modifier = Modifier
        .fillMaxSize()
        .background(ChatBackground)){
        // To avoid clipping it into the top of the screen
        Spacer(Modifier.height(48.dp))

        Row {
            Button(onClick = {navController.popBackStack()},
                modifier = Modifier
                    .padding(vertical = 14.dp, horizontal = 14.dp))  {
                Text("Back")
            }

            Column(modifier = Modifier.padding(vertical = 28.dp, horizontal = 14.dp)) {
                Text(
                    text = campsiteName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                Text(
                    text = "Owner: $ownerName - Entrant: $entrantName",
                    fontSize = 12.sp,
                    color = SubtitleColor
                )
            }
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(messages){ message ->
                MessageRow(message)

            }
        }

    }

}

@Composable
private fun MessageRow(message: ChatMessage) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if(message.fromOwner)Alignment.End else Alignment.Start

    ) {
        Text(
            text = "${message.sender}-${message.timestamp}",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Text(
            text = message.text,
            fontSize = 18.sp,
            color = Color.White,
            modifier = Modifier
                .padding(top = 4.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(BubbleColor)
                .padding(horizontal = 24.dp, vertical = 12.dp)

        )
    }

}
