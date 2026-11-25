package com.example.code

import android.os.Handler
import android.os.Looper
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.code.classes.Administrator
import com.example.code.classes.AppState
import com.example.code.classes.CampsiteOwner
import com.example.code.classes.Entrant

private val ScreenBackground = Color(0xFFFEF9F3)
private val FieldBackground = Color(0xFFF0E0D0)
private val ButtonBlue = Color(0xFF274690)
private val FieldLine = Color(0xFF333333)
/**
 * made it function as how campsite owner log in is**/
@Composable
fun AdminLoginScreen(
    navController: NavController,
    appState: AppState,
    startInSignUp: Boolean = false  // true when opened from the "Sign up" button
) {
    var adminEmail by remember { mutableStateOf("") }
    var adminName by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(startInSignUp) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isSignUp) "Admin Sign Up" else "Admin Login",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(Modifier.height(36.dp))

        if (isSignUp) {
            LoginField(
                label = "Admin Name",
                value = adminName,
                onValueChange = { adminName = it },
                keyboardType = KeyboardType.Text
            )
            Spacer(Modifier.height(16.dp))
        }

        LoginField(
            label = "Admin Email (No password required)",
            value = adminEmail,
            onValueChange = { adminEmail = it },
            keyboardType = KeyboardType.Email
        )

        if (errorMessage != null) {
            Spacer(Modifier.height(12.dp))
            Text(errorMessage!!, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
        }

        Spacer(Modifier.height(36.dp))

        Button(
            onClick = {
                val cleanEmail = adminEmail.trim().lowercase()
                if (cleanEmail.isBlank()) {
                    errorMessage = "Please enter an email."
                    return@Button
                }

                appState.admins.searchAdminByEmail(cleanEmail) { existing ->
                    mainHandler.post {
                        if (isSignUp) {
                            if (existing != null) {
                                errorMessage = "exists"
                            } else {
                                // claude used to add entrant and campsite owner profiles with the same credentials as the new
                                // admin profile just created
                                // chat: https://claude.ai/share/cdf52d32-f73c-4fc2-b19e-b7015e307051
                                val newID = java.util.UUID.randomUUID().toString()
                                val username = adminName.ifBlank { cleanEmail.substringBefore("@") }

                                appState.admins.addAdmin(
                                    Administrator(username = username, email = cleanEmail, userID = newID)
                                )
                                appState.entrants.addEntrant(
                                    Entrant(username = username, email = cleanEmail, userID = newID)
                                )
                                appState.owners.addCampsiteOwner(
                                    CampsiteOwner(username = username, email = cleanEmail, userID = newID)
                                )

                                appState.userID = newID
                                appState.role = "admin"
                                navController.navigate("AdminMainSearchScreen") {
                                    popUpTo(ADMIN_LOGIN_ROUTE) { inclusive = true }
                                }
                            }
                        } else {
                            if (existing != null) {
                                appState.userID = existing.getUserID()
                                appState.role = "admin"
                                navController.navigate("AdminMainSearchScreen") {
                                    popUpTo(ADMIN_LOGIN_ROUTE) { inclusive = true }
                                }
                            } else {
                                errorMessage = "does not exist"
                            }
                        }
                    }
                }
            },
            modifier = Modifier
                .width(243.dp)
                .height(50.dp),
            shape = RoundedCornerShape(25.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ButtonBlue)
        ) {
            Text(if (isSignUp) "Sign Up" else "Log in", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.White)
        }

        Spacer(Modifier.height(16.dp))

        TextButton(onClick = { isSignUp = !isSignUp; errorMessage = null }) {
            Text(if (isSignUp) "Already have an account? Log in" else "Don't have an account? Sign up", color = ButtonBlue)
        }
    }
}

@Composable
private fun LoginField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType
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
            shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            trailingIcon = {
                IconButton(onClick = { onValueChange("") }) {
                    CancelIcon(tint = FieldLine)
                }
            },
            colors = TextFieldDefaults.colors(
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
private fun CancelIcon(tint: Color) {
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
