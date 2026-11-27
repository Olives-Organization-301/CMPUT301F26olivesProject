package com.example.code

import android.os.Build
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.example.code.classes.AppState
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.util.concurrent.TimeUnit

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@RunWith(AndroidJUnit4::class)
class ReviewerQrFirestoreTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    /** Codex prompt: Scan the demo QR and verify the entry in both live Firestore records. */
    @Test
    @ReviewerFirestoreWrite
    fun reviewerQrJoinPersistsOnServer() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("writeReviewerFirestore") == "true")
        val entrantID = "olives_reviewer_20261126_entrant_20"
        val campsiteID = "olives_reviewer_20261126_site_4"
        val db = FirebaseFirestore.getInstance()
        fun read(path: String) = Tasks.await(db.document(path).get(Source.SERVER), 20, TimeUnit.SECONDS)
        val campsite = read("campsites/$campsiteID")
        val end = LocalDate.parse(requireNotNull(campsite.getString("lotteryEndDate")).substringBefore('T'))
        val active = !LocalDate.now().isAfter(end)
        val list = if (active) ACTIVE_CAMPSITES else WAITING_LIST
        val joinLabel = if (active) "Join Active Lottery" else "Join Waiting List"
        val alreadyJoined = (campsite.get("waitingList") as List<*>).contains(entrantID)

        val state = AppState().apply {
            userID = entrantID
            role = "entrant"
        }
        val image = createCampsiteQrBitmap(campsiteID)
        composeTestRule.setContent {
            CompositionLocalProvider(LocalQrTestImage provides { image }) {
                CampsiteNavController(state, startDestination = "mainSearchScreen")
            }
        }
        composeTestRule.onNodeWithText("QR").performClick()
        composeTestRule.onNodeWithText("Tap to choose a QR code image").performClick()
        composeTestRule.waitUntil(15_000) {
            composeTestRule.onAllNodes(hasText("View Campsite") and isEnabled()).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("View Campsite").performClick()
        if (!alreadyJoined) {
            composeTestRule.waitUntil(15_000) {
                composeTestRule.onAllNodes(hasText(joinLabel) and isEnabled()).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText(joinLabel).performClick()
            composeTestRule.onNodeWithText("Agree").performClick()
            composeTestRule.waitUntil(20_000) {
                composeTestRule.onAllNodesWithText("OK").fetchSemanticsNodes().isNotEmpty()
            }
        }
        assertTrue((read("campsites/$campsiteID").get("waitingList") as List<*>).contains(entrantID))
        val lists = read("Entrants/$entrantID").get("campsiteLists") as Map<*, *>
        assertTrue(if (alreadyJoined) {
            listOf(WAITING_LIST, ACTIVE_CAMPSITES).any { (lists[it] as List<*>).contains(campsiteID) }
        } else {
            (lists[list] as List<*>).contains(campsiteID)
        })
    }
}
