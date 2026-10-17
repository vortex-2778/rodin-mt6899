package io.github.neeschal.rodinessential.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.neeschal.rodinessential.backend.RodinBackend
import io.github.neeschal.rodinessential.ui.components.IconTile
import io.github.neeschal.rodinessential.ui.components.PressScale
import io.github.neeschal.rodinessential.ui.components.RodinHaptics
import io.github.neeschal.rodinessential.ui.components.SurfaceCard
import io.github.neeschal.rodinessential.ui.components.rodinShadow
import kotlinx.coroutines.delay

private fun touchProfileTitle(profile: Int): String = when (profile) {
    1 -> "250 Hz Mode"
    2 -> "500 Hz Mode"
    3 -> "1000 Hz Output"
    else -> "OEM Control"
}

private fun touchProfileRate(profile: Int): String = when (profile) {
    1 -> "240 Hz native timing · simple testers display about 250"
    2 -> "480 Hz native timing · simple testers display about 500"
    3 -> "Precise 1 ms Android output · native source remains 480 Hz"
    else -> "Rodin Essential override released · ROM-managed timing"
}

private fun touchProfileAccent(profile: Int): Color = when (profile) {
    1 -> Color(0xFFFFB84D)
    2 -> Color(0xFFFF6B57)
    3 -> Color(0xFFE95CFF)
    else -> Color(0xFFFFB84D)
}

private fun touchProfileIcon(profile: Int): ImageVector = when (profile) {
    1 -> Icons.Rounded.SportsEsports
    2 -> Icons.Rounded.Speed
    3 -> Icons.Rounded.Whatshot
    else -> Icons.Rounded.AutoAwesome
}

/** Touch response: the four fixed timing profiles and the pipeline status. */
@Composable
fun TouchBoostScreen(onBack: () -> Unit) {
    val snapshot = rememberRodinSnapshot()
    val colors = MaterialTheme.colorScheme
    var pendingProfile by remember { mutableIntStateOf(-1) }
    var pendingGeneration by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        RodinBackend.refresh()
    }
    LaunchedEffect(pendingGeneration) {
        if (pendingGeneration > 0) {
            delay(3400)
            pendingProfile = -1
            RodinBackend.refresh()
        }
    }

    fun selectProfile(profile: Int): Boolean {
        if (!RodinBackend.setTouchProfile(profile)) {
            return false
        }
        pendingProfile = profile
        pendingGeneration += 1
        return true
    }

    val savedProfile = if (snapshot.touchState in 1..3) snapshot.touchState else 0
    val activeProfile = if (pendingProfile != -1) pendingProfile else savedProfile
    val accent = touchProfileAccent(activeProfile)
    val touchAck = RodinBackend.extendedValue(29)
    val panelCode = RodinBackend.extendedValue(62)
    val controlPath = RodinBackend.extendedValue(63)
    val panel = when (panelCode) {
        1 -> "Goodix GT9916"
        2 -> "FocalTech"
        else -> "Panel auto-detect"
    }
    val engine = when (controlPath) {
        1 -> "Vendor HAL"
        2 -> "Direct driver fallback"
        3 -> "Rodin Essential 1 ms output scheduler"
        else -> if (snapshot.touchHal == 1) "Vendor HAL ready" else "Unavailable"
    }
    val applying = pendingProfile != -1 && savedProfile != pendingProfile
    val oemManaged = activeProfile == 0 && !applying
    val controlsEnabled = snapshot.controlsAvailable && snapshot.touchHal == 1 && !snapshot.busy

    RodinScrollPage(
        header = RodinPageHeader.Detail(title = "Touch Response", onBack = onBack),
    ) {
        Spacer(Modifier.height(4.dp))
        Text(
            text = "OEM Control leaves touch timing to your ROM by default. " +
                "Choose an override to save it across restarts.",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.5.sp,
                color = colors.onSurfaceVariant,
            ),
        )
        Spacer(Modifier.height(10.dp))
        HeroCard(
            icon = touchProfileIcon(activeProfile),
            accent = accent,
            title = touchProfileTitle(activeProfile),
            subtitle = if (snapshot.touchHal == 1) {
                "${touchProfileRate(activeProfile)} · $panel · $engine"
            } else {
                "Rodin Essential touch engine unavailable on this vendor stack"
            },
        )
        Spacer(Modifier.height(12.dp))
        TouchProfileGrid(
            selectedProfile = activeProfile,
            enabled = controlsEnabled,
            onSelected = { profile -> selectProfile(profile) },
        )
        Spacer(Modifier.height(12.dp))
        SurfaceCard(accent = accent) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(
                    icon = if (oemManaged) {
                        Icons.Rounded.AutoAwesome
                    } else if (!applying && touchAck == 1) {
                        Icons.Rounded.Verified
                    } else {
                        Icons.Rounded.Sync
                    },
                    accent = if (oemManaged) {
                        colors.primary
                    } else if (!applying && touchAck == 1) {
                        Color(0xFF41C98A)
                    } else {
                        Color(0xFFFFB84D)
                    },
                    size = 44.dp,
                )
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = when {
                            oemManaged -> "OEM-managed touch"
                            !applying && touchAck == 1 -> "Profile persisted"
                            else -> "Applying touch pipeline"
                        },
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.W700,
                        ),
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = when {
                            oemManaged -> "No Rodin Essential fixed-rate profile is active. " +
                                "Your ROM manages touch timing."
                            !applying && touchAck == 1 -> "Reasserted after boot, screen wake, and vendor resets"
                            else -> "Waiting for the panel HAL to confirm every required mode"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.5.sp,
                            lineHeight = 11.5.sp * 1.25f,
                            color = colors.onSurfaceVariant,
                        ),
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        SurfaceCard {
            InfoBlock(
                title = "Tester values",
                text = "250 uses the native 240 Hz panel target and 500 uses the native " +
                    "480 Hz target; simple tester apps round those values to about 250 " +
                    "and 500. The 1000 option delivers a one-millisecond Android event " +
                    "stream from the 480 Hz native source. OEM Control releases the " +
                    "fixed profile and returns timing to vendor management.",
            )
        }
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun TouchProfileGrid(
    selectedProfile: Int,
    enabled: Boolean,
    onSelected: (Int) -> Boolean,
) {
    val colors = MaterialTheme.colorScheme
    Column {
        Text(
            text = "TOUCH MODES",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.W800,
                letterSpacing = 0.65.sp,
                color = colors.onSurfaceVariant,
            ),
        )
        Spacer(Modifier.height(9.dp))
        TouchProfileTile(
            profile = 0,
            title = "OEM Control",
            rate = "ROM-managed touch timing",
            detail = "Default · No fixed-rate override",
            icon = Icons.Rounded.AutoAwesome,
            accent = colors.primary,
            selected = selectedProfile == 0,
            enabled = enabled,
            onSelected = onSelected,
        )
        Spacer(Modifier.height(10.dp))
        Row {
            Box(Modifier.weight(1f)) {
                TouchProfileTile(
                    profile = 1,
                    title = "250 Hz",
                    rate = "240 Hz native target",
                    detail = "Tester display ≈250",
                    icon = Icons.Rounded.SportsEsports,
                    accent = Color(0xFFFFB84D),
                    selected = selectedProfile == 1,
                    enabled = enabled,
                    onSelected = onSelected,
                )
            }
            Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f)) {
                TouchProfileTile(
                    profile = 2,
                    title = "500 Hz",
                    rate = "480 Hz native target",
                    detail = "Tester display ≈500",
                    icon = Icons.Rounded.Speed,
                    accent = Color(0xFFFF6B57),
                    selected = selectedProfile == 2,
                    enabled = enabled,
                    onSelected = onSelected,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        TouchProfileTile(
            profile = 3,
            title = "1000 Hz",
            rate = "1 ms Android event output",
            detail = "Resampled from the native 480 Hz source",
            icon = Icons.Rounded.Whatshot,
            accent = Color(0xFFE95CFF),
            selected = selectedProfile == 3,
            enabled = enabled,
            onSelected = onSelected,
        )
    }
}

@Composable
private fun TouchProfileTile(
    profile: Int,
    title: String,
    rate: String,
    detail: String,
    icon: ImageVector,
    accent: Color,
    selected: Boolean,
    enabled: Boolean,
    onSelected: (Int) -> Boolean,
) {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val colors = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val tile: @Composable () -> Unit = {
        Column(
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 132.dp)
                .then(
                    if (selected) {
                        Modifier.rodinShadow(
                            color = accent.copy(alpha = 0.10f),
                            blurRadius = 16f,
                            spreadRadius = -4f,
                            cornerRadius = with(density) { 18.dp.toPx() },
                        )
                    } else {
                        Modifier
                    },
                )
                .background(
                    if (selected) {
                        accent.copy(alpha = if (dark) 0.15f else 0.10f)
                    } else {
                        if (dark) Color(0xFF151A24) else Color(0xFFF4F7FA)
                    },
                    RoundedCornerShape(18.dp),
                )
                .border(
                    if (selected) 1.6.dp else 1.0.dp,
                    if (selected) {
                        accent.copy(alpha = 0.88f)
                    } else {
                        colors.outline.copy(alpha = 0.38f)
                    },
                    RoundedCornerShape(18.dp),
                )
                .padding(13.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(34.dp)
                        .background(
                            accent.copy(alpha = if (selected) 0.20f else 0.10f),
                            RoundedCornerShape(11.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(19.dp),
                    )
                }
                Box(
                    Modifier
                        .size(20.dp)
                        .border(
                            if (selected) 5.5.dp else 2.dp,
                            if (selected) accent else colors.onSurfaceVariant,
                            RoundedCornerShape(99.dp),
                        ),
                )
            }
            Spacer(Modifier.height(11.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 13.2.sp,
                    fontWeight = FontWeight.W800,
                ),
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = rate,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.2.sp,
                    fontWeight = FontWeight.W700,
                    color = if (selected) accent else colors.onSurfaceVariant,
                ),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = detail,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.4.sp,
                    color = colors.onSurfaceVariant,
                ),
            )
        }
    }
    if (enabled && !selected) {
        PressScale(
            onTap = {
                if (onSelected(profile)) {
                    RodinHaptics.segment()
                } else {
                    RodinHaptics.reject()
                }
            },
        ) {
            tile()
        }
    } else {
        tile()
    }
}
