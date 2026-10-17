package io.github.neeschal.rodinessential.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.Balance
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageVector
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.neeschal.rodinessential.backend.RodinBackend
import io.github.neeschal.rodinessential.backend.RodinBackendSnapshot
import io.github.neeschal.rodinessential.ui.components.PressScale
import io.github.neeschal.rodinessential.ui.components.RodinHaptics
import io.github.neeschal.rodinessential.ui.components.StatusPill
import io.github.neeschal.rodinessential.ui.components.SurfaceCard
import io.github.neeschal.rodinessential.ui.components.rodinShadow
import io.github.neeschal.rodinessential.ui.shell.RodinFeedbackOverlay
import io.github.neeschal.rodinessential.ui.shell.RodinInteractionSettings
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt

private const val CPU_ACCENT_EFFICIENCY = 0xFF4EA8DE
private const val CPU_ACCENT_PERFORMANCE = 0xFFA066FF
private const val CPU_ACCENT_PRIME = 0xFFFF8E3C

private fun cpuFreqLabel(value: Long): String {
    if (value <= 0) return "—"
    if (value >= 1000000L) return String.format("%.2f", value / 1000000.0) + " GHz"
    return Math.round(value / 1000.0).toString() + " MHz"
}

private fun cpuCoreMask(value: Int): String {
    if (value < 0) return "Unknown"
    val low = value and 0xFF
    return "0x" + low.toString(16).uppercase().padStart(2, '0')
}

/** CPU core and frequency control with live telemetry and the glass toast. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CpuControlScreen(onBack: () -> Unit) {
    val s = rememberRodinSnapshot()
    val colors = MaterialTheme.colorScheme
    val isDark = colors.surface.luminance() < 0.5f

    var toastTag by remember { mutableStateOf("") }
    var toastMessage by remember { mutableStateOf("") }
    var toastIcon by remember { mutableStateOf<ImageVector>(Icons.Outlined.Info) }
    var toastAccent by remember { mutableStateOf(Color(0xFF67C2FF)) }
    var toastVisible by remember { mutableStateOf(false) }
    var toastSeq by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        RodinBackend.refresh()
    }
    LaunchedEffect(toastSeq) {
        if (toastSeq > 0) {
            delay(2400)
            toastVisible = false
        }
    }

    fun showGlassToast(
        tag: String,
        message: String,
        icon: ImageVector,
        accent: Color,
    ) {
        toastTag = tag
        toastMessage = message
        toastIcon = icon
        toastAccent = accent
        toastVisible = true
        toastSeq += 1
    }

    fun applyCorePreset(name: String, onlineCores: List<Int>, accent: Color) {
        RodinHaptics.confirm()
        RodinBackend.setCpuManualMode(true)
        for (i in 1..7) {
            RodinBackend.setCpuCoreOnline(i, onlineCores.contains(i))
        }
        showGlassToast(
            "CORE PRESET APPLIED",
            "$name (${onlineCores.size} Cores Active)",
            Icons.Rounded.Memory,
            accent,
        )
    }

    val manual = RodinBackend.extendedValue(34)
    val writeAck = RodinBackend.extendedValue(35)
    val savedMask = RodinBackend.extendedValue(36)
    val coreCtlNodes = RodinBackend.extendedValue(37)
    val frequencyWriteAck = RodinBackend.extendedValue(74)
    val canWrite = s.controlsAvailable && !s.busy

    fun clusterCurrentMhz(cores: List<Int>): Int {
        var current = -1
        for (cpu in cores) {
            if (s.cpuOnline(cpu) && s.cpuFreqKhz[cpu] > 0) {
                val mhz = (s.cpuFreqKhz[cpu] / 1000).toInt()
                if (mhz > current) current = mhz
            }
        }
        return current
    }

    var efficiencyOnline = 0
    for (c in 0..3) if (s.cpuOnline(c)) efficiencyOnline++
    var performanceOnline = 0
    for (c in 4..6) if (s.cpuOnline(c)) performanceOnline++
    val primeOnline = if (s.cpuOnline(7)) 1 else 0
    val totalOnline = efficiencyOnline + performanceOnline + primeOnline

    Box(Modifier.fillMaxSize()) {
        RodinScrollPage(
            header = RodinPageHeader.Detail(
                title = "CPU Core & Frequency",
                onBack = onBack,
            ),
        ) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Per-cluster frequency ranges, exact locks, and processor core control",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.5.sp,
                    color = colors.onSurfaceVariant,
                ),
            )
            Spacer(Modifier.height(12.dp))

            SurfaceCard {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                if (isDark) {
                                    listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                                } else {
                                    listOf(Color(0xFFEFF6FF), Color(0xFFDBEAFE))
                                },
                            ),
                            RoundedCornerShape(16.dp),
                        )
                        .padding(16.dp),
                ) {
                    Column(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "CPU TOPOLOGY",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    letterSpacing = 1.sp,
                                    fontWeight = FontWeight.W700,
                                    color = colors.onSurfaceVariant,
                                ),
                            )
                            Spacer(Modifier.width(12.dp))
                            Box(Modifier.widthIn(max = 120.dp)) {
                                HomeLiveBadge(snapshot = s)
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .background(
                                        Color(0xFF38BDF8).copy(alpha = 0.15f),
                                        RoundedCornerShape(10.dp),
                                    )
                                    .padding(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Memory,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = "Dimensity 8400-Ultra CPU",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.W800,
                                        color = colors.onSurface,
                                    ),
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "$totalOnline / 8 cores online",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.W500,
                                        color = colors.onSurfaceVariant,
                                    ),
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Row {
                            HeroStat(
                                modifier = Modifier.weight(1f),
                                label = "EFFICIENCY (4C)",
                                value = "$efficiencyOnline / 4 Online",
                                accent = Color(0xFF4EA8DE),
                                sub = "4× Cortex-A725",
                            )
                            Spacer(Modifier.width(8.dp))
                            HeroStat(
                                modifier = Modifier.weight(1f),
                                label = "PERFORMANCE (3C)",
                                value = "$performanceOnline / 3 Online",
                                accent = Color(0xFFA066FF),
                                sub = "3× Cortex-A725",
                            )
                            Spacer(Modifier.width(8.dp))
                            HeroStat(
                                modifier = Modifier.weight(1f),
                                label = "PRIME (1C)",
                                value = "$primeOnline / 1 Online",
                                accent = Color(0xFFFF8E3C),
                                sub = "1× Cortex-A725",
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            Text(
                text = "CPU FREQUENCY CONTROL",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.W800,
                    letterSpacing = 0.6.sp,
                    color = colors.onSurfaceVariant,
                ),
            )
            Spacer(Modifier.height(9.dp))
            CpuFrequencyCard(
                policy = 0,
                title = "Efficiency Cluster",
                subtitle = "4× Cortex-A725 · Cores 0–3 · 256KB L2",
                accent = Color(CPU_ACCENT_EFFICIENCY),
                currentMhz = clusterCurrentMhz(listOf(0, 1, 2, 3)),
                targetMinMhz = RodinBackend.cpuTargetMinFrequency(0),
                targetMaxMhz = RodinBackend.cpuTargetMaxFrequency(0),
                liveMinMhz = RodinBackend.cpuLiveMinFrequency(0),
                liveMaxMhz = RodinBackend.cpuLiveMaxFrequency(0),
                availableMhz = RodinBackend.cpuAvailableFrequencies(0),
                governorCode = s.cpuGovernor0,
                writeAck = frequencyWriteAck,
                drift = RodinBackend.cpuFrequencyDrift(0),
                enabled = canWrite,
                onFeedback = { tag, message, icon, accent ->
                    showGlassToast(tag, message, icon, accent)
                },
            )
            Spacer(Modifier.height(9.dp))
            CpuFrequencyCard(
                policy = 4,
                title = "Performance Cluster",
                subtitle = "3× Cortex-A725 · Cores 4–6 · 512KB L2",
                accent = Color(CPU_ACCENT_PERFORMANCE),
                currentMhz = clusterCurrentMhz(listOf(4, 5, 6)),
                targetMinMhz = RodinBackend.cpuTargetMinFrequency(4),
                targetMaxMhz = RodinBackend.cpuTargetMaxFrequency(4),
                liveMinMhz = RodinBackend.cpuLiveMinFrequency(4),
                liveMaxMhz = RodinBackend.cpuLiveMaxFrequency(4),
                availableMhz = RodinBackend.cpuAvailableFrequencies(4),
                governorCode = s.cpuGovernor4,
                writeAck = frequencyWriteAck,
                drift = RodinBackend.cpuFrequencyDrift(4),
                enabled = canWrite,
                onFeedback = { tag, message, icon, accent ->
                    showGlassToast(tag, message, icon, accent)
                },
            )
            Spacer(Modifier.height(9.dp))
            CpuFrequencyCard(
                policy = 7,
                title = "Prime Core",
                subtitle = "1× Cortex-A725 · Core 7 · 1MB L2",
                accent = Color(CPU_ACCENT_PRIME),
                currentMhz = clusterCurrentMhz(listOf(7)),
                targetMinMhz = RodinBackend.cpuTargetMinFrequency(7),
                targetMaxMhz = RodinBackend.cpuTargetMaxFrequency(7),
                liveMinMhz = RodinBackend.cpuLiveMinFrequency(7),
                liveMaxMhz = RodinBackend.cpuLiveMaxFrequency(7),
                availableMhz = RodinBackend.cpuAvailableFrequencies(7),
                governorCode = s.cpuGovernor7,
                writeAck = frequencyWriteAck,
                drift = RodinBackend.cpuFrequencyDrift(7),
                enabled = canWrite,
                onFeedback = { tag, message, icon, accent ->
                    showGlassToast(tag, message, icon, accent)
                },
            )
            Spacer(Modifier.height(10.dp))
            SurfaceCard {
                InfoBlock(
                    title = "How to use CPU frequency control",
                    text = "Choose Dynamic Range to set the minimum and maximum, or " +
                        "Exact Lock to make both limits the same. Drag to a supported " +
                        "clock and tap Apply. Live Limits always shows the range " +
                        "currently enforced by the kernel. Each card shows the current " +
                        "governor live; governor selection remains in Advanced " +
                        "Configuration. OEM Reset clears the saved target and returns " +
                        "that cluster to ROM-managed limits.",
                )
            }
            Spacer(Modifier.height(12.dp))

            RodinSwitchCard(
                icon = Icons.Rounded.Tune,
                accent = Color(0xFF67C2FF),
                title = "Manual core control",
                subtitle = if (manual == 1) {
                    "Manual hotplug active · cores stay at toggled states"
                } else {
                    "Enable to manually power on/off individual processor cores"
                },
                value = manual == 1,
                enabled = canWrite,
                stateKnown = manual >= 0,
                onChanged = { value -> RodinBackend.setCpuManualMode(value) },
            )
            Spacer(Modifier.height(12.dp))

            Column {
                Text(
                    text = "QUICK CORE CONFIGURATIONS",
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
                        QuickCoreButton(
                            isDark = isDark,
                            title = "All Cores On",
                            subtitle = "8/8 Cores Online",
                            icon = Icons.Rounded.FlashOn,
                            accent = Color(0xFF00E676),
                            onTap = {
                                applyCorePreset(
                                    "All 8 Cores Online",
                                    listOf(0, 1, 2, 3, 4, 5, 6, 7),
                                    Color(0xFF00E676),
                                )
                            },
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.weight(1f)) {
                        QuickCoreButton(
                            isDark = isDark,
                            title = "Balanced",
                            subtitle = "7/8 · Prime Core Off",
                            icon = Icons.Rounded.Balance,
                            accent = Color(0xFF4EA8DE),
                            onTap = {
                                applyCorePreset(
                                    "Balanced 7-Core Mode",
                                    listOf(0, 1, 2, 3, 4, 5, 6),
                                    Color(0xFF4EA8DE),
                                )
                            },
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.weight(1f)) {
                        QuickCoreButton(
                            isDark = isDark,
                            title = "Power Saver",
                            subtitle = "Efficiency Cores 0–3",
                            icon = Icons.Rounded.Eco,
                            accent = Color(0xFF35C997),
                            onTap = {
                                applyCorePreset(
                                    "Eco 4-Core Mode",
                                    listOf(0, 1, 2, 3),
                                    Color(0xFF35C997),
                                )
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            CpuClusterCard(
                title = "Efficiency Cluster",
                subtitle = "4× Cortex-A725 · Cores 0–3 · $efficiencyOnline/4 Online",
                accent = Color(CPU_ACCENT_EFFICIENCY),
                cores = listOf(0, 1, 2, 3),
                snapshot = s,
                manual = manual,
                canWrite = canWrite,
                masterCore = 0,
            )
            Spacer(Modifier.height(12.dp))
            CpuClusterCard(
                title = "Performance Cluster",
                subtitle = "3× Cortex-A725 · Cores 4–6 · $performanceOnline/3 Online",
                accent = Color(CPU_ACCENT_PERFORMANCE),
                cores = listOf(4, 5, 6),
                snapshot = s,
                manual = manual,
                canWrite = canWrite,
                masterCore = -1,
            )
            Spacer(Modifier.height(12.dp))
            CpuClusterCard(
                title = "Prime Core",
                subtitle = "1× Cortex-A725 · Core 7 · $primeOnline/1 Online",
                accent = Color(CPU_ACCENT_PRIME),
                cores = listOf(7),
                snapshot = s,
                manual = manual,
                canWrite = canWrite,
                masterCore = -1,
            )
            Spacer(Modifier.height(12.dp))

            SurfaceCard {
                Column {
                    DiagnosticRow(
                        label = "Control Mode",
                        good = manual == 1,
                        detail = if (manual == 1) "Manual hotplug control" else "Automatic balance",
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 7.dp),
                        thickness = 0.8.dp,
                    )
                    DiagnosticRow(
                        label = "Frequency Target",
                        good = frequencyWriteAck != 0,
                        detail = when (frequencyWriteAck) {
                            1 -> "Last request verified"
                            0 -> "Last request failed"
                            else -> "No request yet"
                        },
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 7.dp),
                        thickness = 0.8.dp,
                    )
                    DiagnosticRow(
                        label = "Core State",
                        good = writeAck == 1,
                        detail = when (writeAck) {
                            1 -> "Applied"
                            0 -> "Failed"
                            else -> "Checking..."
                        },
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 7.dp),
                        thickness = 0.8.dp,
                    )
                    DiagnosticRow(
                        label = "Active Core Mask",
                        good = savedMask >= 0,
                        detail = cpuCoreMask(savedMask),
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 7.dp),
                        thickness = 0.8.dp,
                    )
                    DiagnosticRow(
                        label = "Processor Clusters",
                        good = coreCtlNodes >= 0,
                        detail = if (coreCtlNodes >= 0) {
                            "$coreCtlNodes clusters active"
                        } else {
                            "Unknown"
                        },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            SurfaceCard {
                InfoBlock(
                    title = "Processor Core Safety",
                    text = "Primary core (CPU0) remains permanently active to ensure " +
                        "kernel and essential background services run smoothly. Core " +
                        "states persist across device reboots.",
                )
            }
            Spacer(Modifier.height(80.dp))
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
private fun HeroStat(
    label: String,
    value: String,
    accent: Color,
    sub: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .background(accent.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
            .border(1.dp, accent.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(5.dp)
                    .background(accent, RoundedCornerShape(99.dp)),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.W800,
                    color = accent,
                    letterSpacing = 0.4.sp,
                ),
            )
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.W900,
            ),
        )
        Spacer(Modifier.height(1.dp))
        Text(
            text = sub,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.5.sp,
                fontWeight = FontWeight.W600,
                color = accent.copy(alpha = 0.9f),
            ),
        )
    }
}

@Composable
private fun QuickCoreButton(
    isDark: Boolean,
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onTap: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    PressScale(onTap = onTap) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(
                    accent.copy(alpha = if (isDark) 0.12f else 0.08f),
                    RoundedCornerShape(12.dp),
                )
                .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 10.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.W800,
                    color = colors.onSurface,
                ),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.W500,
                    color = colors.onSurfaceVariant,
                ),
            )
        }
    }
}

@Composable
private fun CpuClusterCard(
    title: String,
    subtitle: String,
    accent: Color,
    cores: List<Int>,
    snapshot: RodinBackendSnapshot,
    manual: Int,
    canWrite: Boolean,
    masterCore: Int,
) {
    val colors = MaterialTheme.colorScheme
    SurfaceCard {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(10.dp)
                        .background(accent, RoundedCornerShape(99.dp)),
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.W800,
                            color = colors.onSurface,
                        ),
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = colors.onSurfaceVariant,
                        ),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(thickness = 1.dp)
            Spacer(Modifier.height(6.dp))
            cores.forEachIndexed { index, cpu ->
                val online = snapshot.cpuOnline(cpu)
                CpuStatusRow(
                    cpu = cpu,
                    online = online,
                    frequency = if (online) cpuFreqLabel(snapshot.cpuFreqKhz[cpu]) else "Off",
                    accent = accent,
                    clusterTag = when {
                        cpu == masterCore -> "MASTER"
                        masterCore == 0 && cpu in 1..3 -> "EFFICIENCY"
                        cpu in 4..6 -> "PERFORMANCE"
                        else -> "PRIME"
                    },
                    toggleEnabled = cpu != masterCore && manual == 1 && canWrite,
                    onChanged = if (cpu == masterCore) {
                        null
                    } else {
                        { value -> RodinBackend.setCpuCoreOnline(cpu, value) }
                    },
                )
                if (index != cores.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 5.dp),
                        thickness = 0.8.dp,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CpuFrequencyCard(
    policy: Int,
    title: String,
    subtitle: String,
    accent: Color,
    currentMhz: Int,
    targetMinMhz: Int,
    targetMaxMhz: Int,
    liveMinMhz: Int,
    liveMaxMhz: Int,
    availableMhz: List<Int>,
    governorCode: Int,
    writeAck: Int,
    drift: Int,
    enabled: Boolean,
    onFeedback: (String, String, ImageVector, Color) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var minimumMhz by remember { mutableIntStateOf(-1) }
    var maximumMhz by remember { mutableIntStateOf(-1) }
    var exactLock by remember { mutableStateOf(false) }
    var dirty by remember { mutableStateOf(false) }
    var sliderDragging by remember { mutableStateOf(false) }

    fun nearestAvailable(requested: Int): Int {
        if (availableMhz.isEmpty()) return -1
        if (requested <= 0) return availableMhz.first()
        var nearest = availableMhz.first()
        var distance = abs(nearest - requested)
        for (frequency in availableMhz.drop(1)) {
            val nextDistance = abs(frequency - requested)
            if (nextDistance < distance) {
                nearest = frequency
                distance = nextDistance
            }
        }
        return nearest
    }

    LaunchedEffect(availableMhz, targetMinMhz, targetMaxMhz, liveMinMhz, liveMaxMhz, dirty) {
        if (availableMhz.isEmpty() || dirty) return@LaunchedEffect
        val custom = targetMinMhz > 0 && targetMaxMhz > 0
        val requestedMin = if (custom) targetMinMhz else liveMinMhz
        val requestedMax = if (custom) targetMaxMhz else liveMaxMhz
        var nextMin = nearestAvailable(requestedMin)
        var nextMax = nearestAvailable(requestedMax)
        if (nextMin > nextMax) {
            val swap = nextMin
            nextMin = nextMax
            nextMax = swap
        }
        minimumMhz = nextMin
        maximumMhz = nextMax
        exactLock = custom && nextMin == nextMax
    }

    fun frequencyIndex(frequency: Int): Int {
        val exact = availableMhz.indexOf(frequency)
        if (exact >= 0) return exact
        return availableMhz.indexOf(nearestAvailable(frequency))
    }

    fun frequencyLabel(mhz: Int): String {
        if (mhz <= 0) return "—"
        if (mhz >= 1000) {
            val hundredths = (mhz * 100) / 1000
            val digits = if (hundredths % 10 == 0) 1 else 2
            return String.format("%.${digits}f", hundredths / 100.0) + " GHz"
        }
        return "$mhz MHz"
    }

    fun governorLabel(): String = when (governorCode) {
        0 -> "sugov_ext"
        1 -> "conservative"
        2 -> "powersave"
        3 -> "performance"
        4 -> "schedutil"
        else -> "Detecting"
    }

    fun setExactLock(lock: Boolean) {
        if (exactLock == lock || availableMhz.isEmpty()) return
        RodinHaptics.segment()
        exactLock = lock
        if (lock) {
            val target = if (maximumMhz > 0) maximumMhz else availableMhz.last()
            minimumMhz = target
            maximumMhz = target
        } else if (minimumMhz == maximumMhz) {
            minimumMhz = availableMhz.first()
        }
        dirty = true
    }

    fun apply() {
        if (!enabled || availableMhz.isEmpty() || minimumMhz <= 0 || maximumMhz <= 0) {
            RodinHaptics.reject()
            return
        }
        val accepted = RodinBackend.setCpuClusterFreqRange(policy, minimumMhz, maximumMhz)
        if (!accepted) {
            RodinHaptics.reject()
            return
        }
        RodinHaptics.confirm()
        dirty = false
        onFeedback(
            if (exactLock) "EXACT LOCK REQUESTED" else "FREQUENCY RANGE REQUESTED",
            if (exactLock) {
                "$title · ${frequencyLabel(maximumMhz)}"
            } else {
                "$title · ${frequencyLabel(minimumMhz)} – ${frequencyLabel(maximumMhz)}"
            },
            if (exactLock) Icons.Rounded.Lock else Icons.Rounded.Tune,
            accent,
        )
    }

    fun reset() {
        if (!enabled) {
            RodinHaptics.reject()
            return
        }
        val accepted = RodinBackend.resetCpuClusterFreqRange(policy)
        if (!accepted) {
            RodinHaptics.reject()
            return
        }
        RodinHaptics.confirm()
        dirty = false
        exactLock = false
        if (availableMhz.isNotEmpty()) {
            minimumMhz = availableMhz.first()
            maximumMhz = availableMhz.last()
        }
        onFeedback(
            "OEM RANGE REQUESTED",
            "$title returned to OEM-managed limits",
            Icons.Rounded.RestartAlt,
            accent,
        )
    }

    val custom = targetMinMhz > 0 && targetMaxMhz > 0
    val tableReady = availableMhz.isNotEmpty()
    val maxIndex = if (tableReady) availableMhz.lastIndex else 0
    val minIndex = if (tableReady) frequencyIndex(minimumMhz).coerceIn(0, maxIndex) else 0
    val selectedMaxIndex = if (tableReady) {
        frequencyIndex(maximumMhz).coerceIn(0, maxIndex)
    } else {
        0
    }
    val stateLabel = when {
        dirty -> "EDITING"
        !custom -> "OEM"
        targetMinMhz == targetMaxMhz -> "LOCKED"
        else -> "RANGE"
    }
    val verification = when {
        !custom -> "OEM managed"
        drift == 0 -> "Target verified"
        writeAck == 0 -> "Write not verified"
        else -> "Saved target · live limit differs"
    }
    val page = when {
        !tableReady -> 0
        exactLock -> 1
        else -> 2
    }

    SurfaceCard(
        padding = PaddingValues(start = 14.dp, top = 13.dp, end = 14.dp, bottom = 14.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(9.dp)
                        .rodinShadow(
                            color = accent.copy(alpha = 0.45f),
                            blurRadius = 8f,
                            cornerRadius = 5f,
                        )
                        .background(accent, RoundedCornerShape(99.dp)),
                )
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.W800,
                        ),
                    )
                    Spacer(Modifier.height(1.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 10.8.sp,
                            color = colors.onSurfaceVariant,
                        ),
                    )
                }
                StatusPill(label = stateLabel, accent = accent)
            }
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(accent.copy(alpha = 0.075f), RoundedCornerShape(12.dp))
                    .border(1.dp, accent.copy(alpha = 0.22f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f)) {
                    CpuFrequencyMetric(
                        label = "CURRENT",
                        value = frequencyLabel(currentMhz),
                        accent = accent,
                    )
                }
                Box(
                    Modifier
                        .width(1.dp)
                        .height(35.dp)
                        .background(colors.outline.copy(alpha = 0.28f)),
                )
                Box(Modifier.weight(1f)) {
                    CpuFrequencyMetric(
                        label = "LIVE LIMITS",
                        value = "${frequencyLabel(liveMinMhz)} – ${frequencyLabel(liveMaxMhz)}",
                        accent = accent,
                    )
                }
            }
            Spacer(Modifier.height(11.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Live governor: ${governorLabel()}",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 10.8.sp,
                        fontWeight = FontWeight.W600,
                        color = colors.onSurfaceVariant,
                    ),
                )
                Text(
                    text = if (tableReady) {
                        "${availableMhz.size} supported clocks"
                    } else {
                        "Reading clock table"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.2.sp,
                        color = colors.onSurfaceVariant,
                    ),
                )
            }
            Spacer(Modifier.height(10.dp))
            Row {
                Box(Modifier.weight(1f)) {
                    CpuModeButton(
                        label = "Dynamic Range",
                        icon = Icons.Rounded.SwapHoriz,
                        selected = !exactLock,
                        accent = accent,
                        enabled = enabled && tableReady,
                        onTap = { setExactLock(false) },
                    )
                }
                Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(1f)) {
                    CpuModeButton(
                        label = "Exact Lock",
                        icon = Icons.Rounded.Lock,
                        selected = exactLock,
                        accent = accent,
                        enabled = enabled && tableReady,
                        onTap = { setExactLock(true) },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Crossfade(
                targetState = page,
                animationSpec = tween(
                    durationMillis = 220,
                    easing = CubicBezierEasing(0.215f, 0.61f, 0.355f, 1f),
                ),
                label = "cpuRangePage",
            ) { target ->
                when (target) {
                    0 -> Box(
                        Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Waiting for the kernel frequency table…",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = colors.onSurfaceVariant,
                            ),
                        )
                    }
                    1 -> Column {
                        CpuRangeLabels(
                            leftLabel = "LOCK TARGET",
                            leftValue = frequencyLabel(maximumMhz),
                            rightLabel = "MIN = MAX",
                            rightValue = "$maximumMhz MHz",
                            accent = accent,
                        )
                        Slider(
                            value = selectedMaxIndex.toFloat(),
                            onValueChange = { raw ->
                                if (!sliderDragging) {
                                    sliderDragging = true
                                    RodinHaptics.segment()
                                }
                                if (!enabled) return@Slider
                                val target = availableMhz[raw.roundToInt().coerceIn(0, maxIndex)]
                                if (target == maximumMhz) return@Slider
                                minimumMhz = target
                                maximumMhz = target
                                dirty = true
                                RodinHaptics.frequentSegment()
                            },
                            onValueChangeFinished = {
                                sliderDragging = false
                                if (enabled) RodinHaptics.confirm()
                            },
                            enabled = enabled,
                            valueRange = 0f..maxIndex.toFloat(),
                            steps = if (maxIndex > 0) maxIndex - 1 else 0,
                            colors = SliderDefaults.colors(
                                thumbColor = accent,
                                activeTrackColor = accent,
                                inactiveTrackColor = accent.copy(alpha = 0.18f),
                            ),
                        )
                    }
                    else -> Column {
                        CpuRangeLabels(
                            leftLabel = "MINIMUM",
                            leftValue = frequencyLabel(minimumMhz),
                            rightLabel = "MAXIMUM",
                            rightValue = frequencyLabel(maximumMhz),
                            accent = accent,
                        )
                        RangeSlider(
                            value = minIndex.toFloat()..selectedMaxIndex.toFloat(),
                            onValueChange = { range ->
                                if (!sliderDragging) {
                                    sliderDragging = true
                                    RodinHaptics.segment()
                                }
                                if (!enabled) return@RangeSlider
                                val nextMin = availableMhz[
                                    range.start.roundToInt().coerceIn(0, maxIndex)
                                ]
                                val nextMax = availableMhz[
                                    range.endInclusive.roundToInt().coerceIn(0, maxIndex)
                                ]
                                if (nextMin == minimumMhz && nextMax == maximumMhz) {
                                    return@RangeSlider
                                }
                                minimumMhz = nextMin
                                maximumMhz = nextMax
                                dirty = true
                                RodinHaptics.frequentSegment()
                            },
                            onValueChangeFinished = {
                                sliderDragging = false
                                if (enabled) RodinHaptics.confirm()
                            },
                            enabled = enabled,
                            valueRange = 0f..maxIndex.toFloat(),
                            steps = if (maxIndex > 0) maxIndex - 1 else 0,
                            colors = SliderDefaults.colors(
                                thumbColor = accent,
                                activeTrackColor = accent,
                                inactiveTrackColor = accent.copy(alpha = 0.18f),
                            ),
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (drift == 1) Icons.Rounded.Sync else Icons.Rounded.Verified,
                    contentDescription = null,
                    tint = if (drift == 1) colors.onSurfaceVariant else accent,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = verification,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 10.5.sp,
                        lineHeight = 10.5.sp * 1.4f,
                        fontWeight = FontWeight.W600,
                        color = colors.onSurfaceVariant,
                    ),
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { reset() }, enabled = enabled) {
                    Icon(
                        imageVector = Icons.Rounded.RestartAlt,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("OEM Reset")
                }
                Spacer(Modifier.width(8.dp))
                FilledTonalButton(
                    onClick = { apply() },
                    enabled = enabled && tableReady && dirty,
                    colors = ButtonDefaults.filledTonalButtonColors(contentColor = accent),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp),
                ) {
                    Icon(
                        imageVector = if (exactLock) Icons.Rounded.Lock else Icons.Rounded.Check,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Apply")
                }
            }
        }
    }
}

@Composable
private fun CpuFrequencyMetric(label: String, value: String, accent: Color) {
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
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
            style = MaterialTheme.typography.titleSmall.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.W800,
            ),
        )
    }
}

@Composable
private fun CpuModeButton(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    accent: Color,
    enabled: Boolean,
    onTap: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val easing = CubicBezierEasing(0.215f, 0.61f, 0.355f, 1f)
    val background by animateColorAsState(
        targetValue = if (selected) {
            accent.copy(alpha = 0.13f)
        } else {
            colors.surfaceContainer.copy(alpha = 0.4f)
        },
        animationSpec = tween(180, easing = easing),
        label = "cpuModeBg",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) {
            accent.copy(alpha = 0.52f)
        } else {
            colors.outline.copy(alpha = 0.42f)
        },
        animationSpec = tween(180, easing = easing),
        label = "cpuModeBorder",
    )
    val tile: @Composable () -> Unit = {
        Row(
            Modifier
                .fillMaxWidth()
                .background(background, RoundedCornerShape(11.dp))
                .border(1.dp, borderColor, RoundedCornerShape(11.dp))
                .padding(horizontal = 10.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) accent else colors.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.W700,
                    color = if (selected) accent else colors.onSurfaceVariant,
                ),
            )
        }
    }
    if (enabled) {
        PressScale(onTap = onTap) { tile() }
    } else {
        tile()
    }
}

@Composable
private fun CpuRangeLabels(
    leftLabel: String,
    leftValue: String,
    rightLabel: String,
    rightValue: String,
    accent: Color,
) {
    val colors = MaterialTheme.colorScheme
    @Composable
    fun item(label: String, value: String, start: Boolean) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = if (start) Alignment.Start else Alignment.End,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.3.sp,
                    fontWeight = FontWeight.W800,
                    letterSpacing = 0.45.sp,
                    color = colors.onSurfaceVariant,
                ),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.W800,
                    color = accent,
                ),
            )
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) { item(leftLabel, leftValue, true) }
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f)) { item(rightLabel, rightValue, false) }
    }
}
