@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package il.kmi.app.screens

import android.content.SharedPreferences
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import il.kmi.app.ui.KmiTtsManager

private const val PREF_TTS_VOICE = "kmi_tts_voice" // "male" | "female"
private const val VOICE_MALE = "male"
private const val VOICE_FEMALE = "female"

@Composable
fun VoiceSettingsScreen(
    sp: SharedPreferences,
    onBack: () -> Unit
) {
    var selected by remember {
        val raw = sp.getString(PREF_TTS_VOICE, VOICE_MALE) ?: VOICE_MALE
        mutableStateOf(if (raw == VOICE_FEMALE) VOICE_FEMALE else VOICE_MALE)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("הגדרות קול") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Filled.ArrowBack,
                            contentDescription = "חזרה"
                        )
                    }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("בחר/י קול אחיד לכל האפליקציה:", style = MaterialTheme.typography.titleMedium)

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    VoiceRow(
                        selected = selected == VOICE_MALE,
                        label = "גבר",
                        onClick = { selected = VOICE_MALE }
                    )
                    Divider(Modifier.padding(vertical = 8.dp))
                    VoiceRow(
                        selected = selected == VOICE_FEMALE,
                        label = "אישה",
                        onClick = { selected = VOICE_FEMALE }
                    )
                }
            }

            // ... בתוך VoiceSettingsScreen(), לפני ה-Scaffold (או בתחילת ה-Column)
            val ctx = LocalContext.current

            Button(
                onClick = {
                    sp.edit().putString(PREF_TTS_VOICE, selected).apply() // ✅ apply (לא dapply)
                    // ✅ הכל ענן (אנושי): רק init כדי לוודא שהמנהל מוכן
                    KmiTtsManager.init(context = ctx)
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("שמור והחל") }

            OutlinedButton(
                onClick = {
                    // ✅ הכל ענן (אנושי)
                    KmiTtsManager.init(context = ctx)
                    KmiTtsManager.speak("זוהי בדיקת קול לפי הבחירה שלך.")
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("השמעת בדיקה") }

            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun VoiceRow(
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        androidx.compose.foundation.layout.Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
