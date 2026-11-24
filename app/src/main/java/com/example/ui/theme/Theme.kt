package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val DorehamiDarkColorScheme = darkColorScheme(
    primary = RoyalGold,
    onPrimary = Color(0xFF1C1302),
    primaryContainer = Color(0xFF42310E),
    onPrimaryContainer = BrightGold,
    secondary = PersianTurquoise,
    onSecondary = Color(0xFF021F1D),
    secondaryContainer = DeepTeal,
    onSecondaryContainer = NeonCyan,
    tertiary = RubyCrimson,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF4D1121),
    onTertiaryContainer = Color(0xFFFFB4C2),
    background = MidnightBg,
    onBackground = TextPrimary,
    surface = VelvetNavy,
    onSurface = TextPrimary,
    surfaceVariant = ElevatedSlate,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = MidnightBg,
    surfaceContainerLow = VelvetNavy,
    surfaceContainer = ElevatedSlate,
    surfaceContainerHigh = SurfaceHighlight,
    outline = BorderGoldSubtle,
    outlineVariant = Color(0xFF26384E),
    error = RubyCrimson,
    onError = Color.White
)

private val DorehamiShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun DorehamiTheme(
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = DorehamiDarkColorScheme,
            typography = AppTypography,
            shapes = DorehamiShapes,
            content = content
        )
    }
}
