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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager

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
    supportEmail: String = "support@kmi-app.example",
    playStoreAppId: String = "il.kmi.app",
    appStoreAppId: String = "0000000000"
) {
    val ctx = LocalContext.current
    val langManager = remember { AppLanguageManager(ctx) }
    val isEnglish = langManager.getCurrentLanguage() == AppLanguage.ENGLISH

    val screenTitle = if (isEnglish) "Rate Us" else "דרגו אותנו"
    val heroTitle = if (isEnglish) "Enjoying KMI?" else "אהבתם את KMI?"
    val heroSubtitle = if (isEnglish) {
        "Your rating helps us improve and grow."
    } else {
        "הדירוג שלכם עוזר לנו להשתפר ולגדול."
    }

    var stars by remember { mutableStateOf(5) }
    var feedback by remember { mutableStateOf("") }
    val lowRating = stars <= 3

    CompositionLocalProvider(
        LocalLayoutDirection provides if (isEnglish) LayoutDirection.Ltr else LayoutDirection.Rtl
    ) {
        Scaffold(
            topBar = {
                SideScreenTopBar(
                    title = screenTitle,
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
                        text = heroTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = if (isEnglish) TextAlign.Start else TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = heroSubtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = if (isEnglish) TextAlign.Start else TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )

                    StarsRow(
                        value = stars,
                        onChange = { stars = it },
                        isEnglish = isEnglish
                    )

                    if (lowRating) {
                        OutlinedTextField(
                            value = feedback,
                            onValueChange = { feedback = it },
                            label = {
                                Text(if (isEnglish) "What can we improve?" else "מה נוכל לשפר?")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            textStyle = LocalTextStyle.current.copy(
                                textAlign = if (isEnglish) TextAlign.Start else TextAlign.Right
                            )
                        )

                        Button(
                            onClick = {
                                sendFeedbackEmail(
                                    ctx,
                                    supportEmail,
                                    if (isEnglish) "App feedback ($stars★)" else "משוב מהאפליקציה (דירוג $stars★)",
                                    feedback
                                )
                                markRated(ctx)
                                onClose()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(if (isEnglish) "Send Feedback" else "שליחת משוב")
                        }

                        Text(
                            text = if (isEnglish) "We read every message 🙏" else "אנחנו קוראים כל משוב 🙏",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = if (isEnglish) TextAlign.Start else TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
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
                            Text(
                                if (isEnglish) {
                                    "Rate Us on Google Play"
                                } else {
                                    "דרגו אותנו בחנות Google Play"
                                }
                            )
                        }

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
                                Text(if (isEnglish) "Open in App Store" else "פתחו ב-App Store")
                            }
                        }

                        TextButton(
                            onClick = {
                                sendFeedbackEmail(
                                    ctx,
                                    supportEmail,
                                    if (isEnglish) "Feedback ($stars★)" else "משוב (דירוג $stars★)",
                                    ""
                                )
                            }
                        ) {
                            Text(if (isEnglish) "Or send feedback instead" else "או שלחו לנו משוב במקום")
                        }
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
    onChange: (Int) -> Unit,
    isEnglish: Boolean
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
                    onChange(i)
                }
            ) {
                Icon(
                    imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = if (isEnglish) "$i stars" else "$i כוכבים"
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
    val isEnglish = AppLanguageManager(ctx).getCurrentLanguage() == AppLanguage.ENGLISH

    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, arrayOf(to))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
    }
    try {
        ctx.startActivity(
            Intent.createChooser(
                intent,
                if (isEnglish) "Send feedback" else "שליחת משוב"
            )
        )
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
