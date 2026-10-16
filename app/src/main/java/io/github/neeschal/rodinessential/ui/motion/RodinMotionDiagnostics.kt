package io.github.neeschal.rodinessential.ui.motion

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Choreographer
import kotlin.math.roundToInt

/**
 * Development-only measurements, never an always-running production ticker.
 * The Dart version was gated by RODIN_MOTION_TRACE; this port keeps the same
 * opt-in constant, records durations only, and never touches user content.
 *
 * Android has no direct build/raster split outside FrameMetrics; at rest this
 * object is inert, and when enabled it measures frame intervals.
 */
object RodinMotionDiagnostics {
    /** Build-time opt-in, matching the Dart `bool.fromEnvironment` default. */
    const val enabled = false

    private const val TAG = "RodinMotionTrace"

    private val handler = Handler(Looper.getMainLooper())
    private val frames = ArrayList<Long>()
    private var attached = false
    private var edge = ""
    private var finish: Runnable? = null
    private var lastFrameNanos = 0L

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!attached) return
            if (lastFrameNanos != 0L) {
                frames.add((frameTimeNanos - lastFrameNanos) / 1000)
            }
            lastFrameNanos = frameTimeNanos
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    fun begin(rightEdge: Boolean, label: String? = null) {
        if (!enabled) return
        if (attached) report()
        edge = label ?: if (rightEdge) "right" else "left"
        frames.clear()
        finish?.let(handler::removeCallbacks)
        lastFrameNanos = 0L
        Choreographer.getInstance().postFrameCallback(frameCallback)
        attached = true
        // Lifecycle loss must not leave diagnostics collecting indefinitely.
        val stop = Runnable { report() }
        finish = stop
        handler.postDelayed(stop, 4000)
    }

    fun end() {
        if (!enabled || !attached) return
        finish?.let(handler::removeCallbacks)
        // Wait long enough for the final frames of a short opening animation
        // rather than dropping the report.
        val stop = Runnable { report() }
        finish = stop
        handler.postDelayed(stop, 1500)
    }

    private fun report() {
        if (!attached) return
        Choreographer.getInstance().removeFrameCallback(frameCallback)
        attached = false
        finish?.let(handler::removeCallbacks)
        finish = null
        if (frames.isEmpty()) return
        val sorted = frames.sorted()
        val slow120 = sorted.count { it > 8333 }
        val slow60 = sorted.count { it > 16667 }
        val p95 = ((sorted.size - 1) * 0.95).roundToInt()
        Log.d(
            TAG,
            "RODIN_BACK_TIMINGS edge=$edge frames=${sorted.size} " +
                "build_p95_us=${sorted[p95]} raster_p95_us=${sorted[p95]} " +
                "build_max_us=${sorted.last} raster_max_us=${sorted.last} " +
                "over_8ms=$slow120 over_16ms=$slow60",
        )
        frames.clear()
    }
}
