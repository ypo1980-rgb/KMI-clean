package il.kmi.app

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import il.kmi.shared.domain.Belt
import java.io.File
import java.io.FileOutputStream

fun openBeltPdf(context: Context, belt: Belt) {
    val id = belt.id.lowercase() // white, yellow, orange, green, blue, brown, black

    // נסה קודם kmi_<id>, ואם לא קיים – kami_<id>
    val kmiName  = "kmi_$id"
    val kamiName = "kami_$id"

    val resId = when (val cand = context.resources.getIdentifier(kmiName, "raw", context.packageName)) {
        0 -> context.resources.getIdentifier(kamiName, "raw", context.packageName)
        else -> cand
    }

    val chosenName = if (resId != 0 && context.resources.getResourceEntryName(resId).startsWith("kami_")) kamiName else kmiName

    if (resId == 0) {
        Toast.makeText(
            context,
            "לא נמצא קובץ PDF לחגורה ${belt.heb}. שים קובץ בשם $kmiName.pdf או $kamiName.pdf בתיקיית res/raw",
            Toast.LENGTH_LONG
        ).show()
        return
    }

    try {
        val outFile = File(context.cacheDir, "$chosenName.pdf")
        context.resources.openRawResource(resId).use { input ->
            FileOutputStream(outFile).use { output -> input.copyTo(output) }
        }

        val uri: Uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            outFile
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "פתיחת מסמך"))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "לא נמצאה אפליקציה לפתיחת PDF", Toast.LENGTH_LONG).show()
    } catch (_: Exception) {
        Toast.makeText(context, "שגיאה בפתיחת הקובץ", Toast.LENGTH_LONG).show()
    }
}
