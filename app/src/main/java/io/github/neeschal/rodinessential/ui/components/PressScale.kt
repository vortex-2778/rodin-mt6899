package io.github.neeschal.rodinessential.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.lerp
import io.github.neeschal.rodinessential.ui.motion.rodinAnimationsDisabled
import io.github.neeschal.rodinessential.ui.shell.RodinInteractionSettings
import io.github.neeschal.rodinessential.ui.theme.LocalRodinAppearance
import kotlinx.coroutines.launch

/**
 * One press treatment for every tappable surface: a spring-settled depth,
 * a soft touch light and a haptic acknowledgement on release.
 */
@Composable
fun PressScale(
    onTap: (() -> Unit)? = null,
    enableHaptics: Boolean = true,
    enableLight: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (onTap == null) {
        Box(modifier) { content() }
        return
    }
    val appearance = LocalRodinAppearance.current
    val reduced = rodinAnimationsDisabled()
    val scope = rememberCoroutineScope()
    val depth = remember { Animatable(0f) }
    val lightController = remember { RodinTouchLightController() }
    var pressX by remember { mutableFloatStateOf(0f) }
    var pressY by remember { mutableFloatStateOf(0f) }

    fun press(offset: Offset, size: IntSize) {
        if (reduced) return
        if (size.width > 0 && size.height > 0) {
            pressX = (offset.x / size.width * 2f - 1f).coerceIn(-1f, 1f)
            pressY = (offset.y / size.height * 2f - 1f).coerceIn(-1f, 1f)
        }
        scope.launch {
            depth.animateTo(
                targetValue = 1f,
                animationSpec = RodinInteractionSettings.interactionSpring,
                initialVelocity = depth.velocity.coerceIn(-4f, 4f) + 5f,
            )
        }
    }

    fun release() {
        lightController.cancel()
        if (reduced) {
            scope.launch { depth.snapTo(0f) }
            return
        }
        scope.launch {
            depth.animateTo(
                targetValue = 0f,
                animationSpec = RodinInteractionSettings.interactionSpring,
                initialVelocity = depth.velocity.coerceIn(-4f, 4f),
            )
        }
    }

    Box(
        modifier.pointerInput(onTap, enableHaptics) {
            detectTapGestures(
                onPress = { offset ->
                    press(offset, size)
                    val released = tryAwaitRelease()
                    release()
                    if (released) {
                        if (enableHaptics) RodinHaptics.tap()
                        onTap()
                    }
                },
            )
        },
    ) {
        val depthValue = if (reduced) 0f else depth.value.coerceIn(-0.08f, 1.04f)
        val scale = lerp(1f, RodinInteractionSettings.pressScale.toFloat(), depthValue)
        val opacity = lerp(
            1f,
            RodinInteractionSettings.pressOpacity.toFloat(),
            depthValue.coerceIn(0f, 1f),
        ).coerceIn(0f, 1f)
        val transformed: @Composable () -> Unit = {
            Box(
                Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    // -_pressPoint.dy * depth * 0.006 radians, in degrees.
                    rotationX = -pressY * depthValue * 0.3438f
                    rotationY = pressX * depthValue * 0.3438f
                    alpha = opacity
                },
            ) {
                content()
            }
        }
        if (enableLight) {
            RodinTouchLight(
                accent = appearance.activeAccent,
                radius = appearance.cardRadius.toFloat(),
                controller = lightController,
                content = transformed,
            )
        } else {
            transformed()
        }
    }
}
