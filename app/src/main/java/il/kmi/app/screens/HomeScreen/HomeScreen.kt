package il.kmi.app.screens

import android.content.Context
import android.content.SharedPreferences
import android.content.Intent
import android.net.Uri
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
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
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.material3.Switch
import il.kmi.app.ui.rememberHapticsGlobal
import il.kmi.app.ui.rememberClickSound
import il.kmi.app.ui.assistant.ui.AiAssistantDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
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
import android.graphics.Path
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material.icons.filled.History
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.DocumentSnapshot
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager
import il.kmi.app.database.KmiDatabaseProvider
import il.kmi.app.domain.ExerciseExplanationResolver
import il.kmi.app.training.TrainingCatalog
import il.kmi.app.training.TrainingDirectory
import il.kmi.app.ui.KmiTopBar
import il.kmi.app.ui.KmiTypography
import il.kmi.shared.domain.content.ExerciseTitlesEn
import kotlinx.coroutines.delay
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults

//=================================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CoachInfoCard(
    coachName: String?,
    branchName: String?,
    city: String?,
    groupName: String?,
    onOpenProfile: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptic = rememberHapticsGlobal()
    val clickSound = rememberClickSound()

    Surface(
        shape = MaterialTheme.shapes.large,
        color = Color.White.copy(alpha = 0.92f),
        tonalElevation = 1.dp,
        shadowElevation = 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "מאמן: ${coachName.orEmpty()}",
                    style = KmiTypography.cardTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val branchLine = buildString {
                    if (!city.isNullOrBlank()) append(city).append(" · ")
                    append(branchName.orEmpty())
                }

                Text(
                    text = branchLine,
                    style = KmiTypography.secondary,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "קבוצה: ${groupName.orEmpty()}",
                    style = KmiTypography.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = {
                    clickSound()
                    haptic(true)
                    onOpenProfile()
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = "פרופיל",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private enum class HomeNoticeType {
    COACH_MESSAGE,
    TRAINING_TIME_CHANGED,
    TRAINING_CANCELLED,
    TRAINING_RESTORED
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
                    MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
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
                style =
                    MaterialTheme.typography.bodySmall,
                color =
                    Color.White.copy(alpha = 0.92f),
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun <T : AccessibleObject> T.makeAccessible(): T {
    try {
        isAccessible = true
    } catch (_: SecurityException) { /* ignore */
    }
    return this
}

private enum class TrainingManagementMode {
    MENU,
    CANCEL,
    CHANGE_TIME
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrainingClockPickerDialog(
    initialTime: String,
    title: String,
    confirmText: String,
    cancelText: String,
    onDismiss: () -> Unit,
    onTimeSelected: (String) -> Unit
) {
    val initialParts =
        remember(initialTime) {
            initialTime
                .trim()
                .split(":")
        }

    val initialHour =
        initialParts
            .getOrNull(0)
            ?.toIntOrNull()
            ?.coerceIn(0, 23)
            ?: 19

    val initialMinute =
        initialParts
            .getOrNull(1)
            ?.toIntOrNull()
            ?.coerceIn(0, 59)
            ?: 0

    val pickerState =
        rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute,
            is24Hour = true
        )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Black
            )
        },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TimePicker(
                    state = pickerState
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val selectedTime =
                        String.format(
                            Locale.US,
                            "%02d:%02d",
                            pickerState.hour,
                            pickerState.minute
                        )

                    onTimeSelected(selectedTime)
                }
            ) {
                Text(
                    text = confirmText,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    text = cancelText,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        shape = RoundedCornerShape(26.dp),
        containerColor = Color.White
    )
}

@OptIn(ExperimentalMaterial3Api::class)
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
    onOpenTrainingSummary: () -> Unit
) {
    val haptic = rememberHapticsGlobal()
    val clickSound = rememberClickSound()

    // 🔵 מצב לדיאלוג העוזר האישי (AI)
    var showAiDialog by rememberSaveable {
        mutableStateOf(false)
    }

    // 🗂️ מצב חלון ארכיון האימונים
    var showTrainingArchive by rememberSaveable {
        mutableStateOf(false)
    }

    val ctxRole = LocalContext.current
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

        notePrefs.edit().apply {
            if (clean.isBlank()) {
                remove(noteKey)
            } else {
                putString(noteKey, clean)
            }
        }.apply()

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

    val backgroundBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFF8FBFF),
                Color(0xFFEAF4FF),
                Color(0xFFB7DDF7),
                Color(0xFF1F78B4),
                Color(0xFF062B4A)
            )
        )
    }

    Scaffold(
        topBar = {

            val contextLang = LocalContext.current
            val langManager = remember { AppLanguageManager(contextLang) }

            KmiTopBar(
                title = if (langManager.getCurrentLanguage() == AppLanguage.ENGLISH) "Home" else "מסך הבית",
                onHome = { /* no-op במסך הבית */ },
                lockHome = true,
                homeDisabledToast = "אתה כבר במסך הבית 🙂",
                showTopHome = false,
                showTopShare = false,

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
            val userSp = remember { ctx.getSharedPreferences("kmi_user", Context.MODE_PRIVATE) }
            val subsSp = remember { ctx.getSharedPreferences("kmi_subs", Context.MODE_PRIVATE) }
            val legacySp = remember { ctx.getSharedPreferences("kmi_prefs", Context.MODE_PRIVATE) }
            val settingsSp = remember { ctx.getSharedPreferences("kmi_settings", Context.MODE_PRIVATE) }

            var homeAccessRefreshTick by remember { mutableIntStateOf(0) }

            // מצב הגישה מתרענן דרך SharedPreferences listener.
            // אין צורך בלולאת רענון קבועה במסך הבית.

            DisposableEffect(userSp, subsSp, legacySp) {
                val listener =
                    SharedPreferences.OnSharedPreferenceChangeListener { changedSp, key ->
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
                val active = hasSubscriptionFlags && until > now

                // אם הזמן עבר — מנקים את כל הדגלים הישנים כדי שהמנעולים יחזרו.
                if (!active && hasSubscriptionFlags && until > 0L && until <= now) {
                    edit()
                        .putBoolean("google_subscription_verified", false)
                        .putBoolean("has_full_access", false)
                        .putBoolean("full_access", false)
                        .putBoolean("subscription_active", false)
                        .putBoolean("is_subscribed", false)
                        .remove("sub_product")
                        .remove("sub_token")
                        .remove("sub_purchase_time")
                        .remove("sub_access_until")
                        .putLong("access_changed_at", System.currentTimeMillis())
                        .apply()

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
            val currentUid = remember {
                FirebaseAuth.getInstance().currentUser?.uid
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

                    settingsSp.edit()
                        .putBoolean("coach_broadcast_open_dialog", false)
                        .putBoolean("coach_broadcast_open_from_push", false)
                        .remove("coach_broadcast_push_id")
                        .remove("coach_broadcast_push_received_at")
                        .apply()

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
                fun readSelectedGroups(sp: SharedPreferences): List<String> {
                    fun splitGroups(raw: String): List<String> {
                        return raw
                            .split(',', ';', '|', '\n')
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                    }

                    fun readPrefValueAsList(key: String): List<String> {
                        val value = sp.all[key] ?: return emptyList()

                        return when (value) {
                            is String -> {
                                val raw = value.trim()
                                if (raw.isBlank()) {
                                    emptyList()
                                } else if (raw.startsWith("[")) {
                                    runCatching {
                                        val arr = JSONArray(raw)
                                        (0 until arr.length())
                                            .mapNotNull { index -> arr.optString(index, null) }
                                            .map { it.trim() }
                                            .filter { it.isNotBlank() }
                                    }.getOrDefault(emptyList())
                                } else {
                                    splitGroups(raw)
                                }
                            }

                            is Set<*> -> {
                                value
                                    .mapNotNull { it?.toString()?.trim() }
                                    .filter { it.isNotBlank() }
                            }

                            else -> emptyList()
                        }
                    }

                    fun readListFromPrefs(vararg keys: String): List<String> {
                        return keys
                            .flatMap { key -> readPrefValueAsList(key) }
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                            .distinct()
                    }

                    return readListFromPrefs(
                        "groups_json",
                        "selected_groups",
                        "groups",
                        "age_groups",
                        "age_group",
                        "active_group",
                        "activeGroup",
                        "group"
                    )
                        .map {
                            TrainingCatalog
                                .normalizeGroupName(it)
                                .ifBlank { it }
                        }
                        .filter { it.isNotBlank() }
                        .distinct()
                }

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

                val groupsEffective: List<String> = remember(userSp, groupsRefreshTick) {
                    readSelectedGroups(userSp)
                }

                // === KMI_MULTI_GROUPS (FIX) ===

                LaunchedEffect(openCoachMessagesFromPush, recentCoachMessages.size) {
                    if (openCoachMessagesFromPush && recentCoachMessages.isNotEmpty()) {
                        showCoachMessagesDialog = true

                        settingsSp.edit()
                            .putBoolean("coach_broadcast_open_dialog", false)
                            .putBoolean("coach_broadcast_open_from_push", false)
                            .remove("coach_broadcast_push_id")
                            .remove("coach_broadcast_push_received_at")
                            .apply()

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
                                        docBranches.any { it == current }
                                    }

                        val groupMatches =
                            docGroups.isNotEmpty() &&
                                    currentGroups.any { current ->
                                        docGroups.any { it == current }
                                    }

                        return branchMatches && groupMatches
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
                        val db = FirebaseFirestore.getInstance()

                        val query = db.collection("coachBroadcasts")
                            .orderBy("createdAt", Query.Direction.DESCENDING)
                            .limit(40)

                        val reg = query.addSnapshotListener { snap, e ->
                            if (e != null) {
                                return@addSnapshotListener
                            }

                            recentCoachMessages = snap
                                ?.documents
                                .orEmpty()
                                .filter { doc ->
                                    val docBroadcastId = (
                                            doc.getString("broadcastId")
                                                ?: doc.getString("broadcast_id")
                                                ?: doc.id
                                            ).trim()

                                    docTargetsCurrentUser(doc) ||
                                            pushBroadcastId.isNotBlank() &&
                                            docBroadcastId == pushBroadcastId
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
                                        CoachHomeMessage(
                                            text = text,
                                            coachName = (
                                                    doc.getString("coachName")
                                                        ?: doc.getString("coach_name")
                                                        ?: doc.getString("senderName")
                                                        ?: doc.getString("fromName")
                                                        ?: "המאמן"
                                                    ).trim(),
                                            sentAt = doc.getTimestamp("createdAt")?.toDate()
                                                ?: doc.getTimestamp("sentAt")?.toDate()
                                                ?: doc.getTimestamp("timestamp")?.toDate(),
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
                                                "groupKey",
                                                "group_key",
                                                "targetGroup",
                                                "selectedGroup"
                                            )
                                        )
                                    }
                                }
                                .take(5)
                        }

                        onDispose { reg.remove() }
                    }
                }

                // =========================

                fun readSelectedBranches(sp: SharedPreferences): List<String> {
                    fun splitBranches(raw: String): List<String> {
                        return raw
                            .split(',', ';', '|', '\n')
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                    }

                    fun readPrefValueAsList(key: String): List<String> {
                        val value = sp.all[key] ?: return emptyList()

                        return when (value) {
                            is String -> {
                                val raw = value.trim()
                                if (raw.isBlank()) {
                                    emptyList()
                                } else if (raw.startsWith("[")) {
                                    runCatching {
                                        val arr = JSONArray(raw)
                                        (0 until arr.length())
                                            .mapNotNull { index -> arr.optString(index, null) }
                                            .map { it.trim() }
                                            .filter { it.isNotBlank() }
                                    }.getOrDefault(emptyList())
                                } else {
                                    splitBranches(raw)
                                }
                            }

                            is Set<*> -> {
                                value
                                    .mapNotNull { it?.toString()?.trim() }
                                    .filter { it.isNotBlank() }
                            }

                            else -> emptyList()
                        }
                    }

                    fun readListFromPrefs(vararg keys: String): List<String> {
                        return keys
                            .flatMap { key -> readPrefValueAsList(key) }
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                            .distinct()
                    }

                    val fromAllSources = readListFromPrefs(
                        "branches_json",
                        "selected_branches",
                        "branches",
                        "branch",
                        "active_branch",
                        "activeBranch",
                        "branch2",
                        "branch3"
                    )

                    return fromAllSources
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .distinct()
                }

                var branchesRefreshTick by remember { mutableIntStateOf(0) }

                // ✅ שכבת ביטחון:
                // אם ה־SharedPreferences המקומי לא מכיל את כל הסניפים/קבוצות,
                // נטען את הפרופיל מ־Firestore ונעדכן את kmi_user.
                LaunchedEffect(currentUid) {
                    if (currentUid.isNullOrBlank()) return@LaunchedEffect

                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(currentUid)
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
                                userSp.edit()
                                    // ✅ ניקוי טיפוסים ישנים שאולי נשמרו כ־StringSet
                                    .remove("branches")
                                    .remove("selected_branches")
                                    .remove("groups")
                                    .remove("selected_groups")

                                    // ✅ סניפים
                                    .putString("branch", branchesCsv)
                                    .putString("branches", branchesCsv)
                                    .putString("branches_json", branchesJson)
                                    .putString("selected_branches", branchesCsv)
                                    .putString("active_branch", remoteActiveBranch)

                                    // ✅ קבוצות
                                    .putString("age_groups", groupsCsv)
                                    .putString("groups", groupsCsv)
                                    .putString("groups_json", groupsJson)
                                    .putString("selected_groups", groupsCsv)
                                    .putString("age_group", remoteGroups.firstOrNull().orEmpty())
                                    .putString("group", remoteGroups.firstOrNull().orEmpty())
                                    .putString("active_group", remoteActiveGroup)
                                    .apply()

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

                val selectedBranches: List<String> = remember(userSp, branchesRefreshTick) {
                    readSelectedBranches(userSp)
                }

                val branchTypeHome = remember(userSp, branchesRefreshTick) {
                    userSp.getString("branch_type", "israel") ?: "israel"
                }

                val isAbroadBranch = branchTypeHome == "abroad"

                val branchesEffective = remember(selectedBranches, isAbroadBranch) {
                    selectedBranches
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .distinct()
                }

                // ✅ name להצגה + פרמטרים לניווט אימונים חופשיים (נעדכן state כדי שה-FAB יוכל להשתמש גם מחוץ ל-Column)
                val freeName = remember(userSp) {
                    userSp.getString("full_name", null)
                        ?: userSp.getString("name", null)
                        ?: userSp.getString("user_name", null)
                        ?: ""
                }.orEmpty()

                LaunchedEffect(branchesEffective, groupsEffective, currentUid, freeName) {
                    freeBranchUi = branchesEffective.firstOrNull().orEmpty()
                    freeGroupKeyUi = groupsEffective.firstOrNull().orEmpty()
                    freeUidUi = currentUid.orEmpty()
                    freeNameUi = freeName
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
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF062B4A).copy(alpha = 0.92f),
                                    Color(0xFF0F5E9C).copy(alpha = 0.86f),
                                    Color(0xFF062B4A).copy(alpha = 0.92f)
                                )
                            )
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
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xCCFFFFFF),
                                    Color(0x66FFFFFF),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Spacer(Modifier.height(4.dp))

                fun datesRange(
                    from: LocalDate,
                    to: LocalDate
                ): Sequence<LocalDate> =
                    generateSequence(from) { it.plusDays(1) }
                        .takeWhile { !it.isAfter(to) }

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
                        delay(30_000L)

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

                val currentWeekCandidates: List<HomeTrainingCandidate> =
                    remember(
                        branchesEffective,
                        groupsEffective,
                        coachFromPrefs,
                        isEnglish,
                        trainingStatusNowMillis
                    ) {
                        val all =
                            mutableListOf<HomeTrainingCandidate>()

                        branchesEffective.forEach { branchName ->
                            val parts = branchName.split('–', '-').map { it.trim() }
                            val city = parts.getOrNull(0) ?: branchName
                            val venue = parts.getOrNull(1) ?: ""

                            groupsEffective.forEach { grp ->

                                // ✅ 1) ניסיון ראשון: branches.json דרך KmiDatabaseProvider
                                val dbItems = trainingsFromDatabaseForHome(
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

                                // ✅ 2) Fallback זמני: TrainingDirectory הישן
                                val addr =
                                    TrainingCatalog.addressFor(branchName)
                                        .ifBlank {
                                            if (city.isNotBlank() && venue.isNotBlank()) "$venue, $city" else branchName
                                        }

                                val place = TrainingCatalog.placeFor(branchName)

                                val branchVariants = branchScheduleVariants(branchName)
                                val groupVariants = groupScheduleVariants(grp)

                                var matchedBranch = ""
                                var matchedGroup = ""

                                val sched = branchVariants
                                    .asSequence()
                                    .flatMap { branchCandidate ->
                                        groupVariants.asSequence().map { groupCandidate ->
                                            branchCandidate to groupCandidate
                                        }
                                    }
                                    .mapNotNull { pair ->
                                        val found =
                                            TrainingDirectory.getSchedule(
                                                pair.first,
                                                pair.second
                                            )

                                        if (found != null) {
                                            matchedBranch = pair.first
                                            matchedGroup = pair.second
                                            found
                                        } else {
                                            null
                                        }
                                    }
                                    .firstOrNull()

                                val coach =
                                    sched?.coachName?.takeIf { it.isNotBlank() }
                                        ?: coachFromPrefs.takeIf { it.isNotBlank() }
                                        ?: ""

                                val fallbackItems: List<HomeTrainingCandidate> =
                                    sched?.slots?.map { slotAny ->
                                        val s = readSlot(slotAny)

                                        HomeTrainingCandidate(
                                            training = TrainingData.nextWeekly(
                                                dayOfWeek = s.dayOfWeek,
                                                startHour = s.startHour,
                                                startMinute = s.startMinute,
                                                durationMinutes = s.durationMinutes,
                                                place = place,
                                                address = addr,
                                                coach = coach
                                            ),
                                            branch =
                                                matchedBranch
                                                    .ifBlank { branchName }
                                                    .trim(),
                                            group =
                                                matchedGroup
                                                    .ifBlank { grp }
                                                    .trim()
                                        )
                                    } ?: emptyList()

                                val validFallbackItems =
                                    fallbackItems.filter { candidate ->
                                        isWithinUpcomingSevenDays(
                                            candidate.training
                                        )
                                    }

                                all += validFallbackItems
                            }
                        }

                        val result =
                            all.distinctBy { candidate ->
                                buildString {
                                    append(
                                        candidate.training
                                            .cal
                                            .timeInMillis
                                    )
                                    append("|")
                                    append(candidate.branch)
                                    append("|")
                                    append(candidate.group)
                                    append("|")
                                    append(
                                        candidate.training
                                            .place
                                            .orEmpty()
                                    )
                                    append("|")
                                    append(
                                        candidate.training
                                            .address
                                            .orEmpty()
                                    )
                                    append("|")
                                    append(
                                        candidate.training
                                            .coach
                                            .orEmpty()
                                    )
                                }
                            }
                                .sortedBy { candidate ->
                                    candidate.training.startMillis
                                }

                        result
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

                var selectedTrainingForManagement by remember {
                    mutableStateOf<HomeTrainingUi?>(null)
                }

                var trainingManagementMode by remember {
                    mutableStateOf(TrainingManagementMode.MENU)
                }

                var trainingChangeReason by rememberSaveable {
                    mutableStateOf("")
                }

                var changedStartTime by rememberSaveable {
                    mutableStateOf("")
                }

                var changedEndTime by rememberSaveable {
                    mutableStateOf("")
                }

                var showStartTimePicker by rememberSaveable {
                    mutableStateOf(false)
                }

                var showEndTimePicker by rememberSaveable {
                    mutableStateOf(false)
                }

                var isSavingTrainingChange by remember {
                    mutableStateOf(false)
                }

                var trainingManagementError by remember {
                    mutableStateOf<String?>(null)
                }
                var activeTrainingOverrides by remember {
                    mutableStateOf<Map<String, TrainingOverride>>(
                        emptyMap()
                    )
                }

                val occurrenceKeys =
                    remember(currentWeekCandidates) {
                        currentWeekCandidates
                            .associate { candidate ->
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

                                occurrenceKey to candidate
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
                            .take(5)
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
                            place = training.place.orEmpty(),
                            address = training.address.orEmpty(),
                            coach = training.coach.orEmpty(),
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
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
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
                                branch = item.branch,
                                group = item.group,
                                isCoach = isCoach,
                                isEnglish = isEnglish,
                                status = item.status,
                                activeOverride =
                                    item.activeOverride,
                                onManageTraining = {
                                    selectedTrainingForManagement = item
                                    trainingManagementMode =
                                        TrainingManagementMode.MENU

                                    trainingChangeReason =
                                        item.activeOverride
                                            ?.reason
                                            .orEmpty()

                                    changedStartTime = ""
                                    changedEndTime = ""
                                    trainingManagementError = null
                                    isSavingTrainingChange = false
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
                                    Brush.verticalGradient(
                                        listOf(
                                            Color(0xCCFFFFFF),
                                            Color(0x66FFFFFF),
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
                            color = Color.White.copy(alpha = 0.95f),
                            shadowElevation = 6.dp,
                            border = BorderStroke(
                                1.dp,
                                Color(0xFF7DD3FC)
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
                                    color = Color(0xFFE0F2FE),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Color(0xFF0369A1),
                                        modifier = Modifier.padding(8.dp)
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
                                            color = Color(0xFF0C4A6E),
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
                                                color = Color(0xFFE0F2FE),
                                                border = BorderStroke(
                                                    1.dp,
                                                    Color(0xFF7DD3FC)
                                                ),
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Box(
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Email,
                                                        contentDescription =
                                                            if (isEnglish) {
                                                                "Messages and events"
                                                            } else {
                                                                "הודעות ואירועים"
                                                            },
                                                        tint = Color(0xFF0369A1),
                                                        modifier = Modifier.size(17.dp)
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
                                            color = Color(0xFF64748B)
                                        )
                                    } else {
                                        Text(
                                            text = msg,
                                            style = KmiTypography.body,
                                            color = Color(0xFF1E293B),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        val branchGroupLine = buildString {
                                            val b =
                                                latestNotice
                                                    ?.branch
                                                    .orEmpty()
                                                    .trim()

                                            val g =
                                                latestNotice
                                                    ?.group
                                                    .orEmpty()
                                                    .trim()

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
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                text = branchGroupLine,
                                                style = KmiTypography.secondary,
                                                color = Color(0xFF475569),
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
                                                    color = Color(0xFF64748B),
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
                                                    color = Color(0xFF0369A1),
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

                selectedTrainingForManagement?.let { selectedItem ->

                    val dialogAccent =
                        when (trainingManagementMode) {
                            TrainingManagementMode.CANCEL ->
                                Color(0xFFB91C1C)

                            TrainingManagementMode.CHANGE_TIME ->
                                Color(0xFF6D4BB6)

                            TrainingManagementMode.MENU ->
                                Color(0xFF075985)
                        }

                    val timeRegex = remember {
                        Regex("""^(?:[01]\d|2[0-3]):[0-5]\d$""")
                    }

                    val isStartTimeValid =
                        changedStartTime.matches(timeRegex)

                    val isEndTimeValid =
                        changedEndTime.matches(timeRegex)

                    val canSubmit =
                        when (trainingManagementMode) {
                            TrainingManagementMode.CANCEL ->
                                trainingChangeReason
                                    .trim()
                                    .length >= 3

                            TrainingManagementMode.CHANGE_TIME ->
                                isStartTimeValid &&
                                        isEndTimeValid &&
                                        trainingChangeReason
                                            .trim()
                                            .length >= 3

                            TrainingManagementMode.MENU ->
                                false
                        }

                    fun closeTrainingManagementDialog() {
                        selectedTrainingForManagement = null
                        trainingManagementMode =
                            TrainingManagementMode.MENU
                        trainingChangeReason = ""
                        changedStartTime = ""
                        changedEndTime = ""
                        trainingManagementError = null
                        isSavingTrainingChange = false
                        showStartTimePicker = false
                        showEndTimePicker = false
                    }

                    if (showStartTimePicker) {
                        TrainingClockPickerDialog(
                            initialTime =
                                changedStartTime.ifBlank {
                                    "19:00"
                                },
                            title =
                                if (isEnglish) {
                                    "Select start time"
                                } else {
                                    "בחירת שעת התחלה"
                                },
                            confirmText =
                                if (isEnglish) {
                                    "Select"
                                } else {
                                    "בחירה"
                                },
                            cancelText =
                                if (isEnglish) {
                                    "Cancel"
                                } else {
                                    "ביטול"
                                },
                            onDismiss = {
                                showStartTimePicker = false
                            },
                            onTimeSelected = { selectedTime ->
                                changedStartTime = selectedTime
                                trainingManagementError = null
                                showStartTimePicker = false
                            }
                        )
                    }

                    if (showEndTimePicker) {
                        TrainingClockPickerDialog(
                            initialTime =
                                changedEndTime.ifBlank {
                                    "20:30"
                                },
                            title =
                                if (isEnglish) {
                                    "Select end time"
                                } else {
                                    "בחירת שעת סיום"
                                },
                            confirmText =
                                if (isEnglish) {
                                    "Select"
                                } else {
                                    "בחירה"
                                },
                            cancelText =
                                if (isEnglish) {
                                    "Cancel"
                                } else {
                                    "ביטול"
                                },
                            onDismiss = {
                                showEndTimePicker = false
                            },
                            onTimeSelected = { selectedTime ->
                                changedEndTime = selectedTime
                                trainingManagementError = null
                                showEndTimePicker = false
                            }
                        )
                    }

                    AlertDialog(
                        onDismissRequest = {
                            if (!isSavingTrainingChange) {
                                closeTrainingManagementDialog()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(30.dp),
                        containerColor = Color(0xFFF8FAFC),
                        tonalElevation = 12.dp,

                        title = {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        top = 0.dp,
                                        bottom = 2.dp
                                    ),
                                horizontalArrangement =
                                    Arrangement.Center,
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {
                                Surface(
                                    modifier = Modifier.size(42.dp),
                                    shape = CircleShape,
                                    color =
                                        dialogAccent.copy(
                                            alpha = 0.10f
                                        ),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color =
                                            dialogAccent.copy(
                                                alpha = 0.22f
                                            )
                                    ),
                                    tonalElevation = 0.dp,
                                    shadowElevation = 0.dp
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment =
                                            Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector =
                                                when (
                                                    trainingManagementMode
                                                ) {
                                                    TrainingManagementMode.CANCEL ->
                                                        Icons.Filled.Cancel

                                                    TrainingManagementMode.CHANGE_TIME ->
                                                        Icons.Filled.Schedule

                                                    TrainingManagementMode.MENU ->
                                                        Icons.Filled.EditNote
                                                },
                                            contentDescription = null,
                                            tint = dialogAccent,
                                            modifier =
                                                Modifier.size(22.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.width(10.dp))

                                Text(
                                    text =
                                        when (trainingManagementMode) {
                                            TrainingManagementMode.MENU ->
                                                if (isEnglish) {
                                                    "Manage training"
                                                } else {
                                                    "ניהול אימון"
                                                }

                                            TrainingManagementMode.CANCEL ->
                                                if (isEnglish) {
                                                    "Cancel training"
                                                } else {
                                                    "ביטול אימון"
                                                }

                                            TrainingManagementMode.CHANGE_TIME ->
                                                if (isEnglish) {
                                                    "Change training time"
                                                } else {
                                                    "שינוי שעת האימון"
                                                }
                                        },
                                    style = KmiTypography.screenTitle,
                                    color = Color(0xFF172033),
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        },

                        text = {
                            val managementScrollState =
                                rememberScrollState()

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 500.dp)
                                    .verticalScroll(
                                        managementScrollState
                                    )
                                    .padding(
                                        top = 2.dp,
                                        bottom = 8.dp
                                    ),
                                verticalArrangement =
                                    Arrangement.spacedBy(12.dp)
                            ) {

                                /*
                                 * כרטיס פרטי האימון.
                                 */
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color.White,
                                    shadowElevation = 4.dp,
                                    border = BorderStroke(
                                        1.dp,
                                        Color(0xFFDCE6F2)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(
                                            horizontal = 13.dp,
                                            vertical = 9.dp
                                        ),
                                        verticalArrangement =
                                            Arrangement.spacedBy(3.dp)
                                    ) {
                                        Text(
                                            text =
                                                selectedItem.training
                                                    .place
                                                    .orEmpty()
                                                    .ifBlank {
                                                        selectedItem.branch
                                                    },
                                            style = KmiTypography.cardTitle,
                                            color = Color(0xFF1E293B),
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

                                        Text(
                                            text =
                                                if (isEnglish) {
                                                    "Branch: ${selectedItem.branch}"
                                                } else {
                                                    "סניף: ${selectedItem.branch}"
                                                },
                                            fontWeight =
                                                FontWeight.SemiBold,
                                            color = Color(0xFF475569),
                                            modifier =
                                                Modifier.fillMaxWidth(),
                                            textAlign =
                                                if (isEnglish) {
                                                    TextAlign.Left
                                                } else {
                                                    TextAlign.Right
                                                }
                                        )

                                        Text(
                                            text =
                                                if (isEnglish) {
                                                    "Group: ${selectedItem.group}"
                                                } else {
                                                    "קבוצה: ${selectedItem.group}"
                                                },
                                            fontWeight =
                                                FontWeight.SemiBold,
                                            color = Color(0xFF475569),
                                            modifier =
                                                Modifier.fillMaxWidth(),
                                            textAlign =
                                                if (isEnglish) {
                                                    TextAlign.Left
                                                } else {
                                                    TextAlign.Right
                                                }
                                        )

                                        val currentTimeText =
                                            buildString {
                                                append(
                                                    selectedItem.training
                                                        .start
                                                        .trim()
                                                )

                                                val end =
                                                    selectedItem.training
                                                        .end
                                                        .trim()

                                                if (end.isNotBlank()) {
                                                    append(" – ")
                                                    append(end)
                                                }
                                            }

                                        if (currentTimeText.isNotBlank()) {
                                            Text(
                                                text =
                                                    if (isEnglish) {
                                                        "Current time: $currentTimeText"
                                                    } else {
                                                        "שעה נוכחית: $currentTimeText"
                                                    },
                                                fontWeight =
                                                    FontWeight.Bold,
                                                color = dialogAccent,
                                                modifier =
                                                    Modifier.fillMaxWidth(),
                                                textAlign =
                                                    if (isEnglish) {
                                                        TextAlign.Left
                                                    } else {
                                                        TextAlign.Right
                                                    }
                                            )
                                        }
                                    }
                                }

                                when (trainingManagementMode) {

                                    TrainingManagementMode.MENU -> {
                                        Text(
                                            text =
                                                if (isEnglish) {
                                                    "Choose the action you want to perform"
                                                } else {
                                                    "בחר את הפעולה שברצונך לבצע"
                                                },
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF475569),
                                            textAlign = TextAlign.Center,
                                            modifier =
                                                Modifier.fillMaxWidth()
                                        )

                                        Surface(
                                            onClick = {
                                                clickSound()
                                                haptic(true)

                                                trainingManagementMode =
                                                    TrainingManagementMode.CHANGE_TIME

                                                changedStartTime =
                                                    Regex(
                                                        """(?:[01]\d|2[0-3]):[0-5]\d"""
                                                    )
                                                        .find(
                                                            selectedItem.training.start
                                                        )
                                                        ?.value
                                                        .orEmpty()

                                                changedEndTime =
                                                    Regex(
                                                        """(?:[01]\d|2[0-3]):[0-5]\d"""
                                                    )
                                                        .find(
                                                            selectedItem.training.end
                                                        )
                                                        ?.value
                                                        .orEmpty()

                                                trainingManagementError =
                                                    null
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(18.dp),
                                            color = Color(0xFFF1ECFF),
                                            border = BorderStroke(
                                                1.dp,
                                                Color(0xFFB9A4E8)
                                            ),
                                            shadowElevation = 2.dp
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(
                                                    horizontal = 14.dp,
                                                    vertical = 11.dp
                                                ),
                                                verticalAlignment =
                                                    Alignment.CenterVertically,
                                                horizontalArrangement =
                                                    Arrangement.spacedBy(12.dp)
                                            ) {
                                                Surface(
                                                    modifier =
                                                        Modifier.size(38.dp),
                                                    shape = CircleShape,
                                                    color =
                                                        Color(0xFF6D4BB6)
                                                ) {
                                                    Box(
                                                        contentAlignment =
                                                            Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector =
                                                                Icons.Filled.Schedule,
                                                            contentDescription = null,
                                                            tint = Color.White,
                                                            modifier =
                                                                Modifier.size(21.dp)
                                                        )
                                                    }
                                                }

                                                Column(
                                                    modifier =
                                                        Modifier.weight(1f)
                                                ) {
                                                    Text(
                                                        text =
                                                            if (isEnglish) {
                                                                "Change training time"
                                                            } else {
                                                                "שינוי שעת אימון"
                                                            },
                                                        style =
                                                            KmiTypography.sectionTitle,
                                                        color =
                                                            Color(0xFF5B3AA8)
                                                    )

                                                    Text(
                                                        text =
                                                            if (isEnglish) {
                                                                "Choose a new start and end time"
                                                            } else {
                                                                "בחירת שעת התחלה וסיום חדשות"
                                                            },
                                                        style =
                                                            KmiTypography.secondary,
                                                        color =
                                                            Color(0xFF655A78)
                                                    )
                                                }
                                            }
                                        }

                                        Surface(
                                            onClick = {
                                                clickSound()
                                                haptic(true)
                                                trainingManagementMode =
                                                    TrainingManagementMode.CANCEL
                                                trainingManagementError =
                                                    null
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(18.dp),
                                            color = Color(0xFFFFEEEE),
                                            border = BorderStroke(
                                                1.dp,
                                                Color(0xFFF2AAAA)
                                            ),
                                            shadowElevation = 2.dp
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(
                                                    horizontal = 14.dp,
                                                    vertical = 11.dp
                                                ),
                                                verticalAlignment =
                                                    Alignment.CenterVertically,
                                                horizontalArrangement =
                                                    Arrangement.spacedBy(12.dp)
                                            ) {
                                                Surface(
                                                    modifier =
                                                        Modifier.size(38.dp),
                                                    shape = CircleShape,
                                                    color =
                                                        Color(0xFFB91C1C)
                                                ) {
                                                    Box(
                                                        contentAlignment =
                                                            Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector =
                                                                Icons.Filled.Cancel,
                                                            contentDescription = null,
                                                            tint = Color.White,
                                                            modifier =
                                                                Modifier.size(21.dp)
                                                        )
                                                    }
                                                }

                                                Column(
                                                    modifier =
                                                        Modifier.weight(1f)
                                                ) {
                                                    Text(
                                                        text =
                                                            if (isEnglish) {
                                                                "Cancel training"
                                                            } else {
                                                                "ביטול אימון"
                                                            },
                                                        style =
                                                            KmiTypography.sectionTitle,
                                                        color =
                                                            Color(0xFFB91C1C)
                                                    )

                                                    Text(
                                                        text =
                                                            if (isEnglish) {
                                                                "Cancel this training and notify trainees"
                                                            } else {
                                                                "ביטול האימון ושליחת עדכון למתאמנים"
                                                            },
                                                        style =
                                                            KmiTypography.secondary,
                                                        color =
                                                            Color(0xFF7F1D1D)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    TrainingManagementMode.CANCEL -> {
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(16.dp),
                                            color = Color(0xFFFFF1F2),
                                            border = BorderStroke(
                                                1.dp,
                                                Color(0xFFFDA4AF)
                                            )
                                        ) {
                                            Text(
                                                text =
                                                    if (isEnglish) {
                                                        "The training will be marked as cancelled for all trainees."
                                                    } else {
                                                        "האימון יסומן כמבוטל עבור כל המתאמנים."
                                                    },
                                                color = Color(0xFF9F1239),
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(
                                                    horizontal = 12.dp,
                                                    vertical = 10.dp
                                                )
                                            )
                                        }

                                        OutlinedTextField(
                                            value = trainingChangeReason,
                                            onValueChange = {
                                                if (it.length <= 250) {
                                                    trainingChangeReason = it
                                                    trainingManagementError =
                                                        null
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            label = {
                                                Text(
                                                    if (isEnglish) {
                                                        "Cancellation reason"
                                                    } else {
                                                        "סיבת ביטול האימון"
                                                    }
                                                )
                                            },
                                            placeholder = {
                                                Text(
                                                    if (isEnglish) {
                                                        "For example: the coach is unavailable"
                                                    } else {
                                                        "לדוגמה: המאמן אינו זמין"
                                                    }
                                                )
                                            },
                                            minLines = 3,
                                            maxLines = 5,
                                            supportingText = {
                                                Text(
                                                    "${trainingChangeReason.length}/250"
                                                )
                                            },
                                            shape = RoundedCornerShape(16.dp),
                                            colors =
                                                OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor =
                                                        Color(0xFFB91C1C),
                                                    focusedLabelColor =
                                                        Color(0xFFB91C1C),
                                                    cursorColor =
                                                        Color(0xFFB91C1C),
                                                    focusedContainerColor =
                                                        Color.White,
                                                    unfocusedContainerColor =
                                                        Color.White
                                                )
                                        )
                                    }

                                    TrainingManagementMode.CHANGE_TIME -> {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement =
                                                Arrangement.spacedBy(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                OutlinedTextField(
                                                    value = changedStartTime,
                                                    onValueChange = {},
                                                    modifier = Modifier.fillMaxWidth(),
                                                    readOnly = true,
                                                    label = {
                                                        Text(
                                                            if (isEnglish) {
                                                                "Start"
                                                            } else {
                                                                "התחלה"
                                                            }
                                                        )
                                                    },
                                                    placeholder = {
                                                        Text("19:00")
                                                    },
                                                    trailingIcon = {
                                                        Icon(
                                                            imageVector =
                                                                Icons.Filled.Schedule,
                                                            contentDescription = null,
                                                            tint = Color(0xFF6D4BB6)
                                                        )
                                                    },
                                                    singleLine = true,
                                                    textStyle =
                                                        MaterialTheme.typography.bodyLarge.copy(
                                                            textAlign = TextAlign.Center,
                                                            fontWeight = FontWeight.Bold
                                                        ),
                                                    shape = RoundedCornerShape(15.dp)
                                                )

                                                Box(
                                                    modifier = Modifier
                                                        .matchParentSize()
                                                        .clickable {
                                                            clickSound()
                                                            haptic(true)
                                                            showStartTimePicker = true
                                                        }
                                                )
                                            }

                                            Box(
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                OutlinedTextField(
                                                    value = changedEndTime,
                                                    onValueChange = {},
                                                    modifier = Modifier.fillMaxWidth(),
                                                    readOnly = true,
                                                    label = {
                                                        Text(
                                                            if (isEnglish) {
                                                                "End"
                                                            } else {
                                                                "סיום"
                                                            }
                                                        )
                                                    },
                                                    placeholder = {
                                                        Text("20:30")
                                                    },
                                                    trailingIcon = {
                                                        Icon(
                                                            imageVector =
                                                                Icons.Filled.Schedule,
                                                            contentDescription = null,
                                                            tint = Color(0xFF6D4BB6)
                                                        )
                                                    },
                                                    singleLine = true,
                                                    textStyle =
                                                        MaterialTheme.typography.bodyLarge.copy(
                                                            textAlign = TextAlign.Center,
                                                            fontWeight = FontWeight.Bold
                                                        ),
                                                    shape = RoundedCornerShape(15.dp)
                                                )

                                                Box(
                                                    modifier = Modifier
                                                        .matchParentSize()
                                                        .clickable {
                                                            clickSound()
                                                            haptic(true)
                                                            showEndTimePicker = true
                                                        }
                                                )
                                            }
                                        }

                                        Text(
                                            text =
                                                if (isEnglish) {
                                                    "Tap a field to select the hour and minutes"
                                                } else {
                                                    "לחץ על שדה כדי לבחור שעה ודקות"
                                                },
                                            style =
                                                MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF64748B),
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Center
                                        )

                                        OutlinedTextField(
                                            value = trainingChangeReason,
                                            onValueChange = {
                                                if (it.length <= 250) {
                                                    trainingChangeReason = it
                                                    trainingManagementError =
                                                        null
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            label = {
                                                Text(
                                                    if (isEnglish) {
                                                        "Reason for the change"
                                                    } else {
                                                        "סיבת שינוי שעת האימון"
                                                    }
                                                )
                                            },
                                            placeholder = {
                                                Text(
                                                    if (isEnglish) {
                                                        "Write a message for the trainees"
                                                    } else {
                                                        "כתוב הודעה שתוצג למתאמנים"
                                                    }
                                                )
                                            },
                                            minLines = 3,
                                            maxLines = 5,
                                            supportingText = {
                                                Text(
                                                    "${trainingChangeReason.length}/250"
                                                )
                                            },
                                            shape = RoundedCornerShape(16.dp),
                                            colors =
                                                OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor =
                                                        Color(0xFF6D4BB6),
                                                    focusedLabelColor =
                                                        Color(0xFF6D4BB6),
                                                    cursorColor =
                                                        Color(0xFF6D4BB6),
                                                    focusedContainerColor =
                                                        Color.White,
                                                    unfocusedContainerColor =
                                                        Color.White
                                                )
                                        )
                                    }
                                }

                                trainingManagementError?.let { error ->
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(13.dp),
                                        color = Color(0xFFFFE4E6)
                                    ) {
                                        Text(
                                            text = error,
                                            color = Color(0xFFBE123C),
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                }
                            }
                        },

                        confirmButton = {
                            when (trainingManagementMode) {
                                TrainingManagementMode.MENU -> {
                                    TextButton(
                                        onClick = {
                                            closeTrainingManagementDialog()
                                        }
                                    ) {
                                        Text(
                                            text =
                                                if (isEnglish) {
                                                    "Close"
                                                } else {
                                                    "סגור"
                                                },
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                TrainingManagementMode.CANCEL,
                                TrainingManagementMode.CHANGE_TIME -> {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 2.dp),
                                        horizontalArrangement =
                                            Arrangement.spacedBy(8.dp),
                                        verticalAlignment =
                                            Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            modifier = Modifier
                                                .weight(0.34f)
                                                .height(40.dp),
                                            shape = RoundedCornerShape(13.dp),
                                            color =
                                                dialogAccent.copy(
                                                    alpha = 0.08f
                                                ),
                                            border = BorderStroke(
                                                width = 1.dp,
                                                color =
                                                    dialogAccent.copy(
                                                        alpha = 0.24f
                                                    )
                                            ),
                                            tonalElevation = 0.dp,
                                            shadowElevation = 0.dp
                                        ) {
                                            TextButton(
                                                enabled =
                                                    !isSavingTrainingChange,
                                                onClick = {
                                                    clickSound()
                                                    haptic(true)

                                                    trainingManagementMode =
                                                        TrainingManagementMode.MENU

                                                    trainingManagementError =
                                                        null
                                                },
                                                modifier =
                                                    Modifier.fillMaxSize(),
                                                contentPadding =
                                                    PaddingValues(
                                                        horizontal = 6.dp,
                                                        vertical = 0.dp
                                                    ),
                                                colors =
                                                    ButtonDefaults.textButtonColors(
                                                        contentColor =
                                                            dialogAccent,
                                                        disabledContentColor =
                                                            dialogAccent.copy(
                                                                alpha = 0.35f
                                                            )
                                                    )
                                            ) {
                                                Icon(
                                                    imageVector =
                                                        Icons.Filled.ArrowBack,
                                                    contentDescription = null,
                                                    modifier =
                                                        Modifier.size(14.dp)
                                                )

                                                Spacer(
                                                    Modifier.width(3.dp)
                                                )

                                                Text(
                                                    text =
                                                        if (isEnglish) {
                                                            "Back"
                                                        } else {
                                                            "חזרה"
                                                        },
                                                    fontWeight =
                                                        FontWeight.ExtraBold,
                                                    fontSize = 13.sp,
                                                    lineHeight = 15.sp,
                                                    maxLines = 1
                                                )
                                            }
                                        }

                                        Button(
                                            onClick = {
                                                clickSound()
                                                haptic(true)

                                                if (isSavingTrainingChange) {
                                                    return@Button
                                                }

                                                val cleanReason =
                                                    trainingChangeReason.trim()

                                                if (cleanReason.length < 3) {
                                                    trainingManagementError =
                                                        if (isEnglish) {
                                                            "Please enter a reason of at least 3 characters."
                                                        } else {
                                                            "יש להזין סיבה באורך של 3 תווים לפחות."
                                                        }

                                                    return@Button
                                                }

                                                val changedByName =
                                                    coachFromPrefs
                                                        .trim()
                                                        .ifBlank {
                                                            freeNameUi.trim()
                                                        }
                                                        .ifBlank {
                                                            FirebaseAuth.getInstance()
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

                                                isSavingTrainingChange = true
                                                trainingManagementError = null

                                                when (trainingManagementMode) {
                                                    TrainingManagementMode.CANCEL -> {
                                                        TrainingOverrideRepository
                                                            .cancelTraining(
                                                                training =
                                                                    selectedItem.training,
                                                                branch =
                                                                    selectedItem.branch,
                                                                group =
                                                                    selectedItem.group,
                                                                reason =
                                                                    cleanReason,
                                                                changedByName =
                                                                    changedByName,
                                                                onResult = {
                                                                        success,
                                                                        error ->

                                                                    isSavingTrainingChange =
                                                                        false

                                                                    if (success) {
                                                                        closeTrainingManagementDialog()
                                                                    } else {
                                                                        trainingManagementError =
                                                                            if (
                                                                                isEnglish
                                                                            ) {
                                                                                error
                                                                                    ?.localizedMessage
                                                                                    ?.takeIf {
                                                                                        it.isNotBlank()
                                                                                    }
                                                                                    ?: "The training could not be cancelled."
                                                                            } else {
                                                                                error
                                                                                    ?.localizedMessage
                                                                                    ?.takeIf {
                                                                                        it.isNotBlank()
                                                                                    }
                                                                                    ?: "לא ניתן היה לבטל את האימון."
                                                                            }
                                                                    }
                                                                }
                                                            )
                                                    }

                                                    TrainingManagementMode.CHANGE_TIME -> {
                                                        fun parseTime(
                                                            rawValue: String
                                                        ): Pair<Int, Int>? {
                                                            val parts =
                                                                rawValue
                                                                    .trim()
                                                                    .split(":")

                                                            if (parts.size != 2) {
                                                                return null
                                                            }

                                                            val hour =
                                                                parts[0]
                                                                    .toIntOrNull()
                                                                    ?: return null

                                                            val minute =
                                                                parts[1]
                                                                    .toIntOrNull()
                                                                    ?: return null

                                                            if (
                                                                hour !in 0..23 ||
                                                                minute !in 0..59
                                                            ) {
                                                                return null
                                                            }

                                                            return hour to minute
                                                        }

                                                        val parsedStart =
                                                            parseTime(
                                                                changedStartTime
                                                            )

                                                        val parsedEnd =
                                                            parseTime(
                                                                changedEndTime
                                                            )

                                                        if (
                                                            parsedStart == null ||
                                                            parsedEnd == null
                                                        ) {
                                                            isSavingTrainingChange =
                                                                false

                                                            trainingManagementError =
                                                                if (isEnglish) {
                                                                    "Enter valid times in HH:mm format."
                                                                } else {
                                                                    "יש להזין שעות תקינות בפורמט HH:mm."
                                                                }

                                                            return@Button
                                                        }

                                                        val newStartCalendar =
                                                            (
                                                                    selectedItem
                                                                        .training
                                                                        .cal
                                                                        .clone()
                                                                            as Calendar
                                                                    ).apply {
                                                                    set(
                                                                        Calendar.HOUR_OF_DAY,
                                                                        parsedStart.first
                                                                    )
                                                                    set(
                                                                        Calendar.MINUTE,
                                                                        parsedStart.second
                                                                    )
                                                                    set(
                                                                        Calendar.SECOND,
                                                                        0
                                                                    )
                                                                    set(
                                                                        Calendar.MILLISECOND,
                                                                        0
                                                                    )
                                                                }

                                                        val newEndCalendar =
                                                            (
                                                                    selectedItem
                                                                        .training
                                                                        .cal
                                                                        .clone()
                                                                            as Calendar
                                                                    ).apply {
                                                                    set(
                                                                        Calendar.HOUR_OF_DAY,
                                                                        parsedEnd.first
                                                                    )
                                                                    set(
                                                                        Calendar.MINUTE,
                                                                        parsedEnd.second
                                                                    )
                                                                    set(
                                                                        Calendar.SECOND,
                                                                        0
                                                                    )
                                                                    set(
                                                                        Calendar.MILLISECOND,
                                                                        0
                                                                    )
                                                                }

                                                        if (
                                                            newEndCalendar
                                                                .timeInMillis <=
                                                            newStartCalendar
                                                                .timeInMillis
                                                        ) {
                                                            newEndCalendar.add(
                                                                Calendar.DAY_OF_YEAR,
                                                                1
                                                            )
                                                        }

                                                        TrainingOverrideRepository
                                                            .changeTrainingTime(
                                                                training =
                                                                    selectedItem.training,
                                                                branch =
                                                                    selectedItem.branch,
                                                                group =
                                                                    selectedItem.group,
                                                                newStartMillis =
                                                                    newStartCalendar
                                                                        .timeInMillis,
                                                                newEndMillis =
                                                                    newEndCalendar
                                                                        .timeInMillis,
                                                                reason =
                                                                    cleanReason,
                                                                changedByName =
                                                                    changedByName,
                                                                onResult = {
                                                                        success,
                                                                        error ->

                                                                    isSavingTrainingChange =
                                                                        false

                                                                    if (success) {
                                                                        closeTrainingManagementDialog()
                                                                    } else {
                                                                        trainingManagementError =
                                                                            if (
                                                                                isEnglish
                                                                            ) {
                                                                                error
                                                                                    ?.localizedMessage
                                                                                    ?.takeIf {
                                                                                        it.isNotBlank()
                                                                                    }
                                                                                    ?: "The new training time could not be saved."
                                                                            } else {
                                                                                error
                                                                                    ?.localizedMessage
                                                                                    ?.takeIf {
                                                                                        it.isNotBlank()
                                                                                    }
                                                                                    ?: "לא ניתן היה לשמור את שעת האימון החדשה."
                                                                            }
                                                                    }
                                                                }
                                                            )
                                                    }

                                                    TrainingManagementMode.MENU -> {
                                                        isSavingTrainingChange =
                                                            false
                                                    }
                                                }
                                            },
                                            enabled =
                                                canSubmit &&
                                                        !isSavingTrainingChange,
                                            modifier = Modifier
                                                .weight(0.66f)
                                                .height(40.dp),
                                            shape = RoundedCornerShape(13.dp),
                                            contentPadding =
                                                PaddingValues(
                                                    horizontal = 8.dp,
                                                    vertical = 0.dp
                                                ),
                                            colors =
                                                ButtonDefaults.buttonColors(
                                                    containerColor =
                                                        dialogAccent,
                                                    disabledContainerColor =
                                                        dialogAccent.copy(
                                                            alpha = 0.35f
                                                        )
                                                )
                                        ) {
                                            if (isSavingTrainingChange) {
                                                CircularProgressIndicator(
                                                    modifier =
                                                        Modifier.size(15.dp),
                                                    strokeWidth = 2.dp,
                                                    color = Color.White
                                                )

                                                Spacer(
                                                    Modifier.width(5.dp)
                                                )
                                            }

                                            Text(
                                                text =
                                                    when (
                                                        trainingManagementMode
                                                    ) {
                                                        TrainingManagementMode.CANCEL ->
                                                            if (isEnglish) {
                                                                "Confirm cancellation"
                                                            } else {
                                                                "אישור ביטול"
                                                            }

                                                        TrainingManagementMode.CHANGE_TIME ->
                                                            if (isEnglish) {
                                                                "Save new time"
                                                            } else {
                                                                "שמירת שעה"
                                                            }

                                                        else -> ""
                                                    },
                                                fontWeight =
                                                    FontWeight.ExtraBold,
                                                fontSize = 13.sp,
                                                lineHeight = 15.sp,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        },

                        dismissButton = {}
                    )
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
                        shadowElevation = 5.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .shadow(12.dp, RoundedCornerShape(18.dp))
                            .graphicsLayer {
                                scaleX = 1.02f
                                scaleY = 1.02f
                            }
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.85f),
                                        Color.White.copy(alpha = 0.25f),
                                        Color.White.copy(alpha = 0.85f)
                                    )
                                ),
                                shape = RoundedCornerShape(18.dp)
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF7F00FF),
                                            Color(0xFF3F51B5),
                                            Color(0xFF03A9F4)

                                        )
                                    )
                                )
                        ) {

                            Box(
                                modifier = Modifier
                                    .offset(x = bubbleOffset.dp)
                                    .size(140.dp)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(
                                                Color.White.copy(alpha = 0.45f),
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
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )

                                    Spacer(Modifier.width(8.dp))

                                    Text(
                                        text = if (isEnglish) "Go to Belt Selection" else "מעבר לבחירת חגורה",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(2.dp))

                if (showTrainingArchive) {
                    TrainingArchiveDialog(
                        baseTrainings =
                            currentWeekCandidates.map { candidate ->
                                TrainingArchiveSource(
                                    training = candidate.training,
                                    branch = candidate.branch,
                                    group = candidate.group
                                )
                            },
                        isEnglish = isEnglish,
                        onDismiss = {
                            showTrainingArchive = false
                        },
                        onOpenDrawer = {
                            showTrainingArchive = false
                            onOpenDrawer()
                        },
                        onSettings = {
                            showTrainingArchive = false
                            onSettings()
                        },
                        onOpenExercise = { key ->
                            showTrainingArchive = false
                            onOpenExercise(key)
                        },
                        onOpenAi = {
                            showTrainingArchive = false
                            showAiDialog = true
                        }
                    )
                }
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
                        showTrainingArchive = true
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
                    // ✅ כמו מסך החגורות: התפריט נפתח מהצד, ליד הטאב
                    .align(Alignment.CenterStart)
                    .offset(x = 46.dp, y = 88.dp),
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
                    title = if (isEnglish) "Quick Menu" else "תפריט מהיר",
                    isEnglish = isEnglish,
                    items = quickMenuItems,
                    onClose = { fabExpanded = false }
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
                    onClick = {
                        clickSound()
                        haptic(true)
                        fabExpanded = true
                    }
                )
            }

            if (showCoachMessagesDialog) {
                AlertDialog(
                    onDismissRequest = {
                        showCoachMessagesDialog = false
                    },
                    shape = RoundedCornerShape(30.dp),
                    containerColor = Color(0xFFF4F1FB),
                    tonalElevation = 10.dp,
                    title = {
                        Text(
                            text =
                                if (isEnglish) {
                                    "Recent messages and events"
                                } else {
                                    "הודעות ואירועים אחרונים"
                                },
                            style = KmiTypography.sectionTitle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = Color(0xFF0F172A),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
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
                                        color = Color.White.copy(alpha = 0.94f),
                                        shadowElevation = 6.dp,
                                        border = BorderStroke(
                                            1.dp,
                                            Color(0xFFE2E8F0)
                                        )
                                    ) {
                                        Text(
                                            text = if (isEnglish) {
                                                "No messages right now."
                                            } else {
                                                "אין הודעות כרגע."
                                            },
                                            color = Color(0xFF64748B),
                                            fontWeight = FontWeight.SemiBold,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 18.dp)
                                        )
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
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .shadow(
                                                elevation = 10.dp,
                                                shape = RoundedCornerShape(22.dp),
                                                clip = false
                                            ),
                                        shape = RoundedCornerShape(22.dp),
                                        color = Color.Transparent,
                                        border = BorderStroke(
                                            1.dp,
                                            Color(0xFFD6E4F0)
                                        )
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    Brush.linearGradient(
                                                        colors = listOf(
                                                            Color(0xFFF8FCFF),
                                                            Color(0xFFF1F7FB)
                                                        )
                                                    )
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
                                                                    Color(0xFF38BDF8),
                                                                    Color(0xFF7C3AED)
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
                                                                color = Color(0xFFE0F2FE),
                                                                border = BorderStroke(
                                                                    1.dp,
                                                                    Color(0xFFBAE6FD)
                                                                )
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Filled.Person,
                                                                    contentDescription = null,
                                                                    tint = Color(0xFF0369A1),
                                                                    modifier = Modifier
                                                                        .size(30.dp)
                                                                        .padding(6.dp)
                                                                )
                                                            }

                                                            Spacer(Modifier.width(8.dp))
                                                        }

                                                        Text(
                                                            text = message.coachName.ifBlank {
                                                                if (isEnglish) {
                                                                    "Coach"
                                                                } else {
                                                                    "המאמן"
                                                                }
                                                            },
                                                            style = KmiTypography.cardTitle,
                                                            color = Color(0xFF0B5E8E),
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
                                                                color = Color(0xFFE0F2FE),
                                                                border = BorderStroke(
                                                                    1.dp,
                                                                    Color(0xFFBAE6FD)
                                                                )
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Filled.Person,
                                                                    contentDescription = null,
                                                                    tint = Color(0xFF0369A1),
                                                                    modifier = Modifier
                                                                        .size(30.dp)
                                                                        .padding(6.dp)
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
                                                        color = Color(0xFF1E293B),
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
                                                            shape = RoundedCornerShape(999.dp),
                                                            color = Color(0xFFEFF6FF),
                                                            border = BorderStroke(
                                                                1.dp,
                                                                Color(0xFFBFDBFE)
                                                            )
                                                        ) {
                                                            Text(
                                                                text = branchGroupLine,
                                                                style =
                                                                    KmiTypography.secondary.copy(
                                                                        fontWeight =
                                                                            FontWeight.SemiBold
                                                                    ),
                                                                color = Color(0xFF475569),
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
                                                                shape = RoundedCornerShape(999.dp),
                                                                color = Color(0xFFF1F5F9)
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
                                                                        tint = Color(0xFF64748B),
                                                                        modifier = Modifier.size(12.dp)
                                                                    )

                                                                    Text(
                                                                        text = timeText,
                                                                        style =
                                                                            KmiTypography.caption,
                                                                        color =
                                                                            Color(0xFF64748B),
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
                        TextButton(
                            onClick = {
                                showCoachMessagesDialog = false
                            }
                        ) {
                            Text(
                                text = if (isEnglish) "Close" else "סגור",
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF5B21B6)
                            )
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
                            Color.White,
                            lerp(Color.White, belt.color, 0.12f),
                            lerp(Color.White, belt.color, 0.06f),
                            Color.White
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
            onDismiss = { showAiDialog = false },
            onOpenDrawer = {
                onOpenDrawer()
            }
        )
    }
}

@Composable
private fun ModernHomeQuickFab(
    isEnglish: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    // ✅ הטאב יושב בצד שמאל כמו במסך החגורות:
    // צד שמאל ישר, צד ימין מעוגל.
    val tabShape = RoundedCornerShape(
        topStart = 0.dp,
        bottomStart = 0.dp,
        topEnd = 18.dp,
        bottomEnd = 18.dp
    )

    Box(
        modifier = modifier
            .width(38.dp)
            .height(72.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFFFE7A3),
                            Color(0xFFFFC247),
                            Color(0xFFFFA928)
                        )
                    ),
                    shape = tabShape
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.72f),
                    shape = tabShape
                )
                .shadow(
                    elevation = 7.dp,
                    shape = tabShape,
                    clip = false
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Menu,
                contentDescription = if (isEnglish) "Quick menu" else "תפריט מהיר",
                tint = Color.White,
                modifier = Modifier.size(21.dp)
            )
        }
    }
}


/* ========= עזר: למצוא הסבר אמיתי מתוך Explanations ========= */
@Composable
private fun HomePremiumQuickMenuPanel(
    title: String,
    isEnglish: Boolean,
    items: List<Triple<String, ImageVector, () -> Unit>>,
    onClose: () -> Unit
) {
    val panelHeight = 214.dp
    val panelShape = RoundedCornerShape(20.dp)

    Surface(
        shape = panelShape,
        color = Color.White.copy(alpha = 0.98f),
        tonalElevation = 0.dp,
        shadowElevation = 14.dp,
        border = BorderStroke(
            width = 1.dp,
            color = Color(0xFF16A34A).copy(alpha = 0.58f)
        ),
        modifier = Modifier
            .width(190.dp)
            .height(panelHeight)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(panelShape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.98f),
                            Color(0xFFF9FFFB),
                            Color(0xFF16A34A).copy(alpha = 0.12f),
                            Color(0xFFFBFFFC),
                            Color.White.copy(alpha = 0.98f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = Color(0xFF16A34A).copy(alpha = 0.34f),
                    shape = panelShape
                )
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isEnglish) {
                        Text(
                            text = title,
                            color = Color(0xFF16A34A),
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
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF16A34A),
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { onClose() }
                        )
                    } else {
                        Text(
                            text = title,
                            color = Color(0xFF16A34A),
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Right,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontSize = 13.sp,
                                lineHeight = 15.sp
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(Modifier.width(6.dp))

                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "סגור",
                            tint = Color(0xFF16A34A),
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { onClose() }
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                items.forEachIndexed { index, item ->
                    HomePremiumQuickMenuRow(
                        text = item.first,
                        icon = item.second,
                        isEnglish = isEnglish,
                        onClick = item.third
                    )

                    if (index != items.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            thickness = 1.25.dp,
                            color = Color(0xFF0F8A3D).copy(alpha = 0.62f)
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
    onClick: () -> Unit
) {
    val isLocked = text.endsWith(" 🔒")
    val cleanText = if (isLocked) text.removeSuffix(" 🔒") else text

    val lockPulse = rememberInfiniteTransition(label = "homeQuickMenuLockPulse")

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
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isEnglish) {
            HomePremiumQuickMenuIcon(icon)
            Spacer(Modifier.width(7.dp))

            Text(
                text = cleanText,
                color = Color(0xFF16A34A).copy(alpha = 0.94f),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Start,
                style = KmiTypography.caption.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.14).sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            if (isLocked) {
                Spacer(Modifier.width(5.dp))
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier
                        .size(13.dp)
                        .graphicsLayer {
                            scaleX = lockScale
                            scaleY = lockScale
                            alpha = 1f
                        }
                )
            }
        } else {
            if (isLocked) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier
                        .size(13.dp)
                        .graphicsLayer {
                            scaleX = lockScale
                            scaleY = lockScale
                            alpha = 1f
                        }
                )
                Spacer(Modifier.width(5.dp))
            }

            Text(
                text = cleanText,
                color = Color(0xFF16A34A).copy(alpha = 0.94f),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Right,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    letterSpacing = (-0.14).sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Spacer(Modifier.width(7.dp))
            HomePremiumQuickMenuIcon(icon)
        }
    }
}

@Composable
private fun HomePremiumQuickMenuIcon(
    icon: ImageVector
) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .background(Color(0xFF16A34A).copy(alpha = 0.10f), CircleShape)
            .border(
                width = 1.dp,
                color = Color(0xFF16A34A).copy(alpha = 0.24f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF16A34A),
            modifier = Modifier.size(10.5.dp)
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

// ========= עזר: הדגשת "עמידת מוצא ..." עד פסיק/נקודה =========
private fun buildExplanationWithStanceHighlight(
    source: String,
    stanceColor: Color
): AnnotatedString {
    val marker = "עמידת מוצא"

    val idx = source.indexOf(marker)
    if (idx < 0) return AnnotatedString(source)

    val sentenceEndExclusive = run {
        val endIdx = source.indexOfAny(charArrayOf('.', ','), startIndex = idx)
        if (endIdx == -1) source.length else endIdx + 1
    }

    val before = source.substring(0, idx)
    val stanceSentence = source.substring(idx, sentenceEndExclusive)
    val after = source.substring(sentenceEndExclusive)

    return buildAnnotatedString {
        append(before)

        val stanceStart = length
        append(stanceSentence)
        val stanceEnd = length

        addStyle(
            style = SpanStyle(
                fontWeight = FontWeight.Bold,
                color = stanceColor
            ),
            start = stanceStart,
            end = stanceEnd
        )

        append(after)
    }
}

/** כרטיס אימון קומפקטי – כמו לפני השינוי, עם סדר אייקונים ישן */
@Composable
private fun TrainingCardCompact(
    training: TrainingData,
    branch: String,
    group: String,
    isCoach: Boolean,
    isEnglish: Boolean,
    status: TrainingStatusEngine.Status,
    activeOverride: TrainingOverride?,
    onManageTraining: () -> Unit
) {
    val ctx = LocalContext.current
    val haptic = rememberHapticsGlobal()
    val clickSound = rememberClickSound()

    var showNavPicker by rememberSaveable(training.cal.timeInMillis) { mutableStateOf(false) }

    val navPref: NavAppPref = remember(training.cal.timeInMillis) {
        readNavPref(ctx)
    }

    fun onNavigateClick() {
        clickSound()
        haptic(true)

        val safeAddress = training.address?.trim().orEmpty()
        if (safeAddress.isBlank()) return

        when (navPref) {
            NavAppPref.ASK -> showNavPicker = true
            NavAppPref.GOOGLE_MAPS -> openGoogleMaps(ctx, safeAddress)
            NavAppPref.WAZE -> openWaze(ctx, safeAddress)
        }
    }

    if (showNavPicker) {
        AlertDialog(
            onDismissRequest = { showNavPicker = false },
            title = {
                Text(
                    text = "פתיחה באמצעות",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "בחר אפליקציה לניווט. אפשר גם לשמור כברירת מחדל.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )

                    var rememberChoice by rememberSaveable(training.cal.timeInMillis) {
                        mutableStateOf(
                            true
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text("זכור בחירה", modifier = Modifier.padding(end = 8.dp))
                        Switch(
                            checked = rememberChoice,
                            onCheckedChange = { rememberChoice = it }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val safeAddress = training.address?.trim().orEmpty()
                                if (safeAddress.isNotBlank()) {
                                    if (rememberChoice) writeNavPref(ctx, NavAppPref.WAZE)
                                    openWaze(ctx, safeAddress)
                                }
                                showNavPicker = false
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("וייז", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val safeAddress = training.address?.trim().orEmpty()
                                if (safeAddress.isNotBlank()) {
                                    if (rememberChoice) writeNavPref(ctx, NavAppPref.GOOGLE_MAPS)
                                    openGoogleMaps(ctx, safeAddress)
                                }
                                showNavPicker = false
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("גוגל מפות", fontWeight = FontWeight.Bold)
                        }
                    }

                    TextButton(
                        onClick = {
                            writeNavPref(ctx, NavAppPref.ASK)
                            showNavPicker = false
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("כל פעם לשאול")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNavPicker = false }) { Text("סגור") }
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
    val dateTimeText = remember(dayText, dateText, timeText, isEnglish) {
        if (isEnglish) {
            "$dayText $dateText · $timeText"
        } else {
            "$dayText $dateText · $timeText"
        }
    }

    @Composable
    fun MapChip(
        label: String,
        icon: @Composable () -> Unit,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        Surface(
            onClick = { onClick() },
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = modifier.height(40.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
            ) {
                icon()
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
                Spacer(Modifier.weight(1f))
            }
        }
    }

    val trainingCardBorderColor =
        when (status.state) {
            TrainingStatusEngine.State.ONGOING ->
                Color(0xFF047857)

            TrainingStatusEngine.State.COMPLETED ->
                Color(0xFF475569)

            TrainingStatusEngine.State.CANCELLED_BY_HOLIDAY ->
                Color(0xFF9A3412)

            TrainingStatusEngine.State.INVALID ->
                Color(0xFFB91C1C)

            TrainingStatusEngine.State.SCHEDULED ->
                Color(0xFF1D4ED8)
        }

    Surface(
        onClick = {
            if (isCoach) {
                clickSound()
                haptic(true)
                onManageTraining()
            }
        },
        enabled = isCoach,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .heightIn(min = 78.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 3.dp,
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
            val branchLine = remember(training.place, training.address, isEnglish) {
                val displaySource = training.place
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?: training.address.orEmpty()

                TrainingCatalog.placeDisplayName(
                    displaySource,
                    isEnglish
                )
            }

            Text(
                text = branchLine,
                style = KmiTypography.cardTitle,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = dateTimeText,
                style = KmiTypography.secondary.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                softWrap = true,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            val statusMessage =
                when {
                    activeOverride?.hasChangedTime == true ->
                        if (isEnglish) {
                            "Training time changed"
                        } else {
                            "שעת האימון שונתה"
                        }

                    activeOverride?.isCancelled == true ->
                        if (isEnglish) {
                            "Cancelled by coach"
                        } else {
                            "בוטל על ידי המאמן"
                        }

                    else ->
                        status.displayText(isEnglish)
                }

            if (!statusMessage.isNullOrBlank()) {
                val statusBackgroundColor =
                    when (status.state) {
                        TrainingStatusEngine.State.ONGOING ->
                            Color(0xFFECFDF5)

                        TrainingStatusEngine.State.COMPLETED ->
                            Color(0xFFF1F5F9)

                        TrainingStatusEngine.State.CANCELLED_BY_HOLIDAY ->
                            Color(0xFFFFF7ED)

                        TrainingStatusEngine.State.INVALID ->
                            Color(0xFFFEF2F2)

                        TrainingStatusEngine.State.SCHEDULED ->
                            Color(0xFFEFF6FF)
                    }

                val statusContentColor =
                    when (status.state) {
                        TrainingStatusEngine.State.ONGOING ->
                            Color(0xFF047857)

                        TrainingStatusEngine.State.COMPLETED ->
                            Color(0xFF475569)

                        TrainingStatusEngine.State.CANCELLED_BY_HOLIDAY ->
                            Color(0xFF9A3412)

                        TrainingStatusEngine.State.INVALID ->
                            Color(0xFFB91C1C)

                        TrainingStatusEngine.State.SCHEDULED ->
                            Color(0xFF1D4ED8)
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
                            color = statusContentColor.copy(
                                alpha = 0.18f
                            )
                        )
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
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavigationChip(
                    address = TrainingCatalog.addressDisplayName(
                        training.address.orEmpty(),
                        isEnglish
                    ),
                    isEnglish = isEnglish,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.weight(1f))
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

    val safeAddress = address?.trim().orEmpty()

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
            if (safeAddress.isNotBlank()) showPicker = true
        },
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.92f),
        tonalElevation = 2.dp,
        shadowElevation = 3.dp,
        border = BorderStroke(
            1.dp,
            Color.Black.copy(alpha = 0.06f)
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
            // אייקון
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                modifier = Modifier.size(30.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_google_maps),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(18.dp)
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
                    // ✅ הכרטיס לבן גם במצב כהה, לכן צבע קבוע וכהה
                    color = Color(0xFF0B1220),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = if (safeAddress.isBlank()) {
                        if (isEnglish) "No address" else "אין כתובת"
                    } else {
                        safeAddress
                    },
                    style = KmiTypography.secondary,
                    // ✅ לא להשתמש כאן ב-onSurfaceVariant,
                    // כי במצב כהה הוא יוצא בהיר מדי על כרטיס לבן.
                    color = Color(0xFF475569),
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
    rememberChoice: Boolean,
    onRememberChoiceChange: (Boolean) -> Unit,
    onPick: (NavChoice) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(Modifier.fillMaxWidth()) {
                Text(
                    text = "ניווט באמצעות",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 9.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "זכור בחירה",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Right
                        )
                        Switch(
                            checked = rememberChoice,
                            onCheckedChange = onRememberChoiceChange
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    val btnHeight = 52.dp

                    // Waze
                    Surface(
                        onClick = { onPick(NavChoice.WAZE) },
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.95f),
                        tonalElevation = 2.dp,
                        shadowElevation = 2.dp,
                        border = BorderStroke(
                            1.dp,
                            Color.Black.copy(alpha = 0.06f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(btnHeight)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_waze),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Waze",
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }

                    // Google Maps
                    Surface(
                        onClick = { onPick(NavChoice.GOOGLE_MAPS) },
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.95f),
                        tonalElevation = 2.dp,
                        shadowElevation = 2.dp,
                        border = BorderStroke(
                            1.dp,
                            Color.Black.copy(alpha = 0.06f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(btnHeight)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_google_maps),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "גוגל מפות",
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Text(
                    text = "אפשר לשמור בחירה כברירת מחדל (בהמשך נחבר את זה להגדרות).",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("סגור") }
        }
    )
}

// ===== העדפת ניווט (Google Maps / Waze / Ask) =====

private enum class NavAppPref { ASK, GOOGLE_MAPS, WAZE }

private const val NAV_PREFS_FILE = "kmi_user"
private const val NAV_PREF_KEY = "nav_app_pref"

private fun readNavPref(ctx: Context): NavAppPref {
    val sp = ctx.getSharedPreferences(NAV_PREFS_FILE, Context.MODE_PRIVATE)
    return when (sp.getString(NAV_PREF_KEY, "ask")?.lowercase()) {
        "gmaps", "google", "google_maps" -> NavAppPref.GOOGLE_MAPS
        "waze" -> NavAppPref.WAZE
        else -> NavAppPref.ASK
    }
}

private fun writeNavPref(ctx: Context, pref: NavAppPref) {
    val sp = ctx.getSharedPreferences(NAV_PREFS_FILE, Context.MODE_PRIVATE)
    val v = when (pref) {
        NavAppPref.ASK -> "ask"
        NavAppPref.GOOGLE_MAPS -> "gmaps"
        NavAppPref.WAZE -> "waze"
    }
    sp.edit().putString(NAV_PREF_KEY, v).apply()
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
    val pdfFile = createHomePdf(
        context = context,
        trainings = trainings,
        isEnglish = isEnglish
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
    val page = document.startPage(
        PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
    )
    val canvas = page.canvas

    val navy = android.graphics.Color.rgb(2, 43, 74)
    val blue = android.graphics.Color.rgb(12, 78, 130)
    val lightBlue = android.graphics.Color.rgb(234, 246, 255)
    val softBlue = android.graphics.Color.rgb(244, 250, 255)
    val borderBlue = android.graphics.Color.rgb(191, 213, 232)
    val textDark = android.graphics.Color.rgb(15, 23, 42)
    val textMuted = android.graphics.Color.rgb(80, 100, 120)
    val orange = android.graphics.Color.rgb(249, 115, 22)

    val regular = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    val bold = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)

    fun alpha(color: Int, value: Float): Int =
        ColorUtils.setAlphaComponent(color, (value.coerceIn(0f, 1f) * 255).toInt())

    fun paint(
        size: Float,
        color: Int = textDark,
        typeface: Typeface = regular,
        align: Paint.Align = if (isEnglish) Paint.Align.LEFT else Paint.Align.RIGHT
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

    fun drawKmiLogo(cx: Float, cy: Float, radius: Float) {
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

    fun drawHeader() {
        canvas.drawColor(android.graphics.Color.WHITE)

        val diagonal = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = navy }
        val accent1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(36, 103, 158)
        }
        val accent2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(128, 183, 220)
        }

        val path = Path().apply {
            moveTo(pageWidth.toFloat(), 0f)
            lineTo(pageWidth.toFloat(), 122f)
            lineTo(178f, 122f)
            lineTo(238f, 0f)
            close()
        }
        canvas.drawPath(path, diagonal)

        canvas.drawPath(Path().apply {
            moveTo(208f, 122f)
            lineTo(224f, 122f)
            lineTo(284f, 0f)
            lineTo(268f, 0f)
            close()
        }, accent1)

        canvas.drawPath(Path().apply {
            moveTo(230f, 122f)
            lineTo(238f, 122f)
            lineTo(298f, 0f)
            lineTo(290f, 0f)
            close()
        }, accent2)

        drawKmiLogo(78f, 58f, 42f)

        titlePaint.textAlign = Paint.Align.RIGHT
        subTitlePaint.textAlign = Paint.Align.RIGHT

        canvas.drawText(tr("מסך הבית", "Home"), pageWidth - 34f, 52f, titlePaint)
        canvas.drawText(
            tr("דו״ח אימונים לשבוע הקרוב", "Upcoming weekly trainings"),
            pageWidth - 34f,
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

    fun drawFooter() {
        val footerY = 804f

        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = navy
            strokeWidth = 2f
        }

        canvas.drawLine(0f, footerY, pageWidth.toFloat(), footerY, line)

        drawKmiLogo(38f, footerY + 22f, 13f)

        smallPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("Together We Protect", 62f, footerY + 25f, smallPaint)

        smallPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(tr("עמוד 1 מתוך 1", "Page 1 of 1"), pageWidth / 2f, footerY + 25f, smallPaint)

        smallPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Krav Maga Israel", pageWidth - 66f, footerY + 18f, smallPaint)
        canvas.drawText("www.kmi.org.il", pageWidth - 66f, footerY + 31f, smallPaint)

        val flag = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(20, 85, 200)
        }

        canvas.drawRect(pageWidth - 48f, footerY + 14f, pageWidth - 20f, footerY + 18f, flag)
        canvas.drawRect(pageWidth - 48f, footerY + 28f, pageWidth - 20f, footerY + 32f, flag)
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

        sectionPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(
            tr("אימונים לשבוע הקרוב", "Upcoming trainings"),
            pageWidth - margin - 22f,
            top + 32f,
            sectionPaint
        )

        labelPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(
            tr("מספר אימונים מוצגים:", "Displayed trainings:"),
            pageWidth - margin - 22f,
            top + 58f,
            labelPaint
        )

        boldValuePaint.textAlign = Paint.Align.LEFT
        boldValuePaint.textSize = 24f
        boldValuePaint.color = navy
        canvas.drawText("${trainings.size}", margin + 28f, top + 56f, boldValuePaint)

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
        val left = margin
        val right = pageWidth - margin
        val bottom = top + cardHeight
        val mid = pageWidth / 2f

        drawRoundRect(
            left,
            top,
            right,
            bottom,
            if (index % 2 == 0) lightBlue else softBlue,
            12f
        )
        drawRoundRect(
            left,
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

        sectionPaint.textAlign = Paint.Align.RIGHT
        sectionPaint.textSize = 13.5f
        canvas.drawText(
            training.place.ifBlank { tr("מיקום לא הוגדר", "Location not set") }.take(34),
            right - 22f,
            top + 30f,
            sectionPaint
        )
        sectionPaint.textSize = 17f

        labelPaint.textAlign = Paint.Align.RIGHT
        valuePaint.textAlign = Paint.Align.RIGHT
        boldValuePaint.textAlign = Paint.Align.RIGHT

        canvas.drawText(tr("תאריך ושעה:", "Date and time:"), right - 22f, top + 58f, labelPaint)
        canvas.drawText(
            "${training.day} ${training.date} · ${training.time}",
            right - 22f,
            top + 76f,
            boldValuePaint
        )

        canvas.drawText(tr("כתובת:", "Address:"), mid - 22f, top + 30f, labelPaint)
        canvas.drawText(
            training.address.ifBlank { "—" }.take(30),
            mid - 22f,
            top + 48f,
            valuePaint
        )

        canvas.drawText(tr("מאמן:", "Coach:"), mid - 22f, top + 72f, labelPaint)
        canvas.drawText(
            training.coach.ifBlank { "—" }.take(22),
            mid - 22f,
            top + 90f,
            boldValuePaint
        )

        if (training.cancelledByHoliday) {
            drawRoundRect(
                left + 22f,
                bottom - 28f,
                right - 22f,
                bottom - 9f,
                alpha(orange, 0.12f),
                999f
            )
            drawRoundRect(
                left + 22f,
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

    drawHeader()

    var y = 136f
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
    } else {
        trainings.take(5).forEachIndexed { index, training ->
            if (y + 122f < 792f) {
                y = drawTrainingCard(
                    training = training,
                    top = y,
                    index = index
                )
            }
        }
    }

    drawFooter()

    document.finishPage(page)

    val dir = File(context.cacheDir, "pdfs").apply { mkdirs() }
    val file = File(dir, "home_report_${System.currentTimeMillis()}.pdf")

    FileOutputStream(file).use { output ->
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
    if (isPackageInstalled(ctx, mapsPkg)) {
        val navUri = Uri.parse("google.navigation:q=" + Uri.encode(safeAddress))
        val i = Intent(Intent.ACTION_VIEW, navUri).apply {
            setPackage(mapsPkg)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { ctx.startActivity(i) }
        return
    }

    // ❗ Maps לא מותקן → לפתוח Play Store בלי chooser של חנויות
    openPlayStoreDirect(ctx, mapsPkg)
}

private fun isPackageInstalled(ctx: Context, pkg: String): Boolean {
    return runCatching {
        ctx.packageManager.getPackageInfo(pkg, 0)
        true
    }.getOrElse { false }
}

private fun openPlayStoreDirect(ctx: Context, pkg: String) {
    val playPkg = "com.android.vending" // Google Play

    // ניסיון 1: לפתוח Play Store ישירות (בלי לשאול Galaxy Store)
    val marketUri = Uri.parse("market://details?id=$pkg")
    val playIntent = Intent(Intent.ACTION_VIEW, marketUri).apply {
        setPackage(playPkg)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    if (playIntent.resolveActivity(ctx.packageManager) != null) {
        ctx.startActivity(playIntent)
        return
    }

    // ניסיון 2: fallback לדפדפן (אם אין Play Store)
    val webUri = Uri.parse("https://play.google.com/store/apps/details?id=$pkg")
    ctx.startActivity(Intent(Intent.ACTION_VIEW, webUri).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    })
}

private fun clearNavPref(ctx: Context) {
    val sp = ctx.getSharedPreferences(NAV_PREFS_FILE, Context.MODE_PRIVATE)
    sp.edit().remove(NAV_PREF_KEY).apply()   // או putString(NAV_PREF_KEY, "ask")
}

private fun openWaze(ctx: Context, address: String?) {
    val safeAddress = address?.trim().orEmpty()
    if (safeAddress.isEmpty()) return

    val wazeUri = Uri.parse("https://waze.com/ul?q=" + Uri.encode(safeAddress))
    val intent = Intent(Intent.ACTION_VIEW, wazeUri).apply {
        setPackage("com.waze")
    }

    if (intent.resolveActivity(ctx.packageManager) != null) {
        ctx.startActivity(intent)
    } else {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, wazeUri))
    }
}
