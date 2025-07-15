package com.sevenspan.calarm.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFBF1E2D),           // Vibrant red (from bell)
    onPrimary = Color.White,

    secondary = Color(0xFF275D8A),         // Deep navy blue (text and accents)
    onSecondary = Color.White,

    tertiary = Color(0xFFECBA58),          // Muted yellow-orange
    onTertiary = Color.Black,

    background = Color(0xFFFAF6ED),        // Warm cream background
    onBackground = Color(0xFF1C1B1F),

    surface = Color.White,
    onSurface = Color(0xFF1C1B1F),

    surfaceVariant = Color(0xFFF1EAE0),    // Slightly darker cream for cards
    onSurfaceVariant = Color(0xFF49454F),

    outline = Color(0xFF938F99),
)


/*private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,

//     Other default colors to override
    background = Color.White,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
)*/

@Composable
fun CalarmTheme(
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {

    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content,
    )
}
