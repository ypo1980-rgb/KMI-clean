package il.kmi.app.voicecommands

/**
 * גשר גלובלי לפתיחת חלון הפקודות הקוליות.
 *
 * KmiTopBar מבקש לפתוח את החלון דרך open().
 * MainNavHost מחבר ומנתק את המימוש באמצעות bind().
 */
object VoiceCommandsBridge {

    private var openHandler: (() -> Unit)? = null

    fun bind(
        handler: (() -> Unit)?
    ) {
        openHandler = handler
    }

    fun open(): Boolean {
        val handler = openHandler ?: return false
        handler()
        return true
    }

    fun isAvailable(): Boolean {
        return openHandler != null
    }
}