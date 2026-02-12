package il.kmi.app.util

import android.content.Context
import android.content.Intent

fun shareText(context: Context, text: String, title: String = "שיתוף דו\"ח") {
    val i = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(i, title))
}
