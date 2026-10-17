package io.github.neeschal.rodinessential.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.safeDrawing

/**
 * Confirmation only: no device settings change until the user confirms.
 * The caller wraps this in [RodinDialog] and a surface builder.
 */
@Composable
fun RodinServiceConfirmation(
    reset: Boolean,
    cornerRadius: Double,
    accent: Color,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    onConfirmHaptic: (() -> Unit)? = null,
    onCancelHaptic: (() -> Unit)? = null,
    surface: @Composable (content: @Composable () -> Unit) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val onAccent = if (accent.luminance() > 0.179f) Color.Black else Color.White
    val buttonRadius = RoundedCornerShape(cornerRadius.coerceIn(10.0, 18.0).dp)

    val configuration = LocalConfiguration.current
    val imeVertical = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val safe = WindowInsets.safeDrawing.asPaddingValues()
    val safeVertical = safe.calculateTopPadding() + safe.calculateBottomPadding()
    val maxHeight = (configuration.screenHeightDp.dp - imeVertical - safeVertical - 48.dp)
        .coerceAtLeast(0.dp)

    Box(
        modifier
            .widthIn(max = 380.dp)
            .heightIn(max = maxHeight),
    ) {
        surface {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Box(
                    Modifier
                        .size(46.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    accent.copy(alpha = 0.19f),
                                    accent.copy(alpha = 0.07f),
                                ),
                            ),
                            buttonRadius,
                        )
                        .border(1.dp, accent.copy(alpha = 0.22f), buttonRadius),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (reset) {
                            Icons.Rounded.RestartAlt
                        } else {
                            Icons.Rounded.PowerSettingsNew
                        },
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(25.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = if (reset) "Reset Rodin settings?" else "Pause Rodin Essential?",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 20.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.W800,
                        letterSpacing = (-0.4).sp,
                        color = colors.onSurface,
                    ),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (reset) {
                        "Start fresh with no Rodin overrides."
                    } else {
                        "Let your ROM manage the device again."
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        lineHeight = 18.85.sp,
                        color = colors.onSurfaceVariant,
                    ),
                )
                Spacer(Modifier.height(16.dp))
                ConfirmationDetail(
                    icon = Icons.Rounded.Tune,
                    text = if (reset) {
                        "Release device overrides and turn off bypass charging."
                    } else {
                        "Release device overrides, turn off bypass charging, and pause background control."
                    },
                    accent = accent,
                    shape = buttonRadius,
                    contentColor = colors.onSurfaceVariant,
                )
                ConfirmationDetail(
                    icon = if (reset) {
                        Icons.Rounded.DeleteOutline
                    } else {
                        Icons.Rounded.BookmarkBorder
                    },
                    text = if (reset) {
                        "Clear saved device settings and reset app appearance."
                    } else {
                        "Keep your selections saved. Control stays disabled after reboot until you enable it again."
                    },
                    accent = accent,
                    shape = buttonRadius,
                    contentColor = colors.onSurfaceVariant,
                )
                Text(
                    text = "Restore captured original values. Vendor-only display and touch controls return to their default modes.",
                    modifier = Modifier.padding(start = 2.dp, top = 3.dp, end = 2.dp, bottom = 8.dp),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.5.sp,
                        lineHeight = 16.1.sp,
                        color = colors.onSurfaceVariant,
                    ),
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        onConfirmHaptic?.invoke()
                        onConfirm()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = buttonRadius,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent,
                        contentColor = onAccent,
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Text(
                        text = if (reset) "Reset all settings" else "Disable Rodin Essential",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.W700,
                        ),
                    )
                }
                Spacer(Modifier.height(6.dp))
                TextButton(
                    onClick = {
                        onCancelHaptic?.invoke()
                        onCancel()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = buttonRadius,
                    colors = ButtonDefaults.textButtonColors(contentColor = accent),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.W600,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfirmationDetail(
    icon: ImageVector,
    text: String,
    accent: Color,
    shape: Shape,
    contentColor: Color,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .background(accent.copy(alpha = 0.055f), shape)
            .border(1.dp, accent.copy(alpha = 0.10f), shape)
            .padding(12.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.sp,
                lineHeight = 18.85.sp,
                color = contentColor,
            ),
        )
    }
}
