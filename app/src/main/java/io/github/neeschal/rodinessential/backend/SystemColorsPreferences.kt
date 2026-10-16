package io.github.neeschal.rodinessential.backend

import org.json.JSONObject
import java.io.File

/**
 * A UI-only hint. Android remains the owner of the actual palette.
 *
 * The original interface stored this JSON beside the application files
 * directory; [storageDir] is provided by [RodinBackend.initialize].
 */
object RodinSystemColorsPreferences {
    @Volatile
    var storageDir: File? = null

    private const val FILE_NAME = "rodin-system-colors.json"
    private const val MAX_LENGTH = 1024L

    fun read(): RodinSystemColorsSelection? {
        return try {
            val file = File(storageDir ?: return null, FILE_NAME)
            if (!file.exists() || file.length() > MAX_LENGTH) return null
            val text = file.readText().trim()
            if (text == "null" || text.isEmpty()) return null
            RodinSystemColorsSelection.fromJson(JSONObject(text))
        } catch (_: Exception) {
            null
        }
    }

    fun write(selection: RodinSystemColorsSelection?) {
        try {
            val directory = storageDir ?: return
            directory.mkdirs()
            val file = File(directory, FILE_NAME)
            val pending = File(directory, "$FILE_NAME.pending")
            pending.writeText(selection?.toJson()?.toString() ?: "null")
            if (file.exists()) file.delete()
            pending.renameTo(file)
        } catch (_: Exception) {
            // A missing UI hint must not affect Android's saved palette.
        }
    }
}
