package io.github.neeschal.rodinessential.ui.screens

import android.os.SystemClock
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Gradient
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.SpaceBar
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.ActionChip
import androidx.compose.material3.ChoiceChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import io.github.neeschal.rodinessential.backend.RodinBackend
import io.github.neeschal.rodinessential.backend.RodinConnectionState
import io.github.neeschal.rodinessential.backend.RodinSystemColorsSelection
import io.github.neeschal.rodinessential.backend.RodinSystemColorsState
import io.github.neeschal.rodinessential.ui.components.PressScale
import io.github.neeschal.rodinessential.ui.components.RodinHaptics
import io.github.neeschal.rodinessential.ui.components.SectionLabel
import io.github.neeschal.rodinessential.ui.components.SurfaceCard
import io.github.neeschal.rodinessential.ui.motion.RodinDialog
import io.github.neeschal.rodinessential.ui.shell.RodinInteractionSettings
import io.github.neeschal.rodinessential.ui.theme.LocalRodinAppearance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/** HSL draft state; conversions go through Android's own color utilities. */
private data class RodinHsl(
    val hue: Float,
    val saturation: Float,
    val lightness: Float,
) {
    fun toColorInt(): Int {
        val hsl = floatArrayOf(hue, saturation, lightness)
        return ColorUtils.HSLToColor(hsl) and 0xFFFFFF
    }

    fun toColor(): Color = previewColor(toColorInt())

    fun withHue(value: Float) = copy(hue = value.coerceIn(0f, 360f))

    fun withSaturation(value: Float) = copy(saturation = value.coerceIn(0f, 1f))

    fun withLightness(value: Float) = copy(lightness = value.coerceIn(0f, 1f))

    companion object {
        fun fromRgb(rgb: Int): RodinHsl {
            val out = floatArrayOf(0f, 0f, 0f)
            ColorUtils.colorToHSL(0xFF000000.toInt() or (rgb and 0xFFFFFF), out)
            return RodinHsl(out[0], out[1], out[2])
        }
    }
}

private fun previewColor(rgb: Int): Color = Color(0xFF000000.toInt() or (rgb and 0xFFFFFF))

private fun paletteHex(rgb: Int): String = String.format("#%06X", rgb and 0xFFFFFF)

private val paletteStyleNames = listOf(
    "Tonal",
    "Vibrant",
    "Expressive",
    "Soft",
    "Rainbow",
    "Fruit Salad",
    "Monochrome",
)

private val paletteSeeds = listOf(
    "Teal" to 0x008577,
    "Ocean" to 0x386BD5,
    "Iris" to 0x7655CA,
    "Rose" to 0xAD477B,
    "Clay" to 0xB95733,
    "Gold" to 0x997A16,
    "Forest" to 0x4E7541,
    "Slate" to 0x526774,
)

private fun rodinPreviewScheme(colors: List<Int>, dark: Boolean): ColorScheme {
    if (colors.size < 12) throw IllegalArgumentException("Invalid preview")
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        surfaceContainerLow = previewColor(colors[0]),
        outlineVariant = previewColor(colors[1]),
        onSurface = previewColor(colors[2]),
        tertiaryContainer = previewColor(colors[3]),
        onTertiaryContainer = previewColor(colors[4]),
        primaryContainer = previewColor(colors[5]),
        onPrimaryContainer = previewColor(colors[6]),
        primary = previewColor(colors[7]),
        onPrimary = previewColor(colors[8]),
        surfaceContainerHighest = previewColor(colors[9]),
        secondaryContainer = previewColor(colors[10]),
        onSecondaryContainer = previewColor(colors[11]),
    )
}

/**
 * System Colors: Android's own Monet palette. Native readback stays separate
 * from the selected preview so nothing here claims more than Android confirms.
 */
@Composable
fun SystemColorsScreen(onBack: () -> Unit) {
    val snapshot = rememberRodinSnapshot()
    val palette = RodinBackend.systemColors
    val connection = snapshot.connection
    val selection = RodinBackend.systemColorsSelection
    val lifecycleOwner = LocalLifecycleOwner.current

    var lastConnection by remember { mutableStateOf(connection) }
    LaunchedEffect(connection) {
        val connected = lastConnection != RodinConnectionState.ONLINE &&
            connection == RodinConnectionState.ONLINE
        lastConnection = connection
        if (connected) {
            if (!RodinBackend.systemColors.busy) RodinBackend.refreshSystemColors()
            if (!RodinBackend.systemColors.contrastBusy) {
                RodinBackend.refreshSystemColorContrast()
            }
        }
    }

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            if (!RodinBackend.systemColors.busy) {
                RodinBackend.refreshSystemColors()
                RodinBackend.refreshSystemColorContrast()
            }
        }
    }

    RodinScrollPage(
        header = RodinPageHeader.Detail(title = "System Colors", onBack = onBack),
    ) {
        Spacer(Modifier.height(12.dp))
        SystemColorsPanel(
            palette = palette,
            connection = connection,
            selection = selection,
            onApply = { wallpaper, seed, style ->
                if (wallpaper) {
                    RodinBackend.useWallpaperSystemColors()
                } else {
                    RodinBackend.setSystemColors(seed, style)
                }
            },
            onRefresh = { RodinBackend.refreshSystemColors() },
            onRefreshContrast = { RodinBackend.refreshSystemColorContrast() },
            onSetContrast = { level -> RodinBackend.setSystemColorContrast(level) },
            isNativeBusy = { RodinBackend.systemColors.busy },
        )
    }
}
