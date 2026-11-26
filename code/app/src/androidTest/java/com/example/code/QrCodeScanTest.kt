package com.example.code

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.example.code.classes.AppState
import com.example.code.classes.Campsite
import com.example.code.classes.ChatMessage
import com.example.code.classes.Coordinates
import com.example.code.classes.Entrant
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** Test Entrant QR Code Scanning
 * The suite checks generated QR codes, the owner's QR encoder, and a stored image fixture.
 *
 * - decodeQrCode: generated codes are read back exactly, including resized, rotated, and
 *   "photo-like" versions (code sitting in a bigger picture); images with no code give null
 * - findCampsiteForQr: matches by campsite ID, then by name
 * - The QR buttons open the scan dialog (search screen + profile screen)
 * - Scanning a campsite's code and pressing View Campsite opens that campsite's details
 *   (LocalQrTestImage hands the dialog a generated image instead of opening the photo picker)
 * - Unknown codes / images with no code don't open anything
 * - Stored QR image fixtures in androidTest/assets/qr/ decode to their filenames
 *
 * Not tested: the system photo picker itself (it's another app, outside Compose).
 * Conversation (All 4 functions underneath: https://claude.ai/share/6448aa90-1673-4617-9624-eac3afb21ef4)
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@RunWith(AndroidJUnit4::class)
class QrCodeScanTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /* ------------------------------ QR image helpers ------------------------------ */

    /** A black-on-white QR code for [text], [size] x [size] pixels. */
    private fun qrBitmap(text: String, size: Int = 400): Bitmap {
        val matrix = QRCodeWriter().encode(
            text, BarcodeFormat.QR_CODE, size, size,
            mapOf(EncodeHintType.MARGIN to 4, EncodeHintType.CHARACTER_SET to "UTF-8")
        )
        val pixels = IntArray(matrix.width * matrix.height) { i ->
            if (matrix.get(i % matrix.width, i / matrix.width)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        }
        return Bitmap.createBitmap(pixels, matrix.width, matrix.height, Bitmap.Config.ARGB_8888)
    }

    /** [qr] placed off-centre in a bigger light-grey picture, like a photo of a printed code. */
    private fun inBiggerPicture(qr: Bitmap): Bitmap {
        val photo = Bitmap.createBitmap(qr.width * 3, qr.height * 2, Bitmap.Config.ARGB_8888)
        Canvas(photo).apply {
            drawColor(0xFFDDDDDD.toInt())
            drawBitmap(qr, qr.width * 1.6f, qr.height * 0.3f, null)
        }
        return photo
    }

    private fun rotated(bmp: Bitmap, degrees: Float): Bitmap =
        Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postRotate(degrees) }, true)

    private fun solid(color: Int) = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }

    // Lots of different QR contents: short IDs, Firestore-style IDs, names with spaces/accents, long text
    private val sampleTexts = listOf(
        "c_1", "c_2", "c_99",
        "Xq8vLr2NfT0aBcDeFgHi", "0000000000000000000a", "zZ9yY8xX7wW6vV5uU4tT",
        "Banff National Park", "Lac Sainte-Anne – Été 2026", "Jasper Lake",
        "https://example.com/campsite?id=c_42&ref=qr",
        "a".repeat(200)
    )

    /* -------------------------------- decodeQrCode -------------------------------- */

    @Test
    fun decodesEveryGeneratedCode() {
        val failures = sampleTexts.filter { decodeQrCode(qrBitmap(it)) != it }
        assertTrue("Couldn't read back: $failures", failures.isEmpty())
    }

    @Test
    fun decodesDifferentImageSizes() {
        // 400px is the default; also very small and very large images
        for (size in listOf(120, 250, 800, 1500)) {
            val failures = sampleTexts.filter { decodeQrCode(qrBitmap(it, size)) != it }
            assertTrue("Size $size failed for: $failures", failures.isEmpty())
        }
    }

    @Test
    fun decodesRotatedCodes() {
        for (degrees in listOf(90f, 180f, 270f)) {
            val failures = sampleTexts.filter { decodeQrCode(rotated(qrBitmap(it), degrees)) != it }
            assertTrue("Rotation $degrees failed for: $failures", failures.isEmpty())
        }
    }

    @Test
    fun decodesCodeInsideABiggerPicture() {
        val failures = sampleTexts.filter { decodeQrCode(inBiggerPicture(qrBitmap(it))) != it }
        assertTrue("Couldn't find the code in the picture for: $failures", failures.isEmpty())
    }

    @Test
    fun imagesWithoutACodeGiveNull() {
        assertNull(decodeQrCode(solid(0xFFFFFFFF.toInt())))   // blank white
        assertNull(decodeQrCode(solid(0xFF000000.toInt())))   // all black

        // Random noise (fixed seed so the test is repeatable)
        val random = java.util.Random(301)
        val noise = IntArray(300 * 300) { if (random.nextBoolean()) 0xFF000000.toInt() else 0xFFFFFFFF.toInt() }
        assertNull(decodeQrCode(Bitmap.createBitmap(noise, 300, 300, Bitmap.Config.ARGB_8888)))
    }

    /**
     * Decodes packaged image files and compares each result with the payload in its filename.
     */
    @Test
    fun decodesPackagedQrImages() {
        val assets = InstrumentationRegistry.getInstrumentation().context.assets
        val images = assets.list("qr").orEmpty()
            .filter { it.substringAfterLast('.').lowercase() in setOf("png", "jpg", "jpeg", "webp") }
        assertTrue("Expected a packaged QR image in androidTest/assets/qr/", images.isNotEmpty())

        val failures = images.mapNotNull { file ->
            val expected = file.substringBeforeLast('.')
            val bmp = assets.open("qr/$file").use { BitmapFactory.decodeStream(it) }
            val got = bmp?.let { decodeQrCode(it) }
            if (got == expected) null else "$file -> $got"
        }
        assertTrue("Wrong or unreadable: $failures", failures.isEmpty())
    }

    /* ------------------------------ findCampsiteForQr ------------------------------ */

    private fun site(id: String, name: String) = Campsite(
        name = name,
        ownerID = "owner_$id",
        campsiteID = id,
        maxOccupancy = mutableListOf(4),
        price = 50.0f,
        coordinates = mutableListOf(Coordinates("Canada", "Alberta", name, "")),
        lotteryStartDate = LocalDate.now().plusDays(1).atStartOfDay(),
        lotteryEndDate = LocalDate.now().plusDays(2).atTime(23, 59)
    )

    private val banff = site("c_1", "Banff National Park")
    private val jasper = site("c_2", "Jasper Lake")
    private val confusing = site("c_3", "c_1")   // a campsite *named* like another one's ID
    private val allSites = listOf(banff, jasper, confusing)

    private val entrant = Entrant(username = "jdoe", email = "jdoe@example.com", userID = "e_1", name = "John Doe")

    /** AppState with MockK databases (no Firestore), like the other entrant tests. */
    private fun fakeAppState(): AppState {
        val campsites = mockk<DatabaseCampsites>(relaxed = true)
        every { campsites.campsites } returns allSites
        every { campsites.searchCampsite(any(), any()) } answers {
            secondArg<(Campsite?) -> Unit>()(allSites.find { it.campsiteID == firstArg<String>() })
        }

        val entrants = mockk<DatabaseEntrants>(relaxed = true)
        every { campsites.addToWaitingList(any<Campsite>(), any(), any()) } answers {
            firstArg<Campsite>().addEntrant(secondArg())
            thirdArg<(Boolean) -> Unit>()(true)
        }
        every { entrants.entrants } returns listOf(entrant)
        every { entrants.addEntrantCampsite(any(), any(), any(), any()) } answers {
            firstArg<Entrant>().addCampsite(secondArg(), thirdArg())
            arg<(Boolean) -> Unit>(3)(true)
        }
        every { entrants.searchEntrants(any(), any()) } answers {
            secondArg<(Entrant?) -> Unit>()(if (firstArg<String>() == entrant.getUserID()) entrant else null)
        }

        val chat = mockk<DatabaseChat>(relaxed = true)
        every { chat.messages } returns mutableStateListOf<ChatMessage>()
        every { chat.conversation(any(), any()) } returns emptyList()
        every { chat.conversationsFor(any()) } returns emptyMap()

        return AppState(
            admins = mockk(relaxed = true),
            owners = mockk(relaxed = true),
            campsites = campsites,
            entrants = entrants,
            alerts = mockk(relaxed = true),
            chat = chat
        ).apply {
            userID = entrant.getUserID()
            role = "entrant"
        }
    }

    @Test
    fun findsCampsiteById() {
        assertEquals(jasper, findCampsiteForQr(fakeAppState(), "c_2"))
    }

    @Test
    fun findsCampsiteByName() {
        assertEquals(banff, findCampsiteForQr(fakeAppState(), "Banff National Park"))
    }

    @Test
    fun idWinsOverName() {
        // "c_1" is Banff's ID and also c_3's name: the ID match comes first
        assertEquals(banff, findCampsiteForQr(fakeAppState(), "c_1"))
    }

    @Test
    fun ignoresSurroundingWhitespace() {
        assertEquals(jasper, findCampsiteForQr(fakeAppState(), "  c_2\n"))
    }

    @Test
    fun unknownCodeGivesNull() {
        assertNull(findCampsiteForQr(fakeAppState(), "c_404"))
        assertNull(findCampsiteForQr(fakeAppState(), ""))
    }

    @Test
    fun generatedCodeLeadsToTheRightCampsite() {
        // Whole pipeline minus the photo picker: image -> text -> campsite
        val appState = fakeAppState()
        for (site in allSites) {
            val text = decodeQrCode(qrBitmap(site.campsiteID))
            assertEquals(site, findCampsiteForQr(appState, text!!))
        }
    }

    /* ------------------------------------- UI ------------------------------------- */

    private lateinit var nav: NavHostController
    private lateinit var appState: AppState

    // The image the dialog "picks" when the square is tapped (stands in for the photo picker)
    private var pickedImage: Bitmap? = null

    private fun openApp(start: String) {
        appState = fakeAppState()
        composeTestRule.setContent {
            nav = rememberNavController()
            CompositionLocalProvider(LocalQrTestImage provides { pickedImage }) {
                CampsiteNavController(appState, startDestination = start, navController = nav)
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun openScanDialog() {
        composeTestRule.onNodeWithText("QR").performClick()
        composeTestRule.waitForIdle()
    }

    /** "Picks" [image] in the dialog and waits for the scan (it runs off the main thread) to finish. */
    private fun scan(image: Bitmap) {
        pickedImage = image
        composeTestRule.onNodeWithText("Tap to choose a QR code image").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("Tap to choose a QR code image").fetchSemanticsNodes().isEmpty() &&
                composeTestRule.onAllNodesWithText("Scanning…").fetchSemanticsNodes().isEmpty()
        }
    }

    private fun viewCampsite() {
        composeTestRule.onNodeWithText("View Campsite").performClick()
        composeTestRule.waitForIdle()
    }

    /** The details screen is showing [site] (header name + description section), and no other campsite. */
    private fun assertDetailsShowing(site: Campsite) {
        composeTestRule.onNodeWithText("Description:").assertIsDisplayed()
        // The name can show up more than once (header + locations), so check the first one
        composeTestRule.onAllNodesWithText(site.getName())[0].assertIsDisplayed()
        allSites.filter { it != site }.forEach {
            composeTestRule.onNodeWithText(it.getName()).assertDoesNotExist()
        }
        assertEquals(site.campsiteID, appState.viewingCampsiteID)
        assertEquals("campsiteDetails", nav.currentDestination?.route)
    }

    private fun assertScanDialogOpen() {
        composeTestRule.onNodeWithText("Scan QR Code").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tap to choose a QR code image").assertIsDisplayed()
        // Nothing scanned yet, so there's no campsite to view
        composeTestRule.onNodeWithText("View Campsite").assertIsNotEnabled()
    }

    // --- Opening the dialog ---

    @Test
    fun searchScreenQrButtonOpensScanDialog() {
        openApp("mainSearchScreen")
        openScanDialog()
        assertScanDialogOpen()
    }

    @Test
    fun profileQrButtonOpensScanDialog() {
        openApp("profileScreen")
        openScanDialog()
        assertScanDialogOpen()
    }

    @Test
    fun cancelClosesScanDialog() {
        openApp("mainSearchScreen")
        openScanDialog()
        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.onNodeWithText("Scan QR Code").assertDoesNotExist()
    }

    // --- Scanning -> campsite details ---

    @Test
    fun scanFromSearchScreenOpensDetails() {
        openApp("mainSearchScreen")
        openScanDialog()
        scan(qrBitmap(jasper.campsiteID))
        composeTestRule.onNodeWithText("Found: Jasper Lake").assertIsDisplayed()
        viewCampsite()
        assertDetailsShowing(jasper)
    }

    /** Codex prompt: Scan a campsite QR code, enter the lottery, and check both lists. */
    @Test
    fun scanThenJoinUpdatesBothWaitingLists() {
        openApp("mainSearchScreen")
        openScanDialog()
        scan(createCampsiteQrBitmap(jasper.campsiteID))
        viewCampsite()
        composeTestRule.onNodeWithText("Join Waiting List").performClick()
        composeTestRule.runOnIdle {
            assertEquals(listOf(entrant.getUserID()), jasper.getWaitingList())
            assertEquals(listOf(jasper.campsiteID), entrant.getCampsiteLists()[WAITING_LIST])
            assertTrue(banff.getWaitingList().isEmpty())
        }
    }

    @Test
    fun scanFromProfileOpensDetails() {
        openApp("profileScreen")
        openScanDialog()
        scan(qrBitmap(banff.campsiteID))
        viewCampsite()
        assertDetailsShowing(banff)
    }

    @Test
    fun scanCampsiteNameOpensDetails() {
        // Codes that hold the campsite's name instead of its ID still work
        openApp("mainSearchScreen")
        openScanDialog()
        scan(qrBitmap("Jasper Lake"))
        viewCampsite()
        assertDetailsShowing(jasper)
    }

    @Test
    fun scanPhotoLikeImageOpensDetails() {
        // Code sitting off-centre in a bigger, rotated picture
        openApp("mainSearchScreen")
        openScanDialog()
        scan(rotated(inBiggerPicture(qrBitmap(banff.campsiteID)), 90f))
        viewCampsite()
        assertDetailsShowing(banff)
    }

    @Test
    fun choosingADifferentImageOpensTheNewCampsite() {
        // Scan Banff, change your mind, scan Jasper: Jasper's details open
        openApp("mainSearchScreen")
        openScanDialog()
        scan(qrBitmap(banff.campsiteID))
        composeTestRule.onNodeWithText("Found: Banff National Park").assertIsDisplayed()

        pickedImage = qrBitmap(jasper.campsiteID)
        composeTestRule.onNodeWithText("Choose a different image").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("Found: Jasper Lake").fetchSemanticsNodes().isNotEmpty()
        }
        viewCampsite()
        assertDetailsShowing(jasper)
    }

    @Test
    fun closingDetailsGoesBackToSearch() {
        openApp("mainSearchScreen")
        openScanDialog()
        scan(qrBitmap(jasper.campsiteID))
        viewCampsite()
        assertDetailsShowing(jasper)

        composeTestRule.onNodeWithText("✕").performClick()
        composeTestRule.waitForIdle()
        assertEquals("mainSearchScreen", nav.currentDestination?.route)
        composeTestRule.onNodeWithText("Search for a campsite").assertIsDisplayed()
        // The dialog doesn't come back on its own
        composeTestRule.onNodeWithText("Scan QR Code").assertDoesNotExist()
    }

    // --- Scans that shouldn't open anything ---

    @Test
    fun unknownCampsiteCodeDoesNotOpenDetails() {
        openApp("mainSearchScreen")
        openScanDialog()
        scan(qrBitmap("c_404"))
        composeTestRule.onNodeWithText("That QR code isn't for a campsite in this app.").assertIsDisplayed()
        composeTestRule.onNodeWithText("View Campsite").assertIsNotEnabled()
        assertEquals("mainSearchScreen", nav.currentDestination?.route)
    }

    @Test
    fun imageWithoutCodeDoesNotOpenDetails() {
        openApp("mainSearchScreen")
        openScanDialog()
        scan(solid(0xFFFFFFFF.toInt()))
        composeTestRule.onNodeWithText("No QR code found in that image.").assertIsDisplayed()
        composeTestRule.onNodeWithText("View Campsite").assertIsNotEnabled()
        assertEquals("mainSearchScreen", nav.currentDestination?.route)
    }
}
