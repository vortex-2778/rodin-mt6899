package io.github.neeschal.rodinessential.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

/**
 * The Flutter `IgnorePointer` widget has no Compose counterpart; consuming
 * every pointer change on the Initial pass is the exact equivalent — children
 * stay composed and keep their state, but no gesture reaches them.
 */
@Composable
fun RodinIgnorePointer(
    ignoring: Boolean,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = if (ignoring) {
            Modifier.pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                    }
                }
            }
        } else {
            Modifier
        },
    ) {
        content()
    }
}
