package com.myledger.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = LedgerBlueLight, secondary = LedgerBlueLight, tertiary = LedgerBlueLight,
    background = Night, surface = Mist, onBackground = Color(0xFFE7F0EF), onSurface = Color(0xFFE7F0EF)
)

private val LightColorScheme = lightColorScheme(
    primary = LedgerBlue, secondary = LedgerBlue, tertiary = LedgerBlue,
    background = Sand, surface = Color.White, onBackground = Ink, onSurface = Ink
)

@Composable
fun MyLedgerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
