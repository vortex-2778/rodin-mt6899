package io.github.neeschal.rodinessential.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import io.github.neeschal.rodinessential.ui.theme.rodinSliderColors
import kotlin.math.roundToInt

private val BYPASS_THRESHOLD_VALUES = listOf(0, 20, 40, 80, 90)

@Composable
fun RodinBypassThresholdSelector(
    value: Int,
    enabled: Boolean,
    onChanged: (Int) -> Unit,
    accent: Color,
    modifier: Modifier = Modifier,
    onPreview: ((Int) -> Unit)? = null,
) {
    var preview by remember { mutableStateOf<Int?>(null) }
    val index = (preview ?: BYPASS_THRESHOLD_VALUES.indexOf(value)).coerceIn(0, 4)
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Slider(
            value = index.toFloat(),
            onValueChange = { raw ->
                if (enabled) {
                    val next = raw.roundToInt()
                    if (preview != next) {
                        preview = next
                        onPreview?.invoke(BYPASS_THRESHOLD_VALUES[next])
                    }
                }
            },
            onValueChangeFinished = {
                if (enabled) {
                    onChanged(BYPASS_THRESHOLD_VALUES[index])
                    preview = null
                }
            },
            valueRange = 0f..4f,
            steps = 3,
            enabled = enabled,
            colors = rodinSliderColors(accent, colors.outline),
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    stateDescription = if (BYPASS_THRESHOLD_VALUES[index] == 0) {
                        "Immediately"
                    } else {
                        "${BYPASS_THRESHOLD_VALUES[index]} percent"
                    }
                },
        )
        Row(Modifier.fillMaxWidth()) {
            BYPASS_THRESHOLD_VALUES.forEachIndexed { itemIndex, itemValue ->
                Text(
                    text = if (itemValue == 0) "Now" else "$itemValue%",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.W700,
                        color = if (index == itemIndex) accent else colors.onSurfaceVariant,
                    ),
                )
            }
        }
    }
}
