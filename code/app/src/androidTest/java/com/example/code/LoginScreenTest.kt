package com.example.code

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loginScreen_displaysHeaderAndSubtitle() {
        composeTestRule.setContent {
            LoginScreenContent()
        }

        // Verify title text "fyreserve." is displayed
        composeTestRule
            .onNodeWithText("fyreserve.")
            .assertIsDisplayed()

        // Verify subtitle text is displayed
        composeTestRule
            .onNodeWithText("your one-stop shop for campsite reservation")
            .assertIsDisplayed()
    }

    @Test
    fun loginScreen_displaysButtonsAndFooter() {
        composeTestRule.setContent {
            LoginScreenContent()
        }

        // Verify Sign up and Log in buttons are displayed
        composeTestRule
            .onNodeWithText("Sign up")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Log in")
            .assertIsDisplayed()

        // Verify footer text
        composeTestRule
            .onNodeWithText("© Olive Inc. 2026. All rights reserved")
            .assertIsDisplayed()
    }

    @Test
    fun loginScreen_signUpClick_triggersCallback() {
        var signUpClicked = false

        composeTestRule.setContent {
            LoginScreenContent(
                onSignUp = { signUpClicked = true }
            )
        }

        composeTestRule
            .onNodeWithText("Sign up")
            .performClick()

        assertTrue("Expected onSignUp callback to be invoked", signUpClicked)
    }

    @Test
    fun loginScreen_logInClick_triggersCallback() {
        var logInClicked = false

        composeTestRule.setContent {
            LoginScreenContent(
                onLogIn = { logInClicked = true }
            )
        }

        composeTestRule
            .onNodeWithText("Log in")
            .performClick()

        assertTrue("Expected onLogIn callback to be invoked", logInClicked)
    }
}
