@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package il.kmi.app.screens.registration

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import il.kmi.app.KmiViewModel
import il.kmi.shared.prefs.KmiPrefs

/**
 * מסך "משתמש קיים – מאמן".
 *
 * כאן אנחנו פשוט מפעילים את ExistingUserTraineeScreen עם אותם פרמטרים.
 * ההבדל בין מאמן / מתאמן נקבע לפי user_role ב־SharedPreferences,
 * וה־UI (כולל הרקע) כבר מטופל בתוך ExistingUserTraineeScreen.
 */
@Composable
fun ExistingUserCoachScreen(
    onBack: () -> Unit,
    onLoginComplete: () -> Unit,
    onOpenRecovery: () -> Unit = {},
    vm: KmiViewModel,
    sp: SharedPreferences,
    kmiPrefs: KmiPrefs,
    onOpenDrawer: () -> Unit = {}
) {
    ExistingUserTraineeScreen(
        onBack = onBack,
        onLoginComplete = onLoginComplete,
        onOpenRecovery = onOpenRecovery,
        vm = vm,
        sp = sp,
        kmiPrefs = kmiPrefs,
        onOpenDrawer = onOpenDrawer
    )
}

