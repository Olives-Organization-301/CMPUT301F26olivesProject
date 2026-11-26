package com.example.code

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.example.code.classes.Campsite
import java.io.ByteArrayOutputStream
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import java.io.IOException
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal fun Campsite?.thumbnailPhoto(): String? =
    this?.getThumbnailImage()?.takeIf { it.isNotBlank() } ?: this?.getDetailsImage()

internal fun Campsite?.detailsPhoto(): String? =
    this?.getDetailsImage()?.takeIf { it.isNotBlank() } ?: this?.getThumbnailImage()

private const val MaxPhotoDimension = 1280
// Leave room in the campsite document for both image fields and campsite data.
private const val MaxPhotoBytes = 256 * 1024

internal fun readCampsitePhoto(resolver: ContentResolver, uri: Uri): String? {
    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val longestSide = maxOf(info.size.width, info.size.height)
            if (longestSide > MaxPhotoDimension) {
                decoder.setTargetSize(
                    (info.size.width.toLong() * MaxPhotoDimension / longestSide).toInt().coerceAtLeast(1),
                    (info.size.height.toLong() * MaxPhotoDimension / longestSide).toInt().coerceAtLeast(1)
                )
            }
        }
    } else {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        val longestSide = maxOf(options.outWidth, options.outHeight)
        if (longestSide <= 0) return null
        options.inJustDecodeBounds = false
        options.inSampleSize = Integer.highestOneBit((longestSide / MaxPhotoDimension).coerceAtLeast(1))
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    } ?: return null
    return try {
        encodeCampsitePhoto(bitmap)
    } finally {
        bitmap.recycle()
    }
}

internal fun encodeCampsitePhoto(bitmap: Bitmap): String? {
    val longestSide = maxOf(bitmap.width, bitmap.height)
    val scaled = if (longestSide > MaxPhotoDimension) {
        Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width.toLong() * MaxPhotoDimension / longestSide).toInt().coerceAtLeast(1),
            (bitmap.height.toLong() * MaxPhotoDimension / longestSide).toInt().coerceAtLeast(1),
            true
        )
    } else {
        bitmap
    }
    val output = ByteArrayOutputStream()
    for (quality in 85 downTo 25 step 15) {
        output.reset()
        if (scaled.compress(Bitmap.CompressFormat.JPEG, quality, output) &&
            output.size() <= MaxPhotoBytes
        ) {
            return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
        }
    }
    return null
}

@Composable
internal fun rememberPhotoPicker(onPhoto: (String) -> Unit): PhotoPicker {
    val context = LocalContext.current
    var processingPhoto by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    fun processPhoto(read: () -> String?) {
        processingPhoto = true
        scope.launch {
            val image = withContext(Dispatchers.IO) {
                try {
                    read()
                } catch (e: IOException) {
                    null
                } catch (e: RuntimeException) {
                    null
                }
            }
            processingPhoto = false
            if (image != null) {
                onPhoto(image)
            } else {
                Toast.makeText(context, "Could not read this photo", Toast.LENGTH_LONG).show()
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) processPhoto { encodeCampsitePhoto(bitmap) }
    }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) processPhoto { readCampsitePhoto(context.contentResolver, uri) }
    }
    return PhotoPicker(
        processing = processingPhoto,
        takePhoto = { cameraLauncher.launch(null) },
        choosePhoto = {
            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    )
}

internal data class PhotoPicker(
    val processing: Boolean,
    val takePhoto: () -> Unit,
    val choosePhoto: () -> Unit
)

// Slightly desaturated (1 = original colours, 0 = greyscale)
// Desaturate value
private val Desaturate = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.65f) })

// Black gradient, darker at the bottom so white text stays readable
// Value applying black gradient
val CampsiteImageGradient = Brush.verticalGradient(
    listOf(Color.Black.copy(alpha = 0.35f), Color.Black.copy(alpha = 0.85f))
)

/** Draws the saved photo or the bundled default behind [content]. */
@Composable
fun CampsiteImageBackground(
    image: String?,
    @DrawableRes defaultImage: Int,
    fallback: Color,
    modifier: Modifier = Modifier,
    gradient: Brush = CampsiteImageGradient,
    content: @Composable BoxScope.() -> Unit
) {
    val context = LocalContext.current
    val bitmap = key(image, context) {
        produceState<ImageBitmap?>(initialValue = null) {
            if (!image.isNullOrBlank()) {
                value = withContext(Dispatchers.IO) {
                    if (image.startsWith("content://") || image.startsWith("file://")) {
                        loadImage(context, Uri.parse(image))
                    } else {
                        decodeBase64ToImageBitmap(image)
                    }
                }
            }
        }.value
    }
    Box(modifier = modifier.background(fallback)) {
        Image(
            painter = bitmap?.let { BitmapPainter(it) }
                ?: painterResource(defaultImage),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
            colorFilter = Desaturate
        )
        Box(Modifier.matchParentSize().background(gradient))
        content()
    }
}
