package io.github.neeschal.rodinessential.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.HdrOn
import androidx.compose.material.icons.rounded.MovieFilter
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Tonality
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.neeschal.rodinessential.backend.RodinBackend
import io.github.neeschal.rodinessential.backend.RodinBackendSnapshot
import io.github.neeschal.rodinessential.ui.components.RodinHaptics
import io.github.neeschal.rodinessential.ui.components.SectionLabel
import io.github.neeschal.rodinessential.ui.components.SurfaceCard
import io.github.neeschal.rodinessential.ui.components.rodinShadow
import io.github.neeschal.rodinessential.ui.motion.RodinDetailTransition
import io.github.neeschal.rodinessential.ui.motion.RodinPredictivePlane
import io.github.neeschal.rodinessential.ui.motion.rodinAnimationsDisabled
import io.github.neeschal.rodinessential.ui.shell.RodinInteractionSettings
import io.github.neeschal.rodinessential.ui.shell.RodinNestedBackController

/**
 * Display Studio: colour calibration, HDR and visual enhancements. The colour
 * modes page is a nested plane which owns Back, exactly like the Dart page.
 */
@Composable
fun DisplayStudioScreen(onBack: () -> Unit) {
    val snapshot = rememberRodinSnapshot()
    var showColourModes by remember { mutableStateOf(false) }
    var colourVisible by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }
    val reduced = rodinAnimationsDisabled()
    val owner = remember { Any() }

    val gamut = RodinBackend.extendedValue(2)
    val expert = List(8) { index -> RodinBackend.extendedValue(3 + index) }
    val expertEnabled = snapshot.controlsAvailable &&
        snapshot.displayHal == 1 &&
        !snapshot.busy

    fun closeColourModes() {
        if (!showColourModes) return
        RodinHaptics.back()
        showColourModes = false
    }

    LaunchedEffect(Unit) {
        RodinBackend.refresh()
    }
    LaunchedEffect(showColourModes) {
        if (showColourModes) {
            colourVisible = true
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = if (reduced) {
                        0
                    } else {
                        RodinInteractionSettings.motionDuration(320)
                    },
                    easing = LinearEasing,
                ),
            )
        } else {
            progress.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = if (reduced) {
                        0
                    } else {
                        RodinInteractionSettings.motionDuration(220)
                    },
                    easing = LinearEasing,
                ),
            )
            colourVisible = false
        }
    }

    DisposableEffect(owner) {
        RodinNestedBackController.attach(
            owner,
            handler = {
                if (showColourModes) {
                    closeColourModes()
                    true
                } else {
                    false
                }
            },
            canPreview = { showColourModes },
        )
        onDispose { RodinNestedBackController.detach(owner) }
    }

    Box(Modifier.fillMaxSize()) {
        if (!showColourModes && colourVisible && progress.value > 0.001f) {
            RodinDetailTransition(
                progress = 1f - progress.value,
                reverse = true,
                active = true,
                forward = false,
            ) {
                DisplayStudioRoot(
                    snapshot = snapshot,
                    expertEnabled = expertEnabled,
                    onBack = onBack,
                    onOpenColourModes = {
                        if (!showColourModes) showColourModes = true
                    },
                )
            }
        } else {
            DisplayStudioRoot(
                snapshot = snapshot,
                expertEnabled = expertEnabled,
                onBack = onBack,
                onOpenColourModes = {
                    if (!showColourModes) showColourModes = true
                },
            )
        }

        if (showColourModes) {
            RodinPredictivePlane(
                nested = true,
                destination = {
                    DisplayStudioRoot(
                        snapshot = snapshot,
                        expertEnabled = expertEnabled,
                        onBack = onBack,
                        onOpenColourModes = {
                            if (!showColourModes) showColourModes = true
                        },
                    )
                },
            ) {
                if (progress.value >= 0.999f) {
                    ColourModesPage(
                        snapshot = snapshot,
                        gamut = gamut,
                        expert = expert,
                        expertEnabled = expertEnabled,
                        onBack = { closeColourModes() },
                    )
                } else {
                    RodinDetailTransition(
                        progress = progress.value,
                        reverse = false,
                        active = true,
                        forward = true,
                    ) {
                        ColourModesPage(
                            snapshot = snapshot,
                            gamut = gamut,
                            expert = expert,
                            expertEnabled = expertEnabled,
                            onBack = { closeColourModes() },
                        )
                    }
                }
            }
        } else if (colourVisible) {
            RodinDetailTransition(
                progress = progress.value,
                reverse = false,
                active = false,
                forward = false,
            ) {
                ColourModesPage(
                    snapshot = snapshot,
                    gamut = gamut,
                    expert = expert,
                    expertEnabled = expertEnabled,
                    onBack = { closeColourModes() },
                )
            }
        }
    }
}

@Composable
private fun DisplayStudioRoot(
    snapshot: RodinBackendSnapshot,
    expertEnabled: Boolean,
    onBack: () -> Unit,
    onOpenColourModes: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val accent = colors.primary

    RodinScrollPage(
        header = RodinPageHeader.Detail(title = "Display Studio", onBack = onBack),
    ) {
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Screen color calibration, HDR, and visual enhancements",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.5.sp,
                color = colors.onSurfaceVariant,
            ),
        )
        Spacer(Modifier.height(10.dp))
        HeroCard(
            icon = Icons.Rounded.Palette,
            accent = accent,
            title = "Display Engine",
            subtitle = when {
                snapshot.displayHal != 1 -> "Display calibration unavailable"
                snapshot.displayColor == -1 && snapshot.displayTemp == -1 ->
                    "OEM Control · Colour modes managed by your ROM"
                else -> "Saved colour overrides · Hardware engine ready"
            },
        )
        Spacer(Modifier.height(12.dp))
        SectionLabel("Display controls")
        Spacer(Modifier.height(8.dp))
        FeatureCard(
            icon = Icons.Rounded.ColorLens,
            accent = accent,
            title = "Colour modes",
            subtitle = "Display color, temperature, gamut, RGB, and tone balance",
            onTap = onOpenColourModes,
        )
        Spacer(Modifier.height(12.dp))
        SectionLabel("Display features")
        Spacer(Modifier.height(8.dp))
        RodinSwitchCard(
            icon = Icons.Rounded.WbSunny,
            accent = Color(0xFFFFB84D),
            title = "Sunlight mode",
            subtitle = "Improves screen readability under bright outdoor light",
            value = snapshot.sunlight == 1,
            enabled = expertEnabled,
            stateKnown = snapshot.sunlight >= 0,
            unknownLabel = "OEM",
            onChanged = { enabled -> RodinBackend.setSunlight(enabled) },
        )
        Spacer(Modifier.height(9.dp))
        RodinSwitchCard(
            icon = Icons.Rounded.Brightness6,
            accent = Color(0xFF67C2FF),
            title = "Silky brightness",
            subtitle = "Smooth brightness dimming curve for low-light comfort",
            value = snapshot.silky == 1,
            enabled = expertEnabled,
            stateKnown = snapshot.silky >= 0,
            unknownLabel = "OEM",
            onChanged = { enabled -> RodinBackend.setSilky(enabled) },
        )
        Spacer(Modifier.height(9.dp))
        RodinSwitchCard(
            icon = Icons.Rounded.MovieFilter,
            accent = Color(0xFFB087FF),
            title = "Video enhancement",
            subtitle = "Enhance contrast and detail in movies and video playback",
            value = snapshot.video == 1,
            enabled = expertEnabled,
            stateKnown = snapshot.video >= 0,
            unknownLabel = "OEM",
            onChanged = { enabled -> RodinBackend.setVideoEnhancement(enabled) },
        )
        Spacer(Modifier.height(9.dp))
        RodinSwitchCard(
            icon = Icons.Rounded.HdrOn,
            accent = Color(0xFF74E6C6),
            title = "Dolby Vision / HDR",
            subtitle = "Expand dynamic range and depth in HDR content",
            value = snapshot.dolby == 1,
            enabled = expertEnabled,
            stateKnown = snapshot.dolby >= 0,
            unknownLabel = "OEM",
            onChanged = { enabled -> RodinBackend.setDolbyVision(enabled) },
        )
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun ColourModesPage(
    snapshot: RodinBackendSnapshot,
    gamut: Int,
    expert: List<Int>,
    expertEnabled: Boolean,
    onBack: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val accent = colors.primary
    val expertCalibrationEnabled = expertEnabled && snapshot.displayColor == 0
    val density = LocalDensity.current

    val r = if (expert[0] >= 0) expert[0].coerceIn(0, 255) else 255
    val g = if (expert[1] >= 0) expert[1].coerceIn(0, 255) else 255
    val b = if (expert[2] >= 0) expert[2].coerceIn(0, 255) else 255
    val rgb = Color(r / 255f, g / 255f, b / 255f)
    val swatch by animateColorAsState(
        targetValue = rgb,
        animationSpec = tween(100),
        label = "colourSwatch",
    )

    RodinScrollPage(
        header = RodinPageHeader.Detail(title = "Colour modes", onBack = onBack),
    ) {
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Adjust color temperature, color gamut, and RGB channels",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.5.sp,
                color = colors.onSurfaceVariant,
            ),
        )
        Spacer(Modifier.height(10.dp))
        RodinChoiceCard(
            title = "Display colour",
            subtitle = "ROM-managed by default · Optional colour override",
            icon = Icons.Rounded.ColorLens,
            accent = accent,
            selectedCode = snapshot.displayColor,
            options = listOf(
                RodinChoiceOption(-1, "OEM Control"),
                RodinChoiceOption(0, "Original PRO"),
                RodinChoiceOption(1, "Vivid"),
                RodinChoiceOption(2, "Saturated"),
            ),
            enabled = expertEnabled,
            onSelected = { code -> RodinBackend.setDisplayColor(code) },
        )
        Spacer(Modifier.height(9.dp))
        RodinChoiceCard(
            title = "Colour temperature",
            subtitle = "ROM-managed by default · Optional temperature override",
            icon = Icons.Rounded.Thermostat,
            accent = Color(0xFFFFB84D),
            selectedCode = snapshot.displayTemp,
            options = listOf(
                RodinChoiceOption(-1, "OEM Control"),
                RodinChoiceOption(1, "Warm"),
                RodinChoiceOption(2, "Normal"),
                RodinChoiceOption(3, "Cold"),
            ),
            enabled = expertEnabled,
            onSelected = { code -> RodinBackend.setDisplayTemperature(code) },
        )
        Spacer(Modifier.height(9.dp))
        SurfaceCard {
            InfoBlock(
                title = "ROM ownership",
                text = "OEM Control stops Rodin Essential from applying this setting " +
                    "at boot or wake. Saved overrides restore automatically. Returning " +
                    "to OEM does not guess your ROM\u2019s colour profile: the current " +
                    "appearance can remain until your ROM reapplies its display settings.",
            )
        }
        Spacer(Modifier.height(9.dp))
        RodinChoiceCard(
            title = "Expert gamut",
            subtitle = if (snapshot.displayColor == 0) {
                "Original, Display P3 or sRGB"
            } else {
                "Select Original PRO to customize color space"
            },
            icon = Icons.Rounded.Tonality,
            accent = Color(0xFF56D8C7),
            selectedCode = gamut,
            options = listOf(
                RodinChoiceOption(1, "Original"),
                RodinChoiceOption(2, "Display P3"),
                RodinChoiceOption(3, "sRGB"),
            ),
            enabled = expertCalibrationEnabled,
            onSelected = { code -> RodinBackend.setExpertGamut(code) },
        )
        Spacer(Modifier.height(9.dp))
        SurfaceCard {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Live colour swatch",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.W800,
                        ),
                    )
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier
                            .background(
                                Color(r / 255f, g / 255f, b / 255f, 0.18f),
                                RoundedCornerShape(99.dp),
                            )
                            .border(
                                1.dp,
                                Color(r / 255f, g / 255f, b / 255f, 0.6f),
                                RoundedCornerShape(99.dp),
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text(
                            text = if (expert[0] >= 0) {
                                "RGB($r, $g, $b)"
                            } else {
                                "DEFAULT (255, 255, 255)"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.W800,
                                fontFamily = FontFamily.Monospace,
                            ),
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .rodinShadow(
                            color = rgb.copy(alpha = 0.4f),
                            blurRadius = 16f,
                            offsetY = 4f,
                            cornerRadius = with(density) { 14.dp.toPx() },
                        )
                        .background(swatch, RoundedCornerShape(14.dp))
                        .border(1.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(14.dp)),
                )
            }
        }
        Spacer(Modifier.height(9.dp))
        SurfaceCard {
            Column {
                Text(
                    text = "RGB channels",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.W800,
                    ),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Live expert channel calibration",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.2.sp,
                        color = colors.onSurfaceVariant,
                    ),
                )
                Spacer(Modifier.height(6.dp))
                RodinExpertSliderRow(
                    title = "R",
                    value = expert[0],
                    min = 0,
                    max = 255,
                    defaultValue = 255,
                    activeColor = Color(0xFFFF3B30),
                    enabled = expertCalibrationEnabled,
                    onChanged = { value -> RodinBackend.setExpertChannel(1, value) },
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    thickness = 0.8.dp,
                )
                RodinExpertSliderRow(
                    title = "G",
                    value = expert[1],
                    min = 0,
                    max = 255,
                    defaultValue = 255,
                    activeColor = Color(0xFF34C759),
                    enabled = expertCalibrationEnabled,
                    onChanged = { value -> RodinBackend.setExpertChannel(2, value) },
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    thickness = 0.8.dp,
                )
                RodinExpertSliderRow(
                    title = "B",
                    value = expert[2],
                    min = 0,
                    max = 255,
                    defaultValue = 255,
                    activeColor = Color(0xFF007AFF),
                    enabled = expertCalibrationEnabled,
                    onChanged = { value -> RodinBackend.setExpertChannel(3, value) },
                )
            }
        }
        Spacer(Modifier.height(9.dp))
        SurfaceCard {
            Column {
                Text(
                    text = "HSV & tone",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.W800,
                    ),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Live colour and tone shaping",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.2.sp,
                        color = colors.onSurfaceVariant,
                    ),
                )
                Spacer(Modifier.height(6.dp))
                RodinExpertSliderRow(
                    title = "Hue",
                    value = expert[3],
                    min = 0,
                    max = 255,
                    defaultValue = 0,
                    activeColor = Color(0xFFFF9F0A),
                    enabled = expertCalibrationEnabled,
                    onChanged = { value -> RodinBackend.setExpertChannel(4, value) },
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    thickness = 0.8.dp,
                )
                RodinExpertSliderRow(
                    title = "Saturation",
                    value = expert[4],
                    min = -40,
                    max = 50,
                    defaultValue = 0,
                    activeColor = Color(0xFFBF5AF2),
                    enabled = expertCalibrationEnabled,
                    onChanged = { value -> RodinBackend.setExpertChannel(5, value) },
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    thickness = 0.8.dp,
                )
                RodinExpertSliderRow(
                    title = "Value",
                    value = expert[5],
                    min = -240,
                    max = 255,
                    defaultValue = 0,
                    activeColor = Color(0xFF64D2FF),
                    enabled = expertCalibrationEnabled,
                    onChanged = { value -> RodinBackend.setExpertChannel(6, value) },
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    thickness = 0.8.dp,
                )
                RodinExpertSliderRow(
                    title = "Contrast",
                    value = expert[6],
                    min = 0,
                    max = 100,
                    defaultValue = 50,
                    activeColor = Color(0xFFFFD60A),
                    enabled = expertCalibrationEnabled,
                    onChanged = { value -> RodinBackend.setExpertChannel(7, value) },
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    thickness = 0.8.dp,
                )
                RodinExpertSliderRow(
                    title = "Gamma",
                    value = expert[7],
                    min = 254,
                    max = 270,
                    defaultValue = 262,
                    activeColor = Color(0xFF30D158),
                    enabled = expertCalibrationEnabled,
                    onChanged = { value -> RodinBackend.setExpertChannel(8, value) },
                )
                Spacer(Modifier.height(5.dp))
                FilledTonalButton(
                    onClick = {
                        if (RodinBackend.resetExpertDisplay()) {
                            RodinHaptics.confirm()
                        } else {
                            RodinHaptics.reject()
                        }
                    },
                    enabled = expertCalibrationEnabled,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.RestartAlt,
                        contentDescription = null,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Restore expert defaults")
                }
            }
        }
        Spacer(Modifier.height(10.dp))
    }
}
