package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val HirafiDarkColorScheme = darkColorScheme(
    primary = WepeEmerald,
    onPrimary = Color.White,
    primaryContainer = ElectricLimeDarkContainer,
    onPrimaryContainer = WepeEmeraldSoft,
    secondary = VerifiedEmerald,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF0A3823),
    onSecondaryContainer = VerifiedEmeraldSoft,
    tertiary = InfoCobalt,
    onTertiary = Color.White,
    background = ObsidianBlack,
    onBackground = TextPrimaryDark,
    surface = CharcoalSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = CharcoalElevated,
    onSurfaceVariant = TextSecondaryDark,
    outline = SlateBorder,
    error = UrgentCoral,
    onError = Color.White
)

private val HirafiLightColorScheme = lightColorScheme(
    primary = WepeEmerald,
    onPrimary = Color.White,
    primaryContainer = WepeEmeraldSoft,
    onPrimaryContainer = WepeEmeraldDark,
    secondary = ObsidianBlack,
    onSecondary = Color.White,
    secondaryContainer = WepeSoftCard,
    onSecondaryContainer = TextPrimaryLight,
    tertiary = WepeEmerald,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = WepeSoftCard,
    onSurfaceVariant = TextSecondaryLight,
    outline = WepeHairline,
    error = UrgentCoral,
    onError = Color.White
)

val HirafiShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun HirafiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) HirafiDarkColorScheme else HirafiLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = HirafiTypography,
        shapes = HirafiShapes,
        content = content
    )
}
