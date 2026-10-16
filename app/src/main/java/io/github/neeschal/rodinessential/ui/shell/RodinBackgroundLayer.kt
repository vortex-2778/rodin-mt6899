package io.github.neeschal.rodinessential.ui.shell

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import io.github.neeschal.rodinessential.ui.theme.RodinAppearanceConfig
import io.github.neeschal.rodinessential.ui.theme.RodinBackgroundStyle
import io.github.neeschal.rodinessential.ui.theme.RodinLightPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The app-wide visual plane: a fixed dark canvas, the light system surface,
 * or one of the decorative treatments. Purely presentational: it never reads
 * backend state and never changes configuration.
 */
@Composable
fun RodinBackgroundLayer(config: RodinAppearanceConfig, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.surface.luminance() < 0.5f
    val blurAmount = config.backgroundBlur

    Box(modifier.fillMaxSize()) {
        val visual: @Composable () -> Unit = {
            if (dark) {
                Box(Modifier.fillMaxSize().background(Color(0xFF000000)))
            } else {
                when (config.backgroundStyle) {
                    RodinBackgroundStyle.system ->
                        Box(Modifier.fillMaxSize().background(RodinLightPalette.background))
                    RodinBackgroundStyle.midnight -> MidnightBackground(dark = false)
                    RodinBackgroundStyle.aurora -> AuroraBackground(dark = false)
                    RodinBackgroundStyle.custom -> CustomBackground(config.customPath)
                }
            }
        }
        if (!dark && blurAmount > 0.05) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clipToBounds()
                    .graphicsLayer {
                        scaleX = 1.06f
                        scaleY = 1.06f
                    }
                    .blur(blurAmount.toFloat().dp),
            ) {
                visual()
            }
        } else {
            visual()
        }

        val decorated = !dark && config.backgroundStyle != RodinBackgroundStyle.system
        if (decorated) {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to colors.surface.copy(alpha = 0.20f),
                            0.52f to Color.White.copy(alpha = 0.035f),
                            1f to colors.surface.copy(alpha = 0.24f),
                        ),
                    ),
                ),
            )
        }
    }
}

/** One radial wash, positioned with Flutter alignment and radius semantics. */
@Composable
private fun RadialFill(
    alignmentX: Float,
    alignmentY: Float,
    radiusFraction: Float,
    colorStops: Array<Pair<Float, Color>>,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        if (width <= 0f || height <= 0f) return@BoxWithConstraints
        val center = Offset(
            (alignmentX + 1f) / 2f * width,
            (alignmentY + 1f) / 2f * height,
        )
        val radius = radiusFraction * minOf(width, height)
        Box(
            Modifier.fillMaxSize().background(
                Brush.radialGradient(colorStops = colorStops, center = center, radius = radius),
            ),
        )
    }
}

@Composable
private fun MidnightBackground(dark: Boolean) {
    Box(
        Modifier
            .fillMaxSize()
            .background(if (dark) Color(0xFF05080D) else Color(0xFFF3F6FA)),
    ) {
        if (dark) {
            RadialFill(
                alignmentX = -0.82f,
                alignmentY = -0.88f,
                radiusFraction = 1.08f,
                colorStops = arrayOf(
                    0f to Color(0x2E3B82C6),
                    0.47f to Color(0x141B416A),
                    1f to Color(0x0005080D),
                ),
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color(0x00000000),
                            0.58f to Color(0x08000000),
                            1f to Color(0x26000000),
                        ),
                    ),
                ),
            )
        } else {
            RadialFill(
                alignmentX = -0.78f,
                alignmentY = -0.90f,
                radiusFraction = 1.14f,
                colorStops = arrayOf(
                    0f to Color(0x244F8FC7),
                    0.48f to Color(0x10346D9F),
                    1f to Color(0x00F3F6FA),
                ),
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color(0x00FFFFFF),
                            0.5f to Color(0x10FFFFFF),
                            1f to Color(0x2AEEF3F8),
                        ),
                    ),
                ),
            )
        }
    }
}

@Composable
private fun AuroraBackground(dark: Boolean) {
    Box(
        Modifier
            .fillMaxSize()
            .background(if (dark) Color(0xFF060910) else Color(0xFFF7F9FC)),
    ) {
        if (dark) {
            RadialFill(
                alignmentX = -0.92f,
                alignmentY = -0.82f,
                radiusFraction = 1.20f,
                colorStops = arrayOf(
                    0f to Color(0x383E87F5),
                    0.44f to Color(0x162D5DA7),
                    1f to Color(0x00060910),
                ),
            )
            RadialFill(
                alignmentX = 1.02f,
                alignmentY = 0.72f,
                radiusFraction = 1.12f,
                colorStops = arrayOf(
                    0f to Color(0x2A35BE9F),
                    0.43f to Color(0x12368783),
                    1f to Color(0x00060910),
                ),
            )
            RadialFill(
                alignmentX = 0.68f,
                alignmentY = -0.70f,
                radiusFraction = 0.92f,
                colorStops = arrayOf(
                    0f to Color(0x1E8D65D8),
                    1f to Color(0x00060910),
                ),
            )
        } else {
            RadialFill(
                alignmentX = -0.92f,
                alignmentY = -0.82f,
                radiusFraction = 1.18f,
                colorStops = arrayOf(
                    0f to Color(0x2A579CF0),
                    0.46f to Color(0x105F8EC4),
                    1f to Color(0x00F7F9FC),
                ),
            )
            RadialFill(
                alignmentX = 1.0f,
                alignmentY = 0.72f,
                radiusFraction = 1.08f,
                colorStops = arrayOf(
                    0f to Color(0x2439BDA2),
                    0.44f to Color(0x0D65A995),
                    1f to Color(0x00F7F9FC),
                ),
            )
            RadialFill(
                alignmentX = 0.65f,
                alignmentY = -0.72f,
                radiusFraction = 0.88f,
                colorStops = arrayOf(
                    0f to Color(0x188B67D3),
                    1f to Color(0x00F7F9FC),
                ),
            )
        }
    }
}

private sealed interface CustomBackgroundState {
    data object Decoding : CustomBackgroundState
    data class Ready(val bitmap: Bitmap) : CustomBackgroundState
    data object Failed : CustomBackgroundState
}

@Composable
private fun CustomBackground(path: String) {
    if (path.isEmpty()) {
        AuroraBackground(dark = false)
        return
    }
    val state by produceState<CustomBackgroundState>(CustomBackgroundState.Decoding, path) {
        val bitmap = withContext(Dispatchers.IO) {
            try {
                BitmapFactory.decodeFile(path)
            } catch (_: Exception) {
                null
            }
        }
        value = if (bitmap != null) CustomBackgroundState.Ready(bitmap) else CustomBackgroundState.Failed
    }
    when (val current = state) {
        is CustomBackgroundState.Ready -> Image(
            bitmap = current.bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            filterQuality = FilterQuality.Medium,
        )
        CustomBackgroundState.Failed -> AuroraBackground(dark = false)
        CustomBackgroundState.Decoding -> Box(Modifier.fillMaxSize())
    }
}
