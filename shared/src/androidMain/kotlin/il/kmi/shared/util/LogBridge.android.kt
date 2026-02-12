package il.kmi.shared.util

actual object LogBridge {

    actual fun d(tag: String, msg: String) {
        android.util.Log.d(tag, msg)
    }

    actual fun e(tag: String, msg: String, t: Throwable?) {
        // Java API -> בלי named args
        android.util.Log.e(tag, msg, t)
    }
}
