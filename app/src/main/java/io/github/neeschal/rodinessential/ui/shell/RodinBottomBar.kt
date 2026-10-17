package io.github.neeschal.rodinessential.ui.shell

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asFrameworkPaint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.neeschal.rodinessential.ui.RodinScreen
import io.github.neeschal.rodinessential.ui.components.PressScale
import io.github.neeschal.rodinessential.ui.components.RodinHaptics
import io.github.neeschal.rodinessential.ui.components.RodinTouchLight
import io.github.neeschal.rodinessential.ui.motion.rodinAnimationsDisabled
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

private val BOTTOM_SCREENS = listOf(
    RodinScreen.home,
    RodinScreen.hubs,
    RodinScreen.support,
    RodinScreen.settings,
)

private val BOTTOM_LABELS = listOf("Home", "Hubs", "Support", "Settings")

private val BOTTOM_ICONS: List<ImageVector> = listOf(
    Icons.Rounded.Home,
    Icons.Rounded.GridView,
    Icons.Rounded.Favorite,
    Icons.Rounded.Settings,
)

private val SECTION_COLORS = listOf(
    Color(0xFF2563EB), // Home: Electric Azure
    Color(0xFF10B981), // Hubs: Vivid Emerald
    Color(0xFFF43F5E), // Support: Neon Rose
    Color(0xFF8B5CF6), // Settings: Cyber Violet
)

private val dockEaseOutCubic = CubicBezierEasing(0.215f, 0.61f, 0.355f, 1f)

/**
 * The floating dock: four destinations, one elastic indicator and a
 * drag handoff to the root pager. A tab tap has one destination, not a
 * sightseeing tour of the other hubs.
 */
@Composable
fun RodinBottomBar(
    pagerState: PagerState,
    currentRoot: RodinScreen,
    onSelect: (RodinScreen) -> Unit,
    onDragStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.surface.luminance() < 0.5f
    val reduced = rodinAnimationsDisabled()
    val scope = rememberCoroutineScope()
    val layoutDirection = LocalLayoutDirection.current

    val safePadding = WindowInsets.safeDrawing.asPaddingValues()
    val navPadding = WindowInsets.navigationBars.asPaddingValues()
    val bottomClearance = RodinBottomLayout.navigationClearance(
        navPadding.calculateBottomPadding().value,
    )

    val elasticity = remember { Animatable(0f) }
    var barWidthPx by remember { mutableFloatStateOf(1f) }
    var dragging by remember { mutableStateOf(false) }
    var lastDragIndex by remember { mutableStateOf<Int?>(null) }

    fun pageFraction(): Float {
        val offset = pagerState.currentPageOffsetFraction
        val safeOffset = if (offset.isFinite()) offset else 0f
        return (pagerState.currentPage + safeOffset).coerceIn(0f, 3f)
    }

    fun settleDrag(velocity: Float) {
        if (!dragging) return
        dragging = false
        lastDragIndex = null
        val page = pageFraction()
        val itemWidth = (barWidthPx / BOTTOM_SCREENS.size).coerceAtLeast(1f)
        val projected = page + (velocity / itemWidth * 0.06f).coerceIn(-0.5f, 0.5f)
        val target = projected.roundToInt().coerceIn(0, BOTTOM_SCREENS.size - 1)
        scope.launch {
            if (reduced) {
                elasticity.snapTo(0f)
            } else {
                elasticity.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(dampingRatio = 0.685f, stiffness = 360f),
                    initialVelocity = -elasticity.value * 0.8f,
                )
            }
        }
        RodinHaptics.confirm()
        onSelect(BOTTOM_SCREENS[target])
    }

    val draggableState = rememberDraggableState { delta ->
        // Ease the decorative stretch, never the finger-owned page position.
        val targetElasticity = (delta / 13f).coerceIn(-1f, 1f)
        scope.launch {
            if (reduced) {
                elasticity.snapTo(0f)
            } else {
                elasticity.animateTo(targetElasticity, tween(90, easing = dockEaseOutCubic))
            }
        }
        pagerState.dispatchRawDelta(delta)
        val index = pageFraction().roundToInt().coerceIn(0, BOTTOM_SCREENS.size - 1)
        val previous = lastDragIndex
        if (previous != null && previous != index) RodinHaptics.frequentSegment()
        lastDragIndex = index
    }

    val selectedIndex = BOTTOM_SCREENS.indexOf(currentRoot).coerceAtLeast(0)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = (10f + safePadding.calculateLeftPadding(layoutDirection).value).dp,
                end = (10f + safePadding.calculateRightPadding(layoutDirection).value).dp,
                bottom = (bottomClearance + RodinBottomLayout.DOCK_GAP).dp,
            )
            .onSizeChanged { barWidthPx = it.width.toFloat() },
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .rodinDockShadow(RoundedCornerShape(22.dp), dark, colors.primary)
                .clip(RoundedCornerShape(22.dp)),
        ) {
            RodinTouchLight(accent = SECTION_COLORS[selectedIndex], radius = 22f) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(RodinBottomLayout.DOCK_HEIGHT.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.linearGradient(
                                colorStops = arrayOf(
                                    0f to Color.White.copy(alpha = if (dark) 0.13f else 0.80f),
                                    0.50f to colors.surface.copy(alpha = if (dark) 0.66f else 0.64f),
                                    1f to colors.surfaceContainerHighest.copy(
                                        alpha = if (dark) 0.50f else 0.42f,
                                    ),
                                ),
                            ),
                        )
                        .border(
                            width = 0.9.dp,
                            color = Color.White.copy(alpha = if (dark) 0.19f else 0.88f),
                            shape = RoundedCornerShape(22.dp),
                        )
                        .draggable(
                            state = draggableState,
                            orientation = Orientation.Horizontal,
                            onDragStarted = {
                                onDragStart()
                                dragging = true
                                lastDragIndex = null
                                scope.launch {
                                    elasticity.stop()
                                    elasticity.snapTo(0f)
                                }
                                RodinHaptics.segment()
                            },
                            onDragStopped = { velocity -> settleDrag(velocity) },
                        )
                        .drawWithContent {
                            drawContent()
                            // Hairline highlight along the top edge.
                            drawLine(
                                color = Color.White.copy(alpha = if (dark) 0.12f else 0.62f),
                                start = Offset(12.dp.toPx(), 0.35.dp.toPx()),
                                end = Offset(size.width - 12.dp.toPx(), 0.35.dp.toPx()),
                                strokeWidth = 0.7.dp.toPx(),
                                cap = StrokeCap.Round,
                            )
                        }
                        .padding(6.dp),
                ) {
                    BoxWithConstraints(Modifier.fillMaxSize()) {
                        val itemWidth = maxWidth / BOTTOM_SCREENS.size
                        val page = pageFraction()
                        val lower = floor(page).toInt().coerceIn(0, BOTTOM_SCREENS.size - 1)
                        val upper = ceil(page).toInt().coerceIn(0, BOTTOM_SCREENS.size - 1)
                        val indicatorColor = lerp(
                            SECTION_COLORS[lower],
                            SECTION_COLORS[upper],
                            page - lower,
                        )
                        val elasticityValue = elasticity.value
                        val absElasticity = abs(elasticityValue)
                        val indicatorRadius = (17f - absElasticity * 1.0f).dp
                        val indicatorLeft = itemWidth * page + 3.dp -
                            (if (elasticityValue < 0f) (absElasticity * 11f).dp else 0.dp)
                        val indicatorWidth = itemWidth - 6.dp + (absElasticity * 11f).dp

                        Box(Modifier.fillMaxSize()) {
                            Box(
                                Modifier
                                    .offset(x = indicatorLeft, y = 1.dp)
                                    .width(indicatorWidth)
                                    .height(54.dp)
                                    .graphicsLayer { scaleY = 1f - absElasticity * 0.028f }
                                    .shadowedIndicator(indicatorRadius, indicatorColor, dark)
                                    .background(
                                        Brush.linearGradient(
                                            colorStops = arrayOf(
                                                0f to Color.White.copy(
                                                    alpha = if (dark) 0.13f else 0.48f,
                                                ),
                                                0.52f to indicatorColor.copy(
                                                    alpha = if (dark) 0.14f else 0.10f,
                                                ),
                                                1f to colors.surface.copy(
                                                    alpha = if (dark) 0.28f else 0.34f,
                                                ),
                                            ),
                                        ),
                                        RoundedCornerShape(indicatorRadius),
                                    )
                                    .border(
                                        width = 0.7.dp,
                                        color = lerp(
                                            Color.White.copy(alpha = if (dark) 0.14f else 0.52f),
                                            indicatorColor.copy(alpha = if (dark) 0.24f else 0.20f),
                                            0.45f,
                                        ),
                                        shape = RoundedCornerShape(indicatorRadius),
                                    ),
                            )
                            Row(Modifier.fillMaxSize()) {
                                BOTTOM_SCREENS.forEachIndexed { index, screen ->
                                    val rawFocus = (1f - abs(page - index)).coerceIn(0f, 1f)
                                    val focus = RodinInteractionSettings.transitionCurve
                                        .transform(rawFocus)
                                    val itemColor = SECTION_COLORS[index]
                                    val foreground = lerp(
                                        colors.onSurfaceVariant.copy(alpha = 0.72f),
                                        itemColor,
                                        focus,
                                    )
                                    Box(
                                        Modifier
                                            .weight(1f)
                                            .padding(horizontal = 2.dp),
                                    ) {
                                        PressScale(
                                            enableLight = false,
                                            onTap = { onSelect(screen) },
                                        ) {
                                            Box(
                                                Modifier
                                                    .fillMaxWidth()
                                                    .height(56.dp),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Column(
                                                    modifier = Modifier.graphicsLayer {
                                                        scaleX = 0.96f + 0.04f * focus
                                                        scaleY = 0.96f + 0.04f * focus
                                                    },
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                ) {
                                                    Icon(
                                                        imageVector = BOTTOM_ICONS[index],
                                                        contentDescription = null,
                                                        tint = foreground,
                                                        modifier = Modifier.size(21.dp),
                                                    )
                                                    Spacer(Modifier.height(2.dp))
                                                    Text(
                                                        text = BOTTOM_LABELS[index],
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        overflow = TextOverflow.Ellipsis,
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontSize = 9.5.sp,
                                                            lineHeight = 9.5.sp,
                                                            fontWeight = if (focus > 0.5f) {
                                                                FontWeight.W800
                                                            } else {
                                                                FontWeight.W600
                                                            },
                                                            letterSpacing = (-0.08).sp,
                                                            color = foreground,
                                                        ),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The dock's two-layer shadow: a deep drop and a faint tinted glow. */
private fun Modifier.rodinDockShadow(shape: Shape, dark: Boolean, primary: Color): Modifier =
    this.drawWithContent {
        val outline = shape.createOutline(size, layoutDirection, this)
        drawIntoCanvas { canvas ->
            val drop = Paint().asFrameworkPaint().apply {
                isAntiAlias = true
                color = android.graphics.Color.TRANSPARENT
                setShadowLayer(
                    30f,
                    0f,
                    11f,
                    Color.Black.copy(alpha = if (dark) 0.34f else 0.16f).toArgb(),
                )
            }
            canvas.nativeCanvas.drawOutline(outline, drop)
            val glow = Paint().asFrameworkPaint().apply {
                isAntiAlias = true
                color = android.graphics.Color.TRANSPARENT
                setShadowLayer(
                    18f,
                    0f,
                    3f,
                    primary.copy(alpha = if (dark) 0.06f else 0.04f).toArgb(),
                )
            }
            canvas.nativeCanvas.drawOutline(outline, glow)
        }
        drawContent()
    }

/** The indicator's tinted glow, kept below the gradient fill. */
private fun Modifier.shadowedIndicator(
    radius: androidx.compose.ui.unit.Dp,
    indicatorColor: Color,
    dark: Boolean,
): Modifier = this.drawWithContent {
    val outline = RoundedCornerShape(radius).createOutline(size, layoutDirection, this)
    drawIntoCanvas { canvas ->
        val paint = Paint().asFrameworkPaint().apply {
            isAntiAlias = true
            color = android.graphics.Color.TRANSPARENT
            setShadowLayer(
                12f,
                0f,
                4f,
                indicatorColor.copy(alpha = if (dark) 0.10f else 0.07f).toArgb(),
            )
        }
        canvas.nativeCanvas.drawOutline(outline, paint)
    }
    drawContent()
}
