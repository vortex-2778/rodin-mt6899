package io.github.neeschal.rodinessential.backend

import kotlin.math.abs

/**
 * Bypass-charging telemetry shared by the Quick Settings tile and the
 * charging screens. Ported from the native tile bridge
 * (`platform_bridge.rs` + `bypass_tile.rs`) and `bypass_telemetry.dart`.
 *
 * The kernel owns the settling interval; a page must not restart it. Check
 * the current snapshot as well, rather than caching a successful label.
 */
object RodinBypassTelemetry {
    const val BIT_DAEMON = 1
    const val BIT_SUPPORTED = 2
    const val BIT_ENABLED = 4
    const val BIT_ACTIVE = 8
    const val BIT_UPDATING = 16
    const val BIT_SERVICE = 32
    const val BIT_ATTENTION = 64
    const val PHASE_SHIFT = 7

    /** Only measured active power is confirmed, never a saved request. */
    fun directPowerConfirmed(phase: Int, active: Int, online: Int, currentUa: Long): Boolean =
        phase == 4 && active == 1 && online == 1 && currentUa in -100_000..100_000

    /** The tile's packed snapshot word; see the original `nativeSnapshot`. */
    fun tileSnapshot(): Int {
        val snapshot = RodinBackend.latest()
        val phase = RodinBackend.extendedValue(RodinExtendedKeys.BYPASS_PHASE)
        val confirmed = directPowerConfirmed(
            phase = phase,
            active = RodinBackend.extendedValue(RodinExtendedKeys.BYPASS_VERIFIED),
            online = snapshot.usbOnline,
            currentUa = snapshot.batteryCurrentUa,
        )
        var state = 0
        if (snapshot.ready) state = state or BIT_DAEMON
        if (RodinBackend.extendedValue(RodinExtendedKeys.BYPASS_SUPPORTED) == 1) {
            state = state or BIT_SUPPORTED
        }
        if (RodinBackend.extendedValue(RodinExtendedKeys.BYPASS_SAVED) == 1) {
            state = state or BIT_ENABLED
        }
        if (confirmed) state = state or BIT_ACTIVE
        if (snapshot.chargingWriteState == 1) state = state or BIT_UPDATING
        if (RodinBackend.extendedValue(RodinExtendedKeys.SERVICE_ENABLED) == 1) {
            state = state or BIT_SERVICE
        }
        if (RodinBackend.extendedValue(RodinExtendedKeys.BYPASS_ERROR) > 0 ||
            snapshot.chargingWriteState < 0 ||
            phase == 5
        ) {
            state = state or BIT_ATTENTION
        }
        return state or ((phase.coerceIn(0, 15)) shl PHASE_SHIFT)
    }

    /** Tile tap; refuses while a charging write is already settling. */
    fun tileToggle(): Boolean {
        val snapshot = RodinBackend.latest()
        if (!snapshot.ready) return false
        if (RodinBackend.extendedValue(RodinExtendedKeys.BYPASS_SUPPORTED) != 1) return false
        if (RodinBackend.extendedValue(RodinExtendedKeys.SERVICE_ENABLED) != 1) return false
        if (snapshot.chargingWriteState == 1) return false
        val saved = RodinBackend.extendedValue(RodinExtendedKeys.BYPASS_SAVED)
        if (saved != 0 && saved != 1) return false
        return RodinBackend.setBypassCharging(saved == 0)
    }

    /** Tile subtitle; the same ladder as the original `publish()`. */
    fun tileStatus(state: Int): String {
        val enabled = (state and BIT_ENABLED) != 0
        val phase = (state shr PHASE_SHIFT) and 15
        return when {
            (state and BIT_DAEMON) == 0 -> "Daemon unavailable"
            (state and BIT_SERVICE) == 0 -> "Controls disabled"
            (state and BIT_SUPPORTED) == 0 -> "Not supported"
            (state and BIT_UPDATING) != 0 -> "Updating\u2026"
            (state and BIT_ATTENTION) != 0 -> "Check charging status"
            enabled && (state and BIT_ACTIVE) != 0 -> "Active \u00b7 direct power"
            enabled && phase == 7 -> "Charging detected"
            enabled && phase == 6 -> "Battery assist"
            enabled && phase == 1 -> "Charging to threshold"
            enabled && phase == 2 -> "Enabled \u00b7 connect charger"
            enabled -> "Power settling"
            else -> "Off"
        }
    }

    /**
     * UI helper from the original `bypass_telemetry.dart`: readiness, the
     * saved state, the verified kernel write, sema and a settled current.
     */
    fun isBypassDirectPowerConfirmed(
        ready: Boolean,
        enabled: Boolean,
        kernelActive: Boolean,
        usbOnline: Int,
        batteryCurrentA: Double?,
    ): Boolean = ready &&
        enabled &&
        kernelActive &&
        usbOnline == 1 &&
        batteryCurrentA != null &&
        batteryCurrentA.isFinite() &&
        abs(batteryCurrentA) <= 0.10
}
