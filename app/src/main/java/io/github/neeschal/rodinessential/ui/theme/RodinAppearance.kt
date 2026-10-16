package io.github.neeschal.rodinessential.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import org.json.JSONObject
import java.io.File

enum class RodinThemePreference { system, light, dark }

enum class RodinBackgroundStyle { system, midnight, aurora, custom }

/**
 * Drives the Material theme like the original `RodinThemeController`.
 * Persisted as part of `rodin-appearance.json`.
 */
object RodinThemeController {
    var preference by mutableStateOf(RodinThemePreference.system)
        private set

    fun preferenceFromName(name: String?): RodinThemePreference =
        RodinThemePreference.entries.firstOrNull { it.name == name }
            ?: RodinThemePreference.system

    fun setPreference(value: RodinThemePreference) {
        preference = value
    }

    /** Mirrors the Dart `bootstrap`: appearance must never block app boot. */
    fun bootstrap(context: Context) {
        try {
            val file = RodinAppearanceStore.appearanceFile(context)
            if (!file.exists()) return
            val raw = JSONObject(file.readText())
            setPreference(
                preferenceFromName(if (raw.has("themeMode")) raw.optString("themeMode") else null),
            )
        } catch (_: Exception) {
            preference = RodinThemePreference.system
        }
    }
}

data class RodinAppearanceConfig(
    val themePreference: RodinThemePreference = RodinThemePreference.system,
    val backgroundStyle: RodinBackgroundStyle = RodinBackgroundStyle.system,
    val backgroundBlur: Double = 0.0,
    val customPath: String = "",
    val accentIndex: Int = 0,
    val cardRadius: Double = 22.0,
    val cardStyle: Int = 0,
    val heroGlow: Boolean = true,
    val coreVisualizerStyle: Int = 0,
) {
    val activeAccent: Color
        get() = accents[accentIndex.coerceIn(0, accents.size - 1)]

    fun toJson(): JSONObject = JSONObject().apply {
        put("themeMode", themePreference.name)
        put("backgroundStyle", backgroundStyle.name)
        put("backgroundBlur", backgroundBlur)
        put("customPath", customPath)
        put("accentIndex", accentIndex)
        put("cardRadius", cardRadius)
        put("cardStyle", cardStyle)
        put("heroGlow", heroGlow)
        put("coreVisualizerStyle", coreVisualizerStyle)
    }

    companion object {
        val accents: List<Color> = listOf(
            0xFF59BCFF, // Electric Azure (Default)
            0xFF00E5FF, // Neon Cyan
            0xFF38E598, // Emerald Mint
            0xFFB087FF, // Cyber Violet
            0xFFFFA048, // Hyper Amber
            0xFFFF5376, // Crimson Rose
            0xFFD0D7DE, // Titanium Slate
        ).map { Color(it) }

        val accentNames: List<String> = listOf(
            "Azure",
            "Cyan",
            "Mint",
            "Violet",
            "Amber",
            "Rose",
            "Titanium",
        )

        fun fromJson(raw: String?): RodinAppearanceConfig {
            val obj = try {
                raw?.let(::JSONObject)
            } catch (_: Exception) {
                null
            } ?: return RodinAppearanceConfig()

            val themeName = if (obj.has("themeMode")) obj.optString("themeMode") else null
            val styleName = obj.optString("backgroundStyle", "system")
            val style = RodinBackgroundStyle.entries.firstOrNull { it.name == styleName }
                ?: RodinBackgroundStyle.system
            val blur = obj.optDouble("backgroundBlur", 0.0).coerceIn(0.0, 28.0)

            return RodinAppearanceConfig(
                themePreference = RodinThemeController.preferenceFromName(themeName),
                backgroundStyle = style,
                backgroundBlur = blur,
                customPath = obj.optString("customPath", ""),
                accentIndex = obj.optInt("accentIndex", 0),
                cardRadius = obj.optDouble("cardRadius", 22.0),
                cardStyle = obj.optInt("cardStyle", 0),
                heroGlow = if (obj.has("heroGlow")) obj.optBoolean("heroGlow", true) else true,
                coreVisualizerStyle = obj.optInt("coreVisualizerStyle", 0),
            )
        }
    }
}

/** Equivalent of the Dart `RodinAppearanceScope` inherited widget. */
val LocalRodinAppearance = staticCompositionLocalOf { RodinAppearanceConfig() }

object RodinAppearanceStore {
    fun appearanceFile(context: Context): File = File(context.filesDir, "rodin-appearance.json")

    fun load(context: Context): RodinAppearanceConfig? {
        return try {
            val file = appearanceFile(context)
            if (!file.exists()) null else RodinAppearanceConfig.fromJson(file.readText())
        } catch (_: Exception) {
            null
        }
    }

    fun save(context: Context, config: RodinAppearanceConfig) {
        try {
            appearanceFile(context).writeText(config.toJson().toString())
        } catch (_: Exception) {
            // Appearance persistence is non-critical.
        }
    }
}
