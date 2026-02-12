@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
package il.kmi.app.screens

import android.content.SharedPreferences
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import il.kmi.app.KmiViewModel
import il.kmi.app.screens.registration.RegistrationFormScreen
import il.kmi.shared.prefs.KmiPrefs

/**
 * מסך רישום משתמש חדש כמתאמן (לא מאמן).
 *
 * שימו לב:
 *  - כאן אנחנו דואגים שה-SharedPreferences שעובר לטופס הרישום יהיה תמיד "kmi_user",
 *    כדי שכל המסכים האחרונים (פורום הסניף, אודות מתאמנים, נוכחות) יקראו בדיוק
 *    מאותו מקור נתונים מקומי.
 */
@Composable
fun NewUserTraineeScreen(
    nav: NavHostController,
    vm: KmiViewModel,
    kmiPrefs: KmiPrefs,
    onBack: () -> Unit,
    onRegistrationComplete: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenLegal: () -> Unit,
    onOpenDrawer: () -> Unit,
    // יכול להגיע מבחוץ, אבל אם null נפתח כאן בעצמנו את "kmi_user"
    sp: SharedPreferences? = null,
    skipOtp: Boolean = false,        // אפשר להשאיר הפרמטר אם הוא בשימוש חיצוני
    onAuthVerified: () -> Unit = {}  // כנ"ל
) {
    BackHandler { onBack() }

    val ctx = LocalContext.current
    // SharedPreferences אחיד לכל האפליקציה עבור פרופיל המשתמש
    val userSp = remember(sp) {
        sp ?: ctx.getSharedPreferences("kmi_user", android.content.Context.MODE_PRIVATE)
    }

    RegistrationFormScreen(
        initial = "trainee",
        onBack = onBack,
        onRegistrationComplete = onRegistrationComplete,
        onOpenTerms = onOpenTerms,
        onOpenLegal = onOpenLegal,
        onOpenDrawer = onOpenDrawer,
        vm = vm,
        sp = userSp,
        kmiPrefs = kmiPrefs
        // אם בעתיד תרצה לחבר skipOtp / onAuthVerified – נעשה זאת במסך הרישום עצמו
    )
}
