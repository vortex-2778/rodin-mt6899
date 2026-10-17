package io.github.neeschal.rodinessential.ui.screens

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import io.github.neeschal.rodinessential.ui.RodinScreen
import io.github.neeschal.rodinessential.ui.components.PressScale
import io.github.neeschal.rodinessential.ui.components.RodinHaptics
import io.github.neeschal.rodinessential.ui.components.SurfaceCard
import io.github.neeschal.rodinessential.ui.motion.LocalRodinScrollVelocity
import io.github.neeschal.rodinessential.ui.shell.RodinBottomLayout

private val morphEaseInOutCubic = CubicBezierEasing(0.645f, 0.045f, 0.355f, 1f)
private val morphEaseOutCubic = CubicBezierEasing(0.215f, 0.61f, 0.355f, 1f)

/**
 * The page header the scroll scaffold morphs into the frosted glass bar.
 * Compose cannot introspect a child widget, so the kind is explicit.
 */
sealed class RodinPageHeader {
    abstract val title: String

    class Home(
        override val title: String,
        val subtitle: String,
        val large: Boolean = false,
    ) : RodinPageHeader()

    class Detail(
        override val title: String,
        val onBack: () -> Unit,
    ) : RodinPageHeader()
}

/**
 * One scroll frame for every page: safe insets, the dock's content clearance
 * and an optional glass header that takes over as content slides beneath it.
 */
@Composable
fun RodinScrollPage(
    modifier: Modifier = Modifier,
    header: RodinPageHeader? = null,
    topPadding: Dp = 10.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scrollState = rememberScrollState()
    val colors = MaterialTheme.colorScheme
    val dark = colors.surface.luminance() < 0.5f
    val velocity = remember { mutableFloatStateOf(0f) }

    // The shell already cleared the cutout; inside it this is normally zero.
    val safeTop = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
    val bottomPadding = WindowInsets.navigationBars
        .asPaddingValues()
        .calculateBottomPadding()
        .value
    val bottomInset = WindowInsets.safeDrawing
        .asPaddingValues()
        .calculateBottomPadding()
        .value
    val bottomClearance = RodinBottomLayout
        .contentClearance(bottomPadding = bottomPadding, bottomInset = bottomInset)
        .dp

    LaunchedEffect(scrollState) {
        var lastValue = scrollState.value
        var lastNanos = System.nanoTime()
        snapshotFlow { scrollState.value }.collect { value ->
            val now = System.nanoTime()
            val dt = (now - lastNanos) / 1_000_000_000f
            if (dt > 0f) {
                velocity.floatValue = ((value - lastValue) / dt).coerceIn(-6500f, 6500f)
            }
            lastValue = value
            lastNanos = now
        }
    }
    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.isScrollInProgress }.collect { scrolling ->
            if (!scrolling) velocity.floatValue = 0f
        }
    }

    // A firm tick the moment the page runs out of scroll, once per edge.
    val edgeHaptics = remember { EdgeHapticLatch() }
    val edgeConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                edgeHaptics.onOverscroll(available.y)
                return Offset.Zero
            }
        }
    }

    val pixels = scrollState.value.coerceIn(0, 96)
    val morph = morphEaseInOutCubic.transform((pixels / 58f).coerceIn(0f, 1f))

    Box(modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalRodinScrollVelocity provides velocity) {
            Column(
                Modifier
                    .fillMaxSize()
                    .nestedScroll(edgeConnection)
                    .verticalScroll(scrollState)
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = safeTop + topPadding,
                        bottom = bottomClearance,
                    ),
            ) {
                when (header) {
                    is RodinPageHeader.Home -> {
                        Spacer(Modifier.height(if (header.large) 38.dp else 32.dp))
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = header.subtitle,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 13.5.sp,
                                color = colors.onSurfaceVariant,
                            ),
                        )
                    }
                    is RodinPageHeader.Detail -> Spacer(Modifier.height(44.dp))
                    null -> Unit
                }
                content()
            }
        }

        if (header != null) {
            // Nothing under the frosted band may receive a tap or a focus.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(safeTop + lerp(topPadding.value + 44f, 42f, morph).dp)
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Main)
                                event.changes.forEach { change -> change.consume() }
                            }
                        }
                    },
            )
            val startFont = when (header) {
                is RodinPageHeader.Home -> if (header.large) 28f else 24f
                is RodinPageHeader.Detail -> 23f
            }
            val startX = when (header) {
                is RodinPageHeader.Detail -> 68f
                else -> 16f
            }
            RodinHeaderGlassMorph(
                scrollPixels = pixels,
                safeTop = safeTop,
                title = header.title,
                startFont = startFont,
                startX = startX,
                topPadding = topPadding,
                onBack = (header as? RodinPageHeader.Detail)?.onBack,
            )
        }
    }
}

private class EdgeHapticLatch {
    private var latched = false

    fun onOverscroll(availableY: Float) {
        if (availableY != 0f) {
            if (!latched) {
                RodinHaptics.frequentSegment()
                latched = true
            }
        } else {
            latched = false
        }
    }
}

/**
 * The frosted band: gradient glass, a chromatic hairline and the title that
 * travels to its compact seat. Compose 1.7 has no public backdrop blur, so
 * the glass keeps its translucency and skips the sampled blur pass.
 */
@Composable
private fun RodinHeaderGlassMorph(
    scrollPixels: Int,
    safeTop: Dp,
    title: String,
    startFont: Float,
    startX: Float,
    topPadding: Dp,
    onBack: (() -> Unit)?,
) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.surface.luminance() < 0.5f
    val rawMorph = (scrollPixels / 58f).coerceIn(0f, 1f)
    val morph = morphEaseInOutCubic.transform(rawMorph)
    val glass = morphEaseOutCubic.transform(((scrollPixels - 7f) / 39f).coerceIn(0f, 1f))
    val titleFont = lerp(startFont, 17.5f, morph)
    val titleX = lerp(startX, if (onBack == null) 14f else 52f, morph)
    val titleY = lerp(safeTop.value + topPadding.value, safeTop.value + 6f, morph)

    Box(Modifier.fillMaxSize()) {
        if (glass > 0.001f) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(safeTop + 42.dp)
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to (if (dark) Color(0xFF1D2735) else Color.White)
                                    .copy(alpha = 0.48f * glass),
                                0.58f to colors.surfaceContainer.copy(alpha = 0.40f * glass),
                                1f to colors.surface.copy(alpha = 0.28f * glass),
                            ),
                        ),
                    )
                    .drawBehind {
                        drawLine(
                            color = colors.outline.copy(alpha = 0.08f * glass),
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = 0.55.dp.toPx(),
                        )
                    },
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = safeTop + 34.dp)
                    .height(8.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                colors.primary.copy(alpha = 0.028f * glass),
                                colors.secondary.copy(alpha = 0.016f * glass),
                                colors.surface.copy(alpha = 0f),
                            ),
                        ),
                    ),
            )
            Box(
                Modifier
                    .padding(
                        start = (18f + 12f * (1f - morph)).dp,
                        end = (28f - 10f * morph).dp,
                        top = safeTop + 41.dp,
                    )
                    .fillMaxWidth()
                    .height(0.8.dp)
                    .background(
                        Brush.horizontalGradient(
                            colorStops = arrayOf(
                                0f to Color.White.copy(alpha = 0f),
                                0.26f to colors.primary.copy(alpha = 0.34f * 0.15f * glass),
                                0.48f to Color.White.copy(
                                    alpha = (if (dark) 0.22f else 0.36f) * 0.15f * glass,
                                ),
                                0.72f to colors.secondary.copy(alpha = 0.24f * 0.15f * glass),
                                1f to Color.White.copy(alpha = 0f),
                            ),
                        ),
                    ),
            )
        }

        if (onBack != null) {
            Box(
                Modifier
                    .padding(
                        start = lerp(12f, 10f, morph).dp,
                        top = lerp(
                            safeTop.value + topPadding.value - 2f,
                            safeTop.value + 3f,
                            morph,
                        ).dp,
                    )
                    .size(lerp(44f, 36f, morph).dp),
                contentAlignment = Alignment.Center,
            ) {
                PressScale(onTap = onBack) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.onSurface,
                            modifier = Modifier.size(lerp(22f, 20f, morph).dp),
                        )
                    }
                }
            }
        }

        Text(
            text = title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(
                start = titleX.dp,
                end = (if (onBack == null) 14f else 12f).dp,
                top = titleY.dp,
            ),
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = titleFont.sp,
                lineHeight = titleFont.sp,
                fontWeight = FontWeight.W900,
                letterSpacing = lerp(-0.85f, -0.25f, morph).sp,
                color = colors.onSurface,
                shadow = if (glass > 0.15f) {
                    Shadow(
                        color = colors.surface.copy(
                            alpha = if (dark) 0.22f else 0.28f,
                        ),
                        blurRadius = 5f * glass,
                    )
                } else {
                    null
                },
            ),
        )
    }
}

/**
 * The placeholder pages that have no live controls yet.
 */
@Composable
fun DetailScreen(
    screen: RodinScreen,
    icon: ImageVector,
    accent: Color,
    subtitle: String,
    onBack: () -> Unit,
) {
    RodinScrollPage(
        header = RodinPageHeader.Detail(screen.title, onBack),
    ) {
        Spacer(Modifier.height(9.dp))
        HeroCard(
            icon = icon,
            accent = accent,
            title = screen.title,
            subtitle = subtitle,
        )
        Spacer(Modifier.height(9.dp))
        SurfaceCard {
            Column {
                Text(
                    text = "Native backend migration",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.W700,
                    ),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "UI structure is active. Hardware controls remain intentionally disconnected until their Rust daemon/backend path is migrated.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
