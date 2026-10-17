package io.github.neeschal.rodinessential.ui.screens

import com.google.android.material.color.utilities.DynamicScheme
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.MaterialDynamicColors
import com.google.android.material.color.utilities.SchemeExpressive
import com.google.android.material.color.utilities.SchemeFruitSalad
import com.google.android.material.color.utilities.SchemeMonochrome
import com.google.android.material.color.utilities.SchemeNeutral
import com.google.android.material.color.utilities.SchemeRainbow
import com.google.android.material.color.utilities.SchemeTonalSpot
import com.google.android.material.color.utilities.SchemeVibrant

/** Seed, style and brightness identifying one generated preview palette. */
data class RodinPaletteKey(val seed: Int, val style: Int, val dark: Boolean)

/**
 * Generates the same 12 scheme roles and 5 tonal families the Dart preview
 * isolate produced, using the material-color-utilities math that is bundled
 * with com.google.android.material.
 */
fun rodinGeneratePalette(seed: Int, style: Int, dark: Boolean): List<Int> {
    val source = Hct.fromInt(0xFF000000.toInt() or seed)
    val scheme: DynamicScheme = when (style) {
        0 -> SchemeTonalSpot(source, dark, 0.0)
        1 -> SchemeVibrant(source, dark, 0.0)
        2 -> SchemeExpressive(source, dark, 0.0)
        3 -> SchemeNeutral(source, dark, 0.0)
        4 -> SchemeRainbow(source, dark, 0.0)
        5 -> SchemeFruitSalad(source, dark, 0.0)
        6 -> SchemeMonochrome(source, dark, 0.0)
        else -> throw IllegalArgumentException("style")
    }
    val result = mutableListOf<Int>()
    result += MaterialDynamicColors.surfaceContainerLow().getArgb(scheme)
    result += MaterialDynamicColors.outlineVariant().getArgb(scheme)
    result += MaterialDynamicColors.onSurface().getArgb(scheme)
    result += MaterialDynamicColors.tertiaryContainer().getArgb(scheme)
    result += MaterialDynamicColors.onTertiaryContainer().getArgb(scheme)
    result += MaterialDynamicColors.primaryContainer().getArgb(scheme)
    result += MaterialDynamicColors.onPrimaryContainer().getArgb(scheme)
    result += MaterialDynamicColors.primary().getArgb(scheme)
    result += MaterialDynamicColors.onPrimary().getArgb(scheme)
    result += MaterialDynamicColors.surfaceContainerHighest().getArgb(scheme)
    result += MaterialDynamicColors.secondaryContainer().getArgb(scheme)
    result += MaterialDynamicColors.onSecondaryContainer().getArgb(scheme)
    for (family in listOf(
        scheme.primaryPalette,
        scheme.secondaryPalette,
        scheme.tertiaryPalette,
        scheme.neutralPalette,
        scheme.neutralVariantPalette,
    )) {
        family.asList.forEach { tone -> result += tone }
    }
    return result
}

/** Splits the 65 tonal values that follow the 12 scheme roles into families. */
fun rodinPreviewTonalFamilies(colors: List<Int>): List<List<Int>> {
    if (colors.size != 77) throw IllegalArgumentException("Invalid tonal palette")
    return List(5) { family ->
        colors.subList(12 + family * 13, 12 + (family + 1) * 13).toList()
    }
}
