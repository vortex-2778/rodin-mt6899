package io.github.neeschal.rodinessential.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DisplaySettings
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.neeschal.rodinessential.backend.RodinBackend
import io.github.neeschal.rodinessential.ui.RodinScreen
import io.github.neeschal.rodinessential.ui.components.PressScale
import io.github.neeschal.rodinessential.ui.components.SurfaceCard
import io.github.neeschal.rodinessential.ui.components.rodinShadow
import io.github.neeschal.rodinessential.ui.theme.RodinSurfaceLines

/** The shortcut panel closes the home tab: three rows and quiet dividers. */
@Composable
fun HomeShortcutPanel(
    onOpen: (RodinScreen) -> Unit,
    onSupport: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme

    @Composable
    fun divider() {
        Box(Modifier.padding(start = 47.dp)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(0.8.dp)
                    .background(RodinSurfaceLines.divider(colors)),
            )
        }
    }

    SurfaceCard(
        padding = PaddingValues(horizontal = 8.dp, vertical = 5.dp),
    ) {
        Column {
            HomeShortcutRow(
                title = "Diagnostics",
                subtitle = "Live sensors and hardware health",
                icon = Icons.Rounded.MonitorHeart,
                accent = Color(0xFF74E6C6),
                onTap = { onOpen(RodinScreen.diagnostics) },
            )
            divider()
            HomeShortcutRow(
                title = "Resolution",
                subtitle = "Canvas size, density and refresh details",
                icon = Icons.Rounded.AspectRatio,
                accent = Color(0xFFFFBE63),
                onTap = { onOpen(RodinScreen.resolution) },
            )
            divider()
            HomeShortcutRow(
                title = "Community & Support",
                subtitle = "Project source, help and device community",
                icon = Icons.Outlined.Forum,
                accent = Color(0xFF67C2FF),
                onTap = onSupport,
            )
        }
    }
}

@Composable
private fun HomeShortcutRow(
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
                .padding(horizontal = 3.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(38.dp)
                    .background(accent.copy(alpha = 0.10f), RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(19.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.W800,
                    ),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 10.2.sp,
                        color = colors.onSurfaceVariant,
                    ),
                )
            }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = colors.onSurfaceVariant.copy(alpha = 0.65f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** The amber disclaimer, shared by home and support. */
@Composable
fun DisclaimerCard(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.surface.luminance() < 0.5f
    val amber = Color(0xFFFFB84D)
    val density = LocalDensity.current
    Box(
        modifier
            .fillMaxWidth()
            .rodinShadow(
                color = Color.Black.copy(alpha = if (dark) 0.25f else 0.04f),
                blurRadius = 16f,
                offsetY = 4f,
                cornerRadius = with(density) { 18.dp.toPx() },
            )
            .background(
                if (dark) {
                    Color(0xFF141416).copy(alpha = 0.85f)
                } else {
                    Color.White.copy(alpha = 0.85f)
                },
                RoundedCornerShape(18.dp),
            )
            .border(1.2.dp, amber.copy(alpha = 0.22f), RoundedCornerShape(18.dp))
            .padding(14.dp),
    ) {
        Column {
            Row {
                Box(
                    Modifier
                        .size(32.dp)
                        .background(amber.copy(alpha = 0.14f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = amber,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Kernel & Hardware Disclaimer",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.W800,
                            letterSpacing = 0.2.sp,
                        ),
                    )
                    Spacer(Modifier.height(1.dp))
                    Text(
                        text = "Direct native hardware parameter tuning",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.W600,
                            color = amber,
                        ),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Rodin Essential provides direct kernel hardware tuning, " +
                    "MediaTek Mali GPU frequency locks, independent CPU frequency and " +
                    "topology control, and AIDL display calibrations via native ROM system " +
                    "interfaces. GPU modes never change CPU settings. Gaming Dynamic and " +
                    "Extreme Beast intentionally override only the vendor GPU cooling cap; " +
                    "sustained high clocks can cause severe heat, battery drain, " +
                    "instability, or hardware damage.",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.2.sp,
                    lineHeight = 11.2.sp * 1.45f,
                    color = colors.onSurfaceVariant.copy(alpha = 0.9f),
                ),
            )
        }
    }
}

/** Four glanceable tiles over two rows, all live from the daemon cache. */
@Composable
fun LiveOverviewGrid(modifier: Modifier = Modifier) {
    val snapshot = rememberRodinSnapshot()
    val colors = MaterialTheme.colorScheme
    val onlineCores = rodinOnlineCoreCount(snapshot)

    val gpuMhz = RodinBackend.extendedValue(46)
    val gpuValue = if (snapshot.ready && gpuMhz >= 260) "$gpuMhz MHz" else "—"
    val gpuSubtitle = if (snapshot.ready) "Live graphics clock" else "Waiting for GPU"

    val coreValue = if (snapshot.ready) "$onlineCores / 8" else "—"
    val coreSubtitle = if (RodinBackend.extendedValue(34) == 1) "Manual control" else "Auto balance"

    val display = when (snapshot.displayColor) {
        0 -> "Original PRO"
        1 -> "Vivid"
        2 -> "Saturated"
        else -> "OEM"
    }
    val touch = rodinTouchLabel(snapshot.touchState)

    val zramDiskMb = if (RodinBackend.extendedValue(39) > 0) {
        RodinBackend.extendedValue(39)
    } else {
        8192
    }
    val zramUsedMb = if (RodinBackend.extendedValue(42) >= 0) {
        RodinBackend.extendedValue(42)
    } else {
        0
    }
    val zramAlg = when (RodinBackend.extendedValue(44)) {
        1 -> "ZSTD"
        2 -> "LZO-RLE"
        3 -> "LZO"
        else -> "LZ4"
    }
    val memoryVal = String.format("%.1f", zramDiskMb / 1024.0) + " GB"
    val memorySub = if (zramUsedMb > 0) {
        "${zramUsedMb}M used · $zramAlg"
    } else {
        "ZRAM Pool · $zramAlg"
    }

    SurfaceCard(modifier = modifier, padding = PaddingValues(0.dp)) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    OverviewCard(
                        eyebrow = "GPU",
                        value = gpuValue,
                        subtitle = gpuSubtitle,
                        icon = Icons.Rounded.SportsEsports,
                        accent = Color(0xFFFF7279),
                        onTap = { },
                    )
                }
                Box(
                    Modifier
                        .width(1.dp)
                        .height(54.dp)
                        .background(RodinSurfaceLines.divider(colors)),
                )
                Box(Modifier.weight(1f)) {
                    OverviewCard(
                        eyebrow = "CPU",
                        value = coreValue,
                        subtitle = coreSubtitle,
                        icon = Icons.Rounded.Memory,
                        accent = Color(0xFF67C2FF),
                        onTap = { },
                    )
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(0.8.dp)
                    .background(RodinSurfaceLines.divider(colors)),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    OverviewCard(
                        eyebrow = "DISPLAY",
                        value = display,
                        subtitle = if (snapshot.displayColor < 0 && snapshot.touchState < 1) {
                            "ROM colours"
                        } else {
                            touch
                        },
                        icon = Icons.Rounded.DisplaySettings,
                        accent = Color(0xFFB087FF),
                        onTap = { },
                    )
                }
                Box(
                    Modifier
                        .width(1.dp)
                        .height(54.dp)
                        .background(RodinSurfaceLines.divider(colors)),
                )
                Box(Modifier.weight(1f)) {
                    OverviewCard(
                        eyebrow = "MEMORY",
                        value = memoryVal,
                        subtitle = memorySub,
                        icon = Icons.Rounded.Layers,
                        accent = Color(0xFFFFB84D),
                        onTap = { },
                    )
                }
            }
        }
    }
}

@Composable
private fun OverviewCard(
    eyebrow: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onTap: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.surface.luminance() < 0.5f
    val card: @Composable () -> Unit = {
        Row(
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 78.dp)
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(35.dp)
                    .background(
                        accent.copy(alpha = if (dark) 0.13f else 0.09f),
                        RoundedCornerShape(12.dp),
                    )
                    .border(
                        1.dp,
                        accent.copy(alpha = if (dark) 0.24f else 0.16f),
                        RoundedCornerShape(12.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = eyebrow,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 8.4.sp,
                        lineHeight = 8.4.sp,
                        fontWeight = FontWeight.W800,
                        letterSpacing = 0.78.sp,
                        color = colors.onSurfaceVariant,
                    ),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 15.sp,
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.W900,
                        letterSpacing = (-0.25).sp,
                    ),
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 9.4.sp,
                        lineHeight = 9.4.sp,
                        color = colors.onSurfaceVariant,
                    ),
                )
            }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = colors.onSurfaceVariant.copy(alpha = 0.48f),
                modifier = Modifier.size(17.dp),
            )
        }
    }
    if (onTap != null) {
        PressScale(onTap = onTap, modifier = modifier) { card() }
    } else {
        Box(modifier) { card() }
    }
}

/** The door to the control hubs. */
@Composable
fun ControlCenterPortal(onTap: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.surface.luminance() < 0.5f
    val accent = colors.primary
    PressScale(onTap = onTap, modifier = modifier) {
        SurfaceCard(
            padding = PaddingValues(start = 16.dp, top = 15.dp, end = 13.dp, bottom = 15.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(50.dp)
                        .background(
                            accent.copy(alpha = if (dark) 0.12f else 0.09f),
                            RoundedCornerShape(17.dp),
                        )
                        .border(
                            1.dp,
                            accent.copy(alpha = if (dark) 0.28f else 0.18f),
                            RoundedCornerShape(17.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(25.dp),
                    )
                }
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Tune your device",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 16.5.sp,
                                fontWeight = FontWeight.W900,
                                letterSpacing = (-0.25).sp,
                            ),
                        )
                        Box(
                            Modifier
                                .background(accent.copy(alpha = 0.12f), RoundedCornerShape(99.dp))
                                .border(
                                    1.dp,
                                    accent.copy(alpha = 0.25f),
                                    RoundedCornerShape(99.dp),
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = "CONTROL HUBS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 8.2.sp,
                                    fontWeight = FontWeight.W900,
                                    letterSpacing = 0.7.sp,
                                    color = accent,
                                ),
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Every performance and hardware control in one place",
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 10.8.sp,
                            lineHeight = 10.8.sp * 1.25f,
                            color = colors.onSurfaceVariant,
                        ),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Icon(
                    imageVector = Icons.Rounded.ArrowForwardIos,
                    contentDescription = null,
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
