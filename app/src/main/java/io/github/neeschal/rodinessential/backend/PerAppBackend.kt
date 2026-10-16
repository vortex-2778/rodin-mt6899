package io.github.neeschal.rodinessential.backend

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets

/**
 * Daemon-owned profiles, never a second preference owner in the UI process.
 *
 * The daemon remains authoritative for per-app overrides and the memory and
 * storage subsystem clocks; this class only carries framed exchanges and
 * resolves the local package labels and icons for the chooser.
 */
class PerAppBackend private constructor() {
    companion object {
        val instance = PerAppBackend()

        private const val MAX_EXCHANGE_BYTES = 4096
        private const val MAX_ICON_PACKAGES = 40
        private const val ICON_PIXELS = 96
    }

    private val exchangeLock = Any()

    @Volatile
    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    /**
     * Blocking daemon exchange. Call from a worker thread; replies must be
     * `{"ok": true, "data": ...}` and are returned as JSON values.
     */
    @Throws(Exception::class)
    fun command(command: String): Any? {
        if (command.toByteArray(StandardCharsets.UTF_8).size > MAX_EXCHANGE_BYTES) {
            throw IllegalArgumentException("Profile request is too large")
        }
        val raw = synchronized(exchangeLock) {
            RodinDaemonTransport.framedRequest(command)
        }
        val reply = JSONObject(raw)
        if (!reply.optBoolean("ok")) {
            throw IllegalStateException(reply.optString("error", "profile exchange failed"))
        }
        val data = reply.opt("data")
        if (command == "GET app.list" && data is JSONArray) {
            annotateLabels(data)
        }
        return data
    }

    private fun annotateLabels(apps: JSONArray) {
        try {
            val packages = ArrayList<String>(apps.length())
            for (index in 0 until apps.length()) {
                val app = apps.optJSONObject(index) ?: continue
                packages.add(app.optString("package"))
            }
            val labels = labels(packages)
            for (index in 0 until apps.length()) {
                val app = apps.optJSONObject(index) ?: continue
                val packageName = app.optString("package")
                app.put("label", labels[packageName] ?: packageName)
            }
        } catch (_: Exception) {
            // Labels are optional; never hide installed apps.
        }
    }

    /** Local package labels; failures fall back to the package name. */
    fun labels(packages: List<String>): Map<String, String> {
        val context = appContext ?: return emptyMap()
        val manager = context.packageManager
        val result = LinkedHashMap<String, String>()
        for (packageName in packages) {
            if (packageName.isEmpty()) continue
            result[packageName] = try {
                manager.getApplicationInfo(packageName, 0).loadLabel(manager).toString()
            } catch (_: Exception) {
                packageName
            }
        }
        return result
    }

    /** Local package icons rasterized to 96 px, mirroring the native bridge. */
    fun icons(packages: List<String>): Map<String, Bitmap> {
        val context = appContext ?: return emptyMap()
        val manager = context.packageManager
        val result = LinkedHashMap<String, Bitmap>()
        for (packageName in packages.take(MAX_ICON_PACKAGES)) {
            if (packageName.isEmpty()) continue
            try {
                val drawable = manager.getApplicationIcon(packageName)
                result[packageName] = drawableToBitmap(drawable)
            } catch (_: Exception) {
                // Icons are optional; the chooser still lists every app.
            }
        }
        return result
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val bitmap = Bitmap.createBitmap(ICON_PIXELS, ICON_PIXELS, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
