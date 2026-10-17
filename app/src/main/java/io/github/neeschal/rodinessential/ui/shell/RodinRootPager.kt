package io.github.neeschal.rodinessential.ui.shell

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.IgnorePointer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.semantics.clearAndSetSemantics
import io.github.neeschal.rodinessential.ui.motion.LocalRodinMotionViewportInFlight
import io.github.neeschal.rodinessential.ui.motion.rodinAnimationsDisabled
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * A tab tap has one destination, not a sightseeing tour of the other hubs.
 * NEESCHAL: keep the route direct, and keep the user's page state intact.
 *
 * Pages not part of the handoff still travel one viewport apart, so a
 * rounded index never crosses over the active surface mid-flight.
 */
class RodinTabHandoff(
    val startPage: Float,
    val target: Int,
    positions: Map<Int, Float>,
    direction: Int,
) {
    private val starts: MutableMap<Int, Float> = positions.toMutableMap()
    private val ends: MutableMap<Int, Float> = positions.keys
        .associateWith { -direction.toFloat() }
        .toMutableMap()

    init {
        starts.putIfAbsent(target, direction.toFloat())
        ends[target] = 0f
    }

    fun positionsAt(page: Float): Map<Int, Float> {
        val distance = target - startPage
        val progress = if (abs(distance) < 0.00001f) {
            1f
        } else {
            ((page - startPage) / distance).coerceIn(0f, 1f)
        }
        return buildMap {
            for ((index, start) in starts) {
                val end = ends.getValue(index)
                put(index, start + (end - start) * progress)
            }
        }
    }
}

/**
 * Page position remains the single source of drag/fling and dock progress.
 * The content planes are translated from that position, so a direct tab
 * handoff never mounts an intermediate page and never reparents a visited
 * page to a fresh subtree.
 */
@Stable
class RodinRootPagerState(
    val pageCount: Int,
    initialPage: Int = 0,
) {
    var page by mutableFloatStateOf(initialPage.toFloat())
        private set

    var handoff by mutableStateOf<RodinTabHandoff?>(null)
        private set

    /** Pages whose content has ever been on screen; they keep their state. */
    var visited by mutableStateOf(setOf(initialPage))
        private set

    private var motion: Job? = null

    val moving: Boolean
        get() = handoff != null || abs(page - page.roundToInt()) > 0.00001f

    fun positionsAt(atPage: Float): Map<Int, Float> =
        handoff?.positionsAt(atPage) ?: buildMap {
            for (index in 0 until pageCount) {
                val offset = index - atPage
                if (abs(offset) < 1.00001f) put(index, offset)
            }
        }

    /** The page whose surface is closest to the viewport center. */
    fun visibleIndex(): Int {
        val positions = positionsAt(page)
        val closest = positions.keys.minByOrNull { abs(positions.getValue(it)) }
        return (closest ?: page.roundToInt()).coerceIn(0, pageCount - 1)
    }

    fun markVisited(indices: Collection<Int>) {
        val merged = visited + indices
        if (merged.size != visited.size) visited = merged
    }

    fun dragByFingerDelta(deltaPx: Float, widthPx: Float) {
        if (widthPx <= 0f) return
        stopMotion()
        page = (page - deltaPx / widthPx).coerceIn(0f, (pageCount - 1).toFloat())
    }

    fun dragByDockPosition(dxPx: Float, widthPx: Float) {
        if (widthPx <= 0f) return
        stopMotion()
        page = dockPageFor(dxPx, widthPx)
    }

    fun prepareJump(target: Int) {
        // Capture the current geometry, not the previous animation's endpoint.
        // Reversing A to B to A therefore starts where the surfaces actually are.
        val positions = positionsAt(page).toMutableMap()
        positions.entries.removeAll { abs(it.value) >= 0.99999f }
        handoff = RodinTabHandoff(
            startPage = page,
            target = target,
            positions = positions,
            direction = if (target >= page) 1 else -1,
        )
    }

    fun finishJump() {
        handoff = null
    }

    fun jumpToPage(target: Int) {
        stopMotion()
        page = target.coerceIn(0, pageCount - 1).toFloat()
    }

    fun animateToPage(
        target: Int,
        durationMillis: Int,
        scope: CoroutineScope,
        onComplete: () -> Unit,
    ) {
        stopMotion()
        motion = scope.launch {
            val clamped = target.coerceIn(0, pageCount - 1).toFloat()
            if (durationMillis <= 0) {
                page = clamped
            } else {
                Animatable(page).animateTo(
                    targetValue = clamped,
                    animationSpec = tween(durationMillis, easing = pageEaseOutCubic),
                ) {
                    page = value
                }
            }
            onComplete()
        }
    }

    /**
     * Flutter's PageScrollPhysics: a real fling moves half a page past the
     * current position before rounding; a slow release rounds in place.
     */
    fun settleFromFling(
        velocityPxPerSecond: Float,
        widthPx: Float,
        scope: CoroutineScope,
        reduced: Boolean,
    ) {
        if (widthPx <= 0f) return
        val scrollVelocity = -velocityPxPerSecond
        val adjusted = if (abs(scrollVelocity) > 50f) {
            page + (if (scrollVelocity > 0f) 0.5f else -0.5f)
        } else {
            page
        }
        val target = adjusted.roundToInt().coerceIn(0, pageCount - 1)
        stopMotion()
        motion = scope.launch {
            if (reduced) {
                page = target.toFloat()
                return@launch
            }
            Animatable(page).animateTo(
                targetValue = target.toFloat(),
                animationSpec = spring(
                    dampingRatio = 0.98f,
                    stiffness = 323f,
                    visibilityThreshold = 0.0001f,
                ),
            ) {
                page = value
            }
        }
    }

    private fun stopMotion() {
        motion?.cancel()
        motion = null
    }
}

private val pageEaseOutCubic = CubicBezierEasing(0.215f, 0.61f, 0.355f, 1f)

/** The finger's dock position selects the page, like the original. */
private fun dockPageFor(dxPx: Float, widthPx: Float): Float {
    val itemWidth = (widthPx / 4f).coerceAtLeast(1f)
    return ((dxPx / itemWidth) - 0.5f).coerceIn(0f, 3f)
}

/**
 * The page surfaces are stable keyed slots: offstage planes keep their state
 * but do not paint, do not take pointers and do not tick.
 */
@Composable
fun RodinRootPager(
    state: RodinRootPagerState,
    swipeEnabled: Boolean,
    onDragStart: () -> Unit,
    modifier: Modifier = Modifier,
    page: @Composable (Int) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val reduced = rodinAnimationsDisabled()

    LaunchedEffect(state) {
        snapshotFlow { state.page to state.handoff }.collect { (current, handoff) ->
            val positions = state.positionsAt(current)
            val additions = positions.filterValues { abs(it) < 1f }.keys.toMutableList()
            handoff?.let { additions.add(it.target) }
            state.markVisited(additions)
        }
    }

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .clipToBounds()
            .pointerInput(swipeEnabled) {
                if (!swipeEnabled) return@pointerInput
                val tracker = VelocityTracker()
                detectHorizontalDragGestures(
                    onDragStart = { onDragStart() },
                    onHorizontalDrag = { change, dragAmount ->
                        tracker.addPointerInputChange(change)
                        state.dragByFingerDelta(dragAmount, size.width.toFloat())
                    },
                    onDragEnd = {
                        state.settleFromFling(
                            velocityPxPerSecond = tracker.calculateVelocity().x,
                            widthPx = size.width.toFloat(),
                            scope = scope,
                            reduced = reduced,
                        )
                    },
                    onDragCancel = {
                        state.settleFromFling(
                            velocityPxPerSecond = 0f,
                            widthPx = size.width.toFloat(),
                            scope = scope,
                            reduced = reduced,
                        )
                    },
                )
            },
    ) {
        val width = constraints.maxWidth.toFloat()
        val currentPage = state.page
        val positions = state.positionsAt(currentPage)
        val active = state.visibleIndex()
        val moving = state.moving
        Box(Modifier.fillMaxSize()) {
            for (index in 0 until state.pageCount) {
                val offset = positions[index] ?: 2f
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { translationX = offset * width }
                        .then(
                            if (moving || index != active) {
                                Modifier.clearAndSetSemantics {}
                            } else {
                                Modifier
                            },
                        ),
                ) {
                    IgnorePointer(ignoring = moving || index != active) {
                        CompositionLocalProvider(
                            LocalRodinMotionViewportInFlight provides moving,
                        ) {
                            if (state.visited.contains(index)) {
                                page(index)
                            } else {
                                Box(Modifier.fillMaxSize())
                            }
                        }
                    }
                }
            }
        }
    }
}
