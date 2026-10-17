package io.github.neeschal.rodinessential.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.neeschal.rodinessential.ui.theme.LocalRodinAppearance
import io.github.neeschal.rodinessential.ui.theme.RodinSurfaceLines

/**
 * Flutter BoxShadow in Compose: the platform shadow layer keeps the exact
 * blur and offset; a negative spread insets the silhouette and a positive
 * one grows it, matching Flutter's box painting.
 */
fun Modifier.rodinShadow(
    color: Color,
    blurRadius: Float,
    offsetX: Float = 0f,
    offsetY: Float = 0f,
    spreadRadius: Float = 0f,
    cornerRadius: Float = 0f,
): Modifier = this.drawBehind {
    if (color.alpha <= 0f) return@drawBehind
    val inset = -spreadRadius
    val left = inset
    val top = inset
    val right = size.width - inset
    val bottom = size.height - inset
    if (right <= left || bottom <= top) return@drawBehind
    val paint = Paint().asFrameworkPaint().apply {
        isAntiAlias = true
        this.color = android.graphics.Color.TRANSPARENT
        setShadowLayer(
            blurRadius.coerceAtLeast(0f),
            offsetX,
            offsetY,
            color.toArgb(),
        )
    }
    val radius = (cornerRadius + spreadRadius).coerceAtLeast(0f)
    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.drawRoundRect(left, top, right, bottom, radius, radius, paint)
    }
}

/** One card treatment for every surface; the style comes from Appearance. */
@Composable
fun SurfaceCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(14.dp),
    accent: Color? = null,
    content: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.surface.luminance() < 0.5f
    val config = LocalRodinAppearance.current
    val activeAccent = accent ?: config.activeAccent
    val radius = config.cardRadius.dp
    val shape = RoundedCornerShape(radius)
    val radiusPx = with(LocalDensity.current) { radius.toPx() }

    val lightGradient = if (dark) {
        null
    } else {
        Brush.linearGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF7FAFE)))
    }

    val brush: Brush? = when (config.cardStyle) {
        1 -> Brush.linearGradient(
            colorStops = arrayOf(
                0f to colors.surface.copy(alpha = if (dark) 0.68f else 0.80f),
                0.56f to activeAccent.copy(alpha = if (dark) 0.075f else 0.045f)
                    .compositeOver(colors.surface.copy(alpha = if (dark) 0.64f else 0.76f)),
                1f to colors.surface.copy(alpha = if (dark) 0.48f else 0.66f),
            ),
        )
        2, 3 -> lightGradient
        else -> if (dark) null else Brush.linearGradient(
            listOf(Color(0xFFFFFFFF), Color(0xFFF7FAFE)),
        )
    }

    val fillColor: Color? = when (config.cardStyle) {
        2 -> if (dark) Color(0xFF000000) else null
        3 -> if (dark) Color(0xFF101419) else Color(0xFFF0F4F8)
        else -> if (dark) Color(0xFF000000) else null
    }

    val borderColor = if (config.cardStyle == 2) {
        activeAccent.copy(alpha = if (dark) 0.45f else 0.35f)
    } else {
        RodinSurfaceLines.border(colors)
    }
    val borderWidth = when (config.cardStyle) {
        1 -> 0.85.dp
        2 -> 1.1.dp
        else -> 1.dp
    }

    val shadow: Modifier = when (config.cardStyle) {
        1 -> Modifier.rodinShadow(
            color = if (dark) Color.Black.copy(alpha = 0.24f) else Color(0x1A0C2943),
            blurRadius = 24f,
            offsetY = 11f,
            spreadRadius = -8f,
            cornerRadius = radiusPx,
        )
        2 -> Modifier.rodinShadow(
            color = activeAccent.copy(alpha = if (dark) 0.10f else 0.06f),
            blurRadius = 14f,
            spreadRadius = 0.5f,
            cornerRadius = radiusPx,
        )
        else -> if (dark) {
            Modifier
        } else {
            Modifier.rodinShadow(
                color = Color(0x140C2943),
                blurRadius = 18f,
                offsetY = 7f,
                cornerRadius = radiusPx,
            )
        }
    }

    Box(
        modifier
            .then(shadow)
            .fillMaxWidth()
            .then(if (brush != null) Modifier.background(brush, shape) else Modifier)
            .then(if (fillColor != null) Modifier.background(fillColor, shape) else Modifier)
            .border(borderWidth, borderColor, shape)
            .padding(padding),
    ) {
        content()
    }
}

@Composable
fun IconTile(
    accent: Color,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
    icon: ImageVector? = null,
    content: (@Composable () -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val sizePx = with(LocalDensity.current) { size.toPx() }
    val shape = RoundedCornerShape(size * 0.30f)
    Box(
        modifier
            .size(size)
            .rodinShadow(
                color = Color.Black.copy(alpha = 0.045f),
                blurRadius = 10f,
                spreadRadius = -6f,
                offsetY = 4f,
                cornerRadius = sizePx * 0.30f,
            )
            .background(
                Brush.linearGradient(
                    listOf(
                        accent.copy(alpha = 0.30f).compositeOver(colors.surface),
                        accent.copy(alpha = 0.12f).compositeOver(colors.surface),
                    ),
                ),
                shape,
            )
            .border(0.85.dp, accent.copy(alpha = 0.32f), shape),
        contentAlignment = Alignment.Center,
    ) {
        if (content != null) {
            content()
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(size * 0.49f),
            )
        }
    }
}

@Composable
fun SummaryChip(
    title: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onTap: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.surface.luminance() < 0.5f
    val shape = RoundedCornerShape(16.dp)

    val chip: @Composable () -> Unit = {
        Column(
            Modifier
                .defaultMinSize(minHeight = 58.dp)
                .background(
                    Brush.linearGradient(
                        listOf(
                            colors.surface.copy(alpha = if (dark) 0.74f else 0.94f),
                            accent.copy(alpha = if (dark) 0.055f else 0.035f),
                        ),
                    ),
                    shape,
                )
                .border(
                    0.7.dp,
                    colors.outline.copy(alpha = if (dark) 0.58f else 0.44f),
                    shape,
                )
                .padding(horizontal = 10.dp, vertical = 9.dp),
        ) {
            Text(
                text = title,
                maxLines = 1,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.W700,
                    color = accent,
                ),
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = subtitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 12.2.sp,
                    fontWeight = FontWeight.W700,
                    color = colors.onSurface,
                ),
            )
        }
    }

    if (onTap != null) {
        PressScale(onTap = onTap, modifier = modifier) { chip() }
    } else {
        Box(modifier) { chip() }
    }
}

@Composable
fun StatusPill(label: String, accent: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .background(accent.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
            .padding(horizontal = 9.dp, vertical = 5.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = accent,
                fontSize = 10.sp,
                fontWeight = FontWeight.W800,
                letterSpacing = 0.5.sp,
            ),
        )
    }
}

@Composable
fun SectionLabel(label: String, modifier: Modifier = Modifier) {
    Text(
        text = label.uppercase(),
        modifier = modifier.padding(start = 2.dp),
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 10.5.sp,
            fontWeight = FontWeight.W800,
            letterSpacing = 0.82.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
        ),
    )
}
