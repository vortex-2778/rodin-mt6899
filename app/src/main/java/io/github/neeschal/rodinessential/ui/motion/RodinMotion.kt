package io.github.neeschal.rodinessential.ui.motion

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * One restrained motion vocabulary. NEESCHAL: a control should feel alive,
 * but it should never make the user wait for the decoration to finish.
 */
object RodinMotion {
    /** mass 1, stiffness 500, ratio 0.94 */
    val settle: SpringSpec<Float> = spring(dampingRatio = 0.94f, stiffness = 500f)

    /** mass 0.8, stiffness 420, ratio 0.86 */
    val reveal: SpringSpec<Float> = spring(dampingRatio = 0.86f, stiffness = 420f)

    /** The confirmation dialog spring: mass 1, stiffness 330, damping 29. */
    val dialog: SpringSpec<Float> = spring(dampingRatio = 0.798f, stiffness = 330f)
}

object RodinBackMotion {
    var progress by mutableStateOf(0f)
    var nested = false
    var rightEdge = false
    var committedProgress = 0f
}

/**
 * Bounded full-screen preview, shared by the gesture and its release animation.
 * A 90% surface leaves room for an 8dp edge gap; it must not follow the finger
 * all the way off-screen before Android has decided whether Back commits.
 */
object RodinBackGeometry {
    /** Flutter's Curves.easeOutCubic, evaluated with the same cubic bezier. */
    private val easeOutCubic = CubicBezierEasing(0.215f, 0.61f, 0.355f, 1f)

    fun eased(progress: Float): Float = easeOutCubic.transform(progress.coerceIn(0f, 1f))

    fun scale(progress: Float): Float = 1f - 0.10f * eased(progress)

    fun radius(progress: Float): Float = 20f * eased(progress)

    fun maxShift(width: Float): Float = (width * 0.05f - 8f).coerceAtLeast(0f)

    fun shift(width: Float, progress: Float, rightEdge: Boolean): Float =
        (if (rightEdge) -1f else 1f) * maxShift(width) * eased(progress)
}

/**
 * Spend the opening frame on visible controls, not speculative scroll cache.
 * This value changes only at the boundaries of a transition.
 */
val LocalRodinMotionViewportInFlight = compositionLocalOf { false }

/** Velocity supplied by scroll containers; read by [RodinInertiaLayer]. */
val LocalRodinScrollVelocity = compositionLocalOf<MutableState<Float>?> { null }

@Composable
fun rodinAnimationsDisabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) == 0f
        } catch (_: Exception) {
            false
        }
    }
}

@Composable
fun rodinMotionBackgroundColor(): Color =
    if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) {
        Color(0xFF090C11)
    } else {
        Color(0xFFF3F6FA)
    }

/**
 * Short, bounded arrivals; controls remain hit-testable during the reveal.
 * No animation survives at rest, and reduced-motion skips both delay and spring.
 */
@Composable
fun RodinArrival(
    order: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val inFlight = LocalRodinMotionViewportInFlight.current
    val reduced = rodinAnimationsDisabled()
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (reduced || inFlight) {
            progress.snapTo(1f)
            return@LaunchedEffect
        }
        delay((order * 8).coerceIn(0, 48).toLong())
        progress.animateTo(1f, animationSpec = RodinMotion.reveal)
    }
    val p = progress.value.coerceIn(0f, 1.04f)
    Box(
        modifier.graphicsLayer {
            transformOrigin = TransformOrigin(0.5f, 0f)
            translationY = 7f * (1f - p)
            scaleX = 0.995f + 0.005f * p
            scaleY = 0.995f + 0.005f * p
            alpha = p.coerceIn(0f, 1f)
        },
    ) {
        content()
    }
}

/**
 * Progress comes from Android's edge gesture, not a simulated timed swipe.
 */
@Composable
fun RodinPredictivePlane(
    modifier: Modifier = Modifier,
    nested: Boolean = false,
    destination: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val reduced = rodinAnimationsDisabled()
    val value = if (RodinBackMotion.nested == nested && !reduced) {
        RodinBackMotion.progress
    } else {
        0f
    }
    val background = rodinMotionBackgroundColor()
    Box(modifier.fillMaxSize()) {
        if (destination != null && value > 0f) {
            Box(Modifier.fillMaxSize()) {
                Surface(color = background) { destination() }
            }
        }
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = RodinBackGeometry.shift(
                        size.width,
                        value,
                        RodinBackMotion.rightEdge,
                    )
                    val scale = RodinBackGeometry.scale(value)
                    scaleX = scale
                    scaleY = scale
                    shape = RoundedCornerShape(RodinBackGeometry.radius(value).dp)
                    clip = true
                },
        ) {
            CompositionLocalProvider(
                LocalRodinMotionViewportInFlight provides (
                    value > 0f || LocalRodinMotionViewportInFlight.current
                    ),
            ) {
                Surface(color = background) { content() }
            }
        }
    }
}

/**
 * One restrained reveal for confirmation dialogs.
 */
@Composable
fun RodinDialog(
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val reduced = rodinAnimationsDisabled()
        val progress = remember { Animatable(if (reduced) 1f else 0f) }
        LaunchedEffect(Unit) {
            if (!reduced) progress.animateTo(1f, animationSpec = RodinMotion.dialog)
        }
        val p = progress.value.coerceIn(0f, 1f)
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.42f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissRequest,
                ),
            contentAlignment = androidx.compose.ui.Alignment.Center,
        ) {
            Box(
                Modifier
                    .safeDrawingPadding()
                    .graphicsLayer {
                        alpha = p
                        translationY = size.height * 0.025f * (1f - p)
                        scaleX = 0.975f + 0.025f * p
                        scaleY = 0.975f + 0.025f * p
                    },
            ) {
                content()
            }
        }
    }
}

/**
 * A solid page plane: outgoing text must never blend with the page underneath.
 * The caller supplies the raw transition value and its direction, so both a
 * forward push and a back gesture evaluate the same geometry.
 */
@Composable
fun RodinDetailTransition(
    progress: Float,
    reverse: Boolean,
    active: Boolean,
    forward: Boolean = true,
    previewThrough: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val reduced = rodinAnimationsDisabled()
    val direction = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f
    val background = rodinMotionBackgroundColor()
    // Capture the gesture's final geometry once for this outgoing transition;
    // later gestures must not change a page which is already departing.
    val committed = if (!forward && !active) RodinBackMotion.committedProgress else 0f
    val gestureEdge = if (RodinBackMotion.rightEdge) -1f else 1f
    val inFlight = !reduced && progress > 0.0001f && progress < 0.9999f
    val raw = progress.coerceIn(0f, 1f)
    val eased = when {
        reduced -> 1f
        reverse -> 1f - RodinBackGeometry.eased(1f - raw)
        else -> RodinBackGeometry.eased(raw)
    }
    val travel = if (forward) {
        if (active) 1f else -0.10f
    } else {
        if (active) -0.10f else 1f
    }
    BoxWithConstraints(modifier.fillMaxSize()) {
        val width = constraints.maxWidth.toFloat()
        val startShift = if (width > 0f) {
            RodinBackGeometry.maxShift(width) * RodinBackGeometry.eased(committed) / width
        } else {
            0f
        }
        val translation = if (committed > 0f) {
            gestureEdge * (startShift + (1f - startShift) * (1f - eased))
        } else {
            direction * travel * (1f - eased)
        }
        val scale = when {
            reduced -> 1f
            committed > 0f -> RodinBackGeometry.scale(committed)
            else -> 0.985f + 0.015f * eased
        }
        val radius = if (committed > 0f) {
            RodinBackGeometry.radius(committed) +
                (20f - RodinBackGeometry.radius(committed)) * (1f - eased)
        } else {
            12f * (1f - eased).coerceIn(0f, 1f)
        }
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = translation * size.width
                    scaleX = scale
                    scaleY = scale
                    shape = RoundedCornerShape(radius.dp)
                    clip = true
                },
        ) {
            CompositionLocalProvider(
                LocalRodinMotionViewportInFlight provides (
                    inFlight || LocalRodinMotionViewportInFlight.current
                    ),
            ) {
                if (previewThrough) {
                    content()
                } else {
                    Surface(color = background) { content() }
                }
            }
        }
    }
}

/**
 * Velocity supplies the impulse; the spring supplies the return to rest.
 * No perpetual animation.
 */
@Composable
fun RodinInertiaLayer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val reduced = rodinAnimationsDisabled()
    val velocity = LocalRodinScrollVelocity.current
    val depth = remember { Animatable(0f) }
    LaunchedEffect(velocity, reduced) {
        if (reduced || velocity == null) {
            depth.snapTo(0f)
            return@LaunchedEffect
        }
        snapshotFlow { velocity.value }.collect { value ->
            val target = (value / 4000f).coerceIn(-1f, 1f)
            launch {
                depth.animateTo(
                    targetValue = target,
                    animationSpec = spring(dampingRatio = 0.9f, stiffness = 280f),
                    initialVelocity = depth.velocity.coerceIn(-4f, 4f),
                )
            }
        }
    }
    val d = if (reduced) 0f else depth.value.coerceIn(-1f, 1f)
    Box(
        modifier.graphicsLayer {
            translationY = d * 3f
            rotationX = d * 0.2292f // depth * 0.004 radians around the center
        },
    ) {
        content()
    }
}
