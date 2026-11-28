package com.example.code

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.example.code.classes.PreviewCampsite
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Codex prompt: "make test cases and document code" for the owner rules editor and defaults. */
class CampsiteOwnerRulesTest {
    @get:Rule
    val compose = createComposeRule()

    private var savedRules: List<String>? = null

    @Test
    fun newCampsiteRequiresRulesAndKeepsDraftWhenEditorCloses() {
        openForm()
        compose.onNodeWithText("Campsite name").performTextReplacement("Pine Creek")
        compose.onNodeWithText("Save").assertIsNotEnabled()

        openRulesEditor("Add rules and policies")
        replaceRules("   ")
        compose.onNodeWithText("Done").performClick()
        compose.onNodeWithText("Save").assertIsNotEnabled()

        openRulesEditor("Add rules and policies")
        replaceRules(" Quiet hours after 10 PM \n\n Pack out trash ")
        compose.onNodeWithText("Done").performClick()
        openRulesEditor("Edit rules and policies")
        compose.onNodeWithText(" Quiet hours after 10 PM \n\n Pack out trash ").assertExists()
        compose.onNodeWithText("Done").performClick()
        compose.onNodeWithText("Save").performClick()

        compose.runOnIdle {
            assertEquals(listOf("Quiet hours after 10 PM", "Pack out trash"), savedRules)
        }
    }

    @Test
    fun newCampsitePrefillsOwnerDefaultAndCanOverrideIt() {
        openForm(defaultRules = listOf("Pack out trash"))
        compose.onNodeWithText("Campsite name").performTextReplacement("Cedar Lake")
        openRulesEditor("Edit rules and policies")
        compose.onNodeWithText("Pack out trash").assertExists()
        replaceRules("No campfires")
        compose.onNodeWithText("Done").performClick()
        compose.onNodeWithText("Save").performClick()

        compose.runOnIdle { assertEquals(listOf("No campfires"), savedRules) }
    }

    @Test
    fun editingCampsiteUsesItsPublishedRulesInsteadOfTheOwnerDefault() {
        val existing = PreviewCampsite(
            name = "Cedar Lake",
            location = "Edmonton",
            startMillis = null,
            endMillis = null,
            startMillisRegis = null,
            endMillisRegis = null,
            rules = listOf("No campfires")
        )
        openForm(initial = existing, defaultRules = listOf("Pack out trash"))
        openRulesEditor("Edit rules and policies")
        compose.onNodeWithText("No campfires").assertExists()
        compose.onNodeWithText("Pack out trash").assertDoesNotExist()
        replaceRules("Campfires in designated rings only")
        compose.onNodeWithText("Done").performClick()
        compose.onNodeWithText("Save").performClick()

        compose.runOnIdle {
            assertEquals(listOf("Campfires in designated rings only"), savedRules)
        }
    }

    private fun openForm(initial: PreviewCampsite? = null, defaultRules: List<String> = emptyList()) {
        compose.setContent {
            AddEditCampsite(
                title = if (initial == null) "Add Campsite" else "Edit Campsite",
                initial = initial,
                onDismiss = {},
                defaultRules = defaultRules,
                onSave = { savedRules = it.rules }
            )
        }
    }

    private fun openRulesEditor(button: String) {
        compose.onNodeWithText(button).performScrollTo().performClick()
    }

    private fun replaceRules(text: String) {
        compose.onNodeWithText("Campsite rules and policies").performTextReplacement(text)
    }
}
