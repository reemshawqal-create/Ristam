package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Primary brand gradients (Prism Signature - Rose / Violet / Amber)
val PrismRose = Color(0xFFF43F5E)
val PrismViolet = Color(0xFF8B5CF6)
val PrismAmber = Color(0xFFF59E0B)
val PrismEmerald = Color(0xFF10B981)
val PrismCyan = Color(0xFF0EA5E9)

val PrismStoryBrush = Brush.linearGradient(
    colors = listOf(
        Color(0xFFF59E0B),
        Color(0xFFF43F5E),
        Color(0xFF8B5CF6)
    )
)

val PrismAccentGradient = Brush.horizontalGradient(
    colors = listOf(PrismRose, PrismViolet)
)

// AMOLED Pitch Dark Palette
val AmoledBackground = Color(0xFF000000)
val AmoledSurface = Color(0xFF0D0D12)
val AmoledSurfaceElevated = Color(0xFF16161F)
val AmoledSurfaceContainerHigh = Color(0xFF20202C)
val AmoledOutline = Color(0xFF2A2A38)

// Slate Dark Palette
val SlateBackground = Color(0xFF0E1118)
val SlateSurface = Color(0xFF151923)
val SlateSurfaceElevated = Color(0xFF1E2433)
val SlateSurfaceContainerHigh = Color(0xFF272F42)
val SlateOutline = Color(0xFF323C54)

// Warm Midnight Dark Palette
val MidnightWarmBackground = Color(0xFF100E0C)
val MidnightWarmSurface = Color(0xFF191512)
val MidnightWarmSurfaceElevated = Color(0xFF241E1A)
val MidnightWarmSurfaceContainerHigh = Color(0xFF302822)
val MidnightWarmOutline = Color(0xFF3F342C)

// Light Optional Palette
val CleanLightBackground = Color(0xFFFAFAFC)
val CleanLightSurface = Color(0xFFFFFFFF)
val CleanLightSurfaceElevated = Color(0xFFF2F3F7)
val CleanLightSurfaceContainerHigh = Color(0xFFE7E9F0)
val CleanLightOutline = Color(0xFFD5D8E2)

val DarkOnSurface = Color(0xFFF6F6FA)
val DarkOnSurfaceMuted = Color(0xFFA0A3B5)
val LightOnSurface = Color(0xFF12131A)
val LightOnSurfaceMuted = Color(0xFF5D6275)

val VerifiedBlue = Color(0xFF38BDF8)
val SuccessGreen = Color(0xFF22C55E)
val WarningAmber = Color(0xFFFBBF24)
val ErrorRed = Color(0xFFEF4444)
