package io.github.neeschal.rodinessential.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import io.github.neeschal.rodinessential.backend.PerAppBackend
import io.github.neeschal.rodinessential.ui.components.RodinHaptics
import io.github.neeschal.rodinessential.ui.components.StatusPill
import io.github.neeschal.rodinessential.ui.components.SurfaceCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.math.abs

private fun jsonValue(value: Any?): Any? = when {
    value == null -> null
    value === JSONObject.NULL -> null
    value is Map<*, *> -> value.entries.associate { (key, item) -> key.toString() to jsonValue(item) }
    value is List<*> -> value.map { jsonValue(it) }
    else -> value
}

private fun jsonMap(value: Any?): Map<String, Any?>? = jsonValue(value) as? Map<String, Any?>

private fun subsystemHz(value: Any?): String {
    val number = (value as? Number)?.toLong() ?: return "Unavailable"
    val digits = if (number % 1_000_000L == 0L) 0 else 1
    return String.format("%.${digits}f", number / 1_000_000.0) + " MHz"
}

/** DRAM and storage frequency ranges, exact locks and OEM control. */
@Composable
fun SubsystemClocksScreen(onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var state by remember { mutableStateOf<Map<String, Any?>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf<String?>(null) }
    var reading by remember { mutableStateOf(false) }

    suspend fun readClocks() {
        if (reading || busy != null) return
        reading = true
        try {
            val data = withContext(Dispatchers.IO) {
                PerAppBackend.instance.command("GET subsystem.clocks")
            }
            state = jsonMap(data)
            error = null
        } catch (e: Exception) {
            error = e.message ?: e.toString()
        } finally {
            reading = false
        }
    }

    suspend fun performAction(command: String, key: String): Boolean {
        if (busy != null) return false
        busy = key
        return try {
            // Serialize behind any in-flight read, so stale telemetry cannot
            // overwrite a newly acknowledged selection.
            while (reading) delay(50)
            val data = withContext(Dispatchers.IO) {
                PerAppBackend.instance.command(command)
            }
            state = jsonMap(data)
            error = null
            RodinHaptics.confirm()
            true
        } catch (e: Exception) {
            error = e.message ?: e.toString()
            RodinHaptics.reject()
            false
        } finally {
            busy = null
        }
    }

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                readClocks()
                delay(2000)
            }
        }
    }

    val topError = error ?: (state?.get("error") as? String)
    val itemReady = busy == null && state?.get("error") == null

    RodinScrollPage(
        header = RodinPageHeader.Detail(title = "Memory DVFS & UFS", onBack = onBack),
    ) {
        Spacer(Modifier.height(4.dp))
        Text(
            text = "DRAM and storage frequency ranges, exact locks and OEM control",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.5.sp,
                color = colors.onSurfaceVariant,
            ),
        )
        Spacer(Modifier.height(14.dp))
        if (topError != null) {
            SurfaceCard {
                Text(topError)
            }
        }
        if (state == null && error == null) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
        SubsystemSection(
            subsystemKey = "memory",
            title = "LPDDR5X RAM",
            subtitle = "Memory DVFS · DRAM frequency coordinator",
            icon = Icons.Rounded.Memory,
            accent = Color(0xFFB087FF),
            item = jsonMap(state?.get("memory")) ?: emptyMap(),
            enabled = itemReady,
            onApply = { minHz, maxHz ->
                performAction("ACTION subsystem.clocks.range memory $minHz $maxHz", "memory")
            },
            onReset = {
                performAction("ACTION subsystem.clocks.oem memory", "memory")
            },
        )
        Spacer(Modifier.height(20.dp))
        SubsystemSection(
            subsystemKey = "storage",
            title = "UFS 4.0 Storage",
            subtitle = "Storage controller · Clock ranges and exact locks",
            icon = Icons.Rounded.Storage,
            accent = Color(0xFF67C2FF),
            item = jsonMap(state?.get("storage")) ?: emptyMap(),
            enabled = itemReady,
            onApply = { minHz, maxHz ->
                performAction("ACTION subsystem.clocks.range storage $minHz $maxHz", "storage")
            },
            onReset = {
                performAction("ACTION subsystem.clocks.oem storage", "storage")
            },
        )
        Spacer(Modifier.height(12.dp))
        TextButton(
            onClick = { scope.launch { readClocks() } },
            enabled = busy == null,
        ) {
            Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = null,
            )
            Spacer(Modifier.width(6.dp))
            Text("Refresh readings")
        }
        Text(
            text = "Settings remain active when Rodin Essential closes. For now, " +
                "reboot or daemon restart restores OEM settings. Thermal protection " +
                "and suspend remain active.",
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                lineHeight = 16.5.sp,
            ),
        )
    }
}

@Composable
private fun SubsystemSection(
    subsystemKey: String,
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    item: Map<String, Any?>,
    enabled: Boolean,
    onApply: suspend (Int, Int) -> Boolean,
    onReset: suspend () -> Boolean,
) {
    Column {
        HeroCard(
            icon = icon,
            accent = accent,
            title = title,
            subtitle = subtitle,
            leading = {
                SubsystemHardwareIcon(
                    storage = subsystemKey == "storage",
                    accent = accent,
                )
            },
        )
        Spacer(Modifier.height(12.dp))
        SubsystemFrequencyCard(
            subsystem = subsystemKey,
            accent = accent,
            item = item,
            enabled = enabled,
            onApply = onApply,
            onReset = onReset,
        )
    }
}

/** NEESCHAL: recognisable hardware, not another CPU icon wearing a RAM label. */
@Composable
private fun SubsystemHardwareIcon(storage: Boolean, accent: Color) {
    Canvas(
        Modifier
            .size(54.dp)
            .background(
                Brush.linearGradient(
                    listOf(accent.copy(alpha = 0.23f), accent.copy(alpha = 0.07f)),
                ),
                RoundedCornerShape(17.dp),
            )
            .border(1.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(17.dp))
            .padding(10.dp),
    ) {
        drawSubsystemHardware(storage, accent)
    }
}

private fun DrawScope.drawSubsystemHardware(storage: Boolean, accent: Color) {
    val unit = size.width / 40f
    scale(unit, unit, pivot = Offset.Zero) {
        val stroke = Stroke(width = 2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        if (storage) {
            val topLeft = Offset(9f, 6f)
            val bodySize = Size(22f, 28f)
            val corner = CornerRadius(4f, 4f)
            drawRoundRect(
                color = accent.copy(alpha = 0.18f),
                topLeft = topLeft,
                size = bodySize,
                cornerRadius = corner,
            )
            drawRoundRect(
                color = accent,
                topLeft = topLeft,
                size = bodySize,
                cornerRadius = corner,
                style = stroke,
            )
            for (y in listOf(12f, 20f, 28f)) {
                drawLine(accent, Offset(5f, y), Offset(9f, y), strokeWidth = 2.2f, cap = StrokeCap.Round)
                drawLine(accent, Offset(31f, y), Offset(35f, y), strokeWidth = 2.2f, cap = StrokeCap.Round)
            }
            for (y in listOf(14f, 20f, 26f)) {
                drawLine(accent, Offset(15f, y), Offset(25f, y), strokeWidth = 2.2f, cap = StrokeCap.Round)
            }
        } else {
            val topLeft = Offset(3f, 11f)
            val bodySize = Size(34f, 19f)
            val corner = CornerRadius(3f, 3f)
            drawRoundRect(
                color = accent.copy(alpha = 0.18f),
                topLeft = topLeft,
                size = bodySize,
                cornerRadius = corner,
            )
            drawRoundRect(
                color = accent,
                topLeft = topLeft,
                size = bodySize,
                cornerRadius = corner,
                style = stroke,
            )
            for (x in listOf(8f, 16f, 24f)) {
                drawRoundRect(
                    color = accent,
                    topLeft = Offset(x, 16f),
                    size = Size(5f, 8f),
                    cornerRadius = CornerRadius(1f, 1f),
                    style = stroke,
                )
            }
            for (x in listOf(8f, 14f, 20f, 26f, 32f)) {
                drawLine(accent, Offset(x, 30f), Offset(x, 34f), strokeWidth = 2.2f, cap = StrokeCap.Round)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SubsystemFrequencyCard(
    subsystem: String,
    accent: Color,
    item: Map<String, Any?>,
    enabled: Boolean,
    onApply: suspend (Int, Int) -> Boolean,
    onReset: suspend () -> Boolean,
) {
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()

    val ceilingValue = (item["oem_max_hz"] ?: item["max_hz"]).let { (it as? Number)?.toLong() }
    val levels = ((item["frequencies_hz"] as? List<*>) ?: emptyList<Any?>())
        .mapNotNull { (it as? Number)?.toLong() }
        .filter { ceilingValue == null || it <= ceilingValue }
    val ready = levels.size > 1

    var minHz by remember { mutableLongStateOf(0L) }
    var maxHz by remember { mutableLongStateOf(0L) }
    var exact by remember { mutableStateOf(false) }
    var dirty by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }
    var sliderDragging by remember { mutableStateOf(false) }

    fun nearest(hz: Long): Long {
        if (levels.isEmpty()) return 0L
        var best = levels.first()
        for (level in levels) {
            if (abs(level - hz) < abs(best - hz)) best = level
        }
        return best
    }

    fun sync() {
        if (dirty || sending || levels.isEmpty()) return
        val targetMin = ((item["target_min_hz"] ?: item["min_hz"]) as? Number)?.toLong()
            ?: levels.first()
        minHz = nearest(targetMin)
        val targetMax = ((item["target_max_hz"] ?: item["max_hz"]) as? Number)?.toLong()
            ?: levels.last()
        maxHz = nearest(targetMax)
        exact = item["controlled"] == true && minHz == maxHz
    }

    LaunchedEffect(item, dirty, sending) {
        sync()
    }

    fun setMode(next: Boolean) {
        if (exact == next) return
        RodinHaptics.segment()
        exact = next
        if (next) {
            minHz = maxHz
        } else if (minHz == maxHz) {
            minHz = levels.first()
        }
        dirty = true
    }

    val controlsEnabled = enabled && ready && item["supported"] == true && !sending
    val maxIndex = if (ready) levels.size - 1 else 1
    val minIndex = if (ready) levels.indexOf(nearest(minHz)).coerceIn(0, maxIndex) else 0
    val upperIndex = if (ready) {
        levels.indexOf(nearest(maxHz)).coerceIn(minIndex, maxIndex)
    } else {
        1
    }
    val page = when {
        !ready -> 0
        exact -> 1
        else -> 2
    }
    val pageEasing = CubicBezierEasing(0.215f, 0.61f, 0.355f, 1f)

    SurfaceCard(
        padding = androidx.compose.foundation.layout.PaddingValues(
            start = 14.dp,
            top = 13.dp,
            end = 14.dp,
            bottom = 14.dp,
        ),
        accent = accent,
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(9.dp)
                        .background(accent, RoundedCornerShape(99.dp)),
                )
                Spacer(Modifier.width(9.dp))
                Text(
                    text = "Frequency control",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.W800,
                    ),
                )
                StatusPill(
                    label = when {
                        sending -> "APPLYING"
                        dirty -> "EDITING"
                        item["controlled"] != true -> "OEM"
                        minHz == maxHz -> "LOCKED"
                        else -> "RANGE"
                    },
                    accent = accent,
                )
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
                        label = if (subsystem == "memory") "REPORTED CURRENT" else "CURRENT",
                        value = subsystemHz(item["driver_hz"]),
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
                        value = "${subsystemHz(item["min_hz"])} – ${subsystemHz(item["max_hz"])}",
                        accent = accent,
                    )
                }
            }
            Spacer(Modifier.height(11.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Live governor: ${item["governor"] ?: "Unavailable"}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.8.sp,
                        color = colors.onSurfaceVariant,
                    ),
                )
                Text(
                    text = "${levels.size} supported clocks",
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
                        selected = !exact,
                        accent = accent,
                        enabled = controlsEnabled,
                        onTap = { setMode(false) },
                    )
                }
                Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(1f)) {
                    CpuModeButton(
                        label = "Exact Lock",
                        icon = Icons.Rounded.Lock,
                        selected = exact,
                        accent = accent,
                        enabled = controlsEnabled,
                        onTap = { setMode(true) },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Column(
                Modifier.animateContentSize(
                    animationSpec = tween(220, easing = pageEasing),
                ),
            ) {
                Crossfade(
                    targetState = page,
                    animationSpec = tween(180, easing = pageEasing),
                    label = "subsystemPage",
                ) { target ->
                    when (target) {
                        0 -> Text(
                            text = "No supported frequency table available",
                            modifier = Modifier.padding(16.dp),
                        )
                        1 -> Column {
                            CpuRangeLabels(
                                leftLabel = "LOCK TARGET",
                                leftValue = subsystemHz(maxHz),
                                rightLabel = "MIN = MAX",
                                rightValue = subsystemHz(maxHz),
                                accent = accent,
                            )
                            Slider(
                                value = upperIndex.toFloat(),
                                onValueChange = { raw ->
                                    if (!sliderDragging) {
                                        sliderDragging = true
                                        RodinHaptics.segment()
                                    }
                                    val hz = levels[raw.toInt().coerceIn(0, maxIndex)]
                                    if (maxHz == hz) return@Slider
                                    minHz = hz
                                    maxHz = hz
                                    dirty = true
                                    RodinHaptics.frequentSegment()
                                },
                                onValueChangeFinished = {
                                    sliderDragging = false
                                    RodinHaptics.confirm()
                                },
                                enabled = controlsEnabled,
                                valueRange = 0f..maxIndex.toFloat(),
                                steps = if (maxIndex > 0) maxIndex - 1 else 0,
                                colors = SliderDefaults.colors(
                                    thumbColor = accent,
                                    activeTrackColor = accent,
                                ),
                            )
                        }
                        else -> Column {
                            CpuRangeLabels(
                                leftLabel = "MINIMUM",
                                leftValue = subsystemHz(minHz),
                                rightLabel = "MAXIMUM",
                                rightValue = subsystemHz(maxHz),
                                accent = accent,
                            )
                            RangeSlider(
                                value = minIndex.toFloat()..upperIndex.toFloat(),
                                onValueChange = { range ->
                                    if (!sliderDragging) {
                                        sliderDragging = true
                                        RodinHaptics.segment()
                                    }
                                    val nextMin = levels[
                                        range.start.toInt().coerceIn(0, maxIndex)
                                    ]
                                    val nextMax = levels[
                                        range.endInclusive.toInt().coerceIn(0, maxIndex)
                                    ]
                                    if (minHz == nextMin && maxHz == nextMax) {
                                        return@RangeSlider
                                    }
                                    minHz = nextMin
                                    maxHz = nextMax
                                    dirty = true
                                    RodinHaptics.frequentSegment()
                                },
                                onValueChangeFinished = {
                                    sliderDragging = false
                                    RodinHaptics.confirm()
                                },
                                enabled = controlsEnabled,
                                valueRange = 0f..maxIndex.toFloat(),
                                steps = if (maxIndex > 0) maxIndex - 1 else 0,
                                colors = SliderDefaults.colors(
                                    thumbColor = accent,
                                    activeTrackColor = accent,
                                ),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = when {
                    item["controlled"] != true -> "OEM managed"
                    item["verified"] == true -> "Target verified"
                    else -> "Requested target · live limits differ"
                },
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.W600,
                    color = colors.onSurfaceVariant,
                ),
            )
            Spacer(Modifier.height(6.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = {
                        scope.launch {
                            RodinHaptics.tap()
                            sending = true
                            val accepted = onReset()
                            sending = false
                            if (accepted) {
                                dirty = false
                                sync()
                            }
                        }
                    },
                    enabled = enabled && !sending && item["restore_pending"] == true,
                ) {
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
                    onClick = {
                        scope.launch {
                            sending = true
                            val accepted = onApply(minHz.toInt(), maxHz.toInt())
                            sending = false
                            if (accepted) dirty = false
                        }
                    },
                    enabled = controlsEnabled && dirty,
                ) {
                    Icon(
                        imageVector = if (sending) {
                            Icons.Rounded.HourglassTop
                        } else {
                            Icons.Rounded.Check
                        },
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (sending) "Applying…" else "Apply")
                }
            }
            if (subsystem == "memory") {
                Text(
                    text = "DVFSRC reports a cached DRAM request, not independent " +
                        "physical RAM-clock telemetry.",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.5.sp,
                        lineHeight = 14.7.sp,
                        color = colors.onSurfaceVariant,
                    ),
                )
            }
            if (subsystem == "storage" && levels.size == 2) {
                Text(
                    text = "This kernel exposes ${subsystemHz(levels.first())} and " +
                        "${subsystemHz(levels.last())} only. Intermediate clocks are " +
                        "not supported.",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.5.sp,
                        lineHeight = 14.7.sp,
                        color = colors.onSurfaceVariant,
                    ),
                )
            }
            if (item["supported"] != true && item["reason"] != null) {
                Text("${item["reason"]}")
            }
        }
    }
}
