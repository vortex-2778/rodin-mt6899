package io.github.neeschal.rodinessential.ui.shell

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.neeschal.rodinessential.ui.motion.rodinAnimationsDisabled

/** One geometry contract for the floating dock, scroll tails and feedback. */
object RodinBottomLayout {
    const val DOCK_HEIGHT = 68f
    const val DOCK_GAP = 12f
    const val DOCK_FADE = 24f
    const val FEEDBACK_GAP = 12f

    // The platform can report zero insets; keep gesture-strip clearance.
    fun navigationClearance(bottomInset: Float): Float = maxOf(bottomInset, 16f)

    fun dockExtent(bottomInset: Float): Float =
        navigationClearance(bottomInset) + DOCK_GAP + DOCK_HEIGHT + DOCK_FADE

    fun contentClearance(bottomPadding: Float, bottomInset: Float): Float =
        maxOf(bottomPadding, dockExtent(bottomInset)) + FEEDBACK_GAP
}

private val feedbackEaseOutCubic = CubicBezierEasing(0.215f, 0.61f, 0.355f, 1f)

/** Shared GPU/CPU/memory feedback. Does not capture taps or claim success. */
@Composable
fun RodinFeedbackOverlay(
    tag: String,
    message: String,
    icon: ImageVector,
    accent: Color,
    visible: Boolean,
    modifier: Modifier = Modifier,
    durationMillis: Int = 260,
) {
    val reducedMotion = rodinAnimationsDisabled()
    val slide = animateFloatAsState(
        targetValue = if (visible) 0f else 0.08f,
        animationSpec = if (reducedMotion) snap() else tween(durationMillis, easing = feedbackEaseOutCubic),
        label = "rodin-feedback-slide",
    )
    val alpha = animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = if (reducedMotion) snap() else tween(durationMillis, easing = feedbackEaseOutCubic),
        label = "rodin-feedback-alpha",
    )
    Box(
        modifier
            .semantics { liveRegion = LiveRegionMode.Polite }
            .graphicsLayer {
                // Move only a few pixels, never slide through the navigation dock.
                translationY = slide.value * size.height
                this.alpha = alpha.value
            },
    ) {
        RodinFeedbackCard(tag = tag, message = message, icon = icon, accent = accent)
    }
}

@Composable
fun RodinFeedbackCard(
    tag: String,
    message: String,
    icon: ImageVector,
    accent: Color,
) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.surface.luminance() < 0.5f
    val radius = RoundedCornerShape(20.dp)
    val shadowColor = Color.Black.copy(alpha = if (dark) 0.22f else 0.08f)
    val gradientStart = accent.copy(alpha = 0.07f)
        .compositeOver(colors.surface.copy(alpha = 0.96f))
    val gradientEnd = colors.surfaceContainerHigh.copy(alpha = 0.94f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = radius,
                ambientColor = shadowColor,
                spotColor = shadowColor,
            )
            .clip(radius)
            .background(Brush.linearGradient(listOf(gradientStart, gradientEnd)))
            .border(BorderStroke(1.dp, accent.copy(alpha = 0.24f)), radius)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(accent.copy(alpha = 0.12f))
                .border(BorderStroke(1.dp, accent.copy(alpha = 0.16f)), RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = tag,
                maxLines = Int.MAX_VALUE,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.W800,
                    letterSpacing = 0.6.sp,
                    color = accent,
                ),
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    lineHeight = 1.3.em,
                    fontWeight = FontWeight.W600,
                    color = colors.onSurface,
                ),
            )
        }
    }
}
