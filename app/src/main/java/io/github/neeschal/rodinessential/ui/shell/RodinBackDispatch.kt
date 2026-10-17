package io.github.neeschal.rodinessential.ui.shell

import kotlin.math.roundToInt

/**
 * The bridge between Android's predictive Back and the shell's motion plane.
 *
 * Packets keep the native host's exact layout: phase in bits 16+, a right-edge
 * flag in bit 15, and progress in the low 15 bits. The activity pushes events;
 * nothing polls hardware state.
 */
object RodinBackDispatch {
    var packetListener: ((Int) -> Unit)? = null
    var requestListener: (() -> Unit)? = null
    var bypassSettingsListener: (() -> Unit)? = null
    var interceptChangedListener: (() -> Unit)? = null

    @Volatile
    private var intercept = false

    val interceptEnabled: Boolean get() = intercept

    /** Called only when native navigation ownership changes. */
    fun setIntercept(enabled: Boolean) {
        if (intercept == enabled) return
        intercept = enabled
        interceptChangedListener?.invoke()
    }

    fun deliverBack(phase: Int, progress: Float, rightEdge: Boolean) {
        val clamped = progress.coerceIn(0f, 1f)
        val packet = (phase shl 16) or
            (if (rightEdge) 32768 else 0) or
            (clamped * 10000f).roundToInt().coerceIn(0, 32767)
        packetListener?.invoke(packet)
        if (phase == 4) requestListener?.invoke()
    }

    fun deliverBypassSettingsRequest() {
        bypassSettingsListener?.invoke()
    }
}
