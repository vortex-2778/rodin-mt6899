package io.github.neeschal.rodinessential.backend

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Observes only the native palette cache while one transaction is running.
 * No IPC, hardware polling or system writes are performed here.
 *
 * The normal dashboard cadence is 500 ms; palette completions must not sit
 * behind it while the user is choosing the next color, so this monitor
 * samples the two atomic cache fields every 32 ms for a bounded 90 seconds
 * and stops completely on the terminal state.
 */
class RodinSystemColorsMonitor(
    private val readStamp: () -> Pair<Int, Int>,
    private val onChanged: () -> Unit,
) {
    companion object {
        const val INTERVAL_MS = 32L
        const val MAX_SAMPLES = 2813
    }

    private var job: Job? = null

    fun watch(scope: CoroutineScope) {
        job?.cancel()
        job = scope.launch {
            var previous: Pair<Int, Int>? = null
            var samples = 0
            while (isActive) {
                delay(INTERVAL_MS)
                val stamp = readStamp()
                val changed = stamp != previous
                previous = stamp
                samples++
                // Stop before notifying: the completion callback can queue
                // another operation and start a new watch without this one
                // cancelling it.
                val terminal = stamp.first != 1 || samples >= MAX_SAMPLES
                if (changed) onChanged()
                if (terminal) break
            }
        }
    }

    fun dispose() {
        job?.cancel()
        job = null
    }
}
