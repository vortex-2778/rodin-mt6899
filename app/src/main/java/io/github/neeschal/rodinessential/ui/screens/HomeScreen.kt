package io.github.neeschal.rodinessential.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Balance
import androidx.compose.material.icons.rounded.Battery5Bar
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.defaultMinSize
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.neeschal.rodinessential.backend.RodinBackend
import io.github.neeschal.rodinessential.backend.RodinBackendSnapshot
import io.github.neeschal.rodinessential.ui.RodinScreen
import io.github.neeschal.rodinessential.ui.components.IconTile
import io.github.neeschal.rodinessential.ui.components.PressScale
import io.github.neeschal.rodinessential.ui.components.SectionLabel
import io.github.neeschal.rodinessential.ui.components.SurfaceCard
import io.github.neeschal.rodinessential.ui.components.rodinShadow
import io.github.neeschal.rodinessential.ui.motion.rodinAnimationsDisabled
import io.github.neeschal.rodinessential.ui.shell.RodinInteractionSettings
import io.github.neeschal.rodinessential.ui.theme.LocalRodinAppearance
import io.github.neeschal.rodinessential.ui.theme.RodinSurfaceLines

/**
 * The home tab: the pulse hero, live hardware, quick controls, the control
 * centre portal and the shortcut panel, in the original order.
 */
@Composable
fun HomeScreen(
    onOpen: (RodinScreen) -> Unit,
    onHubs: () -> Unit,
    onSupport: () -> Unit,
) {
    RodinScrollPage(
        header = RodinPageHeader.Home(
            title = "Rodin Essential",
            subtitle = "Precision you can feel.",
        ),
    ) {
        Spacer(Modifier.height(10.dp))
        HomePulseHero(onOpen = onOpen)
        Spacer(Modifier.height(14.dp))
        SectionLabel("Live hardware")
        Spacer(Modifier.height(8.dp))
        LiveOverviewGrid(onOpen = onOpen)
        Spacer(Modifier.height(14.dp))
        SectionLabel("Quick controls")
        Spacer(Modifier.height(8.dp))
        HomeQuickActions(onOpen = onOpen)
        Spacer(Modifier.height(14.dp))
        ControlCenterPortal(onTap = onHubs)
        Spacer(Modifier.height(14.dp))
        SectionLabel("Useful shortcuts")
        Spacer(Modifier.height(8.dp))
        HomeShortcutPanel(onOpen = onOpen, onSupport = onSupport)
        Spacer(Modifier.height(12.dp))
    }
}

/** The online badge shared by the home hero and the hubs hero. */
@Composable
fun HomeLiveBadge(snapshot: RodinBackendSnapshot, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val online = snapshot.controlsAvailable
    val accent = if (online) Color(0xFF41C98A) else colors.onSurfaceVariant
    Row(
        modifier
            .background(accent.copy(alpha = 0.10f), RoundedCornerShape(999.dp))
            .border(1.dp, accent.copy(alpha = 0.24f), RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(6.dp)
                .background(accent, RoundedCornerShape(999.dp)),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = snapshot.serviceStatusBadgeLabel,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.8.sp,
                fontWeight = FontWeight.W800,
                letterSpacing = 0.48.sp,
                color = accent,
            ),
        )
    }
}

@Composable
private fun HomePulseHero(onOpen: (RodinScreen) -> Unit) {
    val snapshot = rememberRodinSnapshot()
    val colors = MaterialTheme.colorScheme
    val dark = colors.surface.luminance() < 0.5f
    val appearance = LocalRodinAppearance.current
    val density = LocalDensity.current
    val reduced = rodinAnimationsDisabled()
    val activePerf = if (snapshot.performanceProfile >= 0) snapshot.performanceProfile else 0
    val profile = rodinPerformanceLabel(activePerf)
    val profileAccent = rodinProfileAccent(activePerf)
    val profileIcon = when (activePerf) {
        2 -> Icons.Rounded.Eco
        1 -> Icons.Rounded.SportsEsports
        3 -> Icons.Rounded.LocalFireDepartment
        else -> Icons.Rounded.Balance
    }
    val profileDescription = when (activePerf) {
        2 -> "Efficiency-first clocks and reduced power demand"
        1 -> "Unrestricted 260–1300 MHz GPU scaling with GED boost"
        3 -> "GPU locked at the 1300 MHz hardware ceiling"
        else -> "OEM-balanced performance and efficiency for everyday use"
    }
    val onlineCores = rodinOnlineCoreCount(snapshot)
    val battery = if (snapshot.batteryCapacity >= 0) "${snapshot.batteryCapacity}%" else "—"
    val temperature = snapshot.batteryTempC?.let { String.format("%.1f", it) + "°C" } ?: "—"
    val touchValue = when (snapshot.touchState) {
        1 -> "240 Hz"
        2 -> "480 Hz"
        3 -> "Super"
        else -> "OEM"
    }
    val touchDetail = when (snapshot.touchState) {
        1, 2 -> "Fixed target"
        3 -> "1000 / 500 Hz"
        else -> "ROM control"
    }
    val liveGpuFreq = if (RodinBackend.extendedValue(46) >= 260) RodinBackend.extendedValue(46) else 0
    val gpu = if (liveGpuFreq > 0) "$liveGpuFreq MHz" else "—"

    val arrival = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduced) {
            arrival.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    RodinInteractionSettings.motionDuration(420),
                    easing = RodinInteractionSettings.transitionCurve,
                ),
            )
        }
    }
    val p = arrival.value.coerceIn(0f, 1f)

    Box(
        Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = p
                translationY = (1f - p) * 12f
            },
    ) {
        Box(Modifier.fillMaxWidth()) {
            if (appearance.heroGlow) {
                Box(
                    Modifier
                        .matchParentSize()
                        .padding(4.dp)
                        .rodinShadow(
                            color = profileAccent.copy(alpha = 0.17f),
                            blurRadius = 31f,
                            spreadRadius = -4f,
                            cornerRadius = with(density) {
                                (appearance.cardRadius + 5).dp.toPx()
                            },
                        ),
                )
            }
            SurfaceCard(
                modifier = Modifier.fillMaxWidth(),
                padding = PaddingValues(0.dp),
                accent = profileAccent,
            ) {
                Box(
                    Modifier.clip(RoundedCornerShape(appearance.cardRadius.dp)),
                ) {
                    Box(
                        Modifier
                            .matchParentSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        profileAccent.copy(alpha = if (dark) 0.15f else 0.12f),
                                        colors.surface.copy(alpha = if (dark) 0.76f else 0.90f),
                                        Color(0xFFB087FF).copy(alpha = if (dark) 0.06f else 0.045f),
                                    ),
                                    start = androidx.compose.ui.geometry.Offset.Zero,
                                    end = androidx.compose.ui.geometry.Offset.Infinite,
                                    colorStops = arrayOf(0f, 0.58f, 1f),
                                ),
                            ),
                    )
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 48.dp, y = (-62).dp)
                            .size(150.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        profileAccent.copy(alpha = if (dark) 0.15f else 0.12f),
                                        profileAccent.copy(alpha = 0f),
                                    ),
                                ),
                            ),
                    )
                    Box(
                        Modifier
                            .align(Alignment.BottomStart)
                            .offset(x = (-38).dp, y = 64.dp)
                            .size(132.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        colors.secondary.copy(alpha = if (dark) 0.08f else 0.055f),
                                        colors.secondary.copy(alpha = 0f),
                                    ),
                                ),
                            ),
                    )
                    Column(
                        Modifier.padding(
                            start = 15.dp,
                            top = 13.dp,
                            end = 15.dp,
                            bottom = 13.dp,
                        ),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(7.dp)
                                    .rodinShadow(
                                        color = profileAccent.copy(alpha = 0.60f),
                                        blurRadius = 7f,
                                        cornerRadius = with(density) { 3.5.dp.toPx() },
                                    )
                                    .background(profileAccent, RoundedCornerShape(999.dp)),
                            )
                            Spacer(Modifier.width(7.dp))
                            Text(
                                text = "DEVICE PULSE",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.4.sp,
                                    fontWeight = FontWeight.W800,
                                    letterSpacing = 1.05.sp,
                                    color = colors.onSurfaceVariant,
                                ),
                            )
                            Spacer(Modifier.width(8.dp))
                            Box(Modifier.widthIn(max = 120.dp)) {
                                HomeLiveBadge(snapshot)
                            }
                        }
                        Spacer(Modifier.height(11.dp))
                        Row(verticalAlignment = Alignment.Top) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = profile,
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontSize = 27.sp,
                                        lineHeight = 27.sp,
                                        fontWeight = FontWeight.W900,
                                        letterSpacing = (-0.72).sp,
                                    ),
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = profileDescription,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        lineHeight = 11.sp * 1.28f,
                                        fontWeight = FontWeight.W500,
                                        color = colors.onSurfaceVariant,
                                    ),
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Box(
                                Modifier
                                    .size(49.dp)
                                    .background(
                                        profileAccent.copy(alpha = 0.12f),
                                        RoundedCornerShape(17.dp),
                                    )
                                    .border(
                                        1.dp,
                                        profileAccent.copy(alpha = 0.22f),
                                        RoundedCornerShape(17.dp),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = profileIcon,
                                    contentDescription = null,
                                    tint = profileAccent,
                                    modifier = Modifier.size(25.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        HomeCoreVisualizer(
                            snapshot = snapshot,
                            onlineCores = onlineCores,
                            style = appearance.coreVisualizerStyle,
                        )
                        Spacer(Modifier.height(10.dp))
                        Row {
                            Box(Modifier.weight(1f)) {
                                HomeHeroMetric(
                                    label = "BATTERY",
                                    value = battery,
                                    detail = temperature,
                                    icon = Icons.Rounded.Battery5Bar,
                                    accent = Color(0xFF41C98A),
                                    onTap = { onOpen(RodinScreen.charging) },
                                )
                            }
                            Spacer(Modifier.width(7.dp))
                            Box(Modifier.weight(1f)) {
                                HomeHeroMetric(
                                    label = "MALI GPU",
                                    value = gpu,
                                    detail = if (liveGpuFreq > 0) "Live clock" else "Waiting for GPU",
                                    icon = Icons.Rounded.SportsEsports,
                                    accent = Color(0xFFFF5252),
                                    onTap = { onOpen(RodinScreen.maliGpu) },
                                )
                            }
                            Spacer(Modifier.width(7.dp))
                            Box(Modifier.weight(1f)) {
                                HomeHeroMetric(
                                    label = "TOUCH",
                                    value = touchValue,
                                    detail = touchDetail,
                                    icon = Icons.Rounded.TouchApp,
                                    accent = Color(0xFF41C98A),
                                    onTap = { onOpen(RodinScreen.touchBoost) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeroMetric(
    label: String,
    value: String,
    detail: String,
    icon: ImageVector,
    accent: Color,
    onTap: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    PressScale(onTap = onTap) {
        Row(
            Modifier
                .defaultMinSize(minHeight = 66.dp)
                .fillMaxWidth()
                .background(colors.surface.copy(alpha = 0.52f), RoundedCornerShape(13.dp))
                .border(
                    0.85.dp,
                    RodinSurfaceLines.inset(colors),
                    RoundedCornerShape(13.dp),
                )
                .padding(horizontal = 8.dp, vertical = 7.dp),
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
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 8.5.sp,
                        lineHeight = 8.5.sp * 1.2f,
                        fontWeight = FontWeight.W800,
                        letterSpacing = 0.40.sp,
                        color = colors.onSurfaceVariant,
                    ),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = value,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        lineHeight = 14.sp * 1.2f,
                        fontWeight = FontWeight.W900,
                    ),
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 10.sp,
                        lineHeight = 10.sp * 1.2f,
                        color = colors.onSurfaceVariant,
                    ),
                )
            }
        }
    }
}

@Composable
private fun HomeCoreVisualizer(
    snapshot: RodinBackendSnapshot,
    onlineCores: Int,
    style: Int,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier
            .fillMaxWidth()
            .background(colors.surface.copy(alpha = 0.45f), RoundedCornerShape(13.dp))
            .border(0.85.dp, RodinSurfaceLines.inset(colors), RoundedCornerShape(13.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        BoxWithConstraints {
            val narrow = maxWidth < 280.dp || LocalDensity.current.fontScale * 10f > 12f
            if (narrow) {
                Column {
                    CoreHeading(snapshot = snapshot, onlineCores = onlineCores)
                    Spacer(Modifier.height(8.dp))
                    CoreIndicators(snapshot = snapshot, style = style)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) {
                        CoreHeading(snapshot = snapshot, onlineCores = onlineCores)
                    }
                    CoreIndicators(snapshot = snapshot, style = style)
                }
            }
        }
    }
}

@Composable
private fun CoreHeading(snapshot: RodinBackendSnapshot, onlineCores: Int) {
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "CPU",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.6.sp,
                fontWeight = FontWeight.W800,
                letterSpacing = 0.72.sp,
                color = colors.onSurfaceVariant,
            ),
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = if (snapshot.ready) "$onlineCores / 8 online" else "Waiting for system",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.3.sp,
                fontWeight = FontWeight.W700,
            ),
        )
    }
}

@Composable
private fun CoreIndicators(snapshot: RodinBackendSnapshot, style: Int) {
    val density = LocalDensity.current
    val indicators: @Composable () -> Unit = {
        Row {
            for (cpu in 0 until 8) {
                val color = rodinCoreColor(snapshot, cpu)
                val online = snapshot.cpuOnline(cpu)
                when (style) {
                    1 -> Box(
                        Modifier
                            .padding(horizontal = 2.4.dp)
                            .size(9.dp)
                            .then(
                                if (online) {
                                    Modifier.rodinShadow(
                                        color = color.copy(alpha = 0.55f),
                                        blurRadius = 5f,
                                        cornerRadius = with(density) { 4.5.dp.toPx() },
                                    )
                                } else {
                                    Modifier
                                },
                            )
                            .background(color, RoundedCornerShape(999.dp)),
                    )
                    2 -> Box(
                        Modifier
                            .padding(horizontal = 0.8.dp)
                            .size(width = 12.dp, height = 9.dp)
                            .background(color),
                    )
                    else -> Box(
                        Modifier
                            .padding(horizontal = 2.3.dp)
                            .size(width = 8.dp, height = 14.dp)
                            .then(
                                if (online) {
                                    Modifier.rodinShadow(
                                        color = color.copy(alpha = 0.38f),
                                        blurRadius = 4f,
                                        cornerRadius = with(density) { 3.dp.toPx() },
                                    )
                                } else {
                                    Modifier
                                },
                            )
                            .background(color, RoundedCornerShape(3.dp)),
                    )
                }
            }
        }
    }
    if (style == 2) {
        Box(Modifier.clip(RoundedCornerShape(99.dp))) {
            indicators()
        }
    } else {
        indicators()
    }
}

@Composable
private fun HomeQuickActions(onOpen: (RodinScreen) -> Unit) {
    val colors = MaterialTheme.colorScheme
    SurfaceCard(
        padding = PaddingValues(horizontal = 7.dp, vertical = 7.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                HomeQuickAction(
                    title = "Mali GPU",
                    subtitle = "Profiles",
                    icon = Icons.Rounded.SportsEsports,
                    accent = Color(0xFFFF5252),
                    onTap = { onOpen(RodinScreen.maliGpu) },
                )
            }
            Box(
                Modifier
                    .width(1.dp)
                    .height(42.dp)
                    .background(RodinSurfaceLines.divider(colors)),
            )
            Box(Modifier.weight(1f)) {
                HomeQuickAction(
                    title = "Touch",
                    subtitle = "Response",
                    icon = Icons.Rounded.TouchApp,
                    accent = Color(0xFF41C98A),
                    onTap = { onOpen(RodinScreen.touchBoost) },
                )
            }
            Box(
                Modifier
                    .width(1.dp)
                    .height(42.dp)
                    .background(RodinSurfaceLines.divider(colors)),
            )
            Box(Modifier.weight(1f)) {
                HomeQuickAction(
                    title = "CPU",
                    subtitle = "Cores & MHz",
                    icon = Icons.Rounded.Memory,
                    accent = Color(0xFF67C2FF),
                    onTap = { onOpen(RodinScreen.cpuControl) },
                )
            }
        }
    }
}

@Composable
private fun HomeQuickAction(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onTap: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    PressScale(onTap = onTap) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(61.dp)
                .padding(horizontal = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        ) {
            IconTile(accent = accent, size = 31.dp, icon = icon)
            Spacer(Modifier.width(7.dp))
            Column(Modifier.weight(1f, fill = false)) {
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 11.3.sp,
                        fontWeight = FontWeight.W800,
                    ),
                )
                Spacer(Modifier.height(1.dp))
                Text(
                    text = subtitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 8.8.sp,
                        color = colors.onSurfaceVariant,
                    ),
                )
            }
        }
    }
}
