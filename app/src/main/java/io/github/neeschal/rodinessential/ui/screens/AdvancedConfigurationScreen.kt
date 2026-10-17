package io.github.neeschal.rodinessential.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Tune
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
import io.github.neeschal.rodinessential.ui.components.SectionLabel
import io.github.neeschal.rodinessential.ui.components.SurfaceCard

/** Processor governors, graphics scaling, and storage queue policies. */
@Composable
fun AdvancedConfigurationScreen(onBack: () -> Unit) {
    val snapshot = rememberRodinSnapshot()
    val colors = MaterialTheme.colorScheme

    LaunchedEffect(Unit) {
        RodinBackend.refresh()
    }

    val cpuGovernors = listOf(
        RodinChoiceOption(0, "sugov_ext"),
        RodinChoiceOption(1, "conservative"),
        RodinChoiceOption(2, "powersave"),
        RodinChoiceOption(3, "performance"),
        RodinChoiceOption(4, "schedutil"),
    )
    val schedulers = listOf(
        RodinChoiceOption(0, "none"),
        RodinChoiceOption(1, "mq-deadline"),
        RodinChoiceOption(2, "kyber"),
        RodinChoiceOption(3, "bfq"),
    )

    val drift0 = RodinBackend.extendedValue(14)
    val drift4 = RodinBackend.extendedValue(15)
    val drift7 = RodinBackend.extendedValue(16)
    val ioDrift = RodinBackend.extendedValue(18)
    val enabled = snapshot.controlsAvailable && !snapshot.busy

    RodinScrollPage(
        header = RodinPageHeader.Detail(title = "Advanced Configuration", onBack = onBack),
    ) {
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Processor governors, graphics scaling, and storage queue policies",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.5.sp,
                color = colors.onSurfaceVariant,
            ),
        )
        Spacer(Modifier.height(10.dp))
        HeroCard(
            icon = Icons.Rounded.Tune,
            accent = Color(0xFFFF9F68),
            title = "Persistent System Tuning",
            subtitle = "Settings are saved securely and automatically maintained in the background",
        )
        Spacer(Modifier.height(12.dp))
        SectionLabel("CPU governors")
        Spacer(Modifier.height(8.dp))
        RodinChoiceCard(
            title = "Efficiency cluster",
            subtitle = "Efficiency cores (Cores 0–3)",
            icon = Icons.Rounded.Memory,
            accent = Color(0xFF46CFA1),
            selectedCode = snapshot.cpuGovernor0,
            options = cpuGovernors,
            enabled = enabled,
            onSelected = { code -> RodinBackend.setCpuGovernor(0, code) },
        )
        Spacer(Modifier.height(9.dp))
        RodinChoiceCard(
            title = "Performance cluster",
            subtitle = "Performance cores (Cores 4–6)",
            icon = Icons.Rounded.Memory,
            accent = Color(0xFFB087FF),
            selectedCode = snapshot.cpuGovernor4,
            options = cpuGovernors,
            enabled = enabled,
            onSelected = { code -> RodinBackend.setCpuGovernor(4, code) },
        )
        Spacer(Modifier.height(9.dp))
        RodinChoiceCard(
            title = "Prime cluster",
            subtitle = "Prime core (Core 7)",
            icon = Icons.Rounded.Memory,
            accent = Color(0xFFFF9F68),
            selectedCode = snapshot.cpuGovernor7,
            options = cpuGovernors,
            enabled = enabled,
            onSelected = { code -> RodinBackend.setCpuGovernor(7, code) },
        )
        Spacer(Modifier.height(9.dp))
        SurfaceCard {
            Column {
                PersistenceStatusRow(
                    label = "Efficiency governor persistence",
                    state = drift0,
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 7.dp),
                    thickness = 0.8.dp,
                )
                PersistenceStatusRow(
                    label = "Performance governor persistence",
                    state = drift4,
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 7.dp),
                    thickness = 0.8.dp,
                )
                PersistenceStatusRow(
                    label = "Prime governor persistence",
                    state = drift7,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        SectionLabel("Storage scheduler")
        Spacer(Modifier.height(8.dp))
        RodinChoiceCard(
            title = "UFS storage scheduler",
            subtitle = "Queue policy across every detected UFS LUN",
            icon = Icons.Rounded.Storage,
            accent = colors.primary,
            selectedCode = snapshot.ufsScheduler,
            options = schedulers,
            enabled = enabled,
            onSelected = { code -> RodinBackend.setUfsScheduler(code) },
        )
        Spacer(Modifier.height(9.dp))
        SurfaceCard {
            PersistenceStatusRow(
                label = "UFS scheduler persistence",
                state = ioDrift,
            )
        }
        Spacer(Modifier.height(10.dp))
    }
}
