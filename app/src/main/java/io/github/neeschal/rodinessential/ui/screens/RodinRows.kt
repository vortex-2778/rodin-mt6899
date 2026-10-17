package io.github.neeschal.rodinessential.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.neeschal.rodinessential.ui.components.RodinHaptics
import io.github.neeschal.rodinessential.ui.components.StatusPill

/** One per-core row with its live clock and hotplug switch. */
@Composable
fun CpuStatusRow(
    cpu: Int,
    online: Boolean,
    frequency: String,
    toggleEnabled: Boolean,
    onChanged: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    accent: Color = Color(0xFF67C2FF),
    clusterTag: String? = null,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(36.dp)
                .background(
                    accent.copy(alpha = if (online) 0.14f else 0.06f),
                    RoundedCornerShape(11.dp),
                )
                .border(
                    1.dp,
                    accent.copy(alpha = if (online) 0.28f else 0.12f),
                    RoundedCornerShape(11.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "$cpu",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.W900,
                    color = if (online) accent else Color(0xFF9E9E9E),
                ),
            )
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "CPU$cpu",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 13.8.sp,
                        fontWeight = FontWeight.W800,
                    ),
                )
                if (clusterTag != null) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        Modifier
                            .background(accent.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = clusterTag,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.W800,
                                letterSpacing = 0.4.sp,
                                color = accent,
                            ),
                        )
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (cpu == 0) "$frequency · pinned master" else frequency,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.2.sp,
                    color = colors.onSurfaceVariant,
                ),
            )
        }
        Spacer(Modifier.width(8.dp))
        StatusPill(
            label = if (online) "ONLINE" else "OFFLINE",
            accent = if (online) Color(0xFF41C98A) else Color(0xFFFF7A7A),
        )
        Spacer(Modifier.width(4.dp))
        Switch(
            checked = online,
            onCheckedChange = if (toggleEnabled) {
                RodinHaptics.toggleCallback(onChanged)
            } else {
                null
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = accent,
                checkedTrackColor = accent.copy(alpha = 0.35f),
            ),
        )
    }
}

/** The icon, label and detail line of every diagnostics list. */
@Composable
fun DiagnosticRow(
    label: String,
    good: Boolean,
    detail: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier,
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = if (good) Icons.Rounded.CheckCircle else Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = if (good) Color(0xFF41C98A) else Color(0xFFFFB84D),
            modifier = Modifier
                .padding(top = 2.dp)
                .size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 12.sp,
                    lineHeight = 12.sp * 1.35f,
                    fontWeight = FontWeight.W700,
                ),
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.5.sp,
                    lineHeight = 11.5.sp * 1.45f,
                    color = colors.onSurfaceVariant,
                ),
            )
        }
    }
}

/** The verified / drift / unset triple of every persisted selection. */
@Composable
fun PersistenceStatusRow(label: String, state: Int, modifier: Modifier = Modifier) {
    DiagnosticRow(
        label = label,
        good = state == 0,
        detail = when (state) {
            0 -> "Verified"
            1 -> "Vendor drift detected · guard will reassert"
            else -> "No persisted selection yet"
        },
        modifier = modifier,
    )
}
