package il.kmi.app.voicecommands

/**
 * גשר גלובלי בין הפקודה הקולית לבין פעולת השיתוף
 * של המסך הפעיל.
 *
 * המסך אינו יוצר כאן PDF חדש. הוא רושם את אותה
 * פעולה שמופעלת בלחיצה על אייקון השיתוף שלו.
 */
object VoiceShareBridge {

    private var activeOwner: Any? = null
    private var activeShareAction: (() -> Unit)? = null

    @Synchronized
    fun bind(
        owner: Any,
        onShare: () -> Unit
    ) {
        activeOwner = owner
        activeShareAction = onShare
    }

    /**
     * מסיר פעולה רק אם המסך שמבקש להסיר אותה
     * הוא עדיין המסך הפעיל שנרשם אחרון.
     */
    @Synchronized
    fun unbind(owner: Any) {
        if (activeOwner === owner) {
            activeOwner = null
            activeShareAction = null
        }
    }

    /**
     * מחזיר true כאשר קיימת פעולת שיתוף והיא הופעלה.
     * מחזיר false במסך שאין בו אפשרות שיתוף.
     */
    fun perform(): Boolean {
        val action =
            synchronized(this) {
                activeShareAction
            } ?: return false

        action()
        return true
    }
}