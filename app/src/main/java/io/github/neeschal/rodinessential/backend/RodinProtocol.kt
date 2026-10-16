package io.github.neeschal.rodinessential.backend

/**
 * Wire-format helpers for the daemon's text protocol 13.6.
 *
 * Ported one-to-one from the native host bridge so field parsing, code
 * mapping and palette validation behave exactly like the original
 * interface, including duplicate-field rejection.
 */
object RodinProtocol {
    const val PROTOCOL_VERSION = "13.6"

    /** Snapshot field name to extended cache index. */
    val EXTENDED_FIELDS: List<Pair<String, Int>> = listOf(
        "phase" to RodinExtendedKeys.PHASE,
        // Slot 1 is retired; the other indices stay for ABI compatibility.
        "expert_gamut" to RodinExtendedKeys.EXPERT_GAMUT,
        "expert_1" to RodinExtendedKeys.EXPERT_1,
        "expert_2" to RodinExtendedKeys.EXPERT_2,
        "expert_3" to RodinExtendedKeys.EXPERT_3,
        "expert_4" to RodinExtendedKeys.EXPERT_4,
        "expert_5" to RodinExtendedKeys.EXPERT_5,
        "expert_6" to RodinExtendedKeys.EXPERT_6,
        "expert_7" to RodinExtendedKeys.EXPERT_7,
        "expert_8" to RodinExtendedKeys.EXPERT_8,
        "perf_supported" to RodinExtendedKeys.PERF_SUPPORTED,
        "perf_verified" to RodinExtendedKeys.PERF_VERIFIED,
        "perf_verify_ok" to RodinExtendedKeys.PERF_VERIFY_OK,
        "cpu_drift0" to RodinExtendedKeys.CPU_DRIFT_0,
        "cpu_drift4" to RodinExtendedKeys.CPU_DRIFT_4,
        "cpu_drift7" to RodinExtendedKeys.CPU_DRIFT_7,
        "gpu_drift" to RodinExtendedKeys.GPU_DRIFT,
        "io_drift" to RodinExtendedKeys.IO_DRIFT,
        "display_width" to RodinExtendedKeys.DISPLAY_WIDTH,
        "display_height" to RodinExtendedKeys.DISPLAY_HEIGHT,
        "display_hz_x10" to RodinExtendedKeys.DISPLAY_HZ_X10,
        "display_max_hz_x10" to RodinExtendedKeys.DISPLAY_MAX_HZ_X10,
        "persistence_loaded" to RodinExtendedKeys.PERSISTENCE_LOADED,
        "sunlight_saved" to RodinExtendedKeys.SUNLIGHT_SAVED,
        "display_ack" to RodinExtendedKeys.DISPLAY_ACK,
        "touch_ack" to RodinExtendedKeys.TOUCH_ACK,
        "runtime_keepalive_ack" to RodinExtendedKeys.RUNTIME_KEEPALIVE_ACK,
        "runtime_keepalive_count" to RodinExtendedKeys.RUNTIME_KEEPALIVE_COUNT,
        "cpu_manual" to RodinExtendedKeys.CPU_MANUAL,
        "cpu_write_ack" to RodinExtendedKeys.CPU_WRITE_ACK,
        "cpu_saved_mask" to RodinExtendedKeys.CPU_SAVED_MASK,
        "core_ctl_nodes" to RodinExtendedKeys.CORE_CTL_NODES,
        "display_density" to RodinExtendedKeys.DISPLAY_DENSITY,
        "zram_size" to RodinExtendedKeys.ZRAM_SIZE,
        "zram_orig" to RodinExtendedKeys.ZRAM_ORIG,
        "zram_compr" to RodinExtendedKeys.ZRAM_COMPR,
        "zram_used" to RodinExtendedKeys.ZRAM_USED,
        "zram_swappiness" to RodinExtendedKeys.ZRAM_SWAPPINESS,
        "gpu_load" to RodinExtendedKeys.GPU_LOAD,
        "gpu_cur_freq" to RodinExtendedKeys.GPU_CUR_FREQ,
        "gpu_min_freq" to RodinExtendedKeys.GPU_MIN_FREQ,
        "gpu_max_freq" to RodinExtendedKeys.GPU_MAX_FREQ,
        "gpu_ged_boost" to RodinExtendedKeys.GPU_GED_BOOST,
        "gpu_thermal_state" to RodinExtendedKeys.GPU_THERMAL_STATE,
        "gpu_uncap_active" to RodinExtendedKeys.GPU_UNCAP_ACTIVE,
        "cpu_min0" to RodinExtendedKeys.CPU_MIN_0,
        "cpu_max0" to RodinExtendedKeys.CPU_MAX_0,
        "cpu_min4" to RodinExtendedKeys.CPU_MIN_4,
        "cpu_max4" to RodinExtendedKeys.CPU_MAX_4,
        "cpu_min7" to RodinExtendedKeys.CPU_MIN_7,
        "cpu_max7" to RodinExtendedKeys.CPU_MAX_7,
        "gpu_power_policy" to RodinExtendedKeys.GPU_POWER_POLICY,
        "touch_sustained_rate" to RodinExtendedKeys.TOUCH_SUSTAINED_RATE,
        "touch_instant_rate" to RodinExtendedKeys.TOUCH_INSTANT_RATE,
        "touch_panel" to RodinExtendedKeys.TOUCH_PANEL,
        "touch_control_path" to RodinExtendedKeys.TOUCH_CONTROL_PATH,
        "touch_measured_rate_x10" to RodinExtendedKeys.TOUCH_MEASURED_RATE_X10,
        "touch_resampler_ready" to RodinExtendedKeys.TOUCH_RESAMPLER_READY,
        "touch_measurement_active" to RodinExtendedKeys.TOUCH_MEASUREMENT_ACTIVE,
        "touch_source_rate_x10" to RodinExtendedKeys.TOUCH_SOURCE_RATE_X10,
        "cpu_live_min0" to RodinExtendedKeys.CPU_LIVE_MIN_0,
        "cpu_live_max0" to RodinExtendedKeys.CPU_LIVE_MAX_0,
        "cpu_live_min4" to RodinExtendedKeys.CPU_LIVE_MIN_4,
        "cpu_live_max4" to RodinExtendedKeys.CPU_LIVE_MAX_4,
        "cpu_live_min7" to RodinExtendedKeys.CPU_LIVE_MIN_7,
        "cpu_live_max7" to RodinExtendedKeys.CPU_LIVE_MAX_7,
        "cpu_freq_ack" to RodinExtendedKeys.CPU_FREQ_ACK,
        "cpu_freq_drift0" to RodinExtendedKeys.CPU_FREQ_DRIFT_0,
        "cpu_freq_drift4" to RodinExtendedKeys.CPU_FREQ_DRIFT_4,
        "cpu_freq_drift7" to RodinExtendedKeys.CPU_FREQ_DRIFT_7,
        "display_native_density" to RodinExtendedKeys.DISPLAY_NATIVE_DENSITY,
        "touch_resampler_path" to RodinExtendedKeys.TOUCH_RESAMPLER_PATH,
        "touch_resampler_error" to RodinExtendedKeys.TOUCH_RESAMPLER_ERROR,
        "charging_supported" to RodinExtendedKeys.CHARGING_SUPPORTED,
        "charging_fcc_ua" to RodinExtendedKeys.CHARGING_FCC_UA,
        "charging_adapter_w" to RodinExtendedKeys.CHARGING_ADAPTER_W,
        "charging_pd_auth" to RodinExtendedKeys.CHARGING_PD_AUTH,
        "charging_quick_type" to RodinExtendedKeys.CHARGING_QUICK_TYPE,
        "charging_sic" to RodinExtendedKeys.CHARGING_SIC,
        "charging_live_mw" to RodinExtendedKeys.CHARGING_LIVE_MW,
        "bypass_charging_supported" to RodinExtendedKeys.BYPASS_SUPPORTED,
        "bypass_charging_state" to RodinExtendedKeys.BYPASS_STATE,
        "bypass_charging_saved" to RodinExtendedKeys.BYPASS_SAVED,
        "bypass_charging_verified" to RodinExtendedKeys.BYPASS_VERIFIED,
        "service_enabled" to RodinExtendedKeys.SERVICE_ENABLED,
        "service_configured" to RodinExtendedKeys.SERVICE_CONFIGURED,
        "bypass_threshold" to RodinExtendedKeys.BYPASS_THRESHOLD,
        "bypass_phase" to RodinExtendedKeys.BYPASS_PHASE,
        "bypass_error" to RodinExtendedKeys.BYPASS_ERROR,
    )

    private val PALETTE_FIELDS: List<Triple<String, Int, IntRange>> = listOf(
        Triple("mode", RodinExtendedKeys.PALETTE_MODE, 0..2),
        Triple("seed", RodinExtendedKeys.PALETTE_SEED, -1..0xFFFFFF),
        Triple("style", RodinExtendedKeys.PALETTE_STYLE, -1..6),
        Triple("primary", RodinExtendedKeys.PALETTE_PRIMARY, 0..0xFFFFFF),
        Triple("secondary", RodinExtendedKeys.PALETTE_SECONDARY, 0..0xFFFFFF),
        Triple("tertiary", RodinExtendedKeys.PALETTE_TERTIARY, 0..0xFFFFFF),
        Triple("neutral", RodinExtendedKeys.PALETTE_NEUTRAL, 0..0xFFFFFF),
        Triple("neutral_variant", RodinExtendedKeys.PALETTE_NEUTRAL_VARIANT, 0..0xFFFFFF),
        Triple("sdk", RodinExtendedKeys.PALETTE_SDK, 31..Int.MAX_VALUE),
        Triple("user", RodinExtendedKeys.PALETTE_USER, 0..Int.MAX_VALUE),
        Triple("outcome", RodinExtendedKeys.PALETTE_OUTCOME, 0..2),
        Triple("supported", RodinExtendedKeys.PALETTE_SUPPORTED, 0..1),
    )

    private val CONTRAST_FIELDS: List<Triple<String, Int, IntRange>> = listOf(
        Triple("supported", RodinExtendedKeys.CONTRAST_SUPPORTED, 0..1),
        Triple("level", RodinExtendedKeys.CONTRAST_LEVEL, -1000..1000),
        Triple("light_container", RodinExtendedKeys.CONTRAST_LIGHT_CONTAINER, 0..0xFFFFFF),
        Triple("light_on_container", RodinExtendedKeys.CONTRAST_LIGHT_ON_CONTAINER, 0..0xFFFFFF),
        Triple("dark_container", RodinExtendedKeys.CONTRAST_DARK_CONTAINER, 0..0xFFFFFF),
        Triple("dark_on_container", RodinExtendedKeys.CONTRAST_DARK_ON_CONTAINER, 0..0xFFFFFF),
    )

    /** Splits `key=value;...` fields at the first `=`, like `split_once`. */
    fun parseFields(body: String): Map<String, String> {
        val map = LinkedHashMap<String, String>()
        for (field in body.split(';')) {
            val separator = field.indexOf('=')
            if (separator >= 0) {
                map[field.substring(0, separator)] = field.substring(separator + 1)
            }
        }
        return map
    }

    fun parseI32(value: String?): Int = value?.toIntOrNull() ?: -1

    fun parseI64(value: String?): Long = value?.toLongOrNull() ?: -1L

    fun codeStatus(value: String?): Int = when (value?.lowercase()) {
        "charging" -> 1
        "discharging" -> 2
        "not charging" -> 3
        "full" -> 4
        else -> 0
    }

    fun codeHealth(value: String?): Int = when (value?.lowercase()) {
        "good" -> 1
        "overheat" -> 2
        "dead" -> 3
        "over voltage" -> 4
        "cold" -> 5
        else -> 0
    }

    fun codeUsbType(value: String?): Int {
        val upper = (value ?: "").uppercase()
        return when {
            upper.contains("PD") -> 4
            upper.contains("DCP") -> 3
            upper.contains("CDP") -> 2
            upper.contains("SDP") -> 1
            upper == "NA" || upper.isEmpty() -> -1
            else -> 0
        }
    }

    /** Parses a CPU list such as `0-3,7` into an online mask. */
    fun cpuMask(value: String?): Int {
        var mask = 0
        for (partRaw in (value ?: "").split(',')) {
            val part = partRaw.trim()
            val range = part.split("-", limit = 2)
            if (range.size == 2) {
                val start = range[0].toIntOrNull() ?: continue
                val end = range[1].toIntOrNull() ?: continue
                var cpu = start
                while (cpu <= minOf(end, 31)) {
                    mask = mask or (1 shl cpu)
                    cpu++
                }
            } else {
                val cpu = part.toIntOrNull()
                if (cpu != null && cpu < 32 && cpu >= 0) {
                    mask = mask or (1 shl cpu)
                }
            }
        }
        return mask
    }

    fun cpuGovernorCode(value: String?): Int = when (value) {
        "sugov_ext" -> 0
        "conservative" -> 1
        "powersave" -> 2
        "performance" -> 3
        "schedutil" -> 4
        else -> -1
    }

    fun gpuGovernorCode(value: String?): Int = when (value) {
        "dummy" -> 0
        "powersave" -> 1
        "performance" -> 2
        "simple_ondemand" -> 3
        else -> -1
    }

    fun ioCode(value: String?): Int = when (value) {
        "none" -> 0
        "mq-deadline" -> 1
        "kyber" -> 2
        "bfq" -> 3
        else -> -1
    }

    fun cpuGovernorName(code: Int): String? = when (code) {
        0 -> "sugov_ext"
        1 -> "conservative"
        2 -> "powersave"
        3 -> "performance"
        4 -> "schedutil"
        else -> null
    }

    fun gpuDevfreqGovernorName(code: Int): String? = when (code) {
        0 -> "dummy"
        1 -> "powersave"
        2 -> "performance"
        3 -> "simple_ondemand"
        else -> null
    }

    fun gpuGovernorName(code: Int): String? = when (code) {
        0 -> "dummy"
        1 -> "powersave"
        2 -> "performance"
        3 -> "simple_ondemand"
        else -> null
    }

    fun ioName(code: Int): String? = when (code) {
        0 -> "none"
        1 -> "mq-deadline"
        2 -> "kyber"
        3 -> "bfq"
        else -> null
    }

    fun zramAlgorithmName(code: Int): String = when (code) {
        1 -> "zstd"
        2 -> "lzo-rle"
        3 -> "lzo"
        else -> "lz4"
    }

    fun zramAlgorithmCode(value: String?): Int = when (value) {
        "zstd" -> 1
        "lzo-rle" -> 2
        "lzo" -> 3
        else -> 0
    }

    /** GPU governor code as reported into extended slot 49. */
    fun gpuDevfreqSlotCode(value: String?): Int = when (value) {
        "performance" -> 1
        "powersave" -> 2
        "userspace" -> 3
        "dummy" -> 4
        else -> 0
    }

    @Throws(IllegalArgumentException::class)
    fun parsePaletteResponse(body: String): List<Pair<Int, Int>> =
        parseFieldsChecked(body, PALETTE_FIELDS, "palette_response_invalid")

    @Throws(IllegalArgumentException::class)
    fun parseContrastResponse(body: String): List<Pair<Int, Int>> =
        parseFieldsChecked(body, CONTRAST_FIELDS, "palette_response_invalid")

    @Throws(IllegalArgumentException::class)
    private fun parseFieldsChecked(
        body: String,
        fields: List<Triple<String, Int, IntRange>>,
        errorTag: String,
    ): List<Pair<Int, Int>> {
        val map = HashMap<String, String>()
        for (field in body.split(';')) {
            val separator = field.indexOf('=')
            if (separator < 0) continue
            val key = field.substring(0, separator)
            val value = field.substring(separator + 1)
            if (map.put(key, value) != null) {
                throw IllegalArgumentException("$errorTag: duplicate palette field")
            }
        }
        val values = ArrayList<Pair<Int, Int>>(fields.size)
        for ((name, index, range) in fields) {
            val parsed = map[name]?.toIntOrNull()
                ?: throw IllegalArgumentException("$errorTag: incomplete native palette response")
            if (parsed !in range) {
                throw IllegalArgumentException("$errorTag: invalid native palette value")
            }
            values.add(index to parsed)
        }
        return values
    }

    /** Maps daemon palette failures to the documented error codes. */
    fun paletteErrorCode(error: String): Int = when {
        error.contains("palette_unsupported") || error.contains("unknown command") -> 1
        error.contains("palette_invalid_settings") -> 2
        error.contains("palette_conflict") -> 3
        error.contains("palette_service_unavailable") -> 4
        error.contains("palette_readback") || error.contains("palette_restore_failed") -> 5
        error.contains("palette_busy") -> 6
        error.contains("palette_invalid_choice") -> 8
        error.contains("palette_response_invalid") -> 9
        else -> 7
    }
}
