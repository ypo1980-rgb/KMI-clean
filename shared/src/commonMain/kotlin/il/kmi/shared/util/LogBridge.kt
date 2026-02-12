package il.kmi.shared.util

expect object LogBridge {
    fun d(tag: String, msg: String)
    fun e(tag: String, msg: String, t: Throwable? = null)
}
