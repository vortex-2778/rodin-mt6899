package io.github.neeschal.rodinessential

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.window.BackEvent
import android.window.OnBackAnimationCallback
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import io.github.neeschal.rodinessential.backend.PerAppBackend
import io.github.neeschal.rodinessential.backend.RodinBackend
import io.github.neeschal.rodinessential.platform.RodinPlatform
import io.github.neeschal.rodinessential.ui.shell.RodinBackDispatch
import io.github.neeschal.rodinessential.ui.shell.RodinInteractionSettings
import io.github.neeschal.rodinessential.ui.shell.RodinShell
import io.github.neeschal.rodinessential.ui.theme.RodinThemeController

/** NEESCHAL: a platform adapter, not a second UI toolkit. */
class MainActivity : ComponentActivity() {
    private val main = Handler(Looper.getMainLooper())
    private val refresh = Runnable { refreshBack() }
    private var callback: OnBackInvokedCallback? = null
    private var registered = false
    private var resumed = false

    private val photoPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { /* The UI re-reads the grant state after every request. */ }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        enableEdgeToEdge()

        RodinThemeController.bootstrap(this)
        RodinBackend.initialize(this)
        RodinBackend.start()
        PerAppBackend.instance.initialize(this)
        RodinInteractionSettings.load(this)

        RodinPlatform.attach(this) {
            try {
                photoPermissionLauncher.launch(RodinPlatform.photoPermissionsToRequest())
                true
            } catch (_: Exception) {
                false
            }
        }

        RodinBackDispatch.interceptChangedListener = {
            main.removeCallbacks(refresh)
            main.post(refresh)
        }

        callback = if (Build.VERSION.SDK_INT >= 34) {
            ProgressCallback()
        } else {
            // No progress data below API 34: Back either happens or it does not.
            OnBackInvokedCallback { RodinBackDispatch.deliverBack(4, 1f, false) }
        }

        handleTileIntent(intent)

        setContent { RodinShell() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleTileIntent(intent)
    }

    private fun handleTileIntent(intent: Intent?) {
        if (intent != null &&
            "android.service.quicksettings.action.QS_TILE_PREFERENCES" == intent.action
        ) {
            RodinBackDispatch.deliverBypassSettingsRequest()
        }
    }

    override fun onResume() {
        super.onResume()
        resumed = true
        refreshBack()
        RodinBackend.start()
    }

    override fun onPause() {
        resumed = false
        refreshBack()
        RodinBackDispatch.deliverBack(3, 0f, false)
        super.onPause()
    }

    override fun onDestroy() {
        main.removeCallbacks(refresh)
        RodinPlatform.detach(this)
        super.onDestroy()
    }

    @Deprecated("The predictive path owns Back from API 33 on.")
    override fun onBackPressed() {
        // The 31/32 fallback: Back still reaches the shell, but it carries no
        // progress, exactly like RodinActivity's nativeBack(4, 1, 0) path.
        if (RodinBackDispatch.interceptEnabled) {
            RodinBackDispatch.deliverBack(4, 1f, false)
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }

    private fun refreshBack() {
        val current = callback ?: return
        // Below API 33 there is no dispatcher to register with.
        if (Build.VERSION.SDK_INT < 33) return
        val wanted = resumed && RodinBackDispatch.interceptEnabled
        if (wanted == registered) return
        if (wanted) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                current,
            )
        } else {
            onBackInvokedDispatcher.unregisterOnBackInvokedCallback(current)
        }
        registered = wanted
    }

    // Isolated API-34 class: older devices never instantiate progress callbacks.
    private inner class ProgressCallback : OnBackAnimationCallback {
        private var edge = false

        override fun onBackStarted(event: BackEvent) {
            edge = event.swipeEdge == BackEvent.EDGE_RIGHT
            RodinBackDispatch.deliverBack(1, event.progress, edge)
        }

        override fun onBackProgressed(event: BackEvent) {
            RodinBackDispatch.deliverBack(2, event.progress, edge)
        }

        override fun onBackCancelled() {
            RodinBackDispatch.deliverBack(3, 0f, edge)
        }

        override fun onBackInvoked() {
            RodinBackDispatch.deliverBack(4, 1f, edge)
        }
    }
}
