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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Balance
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeveloperBoard
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.EnergySavingsLeaf
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

private val beastRed = Color(0xFFFF5252)
private val gamingAmber = Color(0xFFFFB84D)
private val batteryGreen = Color(0xFF35C997)
private val balancedBlue = Color(0xFF4EA8DE)

private fun maliMainColor(activePerf: Int, isUncapped: Boolean, minFreq: Int, maxFreq: Int): Color {
    val isBeast = activePerf == 3 || isUncapped || (minFreq == 1300 && maxFreq == 1300)
    val isGaming = !isBeast && activePerf == 1
    val isBattery = !isBeast && activePerf == 2
    return when {
        isBeast -> beastRed
        isGaming -> gamingAmber
        isBattery -> batteryGreen
        else -> balancedBlue
    }
}

/** Live Mali telemetry, GPU modes, GED boost, and frequency policy. */
@Composable
fun MaliGpuScreen(onBack: () -> Unit) {
    val snapshot = rememberRodinSnapshot()
    val colors = MaterialTheme.colorScheme
    val isDark = colors.surface.luminance() < 0.5f

    var optimisticProfile by remember { mutableStateOf<Int?>(null) }
    var optimisticUncap by remember { mutableStateOf<Boolean?>(null) }
    var optimisticMinFreq by remember { mutableStateOf<Int?>(null) }
    var optimisticMaxFreq by remember { mutableStateOf<Int?>(null) }
    var optimisticGov by remember { mutableStateOf<Int?>(null) }
    var optimisticGedBoost by remember { mutableStateOf<Boolean?>(null) }
    var optimisticPowerPolicy by remember { mutableStateOf<Int?>(null) }

    var toastTag by remember { mutableStateOf("MALI GPU") }
    var toastMessage by remember { mutableStateOf("") }
    var toastIcon by remember { mutableStateOf<ImageVector>(Icons.Rounded.SportsEsports) }
    var toastAccent by remember { mutableStateOf(beastRed) }
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
        delay(if (refreshKind == "preset" || refreshKind == "reset") 600L else 500L)
        RodinBackend.refresh()
        when (refreshKind) {
            "preset", "reset" -> {
                optimisticProfile = null
                optimisticUncap = null
                optimisticMinFreq = null
                optimisticMaxFreq = null
                optimisticGov = null
                optimisticGedBoost = null
                optimisticPowerPolicy = null
            }
            "policy" -> optimisticPowerPolicy = null
            "governor" -> optimisticGov = null
        }
    }

    fun showGlassToast(tag: String, message: String, icon: ImageVector, tint: Color) {
        toastTag = tag
        toastMessage = message
        toastIcon = icon
        toastAccent = tint
        toastVisible = true
        toastSeq += 1
    }

    fun applyPreset(
        profileCode: Int,
        min: Int,
        max: Int,
        gov: Int,
        ged: Boolean,
        powerPolicy: Int,
        name: String,
        icon: ImageVector,
        presetAccent: Color,
    ) {
        RodinHaptics.confirm()
        val willUncap = profileCode == 3

        optimisticProfile = profileCode
        optimisticMinFreq = min
        optimisticMaxFreq = max
        optimisticGov = gov
        optimisticGedBoost = ged
        optimisticPowerPolicy = powerPolicy
        optimisticUncap = willUncap

        showGlassToast("GPU PRESET", "Applied $name ($min–$max MHz)", icon, presetAccent)

        RodinBackend.setPerformanceProfile(profileCode)
        refreshKind = "preset"
        refreshSeq += 1
    }

    fun applyPowerPolicy(policyCode: Int) {
        RodinHaptics.confirm()
        optimisticPowerPolicy = policyCode

        val name = if (policyCode == 1) "Always-On (Zero Latency)" else "Coarse Demand (Dynamic)"
        showGlassToast(
            "MALI POWER POLICY",
            "Set Policy to $name",
            if (policyCode == 1) Icons.Rounded.Bolt else Icons.Rounded.Eco,
            if (policyCode == 1) beastRed else Color(0xFF67C2FF),
        )

        RodinBackend.setGpuPowerPolicy(policyCode)
        refreshKind = "policy"
        refreshSeq += 1
    }

    fun applyGovernor(govCode: Int) {
        RodinHaptics.confirm()
        optimisticGov = govCode

        val names = listOf(
            "Simple On-Demand",
            "Performance",
            "Powersave",
            "Userspace",
            "OEM Stock",
        )
        val name = if (govCode in names.indices) names[govCode] else names[0]

        showGlassToast(
            "DEVFREQ GOVERNOR",
            "Switched Governor to $name",
            Icons.Rounded.Tune,
            Color(0xFF67C2FF),
        )

        RodinBackend.setGpuDevfreqGovernor(govCode)
        refreshKind = "governor"
        refreshSeq += 1
    }

    fun resetDefault() {
        RodinHaptics.confirm()
        optimisticProfile = 0
        optimisticUncap = false
        optimisticMinFreq = 260
        optimisticMaxFreq = 1300
        optimisticGov = 4
        optimisticGedBoost = false
        optimisticPowerPolicy = 0

        showGlassToast(
            "GPU RESET",
            "Restored MediaTek Stock Balanced Settings",
            Icons.Rounded.Restore,
            Color(0xFF67C2FF),
        )

        RodinBackend.setPerformanceProfile(0)
        refreshKind = "reset"
        refreshSeq += 1
    }

    val liveLoad = if (RodinBackend.extendedValue(45) >= 0) RodinBackend.extendedValue(45) else 0
    val liveCurFreq = if (RodinBackend.extendedValue(46) >= 0) RodinBackend.extendedValue(46) else 0
    val rawMinFreq = if (RodinBackend.extendedValue(47) >= 0) RodinBackend.extendedValue(47) else 260
    val rawMaxFreq = if (RodinBackend.extendedValue(48) >= 0) RodinBackend.extendedValue(48) else 1300
    val rawGovCode = if (RodinBackend.extendedValue(49) >= 0) RodinBackend.extendedValue(49) else 0
    val rawGedBoost = if (RodinBackend.extendedValue(50) >= 0) RodinBackend.extendedValue(50) else 0
    val thermalState = RodinBackend.extendedValue(51)
    val rawUncap = RodinBackend.extendedValue(52)
    val rawPowerPolicy = if (RodinBackend.extendedValue(59) >= 0) {
        RodinBackend.extendedValue(59)
    } else {
        0
    }
    val profileVerified = optimisticProfile == null && RodinBackend.extendedValue(13) == 1

    val activeMinFreq = optimisticMinFreq ?: rawMinFreq
    val activeMaxFreq = optimisticMaxFreq ?: rawMaxFreq
    val activeGov = optimisticGov ?: rawGovCode
    val activeGedBoost = optimisticGedBoost ?: (rawGedBoost == 1)
    val activePowerPolicy = optimisticPowerPolicy ?: rawPowerPolicy
    val isUncapped = optimisticUncap ?: (rawUncap == 1)
    val activePerf = optimisticProfile
        ?: (if (snapshot.performanceProfile >= 0) snapshot.performanceProfile else 0)
    val requestedMaxFreq = when (activePerf) {
        3 -> 1300
        1 -> 1300
        2 -> 598
        else -> rawMaxFreq
    }
    val hasHardwareReadback = optimisticProfile == null
    val isThrottledByOs = hasHardwareReadback &&
        (
            (
                activePerf == 3 &&
                    ((liveCurFreq > 0 && liveCurFreq < 1300) || rawMaxFreq < requestedMaxFreq)
                ) ||
                (
                    (activePerf == 1 || activePerf == 2) &&
                        rawMaxFreq < requestedMaxFreq
                    )
            )
    val isBeast = activePerf == 3 ||
        isUncapped ||
        (activeMinFreq == 1300 && activeMaxFreq == 1300)
    val isGaming = !isBeast && activePerf == 1
    val isBattery = !isBeast && activePerf == 2
    val mainColor = maliMainColor(activePerf, isUncapped, activeMinFreq, activeMaxFreq)
    val modeIcon = when {
        isBeast -> Icons.Rounded.Bolt
        isGaming -> Icons.Rounded.SportsEsports
        isBattery -> Icons.Rounded.EnergySavingsLeaf
        else -> Icons.Rounded.Balance
    }
    val modeSummary = when {
        isBeast -> if (liveCurFreq > 0) {
            "Live: $liveCurFreq MHz · Fixed at 1.30 GHz"
        } else {
            "Applying fixed 1.30 GHz lock"
        }
        isGaming -> if (liveCurFreq > 0) {
            "Live: $liveCurFreq MHz · Scaling freely to 1.30 GHz"
        } else {
            "Full-range scaling · 260–1300 MHz"
        }
        isBattery -> if (liveCurFreq > 0) {
            "Live: $liveCurFreq MHz · Efficiency cap at 598 MHz"
        } else {
            "Efficiency range · 260–598 MHz"
        }
        else -> if (liveCurFreq > 0) {
            "Live: $liveCurFreq MHz · OEM-managed behavior"
        } else {
            "Vendor-managed clocks and power policy"
        }
    }

    Box(Modifier.fillMaxSize()) {
        RodinScrollPage(
            header = RodinPageHeader.Detail(
                title = "Mali GPU & GED",
                onBack = onBack,
                trailing = {
                    PressScale(onTap = { resetDefault() }) {
                        Box(
                            Modifier
                                .size(38.dp)
                                .background(
                                    if (isDark) {
                                        Color.White.copy(alpha = 0.08f)
                                    } else {
                                        Color.Black.copy(alpha = 0.05f)
                                    },
                                    CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Restore,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                },
            ),
        ) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Live Mali telemetry, GPU modes, GED boost, and frequency policy",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.5.sp,
                    color = colors.onSurfaceVariant,
                ),
            )
            Spacer(Modifier.height(10.dp))

            HeroCard(
                icon = modeIcon,
                accent = mainColor,
                title = "Mali-G720 Graphics",
                subtitle = modeSummary,
            )
            Spacer(Modifier.height(12.dp))

            MaliHeroDashboard(
                isDark = isDark,
                activeProfile = activePerf,
                liveLoad = liveLoad,
                liveCurFreq = liveCurFreq,
                minFreq = activeMinFreq,
                maxFreq = activeMaxFreq,
                isUncapped = isUncapped,
                isThrottledByOs = isThrottledByOs,
                profileVerified = profileVerified,
                oemManaged = snapshot.performanceProfile < 0 && optimisticProfile == null,
            )
            Spacer(Modifier.height(12.dp))

            Text(
                text = "GPU PERFORMANCE MODES",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.W800,
                    letterSpacing = 0.6.sp,
                    color = colors.onSurfaceVariant,
                ),
            )
            Spacer(Modifier.height(10.dp))
            Row {
                Box(Modifier.weight(1f)) {
                    MaliPresetCard(
                        isDark = isDark,
                        title = "Extreme Beast",
                        range = "1300 MHz fixed · No downclock",
                        icon = Icons.Rounded.Bolt,
                        accent = beastRed,
                        isActive = isBeast,
                        onTap = {
                            applyPreset(
                                3, 1300, 1300, 1, true, 1,
                                "Extreme Beast · Fixed 1.30 GHz",
                                Icons.Rounded.Bolt,
                                beastRed,
                            )
                        },
                    )
                }
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    MaliPresetCard(
                        isDark = isDark,
                        title = "Gaming Dynamic",
                        range = "260–1300 MHz · Full-range scaling",
                        icon = Icons.Rounded.SportsEsports,
                        accent = gamingAmber,
                        isActive = isGaming,
                        onTap = {
                            applyPreset(
                                1, 260, 1300, 0, true, 1,
                                "Gaming Dynamic · Full Range",
                                Icons.Rounded.SportsEsports,
                                gamingAmber,
                            )
                        },
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row {
                Box(Modifier.weight(1f)) {
                    MaliPresetCard(
                        isDark = isDark,
                        title = "Stock Balanced",
                        range = "OEM range · Vendor managed",
                        icon = Icons.Rounded.Balance,
                        accent = Color(0xFF67C2FF),
                        isActive = !isBeast && !isGaming && !isBattery,
                        onTap = {
                            applyPreset(
                                0, 260, 1300, 4, false, 0,
                                "Stock Balanced · OEM Control",
                                Icons.Rounded.Balance,
                                Color(0xFF67C2FF),
                            )
                        },
                    )
                }
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    MaliPresetCard(
                        isDark = isDark,
                        title = "Battery Saver",
                        range = "260–598 MHz · Efficiency cap",
                        icon = Icons.Rounded.EnergySavingsLeaf,
                        accent = Color(0xFF41C98A),
                        isActive = isBattery,
                        onTap = {
                            applyPreset(
                                2, 260, 598, 2, false, 0,
                                "Battery Saver · 598 MHz Cap",
                                Icons.Rounded.EnergySavingsLeaf,
                                Color(0xFF41C98A),
                            )
                        },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            SurfaceCard {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .background(beastRed.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                                .padding(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FlashOn,
                                contentDescription = null,
                                tint = beastRed,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "MediaTek GED Instant Boost",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.W700,
                                ),
                            )
                            Text(
                                text = if (isBeast || isGaming) {
                                    if (activeGedBoost) {
                                        "Active for this performance mode"
                                    } else {
                                        "Starting boost for this performance mode"
                                    }
                                } else {
                                    "Off in Stock Balanced and Battery Saver"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    color = colors.onSurfaceVariant,
                                ),
                            )
                        }
                        Box(
                            Modifier
                                .background(
                                    if (activeGedBoost) {
                                        beastRed.copy(alpha = 0.16f)
                                    } else {
                                        colors.onSurfaceVariant.copy(alpha = 0.10f)
                                    },
                                    RoundedCornerShape(999.dp),
                                )
                                .border(
                                    1.dp,
                                    if (activeGedBoost) {
                                        beastRed.copy(alpha = 0.55f)
                                    } else {
                                        colors.onSurfaceVariant.copy(alpha = 0.20f)
                                    },
                                    RoundedCornerShape(999.dp),
                                )
                                .padding(horizontal = 11.dp, vertical = 7.dp),
                        ) {
                            Text(
                                text = if (activeGedBoost) "ON" else "OFF",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.W900,
                                    letterSpacing = 0.5.sp,
                                    color = if (activeGedBoost) beastRed else colors.onSurfaceVariant,
                                ),
                            )
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        thickness = 1.dp,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "MALI POWER POLICY (CSF DRIVER)",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.W800,
                                letterSpacing = 0.6.sp,
                                color = colors.onSurfaceVariant,
                            ),
                        )
                        Text(
                            text = if (activePowerPolicy == 1) "Zero Latency" else "Dynamic Sleep",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.W700,
                                color = if (activePowerPolicy == 1) beastRed else Color(0xFF41C98A),
                            ),
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Controls GPU shader core power domain gating and idle sleep states",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 10.5.sp,
                            color = colors.onSurfaceVariant,
                        ),
                    )
                    Spacer(Modifier.height(8.dp))
                    Row {
                        Box(Modifier.weight(1f)) {
                            MaliPolicySegment(
                                isDark = isDark,
                                label = "Coarse Demand",
                                subtitle = "Dynamic Sleep Gating",
                                icon = Icons.Rounded.Eco,
                                code = 0,
                                isSelected = activePowerPolicy == 0,
                                selectedAccent = Color(0xFF67C2FF),
                                onSelect = { code -> applyPowerPolicy(code) },
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.weight(1f)) {
                            MaliPolicySegment(
                                isDark = isDark,
                                label = "Always On",
                                subtitle = "0 Latency · No Sleep",
                                icon = Icons.Rounded.Bolt,
                                code = 1,
                                isSelected = activePowerPolicy == 1,
                                selectedAccent = beastRed,
                                onSelect = { code -> applyPowerPolicy(code) },
                            )
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        thickness = 1.dp,
                    )
                    Text(
                        text = "DEVFREQ GOVERNOR",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.W800,
                            letterSpacing = 0.6.sp,
                            color = colors.onSurfaceVariant,
                        ),
                    )
                    Spacer(Modifier.height(10.dp))
                    Row {
                        Box(Modifier.weight(1f)) {
                            MaliGovSegment(
                                isDark = isDark,
                                label = "On-Demand",
                                subtitle = "Load-responsive scaling",
                                icon = Icons.Rounded.Speed,
                                selectedAccent = gamingAmber,
                                code = 0,
                                isSelected = activeGov == 0,
                                onSelect = { code -> applyGovernor(code) },
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.weight(1f)) {
                            MaliGovSegment(
                                isDark = isDark,
                                label = "Performance",
                                subtitle = "Maximum-clock priority",
                                icon = Icons.Rounded.Bolt,
                                selectedAccent = beastRed,
                                code = 1,
                                isSelected = activeGov == 1,
                                onSelect = { code -> applyGovernor(code) },
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row {
                        Box(Modifier.weight(1f)) {
                            MaliGovSegment(
                                isDark = isDark,
                                label = "Powersave",
                                subtitle = "Lowest-clock priority",
                                icon = Icons.Rounded.Eco,
                                selectedAccent = Color(0xFF41C98A),
                                code = 2,
                                isSelected = activeGov == 2,
                                onSelect = { code -> applyGovernor(code) },
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.weight(1f)) {
                            MaliGovSegment(
                                isDark = isDark,
                                label = "Userspace",
                                subtitle = "Manual frequency control",
                                icon = Icons.Rounded.Tune,
                                selectedAccent = Color(0xFFA78BFA),
                                code = 3,
                                isSelected = activeGov == 3,
                                onSelect = { code -> applyGovernor(code) },
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    MaliGovSegment(
                        isDark = isDark,
                        label = "OEM Stock",
                        subtitle = "Vendor default governor behavior",
                        icon = Icons.Rounded.Memory,
                        selectedAccent = Color(0xFF67C2FF),
                        code = 4,
                        isSelected = activeGov == 4,
                        onSelect = { code -> applyGovernor(code) },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            SurfaceCard {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.DeveloperBoard,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "MediaTek Mali Architecture",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.W800,
                            ),
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    MaliDetailRow(colors, "GPU Architecture", "Mali-G720 7-Core r0p1 (0x0C080700)")
                    MaliDetailRow(colors, "Active Core Mask", "0x150055 (All 7 execution clusters active)")
                    MaliDetailRow(colors, "Thermal Cooling Node", "/sys/class/thermal/cooling_device3")
                    MaliDetailRow(
                        colors,
                        "Thermal Controller State",
                        when {
                            thermalState < 0 -> "Unavailable"
                            thermalState == 0 -> "0 (Rodin Essential unrestricted override)"
                            else -> "$thermalState (vendor managed)"
                        },
                    )
                    MaliDetailRow(colors, "GED Boost Subsystem", "/sys/module/ged/parameters/")
                    MaliDetailRow(colors, "Hardware Frequencies", "41 Clock Steps (260 MHz → 1.30 GHz)")
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
private fun MaliHeroDashboard(
    isDark: Boolean,
    activeProfile: Int,
    liveLoad: Int,
    liveCurFreq: Int,
    minFreq: Int,
    maxFreq: Int,
    isUncapped: Boolean,
    isThrottledByOs: Boolean,
    profileVerified: Boolean,
    oemManaged: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    val isBeast = activeProfile == 3 ||
        isUncapped ||
        (minFreq == 1300 && maxFreq == 1300)
    val isGaming = !isBeast && activeProfile == 1
    val isBattery = !isBeast && activeProfile == 2
    val mainColor = maliMainColor(activeProfile, isUncapped, minFreq, maxFreq)

    val activeDisplayClock = if (liveCurFreq > 0) "$liveCurFreq" else "—"
    val clockSubtext = when {
        isBeast -> when {
            isThrottledByOs -> "External clock limit detected · 1300 MHz remains the target"
            profileVerified && liveCurFreq == 1300 ->
                "Fixed 1.30 GHz target verified from live hardware telemetry"
            else -> "Fixed 1.30 GHz target requested · waiting for live verification"
        }
        isGaming -> if (liveCurFreq > 0) {
            "Dynamic Gaming Load ($liveCurFreq MHz · 1.30 GHz Ceiling)"
        } else {
            "Standby Dynamic (260 – 1300 MHz · GED Boost Active)"
        }
        isBattery -> if (liveCurFreq > 0) {
            "Live $liveCurFreq MHz · $minFreq–$maxFreq MHz Battery Range"
        } else {
            "Waiting for battery-profile GPU telemetry"
        }
        else -> if (liveCurFreq > 0) {
            "Active OEM OPP ($liveCurFreq MHz)"
        } else {
            "Waiting for OEM GPU telemetry"
        }
    }
    val badgeTitle = when {
        isBeast -> "1.30 GHz FIXED TARGET"
        isGaming -> "GAMING DYNAMIC BOOST"
        isBattery -> "BATTERY SAVER CLAMP"
        else -> "DYNAMIC BALANCED"
    }
    val loadColor = when {
        liveLoad > 80 -> beastRed
        liveLoad > 40 -> gamingAmber
        else -> Color(0xFF41C98A)
    }

    SurfaceCard(accent = mainColor) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(42.dp)
                        .background(mainColor.copy(alpha = 0.16f), CircleShape)
                        .border(1.2.dp, mainColor.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isBeast) Icons.Rounded.Bolt else Icons.Rounded.SportsEsports,
                        contentDescription = null,
                        tint = mainColor,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = badgeTitle,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.W800,
                            letterSpacing = 0.3.sp,
                            color = mainColor,
                        ),
                    )
                    Text(
                        text = "Dimensity 8400-Ultra · Mali-G720 7-Core",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.5.sp,
                            color = colors.onSurfaceVariant,
                        ),
                    )
                }
                if (maxFreq >= 1300) {
                    Box(
                        Modifier
                            .background(beastRed.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                            .border(1.dp, beastRed.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text(
                            text = "1.30 GHz Target",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.W800,
                                color = beastRed,
                            ),
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            Row {
                Text(
                    text = activeDisplayClock,
                    modifier = Modifier.alignByBaseline(),
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontSize = 42.sp,
                        lineHeight = 42.sp,
                        fontWeight = FontWeight.W900,
                        letterSpacing = (-1.0).sp,
                        color = colors.onSurface,
                    ),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "MHz",
                    modifier = Modifier.alignByBaseline(),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.W700,
                        color = mainColor,
                    ),
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = clockSubtext,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.W600,
                    color = colors.onSurfaceVariant,
                ),
            )
            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "LIVE HARDWARE UTILIZATION",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.W800,
                        letterSpacing = 0.6.sp,
                        color = colors.onSurfaceVariant,
                    ),
                )
                Text(
                    text = "$liveLoad%",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.W800,
                        color = loadColor,
                    ),
                )
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { liveLoad.coerceIn(0, 100) / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = loadColor,
                trackColor = if (isDark) {
                    Color.White.copy(alpha = 0.08f)
                } else {
                    Color.Black.copy(alpha = 0.06f)
                },
            )
            Spacer(Modifier.height(16.dp))

            Row {
                Box(Modifier.weight(1f)) {
                    MaliTelemetryPill(
                        isDark = isDark,
                        label = "PROFILE WRITE",
                        value = when {
                            oemManaged -> "OEM Managed"
                            isThrottledByOs -> "Clock Below Target"
                            profileVerified -> "Verified"
                            else -> "Applying"
                        },
                        accent = when {
                            oemManaged -> colors.onSurfaceVariant
                            isThrottledByOs || !profileVerified -> gamingAmber
                            else -> Color(0xFF41C98A)
                        },
                    )
                }
                Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(1f)) {
                    MaliTelemetryPill(
                        isDark = isDark,
                        label = "ACTIVE RANGE",
                        value = "$minFreq – $maxFreq MHz",
                        accent = mainColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun MaliTelemetryPill(
    isDark: Boolean,
    label: String,
    value: String,
    accent: Color,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxWidth()
            .background(
                if (isDark) Color(0xFF171C28) else Color(0xFFEEF2F9),
                RoundedCornerShape(14.dp),
            )
            .border(
                1.dp,
                if (isDark) {
                    Color.White.copy(alpha = 0.06f)
                } else {
                    Color.Black.copy(alpha = 0.04f)
                },
                RoundedCornerShape(14.dp),
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.W800,
                letterSpacing = 0.4.sp,
                color = colors.onSurfaceVariant,
            ),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.W700,
                color = accent,
            ),
        )
    }
}

@Composable
private fun MaliPresetCard(
    isDark: Boolean,
    title: String,
    range: String,
    icon: ImageVector,
    accent: Color,
    isActive: Boolean,
    onTap: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    PressScale(onTap = onTap) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 108.dp)
                .background(
                    if (isDark) Color(0xFF141822) else Color.White,
                    RoundedCornerShape(16.dp),
                )
                .border(
                    if (isActive) 1.5.dp else 1.dp,
                    if (isActive) {
                        accent
                    } else {
                        if (isDark) {
                            Color.White.copy(alpha = 0.08f)
                        } else {
                            Color.Black.copy(alpha = 0.06f)
                        }
                    },
                    RoundedCornerShape(16.dp),
                )
                .padding(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    imageVector = if (isActive) {
                        Icons.Rounded.CheckCircle
                    } else {
                        Icons.Rounded.RadioButtonUnchecked
                    },
                    contentDescription = null,
                    tint = if (isActive) accent else colors.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.W700,
                    color = colors.onSurface,
                ),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = range,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    lineHeight = 10.sp * 1.25f,
                    color = colors.onSurfaceVariant,
                ),
            )
        }
    }
}

@Composable
private fun MaliPolicySegment(
    isDark: Boolean,
    label: String,
    subtitle: String,
    icon: ImageVector,
    code: Int,
    isSelected: Boolean,
    selectedAccent: Color,
    onSelect: (Int) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    PressScale(onTap = { onSelect(code) }) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(
                    if (isSelected) {
                        selectedAccent.copy(alpha = 0.12f)
                    } else {
                        if (isDark) {
                            Color.White.copy(alpha = 0.04f)
                        } else {
                            Color.Black.copy(alpha = 0.03f)
                        }
                    },
                    RoundedCornerShape(12.dp),
                )
                .border(
                    1.2.dp,
                    if (isSelected) selectedAccent else Color.Transparent,
                    RoundedCornerShape(12.dp),
                )
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) selectedAccent else colors.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 11.5.sp,
                        fontWeight = if (isSelected) FontWeight.W800 else FontWeight.W600,
                        color = if (isSelected) selectedAccent else colors.onSurface,
                    ),
                )
                Text(
                    text = subtitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.5.sp,
                        color = colors.onSurfaceVariant,
                    ),
                )
            }
        }
    }
}

@Composable
private fun MaliGovSegment(
    isDark: Boolean,
    label: String,
    subtitle: String,
    icon: ImageVector,
    selectedAccent: Color,
    code: Int,
    isSelected: Boolean,
    onSelect: (Int) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    PressScale(onTap = { onSelect(code) }) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .background(
                    if (isSelected) {
                        selectedAccent.copy(alpha = 0.14f)
                    } else {
                        if (isDark) {
                            Color.White.copy(alpha = 0.04f)
                        } else {
                            Color.Black.copy(alpha = 0.03f)
                        }
                    },
                    RoundedCornerShape(12.dp),
                )
                .border(
                    if (isSelected) 1.4.dp else 1.dp,
                    when {
                        isSelected -> selectedAccent
                        isDark -> Color.White.copy(alpha = 0.05f)
                        else -> Color.Black.copy(alpha = 0.04f)
                    },
                    RoundedCornerShape(12.dp),
                )
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(30.dp)
                    .background(
                        selectedAccent.copy(alpha = if (isSelected) 0.18f else 0.08f),
                        RoundedCornerShape(9.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) selectedAccent else colors.onSurfaceVariant,
                    modifier = Modifier.size(17.dp),
                )
            }
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.W800 else FontWeight.W700,
                        color = if (isSelected) selectedAccent else colors.onSurface,
                    ),
                )
                Spacer(Modifier.height(1.dp))
                Text(
                    text = subtitle,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        lineHeight = 9.sp * 1.15f,
                        color = colors.onSurfaceVariant,
                    ),
                )
            }
        }
    }
}

@Composable
private fun MaliDetailRow(
    colors: androidx.compose.material3.ColorScheme,
    label: String,
    value: String,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                color = colors.onSurfaceVariant,
            ),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.W600,
                color = colors.onSurface,
            ),
        )
    }
}
