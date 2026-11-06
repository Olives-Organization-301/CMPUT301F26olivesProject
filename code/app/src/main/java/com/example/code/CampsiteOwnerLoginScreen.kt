package com.example.code

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

@Composable
fun CampsiteOwnerLoginScreen(
    navController: NavController,
    darkTheme: Boolean = isSystemInDarkTheme()
) {
    var ownerUsername by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val backgroundColor = if (darkTheme) Color(0xFF101828) else Color(0xFFFFF8EE)
    val textColor = if (darkTheme) Color.White else Color(0xFF2B1B17)
    val fieldBackgroundColor = if (darkTheme) Color(0xFF1F2B45) else Color(0xFFF2EBE1)
    val buttonColor = if (darkTheme) Color(0xFFFFB84D) else Color(0xFF2E3A67)
    val buttonTextColor = if (darkTheme) Color(0xFF1A1A1A) else Color.White

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .systemBarsPadding()
            .padding(28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Title
            Text(
                text = "Campsite Owner Login",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = textColor
                ),
                modifier = Modifier.padding(bottom = 48.dp)
            )

            // Owner Log In Field
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Owner Log In",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = textColor,
                        fontWeight = FontWeight.Medium
                    )
                )
                OutlinedTextField(
                    value = ownerUsername,
                    onValueChange = { ownerUsername = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = fieldBackgroundColor,
                        unfocusedContainerColor = fieldBackgroundColor,
                        disabledContainerColor = fieldBackgroundColor,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    trailingIcon = {
                        if (ownerUsername.isNotEmpty()) {
                            IconButton(onClick = { ownerUsername = "" }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear text", tint = Color.Gray)
                            }
                        }
                    }
                )
            }

            Spacer(Modifier.height(20.dp))

            // Password Field
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Password",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = textColor,
                        fontWeight = FontWeight.Medium
                    )
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = fieldBackgroundColor,
                        unfocusedContainerColor = fieldBackgroundColor,
                        disabledContainerColor = fieldBackgroundColor,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    trailingIcon = {
                        if (password.isNotEmpty()) {
                            IconButton(onClick = { password = "" }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear text", tint = Color.Gray)
                            }
                        }
                    }
                )
            }

            Spacer(Modifier.height(36.dp))

            // Log in Button
            Button(
                onClick = { navController.navigate("campsiteOwnerMainScreen") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor,
                    contentColor = buttonTextColor
                )
            ) {
                Text(
                    text = "Log in",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Composable
fun CampsiteOwnerLoginPreview() {
    CampsiteOwnerLoginScreen(navController = rememberNavController(), darkTheme = false)
}
