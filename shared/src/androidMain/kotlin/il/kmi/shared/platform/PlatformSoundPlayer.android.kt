package il.kmi.shared.platform

actual class PlatformSoundPlayer actual constructor(platformContext: Any?) {
    actual fun play(name: String) {
        // No-Op כרגע כדי לא להיות תלוי ב-R של app (ולקמפל נקי).
        // אם תרצה בהמשך: נעביר את קובץ ה-audio למשאבים של shared או נוסיף hook מה-app.
    }

    actual fun release() {
        // No-Op
    }
}
