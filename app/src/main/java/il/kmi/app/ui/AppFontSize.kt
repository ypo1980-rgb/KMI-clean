package il.kmi.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * גודל התצוגה שנבחר בהגדרות האפליקציה.
 *
 * הבחירה משפיעה על:
 * 1. כל יחידות ה־sp באמצעות LocalDensity.
 * 2. אייקונים המחוברים ל־KmiIconSize.
 *
 * שם המחלקה ומפתח השמירה נשארים ללא שינוי
 * כדי לשמור על תאימות להגדרות ולנתונים קיימים.
 */
enum class AppFontSize(
    val storageValue: String,
    val scaleFactor: Float
) {
    SMALL(
        storageValue = "small",
        scaleFactor = 0.90f
    ),

    MEDIUM(
        storageValue = "medium",
        scaleFactor = 1.00f
    ),

    LARGE(
        storageValue = "large",
        scaleFactor = 1.15f
    );

    companion object {

        const val PREFERENCE_KEY = "font_size"

        fun fromStorageValue(value: String?): AppFontSize {
            return when (value?.trim()?.lowercase()) {
                SMALL.storageValue -> SMALL
                LARGE.storageValue -> LARGE
                else -> MEDIUM
            }
        }
    }
}

/**
 * מקדם גודל האייקונים הנוכחי.
 *
 * ברירת המחדל היא 1 כדי שרכיב שמוצג מחוץ ל־MainApp
 * עדיין יקבל גודל תקין.
 */
val LocalAppIconScale = staticCompositionLocalOf {
    1.00f
}

/**
 * מחזיר גודל אייקון לאחר החלת בחירת המשתמש.
 *
 * יש להשתמש בפונקציה עבור אייקון בעל גודל מיוחד
 * שאינו מתאים לאחד הגדלים המוגדרים ב־KmiIconSize.
 */
@Composable
fun scaledIconSize(
    baseSize: Dp
): Dp {
    return baseSize * LocalAppIconScale.current
}

/**
 * מקור אמת יחיד לגדלי האייקונים באפליקציה.
 *
 * הגדלים כאן הם גדלי הבסיס במצב בינוני.
 * מצב קטן או גדול מוחל אוטומטית דרך LocalAppIconScale.
 */
object KmiIconSize {

    /**
     * אייקון זעיר בתוך תג או מידע משני.
     */
    val tiny: Dp
        @Composable
        get() = scaledIconSize(14.dp)

    /**
     * אייקון קטן בתוך טקסט, תג או שורה צפופה.
     */
    val small: Dp
        @Composable
        get() = scaledIconSize(18.dp)

    /**
     * גודל ברירת המחדל לרוב האייקונים.
     */
    val medium: Dp
        @Composable
        get() = scaledIconSize(22.dp)

    /**
     * אייקון פעולה בולט או אייקון בכרטיס.
     */
    val large: Dp
        @Composable
        get() = scaledIconSize(28.dp)

    /**
     * אייקון מרכזי במצב ריק, טעינה או כרטיס מרכזי.
     */
    val extraLarge: Dp
        @Composable
        get() = scaledIconSize(36.dp)

    /**
     * אייקון ראשי גדול במיוחד.
     */
    val hero: Dp
        @Composable
        get() = scaledIconSize(48.dp)
}

/**
 * מקור אמת יחיד לגדלי הבסיס של הטקסט באפליקציה.
 *
 * אין להכפיל כאן את הגדלים לפי בחירת המשתמש.
 * ההגדלה וההקטנה מתבצעות אוטומטית באמצעות
 * LocalDensity במעטפת MainApp.
 */
object KmiTypography {

    /**
     * כותרת המסך הראשית בסרגל העליון.
     */
    val screenTitle = TextStyle(
        fontSize = 20.sp,
        lineHeight = 25.sp,
        fontWeight = FontWeight.ExtraBold
    )

    /**
     * כותרת של אזור מרכזי בתוך מסך.
     */
    val sectionTitle = TextStyle(
        fontSize = 17.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Bold
    )

    /**
     * כותרת ראשית בתוך כרטיס, שורה או פריט ברשימה.
     */
    val cardTitle = TextStyle(
        fontSize = 15.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Bold
    )

    /**
     * טקסט תוכן רגיל.
     */
    val body = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal
    )

    /**
     * טקסט הסבר או מידע משני.
     */
    val secondary = TextStyle(
        fontSize = 12.sp,
        lineHeight = 17.sp,
        fontWeight = FontWeight.Normal
    )

    /**
     * טקסט של כפתורים, טאבים ופעולות.
     */
    val action = TextStyle(
        fontSize = 14.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Bold
    )

    /**
     * כיתוב קטן: תאריך, שעה, תגית או הערת שוליים.
     */
    val caption = TextStyle(
        fontSize = 11.sp,
        lineHeight = 15.sp,
        fontWeight = FontWeight.SemiBold
    )

    /**
     * מספר או נתון מרכזי בכרטיסי סיכום וסטטיסטיקה.
     */
    val metric = TextStyle(
        fontSize = 24.sp,
        lineHeight = 29.sp,
        fontWeight = FontWeight.ExtraBold
    )
}