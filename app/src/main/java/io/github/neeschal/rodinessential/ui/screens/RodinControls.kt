package io.github.neeschal.rodinessential.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.neeschal.rodinessential.ui.components.IconTile
import io.github.neeschal.rodinessential.ui.components.RodinHaptics
import io.github.neeschal.rodinessential.ui.components.StatusPill
import io.github.neeschal.rodinessential.ui.components.SurfaceCard
import kotlin.math.ceil
import kotlin.math.roundToInt

/** One selectable code and its label, exactly like the Dart choice model. */
data class RodinChoiceOption(val code: Int, val label: String)

/** A titled card of radio-like choice chips with a live status pill. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RodinChoiceCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    selectedCode: Int,
    options: List<RodinChoiceOption>,
    enabled: Boolean,
    onSelected: (Int) -> Boolean,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val selected = options.firstOrNull { it.code == selectedCode }

    SurfaceCard(
        modifier = modifier,
        padding = PaddingValues(start = 13.dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(icon = icon, accent = accent, size = 42.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.W700,
                        ),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.2.sp,
                            color = scheme.onSurfaceVariant,
                        ),
                    )
                }
                StatusPill(
                    label = selected?.label ?: "UNSET",
                    accent = if (selected == null) scheme.onSurfaceVariant else accent,
                )
            }
            Spacer(Modifier.height(11.dp))
            FlowRow(
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(7.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(7.dp),
            ) {
                for (option in options) {
                    val isSelected = option.code == selectedCode
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (onSelected(option.code)) {
                                RodinHaptics.segment()
                            } else {
                                RodinHaptics.reject()
                            }
                        },
                        label = {
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.W700,
                                ),
                            )
                        },
                        enabled = enabled,
                        leadingIcon = null,
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = scheme.surfaceContainer.copy(alpha = 0.46f),
                            selectedContainerColor = accent.copy(alpha = 0.15f),
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = enabled,
                            selected = isSelected,
                            borderColor = scheme.outline.copy(alpha = 0.56f),
                            selectedBorderColor = accent.copy(alpha = 0.38f),
                        ),
                    )
                }
            }
        }
    }
}

/**
 * One expert calibration row: live preview label, haptic buckets and a
 * rate-limited live apply so Binder work cannot make the thumb sticky.
 */
@Composable
fun RodinExpertSliderRow(
    title: String,
    value: Int,
    min: Int,
    max: Int,
    defaultValue: Int,
    enabled: Boolean,
    onChanged: (Int) -> Boolean,
    modifier: Modifier = Modifier,
    activeColor: Color? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val trackColor = activeColor ?: scheme.primary
    var dragging by remember { mutableStateOf(false) }
    var preview by remember { mutableFloatStateOf(0f) }
    var lastSent by remember { mutableIntStateOf(0) }
    var lastAccepted by remember { mutableIntStateOf(0) }
    var lastHapticBucket by remember { mutableStateOf<Int?>(null) }
    var lastSendAt by remember { mutableLongStateOf(0L) }

    fun safeValue(): Int = if (value in min..max) value else defaultValue

    fun hapticBucket(candidate: Int): Int {
        val span = max - min
        // About 18 meaningful tactile steps across any channel range.
        val stride = if (span <= 18) 1 else ceil(span / 18.0).toInt()
        return (candidate - min) / stride
    }

    LaunchedEffect(value, min, max, dragging) {
        if (!dragging) {
            val safe = safeValue()
            preview = safe.toFloat()
            lastSent = safe
            lastAccepted = safe
        }
    }
    LaunchedEffect(Unit) {
        val safe = safeValue()
        preview = safe.toFloat()
        lastSent = safe
        lastAccepted = safe
    }

    fun tryLiveApply(candidate: Int, force: Boolean) {
        if (candidate == lastSent) return
        val now = System.currentTimeMillis()
        if (!force && now - lastSendAt < 80) return
        lastSendAt = now
        lastSent = candidate
        if (onChanged(candidate)) {
            lastAccepted = candidate
        }
    }

    Column(modifier.padding(vertical = 2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.W700,
                ),
            )
            Box(
                Modifier
                    .background(trackColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 7.dp, vertical = 2.dp),
            ) {
                Text(
                    text = preview.roundToInt().toString(),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.W900,
                        color = trackColor,
                        fontFamily = FontFamily.Monospace,
                    ),
                )
            }
        }
        Slider(
            value = preview.coerceIn(min.toFloat(), max.toFloat()),
            onValueChange = { raw ->
                if (!dragging) {
                    dragging = true
                    val startValue = raw.roundToInt().coerceIn(min, max)
                    lastSent = startValue
                    lastAccepted = startValue
                    lastHapticBucket = hapticBucket(startValue)
                    lastSendAt = System.currentTimeMillis()
                    RodinHaptics.segment()
                }
                preview = raw
                val rounded = raw.roundToInt().coerceIn(min, max)
                val bucket = hapticBucket(rounded)
                if (lastHapticBucket != bucket) {
                    lastHapticBucket = bucket
                    RodinHaptics.frequentSegment()
                }
                tryLiveApply(rounded, false)
            },
            onValueChangeFinished = {
                val rounded = preview.roundToInt().coerceIn(min, max)
                tryLiveApply(rounded, true)
                dragging = false
                preview = rounded.toFloat()
                lastHapticBucket = null
                if (lastAccepted == rounded) {
                    RodinHaptics.confirm()
                } else {
                    RodinHaptics.reject()
                }
            },
            enabled = enabled,
            valueRange = min.toFloat()..max.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = trackColor,
                activeTrackColor = trackColor,
                inactiveTrackColor = trackColor.copy(alpha = 0.18f),
            ),
        )
    }
}

/** The animated feature switch card shared by the display and CPU pages. */
@Composable
fun RodinSwitchCard(
    icon: ImageVector,
    accent: Color,
    title: String,
    subtitle: String,
    value: Boolean,
    enabled: Boolean,
    stateKnown: Boolean,
    onChanged: (Boolean) -> Boolean,
    modifier: Modifier = Modifier,
    unknownLabel: String = "SESSION",
) {
    val scheme = MaterialTheme.colorScheme
    val amount by animateFloatAsState(
        targetValue = if (value) 1f else 0f,
        animationSpec = tween(220),
        label = "switchCardAmount",
    )
    SurfaceCard(
        modifier = modifier,
        padding = PaddingValues(start = 13.dp, top = 12.dp, end = 8.dp, bottom = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(43.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                accent.copy(alpha = 0.13f + 0.11f * amount),
                                accent.copy(alpha = 0.055f),
                            ),
                        ),
                        RoundedCornerShape(14.dp),
                    )
                    .border(
                        1.dp,
                        accent.copy(alpha = 0.13f + 0.14f * amount),
                        RoundedCornerShape(14.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(21.dp),
                )
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.W700,
                        ),
                    )
                    if (!stateKnown) {
                        Spacer(Modifier.width(6.dp))
                        StatusPill(label = unknownLabel, accent = scheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.5.sp,
                        lineHeight = 11.5.sp * 1.2f,
                        color = scheme.onSurfaceVariant,
                    ),
                )
            }
            Switch(
                checked = value,
                onCheckedChange = if (enabled) {
                    { next ->
                        if (onChanged(next)) {
                            RodinHaptics.toggle(next)
                        } else {
                            RodinHaptics.reject()
                        }
                    }
                } else {
                    null
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = accent,
                    checkedTrackColor = accent.copy(alpha = 0.34f),
                ),
            )
        }
    }
}
