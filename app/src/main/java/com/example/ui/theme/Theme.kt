package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = CyanPrimary,
    onPrimary = CyanOnPrimary,
    primaryContainer = CyanContainer,
    onPrimaryContainer = CyanOnContainer,
    secondary = IndigoSecondary,
    onSecondary = IndigoOnSecondary,
    secondaryContainer = IndigoContainer,
    onSecondaryContainer = IndigoOnContainer,
    tertiary = AmberTertiary,
    onTertiary = AmberOnTertiary,
    tertiaryContainer = AmberContainer,
    onTertiaryContainer = AmberOnContainer,
    background = ObsidianBackground,
    onBackground = TextPrimaryDark,
    surface = ObsidianSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianSurfaceVariant,
    surfaceContainerHigh = ObsidianSurfaceElevated,
    outline = ObsidianOutline,
    error = StatusOffline,
)

private val LightColorScheme = lightColorScheme(
    primary = TealPrimaryLight,
    onPrimary = TealOnPrimaryLight,
    primaryContainer = TealContainerLight,
    onPrimaryContainer = TealOnContainerLight,
    secondary = IndigoSecondaryLight,
    onSecondary = IndigoOnSecondaryLight,
    secondaryContainer = IndigoContainerLight,
    onSecondaryContainer = IndigoOnContainerLight,
    tertiary = StatusPaused,
    onTertiary = TealOnPrimaryLight,
    background = SlateLightBackground,
    onBackground = TextPrimaryLight,
    surface = SlateLightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = SlateLightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainerLow = SlateLightSurface,
    surfaceContainer = SlateLightSurfaceVariant,
    surfaceContainerHigh = SlateLightSurfaceVariant,
    outline = SlateLightOutline,
    error = StatusOffline,
)

@Composable
fun DayPulseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content,
    )
}
