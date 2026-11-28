package com.example.code

import android.graphics.Bitmap
import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CampsitePhotoTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun largeGalleryPhotoFitsFirestoreAndDecodesForDisplay() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val width = 2400
        val height = 1600
        val random = Random(0)
        val pixels = IntArray(width * height) { random.nextInt() or 0xFF000000.toInt() }
        val original = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
        val file = File.createTempFile("campsite-photo", ".jpg", context.cacheDir)
        try {
            file.outputStream().use { original.compress(Bitmap.CompressFormat.JPEG, 100, it) }
            assertTrue(file.length() > 1024 * 1024)

            val encoded = requireNotNull(readCampsitePhoto(context.contentResolver, Uri.fromFile(file)))
            assertTrue(Base64.decode(encoded, Base64.DEFAULT).size <= 256 * 1024)
            val displayed = requireNotNull(decodeBase64ToImageBitmap(encoded))
            assertEquals(1280, displayed.width)
            assertTrue(displayed.height in 852..854)
        } finally {
            original.recycle()
            file.delete()
        }
    }

    /** Codex prompt: Check that a saved campsite photo replaces the default and an invalid photo restores it. */
    @Test
    fun savedPhotoRendersAndInvalidPhotoRestoresDefault() {
        val photo = mutableStateOf<String?>(null)
        val original = Bitmap.createBitmap(80, 60, Bitmap.Config.ARGB_8888)
        original.eraseColor(android.graphics.Color.MAGENTA)
        val encoded = try {
            requireNotNull(encodeCampsitePhoto(original))
        } finally {
            original.recycle()
        }

        compose.setContent {
            CampsiteImageBackground(
                image = photo.value,
                defaultImage = R.drawable.campsite_details,
                fallback = Color.Black,
                modifier = Modifier.size(64.dp).testTag("campsite-photo"),
                gradient = Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
            ) {}
        }

        fun awaitCenterPixel(matches: (Color) -> Boolean): Color {
            var matchedPixel: Color? = null
            compose.waitUntil(10_000) {
                val image = try {
                    compose.onNodeWithTag("campsite-photo").captureToImage()
                } catch (error: Throwable) {
                    // PixelCopy occasionally fails to capture an otherwise visible emulator frame.
                    if (error is ComposeTimeoutException ||
                        error.message?.contains("PixelCopy") == true) return@waitUntil false
                    throw error
                }
                val pixel = image.toPixelMap()[image.width / 2, image.height / 2]
                if (!matches(pixel)) return@waitUntil false
                matchedPixel = pixel
                true
            }
            return checkNotNull(matchedPixel)
        }

        val default = awaitCenterPixel { true }
        compose.runOnIdle { photo.value = encoded }
        awaitCenterPixel { pixel ->
            pixel.red > 0.7f && pixel.blue > 0.7f && pixel.green < 0.3f
        }

        compose.runOnIdle { photo.value = "invalid photo" }
        awaitCenterPixel { it == default }
    }

    @Test
    fun cameraPreviewUsesTheSameImageFormat() {
        val original = Bitmap.createBitmap(80, 60, Bitmap.Config.ARGB_8888)
        try {
            val encoded = requireNotNull(encodeCampsitePhoto(original))
            assertNotNull(decodeBase64ToImageBitmap(encoded))
        } finally {
            original.recycle()
        }
    }
}
