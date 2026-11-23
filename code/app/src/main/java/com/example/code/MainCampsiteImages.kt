package com.example.code

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.code.classes.Campsite

/** Campsite images
 * The images are hardcoded for now: every campsite uses the same thumbnail and details photo.
 * TODO: Once Campsite.thumbnailImage / detailsImage hold real image URLs (e.g. Firebase Storage
 *  download URLs), load them with Coil's AsyncImage instead of painterResource.
 * Return null to fall back to the plain background.
 * Prompt: https://claude.ai/share/e282e67a-f3db-4225-b23d-1cadc614a63f
 * Image Locations: code\app\src\main\res\drawable
 */
@DrawableRes
fun campsiteThumbnailRes(campsite: Campsite?): Int? = R.drawable.campsite_thumbnail

@DrawableRes
fun campsiteDetailsRes(campsite: Campsite?): Int? = R.drawable.campsite_details

// Slightly desaturated (1 = original colours, 0 = greyscale)
// Desaturate value
private val Desaturate = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.65f) })

// Black gradient, darker at the bottom so white text stays readable
// Value applying black gradient
val CampsiteImageGradient = Brush.verticalGradient(
    listOf(Color.Black.copy(alpha = 0.35f), Color.Black.copy(alpha = 0.85f))
)

/** Draws a campsite photo (desaturated, with a black gradient over it) behind [content].
 *  If [imageRes] is null, only the [fallback] colour is drawn, like before.
 */
@Composable
fun CampsiteImageBackground(
    @DrawableRes imageRes: Int?,
    fallback: Color,
    modifier: Modifier = Modifier,
    gradient: Brush = CampsiteImageGradient,
    content: @Composable BoxScope.() -> Unit
) {
    // Box where the magic happens
    Box(modifier = modifier.background(fallback)) {
        if (imageRes != null) {
            // Insert Image if it exists, matching parent
            Image(
                painter = painterResource(imageRes),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
                colorFilter = Desaturate // Apply Filter
            )
            // Box to further colour the image on top
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(gradient) // Apply Gradient
                // If you wanna make this darker, probably modify this modifier
            )
        }
        content()
    }
}
