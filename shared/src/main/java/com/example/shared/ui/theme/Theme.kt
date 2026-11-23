package com.example.shared.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val BeamDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = VoidNavy,
    primaryContainer = CyanContainer,
    onPrimaryContainer = ElectricCyan,
    secondary = MintPulse,
    onSecondary = VoidNavy,
    secondaryContainer = MintContainer,
    onSecondaryContainer = MintPulse,
    tertiary = AmberSpeed,
    onTertiary = VoidNavy,
    background = VoidNavy,
    onBackground = TextPrimary,
    surface = DeepSurface,
    onSurface = TextPrimary,
    surfaceVariant = ElevatedCard,
    onSurfaceVariant = TextSecondary,
    outline = CardBorder,
    error = CoralAlert,
    onError = Color.White
)

private val BeamShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun BeamTvTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BeamDarkColorScheme,
        typography = BeamTypography,
        shapes = BeamShapes,
        content = content
    )
}
