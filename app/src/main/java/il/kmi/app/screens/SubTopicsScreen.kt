package il.kmi.app.screens

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import il.kmi.shared.domain.Belt
import il.kmi.app.domain.AppSubTopicRegistry
import il.kmi.app.domain.ContentRepo
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import il.kmi.shared.questions.model.util.ExerciseTitleFormatter
import androidx.compose.material.icons.outlined.Info
import androidx.compose.ui.draw.clip
import il.kmi.app.ui.ext.color
import il.kmi.app.ui.ext.lightColor

/**
 * מסך שמציג את כל תתי־הנושאים של נושא מסוים בחגורה מסוימת.
 * כל כפתור = תת־נושא. למטה כתוב כמה תרגילים יש בו.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubTopicsScreen(
    belt: Belt,
    topic: String,
    onBack: () -> Unit,                 // נשאר בפרמטרים אך לא מוצג בכותרת
    onHome: () -> Unit,
    onOpenSubTopic: (String) -> Unit,
    onOpenExercise: (String) -> Unit
) {
    // ✅ מפענחים את שם הנושא (למקרה שעבר דרך ה-URL Encoded)
    val topicDecoded = remember(topic) { Uri.decode(topic).trim() }

    // שולפים את תתי־הנושאים...
    val subs: List<String> = remember(belt, topicDecoded) {

        // 1) קודם מנסים מה-Registry שלך (אם יש שם ערכים אמיתיים)
        val fromShared = runCatching {
            AppSubTopicRegistry.getSubTopicsFor(belt, topicDecoded)
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
        }.getOrElse { emptyList() }

        if (fromShared.isNotEmpty()) {
            Log.d("KMI-SubTopics", "fromShared: belt=${belt.id}, topic='$topicDecoded', subs=${fromShared.size}")
            return@remember fromShared
        }

        // 2) אחרת — מה-ContentRepo החדש (רשימת כותרות תתי-נושא)
        val fromRepo = runCatching {
            ContentRepo.listSubTopicTitles(belt, topicDecoded)
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
        }.getOrElse { emptyList() }

        Log.d("KMI-SubTopics", "fromRepo: belt=${belt.id}, topic='$topicDecoded', subs=${fromRepo.size}")
        fromRepo
    }

    // ✅ מסנן "תת־נושא מזויף" שבו subTopic == topic (כלומר אין באמת תתי־נושאים)
    val realSubs: List<String> = remember(subs, topicDecoded) {
        val t = topicDecoded.trim()
        subs.asSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .filter { it != t }          // ✅ זה העיקר
            .distinct()
            .toList()
    }

    // ✅ תת־נושא "פייק" = תת־נושא יחיד שזהה בדיוק לשם הנושא → מתייחסים כאילו אין תתי־נושאים
    val hasRealSubs = remember(subs, topicDecoded) {
        when {
            subs.isEmpty() -> false
            subs.size > 1 -> true
            else -> subs.first().trim() != topicDecoded.trim()
        }
    }

    val onBeltColor = if (belt.color.luminance() < 0.5f) Color.White else Color.Black
    val buttonSpacing = 12.dp

    // כמו במסך הבית: מפתח תרגיל שנבחר מהחיפוש → פותח דיאלוג הסבר יחיד
    var pickedKey by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            il.kmi.app.ui.KmiTopBar(
                title = "תתי־נושאים – $topicDecoded",
                onHome = onHome,
                showTopHome = false,
                showTopSearch = false,
                showBottomActions = true,
                lockSearch = false,
                onSearch = null, // לא לפתוח דיאלוג נוסף מהמסך
                onPickSearchResult = { key -> pickedKey = key }, // כמו בבית
                centerTitle = false
            )
        }
    ) { padding ->

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = belt.lightColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(buttonSpacing),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!hasRealSubs) {

                    // ✅ אין תתי־נושאים אמיתיים → מציגים ישירות את התרגילים של הנושא + אייקון הסבר
                    val items: List<String> = remember(belt, topicDecoded) {
                        val fromRepo = runCatching {

                            // תרגילים ישירים (אם קיימים)
                            val direct = ContentRepo.listItemTitles(
                                belt = belt,
                                topicTitle = topicDecoded,
                                subTopicTitle = null
                            )

                            // ואם בפועל יש תתי-נושאים — נמשוך גם אותם כדי לא לפספס
                            val subsTitles = ContentRepo.listSubTopicTitles(belt, topicDecoded)
                            val viaSubs = subsTitles.flatMap { st ->
                                ContentRepo.listItemTitles(
                                    belt = belt,
                                    topicTitle = topicDecoded,
                                    subTopicTitle = st
                                )
                            }

                            (direct + viaSubs)
                        }.getOrDefault(emptyList())

                        fromRepo
                            .map { ExerciseTitleFormatter.displayName(it).ifBlank { it }.trim() }
                            .filter { it.isNotBlank() }
                            .distinct()
                    }

                    var explain by rememberSaveable { mutableStateOf<String?>(null) }

                    if (items.isEmpty()) {
                        Text(
                            "לא נמצאו תתי־נושאים או תרגילים עבור “$topicDecoded”",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        items.forEach { itemName ->
                            ExerciseRowWithInfo(
                                belt = belt,
                                itemName = itemName,
                                accent = MaterialTheme.colorScheme.primary,
                                onExplain = { _, item -> explain = item },
                                onOpenExercise = onOpenExercise
                            )
                            Spacer(Modifier.height(6.dp))
                        }
                    }

                    // ✅ דיאלוג הסבר לתרגיל (עובד גם בלי תתי־נושאים)
                    explain?.let { item ->
                        val explanation = remember(belt, topicDecoded, item) {
                            findExplanationForHit(
                                belt = belt,
                                rawItem = item,
                                topic = topicDecoded
                            )
                        }

                        AlertDialog(
                            onDismissRequest = { explain = null },
                            confirmButton = { TextButton(onClick = { explain = null }) { Text("סגור") } },
                            title = {
                                Text(
                                    text = item,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            text = {
                                Text(
                                    text = explanation,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        )
                    }

                } else {
                    realSubs.forEach { subTitleRaw ->
                    val subTitle = subTitleRaw.trim()

                        // כמה תרגילים יש לתת־נושא הזה?
                        val itemCount by remember(belt, topicDecoded, subTitle) {
                            mutableStateOf(
                                AppSubTopicRegistry
                                    .getItemsFor(belt, topicDecoded, subTitle)
                                    .takeIf { it.isNotEmpty() }
                                    ?.size
                                    ?: ContentRepo.listItemTitles(
                                        belt = belt,
                                        topicTitle = topicDecoded,
                                        subTopicTitle = subTitle
                                    ).size
                            )
                        }

                        Button(
                            onClick = { onOpenSubTopic(subTitle) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 88.dp),
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = belt.color,
                                contentColor = onBeltColor
                            ),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = subTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Right,
                                    maxLines = 2,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = "$itemCount תרגילים",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ===== דיאלוג הסבר תרגיל (כמו במסך הבית) =====
    pickedKey?.let { key ->
        val (beltHit, topicHit, itemHit) = parseSearchKey(key)
        val displayName = ExerciseTitleFormatter.displayName(itemHit).ifBlank { itemHit }.trim()

        val explanation = remember(beltHit, itemHit) {
            findExplanationForHit(
                belt = beltHit,
                rawItem = itemHit,
                topic = topicHit
            )
        }

        var isFav by remember { mutableStateOf(false) }

        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pickedKey = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "(${beltHit.heb})",
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    IconButton(onClick = { isFav = !isFav }) {
                        if (isFav) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = "מועדף",
                                tint = Color(0xFFFFC107)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.StarBorder,
                                contentDescription = "הוסף למועדפים"
                            )
                        }
                    }
                }
            },
            text = {
                Text(
                    text = explanation,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = { pickedKey = null }) { Text("סגור") }
            }
        )
    }
    // ===== סוף דיאלוג =====
}

/* ========= עזר: לפרק מפתח חיפוש "belt|topic|item" ========= */
private fun parseSearchKey(key: String): Triple<il.kmi.shared.domain.Belt, String, String> {
    val parts = when {
        "|" in key  -> key.split("|", limit = 3)
        "::" in key -> key.split("::", limit = 3)
        "/" in key  -> key.split("/", limit = 3)
        else        -> listOf("", "", "")
    }.let { (it + listOf("", "", "")).take(3) }

    val belt  = il.kmi.shared.domain.Belt.fromId(parts[0]) ?: il.kmi.shared.domain.Belt.WHITE
    val topic = parts[1]
    val item  = parts[2]
    return Triple(belt, topic, item)
}

/* ========= עזר: למצוא הסבר אמיתי מתוך Explanations ========= */
private fun findExplanationForHit(
    belt: il.kmi.shared.domain.Belt,
    rawItem: String,
    topic: String
): String {
    val display = ExerciseTitleFormatter.displayName(rawItem).ifBlank { rawItem }.trim()
    fun String.clean() = replace('–', '-').replace('־', '-').replace("  ", " ").trim()

    // ✅ Explanations עובדים מול ה-Belt של shared (אחרי האיחוד), אין יותר app.domain.Belt
    val appBelt = belt

    val candidates = buildList {
        add(rawItem)
        add(display)
        add(display.clean())
        add(display.substringBefore("(").trim().clean())
    }.distinct()

    for (c in candidates) {
        val got = il.kmi.app.domain.Explanations.get(appBelt, c).trim()
        if (got.isNotBlank()
            && !got.startsWith("הסבר מפורט על")
            && !got.startsWith("אין כרגע")
        ) {
            return got.split("::")
                .map { it.trim() }
                .lastOrNull { it.isNotBlank() }
                ?: got.trim()
        }
    }
    return "אין כרגע הסבר לתרגיל הזה."
}

/* ---------------------- ✅ NEW: שורת תרגיל עם אייקון הסבר ---------------------- */
@Composable
private fun ExerciseRowWithInfo(
    belt: Belt,
    itemName: String,
    accent: Color,
    onExplain: (Belt, String) -> Unit,
    onOpenExercise: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        tonalElevation = 1.dp
    ) {
        // ✅ LTR רק לשורה הזאת כדי שהאייקון יהיה בצד שמאל אמיתי
        CompositionLocalProvider(
            androidx.compose.ui.platform.LocalLayoutDirection provides
                    androidx.compose.ui.unit.LayoutDirection.Ltr
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onExplain(belt, itemName) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "הסבר",
                        tint = accent.copy(alpha = 0.95f)
                    )
                }

                // ✅ הטקסט עצמו RTL כדי שהעברית תישאר מיושרת לימין
                CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalLayoutDirection provides
                            androidx.compose.ui.unit.LayoutDirection.Rtl
                ) {
                    Text(
                        text = itemName,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Right,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 6.dp)
                            .clickable { onOpenExercise(itemName) }
                    )
                }
            }
        }
    }
}
