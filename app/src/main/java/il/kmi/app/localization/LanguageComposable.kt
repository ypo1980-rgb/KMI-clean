package il.kmi.app.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager

@Composable
fun rememberIsEnglish(): Boolean {
    val ctx = LocalContext.current
    val langManager = remember { AppLanguageManager(ctx) }
    return langManager.getCurrentLanguage() == AppLanguage.ENGLISH
}