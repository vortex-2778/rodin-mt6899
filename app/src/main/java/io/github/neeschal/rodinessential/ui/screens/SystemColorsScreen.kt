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

/**
 * Choices apply on tap; consecutive slider gestures form one adjustment.
 * Native readback stays separate from the selected preview. Rapid choices
 * coalesce to the latest selection while one transaction is in progress.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SystemColorsPanel(
    palette: RodinSystemColorsState,
    connection: RodinConnectionState,
    onApply: (Boolean, Int, Int) -> Boolean,
    onRefresh: () -> Boolean,
    onRefreshContrast: (() -> Boolean)? = null,
    onSetContrast: ((Int) -> Boolean)? = null,
    onSelectionFeedback: () -> Unit = { RodinHaptics.segment() },
    isNativeBusy: (() -> Boolean)? = null,
    selection: RodinSystemColorsSelection? = null,
    enableLibrary: Boolean = true,
) {
    val backendOnline = connection == RodinConnectionState.ONLINE
    val latestPalette by rememberUpdatedState(palette)
    val latestOnline by rememberUpdatedState(backendOnline)
    val latestSelection by rememberUpdatedState(selection)
    val latestIsNativeBusy by rememberUpdatedState(isNativeBusy)
    val scope = rememberCoroutineScope()
    val library = RodinBackend.systemColorsLibrary

    var wallpaper by remember { mutableStateOf(true) }
    var seed by remember { mutableIntStateOf(0x008577) }
    var style by remember { mutableIntStateOf(0) }
    var fineTuneBase by remember { mutableIntStateOf(0x008577) }
    var hsl by remember { mutableStateOf(RodinHsl.fromRgb(0x008577)) }
    var edited by remember { mutableStateOf(false) }
    var previewDark by remember { mutableStateOf<Boolean?>(null) }
    var pendingRevision by remember { mutableStateOf<Int?>(null) }
    var applyQueued by remember { mutableStateOf(false) }
    var dispatchScheduled by remember { mutableStateOf(false) }
    var sliding by remember { mutableStateOf(false) }
    var needsQuiet by remember { mutableStateOf(false) }
    var quietJob by remember { mutableStateOf<Job?>(null) }
    val activePointers = remember { mutableSetOf<PointerId>() }
    var notice by remember { mutableStateOf<String?>(null) }
    var hapticStep by remember { mutableIntStateOf(-1) }
    val hapticClock = remember { SystemClock.elapsedRealtime() }
    var lastHapticMs by remember { mutableIntStateOf(-100) }
    var libraryLoaded by remember { mutableStateOf(false) }
    var libraryTick by remember { mutableIntStateOf(0) }
    var hexDialogVisible by remember { mutableStateOf(false) }
    var saveDialogVisible by remember { mutableStateOf(false) }
    var previousRevision by remember { mutableIntStateOf(-1) }
    var previousOpState by remember { mutableIntStateOf(-1) }
    var previousSelection by remember {
        mutableStateOf<RodinSystemColorsSelection?>(null)
    }

    fun isBusy(): Boolean = latestPalette.busy || pendingRevision != null

    fun matchesCurrent(): Boolean {
        val current = latestPalette
        if (!current.hasReadback) return false
        return if (wallpaper) {
            current.mode == 0 && current.style == 0
        } else {
            current.mode == 1 && current.seed == seed && current.style == style
        }
    }

    fun syncDraft() {
        val chosen = RodinSystemColorsSelection.fromNative(latestPalette) ?: latestSelection
        if (chosen == null) return
        // Preserve the unrounded HSL draft and reset point when Android
        // confirms our own edit; converting through RGB every frame would
        // lose hue at gray, black and white.
        if (chosen.seed != seed || chosen.wallpaper != wallpaper) {
            hsl = RodinHsl.fromRgb(chosen.seed)
            fineTuneBase = chosen.seed
        }
        wallpaper = chosen.wallpaper
        seed = chosen.seed
        style = chosen.style
        edited = false
    }

    fun edit(update: () -> Unit) {
        update()
        edited = true
        notice = null
    }

    fun select(update: () -> Unit) {
        quietJob?.cancel()
        quietJob = null
        needsQuiet = false
        // Feedback belongs to the gesture, never to a delayed daemon reply.
        onSelectionFeedback()
        edit(update)
        scheduleApply()
    }

    fun scheduleApply(afterDrag: Boolean = false) {
        val current = latestPalette
        if (!latestOnline ||
            current.failed ||
            (current.hasReadback && !current.supported)
        ) {
            applyQueued = false
            return
        }
        applyQueued = true
        if (afterDrag) needsQuiet = true
        dispatchWhenReady()
    }

    fun dispatchWhenReady() {
        if (!applyQueued || sliding || activePointers.isNotEmpty()) return
        if (needsQuiet) {
            // An Android overlay update can cover the screen with a system
            // snapshot; don't start one between two fine-tune gestures.
            if (quietJob == null) {
                quietJob = scope.launch {
                    delay(280)
                    quietJob = null
                    needsQuiet = false
                    dispatchWhenReady()
                }
            }
            return
        }
        if (dispatchScheduled) return
        dispatchScheduled = true
        // Submission only queues native work; it never waits for Android.
        scope.launch {
            dispatchScheduled = false
            if (applyQueued && !sliding && !needsQuiet && activePointers.isEmpty()) {
                applySelection()
            }
        }
    }

    fun applySelection() {
        val current = latestPalette
        if (!latestOnline ||
            current.failed ||
            (current.hasReadback && !current.supported)
        ) {
            applyQueued = false
            return
        }
        // Preserve taps during discovery and native refreshes that the next
        // UI snapshot has not reported yet; dispatch once the read completes.
        if (!current.hasReadback ||
            isBusy() ||
            (latestIsNativeBusy?.invoke() ?: false)
        ) {
            applyQueued = true
            return
        }
        applyQueued = false
        if (matchesCurrent()) {
            edited = false
            return
        }
        val revision = current.revision
        if (onApply(wallpaper, seed, style)) {
            pendingRevision = revision
            notice = null
        } else if (latestIsNativeBusy?.invoke() ?: false) {
            // The native refresh can start between preflight and submit.
            applyQueued = true
        } else {
            notice = "Could not apply this selection. Refresh and select it again."
        }
    }

    fun changeHsl(color: RodinHsl, hapticStepValue: Int) {
        if (hapticStep != hapticStepValue) {
            hapticStep = hapticStepValue
            val elapsed = SystemClock.elapsedRealtime() - hapticClock
            if (elapsed - lastHapticMs >= 45) {
                lastHapticMs = elapsed
                RodinHaptics.frequentSegment()
            }
        }
        edit {
            hsl = color
            seed = color.toColorInt() and 0xFFFFFF
        }
    }

    fun chooseSeed(rgb: Int) {
        select {
            seed = rgb
            fineTuneBase = rgb
            hsl = RodinHsl.fromRgb(rgb)
        }
    }

    fun choosePalette(seedValue: Int, styleValue: Int) {
        select {
            wallpaper = false
            seed = seedValue
            style = styleValue
            fineTuneBase = seedValue
            hsl = RodinHsl.fromRgb(seedValue)
        }
    }

    fun recordConfirmed(seedValue: Int, styleValue: Int) {
        scope.launch {
            withContext(Dispatchers.IO) { library.record(seedValue, styleValue) }
            libraryTick += 1
            libraryLoaded = true
        }
    }

    fun savePalette(name: String) {
        scope.launch {
            withContext(Dispatchers.IO) { library.save(name, seed, style) }
            libraryTick += 1
            libraryLoaded = true
        }
    }

    fun removePalette(item: io.github.neeschal.rodinessential.backend.RodinSavedPalette) {
        scope.launch {
            withContext(Dispatchers.IO) { library.remove(item) }
            libraryTick += 1
        }
    }

    fun beginSlider() {
        quietJob?.cancel()
        quietJob = null
        needsQuiet = true
        hapticStep = -1
        sliding = true
    }

    fun endSlider() {
        sliding = false
        scheduleApply(afterDrag = true)
    }

    LaunchedEffect(Unit) {
        syncDraft()
        if (enableLibrary) {
            withContext(Dispatchers.IO) { library.load() }
            libraryLoaded = true
        }
    }

    LaunchedEffect(
        palette.busy,
        palette.revision,
        palette.operationState,
        selection,
        backendOnline,
    ) {
        val current = palette
        if (!backendOnline) applyQueued = false
        if (!edited && !current.hasReadback && selection != previousSelection) {
            syncDraft()
        }
        val unchanged = current.revision == previousRevision &&
            current.operationState == previousOpState
        if (!(current.busy || unchanged)) {
            if (pendingRevision != null && current.revision != pendingRevision) {
                pendingRevision = null
                if (current.ready) {
                    if (!applyQueued && !sliding) syncDraft()
                    notice = when (current.outcome) {
                        1 -> if (current.mode == 0) {
                            "Wallpaper colors restored. Android’s palette changed."
                        } else {
                            "System colors updated. Verified against Android’s palette."
                        }
                        2 -> "Wallpaper following restored. Android resolved the same colors."
                        else -> "This palette is already selected in Android."
                    }
                    if (enableLibrary && current.mode == 1 && current.seed >= 0) {
                        recordConfirmed(current.seed, current.style)
                    }
                } else {
                    applyQueued = false
                    notice = null
                }
            } else if (!edited) {
                syncDraft()
            }
            if (applyQueued && !isBusy()) dispatchWhenReady()
        }
        previousRevision = current.revision
        previousOpState = current.operationState
        previousSelection = selection
    }

    DisposableEffect(Unit) {
        onDispose {
            quietJob?.cancel()
            // A completed edit must not disappear if Back is tapped during
            // the short slider settling window. Only flush a write that is
            // already safe to send; never retry a failure here.
            val current = latestPalette
            if (applyQueued &&
                !sliding &&
                !(current.busy || pendingRevision != null) &&
                latestOnline &&
                current.ready &&
                current.supported &&
                !matchesCurrent() &&
                !(latestIsNativeBusy?.invoke() ?: false)
            ) {
                onApply(wallpaper, seed, style)
            }
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Press) {
                            event.changes.forEach { change -> activePointers.add(change.id) }
                            quietJob?.cancel()
                            quietJob = null
                        } else {
                            var released = false
                            event.changes.forEach { change ->
                                if (!change.pressed) {
                                    activePointers.remove(change.id)
                                    released = true
                                }
                            }
                            if (released) dispatchWhenReady()
                        }
                    }
                }
            },
    ) {
        /* RODIN_SYSTEM_COLORS_UI */
    }
}
