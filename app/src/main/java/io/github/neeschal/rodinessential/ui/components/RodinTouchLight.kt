package io.github.neeschal.rodinessential.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import io.github.neeschal.rodinessential.ui.motion.rodinAnimationsDisabled
import kotlinx.coroutines.launch

/** Lets a parent cancel the light on release, like the original global key. */
class RodinTouchLightController {
    internal var cancelSignal by mutableIntStateOf(0)
        private set

    fun cancel() {
        cancelSignal += 1
    }
}

/**
 * App-local soft-light response, not a dependency on an OEM glass engine.
 * Position changes only redraw the overlay; there is no shader compilation,
 * extra backdrop blur, saveLayer, or perpetual animation during a hold.
 */
@Composable
fun RodinTouchLight(
    accent: Color,
    radius: Float = 22f,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    controller: RodinTouchLightController = remember { RodinTouchLightController() },
    content: @Composable () -> Unit,
) {
    if (!enabled) {
        Box(modifier) { content() }
        return
    }
    val reduced = rodinAnimationsDisabled()
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val scope = rememberCoroutineScope()
    var point by remember { mutableStateOf(Offset.Zero) }
    var power by remember { mutableStateOf(0f) }
    var pointerId by remember { mutableStateOf<androidx.compose.ui.input.pointer.PointerId?>(null) }

    LaunchedEffect(controller.cancelSignal) {
        if (controller.cancelSignal == 0) return@LaunchedEffect
        pointerId = null
        if (reduced) {
            power = 0f
        } else {
            // Reverse over 160 ms, matching the original controller.
            val start = power
            val steps = 16
            for (step in 1..steps) {
                power = start * (1f - step / steps.toFloat())
                kotlinx.coroutines.delay(10)
            }
            power = 0f
        }
    }

    Box(
        modifier.pointerInput(enabled, reduced) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                pointerId = down.id
                point = down.position
                if (reduced) {
                    power = 1f
                } else {
                    // First-frame feedback must not wait for the animation.
                    if (power < 0.25f) power = 0.25f
                    scope.launch {
                        val start = power
                        val target = 1f
                        val steps = 8
                        for (step in 1..steps) {
                            power = start + (target - start) * step / steps.toFloat()
                            kotlinx.coroutines.delay(10)
                        }
                        power = 1f
                    }
                }
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed || change.changedToUp()) break
                    if (!change.positionChanged()) continue
                    if (change.position.x < 0f || change.position.y < 0f ||
                        change.position.x > size.width || change.position.y > size.height
                    ) {
                        break
                    }
                    point = change.position
                }
                pointerId = null
                if (!reduced) {
                    scope.launch {
                        val start = power
                        val steps = 16
                        for (step in 1..steps) {
                            power = start * (1f - step / steps.toFloat())
                            kotlinx.coroutines.delay(10)
                        }
                        power = 0f
                    }
                } else {
                    power = 0f
                }
            }
        },
    ) {
        content()
        // The overlay must match the wrapped widget's size without inflating
        // it: a fillMaxSize sibling would force the whole Box (and therefore
        // the dock that wraps it) to the parent's full available height.
        Canvas(Modifier.matchParentSize()) {
            val strength = power.coerceIn(0f, 1f)
            if (strength <= 0f || size.width <= 0f || size.height <= 0f) return@Canvas
            val bounds = Rect(Offset.Zero, size)
            val corner = radius.coerceIn(0f, size.minDimension / 2f)
            val shape = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = bounds.deflate(0.6f),
                        cornerRadius = CornerRadius(corner, corner),
                    ),
                )
            }
            val lightRadius = minOf(140f, maxOf(55f, size.maxDimension * 0.42f))
            val lightBounds = Rect(center = point, radius = lightRadius)
            clipPath(shape) {
                drawRect(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0f to lerp(accent, Color.White, 0.50f).copy(
                                alpha = (if (dark) 0.19f else 0.11f) * strength,
                            ),
                            0.32f to accent.copy(alpha = (if (dark) 0.11f else 0.065f) * strength),
                            1f to accent.copy(alpha = 0f),
                        ),
                        center = lightBounds.center,
                        radius = lightBounds.width / 2f,
                    ),
                )
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0f to Color.White.copy(alpha = (if (dark) 0.65f else 0.80f) * strength),
                            0.45f to accent.copy(alpha = 0.34f * strength),
                            1f to accent.copy(alpha = 0f),
                        ),
                        center = lightBounds.center,
                        radius = lightBounds.width / 2f,
                    ),
                    topLeft = bounds.topLeft,
                    size = bounds.size,
                    cornerRadius = CornerRadius(corner, corner),
                    style = Stroke(width = 1.2f),
                )
            }
        }
    }
}
