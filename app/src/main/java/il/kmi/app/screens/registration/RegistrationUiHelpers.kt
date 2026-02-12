package il.kmi.app.screens.registration

// ===== Imports חייבים להיות בתחילת הקובץ =====
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.*
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import il.kmi.shared.prefs.KmiPrefs
import java.util.concurrent.TimeUnit

// מפתחות שמירה ב-SP (לשמור תאימות אחורה)
private const val KEY_FULL_NAME   = "full_name"
private const val KEY_PHONE       = "phone"
private const val KEY_REGION      = "region"
private const val KEY_BRANCH      = "branch"
private const val KEY_AGE_GROUP   = "age_group"

data class RegistrationUiState(
    val fullName: String = "",
    val phone: String = "",
    val region: String = "",
    val branch: String = "",
    val ageGroup: String = "",

    // שגיאות אימות
    val nameError: String? = null,
    val phoneError: String? = null,
)

fun validateRegistration(state: RegistrationUiState): RegistrationUiState {
    var s = state
    s = s.copy(nameError  = if (s.fullName.trim().length < 2) "שם קצר מדי" else null)
    s = s.copy(phoneError = if (!s.phone.matches(Regex("^\\+?\\d{8,15}\$"))) "טלפון לא תקין" else null)
    return s
}

private fun preferKmpOrSp(kmp: String?, sp: SharedPreferences, key: String): String {
    // אם ב-KMP יש ערך לא-ריק — השתמש בו; אחרת קרא מ-SP (עם ברירת מחדל ריקה)
    return kmp?.takeIf { it.isNotBlank() } ?: (sp.getString(key, "") ?: "")
}

fun restoreRegistration(sp: SharedPreferences, kmiPrefs: KmiPrefs): RegistrationUiState {
    return RegistrationUiState(
        fullName = preferKmpOrSp(kmiPrefs.fullName, sp, KEY_FULL_NAME),
        phone    = preferKmpOrSp(kmiPrefs.phone,    sp, KEY_PHONE),
        region   = preferKmpOrSp(kmiPrefs.region,   sp, KEY_REGION),
        branch   = preferKmpOrSp(kmiPrefs.branch,   sp, KEY_BRANCH),
        ageGroup = preferKmpOrSp(kmiPrefs.ageGroup, sp, KEY_AGE_GROUP)
    ).let(::validateRegistration)
}

fun persistRegistration(sp: SharedPreferences, kmiPrefs: KmiPrefs, s: RegistrationUiState) {
    // ----- KMP (מקור אמת) -----
    kmiPrefs.fullName = s.fullName
    kmiPrefs.phone    = s.phone
    kmiPrefs.region   = s.region
    kmiPrefs.branch   = s.branch
    kmiPrefs.ageGroup = s.ageGroup

    // ----- SP (תאימות לאחור) -----
    sp.edit()
        .putString(KEY_FULL_NAME, s.fullName)
        .putString(KEY_PHONE,    s.phone)
        .putString(KEY_REGION,   s.region)
        .putString(KEY_BRANCH,   s.branch)
        .putString(KEY_AGE_GROUP,s.ageGroup)
        .apply()
}

/* ========================
   תוספות עזר ל-SMS Phone Auth
   (ללא מחיקת שורות קיימות)
   ======================== */

// מציאת Activity מתוך Context באופן בטוח
private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** נירמול ובדיקת מספרים לפורמט E.164 (ברירת מחדל ישראל +972) */
object PhoneFormat {
    fun toE164(raw: String, defaultCountryCode: String = "+972"): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return trimmed

        // אם כבר בפורמט בינלאומי עם פלוס — ננקה תווים לא ספרתיים
        if (trimmed.startsWith("+")) {
            return "+" + trimmed.drop(1).filter(Char::isDigit)
        }

        // המרה מ-00… ל-+…
        if (trimmed.startsWith("00")) {
            val digits = trimmed.drop(2).filter(Char::isDigit)
            return if (digits.isNotEmpty()) "+$digits" else trimmed
        }

        // מקרה כללי: ספרות בלבד, מקפים/רווחים וכו'
        val digits = trimmed.filter(Char::isDigit)
        return when {
            // קוד מדינה בלי פלוס (למשל 9725…) — נוסיף פלוס
            digits.startsWith(defaultCountryCode.drop(1)) -> "+$digits"
            // מספר מקומי שמתחיל ב-0 — נסיר 0 ראשון ונוסיף קידומת מדינה
            digits.startsWith("0") -> defaultCountryCode + digits.drop(1)
            // נראה בינלאומי (8–15 ספרות) — נוסיף פלוס
            digits.length in 8..15 -> "+$digits"
            else -> trimmed
        }
    }

    /** בדיקה בסיסית ל-E.164: + ואחריו 8–15 ספרות */
    fun isE164(number: String): Boolean {
        if (!number.startsWith("+")) return false
        val digits = number.drop(1)
        return digits.isNotEmpty() && digits.all(Char::isDigit) && digits.length in 8..15
    }
}

/**
 * עוזר מרוכז לזרימת אימות SMS של Firebase.
 * מחזיק verificationId/token ומספק שתי פונקציות: שליחה ואימות.
 */
object PhoneAuthHelpers {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    var verificationId: String? = null
        private set
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    fun sendSmsCode(
        context: Context,
        rawPhone: String,
        timeoutSeconds: Long = 60,
        onSent: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val act = context.findActivity()
        if (act == null) {
            onError(IllegalStateException("No Activity context for SMS verification"))
            return
        }

        val phone = PhoneFormat.toE164(rawPhone)
        if (!PhoneFormat.isE164(phone)) {
            onError(IllegalArgumentException("Invalid E.164 phone number"))
            return
        }

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(cred: com.google.firebase.auth.PhoneAuthCredential) {
                // Auto-retrieval / Instant verification
                auth.signInWithCredential(cred).addOnCompleteListener { res ->
                    if (!res.isSuccessful) {
                        onError(res.exception ?: Exception("signInWithCredential failed"))
                    }
                }
            }

            override fun onVerificationFailed(e: FirebaseException) {
                val code = (e as? FirebaseAuthException)?.errorCode
                Log.e("PhoneAuth", "onVerificationFailed code=$code", e)
                onError(e)
            }

            override fun onCodeSent(
                vid: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                verificationId = vid
                resendToken = token
                onSent()
            }
        }

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phone)
            .setTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .setActivity(act)
            .setCallbacks(callbacks)
            .apply { resendToken?.let { setForceResendingToken(it) } }
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifySmsCode(
        code: String,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val vid = verificationId
        if (vid.isNullOrBlank() || code.length != 6) {
            onError(IllegalStateException("Missing verificationId or bad code length"))
            return
        }
        val credential = PhoneAuthProvider.getCredential(vid, code)
        auth.signInWithCredential(credential).addOnCompleteListener { res ->
            if (res.isSuccessful) onSuccess()
            else onError(res.exception ?: Exception("Verification failed"))
        }
    }
}
