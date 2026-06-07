package il.kmi.shared.domain

/**
 * CoachRegistry — תיעוד תהליך אימות מאמנים / מדריכים
 *
 * חשוב:
 * הקובץ הזה לא משמש יותר כמקור אמת להרשאת מאמן.
 * אין לשמור כאן רשימה של 200 מאמנים / מדריכים מורשים.
 *
 * הסיבה:
 * כל קוד שנמצא בתוך האפליקציה נארז בתוך APK / AAB.
 * משתמש מתקדם יכול לפרק את האפליקציה, למצוא קודי מאמן מקומיים,
 * ואז לנסות להיכנס כמאמן ללא הרשאה אמיתית מהשרת.
 *
 * מקור האמת להרשאות מאמן חייב להיות בשרת בלבד:
 *
 * Firestore:
 *
 * collection:
 *   authorizedCoaches
 *
 * מבנה מומלץ לפי UID:
 *
 * authorizedCoaches/{uid}
 *     active: true
 *     coachCode: "234567"
 *     fullName: "שם המאמן"
 *     phone: "0500000000"
 *     email: "coach@example.com"
 *     role: "coach"
 *     region: "..."
 *     branch: "..."
 *     groupKey: "..."
 *     canManageAttendance: true
 *     canManageExams: true
 *     canSendBroadcasts: true
 *     createdAt: serverTimestamp
 *     updatedAt: serverTimestamp
 *
 * תהליך אימות מאמן באפליקציה:
 *
 * 1. משתמש מתחבר עם שם משתמש וסיסמה.
 *
 * 2. אם המשתמש בוחר מצב "מאמן", האפליקציה לא שומרת מיד:
 *      user_role = "coach"
 *
 * 3. האפליקציה קוראת ל־Firestore ובודקת האם קיים מסמך:
 *      authorizedCoaches/{uid}
 *
 * 4. האפליקציה מאשרת מצב מאמן רק אם כל התנאים מתקיימים:
 *      active == true
 *      role == "coach"
 *      coachCode תואם לקוד שהוזן במסך ההתחברות
 *
 * 5. רק לאחר אימות מוצלח מול Firestore מותר לשמור מקומית:
 *      user_role = "coach"
 *      coach_code = הקוד המאומת
 *
 * 6. אם אין הרשאה בשרת, או active != true, או הקוד לא תואם:
 *      user_role חייב להישמר כ־"trainee"
 *      אסור לפתוח את תפריט המאמן
 *      אסור לנווט למסכי מאמן
 *      אסור לאפשר שינוי ציונים / נוכחות / שידורי מאמן
 *
 * 7. גם מסכי המאמן עצמם צריכים להיחסם לפי הרשאה,
 *    ולא להסתמך רק על הסתרת תפריט צדדי.
 *
 * שימוש עתידי:
 * הקובץ הזה נשאר כדי שקוד ישן שמייבא CoachRegistry ימשיך להתקמפל,
 * אבל הוא לא מאשר הרשאת מאמן.
 *
 * אם בעתיד רוצים להציג רשימת מאמנים באדמין:
 * יש לקרוא אותה מ־Firestore ולא מהקובץ הזה.
 */
object CoachRegistry {

    /**
     * הרשאת מאמן מקומית מבוטלת.
     *
     * אין לאשר מאמן לפי קוד שנמצא בתוך האפליקציה.
     * האימות חייב להתבצע מול Firestore בלבד.
     */
    fun isValid(code: String?): Boolean = false

    /**
     * שם מאמן לא נשלף יותר מרשימה מקומית.
     *
     * שם המאמן צריך להגיע מהשרת:
     * authorizedCoaches/{uid}.fullName
     * או users/{uid}.fullName
     */
    fun coachName(code: String?): String? = null

    /**
     * אין רשימת מאמנים מקומית באפליקציה.
     *
     * רשימת 200 המאמנים / מדריכים המורשים צריכה להישמר ב־Firestore:
     * authorizedCoaches
     */
    fun allCoaches(): Map<String, String> = emptyMap()
}