package io.github.neeschal.rodinessential.backend

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.TimeUnit

/**
 * Pure-Kotlin replacement for the Dart FFI client and the native host worker.
 *
 * The daemon remains the single owner of every hardware write; this class
 * only mirrors the original bridge semantics: one worker thread drains a
 * command queue, refreshes the cached snapshot every 500 ms while idle,
 * publishes to a conflated [StateFlow], and keeps the exact gates the native
 * bridge used (charging write state, service action state, palette state).
 */
object RodinBackend {
    private const val TAG = "RodinBackend"
    private const val IDLE_REFRESH_MS = 500L
    private const val POLL_TIMEOUT_MS = 500L

    private val CHARGING_PROFILES = setOf(0, 25, 33, 65, 85, 90)
    private val BYPASS_THRESHOLDS = setOf(0, 20, 40, 80, 90)
    private val CPU_POLICIES = setOf(0, 4, 7)

    /** Worker commands; 1:1 with the native bridge's `Command` enum. */
    private sealed class Command {
        object Refresh : Command()
        data class Charging(val watts: Int) : Command()
        data class BypassCharging(val enabled: Int) : Command()
        data class Touch(val profile: Int) : Command()
        data class DisplayColor(val mode: Int) : Command()
        data class DisplayTemp(val mode: Int) : Command()
        data class Sunlight(val value: Int) : Command()
        data class Silky(val value: Int) : Command()
        data class Video(val value: Int) : Command()
        data class Dolby(val value: Int) : Command()
        data class Performance(val profile: Int) : Command()
        data class CpuGovernor(val policy: Int, val code: Int) : Command()
        data class GpuGovernor(val code: Int) : Command()
        data class IoScheduler(val code: Int) : Command()
        data class Extended(val op: Int, val a: Int, val b: Int) : Command()
    }

    private val stateLock = Any()
    private var cache = RodinBackendSnapshot()
    private val extended = IntArray(RodinExtendedKeys.COUNT) { index ->
        when (index) {
            RodinExtendedKeys.PALETTE_STATE,
            RodinExtendedKeys.PALETTE_REVISION,
            RodinExtendedKeys.CONTRAST_STATE,
            RodinExtendedKeys.CONTRAST_REVISION,
            -> 0
            else -> -1
        }
    }
    private val cpuFrequencyTables = HashMap<Int, List<Int>>()

    private val _snapshots = MutableStateFlow(RodinBackendSnapshot())

    /** Conflated telemetry stream for every observer of the cache. */
    val snapshots: StateFlow<RodinBackendSnapshot> = _snapshots.asStateFlow()

    private val commands = LinkedBlockingDeque<Command>()

    @Volatile
    private var started = false

    @Volatile
    private var worker: Thread? = null

    @Volatile
    private var selectionHint: RodinSystemColorsSelection? = null

    @Volatile
    private var systemColorsRevision = 0

    val systemColorsLibrary = RodinSystemColorsLibrary()

    /** Latest selection hint; Android remains the owner of the palette. */
    val systemColorsSelection: RodinSystemColorsSelection?
        get() = selectionHint

    /**
     * Binds the application files directory used for the UI-only palette
     * hints and the optional palette library.
     */
    fun initialize(context: Context) {
        val directory = context.applicationContext.filesDir
        RodinSystemColorsPreferences.storageDir = directory
        if (systemColorsLibrary.recents.isEmpty() && systemColorsLibrary.saved.isEmpty()) {
            systemColorsLibrary.load()
        }
    }

    /** Starts the worker. Returns true once, like the native `start`. */
    fun start(): Boolean {
        if (started) return true
        synchronized(this) {
            if (started) return true
            // Publish the authenticated reverse listener while the interface
            // is starting, so the daemon can connect before the first frame.
            RodinDaemonTransport.primeReverseListener()
            commands.clear()
            started = true
            worker = Thread({ workerLoop() }, "rodin-backend").apply {
                isDaemon = true
                start()
            }
            return true
        }
    }

    val isStarted: Boolean get() = started

    fun stop() {
        if (!started) return
        synchronized(this) {
            if (!started) return
            started = false
            worker?.interrupt()
            worker = null
            commands.clear()
        }
    }

    fun latest(): RodinBackendSnapshot = synchronized(stateLock) { cache }

    // ------------------------------------------------------------------ worker

    private fun workerLoop() {
        loadSystemColorsSelectionHint()
        var lastIdleRefresh = System.currentTimeMillis() - IDLE_REFRESH_MS
        commands.offer(Command.Refresh)
        while (started) {
            val command = try {
                commands.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            } catch (_: InterruptedException) {
                return
            }
            if (command != null) {
                when (command) {
                    is Command.Refresh -> refreshCache()
                    is Command.Extended -> {
                        if (command.op in RodinExtendedKeys.OP_SYSTEM_COLORS..RodinExtendedKeys.OP_SET_CONTRAST) {
                            performSystemColorsCommand(command.op, command.a, command.b)
                        } else {
                            performAction(command)
                        }
                    }
                    else -> performAction(command)
                }
                lastIdleRefresh = System.currentTimeMillis()
                workerPublish()
                continue
            }
            if (System.currentTimeMillis() - lastIdleRefresh >= IDLE_REFRESH_MS) {
                refreshCache()
                lastIdleRefresh = System.currentTimeMillis()
                workerPublish()
            }
        }
    }

    private fun workerPublish() {
        rememberSystemColors()
        _snapshots.value = synchronized(stateLock) { cache }
    }

    /** Cache-only notification, safe from any thread. */
    private fun publish() {
        _snapshots.value = synchronized(stateLock) { cache }
    }

    private fun loadSystemColorsSelectionHint() {
        val selection = RodinSystemColorsPreferences.read()
        // An actual readback that finished meanwhile always wins over the hint.
        if (systemColorsRevision == 0 && selection != selectionHint) {
            selectionHint = selection
        }
    }

    private fun rememberSystemColors() {
        val palette = systemColorsState()
        if (!palette.ready || !palette.hasReadback || palette.revision == systemColorsRevision) return
        val firstRead = systemColorsRevision == 0
        systemColorsRevision = palette.revision
        val selection = RodinSystemColorsSelection.fromNative(palette)
        if (selection == selectionHint && !firstRead) return
        selectionHint = selection
        // Serialize these small writes on the worker thread so an older
        // completion cannot replace a newer confirmed selection. This is not
        // a background system reapply.
        RodinSystemColorsPreferences.write(selection)
    }

    // ----------------------------------------------------------- action states

    private fun performAction(command: Command) {
        val chargingAction = command is Command.Charging ||
            command is Command.BypassCharging ||
            (command is Command.Extended && command.op == RodinExtendedKeys.OP_BYPASS_THRESHOLD)
        val serviceAction = command is Command.Extended &&
            (command.op == RodinExtendedKeys.OP_SERVICE_ENABLED ||
                command.op == RodinExtendedKeys.OP_SERVICE_RESET)

        synchronized(stateLock) {
            cache = cache.copy(actionState = 1)
            if (chargingAction) cache = cache.copy(chargingWriteState = 1)
        }
        val result = runCatching { execute(command) }
        synchronized(stateLock) {
            if (result.isSuccess) {
                cache = cache.copy(actionState = 2)
                if (chargingAction) cache = cache.copy(chargingWriteState = 2)
                if (serviceAction) extended[RodinExtendedKeys.SERVICE_ACTION] = 2
            } else {
                cache = cache.copy(actionState = -1)
                if (chargingAction) cache = cache.copy(chargingWriteState = -1)
                if (serviceAction) extended[RodinExtendedKeys.SERVICE_ACTION] = -1
            }
        }
        refreshCache()
        result.exceptionOrNull()?.let { error ->
            Log.w(TAG, "RODIN_ACTION_FAIL action=${commandName(command)} error=${error.message}")
        }
    }

    private fun commandName(command: Command): String = when (command) {
        is Command.Refresh -> "refresh"
        is Command.Charging -> "charging"
        is Command.BypassCharging -> "charging.bypass"
        is Command.Touch -> "touch"
        is Command.DisplayColor -> "display.color"
        is Command.DisplayTemp -> "display.temp"
        is Command.Sunlight -> "display.sunlight"
        is Command.Silky -> "display.silky"
        is Command.Video -> "display.video"
        is Command.Dolby -> "display.dolby"
        is Command.Performance -> "perf"
        is Command.CpuGovernor -> "cpu.gov"
        is Command.GpuGovernor -> "gpu.gov"
        is Command.IoScheduler -> "io.scheduler"
        is Command.Extended -> "extended.${command.op}"
    }

    private fun execute(command: Command) {
        when (command) {
            is Command.Refresh -> return
            is Command.Charging -> {
                require(command.watts in CHARGING_PROFILES) { "invalid charging profile" }
                requestTuned("SET charging ${command.watts}")
            }
            is Command.BypassCharging -> {
                require(command.enabled == 0 || command.enabled == 1) { "invalid bypass value" }
                requestTuned("SET charging.bypass ${command.enabled}")
            }
            is Command.Touch -> {
                require(command.profile in 0..7) { "invalid touch profile" }
                requestTuned("SET touch ${command.profile}")
            }
            is Command.DisplayColor -> requestTuned("SET display.color ${command.mode}")
            is Command.DisplayTemp -> requestTuned("SET display.temp ${command.mode}")
            is Command.Sunlight -> requestTuned("SET display.sunlight ${command.value}")
            is Command.Silky -> requestTuned("SET display.silky ${command.value}")
            is Command.Video -> requestTuned("SET display.video ${command.value}")
            is Command.Dolby -> requestTuned("SET display.dolby ${command.value}")
            is Command.Performance -> {
                require(command.profile in 0..3) { "invalid performance profile" }
                requestTuned("SET perf ${command.profile}")
            }
            is Command.CpuGovernor -> {
                require(command.policy in CPU_POLICIES) { "invalid cpu policy" }
                val name = requireNotNull(RodinProtocol.cpuGovernorName(command.code)) {
                    "invalid cpu governor code"
                }
                requestTuned("SET cpu.gov ${command.policy} $name")
            }
            is Command.GpuGovernor -> {
                val name = requireNotNull(RodinProtocol.gpuGovernorName(command.code)) {
                    "invalid gpu governor code"
                }
                requestTuned("SET gpu.gov $name")
            }
            is Command.IoScheduler -> {
                val name = requireNotNull(RodinProtocol.ioName(command.code)) {
                    "invalid io scheduler code"
                }
                requestTuned("SET io.scheduler $name")
            }
            is Command.Extended -> executeExtended(command)
        }
    }

    private fun executeExtended(command: Command.Extended) {
        val op = command.op
        val a = command.a
        val b = command.b
        val request = when (op) {
            RodinExtendedKeys.OP_EXPERT_GAMUT -> {
                require(a in 1..3) { "invalid expert gamut" }
                "SET display.expert.gamut $a"
            }
            RodinExtendedKeys.OP_EXPERT_CHANNEL -> "SET display.expert.channel $a $b"
            RodinExtendedKeys.OP_EXPERT_RESET -> "SET display.expert.reset"
            RodinExtendedKeys.OP_CPU_MANUAL -> {
                require(a == 0 || a == 1) { "invalid cpu manual value" }
                "SET cpu.manual $a"
            }
            RodinExtendedKeys.OP_CPU_CORE -> {
                require(a in 1..7 && (b == 0 || b == 1)) { "invalid cpu core request" }
                "SET cpu.core $a $b"
            }
            RodinExtendedKeys.OP_DISPLAY_RESOLUTION -> {
                val width = a
                val height = (b shr 16) and 0xFFFF
                val density = b and 0xFFFF
                if (width <= 0 || height <= 0 || (width == 1220 && height == 2712)) {
                    "SET display.res 0 0 0"
                } else {
                    "SET display.res $width $height $density"
                }
            }
            RodinExtendedKeys.OP_ZRAM_SIZE -> {
                synchronized(stateLock) { extended[RodinExtendedKeys.ZRAM_SIZE] = a }
                "SET zram.size $a"
            }
            RodinExtendedKeys.OP_ZRAM_ALGORITHM -> {
                synchronized(stateLock) { extended[RodinExtendedKeys.ZRAM_ALGORITHM] = a }
                val algorithm = when (a) {
                    1 -> "zstd"
                    2 -> "lzo-rle"
                    3 -> "lzo"
                    else -> "lz4"
                }
                "SET zram.algorithm $algorithm"
            }
            RodinExtendedKeys.OP_ZRAM_SWAPPINESS -> {
                synchronized(stateLock) { extended[RodinExtendedKeys.ZRAM_SWAPPINESS] = a }
                "SET zram.swappiness $a"
            }
            RodinExtendedKeys.OP_ZRAM_COMPACT -> "ACTION zram.compact"
            RodinExtendedKeys.OP_GPU_UNCAP -> {
                synchronized(stateLock) {
                    extended[RodinExtendedKeys.GPU_UNCAP_ACTIVE] = a
                    if (a == 1) {
                        extended[RodinExtendedKeys.GPU_MAX_FREQ] = 1300
                        extended[RodinExtendedKeys.GPU_THERMAL_STATE] = 0
                    }
                }
                "SET gpu.uncap $a"
            }
            RodinExtendedKeys.OP_GPU_MIN_FREQ -> {
                synchronized(stateLock) { extended[RodinExtendedKeys.GPU_MIN_FREQ] = a }
                "SET gpu.min_freq $a"
            }
            RodinExtendedKeys.OP_GPU_MAX_FREQ -> {
                synchronized(stateLock) { extended[RodinExtendedKeys.GPU_MAX_FREQ] = a }
                "SET gpu.max_freq $a"
            }
            RodinExtendedKeys.OP_GPU_DEVFREQ_GOVERNOR -> {
                synchronized(stateLock) { extended[RodinExtendedKeys.GPU_DEVFREQ_GOVERNOR] = a }
                val governor = when (a) {
                    1 -> "performance"
                    2 -> "powersave"
                    3 -> "userspace"
                    4 -> "dummy"
                    else -> "simple_ondemand"
                }
                "SET gpu.governor $governor"
            }
            RodinExtendedKeys.OP_GPU_GED_BOOST -> {
                synchronized(stateLock) { extended[RodinExtendedKeys.GPU_GED_BOOST] = a }
                "SET gpu.ged_boost $a"
            }
            RodinExtendedKeys.OP_CPU_CLUSTER_MIN_FREQ -> {
                synchronized(stateLock) {
                    when (a) {
                        0 -> extended[RodinExtendedKeys.CPU_MIN_0] = b
                        4 -> extended[RodinExtendedKeys.CPU_MIN_4] = b
                        7 -> extended[RodinExtendedKeys.CPU_MIN_7] = b
                    }
                }
                "SET cpu.min_freq $a $b"
            }
            RodinExtendedKeys.OP_CPU_CLUSTER_MAX_FREQ -> {
                synchronized(stateLock) {
                    when (a) {
                        0 -> extended[RodinExtendedKeys.CPU_MAX_0] = b
                        4 -> extended[RodinExtendedKeys.CPU_MAX_4] = b
                        7 -> extended[RodinExtendedKeys.CPU_MAX_7] = b
                    }
                }
                "SET cpu.max_freq $a $b"
            }
            RodinExtendedKeys.OP_CPU_CLUSTER_FREQ_RANGE -> {
                val min = (b shr 16) and 0xFFFF
                val max = b and 0xFFFF
                synchronized(stateLock) {
                    when (a) {
                        0 -> {
                            extended[RodinExtendedKeys.CPU_MIN_0] = min
                            extended[RodinExtendedKeys.CPU_MAX_0] = max
                        }
                        4 -> {
                            extended[RodinExtendedKeys.CPU_MIN_4] = min
                            extended[RodinExtendedKeys.CPU_MAX_4] = max
                        }
                        7 -> {
                            extended[RodinExtendedKeys.CPU_MIN_7] = min
                            extended[RodinExtendedKeys.CPU_MAX_7] = max
                        }
                    }
                }
                "SET cpu.freq_range $a $min $max"
            }
            RodinExtendedKeys.OP_GPU_POWER_POLICY -> {
                synchronized(stateLock) { extended[RodinExtendedKeys.GPU_POWER_POLICY] = a }
                val policy = if (a == 1) "always_on" else "coarse_demand"
                "SET gpu.power_policy $policy"
            }
            RodinExtendedKeys.OP_CPU_FREQ_RESET -> {
                require(a == 0 || a == 4 || a == 7) { "invalid cpu policy" }
                synchronized(stateLock) {
                    when (a) {
                        0 -> {
                            extended[RodinExtendedKeys.CPU_MIN_0] = -1
                            extended[RodinExtendedKeys.CPU_MAX_0] = -1
                        }
                        4 -> {
                            extended[RodinExtendedKeys.CPU_MIN_4] = -1
                            extended[RodinExtendedKeys.CPU_MAX_4] = -1
                        }
                        7 -> {
                            extended[RodinExtendedKeys.CPU_MIN_7] = -1
                            extended[RodinExtendedKeys.CPU_MAX_7] = -1
                        }
                    }
                }
                "SET cpu.freq_reset $a"
            }
            RodinExtendedKeys.OP_SERVICE_ENABLED -> {
                require(a == 0 || a == 1) { "invalid service request" }
                "SET service.enabled $a"
            }
            RodinExtendedKeys.OP_SERVICE_RESET -> "ACTION service.reset"
            RodinExtendedKeys.OP_BYPASS_THRESHOLD -> {
                require(a in BYPASS_THRESHOLDS) { "invalid bypass threshold" }
                "SET charging.bypass_threshold $a"
            }
            else -> throw IllegalArgumentException("invalid extended command")
        }
        requestTuned(request)
    }

    /** Keeps only the framework palette and service calls on a 75 s deadline. */
    private fun requestTuned(command: String): String {
        val timeout = when {
            command == "GET system.colors" -> RodinDaemonTransport.PALETTE_TIMEOUT_MS
            command.startsWith("SET system.colors") -> RodinDaemonTransport.PALETTE_TIMEOUT_MS
            command.startsWith("SET service.") -> RodinDaemonTransport.PALETTE_TIMEOUT_MS
            command == "ACTION service.reset" -> RodinDaemonTransport.PALETTE_TIMEOUT_MS
            else -> RodinDaemonTransport.RESPONSE_TIMEOUT_MS
        }
        return RodinDaemonTransport.request(command, timeout)
    }

    // ------------------------------------------------------------- palette ops

    private fun performSystemColorsCommand(op: Int, a: Int, b: Int) {
        val contrast = op == RodinExtendedKeys.OP_GET_CONTRAST ||
            op == RodinExtendedKeys.OP_SET_CONTRAST
        val stateIndex = if (contrast) {
            RodinExtendedKeys.CONTRAST_STATE
        } else {
            RodinExtendedKeys.PALETTE_STATE
        }
        val errorIndex = if (contrast) {
            RodinExtendedKeys.CONTRAST_ERROR
        } else {
            RodinExtendedKeys.PALETTE_ERROR
        }
        val revisionIndex = if (contrast) {
            RodinExtendedKeys.CONTRAST_REVISION
        } else {
            RodinExtendedKeys.PALETTE_REVISION
        }

        synchronized(stateLock) {
            extended[stateIndex] = 1
            extended[errorIndex] = 0
        }
        val result = runCatching {
            val command = paletteCommand(op, a, b)
            val body = requestTuned(command)
            if (contrast) {
                RodinProtocol.parseContrastResponse(body)
            } else {
                RodinProtocol.parsePaletteResponse(body)
            }
        }
        synchronized(stateLock) {
            result.onSuccess { values ->
                for ((index, value) in values) extended[index] = value
            }
            result.onFailure { error ->
                extended[errorIndex] = RodinProtocol.paletteErrorCode(error.message.orEmpty())
            }
            // Publish the revision before the terminal state. A client
            // observing completion must also see the new values and revision,
            // not the old stamp.
            extended[revisionIndex] = extended[revisionIndex] + 1
            extended[stateIndex] = if (result.isSuccess) 2 else -1
        }
        if (result.isSuccess) {
            Log.i(TAG, "RODIN_SYSTEM_COLORS_PASS operation=$op")
        } else {
            Log.w(TAG, "RODIN_SYSTEM_COLORS_FAIL operation=$op error=${result.exceptionOrNull()?.message}")
        }
    }

    private fun paletteCommand(op: Int, a: Int, b: Int): String = when {
        op == RodinExtendedKeys.OP_SYSTEM_COLORS &&
            a in 0..0xFFFFFF && b in 0..6 -> "SET system.colors $a $b"
        op == RodinExtendedKeys.OP_SYSTEM_COLORS_WALLPAPER && a == 0 && b == 0 ->
            "SET system.colors.wallpaper"
        op == RodinExtendedKeys.OP_GET_SYSTEM_COLORS && a == 0 && b == 0 -> "GET system.colors"
        op == RodinExtendedKeys.OP_GET_CONTRAST && a == 0 && b == 0 -> "GET system.colors.contrast"
        op == RodinExtendedKeys.OP_SET_CONTRAST && a in -1000..1000 && b == 0 ->
            "SET system.colors.contrast $a"
        else -> throw IllegalArgumentException("palette_invalid_choice")
    }

    private fun injectPaletteOperation(op: Int, a: Int, b: Int): Boolean {
        val contrast = op == RodinExtendedKeys.OP_GET_CONTRAST ||
            op == RodinExtendedKeys.OP_SET_CONTRAST
        val stateIndex = if (contrast) {
            RodinExtendedKeys.CONTRAST_STATE
        } else {
            RodinExtendedKeys.PALETTE_STATE
        }
        val previous = synchronized(stateLock) {
            if (extended[stateIndex] == 1) return false
            val old = extended[stateIndex]
            extended[stateIndex] = 1
            old
        }
        if (commands.offer(Command.Extended(op, a, b))) return true
        synchronized(stateLock) { extended[stateIndex] = previous }
        return false
    }

    fun refreshSystemColors(): Boolean = queueSystemColors(RodinExtendedKeys.OP_GET_SYSTEM_COLORS)

    fun useWallpaperSystemColors(): Boolean =
        queueSystemColors(RodinExtendedKeys.OP_SYSTEM_COLORS_WALLPAPER)

    fun setSystemColors(seed: Int, style: Int): Boolean {
        if (seed < 0 || seed > 0xFFFFFF || style < 0 || style > 6) return false
        return queueSystemColors(RodinExtendedKeys.OP_SYSTEM_COLORS, seed, style)
    }

    fun refreshSystemColorContrast(): Boolean =
        queueSystemColorContrast(RodinExtendedKeys.OP_GET_CONTRAST)

    fun setSystemColorContrast(level: Int): Boolean {
        if (level < -1000 || level > 1000) return false
        return queueSystemColorContrast(RodinExtendedKeys.OP_SET_CONTRAST, level)
    }

    // Palette transactions have their own result and revision in the cache.
    // Do not trigger hardware refresh or reapply operations from this screen.
    private fun queueSystemColors(operation: Int, seed: Int = 0, style: Int = 0): Boolean {
        if (!started) return false
        if (!injectPaletteOperation(operation, seed, style)) return false
        // Make the busy state visible immediately; the worker publishes the
        // completion with the new values and revision.
        publish()
        return true
    }

    private fun queueSystemColorContrast(operation: Int, level: Int = 0): Boolean {
        if (!started) return false
        if (!injectPaletteOperation(operation, level, 0)) return false
        publish()
        return true
    }

    val systemColors: RodinSystemColorsState
        get() = if (started) systemColorsState() else RodinSystemColorsState()

    private fun systemColorsState(): RodinSystemColorsState =
        RodinSystemColorsState.fromNative(::extendedValue)

    // -------------------------------------------------------------- refresh

    private fun refreshCache() {
        try {
            val body = requestTuned("GET snapshot")
            val fields = RodinProtocol.parseFields(body)
            if (fields["protocol"] != RodinProtocol.PROTOCOL_VERSION) {
                throw IllegalStateException("protocol mismatch")
            }
            synchronized(stateLock) {
                for ((key, index) in RodinProtocol.EXTENDED_FIELDS) {
                    extended[index] = RodinProtocol.parseI32(fields[key])
                }
                extended[RodinExtendedKeys.ZRAM_ALGORITHM] =
                    RodinProtocol.zramAlgorithmCode(fields["zram_alg"])
                extended[RodinExtendedKeys.GPU_DEVFREQ_GOVERNOR] =
                    RodinProtocol.gpuDevfreqSlotCode(fields["gpu_gov"])
                for ((slot, key) in listOf(0 to "cpu_avail0", 4 to "cpu_avail4", 7 to "cpu_avail7")) {
                    val raw = fields[key] ?: continue
                    cpuFrequencyTables[slot] = parseFrequencyList(raw)
                }
                cache = RodinBackendSnapshot(
                    connection = RodinConnectionState.ONLINE,
                    serviceEnabled = extended[RodinExtendedKeys.SERVICE_ENABLED] != 0,
                    actionState = cache.actionState,
                    chargingWriteState = cache.chargingWriteState,
                    chargingMode = RodinProtocol.parseI32(fields["charging"]),
                    batteryCapacity = RodinProtocol.parseI32(fields["cap"]),
                    batteryTempTenthsC = RodinProtocol.parseI32(fields["temp"]),
                    batteryVoltageUv = RodinProtocol.parseI64(fields["voltage"]),
                    batteryCurrentUa = fields["current"]?.toLongOrNull() ?: Long.MIN_VALUE,
                    batteryStatus = fields["status"]?.let(RodinProtocol::codeStatus) ?: 0,
                    batteryHealth = fields["health"]?.let(RodinProtocol::codeHealth) ?: 0,
                    usbOnline = RodinProtocol.parseI32(fields["usb_online"]),
                    usbType = fields["usb_type"]?.let(RodinProtocol::codeUsbType) ?: -1,
                    cpuOnlineMask = fields["cpu_online"]?.let(RodinProtocol::cpuMask) ?: 0,
                    cpuFreqKhz = List(8) { cpu -> RodinProtocol.parseI64(fields["cpu$cpu"]) },
                    cpuGovernor0 = fields["gov0"]?.let(RodinProtocol::cpuGovernorCode) ?: -1,
                    cpuGovernor4 = fields["gov4"]?.let(RodinProtocol::cpuGovernorCode) ?: -1,
                    cpuGovernor7 = fields["gov7"]?.let(RodinProtocol::cpuGovernorCode) ?: -1,
                    gpuGovernor = fields["gpu_gov"]?.let(RodinProtocol::gpuGovernorCode) ?: -1,
                    ufsScheduler = fields["io"]?.let(RodinProtocol::ioCode) ?: -1,
                    touchHal = RodinProtocol.parseI32(fields["touch_hal"]),
                    displayHal = RodinProtocol.parseI32(fields["display_hal"]),
                    touchState = RodinProtocol.parseI32(fields["touch"]),
                    displayColor = RodinProtocol.parseI32(fields["display_color"]),
                    displayTemp = RodinProtocol.parseI32(fields["display_temp"]),
                    sunlight = RodinProtocol.parseI32(fields["sunlight"]),
                    silky = RodinProtocol.parseI32(fields["silky"]),
                    video = RodinProtocol.parseI32(fields["video"]),
                    dolby = RodinProtocol.parseI32(fields["dolby"]),
                    performanceProfile = RodinProtocol.parseI32(fields["perf"]),
                )
            }
        } catch (error: Exception) {
            synchronized(stateLock) {
                cache = cache.copy(connection = RodinConnectionState.OFFLINE)
            }
            Log.w(TAG, "RODIN_REFRESH_FAIL error=${error.message}")
        }
    }

    private fun parseFrequencyList(raw: String): List<Int> = raw
        .split(',')
        .mapNotNull { it.trim().toIntOrNull() }
        .filter { it > 0 }
        .sorted()
        .distinct()

    // ------------------------------------------------------------- public API

    fun extendedValue(key: Int): Int {
        if (!started) return -1
        if (key < 0 || key >= RodinExtendedKeys.COUNT) return -1
        synchronized(stateLock) { return extended[key] }
    }

    fun refresh() {
        if (started) commands.offer(Command.Refresh)
    }

    fun setChargingProfile(watts: Int): Boolean {
        if (watts !in CHARGING_PROFILES) return false
        return sendCharging(Command.Charging(watts))
    }

    fun setChargingBoost(enabled: Boolean): Boolean = setChargingProfile(if (enabled) 90 else 0)

    val chargingControlSupported: Boolean get() = extendedValue(RodinExtendedKeys.CHARGING_SUPPORTED) == 1
    val chargingFccUa: Int get() = extendedValue(RodinExtendedKeys.CHARGING_FCC_UA)
    val chargingAdapterWatts: Int get() = extendedValue(RodinExtendedKeys.CHARGING_ADAPTER_W)
    val chargingPdAuthenticated: Boolean get() = extendedValue(RodinExtendedKeys.CHARGING_PD_AUTH) == 1
    val chargingQuickType: Int get() = extendedValue(RodinExtendedKeys.CHARGING_QUICK_TYPE)
    val chargingSicMode: Int get() = extendedValue(RodinExtendedKeys.CHARGING_SIC)
    val chargingLiveMilliwatts: Int get() = extendedValue(RodinExtendedKeys.CHARGING_LIVE_MW)
    val bypassChargingSupported: Boolean get() = extendedValue(RodinExtendedKeys.BYPASS_SUPPORTED) == 1
    val bypassChargingState: Int get() = extendedValue(RodinExtendedKeys.BYPASS_STATE)
    val bypassChargingEnabled: Boolean get() = bypassChargingSaved == 1
    val bypassChargingSaved: Int get() = extendedValue(RodinExtendedKeys.BYPASS_SAVED)
    val bypassHardwareActive: Boolean get() = extendedValue(RodinExtendedKeys.BYPASS_VERIFIED) == 1
    val bypassThreshold: Int get() = extendedValue(RodinExtendedKeys.BYPASS_THRESHOLD)
    val bypassPhase: Int get() = extendedValue(RodinExtendedKeys.BYPASS_PHASE)

    fun setBypassThreshold(percent: Int): Boolean =
        percent in BYPASS_THRESHOLDS && setExtendedOperation(RodinExtendedKeys.OP_BYPASS_THRESHOLD, percent)

    fun setBypassCharging(enabled: Boolean): Boolean =
        sendCharging(Command.BypassCharging(if (enabled) 1 else 0))

    fun setTouchProfile(profile: Int): Boolean {
        if (profile < 0 || profile > 7) return false
        return send(Command.Touch(profile))
    }

    // Kept for older call sites outside the screen bundle. Enabled maps to
    // Rodin's sustained 480 Hz profile; disabled restores OEM Dynamic.
    fun setTouchBoost(enabled: Boolean): Boolean = setTouchProfile(if (enabled) 2 else 0)

    fun setDisplayColor(mode: Int): Boolean = send(Command.DisplayColor(mode))
    fun setDisplayTemperature(mode: Int): Boolean = send(Command.DisplayTemp(mode))
    fun setSunlight(enabled: Boolean): Boolean = send(Command.Sunlight(if (enabled) 1 else 0))
    fun setSilky(enabled: Boolean): Boolean = send(Command.Silky(if (enabled) 1 else 0))
    fun setVideoEnhancement(enabled: Boolean): Boolean = send(Command.Video(if (enabled) 1 else 0))
    fun setDolbyVision(enabled: Boolean): Boolean = send(Command.Dolby(if (enabled) 1 else 0))

    fun setPerformanceProfile(profile: Int): Boolean {
        synchronized(stateLock) {
            if (cache.performanceProfile != profile) {
                cache = cache.copy(
                    gpuGovernor = when (profile) {
                        0 -> 0 // OEM dummy
                        1 -> 3 // simple_ondemand
                        2 -> 1 // powersave
                        3 -> 2 // performance
                        else -> cache.gpuGovernor
                    },
                    performanceProfile = profile,
                )
            }
        }
        publish()
        return send(Command.Performance(profile))
    }

    fun setCpuGovernor(policy: Int, code: Int): Boolean = send(Command.CpuGovernor(policy, code))
    fun setGpuGovernor(code: Int): Boolean = send(Command.GpuGovernor(code))
    fun setUfsScheduler(code: Int): Boolean = send(Command.IoScheduler(code))

    fun setExtendedOperation(op: Int, a: Int, b: Int = 0): Boolean {
        if (!started) return false
        val accepted = when (op) {
            RodinExtendedKeys.OP_BYPASS_THRESHOLD -> {
                if (a !in BYPASS_THRESHOLDS) return false
                sendCharging(Command.Extended(op, a, b))
            }
            RodinExtendedKeys.OP_SERVICE_ENABLED,
            RodinExtendedKeys.OP_SERVICE_RESET,
            -> sendServiceAction(Command.Extended(op, a, b))
            RodinExtendedKeys.OP_SYSTEM_COLORS,
            RodinExtendedKeys.OP_SYSTEM_COLORS_WALLPAPER,
            RodinExtendedKeys.OP_GET_SYSTEM_COLORS,
            RodinExtendedKeys.OP_GET_CONTRAST,
            RodinExtendedKeys.OP_SET_CONTRAST,
            -> injectPaletteOperation(op, a, b)
            else -> send(Command.Extended(op, a, b))
        }
        refresh()
        return accepted
    }

    val serviceEnabled: Boolean get() = extendedValue(RodinExtendedKeys.SERVICE_ENABLED) == 1
    val serviceActionState: Int get() = extendedValue(RodinExtendedKeys.SERVICE_ACTION)
    fun setServiceEnabled(enabled: Boolean): Boolean =
        setExtendedOperation(RodinExtendedKeys.OP_SERVICE_ENABLED, if (enabled) 1 else 0)
    fun resetAllSettings(): Boolean = setExtendedOperation(RodinExtendedKeys.OP_SERVICE_RESET, 0)

    fun setCpuManualMode(enabled: Boolean): Boolean =
        setExtendedOperation(RodinExtendedKeys.OP_CPU_MANUAL, if (enabled) 1 else 0)

    fun setCpuCoreOnline(cpu: Int, online: Boolean): Boolean {
        if (cpu < 1 || cpu > 7) return false
        return setExtendedOperation(RodinExtendedKeys.OP_CPU_CORE, cpu, if (online) 1 else 0)
    }

    fun setDisplayResolution(width: Int, height: Int, density: Int = 0): Boolean {
        if (width <= 0 || height <= 0 || (width == 1220 && height == 2712)) {
            return resetDisplayResolution()
        }
        val detectedDensity = extendedValue(RodinExtendedKeys.DISPLAY_NATIVE_DENSITY)
        val nativeDensity = if (detectedDensity > 0) detectedDensity else 520
        val safeDensity = if (density > 0) density else (nativeDensity * width + 610) / 1220
        return setExtendedOperation(
            RodinExtendedKeys.OP_DISPLAY_RESOLUTION,
            width,
            (height shl 16) or (safeDensity and 0xFFFF),
        )
    }

    fun resetDisplayResolution(): Boolean =
        setExtendedOperation(RodinExtendedKeys.OP_DISPLAY_RESOLUTION, 0, 0)

    fun setZramSize(sizeMb: Int): Boolean = setExtendedOperation(RodinExtendedKeys.OP_ZRAM_SIZE, sizeMb)
    fun setZramAlgorithm(algCode: Int): Boolean =
        setExtendedOperation(RodinExtendedKeys.OP_ZRAM_ALGORITHM, algCode)
    fun setZramSwappiness(swappiness: Int): Boolean =
        setExtendedOperation(RodinExtendedKeys.OP_ZRAM_SWAPPINESS, swappiness)
    fun compactZram(): Boolean = setExtendedOperation(RodinExtendedKeys.OP_ZRAM_COMPACT, 0)

    fun setGpuUncap(uncap: Boolean): Boolean =
        setExtendedOperation(RodinExtendedKeys.OP_GPU_UNCAP, if (uncap) 1 else 0)
    fun setGpuMinFreq(mhz: Int): Boolean = setExtendedOperation(RodinExtendedKeys.OP_GPU_MIN_FREQ, mhz)
    fun setGpuMaxFreq(mhz: Int): Boolean = setExtendedOperation(RodinExtendedKeys.OP_GPU_MAX_FREQ, mhz)
    fun setGpuDevfreqGovernor(code: Int): Boolean =
        setExtendedOperation(RodinExtendedKeys.OP_GPU_DEVFREQ_GOVERNOR, code)
    fun setGpuGedBoost(boost: Boolean): Boolean =
        setExtendedOperation(RodinExtendedKeys.OP_GPU_GED_BOOST, if (boost) 1 else 0)
    fun setGpuPowerPolicy(policyCode: Int): Boolean =
        setExtendedOperation(RodinExtendedKeys.OP_GPU_POWER_POLICY, policyCode)

    fun setCpuClusterMinFreq(policy: Int, mhz: Int): Boolean =
        setExtendedOperation(RodinExtendedKeys.OP_CPU_CLUSTER_MIN_FREQ, policy, mhz)
    fun setCpuClusterMaxFreq(policy: Int, mhz: Int): Boolean =
        setExtendedOperation(RodinExtendedKeys.OP_CPU_CLUSTER_MAX_FREQ, policy, mhz)
    fun setCpuClusterFreqRange(policy: Int, minMhz: Int, maxMhz: Int): Boolean =
        setExtendedOperation(
            RodinExtendedKeys.OP_CPU_CLUSTER_FREQ_RANGE,
            policy,
            (minMhz shl 16) or (maxMhz and 0xFFFF),
        )
    fun resetCpuClusterFreqRange(policy: Int): Boolean =
        setExtendedOperation(RodinExtendedKeys.OP_CPU_FREQ_RESET, policy)

    fun cpuAvailableFrequencies(policy: Int): List<Int> {
        if (!started || policy !in CPU_POLICIES) return emptyList()
        synchronized(stateLock) {
            val table = cpuFrequencyTables[policy]
            if (!table.isNullOrEmpty()) return table
        }
        return emptyList()
    }

    fun cpuTargetMinFrequency(policy: Int): Int = when (policy) {
        0 -> extendedValue(RodinExtendedKeys.CPU_MIN_0)
        4 -> extendedValue(RodinExtendedKeys.CPU_MIN_4)
        7 -> extendedValue(RodinExtendedKeys.CPU_MIN_7)
        else -> -1
    }

    fun cpuTargetMaxFrequency(policy: Int): Int = when (policy) {
        0 -> extendedValue(RodinExtendedKeys.CPU_MAX_0)
        4 -> extendedValue(RodinExtendedKeys.CPU_MAX_4)
        7 -> extendedValue(RodinExtendedKeys.CPU_MAX_7)
        else -> -1
    }

    fun cpuLiveMinFrequency(policy: Int): Int = when (policy) {
        0 -> extendedValue(RodinExtendedKeys.CPU_LIVE_MIN_0)
        4 -> extendedValue(RodinExtendedKeys.CPU_LIVE_MIN_4)
        7 -> extendedValue(RodinExtendedKeys.CPU_LIVE_MIN_7)
        else -> -1
    }

    fun cpuLiveMaxFrequency(policy: Int): Int = when (policy) {
        0 -> extendedValue(RodinExtendedKeys.CPU_LIVE_MAX_0)
        4 -> extendedValue(RodinExtendedKeys.CPU_LIVE_MAX_4)
        7 -> extendedValue(RodinExtendedKeys.CPU_LIVE_MAX_7)
        else -> -1
    }

    fun cpuFrequencyDrift(policy: Int): Int = when (policy) {
        0 -> extendedValue(RodinExtendedKeys.CPU_FREQ_DRIFT_0)
        4 -> extendedValue(RodinExtendedKeys.CPU_FREQ_DRIFT_4)
        7 -> extendedValue(RodinExtendedKeys.CPU_FREQ_DRIFT_7)
        else -> -1
    }

    fun setExpertGamut(gamut: Int): Boolean = setExtendedOperation(RodinExtendedKeys.OP_EXPERT_GAMUT, gamut)
    fun setExpertChannel(channel: Int, value: Int): Boolean =
        setExtendedOperation(RodinExtendedKeys.OP_EXPERT_CHANNEL, channel, value)
    fun resetExpertDisplay(): Boolean = setExtendedOperation(RodinExtendedKeys.OP_EXPERT_RESET, 0)

    // ------------------------------------------------------------------ gates

    private fun send(command: Command): Boolean {
        if (!started) return false
        return commands.offer(command)
    }

    private fun sendCharging(command: Command): Boolean {
        if (!started) return false
        val previous = synchronized(stateLock) {
            if (cache.chargingWriteState == 1) return false
            val old = cache.chargingWriteState
            cache = cache.copy(chargingWriteState = 1)
            old
        }
        if (commands.offer(command)) return true
        synchronized(stateLock) { cache = cache.copy(chargingWriteState = previous) }
        return false
    }

    private fun sendServiceAction(command: Command): Boolean {
        if (!started) return false
        val previous = synchronized(stateLock) {
            if (extended[RodinExtendedKeys.SERVICE_ACTION] == 1) return false
            val old = extended[RodinExtendedKeys.SERVICE_ACTION]
            extended[RodinExtendedKeys.SERVICE_ACTION] = 1
            old
        }
        if (commands.offer(command)) return true
        synchronized(stateLock) { extended[RodinExtendedKeys.SERVICE_ACTION] = previous }
        return false
    }
}
