package io.github.neeschal.rodinessential.backend

/**
 * Connection state of the local `rodin_essentiald` transport.
 *
 * Mirrors the original Dart `RodinConnectionState`: the native worker reports
 * -1 until the first attempt completes, 0 for a verified unavailable daemon
 * and 1 once a snapshot exchange has succeeded.
 */
enum class RodinConnectionState {
    CONNECTING,
    ONLINE,
    OFFLINE;

    val label: String
        get() = when (this) {
            CONNECTING -> "Connecting"
            ONLINE -> "Live"
            OFFLINE -> "Offline"
        }

    val badgeLabel: String
        get() = label.uppercase()

    fun serviceLabel(enabled: Boolean): String =
        if (this == ONLINE && !enabled) "Disabled" else label

    companion object {
        fun fromNative(status: Int): RodinConnectionState = when (status) {
            1 -> ONLINE
            0 -> OFFLINE
            else -> CONNECTING
        }
    }
}
