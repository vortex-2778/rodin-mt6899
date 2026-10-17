package io.github.neeschal.rodinessential.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import io.github.neeschal.rodinessential.ui.motion.LocalRodinMotionViewportInFlight
import io.github.neeschal.rodinessential.ui.motion.rodinAnimationsDisabled
import kotlinx.coroutines.delay

class HubHeadlineCopy(
    val lead: String,
    val emphasis: String,
    val description: String,
    val light: Color,
    val dark: Color,
)

// NEESCHAL: personality belongs in the words, not invented hardware promises.
val hubHeadlines = listOf(
    HubHeadlineCopy(
        "Control",
        "beyond cores.",
        "Explore CPU, GPU, memory and storage controls.",
        Color(0xFF087DA8),
        Color(0xFF67C2FF),
    ),
    HubHeadlineCopy(
        "Go",
        "beyond defaults.",
        "Choose your settings. Keep OEM control when you want it.",
        Color(0xFF7750BB),
        Color(0xFFB087FF),
    ),
    HubHeadlineCopy(
        "Your device.",
        "Your decisions.",
        "Adjust the details that matter to you.",
        Color(0xFF087E59),
        Color(0xFF41C98A),
    ),
    HubHeadlineCopy(
        "Every app.",
        "Its own rhythm.",
        "Create individual touch, refresh, CPU and GPU profiles.",
        Color(0xFF7750BB),
        Color(0xFFB087FF),
    ),
    HubHeadlineCopy(
        "Make every",
        "touch yours.",
        "Choose OEM response or a supported touch override.",
        Color(0xFF087E59),
        Color(0xFF41C98A),
    ),
    HubHeadlineCopy(
        "Find",
        "your balance.",
        "Fine-tune performance and power for your own routine.",
        Color(0xFF9A6518),
        Color(0xFFFFB84D),
    ),
    HubHeadlineCopy(
        "Set",
        "your own pace.",
        "Explore refresh-rate choices in Per-App Controls.",
        Color(0xFF087DA8),
        Color(0xFF67C2FF),
    ),
    HubHeadlineCopy(
        "Fine-tune",
        "the details.",
        "From frequency ranges to exact locks, choose your setup.",
        Color(0xFFB53D60),
        Color(0xFFFF819F),
    ),
    HubHeadlineCopy(
        "From memory",
        "to motion.",
        "Explore memory, storage, graphics and display controls.",
        Color(0xFF7750BB),
        Color(0xFFB087FF),
    ),
    HubHeadlineCopy(
        "Power",
        "on your terms.",
        "Explore charging controls supported by your device.",
        Color(0xFF9A6518),
        Color(0xFFFFB84D),
    ),
    HubHeadlineCopy(
        "Keep OEM.",
        "Or make it yours.",
        "Use vendor defaults or choose your own supported settings.",
        Color(0xFF087E59),
        Color(0xFF41C98A),
    ),
    HubHeadlineCopy(
        "One hub.",
        "More possibilities.",
        "Discover the controls available on your device.",
        Color(0xFF087DA8),
        Color(0xFF67C2FF),
    ),
)

private val headlineEaseInOutCubic = CubicBezierEasing(0.645f, 0.045f, 0.355f, 1f)

/**
 * One short transition per five-second reading interval, never a looping
 * ticker. Pauses offscreen, during navigation, in the background and for
 * reduced motion. Only this text subtree paints; telemetry keeps its state.
 */
@Composable
fun HubHeadline(modifier: Modifier = Modifier) {
    val reduced = rodinAnimationsDisabled()
    val inFlight = LocalRodinMotionViewportInFlight.current
    val enabled = !reduced && !inFlight
    val colors = MaterialTheme.colorScheme
    val isDark = colors.surface.luminance() < 0.5f
    val lifecycleOwner = LocalLifecycleOwner.current

    var foreground by remember { mutableStateOf(true) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> foreground = true
                Lifecycle.Event.ON_PAUSE -> foreground = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var visible by remember { mutableStateOf(true) }
    var index by remember { mutableIntStateOf(0) }
    var swapped by remember { mutableStateOf(false) }
    val motion = remember { Animatable(1f) }

    LaunchedEffect(enabled, visible, foreground) {
        // Finish the local text reveal before a root surface starts moving.
        // A muted ticker must never leave its headline invisible halfway.
        motion.snapTo(1f)
        if (!enabled || !visible || !foreground) return@LaunchedEffect
        while (true) {
            delay(5_000)
            swapped = false
            motion.snapTo(0f)
            motion.animateTo(
                targetValue = 1f,
                animationSpec = tween(360, easing = LinearEasing),
            ) {
                if (value >= 0.5f && !swapped) {
                    swapped = true
                    index = (index + 1) % hubHeadlines.size
                }
            }
        }
    }

    val titleStyle = MaterialTheme.typography.titleLarge.copy(
        fontSize = 23.sp,
        lineHeight = 26.45.sp,
        fontWeight = FontWeight.W800,
        letterSpacing = (-0.55).sp,
        color = colors.onSurface,
    )
    val descriptionStyle = MaterialTheme.typography.bodySmall.copy(
        fontSize = 11.sp,
        lineHeight = 15.4.sp,
        color = colors.onSurfaceVariant,
    )

    val windowHeight = LocalConfiguration.current.screenHeightDp.toFloat()

    BoxWithConstraints(modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    val bounds = coordinates.boundsInWindow()
                    val visibleNow = bounds.top < windowHeight && bounds.bottom > 0f
                    if (visible != visibleNow) visible = visibleNow
                },
        ) {
            // Every copy is laid out in place; the tallest one owns the height.
            Box(
                Modifier
                    .graphicsLayer { alpha = 0f }
                    .clearAndSetSemantics {},
            ) {
                hubHeadlines.forEach { copy ->
                    HeadlineCopyColumn(
                        copy = copy,
                        titleStyle = titleStyle,
                        descriptionStyle = descriptionStyle,
                        accent = if (isDark) copy.dark else copy.light,
                    )
                }
            }

            val copy = hubHeadlines[index]
            val accent = if (isDark) copy.dark else copy.light
            val value = motion.value
            val incoming = value >= 0.5f
            val eased = headlineEaseInOutCubic.transform(
                if (incoming) (value - 0.5f) * 2f else value * 2f,
            )
            val opacity = if (incoming) eased else 1f - eased
            Column(
                Modifier
                    .graphicsLayer {
                        alpha = opacity
                        translationY =
                            (if (incoming) 3f * (1f - eased) else -3f * eased) * density
                    }
                    .clearAndSetSemantics {
                        contentDescription =
                            "${copy.lead} ${copy.emphasis} ${copy.description}"
                    },
            ) {
                Text(
                    text = buildAnnotatedString {
                        append(copy.lead)
                        append('\n')
                        withStyle(SpanStyle(color = accent)) { append(copy.emphasis) }
                    },
                    style = titleStyle,
                )
                Spacer(Modifier.height(8.dp))
                Text(text = copy.description, style = descriptionStyle)
            }
        }
    }
}

@Composable
private fun HeadlineCopyColumn(
    copy: HubHeadlineCopy,
    titleStyle: androidx.compose.ui.text.TextStyle,
    descriptionStyle: androidx.compose.ui.text.TextStyle,
    accent: Color,
) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = buildAnnotatedString {
                append(copy.lead)
                append('\n')
                withStyle(SpanStyle(color = accent)) { append(copy.emphasis) }
            },
            style = titleStyle,
        )
        Spacer(Modifier.height(8.dp))
        Text(text = copy.description, style = descriptionStyle)
    }
}
