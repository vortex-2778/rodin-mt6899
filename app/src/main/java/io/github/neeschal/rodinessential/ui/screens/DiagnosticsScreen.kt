package io.github.neeschal.rodinessential.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.neeschal.rodinessential.backend.RodinBackend
import io.github.neeschal.rodinessential.backend.RodinConnectionState
import io.github.neeschal.rodinessential.ui.components.SurfaceCard

/** Real-time hardware status and system health checks. */
@Composable
fun DiagnosticsScreen(onBack: () -> Unit) {
    val snapshot = rememberRodinSnapshot()
    val colors = MaterialTheme.colorScheme
    val accent = Color(0xFF74E6C6)

    LaunchedEffect(Unit) {
        RodinBackend.refresh()
    }

    val phase = RodinBackend.extendedValue(0)
    val perfSupported = RodinBackend.extendedValue(11)
    val perfVerified = RodinBackend.extendedValue(12)
    val perfOk = RodinBackend.extendedValue(13)
    val persistence = RodinBackend.extendedValue(26)
    val displayAck = RodinBackend.extendedValue(28)
    val touchAck = RodinBackend.extendedValue(29)

    val drifts = listOf(
        RodinBackend.extendedValue(14),
        RodinBackend.extendedValue(15),
        RodinBackend.extendedValue(16),
        RodinBackend.extendedValue(17),
        RodinBackend.extendedValue(18),
    )
    val knownDrifts = drifts.filter { it >= 0 }
    val noKnownDrift = knownDrifts.isEmpty() || knownDrifts.all { it == 0 }

    RodinScrollPage(
        header = RodinPageHeader.Detail(title = "Diagnostics", onBack = onBack),
    ) {
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Real-time hardware status and system health checks",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.5.sp,
                color = colors.onSurfaceVariant,
            ),
        )
        Spacer(Modifier.height(10.dp))
        HeroCard(
            icon = Icons.Rounded.MonitorHeart,
            accent = accent,
            title = when {
                snapshot.ready && !snapshot.serviceEnabled -> "Rodin Essential Disabled"
                snapshot.ready -> "System Health Normal"
                snapshot.connection == RodinConnectionState.CONNECTING ->
                    "Connecting to System Service"
                else -> "System Service Unavailable"
            },
            subtitle = when {
                snapshot.ready && !snapshot.serviceEnabled ->
                    "Hardware control is paused; enable Rodin Essential in Settings"
                phase == 16 -> "All hardware controllers and telemetry streams active"
                else -> "Waiting for system background service"
            },
        )
        Spacer(Modifier.height(9.dp))
        SurfaceCard {
            DiagnosticRow(
                label = "Hardware Controller",
                good = snapshot.controlsAvailable && phase == 16,
                detail = when {
                    snapshot.ready && !snapshot.serviceEnabled -> "Disabled"
                    snapshot.ready -> "Online & Ready"
                    else -> snapshot.serviceStatusLabel
                },
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 7.dp),
                thickness = 0.8.dp,
            )
            DiagnosticRow(
                label = "Touch Controller",
                good = snapshot.touchHal == 1,
                detail = if (snapshot.touchHal == 1) "Active" else "Unavailable",
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 7.dp),
                thickness = 0.8.dp,
            )
            DiagnosticRow(
                label = "Display Engine",
                good = snapshot.displayHal == 1,
                detail = if (snapshot.displayHal == 1) "Active" else "Unavailable",
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 7.dp),
                thickness = 0.8.dp,
            )
            DiagnosticRow(
                label = "Saved Preferences",
                good = persistence == 1,
                detail = if (persistence == 1) "Loaded" else "Not loaded",
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 7.dp),
                thickness = 0.8.dp,
            )
            DiagnosticRow(
                label = "System Optimizations",
                good = perfOk == 1 && perfSupported > 0 && perfSupported == perfVerified,
                detail = if (perfSupported >= 0) {
                    "$perfVerified / $perfSupported active"
                } else {
                    "Checking..."
                },
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 7.dp),
                thickness = 0.8.dp,
            )
            DiagnosticRow(
                label = "Hardware Sync Check",
                good = noKnownDrift,
                detail = if (noKnownDrift) "In sync" else "Adjusting...",
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 7.dp),
                thickness = 0.8.dp,
            )
            DiagnosticRow(
                label = "Display Bridge",
                good = displayAck == 1,
                detail = when (displayAck) {
                    1 -> "Ready"
                    0 -> "Failed"
                    else -> "Active"
                },
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 7.dp),
                thickness = 0.8.dp,
            )
            DiagnosticRow(
                label = "Touch Bridge",
                good = touchAck == 1,
                detail = when (touchAck) {
                    1 -> "Ready"
                    0 -> "Failed"
                    else -> "Active"
                },
            )
        }
        Spacer(Modifier.height(10.dp))
    }
}
