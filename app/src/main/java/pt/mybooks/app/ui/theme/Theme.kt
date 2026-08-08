package pt.mybooks.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Forest,
    onPrimary = Color.White,
    primaryContainer = MintPale,
    onPrimaryContainer = ForestDark,
    secondary = Amber,
    onSecondary = Ink,
    secondaryContainer = AmberPale,
    onSecondaryContainer = Ink,
    tertiary = Coral,
    onTertiary = Color.White,
    tertiaryContainer = CoralPale,
    onTertiaryContainer = Color(0xFF5A160D),
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE6EAE7),
    onSurfaceVariant = Color(0xFF424946),
    outline = Color(0xFF737A77),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8BD2C4),
    onPrimary = Color(0xFF003730),
    primaryContainer = Color(0xFF005048),
    onPrimaryContainer = MintPale,
    secondary = Color(0xFFFFCA65),
    onSecondary = Color(0xFF422C00),
    secondaryContainer = Color(0xFF5F4100),
    onSecondaryContainer = AmberPale,
    tertiary = Color(0xFFFFB4A7),
    onTertiary = Color(0xFF6A1B10),
    tertiaryContainer = Color(0xFF8B3023),
    onTertiaryContainer = CoralPale,
    background = PaperDark,
    onBackground = Color(0xFFE0E4E1),
    surface = PaperDark,
    onSurface = Color(0xFFE0E4E1),
    surfaceVariant = Color(0xFF3F4946),
    onSurfaceVariant = Color(0xFFBEC9C5),
)

@Composable
fun MyBooksTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        content = content,
    )
}
