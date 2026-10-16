package io.github.neeschal.rodinessential.ui.shell

import android.content.Context
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONObject
import java.io.File
import kotlin.math.roundToInt

/**
 * One motion-speed and haptics preference shared by every transition.
 * NEESCHAL: the user tunes the feel once; the whole app follows.
 */
object RodinInteractionSettings {
    private const val MOTION_MODEL_VERSION = 6

    /** Bumped whenever motion or haptics settings change. */
    var revision by mutableIntStateOf(0)
        private set

    var motionSpeed by mutableStateOf(1.00)
        private set

    var hapticsEnabled by mutableStateOf(true)
        private set

    private var loaded = false
    private var storageContext: Context? = null

    private val settingsFile: File?
        get() = storageContext?.let { File(it.filesDir, "rodin-interaction.json") }

    val motionProgress: Double
        get() = (motionSpeed.coerceIn(0.75, 1.35) - 0.75) / 0.60

    val motionLabel: String
        get() = when {
            motionSpeed < 0.90 -> "Relaxed and cinematic"
            motionSpeed > 1.15 -> "Direct and responsive"
            else -> "Balanced and fluid"
        }

    private fun mix(relaxed: Double, direct: Double): Double =
        relaxed + (direct - relaxed) * motionProgress

    val pressCurve: Easing
        get() = CubicBezierEasing(
            mix(0.16, 0.28).toFloat(),
            mix(1.00, 0.00).toFloat(),
            mix(0.30, 0.18).toFloat(),
            1.00f,
        )

    val transitionCurve: Easing
        get() = CubicBezierEasing(
            mix(0.16, 0.26).toFloat(),
            mix(1.00, 0.00).toFloat(),
            mix(0.30, 0.16).toFloat(),
            1.00f,
        )

    val pressScale: Double get() = mix(0.985, 0.965)
    val pressOpacity: Double get() = mix(0.975, 0.935)
    val detailSlideDistance: Double get() = mix(0.035, 0.065)
    val pageScale: Double get() = mix(0.985, 0.992)

    val interactionSpring: SpringSpec<Float>
        get() = spring(dampingRatio = 0.78f, stiffness = mix(430.0, 560.0).toFloat())

    fun motionDuration(baseMilliseconds: Int): Int =
        (baseMilliseconds / motionSpeed.coerceIn(0.75, 1.35)).roundToInt().coerceIn(70, 900)

    fun load(context: Context) {
        storageContext = context.applicationContext
        if (loaded) {
            return
        }
        loaded = true
        try {
            val file = settingsFile
            if (file == null || !file.exists()) {
                revision += 1
                return
            }
            val decoded = JSONObject(file.readText())
            val version = if (decoded.has("motionModelVersion")) {
                decoded.optInt("motionModelVersion", 0)
            } else {
                0
            }
            if (version >= MOTION_MODEL_VERSION) {
                val raw = if (decoded.has("motionSpeed")) decoded.optDouble("motionSpeed") else null
                motionSpeed = (raw ?: 1.00).coerceIn(0.75, 1.35)
            } else {
                motionSpeed = 1.00
            }
            if (decoded.has("hapticsEnabled")) {
                hapticsEnabled = decoded.optBoolean("hapticsEnabled", true)
            }
            revision += 1
            if (version != MOTION_MODEL_VERSION) {
                persist()
            }
        } catch (_: Exception) {
            revision += 1
        }
    }

    fun previewMotionSpeed(value: Double) {
        motionSpeed = value.coerceIn(0.75, 1.35)
    }

    fun commitMotionSpeed(value: Double) {
        previewMotionSpeed(value)
        revision += 1
        persist()
    }

    fun resetMotion() {
        motionSpeed = 1.00
        revision += 1
        persist()
    }

    fun setHaptics(enabled: Boolean) {
        hapticsEnabled = enabled
        revision += 1
        persist()
    }

    private fun persist() {
        try {
            val file = settingsFile ?: return
            file.parentFile?.mkdirs()
            file.writeText(
                JSONObject().apply {
                    put("motionModelVersion", MOTION_MODEL_VERSION)
                    put("motionSpeed", motionSpeed)
                    put("hapticsEnabled", hapticsEnabled)
                }.toString(),
            )
        } catch (_: Exception) {
            // Interaction preferences are non-critical.
        }
    }
}
