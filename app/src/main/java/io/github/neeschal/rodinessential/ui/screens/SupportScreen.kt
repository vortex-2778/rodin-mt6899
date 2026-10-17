package io.github.neeschal.rodinessential.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowOutward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.neeschal.rodinessential.RodinReleaseIdentity
import io.github.neeschal.rodinessential.platform.RodinPlatform
import io.github.neeschal.rodinessential.ui.components.PressScale
import io.github.neeschal.rodinessential.ui.components.RodinAppEmblem
import io.github.neeschal.rodinessential.ui.components.RodinHaptics
import io.github.neeschal.rodinessential.ui.components.RodinVersionSecret
import io.github.neeschal.rodinessential.ui.components.SectionLabel
import io.github.neeschal.rodinessential.ui.components.StatusPill
import io.github.neeschal.rodinessential.ui.components.SurfaceCard

/** Community links, project identity and the shared disclaimer. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SupportScreen() {
    val colors = MaterialTheme.colorScheme
    val dark = colors.surface.luminance() < 0.5f
    val density = LocalDensity.current

    fun openLink(code: Int) {
        RodinHaptics.confirm()
        RodinPlatform.openSupportLink(code)
    }

    RodinScrollPage(
        topPadding = 16.dp,
        header = RodinPageHeader.Home(
            title = "Support",
            subtitle = "Community, source and system information",
        ),
    ) {
        Spacer(Modifier.height(14.dp))
        SurfaceCard(padding = PaddingValues(16.dp)) {
            Column {
                Row(verticalAlignment = Alignment.Top) {
                    RodinAppEmblem(size = 60.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Rodin Essential",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.W900,
                                letterSpacing = (-0.4).sp,
                            ),
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Built-in System Performance Studio",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.W500,
                                color = colors.onSurfaceVariant,
                            ),
                        )
                        Spacer(Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            RodinVersionSecret(version = "v${RodinReleaseIdentity.versionName}")
                            StatusPill(label = "Native Service", accent = Color(0xFF41C98A))
                            StatusPill(label = "Native AOT", accent = colors.secondary)
                            StatusPill(label = "16KB Page", accent = Color(0xFF41C98A))
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(
                                        SpanStyle(
                                            color = colors.onSurfaceVariant,
                                            fontWeight = FontWeight.W500,
                                        ),
                                    ) {
                                        append("Codename · ")
                                    }
                                    withStyle(
                                        SpanStyle(
                                            color = colors.primary,
                                            fontWeight = FontWeight.W800,
                                        ),
                                    ) {
                                        append(RodinReleaseIdentity.releaseCodename)
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    lineHeight = 12.sp * 1.4f,
                                ),
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Psst… try four taps on the version.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 10.sp,
                                lineHeight = 10.sp * 1.4f,
                                color = colors.onSurfaceVariant.copy(alpha = 0.7f),
                            ),
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            colors.surfaceContainer.copy(alpha = if (dark) 0.35f else 0.45f),
                            RoundedCornerShape(12.dp),
                        )
                        .border(
                            1.dp,
                            colors.outline.copy(alpha = 0.10f),
                            RoundedCornerShape(12.dp),
                        )
                        .padding(horizontal = 11.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Bolt,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Engineered exclusively for Dimensity 8400-Ultra (MT6899 / rodin)",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.W600,
                            color = colors.onSurface,
                        ),
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        SectionLabel("Official Community & Source")
        Spacer(Modifier.height(8.dp))
        CommunityCard(
            title = "GitHub Repository",
            subtitle = "Star, fork, and inspect the open-source code",
            badge = "NEESCHAL-3",
            accent = colors.primary,
            icon = { GithubIcon(color = colors.primary, size = 22.dp) },
            onTap = { openLink(0) },
        )
        Spacer(Modifier.height(8.dp))
        CommunityCard(
            title = "Telegram Community",
            subtitle = "Join device discussions, chat & get instant support",
            badge = "@PocoX7ProNepalChat",
            accent = Color(0xFF26A5E4),
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Send,
                    contentDescription = null,
                    tint = Color(0xFF26A5E4),
                    modifier = Modifier.size(20.dp),
                )
            },
            onTap = { openLink(1) },
        )
        Spacer(Modifier.height(16.dp))
        SectionLabel("System Integration Architecture")
        Spacer(Modifier.height(8.dp))
        DisclaimerCard()
        Spacer(Modifier.height(20.dp))
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(0.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                Text(
                    text = "Crafted for the community by ",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.W500,
                        color = colors.onSurfaceVariant,
                    ),
                )
                Text(
                    text = "NEESCHAL 🇳🇵",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.W800,
                        color = colors.primary,
                    ),
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Built-in System Component for Rodin ROMs • Zero Telemetry",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.W500,
                    color = colors.onSurfaceVariant.copy(alpha = 0.65f),
                ),
            )
        }
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun CommunityCard(
    title: String,
    subtitle: String,
    badge: String,
    accent: Color,
    icon: (@Composable () -> Unit),
    onTap: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val density = LocalDensity.current
    PressScale(onTap = onTap) {
        SurfaceCard(
            padding = PaddingValues(start = 13.dp, top = 11.dp, end = 13.dp, bottom = 11.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(44.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(accent.copy(alpha = 0.18f), accent.copy(alpha = 0.05f)),
                                start = Offset.Zero,
                                end = Offset.Infinite,
                            ),
                            RoundedCornerShape(14.dp),
                        )
                        .border(1.dp, accent.copy(alpha = 0.20f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    icon()
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.W700,
                            letterSpacing = (-0.2).sp,
                        ),
                    )
                    Spacer(Modifier.height(3.dp))
                    Box(
                        Modifier
                            .background(accent.copy(alpha = 0.10f), RoundedCornerShape(5.dp))
                            .border(1.dp, accent.copy(alpha = 0.16f), RoundedCornerShape(5.dp))
                            .padding(horizontal = 5.5.dp, vertical = 1.5.dp),
                    ) {
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.W700,
                                color = accent,
                            ),
                        )
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            lineHeight = 11.sp * 1.4f,
                            color = colors.onSurfaceVariant,
                        ),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .size(28.dp)
                        .background(accent.copy(alpha = 0.08f), RoundedCornerShape(9.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowOutward,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}
