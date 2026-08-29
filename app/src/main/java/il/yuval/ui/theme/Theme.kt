package il.yuval.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

private val DarkColors = darkColorScheme(
    /*
     * צבע מותג ופעולות ראשיות.
     */
    primary = Color(0xFFB69CFF),
    onPrimary = Color(0xFF241052),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),

    /*
     * צבעי הדגשה כחולים המשמשים גם בגרדיאנט הרקע.
     */
    secondary = Color(0xFF67D9F3),
    onSecondary = Color(0xFF002F3A),
    secondaryContainer = Color(0xFF0A3657),
    onSecondaryContainer = Color(0xFFD5F2FF),

    tertiary = Color(0xFF7DD3FC),
    onTertiary = Color(0xFF003548),
    tertiaryContainer = Color(0xFF041E33),
    onTertiaryContainer = Color(0xFFD5F2FF),

    /*
     * משטחי האפליקציה במצב כהה.
     */
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
    /*
     * צבע מותג ופעולות ראשיות.
     */
    primary = Color(0xFF6750A4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8DEFF),
    onPrimaryContainer = Color(0xFF241052),

    /*
     * צבעי הרקע הכחולים של מסך הנוכחות המקורי.
     */
    secondary = Color(0xFF1F78B4),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB7DDF7),
    onSecondaryContainer = Color(0xFF062B4A),

    tertiary = Color(0xFF087E9B),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF062B4A),
    onTertiaryContainer = Color.White,

    /*
     * משטחי האפליקציה במצב בהיר.
     */
    background = Color(0xFFF8FBFF),
    onBackground = Color(0xFF172033),

    surface = Color.White,
    onSurface = Color(0xFF172033),

    surfaceVariant = Color(0xFFEAF4FF),
    onSurfaceVariant = Color(0xFF475467),

    outline = Color(0xFF7A8995),
    outlineVariant = Color(0xFFD5DEE5),

    error = Color(0xFFBA1A1A),
    onError = Color.White
)

/*
 * רקע המסכים הגלובלי של האפליקציה.
 *
 * הצבעים עצמם מוגדרים רק ב־DarkColors וב־LightColors.
 * כל מסך משתמש בפונקציה הזאת ואינו מחזיק צבעי רקע קשיחים.
 */
@Composable
fun kmiScreenBackgroundBrush(): Brush {
    /*
     * זיהוי המצב הפעיל בלבד מתוך ערכת הנושא שעוטפת
     * כרגע את המסך.
     */
    val isDarkMode =
        MaterialTheme.colorScheme.background.luminance() < 0.5f

    /*
     * הצבעים עצמם נלקחים ישירות ממקור האמת הגלובלי.
     *
     * כך ערכת נושא מקומית או ישנה שעוטפת מסך מסוים
     * לא יכולה לשנות את צבעי הרקע.
     */
    val backgroundColors =
        if (isDarkMode) {
            /*
             * אותו רקע בדיוק של מסך הבית במצב כהה.
             */
            listOf(
                DarkColors.background,
                DarkColors.surfaceVariant,
                DarkColors.primaryContainer,
                DarkColors.background
            )
        } else {
            /*
             * רקע הנוכחות המקורי במצב בהיר.
             */
            listOf(
                LightColors.background,
                LightColors.surfaceVariant,
                LightColors.secondaryContainer,
                LightColors.secondary,
                LightColors.tertiaryContainer
            )
        }

    return Brush.verticalGradient(
        colors = backgroundColors
    )
}

/*
 * רקע גלובלי לכותרות משנה עליונות.
 *
 * הצבעים מוגדרים כאן בלבד ולא בקובצי המסכים.
 */
@Composable
fun kmiSectionHeaderBrush(): Brush {
    return Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF062B4A),
            Color(0xFF0F5E9C),
            Color(0xFF062B4A)
        )
    )
}

/**
 * צבע הטקסט והאייקונים בכותרות המשנה העליונות.
 *
 * הרקע של הכותרת תמיד כהה ולכן הצבע נשאר לבן
 * גם במצב בהיר וגם במצב כהה.
 */
@Composable
fun kmiSectionHeaderContentColor(): Color {
    return Color.White
}

/**
 * גרדיאנט הגרניט הגלובלי לכפתורי הפעולה התחתונים.
 *
 * זהו הגרדיאנט הסגול־כחול־תכלת המקורי.
 */
@Composable
fun kmiGraniteActionBrush(): Brush {
    return Brush.linearGradient(
        colors = listOf(
            Color(0xFF7F00FF),
            Color(0xFF3F51B5),
            Color(0xFF03A9F4)
        )
    )
}

/**
 * צבע ההברקה שעוברת באנימציה מעל כפתור הגרניט.
 */
@Composable
fun kmiGraniteActionHighlightColor(): Color {
    return Color.White.copy(
        alpha = 0.45f
    )
}

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

