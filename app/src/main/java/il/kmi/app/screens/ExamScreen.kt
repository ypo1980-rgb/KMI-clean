package il.kmi.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.material3.LocalTextStyle
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
import il.kmi.app.favorites.FavoritesStore
import il.kmi.app.ui.KmiTtsManager
import il.kmi.app.ui.ext.color
import il.kmi.app.ui.ext.lightColor
import kotlinx.coroutines.delay

// ✅ קבוע אחד למבחן (לא דיאלוג, לא שינוי, לא כפילויות)
private const val EXAM_SECONDS_PER_EXERCISE = 20

private fun normalizeFavoriteId(raw: String): String =
    raw.substringAfter("::", raw)
        .substringAfter(":", raw)
        .trim()

private fun findExplanationForExam(
    belt: Belt,
    rawItem: String
): String {
    val display = il.kmi.shared.questions.model.util.ExerciseTitleFormatter
        .displayName(rawItem)
        .ifBlank { rawItem }
        .trim()

    fun String.clean(): String =
        this.replace('-', ' ')
            .replace("־", " ")
            .replace("  ", " ")
            .trim()

    val candidates = listOf(
        rawItem,
        display,
        display.clean(),
        display.substringBefore("(").trim().clean()
    ).distinct()

    for (candidate in candidates) {
        val got = il.kmi.app.domain.Explanations.get(belt, candidate).trim()
        if (got.isNotBlank()) return got
    }

    return "אין כרגע הסבר לתרגיל הזה."
}

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
    val notePrefs = remember(context) {
        context.getSharedPreferences("kmi_exercise_notes", android.content.Context.MODE_PRIVATE)
    }
    val favorites: Set<String> by FavoritesStore.favoritesFlow.collectAsState(initial = emptySet())

    // ✅ רב-פלטפורמי (כרגע No-Op כדי לקמפל בלי תלות ב-R)
    val soundPlayer = remember { il.kmi.shared.platform.PlatformSoundPlayer(context) }

    var examStarted by rememberSaveable { mutableStateOf(false) }
    var showHelp by rememberSaveable { mutableStateOf(false) }
    var pickedSearchKey by rememberSaveable { mutableStateOf<String?>(null) }

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
                showBottomActions = true,
                onHome = onHome,
                onPickSearchResult = { key ->
                    pickedSearchKey = key
                },
                lockSearch = false
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
                    .weight(1f),
                onClick = { showHelp = true }
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

                        OutlinedButton(
                            onClick = { showHelp = true },
                            shape = MaterialTheme.shapes.large
                        ) {
                            Text("עזרה")
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

        pickedSearchKey?.let { key ->

            val (b, topic, item) = il.kmi.app.screens.parseSearchKey(key)

            val explanation = remember(b, item, topic) {
                findExplanationForExam(b, item)
            }

            val favId = remember(item) { normalizeFavoriteId(item) }
            val isFav = favorites.contains(favId)

            val noteKey = remember(b, topic, favId) {
                "note_${b.id}_${topic.trim()}_${favId}"
            }

            var noteText by remember(noteKey) {
                mutableStateOf(notePrefs.getString(noteKey, "").orEmpty())
            }

            var showNoteEditor by remember { mutableStateOf(false) }

            fun toggleFav() {
                if (item.isBlank()) return
                FavoritesStore.toggle(favId)
            }

            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

            ModalBottomSheet(
                onDismissRequest = { pickedSearchKey = null },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { toggleFav() }) {
                                if (isFav) {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = "הסר ממועדפים",
                                        tint = Color(0xFFFFC107)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.StarBorder,
                                        contentDescription = "הוסף למועדפים"
                                    )
                                }
                            }

                            IconButton(onClick = { showNoteEditor = true }) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "ערוך הערה",
                                    tint = Color(0xFF42A5F5)
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = item,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "$topic • ${b.heb}",
                                style = MaterialTheme.typography.labelMedium,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.End
                    ) {

                        Text(
                            text = explanation,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (noteText.isNotBlank()) {

                            Spacer(Modifier.height(12.dp))
                            HorizontalDivider()
                            Spacer(Modifier.height(10.dp))

                            Text(
                                text = "הערה של המתאמן:",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(Modifier.height(6.dp))

                            Text(
                                text = noteText,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { pickedSearchKey = null }) {
                            Text("סגור")
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                }
            }

            if (showNoteEditor) {
                AlertDialog(
                    onDismissRequest = { showNoteEditor = false },
                    title = {
                        Text(
                            text = "הערה לתרגיל",
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    text = {
                        OutlinedTextField(
                            value = noteText,
                            onValueChange = { noteText = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Right),
                            label = { Text("כתוב הערה") }
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                notePrefs.edit()
                                    .putString(noteKey, noteText.trim())
                                    .apply()
                                showNoteEditor = false
                            }
                        ) {
                            Text("שמור")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showNoteEditor = false }) {
                            Text("ביטול")
                        }
                    }
                )
            }
        }

        if (showHelp && currentIndex in items.indices) {
            val rawItem = items[currentIndex]
            val displayItem = displayItems[currentIndex]

            val explanation = remember(belt, rawItem) {
                findExplanationForExam(belt, rawItem)
            }

            val favId = remember(rawItem) { normalizeFavoriteId(rawItem) }
            val isFav = favorites.contains(favId)

            val noteKey = remember(belt, favId) {
                "note_${belt.id}_exam_${favId}"
            }
            var noteText by remember(noteKey) {
                mutableStateOf(notePrefs.getString(noteKey, "").orEmpty())
            }
            var showNoteEditor by remember { mutableStateOf(false) }

            fun toggleFav() {
                if (rawItem.isBlank()) return
                FavoritesStore.toggle(favId)
            }

            val helpSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

            ModalBottomSheet(
                onDismissRequest = { showHelp = false },
                sheetState = helpSheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { toggleFav() }) {
                                if (isFav) {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = "הסר ממועדפים",
                                        tint = Color(0xFFFFC107)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.StarBorder,
                                        contentDescription = "הוסף למועדפים"
                                    )
                                }
                            }

                            IconButton(onClick = { showNoteEditor = true }) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "ערוך הערה",
                                    tint = Color(0xFF42A5F5)
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = displayItem,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "מבחן מסכם • ${belt.heb}",
                                style = MaterialTheme.typography.labelMedium,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = explanation,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (noteText.isNotBlank()) {
                            Spacer(Modifier.height(12.dp))
                            HorizontalDivider()
                            Spacer(Modifier.height(10.dp))

                            Text(
                                text = "הערה של המתאמן:",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(Modifier.height(6.dp))

                            Text(
                                text = noteText,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showHelp = false }) {
                            Text("סגור")
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                }
            }

            if (showNoteEditor) {
                AlertDialog(
                    onDismissRequest = { showNoteEditor = false },
                    title = {
                        Text(
                            text = "הערה לתרגיל",
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    text = {
                        OutlinedTextField(
                            value = noteText,
                            onValueChange = { noteText = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Right),
                            label = { Text("כתוב הערה") }
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                notePrefs.edit()
                                    .putString(noteKey, noteText.trim())
                                    .apply()
                                showNoteEditor = false
                            }
                        ) {
                            Text("שמור")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showNoteEditor = false }) {
                            Text("ביטול")
                        }
                    }
                )
            }
        }
    }
}
