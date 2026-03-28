package il.kmi.app.screens

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import il.kmi.shared.domain.Belt
import il.kmi.app.domain.AppSubTopicRegistry
import il.kmi.app.domain.ContentRepo
import il.kmi.shared.domain.content.HardSectionsCatalog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import il.kmi.shared.questions.model.util.ExerciseTitleFormatter
import androidx.compose.material.icons.outlined.Info
import androidx.compose.ui.draw.clip
import il.kmi.app.ui.ext.color
import il.kmi.app.ui.ext.lightColor
import il.kmi.shared.domain.content.HardSectionsCatalog.itemsFor
import il.kmi.shared.domain.content.HardSectionsCatalog.totalItemsCount


private fun topicLookupAliases(topicTitle: String): List<String> {
    val raw = topicTitle.trim()

    val splitParts = raw
        .replace("/", " / ")
        .replace("־", " ")
        .split("ו", "/", ",")
        .map { it.trim() }
        .filter { it.isNotBlank() && it != raw }

    return when (raw) {
        "בלימות וגלגולים" -> listOf(
            "בלימות וגלגולים",
            "גלגולים ובלימות",
            "בלימות",
            "גלגולים",
            "נפילות וגלגולים",
            "נפילות",
            "גלגול",
            "בלימה"
        )

        "גלגולים ובלימות" -> listOf(
            "גלגולים ובלימות",
            "בלימות וגלגולים",
            "בלימות",
            "גלגולים",
            "נפילות וגלגולים",
            "נפילות",
            "גלגול",
            "בלימה"
        )

        else -> listOf(raw) + splitParts
    }
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
}

private fun hardDisplayTitleFallback(raw: String): String {
    return when (raw.trim()) {
        "releases" -> "שחרורים"
        "releases_hands_hair_shirt" -> "שחרור מתפיסות ידיים / שיער / חולצה"
        "releases_chokes" -> "שחרור מחניקות"
        "releases_hugs" -> "שחרור מחביקות"
        "releases_hugs_body" -> "חביקות גוף"
        "releases_hugs_neck" -> "חביקות צוואר"
        "releases_hugs_arm" -> "חביקות זרוע"
        "knife_defense" -> "הגנות מסכין"
        "gun_threat_defense" -> "הגנות מאיום אקדח"
        "stick_defense" -> "הגנות נגד מקל"
        "kicks_hard" -> "הגנות נגד בעיטות"
        "kicks_straight_groin" -> "הגנות נגד בעיטות ישרות / למפשעה"
        "kicks_roundhouse_back_roundhouse" -> "הגנות נגד מגל / מגל לאחור"
        "kicks_knee" -> "הגנות נגד ברך"
        else -> raw
    }
}

private fun normalizeHardNavTopic(raw: String): String {
    val t = raw.trim()
        .replace("\u200F", "")
        .replace("\u200E", "")
        .replace("\u00A0", " ")
        .replace("–", "-")
        .replace("—", "-")
        .replace(Regex("\\s+"), " ")

    return when {
        t == "שחרורים" -> "releases"

        t == "מתפיסות" ||
                t.contains("תפיסות יד") ||
                t.contains("שיער") ||
                t.contains("חולצה") ||
                t.contains("שחרור מתפיסות") ->
            "releases_hands_hair_shirt"

        t.contains("חניקות") ||
                t.contains("שחרור מחניקות") ->
            "releases_chokes"

        t.contains("מחביקות") ||
                t.contains("חביקות גוף") ||
                t.contains("חביקות צוואר") ||
                t.contains("חביקות צואר") ||
                t.contains("חביקות זרוע") ->
            "releases_hugs"

        else -> t
    }
}

private fun loadDirectTopicItems(
    belt: Belt,
    topicTitle: String
): List<String> {
    val topicCandidates = topicLookupAliases(topicTitle)

    val direct = topicCandidates.flatMap { candidate ->
        runCatching {
            ContentRepo.listItemTitles(
                belt = belt,
                topicTitle = candidate,
                subTopicTitle = null
            )
        }.getOrDefault(emptyList())
    }

    val directSameName = topicCandidates.flatMap { candidate ->
        runCatching {
            ContentRepo.listItemTitles(
                belt = belt,
                topicTitle = candidate,
                subTopicTitle = candidate
            )
        }.getOrDefault(emptyList())
    }

    val repoSubTitles = topicCandidates.flatMap { candidate ->
        runCatching {
            ContentRepo.listSubTopicTitles(belt, candidate)
        }.getOrDefault(emptyList())
    }

    val registrySubTitles = topicCandidates.flatMap { candidate ->
        runCatching {
            AppSubTopicRegistry.getSubTopicsFor(belt, candidate)
        }.getOrDefault(emptyList())
    }

    val allSubTitles = (repoSubTitles + registrySubTitles)
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()

    val viaSubs = topicCandidates.flatMap { candidate ->
        allSubTitles.flatMap { st ->
            runCatching {
                ContentRepo.listItemTitles(
                    belt = belt,
                    topicTitle = candidate,
                    subTopicTitle = st
                )
            }.getOrDefault(emptyList())
        }
    }

    val viaRegistrySameName = topicCandidates.flatMap { candidate ->
        runCatching {
            AppSubTopicRegistry.getItemsFor(
                belt = belt,
                topicTitle = candidate,
                subTopicTitle = candidate
            )
        }.getOrDefault(emptyList())
    }

    val viaRegistrySubs = topicCandidates.flatMap { candidate ->
        allSubTitles.flatMap { st ->
            runCatching {
                AppSubTopicRegistry.getItemsFor(
                    belt = belt,
                    topicTitle = candidate,
                    subTopicTitle = st
                )
            }.getOrDefault(emptyList())
        }
    }

    Log.e(
        "KMI-SubTopics",
        "loadDirectTopicItems: belt=${belt.id}, topic='$topicTitle', candidates=$topicCandidates, direct=${direct.size}, directSame=${directSameName.size}, viaSubs=${viaSubs.size}, viaRegSame=${viaRegistrySameName.size}, viaRegSubs=${viaRegistrySubs.size}"
    )

    return (
            direct +
                    directSameName +
                    viaSubs +
                    viaRegistrySameName +
                    viaRegistrySubs
            )
        .asSequence()
        .map { ExerciseTitleFormatter.displayName(it).ifBlank { it }.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .toList()
}

private val catalogScreenGradientTop = Color(0xFFF3F1FB)
private val catalogScreenGradientMid = Color(0xFFF8F9FD)
private val catalogScreenGradientBottom = Color(0xFFFDFDFE)

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

    // ✅ FIX: alias-ים של שחרורים ("מתפיסות", "חניקות", "מחביקות"...)
    // צריכים להפוך ל-id קשיח כדי שהמסך לא ייפול ל-flow הרגיל והריק.
    val hardNavTopic = remember(topicDecoded) {
        normalizeHardNavTopic(topicDecoded)
    }

    val hardRootSections = remember(hardNavTopic) {
        HardSectionsCatalog.sectionsForSubject(hardNavTopic)
    }

    val hardCurrentSection = remember(hardNavTopic, hardRootSections) {
        if (hardRootSections == null) {
            HardSectionsCatalog.findAnySectionById(hardNavTopic)
        } else {
            null
        }
    }

    val isHardFlow = remember(hardNavTopic, hardRootSections, hardCurrentSection) {
        HardSectionsCatalog.supportsSubject(hardNavTopic) ||
                hardRootSections != null ||
                hardCurrentSection != null
    }

    val hardTitle = remember(hardNavTopic, hardRootSections, hardCurrentSection) {
        when {
            hardCurrentSection != null -> hardCurrentSection.title
            hardRootSections != null ->
                HardSectionsCatalog.subjectDisplayTitle(hardNavTopic)
                    ?: hardDisplayTitleFallback(hardNavTopic)
            else -> hardDisplayTitleFallback(hardNavTopic)
        }
    }

    val hardSubSections = remember(hardRootSections, hardCurrentSection) {
        when {
            hardCurrentSection != null && hardCurrentSection.subSections.isNotEmpty() -> {
                hardCurrentSection.subSections
            }

            hardRootSections != null && hardRootSections.size > 1 -> {
                hardRootSections
            }

            else -> emptyList()
        }
    }

    val hardLeafSection = remember(hardRootSections, hardCurrentSection) {
        when {
            hardCurrentSection != null && hardCurrentSection.subSections.isEmpty() -> {
                hardCurrentSection
            }

            hardRootSections != null &&
                    hardRootSections.size == 1 &&
                    hardRootSections.first().subSections.isEmpty() -> {
                hardRootSections.first()
            }

            else -> null
        }
    }

    val hardItems: List<String> = remember(hardLeafSection, belt) {
        hardLeafSection
            ?.itemsFor(belt)
            .orEmpty()
            .asSequence()
            .map { ExerciseTitleFormatter.displayName(it).ifBlank { it }.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .toList()
    }

    val hardBeltGroups = remember(hardLeafSection) {
        hardLeafSection?.beltGroups
            ?.filter { it.items.isNotEmpty() }
            .orEmpty()
    }

    // שולפים את תתי־הנושאים הרגילים רק אם זה לא hard flow
    val subs: List<String> = remember(belt, topicDecoded, isHardFlow) {
        if (isHardFlow) return@remember emptyList()

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

        val fromRepo = runCatching {
            ContentRepo.listSubTopicTitles(belt, topicDecoded)
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
        }.getOrElse { emptyList() }

        Log.d("KMI-SubTopics", "fromRepo: belt=${belt.id}, topic='$topicDecoded', subs=${fromRepo.size}")
        fromRepo
    }

    val realSubs: List<String> = remember(subs, topicDecoded) {
        val t = topicDecoded.trim()
        subs.asSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .filter { it != t }
            .distinct()
            .toList()
    }

    val hasRealSubs = remember(subs, topicDecoded) {
        when {
            subs.isEmpty() -> false
            subs.size > 1 -> true
            else -> subs.first().trim() != topicDecoded.trim()
        }
    }

    val directItems: List<String> = remember(belt, topicDecoded, isHardFlow) {
        if (isHardFlow) {
            emptyList()
        } else {
            loadDirectTopicItems(
                belt = belt,
                topicTitle = topicDecoded
            )
        }
    }

    val buttonSpacing = 12.dp

    data class OpenedExerciseRequest(
        val belt: Belt,
        val item: String
    )

    // כמו במסך הבית: מפתח תרגיל שנבחר מהחיפוש → פותח דיאלוג הסבר יחיד
    var pickedKey by rememberSaveable { mutableStateOf<String?>(null) }

    // ✅ שומר גם את החגורה של התרגיל, כדי שההסבר ייקרא מ-Explanations נכון
    var openedExerciseRequest by remember {
        mutableStateOf<OpenedExerciseRequest?>(null)
    }

    Scaffold(
        topBar = {
            il.kmi.app.ui.KmiTopBar(
                title = hardTitle,
                onHome = onHome,
                showTopHome = false,
                showTopSearch = false,
                showBottomActions = true,
                lockSearch = false,
                onSearch = null,
                onPickSearchResult = { key -> pickedKey = key },
                centerTitle = false
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            catalogScreenGradientTop,
                            catalogScreenGradientMid,
                            catalogScreenGradientBottom
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(buttonSpacing),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isHardFlow && hardSubSections.isNotEmpty()) {

                    hardSubSections.forEach { section ->
                        val totalCount = section.totalItemsCount()

                        HardSubTopicCategoryCard(
                            belt = belt,
                            title = section.title,
                            count = totalCount,
                            onClick = { onOpenSubTopic(section.id) }
                        )
                    }

                } else if (isHardFlow && hardBeltGroups.isNotEmpty()) {

                    hardBeltGroups.forEach { group ->
                        HardBeltGroupCard(
                            belt = group.belt,
                            items = group.items,
                            onOpenExercise = { item ->
                                openedExerciseRequest = OpenedExerciseRequest(
                                    belt = group.belt,
                                    item = item
                                )
                            }
                        )
                    }

                } else if (isHardFlow && hardItems.isNotEmpty()) {

                    var explain by rememberSaveable { mutableStateOf<String?>(null) }

                    hardItems.forEach { itemName ->
                        ExerciseRowWithInfo(
                            belt = belt,
                            itemName = itemName,
                            accent = MaterialTheme.colorScheme.primary,
                            onExplain = { _, item -> explain = item },
                            onOpenExercise = { item ->
                                openedExerciseRequest = OpenedExerciseRequest(
                                    belt = belt,
                                    item = item
                                )
                            }
                        )
                        Spacer(Modifier.height(6.dp))
                    }

                    explain?.let { item ->
                        val explanation = remember(belt, hardTitle, item) {
                            findExplanationForHitLocal(
                                belt = belt,
                                rawItem = item,
                                topic = hardTitle
                            )
                        }

                        ModernExerciseInfoDialog(
                            title = item,
                            subtitle = hardTitle,
                            explanation = explanation,
                            onDismiss = { explain = null }
                        )
                    }

                } else if (isHardFlow) {

                    Text(
                        text = "לא נמצאו תרגילים קשיחים עבור \"$hardTitle\"",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                } else if (!hasRealSubs) {

                    val items: List<String> = remember(belt, topicDecoded) {
                        loadDirectTopicItems(
                            belt = belt,
                            topicTitle = topicDecoded
                        )
                    }

                    var explain by rememberSaveable { mutableStateOf<String?>(null) }

                    if (items.isEmpty()) {
                        Text(
                            text = "לא נמצאו תתי־נושאים או תרגילים עבור \"$topicDecoded\"",
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
                                onOpenExercise = { item ->
                                    openedExerciseRequest = OpenedExerciseRequest(
                                        belt = belt,
                                        item = item
                                    )
                                }
                            )
                            Spacer(Modifier.height(6.dp))
                        }
                    }

                    explain?.let { item ->
                        val explanation = remember(belt, topicDecoded, item) {
                            findExplanationForHitLocal(
                                belt = belt,
                                rawItem = item,
                                topic = topicDecoded
                            )
                        }

                        ModernExerciseInfoDialog(
                            title = item,
                            subtitle = topicDecoded,
                            explanation = explanation,
                            onDismiss = { explain = null }
                        )
                    }

                } else {

                    realSubs.forEach { subTitleRaw ->
                        val subTitle = subTitleRaw.trim()

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

                        HardSubTopicCategoryCard(
                            belt = belt,
                            title = subTitle,
                            count = itemCount,
                            onClick = { onOpenSubTopic(subTitle) }
                        )
                    }
                }
            }
        }
    }

// ===== דיאלוג הסבר לתרגיל שנלחץ מתוך הרשימה עצמה =====
    openedExerciseRequest?.let { req ->
        val explanation = remember(req.belt, hardTitle, topicDecoded, req.item) {
            findExplanationForHitLocal(
                belt = req.belt,
                rawItem = req.item,
                topic = hardTitle.ifBlank { topicDecoded }
            )
        }

        ModernExerciseInfoDialog(
            title = req.item,
            subtitle = "${req.belt.heb} • ${hardTitle.ifBlank { topicDecoded }}",
            explanation = explanation,
            onDismiss = { openedExerciseRequest = null }
        )
    }

    pickedKey?.let { key ->
        val (beltHit, topicHit, itemHit) = parseSearchKeyLocal(key)
        val displayName = ExerciseTitleFormatter.displayName(itemHit).ifBlank { itemHit }.trim()

        val explanation = remember(beltHit, itemHit) {
            findExplanationForHitLocal(
                belt = beltHit,
                rawItem = itemHit,
                topic = topicHit
            )
        }

        var isFav by remember { mutableStateOf(false) }

        ModernExerciseInfoDialog(
            title = displayName,
            subtitle = "${beltHit.heb} • $topicHit",
            explanation = explanation,
            isFav = isFav,
            onToggleFav = { isFav = !isFav },
            onDismiss = { pickedKey = null }
        )
    }
}


/* ========= עזר: לפרק מפתח חיפוש "belt|topic|item" ========= */
private fun parseSearchKeyLocal(key: String): Triple<il.kmi.shared.domain.Belt, String, String> {
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
private fun findExplanationForHitLocal(
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

@Composable
private fun ModernExerciseInfoDialog(
    title: String,
    subtitle: String? = null,
    explanation: String,
    isFav: Boolean? = null,
    onToggleFav: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFFF7F4FB),
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 8.dp,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )

                    subtitle?.takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (isFav != null && onToggleFav != null) {
                    Spacer(Modifier.width(8.dp))

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE0D8EF))
                    ) {
                        IconButton(onClick = onToggleFav) {
                            if (isFav) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = "מועדף",
                                    tint = Color(0xFFFFC107)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.StarBorder,
                                    contentDescription = "הוסף למועדפים",
                                    tint = Color(0xFF7F759A)
                                )
                            }
                        }
                    }
                }
            }
        },
        text = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE4DDF1))
            ) {
                Text(
                    text = explanation,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Right,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "סגור",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}

@Composable
private fun SubTopicCategoryCard(
    belt: Belt,
    title: String,
    count: Int,
    onClick: () -> Unit
) {
    HardSubTopicCategoryCard(
        belt = belt,
        title = title,
        count = count,
        onClick = onClick
    )
}

@Composable
private fun HardSubTopicCategoryCard(
    belt: Belt,
    title: String,
    count: Int,
    onClick: () -> Unit
) {
    val iconTint = belt.color
    val borderColor = belt.color.copy(alpha = 0.42f)
    val chevronColor = belt.color
    val subtitleColor = belt.color.copy(alpha = 0.95f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        CompositionLocalProvider(
            androidx.compose.ui.platform.LocalLayoutDirection provides
                    androidx.compose.ui.unit.LayoutDirection.Ltr
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(22.dp),
                    color = belt.lightColor,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalLayoutDirection provides
                            androidx.compose.ui.unit.LayoutDirection.Rtl
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = "$count תרגילים",
                            style = MaterialTheme.typography.labelLarge,
                            color = subtitleColor,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(Modifier.width(10.dp))

                Text(
                    text = "‹",
                    style = MaterialTheme.typography.headlineSmall,
                    color = chevronColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun HardBeltGroupCard(
    belt: Belt,
    items: List<String>,
    onOpenExercise: (String) -> Unit
) {
    val title = when (belt) {
        Belt.YELLOW -> "חגורה צהובה"
        Belt.ORANGE -> "חגורה כתומה"
        Belt.GREEN -> "חגורה ירוקה"
        Belt.BLUE -> "חגורה כחולה"
        Belt.BROWN -> "חגורה חומה"
        Belt.BLACK -> "חגורה שחורה"
        else -> belt.heb
    }

    val displayItems = remember(items) {
        items.map { raw ->
            ExerciseTitleFormatter.displayName(raw).ifBlank { raw }.trim()
        }.filter { it.isNotBlank() }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = belt.lightColor,
        tonalElevation = 2.dp,
        shadowElevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.End
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${displayItems.size} תרגילים",
                    style = MaterialTheme.typography.labelLarge,
                    color = belt.color,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.weight(1f))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = belt.color,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Right
                )
            }

            Spacer(Modifier.height(10.dp))

            displayItems.forEachIndexed { index, itemName ->
                HardExerciseLegacyRow(
                    belt = belt,
                    itemName = itemName,
                    onOpenExercise = onOpenExercise
                )

                if (index != displayItems.lastIndex) {
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}


@Composable
private fun HardExerciseLegacyRow(
    belt: Belt,
    itemName: String,
    onOpenExercise: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onOpenExercise(itemName) },
        color = Color.White,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp
    ) {
        CompositionLocalProvider(
            androidx.compose.ui.platform.LocalLayoutDirection provides
                    androidx.compose.ui.unit.LayoutDirection.Ltr
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, top = 10.dp, end = 0.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.StarBorder,
                    contentDescription = "מועדפים",
                    tint = Color(0xFF90A4AE),
                    modifier = Modifier.size(20.dp)
                )

                Spacer(Modifier.width(10.dp))

                CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalLayoutDirection provides
                            androidx.compose.ui.unit.LayoutDirection.Rtl
                ) {
                    Text(
                        text = itemName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Right,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 18.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(28.dp)
                        .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                        .background(belt.color)
                )
            }
        }
    }
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
                            .padding(start = 18.dp)
                            .clickable { onOpenExercise(itemName) }
                    )
                }
            }
        }
    }
}
