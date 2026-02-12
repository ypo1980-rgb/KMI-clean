package il.kmi.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import il.kmi.shared.domain.Belt
import il.kmi.app.ui.KmiTtsManager
import il.kmi.app.ui.ext.color
import il.kmi.app.ui.ext.lightColor
import kotlinx.coroutines.delay

// ✅ קבוע אחד למבחן (לא דיאלוג, לא שינוי, לא כפילויות)
private const val EXAM_SECONDS_PER_EXERCISE = 20

/** מזהה אם טקסט נראה כמו tag (לטיני/מספרים/_,:) ולא כמו עברית */
private fun looksLikeTag(s: String): Boolean {
    val t = s.trim()
    if (t.isBlank()) return false
    val hasHebrew = t.any { it in '\u0590'..'\u05FF' }
    if (hasHebrew) return false
    return t.any { it.isLetterOrDigit() } && t.all { it.isLetterOrDigit() || it in "_:-" }
}

/** מחזיר שם תרגיל “נקי” להצגה/הקראה */
private fun toDisplayItem(raw: String): String {
    val s = raw.trim()

    // מקרה נפוץ: tag::שם או שם::tag
    if ("::" in s) {
        val left = s.substringBefore("::").trim()
        val right = s.substringAfterLast("::").trim()
        return when {
            looksLikeTag(left) && !looksLikeTag(right) -> right
            !looksLikeTag(left) && looksLikeTag(right) -> left
            else -> right
        }.ifBlank { s }
    }

    // לפעמים מגיע כ-2 שורות: שורה 1 tag, שורה 2 שם
    val lines = s.lines().map { it.trim() }.filter { it.isNotEmpty() }
    if (lines.size >= 2 && looksLikeTag(lines.first())) {
        val rest = lines.drop(1).joinToString(" ").trim()
        if (rest.isNotBlank()) return rest
    }

    // ניקוי קל: אם מתחיל ב-tag ואז רווח
    return s.replace(Regex("^[a-zA-Z0-9:_-]{2,}\\s+"), "").ifBlank { s }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamScreen(
    belt: Belt,
    onBack: () -> Unit,
    excludedItems: Set<String> = emptySet(),
    onHome: () -> Unit = {},
    onSearch: () -> Unit = {}
) {
    val context = LocalContext.current

    // ✅ רב-פלטפורמי (כרגע No-Op כדי לקמפל בלי תלות ב-R)
    val soundPlayer = remember { il.kmi.shared.platform.PlatformSoundPlayer(context) }

    var examStarted by rememberSaveable { mutableStateOf(false) }

    // ----- שליפת פריטי המבחן (ללא רפלקציה / ללא JVM) -----
    val baseItems: List<String> = remember(belt) {
        il.kmi.shared.exam.ExamFacade.buildExamItems(
            beltId = belt.id,
            topicTitlesProvider = il.kmi.shared.exam.ExamFacade.TopicTitlesProvider { beltId ->
                val appBelt = Belt.fromId(beltId) ?: belt
                runCatching { il.kmi.app.search.KmiSearchBridge.topicTitlesFor(appBelt) }
                    .getOrDefault(emptyList())
            },
            itemsProvider = il.kmi.shared.exam.ExamFacade.ItemsProvider { beltId, topicTitle ->
                val appBelt = Belt.fromId(beltId) ?: belt
                runCatching { il.kmi.app.search.KmiSearchBridge.itemsFor(appBelt, topicTitle) }
                    .getOrDefault(emptyList())
            }
        )
    }

    val items: List<String> = remember(baseItems, excludedItems) {
        baseItems
            .filterNot { it in excludedItems }
            .shuffled()
    }

    val displayItems: List<String> = remember(items) {
        items.map(::toDisplayItem)
    }

    var currentIndex by remember { mutableStateOf(0) }
    var timeLeft by remember { mutableStateOf(EXAM_SECONDS_PER_EXERCISE) }
    var isRunning by remember { mutableStateOf(false) } // מתחיל false, נדליק אחרי "letsgo"
    var isMuted by rememberSaveable { mutableStateOf(false) }

    // ✅ Guard: אם items השתנתה והאינדקס יצא מהטווח – מתקנים
    LaunchedEffect(items.size) {
        currentIndex = when {
            items.isEmpty() -> 0
            currentIndex in items.indices -> currentIndex
            else -> 0
        }
    }

    // אתחול TTS + (אופציונלי) letsgo ואז תחילת המבחן
    LaunchedEffect(Unit) {
        // ✅ רב-פלטפורמי (Android/iOS)
        il.kmi.shared.tts.KmiTtsManager.init(
            il.kmi.shared.tts.PlatformContext(context)
        )

        runCatching { soundPlayer.play("letsgo") }

        examStarted = true
        isRunning = true
    }

    // מקריא אחרי תחילת המבחן ובכל מעבר לתרגיל חדש
    LaunchedEffect(currentIndex, items, isMuted, examStarted) {
        if (examStarted && !isMuted && items.isNotEmpty() && currentIndex in items.indices) {
            delay(300)
            KmiTtsManager.speak(displayItems[currentIndex])
        }
    }

    // ✅ טיימר: תמיד 20 שניות לתרגיל, ובסוף המבחן עוצר
    LaunchedEffect(currentIndex, isRunning, items, examStarted) {
        if (examStarted && isRunning && items.isNotEmpty() && currentIndex in items.indices) {
            timeLeft = EXAM_SECONDS_PER_EXERCISE

            while (timeLeft > 0 && isRunning) {
                delay(1000)
                timeLeft--
            }

            if (!isRunning) return@LaunchedEffect

            if (timeLeft == 0) {
                if (currentIndex < items.lastIndex) {
                    currentIndex++
                } else {
                    // ✅ נגמר המבחן (אין עוד תרגילים) — עוצרים
                    isRunning = false
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            KmiTtsManager.stop()
            runCatching { soundPlayer.release() }
        }
    }

    val total = items.size.coerceAtLeast(1)
    val progress = (currentIndex + 1).toFloat() / total.toFloat()

    Scaffold(
        topBar = {
            il.kmi.app.ui.KmiTopBar(
                title = "מבחן מסכם – ${belt.heb}",
                showTopHome = false,
                showTopSearch = false,
                showBottomActions = false,
            )
        }
    ) { padding ->

        val backgroundBrush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFF7F2FF),
                Color(0xFFECE4FF),
                Color(0xFFE3F2FF)
            )
        )

        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundBrush)
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("אין תרגילים זמינים")
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Surface(
                shape = MaterialTheme.shapes.large,
                color = belt.lightColor.copy(alpha = 0.20f),
                tonalElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = belt.heb,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "תרגיל ${currentIndex + 1} מתוך $total",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }

                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = belt.color.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = String.format("%02d", timeLeft),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                color = belt.color,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp),
                        trackColor = Color.White.copy(alpha = 0.4f),
                        color = belt.color
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        repeat(items.size) { idx ->
                            val isCurrent = idx == currentIndex
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .background(
                                        color = if (isCurrent) belt.color else belt.color.copy(alpha = 0.25f),
                                        shape = MaterialTheme.shapes.large
                                    )
                            )
                        }
                    }
                }
            }

            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = Color.White,
                tonalElevation = 3.dp,
                shadowElevation = 3.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = displayItems[currentIndex],
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                isMuted = !isMuted
                                if (isMuted) {
                                    KmiTtsManager.stop()
                                } else if (currentIndex in items.indices) {
                                    KmiTtsManager.speak(displayItems[currentIndex])
                                }
                            },
                            modifier = Modifier.size(50.dp)
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                                contentDescription = if (isMuted) "בטל השתק" else "השתק",
                                tint = belt.color
                            )
                        }

                        Button(
                            onClick = {
                                KmiTtsManager.stop()
                                if (currentIndex < items.lastIndex) currentIndex++
                            },
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Text("דלג")
                        }
                    }
                }
            }

            Button(
                onClick = {
                    KmiTtsManager.stop()
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEEEEEE),
                    contentColor = Color.Black
                )
            ) {
                Text("סיום מבחן", fontWeight = FontWeight.Bold)
            }
        }
    }
}
