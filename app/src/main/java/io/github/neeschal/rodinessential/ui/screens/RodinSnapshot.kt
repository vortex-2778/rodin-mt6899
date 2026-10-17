package io.github.neeschal.rodinessential.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.github.neeschal.rodinessential.backend.RodinBackend
import io.github.neeschal.rodinessential.backend.RodinBackendSnapshot

/**
 * The live telemetry frame every dashboard page renders from: one state flow
 * off the daemon cache, exactly like the Dart `_BackendSnapshotBuilder`.
 */
@Composable
fun rememberRodinSnapshot(): RodinBackendSnapshot {
    val snapshot by RodinBackend.snapshots.collectAsState()
    return snapshot
}
