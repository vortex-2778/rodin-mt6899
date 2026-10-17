package io.github.neeschal.rodinessential.backend

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** A user-named palette saved beside the recent custom seeds. */
data class RodinSavedPalette(
    val name: String,
    val seed: Int,
    val style: Int,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("name", name)
        .put("seed", seed)
        .put("style", style)

    companion object {
        fun fromJson(value: JSONObject?): RodinSavedPalette? {
            val json = value ?: return null
            val name = json.opt("name")
            val seed = json.opt("seed")
            val style = json.opt("style")
            if (name !is String || name.trim().isEmpty() || name.length > 32) return null
            if (seed !is Int || seed < 0 || seed > 0xFFFFFF) return null
            if (style !is Int || style < 0 || style > 6) return null
            return RodinSavedPalette(name = name.trim(), seed = seed, style = style)
        }
    }
}

/**
 * Optional local history for the System Colors screen. Palette application
 * itself remains independent from this file.
 */
class RodinSystemColorsLibrary {
    companion object {
        private const val FILE_NAME = "rodin-system-colors-library.json"
        private const val MAX_LENGTH = 16L * 1024
        const val MAX_RECENTS = 8
        const val MAX_SAVED = 12
    }

    val recents = mutableListOf<Pair<Int, Int>>()
    val saved = mutableListOf<RodinSavedPalette>()

    fun load() {
        try {
            val file = File(RodinSystemColorsPreferences.storageDir ?: return, FILE_NAME)
            if (!file.exists() || file.length() > MAX_LENGTH) return
            val decoded = JSONObject(file.readText())
            if (decoded.optInt("schema", -1) != 1) return
            decoded.optJSONArray("recents")?.let { array ->
                recents.clear()
                for (index in 0 until array.length()) {
                    val item = array.optJSONArray(index) ?: continue
                    if (item.length() != 2) continue
                    val seed = item.optInt(0, -1)
                    val style = item.optInt(1, -1)
                    if (seed in 0..0xFFFFFF && style in 0..6) recents.add(seed to style)
                    if (recents.size >= MAX_RECENTS) break
                }
            }
            decoded.optJSONArray("saved")?.let { array ->
                saved.clear()
                for (index in 0 until array.length()) {
                    val palette = RodinSavedPalette.fromJson(array.optJSONObject(index))
                        ?: continue
                    saved.add(palette)
                    if (saved.size >= MAX_SAVED) break
                }
            }
        } catch (_: Exception) {
            recents.clear()
            saved.clear()
        }
    }

    fun record(seed: Int, style: Int) {
        recents.removeAll { it.first == seed && it.second == style }
        recents.add(0, seed to style)
        if (recents.size > MAX_RECENTS) {
            recents.subList(MAX_RECENTS, recents.size).clear()
        }
        write()
    }

    fun save(name: String, seed: Int, style: Int) {
        val trimmed = name.trim()
        saved.removeAll { it.name.lowercase() == trimmed.lowercase() }
        saved.add(0, RodinSavedPalette(name = trimmed, seed = seed, style = style))
        if (saved.size > MAX_SAVED) {
            saved.subList(MAX_SAVED, saved.size).clear()
        }
        write()
    }

    fun remove(palette: RodinSavedPalette) {
        saved.remove(palette)
        write()
    }

    private fun write() {
        try {
            val directory = RodinSystemColorsPreferences.storageDir ?: return
            directory.mkdirs()
            val root = JSONObject()
                .put("schema", 1)
                .put(
                    "recents",
                    JSONArray().apply {
                        recents.forEach { item -> put(JSONArray().put(item.first).put(item.second)) }
                    },
                )
                .put(
                    "saved",
                    JSONArray().apply {
                        saved.forEach { palette -> put(palette.toJson()) }
                    },
                )
            val file = File(directory, FILE_NAME)
            val pending = File(directory, "$FILE_NAME.pending")
            pending.writeText(root.toString())
            if (file.exists()) file.delete()
            pending.renameTo(file)
        } catch (_: Exception) {
            // Palette application remains independent from optional local history.
        }
    }
}
