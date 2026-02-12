package il.kmi.app.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * מסך "דרגו אותנו" – תפריט צד (לפני "התנתקות")
 * - X מודרני בסרגל העליון (SideScreenTopBar)
 * - חיפוש חסום עד כניסה/רישום (דרך KmiTopBar במסכים הראשיים; כאן זה מסך צד עם X)
 * - ללא מצב מאמן/מתאמן
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RateUsScreen(
    onClose: () -> Unit,
    supportEmail: String = "support@kmi-app.example", // החלף למייל תמיכה שלך
    playStoreAppId: String = "il.kmi.app",             // החלף ל־applicationId בפועל
    appStoreAppId: String = "0000000000"               // מזהה אפליקציה ב-App Store (ל-iOS)
) {
    val ctx = LocalContext.current

    Scaffold(
        topBar = {
            // סרגל צד עם X מודרני
            SideScreenTopBar(
                title = "דרגו אותנו",
                onClose = onClose
            )
        },
        containerColor = Color(0xFFF7F7FA)
    ) { padding ->

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            color = Color.White,
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 1.dp,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "אהבתם את KMI?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "הדירוג שלכם עוזר לנו להשתפר ולגדול.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )

                // כוכבים (1–5)
                var stars by remember { mutableStateOf(5) }
                StarsRow(
                    value = stars,
                    onChange = { stars = it }
                )

                // אזור משוב כאשר הדירוג נמוך
                var feedback by remember { mutableStateOf("") }
                val lowRating = stars <= 3

                if (lowRating) {
                    OutlinedTextField(
                        value = feedback,
                        onValueChange = { feedback = it },
                        label = { Text("מה נוכל לשפר?") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                    Button(
                        onClick = {
                            sendFeedbackEmail(ctx, supportEmail, "משוב מהאפליקציה (דירוג $stars★)", feedback)
                            markRated(ctx)
                            onClose()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("שליחת משוב")
                    }
                    Text(
                        "אנחנו קוראים כל משוב 🙏",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    // דירוג גבוה → הפניה ל-Google Play (באנדרואיד)
                    Button(
                        onClick = {
                            openPlayStoreForApp(ctx, playStoreAppId)
                            markRated(ctx)
                            onClose()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("דרגו אותנו בחנות Google Play")
                    }

                    // כפתור App Store יוצג רק ב-iOS
                    if (isIos()) {
                        OutlinedButton(
                            onClick = {
                                openAppStoreForApp(ctx, appStoreAppId)
                                markRated(ctx)
                                onClose()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("פתחו ב-App Store")
                        }
                    }

                    // קישור משני לשליחת משוב
                    TextButton(onClick = {
                        sendFeedbackEmail(ctx, supportEmail, "משוב (דירוג $stars★)", "")
                    }) {
                        Text("או שלחו לנו משוב במקום")
                    }
                }
            }
        }
    }
}

/* ------------------ UI: שורת כוכבים ------------------ */

@Composable
private fun StarsRow(
    value: Int,
    onChange: (Int) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 1..5) {
            val filled = i <= value
            FilledIconToggleButton(
                checked = filled,
                onCheckedChange = {
                    onChange(i) // בחירה ישירה בכוכב i
                }
            ) {
                Icon(
                    imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = "$i כוכבים"
                )
            }
        }
    }
}

/* ------------------ Helpers ------------------ */

private fun openPlayStoreForApp(ctx: Context, appId: String) {
    val uriMarket = Uri.parse("market://details?id=$appId")
    val marketIntent = Intent(Intent.ACTION_VIEW, uriMarket).apply {
        addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
    }
    try {
        ctx.startActivity(marketIntent)
    } catch (_: ActivityNotFoundException) {
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$appId"))
        ctx.startActivity(webIntent)
    }
}

// פתיחת דף האפליקציה ב-App Store (ל-iOS)
private fun openAppStoreForApp(ctx: Context, appStoreId: String) {
    val url = "https://apps.apple.com/app/id$appStoreId"
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    try {
        ctx.startActivity(intent)
    } catch (_: Exception) { /* no-op */ }
}

private fun sendFeedbackEmail(ctx: Context, to: String, subject: String, body: String) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, arrayOf(to))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
    }
    try {
        ctx.startActivity(Intent.createChooser(intent, "שליחת משוב"))
    } catch (_: Exception) { /* no-op */ }
}

private fun markRated(ctx: Context) {
    val sp = ctx.getSharedPreferences("kmi_user", Context.MODE_PRIVATE)
    sp.edit()
        .putBoolean("rate_done", true)
        .putLong("rate_last_prompt_ts", System.currentTimeMillis())
        .apply()
}

/**
 * ב־Android source set הפונקציה תחזיר false, כך שהכפתור לא יוצג.
 * ב־iOS (KMP) אפשר לממש actual שמחזיר true.
 */
@Suppress("KotlinJniMissingFunction") // רק להבהרת הכוונה
private fun isIos(): Boolean = false
