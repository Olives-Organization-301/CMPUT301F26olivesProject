package com.example.code

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI Test cases for Entrant Login Screen.
 *
 * Developed with assistance from AI / Assistant session.
 * Prompt: "make test cases for login screen"
 */
@RunWith(AndroidJUnit4::class)
class EntrantLoginScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setUp() {
        composeTestRule.activity
    }

    @Test
    fun testLoginScreenDisplaysElements() {
        composeTestRule.setContent {
            LoginScreenContent()
        }

        // Verify title and primary buttons exist
        composeTestRule.onNodeWithText("fyreserve", substring = true).assertExists() // checks that all the texts exist.
        composeTestRule.onNodeWithText("Sign up").assertExists()
        composeTestRule.onNodeWithText("Log in").assertExists()
        composeTestRule.onNodeWithText("Campsite Owner Login").assertExists()
    }

    @Test
    fun testSignUpButtonTriggersCallback() {
        var signUpClicked = false
        composeTestRule.setContent {
            LoginScreenContent(
                onSignUp = { signUpClicked = true }
            )
        }

        composeTestRule.onNodeWithText("Sign up").performClick() // checks that clicking signup works
        assert(signUpClicked)
    }

    @Test
    fun testLogInButtonTriggersCallback() {
        var logInClicked = false
        composeTestRule.setContent {
            LoginScreenContent(
                onLogIn = { logInClicked = true }
            )
        }

        composeTestRule.onNodeWithText("Log in").performClick()
        assert(logInClicked)
    }

    @Test
    fun testOwnerLoginButtonTriggersCallback() {
        var ownerLoginClicked = false
        composeTestRule.setContent {
            LoginScreenContent(
                onOwnerLogin = { ownerLoginClicked = true }
            )
        }

        composeTestRule.onNodeWithText("Campsite Owner Login").performClick() // tests that campsite owner login was clicked
        assert(ownerLoginClicked)
    }
}
