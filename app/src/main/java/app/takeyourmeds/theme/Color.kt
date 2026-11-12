package app.takeyourmeds.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// GENERATED — do not hand-edit individual roles.
//
// Both schemes are derived from four brand seeds by Material's tonal palette
// construction (CAM16/HCT): the seeds are the judgement, the ~36 roles are
// arithmetic. Hand-picking a role breaks the fixed tonal relationship it holds
// with the rest of its family, and the failure shows up as a contrast bug on one
// screen out of thirty rather than as a visible mistake here.
//
//   primary   #3E7D68  — brand: filled buttons, FAB, selected tab
//   secondary #6A7F76  — quiet containers: chips, tonal buttons, selected segment
//   tertiary  #A87A1E  — accents and caution markers, used sparingly
//   neutral   #E3E1DA  — seeds every surface, so it decides warm or cool overall
//
// Error stays M3 baseline (hue 25, chroma 84): a safety signal, not a brand choice.
//
// To change the palette, edit the seeds in scripts/gen_scheme.py and re-run it.
// Colours come from MaterialTheme, never literals, and dynamic colour defaults
// off (ADR-0006) so this file is what a user actually sees.

private val AppLightColors =
    lightColorScheme(
        primary = Color(0xFF296955),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFAEF0D6),
        onPrimaryContainer = Color(0xFF002117),
        inversePrimary = Color(0xFF93D4BB),
        secondary = Color(0xFF4E635A),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFD1E8DD),
        onSecondaryContainer = Color(0xFF0C1F19),
        tertiary = Color(0xFF7E5700),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFDEA7),
        onTertiaryContainer = Color(0xFF281900),
        error = Color(0xFFBA1B1B),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD4),
        onErrorContainer = Color(0xFF410001),
        background = Color(0xFFFBF9F1),
        onBackground = Color(0xFF1B1C17),
        surface = Color(0xFFFBF9F1),
        onSurface = Color(0xFF1B1C17),
        surfaceVariant = Color(0xFFE3E4D3),
        onSurfaceVariant = Color(0xFF46483C),
        surfaceDim = Color(0xFFDCDAD3),
        surfaceBright = Color(0xFFFBF9F1),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF6F4EC),
        surfaceContainer = Color(0xFFF0EEE6),
        surfaceContainerHigh = Color(0xFFEAE8E1),
        surfaceContainerHighest = Color(0xFFE4E2DB),
        inverseSurface = Color(0xFF31312D),
        inverseOnSurface = Color(0xFFF3F1E9),
        outline = Color(0xFF77786B),
        outlineVariant = Color(0xFFC7C8B8),
        scrim = Color(0xFF000000),
    )

private val AppDarkColors =
    darkColorScheme(
        primary = Color(0xFF93D4BB),
        onPrimary = Color(0xFF003829),
        primaryContainer = Color(0xFF06513E),
        onPrimaryContainer = Color(0xFFAEF0D6),
        inversePrimary = Color(0xFF296955),
        secondary = Color(0xFFB5CBC1),
        onSecondary = Color(0xFF21342D),
        secondaryContainer = Color(0xFF374B43),
        onSecondaryContainer = Color(0xFFD1E8DD),
        tertiary = Color(0xFFF5BE5D),
        onTertiary = Color(0xFF432C00),
        tertiaryContainer = Color(0xFF604100),
        onTertiaryContainer = Color(0xFFFFDEA7),
        error = Color(0xFFFFB4A9),
        onError = Color(0xFF680003),
        errorContainer = Color(0xFF930006),
        onErrorContainer = Color(0xFFFFDAD4),
        background = Color(0xFF131410),
        onBackground = Color(0xFFE4E2DB),
        surface = Color(0xFF131410),
        onSurface = Color(0xFFE4E2DB),
        surfaceVariant = Color(0xFF46483C),
        onSurfaceVariant = Color(0xFFC7C8B8),
        surfaceDim = Color(0xFF131410),
        surfaceBright = Color(0xFF393935),
        surfaceContainerLowest = Color(0xFF0E0F0B),
        surfaceContainerLow = Color(0xFF1B1C17),
        surfaceContainer = Color(0xFF1F201B),
        surfaceContainerHigh = Color(0xFF2A2A26),
        surfaceContainerHighest = Color(0xFF353530),
        inverseSurface = Color(0xFFE4E2DB),
        inverseOnSurface = Color(0xFF31312D),
        outline = Color(0xFF909283),
        outlineVariant = Color(0xFF46483C),
        scrim = Color(0xFF000000),
    )

internal val LightColors = AppLightColors
internal val DarkColors = AppDarkColors
