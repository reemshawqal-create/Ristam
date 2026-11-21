package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.data.model.AccentPreset
import com.example.data.model.CornerDensity
import com.example.data.model.ThemePreset

@Immutable
data class PrismExtraColors(
    val subtleText: Color,
    val cardElevated: Color,
    val divider: Color,
    val isDark: Boolean,
    val cardRadius: Dp
)

val LocalPrismExtra = staticCompositionLocalOf {
    PrismExtraColors(
        subtleText = DarkOnSurfaceMuted,
        cardElevated = AmoledSurfaceElevated,
        divider = AmoledOutline,
        isDark = true,
        cardRadius = 16.dp
    )
}

@Composable
fun PrismTheme(
    themePreset: ThemePreset = ThemePreset.AMOLED_DARK,
    accentPreset: AccentPreset = AccentPreset.ROSE_VIOLET,
    cornerDensity: CornerDensity = CornerDensity.STANDARD,
    content: @Composable () -> Unit
) {
    val primaryAccent = when (accentPreset) {
        AccentPreset.ROSE_VIOLET -> PrismRose
        AccentPreset.AMBER_GOLD -> PrismAmber
        AccentPreset.EMERALD_MINT -> PrismEmerald
        AccentPreset.SKY_CYAN -> PrismCyan
    }
    val secondaryAccent = when (accentPreset) {
        AccentPreset.ROSE_VIOLET -> PrismViolet
        AccentPreset.AMBER_GOLD -> PrismRose
        AccentPreset.EMERALD_MINT -> PrismCyan
        AccentPreset.SKY_CYAN -> PrismViolet
    }

    val isDark = themePreset != ThemePreset.CLEAN_LIGHT
    val colorScheme = when (themePreset) {
        ThemePreset.AMOLED_DARK -> darkColorScheme(
            primary = primaryAccent,
            onPrimary = Color.White,
            secondary = secondaryAccent,
            onSecondary = Color.White,
            tertiary = PrismAmber,
            background = AmoledBackground,
            onBackground = DarkOnSurface,
            surface = AmoledSurface,
            onSurface = DarkOnSurface,
            surfaceVariant = AmoledSurfaceElevated,
            onSurfaceVariant = DarkOnSurfaceMuted,
            surfaceContainerHigh = AmoledSurfaceContainerHigh,
            outline = AmoledOutline,
            error = ErrorRed
        )
        ThemePreset.SLATE_DARK -> darkColorScheme(
            primary = primaryAccent,
            onPrimary = Color.White,
            secondary = secondaryAccent,
            onSecondary = Color.White,
            tertiary = PrismAmber,
            background = SlateBackground,
            onBackground = DarkOnSurface,
            surface = SlateSurface,
            onSurface = DarkOnSurface,
            surfaceVariant = SlateSurfaceElevated,
            onSurfaceVariant = DarkOnSurfaceMuted,
            surfaceContainerHigh = SlateSurfaceContainerHigh,
            outline = SlateOutline,
            error = ErrorRed
        )
        ThemePreset.MIDNIGHT_WARM -> darkColorScheme(
            primary = primaryAccent,
            onPrimary = Color.White,
            secondary = secondaryAccent,
            onSecondary = Color.White,
            tertiary = PrismAmber,
            background = MidnightWarmBackground,
            onBackground = DarkOnSurface,
            surface = MidnightWarmSurface,
            onSurface = DarkOnSurface,
            surfaceVariant = MidnightWarmSurfaceElevated,
            onSurfaceVariant = DarkOnSurfaceMuted,
            surfaceContainerHigh = MidnightWarmSurfaceContainerHigh,
            outline = MidnightWarmOutline,
            error = ErrorRed
        )
        ThemePreset.CLEAN_LIGHT -> lightColorScheme(
            primary = primaryAccent,
            onPrimary = Color.White,
            secondary = secondaryAccent,
            onSecondary = Color.White,
            tertiary = PrismAmber,
            background = CleanLightBackground,
            onBackground = LightOnSurface,
            surface = CleanLightSurface,
            onSurface = LightOnSurface,
            surfaceVariant = CleanLightSurfaceElevated,
            onSurfaceVariant = LightOnSurfaceMuted,
            surfaceContainerHigh = CleanLightSurfaceContainerHigh,
            outline = CleanLightOutline,
            error = ErrorRed
        )
    }

    val radius = when (cornerDensity) {
        CornerDensity.COMPACT -> 8.dp
        CornerDensity.STANDARD -> 16.dp
        CornerDensity.COMFORTABLE -> 24.dp
    }

    val shapes = Shapes(
        small = RoundedCornerShape(radius / 2),
        medium = RoundedCornerShape(radius),
        large = RoundedCornerShape(radius + 4.dp)
    )

    val extra = PrismExtraColors(
        subtleText = if (isDark) DarkOnSurfaceMuted else LightOnSurfaceMuted,
        cardElevated = colorScheme.surfaceVariant,
        divider = colorScheme.outline,
        isDark = isDark,
        cardRadius = radius
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !isDark
                controller.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    CompositionLocalProvider(
        LocalPrismExtra provides extra,
        LocalLayoutDirection provides LayoutDirection.Rtl
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = PrismTypography,
            shapes = shapes,
            content = content
        )
    }
}
