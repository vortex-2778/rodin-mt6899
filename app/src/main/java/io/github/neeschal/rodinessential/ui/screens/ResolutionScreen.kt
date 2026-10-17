package io.github.neeschal.rodinessential.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageVector
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.neeschal.rodinessential.backend.RodinBackend
import io.github.neeschal.rodinessential.platform.RodinPlatform
import io.github.neeschal.rodinessential.ui.components.IconTile
import io.github.neeschal.rodinessential.ui.components.PressScale
import io.github.neeschal.rodinessential.ui.components.RodinHaptics
import io.github.neeschal.rodinessential.ui.components.SurfaceCard
import io.github.neeschal.rodinessential.ui.shell.RodinFeedbackOverlay
import io.github.neeschal.rodinessential.ui.shell.RodinInteractionSettings
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.roundToInt

private val resolutionAccent = Color(0xFFFFBE63)

private fun densityForWidth(nativeDensity: Int, width: Int): Int =
    (nativeDensity * width + 610) / 1220

private fun resolutionHz(value: Int): String {
    if (value <= 0) return "—"
    val rate = value / 10.0
    return if (value % 10 == 0) {
        String.format("%.0f", rate) + " Hz"
    } else {
        String.format("%.1f", rate) + " Hz"
    }
}

private data class ResolutionPreset(
    val title: String,
    val badge: String,
    val resolutionText: String,
    val width: Int,
    val height: Int,
    val density: Int,
    val aspectRatio: String,
    val description: String,
    val accentColor: Color,
    val icon: ImageVector,
    val isActive: Boolean,
)

/** Resolution and logical-density scaling with the custom tuner. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResolutionScreen(onBack: () -> Unit) {
    val snapshot = rememberRodinSnapshot()
    val colors = MaterialTheme.colorScheme
    val dark = colors.surface.luminance() < 0.5f
    val accent = resolutionAccent

    var customWidth by remember { mutableIntStateOf(1080) }
    var customHeight by remember { mutableIntStateOf(2400) }
    var customDensity by remember { mutableIntStateOf(460) }
    var lockAspectRatio by remember { mutableStateOf(true) }
    var applying by remember { mutableStateOf(false) }
    var customDefaultsLoaded by remember { mutableStateOf(false) }
    var sliderDragging by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf("") }
    var toastVisible by remember { mutableStateOf(false) }
    var applySeq by remember { mutableIntStateOf(0) }
    var pendingNative by remember { mutableStateOf(false) }
    var pendingLabel by remember { mutableStateOf("") }
    var pendingWidth by remember { mutableIntStateOf(0) }
    var pendingHeight by remember { mutableIntStateOf(0) }
    var pendingDensity by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        RodinBackend.refresh()
    }
    LaunchedEffect(applySeq) {
        if (applySeq > 0) {
            delay(350)
            RodinBackend.refresh()
            applying = false
            toastMessage = if (pendingNative) {
                "Reset to Rodin Essential native panel"
            } else {
                "Applied $pendingLabel ($pendingWidth × $pendingHeight @ ${pendingDensity}DPI)"
            }
            toastVisible = true
        }
    }
    LaunchedEffect(toastVisible) {
        if (toastVisible) {
            delay(2400)
            toastVisible = false
        }
    }

    fun applyResolution(width: Int, height: Int, density: Int, label: String) {
        RodinPlatform.haptic(2)
        applying = true
        val isNative = (width <= 0 || height <= 0) || (width == 1220 && height == 2712)
        if (isNative) {
            RodinBackend.resetDisplayResolution()
        } else {
            RodinBackend.setDisplayResolution(width, height, density)
        }
        pendingNative = isNative
        pendingLabel = label
        pendingWidth = width
        pendingHeight = height
        pendingDensity = density
        applySeq += 1
    }

    val activeWidth = RodinBackend.extendedValue(22)
    val activeHeight = RodinBackend.extendedValue(23)
    val hzX10 = RodinBackend.extendedValue(24)
    val selectedHzX10 = RodinBackend.extendedValue(25)
    val detectedNativeDensity = RodinBackend.extendedValue(78)
    val nativeDensity = if (detectedNativeDensity > 0) detectedNativeDensity else 520
    val detectedActiveDensity = RodinBackend.extendedValue(38)
    val activeDensity = if (detectedActiveDensity > 0) detectedActiveDensity else nativeDensity

    val isNative = activeWidth <= 0 || (activeWidth == 1220 && activeHeight == 2712)
    val isFhd = activeWidth == 1080 && activeHeight == 2400
    val isHd = activeWidth == 720 && activeHeight == 1600
    val isQhd = activeWidth == 1440 && activeHeight == 3200

    LaunchedEffect(
        customDefaultsLoaded,
        detectedNativeDensity,
        activeWidth,
        activeHeight,
        activeDensity,
        nativeDensity,
        isNative,
    ) {
        if (!customDefaultsLoaded && detectedNativeDensity > 0) {
            customDefaultsLoaded = true
            if (!isNative && activeWidth > 0 && activeHeight > 0) {
                customWidth = activeWidth
                customHeight = activeHeight
                customDensity = activeDensity
            } else {
                customDensity = densityForWidth(nativeDensity, customWidth)
            }
        }
    }

    val refreshValue = if (hzX10 > 0 && selectedHzX10 > 0 && hzX10 != selectedHzX10) {
        resolutionHz(hzX10).replace(" Hz", "") + " / " + resolutionHz(selectedHzX10)
    } else {
        resolutionHz(if (hzX10 > 0) hzX10 else selectedHzX10)
    }
    val densityMax = max(nativeDensity * 2, 800)

    val presets = listOf(
        ResolutionPreset(
            title = "Native 1.5K Pro",
            badge = "1.5K NATIVE",
            resolutionText = "1220 × 2712",
            width = 1220,
            height = 2712,
            density = nativeDensity,
            aspectRatio = "20:9",
            description = "Rodin physical hardware panel resolution. Maximum " +
                "sharpness and true 1:1 pixel rendering with zero scaling.",
            accentColor = Color(0xFF59BCFF),
            icon = Icons.Rounded.AutoAwesome,
            isActive = isNative,
        ),
        ResolutionPreset(
            title = "Full HD+ (High Performance)",
            badge = "FHD+ 1080p",
            resolutionText = "1080 × 2400",
            width = 1080,
            height = 2400,
            density = densityForWidth(nativeDensity, 1080),
            aspectRatio = "20:9",
            description = "Reduces GPU rasterization load by ~22%. Boosts sustained " +
                "120 FPS frame stability in heavy 3D games.",
            accentColor = Color(0xFF38E598),
            icon = Icons.Rounded.SportsEsports,
            isActive = isFhd,
        ),
        ResolutionPreset(
            title = "HD+ (Ultra Battery Saver)",
            badge = "HD+ 720p",
            resolutionText = "720 × 1600",
            width = 720,
            height = 1600,
            density = densityForWidth(nativeDensity, 720),
            aspectRatio = "20:9",
            description = "Lowest GPU workload and power consumption. Drastically " +
                "extends battery life in emergency situations.",
            accentColor = Color(0xFFFFA048),
            icon = Icons.Rounded.BatteryChargingFull,
            isActive = isHd,
        ),
        ResolutionPreset(
            title = "Quad HD+ (High Density Canvas)",
            badge = "QHD+ 1440p",
            resolutionText = "1440 × 3200",
            width = 1440,
            height = 3200,
            density = densityForWidth(nativeDensity, 1440),
            aspectRatio = "20:9",
            description = "Downscaled 2K virtual canvas for maximum screen real " +
                "estate and ultra-compact multitasking view.",
            accentColor = Color(0xFFB087FF),
            icon = Icons.Rounded.Fullscreen,
            isActive = isQhd,
        ),
    )

    Box(Modifier.fillMaxSize()) {
        RodinScrollPage(
            header = RodinPageHeader.Detail(title = "Resolution", onBack = onBack),
        ) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "ROM-aware resolution and logical-density scaling for Rodin " +
                    "(1220×2712 1.5K AMOLED)",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.5.sp,
                    color = colors.onSurfaceVariant,
                ),
            )
            Spacer(Modifier.height(12.dp))

            SurfaceCard(padding = PaddingValues(18.dp)) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(44.dp)
                                .background(
                                    accent.copy(alpha = if (dark) 0.14f else 0.09f),
                                    RoundedCornerShape(14.dp),
                                )
                                .border(
                                    1.dp,
                                    accent.copy(alpha = if (dark) 0.28f else 0.18f),
                                    RoundedCornerShape(14.dp),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.GridView,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        Spacer(Modifier.width(13.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = if (activeWidth > 0 && activeHeight > 0) {
                                    "$activeWidth × $activeHeight"
                                } else {
                                    "1220 × 2712"
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontSize = 21.sp,
                                    lineHeight = 21.sp,
                                    fontWeight = FontWeight.W900,
                                    letterSpacing = (-0.5).sp,
                                ),
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = when {
                                    isNative -> "Rodin 1.5K AMOLED Native (1:1 Map)"
                                    isFhd -> "FHD+ High Performance Mode"
                                    isHd -> "HD+ Battery Saver Mode"
                                    isQhd -> "QHD+ Canvas Mode"
                                    else -> "Custom Resolution Override"
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.W600,
                                    color = colors.onSurfaceVariant,
                                ),
                            )
                        }
                        val badgeColor = if (isNative) Color(0xFF59BCFF) else accent
                        Box(
                            Modifier
                                .background(badgeColor.copy(alpha = 0.14f), RoundedCornerShape(99.dp))
                                .border(1.dp, badgeColor.copy(alpha = 0.35f), RoundedCornerShape(99.dp))
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                        ) {
                            Text(
                                text = if (isNative) "1.5K NATIVE" else "SCALED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.W900,
                                    letterSpacing = 0.7.sp,
                                    color = badgeColor,
                                ),
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ResolutionMetricPill(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.AspectRatio,
                            label = "Aspect",
                            value = "20:9",
                        )
                        Spacer(Modifier.width(8.dp))
                        ResolutionMetricPill(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.Speed,
                            label = if (hzX10 > 0 && selectedHzX10 > 0 && hzX10 != selectedHzX10) {
                                "Live / Selected"
                            } else {
                                "Refresh"
                            },
                            value = refreshValue,
                        )
                        Spacer(Modifier.width(8.dp))
                        ResolutionMetricPill(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.Grain,
                            label = "Density",
                            value = "$activeDensity DPI",
                        )
                    }
                    if (!isNative) {
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = {
                                applyResolution(1220, 2712, nativeDensity, "1.5K Native")
                            },
                            enabled = !applying,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF59BCFF),
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                Color(0xFF59BCFF).copy(alpha = 0.4f),
                            ),
                            shape = RoundedCornerShape(13.dp),
                            contentPadding = PaddingValues(vertical = 11.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Reset to 1.5K Native (1220 × 2712)",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.W800,
                                    fontSize = 13.sp,
                                ),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))

            FlowRow(
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "RODIN PRESET MAPPINGS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.W900,
                        letterSpacing = 1.1.sp,
                    ),
                )
                Text(
                    text = "1220×2712 Physical Panel",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.W600,
                        color = colors.onSurfaceVariant,
                    ),
                )
            }
            Spacer(Modifier.height(8.dp))

            for (preset in presets) {
                ResolutionPresetCard(
                    preset = preset,
                    applying = applying,
                    onTap = {
                        applyResolution(
                            preset.width,
                            preset.height,
                            preset.density,
                            preset.title,
                        )
                    },
                )
                Spacer(Modifier.height(9.dp))
            }

            Spacer(Modifier.height(8.dp))

            SurfaceCard(padding = PaddingValues(16.dp)) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconTile(
                            icon = Icons.Rounded.Tune,
                            accent = accent,
                            size = 40.dp,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Custom Resolution Tuner",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 15.5.sp,
                                    fontWeight = FontWeight.W800,
                                ),
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Uses this ROM's native density as the scaling baseline",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.5.sp,
                                    color = colors.onSurfaceVariant,
                                ),
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (lockAspectRatio) {
                                Icons.Rounded.Lock
                            } else {
                                Icons.Rounded.LockOpen
                            },
                            contentDescription = null,
                            tint = if (lockAspectRatio) accent else colors.onSurfaceVariant,
                            modifier = Modifier.size(17.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Lock 20:9 Aspect Ratio (Prevents distortion)",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.W700,
                            ),
                        )
                        Switch(
                            checked = lockAspectRatio,
                            onCheckedChange = { value ->
                                lockAspectRatio = value
                                RodinHaptics.toggle(value)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = accent,
                                checkedTrackColor = accent.copy(alpha = 0.35f),
                            ),
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 9.dp),
                        thickness = 0.8.dp,
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Horizontal Width",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.W700,
                            ),
                        )
                        Text(
                            text = "$customWidth px",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.W900,
                                color = accent,
                            ),
                        )
                    }
                    Slider(
                        value = customWidth.toFloat(),
                        onValueChange = { raw ->
                            if (!sliderDragging) {
                                sliderDragging = true
                                RodinHaptics.segment()
                            }
                            val w = raw.roundToInt()
                            customWidth = w
                            if (lockAspectRatio) {
                                customHeight = w * 2712 / 1220
                                customDensity = densityForWidth(nativeDensity, w)
                            }
                            RodinHaptics.frequentSegment()
                        },
                        onValueChangeFinished = {
                            sliderDragging = false
                            RodinHaptics.confirm()
                        },
                        valueRange = 720f..1440f,
                        steps = 71,
                        colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent),
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Vertical Height",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.W700,
                            ),
                        )
                        Text(
                            text = "$customHeight px",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.W900,
                                color = accent,
                            ),
                        )
                    }
                    Slider(
                        value = customHeight.toFloat().coerceIn(1600f, 3200f),
                        onValueChange = { raw ->
                            if (!sliderDragging) {
                                sliderDragging = true
                                RodinHaptics.segment()
                            }
                            customHeight = raw.roundToInt()
                            RodinHaptics.frequentSegment()
                        },
                        onValueChangeFinished = {
                            sliderDragging = false
                            RodinHaptics.confirm()
                        },
                        enabled = !lockAspectRatio,
                        valueRange = 1600f..3200f,
                        steps = 159,
                        colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent),
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Pixel Density (DPI)",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.W700,
                            ),
                        )
                        Text(
                            text = "$customDensity DPI",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.W900,
                                color = accent,
                            ),
                        )
                    }
                    Slider(
                        value = customDensity.toFloat().coerceIn(160f, densityMax.toFloat()),
                        onValueChange = { raw ->
                            if (!sliderDragging) {
                                sliderDragging = true
                                RodinHaptics.segment()
                            }
                            customDensity = raw.roundToInt()
                            RodinHaptics.frequentSegment()
                        },
                        onValueChangeFinished = {
                            sliderDragging = false
                            RodinHaptics.confirm()
                        },
                        valueRange = 160f..densityMax.toFloat(),
                        steps = ((densityMax - 160) / 5) - 1,
                        colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent),
                    )

                    Spacer(Modifier.height(6.dp))
                    Button(
                        onClick = {
                            applyResolution(
                                customWidth,
                                customHeight,
                                customDensity,
                                "Custom",
                            )
                        },
                        enabled = !applying,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent,
                            contentColor = Color.Black,
                        ),
                        shape = RoundedCornerShape(13.dp),
                        contentPadding = PaddingValues(vertical = 13.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (applying) {
                                "Applying..."
                            } else {
                                "Apply Custom $customWidth × $customHeight (${customDensity}DPI)"
                            },
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.W900,
                                fontSize = 13.5.sp,
                            ),
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            SurfaceCard(padding = PaddingValues(16.dp)) {
                Column {
                    Text(
                        text = "Rodin Display Architecture",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.W800,
                        ),
                    )
                    Spacer(Modifier.height(10.dp))
                    DiagnosticRow(
                        label = "Physical Panel Size",
                        good = true,
                        detail = "1220 × 2712 (1.5K AMOLED)",
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 7.dp),
                        thickness = 0.8.dp,
                    )
                    DiagnosticRow(
                        label = "ROM Native Logical Density",
                        good = true,
                        detail = "$nativeDensity DPI (detected automatically)",
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 7.dp),
                        thickness = 0.8.dp,
                    )
                    DiagnosticRow(
                        label = "Refresh Rate",
                        good = true,
                        detail = if (hzX10 > 0 && selectedHzX10 > 0 && hzX10 != selectedHzX10) {
                            "${resolutionHz(hzX10)} live · ${resolutionHz(selectedHzX10)} selected"
                        } else {
                            "${resolutionHz(if (hzX10 > 0) hzX10 else selectedHzX10)} system managed"
                        },
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 7.dp),
                        thickness = 0.8.dp,
                    )
                    DiagnosticRow(
                        label = "Panel Aspect Ratio",
                        good = true,
                        detail = "20:9 (Native Precision)",
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 7.dp),
                        thickness = 0.8.dp,
                    )
                    DiagnosticRow(
                        label = "Active Canvas Mode",
                        good = true,
                        detail = if (isNative) {
                            "1:1 Native Physical"
                        } else {
                            "$activeWidth × $activeHeight Scaled"
                        },
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        RodinFeedbackOverlay(
            tag = "RESOLUTION",
            message = toastMessage,
            icon = Icons.Rounded.CheckCircle,
            accent = resolutionAccent,
            visible = toastVisible,
            durationMillis = RodinInteractionSettings.motionDuration(260),
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun ResolutionPresetCard(
    preset: ResolutionPreset,
    applying: Boolean,
    onTap: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.surface.luminance() < 0.5f
    val card: @Composable () -> Unit = {
        SurfaceCard(padding = PaddingValues(15.dp)) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(38.dp)
                            .background(
                                preset.accentColor.copy(alpha = if (dark) 0.14f else 0.09f),
                                RoundedCornerShape(12.dp),
                            )
                            .border(
                                1.dp,
                                preset.accentColor.copy(alpha = if (dark) 0.28f else 0.18f),
                                RoundedCornerShape(12.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = preset.icon,
                            contentDescription = null,
                            tint = preset.accentColor,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = preset.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.W800,
                            ),
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "${preset.resolutionText} · ${preset.density} DPI · " +
                                preset.aspectRatio,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.2.sp,
                                fontWeight = FontWeight.W700,
                                color = preset.accentColor,
                            ),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier
                            .background(
                                if (preset.isActive) {
                                    preset.accentColor.copy(alpha = 0.18f)
                                } else {
                                    colors.surfaceContainerHighest.copy(alpha = 0.5f)
                                },
                                RoundedCornerShape(99.dp),
                            )
                            .border(
                                1.dp,
                                if (preset.isActive) {
                                    preset.accentColor.copy(alpha = 0.45f)
                                } else {
                                    colors.outline.copy(alpha = 0.2f)
                                },
                                RoundedCornerShape(99.dp),
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (preset.isActive) {
                                Box(
                                    Modifier
                                        .size(6.dp)
                                        .background(preset.accentColor, RoundedCornerShape(99.dp)),
                                )
                                Spacer(Modifier.width(5.dp))
                            }
                            Text(
                                text = if (preset.isActive) "ACTIVE" else preset.badge,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.W900,
                                    letterSpacing = 0.6.sp,
                                    color = if (preset.isActive) {
                                        preset.accentColor
                                    } else {
                                        colors.onSurfaceVariant
                                    },
                                ),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = preset.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.8.sp,
                        lineHeight = 11.8.sp * 1.35f,
                        color = colors.onSurfaceVariant,
                    ),
                )
            }
        }
    }
    if (!applying && !preset.isActive) {
        PressScale(onTap = onTap) { card() }
    } else {
        card()
    }
}

@Composable
private fun ResolutionMetricPill(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.surface.luminance() < 0.5f
    Row(
        modifier
            .background(
                colors.surfaceContainerHighest.copy(alpha = if (dark) 0.35f else 0.6f),
                RoundedCornerShape(11.dp),
            )
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.onSurfaceVariant,
            modifier = Modifier.size(13.dp),
        )
        Spacer(Modifier.width(5.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.W700,
                    color = colors.onSurfaceVariant,
                ),
            )
            Text(
                text = value,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.W800,
                ),
            )
        }
    }
}
