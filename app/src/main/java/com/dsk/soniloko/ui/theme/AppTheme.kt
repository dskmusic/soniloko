package com.dsk.soniloko.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily

/** Built into every Android system image (part of the Roboto family) — no asset or network needed. */
private val NarrowFontFamily = FontFamily(Font(DeviceFontFamilyName("sans-serif-condensed")))

private val baseTypography = Typography()

val SoniLokoTypography = Typography(
    displayLarge = baseTypography.displayLarge.copy(fontFamily = NarrowFontFamily),
    displayMedium = baseTypography.displayMedium.copy(fontFamily = NarrowFontFamily),
    displaySmall = baseTypography.displaySmall.copy(fontFamily = NarrowFontFamily),
    headlineLarge = baseTypography.headlineLarge.copy(fontFamily = NarrowFontFamily),
    headlineMedium = baseTypography.headlineMedium.copy(fontFamily = NarrowFontFamily),
    headlineSmall = baseTypography.headlineSmall.copy(fontFamily = NarrowFontFamily),
    titleLarge = baseTypography.titleLarge.copy(fontFamily = NarrowFontFamily),
    titleMedium = baseTypography.titleMedium.copy(fontFamily = NarrowFontFamily),
    titleSmall = baseTypography.titleSmall.copy(fontFamily = NarrowFontFamily),
    bodyLarge = baseTypography.bodyLarge.copy(fontFamily = NarrowFontFamily),
    bodyMedium = baseTypography.bodyMedium.copy(fontFamily = NarrowFontFamily),
    bodySmall = baseTypography.bodySmall.copy(fontFamily = NarrowFontFamily),
    labelLarge = baseTypography.labelLarge.copy(fontFamily = NarrowFontFamily),
    labelMedium = baseTypography.labelMedium.copy(fontFamily = NarrowFontFamily),
    labelSmall = baseTypography.labelSmall.copy(fontFamily = NarrowFontFamily)
)

enum class AppThemeOption(
    val id: String,
    val primary: Color,
    val background: Color,
    val surface: Color
) {
    CLASSIC_RED("classic_red", Color(0xFFE53935), Color(0xFF130404), Color(0xFF1E0808)),
    OCEAN_BLUE("ocean_blue", Color(0xFF2196F3), Color(0xFF04090F), Color(0xFF081420)),
    FOREST_GREEN("forest_green", Color(0xFF43A047), Color(0xFF040A04), Color(0xFF081408)),
    MIDNIGHT_BLACK("midnight_black", Color(0xFF9E9E9E), Color(0xFF060606), Color(0xFF141414)),
    SUNSET_ORANGE("sunset_orange", Color(0xFFFF8F00), Color(0xFF140A02), Color(0xFF1F1206)),
    ROYAL_PURPLE("royal_purple", Color(0xFF8E24AA), Color(0xFF0D0512), Color(0xFF190919));

    companion object {
        fun fromId(id: String): AppThemeOption = entries.firstOrNull { it.id == id } ?: CLASSIC_RED
    }
}

/** All themes are dark: background is a near-black tint of the machine's own color. */
@Composable
fun SoniLokoTheme(themeOption: AppThemeOption, content: @Composable () -> Unit) {
    val colorScheme = darkColorScheme(
        primary = themeOption.primary,
        onPrimary = Color.White,
        secondary = themeOption.primary,
        background = themeOption.background,
        onBackground = Color.White,
        surface = themeOption.surface,
        onSurface = Color.White,
        surfaceVariant = themeOption.surface,
        onSurfaceVariant = Color.White
    )
    MaterialTheme(colorScheme = colorScheme, typography = SoniLokoTypography, content = content)
}
