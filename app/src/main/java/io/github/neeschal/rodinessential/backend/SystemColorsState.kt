package io.github.neeschal.rodinessential.backend

import org.json.JSONObject

/**
 * Cached Android palette readback. Color values are opaque 24-bit RGB, not
 * generated previews. Loading this model never queries an Android service.
 */
data class RodinSystemColorsState(
    val operationState: Int = 0,
    val supported: Boolean = false,
    /** 0: wallpaper, 1: custom seed, 2: external preset without readable seed. */
    val mode: Int = -1,
    val seed: Int = -1,
    val style: Int = -1,
    val primary: Int = -1,
    val secondary: Int = -1,
    val tertiary: Int = -1,
    val neutral: Int = -1,
    val neutralVariant: Int = -1,
    val sdk: Int = -1,
    val user: Int = -1,
    /**
     * 0: read/already selected, 1: native colors changed,
     * 2: wallpaper following restored with the same resolved colors.
     */
    val outcome: Int = 0,
    val error: Int = 0,
    val revision: Int = 0,
    val contrastOperationState: Int = 0,
    val contrastSupported: Boolean = false,
    val contrast: Int = 0,
    val lightContainer: Int = -1,
    val lightOnContainer: Int = -1,
    val darkContainer: Int = -1,
    val darkOnContainer: Int = -1,
    val contrastError: Int = 0,
    val contrastRevision: Int = 0,
) {
    val busy: Boolean get() = operationState == 1
    val ready: Boolean get() = operationState == 2
    val failed: Boolean get() = operationState == -1
    val contrastBusy: Boolean get() = contrastOperationState == 1
    val contrastReady: Boolean get() = contrastOperationState == 2
    val contrastFailed: Boolean get() = contrastOperationState == -1

    val nativeRoleColors: List<Int>
        get() = listOf(lightContainer, lightOnContainer, darkContainer, darkOnContainer)

    val hasNativeRoles: Boolean
        get() = contrastRevision > 0 && contrastSupported &&
            nativeRoleColors.all { it in 0..0xFFFFFF }

    val nativeColors: List<Int>
        get() = listOf(primary, secondary, tertiary, neutral, neutralVariant)

    val hasReadback: Boolean
        get() = revision > 0 && nativeColors.all { it in 0..0xFFFFFF }

    val errorMessage: String
        get() = when (error) {
            1 -> "Material You control is unavailable. This requires Android 12 or newer, " +
                "native dynamic-color resources, and an updated System Colors backend."
            2 -> "Android\u2019s existing theme data could not be read safely. It was left " +
                "untouched. Choose a theme in system settings, then refresh."
            3 -> "The system theme changed while applying. Refresh and try again."
            4 -> "Android denied or did not finish a theme command. Check that the ROM " +
                "or module includes the updated backend and its policy."
            5 -> "Android saved the request but did not produce a verified native palette. " +
                "This ROM may expose the resources without implementing the standard " +
                "Material You update path. The previous palette was restored."
            6 -> "Another palette operation is still running. Please try again shortly."
            8 -> "This color or palette style is not supported by this Android version."
            9 -> "The backend returned an incomplete palette. Update the app and " +
                "backend together."
            else -> "The system service could not be reached. Connect the updated Rodin " +
                "backend, then refresh."
        }

    companion object {
        fun fromNative(read: (Int) -> Int): RodinSystemColorsState = RodinSystemColorsState(
            operationState = read(RodinExtendedKeys.PALETTE_STATE),
            mode = read(RodinExtendedKeys.PALETTE_MODE),
            seed = read(RodinExtendedKeys.PALETTE_SEED),
            style = read(RodinExtendedKeys.PALETTE_STYLE),
            primary = read(RodinExtendedKeys.PALETTE_PRIMARY),
            secondary = read(RodinExtendedKeys.PALETTE_SECONDARY),
            tertiary = read(RodinExtendedKeys.PALETTE_TERTIARY),
            neutral = read(RodinExtendedKeys.PALETTE_NEUTRAL),
            neutralVariant = read(RodinExtendedKeys.PALETTE_NEUTRAL_VARIANT),
            sdk = read(RodinExtendedKeys.PALETTE_SDK),
            user = read(RodinExtendedKeys.PALETTE_USER),
            outcome = read(RodinExtendedKeys.PALETTE_OUTCOME),
            error = read(RodinExtendedKeys.PALETTE_ERROR),
            revision = read(RodinExtendedKeys.PALETTE_REVISION),
            supported = read(RodinExtendedKeys.PALETTE_SUPPORTED) == 1,
            contrastOperationState = read(RodinExtendedKeys.CONTRAST_STATE),
            contrastSupported = read(RodinExtendedKeys.CONTRAST_SUPPORTED) == 1,
            contrast = read(RodinExtendedKeys.CONTRAST_LEVEL),
            lightContainer = read(RodinExtendedKeys.CONTRAST_LIGHT_CONTAINER),
            lightOnContainer = read(RodinExtendedKeys.CONTRAST_LIGHT_ON_CONTAINER),
            darkContainer = read(RodinExtendedKeys.CONTRAST_DARK_CONTAINER),
            darkOnContainer = read(RodinExtendedKeys.CONTRAST_DARK_ON_CONTAINER),
            contrastError = read(RodinExtendedKeys.CONTRAST_ERROR),
            contrastRevision = read(RodinExtendedKeys.CONTRAST_REVISION),
        )
    }
}

/**
 * Last confirmed choice for presentation during startup, never native
 * resource readback or authority to write a system setting.
 */
data class RodinSystemColorsSelection(
    val wallpaper: Boolean,
    val seed: Int,
    val style: Int,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("schema", 1)
        .put("wallpaper", wallpaper)
        .put("seed", seed)
        .put("style", style)

    companion object {
        const val SCHEMA = 1

        fun fromNative(state: RodinSystemColorsState): RodinSystemColorsSelection? {
            if (!state.ready || !state.hasReadback) return null
            if (state.mode != 0 && state.mode != 1) return null
            if (state.style !in 0..6) return null
            if (state.mode == 1 && (state.seed < 0 || state.seed > 0xFFFFFF)) return null
            return RodinSystemColorsSelection(
                wallpaper = state.mode == 0,
                seed = if (state.seed >= 0) state.seed else 0x008577,
                style = state.style,
            )
        }

        fun fromJson(value: JSONObject?): RodinSystemColorsSelection? {
            val json = value ?: return null
            if (json.optInt("schema", -1) != SCHEMA) return null
            if (!json.has("wallpaper") || !json.has("seed") || !json.has("style")) return null
            val wallpaper = json.opt("wallpaper")
            val seed = json.opt("seed")
            val style = json.opt("style")
            if (wallpaper !is Boolean || seed !is Int || style !is Int) return null
            if (seed < 0 || seed > 0xFFFFFF || style < 0 || style > 6) return null
            return RodinSystemColorsSelection(wallpaper = wallpaper, seed = seed, style = style)
        }
    }
}
