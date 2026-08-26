package il.kmi.app.screens.BeltQuestions.Materials

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import il.kmi.app.KmiViewModel
import il.kmi.shared.domain.Belt
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material3.Divider
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import il.kmi.app.ui.ext.lightColor
import il.kmi.shared.questions.model.util.ExerciseTitleFormatter
import il.kmi.shared.domain.ContentRepo as SharedContentRepo
import kotlinx.coroutines.withContext
import androidx.compose.foundation.Image
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import il.kmi.app.R
import il.kmi.app.domain.CanonicalIds
import il.kmi.app.domain.ExerciseExplanationResolver
import il.kmi.app.favorites.FavoritesStore
import il.kmi.app.highlightItem
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.ui.unit.Dp
import il.kmi.app.domain.ContentRepo
import il.kmi.app.ui.color
import il.kmi.app.ui.KmiIconSize
import il.kmi.app.ui.KmiTypography
import il.kmi.app.ui.scaledIconSize
import il.kmi.app.ui.dialogs.ExerciseExplanationDialog
import il.kmi.app.ui.dialogs.ExerciseNoteEditorDialog
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager
import il.kmi.shared.domain.content.ExerciseTitlesEn
import il.kmi.shared.domain.content.ExerciseIdentityRegistry
import il.kmi.app.subscription.KmiAccess
import il.kmi.app.progress.UserProgressRepository
import il.kmi.app.ui.KmiTopBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil

//=================================================================================

/*
 * שלושת המצבים שהמתאמן יכול לבחור.
 *
 * מצב לא מסומן מיוצג באמצעות null ואינו חלק מה-enum.
 */
internal enum class TraineeMaterialStatus(
    val storageValue: String
) {
    KNOWN("known"),
    PARTIALLY_KNOWN("partially_known"),
    UNKNOWN("unknown")
}

internal enum class CoachMaterialStatus(
    val storageValue: String
) {
    NOT_TAUGHT("not_taught"),
    TAUGHT("taught"),
    PRACTICED("practiced"),
    NEEDS_REINFORCEMENT("needs_reinforcement");

    companion object {
        fun fromStorage(value: String?): CoachMaterialStatus {
            return entries.firstOrNull { status ->
                status.storageValue == value
            } ?: NOT_TAUGHT
        }
    }
}

internal data class CoachMaterialProgress(
    val selectedStatuses: Set<CoachMaterialStatus> = emptySet(),
    val updatedAtByStatus: Map<CoachMaterialStatus, Long> = emptyMap()
) {

    fun isSelected(
        status: CoachMaterialStatus
    ): Boolean {
        return selectedStatuses.contains(status)
    }

    fun updatedAtFor(
        status: CoachMaterialStatus
    ): Long {
        return updatedAtByStatus[status] ?: 0L
    }

    /*
     * נשארים גם status / updatedAt כדי שכל הקוד הקיים
     * ימשיך להתקמפל עד שנחליף את ה־UI לשלושת האייקונים.
     *
     * אם נבחרו שני סטטוסים, הסטטוס המתקדם יותר משמש
     * רק כ־fallback לקוד הישן.
     */
    val status: CoachMaterialStatus
        get() = when {
            selectedStatuses.contains(
                CoachMaterialStatus.NEEDS_REINFORCEMENT
            ) -> {
                CoachMaterialStatus.NEEDS_REINFORCEMENT
            }

            selectedStatuses.contains(
                CoachMaterialStatus.PRACTICED
            ) -> {
                CoachMaterialStatus.PRACTICED
            }

            selectedStatuses.contains(
                CoachMaterialStatus.TAUGHT
            ) -> {
                CoachMaterialStatus.TAUGHT
            }

            else -> {
                CoachMaterialStatus.NOT_TAUGHT
            }
        }

    val updatedAt: Long
        get() = updatedAtFor(status)
}

//=================================================================================

@Composable
private fun BeltPill(
    belt: Belt,
    modifier: Modifier = Modifier
) {
    fun beltDrawableRes(
        beltValue: Belt
    ): Int {
        return when (beltValue) {
            Belt.WHITE ->
                R.drawable.intro_belt_white

            Belt.YELLOW ->
                R.drawable.intro_belt_yellow

            Belt.ORANGE ->
                R.drawable.intro_belt_orange

            Belt.GREEN ->
                R.drawable.intro_belt_green

            Belt.BLUE ->
                R.drawable.intro_belt_blue

            Belt.BROWN ->
                R.drawable.intro_belt_brown

            Belt.BLACK ->
                R.drawable.intro_belt_black
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(
                id = beltDrawableRes(belt)
            ),
            contentDescription = null,
            modifier = Modifier.size(
                KmiIconSize.medium
            ),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun premiumSurfaceGradientForBelt(
    belt: Belt
): Brush {
    val isDarkMode =
        MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val colors =
        if (isDarkMode) {
            listOf(
                MaterialTheme.colorScheme.surface.copy(
                    alpha = 0.98f
                ),
                belt.color.copy(alpha = 0.18f),
                MaterialTheme.colorScheme.surfaceVariant.copy(
                    alpha = 0.96f
                )
            )
        } else {
            listOf(
                Color.White.copy(alpha = 0.98f),
                belt.color.copy(alpha = 0.10f),
                Color.White.copy(alpha = 0.94f)
            )
        }

    return Brush.verticalGradient(colors = colors)
}

@Composable
private fun topicTitleForUi(title: String, lang: AppLanguage): String {
    val clean = title.trim()
    return if (lang == AppLanguage.ENGLISH) {
        ExerciseTitlesEn.getOrSame(clean)
    } else {
        clean
    }
}

private fun itemTitleForUi(topic: String, rawItem: String, lang: AppLanguage): String {
    val topicTrim = topic.trim()

    fun normalizeForLookup(s: String): String =
        s.trim()
            .replace("–", "-")
            .replace("—", "-")
            .replace(" - ", " - ")
            .replace("- ", "-")
            .replace(" -", "-")
            .replace(Regex("\\s*/\\s*"), "/")
            .replace(Regex("\\s+"), " ")
            .trim()

    fun removeTopicPrefixOnlyWithSeparator(value: String): String {
        if (topicTrim.isBlank()) return value

        val s = value.trim()

        return when {
            s.startsWith("$topicTrim::") -> {
                s.removePrefix("$topicTrim::").trim()
            }

            s.startsWith("$topicTrim -") -> {
                s.removePrefix(topicTrim).trimStart('-', '–', '—', ':').trim()
            }

            s.startsWith("$topicTrim –") -> {
                s.removePrefix(topicTrim).trimStart('-', '–', '—', ':').trim()
            }

            s.startsWith("$topicTrim —") -> {
                s.removePrefix(topicTrim).trimStart('-', '–', '—', ':').trim()
            }

            s.startsWith("$topicTrim:") -> {
                s.removePrefix(topicTrim).trimStart('-', '–', '—', ':').trim()
            }

            else -> s
        }
    }

    val cleaned = removeTopicPrefixOnlyWithSeparator(rawItem)

    val display = ExerciseTitleFormatter.displayName(cleaned).ifBlank {
        CanonicalIds.uiDisplayName(topicTrim, rawItem).trim()
    }.trim()

    if (lang != AppLanguage.ENGLISH) return display

    val candidates = listOf(
        display,
        normalizeForLookup(display),
        cleaned,
        normalizeForLookup(cleaned),
        rawItem.trim(),
        normalizeForLookup(rawItem.trim()),
        rawItem.substringAfter("::", rawItem).trim(),
        normalizeForLookup(rawItem.substringAfter("::", rawItem).trim())
    ).distinct()

    val translated = candidates.firstNotNullOfOrNull { candidate ->
        ExerciseTitlesEn.get(candidate)?.takeIf { it.isNotBlank() }
    }

    return translated ?: display
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MaterialsScreen(
    vm: KmiViewModel,
    belt: Belt,
    topic: String,
    onBack: () -> Unit,
    // היה: onSummary: (Belt) -> Unit,
    onSummary: (Belt, String, String?) -> Unit,
    onPractice: (Belt, String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHome: () -> Unit,
    subTopicFilter: String? = null,
    isCoach: Boolean = false,
    onOpenSubscription: () -> Unit = {}
) {

    val context = LocalContext.current
    val langManager = remember {
        AppLanguageManager(context)
    }

    val currentLang =
        langManager.getCurrentLanguage()

    val isEnglish =
        currentLang == AppLanguage.ENGLISH

    val sp = remember(context) {
        context.getSharedPreferences(
            "kmi_settings",
            Context.MODE_PRIVATE
        )
    }

    /*
     * מקור האמת לתפקיד הפעיל הוא kmi_user,
     * בדיוק כמו תג התפקיד הגלובלי.
     *
     * הפרמטר isCoach נשאר רק כ־fallback למקרה
     * שבו טרם נשמר user_role.
     */
    val rolePrefs = remember(context) {
        context.getSharedPreferences(
            "kmi_user",
            Context.MODE_PRIVATE
        )
    }

    fun readActiveMaterialsRole(): String? {
        return rolePrefs
            .getString("user_role", null)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: rolePrefs
                .getString("role", null)
                ?.trim()
                ?.takeIf { it.isNotBlank() }
    }

    var activeMaterialsRole by remember {
        mutableStateOf(
            readActiveMaterialsRole()
        )
    }

    DisposableEffect(rolePrefs) {
        val roleListener =
            android.content.SharedPreferences
                .OnSharedPreferenceChangeListener { _,
                                                    key ->

                    if (
                        key == "user_role" ||
                        key == "role"
                    ) {
                        activeMaterialsRole =
                            readActiveMaterialsRole()
                    }
                }

        rolePrefs
            .registerOnSharedPreferenceChangeListener(
                roleListener
            )

        onDispose {
            rolePrefs
                .unregisterOnSharedPreferenceChangeListener(
                    roleListener
                )
        }
    }

    val effectiveIsCoach =
        when (
            activeMaterialsRole
                ?.trim()
                ?.lowercase()
        ) {
            "coach",
            "trainer",
            "מאמן" -> true

            "trainee",
            "student",
            "מתאמן" -> false

            else -> isCoach
        }

    // ✅ גורם למסך להתרענן אחרי סימון יודע/לא יודע ממסכים אחרים, כולל RandomPracticeScreen
    val marksVersion by vm.marksVersion.collectAsState()
    val globalFavorites: Set<String> by FavoritesStore
        .favoritesFlow
        .collectAsState(initial = emptySet())
    val accessSp = remember(context) {
        context.getSharedPreferences("kmi_user", Context.MODE_PRIVATE)
    }

    val hasFullAccess = remember(accessSp, marksVersion) {
        KmiAccess.hasFullAccess(accessSp)
    }

    val scope = rememberCoroutineScope()

    /*
     * הסנכרון ל-Firestore מושהה מעט לאחר לחיצה.
     *
     * אם המשתמש מסמן כמה תרגילים ברצף, הסנכרון
     * הקודם מתבטל ומתבצע רק סנכרון אחד עם המצב
     * האחרון של כל התרגילים.
     */
    var progressSyncJob by remember(
        belt.id
    ) {
        mutableStateOf<Job?>(null)
    }

    DisposableEffect(belt.id) {
        onDispose {
            progressSyncJob?.cancel()
        }
    }

    val scroll = rememberScrollState()
    val itemStates =
        remember(belt.id, topic, subTopicFilter) {
            mutableStateMapOf<String, Boolean?>()
        }

    /*
     * תאריך העדכון האחרון של סימון המתאמן.
     *
     * לכל תרגיל נשמר timestamp נפרד.
     * כאשר הסימון מתאפס ל-null גם התאריך נמחק.
     */
    val traineeUpdatedAtStates =
        remember(belt.id, topic, subTopicFilter) {
            mutableStateMapOf<String, Long>()
        }

    /*
     * סטטוסי המאמן נשמרים בנפרד לחלוטין מסימוני
     * יודע / לא יודע של המתאמן.
     */
    val coachProgressStates =
        remember(belt.id, topic, subTopicFilter) {
            mutableStateMapOf<String, CoachMaterialProgress>()
        }

    /*
     * שומר את הערך שנבחר מיד בלחיצה,
     * כדי שטעינה חוזרת מ־marksVersion לא תדרוס אותו
     * לפני שהשמירה הסתיימה.
     */
    val pendingItemStates =
        remember(belt.id, topic, subTopicFilter) {
            mutableStateMapOf<String, Boolean?>()
        }

    var explainTriple by remember { mutableStateOf<Triple<Belt, String, String>?>(null) }
    var noteEditorFor by rememberSaveable { mutableStateOf<String?>(null) }
    var noteDraft by rememberSaveable { mutableStateOf("") }
    var notesRefreshKey by rememberSaveable { mutableIntStateOf(0) }

    val isDarkSurface = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val explanationTextColor = if (isDarkSurface) Color.White else Color.Black

    // ✅ NEW: נושא לתצוגה/קאנוניקליזציה — כדי ש"" יתנהג בדיוק כמו "כללי"
    val topicUi = remember(topic) { if (topic.isBlank()) "כללי" else topic }

    /*
     * הפרמטר כבר פוענח ב־materialsNavGraph באמצעות
     * Uri.decode. פענוח נוסף באמצעות URLDecoder
     * הופך את הסימן + לרווח ופוגע בשם תת־הנושא.
     */
    fun decodeMaterialParam(
        value: String
    ): String {
        return value.trim()
    }

    val decodedSubTopicFilter = remember(subTopicFilter) {
        subTopicFilter
            ?.takeIf { it.isNotBlank() }
            ?.let { decodeMaterialParam(it) }
    }

    fun isDefenseLevelOneTitle(value: String): Boolean {
        val clean = value
            .replace("\u200F", "")
            .replace("\u200E", "")
            .replace("\u00A0", " ")
            .replace("–", "-")
            .replace("—", "-")
            .replace("־", "-")
            .replace(Regex("\\s+"), " ")
            .trim()

        return clean == "הגנות נגד מכות" ||
                clean == "הגנות נגד בעיטות" ||
                clean == "הגנות - סכין"
    }

    // ✅ תיקון חשוב:
    // לפעמים המסך נפתח עם topic = "הגנות נגד בעיטות" ו-subTopicFilter = null.
    // במקרה כזה root הנושא האמיתי הוא "הגנות", וה-topic עצמו הוא רמה 1.
    val materialRootTopic = remember(belt, topicUi, decodedSubTopicFilter) {
        if (
            decodedSubTopicFilter.isNullOrBlank() &&
            isDefenseLevelOneTitle(topicUi)
        ) {
            "הגנות"
        } else {
            topicUi
        }
    }

    val materialParentSubTopic = remember(belt, topicUi, decodedSubTopicFilter) {
        when {
            !decodedSubTopicFilter.isNullOrBlank() -> decodedSubTopicFilter

            isDefenseLevelOneTitle(topicUi) -> topicUi

            else -> null
        }
    }

    val nestedSubTopicTitles = remember(belt, materialRootTopic, materialParentSubTopic) {
        materialParentSubTopic
            ?.let { sub ->
                runCatching {
                    SharedContentRepo.getNestedSubTopicTitles(
                        belt = belt,
                        topicTitle = materialRootTopic.trim(),
                        subTopicTitle = sub.trim()
                    )
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .distinct()
                }.getOrDefault(emptyList())
            }
            .orEmpty()
    }

    var openedNestedSubTopic by rememberSaveable(
        belt.id,
        materialRootTopic,
        materialParentSubTopic
    ) {
        mutableStateOf<String?>(null)
    }

    var nestedGeneralNoteTitle by rememberSaveable(
        belt.id,
        materialRootTopic,
        materialParentSubTopic
    ) {
        mutableStateOf<String?>(null)
    }

    var nestedGeneralNoteText by rememberSaveable(
        belt.id,
        materialRootTopic,
        materialParentSubTopic
    ) {
        mutableStateOf<String?>(null)
    }

    val isShowingNestedSubTopicPicker = false

    val effectiveSubTopicFilter = remember(
        materialParentSubTopic,
        openedNestedSubTopic
    ) {
        openedNestedSubTopic ?: materialParentSubTopic
    }

    BackHandler(
        enabled = openedNestedSubTopic != null
    ) {
        openedNestedSubTopic = null
    }

    val topicKey = remember(materialRootTopic, materialParentSubTopic, openedNestedSubTopic) {
        when {
            materialParentSubTopic.isNullOrBlank() -> {
                materialRootTopic
            }

            openedNestedSubTopic.isNullOrBlank() -> {
                "${materialRootTopic}__${materialParentSubTopic}"
            }

            else -> {
                "${materialRootTopic}__${materialParentSubTopic}__${openedNestedSubTopic}"
            }
        }
    }

    // ✅ תרגול נעול לכל הנושאים אם אין מנוי פעיל.
    // החומר עצמו יכול להיפתח לפי הלוגיקה הקיימת, אבל כפתור "תרגול" דורש מנוי.
    val isPracticeLocked = remember(hasFullAccess) {
        !hasFullAccess
    }

    // ===== canonical (✅ מקור אמת אחד לכל האפליקציה) =====
    fun canonicalFor(displayItem: String): String =
        CanonicalIds.canonicalFor(belt, topicUi, displayItem)

    fun canonicalFor(topicTitle: String, displayItem: String): String =
        CanonicalIds.canonicalFor(belt, topicTitle, displayItem)

    fun cleanItem(topicTitle: String, item: String): String =
        CanonicalIds.cleanItem(topicTitle, item)

    fun exerciseIdentityIdFor(
        index: Int,
        item: String
    ): String {
        val cleanOriginal = cleanItem(topicUi, item).trim()

        val resolved = ExerciseIdentityRegistry.resolve(
            belt = belt,
            hebrewTitle = cleanOriginal,
            topicKey = topicKey
        )

        if (resolved.isKnown) {
            return resolved.id
        }

        // עד שנמפה את כל 391 התרגילים ידנית:
        // fallback עם index מונע סימון כפול בין תרגילים דומים/זהים בשם.
        return "${resolved.id}_row_$index"
    }

    // ===== סוף canonical =====

    val handlePickFromTopBar: (String) -> Unit = { key ->
        fun dec(s: String) = try {
            URLDecoder.decode(s, "UTF-8")
        } catch (_: Exception) {
            s
        }

        val r = runCatching { ContentRepo.resolveItemKey(key) }.getOrNull()
        if (r != null) {
            explainTriple = Triple(r.belt, r.topicTitle, r.itemTitle)
        } else {
            val parts = when {
                '|' in key -> key.split('|', limit = 3)
                "::" in key -> key.split("::", limit = 3)
                '/' in key -> key.split('/', limit = 3)
                else -> listOf("", "", "")
            }.map(::dec)

            val beltFromKey = Belt.fromId(parts.getOrNull(0).orEmpty()) ?: belt
            val topicFromKey = parts.getOrNull(1).orEmpty().ifBlank { topicUi }
            val itemRaw = cleanItem(topicFromKey, parts.getOrNull(2).orEmpty())
            explainTriple = Triple(beltFromKey, topicFromKey, itemRaw)
        }
    }

    // === שליפת התרגילים (כולל subTopicFilter) ===
    // ✅ Cache בזיכרון כדי שמעבר בין נושאים שכבר נפתחו יהיה מיידי
    val itemsCache = rememberSaveable { mutableMapOf<String, List<String>>() }
    fun itemsCacheKey(): String = buildString {
        append(belt.id)
        append("||")
        append(materialRootTopic.trim())
        append("||")
        append(materialParentSubTopic?.trim().orEmpty())
        append("||")
        append(openedNestedSubTopic?.trim().orEmpty())
    }

    val itemList by produceState<List<String>>(
        initialValue = itemsCache[itemsCacheKey()] ?: emptyList(),
        belt.id,
        materialRootTopic,
        materialParentSubTopic,
        openedNestedSubTopic
    ) {
        val key = itemsCacheKey()

        /*
         * משתמשים במטמון רק כאשר קיימת בו רשימה ממשית.
         * תוצאה ריקה עשויה להיות תוצאה ישנה שנשמרה
         * לפני שינוי שם הנושא או תת־הנושא.
         */
        itemsCache[key]
            ?.takeIf { cachedItems ->
                cachedItems.isNotEmpty()
            }
            ?.let { cachedItems ->
                value = cachedItems
                return@produceState
            }

        value = withContext(Dispatchers.Default) {
            val topicTrim = materialRootTopic.trim()
            val subTrim = materialParentSubTopic?.trim()
            val nestedTrim = openedNestedSubTopic?.trim()

            val list = when {
                subTrim != null && nestedTrim != null -> {
                    SharedContentRepo.getNestedItemsFor(
                        belt = belt,
                        topicTitle = topicTrim,
                        subTopicTitle = subTrim,
                        nestedSubTopicTitle = nestedTrim
                    )
                }

                subTrim != null -> {
                    val nestedTitles = SharedContentRepo.getNestedSubTopicTitles(
                        belt = belt,
                        topicTitle = topicTrim,
                        subTopicTitle = subTrim
                    )
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .distinct()

                    if (nestedTitles.isNotEmpty()) {
                        nestedTitles.flatMap { nestedTitle ->
                            SharedContentRepo.getNestedItemsFor(
                                belt = belt,
                                topicTitle = topicTrim,
                                subTopicTitle = subTrim,
                                nestedSubTopicTitle = nestedTitle
                            )
                        }
                    } else {
                        SharedContentRepo.getAllItemsFor(
                            belt = belt,
                            topicTitle = topicTrim,
                            subTopicTitle = subTrim
                        )
                    }
                }

                else -> {
                    SharedContentRepo.getAllItemsFor(
                        belt = belt,
                        topicTitle = topicTrim,
                        subTopicTitle = null
                    )
                }
            }

            list
                .map { item ->
                    item.trim()
                }
                .filter { item ->
                    item.isNotBlank()
                }
                .distinct()
        }

        if (value.isNotEmpty()) {
            itemsCache[key] = value
        } else {
            itemsCache.remove(key)
        }
    }

    val nestedSectionTitleByItem = remember(
        belt,
        materialRootTopic,
        materialParentSubTopic,
        nestedSubTopicTitles
    ) {
        if (materialParentSubTopic.isNullOrBlank() || nestedSubTopicTitles.isEmpty()) {
            emptyMap()
        } else {
            nestedSubTopicTitles.flatMap { nestedTitle ->
                SharedContentRepo.getNestedItemsFor(
                    belt = belt,
                    topicTitle = materialRootTopic.trim(),
                    subTopicTitle = materialParentSubTopic.trim(),
                    nestedSubTopicTitle = nestedTitle.trim()
                )
                    .map { item -> item.trim() to nestedTitle.trim() }
            }
                .filter { (item, title) -> item.isNotBlank() && title.isNotBlank() }
                .toMap()
        }
    }

    fun normalizeStatusPart(s: String): String =
        s.replace("\u200F", "")
            .replace("\u200E", "")
            .replace("\u00A0", " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    fun legacyStatusIdFor(index: Int, item: String): String {
        val cleanItem = normalizeStatusPart(item)

        // מפתח הסימון הישן לפני המעבר ל-ExerciseIdentityRegistry.
        // נשאר רק לקריאת fallback כדי לא לאבד סימונים קיימים.
        return "status_${belt.id}_${topicKey}_${index}_${cleanItem}"
    }

    /*
   * מזהי הבסיס שמתקבלים מה־Registry עבור הרשימה הנוכחית.
   */
    val baseStatusIds = remember(
        itemList,
        belt.id,
        topicKey,
        topicUi
    ) {
        itemList.mapIndexed { index, item ->
            exerciseIdentityIdFor(
                index = index,
                item = item
            )
        }
    }

    /*
     * אם אותו מזהה הוחזר ליותר משורה אחת,
     * מפרידים רק את השורות המתנגשות.
     *
     * תרגילים בעלי ex_XXX חד־ערכי ממשיכים להשתמש ב־ex_XXX בלבד.
     */
    val duplicatedBaseStatusIds = remember(baseStatusIds) {
        baseStatusIds
            .groupingBy { id -> id }
            .eachCount()
            .filterValues { count -> count > 1 }
            .keys
    }

    fun statusIdFor(index: Int, item: String): String {
        val baseId = baseStatusIds.getOrNull(index)
            ?: exerciseIdentityIdFor(
                index = index,
                item = item
            )

        return if (duplicatedBaseStatusIds.contains(baseId)) {
            "${baseId}__${topicKey}__row_$index"
        } else {
            baseId
        }
    }

    fun traineeUpdatedAtKey(
        statusId: String
    ): String {
        return buildString {
            append("trainee_material_updated_at_")
            append(belt.id)
            append("_")
            append(topicKey)
            append("_")
            append(statusId)
        }
    }

    fun loadTraineeUpdatedAt(
        statusId: String
    ): Long {
        return sp.getLong(
            traineeUpdatedAtKey(statusId),
            0L
        )
    }

    fun saveTraineeUpdatedAt(
        statusId: String,
        value: Boolean?
    ) {
        val key =
            traineeUpdatedAtKey(statusId)

        if (value == null) {
            traineeUpdatedAtStates.remove(statusId)

            sp.edit()
                .remove(key)
                .apply()
        } else {
            val updatedAt =
                System.currentTimeMillis()

            traineeUpdatedAtStates[statusId] =
                updatedAt

            sp.edit()
                .putLong(
                    key,
                    updatedAt
                )
                .apply()
        }
    }

    /*
     * מפתח נפרד לסטטוס המאמן.
     *
     * בשלב הבא נוסיף למפתח גם groupId, לאחר שנחבר
     * את ההתקדמות לקבוצה שנבחרה ול־Firestore.
     */
    fun coachProgressKey(statusId: String): String {
        return buildString {
            append("coach_material_progress_")
            append(belt.id)
            append("_")
            append(topicKey)
            append("_")
            append(statusId)
        }
    }

    fun loadCoachProgress(statusId: String): CoachMaterialProgress {
        val key = coachProgressKey(statusId)

        val selectableStatuses = listOf(
            CoachMaterialStatus.TAUGHT,
            CoachMaterialStatus.PRACTICED,
            CoachMaterialStatus.NEEDS_REINFORCEMENT
        )

        val selectedStatuses =
            selectableStatuses
                .filter { status ->
                    sp.getBoolean(
                        "${key}_${status.storageValue}_selected",
                        false
                    )
                }
                .toSet()

        val updatedAtByStatus =
            selectableStatuses
                .mapNotNull { status ->
                    val updatedAt =
                        sp.getLong(
                            "${key}_${status.storageValue}_updated_at",
                            0L
                        )

                    if (updatedAt > 0L) {
                        status to updatedAt
                    } else {
                        null
                    }
                }
                .toMap()

        /*
         * תאימות לנתונים שכבר נשמרו בגרסה הישנה:
         * אם עדיין אין נתונים במבנה החדש, קוראים את
         * status / updatedAt הישנים וממירים אותם אוטומטית.
         */
        if (selectedStatuses.isEmpty()) {
            val legacyStatus =
                CoachMaterialStatus.fromStorage(
                    sp.getString(
                        "${key}_status",
                        null
                    )
                )

            val legacyUpdatedAt =
                sp.getLong(
                    "${key}_updated_at",
                    0L
                )

            if (
                legacyStatus != CoachMaterialStatus.NOT_TAUGHT
            ) {
                return CoachMaterialProgress(
                    selectedStatuses =
                        setOf(legacyStatus),
                    updatedAtByStatus =
                        if (legacyUpdatedAt > 0L) {
                            mapOf(
                                legacyStatus to legacyUpdatedAt
                            )
                        } else {
                            emptyMap()
                        }
                )
            }
        }

        return CoachMaterialProgress(
            selectedStatuses = selectedStatuses,
            updatedAtByStatus = updatedAtByStatus
        )
    }

    fun saveCoachProgress(
        statusId: String,
        status: CoachMaterialStatus
    ) {
        val key = coachProgressKey(statusId)

        val currentProgress =
            coachProgressStates[statusId]
                ?: loadCoachProgress(statusId)

        /*
         * NOT_TAUGHT = אין אף אחד משלושת האייקונים מסומן.
         */
        if (
            status == CoachMaterialStatus.NOT_TAUGHT
        ) {
            coachProgressStates[statusId] =
                CoachMaterialProgress()

            sp.edit()
                .remove(
                    "${key}_${CoachMaterialStatus.TAUGHT.storageValue}_selected"
                )
                .remove(
                    "${key}_${CoachMaterialStatus.TAUGHT.storageValue}_updated_at"
                )
                .remove(
                    "${key}_${CoachMaterialStatus.PRACTICED.storageValue}_selected"
                )
                .remove(
                    "${key}_${CoachMaterialStatus.PRACTICED.storageValue}_updated_at"
                )
                .remove(
                    "${key}_${CoachMaterialStatus.NEEDS_REINFORCEMENT.storageValue}_selected"
                )
                .remove(
                    "${key}_${CoachMaterialStatus.NEEDS_REINFORCEMENT.storageValue}_updated_at"
                )
                .remove("${key}_status")
                .remove("${key}_updated_at")
                .apply()

            return
        }

        val nextSelectedStatuses =
            currentProgress
                .selectedStatuses
                .toMutableSet()

        val nextUpdatedAtByStatus =
            currentProgress
                .updatedAtByStatus
                .toMutableMap()

        if (
            nextSelectedStatuses.contains(status)
        ) {
            /*
             * לחיצה על אייקון שכבר מסומן =
             * ביטול אותו סטטוס בלבד.
             */
            nextSelectedStatuses.remove(status)
            nextUpdatedAtByStatus.remove(status)
        } else {
            /*
             * מותר לבחור עד 2 מתוך:
             * נלמד / תורגל / נדרש חיזוק.
             *
             * אם כבר נבחרו שניים, לא מוסיפים שלישי.
             * בהמשך ה־UI יציג למשתמש הודעה מתאימה.
             */
            if (nextSelectedStatuses.size >= 2) {
                return
            }

            nextSelectedStatuses.add(status)

            nextUpdatedAtByStatus[status] =
                System.currentTimeMillis()
        }

        val nextProgress =
            CoachMaterialProgress(
                selectedStatuses =
                    nextSelectedStatuses,
                updatedAtByStatus =
                    nextUpdatedAtByStatus
            )

        coachProgressStates[statusId] =
            nextProgress

        val editor = sp.edit()

        listOf(
            CoachMaterialStatus.TAUGHT,
            CoachMaterialStatus.PRACTICED,
            CoachMaterialStatus.NEEDS_REINFORCEMENT
        ).forEach { selectableStatus ->

            val isSelected =
                nextSelectedStatuses.contains(
                    selectableStatus
                )

            if (isSelected) {
                editor.putBoolean(
                    "${key}_${selectableStatus.storageValue}_selected",
                    true
                )

                editor.putLong(
                    "${key}_${selectableStatus.storageValue}_updated_at",
                    nextUpdatedAtByStatus[
                        selectableStatus
                    ] ?: 0L
                )
            } else {
                editor.remove(
                    "${key}_${selectableStatus.storageValue}_selected"
                )

                editor.remove(
                    "${key}_${selectableStatus.storageValue}_updated_at"
                )
            }
        }

        /*
         * מוחקים את המבנה הישן לאחר שהשמירה החדשה
         * כבר הפכה למקור האמת.
         */
        editor
            .remove("${key}_status")
            .remove("${key}_updated_at")
            .apply()
    }

    /*
     * טוען את סטטוסי המאמן לאחר שרשימת התרגילים מוכנה.
     */
    LaunchedEffect(
        effectiveIsCoach,
        belt.id,
        topicKey,
        itemList
    ) {
        if (
            !effectiveIsCoach ||
            itemList.isEmpty()
        ) {
            if (!effectiveIsCoach) {
                coachProgressStates.clear()
            }

            return@LaunchedEffect
        }

        val loadedProgress = buildMap {
            itemList.forEachIndexed { index, item ->
                val statusId = statusIdFor(index, item)

                put(
                    statusId,
                    loadCoachProgress(statusId)
                )
            }
        }

        coachProgressStates.clear()
        coachProgressStates.putAll(loadedProgress)
    }

// הדגשת תרגיל (✅ בלי Reflection: זה top-level flow)
    val highlight by highlightItem.collectAsState(initial = null)

    // ✅ NEW: נרמול אחיד למפתחות SP (כדי שסיכום ותוכן יקראו את אותו מפתח)
    fun spKeyPart(s: String): String = s
        .replace("\u200F", "")
        .replace("\u200E", "")
        .replace("\u00A0", " ")
        .trim()

    val excludedKeySuffix = remember(topicUi, subTopicFilter) {
        val t = spKeyPart(topicUi)
        val st = subTopicFilter?.let(::spKeyPart).orEmpty()
        if (st.isBlank()) t else "${t}__${st}"
    }

    val excludedItems = remember { mutableStateListOf<String>() }
    LaunchedEffect(belt, excludedKeySuffix) {
        excludedItems.clear()
        excludedItems.addAll(
            sp.getStringSet("excluded_${belt.id}_$excludedKeySuffix", emptySet()) ?: emptySet()
        )
    }
    fun toggleExclude(item: String) {
        // ✅ item כאן כבר canonicalId
        if (excludedItems.contains(item)) excludedItems.remove(item) else excludedItems.add(item)

        sp.edit()
            .putStringSet("excluded_${belt.id}_$excludedKeySuffix", excludedItems.toSet())
            .apply()
    }

    // ⬇️ מועדפים / הערות נשארים ב-SP (לא קשור לסימונים)
    val favKey = remember(belt.id, excludedKeySuffix) { "fav_${belt.id}_$excludedKeySuffix" }
    var favorites by remember(favKey) {
        mutableStateOf<MutableSet<String>>(
            sp.getStringSet(favKey, emptySet())?.toMutableSet() ?: mutableSetOf()
        )
    }

    fun globalFavoriteIdFor(rawItem: String): String {
        return rawItem
            .substringAfter("::", rawItem)
            .substringAfter(":", rawItem)
            .trim()
    }

    fun favoriteAliasesFor(
        topicTitle: String,
        rawItem: String
    ): Set<String> {
        val clean = cleanItem(topicTitle, rawItem).trim()

        val registryId = ExerciseIdentityRegistry.resolve(
            belt = belt,
            hebrewTitle = clean,
            topicKey = topicKey
        ).id

        val explanationId = CanonicalIds.resolveCanonicalForExplanation(
            belt = belt,
            topicTitle = topicTitle,
            rawItemFromRepo = rawItem
        )

        val canonicalIdForTopic = canonicalFor(topicTitle, rawItem)
        val canonicalIdForScreenTopic = canonicalFor(rawItem)

        return setOf(
            registryId,
            explanationId,
            canonicalIdForTopic,
            canonicalIdForScreenTopic
        )
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()
    }

    fun isFavoriteByAliases(
        topicTitle: String,
        rawItem: String
    ): Boolean {
        val globalFavoriteId = globalFavoriteIdFor(rawItem)

        return globalFavorites.contains(globalFavoriteId) ||
                favoriteAliasesFor(topicTitle, rawItem).any { id ->
                    favorites.contains(id)
                }
    }

    fun toggleFavoriteAliases(
        topicTitle: String,
        rawItem: String
    ) {
        val aliases = favoriteAliasesFor(
            topicTitle = topicTitle,
            rawItem = rawItem
        )

        val globalFavoriteId = globalFavoriteIdFor(rawItem)
        val nextFavorites = favorites.toMutableSet()

        val isCurrentlyFavorite =
            globalFavorites.contains(globalFavoriteId) ||
                    aliases.any { id ->
                        nextFavorites.contains(id)
                    }

        val shouldAdd = !isCurrentlyFavorite

        if (shouldAdd) {
            nextFavorites.addAll(aliases)
        } else {
            aliases.forEach { id ->
                nextFavorites.remove(id)
            }
        }

        val isGlobalFavorite =
            globalFavorites.contains(globalFavoriteId)

        if (isGlobalFavorite != shouldAdd) {
            FavoritesStore.toggle(globalFavoriteId)
        }

        favorites = nextFavorites

        sp.edit()
            .putStringSet(favKey, nextFavorites)
            .apply()
    }

    fun toggleFavorite(id: String) {
        val nextFavorites = favorites.toMutableSet()

        if (!nextFavorites.add(id)) {
            nextFavorites.remove(id)
        }

        favorites = nextFavorites
        sp.edit()
            .putStringSet(favKey, nextFavorites)
            .apply()
    }

    // ✅ סימונים (✓/✗/—) — מקור אמת יחיד: ViewModel/DataStore

    val unknownKey =
        remember(belt.id, excludedKeySuffix) { "unknown_${belt.id}_$excludedKeySuffix" }
    var unknowns by remember(unknownKey) {
        mutableStateOf<MutableSet<String>>(
            sp.getStringSet(unknownKey, emptySet())?.toMutableSet() ?: mutableSetOf()
        )
    }

    fun setUnknown(id: String, set: Boolean) {
        val s = unknowns.toMutableSet()

        if (set) {
            s.add(id)
        } else {
            s.remove(id)
        }

        unknowns = s

        sp.edit()
            .putStringSet(
                unknownKey,
                s
            )
            .apply()
    }

    /*
     * „יודע חלקית” נשמר בנפרד.
     *
     * ברמת מנגנון ההתקדמות הקיים הוא עדיין אינו
     * נחשב „יודע”, אך ניתן להבדיל בינו לבין
     * „לא יודע” במסך התרגילים.
     */
    val partiallyKnownKey =
        remember(
            belt.id,
            excludedKeySuffix
        ) {
            "partially_known_${belt.id}_$excludedKeySuffix"
        }

    var partiallyKnownSet by remember(
        partiallyKnownKey
    ) {
        mutableStateOf<MutableSet<String>>(
            sp.getStringSet(
                partiallyKnownKey,
                emptySet()
            )
                ?.toMutableSet()
                ?: mutableSetOf()
        )
    }

    fun setPartiallyKnownLocal(
        id: String,
        set: Boolean
    ) {
        val next =
            partiallyKnownSet.toMutableSet()

        if (set) {
            next.add(id)
        } else {
            next.remove(id)
        }

        partiallyKnownSet = next

        sp.edit()
            .putStringSet(
                partiallyKnownKey,
                next
            )
            .apply()
    }

    fun traineeMaterialStatusFor(
        statusId: String,
        mastered: Boolean?
    ): TraineeMaterialStatus? {
        return when {
            mastered == true ->
                TraineeMaterialStatus.KNOWN

            partiallyKnownSet.contains(statusId) ->
                TraineeMaterialStatus.PARTIALLY_KNOWN

            mastered == false ->
                TraineeMaterialStatus.UNKNOWN

            else ->
                null
        }
    }

// ✅ NEW: נשמור גם mastered (וי ירוק) ב-SP כדי שהסיכום יראה אותו
    val masteredKey =
        remember(belt.id, excludedKeySuffix) { "mastered_${belt.id}_$excludedKeySuffix" }
    var masteredSet by remember(masteredKey) {
        mutableStateOf<MutableSet<String>>(
            sp.getStringSet(masteredKey, emptySet())?.toMutableSet() ?: mutableSetOf()
        )
    }

    fun setMasteredLocal(id: String, set: Boolean) {
        val s = masteredSet.toMutableSet()
        if (set) s.add(id) else s.remove(id)
        masteredSet = s
        sp.edit().putStringSet(masteredKey, s).apply()
    }

    // (SharedPreferences) הערות חופשיות לכל תרגיל – בלי excludedKeySuffix גלובלי
    fun loadNote(itemId: String): String {
        val suffix = if (subTopicFilter.isNullOrBlank()) {
            topicUi
        } else {
            "${topicUi}__${subTopicFilter}"
        }
        val key = "note_${belt.id}_${suffix}_$itemId"
        return sp.getString(key, "") ?: ""
    }

    fun saveNote(itemId: String, value: String) {
        val suffix = if (subTopicFilter.isNullOrBlank()) {
            topicUi
        } else {
            "${topicUi}__${subTopicFilter}"
        }

        val key = "note_${belt.id}_${suffix}_$itemId"
        val clean = value.trim()

        sp.edit().apply {
            if (clean.isBlank()) {
                remove(key)
            } else {
                putString(key, clean)
            }
        }.apply()

        notesRefreshKey++
    }

    // טעינת מצבי שליטה — מקור אמת יחיד: VM/DataStore
    // ✅ טוען את כל הסימונים למפה זמנית ורק בסוף מעדכן UI,
    // כדי למנוע מצב ביניים שבו רוב השורות מצוירות כ-null.
    LaunchedEffect(
        belt,
        topicUi,
        subTopicFilter,
        itemList,
        marksVersion
    ) {
        if (itemList.isEmpty()) {
            return@LaunchedEffect
        }

        /*
         * לחיצה אחת יכולה לעדכן כמה מפתחות ולשנות את marksVersion
         * מספר פעמים.
         *
         * כל שינוי חדש מבטל אוטומטית את הרצת ה-LaunchedEffect
         * הקודמת. כך רק השינוי האחרון גורם לקריאה מחודשת של
         * הרשימה, בזמן שהסימון המקומי כבר מוצג מיד.
         */
        delay(120L)

        val nextStates = withContext(Dispatchers.Default) {
            buildMap<String, Boolean?> {
                itemList.forEachIndexed { index, item ->
                    val statusId = statusIdFor(index, item)
                    val legacyStatusId = legacyStatusIdFor(index, item)

                    val topicKeysToRead = if (subTopicFilter.isNullOrBlank()) {
                        listOf(
                            topicKey,
                            topicUi,
                            "כללי"
                        )
                    } else {
                        listOf(topicKey)
                    }
                        .map { key -> key.trim() }
                        .filter { key -> key.isNotBlank() }
                        .distinct()

                    var valueFromViewModel: Boolean? = null

                    for (key in topicKeysToRead) {
                        val value = runCatching {
                            vm.getItemStatusNullable(
                                belt = belt,
                                topic = key,
                                item = statusId
                            )
                        }.getOrNull()
                            ?: runCatching {
                                if (
                                    vm.isMastered(
                                        belt = belt,
                                        topic = key,
                                        item = statusId
                                    )
                                ) {
                                    true
                                } else {
                                    null
                                }
                            }.getOrNull()
                            ?: runCatching {
                                vm.getItemStatusNullable(
                                    belt = belt,
                                    topic = key,
                                    item = legacyStatusId
                                )
                            }.getOrNull()
                            ?: runCatching {
                                if (
                                    vm.isMastered(
                                        belt = belt,
                                        topic = key,
                                        item = legacyStatusId
                                    )
                                ) {
                                    true
                                } else {
                                    null
                                }
                            }.getOrNull()

                        if (value != null) {
                            valueFromViewModel = value
                            break
                        }
                    }

                    val localFallback: Boolean? = when {
                        masteredSet.contains(statusId) ||
                                masteredSet.contains(legacyStatusId) -> true

                        unknowns.contains(statusId) ||
                                unknowns.contains(legacyStatusId) -> false

                        else -> null
                    }

                    val loadedValue =
                        valueFromViewModel ?: localFallback

                    put(
                        statusId,
                        if (pendingItemStates.containsKey(statusId)) {
                            pendingItemStates[statusId]
                        } else {
                            loadedValue
                        }
                    )
                }
            }
        }

        /*
 * לא מנקים את המפה לפני העדכון.
 * clear() גרם לפריים שבו כל הסימונים הופיעו במצב אפור.
 */
        nextStates.forEach { (statusId, loadedValue) ->
            val visibleValue =
                if (pendingItemStates.containsKey(statusId)) {
                    pendingItemStates[statusId]
                } else {
                    loadedValue
                }

            itemStates[statusId] = visibleValue

            val updatedAt =
                loadTraineeUpdatedAt(statusId)

            if (updatedAt > 0L) {
                traineeUpdatedAtStates[statusId] =
                    updatedAt
            } else {
                traineeUpdatedAtStates.remove(
                    statusId
                )
            }
        }
    }

    val currentCanonicalIds = remember(
        itemList,
        belt.id,
        topicUi
    ) {
        itemList
            .map { item -> canonicalFor(item) }
            .distinct()
    }

    val summaryTotalCount = remember(itemList) {
        itemList.size
    }

    val summaryMasteredCount = remember(
        itemList,
        itemStates.toMap(),
        masteredSet,
        unknowns,
        belt.id,
        topicKey
    ) {
        itemList
            .mapIndexed { index, item ->
                val statusId = statusIdFor(index, item)

                itemStates[statusId] ?: when {
                    masteredSet.contains(statusId) -> true
                    unknowns.contains(statusId) -> false
                    else -> null
                }
            }
            .count { it == true }
    }

    val summaryPartiallyKnownCount = remember(
        itemList,
        partiallyKnownSet,
        belt.id,
        topicKey
    ) {
        itemList
            .mapIndexed { index, item ->
                val statusId =
                    statusIdFor(
                        index,
                        item
                    )

                partiallyKnownSet.contains(
                    statusId
                )
            }
            .count { isPartiallyKnown ->
                isPartiallyKnown
            }
    }

    val summaryUnknownCount = remember(
        itemList,
        itemStates.toMap(),
        masteredSet,
        unknowns,
        partiallyKnownSet,
        belt.id,
        topicKey
    ) {
        itemList
            .mapIndexed { index, item ->
                val statusId =
                    statusIdFor(
                        index,
                        item
                    )

                val state =
                    itemStates[statusId]
                        ?: when {
                            masteredSet.contains(
                                statusId
                            ) ->
                                true

                            unknowns.contains(
                                statusId
                            ) ->
                                false

                            else ->
                                null
                        }

                state == false &&
                        !partiallyKnownSet.contains(
                            statusId
                        )
            }
            .count { isUnknown ->
                isUnknown
            }
    }

    val summaryFavoritesCount = remember(
        itemList,
        favorites,
        materialRootTopic,
        belt.id,
        topicKey
    ) {
        itemList.count { item ->
            isFavoriteByAliases(materialRootTopic, item)
        }
    }

    val summaryExcludedCount = remember(
        currentCanonicalIds,
        excludedItems.toList()
    ) {
        currentCanonicalIds.count { id ->
            excludedItems.contains(id)
        }
    }

    val summaryNotesCount = remember(
        currentCanonicalIds,
        notesRefreshKey,
        belt.id,
        excludedKeySuffix
    ) {
        currentCanonicalIds.count { id ->
            loadNote(id).isNotBlank()
        }
    }

    /*
     * ספירת סטטוסי המאמן.
     *
     * תרגיל שעדיין לא קיבל סטטוס נחשב "לא נלמד".
     */
    val coachNotTaughtCount = remember(
        itemList,
        coachProgressStates.toMap(),
        belt.id,
        topicKey
    ) {
        itemList.count { item ->
            val index = itemList.indexOf(item)
            val statusId = statusIdFor(index, item)

            coachProgressStates[statusId]
                ?.selectedStatuses
                .orEmpty()
                .isEmpty()
        }
    }

    val coachTaughtCount = remember(
        itemList,
        coachProgressStates.toMap(),
        belt.id,
        topicKey
    ) {
        itemList.count { item ->
            val index = itemList.indexOf(item)
            val statusId = statusIdFor(index, item)

            coachProgressStates[statusId]
                ?.isSelected(
                    CoachMaterialStatus.TAUGHT
                ) == true
        }
    }

    val coachPracticedCount = remember(
        itemList,
        coachProgressStates.toMap(),
        belt.id,
        topicKey
    ) {
        itemList.count { item ->
            val index = itemList.indexOf(item)
            val statusId = statusIdFor(index, item)

            coachProgressStates[statusId]
                ?.isSelected(
                    CoachMaterialStatus.PRACTICED
                ) == true
        }
    }

    val coachNeedsReinforcementCount = remember(
        itemList,
        coachProgressStates.toMap(),
        belt.id,
        topicKey
    ) {
        itemList.count { item ->
            val index = itemList.indexOf(item)
            val statusId = statusIdFor(index, item)

            coachProgressStates[statusId]
                ?.isSelected(
                    CoachMaterialStatus.NEEDS_REINFORCEMENT
                ) == true
        }
    }

    Scaffold(
        topBar = {
            /*
             * הכותרת מציגה רק את הרמה הפעילה:
             *
             * נושא רגיל       -> שם הנושא
             * תת־נושא         -> שם תת־הנושא
             * תת־נושא פנימי   -> שם הרמה הפנימית
             */
            val headerTitle =
                when {
                    !openedNestedSubTopic.isNullOrBlank() -> {
                        topicTitleForUi(
                            openedNestedSubTopic.orEmpty(),
                            currentLang
                        )
                    }

                    !decodedSubTopicFilter.isNullOrBlank() -> {
                        topicTitleForUi(
                            decodedSubTopicFilter,
                            currentLang
                        )
                    }

                    else -> {
                        topicTitleForUi(
                            topic,
                            currentLang
                        )
                    }
                }

            val contextLang = LocalContext.current
            val langManager = remember { AppLanguageManager(contextLang) }

            KmiTopBar(
                title = headerTitle,
                onBack = onBack,
                onHome = onOpenHome,
                // לא רוצים אייקון בית עליון כי הוא כבר קיים
                showTopHome = false,
                showRoleStatus = false,      // מבטל את תג "מאמן" בצד
                centerTitle = true,
                alignTitleEnd = false,
                showBottomActions = true,
                showTopShare = false,
                onShare = {
                    shareMaterialsPdf(
                        context = context,
                        belt = belt,
                        topicTitle = headerTitle,
                        items = itemList.mapIndexed { index, item ->

                            val statusId =
                                statusIdFor(
                                    index,
                                    item
                                )

                            val canonicalId =
                                canonicalFor(item)

                            val pdfStatus =
                                if (effectiveIsCoach) {

                                    val coachProgress =
                                        coachProgressStates[statusId]
                                            ?: loadCoachProgress(statusId)

                                    val selectedStatuses =
                                        coachProgress.selectedStatuses

                                    if (selectedStatuses.isEmpty()) {

                                        if (isEnglish) {
                                            "Not taught"
                                        } else {
                                            "לא נלמד"
                                        }

                                    } else {

                                        buildList {

                                            if (
                                                selectedStatuses.contains(
                                                    CoachMaterialStatus.TAUGHT
                                                )
                                            ) {
                                                add(
                                                    if (isEnglish) {
                                                        "Taught"
                                                    } else {
                                                        "נלמד"
                                                    }
                                                )
                                            }

                                            if (
                                                selectedStatuses.contains(
                                                    CoachMaterialStatus.PRACTICED
                                                )
                                            ) {
                                                add(
                                                    if (isEnglish) {
                                                        "Practiced"
                                                    } else {
                                                        "תורגל"
                                                    }
                                                )
                                            }

                                            if (
                                                selectedStatuses.contains(
                                                    CoachMaterialStatus.NEEDS_REINFORCEMENT
                                                )
                                            ) {
                                                add(
                                                    if (isEnglish) {
                                                        "Needs reinforcement"
                                                    } else {
                                                        "טעון שיפור"
                                                    }
                                                )
                                            }
                                        }.joinToString(" · ")
                                    }

                                } else {

                                    val state =
                                        itemStates[statusId]

                                    if (
                                        partiallyKnownSet.contains(
                                            statusId
                                        )
                                    ) {
                                        if (isEnglish) {
                                            "Partially known"
                                        } else {
                                            "יודע חלקית"
                                        }
                                    } else {
                                        when (state) {
                                            true ->
                                                if (isEnglish) {
                                                    "Known"
                                                } else {
                                                    "יודע"
                                                }

                                            false ->
                                                if (isEnglish) {
                                                    "Unknown"
                                                } else {
                                                    "לא יודע"
                                                }

                                            null ->
                                                if (isEnglish) {
                                                    "Not marked"
                                                } else {
                                                    "לא סומן"
                                                }
                                        }
                                    }
                                }

                            MaterialPdfItem(
                                number = index + 1,
                                title =
                                    itemTitleForUi(
                                        topicUi,
                                        item,
                                        currentLang
                                    ),
                                status = pdfStatus,
                                isFavorite =
                                    isFavoriteByAliases(
                                        materialRootTopic,
                                        item
                                    ),
                                isExcluded =
                                    excludedItems.contains(
                                        canonicalId
                                    ),
                                hasNote =
                                    loadNote(
                                        canonicalId
                                    ).isNotBlank()
                            )
                        },
                        isEnglish = isEnglish,
                        isCoach = effectiveIsCoach
                    )
                },
                onPickSearchResult = { key -> handlePickFromTopBar(key) },
                currentLang = if (langManager.getCurrentLanguage() == AppLanguage.ENGLISH) "en" else "he",
                onToggleLanguage = {
                    val newLang =
                        if (langManager.getCurrentLanguage() == AppLanguage.HEBREW) {
                            AppLanguage.ENGLISH
                        } else {
                            AppLanguage.HEBREW
                        }

                    langManager.setLanguage(newLang)
                    (contextLang as? Activity)?.recreate()
                }
            )
        },
        bottomBar = {
            Surface(
                color = Color.Transparent,
                shadowElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = premiumSurfaceGradientForBelt(
                                belt = belt
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = belt.color.copy(
                                alpha = if (isDarkSurface) {
                                    0.30f
                                } else {
                                    0.14f
                                }
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AnimatedButton(
                                text = when {
                                    isPracticeLocked && isEnglish -> "Train 🔒"
                                    isPracticeLocked -> "תרגול 🔒"
                                    isEnglish -> "Practice"
                                    else -> "תרגול"
                                },
                                modifier = Modifier.weight(1f),
                                containerColor = if (isPracticeLocked) {
                                    Color(0xFF9A7A22)
                                } else {
                                    belt.color.copy(alpha = 0.92f)
                                },
                                onClick = {
                                    if (isPracticeLocked) {
                                        onOpenSubscription()
                                    } else {
                                        onPractice(belt, topicUi)
                                    }
                                }
                            )

                            AnimatedButton(
                                text =
                                    if (isEnglish) {
                                        "Reset"
                                    } else {
                                        "איפוס"
                                    },
                                modifier = Modifier.weight(1f),
                                containerColor = Color(0xFFB3261E),
                                onClick = {
                                    val keysToClear =
                                        if (
                                            subTopicFilter
                                                .isNullOrBlank()
                                        ) {
                                            listOf(
                                                topicKey,
                                                topicUi,
                                                "כללי"
                                            )
                                        } else {
                                            listOf(topicKey)
                                        }
                                            .map { key ->
                                                key.trim()
                                            }
                                            .filter { key ->
                                                key.isNotBlank()
                                            }
                                            .distinct()

                                    /*
                                     * ניקוי מיידי של המצב שמוצג במסך.
                                     *
                                     * הפעולות האלה אינן ממתינות ל־ViewModel,
                                     * ולכן הכרטיסים והמונים מתאפסים מיד.
                                     */
                                    pendingItemStates.clear()

                                    itemList.forEachIndexed { index,
                                                              item ->

                                        val statusId =
                                            statusIdFor(
                                                index = index,
                                                item = item
                                            )

                                        val legacyStatusId =
                                            legacyStatusIdFor(
                                                index = index,
                                                item = item
                                            )

                                        itemStates[statusId] = null
                                        itemStates[legacyStatusId] = null
                                    }

                                    coachProgressStates.clear()
                                    excludedItems.clear()

                                    favorites =
                                        mutableSetOf()

                                    masteredSet =
                                        mutableSetOf()

                                    unknowns =
                                        mutableSetOf()

                                    partiallyKnownSet =
                                        mutableSetOf()

                                    /*
                                     * מחיקת כל הנתונים השמורים מקומית.
                                     * apply מעדכן מיד את הזיכרון וכותב
                                     * לדיסק ברקע.
                                     */
                                    val editor = sp.edit()
                                        .remove(
                                            "excluded_${belt.id}_$excludedKeySuffix"
                                        )
                                        .remove(
                                            "fav_${belt.id}_$excludedKeySuffix"
                                        )

                                    keysToClear.forEach { key ->
                                        editor
                                            .remove(
                                                "mastered_${belt.id}_$key"
                                            )
                                            .remove(
                                                "unknown_${belt.id}_$key"
                                            )
                                            .remove(
                                                "partially_known_${belt.id}_$key"
                                            )
                                    }

                                    /*
                                     * מחיקת סטטוסי המאמן:
                                     * לא נלמד / נלמד / תורגל /
                                     * נדרש חיזוק.
                                     */
                                    itemList.forEachIndexed { index,
                                                              item ->

                                        val statusId =
                                            statusIdFor(
                                                index = index,
                                                item = item
                                            )

                                        val progressKey =
                                            coachProgressKey(
                                                statusId
                                            )

                                        editor
                                            .remove(
                                                "${progressKey}_status"
                                            )
                                            .remove(
                                                "${progressKey}_updated_at"
                                            )
                                    }

                                    editor.apply()

                                    /*
   * מאפשרים ל־Compose לצייר קודם את מצב
   * האיפוס שכבר עודכן במפות המקומיות.
   *
   * לאחר מכן מבצעים את הניקוי הכבד
   * מחוץ ל־Main Thread כדי לא לחסום
   * את רענון המסך.
   */
                                    scope.launch {
                                        delay(32L)

                                        withContext(Dispatchers.IO) {
                                            keysToClear.forEach { key ->
                                                vm.clearTopic(
                                                    belt = belt,
                                                    topic = key
                                                )
                                            }

                                            /*
                                             * איפוס של מאמן אינו נתון
                                             * התקדמות אישי של מתאמן.
                                             */
                                            if (!effectiveIsCoach) {
                                                runCatching {
                                                    UserProgressRepository
                                                        .syncCurrentUserBeltProgress(
                                                            vm = vm,
                                                            belt = belt
                                                        )
                                                }
                                            }
                                        }
                                    }
                                }
                            )
                        }

                        AnimatedButton(
                            text =
                                if (isEnglish) {
                                    "Summary Screen"
                                } else {
                                    "מסך סיכום"
                                },
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = Color(0xFF1F2937),
                            onClick = {
                                onSummary(
                                    belt,
                                    topicUi,
                                    subTopicFilter
                                )
                            }
                        )
                    }
                }
            }
        }

    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = belt.lightColor,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {

            nestedGeneralNoteText
                ?.trim()
                ?.takeIf { note ->
                    note.isNotBlank()
                }
                ?.let { visibleNote ->
                    AlertDialog(
                        onDismissRequest = {
                            nestedGeneralNoteTitle = null
                            nestedGeneralNoteText = null
                        },
                        icon = {
                            Surface(
                                modifier = Modifier.size(38.dp),
                                shape = CircleShape,
                                color =
                                    if (isDarkSurface) {
                                        Color(0xFF2563EB)
                                            .copy(alpha = 0.24f)
                                    } else {
                                        Color(0xFFE8F1FF)
                                    },
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = Color(0xFF2563EB)
                                        .copy(alpha = 0.40f)
                                )
                            ) {
                                Box(
                                    contentAlignment =
                                        Alignment.Center
                                ) {
                                    Icon(
                                        imageVector =
                                            Icons.Filled.Info,
                                        contentDescription = null,
                                        tint =
                                            if (isDarkSurface) {
                                                Color(0xFF60A5FA)
                                            } else {
                                                Color(0xFF2563EB)
                                            },
                                        modifier =
                                            Modifier.size(20.dp)
                                    )
                                }
                            }
                        },
                        title = {
                            Text(
                                text =
                                    nestedGeneralNoteTitle
                                        .orEmpty(),
                                modifier =
                                    Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                style =
                                    KmiTypography.sectionTitle,
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurface
                            )
                        },
                        text = {
                            Text(
                                text = visibleNote,
                                modifier =
                                    Modifier.fillMaxWidth(),
                                textAlign =
                                    if (isEnglish) {
                                        TextAlign.Left
                                    } else {
                                        TextAlign.Right
                                    },
                                style =
                                    KmiTypography.secondary.copy(
                                        fontWeight =
                                            FontWeight.Medium
                                    ),
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    nestedGeneralNoteTitle =
                                        null
                                    nestedGeneralNoteText =
                                        null
                                }
                            ) {
                                Text(
                                    text =
                                        if (isEnglish) {
                                            "Close"
                                        } else {
                                            "סגור"
                                        },
                                    style =
                                        KmiTypography.action,
                                    color = Color(0xFF2563EB)
                                )
                            }
                        },
                        shape = RoundedCornerShape(26.dp),
                        containerColor =
                            MaterialTheme.colorScheme.surface
                    )
                }

            // ===== דיאלוג הסבר בעקבות חיפוש / מידע =====
            explainTriple?.let { (b, t, iRaw) ->

                val cleanItemForResolver = remember(t, iRaw) {
                    cleanItem(t, iRaw).trim()
                }

                val resolvedIdentity = remember(b, t, cleanItemForResolver) {
                    ExerciseIdentityRegistry.resolve(
                        belt = b,
                        hebrewTitle = cleanItemForResolver,
                        topicKey = t.trim().ifBlank { null }
                    )
                }

                val dialogActionId =
                    remember(b, t, iRaw, cleanItemForResolver, resolvedIdentity.id) {
                        if (resolvedIdentity.isKnown) {
                            resolvedIdentity.id
                        } else {
                            CanonicalIds.resolveCanonicalForExplanation(
                                belt = b,
                                topicTitle = t,
                                rawItemFromRepo = iRaw
                            )
                        }
                    }

                val explanation = remember(b, t, cleanItemForResolver, isEnglish) {
                    ExerciseExplanationResolver.get(
                        belt = b,
                        topic = t,
                        item = cleanItemForResolver,
                        isEnglish = isEnglish
                    ).trim()
                }.ifBlank {
                    if (isEnglish) {
                        "No explanation found for \"$cleanItemForResolver\"."
                    } else {
                        "לא נמצא הסבר עבור \"$cleanItemForResolver\"."
                    }
                }

                val dialogTitle = itemTitleForUi(
                    topic = t,
                    rawItem = cleanItemForResolver,
                    lang = currentLang
                )

                val dialogBeltLabel = if (isEnglish) {
                    "(${b.en} belt)"
                } else {
                    "(${b.heb})"
                }

                val dialogNoteText = remember(dialogActionId, notesRefreshKey) {
                    loadNote(dialogActionId)
                }

                ExerciseExplanationDialog(
                    title = dialogTitle,
                    beltLabel = dialogBeltLabel,
                    explanation = explanation,
                    noteText = dialogNoteText,
                    isFavorite = favorites.contains(dialogActionId) ||
                            isFavoriteByAliases(
                                topicTitle = t,
                                rawItem = iRaw
                            ),
                    accentColor = b.color,
                    isEnglish = isEnglish,
                    onDismiss = { explainTriple = null },
                    onEditNote = {
                        noteEditorFor = dialogActionId
                        noteDraft = loadNote(dialogActionId)
                    },
                    onDeleteNote = {
                        noteDraft = ""
                        saveNote(dialogActionId, "")
                    },
                    onToggleFavorite = {
                        toggleFavoriteAliases(
                            topicTitle = t,
                            rawItem = iRaw
                        )
                    }
                )
            }
            // ===== סוף הדיאלוג =====

            noteEditorFor?.let { itemId ->
                ExerciseNoteEditorDialog(
                    exerciseTitle =
                        explainTriple
                            ?.third
                            ?.let { rawTitle ->
                                itemTitleForUi(
                                    topic = explainTriple?.second.orEmpty(),
                                    rawItem = rawTitle,
                                    lang = currentLang
                                )
                            }
                            .orEmpty(),
                    noteText = noteDraft,
                    isEnglish = isEnglish,
                    accentColor = belt.color,
                    onNoteChange = { noteDraft = it },
                    onDismiss = {
                        noteEditorFor = null
                    },
                    onSave = {
                        val cleanNote = noteDraft.trim()
                        noteDraft = cleanNote
                        saveNote(itemId, cleanNote)
                        noteEditorFor = null
                    }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize(),
                horizontalAlignment = Alignment.End
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(
                            if (isDarkSurface) {
                                MaterialTheme.colorScheme.background
                            } else {
                                belt.lightColor
                            }
                        )
                        .padding(
                            top = 4.dp,
                            start = 12.dp,
                            end = 12.dp
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                        horizontalAlignment = Alignment.End
                    ) {

                        if (!isShowingNestedSubTopicPicker && itemList.isNotEmpty()) {
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text =
                                        if (isEnglish) {
                                            "← Swipe sideways to see more stats →"
                                        } else {
                                            "→→ הזז לצד כדי לראות עוד נתונים →→"
                                        },
                                    style = KmiTypography.caption.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color =
                                        if (isDarkSurface) {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        } else {
                                            Color(0xFF5B6472)
                                        },
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            top = 4.dp,
                                            bottom = 2.dp
                                        )
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (effectiveIsCoach) {
                                        MaterialsTopStatChip(
                                            value = coachPracticedCount.toString(),
                                            label = if (isEnglish) "Practiced" else "תורגל",
                                            containerColor = Color(0xFF6FC47D),
                                            contentColor = Color.White
                                        )

                                        MaterialsTopStatChip(
                                            value = coachNeedsReinforcementCount.toString(),
                                            label = if (isEnglish) {
                                                "Reinforce"
                                            } else {
                                                "נדרש חיזוק"
                                            },
                                            containerColor = Color(0xFF3677DF),
                                            contentColor = Color.White,
                                            minWidth = 76.dp,
                                            horizontalPadding = 10.dp
                                        )

                                        MaterialsTopStatChip(
                                            value = coachTaughtCount.toString(),
                                            label = if (isEnglish) "Taught" else "נלמד",
                                            containerColor = Color(0xFFF3A062),
                                            contentColor = Color.White
                                        )

                                        MaterialsTopStatChip(
                                            value = coachNotTaughtCount.toString(),
                                            label = if (isEnglish) {
                                                "Not taught"
                                            } else {
                                                "לא נלמד"
                                            },
                                            containerColor = Color(0xFFE59AB1),
                                            contentColor = Color.White,
                                            minWidth = 72.dp,
                                            horizontalPadding = 10.dp
                                        )
                                    } else {
                                        MaterialsTopStatChip(
                                            value = summaryTotalCount.toString(),
                                            label = if (isEnglish) "Exercises" else "תרגילים",
                                            containerColor = Color(0xFF98A2B3),
                                            contentColor = Color.White
                                        )

                                        MaterialsTopStatChip(
                                            value =
                                                summaryMasteredCount
                                                    .toString(),
                                            label =
                                                if (isEnglish) {
                                                    "Known"
                                                } else {
                                                    "יודע"
                                                },
                                            containerColor =
                                                Color(0xFF7ACB88),
                                            contentColor =
                                                Color.White,
                                            minWidth = 64.dp,
                                            horizontalPadding =
                                                12.dp
                                        )

                                        MaterialsTopStatChip(
                                            value =
                                                summaryPartiallyKnownCount
                                                    .toString(),
                                            label =
                                                if (isEnglish) {
                                                    "Partial"
                                                } else {
                                                    "חלקית"
                                                },
                                            containerColor =
                                                Color(0xFFF28C28),
                                            contentColor =
                                                Color.White,
                                            minWidth = 64.dp,
                                            horizontalPadding =
                                                10.dp
                                        )

                                        MaterialsTopStatChip(
                                            value =
                                                summaryUnknownCount
                                                    .toString(),
                                            label = if (isEnglish) "Unknown" else "לא יודע",
                                            containerColor = Color(0xFFF1A97A),
                                            contentColor = Color.White
                                        )

                                        MaterialsTopStatChip(
                                            value = summaryFavoritesCount.toString(),
                                            label = if (isEnglish) "Favorites" else "מועדפים",
                                            containerColor = Color(0xFFE7A3B5),
                                            contentColor = Color.White
                                        )

                                        MaterialsTopStatChip(
                                            value = summaryExcludedCount.toString(),
                                            label = if (isEnglish) "Excluded" else "מוחרגים",
                                            containerColor = Color(0xFF95D69A),
                                            contentColor = Color.White
                                        )

                                        MaterialsTopStatChip(
                                            value = summaryNotesCount.toString(),
                                            label = if (isEnglish) "Notes" else "הערות",
                                            containerColor = Color(0xFF8596C9),
                                            contentColor = Color.White
                                        )
                                    }
                                }

                                Text(
                                    text =
                                        if (isEnglish) {
                                            "More cards are available off-screen"
                                        } else {
                                            "יש עוד כרטיסים בהמשך הגלילה"
                                        },
                                    style = KmiTypography.caption.copy(
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = Color(0xFF7A8392),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            top = 0.dp,
                                            bottom = 4.dp
                                        )
                                )
                            }

                            Spacer(Modifier.height(4.dp))
                        }

                        if (isShowingNestedSubTopicPicker) {
                            nestedSubTopicTitles.forEach { nestedTitle ->
                                val count =
                                    remember(belt, topicUi, decodedSubTopicFilter, nestedTitle) {
                                        decodedSubTopicFilter
                                            ?.let { sub ->
                                                SharedContentRepo.getNestedItemsFor(
                                                    belt = belt,
                                                    topicTitle = materialRootTopic.trim(),
                                                    subTopicTitle = sub.trim(),
                                                    nestedSubTopicTitle = nestedTitle.trim()
                                                ).size
                                            }
                                            ?: 0
                                    }

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 90.dp)
                                        .padding(
                                            horizontal = 8.dp,
                                            vertical = 6.dp
                                        )
                                        .clickable {
                                            openedNestedSubTopic = nestedTitle
                                        },
                                    shape = RoundedCornerShape(20.dp),
                                    color =
                                        if (isDarkSurface) {
                                            MaterialTheme.colorScheme.surfaceVariant
                                                .copy(alpha = 0.96f)
                                        } else {
                                            Color.White.copy(alpha = 0.92f)
                                        },
                                    tonalElevation = 2.dp,
                                    shadowElevation = 3.dp,
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = belt.color.copy(
                                            alpha =
                                                if (isDarkSurface) {
                                                    0.55f
                                                } else {
                                                    0.35f
                                                }
                                        )
                                    )
                                ) {
                                    CompositionLocalProvider(
                                        LocalLayoutDirection provides if (isEnglish) {
                                            LayoutDirection.Ltr
                                        } else {
                                            LayoutDirection.Rtl
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text =
                                                    topicTitleForUi(
                                                        nestedTitle,
                                                        currentLang
                                                    ),
                                                style = KmiTypography.cardTitle,
                                                textAlign =
                                                    if (isEnglish) {
                                                        TextAlign.Left
                                                    } else {
                                                        TextAlign.Right
                                                    },
                                                color =
                                                    if (isDarkSurface) {
                                                        MaterialTheme.colorScheme.onSurface
                                                    } else {
                                                        Color(0xFF1F2937)
                                                    },
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )

                                            Spacer(Modifier.width(10.dp))

                                            Text(
                                                text =
                                                    if (isEnglish) {
                                                        if (count == 1) {
                                                            "1 exercise"
                                                        } else {
                                                            "$count exercises"
                                                        }
                                                    } else {
                                                        "$count תרגילים"
                                                    },
                                                style = KmiTypography.caption.copy(
                                                    fontWeight =
                                                        FontWeight.ExtraBold
                                                ),
                                                color = belt.color,
                                                textAlign =
                                                    if (isEnglish) {
                                                        TextAlign.Right
                                                    } else {
                                                        TextAlign.Left
                                                    },
                                                maxLines = 2,
                                                overflow =
                                                    TextOverflow.Ellipsis,
                                                modifier =
                                                    Modifier.widthIn(
                                                        min = 74.dp
                                                    )
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            val filtered = itemList

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(0.dp),
                                contentPadding = PaddingValues(bottom = 12.dp)
                            ) {
                                filtered.forEachIndexed { index, item ->
                                    val currentSectionTitle = nestedSectionTitleByItem[item.trim()]
                                    val previousSectionTitle = filtered
                                        .getOrNull(index - 1)
                                        ?.trim()
                                        ?.let { previousItem -> nestedSectionTitleByItem[previousItem] }

                                    if (
                                        !currentSectionTitle.isNullOrBlank() &&
                                        currentSectionTitle != previousSectionTitle
                                    ) {
                                        stickyHeader(
                                            key = "section_${currentSectionTitle}_${index}"
                                        ) {
                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                color =
                                                    if (isDarkSurface) {
                                                        MaterialTheme.colorScheme.surface
                                                    } else {
                                                        belt.lightColor
                                                    },
                                                tonalElevation = 0.dp,
                                                shadowElevation = 0.dp
                                            ) {
                                                val displaySectionTitle =
                                                    topicTitleForUi(
                                                        currentSectionTitle,
                                                        currentLang
                                                    )

                                                val sectionGeneralNote =
                                                    remember(
                                                        belt,
                                                        materialRootTopic,
                                                        materialParentSubTopic,
                                                        currentSectionTitle
                                                    ) {
                                                        materialParentSubTopic
                                                            ?.let { parent ->
                                                                SharedContentRepo
                                                                    .getNestedSubTopicGeneralNote(
                                                                        belt = belt,
                                                                        topicTitle =
                                                                            materialRootTopic,
                                                                        subTopicTitle =
                                                                            parent,
                                                                        nestedSubTopicTitle =
                                                                            currentSectionTitle
                                                                    )
                                                            }
                                                    }

                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(
                                                            if (isDarkSurface) {
                                                                MaterialTheme
                                                                    .colorScheme
                                                                    .surface
                                                            } else {
                                                                belt.lightColor
                                                            }
                                                        )
                                                ) {
                                                    Text(
                                                        text =
                                                            displaySectionTitle,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .absolutePadding(
                                                                left = 48.dp,
                                                                right = 8.dp,
                                                                top = 8.dp,
                                                                bottom = 6.dp
                                                            ),
                                                        textAlign =
                                                            if (isEnglish) {
                                                                TextAlign.Left
                                                            } else {
                                                                TextAlign.Right
                                                            },
                                                        style =
                                                            KmiTypography
                                                                .cardTitle,
                                                        color =
                                                            if (isDarkSurface) {
                                                                MaterialTheme
                                                                    .colorScheme
                                                                    .primary
                                                            } else {
                                                                belt.color
                                                            }
                                                    )

                                                    if (
                                                        !sectionGeneralNote
                                                            .isNullOrBlank()
                                                    ) {
                                                        Surface(
                                                            modifier = Modifier
                                                                .align(
                                                                    AbsoluteAlignment
                                                                        .CenterLeft
                                                                )
                                                                .padding(
                                                                    start = 12.dp
                                                                )
                                                                .size(30.dp)
                                                                .clickable {
                                                                    nestedGeneralNoteTitle =
                                                                        displaySectionTitle

                                                                    nestedGeneralNoteText =
                                                                        sectionGeneralNote
                                                                },
                                                            shape = CircleShape,
                                                            color =
                                                                if (
                                                                    isDarkSurface
                                                                ) {
                                                                    Color(
                                                                        0xFF2563EB
                                                                    )
                                                                        .copy(
                                                                            alpha =
                                                                                0.24f
                                                                        )
                                                                } else {
                                                                    Color(
                                                                        0xFFE8F1FF
                                                                    )
                                                                },
                                                            border =
                                                                BorderStroke(
                                                                    width = 1.dp,
                                                                    color =
                                                                        Color(
                                                                            0xFF2563EB
                                                                        )
                                                                            .copy(
                                                                                alpha =
                                                                                    0.45f
                                                                            )
                                                                ),
                                                            tonalElevation = 0.dp,
                                                            shadowElevation = 0.dp
                                                        ) {
                                                            Box(
                                                                contentAlignment =
                                                                    Alignment.Center
                                                            ) {
                                                                Icon(
                                                                    imageVector =
                                                                        Icons.Filled
                                                                            .Info,
                                                                    contentDescription =
                                                                        if (
                                                                            isEnglish
                                                                        ) {
                                                                            "General note"
                                                                        } else {
                                                                            "הערה כללית"
                                                                        },
                                                                    tint =
                                                                        if (
                                                                            isDarkSurface
                                                                        ) {
                                                                            Color(
                                                                                0xFF60A5FA
                                                                            )
                                                                        } else {
                                                                            Color(
                                                                                0xFF2563EB
                                                                            )
                                                                        },
                                                                    modifier =
                                                                        Modifier.size(
                                                                            18.dp
                                                                        )
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    item(
                                        key = "${index}_${item.hashCode()}"
                                    ) {
                                        var showNoteDialog by remember { mutableStateOf(false) }

                                        // ✅ מזהה אחיד להסבר / הערות / החרגות
                                        val canonicalId = remember(item, belt.id, topicUi) {
                                            canonicalFor(item)
                                        }

                                        // ✅ מועדפים לפי כל המזהים האפשריים של אותו תרגיל,
                                        // כדי שכוכבית בכרטיס הגלובאלי ותפריט ה-i יהיו מסונכרנים.
                                        val isFavorite = remember(
                                            favorites,
                                            item,
                                            belt.id,
                                            materialRootTopic,
                                            topicKey
                                        ) {
                                            isFavoriteByAliases(
                                                topicTitle = materialRootTopic,
                                                rawItem = item
                                            )
                                        }

                                        // ✅ מזהה לסימון יודע/לא יודע בלבד.
                                        // אם canonicalId כפול בין כמה שורות, statusId מפריד ביניהן לפי מיקום השורה.
                                        val statusId =
                                            remember(index, item, belt.id, topicKey, topicUi) {
                                                statusIdFor(index, item)
                                            }

                                        // ✅ טקסט לתצוגה בלבד
                                        val displayName = remember(item, topicUi, currentLang) {
                                            itemTitleForUi(topicUi, item, currentLang)
                                        }

                                        var noteText by remember(
                                            item,
                                            belt.id,
                                            excludedKeySuffix,
                                            notesRefreshKey
                                        ) {
                                            mutableStateOf(loadNote(canonicalId))
                                        }

                                        val mastered: Boolean? =
                                            if (itemStates.containsKey(statusId)) {
                                                itemStates[statusId]
                                            } else {
                                                when {
                                                    masteredSet.contains(statusId) -> true
                                                    unknowns.contains(statusId) -> false
                                                    else -> null
                                                }
                                            }

                                        val isExcluded = excludedItems.contains(canonicalId)
                                        val isHighlighted =
                                            highlight != null && canonicalId == highlight

                                        val bringer =
                                            remember { BringIntoViewRequester() }
                                        LaunchedEffect(isHighlighted) {
                                            if (isHighlighted) {
                                                delay(120)
                                                bringer.bringIntoView()
                                            }
                                        }

                                        var pressed by remember { mutableStateOf(false) }
                                        val scale by animateFloatAsState(
                                            targetValue = if (pressed) 1.2f else 1f,
                                            label = "scaleAnim"
                                        )

                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .scale(scale)
                                                .bringIntoViewRequester(bringer)
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(min = 48.dp)
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {

                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(
                                                            start = 4.dp,
                                                            end = 6.dp
                                                        ),
                                                    horizontalAlignment =
                                                        if (isEnglish) {
                                                            Alignment.Start
                                                        } else {
                                                            Alignment.End
                                                        }
                                                ) {
                                                    CompositionLocalProvider(
                                                        LocalLayoutDirection provides if (isEnglish) {
                                                            LayoutDirection.Ltr
                                                        } else {
                                                            LayoutDirection.Rtl
                                                        }
                                                    ) {
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(
                                                                    top = 2.dp,
                                                                    bottom = 2.dp
                                                                ),
                                                            verticalAlignment = Alignment.Top
                                                        ) {
                                                            Text(
                                                                text = displayName,
                                                                textAlign = TextAlign.Start,
                                                                modifier = Modifier
                                                                    .weight(1f)
                                                                    .clickable {
                                                                        pressed = true

                                                                        explainTriple =
                                                                            Triple(
                                                                                belt,
                                                                                materialRootTopic,
                                                                                item
                                                                            )

                                                                        scope.launch {
                                                                            delay(150)
                                                                            pressed = false
                                                                        }
                                                                    },
                                                                color =
                                                                    when {
                                                                        isExcluded ->
                                                                            MaterialTheme.colorScheme
                                                                                .onSurfaceVariant
                                                                                .copy(alpha = 0.55f)

                                                                        isHighlighted ->
                                                                            if (isDarkSurface) {
                                                                                MaterialTheme.colorScheme
                                                                                    .primary
                                                                            } else {
                                                                                belt.color.copy(
                                                                                    alpha = 0.95f
                                                                                )
                                                                            }

                                                                        isDarkSurface ->
                                                                            MaterialTheme.colorScheme
                                                                                .onSurface

                                                                        else ->
                                                                            Color(0xFF111827)
                                                                    },
                                                                style =
                                                                    KmiTypography.body.copy(
                                                                        fontWeight =
                                                                            if (isHighlighted) {
                                                                                FontWeight.Bold
                                                                            } else {
                                                                                FontWeight.SemiBold
                                                                            }
                                                                    ),
                                                                maxLines = 3,
                                                                overflow =
                                                                    TextOverflow.Ellipsis
                                                            )

                                                            if (isFavorite) {
                                                                Spacer(
                                                                    Modifier.width(6.dp)
                                                                )

                                                                ExerciseMetaBadge(
                                                                    text =
                                                                        if (isEnglish) {
                                                                            "Favorite"
                                                                        } else {
                                                                            "מועדף"
                                                                        },
                                                                    containerColor =
                                                                        Color(0xFFF9D9B8),
                                                                    contentColor =
                                                                        Color(0xFF9A5A00)
                                                                )
                                                            }

                                                            if (isExcluded) {
                                                                Spacer(
                                                                    Modifier.width(6.dp)
                                                                )

                                                                ExerciseMetaBadge(
                                                                    text =
                                                                        if (isEnglish) {
                                                                            "Excluded"
                                                                        } else {
                                                                            "מוחרג"
                                                                        },
                                                                    containerColor =
                                                                        if (isDarkSurface) {
                                                                            MaterialTheme.colorScheme
                                                                                .surfaceVariant
                                                                        } else {
                                                                            Color(0xFFE5E7EB)
                                                                        },
                                                                    contentColor =
                                                                        if (isDarkSurface) {
                                                                            MaterialTheme.colorScheme
                                                                                .onSurfaceVariant
                                                                        } else {
                                                                            Color(0xFF6B7280)
                                                                        }
                                                                )
                                                            }

                                                            if (noteText.isNotBlank()) {
                                                                Spacer(
                                                                    Modifier.width(6.dp)
                                                                )

                                                                ExerciseMetaBadge(
                                                                    text =
                                                                        if (isEnglish) {
                                                                            "Note"
                                                                        } else {
                                                                            "הערה"
                                                                        },
                                                                    containerColor =
                                                                        if (isDarkSurface) {
                                                                            Color(0xFF5B4A22)
                                                                        } else {
                                                                            Color(0xFFFFE7B3)
                                                                        },
                                                                    contentColor =
                                                                        if (isDarkSurface) {
                                                                            Color(0xFFFFD978)
                                                                        } else {
                                                                            Color(0xFF8A5A00)
                                                                        }
                                                                )
                                                            }
                                                        }
                                                    }
                                                }

                                                Spacer(
                                                    Modifier.height(6.dp)
                                                )

                                                Box(
                                                    modifier =
                                                        Modifier.fillMaxWidth(),
                                                    contentAlignment =
                                                        if (isEnglish) {
                                                            Alignment.CenterStart
                                                        } else {
                                                            Alignment.CenterEnd
                                                        }
                                                ) {
                                                    if (effectiveIsCoach) {
                                                        val coachProgress =
                                                            coachProgressStates[statusId]
                                                                ?: CoachMaterialProgress()

                                                        CoachMaterialStatusSelector(
                                                            progress = coachProgress,
                                                            isEnglish = isEnglish,
                                                            excluded = isExcluded,
                                                            isFav = isFavorite,
                                                            hasNote = noteText.isNotBlank(),
                                                            onToggleExclude = {
                                                                toggleExclude(canonicalId)
                                                            },
                                                            onInfo = {
                                                                pressed = true

                                                                explainTriple = Triple(
                                                                    belt,
                                                                    materialRootTopic,
                                                                    item
                                                                )

                                                                scope.launch {
                                                                    delay(150)
                                                                    pressed = false
                                                                }
                                                            },
                                                            onToggleFavorite = {
                                                                toggleFavoriteAliases(
                                                                    topicTitle = materialRootTopic,
                                                                    rawItem = item
                                                                )
                                                            },
                                                            onEditNote = {
                                                                showNoteDialog = true
                                                            },
                                                            onSelect = { selectedStatus ->
                                                                saveCoachProgress(
                                                                    statusId = statusId,
                                                                    status = selectedStatus
                                                                )
                                                            }
                                                        )
                                                    } else {
                                                        val traineeUpdatedAt =
                                                            traineeUpdatedAtStates[
                                                                statusId
                                                            ] ?: loadTraineeUpdatedAt(
                                                                statusId
                                                            )

                                                        val traineeDateText =
                                                            if (
                                                                mastered != null &&
                                                                traineeUpdatedAt > 0L
                                                            ) {
                                                                SimpleDateFormat(
                                                                    "dd/MM/yy",
                                                                    Locale.getDefault()
                                                                ).format(
                                                                    Date(traineeUpdatedAt)
                                                                )
                                                            } else {
                                                                ""
                                                            }

                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(
                                                                    IntrinsicSize.Min
                                                                ),
                                                            horizontalArrangement =
                                                                Arrangement.spacedBy(
                                                                    7.dp
                                                                ),
                                                            verticalAlignment =
                                                                Alignment.Top
                                                        ) {

                                                            Column(
                                                                modifier = Modifier
                                                                    .weight(0.82f)
                                                                    .fillMaxHeight()
                                                                    .background(
                                                                        color =
                                                                            Color(
                                                                                0xFF1976D2
                                                                            ).copy(
                                                                                alpha =
                                                                                    0.055f
                                                                            ),
                                                                        shape =
                                                                            RoundedCornerShape(
                                                                                16.dp
                                                                            )
                                                                    )
                                                                    .border(
                                                                        border =
                                                                            BorderStroke(
                                                                                width =
                                                                                    1.dp,
                                                                                color =
                                                                                    Color(
                                                                                        0xFF1976D2
                                                                                    ).copy(
                                                                                        alpha =
                                                                                            0.28f
                                                                                    )
                                                                            ),
                                                                        shape =
                                                                            RoundedCornerShape(
                                                                                16.dp
                                                                            )
                                                                    )
                                                                    .padding(
                                                                        horizontal =
                                                                            4.dp,
                                                                        vertical =
                                                                            6.dp
                                                                    ),
                                                                horizontalAlignment =
                                                                    Alignment.CenterHorizontally
                                                            ) {
                                                                ItemFloatingActions(
                                                                    isEnglish = isEnglish,
                                                                    excluded = isExcluded,
                                                                    isFav = isFavorite,
                                                                    hasNote = noteText.isNotBlank(),
                                                                    onToggleExclude = {
                                                                        toggleExclude(canonicalId)
                                                                    },
                                                                    onInfo = {
                                                                        pressed = true

                                                                        explainTriple = Triple(
                                                                            belt,
                                                                            materialRootTopic,
                                                                            item
                                                                        )

                                                                        scope.launch {
                                                                            delay(150)
                                                                            pressed = false
                                                                        }
                                                                    },
                                                                    onToggleFavorite = {
                                                                        toggleFavoriteAliases(
                                                                            topicTitle = materialRootTopic,
                                                                            rawItem = item
                                                                        )
                                                                    },
                                                                    onEditNote = {
                                                                        showNoteDialog = true
                                                                    }
                                                                )

                                                                Spacer(
                                                                    Modifier.height(2.dp)
                                                                )

                                                                Text(
                                                                    text =
                                                                        if (isEnglish) {
                                                                            "Info"
                                                                        } else {
                                                                            "מידע"
                                                                        },
                                                                    style =
                                                                        KmiTypography.caption.copy(
                                                                            fontWeight =
                                                                                FontWeight.SemiBold
                                                                        ),
                                                                    color =
                                                                        Color(0xFF1976D2),
                                                                    textAlign =
                                                                        TextAlign.Center,
                                                                    maxLines = 1
                                                                )
                                                            }

                                                            Column(
                                                                modifier = Modifier
                                                                    .weight(3f)
                                                                    .fillMaxHeight()
                                                                    .background(
                                                                        color =
                                                                            MaterialTheme
                                                                                .colorScheme
                                                                                .surfaceVariant
                                                                                .copy(
                                                                                    alpha =
                                                                                        0.32f
                                                                                ),
                                                                        shape =
                                                                            RoundedCornerShape(
                                                                                18.dp
                                                                            )
                                                                    )
                                                                    .border(
                                                                        border =
                                                                            BorderStroke(
                                                                                width =
                                                                                    1.dp,
                                                                                color =
                                                                                    MaterialTheme
                                                                                        .colorScheme
                                                                                        .outline
                                                                                        .copy(
                                                                                            alpha =
                                                                                                0.22f
                                                                                        )
                                                                            ),
                                                                        shape =
                                                                            RoundedCornerShape(
                                                                                18.dp
                                                                            )
                                                                    )
                                                                    .padding(
                                                                        horizontal =
                                                                            4.dp,
                                                                        vertical =
                                                                            6.dp
                                                                    ),
                                                                horizontalAlignment =
                                                                    Alignment.CenterHorizontally,
                                                                verticalArrangement =
                                                                    Arrangement.Center
                                                            ) {
                                                                Box(
                                                                    modifier =
                                                                        Modifier.scale(0.82f),
                                                                    contentAlignment =
                                                                        Alignment.Center
                                                                ) {
                                                                    TraineeMaterialStatusSelector(
                                                                        selectedStatus =
                                                                            traineeMaterialStatusFor(
                                                                                statusId =
                                                                                    statusId,
                                                                                mastered =
                                                                                    mastered
                                                                            ),
                                                                        dateText =
                                                                            traineeDateText,
                                                                        isEnglish =
                                                                            isEnglish,
                                                                        onSelect = { selectedStatus ->

                                                                            val newVal:
                                                                                    Boolean? =
                                                                                when (
                                                                                    selectedStatus
                                                                                ) {
                                                                                    TraineeMaterialStatus.KNOWN ->
                                                                                        true

                                                                                    TraineeMaterialStatus.PARTIALLY_KNOWN ->
                                                                                        false

                                                                                    TraineeMaterialStatus.UNKNOWN ->
                                                                                        false

                                                                                    null ->
                                                                                        null
                                                                                }

                                                                            /*
                                                                             * המצב החלקי נשמר בנוסף
                                                                             * לסימון הבינארי הקיים.
                                                                             *
                                                                             * כך מסכי הסיכום ממשיכים
                                                                             * כרגע לעבוד ללא שינוי.
                                                                             */
                                                                            setPartiallyKnownLocal(
                                                                                id =
                                                                                    statusId,
                                                                                set =
                                                                                    selectedStatus ==
                                                                                            TraineeMaterialStatus
                                                                                                .PARTIALLY_KNOWN
                                                                            )

                                                                            pendingItemStates[statusId] =
                                                                                newVal
                                                                            itemStates[statusId] =
                                                                                newVal

                                                                            /*
                                                                             * שומר את זמן השינוי של ✓ / ✗.
                                                                             * באיפוס ל-null התאריך נמחק.
                                                                             */
                                                                            saveTraineeUpdatedAt(
                                                                                statusId = statusId,
                                                                                value = newVal
                                                                            )

                                                                            val nextMasteredSet =
                                                                                masteredSet.toMutableSet()

                                                                            val nextUnknownSet =
                                                                                unknowns.toMutableSet()

                                                                            when (newVal) {
                                                                                true -> {
                                                                                    nextMasteredSet.add(
                                                                                        statusId
                                                                                    )
                                                                                    nextUnknownSet.remove(
                                                                                        statusId
                                                                                    )
                                                                                }

                                                                                false -> {
                                                                                    nextUnknownSet.add(
                                                                                        statusId
                                                                                    )
                                                                                    nextMasteredSet.remove(
                                                                                        statusId
                                                                                    )
                                                                                }

                                                                                null -> {
                                                                                    nextMasteredSet.remove(
                                                                                        statusId
                                                                                    )
                                                                                    nextUnknownSet.remove(
                                                                                        statusId
                                                                                    )
                                                                                }
                                                                            }

                                                                            masteredSet =
                                                                                nextMasteredSet
                                                                            unknowns =
                                                                                nextUnknownSet

                                                                            val statusTopicKeys =
                                                                                if (subTopicFilter.isNullOrBlank()) {
                                                                                    listOf(
                                                                                        topicKey,
                                                                                        topicUi,
                                                                                        "כללי"
                                                                                    )
                                                                                } else {
                                                                                    listOf(topicKey)
                                                                                }
                                                                                    .map { key ->
                                                                                        key.trim()
                                                                                    }
                                                                                    .filter { key ->
                                                                                        key.isNotBlank()
                                                                                    }
                                                                                    .distinct()

                                                                            /*
                                                                             * האייקון והמונים כבר עודכנו
                                                                             * באופן מקומי מעל החלק הזה.
                                                                             *
                                                                             * הסנכרון ל-Firestore אינו צריך
                                                                             * להתבצע מחדש בכל לחיצה מהירה.
                                                                             */
                                                                            progressSyncJob?.cancel()

                                                                            progressSyncJob =
                                                                                scope.launch {
                                                                                    delay(650L)

                                                                                    withContext(
                                                                                        Dispatchers.IO
                                                                                    ) {
                                                                                        runCatching {
                                                                                            UserProgressRepository
                                                                                                .syncCurrentUserBeltProgress(
                                                                                                    vm = vm,
                                                                                                    belt = belt
                                                                                                )
                                                                                        }
                                                                                    }
                                                                                }

                                                                            scope.launch(Dispatchers.IO) {
                                                                                statusTopicKeys.forEach { topicKeyToSave ->
                                                                                    vm.setItemStatusNullable(
                                                                                        belt = belt,
                                                                                        topic = topicKeyToSave,
                                                                                        item = statusId,
                                                                                        value = newVal
                                                                                    )
                                                                                }

                                                                                val editor =
                                                                                    sp.edit()

                                                                                statusTopicKeys.forEach { topicKeyToSave ->
                                                                                    val masteredPreferenceKey =
                                                                                        "mastered_${belt.id}_${topicKeyToSave}"

                                                                                    val unknownPreferenceKey =
                                                                                        "unknown_${belt.id}_${topicKeyToSave}"

                                                                                    val partiallyKnownPreferenceKey =
                                                                                        "partially_known_${belt.id}_${topicKeyToSave}"

                                                                                    val savedMastered =
                                                                                        (
                                                                                                sp.getStringSet(
                                                                                                    masteredPreferenceKey,
                                                                                                    emptySet()
                                                                                                )
                                                                                                    ?: emptySet()
                                                                                                ).toMutableSet()

                                                                                    val savedUnknown =
                                                                                        (
                                                                                                sp.getStringSet(
                                                                                                    unknownPreferenceKey,
                                                                                                    emptySet()
                                                                                                )
                                                                                                    ?: emptySet()
                                                                                                ).toMutableSet()


                                                                                    val savedPartiallyKnown =
                                                                                        (
                                                                                                sp.getStringSet(
                                                                                                    partiallyKnownPreferenceKey,
                                                                                                    emptySet()
                                                                                                )
                                                                                                    ?: emptySet()
                                                                                                ).toMutableSet()

                                                                                    when (
                                                                                        selectedStatus
                                                                                    ) {
                                                                                        TraineeMaterialStatus.KNOWN -> {
                                                                                            savedMastered.add(
                                                                                                statusId
                                                                                            )
                                                                                            savedUnknown.remove(
                                                                                                statusId
                                                                                            )
                                                                                            savedPartiallyKnown.remove(
                                                                                                statusId
                                                                                            )
                                                                                        }

                                                                                        TraineeMaterialStatus.PARTIALLY_KNOWN -> {
                                                                                            savedMastered.remove(
                                                                                                statusId
                                                                                            )
                                                                                            savedUnknown.add(
                                                                                                statusId
                                                                                            )
                                                                                            savedPartiallyKnown.add(
                                                                                                statusId
                                                                                            )
                                                                                        }

                                                                                        TraineeMaterialStatus.UNKNOWN -> {
                                                                                            savedMastered.remove(
                                                                                                statusId
                                                                                            )
                                                                                            savedUnknown.add(
                                                                                                statusId
                                                                                            )
                                                                                            savedPartiallyKnown.remove(
                                                                                                statusId
                                                                                            )
                                                                                        }

                                                                                        null -> {
                                                                                            savedMastered.remove(
                                                                                                statusId
                                                                                            )
                                                                                            savedUnknown.remove(
                                                                                                statusId
                                                                                            )
                                                                                            savedPartiallyKnown.remove(
                                                                                                statusId
                                                                                            )
                                                                                        }
                                                                                    }

                                                                                    editor.putStringSet(
                                                                                        masteredPreferenceKey,
                                                                                        savedMastered
                                                                                    )

                                                                                    editor.putStringSet(
                                                                                        unknownPreferenceKey,
                                                                                        savedUnknown
                                                                                    )

                                                                                    editor.putStringSet(
                                                                                        partiallyKnownPreferenceKey,
                                                                                        savedPartiallyKnown
                                                                                    )
                                                                                }

                                                                                editor.apply()

                                                                                /*
                                                                                 * השמירה המקומית הסתיימה.
                                                                                 * סנכרון Firestore מתבצע
                                                                                 * בנפרד לאחר רצף הלחיצות.
                                                                                 */
                                                                                withContext(
                                                                                    Dispatchers.Main.immediate
                                                                                ) {
                                                                                    if (
                                                                                        pendingItemStates.containsKey(
                                                                                            statusId
                                                                                        ) &&
                                                                                        pendingItemStates[statusId] == newVal
                                                                                    ) {
                                                                                        pendingItemStates.remove(
                                                                                            statusId
                                                                                        )
                                                                                        itemStates[statusId] =
                                                                                            newVal
                                                                                    }
                                                                                }
                                                                            }
                                                                        }
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            // קו מפריד בין התרגיל הנוכחי לתרגיל הבא.
                                            // הקו לא מוצג אחרי התרגיל האחרון ברשימה.
                                            if (index < filtered.lastIndex) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(
                                                            horizontal = 14.dp
                                                        )
                                                        .height(1.dp)
                                                        .background(
                                                            color =
                                                                if (isDarkSurface) {
                                                                    MaterialTheme.colorScheme
                                                                        .onSurface
                                                                        .copy(alpha = 0.26f)
                                                                } else {
                                                                    Color(0xFF607D8B)
                                                                        .copy(alpha = 0.42f)
                                                                }
                                                        )
                                                )
                                            }

                                            // דיאלוג הערה
                                            if (showNoteDialog) {
                                                ExerciseNoteEditorDialog(
                                                    exerciseTitle =
                                                        itemTitleForUi(
                                                            topic = topicUi,
                                                            rawItem = item,
                                                            lang = currentLang
                                                        ),
                                                    noteText = noteText,
                                                    isEnglish = isEnglish,
                                                    accentColor = belt.color,
                                                    onNoteChange = { noteText = it },
                                                    onDismiss = {
                                                        showNoteDialog = false
                                                    },
                                                    onSave = {
                                                        val cleanNote = noteText.trim()
                                                        noteText = cleanNote
                                                        saveNote(canonicalId, cleanNote)
                                                        showNoteDialog = false
                                                    }
                                                )
                                            }

                                            Spacer(Modifier.height(0.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class MaterialPdfItem(
    val number: Int,
    val title: String,
    val status: String,
    val isFavorite: Boolean,
    val isExcluded: Boolean,
    val hasNote: Boolean
)

private fun shareMaterialsPdf(
    context: Context,
    belt: Belt,
    topicTitle: String,
    items: List<MaterialPdfItem>,
    isEnglish: Boolean,
    isCoach: Boolean
) {
    val pdfFile = createMaterialsPdf(
        context = context,
        belt = belt,
        topicTitle = topicTitle,
        items = items,
        isEnglish = isEnglish,
        isCoach = isCoach
    )

    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        pdfFile
    )

    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(
            Intent.EXTRA_SUBJECT,
            if (isEnglish) {
                "Exercises - $topicTitle"
            } else {
                "תרגילים - $topicTitle"
            }
        )
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    context.startActivity(
        Intent.createChooser(
            sendIntent,
            if (isEnglish) "Share PDF" else "שיתוף PDF"
        )
    )
}

private fun createMaterialsPdf(
    context: Context,
    belt: Belt,
    topicTitle: String,
    items: List<MaterialPdfItem>,
    isEnglish: Boolean,
    isCoach: Boolean
): File {
    val pageWidth = 595
    val pageHeight = 842
    val margin = 24f

    fun tr(he: String, en: String): String = if (isEnglish) en else he

    val beltTitle = when (belt) {
        Belt.WHITE -> tr("חגורה לבנה", "White belt")
        Belt.YELLOW -> tr("חגורה צהובה", "Yellow belt")
        Belt.ORANGE -> tr("חגורה כתומה", "Orange belt")
        Belt.GREEN -> tr("חגורה ירוקה", "Green belt")
        Belt.BLUE -> tr("חגורה כחולה", "Blue belt")
        Belt.BROWN -> tr("חגורה חומה", "Brown belt")
        Belt.BLACK -> tr("חגורה שחורה", "Black belt")
    }

    val document = PdfDocument()

    val navy = android.graphics.Color.rgb(2, 43, 74)
    val blue = android.graphics.Color.rgb(12, 78, 130)
    val lightBlue = android.graphics.Color.rgb(234, 246, 255)
    val softBlue = android.graphics.Color.rgb(244, 250, 255)
    val borderBlue = android.graphics.Color.rgb(191, 213, 232)
    val textDark = android.graphics.Color.rgb(15, 23, 42)
    val textMuted = android.graphics.Color.rgb(80, 100, 120)
    val red =
        android.graphics.Color.rgb(
            220,
            38,
            38
        )

    val orange =
        android.graphics.Color.rgb(
            242,
            140,
            40
        )

    val green =
        android.graphics.Color.rgb(
            22,
            163,
            74
        )

    val regular = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    val bold = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)

    fun paint(
        size: Float,
        color: Int = textDark,
        typeface: Typeface = regular,
        align: Paint.Align = Paint.Align.RIGHT
    ) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        this.typeface = typeface
        textAlign = align
    }

    val titlePaint = paint(29f, android.graphics.Color.WHITE, bold)
    val subTitlePaint = paint(14f, android.graphics.Color.WHITE, regular)
    val sectionPaint = paint(17f, blue, bold)
    val labelPaint = paint(10.5f, blue, bold)
    val valuePaint = paint(12.5f, textDark, regular)
    val boldValuePaint = paint(13f, textDark, bold)
    val smallPaint = paint(9f, textMuted, regular)

    fun drawRoundRect(
        canvas: Canvas,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        color: Int,
        radius: Float = 12f,
        stroke: Boolean = false,
        strokeWidth: Float = 1.2f
    ) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = if (stroke) Paint.Style.STROKE else Paint.Style.FILL
            this.strokeWidth = strokeWidth
        }
        canvas.drawRoundRect(left, top, right, bottom, radius, radius, p)
    }

    fun drawKmiLogo(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        val outer = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = navy }
        val inner = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE }
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = navy
            typeface = bold
            textSize = radius * 0.62f
            textAlign = Paint.Align.CENTER
        }

        canvas.drawCircle(cx, cy, radius, outer)
        canvas.drawCircle(cx, cy, radius - 4f, inner)
        canvas.drawText("KAMI", cx, cy + radius * 0.22f, text)
    }

    fun drawHeader(canvas: Canvas) {
        canvas.drawColor(android.graphics.Color.WHITE)

        canvas.drawPath(Path().apply {
            moveTo(pageWidth.toFloat(), 0f)
            lineTo(pageWidth.toFloat(), 122f)
            lineTo(178f, 122f)
            lineTo(238f, 0f)
            close()
        }, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = navy })

        canvas.drawPath(Path().apply {
            moveTo(208f, 122f)
            lineTo(224f, 122f)
            lineTo(284f, 0f)
            lineTo(268f, 0f)
            close()
        }, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(36, 103, 158)
        })

        canvas.drawPath(Path().apply {
            moveTo(230f, 122f)
            lineTo(238f, 122f)
            lineTo(298f, 0f)
            lineTo(290f, 0f)
            close()
        }, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(128, 183, 220)
        })

        drawKmiLogo(canvas, 78f, 58f, 42f)

        titlePaint.textAlign = Paint.Align.RIGHT
        subTitlePaint.textAlign = Paint.Align.RIGHT

        titlePaint.textAlign =
            if (isEnglish) Paint.Align.LEFT else Paint.Align.RIGHT

        subTitlePaint.textAlign =
            if (isEnglish) Paint.Align.LEFT else Paint.Align.RIGHT

        val headerX =
            if (isEnglish) margin else pageWidth - margin

        canvas.drawText(
            tr("תרגילים לפי חגורה", "Belt exercises"),
            headerX,
            52f,
            titlePaint
        )

        val pdfSubTitle = "$topicTitle — $beltTitle"

        canvas.drawText(
            pdfSubTitle.take(55),
            headerX,
            78f,
            subTitlePaint
        )

        smallPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(
            tr("תאריך הפקה:", "Generated:") + " " +
                    SimpleDateFormat(
                        "dd/MM/yyyy",
                        Locale.getDefault()
                    ).format(Date()),
            pageWidth - 34f,
            142f,
            smallPaint
        )
    }

    fun drawFooter(canvas: Canvas, pageNumber: Int, totalPages: Int) {
        val footerY = 804f

        canvas.drawLine(
            0f,
            footerY,
            pageWidth.toFloat(),
            footerY,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = navy
                strokeWidth = 2f
            })

        drawKmiLogo(canvas, 38f, footerY + 22f, 13f)

        smallPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("Together We Protect", 62f, footerY + 25f, smallPaint)

        smallPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(
            tr("עמוד $pageNumber מתוך $totalPages", "Page $pageNumber of $totalPages"),
            pageWidth / 2f,
            footerY + 25f,
            smallPaint
        )

        smallPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Krav Maga Israel", pageWidth - 66f, footerY + 18f, smallPaint)
        canvas.drawText("www.kmi.org.il", pageWidth - 66f, footerY + 31f, smallPaint)
    }

    fun drawSummary(
        canvas: Canvas,
        top: Float
    ): Float {
        val total =
            items.size

        val known =
            if (!isCoach) {
                items.count {
                    it.status ==
                            tr(
                                "יודע",
                                "Known"
                            )
                }
            } else {
                0
            }

        val partiallyKnown =
            if (!isCoach) {
                items.count {
                    it.status ==
                            tr(
                                "יודע חלקית",
                                "Partially known"
                            )
                }
            } else {
                0
            }

        val unknown =
            if (!isCoach) {
                items.count {
                    it.status ==
                            tr(
                                "לא יודע",
                                "Unknown"
                            )
                }
            } else {
                0
            }

        val unmarked =
            if (!isCoach) {
                items.count {
                    it.status ==
                            tr(
                                "לא סומן",
                                "Not marked"
                            )
                }
            } else {
                0
            }

        val coachNotTaught =
            if (isCoach) {
                items.count {
                    it.status ==
                            tr(
                                "לא נלמד",
                                "Not taught"
                            )
                }
            } else {
                0
            }

        val coachTaught =
            if (isCoach) {
                items.count {
                    it.status
                        .split("·")
                        .map { part -> part.trim() }
                        .contains(
                            tr(
                                "נלמד",
                                "Taught"
                            )
                        )
                }
            } else {
                0
            }

        val coachPracticed =
            if (isCoach) {
                items.count {
                    it.status
                        .split("·")
                        .map { part -> part.trim() }
                        .contains(
                            tr(
                                "תורגל",
                                "Practiced"
                            )
                        )
                }
            } else {
                0
            }

        val coachNeedsReinforcement =
            if (isCoach) {
                items.count {
                    it.status
                        .split("·")
                        .map { part -> part.trim() }
                        .contains(
                            tr(
                                "טעון שיפור",
                                "Needs reinforcement"
                            )
                        )
                }
            } else {
                0
            }

        val excluded =
            items.count {
                it.isExcluded
            }

        val favorites =
            items.count {
                it.isFavorite
            }

        val notes =
            items.count {
                it.hasNote
            }

        drawRoundRect(canvas, margin, top, pageWidth - margin, top + 120f, lightBlue, 12f)
        drawRoundRect(
            canvas,
            margin,
            top,
            pageWidth - margin,
            top + 120f,
            borderBlue,
            12f,
            stroke = true
        )

        sectionPaint.textAlign =
            if (isEnglish) Paint.Align.LEFT else Paint.Align.RIGHT

        canvas.drawText(
            tr("סיכום תרגילים", "Exercises summary"),
            if (isEnglish) margin + 22f else pageWidth - margin - 22f,
            top + 30f,
            sectionPaint
        )

        val stats =
            if (isCoach) {
                listOf(
                    total.toString() to
                            tr(
                                "תרגילים",
                                "Exercises"
                            ),

                    coachNotTaught.toString() to
                            tr(
                                "לא נלמד",
                                "Not taught"
                            ),

                    coachTaught.toString() to
                            tr(
                                "נלמד",
                                "Taught"
                            ),

                    coachPracticed.toString() to
                            tr(
                                "תורגל",
                                "Practiced"
                            ),

                    coachNeedsReinforcement.toString() to
                            tr(
                                "טעון שיפור",
                                "Needs reinforcement"
                            ),

                    excluded.toString() to
                            tr(
                                "מוחרגים",
                                "Excluded"
                            ),

                    favorites.toString() to
                            tr(
                                "מועדפים",
                                "Favorites"
                            ),

                    notes.toString() to
                            tr(
                                "הערות",
                                "Notes"
                            )
                )
            } else {
                listOf(
                    total.toString() to
                            tr(
                                "תרגילים",
                                "Exercises"
                            ),

                    known.toString() to
                            tr(
                                "יודע",
                                "Known"
                            ),

                    partiallyKnown.toString() to
                            tr(
                                "חלקית",
                                "Partial"
                            ),

                    unknown.toString() to
                            tr(
                                "לא יודע",
                                "Unknown"
                            ),

                    unmarked.toString() to
                            tr(
                                "לא סומן",
                                "Unmarked"
                            ),

                    excluded.toString() to
                            tr(
                                "מוחרגים",
                                "Excluded"
                            ),

                    favorites.toString() to
                            tr(
                                "מועדפים",
                                "Favorites"
                            ),

                    notes.toString() to
                            tr(
                                "הערות",
                                "Notes"
                            )
                )
            }

        val boxTop =
            top + 48f

        val boxW =
            (
                    pageWidth -
                            margin * 2f -
                            30f
                    ) / 4f

        stats.forEachIndexed {
                index,
                pair ->

            val row =
                index / 4

            val col =
                index % 4
            val left = margin + 15f + col * boxW
            val cardTop = boxTop + row * 34f
            val right = left + boxW - 8f

            drawRoundRect(canvas, left, cardTop, right, cardTop + 28f, softBlue, 10f)
            drawRoundRect(
                canvas,
                left,
                cardTop,
                right,
                cardTop + 28f,
                borderBlue,
                10f,
                stroke = true
            )

            boldValuePaint.textAlign =
                Paint.Align.CENTER

            boldValuePaint.color =
                when (index) {
                    1 ->
                        green

                    2 ->
                        orange

                    3 ->
                        red

                    else ->
                        navy
                }

            canvas.drawText(
                pair.first,
                (left + right) / 2f,
                cardTop + 12f,
                boldValuePaint
            )

            smallPaint.textAlign = Paint.Align.CENTER
            smallPaint.color = textMuted
            canvas.drawText(pair.second.take(14), (left + right) / 2f, cardTop + 24f, smallPaint)
        }

        return top + 144f
    }

    fun drawItemCard(
        canvas: Canvas,
        item: MaterialPdfItem,
        top: Float,
        index: Int
    ): Float {
        val left = margin
        val right = pageWidth - margin
        val bottom = top + 74f

        drawRoundRect(
            canvas,
            left,
            top,
            right,
            bottom,
            if (index % 2 == 0) lightBlue else softBlue,
            12f
        )
        drawRoundRect(canvas, left, top, right, bottom, borderBlue, 12f, stroke = true)

        val statusColor =
            if (isCoach) {

                when {
                    item.status.contains(
                        tr(
                            "טעון שיפור",
                            "Needs reinforcement"
                        )
                    ) ->
                        red

                    item.status.contains(
                        tr(
                            "תורגל",
                            "Practiced"
                        )
                    ) ->
                        blue

                    item.status.contains(
                        tr(
                            "נלמד",
                            "Taught"
                        )
                    ) ->
                        green

                    else ->
                        textMuted
                }

            } else {

                when (item.status) {
                    tr(
                        "יודע",
                        "Known"
                    ) ->
                        green

                    tr(
                        "יודע חלקית",
                        "Partially known"
                    ) ->
                        orange

                    tr(
                        "לא יודע",
                        "Unknown"
                    ) ->
                        red

                    else ->
                        textMuted
                }
            }

        val numberRect = RectF(right - 54f, top + 18f, right - 22f, top + 50f)
        canvas.drawRoundRect(numberRect, 999f, 999f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = blue
            style = Paint.Style.FILL
        })

        val numberPaint = paint(12f, android.graphics.Color.WHITE, bold, Paint.Align.CENTER)
        canvas.drawText(
            item.number.toString(),
            numberRect.centerX(),
            numberRect.centerY() - (numberPaint.descent() + numberPaint.ascent()) / 2f,
            numberPaint
        )

        boldValuePaint.textAlign =
            if (isEnglish) Paint.Align.LEFT else Paint.Align.RIGHT

        boldValuePaint.color = textDark

        canvas.drawText(
            item.title.take(42),
            if (isEnglish) left + 66f else right - 66f,
            top + 28f,
            boldValuePaint
        )

        labelPaint.textAlign =
            if (isEnglish) Paint.Align.LEFT else Paint.Align.RIGHT

        labelPaint.color = statusColor

        canvas.drawText(
            item.status,
            if (isEnglish) left + 66f else right - 66f,
            top + 52f,
            labelPaint
        )

        val tags = buildList {
            if (item.isFavorite) add(tr("מועדף", "Favorite"))
            if (item.isExcluded) add(tr("מוחרג", "Excluded"))
            if (item.hasNote) add(tr("הערה", "Note"))
        }.joinToString(" · ")

        valuePaint.textAlign = Paint.Align.LEFT
        valuePaint.color = if (item.isExcluded) red else textMuted
        canvas.drawText(tags.ifBlank { "—" }.take(24), left + 22f, top + 42f, valuePaint)

        return bottom + 8f
    }

    val firstPageCapacity = 6
    val nextPageCapacity = 8

    val totalPages = if (items.size <= firstPageCapacity) {
        1
    } else {
        1 + ceil((items.size - firstPageCapacity) / nextPageCapacity.toDouble()).toInt()
    }

    var pageNumber = 1
    var itemIndex = 0

    while (pageNumber <= totalPages) {
        val page = document.startPage(
            PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        )
        val canvas = page.canvas

        drawHeader(canvas)

        var y = 136f

        if (pageNumber == 1) {
            y = drawSummary(canvas, y)
        } else {
            sectionPaint.textAlign = Paint.Align.CENTER
            sectionPaint.color = blue
            canvas.drawText(tr("רשימת תרגילים", "Exercises list"), pageWidth / 2f, y, sectionPaint)
            y += 28f
        }

        val capacity = if (pageNumber == 1) firstPageCapacity else nextPageCapacity

        if (items.isEmpty()) {
            drawRoundRect(canvas, margin, y, pageWidth - margin, y + 92f, softBlue, 12f)
            drawRoundRect(
                canvas,
                margin,
                y,
                pageWidth - margin,
                y + 92f,
                borderBlue,
                12f,
                stroke = true
            )

            sectionPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(
                tr("אין תרגילים להצגה", "No exercises to display"),
                pageWidth / 2f,
                y + 42f,
                sectionPaint
            )
        } else {
            repeat(capacity) {
                if (itemIndex >= items.size) return@repeat

                y = drawItemCard(
                    canvas = canvas,
                    item = items[itemIndex],
                    top = y,
                    index = itemIndex
                )

                itemIndex++
            }
        }

        drawFooter(canvas, pageNumber, totalPages)
        document.finishPage(page)

        pageNumber++
    }

    val dir =
        File(
            context.cacheDir,
            "pdfs"
        ).apply {
            mkdirs()
        }

    /*
     * שם הקובץ מבוסס על הנושא / תת־הנושא
     * שמוצג כרגע במסך.
     *
     * מסירים תווים שאינם חוקיים בשם קובץ,
     * אך משאירים עברית ואנגלית ללא שינוי.
     */
    val safeTopicTitle =
        topicTitle
            .trim()
            .replace(
                Regex("""[\\/:*?"<>|]"""),
                "-"
            )
            .ifBlank {
                if (isEnglish) {
                    "Topic"
                } else {
                    "נושא"
                }
            }

    val fileName =
        if (isEnglish) {
            "Exercises - $safeTopicTitle.pdf"
        } else {
            "תרגילים - $safeTopicTitle.pdf"
        }

    val file =
        File(
            dir,
            fileName
        )

    /*
     * כל יצירה חדשה של אותו דוח
     * מחליפה את הקובץ הקודם.
     */
    if (file.exists()) {
        file.delete()
    }

    FileOutputStream(file).use { output ->
        document.writeTo(output)
    }

    document.close()

    return file
}

// ===== כפתור מונפש =====
@Composable
fun AnimatedButton(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        label = "buttonScaleAnim"
    )
    val scope = rememberCoroutineScope()

    val contentOnContainer =
        if (containerColor.luminance() < 0.5f) Color.White else Color.Black

    Button(
        onClick = {
            pressed = true
            onClick()
            scope.launch {
                delay(140)
                pressed = false
            }
        },
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .scale(scale)
            .height(42.dp)
            .defaultMinSize(minWidth = 72.dp),
        border = BorderStroke(
            1.dp,
            Color.White.copy(alpha = 0.22f)
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentOnContainer
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 4.dp,
            pressedElevation = 1.5.dp
        )
    ) {
        Text(
            text = text,
            style = KmiTypography.action,
            color = contentOnContainer,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MaterialsTopStatChip(
    value: String,
    label: String,
    containerColor: Color,
    contentColor: Color = Color.White,
    minWidth: Dp = 64.dp,
    horizontalPadding: Dp = 12.dp
) {
    Surface(
        modifier = Modifier.widthIn(min = minWidth),
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        shadowElevation = 1.dp,
        border = BorderStroke(
            1.dp,
            contentColor.copy(alpha = 0.14f)
        )
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = horizontalPadding,
                vertical = 6.dp
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = KmiTypography.cardTitle,
                color = contentColor,
                maxLines = 1
            )

            Text(
                text = label,
                style = KmiTypography.caption.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = contentColor.copy(alpha = 0.92f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TraineeMaterialStatusSelector(
    selectedStatus: TraineeMaterialStatus?,
    dateText: String,
    isEnglish: Boolean,
    onSelect: (
        TraineeMaterialStatus?
    ) -> Unit
) {
    val statuses =
        listOf(
            TraineeMaterialStatus.KNOWN,
            TraineeMaterialStatus.PARTIALLY_KNOWN,
            TraineeMaterialStatus.UNKNOWN
        )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Top
    ) {
        statuses.forEach { status ->
            val isSelected =
                selectedStatus == status

            val activeColor =
                when (status) {
                    TraineeMaterialStatus.KNOWN ->
                        Color(0xFF2F9B4E)

                    TraineeMaterialStatus.PARTIALLY_KNOWN ->
                        Color(0xFFF28C28)

                    TraineeMaterialStatus.UNKNOWN ->
                        Color(0xFFC62828)
                }

            val symbol =
                when (status) {
                    TraineeMaterialStatus.KNOWN ->
                        "✓"

                    TraineeMaterialStatus.PARTIALLY_KNOWN ->
                        "◐"

                    TraineeMaterialStatus.UNKNOWN ->
                        "×"
                }

            val label =
                when (status) {
                    TraineeMaterialStatus.KNOWN ->
                        if (isEnglish) {
                            "Known"
                        } else {
                            "יודע"
                        }

                    TraineeMaterialStatus.PARTIALLY_KNOWN ->
                        if (isEnglish) {
                            "Partly"
                        } else {
                            "חלקית"
                        }

                    TraineeMaterialStatus.UNKNOWN ->
                        if (isEnglish) {
                            "Unknown"
                        } else {
                            "לא יודע"
                        }
                }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        /*
                         * לחיצה נוספת על המצב המסומן
                         * מחזירה את התרגיל ללא מסומן.
                         */
                        onSelect(
                            if (isSelected) {
                                null
                            } else {
                                status
                            }
                        )
                    },
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color =
                        if (isSelected) {
                            activeColor
                        } else {
                            Color(0xFF8A939D)
                                .copy(alpha = 0.34f)
                        },
                    shadowElevation =
                        if (isSelected) {
                            3.dp
                        } else {
                            0.dp
                        },
                    tonalElevation = 0.dp,
                    border = BorderStroke(
                        width = 1.dp,
                        color =
                            if (isSelected) {
                                Color.White.copy(
                                    alpha = 0.38f
                                )
                            } else {
                                MaterialTheme
                                    .colorScheme
                                    .outline
                                    .copy(alpha = 0.20f)
                            }
                    ),
                    modifier = Modifier.size(
                        scaledIconSize(32.dp)
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = symbol,
                            style =
                                KmiTypography.metric.copy(
                                    fontWeight =
                                        FontWeight.ExtraBold
                                ),
                            color =
                                if (isSelected) {
                                    Color.White
                                } else {
                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant
                                        .copy(alpha = 0.62f)
                                },
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(
                    Modifier.height(2.dp)
                )

                Text(
                    text = label,
                    style =
                        KmiTypography.caption.copy(
                            fontWeight =
                                if (isSelected) {
                                    FontWeight.ExtraBold
                                } else {
                                    FontWeight.SemiBold
                                }
                        ),
                    color =
                        if (isSelected) {
                            activeColor
                        } else {
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                                .copy(alpha = 0.65f)
                        },
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text =
                        if (isSelected) {
                            dateText
                        } else {
                            ""
                        },
                    style =
                        KmiTypography.caption.copy(
                            fontWeight =
                                FontWeight.Medium
                        ),
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
internal fun CoachMaterialStatusSelector(
    progress: CoachMaterialProgress,
    isEnglish: Boolean,
    excluded: Boolean = false,
    isFav: Boolean = false,
    hasNote: Boolean = false,
    onToggleExclude: () -> Unit = {},
    onInfo: () -> Unit,
    onToggleFavorite: () -> Unit = {},
    onEditNote: () -> Unit = {},
    onSelect: (CoachMaterialStatus) -> Unit
) {
    val context = LocalContext.current

    val statuses = listOf(
        CoachMaterialStatus.TAUGHT,
        CoachMaterialStatus.PRACTICED,
        CoachMaterialStatus.NEEDS_REINFORCEMENT
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(
                horizontal = 8.dp,
                vertical = 2.dp
            ),
        horizontalArrangement =
            Arrangement.spacedBy(7.dp),
        verticalAlignment =
            Alignment.Top
    ) {

        /*
         * פעולות המידע נמצאות בכרטיס נפרד,
         * כדי להבדיל אותן ממצבי החומר של המאמן
         * ללא צורך בקו אנכי.
         */
        Surface(
            modifier = Modifier
                .weight(0.82f)
                .fillMaxHeight(),
            shape = RoundedCornerShape(16.dp),
            color =
                Color(0xFF1976D2)
                    .copy(alpha = 0.055f),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            border = BorderStroke(
                width = 1.dp,
                color =
                    Color(0xFF1976D2)
                        .copy(alpha = 0.28f)
            )
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = 4.dp,
                    vertical = 6.dp
                ),
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {
                ItemFloatingActions(
                    isEnglish = isEnglish,
                    excluded = excluded,
                    isFav = isFav,
                    hasNote = hasNote,
                    onToggleExclude = onToggleExclude,
                    onInfo = onInfo,
                    onToggleFavorite = onToggleFavorite,
                    onEditNote = onEditNote
                )

                Spacer(
                    Modifier.height(2.dp)
                )

                Text(
                    text =
                        if (isEnglish) {
                            "Info"
                        } else {
                            "מידע"
                        },
                    style =
                        KmiTypography.caption.copy(
                            fontWeight =
                                FontWeight.SemiBold
                        ),
                    color = Color(0xFF1976D2),
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }

        /*
         * שלושת מצבי החומר מוצגים כקבוצה אחת
         * בתוך מסגרת משותפת ועדינה.
         */
        Surface(
            modifier = Modifier
                .weight(3f)
                .fillMaxHeight(),
            shape = RoundedCornerShape(18.dp),
            color =
                MaterialTheme
                    .colorScheme
                    .surfaceVariant
                    .copy(alpha = 0.32f),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            border = BorderStroke(
                width = 1.dp,
                color =
                    MaterialTheme
                        .colorScheme
                        .outline
                        .copy(alpha = 0.22f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 4.dp,
                        vertical = 6.dp
                    ),
                horizontalArrangement =
                    Arrangement.SpaceEvenly,
                verticalAlignment =
                    Alignment.Top
            ) {
                statuses.forEach { status ->

                    val isSelected =
                        progress.isSelected(status)

                    val activeColor =
                        when (status) {
                            CoachMaterialStatus.TAUGHT ->
                                Color(0xFF2F9B4E)

                            CoachMaterialStatus.PRACTICED ->
                                Color(0xFF6D4BD8)

                            CoachMaterialStatus.NEEDS_REINFORCEMENT ->
                                Color(0xFFF28C28)

                            CoachMaterialStatus.NOT_TAUGHT ->
                                Color(0xFF8A939D)
                        }

                    val symbol =
                        when (status) {
                            CoachMaterialStatus.TAUGHT ->
                                "✓"

                            CoachMaterialStatus.PRACTICED ->
                                "↻"

                            CoachMaterialStatus.NEEDS_REINFORCEMENT ->
                                "!"

                            CoachMaterialStatus.NOT_TAUGHT ->
                                "—"
                        }

                    val label =
                        when (status) {
                            CoachMaterialStatus.TAUGHT ->
                                if (isEnglish) {
                                    "Taught"
                                } else {
                                    "נלמד"
                                }

                            CoachMaterialStatus.PRACTICED ->
                                if (isEnglish) {
                                    "Practiced"
                                } else {
                                    "תורגל"
                                }

                            CoachMaterialStatus.NEEDS_REINFORCEMENT ->
                                if (isEnglish) {
                                    "Reinforce"
                                } else {
                                    "חיזוק"
                                }

                            CoachMaterialStatus.NOT_TAUGHT ->
                                if (isEnglish) {
                                    "Not taught"
                                } else {
                                    "לא נלמד"
                                }
                        }

                    val updatedAt =
                        progress.updatedAtFor(status)

                    val dateText =
                        if (
                            isSelected &&
                            updatedAt > 0L
                        ) {
                            SimpleDateFormat(
                                "dd/MM/yy",
                                Locale.getDefault()
                            ).format(
                                Date(updatedAt)
                            )
                        } else {
                            ""
                        }

                    val canSelect =
                        isSelected ||
                                progress.selectedStatuses.size < 2

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                if (canSelect) {
                                    onSelect(status)
                                } else {
                                    Toast
                                        .makeText(
                                            context,
                                            if (isEnglish) {
                                                "You can select up to 2 statuses."
                                            } else {
                                                "ניתן לבחור עד 2 סטטוסים."
                                            },
                                            Toast.LENGTH_SHORT
                                        )
                                        .show()
                                }
                            },
                        horizontalAlignment =
                            Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color =
                                if (isSelected) {
                                    activeColor
                                } else {
                                    Color(0xFF8A939D)
                                        .copy(alpha = 0.34f)
                                },
                            shadowElevation =
                                if (isSelected) {
                                    3.dp
                                } else {
                                    0.dp
                                },
                            border = BorderStroke(
                                width = 1.dp,
                                color =
                                    if (isSelected) {
                                        Color.White
                                            .copy(alpha = 0.38f)
                                    } else {
                                        MaterialTheme
                                            .colorScheme
                                            .outline
                                            .copy(alpha = 0.20f)
                                    }
                            ),
                            modifier = Modifier.size(
                                scaledIconSize(32.dp)
                            )
                        ) {
                            Box(
                                modifier =
                                    Modifier.fillMaxSize(),
                                contentAlignment =
                                    Alignment.Center
                            ) {
                                Text(
                                    text = symbol,
                                    style =
                                        KmiTypography.metric.copy(
                                            fontWeight =
                                                FontWeight.ExtraBold
                                        ),
                                    color =
                                        if (isSelected) {
                                            Color.White
                                        } else {
                                            MaterialTheme
                                                .colorScheme
                                                .onSurfaceVariant
                                                .copy(alpha = 0.62f)
                                        },
                                    textAlign =
                                        TextAlign.Center
                                )
                            }
                        }

                        Spacer(
                            Modifier.height(2.dp)
                        )

                        Text(
                            text = label,
                            style =
                                KmiTypography.caption.copy(
                                    fontWeight =
                                        if (isSelected) {
                                            FontWeight.ExtraBold
                                        } else {
                                            FontWeight.SemiBold
                                        }
                                ),
                            color =
                                if (isSelected) {
                                    activeColor
                                } else {
                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant
                                        .copy(alpha = 0.65f)
                                },
                            textAlign =
                                TextAlign.Center,
                            maxLines = 1,
                            overflow =
                                TextOverflow.Ellipsis
                        )

                        Text(
                            text = dateText,
                            style =
                                KmiTypography.caption.copy(
                                    fontWeight =
                                        FontWeight.Medium
                                ),
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant,
                            textAlign =
                                TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseMetaBadge(
    text: String,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = containerColor,
        border = BorderStroke(
            1.dp,
            contentColor.copy(alpha = 0.14f)
        ),
        shadowElevation = 0.dp
    ) {
        Text(
            text = text,
            style = KmiTypography.caption.copy(
                fontWeight = FontWeight.ExtraBold
            ),
            color = contentColor,
            modifier = Modifier.padding(
                horizontal = 7.dp,
                vertical = 2.dp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CompactDropdownAction(
    text: String,
    isEnglish: Boolean,
    enabled: Boolean = true,
    textColor: Color? = null,
    fontWeight: FontWeight = FontWeight.SemiBold,
    onClick: () -> Unit
) {
    val resolvedTextColor =
        textColor ?: MaterialTheme.colorScheme.onSurface

    CompositionLocalProvider(
        LocalLayoutDirection provides if (isEnglish) LayoutDirection.Ltr else LayoutDirection.Rtl
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 40.dp)
                .clickable(
                    enabled = enabled,
                    onClick = onClick
                )
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isEnglish) Arrangement.Start else Arrangement.End
        ) {
            Text(
                text = text,
                modifier = Modifier.fillMaxWidth(),
                style = KmiTypography.action.copy(
                    fontWeight = fontWeight
                ),
                color =
                    if (enabled) {
                        resolvedTextColor
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                            .copy(alpha = 0.65f)
                    },
                textAlign =
                    if (isEnglish) {
                        TextAlign.Left
                    } else {
                        TextAlign.Right
                    },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ItemFloatingActions(
    isEnglish: Boolean,
    excluded: Boolean,
    isFav: Boolean,
    hasNote: Boolean,
    onToggleExclude: () -> Unit,
    onInfo: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEditNote: () -> Unit
) {
    val context = LocalContext.current

    val isDarkMode =
        MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val dropdownContainerColor =
        if (isDarkMode) {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            Color(0xFFF7F5FB)
        }

    val sp = remember {
        context.getSharedPreferences(
            "kmi_settings",
            Context.MODE_PRIVATE
        )
    }
    var expanded by remember { mutableStateOf(false) }
    var helpSeen by remember { mutableStateOf(sp.getBoolean("exclude_help_seen", false)) }

    val infoScale by animateFloatAsState(
        targetValue = if (expanded) 1.08f else 1f,
        animationSpec = tween(180),
        label = "materialsInfoScale"
    )

    val infoRotation by animateFloatAsState(
        targetValue = if (expanded) 12f else 0f,
        animationSpec = tween(180),
        label = "materialsInfoRotation"
    )

    LaunchedEffect(expanded) {
        if (expanded && !helpSeen) {
            helpSeen = true
            sp.edit().putBoolean("exclude_help_seen", true).apply()
        }
    }

    Box {
        Surface(
            onClick = { expanded = true },
            shape = CircleShape,
            color = Color(0xFF1976D2),
            shadowElevation = 3.dp,
            border = BorderStroke(
                1.dp,
                Color.White.copy(alpha = 0.22f)
            ),
            modifier = Modifier
                .size(
                    scaledIconSize(32.dp)
                )
                .graphicsLayer {
                    scaleX = infoScale
                    scaleY = infoScale
                }
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "i",
                    style = KmiTypography.body.copy(
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = Color.White,
                    modifier = Modifier.graphicsLayer {
                        rotationZ = infoRotation
                    }
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(22.dp),
            containerColor = dropdownContainerColor,
            tonalElevation = 0.dp,
            shadowElevation = 8.dp,
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(
                    alpha = if (isDarkMode) 0.32f else 0.14f
                )
            ),
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors =
                            if (isDarkMode) {
                                listOf(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    MaterialTheme.colorScheme.surface,
                                    MaterialTheme.colorScheme.surfaceVariant
                                )
                            } else {
                                listOf(
                                    Color(0xFFF9F8FC),
                                    Color(0xFFF3F0FA),
                                    Color(0xFFF7F5FB)
                                )
                            }
                    ),
                    shape = RoundedCornerShape(22.dp)
                )
                .padding(vertical = 4.dp)
        ) {

            if (!helpSeen) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = if (isEnglish) Arrangement.Start else Arrangement.End
                ) {
                    Text(
                        text =
                            if (isEnglish) {
                                "What does “Exclude” mean?\nRemoves this exercise from practice."
                            } else {
                                "מה זה “החרג”?\nמנטרל את התרגיל מהתרגול."
                            },
                        style = KmiTypography.secondary.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign =
                            if (isEnglish) {
                                TextAlign.Left
                            } else {
                                TextAlign.Right
                            }
                    )
                }

                Divider(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                    thickness = 0.8.dp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                )
            }

            CompactDropdownAction(
                text = if (isEnglish) "Info" else "מידע",
                isEnglish = isEnglish,
                onClick = {
                    expanded = false
                    onInfo()
                }
            )

            CompactDropdownAction(
                text = when {
                    isEnglish && isFav -> "Remove from favorites"
                    isEnglish -> "Add to favorites"
                    isFav -> "הסר ממועדפים"
                    else -> "הוסף למועדפים"
                },
                isEnglish = isEnglish,
                onClick = {
                    expanded = false
                    onToggleFavorite()
                    Toast
                        .makeText(
                            context,
                            when {
                                isEnglish && isFav -> "Removed from favorites."
                                isEnglish -> "Added to favorites."
                                isFav -> "הוסר מהמועדפים."
                                else -> "נוסף למועדפים."
                            },
                            Toast.LENGTH_SHORT
                        )
                        .show()
                }
            )

            CompactDropdownAction(
                text = when {
                    isEnglish && hasNote -> "Edit / delete note"
                    isEnglish -> "Add exercise note"
                    hasNote -> "ערוך / מחק הערה"
                    else -> "הוסף הערה לתרגיל"
                },
                isEnglish = isEnglish,
                onClick = {
                    expanded = false
                    onEditNote()
                }
            )

            Divider(
                color = Color(0xFFDC2626).copy(alpha = 0.22f),
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )

            CompactDropdownAction(
                text = when {
                    isEnglish && excluded -> "Cancel exclusion"
                    isEnglish -> "Exclude from practice"
                    excluded -> "בטל החרגה"
                    else -> "החרג מתרגול"
                },
                isEnglish = isEnglish,
                textColor = Color(0xFFDC2626),
                fontWeight = FontWeight.ExtraBold,
                onClick = {
                    expanded = false
                    onToggleExclude()
                    Toast
                        .makeText(
                            context,
                            when {
                                isEnglish && excluded -> "Exclusion canceled."
                                isEnglish -> "Exercise excluded."
                                excluded -> "בוטלה ההחרגה."
                                else -> "התרגיל הוחרג."
                            },
                            Toast.LENGTH_SHORT
                        )
                        .show()
                }
            )
        }
    }
}
