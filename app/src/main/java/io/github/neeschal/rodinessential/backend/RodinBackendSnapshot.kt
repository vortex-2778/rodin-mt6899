package io.github.neeschal.rodinessential.backend

/**
 * Immutable telemetry snapshot served from the daemon-backed cache.
 *
 * Field semantics (including the -1 unknown markers and the
 * [Long.MIN_VALUE] current sentinel) match the original Dart
 * `RodinBackendSnapshot` exactly.
 */
data class RodinBackendSnapshot(
    val connection: RodinConnectionState = RodinConnectionState.CONNECTING,
    val serviceEnabled: Boolean = true,
    val actionState: Int = 0,
    val chargingWriteState: Int = 0,
    val chargingMode: Int = -1,
    val batteryCapacity: Int = -1,
    val batteryTempTenthsC: Int = -1,
    val batteryVoltageUv: Long = -1L,
    val batteryCurrentUa: Long = Long.MIN_VALUE,
    val batteryStatus: Int = -1,
    val batteryHealth: Int = -1,
    val usbOnline: Int = -1,
    val usbType: Int = -1,
    val cpuOnlineMask: Int = 0,
    val cpuFreqKhz: List<Long> = DEFAULT_CPU_FREQS,
    val cpuGovernor0: Int = -1,
    val cpuGovernor4: Int = -1,
    val cpuGovernor7: Int = -1,
    val gpuGovernor: Int = -1,
    val ufsScheduler: Int = -1,
    val touchHal: Int = 0,
    val displayHal: Int = 0,
    val touchState: Int = -1,
    val displayColor: Int = -1,
    val displayTemp: Int = -1,
    val sunlight: Int = -1,
    val silky: Int = -1,
    val video: Int = -1,
    val dolby: Int = -1,
    val performanceProfile: Int = -1,
) {
    val ready: Boolean get() = connection == RodinConnectionState.ONLINE
    val controlsAvailable: Boolean get() = ready && serviceEnabled
    val serviceStatusLabel: String
        get() = connection.serviceLabel(enabled = serviceEnabled)
    val serviceStatusBadgeLabel: String get() = serviceStatusLabel.uppercase()
    val chargingBoost: Boolean get() = chargingMode == 90
    val busy: Boolean get() = actionState == 1
    val batteryTempC: Double? get() = if (batteryTempTenthsC >= 0) batteryTempTenthsC / 10.0 else null
    val batteryVoltageV: Double? get() = if (batteryVoltageUv >= 0) batteryVoltageUv / 1_000_000.0 else null
    val batteryCurrentA: Double?
        get() = if (batteryCurrentUa != Long.MIN_VALUE) batteryCurrentUa / 1_000_000.0 else null

    fun cpuOnline(cpu: Int): Boolean =
        cpu >= 0 && cpu < 32 && (cpuOnlineMask and (1 shl cpu)) != 0

    companion object {
        val DEFAULT_CPU_FREQS: List<Long> = List(8) { -1L }
    }
}
