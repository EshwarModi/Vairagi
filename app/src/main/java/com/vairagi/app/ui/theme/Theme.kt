package com.vairagi.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkSageColorScheme = darkColorScheme(
    primary = FreshSprout,
    onPrimary = ForestSage,
    primaryContainer = ForestSage,
    onPrimaryContainer = SageLight,
    secondary = WarmUmber,
    onSecondary = WarmParchment,
    tertiary = AmberClay,
    background = WarmCharcoal,
    onBackground = WarmParchment,
    surface = CharcoalSurface,
    onSurface = WarmParchment,
    error = MutedTerracotta
)

private val LightSageColorScheme = lightColorScheme(
    primary = LivingLeaf,
    onPrimary = WarmParchment,
    primaryContainer = SageLight,
    onPrimaryContainer = ForestSage,
    secondary = WarmUmber,
    onSecondary = WarmParchment,
    tertiary = AmberClay,
    background = WarmParchment,
    onBackground = WarmCharcoal,
    surface = ParchmentSurface,
    onSurface = WarmCharcoal,
    error = MutedTerracotta
)

@Composable
fun VairagiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Setting opt-in for Material You
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkSageColorScheme
        else -> LightSageColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = VairagiShapes,
        content = content
    )
}
