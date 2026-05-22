package il.kmi.shared.util

actual object LogBridge {

    actual fun d(tag: String, msg: String) {
        // Production no-op
    }

    actual fun e(tag: String, msg: String, t: Throwable?) {
        // Production no-op
    }
}