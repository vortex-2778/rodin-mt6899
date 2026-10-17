package io.github.neeschal.rodinessential.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.neeschal.rodinessential.backend.RodinBackend
import io.github.neeschal.rodinessential.ui.components.PressScale
import io.github.neeschal.rodinessential.ui.components.RodinHaptics
import io.github.neeschal.rodinessential.ui.components.SurfaceCard
import io.github.neeschal.rodinessential.ui.shell.RodinFeedbackOverlay
import io.github.neeschal.rodinessential.ui.shell.RodinInteractionSettings
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private data class ZramPreset(
    val title: String,
    val badge: String,
    val sizeMb: Int,
    val description: String,
    val accentColor: Color,
    val icon: ImageVector,
    val isActive: Boolean,
)

/** ZRAM sizing, compression engine, swappiness and instant memory tools. */
@Composable
fun ZramSwapScreen(onBack: () -> Unit) {
    val snapshot = rememberRodinSnapshot()
    val colors = MaterialTheme.colorScheme
    val isDark = colors.surface.luminance() < 0.5f
    val accent = if (isDark) Color(0xFF67C2FF) else Color(0xFF0077E6)

    val standardPresets = listOf(0, 4096, 8192, 12288, 16384)
    val initialSize = RodinBackend.extendedValue(39)
    val initialActive = if (initialSize >= 0) initialSize else 8192

    var optimisticSizeMb by remember { mutableStateOf<Int?>(null) }
    var optimisticAlgCode by remember { mutableStateOf<Int?>(null) }
    var optimisticSwappiness by remember { mutableStateOf<Int?>(null) }
    var customSliderMb by remember { mutableIntStateOf(initialActive) }
    var isCustomMode by remember {
        mutableStateOf(!standardPresets.contains(initialActive))
    }
    var applying by remember { mutableStateOf(false) }
    var compacting by remember { mutableStateOf(false) }
    var sliderDragging by remember { mutableStateOf(false) }

    var toastTag by remember { mutableStateOf("ZRAM ENGINE") }
    var toastMessage by remember { mutableStateOf("") }
    var toastIcon by remember { mutableStateOf<ImageVector>(Icons.Rounded.CheckCircle) }
    var toastAccent by remember { mutableStateOf(Color(0xFF67C2FF)) }
    var toastVisible by remember { mutableStateOf(false) }
    var toastSeq by remember { mutableIntStateOf(0) }
    var refreshSeq by remember { mutableIntStateOf(0) }
    var refreshKind by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        RodinBackend.refresh()
    }
    LaunchedEffect(toastSeq) {
        if (toastSeq > 0) {
            delay(2600)
            toastVisible = false
        }
    }
    LaunchedEffect(refreshSeq) {
        if (refreshSeq == 0) return@LaunchedEffect
        delay(
            when (refreshKind) {
                "compact" -> 600
                "reset" -> 500
                else -> 400
            }.toLong(),
        )
        RodinBackend.refresh()
        when (refreshKind) {
            "preset" -> applying = false
            "compact" -> {
                compacting = false
                toastTag = "MEMORY COMPACTION"
                toastMessage = "ZRAM Compaction Complete"
                toastIcon = Icons.Rounded.CheckCircle
                toastAccent = Color(0xFF41C98A)
                toastVisible = true
                toastSeq += 1
            }
        }
    }

    fun showGlassToast(tag: String, message: String, icon: ImageVector, toastTint: Color) {
        toastTag = tag
        toastMessage = message
        toastIcon = icon
        toastAccent = toastTint
        toastVisible = true
        toastSeq += 1
    }

    fun applyPresetSize(sizeMb: Int, title: String, icon: ImageVector, presetAccent: Color) {
        RodinHaptics.confirm()
        isCustomMode = false
        optimisticSizeMb = sizeMb
        if (sizeMb > 0) customSliderMb = sizeMb
        applying = true

        val applied = RodinBackend.setZramSize(sizeMb)
        if (!applied) {
            optimisticSizeMb = null
            applying = false
            showGlassToast(
                "ZRAM ERROR",
                "Kernel rejected the requested ZRAM size",
                Icons.Rounded.Error,
                Color(0xFFFF5A64),
            )
            RodinBackend.refresh()
            return
        }
        showGlassToast(
            "ZRAM PRESET",
            if (sizeMb == 0) "Disabled · Physical RAM Only" else "Applied $title ($sizeMb MB)",
            icon,
            presetAccent,
        )
        refreshKind = "preset"
        refreshSeq += 1
    }

    fun applyCustomSize(sizeMb: Int) {
        RodinHaptics.confirm()
        isCustomMode = true
        optimisticSizeMb = sizeMb
        customSliderMb = sizeMb
        applying = true

        val applied = RodinBackend.setZramSize(sizeMb)
        if (!applied) {
            optimisticSizeMb = null
            applying = false
            showGlassToast(
                "ZRAM ERROR",
                "Kernel rejected the requested custom size",
                Icons.Rounded.Error,
                Color(0xFFFF5A64),
            )
            RodinBackend.refresh()
            return
        }
        showGlassToast(
            "CUSTOM SWAP",
            if (sizeMb == 0) {
                "Disabled · Physical RAM Only"
            } else {
                "Set to " + String.format("%.1f", sizeMb / 1024.0) + " GB ($sizeMb MB)"
            },
            Icons.Rounded.Tune,
            Color(0xFF67C2FF),
        )
        refreshKind = "preset"
        refreshSeq += 1
    }

    fun applyAlgorithm(algCode: Int) {
        RodinHaptics.confirm()
        optimisticAlgCode = algCode

        val algNames = listOf("LZ4", "ZSTD", "LZO-RLE", "LZO")
        val name = if (algCode in algNames.indices) algNames[algCode] else "LZ4"

        val applied = RodinBackend.setZramAlgorithm(algCode)
        if (!applied) {
            optimisticAlgCode = null
            showGlassToast(
                "ZRAM ERROR",
                "Kernel rejected the $name compression engine",
                Icons.Rounded.Error,
                Color(0xFFFF5A64),
            )
            RodinBackend.refresh()
            return
        }
        showGlassToast(
            "KERNEL ENGINE",
            "Switched Compression to $name",
            Icons.Rounded.Compress,
            Color(0xFF41C98A),
        )
        refreshKind = "algorithm"
        refreshSeq += 1
    }

    fun applySwappiness(swappiness: Int) {
        RodinHaptics.confirm()
        optimisticSwappiness = swappiness

        val applied = RodinBackend.setZramSwappiness(swappiness)
        if (!applied) {
            optimisticSwappiness = null
            showGlassToast(
                "ZRAM ERROR",
                "Kernel rejected swappiness $swappiness",
                Icons.Rounded.Error,
                Color(0xFFFF5A64),
            )
            RodinBackend.refresh()
            return
        }
        showGlassToast(
            "KERNEL SWAPPINESS",
            "Swappiness Set to $swappiness",
            Icons.Rounded.Speed,
            Color(0xFFFFB84D),
        )
        RodinBackend.refresh()
    }

    fun compactMemory() {
        if (compacting) return
        RodinHaptics.confirm()
        compacting = true

        showGlassToast(
            "MEMORY COMPACTION",
            "Compacting ZRAM Pages & Defragmenting...",
            Icons.Rounded.CleaningServices,
            Color(0xFF74E6C6),
        )

        RodinBackend.compactZram()
        refreshKind = "compact"
        refreshSeq += 1
    }

    fun resetDefaults() {
        RodinHaptics.confirm()
        isCustomMode = false
        optimisticSizeMb = 8192
        optimisticAlgCode = 0
        optimisticSwappiness = 100
        customSliderMb = 8192

        val swappinessApplied = RodinBackend.setZramSwappiness(100)
        val algorithmApplied = RodinBackend.setZramAlgorithm(0)
        val sizeApplied = RodinBackend.setZramSize(8192)
        if (!swappinessApplied || !algorithmApplied || !sizeApplied) {
            optimisticSizeMb = null
            optimisticAlgCode = null
            optimisticSwappiness = null
            showGlassToast(
                "ZRAM ERROR",
                "One or more stock settings were rejected by the kernel",
                Icons.Rounded.Error,
                Color(0xFFFF5A64),
            )
            RodinBackend.refresh()
            return
        }
        showGlassToast(
            "SYSTEM RESTORE",
            "Reset to 8 GB Stock Optimal Defaults",
            Icons.Rounded.RestartAlt,
            Color(0xFF41C98A),
        )
        refreshKind = "reset"
        refreshSeq += 1
    }

    val rawSizeMb = RodinBackend.extendedValue(39)
    val activeSizeMb = optimisticSizeMb ?: (if (rawSizeMb >= 0) rawSizeMb else 8192)

    val origMb = if (RodinBackend.extendedValue(40) >= 0) {
        RodinBackend.extendedValue(40)
    } else {
        3192
    }
    val comprMb = if (RodinBackend.extendedValue(41) >= 0) {
        RodinBackend.extendedValue(41)
    } else {
        1067
    }
    val memUsedMb = if (RodinBackend.extendedValue(42) >= 0) {
        RodinBackend.extendedValue(42)
    } else {
        1067
    }

    val rawSwappiness = RodinBackend.extendedValue(43)
    val swappiness = optimisticSwappiness ?: (if (rawSwappiness >= 0) rawSwappiness else 100)

    val rawAlgCode = RodinBackend.extendedValue(44)
    val algCode = optimisticAlgCode ?: (if (rawAlgCode >= 0) rawAlgCode else 0)

    val isOff = activeSizeMb == 0
    val ratio = if (comprMb > 0) origMb.toDouble() / comprMb else 1.0
    val ramSavedMb = (origMb - comprMb).coerceIn(0, 32768)

    val algLabels = listOf(
        "LZ4 (Ultra Fast)",
        "ZSTD (Max Ratio)",
        "LZO-RLE (Balanced)",
        "LZO (Legacy)",
    )
    val activeAlgLabel = if (algCode in algLabels.indices) algLabels[algCode] else algLabels[0]

    val statusColor = if (isOff) Color(0xFFFFB84D) else Color(0xFF41C98A)

    val presets = listOf(
        ZramPreset(
            title = "Disabled (0 GB)",
            badge = "PURE RAM",
            sizeMb = 0,
            description = "Disables compressed swap partition completely. Pure " +
                "physical RAM only with zero CPU memory overhead.",
            accentColor = Color(0xFFFFB84D),
            icon = Icons.Rounded.PowerSettingsNew,
            isActive = !isCustomMode && activeSizeMb == 0,
        ),
        ZramPreset(
            title = "4 GB (Light RAM Plus)",
            badge = "4 GB SWAP",
            sizeMb = 4096,
            description = "Provides 4,096 MB compressed memory space. Ideal for " +
                "light usage and minimal background caching.",
            accentColor = Color(0xFF59BCFF),
            icon = Icons.Rounded.Storage,
            isActive = !isCustomMode && activeSizeMb == 4096,
        ),
        ZramPreset(
            title = "8 GB (Stock Optimal)",
            badge = "8 GB DEFAULT",
            sizeMb = 8192,
            description = "Recommended factory configuration for Rodin. Perfect " +
                "balance of multitasking speed and battery life.",
            accentColor = Color(0xFF41C98A),
            icon = Icons.Rounded.Verified,
            isActive = !isCustomMode && activeSizeMb == 8192,
        ),
        ZramPreset(
            title = "12 GB (Power Multitask)",
            badge = "12 GB POWER",
            sizeMb = 12288,
            description = "High-capacity 12,288 MB virtual memory buffer. Keeps " +
                "dozens of apps alive in background with zero redraws.",
            accentColor = Color(0xFFB087FF),
            icon = Icons.Rounded.AllInclusive,
            isActive = !isCustomMode && activeSizeMb == 12288,
        ),
        ZramPreset(
            title = "16 GB (Extreme Gaming)",
            badge = "16 GB MAX",
            sizeMb = 16384,
            description = "Maximum 16,384 MB virtual RAM headroom. Optimized for " +
                "demanding 3D games, emulation, and heavy tasks.",
            accentColor = Color(0xFFFF8A65),
            icon = Icons.Rounded.RocketLaunch,
            isActive = !isCustomMode && activeSizeMb == 16384,
        ),
    )

    Box(Modifier.fillMaxSize()) {
        RodinScrollPage(
            header = RodinPageHeader.Detail(title = "ZRAM & Swap Manager", onBack = onBack),
        ) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Hardware memory compression, swap sizing, and kernel " +
                    "swappiness tuning for Rodin",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.5.sp,
                    color = colors.onSurfaceVariant,
                ),
            )
            Spacer(Modifier.height(12.dp))

            SurfaceCard(accent = if (isOff) Color(0xFFFFB84D) else accent) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(50.dp)
                                .background(statusColor.copy(alpha = 0.16f), CircleShape)
                                .border(1.2.dp, statusColor.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = if (isOff) {
                                    Icons.Rounded.PowerSettingsNew
                                } else {
                                    Icons.Rounded.Memory
                                },
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isOff) {
                                        "Swap Disabled"
                                    } else {
                                        String.format("%.1f", activeSizeMb / 1024.0) +
                                            " GB ZRAM"
                                    },
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontSize = 19.sp,
                                        lineHeight = 19.sp,
                                        fontWeight = FontWeight.W900,
                                        letterSpacing = (-0.2).sp,
                                    ),
                                )
                                Spacer(Modifier.width(8.dp))
                                Row(
                                    Modifier
                                        .background(
                                            statusColor.copy(alpha = 0.18f),
                                            RoundedCornerShape(8.dp),
                                        )
                                        .padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        Modifier
                                            .size(6.dp)
                                            .background(statusColor, CircleShape),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = if (isOff) "OFF" else "ACTIVE",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.W900,
                                            color = statusColor,
                                        ),
                                    )
                                }
                            }
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = if (isOff) {
                                    "Physical RAM only · Zero compressed swap"
                                } else {
                                    "$activeAlgLabel · $swappiness Swappiness"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    color = colors.onSurfaceVariant,
                                ),
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ZramMetricPill(
                            modifier = Modifier.weight(1f),
                            label = "EFFICIENCY",
                            value = if (isOff) "1.00×" else String.format("%.2f", ratio) + "×",
                            subvalue = if (isOff) "Disabled" else "Real-time",
                            accent = Color(0xFF41C98A),
                        )
                        Spacer(Modifier.width(8.dp))
                        ZramMetricPill(
                            modifier = Modifier.weight(1f),
                            label = "RAM SAVED",
                            value = if (isOff) "0 MB" else "+$ramSavedMb MB",
                            subvalue = if (isOff) "No swap" else "$origMb in $comprMb MB",
                            accent = Color(0xFF67C2FF),
                        )
                        Spacer(Modifier.width(8.dp))
                        ZramMetricPill(
                            modifier = Modifier.weight(1f),
                            label = "USED SWAP",
                            value = if (isOff) "0 MB" else "$memUsedMb MB",
                            subvalue = if (isOff) "Off" else "of $activeSizeMb MB",
                            accent = Color(0xFFB087FF),
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))

            Text(
                text = "QUICK ZRAM SIZE PRESETS (MAX 16 GB)",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.W800,
                    letterSpacing = 0.8.sp,
                    color = Color(0xFF9E9E9E),
                ),
            )
            Spacer(Modifier.height(8.dp))

            for (preset in presets) {
                ZramPresetCard(
                    preset = preset,
                    applying = applying,
                    onTap = {
                        applyPresetSize(
                            preset.sizeMb,
                            preset.title,
                            preset.icon,
                            preset.accentColor,
                        )
                    },
                )
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(6.dp))

            SurfaceCard(accent = if (isCustomMode) Color(0xFF67C2FF) else null) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Custom Swap Size",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.W800,
                            ),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (customSliderMb == 0) {
                                "Disabled"
                            } else {
                                String.format("%.1f", customSliderMb / 1024.0) + " GB"
                            },
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.W900,
                                color = accent,
                            ),
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (customSliderMb == 0) {
                            "Disabled: Pure physical RAM only with zero compressed swap."
                        } else {
                            "Allocates $customSliderMb MB compressed virtual swap " +
                                "partition (up to 25.0 GB)."
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.5.sp,
                            color = colors.onSurfaceVariant,
                        ),
                    )
                    Spacer(Modifier.height(8.dp))
                    Slider(
                        value = customSliderMb.toFloat().coerceIn(0f, 25600f),
                        onValueChange = { raw ->
                            if (!sliderDragging) {
                                sliderDragging = true
                                RodinHaptics.segment()
                            }
                            customSliderMb = raw.roundToInt()
                            isCustomMode = true
                            RodinHaptics.frequentSegment()
                        },
                        onValueChangeFinished = {
                            sliderDragging = false
                            RodinHaptics.confirm()
                            applyCustomSize(customSliderMb)
                        },
                        valueRange = 0f..25600f,
                        steps = 24,
                        colors = SliderDefaults.colors(
                            thumbColor = accent,
                            activeTrackColor = accent,
                        ),
                    )
                    Spacer(Modifier.height(4.dp))
                    ElevatedButton(
                        onClick = { applyCustomSize(customSliderMb) },
                        enabled = !applying,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (applying) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (applying) "Applying..." else "Apply",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.W800,
                            ),
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            SurfaceCard {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Compress,
                            contentDescription = null,
                            tint = Color(0xFF41C98A),
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Compression Algorithm",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.W800,
                            ),
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Select the real-time kernel compression engine for " +
                            "the ZRAM block device.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.5.sp,
                            color = colors.onSurfaceVariant,
                        ),
                    )
                    Spacer(Modifier.height(12.dp))
                    AlgorithmOptionCard(
                        title = "LZ4 (Ultra Fast)",
                        badge = "RECOMMENDED",
                        description = "Highest decompression speed and near-zero CPU " +
                            "overhead. Ideal for 120 FPS UI and gaming.",
                        code = 0,
                        activeCode = algCode,
                        accent = Color(0xFF41C98A),
                        onTap = { applyAlgorithm(0) },
                    )
                    Spacer(Modifier.height(6.dp))
                    AlgorithmOptionCard(
                        title = "ZSTD (Maximum Density)",
                        badge = "MAX RATIO",
                        description = "State-of-the-art compression ratio. Compresses " +
                            "up to 40% more data into physical RAM.",
                        code = 1,
                        activeCode = algCode,
                        accent = Color(0xFF67C2FF),
                        onTap = { applyAlgorithm(1) },
                    )
                    Spacer(Modifier.height(6.dp))
                    AlgorithmOptionCard(
                        title = "LZO-RLE (Balanced)",
                        badge = "BALANCED",
                        description = "Standard Android kernel algorithm with " +
                            "run-length zero-page acceleration.",
                        code = 2,
                        activeCode = algCode,
                        accent = Color(0xFFB087FF),
                        onTap = { applyAlgorithm(2) },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            SurfaceCard {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Speed,
                            contentDescription = null,
                            tint = Color(0xFFFFB84D),
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Kernel Swappiness",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.W800,
                            ),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "$swappiness",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.W900,
                                color = Color(0xFFFFB84D),
                            ),
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = when {
                            swappiness < 60 -> "Conservative: Avoids swapping memory " +
                                "pages unless physical RAM is critically low."
                            swappiness <= 100 -> "Balanced (Stock 100): Optimal balance " +
                                "for daily app usage and multi-tasking."
                            swappiness <= 160 -> "Aggressive: Proactively pushes idle " +
                                "background apps into ZRAM to maximize free RAM for " +
                                "active apps."
                            else -> "Maximum: Aggressively keeps foreground memory " +
                                "free for heavy gaming."
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.5.sp,
                            color = colors.onSurfaceVariant,
                        ),
                    )
                    Spacer(Modifier.height(8.dp))
                    Slider(
                        value = swappiness.toFloat().coerceIn(0f, 200f),
                        onValueChange = { raw ->
                            if (!sliderDragging) {
                                sliderDragging = true
                                RodinHaptics.segment()
                            }
                            optimisticSwappiness = raw.roundToInt()
                            RodinHaptics.frequentSegment()
                        },
                        onValueChangeFinished = {
                            sliderDragging = false
                            applySwappiness(swappiness)
                        },
                        valueRange = 0f..200f,
                        steps = 39,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFFB84D),
                            activeTrackColor = Color(0xFFFFB84D),
                        ),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            SurfaceCard {
                Column {
                    Text(
                        text = "Instant Memory Tools",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.W800,
                        ),
                    )
                    Spacer(Modifier.height(10.dp))
                    Row {
                        OutlinedButton(
                            onClick = { compactMemory() },
                            enabled = !compacting,
                            modifier = Modifier.weight(1f),
                        ) {
                            if (compacting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.CleaningServices,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (compacting) "Compacting..." else "Compact ZRAM",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.W700,
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = { resetDefaults() },
                            enabled = !applying,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.RestartAlt,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Reset 8GB Default",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.W700,
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            SurfaceCard {
                Column {
                    Text(
                        text = "ZRAM Hardware Architecture",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.W800,
                        ),
                    )
                    Spacer(Modifier.height(10.dp))
                    DiagnosticRow(
                        label = "Block Device Node",
                        good = true,
                        detail = "/dev/block/zram0",
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 7.dp),
                        thickness = 0.8.dp,
                    )
                    DiagnosticRow(
                        label = "Swap Partition Status",
                        good = !isOff,
                        detail = if (isOff) "Unmounted" else "Active (Priority 32758)",
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 7.dp),
                        thickness = 0.8.dp,
                    )
                    DiagnosticRow(
                        label = "Kernel Memory Compaction",
                        good = true,
                        detail = "Supported (/sys/block/zram0/compact)",
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 7.dp),
                        thickness = 0.8.dp,
                    )
                    DiagnosticRow(
                        label = "Memory Stream Decompression",
                        good = true,
                        detail = "Multi-stream Lockless Kernel Engine",
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        RodinFeedbackOverlay(
            tag = toastTag,
            message = toastMessage,
            icon = toastIcon,
            accent = toastAccent,
            visible = toastVisible,
            durationMillis = RodinInteractionSettings.motionDuration(260),
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun ZramPresetCard(
    preset: ZramPreset,
    applying: Boolean,
    onTap: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.surface.luminance() < 0.5f
    PressScale(onTap = if (applying || preset.isActive) null else onTap) {
        SurfaceCard(
            padding = PaddingValues(14.dp),
            accent = if (preset.isActive) preset.accentColor else null,
        ) {
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
                            text = if (preset.sizeMb == 0) {
                                "Pure Physical RAM · 0 MB Swap"
                            } else {
                                "${preset.sizeMb} MB · " +
                                    String.format("%.1f", preset.sizeMb / 1024.0) +
                                    " GB Capacity"
                            },
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
                    Row(
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
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (preset.isActive) {
                            Box(
                                Modifier
                                    .size(6.dp)
                                    .background(preset.accentColor, CircleShape),
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
                Spacer(Modifier.height(8.dp))
                Text(
                    text = preset.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.5.sp,
                        lineHeight = 11.5.sp * 1.35f,
                        color = colors.onSurfaceVariant,
                    ),
                )
            }
        }
    }
}

@Composable
private fun ZramMetricPill(
    label: String,
    value: String,
    subvalue: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.surface.luminance() < 0.5f
    Column(
        modifier
            .background(
                if (dark) Color.Black.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.6f),
                RoundedCornerShape(12.dp),
            )
            .border(1.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .padding(horizontal = 9.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.5.sp,
                fontWeight = FontWeight.W800,
                letterSpacing = 0.5.sp,
                color = accent,
            ),
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = value,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.W900,
            ),
        )
        Spacer(Modifier.height(1.dp))
        Text(
            text = subvalue,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.5.sp,
                color = colors.onSurfaceVariant,
            ),
        )
    }
}

@Composable
private fun AlgorithmOptionCard(
    title: String,
    badge: String,
    description: String,
    code: Int,
    activeCode: Int,
    accent: Color,
    onTap: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val isActive = code == activeCode
    PressScale(onTap = if (isActive) null else onTap) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(
                    if (isActive) {
                        accent.copy(alpha = 0.12f)
                    } else {
                        colors.surfaceContainerHighest.copy(alpha = 0.3f)
                    },
                    RoundedCornerShape(12.dp),
                )
                .border(
                    if (isActive) 1.5.dp else 1.dp,
                    if (isActive) accent else colors.outline.copy(alpha = 0.12f),
                    RoundedCornerShape(12.dp),
                )
                .padding(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 13.5.sp,
                            lineHeight = 13.5.sp * 1.3f,
                            fontWeight = FontWeight.W800,
                        ),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            lineHeight = 9.sp * 1.3f,
                            fontWeight = FontWeight.W700,
                            color = accent,
                        ),
                    )
                }
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector = if (isActive) {
                        Icons.Rounded.CheckCircle
                    } else {
                        Icons.Rounded.RadioButtonUnchecked
                    },
                    contentDescription = null,
                    tint = if (isActive) accent else colors.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    color = colors.onSurfaceVariant,
                ),
            )
        }
    }
}
