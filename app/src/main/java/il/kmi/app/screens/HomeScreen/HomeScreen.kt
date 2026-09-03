package il.kmi.app.screens

import android.content.Context
import android.content.SharedPreferences
import android.content.Intent
import android.net.Uri
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.graphics.ColorUtils
import java.io.File
import java.io.FileOutputStream
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import il.kmi.app.R
import il.kmi.app.training.TrainingData
import il.kmi.app.training.TrainingStatusEngine
import il.kmi.app.training.TrainingOverride
import il.kmi.app.training.TrainingOverrideRepository
import java.lang.reflect.AccessibleObject
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.style.TextOverflow
import il.kmi.shared.domain.Belt
import androidx.compose.material3.Switch
import il.kmi.app.ui.rememberHapticsGlobal
import il.kmi.app.ui.rememberClickSound
import il.kmi.app.ui.assistant.ui.AiAssistantDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import il.kmi.shared.questions.model.util.ExerciseTitleFormatter
import il.kmi.app.ui.dialogs.ExerciseExplanationDialog
import il.kmi.app.ui.dialogs.ExerciseNoteEditorDialog
import il.kmi.app.domain.color
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.HorizontalDivider
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import il.kmi.app.favorites.FavoritesStore
import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import com.google.firebase.firestore.DocumentSnapshot
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager
import il.kmi.app.database.KmiDatabaseProvider
import il.kmi.app.domain.ExerciseExplanationResolver
import il.kmi.app.privacy.DemoPrivacy
import il.kmi.app.training.TrainingCatalog
import il.kmi.app.screens.registration.CoachBranchAssignmentsCodec
import il.kmi.app.ui.KmiIconSize
import il.kmi.app.ui.KmiTopBar
import il.kmi.app.ui.KmiTypography
import il.kmi.app.ui.pdf.KmiPdfDirection
import il.kmi.app.ui.scaledIconSize
import il.kmi.app.ui.pdf.KmiPdfFooter
import il.kmi.app.ui.pdf.KmiPdfHeader
import il.kmi.shared.domain.content.ExerciseTitlesEn
import il.yuval.ui.theme.kmiGraniteActionBrush
import il.yuval.ui.theme.kmiGraniteActionHighlightColor
import il.yuval.ui.theme.kmiScreenBackgroundBrush
import il.yuval.ui.theme.kmiSectionHeaderBrush
import il.yuval.ui.theme.kmiSectionHeaderContentColor
import kotlinx.coroutines.delay
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalTime
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import il.yuval.ui.theme.kmiOnSuccessContainerColor
import il.yuval.ui.theme.kmiOnWarningContainerColor
import il.yuval.ui.theme.kmiSuccessColor
import il.yuval.ui.theme.kmiSuccessContainerColor
import il.yuval.ui.theme.kmiWarningColor
import il.yuval.ui.theme.kmiWarningContainerColor
import kotlin.time.Duration.Companion.seconds

//=================================================================================

private enum class HomeNoticeType {
    COACH_MESSAGE,
    TRAINING_TIME_CHANGED,
    TRAINING_CANCELLED,
}

private data class HomeNotice(
    val id: String,
    val type: HomeNoticeType,
    val title: String,
    val text: String,
    val coachName: String,
    val sentAt: Date?,
    val branch: String,
    val group: String
)

private data class CoachHomeMessage(
    val text: String,
    val coachName: String,
    val sentAt: Date?,
    val branch: String,
    val group: String
)

/**
 * מחזיר את שם המאמן שמותר להציג.
 *
 * הנתון האמיתי נשמר ללא שינוי ב־Firestore,
 * ב־SharedPreferences ובמודלים הפנימיים.
 */
private fun homeCoachDisplayName(
    realName: String?,
    isEnglish: Boolean
): String {
    if (DemoPrivacy.isEnabled()) {
        return if (isEnglish) {
            "Coach"
        } else {
            "מאמן"
        }
    }

    return realName
        ?.trim()
        .orEmpty()
        .ifBlank {
            if (isEnglish) {
                "Coach"
            } else {
                "המאמן"
            }
        }
}

/**
 * קורא את החגורה הפעילה בלי לשנות נתונים שמורים.
 */
private fun resolveHomeActiveBelt(
    userSp: SharedPreferences,
    legacySp: SharedPreferences,
    settingsSp: SharedPreferences
): Belt {
    val rawBelt =
        listOf(
            userSp.getString(
                "current_belt",
                null
            ),
            userSp.getString(
                "belt_current",
                null
            ),
            userSp.getString(
                "belt",
                null
            ),
            legacySp.getString(
                "current_belt",
                null
            ),
            legacySp.getString(
                "belt_current",
                null
            ),
            legacySp.getString(
                "belt",
                null
            ),
            settingsSp.getString(
                "current_belt",
                null
            ),
            settingsSp.getString(
                "belt",
                null
            )
        )
            .firstOrNull {
                !it.isNullOrBlank()
            }
            ?.trim()
            .orEmpty()

    return Belt.fromId(rawBelt)
        ?: Belt.entries.firstOrNull { belt ->
            belt.id.equals(
                rawBelt,
                ignoreCase = true
            ) ||
                    belt.heb.equals(
                        rawBelt,
                        ignoreCase = true
                    ) ||
                    belt.en.equals(
                        rawBelt,
                        ignoreCase = true
                    )
        }
        ?: Belt.WHITE
}

/**
 * מתאים חגורה בהירה או שחורה למצב התצוגה,
 * תוך שמירה על גוון החגורה המקורי.
 */
@Composable
private fun readableHomeBeltAccent(
    beltColor: Color
): Color {
    val colorScheme =
        MaterialTheme.colorScheme

    val isDarkMode =
        colorScheme
            .background
            .luminance() < 0.5f

    return when {
        beltColor.luminance() > 0.72f -> {
            lerp(
                beltColor,
                if (isDarkMode) {
                    colorScheme.surface
                } else {
                    colorScheme.onSurface
                },
                0.34f
            )
        }

        beltColor.luminance() < 0.12f &&
                isDarkMode -> {
            lerp(
                beltColor,
                colorScheme.onSurface,
                0.38f
            )
        }

        else -> beltColor
    }
}

@Composable
private fun TrainingsWeekHeader(
    isEnglish: Boolean,
    modifier: Modifier = Modifier
) {
    val locale =
        if (isEnglish) {
            Locale.US
        } else {
            Locale("he", "IL")
        }

    val dateFormatter =
        SimpleDateFormat(
            "dd/MM",
            locale
        )

    val dayFormatter =
        SimpleDateFormat(
            "EEEE",
            locale
        )

    val start =
        Calendar.getInstance()

    val end =
        (start.clone() as Calendar).apply {
            add(
                Calendar.DAY_OF_YEAR,
                6
            )
        }

    val startLabel =
        "${dayFormatter.format(start.time)} ${
            dateFormatter.format(start.time)
        }"

    val endLabel =
        "${dayFormatter.format(end.time)} ${
            dateFormatter.format(end.time)
        }"

    Box(
        modifier = modifier
            .heightIn(min = 52.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.Center,
            modifier = Modifier.padding(
                top = 2.dp,
                bottom = 5.dp
            )
        ) {
            Text(
                text =
                    if (isEnglish) {
                        "Trainings for the upcoming week"
                    } else {
                        "אימונים לשבוע הקרוב"
                    },
                style =
                    KmiTypography.secondary.copy(
                        fontWeight = FontWeight.Bold
                    ),
                color =
                    kmiSectionHeaderContentColor(),
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Spacer(Modifier.height(1.dp))

            Text(
                text =
                    if (isEnglish) {
                        "Dates: $startLabel–$endLabel"
                    } else {
                        "(תאריכים: $startLabel–$endLabel)"
                    },
                style = KmiTypography.caption,
                color =
                    kmiSectionHeaderContentColor()
                        .copy(alpha = 0.92f),
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("UNUSED_PARAMETER")
@Composable
fun HomeScreen(
    onContinue: () -> Unit,
    onSettings: () -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenSubscription: () -> Unit,
    trainings: List<TrainingData>,
    onOpenExercise: (String) -> Unit,
    onOpenFreeSessions: (String, String, String, String) -> Unit,
    onOpenMonthlyCalendar: () -> Unit,
    onOpenTrainingSummary: () -> Unit,
    onOpenTrainingArchive: () -> Unit,
    onOpenTrainingManagement: () -> Unit
) {
    val haptic = rememberHapticsGlobal()
    val clickSound = rememberClickSound()

    // 🔵 מצב לדיאלוג העוזר האישי (AI)
    var showAiDialog by rememberSaveable {
        mutableStateOf(false)
    }

    val ctxRole = LocalContext.current

    /*
     * פעולות שנשלחו ממנגנון הפקודות הקוליות.
     * הארכיון הוא דיאלוג פנימי במסך הבית ולכן
     * אי אפשר לפתוח אותו באמצעות Route רגיל.
     */
    val voiceHomeActionsPrefs =
        remember(ctxRole) {
            ctxRole.getSharedPreferences(
                "kmi_voice_home_actions",
                Context.MODE_PRIVATE
            )
        }

    var openFreeTrainingsFromVoice by remember {
        mutableStateOf(
            voiceHomeActionsPrefs.getBoolean(
                "open_free_trainings",
                false
            )
        )
    }

    DisposableEffect(voiceHomeActionsPrefs) {
        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener {
                    preferences,
                    key ->

                when (key) {
                    "open_free_trainings" -> {
                        openFreeTrainingsFromVoice =
                            preferences.getBoolean(
                                key,
                                false
                            )
                    }
                }
            }

        voiceHomeActionsPrefs
            .registerOnSharedPreferenceChangeListener(
                listener
            )

        onDispose {
            voiceHomeActionsPrefs
                .unregisterOnSharedPreferenceChangeListener(
                    listener
                )
        }
    }

    val contextLang = LocalContext.current
    val langManager = remember { AppLanguageManager(contextLang) }
    val isEnglish = langManager.getCurrentLanguage() == AppLanguage.ENGLISH
    val userSpRole = remember { ctxRole.getSharedPreferences("kmi_user", Context.MODE_PRIVATE) }
    var userRole by remember { mutableStateOf(userSpRole.getString("user_role", "trainee")) }

    var homePdfTrainings by remember {
        mutableStateOf<List<HomePdfTraining>>(emptyList())
    }

    val notePrefs = remember(ctxRole) {
        ctxRole.getSharedPreferences("kmi_exercise_notes", Context.MODE_PRIVATE)
    }

    fun normalizeFavoriteId(raw: String): String =
        raw.substringAfter("::", raw)
            .substringAfter(":", raw)
            .trim()

    var pickedKey by rememberSaveable { mutableStateOf<String?>(null) }
    var notesRefreshKey by rememberSaveable { mutableIntStateOf(0) }

    fun saveHomeExerciseNote(noteKey: String, text: String) {
        val clean = text.trim()

        notePrefs.edit {
            if (clean.isBlank()) {
                remove(noteKey)
            } else {
                putString(noteKey, clean)
            }
        }

        notesRefreshKey++
    }

// ✅ Speed Dial FAB
    var fabExpanded by rememberSaveable { mutableStateOf(false) }

    // ✅ נשמור פרמטרים ל-FAB "אימונים חופשיים" מחוץ ל-Column (כדי שיהיו זמינים גם אחרי שה-Column נסגר)
    var freeBranchUi by rememberSaveable { mutableStateOf("") }
    var freeGroupKeyUi by rememberSaveable { mutableStateOf("") }
    var freeUidUi by rememberSaveable { mutableStateOf("") }
    var freeNameUi by rememberSaveable { mutableStateOf("") }

    DisposableEffect(userSpRole) {
        val l = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "user_role") userRole = userSpRole.getString("user_role", "trainee")
        }
        userSpRole.registerOnSharedPreferenceChangeListener(l)
        onDispose { userSpRole.unregisterOnSharedPreferenceChangeListener(l) }
    }

    val isCoach = remember(userRole) {
        when (userRole?.trim()?.lowercase()) {
            "coach", "trainer", "מאמן" -> true
            else -> false
        }
    }

    val backgroundBrush =
        kmiScreenBackgroundBrush()

    val screenLayoutDirection =
        if (isEnglish) {
            LayoutDirection.Ltr
        } else {
            LayoutDirection.Rtl
        }

    CompositionLocalProvider(
        LocalLayoutDirection provides
                screenLayoutDirection
    ) {
        Scaffold(
            topBar = {

            val contextLang = LocalContext.current
            val langManager = remember { AppLanguageManager(contextLang) }

            KmiTopBar(
                title = if (langManager.getCurrentLanguage() == AppLanguage.ENGLISH) "Home" else "מסך הבית",
                onHome = { /* no-op במסך הבית */ },
                lockHome = true,
                homeDisabledToast =
                    if (isEnglish) {
                        "You are already on the home screen 🙂"
                    } else {
                        "אתה כבר במסך הבית 🙂"
                    },
                showTopHome = false,
                showTopShare = true,

                currentLang =
                    if (langManager.getCurrentLanguage() == AppLanguage.ENGLISH) "en" else "he",

                onToggleLanguage = {

                    val newLang =
                        if (langManager.getCurrentLanguage() == AppLanguage.HEBREW)
                            AppLanguage.ENGLISH
                        else
                            AppLanguage.HEBREW

                    langManager.setLanguage(newLang)

                    (contextLang as Activity).recreate()
                },

                onShare = {
                    shareHomePdf(
                        context = ctxRole,
                        trainings = homePdfTrainings,
                        isEnglish = isEnglish
                    )
                },

                // חיפוש תרגיל מהסרגל התחתון
                onPickSearchResult = { key ->
                    clickSound()
                    haptic(true)
                    pickedKey = key
                },
            )
        },
        contentWindowInsets = WindowInsets(0)
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = padding.calculateTopPadding(),
                    start = padding.calculateStartPadding(LocalLayoutDirection.current),
                    end = padding.calculateEndPadding(LocalLayoutDirection.current),
                    bottom = padding.calculateBottomPadding()
                )
                .background(backgroundBrush)
        ) {
            val listState = rememberLazyListState()

            val ctx = LocalContext.current
            val userSp =
                remember {
                    ctx.getSharedPreferences(
                        "kmi_user",
                        Context.MODE_PRIVATE
                    )
                }

            val subsSp =
                remember {
                    ctx.getSharedPreferences(
                        "kmi_subs",
                        Context.MODE_PRIVATE
                    )
                }

            val legacySp =
                remember {
                    ctx.getSharedPreferences(
                        "kmi_prefs",
                        Context.MODE_PRIVATE
                    )
                }

            val settingsSp =
                remember {
                    ctx.getSharedPreferences(
                        "kmi_settings",
                        Context.MODE_PRIVATE
                    )
                }

            val activeHomeBelt =
                remember(
                    userSp,
                    legacySp,
                    settingsSp
                ) {
                    resolveHomeActiveBelt(
                        userSp = userSp,
                        legacySp = legacySp,
                        settingsSp = settingsSp
                    )
                }

            val homeBeltAccent =
                readableHomeBeltAccent(
                    beltColor =
                        activeHomeBelt.color
                )

            var homeAccessRefreshTick by
            remember {
                mutableIntStateOf(0)
            }

            // מצב הגישה מתרענן דרך SharedPreferences listener.
            // אין צורך בלולאת רענון קבועה במסך הבית.

            DisposableEffect(userSp, subsSp, legacySp) {
                val listener =
                    SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                        if (
                            key == "has_full_access" ||
                            key == "full_access" ||
                            key == "subscription_active" ||
                            key == "is_subscribed" ||
                            key == "google_subscription_verified" ||
                            key == "google_subscription_checked_at" ||
                            key == "sub_product" ||
                            key == "sub_access_until" ||
                            key == "access_changed_at"
                        ) {
                            homeAccessRefreshTick++
                        }
                    }

                userSp.registerOnSharedPreferenceChangeListener(listener)
                subsSp.registerOnSharedPreferenceChangeListener(listener)
                legacySp.registerOnSharedPreferenceChangeListener(listener)

                onDispose {
                    userSp.unregisterOnSharedPreferenceChangeListener(listener)
                    subsSp.unregisterOnSharedPreferenceChangeListener(listener)
                    legacySp.unregisterOnSharedPreferenceChangeListener(listener)
                }
            }

            fun SharedPreferences.hasActiveSubscriptionAccess(): Boolean {
                val now = System.currentTimeMillis()
                val until = getLong("sub_access_until", 0L)

                val hasSubscriptionFlags =
                    getBoolean("google_subscription_verified", false) ||
                            getBoolean("has_full_access", false) ||
                            getBoolean("full_access", false) ||
                            getBoolean("subscription_active", false) ||
                            getBoolean("is_subscribed", false) ||
                            getString("sub_product", "").orEmpty().isNotBlank()

                // מנוי רגיל / בדיקות פותח רק אם יש זמן תקף.
                val active =
                    hasSubscriptionFlags &&
                            until > now

// אם הזמן עבר — מנקים את כל הדגלים הישנים כדי שהמנעולים יחזרו.
                if (
                    !active &&
                    hasSubscriptionFlags &&
                    until > 0L
                ) {
                    edit {
                        putBoolean("google_subscription_verified", false)
                        putBoolean("has_full_access", false)
                        putBoolean("full_access", false)
                        putBoolean("subscription_active", false)
                        putBoolean("is_subscribed", false)
                        remove("sub_product")
                        remove("sub_token")
                        remove("sub_purchase_time")
                        remove("sub_access_until")
                        putLong("access_changed_at", System.currentTimeMillis())
                    }
                }

                return active
            }

            val hasFullAccess = remember(homeAccessRefreshTick) {
                userSp.hasActiveSubscriptionAccess() ||
                        subsSp.hasActiveSubscriptionAccess() ||
                        legacySp.hasActiveSubscriptionAccess()
            }

            // התפריט המהיר מוצג תמיד, גם כאשר אין מספיק תוכן לגלילה.
            val showFab = true

            // =========================
            // ⭐ הודעות מהמאמן – state ברמת Box כדי שגם הכרטיס וגם הדיאלוג יכירו אותו
            // =========================
            /*
             * Firebase עשוי לשחזר את המשתמש רק לאחר שהמסך כבר הורכב.
             * לכן לא שומרים את ה-UID באמצעות remember קבוע, אלא מאזינים
             * לשינויי ההתחברות ומעדכנים את ה-State.
             */
            var currentUid by remember {
                mutableStateOf(
                    FirebaseAuth.getInstance()
                        .currentUser
                        ?.uid
                )
            }

            DisposableEffect(Unit) {
                val auth =
                    FirebaseAuth.getInstance()

                val authListener =
                    FirebaseAuth.AuthStateListener { changedAuth ->
                        currentUid =
                            changedAuth.currentUser
                                ?.uid
                    }

                auth.addAuthStateListener(authListener)

                /*
                 * עדכון מיידי, בנוסף ל-listener, למקרה שהמשתמש
                 * כבר היה מחובר לפני פתיחת המסך.
                 */
                currentUid =
                    auth.currentUser
                        ?.uid

                onDispose {
                    auth.removeAuthStateListener(authListener)
                }
            }

            var recentCoachMessages by remember {
                mutableStateOf<List<CoachHomeMessage>>(emptyList())
            }

            var recentTrainingNotices by remember {
                mutableStateOf<List<HomeNotice>>(emptyList())
            }

            val homeNotices =
                remember(
                    recentCoachMessages,
                    recentTrainingNotices,
                    isEnglish
                ) {
                    val coachNotices =
                        recentCoachMessages.mapIndexed {
                                index,
                                message ->

                            HomeNotice(
                                id =
                                    "message|${message.sentAt?.time ?: 0L}|$index",
                                type =
                                    HomeNoticeType.COACH_MESSAGE,
                                title =
                                    if (isEnglish) {
                                        "Message from coach"
                                    } else {
                                        "הודעה מהמאמן"
                                    },
                                text = message.text,
                                coachName = message.coachName,
                                sentAt = message.sentAt,
                                branch = message.branch,
                                group = message.group
                            )
                        }

                    (
                            coachNotices +
                                    recentTrainingNotices
                            )
                        .distinctBy { notice ->
                            notice.id
                        }
                        .sortedByDescending { notice ->
                            notice.sentAt?.time ?: 0L
                        }
                        .take(5)
                }

            var showCoachMessagesDialog by rememberSaveable {
                mutableStateOf(false)
            }

            var openCoachMessagesFromPush by remember {
                mutableStateOf(settingsSp.getBoolean("coach_broadcast_open_dialog", false))
            }

            var pushBroadcastId by remember {
                mutableStateOf(settingsSp.getString("coach_broadcast_push_id", "").orEmpty())
            }

            LaunchedEffect(pushBroadcastId) {
                val cleanPushId = pushBroadcastId.trim()
                if (cleanPushId.isBlank()) return@LaunchedEffect

                val db = FirebaseFirestore.getInstance()

                fun messageFromDoc(
                    doc: DocumentSnapshot
                ): CoachHomeMessage? {
                    val text = (
                            doc.getString("text")
                                ?: doc.getString("message")
                                ?: doc.getString("body")
                                ?: doc.getString("content")
                            )
                        ?.trim()
                        .orEmpty()

                    if (text.isBlank()) return null

                    fun firstString(vararg keys: String): String {
                        keys.forEach { key ->
                            val clean = doc.getString(key)?.trim().orEmpty()
                            if (clean.isNotBlank()) return clean
                        }
                        return ""
                    }

                    return CoachHomeMessage(
                        text = text,
                        coachName = firstString(
                            "coachName",
                            "coach_name",
                            "senderName",
                            "fromName"
                        ).ifBlank { "המאמן" },
                        sentAt = doc.getTimestamp("createdAt")?.toDate()
                            ?: doc.getTimestamp("sentAt")?.toDate()
                            ?: doc.getTimestamp("timestamp")?.toDate(),
                        branch = firstString(
                            "branch",
                            "branchName",
                            "branch_name",
                            "targetBranch",
                            "selectedBranch"
                        ),
                        group = firstString(
                            "group",
                            "groupKey",
                            "group_key",
                            "targetGroup",
                            "selectedGroup"
                        )
                    )
                }

                fun openMessage(message: CoachHomeMessage) {
                    recentCoachMessages = (
                            listOf(message) + recentCoachMessages
                            )
                        .distinctBy {
                            "${it.sentAt?.time ?: 0L}|${it.coachName}|${it.text.take(40)}"
                        }
                        .take(5)

                    showCoachMessagesDialog = true

                    settingsSp.edit {
                        putBoolean("coach_broadcast_open_dialog", false)
                        putBoolean("coach_broadcast_open_from_push", false)
                        remove("coach_broadcast_push_id")
                        remove("coach_broadcast_push_received_at")
                    }

                    pushBroadcastId = ""
                    openCoachMessagesFromPush = false
                }

                db.collection("coachBroadcasts")
                    .document(cleanPushId)
                    .get()
                    .addOnSuccessListener { doc ->
                        val directMessage = if (doc.exists()) messageFromDoc(doc) else null

                        if (directMessage != null) {
                            openMessage(directMessage)
                        } else {
                            db.collection("coachBroadcasts")
                                .whereEqualTo("broadcastId", cleanPushId)
                                .limit(1)
                                .get()
                                .addOnSuccessListener { snap ->
                                    val message = snap.documents
                                        .firstOrNull()
                                        ?.let { messageFromDoc(it) }

                                    if (message != null) {
                                        openMessage(message)
                                    } else {
                                        db.collection("coachBroadcasts")
                                            .whereEqualTo("broadcast_id", cleanPushId)
                                            .limit(1)
                                            .get()
                                            .addOnSuccessListener { snap2 ->
                                                snap2.documents
                                                    .firstOrNull()
                                                    ?.let { messageFromDoc(it) }
                                                    ?.let { openMessage(it) }
                                            }
                                    }
                                }
                        }
                    }
            }

            DisposableEffect(settingsSp) {
                val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    if (
                        key == "coach_broadcast_open_dialog" ||
                        key == "coach_broadcast_open_from_push" ||
                        key == "coach_broadcast_push_id"
                    ) {
                        openCoachMessagesFromPush =
                            settingsSp.getBoolean("coach_broadcast_open_dialog", false)

                        pushBroadcastId =
                            settingsSp.getString("coach_broadcast_push_id", "").orEmpty()
                    }
                }

                settingsSp.registerOnSharedPreferenceChangeListener(listener)

                onDispose {
                    settingsSp.unregisterOnSharedPreferenceChangeListener(listener)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 0.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(2.dp))

                // === KMI_MULTI_GROUPS (FIX) ===
                var groupsRefreshTick by remember { mutableIntStateOf(0) }

                var coachFromPrefs by remember(userSp) {
                    mutableStateOf(userSp.getString("coach_name", "") ?: "")
                }

                DisposableEffect(userSp) {
                    val l = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                        when (key) {
                            "groups_json",
                            "selected_groups",
                            "groups",
                            "age_groups",
                            "age_group",
                            "group" -> {
                                groupsRefreshTick++
                            }

                            "coach_name" -> coachFromPrefs =
                                userSp.getString("coach_name", "") ?: ""
                        }
                    }
                    userSp.registerOnSharedPreferenceChangeListener(l)
                    onDispose { userSp.unregisterOnSharedPreferenceChangeListener(l) }
                }

                // === KMI_MULTI_GROUPS (FIX) ===

                LaunchedEffect(openCoachMessagesFromPush, recentCoachMessages.size) {
                    if (openCoachMessagesFromPush && recentCoachMessages.isNotEmpty()) {
                        showCoachMessagesDialog = true

                        settingsSp.edit {
                            putBoolean("coach_broadcast_open_dialog", false)
                            putBoolean("coach_broadcast_open_from_push", false)
                            remove("coach_broadcast_push_id")
                            remove("coach_broadcast_push_received_at")
                        }

                        openCoachMessagesFromPush = false
                    }
                }

                // =========================
                // ⭐ הודעות מהמאמן – Firestore
                // מציגים הודעה אחרונה בכרטיס + 5 הודעות אחרונות בדיאלוג
                // =========================
                DisposableEffect(currentUid, userSp, pushBroadcastId) {
                    val uid = currentUid.orEmpty().trim()

                    val currentEmail = FirebaseAuth.getInstance()
                        .currentUser
                        ?.email
                        ?.trim()
                        .orEmpty()

                    fun normalizePhone(raw: String): String =
                        raw.filter { it.isDigit() }

                    fun normalizeText(raw: String): String =
                        raw.trim()
                            .replace('־', '-')
                            .replace('–', '-')
                            .replace('—', '-')
                            .replace(Regex("\\s+"), " ")
                            .lowercase(Locale("he", "IL"))

                    fun prefsAsList(vararg keys: String): List<String> {
                        val out = mutableListOf<String>()

                        keys.forEach { key ->
                            when (val value = userSp.all[key]) {
                                is String -> {
                                    value
                                        .removePrefix("[")
                                        .removeSuffix("]")
                                        .split(',', ';', '|', '\n')
                                        .map { it.trim().trim('"') }
                                        .filter { it.isNotBlank() }
                                        .forEach { out += it }
                                }

                                is Set<*> -> {
                                    value
                                        .mapNotNull { it?.toString()?.trim() }
                                        .filter { it.isNotBlank() }
                                        .forEach { out += it }
                                }

                                is List<*> -> {
                                    value
                                        .mapNotNull { it?.toString()?.trim() }
                                        .filter { it.isNotBlank() }
                                        .forEach { out += it }
                                }
                            }
                        }

                        return out.distinct()
                    }

                    val currentPhones = prefsAsList(
                        "phone",
                        "phoneNumber",
                        "phone_number",
                        "user_phone",
                        "mobile"
                    )
                        .map { normalizePhone(it) }
                        .filter { it.isNotBlank() }
                        .distinct()

                    val currentNames = prefsAsList(
                        "fullName",
                        "full_name",
                        "name",
                        "displayName",
                        "user_name"
                    )

                    val currentBranches = prefsAsList(
                        "active_branch",
                        "activeBranch",
                        "branch",
                        "branches",
                        "branches_json",
                        "selected_branches",
                        "branch2",
                        "branch3"
                    )
                        .map { normalizeText(it) }
                        .filter { it.isNotBlank() }
                        .distinct()

                    val currentGroups = prefsAsList(
                        "active_group",
                        "group",
                        "groups",
                        "groups_json",
                        "selected_groups",
                        "age_group",
                        "age_groups"
                    )
                        .map {
                            normalizeText(
                                TrainingCatalog
                                    .normalizeGroupName(it)
                                    .ifBlank { it }
                            )
                        }
                        .filter { it.isNotBlank() }
                        .distinct()

                    fun stringListFromDoc(
                        doc: DocumentSnapshot,
                        vararg keys: String
                    ): List<String> {
                        val out = mutableListOf<String>()

                        keys.forEach { key ->
                            when (val value = doc.get(key)) {
                                is String -> {
                                    value
                                        .removePrefix("[")
                                        .removeSuffix("]")
                                        .split(',', ';', '|', '\n')
                                        .map { it.trim().trim('"') }
                                        .filter { it.isNotBlank() }
                                        .forEach { out += it }
                                }

                                is List<*> -> {
                                    value
                                        .mapNotNull { it?.toString()?.trim() }
                                        .filter { it.isNotBlank() }
                                        .forEach { out += it }
                                }

                                is Set<*> -> {
                                    value
                                        .mapNotNull { it?.toString()?.trim() }
                                        .filter { it.isNotBlank() }
                                        .forEach { out += it }
                                }
                            }
                        }

                        return out.distinct()
                    }

                    fun mapListFromDoc(
                        doc: DocumentSnapshot,
                        vararg keys: String
                    ): List<Map<String, String>> {
                        val out = mutableListOf<Map<String, String>>()

                        keys.forEach { key ->
                            val value = doc.get(key)

                            if (value is List<*>) {
                                value.forEach { item ->
                                    val map = item as? Map<*, *> ?: return@forEach

                                    val cleanMap = map.mapNotNull { entry ->
                                        val k = entry.key?.toString()?.trim().orEmpty()
                                        val v = entry.value?.toString()?.trim().orEmpty()

                                        if (k.isBlank() || v.isBlank()) {
                                            null
                                        } else {
                                            k to v
                                        }
                                    }.toMap()

                                    if (cleanMap.isNotEmpty()) {
                                        out += cleanMap
                                    }
                                }
                            }
                        }

                        return out
                    }

                    fun firstStringFromDoc(
                        doc: DocumentSnapshot,
                        vararg keys: String
                    ): String {
                        keys.forEach { key ->
                            when (val value = doc.get(key)) {
                                is String -> {
                                    val clean = value.trim()
                                    if (clean.isNotBlank()) return clean
                                }

                                is List<*> -> {
                                    val clean = value
                                        .mapNotNull { it?.toString()?.trim() }
                                        .firstOrNull { it.isNotBlank() }

                                    if (!clean.isNullOrBlank()) return clean
                                }

                                is Set<*> -> {
                                    val clean = value
                                        .mapNotNull { it?.toString()?.trim() }
                                        .firstOrNull { it.isNotBlank() }

                                    if (!clean.isNullOrBlank()) return clean
                                }
                            }
                        }

                        return ""
                    }

                    fun docTargetsCurrentUser(
                        doc: DocumentSnapshot
                    ): Boolean {
                        if (
                            uid.isBlank() &&
                            currentEmail.isBlank() &&
                            currentPhones.isEmpty() &&
                            currentNames.isEmpty() &&
                            currentBranches.isEmpty() &&
                            currentGroups.isEmpty()
                        ) {
                            return false
                        }

                        val authorUid = (
                                doc.getString("authorUid")
                                    ?: doc.getString("coachUid")
                                    ?: doc.getString("senderUid")
                                    ?: ""
                                ).trim()

                        // ✅ המאמן ששלח את ההודעה יראה אותה גם במסך הבית שלו
                        if (uid.isNotBlank() && authorUid == uid) {
                            return true
                        }

                        val uidTargets = stringListFromDoc(
                            doc,
                            "targetUids",
                            "targetUid",
                            "recipientUids",
                            "recipientUid",
                            "uids",
                            "userIds",
                            "userId",
                            "targetIds",
                            "targetId",
                            "participantIds",
                            "participantId",
                            "selectedUids",
                            "selectedUid",
                            "traineeUids",
                            "traineeUid",
                            "traineeIds",
                            "traineeId",
                            "studentUids",
                            "studentUid",
                            "studentIds",
                            "studentId"
                        )

                        if (uid.isNotBlank() && uidTargets.any { it.trim() == uid }) {
                            return true
                        }

                        val recipientMaps = mapListFromDoc(
                            doc,
                            "targetRecipients",
                            "recipients",
                            "selectedRecipients"
                        )

                        if (recipientMaps.isNotEmpty()) {
                            val mapUids = recipientMaps
                                .flatMap { recipient ->
                                    listOf(
                                        recipient["uid"],
                                        recipient["userId"],
                                        recipient["user_id"],
                                        recipient["id"],
                                        recipient["traineeUid"],
                                        recipient["trainee_uid"],
                                        recipient["studentUid"],
                                        recipient["student_uid"]
                                    )
                                }
                                .mapNotNull { it?.trim()?.takeIf { value -> value.isNotBlank() } }

                            if (uid.isNotBlank() && mapUids.any { it == uid }) {
                                return true
                            }
                            val mapEmails = recipientMaps
                                .mapNotNull { it["email"]?.trim()?.takeIf { value -> value.isNotBlank() } }

                            if (
                                currentEmail.isNotBlank() &&
                                mapEmails.any { it.equals(currentEmail, ignoreCase = true) }
                            ) {
                                return true
                            }

                            val mapPhones = recipientMaps
                                .flatMap { recipient ->
                                    listOf(
                                        recipient["phone"],
                                        recipient["phoneNumber"],
                                        recipient["phone_number"],
                                        recipient["mobile"],
                                        recipient["userPhone"],
                                        recipient["user_phone"]
                                    )
                                }
                                .mapNotNull { it?.trim()?.takeIf { value -> value.isNotBlank() } }
                                .map { normalizePhone(it) }
                                .filter { it.isNotBlank() }

                            if (
                                currentPhones.isNotEmpty() &&
                                mapPhones.any { target ->
                                    currentPhones.any { current -> current == target }
                                }
                            ) {
                                return true
                            }

                            val mapNames = recipientMaps
                                .flatMap { recipient ->
                                    listOf(
                                        recipient["name"],
                                        recipient["fullName"],
                                        recipient["full_name"],
                                        recipient["displayName"],
                                        recipient["display_name"],
                                        recipient["userName"],
                                        recipient["user_name"]
                                    )
                                }
                                .mapNotNull { it?.trim()?.takeIf { value -> value.isNotBlank() } }

                            if (
                                currentNames.isNotEmpty() &&
                                mapNames.any { target ->
                                    currentNames.any { current ->
                                        current.trim().equals(target.trim(), ignoreCase = true)
                                    }
                                }
                            ) {
                                return true
                            }
                        }

                        val emailTargets = stringListFromDoc(
                            doc,
                            "targetEmails",
                            "targetEmail",
                            "recipientEmails",
                            "recipientEmail",
                            "emails",
                            "selectedEmails"
                        )

                        if (
                            currentEmail.isNotBlank() &&
                            emailTargets.any { it.equals(currentEmail, ignoreCase = true) }
                        ) {
                            return true
                        }

                        val phoneTargets = stringListFromDoc(
                            doc,
                            "targetPhones",
                            "targetPhone",
                            "recipientPhones",
                            "recipientPhone",
                            "phones",
                            "selectedPhones"
                        ).map { normalizePhone(it) }

                        if (
                            currentPhones.isNotEmpty() &&
                            phoneTargets.any { target ->
                                currentPhones.any { current -> current == target }
                            }
                        ) {
                            return true
                        }

                        val nameTargets = stringListFromDoc(
                            doc,
                            "targetNames",
                            "targetName",
                            "recipientNames",
                            "recipientName",
                            "names",
                            "selectedNames"
                        )

                        if (
                            currentNames.isNotEmpty() &&
                            nameTargets.any { target ->
                                currentNames.any { current ->
                                    current.trim().equals(target.trim(), ignoreCase = true)
                                }
                            }
                        ) {
                            return true
                        }

                        val docBranches = stringListFromDoc(
                            doc,
                            "branch",
                            "branches",
                            "branchName",
                            "branch_name",
                            "targetBranch",
                            "targetBranches",
                            "selectedBranch",
                            "selectedBranches"
                        ).map { normalizeText(it) }

                        val docGroups = stringListFromDoc(
                            doc,
                            "group",
                            "groups",
                            "groupKey",
                            "group_key",
                            "targetGroup",
                            "targetGroups",
                            "selectedGroup",
                            "selectedGroups"
                        ).map {
                            normalizeText(
                                TrainingCatalog
                                    .normalizeGroupName(it)
                                    .ifBlank { it }
                            )
                        }

                        val branchMatches =
                            docBranches.isNotEmpty() &&
                                    currentBranches.any { current ->
                                        docBranches.any { target ->
                                            target == current
                                        }
                                    }

                        val groupMatches =
                            docGroups.isNotEmpty() &&
                                    currentGroups.any { current ->
                                        docGroups.any { target ->
                                            target == current
                                        }
                                    }

                        /*
                         * הודעות חדשות עשויות להכיל גם סניף וגם קבוצה,
                         * אבל הודעות ישנות עשויות להכיל רק אחד מהם.
                         *
                         * אם קיימים שניהם – דורשים התאמה של שניהם.
                         * אם קיים רק אחד – מספיקה ההתאמה שלו.
                         */
                        return when {
                            docBranches.isNotEmpty() &&
                                    docGroups.isNotEmpty() -> {
                                branchMatches && groupMatches
                            }

                            docBranches.isNotEmpty() -> {
                                branchMatches
                            }

                            docGroups.isNotEmpty() -> {
                                groupMatches
                            }

                            else -> {
                                false
                            }
                        }
                    }

                    if (
                        uid.isBlank() &&
                        currentEmail.isBlank() &&
                        currentPhones.isEmpty() &&
                        currentNames.isEmpty() &&
                        currentBranches.isEmpty() &&
                        currentGroups.isEmpty()
                    ) {
                        recentCoachMessages = emptyList()
                        onDispose { }
                    } else {
                        val db =
                            FirebaseFirestore.getInstance()

                        val broadcastsCollection =
                            db.collection("coachBroadcasts")

                        /*
                         * מאחדים תוצאות משתי שאילתות מאובטחות:
                         *
                         * 1. הודעות שהמשתמש נמצא ב-targetUids שלהן.
                         * 2. הודעות שהמשתמש הנוכחי הוא המאמן ששלח אותן.
                         *
                         * אותו מסמך עשוי להופיע בשתי השאילתות ולכן
                         * האיחוד מתבצע לפי מזהה המסמך.
                         */
                        val recipientDocuments =
                            mutableMapOf<String, DocumentSnapshot>()

                        val authoredDocuments =
                            mutableMapOf<String, DocumentSnapshot>()

                        fun publishRecentMessages() {
                            val mergedDocuments =
                                buildMap {
                                    putAll(recipientDocuments)
                                    putAll(authoredDocuments)
                                }
                                    .values
                                    .toList()

                            recentCoachMessages =
                                mergedDocuments
                                    .asSequence()
                                    .filter { doc ->
                                        val docBroadcastId = (
                                                doc.getString("broadcastId")
                                                    ?: doc.getString("broadcast_id")
                                                    ?: doc.id
                                                ).trim()

                                        docTargetsCurrentUser(doc) ||
                                                (
                                                        pushBroadcastId.isNotBlank() &&
                                                                docBroadcastId ==
                                                                pushBroadcastId
                                                        )
                                    }
                                    .mapNotNull { doc ->
                                        val text = (
                                                doc.getString("text")
                                                    ?: doc.getString("message")
                                                    ?: doc.getString("body")
                                                    ?: doc.getString("content")
                                                )
                                            ?.trim()
                                            .orEmpty()

                                        if (text.isBlank()) {
                                            null
                                        } else {
                                            val sentAt =
                                                doc.getTimestamp("createdAt")
                                                    ?.toDate()
                                                    ?: doc.getTimestamp("sentAt")
                                                        ?.toDate()
                                                    ?: doc.getTimestamp("timestamp")
                                                        ?.toDate()
                                                    ?: doc.getLong("createdAtMillis")
                                                        ?.takeIf { it > 0L }
                                                        ?.let { Date(it) }
                                                    ?: doc.getLong("sentAtMillis")
                                                        ?.takeIf { it > 0L }
                                                        ?.let { Date(it) }

                                            CoachHomeMessage(
                                                text = text,
                                                coachName = (
                                                        doc.getString("coachName")
                                                            ?: doc.getString("coach_name")
                                                            ?: doc.getString("senderName")
                                                            ?: doc.getString("fromName")
                                                            ?: "המאמן"
                                                        ).trim(),
                                                sentAt = sentAt,
                                                branch = firstStringFromDoc(
                                                    doc,
                                                    "branch",
                                                    "branchName",
                                                    "branch_name",
                                                    "targetBranch",
                                                    "selectedBranch"
                                                ),
                                                group = firstStringFromDoc(
                                                    doc,
                                                    "group",
                                                    "groups",
                                                    "groupKey",
                                                    "group_key",
                                                    "targetGroup",
                                                    "targetGroups",
                                                    "selectedGroup",
                                                    "selectedGroups"
                                                )
                                            )
                                        }
                                    }
                                    .distinctBy { message ->
                                        listOf(
                                            message.sentAt?.time ?: 0L,
                                            message.coachName.trim(),
                                            message.text.trim(),
                                            message.branch.trim(),
                                            message.group.trim()
                                        ).joinToString("|")
                                    }
                                    .sortedByDescending { message ->
                                        message.sentAt?.time ?: 0L
                                    }
                                    .take(5)
                                    .toList()
                        }

                        val recipientRegistration =
                            broadcastsCollection
                                .whereArrayContains(
                                    "targetUids",
                                    uid
                                )
                                .limit(50)
                                .addSnapshotListener { snapshot, error ->
                                    if (error != null) {
                                        return@addSnapshotListener
                                    }

                                    recipientDocuments.clear()

                                    snapshot
                                        ?.documents
                                        .orEmpty()
                                        .forEach { document ->
                                            recipientDocuments[document.id] =
                                                document
                                        }

                                    publishRecentMessages()
                                }

                        val authoredRegistration =
                            broadcastsCollection
                                .whereEqualTo(
                                    "authorUid",
                                    uid
                                )
                                .limit(50)
                                .addSnapshotListener { snapshot, error ->
                                    if (error != null) {
                                        return@addSnapshotListener
                                    }

                                    authoredDocuments.clear()

                                    snapshot
                                        ?.documents
                                        .orEmpty()
                                        .forEach { document ->
                                            authoredDocuments[document.id] =
                                                document
                                        }

                                    publishRecentMessages()
                                }

                        onDispose {
                            recipientRegistration.remove()
                            authoredRegistration.remove()
                        }
                    }
                }

                var branchesRefreshTick by remember { mutableIntStateOf(0) }

                // ✅ שכבת ביטחון:
                // אם ה־SharedPreferences המקומי לא מכיל את כל הסניפים/קבוצות,
                // נטען את הפרופיל מ־Firestore ונעדכן את kmi_user.
                LaunchedEffect(currentUid) {
                    val uidForProfile =
                        currentUid
                            ?.trim()
                            ?.takeIf { it.isNotBlank() }
                            ?: return@LaunchedEffect

                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(uidForProfile)
                        .get()
                        .addOnSuccessListener { doc ->

                            fun listFromFirestoreListOrCsv(
                                listKey: String,
                                csvKey: String,
                                fallbackKey: String
                            ): List<String> {
                                val fromList = (doc.get(listKey) as? List<*>)
                                    ?.mapNotNull { it?.toString()?.trim() }
                                    ?.filter { it.isNotBlank() }
                                    ?.distinct()
                                    .orEmpty()

                                if (fromList.isNotEmpty()) return fromList

                                val csv = doc.getString(csvKey)
                                    ?.takeIf { it.isNotBlank() }
                                    ?: doc.getString(fallbackKey)
                                    ?: ""

                                return csv
                                    .split(',', ';', '|', '\n')
                                    .map { it.trim() }
                                    .filter { it.isNotBlank() }
                                    .distinct()
                            }

                            val remoteBranches = listFromFirestoreListOrCsv(
                                listKey = "branches",
                                csvKey = "branchesCsv",
                                fallbackKey = "branch"
                            )

                            val remoteGroups = listFromFirestoreListOrCsv(
                                listKey = "groups",
                                csvKey = "groupsCsv",
                                fallbackKey = "primaryGroup"
                            )

                            val remoteActiveBranch =
                                doc.getString("activeBranch")
                                    ?.takeIf { it.isNotBlank() && it in remoteBranches }
                                    ?: remoteBranches.firstOrNull()
                                    ?: ""

                            val remoteActiveGroup =
                                doc.getString("activeGroup")
                                    ?.takeIf { it.isNotBlank() && it in remoteGroups }
                                    ?: remoteGroups.firstOrNull()
                                    ?: ""

                            val branchesCsv = remoteBranches.joinToString(", ")
                            val groupsCsv = remoteGroups.joinToString(", ")
                            val branchesJson = JSONArray(remoteBranches).toString()
                            val groupsJson = JSONArray(remoteGroups).toString()

                            if (remoteBranches.isNotEmpty() || remoteGroups.isNotEmpty()) {
                                userSp.edit {
                                    // ✅ ניקוי טיפוסים ישנים שאולי נשמרו כ־StringSet
                                    remove("branches")
                                    remove("selected_branches")
                                    remove("groups")
                                    remove("selected_groups")

                                    // ✅ סניפים
                                    putString("branch", branchesCsv)
                                    putString("branches", branchesCsv)
                                    putString("branches_json", branchesJson)
                                    putString("selected_branches", branchesCsv)
                                    putString("active_branch", remoteActiveBranch)

                                    // ✅ קבוצות
                                    putString("age_groups", groupsCsv)
                                    putString("groups", groupsCsv)
                                    putString("groups_json", groupsJson)
                                    putString("selected_groups", groupsCsv)
                                    putString("age_group", remoteGroups.firstOrNull().orEmpty())
                                    putString("group", remoteGroups.firstOrNull().orEmpty())
                                    putString("active_group", remoteActiveGroup)
                                }

                                branchesRefreshTick++
                                groupsRefreshTick++
                            }
                        }
                        .addOnFailureListener {
                        }
                }

                DisposableEffect(userSp) {
                    val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                        if (
                            key == "branches_json" ||
                            key == "selected_branches" ||
                            key == "branches" ||
                            key == "branch" ||
                            key == "branch2" ||
                            key == "branch3" ||
                            key == "branch_type"
                        ) {
                            branchesRefreshTick++
                        }
                    }

                    userSp.registerOnSharedPreferenceChangeListener(listener)

                    onDispose {
                        userSp.unregisterOnSharedPreferenceChangeListener(listener)
                    }
                }

                val branchTypeHome = remember(userSp, branchesRefreshTick) {
                    userSp.getString("branch_type", "israel") ?: "israel"
                }

                val isAbroadBranch = branchTypeHome == "abroad"

                /*
                 * המבנה החדש שבו כל סניף מחזיק רק
                                 * את הקבוצות ששויכו אליו ברישום.
                                 */
                val homeBranchAssignments =
                    remember(
                        userSp,
                        branchesRefreshTick,
                        groupsRefreshTick
                    ) {
                        CoachBranchAssignmentsCodec
                            .decode(
                                userSp.getString(
                                    "coach_branch_assignments_json",
                                    ""
                                )
                            )
                    }

                /*
                 * זוגות מדויקים של סניף וקבוצה.
                 *
                 * רק משתמשים ישנים, שעדיין לא נשמר אצלם
                 * המבנה החדש, משתמשים ברשימות הישנות.
                 */
                val branchGroupPairsEffective:
                        List<Pair<String, String>> =
                    remember(
                        homeBranchAssignments
                    ) {
                        homeBranchAssignments
                            .flatMap { assignment ->
                                assignment.groups.map {
                                        groupName ->

                                    assignment.branch.trim() to
                                            groupName.trim()
                                }
                            }
                            .filter {
                                    (branchName, groupName) ->

                                branchName.isNotBlank() &&
                                        groupName.isNotBlank()
                            }
                            .distinct()
                    }

                // ✅ name להצגה + פרמטרים לניווט אימונים חופשיים (נעדכן state כדי שה-FAB יוכל להשתמש גם מחוץ ל-Column)
                val freeName = remember(userSp) {
                    userSp.getString("full_name", null)
                        ?: userSp.getString("name", null)
                        ?: userSp.getString("user_name", null)
                        ?: ""
                }

                LaunchedEffect(
                    branchGroupPairsEffective,
                    currentUid,
                    freeName
                ) {
                    val firstAssignment =
                        branchGroupPairsEffective
                            .firstOrNull()

                    freeBranchUi =
                        firstAssignment
                            ?.first
                            .orEmpty()

                    freeGroupKeyUi =
                        firstAssignment
                            ?.second
                            .orEmpty()

                    freeUidUi =
                        currentUid.orEmpty()

                    freeNameUi =
                        freeName
                }

                LaunchedEffect(
                    openFreeTrainingsFromVoice,
                    freeBranchUi,
                    freeGroupKeyUi,
                    freeUidUi,
                    freeNameUi
                ) {
                    if (openFreeTrainingsFromVoice) {
                        voiceHomeActionsPrefs.edit {
                            putBoolean(
                                "open_free_trainings",
                                false
                            )
                        }

                        openFreeTrainingsFromVoice = false

                        onOpenFreeSessions(
                            freeBranchUi,
                            freeGroupKeyUi,
                            freeUidUi,
                            freeNameUi
                        )
                    }
                }

                data class SlotLike(
                    val dayOfWeek: Int,
                    val startHour: Int,
                    val startMinute: Int,
                    val durationMinutes: Int
                )

                fun <T : AccessibleObject> T.makeAccessibleSafe(): T {
                    try {
                        isAccessible = true
                    } catch (_: SecurityException) {
                        /* ignore */
                    }
                    return this
                }

                fun readSlot(slot: Any): SlotLike {
                    val cls = slot::class.java

                    val dayField =
                        runCatching { cls.getDeclaredField("day").makeAccessibleSafe() }.getOrNull()
                    val startField =
                        runCatching {
                            cls.getDeclaredField("start").makeAccessibleSafe()
                        }.getOrNull()
                    val endField =
                        runCatching { cls.getDeclaredField("end").makeAccessibleSafe() }.getOrNull()

                    if (dayField != null && startField != null && endField != null) {
                        val dayEnum =
                            runCatching { dayField.get(slot) as? DayOfWeek }.getOrNull()
                        val startLt =
                            runCatching { startField.get(slot) as? LocalTime }.getOrNull()
                        val endLt =
                            runCatching { endField.get(slot) as? LocalTime }.getOrNull()

                        val calDay = when (dayEnum) {
                            DayOfWeek.SUNDAY -> Calendar.SUNDAY
                            DayOfWeek.MONDAY -> Calendar.MONDAY
                            DayOfWeek.TUESDAY -> Calendar.TUESDAY
                            DayOfWeek.WEDNESDAY -> Calendar.WEDNESDAY
                            DayOfWeek.THURSDAY -> Calendar.THURSDAY
                            DayOfWeek.FRIDAY -> Calendar.FRIDAY
                            DayOfWeek.SATURDAY -> Calendar.SATURDAY
                            else -> Calendar.MONDAY
                        }

                        val durMin = if (startLt != null && endLt != null)
                            Duration.between(startLt, endLt).toMinutes().toInt()
                        else 90

                        return SlotLike(
                            dayOfWeek = calDay,
                            startHour = startLt?.hour ?: 19,
                            startMinute = startLt?.minute ?: 0,
                            durationMinutes = durMin
                        )
                    }

                    fun intField(vararg names: String, fallback: Int): Int {
                        for (n in names) {
                            val v = runCatching {
                                val f = cls.getDeclaredField(n).makeAccessibleSafe()
                                (f.get(slot) as? Number)?.toInt()
                            }.getOrNull()
                            if (v != null) return v
                        }
                        return fallback
                    }

                    return SlotLike(
                        dayOfWeek = intField(
                            "dayOfWeek",
                            "day",
                            "dow",
                            fallback = Calendar.MONDAY
                        ),
                        startHour = intField("startHour", "hour", "h", fallback = 19),
                        startMinute = intField(
                            "startMinute",
                            "minute",
                            "min",
                            "startMin",
                            fallback = 0
                        ),
                        durationMinutes = intField(
                            "durationMinutes",
                            "duration",
                            "dur",
                            "length",
                            fallback = 90
                        )
                    )
                }

                // הכותרת "מסך הבית" כבר מוצגת ב־KmiTopBar.
                // לכן מסירים את הכותרת הפנימית כדי להרוויח עוד שטח תצוגה.

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush =
                                kmiSectionHeaderBrush()
                        )
                        .padding(vertical = 4.dp)
                ) {
                    TrainingsWeekHeader(
                        isEnglish = isEnglish,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }

                Spacer(Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(
                            brush =
                                Brush.verticalGradient(
                                    colors =
                                        listOf(
                                            MaterialTheme
                                                .colorScheme
                                                .outlineVariant
                                                .copy(
                                                    alpha = 0.86f
                                                ),
                                            MaterialTheme
                                                .colorScheme
                                                .outlineVariant
                                                .copy(
                                                    alpha = 0.38f
                                                ),
                                            Color.Transparent
                                        )
                                )
                        )
                )

                Spacer(Modifier.height(4.dp))

                /*
                 * שעון משותף ליצירת האימונים ולחישוב הסטטוס.
                 * הרענון מאפשר לאימון לעבור אוטומטית בין
                 * מתוכנן, מתקיים והסתיים.
                 */
                var trainingStatusNowMillis by remember {
                    mutableLongStateOf(
                        System.currentTimeMillis()
                    )
                }

                LaunchedEffect(Unit) {
                    while (true) {
                        delay(5.seconds)

                        trainingStatusNowMillis =
                            System.currentTimeMillis()
                    }
                }

                fun upcomingWindowEndMillis(): Long {
                    return Calendar.getInstance().apply {
                        timeInMillis = trainingStatusNowMillis
                        add(Calendar.DAY_OF_YEAR, 6)
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }.timeInMillis
                }

                fun isWithinUpcomingSevenDays(
                    training: TrainingData
                ): Boolean {
                    val windowEndMillis =
                        upcomingWindowEndMillis()

                    /*
                     * אם זמן הסיום אינו קיים, משתמשים בזמן ההתחלה.
                     * ברוב האימונים שנוצרים דרך nextWeekly יהיה
                     * זמן סיום תקין.
                     */
                    val effectiveEndMillis =
                        training.endMillis
                            ?: training.startMillis

                    return effectiveEndMillis >=
                            trainingStatusNowMillis &&
                            training.startMillis <=
                            windowEndMillis
                }

                fun branchScheduleVariants(branch: String): List<String> {
                    val clean = branch
                        .trim()
                        .replace("־", "-")
                        .replace("–", "-")
                        .replace("—", "-")
                        .replace("  ", " ")

                    val pretty = branch
                        .trim()
                        .replace("־", "–")
                        .replace("-", "–")
                        .replace("—", "–")
                        .replace("  ", " ")

                    return listOf(
                        branch.trim(),
                        clean,
                        pretty,
                        clean.replace(" - ", " – "),
                        pretty.replace(" – ", " - ")
                    )
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .distinct()
                }

                fun groupScheduleVariants(group: String): List<String> {
                    val clean = group
                        .trim()
                        .replace("־", "-")
                        .replace("–", "-")
                        .replace("—", "-")
                        .replace("  ", " ")

                    return listOf(
                        group.trim(),
                        clean,
                        clean.replace("+", " + "),
                        clean.replace(" + ", "+"),
                        TrainingCatalog.normalizeGroupName(group)
                            .ifBlank { group }
                    )
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .distinct()
                }

                fun calendarDayFromDatabase(dayOfWeek: String): Int {
                    return when (dayOfWeek.trim().uppercase(Locale.US)) {
                        "SUNDAY" -> Calendar.SUNDAY
                        "MONDAY" -> Calendar.MONDAY
                        "TUESDAY" -> Calendar.TUESDAY
                        "WEDNESDAY" -> Calendar.WEDNESDAY
                        "THURSDAY" -> Calendar.THURSDAY
                        "FRIDAY" -> Calendar.FRIDAY
                        "SATURDAY" -> Calendar.SATURDAY
                        else -> Calendar.MONDAY
                    }
                }

                fun hourFromTimeText(time: String, fallback: Int): Int {
                    return time
                        .substringBefore(":")
                        .trim()
                        .toIntOrNull()
                        ?: fallback
                }

                fun minuteFromTimeText(time: String, fallback: Int): Int {
                    return time
                        .substringAfter(":", "")
                        .trim()
                        .toIntOrNull()
                        ?: fallback
                }

                fun databaseGroupMatches(
                    selectedGroup: String,
                    databaseGroupHe: String,
                    databaseGroupEn: String
                ): Boolean {
                    val wanted = TrainingCatalog
                        .normalizeGroupName(selectedGroup)
                        .ifBlank { selectedGroup }
                        .trim()

                    val dbHe = TrainingCatalog
                        .normalizeGroupName(databaseGroupHe)
                        .ifBlank { databaseGroupHe }
                        .trim()

                    val dbEn = databaseGroupEn.trim()

                    if (wanted.equals(dbHe, ignoreCase = true)) return true
                    if (selectedGroup.trim()
                            .equals(databaseGroupHe.trim(), ignoreCase = true)
                    ) return true
                    if (selectedGroup.trim().equals(dbEn, ignoreCase = true)) return true

                    // התאמות מרחיבות כמו ב-TrainingCatalog:
                    // מי שבחר נוער או בוגרים יכול לקבל גם "נוער + בוגרים".
                    if (wanted == "נוער" && dbHe == "נוער + בוגרים") return true
                    if (wanted == "בוגרים" && dbHe == "נוער + בוגרים") return true

                    return false
                }

                data class HomeTrainingCandidate(
                    val training: TrainingData,
                    val branch: String,
                    val group: String
                )

                fun trainingsFromDatabaseForHome(
                    branchName: String,
                    groupName: String,
                    coachFallback: String
                ): List<HomeTrainingCandidate> {
                    val dbBranch = KmiDatabaseProvider.branchByName(ctx, branchName)
                        ?: return emptyList()

                    val matchingDays = dbBranch.trainingDays.filter { day ->
                        databaseGroupMatches(
                            selectedGroup = groupName,
                            databaseGroupHe = day.groupHe,
                            databaseGroupEn = day.groupEn
                        )
                    }

                    if (matchingDays.isEmpty()) return emptyList()

                    return matchingDays.map { day ->
                        val durationMinutes =
                            day.durationMinutes.takeIf { it > 0 }
                                ?: 90

                        /*
                         * מזיזים את זמן הייחוס לאחור לפי משך האימון.
                         * כך אימון שכבר התחיל אך טרם הסתיים נשאר
                         * המופע של השבוע הנוכחי ולא קופץ לשבוע הבא.
                         */
                        val occurrenceReference =
                            Calendar.getInstance().apply {
                                timeInMillis =
                                    trainingStatusNowMillis

                                add(
                                    Calendar.MINUTE,
                                    -durationMinutes
                                )
                            }

                        HomeTrainingCandidate(
                            training = TrainingData.nextWeekly(
                                dayOfWeek =
                                    calendarDayFromDatabase(
                                        day.dayOfWeek
                                    ),
                                startHour =
                                    hourFromTimeText(
                                        day.startTime,
                                        19
                                    ),
                                startMinute =
                                    minuteFromTimeText(
                                        day.startTime,
                                        0
                                    ),
                                durationMinutes =
                                    durationMinutes,
                                place =
                                    dbBranch.displayPlace(
                                        isEnglish
                                    ),
                                address =
                                    dbBranch.displayAddress(
                                        isEnglish
                                    ),
                                coach =
                                    day.displayCoachName(
                                        isEnglish
                                    ).ifBlank {
                                        coachFallback
                                    },
                                now = occurrenceReference
                            ),
                            branch = branchName.trim(),
                            group = groupName.trim()
                        )
                    }
                }

                val currentWeekCandidates:
                        List<HomeTrainingCandidate> =
                    remember(
                        branchGroupPairsEffective,
                        coachFromPrefs,
                        isEnglish,
                        trainingStatusNowMillis
                    ) {
                        val all =
                            mutableListOf<HomeTrainingCandidate>()

                        branchGroupPairsEffective.forEach {
                                (branchName, grp) ->

                            // ✅ 1) ניסיון ראשון: branches.json דרך KmiDatabaseProvider
                            val dbItems =
                                trainingsFromDatabaseForHome(
                                    branchName = branchName,
                                    groupName = grp,
                                    coachFallback = coachFromPrefs
                                )

                                if (dbItems.isNotEmpty()) {
                                    val validDbItems =
                                        dbItems.filter { candidate ->
                                            isWithinUpcomingSevenDays(
                                                candidate.training
                                            )
                                        }

                                    all += validDbItems
                                    return@forEach
                                }

                            // ✅ 2) Fallback דרך TrainingCatalog
                            val branchVariants =
                                branchScheduleVariants(branchName)

                            val groupVariants =
                                groupScheduleVariants(grp)

                            var matchedBranch = ""
                            var matchedGroup = ""

                            val sched =
                                branchVariants
                                    .asSequence()
                                    .flatMap { branchCandidate ->
                                        groupVariants
                                            .asSequence()
                                            .map { groupCandidate ->
                                                branchCandidate to groupCandidate
                                            }
                                    }
                                    .firstNotNullOfOrNull {
                                            (branchCandidate, groupCandidate) ->

                                        val found =
                                            TrainingCatalog.trainingsFor(
                                                branch = branchCandidate,
                                                group = groupCandidate,
                                                isEnglish = isEnglish
                                            )

                                        found
                                            .takeIf { it.isNotEmpty() }
                                            ?.also {
                                                matchedBranch = branchCandidate
                                                matchedGroup = groupCandidate
                                            }
                                    }

                            val fallbackItems: List<HomeTrainingCandidate> =
                                sched
                                    ?.map { training ->
                                        HomeTrainingCandidate(
                                            training = training,
                                            branch =
                                                matchedBranch
                                                    .ifBlank { branchName }
                                                    .trim(),
                                            group =
                                                matchedGroup
                                                    .ifBlank { grp }
                                                    .trim()
                                        )
                                    }
                                    .orEmpty()

                            val validFallbackItems =
                                fallbackItems.filter { candidate ->
                                    isWithinUpcomingSevenDays(
                                        candidate.training
                                    )
                                }

                            all += validFallbackItems
                        }

                        /*
                         * מנרמלים חלקי טקסט לצורך זיהוי אותו אימון,
                         * גם כאשר קיימים הבדלים קטנים ברווחים,
                         * מקפים או אותיות גדולות/קטנות.
                         */
                        fun normalizeTrainingIdentityPart(
                            value: String
                        ): String {
                            return value
                                .replace("\u200F", "")
                                .replace("\u200E", "")
                                .replace("\u00A0", " ")
                                .trim()
                                .lowercase()
                                .replace("־", "-")
                                .replace("–", "-")
                                .replace("—", "-")
                                .replace(Regex("\\s+"), " ")
                        }

                        /*
    * מזהה מופע פיזי של אימון.
    *
    * הקבוצה אינה חלק מהמפתח בכוונה:
    * אותו אימון עשוי להתאים ליותר מקבוצה אחת,
    * אך הוא צריך להופיע במסך הבית פעם אחת בלבד.
    */
                        fun physicalTrainingKey(
                            candidate: HomeTrainingCandidate
                        ): String {
                            val training = candidate.training

                            val startMinute =
                                training.startMillis / 60_000L

                            val endMinute =
                                (
                                        training.endMillis
                                            ?: training.startMillis
                                        ) / 60_000L

                            val placeIdentity =
                                training.place
                                    .ifBlank {
                                        candidate.branch
                                    }

                            val addressIdentity =
                                training.address

                            return buildString {
                                append(startMinute)
                                append("|")
                                append(endMinute)
                                append("|")
                                append(
                                    normalizeTrainingIdentityPart(
                                        placeIdentity
                                    )
                                )
                                append("|")
                                append(
                                    normalizeTrainingIdentityPart(
                                        addressIdentity
                                    )
                                )
                            }
                        }

                        val result =
                            all
                                .groupBy(::physicalTrainingKey)
                                .values
                                .map { samePhysicalTraining ->
                                    /*
                                     * אם אותו אימון הגיע מכמה מקורות,
                                     * מעדיפים את הרשומה המלאה ביותר.
                                     */
                                    samePhysicalTraining.maxByOrNull { candidate ->
                                        listOf(
                                            candidate.training.place,
                                            candidate.training.address,
                                            candidate.training.coach,
                                            candidate.branch,
                                            candidate.group
                                        )
                                            .count { value ->
                                                value.isNotBlank()
                                            }
                                    } ?: samePhysicalTraining.first()
                                }
                                .sortedBy { candidate ->
                                    candidate.training.startMillis
                                }

                        result
                    }

                /*
                 * הרשימה זמינה כאן בתוך תחום התוכן של
                 * מסך הבית. מעדכנים את מאגר הניווט בכל
                 * פעם שמקורות האימונים משתנים.
                 */
                LaunchedEffect(
                    currentWeekCandidates
                ) {
                    TrainingArchiveNavigationStore.update(
                        currentWeekCandidates.map { candidate ->
                            TrainingArchiveSource(
                                training =
                                    candidate.training,
                                branch =
                                    candidate.branch,
                                group =
                                    candidate.group
                            )
                        }
                    )
                }

                data class HomeTrainingUi(
                    val training: TrainingData,
                    val branch: String,
                    val group: String,
                    val status: TrainingStatusEngine.Status,
                    val occurrenceKey: String,
                    val activeOverride: TrainingOverride?
                ) {
                    val isCancelledByHoliday: Boolean
                        get() =
                            status.state ==
                                    TrainingStatusEngine.State.CANCELLED_BY_HOLIDAY

                    fun cancellationReason(
                        isEnglish: Boolean
                    ): String? {
                        return status.reason(isEnglish)
                    }
                }

                var activeTrainingOverrides by remember {
                    mutableStateOf<Map<String, TrainingOverride>>(
                        emptyMap()
                    )
                }

                val occurrenceKeys =
                    remember(currentWeekCandidates) {
                        currentWeekCandidates
                            .associateBy { candidate ->
                                TrainingOverrideRepository
                                    .buildOccurrenceKey(
                                        training =
                                            candidate.training,
                                        branch =
                                            candidate.branch,
                                        group =
                                            candidate.group
                                    )
                            }
                    }

                DisposableEffect(occurrenceKeys.keys) {
                    val listenerHandle =
                        TrainingOverrideRepository
                            .listenForOccurrenceKeys(
                                occurrenceKeys =
                                    occurrenceKeys.keys,
                                onChanged = { overrides ->
                                    activeTrainingOverrides =
                                        overrides

                                    val timeFormatter =
                                        SimpleDateFormat(
                                            "HH:mm",
                                            Locale("he", "IL")
                                        ).apply {
                                            timeZone =
                                                TimeZone.getTimeZone(
                                                    "Asia/Jerusalem"
                                                )
                                        }

                                    recentTrainingNotices =
                                        overrides
                                            .values
                                            .filter { override ->
                                                override.isActive
                                            }
                                            .mapNotNull { override ->
                                                val eventDate =
                                                    override.updatedAt
                                                        ?.toDate()
                                                        ?: override.createdAt
                                                            ?.toDate()
                                                        ?: Date()

                                                when {
                                                    override.isCancelled -> {
                                                        HomeNotice(
                                                            id =
                                                                "cancelled|${override.occurrenceKey}",
                                                            type =
                                                                HomeNoticeType.TRAINING_CANCELLED,
                                                            title =
                                                                if (isEnglish) {
                                                                    "Training cancelled"
                                                                } else {
                                                                    "האימון בוטל"
                                                                },
                                                            text =
                                                                buildString {
                                                                    append(
                                                                        if (isEnglish) {
                                                                            "The training at "
                                                                        } else {
                                                                            "האימון ב־"
                                                                        }
                                                                    )

                                                                    append(
                                                                        override.place
                                                                            .ifBlank {
                                                                                override.branch
                                                                            }
                                                                    )

                                                                    val reason =
                                                                        override.reason.trim()

                                                                    if (reason.isNotBlank()) {
                                                                        append("\n")

                                                                        append(
                                                                            if (isEnglish) {
                                                                                "Reason: "
                                                                            } else {
                                                                                "סיבה: "
                                                                            }
                                                                        )

                                                                        append(reason)
                                                                    }
                                                                },
                                                            coachName =
                                                                override.changedByName
                                                                    .ifBlank {
                                                                        if (isEnglish) {
                                                                            "Coach"
                                                                        } else {
                                                                            "המאמן"
                                                                        }
                                                                    },
                                                            sentAt = eventDate,
                                                            branch = override.branch,
                                                            group = override.group
                                                        )
                                                    }

                                                    override.hasChangedTime -> {
                                                        val originalStart =
                                                            timeFormatter.format(
                                                                Date(
                                                                    override.originalStartMillis
                                                                )
                                                            )

                                                        val originalEnd =
                                                            timeFormatter.format(
                                                                Date(
                                                                    override.originalEndMillis
                                                                )
                                                            )

                                                        val newStart =
                                                            timeFormatter.format(
                                                                Date(
                                                                    override.effectiveStartMillis
                                                                )
                                                            )

                                                        val newEnd =
                                                            timeFormatter.format(
                                                                Date(
                                                                    override.effectiveEndMillis
                                                                )
                                                            )

                                                        HomeNotice(
                                                            id =
                                                                "time_changed|${override.occurrenceKey}",
                                                            type =
                                                                HomeNoticeType.TRAINING_TIME_CHANGED,
                                                            title =
                                                                if (isEnglish) {
                                                                    "Training time changed"
                                                                } else {
                                                                    "שעת האימון שונתה"
                                                                },
                                                            text =
                                                                buildString {
                                                                    append(
                                                                        override.place
                                                                            .ifBlank {
                                                                                override.branch
                                                                            }
                                                                    )

                                                                    append("\n")

                                                                    append(originalStart)
                                                                    append("–")
                                                                    append(originalEnd)

                                                                    append("  ←  ")

                                                                    append(newStart)
                                                                    append("–")
                                                                    append(newEnd)

                                                                    val reason =
                                                                        override.reason.trim()

                                                                    if (reason.isNotBlank()) {
                                                                        append("\n")

                                                                        append(
                                                                            if (isEnglish) {
                                                                                "Reason: "
                                                                            } else {
                                                                                "סיבה: "
                                                                            }
                                                                        )

                                                                        append(reason)
                                                                    }
                                                                },
                                                            coachName =
                                                                override.changedByName
                                                                    .ifBlank {
                                                                        if (isEnglish) {
                                                                            "Coach"
                                                                        } else {
                                                                            "המאמן"
                                                                        }
                                                                    },
                                                            sentAt = eventDate,
                                                            branch = override.branch,
                                                            group = override.group
                                                        )
                                                    }

                                                    else -> null
                                                }
                                            }
                                            .sortedByDescending { notice ->
                                                notice.sentAt?.time ?: 0L
                                            }
                                            .take(5)
                                },
                                onError = {
                                    /*
                                     * אין מפילים את מסך הבית כאשר
                                     * Firestore אינו זמין זמנית.
                                     */
                                }
                            )

                    onDispose {
                        listenerHandle.remove()
                    }
                }

                val upcoming: List<HomeTrainingUi> =
                    remember(
                        currentWeekCandidates,
                        activeTrainingOverrides,
                        trainingStatusNowMillis
                    ) {
                        currentWeekCandidates
                            .map { candidate ->
                                val occurrenceKey =
                                    TrainingOverrideRepository
                                        .buildOccurrenceKey(
                                            training =
                                                candidate.training,
                                            branch =
                                                candidate.branch,
                                            group =
                                                candidate.group
                                        )

                                HomeTrainingUi(
                                    training =
                                        candidate.training,
                                    branch =
                                        candidate.branch,
                                    group =
                                        candidate.group,
                                    status =
                                        TrainingStatusEngine.evaluate(
                                            context = ctx,
                                            training =
                                                candidate.training,
                                            nowMillis =
                                                trainingStatusNowMillis
                                        ),
                                    occurrenceKey =
                                        occurrenceKey,
                                    activeOverride =
                                        activeTrainingOverrides[
                                            occurrenceKey
                                        ]
                                )
                            }
                            .sortedBy { item ->
                                item.activeOverride
                                    ?.effectiveStartMillis
                                    ?: item.training.startMillis
                            }
                            .filter { item ->
                                item.status.isScheduled ||
                                        item.status.isOngoing ||
                                        item.status.isCancelled ||
                                        item.activeOverride != null
                            }
                    }

                LaunchedEffect(upcoming, isEnglish) {
                    val locale = if (isEnglish) {
                        Locale.ENGLISH
                    } else {
                        Locale("he", "IL")
                    }

                    val dayFmt = SimpleDateFormat("EEEE", locale)
                    val dateFmt = SimpleDateFormat("dd/MM", locale)
                    val timeFmt = SimpleDateFormat("HH:mm", locale)

                    homePdfTrainings = upcoming.map { item ->
                        val training = item.training

                        HomePdfTraining(
                            place = training.place,
                            address = training.address,
                            coach = training.coach,
                            day = dayFmt.format(training.cal.time),
                            date = dateFmt.format(training.cal.time),
                            time = timeFmt.format(training.cal.time),
                            cancellationReason =
                                item.cancellationReason(isEnglish)
                        )
                    }
                }

                val weekBlockedByHoliday = remember(upcoming) {
                    upcoming.isNotEmpty() &&
                            upcoming.all {
                                it.isCancelledByHoliday
                            }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(top = 6.dp, bottom = 28.dp)
                ) {

                    if (upcoming.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 96.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (weekBlockedByHoliday) {
                                        if (isEnglish)
                                            "Passover holiday\nNo trainings this week"
                                        else
                                            "חג פסח / חול המועד פסח\nאין אימונים בשבוע זה"
                                    } else {
                                        if (isAbroadBranch) {
                                            if (isEnglish)
                                                "Training schedule is not available for international branches this week"
                                            else
                                                "אין מידע על אימונים לשבוע הקרוב בסניפי חו״ל"
                                        } else {
                                            if (isEnglish) "No upcoming trainings" else "אין אימונים קרובים"
                                        }

                                    },
                                    style =
                                        KmiTypography.cardTitle,
                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .onBackground,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                )
                            }
                        }
                    } else {
                        items(
                            items = upcoming,
                            key = { item ->
                                buildString {
                                    append(item.occurrenceKey)
                                    append("|holiday=")
                                    append(item.isCancelledByHoliday)
                                }
                            }
                        ) { item ->
                            TrainingCardCompact(
                                training = item.training,
                                group = item.group,
                                isCoach = isCoach,
                                isEnglish = isEnglish,
                                status = item.status,
                                nowMillis = trainingStatusNowMillis,
                                activeOverride =
                                    item.activeOverride,
                                onManageTraining = {
                                    val timeFormatter =
                                        SimpleDateFormat(
                                            "HH:mm",
                                            Locale.getDefault()
                                        )

                                    val dateFormatter =
                                        SimpleDateFormat(
                                            "dd/MM/yyyy",
                                            Locale.getDefault()
                                        )

                                    val displayedStartTime =
                                        if (
                                            item.activeOverride
                                                ?.hasChangedTime == true
                                        ) {
                                            timeFormatter.format(
                                                Date(
                                                    item.activeOverride
                                                        .effectiveStartMillis
                                                )
                                            )
                                        } else {
                                            item.training.start
                                        }

                                    val displayedEndTime =
                                        if (
                                            item.activeOverride
                                                ?.hasChangedTime == true
                                        ) {
                                            timeFormatter.format(
                                                Date(
                                                    item.activeOverride
                                                        .effectiveEndMillis
                                                )
                                            )
                                        } else {
                                            item.training.end
                                        }

                                    val changedByName =
                                        coachFromPrefs
                                            .trim()
                                            .ifBlank {
                                                freeNameUi.trim()
                                            }
                                            .ifBlank {
                                                FirebaseAuth
                                                    .getInstance()
                                                    .currentUser
                                                    ?.displayName
                                                    ?.trim()
                                                    .orEmpty()
                                            }
                                            .ifBlank {
                                                if (isEnglish) {
                                                    "Coach"
                                                } else {
                                                    "מאמן"
                                                }
                                            }

                                    TrainingManagementNavigationStore.open(
                                        TrainingManagementRequest(
                                            uiData =
                                                TrainingManagementUiData(
                                                    occurrenceKey =
                                                        item.occurrenceKey,
                                                    place =
                                                        item.training.place,
                                                    branch = item.branch,
                                                    group = item.group,
                                                    dateText =
                                                        dateFormatter.format(
                                                            item.training
                                                                .cal.time
                                                        ),
                                                    startTime =
                                                        displayedStartTime,
                                                    endTime =
                                                        displayedEndTime
                                                ),
                                            training = item.training,
                                            branch = item.branch,
                                            group = item.group,
                                            changedByName = changedByName
                                        )
                                    )

                                    onOpenTrainingManagement()
                                }
                            )
                        }
                        item { Spacer(Modifier.height(6.dp)) }
                    }

                    item {
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .background(
                                    brush =
                                        Brush.verticalGradient(
                                            colors =
                                                listOf(
                                                    MaterialTheme
                                                        .colorScheme
                                                        .outlineVariant
                                                        .copy(
                                                            alpha = 0.86f
                                                        ),
                                                    MaterialTheme
                                                        .colorScheme
                                                        .outlineVariant
                                                        .copy(
                                                            alpha = 0.38f
                                                        ),
                                                    Color.Transparent
                                                )
                                        )
                                )
                        )
                        Spacer(Modifier.height(6.dp))
                    }

                    // ===== כרטיס הודעות מהמאמן – הודעה אחרונה + דיאלוג הודעות אחרונות =====
                    item {
                        val latestNotice =
                            homeNotices.firstOrNull()

                        val msg =
                            latestNotice
                                ?.text
                                ?.trim()

                        val extraCount =
                            (homeNotices.size - 1)
                                .coerceAtLeast(0)

                        Surface(
                            onClick = {
                                /*
                                 * פותחים תמיד את חלון ההודעות.
                                 * אם טרם התקבלו הודעות, החלון יציג
                                 * הודעה מתאימה במקום להתעלם מהלחיצה.
                                 */
                                clickSound()
                                haptic(true)
                                showCoachMessagesDialog = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface.copy(
                                alpha = 0.96f
                            ),
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp,
                            border = BorderStroke(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.primary.copy(
                                    alpha = 0.45f
                                )
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(
                                        alpha = 0.14f
                                    ),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.primary.copy(
                                            alpha = 0.35f
                                        )
                                    ),
                                    modifier =
                                        Modifier.size(
                                            scaledIconSize(38.dp)
                                        )
                                ) {
                                    Icon(
                                        imageVector =
                                            Icons.Default.Person,
                                        contentDescription = null,
                                        tint =
                                            MaterialTheme
                                                .colorScheme
                                                .primary,
                                        modifier =
                                            Modifier.padding(
                                                scaledIconSize(8.dp)
                                            )
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text =
                                                if (isEnglish) {
                                                    "Messages & Events"
                                                } else {
                                                    "הודעות ואירועים"
                                                },
                                            style = KmiTypography.cardTitle,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )

                                        if (homeNotices.isNotEmpty()) {
                                            Surface(
                                                onClick = {
                                                    showCoachMessagesDialog = true
                                                },
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.primary.copy(
                                                    alpha = 0.14f
                                                ),
                                                border = BorderStroke(
                                                    width = 1.dp,
                                                    color = MaterialTheme.colorScheme.primary.copy(
                                                        alpha = 0.40f
                                                    )
                                                ),
                                                modifier =
                                                    Modifier.size(
                                                        scaledIconSize(32.dp)
                                                    )
                                            ) {
                                                Box(
                                                    contentAlignment =
                                                        Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector =
                                                            Icons.Filled.Email,
                                                        contentDescription =
                                                            if (isEnglish) {
                                                                "Messages and events"
                                                            } else {
                                                                "הודעות ואירועים"
                                                            },
                                                        tint =
                                                            MaterialTheme
                                                                .colorScheme
                                                                .primary,
                                                        modifier =
                                                            Modifier.size(
                                                                scaledIconSize(
                                                                    17.dp
                                                                )
                                                            )
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(4.dp))

                                    if (msg.isNullOrEmpty()) {
                                        Text(
                                            text = if (isEnglish) {
                                                "No new messages right now"
                                            } else {
                                                "אין הודעות חדשות כרגע"
                                            },
                                            style = KmiTypography.body,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        Text(
                                            text = msg,
                                            style = KmiTypography.body,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        val branchGroupLine = buildString {
                                            val b =
                                                latestNotice.branch
                                                    .trim()

                                            val g =
                                                latestNotice.group
                                                    .trim()

                                            if (b.isNotBlank()) {
                                                append(
                                                    if (isEnglish) {
                                                        "Branch: "
                                                    } else {
                                                        "סניף: "
                                                    }
                                                )
                                                append(b)
                                            }

                                            if (g.isNotBlank()) {
                                                if (isNotBlank()) {
                                                    append(" · ")
                                                }

                                                append(
                                                    if (isEnglish) {
                                                        "Group: "
                                                    } else {
                                                        "קבוצה: "
                                                    }
                                                )
                                                append(g)
                                            }
                                        }

                                        if (branchGroupLine.isNotBlank()) {
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                text = branchGroupLine,
                                                style = KmiTypography.secondary,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Spacer(Modifier.height(6.dp))

                                    val timeText = latestNotice?.sentAt?.let {
                                        SimpleDateFormat(
                                            "dd/MM/yyyy · HH:mm",
                                            Locale("he", "IL")
                                        ).format(it)
                                    }.orEmpty()

                                    val openRecentText =
                                        if (extraCount > 0) {
                                            if (isEnglish) {
                                                "Open recent updates · +$extraCount more"
                                            } else {
                                                "פתח הודעות ואירועים אחרונים"
                                            }
                                        } else {
                                            if (isEnglish) {
                                                "Open recent updates"
                                            } else {
                                                "פתח הודעות ואירועים אחרונים"
                                            }
                                        }

                                    if (timeText.isNotBlank() || recentCoachMessages.isNotEmpty()) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            if (timeText.isNotBlank()) {
                                                Text(
                                                    text = timeText,
                                                    style = KmiTypography.caption,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    textAlign =
                                                        if (isEnglish) {
                                                            TextAlign.Left
                                                        } else {
                                                            TextAlign.Right
                                                        },
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }

                                            if (recentCoachMessages.isNotEmpty()) {
                                                Spacer(Modifier.height(3.dp))

                                                Text(
                                                    text = openRecentText,
                                                    style = KmiTypography.caption.copy(
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = MaterialTheme.colorScheme.primary,
                                                    textAlign =
                                                        if (isEnglish) {
                                                            TextAlign.Right
                                                        } else {
                                                            TextAlign.Left
                                                        },
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            showCoachMessagesDialog = true
                                                        }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(1.dp))

                val bubbleTransition = rememberInfiniteTransition(
                    label = "homeBottomButtonBubbleTransition"
                )

                val bubbleOffset by bubbleTransition.animateFloat(
                    initialValue = -120f,
                    targetValue = 320f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = 2600,
                            easing = LinearEasing
                        ),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "homeBottomButtonBubbleOffset"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 12.dp, vertical = 1.dp)
                ) {
                    Surface(
                        onClick = {
                            clickSound()
                            haptic(true)
                            onContinue()
                        },
                        shape = RoundedCornerShape(18.dp),
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(
                                    min = 46.dp,
                                    max = 58.dp
                                )
                                .border(
                                    width = 1.dp,
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.onPrimary.copy(
                                                alpha = 0.85f
                                            ),
                                            MaterialTheme.colorScheme.onPrimary.copy(
                                                alpha = 0.25f
                                            ),
                                            MaterialTheme.colorScheme.onPrimary.copy(
                                                alpha = 0.85f
                                            )
                                        )
                                    ),
                                    shape = RoundedCornerShape(18.dp)
                                )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush =
                                        kmiGraniteActionBrush()
                                )
                        ) {

                            Box(
                                modifier = Modifier
                                    .offset(x = bubbleOffset.dp)
                                    .size(140.dp)
                                    .background(
                                        brush =
                                            Brush.radialGradient(
                                                colors =
                                                    listOf(
                                                        kmiGraniteActionHighlightColor(),
                                                        Color.Transparent
                                                    )
                                            ),
                                        shape = CircleShape
                                    )
                            )

                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector =
                                            Icons.Filled.Star,
                                        contentDescription = null,
                                        tint =
                                            MaterialTheme
                                                .colorScheme
                                                .onPrimary,
                                        modifier =
                                            Modifier.size(
                                                scaledIconSize(16.dp)
                                            )
                                    )

                                    Spacer(Modifier.width(6.dp))

                                    Text(
                                        text =
                                            if (isEnglish) {
                                                "Go to Belt Selection"
                                            } else {
                                                "מעבר לבחירת חגורה"
                                            },
                                        fontWeight =
                                            FontWeight.ExtraBold,
                                        color =
                                            MaterialTheme
                                                .colorScheme
                                                .onPrimary,
                                        style =
                                            KmiTypography.action,
                                        maxLines = 1,
                                        overflow =
                                            TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(2.dp))
            }

            val lockSuffix = if (hasFullAccess) "" else " 🔒"

            val quickMenuItems = listOf(
                Triple(
                    (if (isEnglish) "Voice Assistant" else "עוזר קולי") + lockSuffix,
                    Icons.Filled.Mic
                ) {
                    clickSound()
                    haptic(true)
                    fabExpanded = false

                    if (hasFullAccess) {
                        showAiDialog = true
                    } else {
                        onOpenSubscription()
                    }
                },
                Triple(
                    (if (isEnglish) "Monthly Calendar" else "לוח אימונים חודשי") + lockSuffix,
                    Icons.Filled.DateRange
                ) {
                    clickSound()
                    haptic(true)
                    fabExpanded = false
                    if (hasFullAccess) {
                        onOpenMonthlyCalendar()
                    } else {
                        onOpenSubscription()
                    }
                },
                Triple(
                    (if (isEnglish) "Training Summary" else "סיכום אימון") + lockSuffix,
                    Icons.Filled.EditNote
                ) {
                    clickSound()
                    haptic(true)
                    fabExpanded = false

                    if (hasFullAccess) {
                        onOpenTrainingSummary()
                    } else {
                        onOpenSubscription()
                    }
                },

                Triple(
                    (
                            if (isEnglish) {
                                "Training Archive"
                            } else {
                                "ארכיון אימונים"
                            }
                            ) + lockSuffix,
                    Icons.Filled.History
                ) {
                    clickSound()
                    haptic(true)
                    fabExpanded = false

                    if (hasFullAccess) {
                        onOpenTrainingArchive()
                    } else {
                        onOpenSubscription()
                    }
                },

                Triple(
                    (if (isEnglish) "Free Trainings" else "אימונים חופשיים") + lockSuffix,
                    Icons.Filled.Add
                ) {
                    clickSound()
                    haptic(true)
                    fabExpanded = false
                    if (hasFullAccess) {
                        onOpenFreeSessions(
                            freeBranchUi,
                            freeGroupKeyUi,
                            freeUidUi,
                            freeNameUi
                        )
                    } else {
                        onOpenSubscription()
                    }
                }
            )

            if (fabExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent)
                        .clickable { fabExpanded = false }
                )
            }

            AnimatedVisibility(
                visible = fabExpanded,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .wrapContentSize()
                    .offset(
                        x = 46.dp,
                        y = 88.dp
                    ),
                enter =
                    fadeIn(animationSpec = tween(180)) +
                            scaleIn(
                                initialScale = 0.92f,
                                animationSpec = tween(220)
                            ),
                exit =
                    fadeOut(animationSpec = tween(140)) +
                            scaleOut(
                                targetScale = 0.96f,
                                animationSpec = tween(160)
                            )
            ) {
                HomePremiumQuickMenuPanel(
                    title =
                        if (isEnglish) {
                            "Quick Menu"
                        } else {
                            "תפריט מהיר"
                        },
                    isEnglish = isEnglish,
                    accentColor = homeBeltAccent,
                    items = quickMenuItems,
                    onClose = {
                        fabExpanded = false
                    }
                )
            }

            AnimatedVisibility(
                visible = showFab && !fabExpanded,
                modifier = Modifier
                    // ✅ בדיוק כמו במסך החגורות: צד שמאל פיזי של המסך
                    .align(Alignment.CenterStart)
                    .offset(y = 88.dp),
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                ModernHomeQuickFab(
                    isEnglish = isEnglish,
                    accentColor = activeHomeBelt.color,
                    onClick = {
                        clickSound()
                        haptic(true)
                        fabExpanded = true
                    }
                )
            }

            if (showCoachMessagesDialog) {
                val noticeColors =
                    MaterialTheme.colorScheme

                val noticeAccent =
                    noticeColors.primary

                val noticeCardBrush =
                    Brush.linearGradient(
                        colors = listOf(
                            noticeColors.surface.copy(
                                alpha = 0.97f
                            ),
                            noticeColors.surfaceVariant.copy(
                                alpha = 0.92f
                            ),
                            noticeAccent.copy(
                                alpha = 0.10f
                            )
                        )
                    )

                AlertDialog(
                    onDismissRequest = {
                        showCoachMessagesDialog = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        .clip(
                            RoundedCornerShape(30.dp)
                        )
                        .background(
                            backgroundBrush
                        ),
                    shape =
                        RoundedCornerShape(30.dp),
                    containerColor =
                        Color.Transparent,
                    tonalElevation = 0.dp,
                    title = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment =
                                Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color =
                                    noticeAccent.copy(alpha = 0.14f),
                                tonalElevation = 0.dp,
                                shadowElevation = 0.dp
                            ) {
                                Icon(
                                    imageVector =
                                        Icons.Filled.Email,
                                    contentDescription = null,
                                    tint = noticeAccent,
                                    modifier =
                                        Modifier
                                            .size(
                                                scaledIconSize(40.dp)
                                            )
                                            .padding(
                                                scaledIconSize(9.dp)
                                            )
                                )
                            }

                            Spacer(Modifier.height(10.dp))

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                color =
                                    noticeColors
                                        .surface
                                        .copy(alpha = 0.94f),
                                tonalElevation = 0.dp,
                                shadowElevation = 0.dp,
                                border =
                                    BorderStroke(
                                        width = 1.dp,
                                        color =
                                            noticeColors
                                                .outline
                                                .copy(alpha = 0.28f)
                                    )
                            ) {
                                Text(
                                    text =
                                        if (isEnglish) {
                                            "Recent messages and events"
                                        } else {
                                            "הודעות ואירועים אחרונים"
                                        },
                                    style =
                                        KmiTypography.sectionTitle.copy(
                                            fontWeight =
                                                FontWeight.Black
                                        ),
                                    maxLines = 2,
                                    overflow =
                                        TextOverflow.Ellipsis,
                                    color =
                                        noticeColors
                                            .onSurface,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(
                                        horizontal = 14.dp,
                                        vertical = 10.dp
                                    )
                                )
                            }

                            Spacer(Modifier.height(9.dp))

                            Box(
                                modifier = Modifier
                                    .width(64.dp)
                                    .height(3.dp)
                                    .background(
                                        brush =
                                            Brush.horizontalGradient(
                                                colors = listOf(
                                                    noticeAccent,
                                                    noticeColors.secondary
                                                )
                                            ),
                                        shape =
                                            RoundedCornerShape(999.dp)
                                    )
                            )
                        }
                    },
                    text = {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 430.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (homeNotices.isEmpty()) {
                                item {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(20.dp),
                                        color =
                                            noticeColors.surfaceVariant.copy(
                                                alpha = 0.88f
                                            ),
                                        tonalElevation = 0.dp,
                                        shadowElevation = 0.dp
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(
                                                    horizontal = 16.dp,
                                                    vertical = 20.dp
                                                ),
                                            horizontalAlignment =
                                                Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                imageVector =
                                                    Icons.Filled.Email,
                                                contentDescription = null,
                                                tint =
                                                    noticeAccent.copy(
                                                        alpha = 0.78f
                                                    ),
                                                modifier =
                                                    Modifier.size(
                                                        KmiIconSize.large
                                                    )
                                            )

                                            Spacer(Modifier.height(8.dp))

                                            Text(
                                                text =
                                                    if (isEnglish) {
                                                        "No messages right now."
                                                    } else {
                                                        "אין הודעות כרגע."
                                                    },
                                                color =
                                                    noticeColors.onSurfaceVariant,
                                                fontWeight =
                                                    FontWeight.SemiBold,
                                                textAlign = TextAlign.Center,
                                                modifier =
                                                    Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            } else {
                                items(
                                    items = homeNotices,
                                    key = { notice ->
                                        notice.id
                                    }
                                ) { message ->
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(22.dp),
                                        color = noticeColors.surface,
                                        tonalElevation = 0.dp,
                                        shadowElevation = 0.dp
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    noticeCardBrush
                                                )
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .width(5.dp)
                                                        .fillMaxHeight()
                                                        .background(
                                                            Brush.verticalGradient(
                                                                colors = listOf(
                                                                    noticeAccent,
                                                                    noticeColors.secondary
                                                                )
                                                            )
                                                        )
                                                )

                                                Column(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .padding(
                                                            horizontal = 14.dp,
                                                            vertical = 14.dp
                                                        ),
                                                    horizontalAlignment = if (isEnglish) {
                                                        Alignment.Start
                                                    } else {
                                                        Alignment.End
                                                    }
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = if (isEnglish) {
                                                            Arrangement.Start
                                                        } else {
                                                            Arrangement.End
                                                        }
                                                    ) {
                                                        if (isEnglish) {
                                                            Surface(
                                                                shape = CircleShape,
                                                                color =
                                                                    noticeAccent.copy(
                                                                        alpha = 0.14f
                                                                    ),
                                                                tonalElevation = 0.dp,
                                                                shadowElevation = 0.dp
                                                            ) {
                                                                Icon(
                                                                    imageVector =
                                                                        Icons.Filled.Person,
                                                                    contentDescription =
                                                                        null,
                                                                    tint =
                                                                        noticeAccent,
                                                                    modifier =
                                                                        Modifier
                                                                            .size(
                                                                                scaledIconSize(
                                                                                    30.dp
                                                                                )
                                                                            )
                                                                            .padding(
                                                                                scaledIconSize(
                                                                                    6.dp
                                                                                )
                                                                            )
                                                                )
                                                            }

                                                            Spacer(Modifier.width(8.dp))
                                                        }

                                                        Text(
                                                            text =
                                                                homeCoachDisplayName(
                                                                    realName =
                                                                        message.coachName,
                                                                    isEnglish =
                                                                        isEnglish
                                                                ),
                                                            style =
                                                                KmiTypography.cardTitle,
                                                            color = noticeAccent,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            textAlign =
                                                                if (isEnglish) {
                                                                    TextAlign.Left
                                                                } else {
                                                                    TextAlign.Right
                                                                },
                                                            modifier = Modifier.weight(1f)
                                                        )

                                                        if (!isEnglish) {
                                                            Spacer(Modifier.width(8.dp))

                                                            Surface(
                                                                shape = CircleShape,
                                                                color =
                                                                    noticeAccent.copy(
                                                                        alpha = 0.14f
                                                                    ),
                                                                tonalElevation = 0.dp,
                                                                shadowElevation = 0.dp
                                                            ) {
                                                                Icon(
                                                                    imageVector =
                                                                        Icons.Filled.Person,
                                                                    contentDescription =
                                                                        null,
                                                                    tint =
                                                                        noticeAccent,
                                                                    modifier =
                                                                        Modifier
                                                                            .size(
                                                                                scaledIconSize(
                                                                                    30.dp
                                                                                )
                                                                            )
                                                                            .padding(
                                                                                scaledIconSize(
                                                                                    6.dp
                                                                                )
                                                                            )
                                                                )
                                                            }
                                                        }
                                                    }

                                                    Spacer(Modifier.height(7.dp))

                                                    Text(
                                                        text = message.text,
                                                        style = KmiTypography.body.copy(
                                                            fontWeight = FontWeight.Bold
                                                        ),
                                                        color = noticeColors.onSurface,
                                                        textAlign =
                                                            if (isEnglish) {
                                                                TextAlign.Left
                                                            } else {
                                                                TextAlign.Right
                                                            },
                                                        modifier = Modifier.fillMaxWidth()
                                                    )

                                                    val branchGroupLine = buildString {
                                                        val b = message.branch.trim()
                                                        val g = message.group.trim()

                                                        if (b.isNotBlank()) {
                                                            append(if (isEnglish) "Branch: " else "סניף: ")
                                                            append(b)
                                                        }

                                                        if (g.isNotBlank()) {
                                                            if (isNotBlank()) append(" · ")
                                                            append(if (isEnglish) "Group: " else "קבוצה: ")
                                                            append(g)
                                                        }
                                                    }

                                                    if (branchGroupLine.isNotBlank()) {
                                                        Spacer(Modifier.height(8.dp))

                                                        Surface(
                                                            shape =
                                                                RoundedCornerShape(18.dp),
                                                            color =
                                                                noticeAccent.copy(
                                                                    alpha = 0.12f
                                                                ),
                                                            tonalElevation = 0.dp,
                                                            shadowElevation = 0.dp
                                                        ) {
                                                            Text(
                                                                text = branchGroupLine,
                                                                style =
                                                                    KmiTypography.secondary.copy(
                                                                        fontWeight =
                                                                            FontWeight.SemiBold
                                                                    ),
                                                                color =
                                                                    noticeColors.onSurfaceVariant,
                                                                maxLines = 2,
                                                                overflow = TextOverflow.Ellipsis,
                                                                textAlign =
                                                                    if (isEnglish) {
                                                                        TextAlign.Left
                                                                    } else {
                                                                        TextAlign.Right
                                                                    },
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .padding(
                                                                        horizontal = 10.dp,
                                                                        vertical = 5.dp
                                                                    )
                                                            )
                                                        }
                                                    }

                                                    val timeText = message.sentAt?.let {
                                                        SimpleDateFormat(
                                                            "dd/MM/yyyy · HH:mm",
                                                            Locale("he", "IL")
                                                        ).format(it)
                                                    }.orEmpty()

                                                    if (timeText.isNotBlank()) {
                                                        Spacer(Modifier.height(9.dp))

                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = if (isEnglish) {
                                                                Arrangement.Start
                                                            } else {
                                                                Arrangement.End
                                                            }
                                                        ) {
                                                            Surface(
                                                                shape =
                                                                    RoundedCornerShape(999.dp),
                                                                color =
                                                                    noticeColors.surfaceVariant
                                                                        .copy(alpha = 0.82f),
                                                                tonalElevation = 0.dp,
                                                                shadowElevation = 0.dp
                                                            ) {
                                                                Row(
                                                                    modifier = Modifier.padding(
                                                                        horizontal = 9.dp,
                                                                        vertical = 4.dp
                                                                    ),
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.spacedBy(
                                                                        5.dp
                                                                    )
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Filled.DateRange,
                                                                        contentDescription = null,
                                                                        tint =
                                                                            noticeColors.onSurfaceVariant,
                                                                        modifier =
                                                                            Modifier.size(
                                                                                scaledIconSize(
                                                                                    12.dp
                                                                                )
                                                                            )
                                                                    )

                                                                    Text(
                                                                        text = timeText,
                                                                        style =
                                                                            KmiTypography.caption,
                                                                        color =
                                                                            noticeColors.onSurfaceVariant,
                                                                        maxLines = 1,
                                                                        overflow =
                                                                            TextOverflow.Ellipsis
                                                                    )
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
                    },
                    confirmButton = {
                        Surface(
                            onClick = {
                                clickSound()
                                haptic(true)
                                showCoachMessagesDialog = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp),
                            shape = RoundedCornerShape(18.dp),
                            color = noticeAccent,
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = 18.dp,
                                        vertical = 14.dp
                                    ),
                                horizontalArrangement =
                                    Arrangement.Center,
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {

                                Text(
                                    text =
                                        if (isEnglish) {
                                            "Close"
                                        } else {
                                            "סגור"
                                        },
                                    style =
                                        KmiTypography.cardTitle,
                                    fontWeight =
                                        FontWeight.ExtraBold,
                                    color =
                                        noticeColors.onPrimary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                )
            }

// ===== דיאלוג תרגיל מהחיפוש =====
            pickedKey?.let { key ->
                val (belt, topic, item) = parseSearchKey(key)

                val displayName = ExerciseTitleFormatter
                    .displayName(item)
                    .ifBlank { item }

                val favoriteId = remember(item) { normalizeFavoriteId(item) }

                val favorites: Set<String> by FavoritesStore
                    .favoritesFlow
                    .collectAsState(initial = emptySet())

                val isFavorite = favorites.contains(favoriteId)

                val noteKey = remember(belt, topic, favoriteId) {
                    "note_${belt.id}_${topic.trim()}_${favoriteId}"
                }

                var noteText by remember(noteKey, notesRefreshKey) {
                    mutableStateOf(notePrefs.getString(noteKey, "").orEmpty())
                }

                var showNoteEditor by rememberSaveable(noteKey) {
                    mutableStateOf(false)
                }

                val explanation = remember(belt, item, topic, isEnglish) {
                    findExplanationForHit(
                        belt = belt,
                        rawItem = item,
                        topic = topic,
                        isEnglish = isEnglish
                    )
                }

                ExerciseExplanationDialog(
                    title = if (isEnglish) {
                        ExerciseTitlesEn.getOrSame(displayName)
                    } else {
                        displayName
                    },
                    beltLabel = if (isEnglish) "(${belt.en})" else "(${belt.heb})",
                    explanation = explanation,
                    noteText = noteText,
                    isFavorite = isFavorite,
                    accentColor = belt.color,
                    isEnglish = isEnglish,
                    backgroundBrush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            lerp(
                                MaterialTheme.colorScheme.surface,
                                belt.color,
                                0.12f
                            ),
                            lerp(
                                MaterialTheme.colorScheme.surface,
                                belt.color,
                                0.06f
                            ),
                            MaterialTheme.colorScheme.surface
                        )
                    ),
                    onDismiss = {
                        clickSound()
                        haptic(true)
                        pickedKey = null
                        showNoteEditor = false
                    },
                    onEditNote = {
                        clickSound()
                        haptic(true)
                        showNoteEditor = true
                    },
                    onDeleteNote = {
                        clickSound()
                        haptic(true)

                        noteText = ""
                        saveHomeExerciseNote(noteKey, "")
                    },
                    onToggleFavorite = {
                        clickSound()
                        haptic(true)
                        FavoritesStore.toggle(favoriteId)
                    }
                )

                if (showNoteEditor) {
                    ExerciseNoteEditorDialog(
                        exerciseTitle = if (isEnglish) {
                            ExerciseTitlesEn.getOrSame(displayName)
                        } else {
                            displayName
                        },
                        noteText = noteText,
                        isEnglish = isEnglish,
                        accentColor = belt.color,
                        onNoteChange = { noteText = it },
                        onDismiss = {
                            showNoteEditor = false
                        },
                        onSave = {
                            clickSound()
                            haptic(true)

                            val cleanNote = noteText.trim()
                            noteText = cleanNote

                            saveHomeExerciseNote(noteKey, cleanNote)

                            showNoteEditor = false
                        }
                    )
                }
            }
        }
    }

    // 🔊 דיאלוג העוזר הקולי – מחוץ ל-Box כדי להיות מעל כל המסך
        if (showAiDialog) {
            AiAssistantDialog(
                onDismiss = {
                    showAiDialog = false
                },
                onOpenDrawer = {
                    onOpenDrawer()
                }
            )
        }
    }
}

@Composable
private fun ModernHomeQuickFab(
    isEnglish: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val accentContentColor =
        if (accentColor.luminance() < 0.55f) {
            Color.White
        } else {
            Color.Black
        }

    val tabShape = RoundedCornerShape(
        topStart = 0.dp,
        bottomStart = 0.dp,
        topEnd = 18.dp,
        bottomEnd = 18.dp
    )

    Surface(
        onClick = onClick,
        shape = tabShape,
        color = Color.Transparent,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        border = BorderStroke(
            width = 0.75.dp,
            color =
                MaterialTheme
                    .colorScheme
                    .outlineVariant
                    .copy(alpha = 0.55f)
        ),
        modifier = modifier
            .width(38.dp)
            .height(72.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(tabShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.84f),
                            accentColor,
                            accentColor.copy(alpha = 0.88f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                accentContentColor.copy(
                                    alpha = 0.22f
                                ),
                                Color.Transparent
                            )
                        )
                    )
            )

            Icon(
                imageVector = Icons.Filled.Menu,
                contentDescription =
                    if (isEnglish) {
                        "Open quick menu"
                    } else {
                        "פתח תפריט מהיר"
                    },
                tint = accentContentColor,
                modifier = Modifier.size(
                    KmiIconSize.large
                )
            )
        }
    }
}


/* ========= עזר: למצוא הסבר אמיתי מתוך Explanations ========= */
@Composable
private fun HomePremiumQuickMenuPanel(
    title: String,
    isEnglish: Boolean,
    accentColor: Color,
    items: List<Triple<String, ImageVector, () -> Unit>>,
    onClose: () -> Unit
) {
    val panelShape = RoundedCornerShape(20.dp)

    val colorScheme =
        MaterialTheme.colorScheme

    val isDarkMode =
        colorScheme.background.luminance() < 0.5f

    val panelColor =
        colorScheme.surface

    val panelSecondaryColor =
        colorScheme.surfaceVariant

    /*
     * בחגורה שחורה ובחגורות כהות הטקסט והאייקונים
     * מוצגים בלבן במצב כהה.
     */
    val menuAccent =
        when {
            isDarkMode &&
                    accentColor.luminance() < 0.45f ->
                Color.White

            accentColor == Belt.GREEN.color ->
                kmiSuccessColor()

            !isDarkMode &&
                    accentColor.luminance() > 0.78f ->
                colorScheme.onSurfaceVariant

            else ->
                accentColor
        }

    val borderColor =
        menuAccent.copy(
            alpha = 0.58f
        )

    val dividerColor =
        menuAccent.copy(
            alpha = 0.32f
        )

    Surface(
        shape = panelShape,
        color = panelColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(
            width = 1.dp,
            color = borderColor
        ),
        modifier = Modifier
            .widthIn(
                min = 190.dp,
                max = 230.dp
            )
            .fillMaxWidth()
    ) {
        /*
         * ה־Box מקבל את אותו גובה מינימלי של ה־Surface,
         * ולכן לא נשארת שכבה לבנה גלויה מאחור.
         *
         * הגבול מוגדר רק ב־Surface כדי שלא ייראו
         * שני קווי מסגרת אחד מעל השני.
         */
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(panelShape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            panelColor,
                            panelSecondaryColor.copy(
                                alpha = 0.90f
                            ),
                            menuAccent.copy(
                                alpha = 0.10f
                            ),
                            panelSecondaryColor.copy(
                                alpha = 0.82f
                            ),
                            panelColor
                        )
                    )
                )
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 40.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isEnglish) {
                        Text(
                            text = title,
                            color = menuAccent,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Start,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = KmiTypography.secondary.copy(
                                fontWeight = FontWeight.ExtraBold
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        Icon(
                            imageVector =
                                Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = menuAccent,
                            modifier =
                                Modifier
                                    .size(
                                        KmiIconSize.small
                                    )
                                    .clickable {
                                        onClose()
                                    }
                        )
                    } else {
                        Text(
                            text = title,
                            color = menuAccent,
                            fontWeight =
                                FontWeight.ExtraBold,
                            textAlign = TextAlign.Right,
                            maxLines = 1,
                            overflow =
                                TextOverflow.Ellipsis,
                            style =
                                KmiTypography.cardTitle,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(Modifier.width(6.dp))

                        Icon(
                            imageVector =
                                Icons.Filled.Close,
                            contentDescription = "סגור",
                            tint = menuAccent,
                            modifier =
                                Modifier
                                    .size(
                                        KmiIconSize.small
                                    )
                                    .clickable {
                                        onClose()
                                    }
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                items.forEachIndexed { index, item ->
                    HomePremiumQuickMenuRow(
                        text = item.first,
                        icon = item.second,
                        isEnglish = isEnglish,
                        accentColor = menuAccent,
                        onClick = item.third
                    )

                    if (index != items.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            thickness = 1.25.dp,
                            color = dividerColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomePremiumQuickMenuRow(
    text: String,
    icon: ImageVector,
    isEnglish: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val isLocked = text.endsWith(" 🔒")
    val cleanText = if (isLocked) text.removeSuffix(" 🔒") else text
    val menuAccent =
        accentColor

    val lockPulse =
        rememberInfiniteTransition(
            label = "homeQuickMenuLockPulse"
        )

    val lockScale by lockPulse.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.00f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "homeQuickMenuLockScale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(
                horizontal = 4.dp,
                vertical = 5.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isEnglish) {
            HomePremiumQuickMenuIcon(
                icon = icon,
                accentColor = menuAccent
            )
            Spacer(Modifier.width(7.dp))

            Text(
                text = cleanText,
                color = menuAccent,
                fontWeight =
                    FontWeight.SemiBold,
                textAlign = TextAlign.Start,
                style =
                    KmiTypography.caption.copy(
                        fontWeight =
                            FontWeight.SemiBold
                    ),
                maxLines = 1,
                overflow =
                    TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            if (isLocked) {
                Spacer(Modifier.width(5.dp))
                Icon(
                    imageVector =
                        Icons.Filled.Lock,
                    contentDescription = "Premium feature",
                    tint =
                        MaterialTheme
                            .colorScheme
                            .tertiary,
                    modifier =
                        Modifier
                            .size(
                                scaledIconSize(13.dp)
                            )
                            .graphicsLayer {
                                scaleX = lockScale
                                scaleY = lockScale
                                alpha = 1f
                            }
                )
            }
        } else {
            /*
             * ב־RTL הרכיב הראשון מוצג בצד ימין:
             * האייקון בצד ימין והמנעול בצד שמאל.
             */
            HomePremiumQuickMenuIcon(
                icon = icon,
                accentColor = menuAccent
            )

            Spacer(Modifier.width(7.dp))

            Text(
                text = cleanText,
                color = menuAccent,
                fontWeight =
                    FontWeight.SemiBold,
                textAlign = TextAlign.Right,
                style =
                    KmiTypography.caption.copy(
                        fontWeight =
                            FontWeight.SemiBold
                    ),
                maxLines = 1,
                overflow =
                    TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            if (isLocked) {
                Spacer(Modifier.width(5.dp))

                Icon(
                    imageVector =
                        Icons.Filled.Lock,
                    contentDescription = "תכונת פרימיום",
                    tint =
                        MaterialTheme
                            .colorScheme
                            .tertiary,
                    modifier =
                        Modifier
                            .size(
                                scaledIconSize(13.dp)
                            )
                            .graphicsLayer {
                                scaleX = lockScale
                                scaleY = lockScale
                                alpha = 1f
                            }
                )
            }
        }
    }
}

@Composable
private fun HomePremiumQuickMenuIcon(
    icon: ImageVector,
    accentColor: Color
) {

    Box(
        modifier =
            Modifier
                .size(
                    scaledIconSize(20.dp)
                )
                .background(
                    color =
                        accentColor.copy(
                            alpha = 0.12f
                        ),
                    shape = CircleShape
                )
                .border(
                    width = 1.dp,
                    color =
                        accentColor.copy(
                            alpha = 0.30f
                        ),
                    shape = CircleShape
                ),
        contentAlignment =
            Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier =
                Modifier.size(
                    scaledIconSize(10.5.dp)
                )
        )
    }
}

private fun findExplanationForHit(
    belt: Belt,
    rawItem: String,
    topic: String,
    isEnglish: Boolean
): String {
    val display = ExerciseTitleFormatter
        .displayName(rawItem)
        .ifBlank { rawItem }
        .trim()

    val resolved = ExerciseExplanationResolver.get(
        belt = belt,
        topic = topic,
        item = display,
        isEnglish = isEnglish
    ).trim()

    val cleaned = if ("::" in resolved) {
        resolved
            .split("::")
            .map { it.trim() }
            .lastOrNull { it.isNotBlank() }
            ?: resolved
    } else {
        resolved
    }.trim()

    val isFallback = if (isEnglish) {
        cleaned.isBlank() ||
                cleaned.startsWith("Detailed explanation for:") ||
                cleaned.startsWith("There is currently no explanation")
    } else {
        cleaned.isBlank() ||
                cleaned.startsWith("הסבר מפורט על") ||
                cleaned.startsWith("אין כרגע")
    }

    if (!isFallback) {
        return cleaned
    }

    return if (isEnglish) {
        "There is currently no explanation for this exercise."
    } else {
        "אין כרגע הסבר לתרגיל הזה."
    }
}

/** כרטיס אימון קומפקטי – כמו לפני השינוי, עם סדר אייקונים ישן */
@Composable
private fun TrainingCardCompact(
    training: TrainingData,
    group: String,
    isCoach: Boolean,
    isEnglish: Boolean,
    status: TrainingStatusEngine.Status,
    nowMillis: Long,
    activeOverride: TrainingOverride?,
    onManageTraining: () -> Unit
) {
    val ctx = LocalContext.current
    val haptic = rememberHapticsGlobal()
    val clickSound = rememberClickSound()

    var showNavPicker by rememberSaveable(
        training.cal.timeInMillis
    ) {
        mutableStateOf(false)
    }

    var rememberNavigationChoice by rememberSaveable(
        training.cal.timeInMillis
    ) {
        mutableStateOf(true)
    }

    if (showNavPicker) {
        val navigationAddress =
            training.address
                .trim()

        NavPickerDialog(
            address = navigationAddress,
            isEnglish = isEnglish,
            rememberChoice =
                rememberNavigationChoice,
            onRememberChoiceChange = {
                rememberNavigationChoice = it
            },
            onPick = { navigationChoice ->
                if (navigationAddress.isNotBlank()) {
                    if (rememberNavigationChoice) {
                        writeNavPref(
                            ctx,
                            when (navigationChoice) {
                                NavChoice.WAZE ->
                                    NavAppPref.WAZE

                                NavChoice.GOOGLE_MAPS ->
                                    NavAppPref.GOOGLE_MAPS
                            }
                        )
                    } else {
                        /*
                         * המשתמש ביקש לא לשמור ברירת מחדל,
                         * ולכן בפעם הבאה החלון יוצג שוב.
                         */
                        writeNavPref(
                            ctx,
                            NavAppPref.ASK
                        )
                    }

                    when (navigationChoice) {
                        NavChoice.WAZE -> {
                            openWaze(
                                ctx,
                                navigationAddress
                            )
                        }

                        NavChoice.GOOGLE_MAPS -> {
                            openGoogleMaps(
                                ctx,
                                navigationAddress
                            )
                        }
                    }
                }

                showNavPicker = false
            },
            onDismiss = {
                showNavPicker = false
            }
        )
    }

    val locale = if (isEnglish) {
        Locale.ENGLISH
    } else {
        Locale("he", "IL")
    }

    val effectiveStartMillis =
        activeOverride
            ?.takeIf {
                it.hasChangedTime
            }
            ?.effectiveStartMillis
            ?: training.startMillis

    val effectiveEndMillis =
        activeOverride
            ?.takeIf {
                it.hasChangedTime
            }
            ?.effectiveEndMillis
            ?: training.endMillis
            ?: training.startMillis

    val isTrainingCancelled =
        activeOverride?.isCancelled == true ||
                status.state ==
                TrainingStatusEngine.State.CANCELLED_BY_HOLIDAY

    val millisUntilTraining =
        effectiveStartMillis - nowMillis

    val countdownMinutes =
        if (
            !isTrainingCancelled &&
            millisUntilTraining > 0L &&
            millisUntilTraining <= 30L * 60L * 1_000L
        ) {
            (
                    (millisUntilTraining + 59_999L) /
                            60_000L
                    )
                .toInt()
                .coerceIn(1, 30)
        } else {
            null
        }

    val isTrainingOngoing =
        !isTrainingCancelled &&
                nowMillis >= effectiveStartMillis &&
                nowMillis < effectiveEndMillis

    val visualStatusState =
        if (isTrainingOngoing) {
            TrainingStatusEngine.State.ONGOING
        } else {
            status.state
        }

    val effectiveStartDate =
        remember(effectiveStartMillis) {
            Date(effectiveStartMillis)
        }

    val dayText =
        remember(
            effectiveStartMillis,
            isEnglish
        ) {
            SimpleDateFormat(
                "EEEE",
                locale
            ).apply {
                timeZone =
                    TimeZone.getTimeZone(
                        "Asia/Jerusalem"
                    )
            }.format(effectiveStartDate)
        }

    val dateText =
        remember(
            effectiveStartMillis,
            isEnglish
        ) {
            SimpleDateFormat(
                "dd/MM",
                locale
            ).apply {
                timeZone =
                    TimeZone.getTimeZone(
                        "Asia/Jerusalem"
                    )
            }.format(effectiveStartDate)
        }

    val timeText =
        remember(
            effectiveStartMillis,
            effectiveEndMillis,
            isEnglish
        ) {
            val formatter =
                SimpleDateFormat(
                    "HH:mm",
                    locale
                ).apply {
                    timeZone =
                        TimeZone.getTimeZone(
                            "Asia/Jerusalem"
                        )
                }

            val start =
                formatter.format(
                    Date(effectiveStartMillis)
                )

            val end =
                formatter.format(
                    Date(effectiveEndMillis)
                )

            "$start – $end"
        }
    val dateTimeText =
        remember(
            dayText,
            dateText,
            timeText
        ) {
            "$dayText $dateText · $timeText"
        }

    val trainingCardBorderColor =
        when (visualStatusState) {
            TrainingStatusEngine.State.ONGOING ->
                kmiSuccessColor()

            TrainingStatusEngine.State.COMPLETED ->
                MaterialTheme
                    .colorScheme
                    .outline

            TrainingStatusEngine.State.CANCELLED_BY_HOLIDAY ->
                MaterialTheme
                    .colorScheme
                    .secondary

            TrainingStatusEngine.State.INVALID ->
                MaterialTheme
                    .colorScheme
                    .error

            TrainingStatusEngine.State.SCHEDULED ->
                MaterialTheme
                    .colorScheme
                    .primary
        }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 16.dp,
                top = 12.dp,
                end = 16.dp
            )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 78.dp),
            color =
                MaterialTheme
                    .colorScheme
                    .surface,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(
                width = 1.dp,
                color = trainingCardBorderColor.copy(
                    alpha = 0.35f
                )
            )
        ) {
            Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
                val branchLine =
                    remember(
                        training.place,
                        training.address,
                        isEnglish
                    ) {
                        val displaySource =
                            training.place
                                .trim()
                                .takeIf { it.isNotBlank() }
                                ?: training.address

                        TrainingCatalog.placeDisplayName(
                            displaySource,
                            isEnglish
                        )
                    }

                val groupLine =
                    remember(group) {
                        group.trim()
                    }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {
                Text(
                    text = branchLine,
                    style = KmiTypography.cardTitle,
                    color =
                        MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (groupLine.isNotBlank()) {
                    Text(
                        text = groupLine,
                        style = KmiTypography.secondary,
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = dateTimeText,
                    style = KmiTypography.secondary.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color =
                        MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            val statusMessage =
                when {
                    activeOverride?.isCancelled == true ->
                        if (isEnglish) {
                            "Cancelled by coach"
                        } else {
                            "בוטל על ידי המאמן"
                        }

                    isTrainingOngoing ->
                        if (isEnglish) {
                            "Training in progress"
                        } else {
                            "האימון מתקיים עכשיו"
                        }

                    countdownMinutes != null ->
                        if (isEnglish) {
                            if (countdownMinutes == 1) {
                                "Training starts in 1 minute"
                            } else {
                                "Training starts in $countdownMinutes minutes"
                            }
                        } else {
                            if (countdownMinutes == 1) {
                                "עוד דקה האימון מתחיל"
                            } else {
                                "עוד $countdownMinutes דקות האימון מתחיל"
                            }
                        }

                    activeOverride?.hasChangedTime == true ->
                        if (isEnglish) {
                            "Training time changed"
                        } else {
                            "שעת האימון שונתה"
                        }

                    else ->
                        status.displayText(isEnglish)
                }

            val ongoingPulseTransition =
                rememberInfiniteTransition(
                    label = "ongoingTrainingStatusPulse"
                )

                val ongoingStatusAlpha by
                ongoingPulseTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 0.72f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = 720
                        ),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "ongoingTrainingStatusAlpha"
                )

            val statusAlpha =
                if (isTrainingOngoing) {
                    ongoingStatusAlpha
                } else {
                    1f
                }

                if (statusMessage.isNotBlank()) {
                    val statusContentColor =
                        if (countdownMinutes != null) {
                            kmiOnWarningContainerColor()
                        } else {
                            when (visualStatusState) {
                                TrainingStatusEngine.State.ONGOING ->
                                    kmiOnSuccessContainerColor()

                                TrainingStatusEngine.State.COMPLETED ->
                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant

                                TrainingStatusEngine.State.CANCELLED_BY_HOLIDAY ->
                                    MaterialTheme
                                        .colorScheme
                                        .onSecondaryContainer

                                TrainingStatusEngine.State.INVALID ->
                                    MaterialTheme
                                        .colorScheme
                                        .onErrorContainer

                                TrainingStatusEngine.State.SCHEDULED ->
                                    MaterialTheme
                                        .colorScheme
                                        .onPrimaryContainer
                            }
                        }

                    val statusBackgroundColor =
                        if (countdownMinutes != null) {
                            kmiWarningContainerColor()
                        } else {
                            when (visualStatusState) {
                                TrainingStatusEngine.State.ONGOING ->
                                    kmiSuccessContainerColor()

                                TrainingStatusEngine.State.COMPLETED ->
                                    MaterialTheme
                                        .colorScheme
                                        .surfaceVariant

                                TrainingStatusEngine.State.CANCELLED_BY_HOLIDAY ->
                                    MaterialTheme
                                        .colorScheme
                                        .secondaryContainer

                                TrainingStatusEngine.State.INVALID ->
                                    MaterialTheme
                                        .colorScheme
                                        .errorContainer

                                TrainingStatusEngine.State.SCHEDULED ->
                                    MaterialTheme
                                        .colorScheme
                                        .primaryContainer
                            }
                        }

                    Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = statusBackgroundColor,
                        border = BorderStroke(
                            width = 1.dp,
                            color =
                                when {
                                    isTrainingOngoing ->
                                        kmiSuccessColor()

                                    countdownMinutes != null ->
                                        kmiWarningColor()

                                    else ->
                                        statusContentColor.copy(
                                            alpha = 0.18f
                                        )
                                }
                        ),
                        modifier = Modifier.graphicsLayer {
                            alpha = statusAlpha
                        }
                    ) {
                        Text(
                            text = statusMessage,
                            modifier = Modifier.padding(
                                horizontal = 14.dp,
                                vertical = 5.dp
                            ),
                            textAlign = TextAlign.Center,
                            style = KmiTypography.caption.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = statusContentColor,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                NavigationChip(
                    address =
                        TrainingCatalog.addressDisplayName(
                            training.address,
                            isEnglish
                        ),
                    isEnglish = isEnglish,
                    modifier = Modifier.weight(1f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    NavigationChip(
                        address =
                            TrainingCatalog.addressDisplayName(
                                training.address,
                                isEnglish
                            ),
                        isEnglish = isEnglish,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

                Spacer(Modifier.weight(1f))
            }
        }

        if (isCoach) {
            /*
             * האייקון ממוקם בפינה השמאלית הפיזית
             * גם בעברית וגם באנגלית.
             *
             * ההזזה גורמת לכך שחלק מהעיגול נמצא
             * בתוך הכרטיס וחלקו מחוץ לכרטיס.
             */
            val editButtonAlignment =
                if (isEnglish) {
                    Alignment.TopStart
                } else {
                    Alignment.TopEnd
                }

            Surface(
                modifier = Modifier
                    .align(editButtonAlignment)
                    .absoluteOffset(
                        x = (-10).dp,
                        y = (-10).dp
                    )
                    .size(
                        scaledIconSize(46.dp)
                    )
                    .zIndex(3f),
                shape = CircleShape,
                color =
                    MaterialTheme.colorScheme
                        .primaryContainer,
                border = BorderStroke(
                    width = 1.dp,
                    color =
                        MaterialTheme.colorScheme.primary
                            .copy(alpha = 0.55f)
                ),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                IconButton(
                    onClick = {
                        clickSound()
                        haptic(true)
                        onManageTraining()
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector =
                            Icons.Filled.EditNote,
                        contentDescription =
                            if (isEnglish) {
                                "Change or cancel training"
                            } else {
                                "שינוי או ביטול אימון"
                            },
                        tint =
                            MaterialTheme
                                .colorScheme
                                .primary,
                        modifier =
                            Modifier.size(
                                scaledIconSize(23.dp)
                            )
                    )
                }
            }
        }
    }
}
// ===============================
// ✅ ניווט: כפתור אחד + דיאלוג בחירה יפה
// ===============================

private enum class NavChoice { GOOGLE_MAPS, WAZE }

@Composable
private fun NavigationChip(
    address: String?,
    isEnglish: Boolean,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    val haptic = rememberHapticsGlobal()
    val clickSound = rememberClickSound()

    var showPicker by rememberSaveable(address) { mutableStateOf(false) }
    var rememberChoice by rememberSaveable(address) { mutableStateOf(true) }

    val safeAddress =
        address
            ?.trim()
            .orEmpty()

    /*
     * משמש רק להתאמת שקיפות המסגרת והאייקון.
     * רקע הכרטיס עצמו נשאר גלובלי.
     */
    val isDarkNavigationCard =
        MaterialTheme
            .colorScheme
            .background
            .luminance() < 0.5f

    /*
     * רקע גלובלי לכרטיס הניווט.
     */
    val graniteCardColor =
        MaterialTheme
            .colorScheme
            .surfaceVariant

    fun open(choice: NavChoice) {
        if (safeAddress.isBlank()) return
        when (choice) {
            NavChoice.GOOGLE_MAPS -> openGoogleMaps(ctx, safeAddress)
            NavChoice.WAZE -> openWaze(ctx, safeAddress)
        }
    }

    Surface(
        onClick = {
            clickSound()
            haptic(true)

            if (safeAddress.isNotBlank()) {
                showPicker = true
            }
        },
        shape = RoundedCornerShape(18.dp),

        /*
         * רקע גרניט במקום הרקע הלבן.
         */
        color = graniteCardColor,

        tonalElevation = 0.dp,
        shadowElevation = 0.dp,

        /*
         * קו מתאר עדין באותו סגנון של כרטיס האימון.
         */
        border = BorderStroke(
            width = 0.75.dp,
            color =
                MaterialTheme.colorScheme.primary.copy(
                    alpha =
                        if (isDarkNavigationCard) {
                            0.28f
                        } else {
                            0.20f
                        }
                )
        ),

        modifier = modifier
            .heightIn(min = 62.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // אייקון ניווט פרימיום
            Surface(
                shape = CircleShape,
                color =
                    MaterialTheme.colorScheme.primary.copy(
                        alpha =
                            if (isDarkNavigationCard) {
                                0.18f
                            } else {
                                0.10f
                            }
                    ),
                border = BorderStroke(
                    width = 0.75.dp,
                    color =
                        MaterialTheme.colorScheme.primary.copy(
                            alpha =
                                if (isDarkNavigationCard) {
                                    0.30f
                                } else {
                                    0.20f
                                }
                        )
                ),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                modifier =
                    Modifier.size(
                        scaledIconSize(34.dp)
                    )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.NearMe,
                        contentDescription =
                            if (isEnglish) {
                                "Navigate"
                            } else {
                                "ניווט"
                            },
                        tint =
                            MaterialTheme
                                .colorScheme
                                .primary,
                        modifier =
                            Modifier.size(
                                scaledIconSize(19.dp)
                            )
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (isEnglish) "Navigate" else "ניווט",
                    style = KmiTypography.action,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = safeAddress.ifBlank {
                        if (isEnglish) "No address" else "אין כתובת"
                    },
                    style = KmiTypography.secondary,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    softWrap = true,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(8.dp))
        }
    }

    if (showPicker) {
        NavPickerDialog(
            address = safeAddress,
            isEnglish = isEnglish,
            rememberChoice = rememberChoice,
            onRememberChoiceChange = { rememberChoice = it },
            onPick = { picked ->
                // אם בעתיד תרצה לשמור העדפה ב-SP — אפשר כאן.
                open(picked)
                showPicker = false
            },
            onDismiss = { showPicker = false }
        )
    }
}

@Composable
private fun NavPickerDialog(
    address: String,
    isEnglish: Boolean,
    rememberChoice: Boolean,
    onRememberChoiceChange: (Boolean) -> Unit,
    onPick: (NavChoice) -> Unit,
    onDismiss: () -> Unit
) {
    fun tr(
        he: String,
        en: String
    ): String {
        return if (isEnglish) en else he
    }

    /*
     * משמש רק להתאמת שקיפות המסגרת.
     * צבעי הרקע נשארים גלובליים.
     */
    val isDarkMode =
        MaterialTheme
            .colorScheme
            .background
            .luminance() < 0.5f

    /*
     * צבעי משטח גלובליים לדיאלוג הניווט.
     */
    val graniteCardColor =
        MaterialTheme
            .colorScheme
            .surfaceVariant

    val innerCardColor =
        MaterialTheme
            .colorScheme
            .surface

    Dialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .widthIn(
                        max = 430.dp
                    ),
            shape =
                RoundedCornerShape(
                    28.dp
                ),
            color = graniteCardColor,

            /*
             * ללא הצל העבה שהיה ב־AlertDialog.
             */
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,

            /*
             * קו מתאר עדין בלבד.
             */
            border = BorderStroke(
                width = 0.75.dp,
                color =
                    MaterialTheme.colorScheme.primary.copy(
                        alpha =
                            if (isDarkMode) {
                                0.28f
                            } else {
                                0.20f
                            }
                    )
            )
        ) {
            Column(
                modifier =
                    Modifier.padding(
                        horizontal = 20.dp,
                        vertical = 20.dp
                    ),
                verticalArrangement =
                    Arrangement.spacedBy(
                        16.dp
                    )
            ) {
                /*
                 * כותרת וכתובת.
                 */
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement =
                        Arrangement.spacedBy(
                            6.dp
                        )
                ) {
                    Text(
                        text = tr(
                            "ניווט באמצעות",
                            "Navigate with"
                        ),
                        style =
                            KmiTypography.cardTitle.copy(
                                fontWeight = FontWeight.ExtraBold
                            ),
                        color =
                            MaterialTheme.colorScheme.onSurface,
                        textAlign =
                            if (isEnglish) {
                                TextAlign.Left
                            } else {
                                TextAlign.Right
                            },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = address,
                        style = KmiTypography.secondary,
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign =
                            if (isEnglish) {
                                TextAlign.Left
                            } else {
                                TextAlign.Right
                            },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                /*
                 * בחירת שמירת ברירת המחדל.
                 */
                Surface(
                    modifier =
                        Modifier.fillMaxWidth(),
                    shape =
                        RoundedCornerShape(
                            16.dp
                        ),
                    color = innerCardColor,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    border = BorderStroke(
                        width = 0.5.dp,
                        color =
                            MaterialTheme.colorScheme.outlineVariant
                                .copy(alpha = 0.35f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 14.dp,
                                vertical = 8.dp
                            ),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        Text(
                            text = tr(
                                "זכור בחירה",
                                "Remember selection"
                            ),
                            style =
                                KmiTypography.secondary.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                            color =
                                MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                            textAlign =
                                if (isEnglish) {
                                    TextAlign.Left
                                } else {
                                    TextAlign.Right
                                }
                        )

                        Switch(
                            checked = rememberChoice,
                            onCheckedChange =
                                onRememberChoiceChange
                        )
                    }
                }

                /*
                 * אפליקציות הניווט.
                 */
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            10.dp
                        ),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    NavigationAppChoiceCard(
                        label = "Waze",
                        iconRes = R.drawable.ic_waze,
                        containerColor = innerCardColor,
                        onClick = {
                            onPick(
                                NavChoice.WAZE
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )

                    NavigationAppChoiceCard(
                        label = tr(
                            "Google Maps",
                            "Google Maps"
                        ),
                        iconRes =
                            R.drawable.ic_google_maps,
                        containerColor = innerCardColor,
                        onClick = {
                            onPick(
                                NavChoice.GOOGLE_MAPS
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = tr(
                        "ניתן לשמור את הבחירה כברירת מחדל.",
                        "You can save this selection as the default."
                    ),
                    style = KmiTypography.caption,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign =
                        if (isEnglish) {
                            TextAlign.Left
                        } else {
                            TextAlign.Right
                        },
                    modifier = Modifier.fillMaxWidth()
                )

                TextButton(
                    onClick = onDismiss,
                    modifier =
                        if (isEnglish) {
                            Modifier.align(Alignment.End)
                        } else {
                            Modifier.align(Alignment.Start)
                        }
                ) {
                    Text(
                        text = tr(
                            "סגור",
                            "Close"
                        ),
                        style =
                            KmiTypography.secondary.copy(
                                fontWeight = FontWeight.Bold
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun NavigationAppChoiceCard(
    label: String,
    iconRes: Int,
    containerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier =
            modifier
                .fillMaxHeight()
                .heightIn(
                    min = 56.dp
                ),
        shape =
            RoundedCornerShape(
                16.dp
            ),
        color = containerColor,

        /*
         * ללא צל חיצוני עבה.
         */
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,

        border = BorderStroke(
            width = 0.6.dp,
            color =
                MaterialTheme.colorScheme.primary.copy(
                    alpha = 0.16f
                )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 8.dp,
                    vertical = 12.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically,
            horizontalArrangement =
                Arrangement.Center
        ) {
            Icon(
                painter =
                    painterResource(
                        id = iconRes
                    ),
                contentDescription = label,
                tint = Color.Unspecified,
                modifier =
                    Modifier.size(
                        scaledIconSize(23.dp)
                    )
            )

            Spacer(
                Modifier.width(
                    8.dp
                )
            )

            Text(
                text = label,
                style =
                    KmiTypography.secondary.copy(
                        fontWeight = FontWeight.Bold
                    ),
                color =
                    MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


// ===== העדפת ניווט (Google Maps / Waze / Ask) =====

private enum class NavAppPref { ASK, GOOGLE_MAPS, WAZE }

private const val NAV_PREFS_FILE = "kmi_user"
private const val NAV_PREF_KEY = "nav_app_pref"

private fun writeNavPref(ctx: Context, pref: NavAppPref) {
    val sp = ctx.getSharedPreferences(NAV_PREFS_FILE, Context.MODE_PRIVATE)

    val v = when (pref) {
        NavAppPref.ASK -> "ask"
        NavAppPref.GOOGLE_MAPS -> "gmaps"
        NavAppPref.WAZE -> "waze"
    }

    sp.edit {
        putString(NAV_PREF_KEY, v)
    }
}

private data class HomePdfTraining(
    val place: String,
    val address: String,
    val coach: String,
    val day: String,
    val date: String,
    val time: String,
    val cancellationReason: String?
) {
    val cancelledByHoliday: Boolean
        get() = !cancellationReason.isNullOrBlank()
}

private fun shareHomePdf(
    context: Context,
    trainings: List<HomePdfTraining>,
    isEnglish: Boolean
) {
    /*
     * שכבת הגנת פרטיות לתצוגת ה־PDF בלבד.
     * הרשימה המקורית והנתונים האמיתיים אינם משתנים.
     */
    val displayTrainings =
        trainings.map { training ->
            training.copy(
                coach =
                    homeCoachDisplayName(
                        realName =
                            training.coach,
                        isEnglish =
                            isEnglish
                    )
            )
        }

    val pdfFile =
        createHomePdf(
            context = context,
            trainings = displayTrainings,
            isEnglish = isEnglish
        )

    val uri =
        FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        pdfFile
    )

    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(
            Intent.EXTRA_SUBJECT,
            if (isEnglish) "KAMI home report" else "מסך הבית - KAMI"
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

private fun createHomePdf(
    context: Context,
    trainings: List<HomePdfTraining>,
    isEnglish: Boolean
): File {
    val pageWidth = 595
    val pageHeight = 842
    val margin = 24f

    fun tr(he: String, en: String): String = if (isEnglish) en else he

    val document = PdfDocument()

    var pageNumber = 1

    var page = document.startPage(
        PdfDocument.PageInfo.Builder(
            pageWidth,
            pageHeight,
            pageNumber
        ).create()
    )

    var canvas = page.canvas

    val navy = android.graphics.Color.rgb(2, 43, 74)
    val blue = android.graphics.Color.rgb(12, 78, 130)
    val lightBlue = android.graphics.Color.rgb(234, 246, 255)
    val softBlue = android.graphics.Color.rgb(244, 250, 255)
    val borderBlue = android.graphics.Color.rgb(191, 213, 232)
    val textDark = android.graphics.Color.rgb(15, 23, 42)
    val orange = android.graphics.Color.rgb(249, 115, 22)

    val regular =
        Typeface.create(
            Typeface.SANS_SERIF,
            Typeface.NORMAL
        )
    val bold = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)

    fun alpha(color: Int, value: Float): Int =
        ColorUtils.setAlphaComponent(color, (value.coerceIn(0f, 1f) * 255).toInt())

    fun paint(
        size: Float,
        color: Int = textDark,
        typeface: Typeface = regular,
        align: Paint.Align =
            KmiPdfDirection.textAlign(
                isEnglish = isEnglish
            )
    ) = Paint(
        Paint.ANTI_ALIAS_FLAG
    ).apply {
        textSize = size
        this.color = color
        this.typeface = typeface
        textAlign = align
    }

    val sectionPaint = paint(17f, blue, bold)
    val labelPaint = paint(10.5f, blue, bold)
    val valuePaint = paint(12.5f, textDark, regular)
    val boldValuePaint = paint(13f, textDark, bold)

    fun drawRoundRect(
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

    fun drawHeader() {
        KmiPdfHeader.draw(
            context = context,
            canvas = canvas,
            pageWidth = pageWidth,
            isEnglish = isEnglish,
            titleHebrew = "מסך הבית",
            titleEnglish = "Home",
            subtitleHebrew = "דו״ח אימונים לשבוע הקרוב",
            subtitleEnglish = "Upcoming Weekly Trainings"
        )
    }

    fun drawFooter(
        currentPage: Int,
        totalPages: Int
    ) {
        KmiPdfFooter.draw(
            canvas = canvas,
            pageWidth = pageWidth,
            pageHeight = pageHeight,
            pageNumber = currentPage,
            totalPages = totalPages,
            isEnglish = isEnglish
        )
    }

    fun drawSummary(top: Float): Float {
        drawRoundRect(
            margin,
            top,
            pageWidth - margin,
            top + 78f,
            lightBlue,
            12f
        )
        drawRoundRect(
            margin,
            top,
            pageWidth - margin,
            top + 78f,
            borderBlue,
            12f,
            stroke = true
        )

        val contentRight =
            pageWidth - margin

        val summaryTextAlign =
            KmiPdfDirection.textAlign(
                isEnglish = isEnglish
            )

        val summaryTextX =
            KmiPdfDirection.startPaddingX(
                isEnglish = isEnglish,
                left = margin,
                right = contentRight,
                padding = 22f
            )

        val summaryValueAlign =
            KmiPdfDirection.endTextAlign(
                isEnglish = isEnglish
            )

        val summaryValueX =
            KmiPdfDirection.endPaddingX(
                isEnglish = isEnglish,
                left = margin,
                right = contentRight,
                padding = 28f
            )

        sectionPaint.textAlign =
            summaryTextAlign

        canvas.drawText(
            tr(
                "אימונים לשבוע הקרוב",
                "Upcoming Trainings"
            ),
            summaryTextX,
            top + 32f,
            sectionPaint
        )

        labelPaint.textAlign =
            summaryTextAlign

        canvas.drawText(
            tr(
                "מספר אימונים מוצגים:",
                "Displayed trainings:"
            ),
            summaryTextX,
            top + 58f,
            labelPaint
        )

        boldValuePaint.textAlign =
            summaryValueAlign

        boldValuePaint.textSize = 24f
        boldValuePaint.color = navy

        canvas.drawText(
            "${trainings.size}",
            summaryValueX,
            top + 56f,
            boldValuePaint
        )

        boldValuePaint.textSize = 13f
        boldValuePaint.color = textDark

        return top + 100f
    }

    fun drawEmptyState(top: Float): Float {
        drawRoundRect(
            margin,
            top,
            pageWidth - margin,
            top + 112f,
            softBlue,
            12f
        )
        drawRoundRect(
            margin,
            top,
            pageWidth - margin,
            top + 112f,
            borderBlue,
            12f,
            stroke = true
        )

        sectionPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(
            tr("אין אימונים קרובים", "No upcoming trainings"),
            pageWidth / 2f,
            top + 46f,
            sectionPaint
        )

        valuePaint.textAlign = Paint.Align.CENTER
        canvas.drawText(
            tr(
                "לא נמצאו אימונים לשבוע הקרוב.",
                "No trainings were found for the upcoming week."
            ),
            pageWidth / 2f,
            top + 74f,
            valuePaint
        )

        return top + 132f
    }

    fun drawTrainingCard(
        training: HomePdfTraining,
        top: Float,
        index: Int
    ): Float {
        val cardHeight = if (training.cancelledByHoliday) 116f else 100f
        val right = pageWidth - margin
        val bottom = top + cardHeight
        val mid = pageWidth / 2f

        drawRoundRect(
            margin,
            top,
            right,
            bottom,
            if (index % 2 == 0) lightBlue else softBlue,
            12f
        )
        drawRoundRect(
            margin,
            top,
            right,
            bottom,
            borderBlue,
            12f,
            stroke = true
        )

        val divider = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = borderBlue
            strokeWidth = 1f
        }

        canvas.drawLine(mid, top + 22f, mid, bottom - 20f, divider)

        val cardTextAlign =
            KmiPdfDirection.textAlign(
                isEnglish = isEnglish
            )

        val primaryColumnX =
            KmiPdfDirection.startPaddingX(
                isEnglish = isEnglish,
                left = margin,
                right = right,
                padding = 22f
            )

        /*
         * תחילת העמודה המשנית נמצאת משני צדי
         * קו האמצע בהתאם לכיוון השפה.
         */
        val secondaryColumnX =
            KmiPdfDirection.startPaddingX(
                isEnglish = isEnglish,
                left = mid,
                right = mid,
                padding = 22f
            )

        sectionPaint.textAlign =
            cardTextAlign

        sectionPaint.textSize = 13.5f

        canvas.drawText(
            training.place
                .ifBlank {
                    tr(
                        "מיקום לא הוגדר",
                        "Location not set"
                    )
                }
                .take(34),
            primaryColumnX,
            top + 30f,
            sectionPaint
        )

        sectionPaint.textSize = 17f

        labelPaint.textAlign =
            cardTextAlign

        valuePaint.textAlign =
            cardTextAlign

        boldValuePaint.textAlign =
            cardTextAlign

        canvas.drawText(
            tr(
                "תאריך ושעה:",
                "Date and time:"
            ),
            primaryColumnX,
            top + 58f,
            labelPaint
        )

        canvas.drawText(
            "${training.day} ${training.date} · ${training.time}",
            primaryColumnX,
            top + 76f,
            boldValuePaint
        )

        canvas.drawText(
            tr(
                "כתובת:",
                "Address:"
            ),
            secondaryColumnX,
            top + 30f,
            labelPaint
        )

        canvas.drawText(
            training.address
                .ifBlank { "—" }
                .take(30),
            secondaryColumnX,
            top + 48f,
            valuePaint
        )

        canvas.drawText(
            tr(
                "מאמן:",
                "Coach:"
            ),
            secondaryColumnX,
            top + 72f,
            labelPaint
        )

        canvas.drawText(
            training.coach
                .ifBlank { "—" }
                .take(22),
            secondaryColumnX,
            top + 90f,
            boldValuePaint
        )

        if (training.cancelledByHoliday) {
            drawRoundRect(
                margin + 22f,
                bottom - 28f,
                right - 22f,
                bottom - 9f,
                alpha(orange, 0.12f),
                999f
            )
            drawRoundRect(
                margin + 22f,
                bottom - 28f,
                right - 22f,
                bottom - 9f,
                alpha(orange, 0.42f),
                999f,
                stroke = true
            )

            labelPaint.textAlign = Paint.Align.CENTER
            labelPaint.color = orange
            canvas.drawText(
                if (isEnglish) {
                    "Training cancelled due to ${
                        training.cancellationReason.orEmpty()
                    }"
                } else {
                    "האימון מבוטל עקב ${
                        training.cancellationReason.orEmpty()
                    }"
                },
                pageWidth / 2f,
                bottom - 14f,
                labelPaint
            )
            labelPaint.color = blue
        }

        return bottom + 8f
    }

    /*
   * כמה כרטיסים יכולים להיכנס בעמוד.
   *
   * העמוד הראשון מכיל גם Header + Summary,
   * ולכן נכנסים בו פחות אימונים.
   * בעמודי ההמשך יש יותר מקום.
   */
    /*
     * סימולציה של חלוקת הכרטיסים לעמודים.
     * אימון מבוטל גבוה יותר מאימון רגיל.
     */
    val totalPages =
        if (trainings.isEmpty()) {
            1
        } else {
            var calculatedPages = 1

            /*
             * בעמוד הראשון:
             * כותרת גלובלית + סיכום + כותרת פירוט.
             */
            var calculatedY =
                KmiPdfHeader.CONTENT_TOP +
                        100f +
                        24f

            trainings.forEach { training ->
                val requiredHeight =
                    if (training.cancelledByHoliday) {
                        124f
                    } else {
                        108f
                    }

                if (
                    calculatedY + requiredHeight >=
                    pageHeight -
                    KmiPdfFooter.CONTENT_BOTTOM_PADDING
                ) {
                    calculatedPages++

                    /*
                     * בעמוד המשך:
                     * כותרת גלובלית + כותרת המשך.
                     */
                    calculatedY =
                        KmiPdfHeader.CONTENT_TOP +
                                28f
                }

                calculatedY += requiredHeight
            }

            calculatedPages
        }

    fun startNewPage() {
        pageNumber++

        page = document.startPage(
            PdfDocument.PageInfo.Builder(
                pageWidth,
                pageHeight,
                pageNumber
            ).create()
        )

        canvas = page.canvas

        drawHeader()
    }

    drawHeader()

    var y = KmiPdfHeader.CONTENT_TOP
    y = drawSummary(y)

    sectionPaint.textAlign = Paint.Align.CENTER
    canvas.drawText(
        tr("פירוט אימונים", "Training details"),
        pageWidth / 2f,
        y,
        sectionPaint
    )

    y += 24f

    if (trainings.isEmpty()) {
        drawEmptyState(y)

        drawFooter(
            currentPage = pageNumber,
            totalPages = totalPages
        )

        document.finishPage(page)
    } else {
        trainings.forEachIndexed { index, training ->

            val requiredHeight =
                if (training.cancelledByHoliday) {
                    124f
                } else {
                    108f
                }

            /*
             * אין מספיק מקום לכרטיס הבא:
             * סוגרים את העמוד ומתחילים עמוד חדש.
             */
            if (
                y + requiredHeight >=
                pageHeight -
                KmiPdfFooter.CONTENT_BOTTOM_PADDING
            ) {
                drawFooter(
                    currentPage = pageNumber,
                    totalPages = totalPages
                )

                document.finishPage(page)

                startNewPage()

                y = KmiPdfHeader.CONTENT_TOP

                sectionPaint.textAlign =
                    Paint.Align.CENTER

                canvas.drawText(
                    tr(
                        "המשך פירוט אימונים",
                        "Training details continued"
                    ),
                    pageWidth / 2f,
                    y,
                    sectionPaint
                )

                y += 28f
            }

            y = drawTrainingCard(
                training = training,
                top = y,
                index = index
            )
        }

        drawFooter(
            currentPage = pageNumber,
            totalPages = totalPages
        )

        document.finishPage(page)
    }

    val dir =
        File(
            context.cacheDir,
            "pdfs"
        ).apply {
            mkdirs()
        }

    val fileName =
        if (isEnglish) {
            "Upcoming Trainings.pdf"
        } else {
            "אימונים קרובים.pdf"
        }

    val file =
        File(
            dir,
            fileName
        )

    FileOutputStream(
        file,
        false
    ).use { output ->
        document.writeTo(output)
    }

    document.close()

    return file
}

// ===== עזרי ניווט מפות =====
private fun openGoogleMaps(ctx: Context, address: String?) {
    val safeAddress = address?.trim().orEmpty()
    if (safeAddress.isEmpty()) return

    val mapsPkg = "com.google.android.apps.maps"

    // ✅ אם Maps מותקן – לפתוח ישר
    if (isGoogleMapsInstalled(ctx)) {
        val navUri =
            ("google.navigation:q=" + Uri.encode(safeAddress)).toUri()
        val i = Intent(Intent.ACTION_VIEW, navUri).apply {
            setPackage(mapsPkg)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { ctx.startActivity(i) }
        return
    }

    // ❗ Maps לא מותקן → לפתוח Play Store בלי chooser של חנויות
    openPlayStoreDirect(ctx)
}

private fun isGoogleMapsInstalled(ctx: Context): Boolean {
    return runCatching {
        ctx.packageManager.getPackageInfo(
            "com.google.android.apps.maps",
            0
        )
        true
    }.getOrElse { false }
}

private fun openPlayStoreDirect(ctx: Context) {
    val playPkg = "com.android.vending" // Google Play
    val mapsPkg = "com.google.android.apps.maps"

    // ניסיון 1: לפתוח Play Store ישירות (בלי לשאול Galaxy Store)
    val marketUri = "market://details?id=$mapsPkg".toUri()
    val playIntent = Intent(Intent.ACTION_VIEW, marketUri).apply {
        setPackage(playPkg)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    if (playIntent.resolveActivity(ctx.packageManager) != null) {
        ctx.startActivity(playIntent)
        return
    }

    // ניסיון 2: fallback לדפדפן (אם אין Play Store)
    val webUri =
        "https://play.google.com/store/apps/details?id=$mapsPkg".toUri()

    ctx.startActivity(
        Intent(Intent.ACTION_VIEW, webUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    )
}

private fun openWaze(ctx: Context, address: String?) {
    val safeAddress = address?.trim().orEmpty()
    if (safeAddress.isEmpty()) return

    val wazeUri = ("https://waze.com/ul?q=" + Uri.encode(safeAddress)).toUri()
    val intent = Intent(Intent.ACTION_VIEW, wazeUri).apply {
        setPackage("com.waze")
    }

    if (intent.resolveActivity(ctx.packageManager) != null) {
        ctx.startActivity(intent)
    } else {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, wazeUri))
    }
}
