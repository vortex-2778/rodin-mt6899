package io.github.neeschal.rodinessential.platform

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.core.content.ContextCompat
import java.lang.ref.WeakReference

/**
 * Small Android framework adapter, ported from the native host's JNI helpers
 * (`lib.rs`, `platform_bridge.rs`). No charging writes and no backend work on
 * Android's main thread. NEESCHAL's rule: report state, don't invent it.
 */
object RodinPlatform {
    private var activityReference: WeakReference<Activity>? = null
    private var photoPermissionRequester: (() -> Boolean)? = null

    fun attach(activity: Activity, requestPhotoPermission: () -> Boolean) {
        activityReference = WeakReference(activity)
        photoPermissionRequester = requestPhotoPermission
    }

    fun detach(activity: Activity) {
        if (activityReference?.get() === activity) {
            activityReference = null
            photoPermissionRequester = null
        }
    }

    // ---------------------------------------------------------------- haptics


    private fun hapticConstant(kind: Int, sdk: Int): Int? = when (kind) {
        1 -> HapticFeedbackConstants.VIRTUAL_KEY
        2 -> HapticFeedbackConstants.CONTEXT_CLICK
        3 -> if (sdk >= 34) HapticFeedbackConstants.TOGGLE_ON else HapticFeedbackConstants.CONTEXT_CLICK
        4 -> if (sdk >= 34) HapticFeedbackConstants.TOGGLE_OFF else HapticFeedbackConstants.CONTEXT_CLICK
        5 -> if (sdk >= 34) HapticFeedbackConstants.SEGMENT_TICK else HapticFeedbackConstants.CLOCK_TICK
        6 -> if (sdk >= 34) HapticFeedbackConstants.SEGMENT_FREQUENT_TICK else HapticFeedbackConstants.CLOCK_TICK
        7 -> if (sdk >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY
        8 -> if (sdk >= 30) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.CONTEXT_CLICK
        9 -> if (sdk >= 30) HapticFeedbackConstants.GESTURE_END else 8 // End gesture.
        else -> null
    }

    /** Kinds 1..9 match the native `rodin_host_haptic` table. */
    fun haptic(kind: Int): Boolean {
        if (kind < 1 || kind > 9) return false
        val activity = activityReference?.get() ?: return false
        val constant = hapticConstant(kind, Build.VERSION.SDK_INT) ?: return false
        return activity.window.decorView.performHapticFeedback(constant)
    }

    // ----------------------------------------------------------- support links

    fun openSupportLink(code: Int): Boolean {
        val url = when (code) {
            0 -> "https://github.com/NEESCHAL-3"
            1 -> "https://t.me/PocoX7ProNepalChat"
            else -> return false
        }
        val activity = activityReference?.get() ?: return false
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            activity.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    // -------------------------------------------------------- photo permission

    private fun granted(activity: Activity, permission: String): Boolean =
        ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED

    /** 2 full, 1 partial (selected photos), 0 none. */
    fun photoPermissionState(): Int {
        val activity = activityReference?.get() ?: return 0
        return if (Build.VERSION.SDK_INT >= 33) {
            when {
                granted(activity, Manifest.permission.READ_MEDIA_IMAGES) -> 2
                Build.VERSION.SDK_INT >= 34 &&
                    granted(activity, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) -> 1
                else -> 0
            }
        } else {
            if (granted(activity, Manifest.permission.READ_EXTERNAL_STORAGE)) 2 else 0
        }
    }

    /** Permissions handed to the Activity's permission launcher. */
    fun photoPermissionsToRequest(): Array<String> = when {
        Build.VERSION.SDK_INT >= 34 -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
        )
        Build.VERSION.SDK_INT >= 33 -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    fun requestPhotoPermission(): Boolean {
        if (activityReference?.get() == null) return false
        if (photoPermissionState() == 2) return true
        val requester = photoPermissionRequester ?: return false
        return requester()
    }
}
