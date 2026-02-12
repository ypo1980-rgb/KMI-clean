// ==== imports (יש לשים למעלה בקובץ) ====
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.view.View
import androidx.core.content.FileProvider
import androidx.core.view.drawToBitmap
import java.io.File
import java.io.FileOutputStream

// ==== פונקציית עזר: לכידת מסך ושיתוף ====
fun shareCurrentScreen(
    context: Context,
    rootView: View,
    targetPackage: String? = null,        // לדוגמה "com.whatsapp" כדי לשתף ישירות לווטסאפ
    subject: String = "ק.מ.י – קרב מגן ישראלי"
) {
    // 1) לוכדים את התצוגה הנוכחית כ־Bitmap
    val bitmap: Bitmap = rootView.drawToBitmap()

    // 2) כותבים לקובץ זמני בתיקיית cache
    val cacheDir = File(context.cacheDir, "shares").apply { mkdirs() }
    val outFile = File(cacheDir, "screenshot_${System.currentTimeMillis()}.png")
    FileOutputStream(outFile).use { fos ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
    }

    // 3) מקבלים URI דרך FileProvider (ודואגים להרשאה לקריאה)
    val uri = FileProvider.getUriForFile(
        context,
        context.packageName + ".fileprovider",   // ודא שה־authority זהה למניפסט
        outFile
    )

    // 4) בונים Intent שיתוף
    val share = Intent(Intent.ACTION_SEND)
        .setType("image/png")
        .putExtra(Intent.EXTRA_SUBJECT, subject)
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

    if (targetPackage != null) share.setPackage(targetPackage)

    // 5) מפעילים שיתוף (chooser אם לא כיוונו אפליקציה ספציפית)
    val chooserTitle = "שיתוף צילום מסך"
    val intent = if (targetPackage == null)
        Intent.createChooser(share, chooserTitle)
    else share

    runCatching { context.startActivity(intent) }.onFailure {
        // fallback: אם אפליקציה ספציפית לא נמצאה, נפתח chooser רגיל
        context.startActivity(Intent.createChooser(share, chooserTitle))
    }
}
