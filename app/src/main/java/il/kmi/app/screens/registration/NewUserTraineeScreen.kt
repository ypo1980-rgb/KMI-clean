@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package il.kmi.app.screens

import android.content.Context
import android.content.SharedPreferences
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import il.kmi.app.KmiViewModel
import il.kmi.app.screens.registration.RegistrationFormScreen
import il.kmi.shared.prefs.KmiPrefs

/**
 * מסך רישום משתמש חדש כמתאמן.
 *
 * בכניסה רגילה:
 *  - המשתמש מגיע דרך מסך "לקוח חדש / קיים"
 *  - הטופס נשאר רגיל.
 *
 * בכניסה עם Google:
 *  - Google כבר זיהה uid / email / displayName
 *  - כאן אנחנו זורעים את הנתונים ל-SharedPreferences לפני פתיחת הטופס
 *  - הטופס נפתח עם שם, מייל ושם משתמש מלאים מראש
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
    skipOtp: Boolean = false,
    onAuthVerified: () -> Unit = {}
) {
    BackHandler { onBack() }

    val ctx = LocalContext.current

    // SharedPreferences אחיד לכל האפליקציה עבור פרופיל המשתמש
    val userSp = remember(sp) {
        sp ?: ctx.getSharedPreferences("kmi_user", Context.MODE_PRIVATE)
    }

    // חשוב שזה ירוץ לפני RegistrationFormScreen נבנה,
    // כדי שה-state הראשוני של הטופס יקרא כבר את ערכי Google.
    remember(skipOtp, userSp) {
        if (skipOtp) {
            seedGoogleUserIntoPrefs(userSp)
        }
        true
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
    )
}

/**
 * שומר את נתוני Google/FirebaseAuth לתוך kmi_user לפני פתיחת טופס ההרשמה.
 *
 * לא מוחק ערכים קיימים שהמשתמש כבר מילא.
 * ממלא רק אם השדה עדיין ריק.
 */
private fun seedGoogleUserIntoPrefs(sp: SharedPreferences) {
    val user = FirebaseAuth.getInstance().currentUser ?: return

    val uid = user.uid
    val email = user.email.orEmpty().trim()
    val displayName = user.displayName.orEmpty().trim()
    val photoUrl = user.photoUrl?.toString().orEmpty()

    val fallbackName = when {
        displayName.isNotBlank() -> displayName
        email.contains("@") -> email.substringBefore("@")
        else -> ""
    }

    val usernameFromGoogle = when {
        email.isNotBlank() -> email
        fallbackName.isNotBlank() -> fallbackName
        else -> uid
    }

    sp.edit().apply {
        putString("uid", uid)
        putString("firebase_uid", uid)
        putString("authProvider", "google")
        putBoolean("google_login", true)
        putBoolean("skip_otp", true)

        if (email.isNotBlank()) {
            putStringIfBlank(sp, "email", email)
            putStringIfBlank(sp, "user_email", email)
        }

        if (fallbackName.isNotBlank()) {
            putStringIfBlank(sp, "fullName", fallbackName)
            putStringIfBlank(sp, "displayName", fallbackName)
            putStringIfBlank(sp, "name", fallbackName)
            putStringIfBlank(sp, "user_name", fallbackName)
        }

        if (photoUrl.isNotBlank()) {
            putStringIfBlank(sp, "photoUrl", photoUrl)
        }

        putStringIfBlank(sp, "username", usernameFromGoogle)
        putStringIfBlank(sp, "userName", usernameFromGoogle)

        // כדי ששדה סיסמה לא יישאר ריק אם הטופס עדיין דורש אותו.
        // בשלב הבא עדיף להסתיר/לבטל שדה סיסמה כאשר authProvider=google.
        putStringIfBlank(sp, "password", "GOOGLE_AUTH")
        putStringIfBlank(sp, "user_password", "GOOGLE_AUTH")

        commit()
    }
}

private fun SharedPreferences.Editor.putStringIfBlank(
    sp: SharedPreferences,
    key: String,
    value: String
): SharedPreferences.Editor {
    val existing = sp.getString(key, null)
    if (existing.isNullOrBlank() && value.isNotBlank()) {
        putString(key, value)
    }
    return this
}