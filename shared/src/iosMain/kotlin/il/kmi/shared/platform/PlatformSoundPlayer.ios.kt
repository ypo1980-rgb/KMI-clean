package il.kmi.shared.platform

actual class PlatformSoundPlayer actual constructor(platformContext: Any?) {
    actual fun play(name: String) {
        // No-Op. אפשר לממש עם AVFoundation בהמשך.
    }

    actual fun release() {
        // No-Op
    }
}
