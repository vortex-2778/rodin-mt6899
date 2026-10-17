package io.github.neeschal.rodinessential.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Battery5Bar
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.neeschal.rodinessential.backend.RodinBackend
import io.github.neeschal.rodinessential.backend.RodinBackendSnapshot
import io.github.neeschal.rodinessential.ui.RodinScreen
import io.github.neeschal.rodinessential.ui.components.HubHeadline
import io.github.neeschal.rodinessential.ui.components.PressScale
import io.github.neeschal.rodinessential.ui.components.SurfaceCard
import io.github.neeschal.rodinessential.ui.motion.LocalRodinMotionViewportInFlight
import io.github.neeschal.rodinessential.ui.motion.rodinAnimationsDisabled
import io.github.neeschal.rodinessential.ui.shell.RodinInteractionSettings
import io.github.neeschal.rodinessential.ui.theme.RodinSurfaceLines

/** The control hubs index: profile hero, three category cards and diagnostics. */
@Composable
fun HubsScreen(onOpen: (RodinScreen) -> Unit) {
    val snapshot = rememberRodinSnapshot()
    val battery = if (snapshot.batteryCapacity >= 0) "${snapshot.batteryCapacity}%" else "—"
    val onlineCores = rodinOnlineCoreCount(snapshot)
    val cores = if (snapshot.ready) "$onlineCores / 8" else "—"
    val liveGpuFreq = if (RodinBackend.extendedValue(46) >= 0) RodinBackend.extendedValue(46) else 0
    val rawMinFreq = if (RodinBackend.extendedValue(47) >= 0) RodinBackend.extendedValue(47) else 260
    val rawMaxFreq = if (RodinBackend.extendedValue(48) >= 0) RodinBackend.extendedValue(48) else 1300
    val rawUncap = RodinBackend.extendedValue(52)
    val activePerf = if (snapshot.performanceProfile >= 0) snapshot.performanceProfile else 0
    val isBeast = (rawUncap == 1 && rawMinFreq == 1300 && rawMaxFreq == 1300) || activePerf == 3
    val gpuLabel = when {
        liveGpuFreq >= 260 -> "$liveGpuFreq MHz"
        isBeast -> "1300 MHz"
        else -> "$rawMaxFreq MHz"
    }
    val displayMode = when (snapshot.displayColor) {
        0 -> "Original PRO"
        1 -> "Vivid"
        2 -> "Saturated"
        else -> "OEM Control"
    }
    val touchMode = rodinTouchLabel(snapshot.touchState)
    val zramDiskMb = if (RodinBackend.extendedValue(39) > 0) RodinBackend.extendedValue(39) else 8192
    val memory = String.format("%.1f", zramDiskMb / 1024.0) + " GB"
    val cpuSummary = if (snapshot.ready) {
        "$cores · Core topology and clock ranges"
    } else {
        "Core topology and clock ranges"
    }
    val chargingSummary = if (battery == "—") {
        "Charging controls and power statistics"
    } else {
        "$battery · Charging controls and power stats"
    }

    RodinScrollPage(
        header = RodinPageHeader.Home(
            title = "Control Hubs",
            subtitle = "Performance, display and system controls",
        ),
    ) {
        Spacer(Modifier.height(11.dp))
        HubReveal {
            HubProfileHero(
                profile = rodinPerformanceLabel(activePerf),
                snapshot = snapshot,
                battery = battery,
                cores = cores,
                gpu = gpuLabel,
                gpuAccent = Color(0xFFFF5252),
                onBattery = { onOpen(RodinScreen.charging) },
                onCpu = { onOpen(RodinScreen.cpuControl) },
                onGpu = { onOpen(RodinScreen.maliGpu) },
            )
        }
        Spacer(Modifier.height(12.dp))
        HubReveal(duration = 420) {
            HubCategoryCard(
                title = "Performance",
                subtitle = "Compute, graphics and system policy",
                icon = Icons.Rounded.Speed,
                accent = Color(0xFF67C2FF),
            ) {
                Column {
                    HubControlRow(
                        title = "CPU Core & Frequency",
                        subtitle = cpuSummary,
                        icon = Icons.Rounded.Memory,
                        accent = Color(0xFF67C2FF),
                        onTap = { onOpen(RodinScreen.cpuControl) },
                    )
                    HubControlDivider()
                    HubControlRow(
                        emphasized = true,
                        title = "Mali GPU & GED",
                        subtitle = "Clocks, boost and power policy",
                        value = gpuLabel,
                        icon = Icons.Rounded.SportsEsports,
                        accent = Color(0xFFFF5252),
                        onTap = { onOpen(RodinScreen.maliGpu) },
                    )
                    HubControlDivider()
                    HubControlRow(
                        title = "Advanced Configuration",
                        subtitle = "Governors, scheduler & device tuning",
                        icon = Icons.Rounded.Tune,
                        accent = Color(0xFFFFB84D),
                        onTap = { onOpen(RodinScreen.advancedConfiguration) },
                    )
                    HubControlDivider()
                    HubControlRow(
                        title = "Memory DVFS & UFS",
                        subtitle = "DRAM & storage ranges and exact locks",
                        icon = Icons.Rounded.Storage,
                        accent = Color(0xFFB087FF),
                        onTap = { onOpen(RodinScreen.subsystemClocks) },
                    )
                    HubControlDivider()
                    HubControlRow(
                        title = "Per-App Controls",
                        subtitle = "Set individual touch, refresh rate, CPU and GPU controls for each app.",
                        wrapSubtitle = true,
                        icon = Icons.Rounded.Apps,
                        accent = Color(0xFFB087FF),
                        onTap = { onOpen(RodinScreen.perAppControls) },
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        HubReveal(duration = 500) {
            HubCategoryCard(
                title = "Display & Input",
                subtitle = "Touch response, color and canvas",
                icon = Icons.Rounded.TouchApp,
                accent = Color(0xFFB087FF),
            ) {
                Column {
                    HubControlRow(
                        title = "Touch Response",
                        subtitle = "$touchMode · Sampling and instant boost",
                        icon = Icons.Rounded.Swipe,
                        accent = Color(0xFF41C98A),
                        onTap = { onOpen(RodinScreen.touchBoost) },
                    )
                    HubControlDivider()
                    HubControlRow(
                        title = "Display Studio",
                        subtitle = "$displayMode · Color and HDR tuning",
                        icon = Icons.Rounded.Palette,
                        accent = Color(0xFFB087FF),
                        onTap = { onOpen(RodinScreen.displayStudio) },
                    )
                    HubControlDivider()
                    HubControlRow(
                        title = "System Colors",
                        subtitle = "Material You palettes for Android & apps",
                        icon = Icons.Rounded.ColorLens,
                        accent = Color(0xFFB087FF),
                        onTap = { onOpen(RodinScreen.systemColors) },
                    )
                    HubControlDivider()
                    HubControlRow(
                        title = "Resolution",
                        subtitle = "1.5K · Canvas, density and refresh rate",
                        icon = Icons.Rounded.GridView,
                        accent = Color(0xFFFFBE63),
                        onTap = { onOpen(RodinScreen.resolution) },
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        HubReveal(duration = 580) {
            HubCategoryCard(
                title = "Power & Memory",
                subtitle = "Charging and memory management",
                icon = Icons.Rounded.BatterySaver,
                accent = Color(0xFF41C98A),
            ) {
                Column {
                    HubControlRow(
                        title = "Charging",
                        subtitle = chargingSummary,
                        icon = Icons.Rounded.BatteryChargingFull,
                        accent = Color(0xFF41C98A),
                        onTap = { onOpen(RodinScreen.charging) },
                    )
                    HubControlDivider()
                    HubControlRow(
                        title = "ZRAM & Swap Manager",
                        subtitle = "$memory · Compression and swap policy",
                        icon = Icons.Rounded.Storage,
                        accent = Color(0xFF67C2FF),
                        onTap = { onOpen(RodinScreen.zramSwap) },
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        HubReveal(duration = 640) {
            SurfaceCard(padding = PaddingValues(11.dp)) {
                HubControlRow(
                    title = "Diagnostics",
                    subtitle = "Sensors and hardware health",
                    value = snapshot.serviceStatusLabel,
                    icon = Icons.Rounded.MonitorHeart,
                    accent = Color(0xFF74E6C6),
                    onTap = { onOpen(RodinScreen.diagnostics) },
                )
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

/** A sliding page is already being revealed by the finger: never keep its
 * content transparent behind a muted first-visit arrival ticker. */
@Composable
private fun HubReveal(
    duration: Int = 340,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val inFlight = LocalRodinMotionViewportInFlight.current
    val reduced = rodinAnimationsDisabled()
    val moving = inFlight || reduced
    val progress = remember { Animatable(if (moving) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!moving) {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    RodinInteractionSettings.motionDuration(duration),
                    easing = RodinInteractionSettings.transitionCurve,
                ),
            )
        }
    }
    val p = progress.value.coerceIn(0f, 1f)
    Box(
        modifier.graphicsLayer {
            alpha = p
            translationY = (1f - p) * 10f
        },
    ) {
        content()
    }
}

@Composable
private fun HubProfileHero(
    profile: String,
    snapshot: RodinBackendSnapshot,
    battery: String,
    cores: String,
    gpu: String,
    gpuAccent: Color,
    onBattery: () -> Unit,
    onCpu: () -> Unit,
    onGpu: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    SurfaceCard(
        accent = colors.primary,
        padding = PaddingValues(16.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "HARDWARE CONTROLS",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.W800,
                        letterSpacing = 1.sp,
                        color = colors.onSurfaceVariant,
                    ),
                )
                Spacer(Modifier.width(10.dp))
                Box(Modifier.widthIn(max = 120.dp)) {
                    HomeLiveBadge(snapshot)
                }
            }
            Spacer(Modifier.height(10.dp))
            HubHeadline()
            Spacer(Modifier.height(16.dp))
            Row {
                Box(Modifier.weight(1f)) {
                    HubMetric(
                        label = "BATTERY",
                        value = battery,
                        icon = Icons.Rounded.Battery5Bar,
                        accent = Color(0xFF41C98A),
                        onTap = onBattery,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(1f)) {
                    HubMetric(
                        label = "CPU",
                        value = cores,
                        icon = Icons.Rounded.Memory,
                        accent = Color(0xFF67C2FF),
                        onTap = onCpu,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(1f)) {
                    HubMetric(
                        label = "GPU",
                        value = gpu,
                        icon = Icons.Rounded.SportsEsports,
                        accent = gpuAccent,
                        onTap = onGpu,
                    )
                }
            }
        }
    }
}

@Composable
private fun HubMetric(
    label: String,
    value: String,
    icon: ImageVector,
    accent: Color,
    onTap: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    PressScale(onTap = onTap) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(colors.surface.copy(alpha = 0.50f), RoundedCornerShape(13.dp))
                .border(
                    0.85.dp,
                    RodinSurfaceLines.inset(colors),
                    RoundedCornerShape(13.dp),
                )
                .padding(horizontal = 9.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(15.dp),
            )
            Spacer(Modifier.width(6.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = label,
                    maxLines = 1,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 8.sp,
                        lineHeight = 8.sp,
                        fontWeight = FontWeight.W800,
                        letterSpacing = 0.42.sp,
                        color = colors.onSurfaceVariant,
                    ),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = value,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 11.5.sp,
                        lineHeight = 11.5.sp,
                        fontWeight = FontWeight.W800,
                    ),
                )
            }
        }
    }
}

@Composable
private fun HubCategoryCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    content: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    SurfaceCard(
        padding = PaddingValues(11.dp),
        accent = accent,
    ) {
        Column {
            Row(
                Modifier.padding(start = 2.dp, top = 1.dp, end = 2.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(31.dp)
                        .background(accent.copy(alpha = 0.11f), RoundedCornerShape(11.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(17.dp),
                    )
                }
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.W800,
                            letterSpacing = (-0.22).sp,
                        ),
                    )
                    Spacer(Modifier.height(1.dp))
                    Text(
                        text = subtitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 10.5.sp,
                            color = colors.onSurfaceVariant,
                        ),
                    )
                }
            }
            content()
        }
    }
}

@Composable
private fun HubControlDivider() {
    val colors = MaterialTheme.colorScheme
    Box(Modifier.padding(start = 52.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(0.8.dp)
                .background(RodinSurfaceLines.divider(colors)),
        )
    }
}

@Composable
private fun HubControlRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onTap: () -> Unit,
    value: String? = null,
    emphasized: Boolean = false,
    wrapSubtitle: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.surface.luminance() < 0.5f
    val shape = RoundedCornerShape(15.dp)
    PressScale(onTap = onTap) {
        Row(
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 62.dp)
                .then(
                    if (emphasized) {
                        Modifier.background(
                            Brush.horizontalGradient(
                                listOf(
                                    accent.copy(alpha = if (dark) 0.095f else 0.060f),
                                    accent.copy(alpha = 0.012f),
                                ),
                            ),
                            shape,
                        )
                    } else {
                        Modifier
                    },
                )
                .then(
                    if (emphasized) {
                        Modifier.border(
                            0.7.dp,
                            accent.copy(alpha = if (dark) 0.16f else 0.12f),
                            shape,
                        )
                    } else {
                        Modifier
                    },
                )
                .padding(horizontal = 5.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                accent.copy(alpha = if (dark) 0.18f else 0.13f),
                                accent.copy(alpha = if (dark) 0.08f else 0.055f),
                            ),
                            start = Offset.Zero,
                            end = Offset.Infinite,
                        ),
                        RoundedCornerShape(14.dp),
                    )
                    .border(
                        0.75.dp,
                        accent.copy(alpha = if (dark) 0.24f else 0.18f),
                        RoundedCornerShape(14.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.W800,
                        letterSpacing = (-0.16).sp,
                    ),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    maxLines = if (wrapSubtitle) Int.MAX_VALUE else 1,
                    overflow = if (wrapSubtitle) TextOverflow.Visible else TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 10.4.sp,
                        lineHeight = 10.4.sp * 1.12f,
                        color = colors.onSurfaceVariant,
                    ),
                )
            }
            if (value != null) {
                Spacer(Modifier.width(7.dp))
                Box(
                    Modifier
                        .widthIn(max = 78.dp)
                        .background(
                            accent.copy(alpha = if (dark) 0.13f else 0.09f),
                            RoundedCornerShape(999.dp),
                        )
                        .padding(horizontal = 7.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = value,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.W800,
                            color = accent,
                        ),
                    )
                }
            }
            Spacer(Modifier.width(5.dp))
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = colors.onSurfaceVariant.copy(alpha = 0.65f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
