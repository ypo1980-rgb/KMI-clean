package il.kmi.app.reminders

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import il.kmi.app.MainActivity
import il.kmi.app.domain.Explanations
import il.kmi.shared.domain.Belt
import il.kmi.shared.reminders.DailyExercisePicker
import il.kmi.app.favorites.FavoritesStore
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class DailyReminderCardActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val belt = intent.getStringExtra("daily_reminder_belt_id") ?: ""
        val topic = intent.getStringExtra("daily_reminder_topic") ?: ""
        val item = intent.getStringExtra("daily_reminder_item") ?: ""
        val explanation = intent.getStringExtra("daily_reminder_explanation") ?: ""
        val extraCount = intent.getIntExtra("daily_reminder_extra_count", 0)

        setContent {

            val favorites by FavoritesStore.favoritesFlow.collectAsState(initial = emptySet())

            ReminderCardUI(
                belt = belt,
                topic = topic,
                item = item,
                explanation = explanation,
                extraCount = extraCount,
                isFavorite = favorites.contains(item),
                onToggleFavorite = {
                    FavoritesStore.toggle(item)
                },
                onClose = { finish() },
                onOpenApp = {
                    startActivity(
                        Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                    )
                    finish()
                },
                onOpenExactAlarmSettings = {
                    DailyReminderPowerHelper.openExactAlarmSettings(this)
                },
                onOpenBatteryOptimizationSettings = {
                    DailyReminderPowerHelper.openBatteryOptimizationSettings(this)
                },
                onAnotherExercise = {
                    val beltEnum = Belt.fromId(belt)
                    if (beltEnum != null) {
                        val picker = DailyExercisePicker()
                        val nextPicked = picker.pickNextExerciseForUser(
                            registeredBelt = previousBeltForTarget(beltEnum),
                            lastItemKey = "${beltEnum.name}|$topic|$item"
                        )

                        if (nextPicked != null) {
                            val nextExplanation = Explanations.get(nextPicked.belt, nextPicked.item)

                            startActivity(
                                Intent(this, DailyReminderCardActivity::class.java).apply {
                                    putExtra("daily_reminder_belt_id", nextPicked.belt.id)
                                    putExtra("daily_reminder_topic", nextPicked.topic)
                                    putExtra("daily_reminder_item", nextPicked.item)
                                    putExtra("daily_reminder_explanation", nextExplanation)
                                    putExtra("daily_reminder_extra_count", extraCount + 1)
                                }
                            )
                            finish()
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun ReminderCardUI(
    belt: String,
    topic: String,
    item: String,
    explanation: String,
    extraCount: Int,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClose: () -> Unit,
    onOpenApp: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onOpenBatteryOptimizationSettings: () -> Unit,
    onAnotherExercise: () -> Unit
) {
    var localFavorite by remember(belt, topic, item, isFavorite) {
        mutableStateOf(isFavorite)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xAA000000)),
        contentAlignment = Alignment.Center
    ) {

        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
        ) {

            Column(
                modifier = Modifier.padding(20.dp)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "התרגיל היומי שלך",
                        fontSize = 20.sp
                    )

                    TextButton(
                        onClick = {
                            localFavorite = !localFavorite
                            onToggleFavorite()
                        }
                    ) {
                        Text(if (localFavorite) "★" else "☆")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "$belt • $topic",
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = item,
                    fontSize = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = explanation,
                    fontSize = 14.sp
                )

                if (extraCount < 3) {

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onAnotherExercise,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("תרגיל נוסף להיום")
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedButton(
                    onClick = onOpenExactAlarmSettings,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("אפשר תזמון מדויק")
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onOpenBatteryOptimizationSettings,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("הגדרות חיסכון סוללה")
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onClose,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("סגור")
                    }

                    Button(
                        onClick = onOpenApp,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("מעבר לאפליקציה")
                    }
                }
            }
        }
    }
}

private fun previousBeltForTarget(targetBelt: Belt): Belt {
    return when (targetBelt) {
        Belt.YELLOW -> Belt.WHITE
        Belt.ORANGE -> Belt.YELLOW
        Belt.GREEN -> Belt.ORANGE
        Belt.BLUE -> Belt.GREEN
        Belt.BROWN -> Belt.BLUE
        Belt.BLACK -> Belt.BROWN
        Belt.WHITE -> Belt.WHITE
    }
}

