package il.kmi.app.privacy

import java.util.Locale
import kotlin.math.abs

/**
 * מחליף שמות אמיתיים בכינויים אנונימיים
 * כאשר מצב ההדגמה פעיל.
 *
 * הקובץ אינו משנה שום מידע במסד הנתונים.
 * ההחלפה מתבצעת בתצוגה בלבד.
 */
object TraineeDisplayNameMapper {

    private const val DEFAULT_DEMO_NAME_HE =
        "מתאמן"

    private const val DEFAULT_DEMO_NAME_EN =
        "Trainee"

    /**
     * מחזיר את השם שצריך להציג במסך.
     *
     * demoIndex:
     * כאשר מציגים רשימה, מומלץ להעביר את
     * מיקום המתאמן ברשימה. כך מתקבלים שמות
     * רציפים וללא כפילויות:
     *
     * מתאמן 1, מתאמן 2, מתאמן 3...
     *
     * כאשר demoIndex לא נשלח, נוצר מספר
     * עקבי מתוך stableKey.
     */
    fun displayName(
        realName: String?,
        stableKey: String?,
        demoIndex: Int? = null,
        isEnglish: Boolean = false
    ): String {
        val cleanRealName =
            realName
                ?.trim()
                .orEmpty()

        if (!DemoPrivacy.isEnabled()) {
            return cleanRealName
        }

        val prefix =
            if (isEnglish) {
                DEFAULT_DEMO_NAME_EN
            } else {
                DEFAULT_DEMO_NAME_HE
            }

        val sequentialNumber =
            demoIndex
                ?.coerceAtLeast(0)
                ?.plus(1)

        if (sequentialNumber != null) {
            return "$prefix $sequentialNumber"
        }

        val cleanStableKey =
            stableKey
                ?.trim()
                .orEmpty()
                .ifBlank {
                    cleanRealName
                }

        if (cleanStableKey.isBlank()) {
            return prefix
        }

        /*
         * מספר עקבי בטווח רחב כדי לצמצם מאוד
         * אפשרות ששני מתאמנים יקבלו אותו כינוי
         * כאשר לא נשלח demoIndex.
         */
        val safeHash =
            cleanStableKey.hashCode().let { hash ->
                if (hash == Int.MIN_VALUE) {
                    0
                } else {
                    abs(hash)
                }
            }

        val stableNumber =
            safeHash % 9_999 + 1

        return String.format(
            Locale.ROOT,
            "%s %d",
            prefix,
            stableNumber
        )
    }

    /**
     * פונקציית עזר לרשימה שלמה.
     *
     * מחזירה שמות רציפים וללא כפילויות
     * בהתאם לסדר הרשימה.
     */
    fun displayNames(
        trainees: List<TraineeNameSource>,
        isEnglish: Boolean = false
    ): List<String> {
        return trainees.mapIndexed {
                index,
                trainee ->

            displayName(
                realName = trainee.realName,
                stableKey = trainee.stableKey,
                demoIndex = index,
                isEnglish = isEnglish
            )
        }
    }

    data class TraineeNameSource(
        val realName: String?,
        val stableKey: String?
    )
}