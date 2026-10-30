package com.example.code

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

/** # Search Screen
 *  Implementation of the Entrants' Login screen as seen in the Figma.
 *
 *  Prototype was gained from a Conversation with Android Studio's agent
 *  Prompt: "How do I Correctly Replicate this login screen"
 *
 */

private data class HomeColors(//Holds all the color tokens for the login screen
    val gradient: List<Color>,
    val title: Color,
    val subtitle: Color,
    val footer: Color,
    val primaryButton: Color,
    val primaryText: Color,
    val secondaryButton: Color,
    val secondaryText: Color
)

private val LightColors = HomeColors(//sets the colors for light mode and dark mode
    gradient = listOf(Color(0xFFFFF8EE), Color(0xFFFDE8CB)),
    title = Color(0xFF2B1B17),
    subtitle = Color(0xFF7A6A62),
    footer = Color(0xFF1A1A1A),
    primaryButton = Color(0xFF45238A),
    primaryText = Color.White,
    secondaryButton = Color(0xFF6B49B0),
    secondaryText = Color.White
)

private val DarkColors = HomeColors(
    gradient = listOf(Color(0xFF101828), Color(0xFF1F2B45)),
    title = Color.White,
    subtitle = Color(0xFF9AA5BD),
    footer = Color.White,
    primaryButton = Color(0xFFFFB84D),
    primaryText = Color(0xFF1A1A1A),
    secondaryButton = Color(0xFFFFE7BF),
    secondaryText = Color(0xFF1A1A1A)
)

private val Pink = Color(0xFFE91E63)

@Composable
fun LoginScreen(
    navController: NavController,
    darkTheme: Boolean = isSystemInDarkTheme()
) {
    LoginScreenContent(
        onSignUp = { navController.navigate("searchScreen") },//takes you to the campsite search screen for now, there will be a login screen soon
        onLogIn = { navController.navigate("searchScreen") },
        onPrivacyPolicy = {},
        darkTheme = darkTheme
    )
}

@Composable
fun LoginScreenContent(
    onSignUp: () -> Unit = {},
    onLogIn: () -> Unit = {},
    onPrivacyPolicy: () -> Unit = {},
    darkTheme: Boolean = isSystemInDarkTheme()//checks for dark theme
) {
    val colors = if (darkTheme) DarkColors else LightColors

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.horizontalGradient(colors.gradient))//fills the background with the gradient colours
            .systemBarsPadding()
    ) {
        Column(//adds the spacing  for the children vertically
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Section
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = buildAnnotatedString {//adds the fyreserve and the pink dot(has to be done seperately)
                        append("fyreserve")
                        withStyle(SpanStyle(color = Pink)) { append(".") }
                    },
                    style = TextStyle(
                        color = colors.title,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-1).sp
                    )
                )
                Text(
                    text = "your one-stop shop for campsite reservation",
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth(0.7f),
                    color = colors.subtitle,
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Center
                )
            }

            // Scenic Image Card Placeholder with Badge
            Box(
                modifier = Modifier//this is for the image that will be placed soon
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF2E4053), Color(0xFF1B2631))
                        )
                    )
            ) {
                Surface(
                    modifier = Modifier
                        .padding(14.dp)
                        .align(Alignment.TopStart),
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.25f)
                ) {
                    Text(
                        text = "National Park Access",//the image that will be added soon.
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Buttons Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                HomeButton("Sign up", colors.primaryButton, colors.primaryText, onSignUp)//login and signup buttons
                HomeButton("Log in", colors.secondaryButton, colors.secondaryText, onLogIn)
            }

            // Footer Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {//footers
                Text(
                    text = "© Olive Inc. 2026. All rights reserved",
                    color = colors.footer,
                    fontSize = 12.sp
                )
                Text(
                    text = "Privacy policy",

                    color = colors.footer,
                    fontSize = 12.sp,
                    textDecoration = TextDecoration.Underline
                )
            }
        }
    }
}

@Composable
private fun HomeButton(
    label: String,
    container: Color,
    content: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(26.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content
        )
    ) {
        Text(text = label, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}


@Preview(name = "Light Mode", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
fun LoginScreenLightPreview() {
    LoginScreenContent(darkTheme = false)
}

@Preview(name = "Dark Mode", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
fun LoginScreenDarkPreview() {
    LoginScreenContent(darkTheme = true)
}
