package il.kmi.shared.util

import platform.Foundation.NSLog

actual object LogBridge {

    actual fun d(tag: String, msg: String) {
        NSLog("%@: %@", tag, msg)
    }

    actual fun e(tag: String, msg: String, t: Throwable?) {
        if (t != null) {
            NSLog("ERROR [%@] %@ | %@", tag, msg, t.toString())
        } else {
            NSLog("ERROR [%@] %@", tag, msg)
        }
    }
}
