package io.github.neeschal.rodinessential

import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.Icon
import android.os.Handler
import android.os.Looper
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.view.animation.DecelerateInterpolator
import io.github.neeschal.rodinessential.backend.RodinBackend
import io.github.neeschal.rodinessential.backend.RodinBypassTelemetry

/** SystemUI owns the tile. The daemon owns charging. Never flip a fake state. */
class BypassTileService : TileService() {
    private val main = Handler(Looper.getMainLooper())
    private var listening = false
    private var lastState = -1
    private var motionIcons: Array<Icon>? = null
    private var iconMotion: ValueAnimator? = null
    private var iconFrame = 0

    private val update = object : Runnable {
        override fun run() {
            if (!listening) return
            publish()
            main.postDelayed(this, if ((lastState and 16) != 0) 80L else 500L)
        }
    }

    private fun ensureBackend() {
        RodinBackend.initialize(applicationContext)
        RodinBackend.start()
    }

    override fun onStartListening() {
        super.onStartListening()
        ensureBackend()
        listening = true
        lastState = -1
        main.removeCallbacks(update)
        main.post(update)
    }

    override fun onStopListening() {
        listening = false
        main.removeCallbacks(update)
        stopIconMotion()
        super.onStopListening()
    }

    override fun onDestroy() {
        listening = false
        main.removeCallbacks(update)
        stopIconMotion()
        super.onDestroy()
    }

    override fun onClick() {
        super.onClick()
        ensureBackend()
        if (isLocked) {
            unlockAndRun { toggle() }
            return
        }
        toggle()
    }

    private fun toggle() {
        if (!RodinBypassTelemetry.tileToggle()) return
        publish()
        main.removeCallbacks(update)
        if (listening) main.postDelayed(update, 80)
    }

    private fun publish() {
        val tile = qsTile ?: return
        val state = RodinBypassTelemetry.tileSnapshot()
        if (state == lastState) return
        val available = (state and 35) == 35
        val enabled = (state and 4) != 0
        val status = RodinBypassTelemetry.tileStatus(state)
        // Animate only a confirmed ON/OFF change. Unchanged snapshots never
        // resend the drawable; SystemUI controls animation and reduced motion.
        val changed = lastState >= 0 && available && (lastState and 35) == 35 &&
            ((lastState xor state) and 4) != 0
        // Remote animated vectors are frozen by some OEM Control Centers.
        // Initial/resting states use vectors; a confirmed toggle gets cached
        // monochrome frames. Status updates never reset the animation.
        if (!changed && (lastState < 0 || ((lastState xor state) and 4) != 0)) {
            stopIconMotion()
            iconFrame = if (enabled) LAST_ICON_FRAME else 0
            val drawable = if (enabled) "ic_bypass_on" else "ic_bypass"
            val icon = resources.getIdentifier(drawable, "drawable", packageName)
            if (icon != 0) tile.icon = Icon.createWithResource(this, icon)
        }
        tile.label = "Bypass charging"
        tile.subtitle = status
        tile.state = when {
            !available -> Tile.STATE_UNAVAILABLE
            enabled -> Tile.STATE_ACTIVE
            else -> Tile.STATE_INACTIVE
        }
        tile.contentDescription = "Bypass charging, $status"
        tile.updateTile()
        lastState = state
        if (changed) animateIcon(enabled)
    }

    private fun stopIconMotion() {
        iconMotion?.cancel()
        iconMotion = null
    }

    private fun animateIcon(enabled: Boolean) {
        stopIconMotion()
        if (!listening) return
        if (motionIcons == null) {
            motionIcons = Array(LAST_ICON_FRAME + 1) { index ->
                powerPathIcon(index / LAST_ICON_FRAME.toFloat())
            }
        }
        val icons = motionIcons ?: return
        val destination = if (enabled) LAST_ICON_FRAME else 0
        if (!ValueAnimator.areAnimatorsEnabled()) {
            iconFrame = destination
            qsTile?.let { tile ->
                tile.icon = icons[destination]
                tile.updateTile()
            }
            return
        }
        iconMotion = ValueAnimator.ofInt(iconFrame, destination).apply {
            duration = if (enabled) 320L else 220L
            interpolator = DecelerateInterpolator(1.5f)
            addUpdateListener { animation ->
                if (!listening) return@addUpdateListener
                val frame = animation.animatedValue as Int
                if (frame == iconFrame) return@addUpdateListener
                iconFrame = frame
                qsTile?.let { tile ->
                    tile.icon = icons[frame]
                    tile.updateTile()
                }
            }
            start()
        }
    }

    private fun powerPathIcon(flow: Float): Icon {
        // Same 32-unit artwork as the app's _BypassPowerPathPainter.
        // NEESCHAL: animate the rail, never invent the charging state.
        val bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
        // 96 px at 640 dpi has the same intrinsic 24 dp as the resting vector.
        bitmap.density = 640
        val canvas = Canvas(bitmap)
        canvas.scale(3f, 3f)
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 1.8f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        canvas.drawLine(4f, 7f, 4f, 10f, stroke)
        canvas.drawLine(8f, 7f, 8f, 10f, stroke)
        canvas.drawRoundRect(RectF(2f, 10f, 10f, 17f), 2f, 2f, stroke)
        val rail = Path().apply {
            moveTo(6f, 17f)
            lineTo(6f, 20f)
            quadTo(6f, 22f, 8f, 22f)
            lineTo(14f, 22f)
            quadTo(16f, 22f, 16f, 20f)
            lineTo(16f, 13f)
            lineTo(21f, 13f)
        }
        canvas.drawPath(rail, stroke)
        canvas.drawRoundRect(RectF(21f, 8f, 30f, 18f), 2f, 2f, stroke)
        for (x in floatArrayOf(24f, 27f)) {
            canvas.drawLine(x, 5.5f, x, 8f, stroke)
            canvas.drawLine(x, 18f, x, 20.5f, stroke)
        }
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        canvas.drawRect(24f, 11f, 27f, 15f, fill)
        stroke.strokeWidth = 1.4f
        stroke.alpha = Math.round(255 * (0.55f - flow * 0.25f))
        canvas.drawRoundRect(RectF(20f, 25f, 30f, 30f), 1f, 1f, stroke)
        canvas.drawLine(18.5f, 26.5f, 18.5f, 28.5f, stroke)
        canvas.drawLine(17f, 23.5f, 20f, 23.5f, stroke)
        if (flow > 0f) {
            fill.alpha = Math.round(255 * flow)
            canvas.drawCircle(16f + 3f * flow, 13f, 2f, fill)
        }
        return Icon.createWithBitmap(bitmap)
    }

    private companion object {
        const val LAST_ICON_FRAME = 16
    }
}
