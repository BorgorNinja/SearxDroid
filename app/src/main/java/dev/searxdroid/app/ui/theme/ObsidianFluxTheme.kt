package dev.searxdroid.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Obsidian Flux — Light palette ────────────────────────────────────────────
val Primary            = Color(0xFF041627)
val OnPrimary          = Color(0xFFFFFFFF)
val PrimaryContainer   = Color(0xFF1A2B3C)
val OnPrimaryContainer = Color(0xFF8192A7)
val Secondary          = Color(0xFF00677D)
val OnSecondary        = Color(0xFFFFFFFF)
val SecondaryContainer = Color(0xFF50D9FE)
val OnSecondaryContainer = Color(0xFF005C70)
val Tertiary           = Color(0xFF051629)
val OnTertiary         = Color(0xFFFFFFFF)
val TertiaryContainer  = Color(0xFF1B2B3F)
val OnTertiaryContainer= Color(0xFF8292AA)
val Error              = Color(0xFFBA1A1A)
val OnError            = Color(0xFFFFFFFF)
val ErrorContainer     = Color(0xFFFFDAD6)
val OnErrorContainer   = Color(0xFF93000A)
val Background         = Color(0xFFF8F9FA)
val OnBackground       = Color(0xFF191C1D)
val Surface            = Color(0xFFF8F9FA)
val OnSurface          = Color(0xFF191C1D)
val SurfaceVariant     = Color(0xFFE1E3E4)
val OnSurfaceVariant   = Color(0xFF44474C)
val Outline            = Color(0xFF74777D)
val OutlineVariant     = Color(0xFFC4C6CD)
val InverseSurface     = Color(0xFF2E3132)
val InverseOnSurface   = Color(0xFFF0F1F2)
val InversePrimary     = Color(0xFFB7C8DE)
val SurfaceContainerLowest  = Color(0xFFFFFFFF)
val SurfaceContainerLow     = Color(0xFFF3F4F5)
val SurfaceContainer        = Color(0xFFEDEEEF)
val SurfaceContainerHigh    = Color(0xFFE7E8E9)
val SurfaceContainerHighest = Color(0xFFE1E3E4)

// ── Obsidian Flux — Dark palette ─────────────────────────────────────────────
val DarkPrimary            = Color(0xFFB7C8DE)
val DarkOnPrimary          = Color(0xFF0B1D2D)
val DarkPrimaryContainer   = Color(0xFF253645)
val DarkOnPrimaryContainer = Color(0xFFD2E4FB)
val DarkSecondary          = Color(0xFF4CD6FB)
val DarkOnSecondary        = Color(0xFF001F27)
val DarkSecondaryContainer = Color(0xFF004E5F)
val DarkOnSecondaryContainer = Color(0xFFB3EBFF)
val DarkTertiary           = Color(0xFFB7C8E1)
val DarkOnTertiary         = Color(0xFF0B1C30)
val DarkTertiaryContainer  = Color(0xFF253446)
val DarkOnTertiaryContainer= Color(0xFFD3E4FE)
val DarkError              = Color(0xFFFFB4AB)
val DarkOnError            = Color(0xFF690005)
val DarkErrorContainer     = Color(0xFF93000A)
val DarkOnErrorContainer   = Color(0xFFFFDAD6)
val DarkBackground         = Color(0xFF0F172A)
val DarkOnBackground       = Color(0xFFE1E3E4)
val DarkSurface            = Color(0xFF0F172A)
val DarkOnSurface          = Color(0xFFE1E3E4)
val DarkSurfaceVariant     = Color(0xFF44474C)
val DarkOnSurfaceVariant   = Color(0xFFC4C6CD)
val DarkOutline            = Color(0xFF8E9197)
val DarkOutlineVariant     = Color(0xFF44474C)
val DarkInverseSurface     = Color(0xFFE1E3E4)
val DarkInverseOnSurface   = Color(0xFF2E3132)
val DarkInversePrimary     = Color(0xFF38485A)
val DarkSurfaceContainerLowest  = Color(0xFF0A1220)
val DarkSurfaceContainerLow     = Color(0xFF141F30)
val DarkSurfaceContainer        = Color(0xFF1E293B)
val DarkSurfaceContainerHigh    = Color(0xFF253347)
val DarkSurfaceContainerHighest = Color(0xFF2D3D52)

private val LightColorScheme = lightColorScheme(
    primary             = Primary,
    onPrimary           = OnPrimary,
    primaryContainer    = PrimaryContainer,
    onPrimaryContainer  = OnPrimaryContainer,
    secondary           = Secondary,
    onSecondary         = OnSecondary,
    secondaryContainer  = SecondaryContainer,
    onSecondaryContainer= OnSecondaryContainer,
    tertiary            = Tertiary,
    onTertiary          = OnTertiary,
    tertiaryContainer   = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,
    error               = Error,
    onError             = OnError,
    errorContainer      = ErrorContainer,
    onErrorContainer    = OnErrorContainer,
    background          = Background,
    onBackground        = OnBackground,
    surface             = Surface,
    onSurface           = OnSurface,
    surfaceVariant      = SurfaceVariant,
    onSurfaceVariant    = OnSurfaceVariant,
    outline             = Outline,
    outlineVariant      = OutlineVariant,
    inverseSurface      = InverseSurface,
    inverseOnSurface    = InverseOnSurface,
    inversePrimary      = InversePrimary,
    surfaceContainerLowest  = SurfaceContainerLowest,
    surfaceContainerLow     = SurfaceContainerLow,
    surfaceContainer        = SurfaceContainer,
    surfaceContainerHigh    = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,
)

private val DarkColorScheme = darkColorScheme(
    primary             = DarkPrimary,
    onPrimary           = DarkOnPrimary,
    primaryContainer    = DarkPrimaryContainer,
    onPrimaryContainer  = DarkOnPrimaryContainer,
    secondary           = DarkSecondary,
    onSecondary         = DarkOnSecondary,
    secondaryContainer  = DarkSecondaryContainer,
    onSecondaryContainer= DarkOnSecondaryContainer,
    tertiary            = DarkTertiary,
    onTertiary          = DarkOnTertiary,
    tertiaryContainer   = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    error               = DarkError,
    onError             = DarkOnError,
    errorContainer      = DarkErrorContainer,
    onErrorContainer    = DarkOnErrorContainer,
    background          = DarkBackground,
    onBackground        = DarkOnBackground,
    surface             = DarkSurface,
    onSurface           = DarkOnSurface,
    surfaceVariant      = DarkSurfaceVariant,
    onSurfaceVariant    = DarkOnSurfaceVariant,
    outline             = DarkOutline,
    outlineVariant      = DarkOutlineVariant,
    inverseSurface      = DarkInverseSurface,
    inverseOnSurface    = DarkInverseOnSurface,
    inversePrimary      = DarkInversePrimary,
    surfaceContainerLowest  = DarkSurfaceContainerLowest,
    surfaceContainerLow     = DarkSurfaceContainerLow,
    surfaceContainer        = DarkSurfaceContainer,
    surfaceContainerHigh    = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
)

@Composable
fun ObsidianFluxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography  = SearxTypography,
        content     = content,
    )
}
