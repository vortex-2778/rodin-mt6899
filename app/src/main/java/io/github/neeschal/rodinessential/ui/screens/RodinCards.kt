package io.github.neeschal.rodinessential.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.neeschal.rodinessential.backend.RodinBackendSnapshot
import io.github.neeschal.rodinessential.ui.components.HeroTilePlaceholder
import io.github.neeschal.rodinessential.ui.components.IconTile
import io.github.neeschal.rodinessential.ui.components.PressScale
import io.github.neeschal.rodinessential.ui.components.SurfaceCard
import io.github.neeschal.rodinessential.ui.components.rodinShadow
import io.github.neeschal.rodinessential.ui.theme.LocalRodinAppearance
import io.github.neeschal.rodinessential.ui.theme.RodinSurfaceLines

/** The telemetry helpers every dashboard shares, ported verbatim. */
fun rodinOnlineCoreCount(snapshot: RodinBackendSnapshot): Int {
    var count = 0
    for (cpu in 0 until 8) {
        if (snapshot.cpuOnline(cpu)) {
            count += 1
        }
    }
    return count
}

fun rodinPerformanceLabel(profile: Int): String = when (profile) {
    0 -> "Stock Balanced"
    1 -> "Gaming Dynamic"
    2 -> "Battery Saver"
    3 -> "Extreme Beast"
    else -> "Stock Balanced"
}

fun rodinTouchLabel(profile: Int): String = when (profile) {
    1 -> "240 Hz"
    2 -> "480 Hz"
    3 -> "Super Touch"
    else -> "OEM Control"
}

private fun rodinDisplayMode(snapshot: RodinBackendSnapshot): String = when (snapshot.displayColor) {
    0 -> "Original PRO"
    1 -> "Vivid"
    2 -> "Saturated"
    else -> if (snapshot.ready) "OEM Control" else "—"
}

private fun rodinProfileAccent(profile: Int): Color = when (profile) {
    2 -> Color(0xFF35C997)
    1 -> Color(0xFFFFB84D)
    3 -> Color(0xFFFF5252)
    else -> Color(0xFF4EA8DE)
}

private fun rodinCoreColor(snapshot: RodinBackendSnapshot, cpu: Int): Color {
    if (!snapshot.cpuOnline(cpu)) return Color(0xFF252A31)
    if (cpu == 7) return Color(0xFFFF8E3C)
    if (cpu >= 4) return Color(0xFFA066FF)
    return Color(0xFF4EA8DE)
}

/**
 * The full DEVICE PULSE board: one glance at profile, cores, display, touch
 * and power. Reads the same daemon cache the Dart builder subscribed to.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LiveDashboardHero(modifier: Modifier = Modifier) {
    val snapshot = rememberRodinSnapshot()
    val colors = MaterialTheme.colorScheme
    val ready = snapshot.controlsAvailable
    val onlineCores = rodinOnlineCoreCount(snapshot)
    val activePerf = if (snapshot.performanceProfile >= 0) snapshot.performanceProfile else 0
    val profile = rodinPerformanceLabel(activePerf)
    val profileAccent = rodinProfileAccent(activePerf)
    val battery = if (snapshot.batteryCapacity >= 0) "${snapshot.batteryCapacity}%" else "—"
    val temp = snapshot.batteryTempC?.let { String.format("%.1f", it) + "°C" } ?: "—"
    val touch = rodinTouchLabel(snapshot.touchState)
    val appearance = LocalRodinAppearance.current
    val density = LocalDensity.current

    Box(modifier.fillMaxWidth()) {
        if (appearance.heroGlow) {
            Box(
                Modifier
                    .matchParentSize()
                    .padding(4.dp)
                    .rodinShadow(
                        color = profileAccent.copy(alpha = 0.14f),
                        blurRadius = 28f,
                        spreadRadius = -2f,
                        cornerRadius = with(density) { (appearance.cardRadius + 4).dp.toPx() },
                    ),
            )
        }
        SurfaceCard(
            padding = PaddingValues(start = 18.dp, top = 16.dp, end = 16.dp, bottom = 16.dp),
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "DEVICE PULSE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.2.sp,
                            fontWeight = FontWeight.W800,
                            letterSpacing = 1.25.sp,
                            color = colors.onSurfaceVariant,
                        ),
                    )
                    Spacer(Modifier.weight(1f))
                    val badgeColor = if (ready) Color(0xFF41C98A) else colors.onSurfaceVariant
                    Row(
                        Modifier
                            .background(badgeColor.copy(alpha = 0.12f), RoundedCornerShape(99.dp))
                            .border(1.dp, badgeColor.copy(alpha = 0.28f), RoundedCornerShape(99.dp))
                            .padding(horizontal = 8.dp, vertical = 3.5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(6.dp)
                                .then(
                                    if (ready) {
                                        Modifier.rodinShadow(
                                            color = Color(0xFF41C98A).copy(alpha = 0.7f),
                                            blurRadius = 6f,
                                            cornerRadius = with(density) { 3.dp.toPx() },
                                        )
                                    } else {
                                        Modifier
                                    },
                                )
                                .background(badgeColor, RoundedCornerShape(99.dp)),
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = snapshot.serviceStatusBadgeLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.W800,
                                letterSpacing = 0.6.sp,
                                color = badgeColor,
                            ),
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = profile,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 30.sp,
                        lineHeight = 30.sp * 0.98f,
                        fontWeight = FontWeight.W900,
                        letterSpacing = (-0.8).sp,
                    ),
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = null,
                        tint = profileAccent,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Active performance profile",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 11.4.sp,
                            fontWeight = FontWeight.W600,
                            color = colors.onSurfaceVariant,
                        ),
                    )
                }
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            colors.surfaceContainer.copy(alpha = 0.35f),
                            RoundedCornerShape(12.dp),
                        )
                        .border(
                            1.dp,
                            colors.outline.copy(alpha = 0.20f),
                            RoundedCornerShape(12.dp),
                        )
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "CORES",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.W800,
                            letterSpacing = 0.8.sp,
                            color = colors.onSurfaceVariant,
                        ),
                    )
                    Spacer(Modifier.weight(1f))
                    when (appearance.coreVisualizerStyle) {
                        1 -> {
                            for (cpu in 0 until 8) {
                                val color = rodinCoreColor(snapshot, cpu)
                                Box(
                                    Modifier
                                        .padding(horizontal = 2.5.dp)
                                        .size(9.dp)
                                        .then(
                                            if (snapshot.cpuOnline(cpu)) {
                                                Modifier.rodinShadow(
                                                    color = color.copy(alpha = 0.65f),
                                                    blurRadius = 5f,
                                                    cornerRadius = with(density) { 4.5.dp.toPx() },
                                                )
                                            } else {
                                                Modifier
                                            },
                                        )
                                        .background(color, RoundedCornerShape(99.dp)),
                                )
                            }
                        }
                        2 -> {
                            Row(
                                Modifier.clip(RoundedCornerShape(99.dp)),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                for (cpu in 0 until 8) {
                                    Box(
                                        Modifier
                                            .padding(horizontal = 0.5.dp)
                                            .size(width = 13.dp, height = 10.dp)
                                            .background(rodinCoreColor(snapshot, cpu)),
                                    )
                                }
                            }
                        }
                        else -> {
                            for (cpu in 0 until 8) {
                                val color = rodinCoreColor(snapshot, cpu)
                                Box(
                                    Modifier
                                        .padding(horizontal = 2.5.dp)
                                        .size(width = 8.dp, height = 14.dp)
                                        .then(
                                            if (snapshot.cpuOnline(cpu)) {
                                                Modifier.rodinShadow(
                                                    color = color.copy(alpha = 0.45f),
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
                Spacer(Modifier.height(11.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    PulseChip(
                        icon = Icons.Rounded.Memory,
                        label = if (ready) "$onlineCores / 8 online" else "CPU —",
                        accent = Color(0xFF67C2FF),
                    )
                    PulseChip(
                        icon = Icons.Rounded.Palette,
                        label = rodinDisplayMode(snapshot),
                        accent = Color(0xFFB087FF),
                    )
                    PulseChip(
                        icon = Icons.Rounded.TouchApp,
                        label = "Touch $touch",
                        accent = Color(0xFF41C98A),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(colors.outline.copy(alpha = 0.15f)),
                )
                Spacer(Modifier.height(11.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.BatteryStd,
                        contentDescription = null,
                        tint = Color(0xFF41C98A),
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = battery,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.W800,
                        ),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "\u2022",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            color = colors.onSurfaceVariant,
                        ),
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Rounded.Thermostat,
                        contentDescription = null,
                        tint = Color(0xFF59BCFF),
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = temp,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.W700,
                        ),
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = snapshot.serviceStatusLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.W700,
                            color = profileAccent,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun PulseChip(icon: ImageVector, label: String, accent: Color) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .background(colors.surfaceContainer.copy(alpha = 0.34f), RoundedCornerShape(99.dp))
            .border(1.dp, RodinSurfaceLines.inset(colors), RoundedCornerShape(99.dp))
            .padding(horizontal = 9.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.5.sp,
                fontWeight = FontWeight.W700,
            ),
        )
    }
}

/** The tinted hero banner every detail page opens with. */
@Composable
fun HeroCard(
    icon: ImageVector,
    accent: Color,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.surface.luminance() < 0.5f
    val density = LocalDensity.current
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier
            .fillMaxWidth()
            .rodinShadow(
                color = accent.copy(alpha = 0.10f),
                blurRadius = 24f,
                offsetY = 10f,
                spreadRadius = -7f,
                cornerRadius = with(density) { 24.dp.toPx() },
            )
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        accent.copy(alpha = if (dark) 0.20f else 0.12f)
                            .compositeOver(colors.surface),
                        colors.surface,
                        colors.secondary.copy(alpha = if (dark) 0.05f else 0.03f)
                            .compositeOver(colors.surface),
                    ),
                    start = Offset.Zero,
                    end = Offset.Infinite,
                ),
                shape,
            )
            .border(1.dp, accent.copy(alpha = 0.23f), shape)
            .padding(start = 15.dp, top = 15.dp, end = 15.dp, bottom = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leading != null) {
                leading()
            } else {
                IconTile(accent = accent, size = 54.dp, icon = icon)
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 18.5.sp,
                        fontWeight = FontWeight.W800,
                        letterSpacing = (-0.25).sp,
                    ),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.3.sp,
                        lineHeight = 12.3.sp * 1.25f,
                        color = colors.onSurfaceVariant,
                    ),
                )
            }
        }
    }
}

/** The GitHub mark, traced from the original painter, DPI independent. */
@Composable
fun GithubIcon(color: Color, modifier: Modifier = Modifier, size: Dp = 23.dp) {
    Canvas(modifier.size(size)) {
        val s = this.size.width / 16f
        scale(scaleX = s, scaleY = s, pivot = Offset.Zero) {
            val path = Path().apply {
                moveTo(8f, 0f)
                cubicTo(3.58f, 0f, 0f, 3.58f, 0f, 8f)
                cubicTo(0f, 11.54f, 2.29f, 14.53f, 5.47f, 15.59f)
                cubicTo(5.87f, 15.66f, 6.02f, 15.42f, 6.02f, 15.21f)
                cubicTo(6.02f, 15.02f, 6.01f, 14.39f, 6.01f, 13.72f)
                cubicTo(4f, 14.09f, 3.48f, 13.23f, 3.32f, 12.78f)
                cubicTo(3.23f, 12.55f, 2.84f, 11.84f, 2.5f, 11.65f)
                cubicTo(2.22f, 11.5f, 1.82f, 11.13f, 2.49f, 11.12f)
                cubicTo(3.12f, 11.11f, 3.57f, 11.7f, 3.72f, 11.94f)
                cubicTo(4.44f, 13.15f, 5.59f, 12.81f, 6.05f, 12.6f)
                cubicTo(6.12f, 12.08f, 6.33f, 11.73f, 6.56f, 11.53f)
                cubicTo(4.78f, 11.33f, 2.92f, 10.64f, 2.92f, 7.58f)
                cubicTo(2.92f, 6.71f, 3.23f, 5.99f, 3.74f, 5.43f)
                cubicTo(3.66f, 5.23f, 3.38f, 4.41f, 3.82f, 3.31f)
                cubicTo(3.82f, 3.31f, 4.49f, 3.1f, 6.02f, 4.13f)
                cubicTo(6.66f, 3.95f, 7.34f, 3.86f, 8.02f, 3.86f)
                cubicTo(8.7f, 3.86f, 9.38f, 3.95f, 10.02f, 4.13f)
                cubicTo(11.55f, 3.1f, 12.22f, 3.31f, 12.22f, 3.31f)
                cubicTo(12.66f, 4.41f, 12.38f, 5.23f, 12.3f, 5.43f)
                cubicTo(12.81f, 5.99f, 13.12f, 6.71f, 13.12f, 7.58f)
                cubicTo(13.12f, 10.65f, 11.25f, 11.33f, 9.47f, 11.53f)
                cubicTo(9.76f, 11.78f, 10.01f, 12.26f, 10.01f, 13.01f)
                cubicTo(10.01f, 14.08f, 10f, 14.94f, 10f, 15.21f)
                cubicTo(10f, 15.42f, 10.15f, 15.67f, 10.55f, 15.59f)
                cubicTo(13.71f, 14.53f, 16f, 11.54f, 16f, 8f)
                cubicTo(16f, 3.58f, 12.42f, 0f, 8f, 0f)
                close()
            }
            drawPath(path, color)
        }
    }
}

/** A tappable feature surface with an accent arrow seal. */
@Composable
fun FeatureCard(
    title: String,
    subtitle: String,
    accent: Color,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    customIcon: (@Composable () -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    PressScale(onTap = onTap, modifier = modifier) {
        SurfaceCard(
            padding = PaddingValues(start = 14.dp, top = 12.dp, end = 13.dp, bottom = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(accent = accent, size = 46.dp, icon = icon, content = customIcon)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.W700,
                            letterSpacing = (-0.15).sp,
                        ),
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            color = colors.onSurfaceVariant,
                        ),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .size(31.dp)
                        .background(accent.copy(alpha = 0.075f), RoundedCornerShape(11.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowForwardIos,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(13.dp),
                    )
                }
            }
        }
    }
}

/** The hub index row: tile, copy and a quiet chevron. */
@Composable
fun HubRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    PressScale(onTap = onTap, modifier = modifier) {
        Box(Modifier.padding(horizontal = 4.dp, vertical = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(accent = accent, size = 42.dp, icon = icon)
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.W700,
                        ),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.5.sp,
                            lineHeight = 11.5.sp * 1.35f,
                            color = colors.onSurfaceVariant,
                        ),
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = colors.onSurfaceVariant.copy(alpha = 0.72f),
                    modifier = Modifier.size(19.dp),
                )
            }
        }
    }
}
