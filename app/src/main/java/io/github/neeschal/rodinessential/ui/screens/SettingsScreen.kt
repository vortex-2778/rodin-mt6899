package io.github.neeschal.rodinessential.ui.screens

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Square
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CropSquare
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.HorizontalDistribute
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.MotionPhotosOn
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.RoundedCorner
import androidx.compose.material.icons.rounded.ViewColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.neeschal.rodinessential.backend.RodinBackend
import io.github.neeschal.rodinessential.platform.RodinPlatform
import io.github.neeschal.rodinessential.ui.components.IconTile
import io.github.neeschal.rodinessential.ui.components.PressScale
import io.github.neeschal.rodinessential.ui.components.RodinHaptics
import io.github.neeschal.rodinessential.ui.components.RodinServiceConfirmation
import io.github.neeschal.rodinessential.ui.components.SectionLabel
import io.github.neeschal.rodinessential.ui.components.SurfaceCard
import io.github.neeschal.rodinessential.ui.components.rodinShadow
import io.github.neeschal.rodinessential.ui.motion.RodinDialog
import io.github.neeschal.rodinessential.ui.shell.RodinInteractionSettings
import io.github.neeschal.rodinessential.ui.theme.LocalRodinAppearance
import io.github.neeschal.rodinessential.ui.theme.RodinAppearanceConfig
import io.github.neeschal.rodinessential.ui.theme.RodinBackgroundStyle
import io.github.neeschal.rodinessential.ui.theme.RodinSurfaceLines
import io.github.neeschal.rodinessential.ui.theme.RodinThemePreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

private fun settingsCardStyleLabel(style: Int): String = when (style) {
    1 -> "Frosted Glass"
    2 -> "Neon Ambient Edge"
    3 -> "Minimal Slate"
    else -> "AMOLED Deep"
}

/** The private copy target for a picked background, mirroring the Dart path. */
private fun privateBackgroundPath(context: Context, selected: String): String {
    val lower = selected.lowercase()
    val extension = when {
        lower.endsWith(".png") -> ".png"
        lower.endsWith(".webp") -> ".webp"
        else -> ".jpg"
    }
    val nonce = System.nanoTime() / 1000L
    return File(context.filesDir, "rodin-custom-background-$nonce$extension").path
}

/**
 * Settings: service controls, live style preview, accent studio, surface
 * elements, theme, motion and the app background in the original order.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    config: RodinAppearanceConfig,
    onThemePreferenceChanged: (RodinThemePreference) -> Unit,
    onBackgroundStyleChanged: (RodinBackgroundStyle) -> Unit,
    onCustomBackgroundSelected: (String) -> Unit,
    onBlurChanged: (Double) -> Unit,
    onBlurChangeEnd: (Double) -> Unit,
    onHorizontalControlActive: (Boolean) -> Unit,
    onResetAppearance: () -> Unit,
    onAccentChanged: (Int) -> Unit,
    onCardRadiusChanged: (Double) -> Unit,
    onCardStyleChanged: (Int) -> Unit,
    onHeroGlowChanged: (Boolean) -> Unit,
    onCoreVisualizerStyleChanged: (Int) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var choosingPhoto by remember { mutableStateOf(false) }
    var photoMessage by remember { mutableStateOf<String?>(null) }
    var galleryVisible by remember { mutableStateOf(false) }
    var galleryPermission by remember { mutableIntStateOf(0) }

    suspend fun ensurePhotoPermission(): Int {
        var state = RodinPlatform.photoPermissionState()
        if (state > 0) return state
        if (!RodinPlatform.requestPhotoPermission()) return 0
        repeat(100) {
            delay(200)
            state = RodinPlatform.photoPermissionState()
            if (state > 0) return state
        }
        return 0
    }

    fun applySelected(selected: String) {
        scope.launch {
            try {
                val destination = privateBackgroundPath(context, selected)
                val target = File(destination)
                withContext(Dispatchers.IO) {
                    target.parentFile?.mkdirs()
                    File(selected).copyTo(target, overwrite = true)
                    // Decode the new unique file first so the shell swaps to an
                    // already-warm bitmap with no stale same-path cache.
                    BitmapFactory.decodeFile(destination)?.recycle()
                }
                onCustomBackgroundSelected(target.path)
                choosingPhoto = false
                photoMessage = "Custom background applied"
            } catch (_: Exception) {
                choosingPhoto = false
                photoMessage = "That image could not be applied."
            }
        }
    }

    fun choosePhoto() {
        if (choosingPhoto) return
        choosingPhoto = true
        photoMessage = null
        scope.launch {
            val permissionState = ensurePhotoPermission()
            if (permissionState <= 0) {
                choosingPhoto = false
                photoMessage = "Photo access was not granted. " +
                    "Rodin Essential keeps working " +
                    "normally without it."
                return@launch
            }
            galleryPermission = permissionState
            galleryVisible = true
        }
    }

    if (galleryVisible) {
        ModalBottomSheet(
            onDismissRequest = {
                galleryVisible = false
                choosingPhoto = false
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.Transparent,
            scrimColor = Color.Black.copy(alpha = 0.38f),
            dragHandle = null,
        ) {
            Box(Modifier.fillMaxHeight(0.91f)) {
                RodinGalleryBrowser(
                    permissionState = galleryPermission,
                    onCancel = {
                        galleryVisible = false
                        choosingPhoto = false
                    },
                    onSelected = { path ->
                        galleryVisible = false
                        applySelected(path)
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    RodinScrollPage(
        header = RodinPageHeader.Home(
            title = "Settings",
            subtitle = "Service controls, theme, and customization",
        ),
    ) {
        Spacer(Modifier.height(10.dp))
        ServiceSettingsCard(onResetAppearance = onResetAppearance)
        Spacer(Modifier.height(20.dp))

        SectionLabel("Live preview")
        Spacer(Modifier.height(8.dp))
        SurfaceCard {
            Column {
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .rodinShadow(
                                color = config.activeAccent.copy(alpha = 0.8f),
                                blurRadius = 8f,
                                cornerRadius = 4f,
                            )
                            .background(config.activeAccent, RoundedCornerShape(99.dp)),
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = "ACTIVE STYLE PREVIEW",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.W800,
                            letterSpacing = 1.1.sp,
                            color = config.activeAccent,
                        ),
                    )
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier
                            .background(
                                config.activeAccent.copy(alpha = 0.12f),
                                RoundedCornerShape(99.dp),
                            )
                            .border(
                                1.dp,
                                config.activeAccent.copy(alpha = 0.28f),
                                RoundedCornerShape(99.dp),
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text(
                            text = "${config.cardRadius.toInt()}px radius",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.W800,
                                color = config.activeAccent,
                            ),
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(
                        icon = Icons.Rounded.Palette,
                        accent = config.activeAccent,
                        size = 44.dp,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Rodin Essential Custom Surface",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.W800,
                            ),
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "${RodinAppearanceConfig.accentNames[config.accentIndex]} " +
                                "accent · ${settingsCardStyleLabel(config.cardStyle)}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.5.sp,
                                color = colors.onSurfaceVariant,
                            ),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        SectionLabel("Accent color")
        Spacer(Modifier.height(8.dp))
        SurfaceCard {
            Column {
                Text(
                    text = "Signature color palette",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.W700,
                    ),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Changes primary highlights, toggles, badges, and card accents.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.5.sp,
                        color = colors.onSurfaceVariant,
                    ),
                )
                Spacer(Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    for (i in RodinAppearanceConfig.accents.indices) {
                        val selected = config.accentIndex == i
                        val accent = RodinAppearanceConfig.accents[i]
                        PressScale(
                            onTap = {
                                onAccentChanged(i)
                                RodinHaptics.tap()
                            },
                        ) {
                            Row(
                                Modifier
                                    .background(
                                        if (selected) {
                                            accent.copy(alpha = 0.16f)
                                        } else {
                                            colors.surfaceContainer.copy(alpha = 0.8f)
                                        },
                                        RoundedCornerShape(14.dp),
                                    )
                                    .border(
                                        if (selected) 1.5.dp else 1.0.dp,
                                        if (selected) accent else colors.outline.copy(alpha = 0.22f),
                                        RoundedCornerShape(14.dp),
                                    )
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    Modifier
                                        .size(12.dp)
                                        .then(
                                            if (selected) {
                                                Modifier.rodinShadow(
                                                    color = accent.copy(alpha = 0.6f),
                                                    blurRadius = 6f,
                                                    cornerRadius = 6f,
                                                )
                                            } else {
                                                Modifier
                                            },
                                        )
                                        .background(accent, RoundedCornerShape(99.dp)),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = RodinAppearanceConfig.accentNames[i],
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontSize = 11.5.sp,
                                        fontWeight = if (selected) {
                                            FontWeight.W800
                                        } else {
                                            FontWeight.W600
                                        },
                                        color = if (selected) {
                                            colors.onSurface
                                        } else {
                                            colors.onSurfaceVariant
                                        },
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        SectionLabel("Card & surface elements")
        Spacer(Modifier.height(8.dp))
        SurfaceCard {
            Column {
                Text(
                    text = "Corner curvature",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.W700,
                    ),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Adjust how rounded UI cards, tiles, and panels appear.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.5.sp,
                        color = colors.onSurfaceVariant,
                    ),
                )
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    RodinAppearanceChoice(
                        label = "Sharp (10px)",
                        icon = Icons.Outlined.Square,
                        selected = (config.cardRadius - 10.0).let { it < 1.0 && it > -1.0 },
                        onTap = {
                            onCardRadiusChanged(10.0)
                            RodinHaptics.tap()
                        },
                    )
                    RodinAppearanceChoice(
                        label = "Classic (16px)",
                        icon = Icons.Rounded.RoundedCorner,
                        selected = (config.cardRadius - 16.0).let { it < 1.0 && it > -1.0 },
                        onTap = {
                            onCardRadiusChanged(16.0)
                            RodinHaptics.tap()
                        },
                    )
                    RodinAppearanceChoice(
                        label = "Squircle (22px)",
                        icon = Icons.Rounded.CropSquare,
                        selected = (config.cardRadius - 22.0).let { it < 1.0 && it > -1.0 },
                        onTap = {
                            onCardRadiusChanged(22.0)
                            RodinHaptics.tap()
                        },
                    )
                    RodinAppearanceChoice(
                        label = "Pill (28px)",
                        icon = Icons.Outlined.Circle,
                        selected = (config.cardRadius - 28.0).let { it < 1.0 && it > -1.0 },
                        onTap = {
                            onCardRadiusChanged(28.0)
                            RodinHaptics.tap()
                        },
                    )
                }
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 11.dp),
                    thickness = 0.8.dp,
                    color = RodinSurfaceLines.divider(colors),
                )
                Text(
                    text = "Surface visual style",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.W700,
                    ),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Select the material finish for cards across all screens.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.5.sp,
                        color = colors.onSurfaceVariant,
                    ),
                )
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    RodinAppearanceChoice(
                        label = "AMOLED Deep",
                        icon = Icons.Outlined.DarkMode,
                        selected = config.cardStyle == 0,
                        onTap = {
                            onCardStyleChanged(0)
                            RodinHaptics.tap()
                        },
                    )
                    RodinAppearanceChoice(
                        label = "Frosted Glass",
                        icon = Icons.Rounded.BlurOn,
                        selected = config.cardStyle == 1,
                        onTap = {
                            onCardStyleChanged(1)
                            RodinHaptics.tap()
                        },
                    )
                    RodinAppearanceChoice(
                        label = "Neon Edge",
                        icon = Icons.Outlined.AutoAwesome,
                        selected = config.cardStyle == 2,
                        onTap = {
                            onCardStyleChanged(2)
                            RodinHaptics.tap()
                        },
                    )
                    RodinAppearanceChoice(
                        label = "Minimal Slate",
                        icon = Icons.Outlined.Layers,
                        selected = config.cardStyle == 3,
                        onTap = {
                            onCardStyleChanged(3)
                            RodinHaptics.tap()
                        },
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        SectionLabel("Visual elements")
        Spacer(Modifier.height(8.dp))
        SurfaceCard {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Device pulse ambient halo",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.W700,
                            ),
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Display a glowing ambient halo behind the Home dashboard.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.2.sp,
                                color = colors.onSurfaceVariant,
                            ),
                        )
                    }
                    Switch(
                        checked = config.heroGlow,
                        onCheckedChange = { enabled ->
                            onHeroGlowChanged(enabled)
                            RodinHaptics.tap()
                        },
                    )
                }
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    thickness = 0.8.dp,
                    color = RodinSurfaceLines.divider(colors),
                )
                Text(
                    text = "CPU core visualizer style",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.W700,
                    ),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Choose how processor cores are displayed on the Home hero.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.2.sp,
                        color = colors.onSurfaceVariant,
                    ),
                )
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    RodinAppearanceChoice(
                        label = "Capsules",
                        icon = Icons.Rounded.ViewColumn,
                        selected = config.coreVisualizerStyle == 0,
                        onTap = {
                            onCoreVisualizerStyleChanged(0)
                            RodinHaptics.tap()
                        },
                    )
                    RodinAppearanceChoice(
                        label = "Neon Dots",
                        icon = Icons.Rounded.RadioButtonChecked,
                        selected = config.coreVisualizerStyle == 1,
                        onTap = {
                            onCoreVisualizerStyleChanged(1)
                            RodinHaptics.tap()
                        },
                    )
                    RodinAppearanceChoice(
                        label = "Energy Bar",
                        icon = Icons.Rounded.HorizontalDistribute,
                        selected = config.coreVisualizerStyle == 2,
                        onTap = {
                            onCoreVisualizerStyleChanged(2)
                            RodinHaptics.tap()
                        },
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        SectionLabel("Theme")
        Spacer(Modifier.height(9.dp))
        SurfaceCard {
            Column {
                Text(
                    text = "App theme",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.W700,
                    ),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Use Android automatically or force Rodin Essential to Light or pure AMOLED Black.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.5.sp,
                        color = colors.onSurfaceVariant,
                    ),
                )
                Spacer(Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    RodinAppearanceChoice(
                        label = "System",
                        icon = Icons.Rounded.BrightnessAuto,
                        selected = config.themePreference == RodinThemePreference.system,
                        onTap = { onThemePreferenceChanged(RodinThemePreference.system) },
                    )
                    RodinAppearanceChoice(
                        label = "Light",
                        icon = Icons.Rounded.LightMode,
                        selected = config.themePreference == RodinThemePreference.light,
                        onTap = { onThemePreferenceChanged(RodinThemePreference.light) },
                    )
                    RodinAppearanceChoice(
                        label = "AMOLED",
                        icon = Icons.Rounded.DarkMode,
                        selected = config.themePreference == RodinThemePreference.dark,
                        onTap = { onThemePreferenceChanged(RodinThemePreference.dark) },
                    )
                }
            }
        }
        Spacer(Modifier.height(13.dp))

        SectionLabel("Interaction")
        Spacer(Modifier.height(9.dp))
        InteractionCard(onHorizontalControlActive = onHorizontalControlActive)
        Spacer(Modifier.height(13.dp))

        SectionLabel("App background")
        Spacer(Modifier.height(9.dp))
        SurfaceCard {
            Column {
                Text(
                    text = "Background style",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.W700,
                    ),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Midnight and Aurora now stay behind the controls instead of competing with them.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.5.sp,
                        color = colors.onSurfaceVariant,
                    ),
                )
                Spacer(Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    RodinAppearanceChoice(
                        label = "System",
                        icon = Icons.Rounded.PhoneAndroid,
                        selected = config.backgroundStyle == RodinBackgroundStyle.system,
                        onTap = { onBackgroundStyleChanged(RodinBackgroundStyle.system) },
                    )
                    RodinAppearanceChoice(
                        label = "Midnight",
                        icon = Icons.Rounded.NightsStay,
                        selected = config.backgroundStyle == RodinBackgroundStyle.midnight,
                        onTap = { onBackgroundStyleChanged(RodinBackgroundStyle.midnight) },
                    )
                    RodinAppearanceChoice(
                        label = "Aurora",
                        icon = Icons.Rounded.AutoAwesome,
                        selected = config.backgroundStyle == RodinBackgroundStyle.aurora,
                        onTap = { onBackgroundStyleChanged(RodinBackgroundStyle.aurora) },
                    )
                    RodinAppearanceChoice(
                        label = if (choosingPhoto) "Opening…" else "Gallery",
                        icon = Icons.Rounded.PhotoLibrary,
                        selected = config.backgroundStyle == RodinBackgroundStyle.custom,
                        onTap = { choosePhoto() },
                    )
                }
                photoMessage?.let { message ->
                    Spacer(Modifier.height(9.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = colors.onSurfaceVariant,
                        ),
                    )
                }
            }
        }
        Spacer(Modifier.height(9.dp))
        SurfaceCard {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Background blur",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.W700,
                        ),
                    )
                    Text(
                        text = if (config.backgroundBlur < 0.5) {
                            "Off"
                        } else {
                            config.backgroundBlur.roundToInt().toString()
                        },
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.W800,
                            color = colors.primary,
                        ),
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Blur affects only the wallpaper. Cards, text and controls remain sharp.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.5.sp,
                        color = colors.onSurfaceVariant,
                    ),
                )
                BlurSlider(
                    value = config.backgroundBlur,
                    onHorizontalControlActive = onHorizontalControlActive,
                    onBlurChanged = onBlurChanged,
                    onBlurChangeEnd = onBlurChangeEnd,
                )
            }
        }
        if (config.customPath.isNotEmpty()) {
            Spacer(Modifier.height(9.dp))
            SurfaceCard {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = colors.secondary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "The selected image is copied into Rodin Essential " +
                                "private storage so it remains usable after photo " +
                                "permission changes.",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.5.sp,
                                lineHeight = 11.5.sp * 1.32f,
                                color = colors.onSurfaceVariant,
                            ),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(9.dp))
        PressScale(onTap = onResetAppearance) {
            SurfaceCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.RestartAlt,
                        contentDescription = null,
                        tint = colors.secondary,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Reset all appearance settings",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.W700,
                        ),
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

/** The service enable switch and the full reset, with confirmation. */
@Composable
private fun ServiceSettingsCard(onResetAppearance: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val appearance = LocalRodinAppearance.current
    val accent = appearance.activeAccent
    val snapshot = rememberRodinSnapshot()
    var pending by remember { mutableStateOf(false) }
    var resetRequest by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var confirming by remember { mutableStateOf<Boolean?>(null) }

    val actionState = RodinBackend.serviceActionState
    LaunchedEffect(snapshot) {
        if (pending && actionState != 1 && actionState != 0) {
            if (actionState == 2 && resetRequest) {
                onResetAppearance()
                RodinInteractionSettings.resetMotion()
                RodinInteractionSettings.setHaptics(true)
            }
            pending = false
            message = if (actionState == 2) {
                if (resetRequest) {
                    "All device controls reset."
                } else if (RodinBackend.serviceEnabled) {
                    "Rodin Essential enabled."
                } else {
                    "Rodin Essential disabled. Your preferences are saved."
                }
            } else {
                "Some settings could not be restored. Rodin Essential control " +
                    "remains paused. Retry or check backend diagnostics."
            }
        }
    }

    fun perform(reset: Boolean) {
        val accepted = if (reset) {
            RodinBackend.resetAllSettings()
        } else {
            RodinBackend.setServiceEnabled(false)
        }
        resetRequest = reset
        pending = accepted
        message = if (accepted) {
            null
        } else {
            "Backend unavailable or another operation is in progress."
        }
    }

    fun change(reset: Boolean, enable: Boolean) {
        if (pending) return
        RodinHaptics.tap()
        if (reset || !enable) {
            confirming = reset
            return
        }
        val accepted = RodinBackend.setServiceEnabled(enable)
        resetRequest = reset
        pending = accepted
        message = if (accepted) {
            null
        } else {
            "Backend unavailable or another operation is in progress."
        }
    }

    confirming?.let { reset ->
        RodinDialog(onDismissRequest = { confirming = null }) {
            RodinServiceConfirmation(
                reset = reset,
                cornerRadius = appearance.cardRadius,
                accent = accent,
                onConfirmHaptic = { RodinHaptics.confirm() },
                onCancelHaptic = { RodinHaptics.back() },
                onConfirm = {
                    confirming = null
                    perform(reset)
                },
                onCancel = { confirming = null },
                surface = { content ->
                    SurfaceCard(
                        padding = PaddingValues(22.dp),
                        accent = accent,
                    ) {
                        content()
                    }
                },
            )
        }
    }

    val connected = snapshot.ready
    val known = RodinBackend.extendedValue(116) >= 0
    val enabled = RodinBackend.serviceEnabled
    val busy = pending || actionState == 1
    val controllable = connected && known && !busy

    Column {
        SectionLabel("Service & reset")
        Spacer(Modifier.height(9.dp))
        SurfaceCard {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.PowerSettingsNew,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(25.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Enable Rodin Essential",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.W700,
                        ),
                    )
                    Switch(
                        checked = enabled,
                        enabled = controllable,
                        colors = SwitchDefaults.colors(checkedTrackColor = accent),
                        onCheckedChange = { value -> change(reset = false, enable = value) },
                    )
                }
                Spacer(Modifier.height(5.dp))
                Text(
                    text = when {
                        !connected -> "Connect to the backend to manage device controls."
                        !known -> "Update the backend to use service controls."
                        enabled -> "Device controls are available. Your saved selections are restored after reboot."
                        else -> "Background device control is paused. Open this page to enable it again."
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 12.sp,
                        lineHeight = 12.sp * 1.45f,
                        color = colors.onSurfaceVariant,
                    ),
                )
                Spacer(Modifier.height(15.dp))
                HorizontalDivider(thickness = 0.8.dp, color = RodinSurfaceLines.divider(colors))
                Spacer(Modifier.height(13.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Reset all device settings",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.W700,
                            ),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Restore captured originals and vendor defaults. Clear all saved device overrides.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 12.sp,
                                lineHeight = 12.sp * 1.45f,
                                color = colors.onSurfaceVariant,
                            ),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    FilledTonalIconButton(
                        onClick = { change(reset = true, enable = false) },
                        enabled = controllable,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = accent.copy(alpha = 0.12f),
                            contentColor = accent,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RestartAlt,
                            contentDescription = "Reset all device settings",
                        )
                    }
                }
                if (busy) {
                    Spacer(Modifier.height(14.dp))
                    LinearProgressIndicator(color = accent)
                    Spacer(Modifier.height(7.dp))
                    Text(
                        text = "Applying service change…",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 12.sp,
                            color = colors.onSurfaceVariant,
                        ),
                    )
                }
                message?.let { text ->
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 12.sp,
                            lineHeight = 12.sp * 1.45f,
                            color = colors.onSurfaceVariant,
                        ),
                    )
                }
            }
        }
    }
}

/** Motion speed, its travelling knob and the haptics switch. */
@Composable
private fun InteractionCard(onHorizontalControlActive: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    var dragging by remember { mutableStateOf(false) }
    val motionSpeed = RodinInteractionSettings.motionSpeed

    SurfaceCard {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(38.dp)
                        .background(colors.primary.copy(alpha = 0.12f), RoundedCornerShape(13.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MotionPhotosOn,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(21.dp),
                    )
                }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Motion feel",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.W700,
                        ),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = RodinInteractionSettings.motionLabel,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.W700,
                            color = colors.primary,
                        ),
                    )
                }
                Box(
                    Modifier
                        .background(colors.primary.copy(alpha = 0.10f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = String.format("%.2f", motionSpeed) + "×",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.W800,
                            color = colors.primary,
                        ),
                    )
                }
            }
            Spacer(Modifier.height(9.dp))
            Text(
                text = "One control now tunes navigation pace, easing, press depth, fades, and page movement across the whole app.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 11.5.sp,
                    lineHeight = 11.5.sp * 1.35f,
                    color = colors.onSurfaceVariant,
                ),
            )
            Spacer(Modifier.height(11.dp))
            MotionKnobTrack()
            Slider(
                value = motionSpeed.toFloat(),
                onValueChange = { value ->
                    if (!dragging) {
                        dragging = true
                        onHorizontalControlActive(true)
                        RodinHaptics.segment()
                    }
                    RodinInteractionSettings.previewMotionSpeed(value.toDouble())
                    RodinHaptics.frequentSegment()
                },
                onValueChangeFinished = {
                    dragging = false
                    RodinInteractionSettings.commitMotionSpeed(RodinInteractionSettings.motionSpeed)
                    onHorizontalControlActive(false)
                    RodinHaptics.confirm()
                },
                valueRange = 0.75f..1.35f,
                steps = 23,
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Relaxed",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 10.5.sp,
                        color = colors.onSurfaceVariant,
                    ),
                )
                Text(
                    text = "Balanced",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 10.5.sp,
                        color = colors.onSurfaceVariant,
                    ),
                )
                Text(
                    text = "Direct",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 10.5.sp,
                        color = colors.onSurfaceVariant,
                    ),
                )
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    onClick = {
                        RodinInteractionSettings.resetMotion()
                        RodinHaptics.confirm()
                    },
                ) {
                    Icon(
                        imageVector = Icons.Rounded.RestartAlt,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Reset to balanced")
                }
            }
            Spacer(Modifier.height(2.dp))
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 9.dp),
                thickness = 0.8.dp,
                color = RodinSurfaceLines.divider(colors),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Haptic feedback",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.W700,
                        ),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Android semantic feedback for taps, toggles, choices and sliders.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.2.sp,
                            color = colors.onSurfaceVariant,
                        ),
                    )
                }
                Switch(
                    checked = RodinInteractionSettings.hapticsEnabled,
                    onCheckedChange = { enabled ->
                        if (!enabled) {
                            RodinHaptics.toggle(false)
                        }
                        RodinInteractionSettings.setHaptics(enabled)
                        if (enabled) {
                            RodinHaptics.confirm()
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun MotionKnobTrack() {
    val colors = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val progress = RodinInteractionSettings.motionProgress
    val bias by animateFloatAsState(
        targetValue = (-1f + progress.toFloat() * 2f),
        animationSpec = tween(
            RodinInteractionSettings.motionDuration(300),
            easing = RodinInteractionSettings.transitionCurve,
        ),
        label = "motionBias",
    )
    Box(
        Modifier
            .fillMaxWidth()
            .height(42.dp)
            .background(
                colors.surfaceContainerHighest.copy(alpha = 0.45f),
                RoundedCornerShape(14.dp),
            )
            .border(1.dp, colors.outline.copy(alpha = 0.28f), RoundedCornerShape(14.dp))
            .padding(horizontal = 7.dp),
    ) {
        Box(
            Modifier
                .align(BiasAlignment(horizontalBias = bias, verticalBias = 0f))
                .size(28.dp)
                .rodinShadow(
                    color = colors.primary.copy(alpha = 0.28f),
                    blurRadius = 12f,
                    spreadRadius = -2f,
                    cornerRadius = with(density) { 10.dp.toPx() },
                )
                .background(colors.primary, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.ArrowForward,
                contentDescription = null,
                tint = colors.onPrimary,
                modifier = Modifier.size(17.dp),
            )
        }
    }
}

@Composable
private fun BlurSlider(
    value: Double,
    onHorizontalControlActive: (Boolean) -> Unit,
    onBlurChanged: (Double) -> Unit,
    onBlurChangeEnd: (Double) -> Unit,
) {
    var dragging by remember { mutableStateOf(false) }
    Slider(
        value = value.coerceIn(0.0, 28.0).toFloat(),
        onValueChange = { next ->
            if (!dragging) {
                dragging = true
                onHorizontalControlActive(true)
                RodinHaptics.segment()
            }
            onBlurChanged(next.toDouble())
            RodinHaptics.frequentSegment()
        },
        onValueChangeFinished = {
            dragging = false
            onBlurChangeEnd(value.coerceIn(0.0, 28.0))
            onHorizontalControlActive(false)
            RodinHaptics.confirm()
        },
        valueRange = 0f..28f,
        steps = 27,
    )
}
