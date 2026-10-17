package io.github.neeschal.rodinessential.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.neeschal.rodinessential.R

/** The launcher mark with its glow, one visual identity on every screen. */
@Composable
fun RodinAppEmblem(size: Dp, modifier: Modifier = Modifier) {
    val sizePx = with(LocalDensity.current) { size.toPx() }
    val shape = RoundedCornerShape(size * 0.24f)
    Box(
        modifier
            .size(size)
            .rodinShadow(
                color = Color(0xFF0066FF).copy(alpha = 0.28f),
                blurRadius = 16f,
                spreadRadius = -2f,
                offsetY = 4f,
                cornerRadius = sizePx * 0.24f,
            )
            .rodinShadow(
                color = Color(0x33000000),
                blurRadius = 8f,
                offsetY = 3f,
                cornerRadius = sizePx * 0.24f,
            )
            .clip(shape),
    ) {
        Image(
            painter = painterResource(R.mipmap.ic_launcher),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
