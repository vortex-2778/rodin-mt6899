package io.github.neeschal.rodinessential.ui.screens

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Battery5Bar
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.OfflineBolt
import androidx.compose.material.icons.rounded.Power
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.neeschal.rodinessential.backend.BypassTelemetry
import io.github.neeschal.rodinessential.backend.RodinBackend
import io.github.neeschal.rodinessential.backend.RodinBackendSnapshot
import io.github.neeschal.rodinessential.ui.components.PressScale
import io.github.neeschal.rodinessential.ui.components.RodinBypassThresholdSelector
import io.github.neeschal.rodinessential.ui.components.RodinHaptics
import io.github.neeschal.rodinessential.ui.components.SurfaceCard
import io.github.neeschal.rodinessential.ui.components.rodinShadow
import io.github.neeschal.rodinessential.ui.theme.RodinSurfaceLines
import kotlin.math.abs
import kotlin.math.roundToInt

private val bypassCardAccent = Color(0xFF9B7CFF)

private fun chargingProfileAccent(watts: Int): Color = when (watts) {
    0 -> Color(0xFF67C2FF)
    25, 33, 65, 85 -> Color(0xFF35C997)
    90 -> Color(0xFFFFA340)
    else -> Color(0xFF35C997)
}

private fun chargingProfileIcon(watts: Int): ImageVector = when (watts) {
    0 -> Icons.Rounded.AutoAwesome
    25 -> Icons.Rounded.Eco
    33 -> Icons.Rounded.BatteryChargingFull
    65 -> Icons.Rounded.Bolt
    85 -> Icons.Rounded.FlashOn
    90 -> Icons.Rounded.OfflineBolt
    else -> Icons.Rounded.Bolt
}

private fun chargingProfileName(watts: Int): String = when (watts) {
    0 -> "OEM Adaptive"
    25 -> "Cool Daily"
    33 -> "Balanced Fast"
    65 -> "Fast Daily"
    85 -> "Near Maximum"
    90 -> "HyperCharge"
    else -> "Charge Output"
}

private fun chargingProfileDescription(watts: Int): String = when (watts) {
    0 -> "ROM-managed charging, taper and battery care"
    25 -> "Lower-heat everyday charging"
    33 -> "Balanced speed and temperature"
    65 -> "Fast daily charging with headroom"
    85 -> "Near-maximum charging"
    90 -> "Full software charging ceiling"
    else -> "Select a charging profile"
}

/**
 * Charging control: live battery telemetry, the kernel charge-pause bypass
 * and the fixed power profiles.
 */
@Composable
fun ChargingScreen(onBack: () -> Unit, focusRequest: Int) {
    val snapshot = rememberRodinSnapshot()
    val colors = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()
    var bypassOffset by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val screenHeightPx = with(density) { LocalConfiguration.current.screenHeightDp.dp.toPx() }

    LaunchedEffect(Unit) {
        RodinBackend.refresh()
    }
    // The dock shortcut may ask for the bypass card directly, exactly like the
    // Dart post-frame ensure-visible with a 0.16 alignment.
    LaunchedEffect(focusRequest) {
        if (focusRequest > 0) {
            withFrameNanos { }
            val target = (bypassOffset - screenHeightPx * 0.16f).roundToInt().coerceAtLeast(0)
            scrollState.scrollTo(target)
        }
    }

    RodinScrollPage(
        header = RodinPageHeader.Detail(title = "Charging Control", onBack = onBack),
        scrollState = scrollState,
    ) {
        Spacer(Modifier.height(3.dp))
        Text(
            text = "Rodin-native power ceilings with verified live telemetry",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.5.sp,
                color = colors.onSurfaceVariant,
            ),
        )
        Spacer(Modifier.height(10.dp))
        ChargingBatteryCard(snapshot = snapshot)
        Spacer(Modifier.height(9.dp))
        Box(
            Modifier.onGloballyPositioned {
                bypassOffset = it.positionInParent().y.roundToInt()
            },
        ) {
            BypassChargingCard(snapshot = snapshot)
        }
        Spacer(Modifier.height(9.dp))
        ChargingModeCard(snapshot = snapshot)
        Spacer(Modifier.height(10.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChargingBatteryCard(snapshot: RodinBackendSnapshot) {
    val colors = MaterialTheme.colorScheme
    val accent = Color(0xFF35C997)
    val bypassEnabled = RodinBackend.bypassChargingEnabled
    val bypassPhase = RodinBackend.bypassPhase
    val isBypass = snapshot.ready &&
        snapshot.serviceEnabled &&
        bypassEnabled &&
        bypassPhase == 4
    val isCharging = snapshot.batteryStatus == 1 &&
        !isBypass &&
        !(bypassEnabled && (bypassPhase == 3 || bypassPhase == 6))

    SurfaceCard(
        padding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 14.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(48.dp)
                        .background(
                            (if (isCharging) Color(0xFF35C997) else Color(0xFF4EA8DE))
                                .copy(alpha = 0.12f),
                            RoundedCornerShape(16.dp),
                        )
                        .border(
                            1.dp,
                            (if (isCharging) Color(0xFF35C997) else Color(0xFF4EA8DE))
                                .copy(alpha = 0.28f),
                            RoundedCornerShape(16.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isCharging) {
                            Icons.Rounded.Bolt
                        } else {
                            Icons.Rounded.Battery5Bar
                        },
                        contentDescription = null,
                        tint = if (isCharging) Color(0xFF35C997) else Color(0xFF4EA8DE),
                        modifier = Modifier.size(26.dp),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Text(
                            text = chargingCapacity(snapshot),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = 26.sp,
                                lineHeight = 26.sp,
                                fontWeight = FontWeight.W900,
                                letterSpacing = (-0.5).sp,
                            ),
                        )
                        if (isCharging || isBypass) {
                            Box(
                                Modifier
                                    .background(
                                        Color(0xFF35C997).copy(alpha = 0.14f),
                                        RoundedCornerShape(99.dp),
                                    )
                                    .border(
                                        1.dp,
                                        Color(0xFF35C997).copy(alpha = 0.35f),
                                        RoundedCornerShape(99.dp),
                                    )
                                    .padding(horizontal = 7.dp, vertical = 3.dp),
                            ) {
                                Text(
                                    text = if (isBypass) "BYPASS" else "CHARGING",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 8.8.sp,
                                        fontWeight = FontWeight.W900,
                                        letterSpacing = 0.6.sp,
                                        color = Color(0xFF35C997),
                                    ),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${chargingStatus(snapshot)} · ${chargingHealth(snapshot)}",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.W600,
                            color = colors.onSurfaceVariant,
                        ),
                    )
                }
                ChargingStatePill(
                    label = snapshot.serviceStatusBadgeLabel,
                    accent = if (snapshot.controlsAvailable) accent else colors.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(RodinSurfaceLines.divider(colors)),
            )
            Spacer(Modifier.height(12.dp))
            Row {
                Box(Modifier.weight(1f)) {
                    ChargingMetric(
                        label = "Temperature",
                        value = chargingTemperature(snapshot),
                        icon = Icons.Rounded.Thermostat,
                        accentColor = chargingTempColor(snapshot),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(1f)) {
                    ChargingMetric(
                        label = "Battery Input",
                        value = chargingWattage(snapshot),
                        icon = Icons.Rounded.Bolt,
                        accentColor = Color(0xFFFFB84D),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row {
                Box(Modifier.weight(1f)) {
                    ChargingMetric(
                        label = if (snapshot.chargingMode > 0) "Selected Max" else "Adapter Max",
                        value = chargingAdapter(snapshot),
                        icon = Icons.Rounded.Power,
                        accentColor = Color(0xFF67C2FF),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(1f)) {
                    ChargingMetric(
                        label = "Current",
                        value = chargingCurrent(snapshot),
                        icon = Icons.Rounded.Speed,
                        accentColor = Color(0xFFB087FF),
                    )
                }
            }
        }
    }
}

private fun chargingCapacity(snapshot: RodinBackendSnapshot): String =
    if (snapshot.batteryCapacity >= 0) "${snapshot.batteryCapacity}%" else "—"

private fun chargingTemperature(snapshot: RodinBackendSnapshot): String {
    val value = snapshot.batteryTempC ?: return "—"
    return String.format("%.1f", value) + "°C"
}

private fun chargingCurrent(snapshot: RodinBackendSnapshot): String {
    val value = snapshot.batteryCurrentA ?: return "—"
    return String.format("%.2f", abs(value)) + " A"
}

private fun chargingWattage(snapshot: RodinBackendSnapshot): String {
    val milliwatts = RodinBackend.chargingLiveMilliwatts
    if (snapshot.batteryStatus != 1 || milliwatts < 0) return "—"
    return String.format("%.1f", milliwatts / 1000.0) + " W"
}

private fun chargingAdapter(snapshot: RodinBackendSnapshot): String {
    val selected = snapshot.chargingMode
    if (selected > 0) return "$selected W"
    val watts = RodinBackend.chargingAdapterWatts
    return if (watts > 0) "$watts W" else "Detecting"
}

private fun chargingStatus(snapshot: RodinBackendSnapshot): String {
    if (snapshot.controlsAvailable &&
        RodinBackend.bypassChargingEnabled &&
        RodinBackend.bypassPhase == 4
    ) {
        return "Direct power · Battery charging paused"
    }
    if (RodinBackend.bypassChargingEnabled) {
        when (RodinBackend.bypassPhase) {
            1 -> return "Charging to ${RodinBackend.bypassThreshold}%"
            3 -> return "Charge pause requested · Power settling"
            5 -> return "Bypass control unavailable"
            6 -> return "Battery supplementing adapter power"
            7 -> return "Charging detected · Bypass not confirmed"
        }
    }
    return when (snapshot.batteryStatus) {
        1 -> "Charging"
        2 -> "Discharging"
        3 -> "Not charging"
        4 -> "Full"
        0 -> "Unknown"
        else -> "Unavailable"
    }
}

private fun chargingHealth(snapshot: RodinBackendSnapshot): String =
    when (snapshot.batteryHealth) {
        1 -> "Good health"
        2 -> "Overheat"
        3 -> "Dead"
        4 -> "Over voltage"
        5 -> "Cold"
        0 -> "Unknown"
        else -> "Unavailable"
    }

private fun chargingTempColor(snapshot: RodinBackendSnapshot): Color {
    val t = snapshot.batteryTempC ?: return Color(0xFF67C2FF)
    if (t >= 44.0) return Color(0xFFFF453A)
    if (t >= 38.0) return Color(0xFFFFB84D)
    return Color(0xFF41C98A)
}

@Composable
private fun ChargingMetric(
    label: String,
    value: String,
    icon: ImageVector,
    accentColor: Color? = null,
) {
    val colors = MaterialTheme.colorScheme
    val color = accentColor ?: colors.primary
    Row(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 68.dp)
            .background(
                colors.surfaceContainer.copy(alpha = 0.45f),
                RoundedCornerShape(15.dp),
            )
            .border(1.dp, RodinSurfaceLines.inset(colors), RoundedCornerShape(15.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(32.dp)
                .background(color.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(17.dp),
            )
        }
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.W600,
                    color = colors.onSurfaceVariant,
                ),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.W800,
                    letterSpacing = (-0.2).sp,
                ),
            )
        }
    }
}

@Composable
private fun ChargingStatePill(label: String, accent: Color) {
    Box(
        Modifier
            .background(accent.copy(alpha = 0.10f), RoundedCornerShape(999.dp))
            .border(1.dp, accent.copy(alpha = 0.18f), RoundedCornerShape(999.dp))
            .padding(horizontal = 9.dp, vertical = 5.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = accent,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.W800,
                letterSpacing = 0.5.sp,
            ),
        )
    }
}

@Composable
private fun BypassChargingCard(snapshot: RodinBackendSnapshot) {
    val colors = MaterialTheme.colorScheme
    val accent = bypassCardAccent
    val supported = RodinBackend.bypassChargingSupported
    var pending by remember { mutableStateOf<Boolean?>(null) }
    var thresholdPending by remember { mutableStateOf<Int?>(null) }
    var thresholdPreview by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(snapshot) {
        if (pending != null &&
            (RodinBackend.bypassChargingEnabled == pending ||
                snapshot.chargingWriteState == -1)
        ) {
            pending = null
        }
        if (thresholdPending != null &&
            (RodinBackend.bypassThreshold == thresholdPending ||
                snapshot.chargingWriteState == -1)
        ) {
            thresholdPending = null
        }
    }

    val directPowerConfirmed = BypassTelemetry.isBypassDirectPowerConfirmed(
        ready = snapshot.ready,
        enabled = RodinBackend.bypassChargingEnabled,
        kernelActive = RodinBackend.bypassHardwareActive,
        usbOnline = snapshot.usbOnline,
        batteryCurrentA = snapshot.batteryCurrentA,
    )

    fun stateLabel(enabled: Boolean): String {
        if (!snapshot.ready) return "BACKEND UNAVAILABLE"
        if (!snapshot.serviceEnabled) return "DISABLED"
        if (pending != null) return "APPLYING"
        if (!supported) return "NOT SUPPORTED"
        if (!enabled) return "OFF"
        return when (RodinBackend.bypassPhase) {
            1 -> "CHARGING TO THRESHOLD"
            2 -> "ARMED"
            4 -> if (directPowerConfirmed) "DIRECT POWER" else "POWER SETTLING"
            5 -> "CONTROL UNAVAILABLE"
            6 -> "BATTERY ASSIST"
            7 -> "CHARGING DETECTED"
            else -> "POWER SETTLING"
        }
    }

    fun description(enabled: Boolean): String {
        if (!snapshot.ready) {
            return "Connect to the device backend to check kernel bypass support."
        }
        if (!snapshot.serviceEnabled) {
            return "Rodin Essential is disabled. Enable it in Settings to use bypass charging."
        }
        if (pending != null) return "Applying the kernel bypass setting…"
        if (!supported) {
            return "A compatible kernel power-path bypass node is required. " +
                "input_suspend is not used: stopping charger input alone does not " +
                "provide direct system power."
        }
        if (!enabled) {
            return "Uses the supported kernel charge-pause power path. Select a " +
                "battery threshold, or Immediately to pause charging as soon as enabled. " +
                "Works independently of this page and the app."
        }
        if (snapshot.usbOnline == 0) {
            return "Bypass is armed. Connect a charger to activate direct system power."
        }
        val threshold = RodinBackend.bypassThreshold
        if (RodinBackend.bypassPhase == 1) {
            return "OEM charging continues below $threshold%. At $threshold%, Rodin Essential " +
                "requests bypass automatically. Charging resumes only if the level " +
                "falls two percentage points below the threshold."
        }
        if (RodinBackend.bypassPhase == 5) {
            return "The saved choice is still enabled, but kernel control or battery " +
                "telemetry is unavailable. Bypass is not confirmed."
        }
        if (RodinBackend.bypassPhase == 6) {
            return "Charge pause is requested, but the battery is supplying part of " +
                "the load. Check your adapter and cable; direct power is not confirmed."
        }
        if (RodinBackend.bypassPhase == 7) {
            return "Battery current shows charging despite the bypass request. " +
                "Rodin Essential is not reporting this as working bypass."
        }
        if (!directPowerConfirmed) {
            return "The kernel charge-pause request is active. Power negotiation and " +
                "battery-current measurement are settling; opening this page does " +
                "not restart them."
        }
        return "Kernel bypass is enabled. External power is connected and battery " +
            "current has stayed near zero, consistent with direct system power."
    }

    fun setEnabled(enabled: Boolean) {
        if (pending != null ||
            thresholdPending != null ||
            snapshot.chargingWriteState == 1 ||
            !supported
        ) {
            return
        }
        pending = enabled
        RodinHaptics.toggle(enabled)
        if (!RodinBackend.setBypassCharging(enabled)) {
            pending = null
            RodinHaptics.reject()
            RodinBackend.refresh()
        }
    }

    fun setThreshold(threshold: Int) {
        thresholdPreview = null
        if (thresholdPending != null || !snapshot.controlsAvailable) return
        if (threshold == RodinBackend.bypassThreshold) return
        RodinHaptics.segment()
        thresholdPending = threshold
        if (!RodinBackend.setBypassThreshold(threshold)) {
            thresholdPending = null
            RodinHaptics.reject()
        }
    }

    val enabled = pending ?: RodinBackend.bypassChargingEnabled
    val applying = pending != null || snapshot.chargingWriteState == 1
    val state = stateLabel(enabled)
    val flow by animateFloatAsState(
        targetValue = if (enabled) 1f else 0f,
        animationSpec = tween(
            durationMillis = 420,
            easing = CubicBezierEasing(0.215f, 0.61f, 0.355f, 1f),
        ),
        label = "bypassFlow",
    )
    val thresholdShown = thresholdPreview ?: thresholdPending ?: RodinBackend.bypassThreshold

    SurfaceCard(
        padding = PaddingValues(start = 14.dp, top = 14.dp, end = 10.dp, bottom = 14.dp),
        accent = if (supported) accent else colors.onSurfaceVariant,
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(48.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    accent.copy(alpha = if (enabled) 0.30f else 0.14f),
                                    accent.copy(alpha = if (enabled) 0.09f else 0.035f),
                                ),
                                start = Offset.Zero,
                                end = Offset.Infinite,
                            ),
                            RoundedCornerShape(16.dp),
                        )
                        .border(
                            1.dp,
                            accent.copy(alpha = if (enabled) 0.42f else 0.18f),
                            RoundedCornerShape(16.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    BypassPowerPath(
                        color = if (supported) accent else colors.onSurfaceVariant,
                        flow = flow,
                    )
                }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Bypass Charging",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.W800,
                        ),
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier
                            .background(
                                (if (enabled) accent else colors.onSurfaceVariant)
                                    .copy(alpha = 0.11f),
                                RoundedCornerShape(8.dp),
                            )
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                    ) {
                        Text(
                            text = state,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.6.sp,
                                fontWeight = FontWeight.W900,
                                letterSpacing = 0.55.sp,
                                color = if (enabled) accent else colors.onSurfaceVariant,
                            ),
                        )
                    }
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = if (supported && snapshot.controlsAvailable) {
                        { value -> setEnabled(value) }
                    } else {
                        null
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = accent,
                        checkedTrackColor = accent.copy(alpha = 0.34f),
                    ),
                )
            }
            Spacer(Modifier.height(11.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Start bypass at",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.W700,
                    ),
                )
                Text(
                    text = if (thresholdShown <= 0) "Immediately" else "$thresholdShown%",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = accent,
                        fontWeight = FontWeight.W800,
                        fontSize = 12.sp,
                    ),
                )
            }
            Spacer(Modifier.height(8.dp))
            RodinBypassThresholdSelector(
                value = thresholdPending ?: RodinBackend.bypassThreshold,
                enabled = supported &&
                    snapshot.controlsAvailable &&
                    !applying &&
                    thresholdPending == null,
                onChanged = { threshold -> setThreshold(threshold) },
                accent = accent,
                onPreview = { threshold ->
                    RodinHaptics.frequentSegment()
                    thresholdPreview = threshold
                },
            )
            Spacer(Modifier.height(11.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(
                        (if (supported) accent else colors.onSurfaceVariant).copy(alpha = 0.065f),
                        RoundedCornerShape(14.dp),
                    )
                    .border(
                        1.dp,
                        (if (supported) accent else colors.onSurfaceVariant).copy(alpha = 0.16f),
                        RoundedCornerShape(14.dp),
                    )
                    .padding(horizontal = 11.dp, vertical = 10.dp),
            ) {
                Box(
                    Modifier.defaultMinSize(
                        minHeight = with(LocalDensity.current) { (10.7f * 1.35f * 5f).sp.toDp() },
                    ),
                ) {
                    Text(
                        text = description(enabled),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 10.7.sp,
                            lineHeight = 10.7.sp * 1.35f,
                            color = colors.onSurfaceVariant,
                        ),
                    )
                }
            }
        }
    }
}

/** Adapter feeds the system rail; the lower battery branch is isolated. */
@Composable
private fun BypassPowerPath(color: Color, flow: Float, modifier: Modifier = Modifier) {
    Canvas(modifier.size(32.dp)) {
        val s = size.width / 32f
        scale(scaleX = s, scaleY = s, pivot = Offset.Zero) {
            val stroke = Stroke(
                width = 1.8f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            )
            drawLine(color, Offset(4f, 7f), Offset(4f, 10f), style = stroke)
            drawLine(color, Offset(8f, 7f), Offset(8f, 10f), style = stroke)
            drawRoundRect(
                color = color,
                topLeft = Offset(2f, 10f),
                size = Size(8f, 7f),
                cornerRadius = CornerRadius(2f, 2f),
                style = stroke,
            )
            val path = Path().apply {
                moveTo(6f, 17f)
                lineTo(6f, 20f)
                quadraticBezierTo(6f, 22f, 8f, 22f)
                lineTo(14f, 22f)
                quadraticBezierTo(16f, 22f, 16f, 20f)
                lineTo(16f, 13f)
                lineTo(21f, 13f)
            }
            drawPath(path, color, style = stroke)
            drawRoundRect(
                color = color,
                topLeft = Offset(21f, 8f),
                size = Size(9f, 10f),
                cornerRadius = CornerRadius(2f, 2f),
                style = stroke,
            )
            for (x in listOf(24f, 27f)) {
                drawLine(color, Offset(x, 5.5f), Offset(x, 8f), style = stroke)
                drawLine(color, Offset(x, 18f), Offset(x, 20.5f), style = stroke)
            }
            drawRect(color, Offset(24f, 11f), Size(3f, 4f))
            val battery = Stroke(
                width = 1.4f,
                cap = StrokeCap.Round,
            )
            val batteryColor = color.copy(alpha = 0.55f - flow * 0.25f)
            drawRoundRect(
                color = batteryColor,
                topLeft = Offset(20f, 25f),
                size = Size(10f, 5f),
                cornerRadius = CornerRadius(1f, 1f),
                style = battery,
            )
            drawLine(batteryColor, Offset(18.5f, 26.5f), Offset(18.5f, 28.5f), style = battery)
            drawLine(batteryColor, Offset(17f, 23.5f), Offset(20f, 23.5f), style = battery)
            if (flow > 0f) {
                drawCircle(color, radius = 2f, center = Offset(16f + 3f * flow, 13f))
            }
        }
    }
}

@Composable
private fun ChargingModeCard(snapshot: RodinBackendSnapshot) {
    val colors = MaterialTheme.colorScheme
    val accent = Color(0xFF35C997)
    var pendingProfile by remember { mutableIntStateOf(-1) }

    LaunchedEffect(snapshot) {
        if (pendingProfile != -1 &&
            (snapshot.chargingMode == pendingProfile || snapshot.chargingWriteState == -1)
        ) {
            pendingProfile = -1
        }
    }

    val selected = if (pendingProfile != -1) pendingProfile else snapshot.chargingMode
    val profileAccent = chargingProfileAccent(selected)
    val profileIcon = chargingProfileIcon(selected)
    val enabled = snapshot.controlsAvailable &&
        RodinBackend.chargingControlSupported &&
        !RodinBackend.bypassChargingEnabled &&
        snapshot.chargingWriteState != 1

    fun detail(selectedWatts: Int): String {
        if (!snapshot.ready) return "Charging backend unavailable"
        if (!RodinBackend.chargingControlSupported) {
            return "Rodin Essential FCC control is unavailable on this kernel"
        }
        if (RodinBackend.bypassChargingEnabled) {
            return "Disable Bypass Charging to use charging profiles"
        }
        if (snapshot.chargingWriteState == 1) {
            return "Applying and verifying the selected ceiling…"
        }
        if (snapshot.chargingWriteState == -1) {
            return "The requested ceiling could not be verified"
        }
        return if (selectedWatts == 0) {
            "ROM and battery controller manage charging"
        } else {
            "Rodin Essential fast-charge path · $selectedWatts W maximum"
        }
    }

    fun select(profile: Int) {
        val allowed = snapshot.controlsAvailable &&
            RodinBackend.chargingControlSupported &&
            !RodinBackend.bypassChargingEnabled &&
            snapshot.chargingWriteState != 1
        val current = if (pendingProfile != -1) pendingProfile else snapshot.chargingMode
        if (!allowed || profile == current) return
        RodinHaptics.segment()
        pendingProfile = profile
        if (!RodinBackend.setChargingProfile(profile)) {
            pendingProfile = -1
            RodinHaptics.reject()
            RodinBackend.refresh()
        }
    }

    val faded = if (enabled) 1f else 0.46f

    SurfaceCard(
        padding = PaddingValues(start = 14.dp, top = 14.dp, end = 14.dp, bottom = 15.dp),
        accent = profileAccent,
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(48.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    profileAccent.copy(alpha = 0.22f),
                                    profileAccent.copy(alpha = 0.07f),
                                ),
                                start = Offset.Zero,
                                end = Offset.Infinite,
                            ),
                            RoundedCornerShape(16.dp),
                        )
                        .border(
                            1.dp,
                            profileAccent.copy(alpha = 0.28f),
                            RoundedCornerShape(16.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = profileIcon,
                        contentDescription = null,
                        tint = profileAccent,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = when {
                            selected == 0 -> "OEM Adaptive"
                            selected == 90 -> "90 W HyperCharge"
                            else -> "$selected W Charge Limit"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.W800,
                            color = colors.onSurface,
                        ),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = detail(selected),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.5.sp,
                            color = colors.onSurfaceVariant,
                        ),
                    )
                }
                if (snapshot.chargingWriteState == 1) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = profileAccent,
                    )
                }
            }
            Spacer(Modifier.height(13.dp))
            if (RodinBackend.bypassChargingEnabled) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(bypassCardAccent.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                        .border(
                            1.dp,
                            bypassCardAccent.copy(alpha = 0.20f),
                            RoundedCornerShape(14.dp),
                        )
                        .padding(horizontal = 11.dp, vertical = 10.dp),
                ) {
                    Row {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = bypassCardAccent,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Charging profiles are unavailable while Bypass Charging " +
                                "is enabled. Disable it to change or use a charging profile.",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 10.5.sp,
                                lineHeight = 10.5.sp * 1.3f,
                                color = colors.onSurfaceVariant,
                            ),
                        )
                    }
                }
                Spacer(Modifier.height(11.dp))
            }
            Box(Modifier.graphicsLayer { alpha = faded }) {
                ChargingAdaptiveTile(
                    selected = selected == 0,
                    enabled = enabled,
                    onTap = { select(0) },
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "FIXED POWER PROFILES",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.W800,
                        letterSpacing = 0.55.sp,
                        color = colors.onSurfaceVariant,
                    ),
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "5 LEVELS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.W700,
                        letterSpacing = 0.45.sp,
                        color = colors.onSurfaceVariant.copy(alpha = 0.72f),
                    ),
                )
            }
            Spacer(Modifier.height(8.dp))
            Box(Modifier.graphicsLayer { alpha = faded }) {
                ChargingPowerSpectrum(
                    selected = selected,
                    enabled = enabled,
                    onSelect = { watts -> select(watts) },
                )
            }
            Spacer(Modifier.height(10.dp))
            Box(Modifier.graphicsLayer { alpha = faded }) {
                ChargingProfileDetail(
                    watts = selected,
                    applying = snapshot.chargingWriteState == 1,
                )
            }
            Spacer(Modifier.height(11.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(accent.copy(alpha = 0.07f), RoundedCornerShape(14.dp))
                    .border(1.dp, accent.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 11.dp, vertical = 10.dp),
            ) {
                Row {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier
                            .padding(top = 1.dp)
                            .size(15.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "The selected value is a maximum. Live power follows adapter " +
                            "capability, battery level, temperature and charger-IC protection.",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 10.5.sp,
                            lineHeight = 10.5.sp * 1.35f,
                            color = colors.onSurfaceVariant,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun ChargingAdaptiveTile(
    selected: Boolean,
    enabled: Boolean,
    onTap: () -> Unit,
) {
    val accent = Color(0xFF67C2FF)
    val colors = MaterialTheme.colorScheme
    val tile: @Composable () -> Unit = {
        Row(
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            accent.copy(alpha = if (selected) 0.18f else 0.085f),
                            colors.surfaceContainer.copy(alpha = 0.46f),
                        ),
                        start = Offset.Zero,
                        end = Offset.Infinite,
                    ),
                    RoundedCornerShape(16.dp),
                )
                .border(
                    if (selected) 1.3.dp else 0.9.dp,
                    if (selected) {
                        accent.copy(alpha = 0.68f)
                    } else {
                        colors.outline.copy(alpha = 0.22f)
                    },
                    RoundedCornerShape(16.dp),
                )
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(42.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(accent.copy(alpha = 0.28f), accent.copy(alpha = 0.09f)),
                            start = Offset.Zero,
                            end = Offset.Infinite,
                        ),
                        RoundedCornerShape(14.dp),
                    )
                    .border(1.dp, accent.copy(alpha = 0.30f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "OEM Adaptive",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.W800,
                            color = if (selected) accent else colors.onSurface,
                        ),
                    )
                    Spacer(Modifier.width(7.dp))
                    Box(
                        Modifier
                            .background(accent.copy(alpha = 0.11f), RoundedCornerShape(7.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "AUTO",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.sp,
                                fontWeight = FontWeight.W900,
                                letterSpacing = 0.45.sp,
                                color = accent,
                            ),
                        )
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = chargingProfileDescription(0),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 10.2.sp,
                        lineHeight = 10.2.sp * 1.2f,
                        color = colors.onSurfaceVariant,
                    ),
                )
            }
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .size(21.dp)
                    .background(
                        if (selected) accent else Color.Transparent,
                        RoundedCornerShape(99.dp),
                    )
                    .border(
                        1.5.dp,
                        if (selected) accent else colors.outline.copy(alpha = 0.55f),
                        RoundedCornerShape(99.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
    if (enabled) {
        PressScale(onTap = onTap) { tile() }
    } else {
        tile()
    }
}

private val chargingSpectrumProfiles = listOf(25, 33, 65, 85, 90)

@Composable
private fun ChargingPowerSpectrum(
    selected: Int,
    enabled: Boolean,
    onSelect: (Int) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier
            .fillMaxWidth()
            .background(colors.surfaceContainer.copy(alpha = 0.38f), RoundedCornerShape(16.dp))
            .border(1.dp, RodinSurfaceLines.inset(colors), RoundedCornerShape(16.dp))
            .padding(7.dp),
    ) {
        Column {
            Row {
                for (watts in chargingSpectrumProfiles.take(3)) {
                    Box(Modifier.weight(1f)) {
                        ChargingProfileTile(
                            watts = watts,
                            active = selected == watts,
                            enabled = enabled,
                            onSelect = onSelect,
                        )
                    }
                    if (watts != chargingSpectrumProfiles[2]) Spacer(Modifier.width(6.dp))
                }
            }
            Spacer(Modifier.height(6.dp))
            Row {
                for (watts in chargingSpectrumProfiles.drop(3)) {
                    Box(Modifier.weight(1f)) {
                        ChargingProfileTile(
                            watts = watts,
                            active = selected == watts,
                            enabled = enabled,
                            onSelect = onSelect,
                        )
                    }
                    if (watts != chargingSpectrumProfiles.last()) Spacer(Modifier.width(6.dp))
                }
            }
        }
    }
}

@Composable
private fun ChargingProfileTile(
    watts: Int,
    active: Boolean,
    enabled: Boolean,
    onSelect: (Int) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val accent = chargingProfileAccent(watts)
    val density = LocalDensity.current
    val tile: @Composable () -> Unit = {
        Column(
            Modifier
                .fillMaxWidth()
                .height(76.dp)
                .then(
                    if (active) {
                        Modifier.rodinShadow(
                            color = accent.copy(alpha = 0.16f),
                            blurRadius = 14f,
                            spreadRadius = -5f,
                            cornerRadius = with(density) { 13.dp.toPx() },
                        )
                    } else {
                        Modifier
                    },
                )
                .background(
                    Brush.linearGradient(
                        listOf(
                            if (active) {
                                accent.copy(alpha = 0.18f)
                            } else {
                                colors.surface.copy(alpha = 0.70f)
                            },
                            if (active) {
                                accent.copy(alpha = 0.07f)
                            } else {
                                colors.surfaceContainer.copy(alpha = 0.34f)
                            },
                        ),
                        start = Offset.Zero,
                        end = Offset.Infinite,
                    ),
                    RoundedCornerShape(13.dp),
                )
                .border(
                    if (active) 1.25.dp else 0.8.dp,
                    if (active) {
                        accent.copy(alpha = 0.72f)
                    } else {
                        colors.outline.copy(alpha = 0.20f)
                    },
                    RoundedCornerShape(13.dp),
                )
                .padding(horizontal = 9.dp, vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = chargingProfileIcon(watts),
                    contentDescription = null,
                    tint = if (active) accent else colors.onSurfaceVariant.copy(alpha = 0.78f),
                    modifier = Modifier.size(17.dp),
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = "$watts W",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 13.2.sp,
                        lineHeight = 13.2.sp,
                        fontWeight = FontWeight.W900,
                        color = if (active) accent else colors.onSurface,
                    ),
                )
            }
            Spacer(Modifier.height(7.dp))
            Text(
                text = chargingProfileName(watts),
                textAlign = TextAlign.Center,
                maxLines = 2,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.4.sp,
                    lineHeight = 9.4.sp * 1.08f,
                    fontWeight = if (active) FontWeight.W800 else FontWeight.W600,
                    color = if (active) accent else colors.onSurfaceVariant,
                ),
            )
        }
    }
    if (enabled) {
        PressScale(onTap = { onSelect(watts) }) { tile() }
    } else {
        tile()
    }
}

@Composable
private fun ChargingProfileDetail(watts: Int, applying: Boolean) {
    val colors = MaterialTheme.colorScheme
    val accent = chargingProfileAccent(watts)
    Row(
        Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    listOf(
                        accent.copy(alpha = 0.15f),
                        colors.surfaceContainer.copy(alpha = 0.40f),
                    ),
                    start = Offset.Zero,
                    end = Offset.Infinite,
                ),
                RoundedCornerShape(16.dp),
            )
            .border(1.dp, accent.copy(alpha = 0.28f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(38.dp)
                .background(accent.copy(alpha = 0.14f), RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (applying) Icons.Rounded.Sync else chargingProfileIcon(watts),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = if (applying) "Applying…" else chargingProfileName(watts),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.W800,
                    color = colors.onSurface,
                ),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = chargingProfileDescription(watts),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 10.2.sp,
                    lineHeight = 10.2.sp * 1.2f,
                    color = colors.onSurfaceVariant,
                ),
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .background(accent.copy(alpha = 0.12f), RoundedCornerShape(9.dp))
                .border(1.dp, accent.copy(alpha = 0.22f), RoundedCornerShape(9.dp))
                .padding(horizontal = 8.dp, vertical = 5.dp),
        ) {
            Text(
                text = if (watts == 0) "AUTO" else "$watts W MAX",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.W900,
                    letterSpacing = 0.35.sp,
                    color = accent,
                ),
            )
        }
    }
}
