package il.yuval.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB69CFF),
    onPrimary = Color(0xFF241052),

    secondary = Color(0xFF67D9F3),
    onSecondary = Color(0xFF002F3A),

    tertiary = Color(0xFFFFB2C8),
    onTertiary = Color(0xFF5D1130),

    background = Color(0xFF06111C),
    onBackground = Color(0xFFF1F5F9),

    surface = Color(0xFF101B27),
    onSurface = Color(0xFFF1F5F9),

    surfaceVariant = Color(0xFF192A38),
    onSurfaceVariant = Color(0xFFBCC9D4),

    outline = Color(0xFF718392),
    outlineVariant = Color(0xFF344957),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color.White,

    secondary = Color(0xFF087E9B),
    onSecondary = Color.White,

    tertiary = Color(0xFF9B405F),
    onTertiary = Color.White,

    background = Color(0xFFF8FBFF),
    onBackground = Color(0xFF172033),

    surface = Color.White,
    onSurface = Color(0xFF172033),

    surfaceVariant = Color(0xFFEDF4F8),
    onSurfaceVariant = Color(0xFF475467),

    outline = Color(0xFF7A8995),
    outlineVariant = Color(0xFFD5DEE5),

    error = Color(0xFFBA1A1A),
    onError = Color.White
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme =
            if (darkTheme) {
                DarkColors
            } else {
                LightColors
            },
        content = content
    )
}
