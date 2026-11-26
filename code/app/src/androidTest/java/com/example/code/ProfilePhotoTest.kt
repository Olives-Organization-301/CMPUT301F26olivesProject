package com.example.code

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.code.classes.CampsiteOwner
import com.example.code.classes.Entrant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class ProfilePhotoTest {
    @get:Rule
    val compose = createComposeRule()

    /** Codex prompt: Verify that removing an entrant's photo can be canceled or saved. */
    @Test
    fun entrantPhotoRemovalRequiresSave() {
        val original = encodedPhoto()
        val entrant = Entrant(profileImage = original)
        verifyRemoval(original) { onSave, onDismiss ->
            EditProfileDialog(
                entrant = entrant,
                onDismiss = onDismiss,
                onSave = { _, _, _, _, _, _, image, _ -> onSave(image) },
                onDeleteProfile = {}
            )
        }
    }

    /** Codex prompt: Verify that an owner's saved photo renders and removal requires Save. */
    @Test
    fun ownerPhotoRemovalRequiresSave() {
        val original = encodedPhoto()
        val owner = CampsiteOwner(profileImage = original)
        verifyRemoval(original) { onSave, onDismiss ->
            EditCampsiteOwnerProfileDialog(
                owner = owner,
                onDismiss = onDismiss,
                onSave = { _, _, _, _, _, _, image, _ -> onSave(image) },
                onDeleteProfile = {}
            )
        }
    }

    private fun verifyRemoval(
        original: String,
        dialog: @androidx.compose.runtime.Composable ((String) -> Unit, () -> Unit) -> Unit
    ) {
        val editing = mutableStateOf(true)
        val saved = mutableStateOf(original)
        var submitted: String? = null
        compose.setContent {
            if (editing.value) {
                dialog({ image ->
                    submitted = image
                    saved.value = image
                    editing.value = false
                }, { editing.value = false })
            } else {
                ProfilePicture(saved.value)
            }
        }

        fun remove() {
            compose.onNodeWithText("Remove").performScrollTo()
            compose.waitUntil(5_000) {
                compose.onAllNodesWithContentDescription("Profile picture").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithContentDescription("Profile picture").assertIsDisplayed()
            compose.onNodeWithText("Remove").performClick()
            compose.onNodeWithText("Remove").assertDoesNotExist()
        }

        remove()
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle {
            assertNull(submitted)
            assertEquals(original, saved.value)
            editing.value = true
        }

        remove()
        compose.onNodeWithText("Save").performClick()
        compose.runOnIdle { assertEquals("", submitted) }
        compose.onNodeWithContentDescription("Profile picture").assertDoesNotExist()
    }

    private fun encodedPhoto(): String {
        val bitmap = Bitmap.createBitmap(80, 60, Bitmap.Config.ARGB_8888)
        return try {
            requireNotNull(encodeCampsitePhoto(bitmap))
        } finally {
            bitmap.recycle()
        }
    }
}
