package io.github.neeschal.rodinessential.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.neeschal.rodinessential.ui.components.PressScale

/** One labelled live metric inside the frequency cards. */
@Composable
fun CpuFrequencyMetric(label: String, value: String, accent: Color) {
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

/** The animated Dynamic Range / Exact Lock selector tile. */
@Composable
fun CpuModeButton(
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

/** The MINIMUM / MAXIMUM value row above every range slider. */
@Composable
fun CpuRangeLabels(
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
