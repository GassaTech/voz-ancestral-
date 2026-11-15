package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = DarkCanopyGreen,
    onPrimary = DarkOnCanopyGreen,
    primaryContainer = DarkCanopyContainer,
    onPrimaryContainer = DarkOnCanopyContainer,
    secondary = DarkUrucum,
    onSecondary = DarkOnUrucum,
    secondaryContainer = DarkUrucumContainer,
    onSecondaryContainer = DarkOnUrucumContainer,
    tertiary = DarkOchre,
    onTertiary = DarkOnOchre,
    tertiaryContainer = DarkOchreContainer,
    onTertiaryContainer = DarkOnOchreContainer,
    background = DarkForestBackground,
    onBackground = DarkOnForest,
    surface = DarkForestBackground,
    onSurface = DarkOnForest,
    surfaceVariant = DarkForestSurfaceVariant,
    onSurfaceVariant = DarkOnForestSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant
)

private val LightColorScheme = lightColorScheme(
    primary = CanopyGreen,
    onPrimary = OnCanopyGreen,
    primaryContainer = CanopyContainer,
    onPrimaryContainer = OnCanopyContainer,
    secondary = UrucumTerracotta,
    onSecondary = OnUrucumTerracotta,
    secondaryContainer = UrucumContainer,
    onSecondaryContainer = OnUrucumContainer,
    tertiary = OchreGold,
    onTertiary = OnOchreGold,
    tertiaryContainer = OchreContainer,
    onTertiaryContainer = OnOchreContainer,
    background = ParchmentBackground,
    onBackground = OnParchment,
    surface = ParchmentSurface,
    onSurface = OnParchment,
    surfaceVariant = ParchmentSurfaceVariant,
    onSurfaceVariant = OnParchmentSurfaceVariant,
    outline = ParchmentOutline,
    outlineVariant = ParchmentOutlineVariant
)

val ParintintinShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun ParintintinTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ParintintinTypography,
        shapes = ParintintinShapes,
        content = content
    )
}
