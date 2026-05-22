package il.kmi.app.screens

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager

@Composable
fun InitialLanguageScreen(
    entrySp: SharedPreferences? = null,
    onLanguageSelected: () -> Unit
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val langManager = remember { AppLanguageManager(ctx) }

    // חשוב: משתמשים באותו SharedPreferences שמגיע מ-MainNavHost.
    // אם משום מה לא הגיע, נופלים ל-kmi_user.
    val sp = remember(entrySp, ctx) {
        entrySp ?: ctx.getSharedPreferences("kmi_user", Context.MODE_PRIVATE)
    }

    // גיבוי: יש מסכים שקוראים ישירות מ-kmi_user, לכן נשמור גם שם.
    val userSp = remember(ctx) {
        ctx.getSharedPreferences("kmi_user", Context.MODE_PRIVATE)
    }

    val clickLocked = remember { mutableStateOf(false) }


    fun selectLanguage(lang: AppLanguage) {
        if (clickLocked.value) {
            return
        }

        clickLocked.value = true

        // ✅ קודם שומרים שהשפה כבר נבחרה.
        // שומרים גם ב-sp שהגיע מ-MainNavHost וגם ב-kmi_user כדי למנוע מצב
        // שבו Activity recreate קורא SharedPreferences אחר ורואה v4=false.
        fun SharedPreferences.markSelected(): Boolean {
            return edit()
                .putBoolean("initial_language_selected", true)
                .putBoolean("initial_language_selected_v2", true)
                .putBoolean("initial_language_selected_v3", true)
                .putBoolean("initial_language_selected_v4", true)
                .putString("initial_language_code", lang.name)
                .commit()
        }

        sp.markSelected()
        userSp.markSelected()

        // ✅ רק אחרי שהדגלים נשמרו, משנים את שפת האפליקציה.
        langManager.setLanguage(lang)

        // מעבר למסך הבא
        onLanguageSelected()
    }

    val background = Brush.linearGradient(
        colors = listOf(
            Color(0xFF0F172A),
            Color(0xFF1E293B),
            Color(0xFF020617)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background),
        contentAlignment = Alignment.Center
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "בחר שפה\nChoose Language",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(40.dp))

            // 🇮🇱 עברית
            LanguageButton(
                text = "עברית 🇮🇱",
                enabled = !clickLocked.value,
                onClick = { selectLanguage(AppLanguage.HEBREW) }
            )

            Spacer(Modifier.height(18.dp))

            // 🇺🇸 English
            LanguageButton(
                text = "English 🇺🇸",
                enabled = !clickLocked.value,
                onClick = { selectLanguage(AppLanguage.ENGLISH) }
            )
        }
    }
}

@Composable
private fun LanguageButton(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clickable(enabled = enabled) { onClick() },
        shape = RoundedCornerShape(18.dp),
        shadowElevation = if (enabled) 10.dp else 2.dp,
        color = if (enabled) Color(0xFF1E40AF) else Color(0xFF334155)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        if (enabled) {
                            listOf(
                                Color(0xFF6366F1),
                                Color(0xFF3B82F6),
                                Color(0xFF06B6D4)
                            )
                        } else {
                            listOf(
                                Color(0xFF475569),
                                Color(0xFF334155),
                                Color(0xFF1E293B)
                            )
                        }
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}