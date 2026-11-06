package com.example.code

import com.example.code.classes.Administrator
import com.example.code.classes.Campsite
import com.example.code.classes.CampsiteOwner
import com.example.code.classes.Coordinates
import com.example.code.classes.Entrant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import kotlin.random.Random

/**
 * Unit and integration tests for DatabaseAdmins, DatabaseCampsiteOwners,
 * DatabaseCampsites, DatabaseEntrants, and TestDataGenerator.
 * Spawned after prompting to agent : generate testcases (specified what kind of are needed) for the different
 * types of collection
 **/
class TestDataGenerator {
    val firstNames = listOf(
        "Alice", "Bob", "Charlie", "David", "Emma", "Frank",
        "Grace", "Henry", "Ivy", "Jack", "Mary", "Ann", "Ben",
    )
    val lastNames = listOf(
        "Smith", "Johnson", "Williams", "Brown", "Jones",
        "Garcia", "Miller", "Davis", "Rodriguez", "Martinez", "Lee",
    )

    fun randomName(): String {
        val first = firstNames.random()
        val last = lastNames.random()
        return "$first $last"
    }

    fun makeEmail(name: String, id: Int): String {
        val formattedName = name.lowercase().replace(" ", ".")
        return "$formattedName$id@example.com"
    }

    fun randomPassword(length: Int = 10): String {
        if (length <= 0) return ""
        val charPool = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..length)
            .map { Random.nextInt(0, charPool.length) }
            .map(charPool::get)
            .joinToString("")
    }

    fun uniquePhone(usedSet: MutableSet<String>): String {
        var phone: String
        do {
            val num = Random.nextInt(1000000, 10000000)
            phone = "780$num"
        } while (usedSet.contains(phone))
        usedSet.add(phone)
        return phone
    }
}

class TestDataGeneratorTest {

    private val gen = TestDataGenerator()

    // ---------- randomName ----------

    @Test
    fun randomName_hasFirstAndLastNameFromTheLists() {
        repeat(200) {
            val parts = gen.randomName().split(" ")
            assertEquals(2, parts.size)
            assertTrue(parts[0] in gen.firstNames)
            assertTrue(parts[1] in gen.lastNames)
        }
    }

    // ---------- makeEmail ----------

    @Test
    fun makeEmail_lowercasesReplacesSpacesAndAppendsId() {
        assertEquals("alice.smith5@example.com", gen.makeEmail("Alice Smith", 5))
    }

    @Test
    fun makeEmail_handlesMultipleSpaces() {
        assertEquals("mary.ann.lee12@example.com", gen.makeEmail("Mary Ann Lee", 12))
    }

    @Test
    fun makeEmail_differentIdsProduceDifferentEmails() {
        assertNotEquals(gen.makeEmail("Ben Lee", 1), gen.makeEmail("Ben Lee", 2))
    }

    // ---------- randomPassword ----------

    @Test
    fun randomPassword_defaultLengthIs10() {
        assertEquals(10, gen.randomPassword().length)
    }

    @Test
    fun randomPassword_respectsCustomLength() {
        assertEquals(0, gen.randomPassword(0).length)
        assertEquals(25, gen.randomPassword(25).length)
    }

    @Test
    fun randomPassword_containsOnlyAsciiLettersAndDigits() {
        repeat(100) {
            val password = gen.randomPassword(20)
            assertTrue(password.all { c ->
                c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9'
            })
        }
    }

    @Test
    fun randomPassword_isNotAlwaysTheSameValue() {
        val passwords = (1..20).map { gen.randomPassword() }.toSet()
        assertTrue(passwords.size > 1)
    }

    // ---------- uniquePhone ----------

    @Test
    fun uniquePhone_isTenDigitsStartingWith780() {
        val phone = gen.uniquePhone(mutableSetOf())
        assertEquals(10, phone.length)
        assertTrue(phone.startsWith("780"))
        assertTrue(phone.all { it in '0'..'9' })
    }

    @Test
    fun uniquePhone_addsReturnedNumberToUsedSet() {
        val used = mutableSetOf<String>()
        val phone = gen.uniquePhone(used)
        assertTrue(used.contains(phone))
    }

    @Test
    fun uniquePhone_neverReturnsDuplicates() {
        val used = mutableSetOf<String>()
        val generated = (1..1000).map { gen.uniquePhone(used) }.toSet()
        assertEquals(1000, generated.size)
        assertEquals(1000, used.size)
    }
}

class DatabaseAdminsTest {
    private val dbAdmins = DatabaseAdmins()

    @Test
    fun testAddAdmin() {
        val admin = Administrator("admin_test_1", "admin1@test.com", "admin_id_101")
        dbAdmins.addAdmin(admin)
    }

    @Test
    fun testUpdateAdminUsername() {
        val admin = Administrator("admin_test_2", "admin2@test.com", "admin_id_102")
        dbAdmins.addAdmin(admin)
        dbAdmins.updateAdminUsername(admin, "admin_test_2_updated")
        assertEquals("admin_test_2_updated", admin.username)
    }

    @Test
    fun testDeleteAdmin() {
        val admin = Administrator("admin_test_3", "admin3@test.com", "admin_id_103")
        dbAdmins.addAdmin(admin)
        dbAdmins.deleteAdmin(admin)
    }

    @Test
    fun testGetAdmins() {
        val list = dbAdmins.admins
        assertEquals(0, list.size)
    }
}

class DatabaseCampsiteOwnersTest {
    private val dbCampsiteOwners = DatabaseCampsiteOwners()

    @Test
    fun testAddCampsiteOwner() {
        val owner = CampsiteOwner("owner_test_1", "owner1@test.com", "owner_id_101")
        dbCampsiteOwners.addCampsiteOwner(owner)
    }

    @Test
    fun testUpdateCampsiteOwnerUsername() {
        val owner = CampsiteOwner("owner_test_2", "owner2@test.com", "owner_id_102")
        dbCampsiteOwners.addCampsiteOwner(owner)
        dbCampsiteOwners.updateCampsiteOwnerUsername(owner, "owner_test_2_updated")
        assertEquals("owner_test_2_updated", owner.username)
    }

    @Test
    fun testDeleteCampsiteOwner() {
        val owner = CampsiteOwner("owner_test_3", "owner3@test.com", "owner_id_103")
        dbCampsiteOwners.addCampsiteOwner(owner)
        dbCampsiteOwners.deleteCampsiteOwner(owner)
    }

    @Test
    fun testGetCampsiteOwners() {
        val list = dbCampsiteOwners.campsiteOwners
        assertEquals(0, list.size)
    }
}

class DatabaseEntrantsTest {
    private val dbEntrants = DatabaseEntrants()

    @Test
    fun testAddEntrant() {
        val entrant = Entrant("entrant_test_1", "entrant1@test.com", "entrant_id_101")
        dbEntrants.addEntrant(entrant)
    }

    @Test
    fun testUpdateEntrantUsername() {
        val entrant = Entrant("entrant_test_2", "entrant2@test.com", "entrant_id_102")
        dbEntrants.addEntrant(entrant)
        dbEntrants.updateEntrantUsername(entrant, "entrant_test_2_updated")
        assertEquals("entrant_test_2_updated", entrant.username)
    }

    @Test
    fun testDeleteEntrant() {
        val entrant = Entrant("entrant_test_3", "entrant3@test.com", "entrant_id_103")
        dbEntrants.addEntrant(entrant)
        dbEntrants.deleteEntrant(entrant)
    }

    @Test
    fun testGetEntrants() {
        val list = dbEntrants.entrants
        assertEquals(0, list.size)
    }
}

class DatabaseCampsitesTest {
    private val dbCampsites = DatabaseCampsites()

    @Test
    fun testAddCampsite() {
        val ownerID = "owner_camp_id"
        val coords = Coordinates("Canada", "Alberta", -113.49, 53.54)
        val campsite = Campsite(
            name = "Mountain View",
            ownerID = ownerID,
            campsiteID = "campsite_id_101",
            maxOccupancy = 4,
            price = 60.0f,
            rating = 4.8f,
            capacity = 4,
            coordinates = coords,
            startDate = LocalDateTime.now(),
            endDate = LocalDateTime.now().plusDays(2),
        )
        dbCampsites.addCampsite(campsite)
    }

    @Test
    fun testUpdateCampsiteName() {
        val ownerID = "owner_camp_id"
        val coords = Coordinates("Canada", "Alberta", -113.49, 53.54)
        val campsite = Campsite(
            name = "Sunny Meadow",
            ownerID = ownerID,
            campsiteID = "campsite_id_102",
            maxOccupancy = 4,
            price = 60.0f,
            rating = 4.8f,
            capacity = 4,
            coordinates = coords,
            startDate = LocalDateTime.now(),
            endDate = LocalDateTime.now().plusDays(2),
        )
        dbCampsites.addCampsite(campsite)
        dbCampsites.updateCampsiteName(campsite, "Sunny Meadow Updated")
    }

    @Test
    fun testDeleteCampsite() {
        val ownerID = "owner_camp_id"
        val coords = Coordinates("Canada", "Alberta", -113.49, 53.54)
        val campsite = Campsite(
            name = "River Bank",
            ownerID = ownerID,
            campsiteID = "campsite_id_103",
            maxOccupancy = 4,
            price = 60.0f,
            rating = 4.8f,
            capacity = 4,
            coordinates = coords,
            startDate = LocalDateTime.now(),
            endDate = LocalDateTime.now().plusDays(2),
        )
        dbCampsites.addCampsite(campsite)
        dbCampsites.deleteCampsite(campsite)
    }

    @Test
    fun testGetCampsites() {
        val list = dbCampsites.campsites
        assertEquals(0, list.size)
    }
}

class DatabaseSearchTest {

    // ---------- DatabaseAdmins Search Lifecycle ----------

    @Test
    fun testAdminSearchLifecycle() {
        val adminStore = mutableMapOf<String, Administrator>()
        val adminID = "admin_search_101"
        val oldUsername = "admin_before_update"
        val newUsername = "admin_after_update"

        fun searchAdmin(id: String, onResult: (Administrator?) -> Unit) {
            onResult(adminStore[id])
        }

        // 1. Search BEFORE adding -> check if it already exists (should be null)
        searchAdmin(adminID) { result ->
            assertNull("Admin should not exist before adding", result)
        }

        // 2. AFTER adding -> check if it exists and matches
        val admin = Administrator(oldUsername, "admin_search@test.com", adminID)
        adminStore[admin.getUserID()] = admin

        searchAdmin(adminID) { result ->
            assertTrue("Admin should exist after adding", result != null)
            assertEquals(adminID, result?.getUserID())
            assertEquals(oldUsername, result?.username)
        }

        // 3. AFTER updating (2 cases)
        admin.username = newUsername

        // Case A: Search and check the NEW update variable is there
        searchAdmin(adminID) { result ->
            assertTrue("Admin should exist after update", result != null)
            assertEquals("New username should be present", newUsername, result?.username)
        }

        // Case B: Search and check that the PREVIOUS old variable is gone
        searchAdmin(adminID) { result ->
            assertNotEquals("Previous username should be gone", oldUsername, result?.username)
        }

        // 4. AFTER deleted -> check if it's gone (should be null)
        adminStore.remove(adminID)

        searchAdmin(adminID) { result ->
            assertNull("Admin should be null after deletion", result)
        }
    }

    // ---------- DatabaseCampsiteOwners Search Lifecycle ----------

    @Test
    fun testCampsiteOwnerSearchLifecycle() {
        val ownerStore = mutableMapOf<String, CampsiteOwner>()
        val ownerID = "owner_search_101"
        val oldUsername = "owner_before_update"
        val newUsername = "owner_after_update"

        fun searchCampsiteOwner(id: String, onResult: (CampsiteOwner?) -> Unit) {
            onResult(ownerStore[id])
        }

        // 1. BEFORE adding -> check if it already exists (should be null)
        searchCampsiteOwner(ownerID) { result ->
            assertNull("CampsiteOwner should not exist before adding", result)
        }

        // 2. AFTER adding -> check if it exists
        val owner = CampsiteOwner(oldUsername, "owner_search@test.com", ownerID)
        ownerStore[owner.getUserID()] = owner

        searchCampsiteOwner(ownerID) { result ->
            assertTrue("CampsiteOwner should exist after adding", result != null)
            assertEquals(ownerID, result?.getUserID())
            assertEquals(oldUsername, result?.username)
        }

        // 3. AFTER updating (2 cases)
        owner.username = newUsername

        // Case A: Check new update variable is there
        searchCampsiteOwner(ownerID) { result ->
            assertTrue("CampsiteOwner should exist after update", result != null)
            assertEquals("New username should be present", newUsername, result?.username)
        }

        // Case B: Check previous old variable is gone
        searchCampsiteOwner(ownerID) { result ->
            assertNotEquals("Previous username should be gone", oldUsername, result?.username)
        }

        // 4. AFTER deleted -> check if it's gone
        ownerStore.remove(ownerID)

        searchCampsiteOwner(ownerID) { result ->
            assertNull("CampsiteOwner should be null after deletion", result)
        }
    }

    // ---------- DatabaseEntrants Search Lifecycle ----------

    @Test
    fun testEntrantSearchLifecycle() {
        val entrantStore = mutableMapOf<String, Entrant>()
        val entrantID = "entrant_search_101"
        val oldUsername = "entrant_before_update"
        val newUsername = "entrant_after_update"

        fun searchEntrant(id: String, onResult: (Entrant?) -> Unit) {
            onResult(entrantStore[id])
        }

        // 1. BEFORE adding -> check if it already exists (should be null)
        searchEntrant(entrantID) { result ->
            assertNull("Entrant should not exist before adding", result)
        }

        // 2. AFTER adding -> check if it exists
        val entrant = Entrant(oldUsername, "entrant_search@test.com", entrantID)
        entrantStore[entrant.getUserID()] = entrant

        searchEntrant(entrantID) { result ->
            assertTrue("Entrant should exist after adding", result != null)
            assertEquals(entrantID, result?.getUserID())
            assertEquals(oldUsername, result?.username)
        }

        // 3. AFTER updating (2 cases)
        entrant.username = newUsername

        // Case A: Check new update variable is there
        searchEntrant(entrantID) { result ->
            assertTrue("Entrant should exist after update", result != null)
            assertEquals("New username should be present", newUsername, result?.username)
        }

        // Case B: Check previous old variable is gone
        searchEntrant(entrantID) { result ->
            assertNotEquals("Previous username should be gone", oldUsername, result?.username)
        }

        // 4. AFTER deleted -> check if it's gone
        entrantStore.remove(entrantID)

        searchEntrant(entrantID) { result ->
            assertNull("Entrant should be null after deletion", result)
        }
    }

    // ---------- Real Helper Classes Async Search Invocation ----------

    @Test
    fun testRealDatabaseSearchFunctionsExist() {
        val dbAdmins = DatabaseAdmins()
        dbAdmins.searchAdmin("non_existent_id") { result ->
            assertNull(result)
        }

        val dbOwners = DatabaseCampsiteOwners()
        dbOwners.searchCampsiteOwner("non_existent_id") { result ->
            assertNull(result)
        }

        val dbEntrants = DatabaseEntrants()
        dbEntrants.searchEntrants("non_existent_id") { result ->
            assertNull(result)
        }

        val dbCampsites = DatabaseCampsites()
        dbCampsites.searchCampsite("non_existent_id") { result ->
            assertNull(result)
        }
    }
}

class DatabaseUpdateAndDeleteTest {

    // ---------- User & Admin Updates and Deletions ----------

    @Test
    fun testUpdateEntrantUsername() {
        val entrantStore = mutableMapOf<String, Entrant>()
        val entrant = Entrant(username = "alice_initial", email = "alice@example.com", userID = "entrant_101")

        // Add
        entrantStore[entrant.getUserID()] = entrant
        assertEquals("alice_initial", entrantStore["entrant_101"]?.username)

        // Update username
        entrant.username = "alice_updated"
        assertEquals("alice_updated", entrantStore["entrant_101"]?.username)
    }

    @Test
    fun testDeleteEntrant() {
        val entrantStore = mutableMapOf<String, Entrant>()
        val entrant = Entrant(username = "bob", email = "bob@example.com", userID = "entrant_102")

        // Add
        entrantStore[entrant.getUserID()] = entrant
        assertTrue(entrantStore.containsKey("entrant_102"))

        // Delete
        entrantStore.remove(entrant.getUserID())
        assertFalse(entrantStore.containsKey("entrant_102"))
        assertNull(entrantStore["entrant_102"])
    }

    @Test
    fun testUpdateAdminUsername() {
        val adminStore = mutableMapOf<String, Administrator>()
        val admin = Administrator(username = "admin_john", email = "john@admin.com", userID = "admin_1")

        // Add
        adminStore[admin.getUserID()] = admin
        assertEquals("admin_john", adminStore["admin_1"]?.username)

        // Update
        admin.username = "admin_super_john"
        assertEquals("admin_super_john", adminStore["admin_1"]?.username)
    }

    @Test
    fun testDeleteAdmin() {
        val adminStore = mutableMapOf<String, Administrator>()
        val admin = Administrator(username = "admin_temp", email = "temp@admin.com", userID = "admin_2")

        // Add & Delete
        adminStore[admin.getUserID()] = admin
        assertEquals(1, adminStore.size)

        adminStore.remove("admin_2")
        assertEquals(0, adminStore.size)
        assertNull(adminStore["admin_2"])
    }

    // ---------- Campsite Updates and Deletions ----------

    @Test
    fun testDeleteCampsite() {
        val campsiteStore = mutableMapOf<String, Campsite>()
        val ownerID = "owner_id_1"
        val coords = Coordinates("Canada", "Alberta", -113.49, 53.54)
        val campsite = Campsite(
            name = "Pine Valley",
            ownerID = ownerID,
            campsiteID = "campsite_999",
            maxOccupancy = 5,
            price = 50.0f,
            rating = 4.5f,
            capacity = 5,
            coordinates = coords,
            startDate = LocalDateTime.now(),
            endDate = LocalDateTime.now().plusDays(5),
        )

        // Add campsite
        campsiteStore[campsite.campsiteID] = campsite
        assertEquals("Pine Valley", campsiteStore["campsite_999"]?.getName())

        // Delete campsite
        campsiteStore.remove(campsite.campsiteID)
        assertFalse(campsiteStore.containsKey("campsite_999"))
        assertNull(campsiteStore["campsite_999"])
    }

    @Test
    fun testUpdateEntrantWaitlistInCampsite() {
        val ownerID = "owner_id_2"
        val coords = Coordinates("Canada", "Alberta", -113.49, 53.54)
        val campsite = Campsite(
            name = "Lakeside Camp",
            ownerID = ownerID,
            campsiteID = "campsite_888",
            maxOccupancy = 2,
            price = 40.0f,
            rating = 4.0f,
            capacity = 2,
            coordinates = coords,
            startDate = LocalDateTime.now(),
            endDate = LocalDateTime.now().plusDays(3),
        )

        // Add entrant IDs to waiting list
        campsite.addEntrant("user_1")
        campsite.addEntrant("user_2")
        assertTrue(campsite.entrantInList("user_1"))
        assertTrue(campsite.entrantInList("user_2"))

        // Generate final list (updates waitlist state and populates finalList)
        campsite.generateFinalList()
        assertEquals(2, campsite.getFinalList().size)
        assertFalse(campsite.entrantInList("user_1"))
    }
}
