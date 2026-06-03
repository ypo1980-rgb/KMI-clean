package il.kmi.app.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.draw.clip
import il.kmi.app.ui.KmiTopBar
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
    onHome: () -> Unit = {},
    onOpenExercise: ((String) -> Unit)? = null,
    supportEmail: String = "ypo1980@gmail.com",
    playStoreAppId: String = ""
) {
    val ctx = LocalContext.current
    val langManager = remember { AppLanguageManager(ctx) }
    val isEnglish = langManager.getCurrentLanguage() == AppLanguage.ENGLISH

    val screenTitle = if (isEnglish) "Rate Us" else "דרגו אותנו"
    val heroTitle = if (isEnglish) "Enjoying KAMI?" else "אהבתם את KAMI?"
    val heroSubtitle = if (isEnglish) {
        "Your rating helps us improve and grow."
    } else {
        "הדירוג שלכם עוזר לנו להשתפר ולגדול."
    }

    // ✅ מתחילים בלי סימון כדי שהמשתמש יבחר דירוג בעצמו
    var stars by remember { mutableStateOf(0) }
    var feedback by remember { mutableStateOf("") }

    // ✅ דירוג נמוך רק אחרי שהמשתמש באמת לחץ על 1–3
    val lowRating = stars in 1..3

    CompositionLocalProvider(
        LocalLayoutDirection provides if (isEnglish) LayoutDirection.Ltr else LayoutDirection.Rtl
    ) {
        Scaffold(
            topBar = {
                KmiTopBar(
                    title = screenTitle,
                    centerTitle = true,

                    // ✅ מציג אייקון סרגל צד בכותרת כמו בשאר המסכים
                    showMenu = true,

                    // ✅ בלי איקס ובלי חזור
                    onBack = null,

                    // ✅ מאפשר אייקון בית פעיל אם מעבירים אותו מה-NavHost
                    onHome = onHome,

                    // ✅ מאפשר חיפוש גלובלי אם תרצה להשתמש בו מה-KmiTopBar
                    onOpenExercise = onOpenExercise,

                    // ✅ מציג סרגל אייקונים נסתר + מצב מאמן/מתאמן
                    showBottomActions = true,
                    showRoleBadge = true,
                    showModePill = true,

                    // ✅ לא להציג אייקוני חיפוש/בית בכותרת עצמה
                    // אלא רק בסרגל הצד
                    showTopHome = false,
                    showTopSearch = false,
                    showTopShare = false,

                    // ✅ החיפוש לא נעול
                    lockSearch = false,
                    lockHome = false
                )
            },
            containerColor = Color.Transparent
        ) { padding ->

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFF8F5FF),
                                Color(0xFFF1F5FF),
                                Color(0xFFEAF6FF)
                            )
                        )
                    )
                    .padding(padding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            // ✅ מורידים את הכרטיס למטה כמו במסך פורום הסניף
                            .padding(top = 24.dp),
                        color = Color(0xFFEAF2FF),
                        shape = RoundedCornerShape(20.dp),
                        tonalElevation = 2.dp,
                        shadowElevation = 4.dp,
                        border = BorderStroke(
                            1.dp,
                            Color(0xFFD8E3F5)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 22.dp),
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

                            if (stars == 0) {
                                Text(
                                    text = if (isEnglish) {
                                        "Tap a star to choose your rating"
                                    } else {
                                        "לחצו על כוכב כדי לבחור דירוג"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF6B5CA5),
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else if (lowRating) {
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
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(
            space = 10.dp,
            alignment = Alignment.CenterHorizontally
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 1..5) {
            val filled = value > 0 && i <= value

            val circleColor = if (filled) {
                ratingColorForStep(i)
            } else {
                Color(0xFFE1E7F2)
            }

            val starTint = if (filled) {
                Color.White
            } else {
                Color(0xFF7A6AAE)
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(circleColor)
                    .clickable {
                        onChange(i)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (filled) {
                        Icons.Filled.Star
                    } else {
                        Icons.Outlined.StarBorder
                    },
                    contentDescription = if (isEnglish) "$i stars" else "$i כוכבים",
                    tint = starTint,
                    modifier = Modifier.size(25.dp)
                )
            }
        }
    }
}

private fun ratingColorForStep(step: Int): Color {
    return when (step) {
        1 -> Color(0xFFE53935) // אדום
        2 -> Color(0xFFFB8C00) // כתום
        3 -> Color(0xFFFBC02D) // צהוב
        4 -> Color(0xFF8BC34A) // ירוק בהיר
        else -> Color(0xFF2E7D32) // ירוק
    }
}

/* ------------------ Helpers ------------------ */

private fun openPlayStoreForApp(ctx: Context, appId: String) {
    val resolvedAppId = appId.trim().ifBlank { ctx.packageName }

    val uriMarket = Uri.parse("market://details?id=$resolvedAppId")
    val marketIntent = Intent(Intent.ACTION_VIEW, uriMarket).apply {
        addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
    }

    try {
        ctx.startActivity(marketIntent)
    } catch (_: ActivityNotFoundException) {
        val webIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=$resolvedAppId")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            ctx.startActivity(webIntent)
        } catch (_: Exception) {
            Toast.makeText(
                ctx,
                if (AppLanguageManager(ctx).getCurrentLanguage() == AppLanguage.ENGLISH) {
                    "Unable to open the store"
                } else {
                    "לא ניתן לפתוח את החנות"
                },
                Toast.LENGTH_SHORT
            ).show()
        }
    }
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
    } catch (_: Exception) {
        Toast.makeText(
            ctx,
            if (isEnglish) "No email app was found" else "לא נמצאה אפליקציית דוא״ל",
            Toast.LENGTH_SHORT
        ).show()
    }
}

private fun markRated(ctx: Context) {
    val sp = ctx.getSharedPreferences("kmi_user", Context.MODE_PRIVATE)
    sp.edit()
        .putBoolean("rate_done", true)
        .putLong("rate_last_prompt_ts", System.currentTimeMillis())
        .apply()
}
