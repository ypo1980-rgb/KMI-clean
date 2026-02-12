package il.kmi.app.utils

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import il.kmi.shared.domain.Belt
import java.io.File
import java.io.FileOutputStream

object PdfShareUtil {

    private val beltColors = mapOf(
        Belt.YELLOW to Color.rgb(255, 235, 59),   // צהוב
        Belt.ORANGE to Color.rgb(255, 152, 0),   // כתום
        Belt.GREEN to Color.rgb(76, 175, 80),    // ירוק
        Belt.BLUE to Color.rgb(33, 150, 243),    // כחול
        Belt.BROWN to Color.rgb(121, 85, 72),    // חום
        Belt.BLACK to Color.rgb(0, 0, 0)         // שחור
    )

    fun createAndShareProgressPdf(context: Context, progress: Map<Belt, Int>) {
        try {
            // 📝 יצירת קובץ PDF בתוך cacheDir
            val pdfFile = File(context.cacheDir, "progress_report.pdf")
            val outputStream = FileOutputStream(pdfFile)

            val pdfDoc = android.graphics.pdf.PdfDocument()
            val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(600, 800, 1).create()
            val page = pdfDoc.startPage(pageInfo)

            val canvas = page.canvas
            val paint = Paint().apply {
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            var y = 50
            paint.color = Color.BLACK
            canvas.drawText("דו״ח התקדמות חגורות", 200f, y.toFloat(), paint)
            y += 50

            progress.forEach { (belt, percent) ->
                val color = beltColors[belt] ?: Color.DKGRAY
                paint.color = color
                canvas.drawText("${belt.heb}: $percent%", 100f, y.toFloat(), paint)
                y += 40
            }

            pdfDoc.finishPage(page)
            pdfDoc.writeTo(outputStream)
            pdfDoc.close()
            outputStream.close()

            // ✅ URI בטוח לשיתוף
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "il.kmi.app.fileprovider", // חייב להיות תואם ל־AndroidManifest.xml
                pdfFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "שתף דו״ח PDF")
            context.startActivity(chooser)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "שגיאה בשיתוף ה־PDF", Toast.LENGTH_SHORT).show()
        }
    }
}
