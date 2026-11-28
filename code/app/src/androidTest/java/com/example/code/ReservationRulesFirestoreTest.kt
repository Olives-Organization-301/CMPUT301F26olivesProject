package com.example.code

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.code.classes.ChatMessage
import com.example.code.classes.Entrant
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/** US 02.11.01 integration coverage against the opt-in local Firestore emulator. */
class ReservationRulesFirestoreTest {
    @Test
    fun confirmsOnlyCurrentRulesAndRecordsAcknowledgementOnce() {
        val arguments = InstrumentationRegistry.getArguments()
        val host = arguments.getString("firestoreEmulatorHost")
        assumeTrue(host == "127.0.0.1" || host == "10.0.2.2")
        val port = arguments.getString("firestoreEmulatorPort")?.toIntOrNull() ?: 8188
        val id = UUID.randomUUID().toString()
        val app = FirebaseApp.initializeApp(
            ApplicationProvider.getApplicationContext<Context>(),
            FirebaseOptions.Builder().setProjectId("demo-olives-lottery")
                .setApplicationId("1:1234567890:android:reservationrulestest")
                .setApiKey("emulator-only").build(),
            "reservation-rules-$id"
        )
        try {
            val db = FirebaseFirestore.getInstance(app)
            db.useEmulator(requireNotNull(host), port)
            val entrant = Entrant(userID = "$id-entrant")
            val database = DatabaseEntrants(db)
            val campsite = db.collection("campsites").document("$id-campsite")
            val profile = db.collection("Entrants").document(entrant.getUserID())
            val publishedRules = listOf("Pack out waste.", "Quiet hours begin at 10 p.m.")
            Tasks.await(campsite.set(mapOf("rules" to publishedRules)), 15, TimeUnit.SECONDS)
            Tasks.await(profile.set(mapOf("campsiteLists" to mapOf(
                ACTIVE_CAMPSITES to listOf(campsite.id)
            ))), 15, TimeUnit.SECONDS)

            val confirmation = ChatMessage(
                entrantID = entrant.getUserID(), campsiteID = campsite.id, locationIndex = 0,
                stayDates = listOf("2026-12-10"), type = RESERVATION_CONFIRMED
            )
            val messages = db.collection("messages").whereEqualTo("entrantID", entrant.getUserID())
            assertFalse(confirm(database, entrant, confirmation, listOf("Pack out waste.")))
            assertTrue(Tasks.await(messages.get(), 15, TimeUnit.SECONDS).isEmpty)
            assertTrue(Tasks.await(profile.get(), 15, TimeUnit.SECONDS)
                .get(FieldPath.of("campsiteLists", ACTIVE_CAMPSITES)) == listOf(campsite.id))

            assertTrue(confirm(database, entrant, confirmation, publishedRules))
            assertTrue(confirm(database, entrant, confirmation, publishedRules))
            val result = Tasks.await(messages.get(), 15, TimeUnit.SECONDS).documents.single()
            assertEquals(true, result.getBoolean("rulesAcknowledged"))
            assertEquals(confirmation.stayDates, result.get("stayDates"))
            val booked = Tasks.await(profile.get(), 15, TimeUnit.SECONDS)
            assertEquals(publishedRules, booked.get(FieldPath.of("reservationAcknowledgedRules", campsite.id, "0")))
            assertEquals(listOf(campsite.id), booked.get(FieldPath.of("campsiteLists", RESERVED_CAMPSITES)))
        } finally {
            app.delete()
        }
    }

    private fun confirm(
        database: DatabaseEntrants,
        entrant: Entrant,
        message: ChatMessage,
        rules: List<String>,
    ): Boolean {
        val result = CompletableFuture<Boolean>()
        database.confirmStay(entrant, message, rules) { result.complete(it) }
        return result.get(15, TimeUnit.SECONDS)
    }
}
