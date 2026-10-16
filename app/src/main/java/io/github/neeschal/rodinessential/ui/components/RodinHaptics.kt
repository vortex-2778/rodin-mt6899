package io.github.neeschal.rodinessential.ui.components

import io.github.neeschal.rodinessential.platform.RodinPlatform
import io.github.neeschal.rodinessential.ui.shell.RodinInteractionSettings

/**
 * Kind codes 1..9 map 1:1 onto the native haptic table. Respects the user's
 * haptics preference before any platform call.
 */
object RodinHaptics {
    private fun send(kind: Int) {
        if (!RodinInteractionSettings.hapticsEnabled) return
        RodinPlatform.haptic(kind)
    }

    fun tap() = send(1)
    fun context() = send(2)
    fun toggle(enabled: Boolean) = send(if (enabled) 3 else 4)
    fun segment() = send(5)
    fun frequentSegment() = send(6)
    fun confirm() = send(7)
    fun reject() = send(8)
    fun back() = send(9)

    fun toggleCallback(callback: ((Boolean) -> Unit)?): ((Boolean) -> Unit)? {
        if (callback == null) return null
        return { enabled ->
            toggle(enabled)
            callback(enabled)
        }
    }
}
