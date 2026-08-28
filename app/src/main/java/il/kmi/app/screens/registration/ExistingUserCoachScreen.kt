@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package il.kmi.app.screens.registration

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import il.kmi.shared.prefs.KmiPrefs

/**
 * מסך "משתמש קיים – מאמן".
 *
 * כאן אנחנו מפעילים את ExistingUserTraineeScreen.
 * ההבדל בין מאמן / מתאמן נקבע לפי user_role ב־SharedPreferences,
 * וה־UI מטופל בתוך ExistingUserTraineeScreen.
 */
@Composable
fun ExistingUserCoachScreen(
    onBack: () -> Unit,
    onLoginComplete: () -> Unit,
    sp: SharedPreferences,
    kmiPrefs: KmiPrefs
) {
    ExistingUserTraineeScreen(
        onBack = onBack,
        onLoginComplete = onLoginComplete,
        sp = sp,
        kmiPrefs = kmiPrefs
    )
}