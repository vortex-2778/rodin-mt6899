package io.github.neeschal.rodinessential.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.SchemeTonalSpot
import com.google.android.material.color.utilities.TonalPalette

/**
 * The fixed brand palette from the Dart app; the seed palette is passed to
 * Material 3 exactly like `ColorScheme.fromSeed` received it.
 */
data class RodinPalette(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
)

val RodinDarkPalette = RodinPalette(
    background = Color(0xFF000000),
    surface = Color(0xFF000000),
    surfaceVariant = Color(0xFF080808),
    primary = Color(0xFF59BCFF),
    secondary = Color(0xFF4CDEB6),
    tertiary = Color(0xFFFFB95E),
    onSurface = Color(0xFFF4F7FB),
    onSurfaceVariant = Color(0xFFAEB8C5),
    outline = Color(0xFF1C1C1C),
)

val RodinLightPalette = RodinPalette(
    background = Color(0xFFF4F7FB),
    surface = Color(0xFFFCFDFF),
    surfaceVariant = Color(0xFFEEF3F8),
    primary = Color(0xFF087AC7),
    secondary = Color(0xFF078F79),
    tertiary = Color(0xFFB8730A),
    onSurface = Color(0xFF111925),
    onSurfaceVariant = Color(0xFF566477),
    outline = Color(0xFFD2DCE7),
)

private fun schemeTone(palette: TonalPalette, light: Int, darkTone: Int, dark: Boolean): Color =
    Color(palette.get((if (dark) darkTone else light).toDouble()))

/**
 * Recreates Flutter's Material 3 tonal-spot scheme for this seed and then
 * applies the same slot overrides the Dart app used.
 */
fun rodinColorScheme(dark: Boolean, palette: RodinPalette): ColorScheme {
    val dynamic = SchemeTonalSpot(Hct.fromInt(palette.primary.toArgb()), dark, 0.0)
    val p = dynamic.primaryPalette
    val s = dynamic.secondaryPalette
    val t = dynamic.tertiaryPalette
    val n = dynamic.neutralPalette
    val nv = dynamic.neutralVariantPalette
    val e = dynamic.errorPalette

    val generated = (if (dark) darkColorScheme() else lightColorScheme()).copy(
        primary = schemeTone(p, 40, 80, dark),
        onPrimary = schemeTone(p, 100, 20, dark),
        primaryContainer = schemeTone(p, 90, 30, dark),
        onPrimaryContainer = schemeTone(p, 10, 90, dark),
        inversePrimary = schemeTone(p, 80, 40, dark),
        secondary = schemeTone(s, 40, 80, dark),
        onSecondary = schemeTone(s, 100, 20, dark),
        secondaryContainer = schemeTone(s, 90, 30, dark),
        onSecondaryContainer = schemeTone(s, 10, 90, dark),
        tertiary = schemeTone(t, 40, 80, dark),
        onTertiary = schemeTone(t, 100, 20, dark),
        tertiaryContainer = schemeTone(t, 90, 30, dark),
        onTertiaryContainer = schemeTone(t, 10, 90, dark),
        error = schemeTone(e, 40, 80, dark),
        onError = schemeTone(e, 100, 20, dark),
        errorContainer = schemeTone(e, 90, 30, dark),
        onErrorContainer = schemeTone(e, 10, 90, dark),
        background = schemeTone(n, 99, 10, dark),
        onBackground = schemeTone(n, 10, 90, dark),
        surface = schemeTone(n, 99, 10, dark),
        onSurface = schemeTone(n, 10, 90, dark),
        surfaceVariant = schemeTone(nv, 90, 30, dark),
        onSurfaceVariant = schemeTone(nv, 30, 80, dark),
        outline = schemeTone(nv, 50, 60, dark),
        outlineVariant = schemeTone(nv, 80, 30, dark),
        inverseSurface = schemeTone(n, 20, 95, dark),
        inverseOnSurface = schemeTone(n, 95, 20, dark),
        scrim = schemeTone(n, 0, 0, dark),
        surfaceTint = schemeTone(p, 40, 80, dark),
        surfaceBright = schemeTone(n, 98, 24, dark),
        surfaceDim = schemeTone(n, 87, 6, dark),
        surfaceContainer = schemeTone(nv, 94, 12, dark),
        surfaceContainerLow = schemeTone(nv, 96, 10, dark),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerHigh = schemeTone(nv, 92, 17, dark),
        surfaceContainerHighest = schemeTone(nv, 90, 22, dark),
    )

    return generated.copy(
        primary = palette.primary,
        secondary = palette.secondary,
        tertiary = palette.tertiary,
        background = palette.background,
        surface = palette.surface,
        surfaceContainer = palette.surfaceVariant,
        surfaceContainerLow = palette.surfaceVariant,
        surfaceContainerHighest = palette.surfaceVariant,
        onSurface = palette.onSurface,
        onSurfaceVariant = palette.onSurfaceVariant,
        outline = palette.outline,
    )
}

@Composable
fun RodinEssentialTheme(
    config: RodinAppearanceConfig,
    content: @Composable () -> Unit,
) {
    val dark = when (config.themePreference) {
        RodinThemePreference.system -> isSystemInDarkTheme()
        RodinThemePreference.light -> false
        RodinThemePreference.dark -> true
    }
    val palette = if (dark) RodinDarkPalette else RodinLightPalette
    val scheme = remember(dark) { rodinColorScheme(dark, palette) }
    CompositionLocalProvider(LocalRodinAppearance provides config) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

private fun ColorScheme.isDarkTheme(): Boolean = surface.luminance() < 0.5f

/** Thin, resolved strokes: glass translucency must not erase its boundaries. */
object RodinSurfaceLines {
    fun border(colors: ColorScheme): Color = colors.onSurfaceVariant
        .copy(alpha = if (colors.isDarkTheme()) 0.10f else 0.04f)
        .compositeOver(colors.outline)

    fun inset(colors: ColorScheme): Color = if (colors.isDarkTheme()) {
        colors.onSurfaceVariant.copy(alpha = 0.08f).compositeOver(colors.outline)
    } else {
        colors.outline.copy(alpha = 0.90f).compositeOver(colors.surface)
    }

    fun divider(colors: ColorScheme): Color = if (colors.isDarkTheme()) {
        colors.onSurfaceVariant.copy(alpha = 0.06f).compositeOver(colors.outline)
    } else {
        colors.outline.copy(alpha = 0.86f).compositeOver(colors.surface)
    }
}

/** One value bubble treatment for both exact locks and dynamic ranges. */
@Composable
fun rodinSliderColors(accent: Color, outline: Color): SliderColors = SliderDefaults.colors(
    thumbColor = accent,
    activeTrackColor = accent,
    inactiveTrackColor = outline.copy(alpha = 0.50f),
    activeTickColor = Color.Transparent,
    inactiveTickColor = Color.Transparent,
)
