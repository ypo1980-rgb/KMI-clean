package il.yuval.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import il.kmi.shared.domain.Belt

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
 * רקע גלובלי ייעודי לסרגל הצד.
 *
 * הרקע מובחן מרקע המסכים הרגיל, אך נשען על צבעי
 * ערכת הנושא ואינו מגדיר צבעים מקומיים בסרגל.
 */
@Composable
fun kmiDrawerBackgroundBrush(): Brush {
    val isDarkMode =
        MaterialTheme.colorScheme.background
            .luminance() < 0.5f

    val graniteColors =
        if (isDarkMode) {
            listOf(
                Color(0xFF071A2A),
                Color(0xFF163B55),
                Color(0xFF344F63),
                Color(0xFF102F47),
                Color(0xFF061521)
            )
        } else {
            listOf(
                Color(0xFFF1F4F7),
                Color(0xFFB8C4CF),
                Color(0xFFDDE3E9),
                Color(0xFF9EAFBD),
                Color(0xFFE9EEF2)
            )
        }

    return Brush.linearGradient(
        colors = graniteColors
    )
}

/**
 * סוגי כרטיסי התפקיד בסרגל הצד.
 */
enum class KmiDrawerRoleType {
    TRAINEE,
    COACH,
    MANAGER
}

data class KmiDrawerRoleColors(
    val background: Brush,
    val content: Color,
    val border: Color
)

/**
 * צבעים קבועים לכרטיסי התפקיד בסרגל הצד.
 *
 * הצבעים אינם משתנים בין Light ו־Dark ואינם תלויים
 * בחגורה או בערכת הצבעים הפעילה.
 */
fun kmiDrawerRoleColors(
    type: KmiDrawerRoleType
): KmiDrawerRoleColors {
    return when (type) {
        KmiDrawerRoleType.TRAINEE ->
            KmiDrawerRoleColors(
                background = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFDCEEFF),
                        Color(0xFF9AC7EF),
                        Color(0xFF5A9FD6)
                    )
                ),
                content = Color(0xFF0B2942),
                border = Color(0xFF347FB8)
            )

        KmiDrawerRoleType.COACH ->
            KmiDrawerRoleColors(
                background = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE8DDF7),
                        Color(0xFFC6B4E1),
                        Color(0xFFA58FC8)
                    )
                ),
                content = Color(0xFF302044),
                border = Color(0xFF7D61A5)
            )

        KmiDrawerRoleType.MANAGER ->
            KmiDrawerRoleColors(
                background = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFD9F3E4),
                        Color(0xFF8FD3AC),
                        Color(0xFF48A877)
                    )
                ),
                content = Color(0xFF123B27),
                border = Color(0xFF27845A)
            )
    }
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
 * צבע הצלחה גלובלי.
 *
 * משמש לסטטוס פעיל, אישור והצלחה ואינו תלוי
 * בצבע tertiary שמשמש באפליקציה להדגשה תכלת.
 */
@Composable
fun kmiSuccessColor(): Color {
    val isDarkMode =
        MaterialTheme.colorScheme.background
            .luminance() < 0.5f

    return if (isDarkMode) {
        Color(0xFF4ADE80)
    } else {
        Color(0xFF15803D)
    }
}

/**
 * רקע גלובלי לרכיבי הצלחה.
 */
@Composable
fun kmiSuccessContainerColor(): Color {
    val isDarkMode =
        MaterialTheme.colorScheme.background
            .luminance() < 0.5f

    return if (isDarkMode) {
        Color(0xFF123D27)
    } else {
        Color(0xFFDCFCE7)
    }
}

/**
 * צבע תוכן גלובלי מעל רקע הצלחה.
 */
@Composable
fun kmiOnSuccessContainerColor(): Color {
    val isDarkMode =
        MaterialTheme.colorScheme.background
            .luminance() < 0.5f

    return if (isDarkMode) {
        Color(0xFFBBF7D0)
    } else {
        Color(0xFF166534)
    }
}

/**
 * צבע אזהרה גלובלי.
 *
 * משמש לספירה לאחור, התראה ומצב שדורש תשומת לב.
 */
@Composable
fun kmiWarningColor(): Color {
    val isDarkMode =
        MaterialTheme.colorScheme.background
            .luminance() < 0.5f

    return if (isDarkMode) {
        Color(0xFFFDBA74)
    } else {
        Color(0xFFC2410C)
    }
}

/**
 * רקע גלובלי לרכיבי אזהרה.
 */
@Composable
fun kmiWarningContainerColor(): Color {
    val isDarkMode =
        MaterialTheme.colorScheme.background
            .luminance() < 0.5f

    return if (isDarkMode) {
        Color(0xFF431407)
    } else {
        Color(0xFFFFEDD5)
    }
}

/**
 * צבע תוכן גלובלי מעל רקע אזהרה.
 */
@Composable
fun kmiOnWarningContainerColor(): Color {
    val isDarkMode =
        MaterialTheme.colorScheme.background
            .luminance() < 0.5f

    return if (isDarkMode) {
        Color(0xFFFFEDD5)
    } else {
        Color(0xFF7C2D12)
    }
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

/**
 * מקור האמת הגלובלי לצבעי החגורות.
 */
fun kmiBeltColor(
    belt: Belt
): Color {
    return when (belt) {
        Belt.WHITE -> Color(0xFFF5F5F5)
        Belt.YELLOW -> Color(0xFFFFD54F)
        Belt.ORANGE -> Color(0xFFFF9800)
        Belt.GREEN -> Color(0xFF2E7D32)
        Belt.BLUE -> Color(0xFF1565C0)
        Belt.BROWN -> Color(0xFF6D4C41)
        Belt.BLACK -> Color(0xFF111111)
    }
}

enum class KmiQuickActionType {
    SEARCH,
    HOME,
    SETTINGS,
    STATISTICS,
    ASSISTANT,
    GUIDE,
    SHARE
}

data class KmiQuickActionColors(
    val background: Color,
    val content: Color
)

/**
 * צבעי הפעולות בסרגל האייקונים.
 *
 * לכל פעולה נשמר צבע מזוהה ועקבי ב־Light וב־Dark.
 */
@Composable
fun kmiQuickActionColors(
    type: KmiQuickActionType
): KmiQuickActionColors {
    val isDarkMode =
        MaterialTheme.colorScheme.background
            .luminance() < 0.5f

    return if (isDarkMode) {
        when (type) {
            KmiQuickActionType.SEARCH ->
                KmiQuickActionColors(
                    background = Color(0xFF312E81),
                    content = Color(0xFFC4B5FD)
                )

            KmiQuickActionType.HOME ->
                KmiQuickActionColors(
                    background = Color(0xFF172554),
                    content = Color(0xFF93C5FD)
                )

            KmiQuickActionType.SETTINGS ->
                KmiQuickActionColors(
                    background = Color(0xFF4C0519),
                    content = Color(0xFFFDA4AF)
                )

            KmiQuickActionType.STATISTICS ->
                KmiQuickActionColors(
                    background = Color(0xFF042F2E),
                    content = Color(0xFF5EEAD4)
                )

            KmiQuickActionType.ASSISTANT ->
                KmiQuickActionColors(
                    background = Color(0xFF422006),
                    content = Color(0xFFFCD34D)
                )

            KmiQuickActionType.GUIDE ->
                KmiQuickActionColors(
                    background = Color(0xFF1E1B4B),
                    content = Color(0xFFA5B4FC)
                )

            KmiQuickActionType.SHARE ->
                KmiQuickActionColors(
                    background = Color(0xFF500724),
                    content = Color(0xFFF9A8D4)
                )
        }
    } else {
        when (type) {
            KmiQuickActionType.SEARCH ->
                KmiQuickActionColors(
                    background = Color(0xFFEDE9FE),
                    content = Color(0xFF6D28D9)
                )

            KmiQuickActionType.HOME ->
                KmiQuickActionColors(
                    background = Color(0xFFDBEAFE),
                    content = Color(0xFF2563EB)
                )

            KmiQuickActionType.SETTINGS ->
                KmiQuickActionColors(
                    background = Color(0xFFFFE4E6),
                    content = Color(0xFFBE123C)
                )

            KmiQuickActionType.STATISTICS ->
                KmiQuickActionColors(
                    background = Color(0xFFCCFBF1),
                    content = Color(0xFF0F766E)
                )

            KmiQuickActionType.ASSISTANT ->
                KmiQuickActionColors(
                    background = Color(0xFFFEF3C7),
                    content = Color(0xFFB45309)
                )

            KmiQuickActionType.GUIDE ->
                KmiQuickActionColors(
                    background = Color(0xFFE0E7FF),
                    content = Color(0xFF4338CA)
                )

            KmiQuickActionType.SHARE ->
                KmiQuickActionColors(
                    background = Color(0xFFFCE7F3),
                    content = Color(0xFFBE185D)
                )
        }
    }
}

/**
 * צבעי תג מצב מאמן/מתאמן.
 */
@Composable
fun kmiRolePillColors(
    isCoach: Boolean
): KmiQuickActionColors {
    return if (isCoach) {
        KmiQuickActionColors(
            background = MaterialTheme.colorScheme.primaryContainer,
            content = MaterialTheme.colorScheme.onPrimaryContainer
        )
    } else {
        KmiQuickActionColors(
            background = MaterialTheme.colorScheme.secondaryContainer,
            content = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
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

