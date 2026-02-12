package il.kmi.app.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG

enum class AppLockMethod { NONE, BIOMETRIC }

object AppLockStore {
    private const val SP = "kmi_settings"
    private const val KEY_METHOD = "app_lock_method"
    private const val KEY_LAST_OK = "app_lock_last_ok"

    fun getMethod(ctx: Context): AppLockMethod {
        val m = ctx.getSharedPreferences(SP, Context.MODE_PRIVATE)
            .getString(KEY_METHOD, "NONE") ?: "NONE"
        return runCatching { AppLockMethod.valueOf(m) }.getOrDefault(AppLockMethod.NONE)
    }

    fun setMethod(ctx: Context, m: AppLockMethod) {
        ctx.getSharedPreferences(SP, Context.MODE_PRIVATE)
            .edit().putString(KEY_METHOD, m.name).apply()
        if (m == AppLockMethod.NONE) {
            // ננקה חותמת אימות אחרונה
            ctx.getSharedPreferences(SP, Context.MODE_PRIVATE)
                .edit().remove(KEY_LAST_OK).apply()
        }
    }

    fun setLastOk(ctx: Context, timeMs: Long = System.currentTimeMillis()) {
        ctx.getSharedPreferences(SP, Context.MODE_PRIVATE)
            .edit().putLong(KEY_LAST_OK, timeMs).apply()
    }

    fun getLastOk(ctx: Context): Long =
        ctx.getSharedPreferences(SP, Context.MODE_PRIVATE).getLong(KEY_LAST_OK, 0L)
}

object AppLock {
    /** כמה זמן האימות תקף לפני שנדרש שוב (כאן: 5 דקות) */
    private const val TIMEOUT_MS = 5 * 60_000L

    /** האם יש ביומטרי זמין במכשיר/למשתמש */
    fun canUseBiometrics(ctx: Context): Boolean {
        val mgr = BiometricManager.from(ctx)
        val res = mgr.canAuthenticate(BIOMETRIC_STRONG)
        return res == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * קרא ב־onResume או כשחוזרים לקדמה.
     * אם force=true נדרוש אימות עכשיו גם אם יש תוקף קיים.
     */
    fun requireIfNeeded(
        activity: FragmentActivity,
        force: Boolean = false,
        onResult: ((Boolean) -> Unit)? = null
    ) {
        val ctx = activity
        when (AppLockStore.getMethod(ctx)) {
            AppLockMethod.NONE -> {
                onResult?.invoke(true)
                return
            }
            AppLockMethod.BIOMETRIC -> {
                if (!canUseBiometrics(ctx)) {
                    onResult?.invoke(true) // לא ניתן לאכוף – לא נחסום את המשתמש
                    return
                }
                val last = AppLockStore.getLastOk(ctx)
                val fresh = !force && (System.currentTimeMillis() - last) < TIMEOUT_MS
                if (fresh) {
                    onResult?.invoke(true)
                    return
                }

                val executor = ContextCompat.getMainExecutor(ctx)
                val prompt = BiometricPrompt(
                    activity,
                    executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            AppLockStore.setLastOk(ctx)
                            onResult?.invoke(true)
                        }
                        override fun onAuthenticationError(code: Int, err: CharSequence) {
                            onResult?.invoke(false)
                        }
                        override fun onAuthenticationFailed() { /* נמשיך עד שהדיאלוג ייסגר */ }
                    }
                )

                val info = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("כניסה מאובטחת")
                    .setSubtitle("אימות באמצעות טביעת אצבע או זיהוי פנים")
                    .setNegativeButtonText("ביטול")
                    .setAllowedAuthenticators(BIOMETRIC_STRONG)
                    .build()

                prompt.authenticate(info)
            }
        }
    }
}
