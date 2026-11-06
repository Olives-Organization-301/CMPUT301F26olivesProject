package com.example.code

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Test Entrant Search Screen
 * Filled in with Sonnet 5.0
 * Prompt: "Can you finish this prompt for me?"
 * Conversation: https://claude.ai/share/ed96cd27-4c3b-46bf-b0dc-6139be152aba
 */
@RunWith(AndroidJUnit4::class)
class EntrantSearchTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>() // access MainActivity

    @Test
    fun testLevenshteinDist() {
        // Compare Kitten and Mitten

        // Compare Banff and Jasper
    }

    @Test
    fun filterTest() {
        // Make Campsites so that

        // Filter it to have
    }

    @Test
    fun searchTest() {
        // Search first Banff and make sure Banff is first
    }

    @Test
    fun clickToCampsiteDetails() {

    }
}