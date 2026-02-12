package il.kmi.app.training

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import il.kmi.app.R
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import java.util.*
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Place
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.remember


private fun dayOfWeekName(cal: Calendar): String {
    return when (cal.get(Calendar.DAY_OF_WEEK)) {
        Calendar.SUNDAY -> "יום ראשון"
        Calendar.MONDAY -> "יום שני"
        Calendar.TUESDAY -> "יום שלישי"
        Calendar.WEDNESDAY -> "יום רביעי"
        Calendar.THURSDAY -> "יום חמישי"
        Calendar.FRIDAY -> "יום שישי"
        Calendar.SATURDAY -> "שבת"
        else -> ""
    }
}

/** מחלץ HH:mm ממחרוזת (אם קיימת); אחרת מחזיר את המקור */
private fun onlyTime(s: String): String {
    val m = Regex("""\b([01]?\d|2[0-3]):\d{2}\b""").find(s)
    return m?.value ?: s
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun TrainingCardCompact(training: TrainingData) {
    val context = LocalContext.current

    // יום + תאריך
    val dateText = remember(training.cal.timeInMillis) {
        val fmt = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale("he", "IL"))
        fmt.format(training.cal.time)
    }
    val dayText = remember(training.cal.timeInMillis) {
        when (training.cal.get(java.util.Calendar.DAY_OF_WEEK)) {
            java.util.Calendar.SUNDAY    -> "יום ראשון"
            java.util.Calendar.MONDAY    -> "יום שני"
            java.util.Calendar.TUESDAY   -> "יום שלישי"
            java.util.Calendar.WEDNESDAY -> "יום רביעי"
            java.util.Calendar.THURSDAY  -> "יום חמישי"
            java.util.Calendar.FRIDAY    -> "יום שישי"
            else                         -> "שבת"
        }
    }

    val timeLine = "${onlyTime(training.start)} – ${onlyTime(training.end)}"
    val addrSafe = (training.address ?: "").ifBlank { "Israel" }
    val encodedAddr = Uri.encode(addrSafe)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text("$dayText, $dateText", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(timeLine, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Spacer(Modifier.height(8.dp))

            // ── שורה תחתונה: אייקוני ניווט באותה שורה (ללא כיתוב) — קומפקטיים
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {

                    // וייז
                    IconButton(
                        onClick = {
                            try {
                                val uri = Uri.parse("waze://?q=$encodedAddr")
                                val intent = Intent(Intent.ACTION_VIEW, uri)
                                    .apply { setPackage("com.waze") }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                // נפילת חסד: דפדפן
                                val web = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://waze.com/ul?q=$encodedAddr")
                                )
                                context.startActivity(web)
                            }
                        },
                        modifier = Modifier
                            .size(32.dp) // כפתור קטן יותר ~10%
                            .background(Color(0xFF0C9EFF), shape = RoundedCornerShape(10.dp))
                    ) {
                        // שימוש ב-Image כדי לשמור על צורת הווקטור המקורית (בלי tint)
                        Image(
                            painter = painterResource(id = R.drawable.ic_waze),
                            contentDescription = "Waze",
                            modifier = Modifier.size(22.dp),  // האייקון עצמו קטן ~10%
                            contentScale = ContentScale.Fit
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    // גוגל מפות
                    IconButton(
                        onClick = {
                            try {
                                val uri = Uri.parse("geo:0,0?q=$encodedAddr")
                                val intent = Intent(Intent.ACTION_VIEW, uri)
                                    .apply { setPackage("com.google.android.apps.maps") }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                // נפילת חסד: דפדפן
                                val web = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://www.google.com/maps/search/?api=1&query=$encodedAddr")
                                )
                                context.startActivity(web)
                            }
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFF34A853), shape = RoundedCornerShape(10.dp))
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_google_maps),
                            contentDescription = "Google Maps",
                            modifier = Modifier.size(22.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }
    }
}
