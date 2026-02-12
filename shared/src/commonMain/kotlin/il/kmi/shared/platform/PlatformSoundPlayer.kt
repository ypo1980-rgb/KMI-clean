package il.kmi.shared.platform

/**
 * נגן צליל קצר.
 * כרגע: API אחיד לכל הפלטפורמות. אפשר להשאיר No-Op ב-iOS/Android אם אין משאבים משותפים.
 */
expect class PlatformSoundPlayer(platformContext: Any?) {
    fun play(name: String)
    fun release()
}
