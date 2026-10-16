package io.github.neeschal.rodinessential.backend

/**
 * Extended cache keys and operation codes of the daemon protocol 13.6
 * backend, ported one-to-one from the original native host
 * (`runtime/host-rust/src/backend_bridge.rs`).
 */
object RodinExtendedKeys {
    const val COUNT = 122

    // Snapshot telemetry slots.
    const val PHASE = 0
    const val EXPERT_GAMUT = 2
    const val EXPERT_1 = 3
    const val EXPERT_2 = 4
    const val EXPERT_3 = 5
    const val EXPERT_4 = 6
    const val EXPERT_5 = 7
    const val EXPERT_6 = 8
    const val EXPERT_7 = 9
    const val EXPERT_8 = 10
    const val PERF_SUPPORTED = 11
    const val PERF_VERIFIED = 12
    const val PERF_VERIFY_OK = 13
    const val CPU_DRIFT_0 = 14
    const val CPU_DRIFT_4 = 15
    const val CPU_DRIFT_7 = 16
    const val GPU_DRIFT = 17
    const val IO_DRIFT = 18
    const val DISPLAY_WIDTH = 22
    const val DISPLAY_HEIGHT = 23
    const val DISPLAY_HZ_X10 = 24
    const val DISPLAY_MAX_HZ_X10 = 25
    const val PERSISTENCE_LOADED = 26
    const val SUNLIGHT_SAVED = 27
    const val DISPLAY_ACK = 28
    const val TOUCH_ACK = 29
    const val RUNTIME_KEEPALIVE_ACK = 32
    const val RUNTIME_KEEPALIVE_COUNT = 33
    const val CPU_MANUAL = 34
    const val CPU_WRITE_ACK = 35
    const val CPU_SAVED_MASK = 36
    const val CORE_CTL_NODES = 37
    const val DISPLAY_DENSITY = 38
    const val ZRAM_SIZE = 39
    const val ZRAM_ORIG = 40
    const val ZRAM_COMPR = 41
    const val ZRAM_USED = 42
    const val ZRAM_SWAPPINESS = 43
    const val ZRAM_ALGORITHM = 44
    const val GPU_LOAD = 45
    const val GPU_CUR_FREQ = 46
    const val GPU_MIN_FREQ = 47
    const val GPU_MAX_FREQ = 48
    const val GPU_DEVFREQ_GOVERNOR = 49
    const val GPU_GED_BOOST = 50
    const val GPU_THERMAL_STATE = 51
    const val GPU_UNCAP_ACTIVE = 52
    const val CPU_MIN_0 = 53
    const val CPU_MAX_0 = 54
    const val CPU_MIN_4 = 55
    const val CPU_MAX_4 = 56
    const val CPU_MIN_7 = 57
    const val CPU_MAX_7 = 58
    const val GPU_POWER_POLICY = 59
    const val TOUCH_SUSTAINED_RATE = 60
    const val TOUCH_INSTANT_RATE = 61
    const val TOUCH_PANEL = 62
    const val TOUCH_CONTROL_PATH = 63
    const val TOUCH_MEASURED_RATE_X10 = 64
    const val TOUCH_RESAMPLER_READY = 65
    const val TOUCH_MEASUREMENT_ACTIVE = 66
    const val TOUCH_SOURCE_RATE_X10 = 67
    const val CPU_LIVE_MIN_0 = 68
    const val CPU_LIVE_MAX_0 = 69
    const val CPU_LIVE_MIN_4 = 70
    const val CPU_LIVE_MAX_4 = 71
    const val CPU_LIVE_MIN_7 = 72
    const val CPU_LIVE_MAX_7 = 73
    const val CPU_FREQ_ACK = 74
    const val CPU_FREQ_DRIFT_0 = 75
    const val CPU_FREQ_DRIFT_4 = 76
    const val CPU_FREQ_DRIFT_7 = 77
    const val DISPLAY_NATIVE_DENSITY = 78
    const val TOUCH_RESAMPLER_PATH = 79
    const val TOUCH_RESAMPLER_ERROR = 80

    // System Colors transaction state (palette).
    const val PALETTE_STATE = 81
    const val PALETTE_MODE = 82
    const val PALETTE_SEED = 83
    const val PALETTE_STYLE = 84
    const val PALETTE_PRIMARY = 85
    const val PALETTE_SECONDARY = 86
    const val PALETTE_TERTIARY = 87
    const val PALETTE_NEUTRAL = 88
    const val PALETTE_NEUTRAL_VARIANT = 89
    const val PALETTE_SDK = 90
    const val PALETTE_USER = 91
    const val PALETTE_OUTCOME = 92
    const val PALETTE_ERROR = 93
    const val PALETTE_REVISION = 94
    const val PALETTE_SUPPORTED = 95

    // System Colors transaction state (contrast).
    const val CONTRAST_STATE = 96
    const val CONTRAST_SUPPORTED = 97
    const val CONTRAST_LEVEL = 98
    const val CONTRAST_LIGHT_CONTAINER = 99
    const val CONTRAST_LIGHT_ON_CONTAINER = 100
    const val CONTRAST_DARK_CONTAINER = 101
    const val CONTRAST_DARK_ON_CONTAINER = 102
    const val CONTRAST_ERROR = 103
    const val CONTRAST_REVISION = 104

    // Charging and bypass telemetry.
    const val CHARGING_SUPPORTED = 105
    const val CHARGING_FCC_UA = 106
    const val CHARGING_ADAPTER_W = 107
    const val CHARGING_PD_AUTH = 108
    const val CHARGING_QUICK_TYPE = 109
    const val CHARGING_SIC = 110
    const val CHARGING_LIVE_MW = 111
    const val BYPASS_SUPPORTED = 112
    const val BYPASS_STATE = 113
    const val BYPASS_SAVED = 114
    const val BYPASS_VERIFIED = 115
    const val SERVICE_ENABLED = 116
    const val SERVICE_CONFIGURED = 117
    const val SERVICE_ACTION = 118
    const val BYPASS_THRESHOLD = 119
    const val BYPASS_PHASE = 120
    const val BYPASS_ERROR = 121

    // Extended operation codes (`rodin_backend_extended_set`).
    const val OP_EXPERT_GAMUT = 2
    const val OP_EXPERT_CHANNEL = 3
    const val OP_EXPERT_RESET = 4
    const val OP_CPU_MANUAL = 7
    const val OP_CPU_CORE = 8
    const val OP_DISPLAY_RESOLUTION = 9
    const val OP_ZRAM_SIZE = 10
    const val OP_ZRAM_ALGORITHM = 11
    const val OP_ZRAM_SWAPPINESS = 12
    const val OP_ZRAM_COMPACT = 13
    const val OP_GPU_UNCAP = 14
    const val OP_GPU_MIN_FREQ = 15
    const val OP_GPU_MAX_FREQ = 16
    const val OP_GPU_DEVFREQ_GOVERNOR = 17
    const val OP_GPU_GED_BOOST = 18
    const val OP_CPU_CLUSTER_MIN_FREQ = 19
    const val OP_CPU_CLUSTER_MAX_FREQ = 20
    const val OP_CPU_CLUSTER_FREQ_RANGE = 21
    const val OP_GPU_POWER_POLICY = 22
    const val OP_CPU_FREQ_RESET = 23
    const val OP_SYSTEM_COLORS = 24
    const val OP_SYSTEM_COLORS_WALLPAPER = 25
    const val OP_GET_SYSTEM_COLORS = 26
    const val OP_GET_CONTRAST = 27
    const val OP_SET_CONTRAST = 28
    const val OP_SERVICE_ENABLED = 29
    const val OP_SERVICE_RESET = 30
    const val OP_BYPASS_THRESHOLD = 31
}
