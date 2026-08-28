package il.kmi.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

/**
 * מגדיר את כיוון הפריסה לפי השפה הפעילה.
 *
 * עברית  -> RTL
 * אנגלית -> LTR
 *
 * בתוך המעטפת יש להשתמש בכיוונים לוגיים:
 * Start / End
 *
 * אין צורך לבדוק בכל רכיב בנפרד אם השפה אנגלית.
 */
@Composable
fun KmiLanguageDirection(
    isEnglish: Boolean,
    content: @Composable () -> Unit
) {
    val layoutDirection =
        if (isEnglish) {
            LayoutDirection.Ltr
        } else {
            LayoutDirection.Rtl
        }

    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection,
        content = content
    )
}