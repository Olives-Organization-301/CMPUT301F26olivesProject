package com.example.code

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
// UI was built with the help of Claude : https://claude.ai/share/1c03451a-fc15-4299-b3cf-86965896ee5a
private val ScreenBackground = Color(0xFFFEF9F3)
private val FieldBackground = Color(0xFFF0E0D0)
private val ButtonBlue = Color(0xFF274690)
private val FieldLine = Color(0xFF333333)

@Composable
fun CampsiteOwnerLoginScreen(navController: NavController) {
    var owner by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Campsite Owner Login",//login
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(Modifier.height(48.dp))

        LoginField(
            label = "Owner Log In",//This is where the owner logs in
            value = owner,
            onValueChange = { owner = it },
            keyboardType = KeyboardType.Text,
            hidden = false
        )

        Spacer(Modifier.height(16.dp))

        LoginField(
            label = "Password",//password for login
            value = password,
            onValueChange = { password = it },
            keyboardType = KeyboardType.Password,
            hidden = true
        )

        Spacer(Modifier.height(36.dp))

        Button(
            onClick = { navController.navigate("campsiteOwnerMainScreen") },//takes you to campsite owner main screen
            modifier = Modifier
                .width(243.dp)
                .height(50.dp),
            shape = RoundedCornerShape(25.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ButtonBlue)
        ) {
            Text("Log in", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.White)
        }
    }
}

@Composable
private fun LoginField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType,
    hidden: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1A1A1A)
        )
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp),
            singleLine = true,
            shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp),//rounded corners to the screen
            visualTransformation = if (hidden) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,//can show password if you want
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),//keyboard type
            trailingIcon = {//Custom button
                IconButton(onClick = { onValueChange("") }) {
                    CancelIcon(tint = FieldLine)
                }
            },
            colors = TextFieldDefaults.colors(//changes background colors
                focusedContainerColor = FieldBackground,
                unfocusedContainerColor = FieldBackground,
                focusedIndicatorColor = ButtonBlue,
                unfocusedIndicatorColor = FieldLine,
                cursorColor = ButtonBlue
            )
        )
    }
}

@Composable
private fun CancelIcon(tint: Color) {//draws the cancellation icon
    Canvas(modifier = Modifier.size(26.dp)) {
        val stroke = 2.dp.toPx()
        val d = size.minDimension * 0.18f
        drawCircle(
            color = tint,
            radius = size.minDimension / 2f - stroke / 2f,
            style = Stroke(width = stroke)
        )
        drawLine(
            color = tint,
            start = Offset(center.x - d, center.y - d),
            end = Offset(center.x + d, center.y + d),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(center.x + d, center.y - d),
            end = Offset(center.x - d, center.y + d),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}