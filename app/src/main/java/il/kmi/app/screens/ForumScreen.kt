@file:OptIn(ExperimentalMaterial3Api::class)
package il.kmi.app.screens

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.core.net.toUri
import il.kmi.app.ui.pdf.KmiPdfFooter
import il.kmi.app.ui.pdf.KmiPdfHeader
import java.io.File
import java.io.FileOutputStream
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import il.kmi.app.subscription.KmiAccess
import il.kmi.app.localization.rememberIsEnglish
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.datetime.Instant
import kotlinx.datetime.toKotlinInstant
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import il.kmi.app.ui.KmiIconSize
import il.kmi.app.ui.KmiPremiumDropdown
import il.kmi.app.ui.KmiTypography
import il.kmi.app.ui.loading.KmiLoadingRings
import il.kmi.app.ui.scaledIconSize
import il.kmi.app.privacy.TraineeDisplayNameMapper
import il.kmi.app.screens.registration.CoachBranchAssignmentsCodec
import il.yuval.ui.theme.kmiScreenBackgroundBrush

//==================================================================

// הודעה למסך (כולל מידע אם זו הודעה שלי + מדיה)
private data class ForumUiMessage(
    val id: String,
    val messageId: String,
    val branch: String,
    val groupKey: String,
    val authorName: String,
    val authorEmail: String,
    val authorUid: String?,
    val text: String,
    val createdAt: Instant,
    val createdAtMillis: Long,
    val updatedAtMillis: Long?,
    val mediaUrl: String?,
    val mediaType: String?,   // "image" / "video" / null
    val isMine: Boolean
)

// משתתף בפורום – לצורך רשימת המשתתפים
private data class ForumParticipantUi(
    val id: String,
    val name: String,
    val isMe: Boolean
)

private fun forumDisplayPersonName(
    realName: String?,
    stableKey: String?,
    demoIndex: Int?,
    isEnglish: Boolean
): String {
    return TraineeDisplayNameMapper.displayName(
        realName = realName,
        stableKey = stableKey,
        demoIndex = demoIndex,
        isEnglish = isEnglish
    ).ifBlank {
        forumTr(
            isEnglish,
            "משתתף",
            "Participant"
        )
    }
}

private fun forumTr(isEnglish: Boolean, he: String, en: String): String =
    if (isEnglish) en else he

private fun forumTextAlign(isEnglish: Boolean): TextAlign =
    if (isEnglish) TextAlign.Left else TextAlign.Right

private fun createForumPdf(
    context: android.content.Context,
    branch: String,
    groupKey: String,
    messages: List<ForumUiMessage>,
    participants: List<ForumParticipantUi>,
    isEnglish: Boolean
): File {
    val pageWidth = 595
    val pageHeight = 842

    val contentLeft = 36f
    val contentRight = pageWidth - 36f
    val contentWidth = contentRight - contentLeft

    val contentBottom =
        pageHeight -
                KmiPdfFooter.CONTENT_BOTTOM_PADDING

    val document = PdfDocument()

    val titleColor =
        android.graphics.Color.rgb(15, 23, 42)

    val secondaryTextColor =
        android.graphics.Color.rgb(71, 85, 105)

    val rowBackground =
        android.graphics.Color.rgb(248, 250, 252)

    val rowBorder =
        android.graphics.Color.rgb(203, 213, 225)

    val authorPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = titleColor
            textSize = 11.5f
            typeface = android.graphics.Typeface.create(
                android.graphics.Typeface.SANS_SERIF,
                android.graphics.Typeface.BOLD
            )
            textAlign =
                if (isEnglish) {
                    Paint.Align.LEFT
                } else {
                    Paint.Align.RIGHT
                }
        }

    val messagePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = titleColor
            textSize = 10.5f
            typeface = android.graphics.Typeface.create(
                android.graphics.Typeface.SANS_SERIF,
                android.graphics.Typeface.NORMAL
            )
            textAlign =
                if (isEnglish) {
                    Paint.Align.LEFT
                } else {
                    Paint.Align.RIGHT
                }
        }

    val datePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryTextColor
            textSize = 8.5f
            typeface = android.graphics.Typeface.create(
                android.graphics.Typeface.SANS_SERIF,
                android.graphics.Typeface.NORMAL
            )
            textAlign =
                if (isEnglish) {
                    Paint.Align.RIGHT
                } else {
                    Paint.Align.LEFT
                }
        }

    val emptyPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryTextColor
            textSize = 12f
            typeface = android.graphics.Typeface.create(
                android.graphics.Typeface.SANS_SERIF,
                android.graphics.Typeface.BOLD
            )
            textAlign = Paint.Align.CENTER
        }

    val fillPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = rowBackground
            style = Paint.Style.FILL
        }

    val borderPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = rowBorder
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

    val dateFormatter =
        SimpleDateFormat(
            if (isEnglish) {
                "dd/MM/yyyy HH:mm"
            } else {
                "dd/MM/yyyy HH:mm"
            },
            Locale.getDefault()
        )

    fun cleanText(value: String): String {
        return value
            .replace("\r", " ")
            .replace("\n", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun wrapText(
        value: String,
        paint: Paint,
        maxWidth: Float
    ): List<String> {
        val clean = cleanText(value)

        if (clean.isBlank()) {
            return emptyList()
        }

        val words = clean.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""

        words.forEach { word ->
            val candidate =
                if (currentLine.isBlank()) {
                    word
                } else {
                    "$currentLine $word"
                }

            if (
                currentLine.isNotBlank() &&
                paint.measureText(candidate) > maxWidth
            ) {
                lines += currentLine
                currentLine = word
            } else {
                currentLine = candidate
            }
        }

        if (currentLine.isNotBlank()) {
            lines += currentLine
        }

        return lines
    }

    fun participantIndexFor(
        message: ForumUiMessage
    ): Int? {
        val index =
            participants.indexOfFirst { participant ->
                (
                        !message.authorUid.isNullOrBlank() &&
                                participant.id == message.authorUid
                        ) ||
                        (
                                message.authorEmail.isNotBlank() &&
                                        participant.id.equals(
                                            message.authorEmail,
                                            ignoreCase = true
                                        )
                                ) ||
                        (
                                message.authorName.isNotBlank() &&
                                        participant.name.trim().equals(
                                            message.authorName.trim(),
                                            ignoreCase = true
                                        )
                                )
            }

        return index.takeIf { it >= 0 }
    }

    fun authorNameFor(
        message: ForumUiMessage
    ): String {
        val participant =
            participants.getOrNull(
                participantIndexFor(message) ?: -1
            )

        val realName =
            message.authorName
                .ifBlank {
                    participant?.name.orEmpty()
                }
                .ifBlank {
                    message.authorEmail
                }

        return TraineeDisplayNameMapper.displayName(
            realName = realName,
            stableKey =
                message.authorUid
                    ?.takeIf { it.isNotBlank() }
                    ?: message.authorEmail
                        .takeIf { it.isNotBlank() }
                    ?: message.id,
            demoIndex = participantIndexFor(message),
            isEnglish = isEnglish
        ).ifBlank {
            if (isEnglish) {
                "Participant"
            } else {
                "משתתף"
            }
        }
    }

    var pageNumber = 0
    var hasActivePage = false
    lateinit var page: PdfDocument.Page
    lateinit var canvas: android.graphics.Canvas
    var y = KmiPdfHeader.CONTENT_TOP

    fun drawHeader() {
        KmiPdfHeader.draw(
            context = context,
            canvas = canvas,
            pageWidth = pageWidth,
            isEnglish = isEnglish,
            titleHebrew = "דו״ח פורום הסניף",
            titleEnglish = "Branch Forum Report",
            subtitleHebrew =
                listOf(branch, groupKey)
                    .filter { it.isNotBlank() }
                    .joinToString(" · "),
            subtitleEnglish =
                listOf(branch, groupKey)
                    .filter { it.isNotBlank() }
                    .joinToString(" · ")
        )
    }

    fun drawFooter() {
        KmiPdfFooter.draw(
            canvas = canvas,
            pageWidth = pageWidth,
            pageHeight = pageHeight,
            pageNumber = pageNumber,
            totalPages = null,
            isEnglish = isEnglish
        )
    }

    fun startPage() {
        if (hasActivePage) {
            drawFooter()
            document.finishPage(page)
        }

        pageNumber++

        page =
            document.startPage(
                PdfDocument.PageInfo.Builder(
                    pageWidth,
                    pageHeight,
                    pageNumber
                ).create()
            )

        canvas = page.canvas
        hasActivePage = true

        drawHeader()

        y = KmiPdfHeader.CONTENT_TOP
    }

    fun ensureSpace(requiredHeight: Float) {
        if (y + requiredHeight > contentBottom) {
            startPage()
        }
    }

    startPage()

    if (messages.isEmpty()) {
        canvas.drawText(
            if (isEnglish) {
                "There are no messages in this forum room."
            } else {
                "אין הודעות בחדר הפורום שנבחר."
            },
            pageWidth / 2f,
            y + 42f,
            emptyPaint
        )
    } else {
        /*
         * רשימת המסך מוצגת ב-reverseLayout.
         * ב-PDF ההודעות מסודרות מהישנה לחדשה.
         */
        messages
            .sortedBy { it.createdAtMillis }
            .forEach { message ->
                val authorName = authorNameFor(message)

                val messageText =
                    message.text.ifBlank {
                        when (message.mediaType) {
                            "image" ->
                                if (isEnglish) {
                                    "Attached image"
                                } else {
                                    "תמונה מצורפת"
                                }

                            "video" ->
                                if (isEnglish) {
                                    "Attached video"
                                } else {
                                    "סרטון מצורף"
                                }

                            else ->
                                if (isEnglish) {
                                    "Message without text"
                                } else {
                                    "הודעה ללא טקסט"
                                }
                        }
                    }

                val lines =
                    wrapText(
                        value = messageText,
                        paint = messagePaint,
                        maxWidth = contentWidth - 32f
                    )

                val rowHeight =
                    56f +
                            lines.size.coerceAtLeast(1) * 15f

                ensureSpace(rowHeight + 10f)

                val rowTop = y
                val rowBottom = y + rowHeight

                canvas.drawRoundRect(
                    contentLeft,
                    rowTop,
                    contentRight,
                    rowBottom,
                    12f,
                    12f,
                    fillPaint
                )

                canvas.drawRoundRect(
                    contentLeft,
                    rowTop,
                    contentRight,
                    rowBottom,
                    12f,
                    12f,
                    borderPaint
                )

                val textX =
                    if (isEnglish) {
                        contentLeft + 16f
                    } else {
                        contentRight - 16f
                    }

                val dateX =
                    if (isEnglish) {
                        contentRight - 16f
                    } else {
                        contentLeft + 16f
                    }

                canvas.drawText(
                    authorName,
                    textX,
                    rowTop + 22f,
                    authorPaint
                )

                canvas.drawText(
                    dateFormatter.format(
                        Date(message.createdAtMillis)
                    ),
                    dateX,
                    rowTop + 22f,
                    datePaint
                )

                var lineY = rowTop + 43f

                lines
                    .ifEmpty { listOf("—") }
                    .forEach { line ->
                        canvas.drawText(
                            line,
                            textX,
                            lineY,
                            messagePaint
                        )

                        lineY += 15f
                    }

                y = rowBottom + 10f
            }
    }

    drawFooter()
    document.finishPage(page)

    val directory =
        File(
            context.cacheDir,
            "pdfs"
        ).apply {
            mkdirs()
        }

    val fileName =
        if (isEnglish) {
            "Branch Forum.pdf"
        } else {
            "פורום הסניף.pdf"
        }

    val file =
        File(
            directory,
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

private fun shareForumPdf(
    context: android.content.Context,
    pdfFile: File,
    isEnglish: Boolean
) {
    val uri =
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )

    val intent =
        Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"

            putExtra(
                Intent.EXTRA_SUBJECT,
                if (isEnglish) {
                    "Branch Forum"
                } else {
                    "פורום הסניף"
                }
            )

            putExtra(
                Intent.EXTRA_STREAM,
                uri
            )

            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }

    context.startActivity(
        Intent.createChooser(
            intent,
            if (isEnglish) {
                "Share PDF"
            } else {
                "שיתוף PDF"
            }
        )
    )
}

private const val FORUM_MESSAGE_RETENTION_DAYS = 90L
private const val FORUM_MESSAGE_RETENTION_MILLIS =
    FORUM_MESSAGE_RETENTION_DAYS * 24L * 60L * 60L * 1000L

private const val FORUM_LAST_SELECTED_BRANCH_KEY = "forum_last_selected_branch"
private const val FORUM_LAST_SELECTED_GROUP_KEY = "forum_last_selected_group"

private fun forumLastReadKey(branch: String, groupKey: String): String =
    "forum_last_read_at_${branch.trim()}_${groupKey.trim()}"

private fun forumSafeDocId(raw: String): String {
    return raw
        .trim()
        .lowercase()
        .replace('־', '-')
        .replace('–', '-')
        .replace('—', '-')
        .replace(Regex("\\s+"), "_")
        .replace(Regex("[^a-z0-9א-ת_\\-]+"), "_")
        .trim('_')
        .ifBlank { "default" }
}

private fun forumRoomDocId(branch: String, groupKey: String): String {
    return "room_${forumSafeDocId(branch)}_${forumSafeDocId(groupKey)}"
}

private fun forumPrefsList(
    sp: SharedPreferences,
    vararg keys: String
): List<String> {
    val out = mutableListOf<String>()

    fun addClean(value: String?) {
        val clean = value
            ?.trim()
            ?.removeSurrounding("\"")
            ?.trim()
            .orEmpty()

        if (
            clean.isNotBlank() &&
            clean != "null" &&
            !clean.startsWith("{") &&
            !clean.startsWith("[")
        ) {
            out += clean
        }
    }

    fun readJsonObject(obj: org.json.JSONObject) {
        val possibleKeys = listOf(
            "name",
            "title",
            "label",
            "value",
            "branch",
            "branchName",
            "branch_name",
            "group",
            "groupName",
            "group_key",
            "groupKey",
            "age_group",
            "id",
            "key"
        )

        possibleKeys.forEach { jsonKey ->
            if (obj.has(jsonKey)) {
                addClean(obj.optString(jsonKey))
            }
        }
    }

    keys.forEach { key ->
        when (val value = sp.all[key]) {
            is String -> {
                val raw = value.trim()

                when {
                    raw.isBlank() -> Unit

                    raw.startsWith("[") -> {
                        runCatching {
                            val arr = org.json.JSONArray(raw)

                            for (i in 0 until arr.length()) {
                                when (
                                    val item =
                                        arr.opt(i)
                                ) {
                                    is String -> addClean(item)
                                    is org.json.JSONObject -> readJsonObject(item)
                                    else -> addClean(item?.toString())
                                }
                            }
                        }.onFailure {
                            raw
                                .split(',', ';', '|', '\n', '•')
                                .forEach { addClean(it) }
                        }
                    }

                    raw.startsWith("{") -> {
                        runCatching {
                            readJsonObject(org.json.JSONObject(raw))
                        }.onFailure {
                            addClean(raw)
                        }
                    }

                    else -> {
                        raw
                            .split(',', ';', '|', '\n', '•')
                            .forEach { addClean(it) }
                    }
                }
            }

            is List<*> -> {
                value.forEach { addClean(it?.toString()) }
            }

            is Set<*> -> {
                value.forEach { addClean(it?.toString()) }
            }
        }
    }

    return out
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
}

@Composable
fun ForumScreen(
    sp: SharedPreferences,
    onBack: () -> Unit,
    onOpenExercise: (String) -> Unit = { _ -> },
    onOpenSubscription: () -> Unit = {},   // 👈 חדש
    onGoHome: () -> Unit                   // 👈 נשתמש בו באמת
) {
    val ctx = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val isEnglish = rememberIsEnglish()
    val screenTextAlign = forumTextAlign(isEnglish)

    // 🔵 דיאלוג AI פתוח/סגור
    var showAiDialog by rememberSaveable {
        mutableStateOf(false)
    }

    val userSp = remember {
        ctx.getSharedPreferences(
            "kmi_user",
            android.content.Context.MODE_PRIVATE
        )
    }

    val settingsSp = remember(ctx) {
        ctx.getSharedPreferences(
            "kmi_settings",
            android.content.Context.MODE_PRIVATE
        )
    }

    /*
     * ערכת הנושא כבר נקבעת ב־MainActivity וב־AppTheme.
     * לכן המסך צריך לקרוא את ערכי MaterialTheme ולא
     * לנהל מנגנון כהה נוסף מתוך SharedPreferences.
     */
    val isDarkMode =
        MaterialTheme.colorScheme.background
            .luminance() < 0.5f

    // --- זיהוי מנהל / override ---

    // דגל מנהל כפי שנשמר במסך המנוי (kmi_user.is_manager)
    var isManagerOverride by remember {
        mutableStateOf(userSp.getBoolean("is_manager", false))
    }

    // עדכון חי כשמשתנה SharedPreferences (כניסה/יציאה ממצב מנהל)
    DisposableEffect(userSp) {
        val l = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "is_manager") {
                isManagerOverride = userSp.getBoolean("is_manager", false)
            }
        }
        userSp.registerOnSharedPreferenceChangeListener(l)
        onDispose { userSp.unregisterOnSharedPreferenceChangeListener(l) }
    }

    // --- מצב מנוי ---

    val subsSp = remember(ctx) {
        ctx.getSharedPreferences("kmi_subs", android.content.Context.MODE_PRIVATE)
    }

    val legacySp = remember(ctx) {
        ctx.getSharedPreferences("kmi_prefs", android.content.Context.MODE_PRIVATE)
    }

    var forumAccessRefreshTick by remember { mutableIntStateOf(0) }

    DisposableEffect(userSp, subsSp, legacySp) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (
                key == "has_full_access" ||
                key == "full_access" ||
                key == "subscription_active" ||
                key == "is_subscribed" ||
                key == "google_subscription_verified" ||
                key == "google_subscription_checked_at" ||
                key == "sub_access_until" ||
                key == "access_changed_at" ||
                key == "sub_product"
            ) {
                forumAccessRefreshTick++
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

        val verifiedAndValid =
            getBoolean("google_subscription_verified", false) && until > now

        return KmiAccess.hasFullAccess(this) ||
                verifiedAndValid ||
                getBoolean("has_full_access", false) ||
                getBoolean("full_access", false) ||
                getBoolean("subscription_active", false) ||
                getBoolean("is_subscribed", false)
    }

    val isTrial = remember(forumAccessRefreshTick, isManagerOverride) {
        KmiAccess.isTrialActive(userSp) &&
                !userSp.hasActiveSubscriptionAccess() &&
                !subsSp.hasActiveSubscriptionAccess() &&
                !legacySp.hasActiveSubscriptionAccess()
    }

    val hasFull = remember(forumAccessRefreshTick, isManagerOverride) {
        isManagerOverride ||
                userSp.hasActiveSubscriptionAccess() ||
                subsSp.hasActiveSubscriptionAccess() ||
                legacySp.hasActiveSubscriptionAccess()
    }

    // 🔒 גישת פורום:
    // פתוח רק למנהל או למנוי חודשי/שנתי פעיל.
    // Trial לא פותח פורום.
    val canUseExtras = hasFull

    /*
     * המבנה החדש שבו הקבוצות נשמרות לפי הסניף
     * שאליו הן שייכות.
     */
    val forumBranchAssignments =
        remember(
            userSp,
            sp,
            legacySp,
            settingsSp
        ) {
            listOf(
                userSp,
                sp,
                legacySp,
                settingsSp
            )
                .asSequence()
                .map { prefs ->
                    CoachBranchAssignmentsCodec.decode(
                        prefs.getString(
                            "coach_branch_assignments_json",
                            ""
                        )
                    )
                }
                .firstOrNull {
                    it.isNotEmpty()
                }
                .orEmpty()
        }

    // סניפים של המשתמש — המבנה החדש קודם למבנה הישן.
    val availableForumBranches =
        remember(
            forumBranchAssignments,
            userSp,
            sp,
            legacySp,
            settingsSp
        ) {
            if (forumBranchAssignments.isNotEmpty()) {
                CoachBranchAssignmentsCodec
                    .flattenBranches(
                        forumBranchAssignments
                    )
            } else {
        listOf(userSp, sp, legacySp, settingsSp)
            .flatMap { prefs ->
                forumPrefsList(
                    prefs,
                    "active_branch",
                    "activeBranch",
                    "branches_json",
                    "selected_branches",
                    "selectedBranches",
                    "branches",
                    "branchesCsv",
                    "branches_csv",
                    "branchNames",
                    "branch_names",
                    "branch"
                )
            }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            }
        }

    val legacyForumGroups =
        remember(
            userSp,
            sp,
            legacySp,
            settingsSp
        ) {
        listOf(userSp, sp, legacySp, settingsSp)
            .flatMap { prefs ->
                forumPrefsList(
                    prefs,
                    "active_group",
                    "activeGroup",
                    "groups_json",
                    "selected_groups",
                    "selectedGroups",
                    "groups",
                    "groupsCsv",
                    "groups_csv",
                    "primaryGroup",
                    "groupKey",
                    "group_key",
                    "groupName",
                    "group_name",
                    "age_group",
                    "group"
                )
            }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    var selectedForumBranch by
    rememberSaveable(
        availableForumBranches
    ) {
        val lastBranch =
            userSp
                .getString(
                    FORUM_LAST_SELECTED_BRANCH_KEY,
                    ""
                )
                .orEmpty()
                .trim()

        mutableStateOf(
            lastBranch
                .takeIf {
                    it.isNotBlank() &&
                            it in availableForumBranches
                }
                ?: availableForumBranches
                    .firstOrNull()
                    .orEmpty()
        )
    }

    fun normalizedForumAssignmentValue(
        value: String
    ): String {
        return value
            .trim()
            .replace('־', '-')
            .replace('–', '-')
            .replace('—', '-')
            .replace(Regex("\\s+"), " ")
            .lowercase()
    }

    /*
     * מציגים רק את הקבוצות ששויכו לסניף
     * שנבחר כרגע.
     */
    val availableForumGroups =
        remember(
            forumBranchAssignments,
            selectedForumBranch,
            legacyForumGroups
        ) {
            if (forumBranchAssignments.isNotEmpty()) {
                forumBranchAssignments
                    .firstOrNull { assignment ->
                        normalizedForumAssignmentValue(
                            assignment.branch
                        ) ==
                                normalizedForumAssignmentValue(
                                    selectedForumBranch
                                )
                    }
                    ?.groups
                    .orEmpty()
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
            } else {
                legacyForumGroups
            }
        }

    var selectedForumGroup by
    rememberSaveable(
        availableForumGroups,
        selectedForumBranch
    ) {
        val lastGroup = userSp
            .getString(FORUM_LAST_SELECTED_GROUP_KEY, "")
            .orEmpty()
            .trim()

        mutableStateOf(
            lastGroup
                .takeIf { it.isNotBlank() && it in availableForumGroups }
                ?: availableForumGroups.firstOrNull().orEmpty()
        )
    }

    LaunchedEffect(availableForumBranches) {
        if (selectedForumBranch.isBlank() || selectedForumBranch !in availableForumBranches) {
            val lastBranch = userSp
                .getString(FORUM_LAST_SELECTED_BRANCH_KEY, "")
                .orEmpty()
                .trim()

            selectedForumBranch = lastBranch
                .takeIf { it.isNotBlank() && it in availableForumBranches }
                ?: availableForumBranches.firstOrNull().orEmpty()
        }
    }

    LaunchedEffect(availableForumGroups) {
        if (selectedForumGroup.isBlank() || selectedForumGroup !in availableForumGroups) {
            val lastGroup = userSp
                .getString(FORUM_LAST_SELECTED_GROUP_KEY, "")
                .orEmpty()
                .trim()

            selectedForumGroup = lastGroup
                .takeIf { it.isNotBlank() && it in availableForumGroups }
                ?: availableForumGroups.firstOrNull().orEmpty()
        }
    }

    val branch = selectedForumBranch
    val groupKey = selectedForumGroup
    val forumRoomId = remember(branch, groupKey) {
        forumRoomDocId(branch, groupKey)
    }

    val openFromPushInitial = remember {
        sp.getBoolean("forum_open_from_push", false)
    }

    var pendingPushRoomId by rememberSaveable {
        mutableStateOf(
            if (openFromPushInitial) {
                sp.getString("forum_push_room_id", "").orEmpty()
            } else {
                ""
            }
        )
    }

    var pendingPushMessageId by rememberSaveable {
        mutableStateOf(
            if (openFromPushInitial) {
                sp.getString("forum_push_message_id", "").orEmpty()
            } else {
                ""
            }
        )
    }

    var pendingPushHandled by rememberSaveable {
        mutableStateOf(!openFromPushInitial)
    }

    LaunchedEffect(openFromPushInitial, pendingPushMessageId) {
        if (openFromPushInitial && pendingPushMessageId.isBlank()) {
            pendingPushHandled = true

            sp.edit {
                putBoolean(
                    "forum_open_from_push",
                    false
                )
                remove("forum_push_message_id")
                remove("forum_push_room_id")
                remove("forum_push_room_name")
                remove("forum_push_branch_id")
                remove("forum_push_group_key")
                remove("forum_push_sender_id")
                remove("forum_push_received_at")
            }
        }
    }

    // 👇 שם המשתמש – מנסה כמה מפתחות מהרישום
    val fullName = remember {
        userSp.getString("fullName", null)
            ?: userSp.getString("name", null)
            ?: userSp.getString("displayName", null)
            ?: ""
    }
    val email = remember { userSp.getString("email", "") ?: "" }

    LaunchedEffect(branch, groupKey) {
        if (branch.isNotBlank() && groupKey.isNotBlank()) {
            userSp.edit {
                putString(
                    FORUM_LAST_SELECTED_BRANCH_KEY,
                    branch
                )
                putString(
                    FORUM_LAST_SELECTED_GROUP_KEY,
                    groupKey
                )
                putLong(
                    forumLastReadKey(
                        branch,
                        groupKey
                    ),
                    System.currentTimeMillis()
                )
            }
        }
    }

    val db = remember { Firebase.firestore }
    val storage = remember { Firebase.storage }    // 👈 storage זמין
    val scope = rememberCoroutineScope()

    var input by remember {
        mutableStateOf("")
    }

    var messages by remember {
        mutableStateOf(
            listOf<ForumUiMessage>()
        )
    }

    /*
     * נשאר true עד לקבלת התוצאה הראשונה
     * ממאזין ההודעות של Firestore.
     */
    var isMessagesLoading by remember {
        mutableStateOf(false)
    }

    val listState =
        rememberLazyListState()

    // רשימת משתתפים לפי משתמשים ב־Firestore (בסניף)
    var participantsByUsers by remember { mutableStateOf<List<ForumParticipantUi>>(emptyList()) }

// ✅ בזמן החלפת סניף/קבוצה לא מציגים משתתפים מהחדר הקודם
    var isParticipantsLoading by remember { mutableStateOf(false) }

// ✅ החלק העליון יהיה מתקפל כדי להשאיר מקום לפורום
    var isRoomPickerExpanded by rememberSaveable { mutableStateOf(false) }
    var isParticipantsExpanded by rememberSaveable { mutableStateOf(false) }

    // ✅ הפורום נפתח כשהכרטיס העליון סגור כדי לתת יותר שדה ראייה להודעות
    var isForumControlsCollapsed by rememberSaveable { mutableStateOf(true) }

// הודעה בעריכה (אם יש) + טקסט לעריכה
    var editingMessage by remember { mutableStateOf<ForumUiMessage?>(null) }
    var editText by remember { mutableStateOf("") }

    // מדיה שמצורפת להודעה שנשלחת
    var attachedUri by remember { mutableStateOf<Uri?>(null) }
    var attachedMediaType by remember { mutableStateOf<String?>(null) } // "image"/"video"/null

    // במסך אמת לא מתחברים אנונימית.
    // הפורום צריך לעבוד עם משתמש Firebase אמיתי מהכניסה לאפליקציה.
    LaunchedEffect(Unit) {
        FirebaseAuth.getInstance()
    }

    // ================== האזנה בזמן אמת ==================
    DisposableEffect(
        branch,
        groupKey
    ) {
        /*
         * בכל מעבר לסניף או לקבוצה מנקים
         * מיד את ההודעות הישנות ומציגים טעינה.
         */
        messages = emptyList()

        if (
            branch.isBlank() ||
            groupKey.isBlank()
        ) {
            isMessagesLoading = false

            onDispose { }
        } else {
            isMessagesLoading = true

            val registration =
                db.collection("branches")
                .document(branch)
                .collection("forumRooms")
                .document(forumRoomId)
                .collection("messages")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(200)
                .addSnapshotListener { snap, error ->

                    if (error != null) {
                        isMessagesLoading = false
                        messages = emptyList()

                        return@addSnapshotListener
                    }

                    val currentUid =
                        FirebaseAuth
                            .getInstance()
                            .currentUser
                            ?.uid

                    val uiList = snap?.documents
                        ?.mapNotNull { doc ->
                            val rawTs = doc.getTimestamp("createdAt")
                            val instant = rawTs
                                ?.toDate()
                                ?.toInstant()
                                ?.toKotlinInstant()
                                ?: return@mapNotNull null

                            // 👇 שם השולח – מנסה כמה שדות: authorName / fullName / name / displayName
                            val authorNameDoc =
                                doc.getString("authorName")
                                    ?: doc.getString("fullName")
                                    ?: doc.getString("name")
                                    ?: doc.getString("displayName")
                                    ?: ""

                            val authorEmailDoc = doc.getString("authorEmail") ?: ""
                            val authorUidDoc = doc.getString("authorUid")

                            ForumUiMessage(
                                id = doc.id,
                                messageId = doc.getString("messageId") ?: doc.id,
                                branch = doc.getString("branch") ?: branch,
                                groupKey = doc.getString("groupKey") ?: groupKey,
                                authorName = authorNameDoc,
                                authorEmail = authorEmailDoc,
                                authorUid = authorUidDoc,
                                text = doc.getString("text") ?: "",
                                createdAt = instant,
                                createdAtMillis = doc.getLong("createdAtMillis")
                                    ?: instant.toEpochMilliseconds(),
                                updatedAtMillis = doc.getLong("updatedAtMillis"),
                                mediaUrl = doc.getString("mediaUrl"),
                                mediaType = doc.getString("mediaType"),
                                isMine = (authorUidDoc != null && authorUidDoc == currentUid)
                            )
                        }
                        ?: emptyList()

                    messages = uiList
                    isMessagesLoading = false

                    scope.launch {
                        if (
                            !pendingPushHandled &&
                            pendingPushMessageId.isNotBlank() &&
                            uiList.isNotEmpty()
                        ) {
                            val targetIndex = uiList.indexOfFirst { msg ->
                                msg.id == pendingPushMessageId ||
                                        msg.messageId == pendingPushMessageId
                            }

                            if (targetIndex >= 0) {
                                listState.animateScrollToItem(targetIndex)
                                pendingPushHandled = true

                                sp.edit {
                                    putBoolean(
                                        "forum_open_from_push",
                                        false
                                    )
                                    remove("forum_push_message_id")
                                    remove("forum_push_room_id")
                                    remove("forum_push_room_name")
                                    remove("forum_push_branch_id")
                                    remove("forum_push_group_key")
                                    remove("forum_push_sender_id")
                                    remove("forum_push_received_at")
                                }

                                pendingPushMessageId = ""
                                pendingPushRoomId = ""
                            } else {
                                listState.animateScrollToItem(0)
                            }
                        } else if (uiList.isNotEmpty()) {
                            listState.animateScrollToItem(0)
                        }
                    }
                }

            onDispose {
                registration.remove()
                messages = emptyList()
                isMessagesLoading = false
            }
        }
    }

    // ================== משתתפים בפורום — משתמשים אמיתיים מ-Firestore ==================
    // מסך אמת: אין שימוש ב-DemoTrainees.
    // חשוב: משתמשים יכולים לשמור סניף ב-branch / branches / branchesCsv,
    // וגם עם סוגי מקפים שונים. לכן לא מספיק whereEqualTo("branch", branch).
    LaunchedEffect(branch, groupKey, fullName, email) {
        // ✅ מיד עם החלפת חדר — מנקים נתונים ישנים ומציגים טעינה
        participantsByUsers = emptyList()
        isParticipantsLoading = true
        isParticipantsExpanded = false

        if (branch.isBlank() || groupKey.isBlank()) {
            isParticipantsLoading = false
            return@LaunchedEffect
        }

        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        val currentEmail = email.trim()
        val currentName = fullName.trim()

        fun String.normForum(): String {
            val t = trim()
            val sb = StringBuilder(t.length)
            var lastWasWs = false

            for (ch0 in t) {
                val ch = when (ch0) {
                    '-', '–', '—', '־' -> '-'
                    else -> ch0
                }

                val ws = ch.isWhitespace()
                if (ws) {
                    if (!lastWasWs) sb.append(' ')
                } else {
                    sb.append(ch)
                }
                lastWasWs = ws
            }

            return sb.toString().trim()
        }

        fun String.swapDash(to: Char): String = buildString(length) {
            for (ch in this@swapDash) {
                append(
                    when (ch) {
                        '-', '–', '—', '־' -> to
                        else -> ch
                    }
                )
            }
        }

        fun splitTokensNorm(raw: String?): List<String> {
            if (raw.isNullOrBlank()) return emptyList()

            return raw
                .replace(" • ", ",")
                .replace("|", ",")
                .replace("\n", ",")
                .split(',', ';', '；')
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .map { it.normForum() }
        }

        fun DocumentSnapshot.userNameOrNull(): String? {
            val full =
                getString("fullName")
                    ?: getString("name")
                    ?: getString("displayName")
                    ?: getString("email")

            return full?.trim()?.takeIf { it.isNotBlank() }
        }

        fun DocumentSnapshot.roleText(): String {
            return (getString("role")
                ?: getString("userType")
                ?: getString("type")
                ?: "")
                .trim()
                .lowercase()
        }

        fun DocumentSnapshot.isAllowedForumRole(): Boolean {
            val role = roleText()

            return role.isBlank() ||
                    role.contains("trainee") ||
                    role.contains("coach") ||
                    role.contains("trainer") ||
                    role.contains("instructor") ||
                    role.contains("מתאמן") ||
                    role.contains("מאמן")
        }

        fun DocumentSnapshot.branchTokensNorm(): List<String> {
            val out = mutableListOf<String>()

            val branchesList = (get("branches") as? List<*>)
                ?.mapNotNull { it?.toString()?.trim() }
                .orEmpty()

            out.addAll(branchesList.map { it.normForum() })
            out.addAll(splitTokensNorm(getString("branchesCsv")))
            out.addAll(splitTokensNorm(getString("branch")))
            out.addAll(splitTokensNorm(getString("activeBranch")))
            out.addAll(splitTokensNorm(getString("active_branch")))

            return out
                .filter { it.isNotBlank() }
                .distinct()
        }

        fun expandForumGroupAliases(raw: String): List<String> {
            val n = raw.normForum()

            return buildList {
                add(n)
                addAll(splitTokensNorm(n))

                if (n.contains("נוער") && n.contains("בוגרים")) {
                    add("נוער")
                    add("בוגרים")
                    add("נוער ובוגרים")
                    add("נוער + בוגרים")
                }

                if (n.contains("children", ignoreCase = true)) add("ילדים")
                if (n.contains("kids", ignoreCase = true)) add("ילדים")
                if (n.contains("youth", ignoreCase = true)) add("נוער")
                if (n.contains("adults", ignoreCase = true)) add("בוגרים")
                if (n.contains("adult", ignoreCase = true)) add("בוגרים")
            }
                .map { it.normForum() }
                .filter { it.isNotBlank() }
                .distinct()
        }

        fun DocumentSnapshot.groupTokensNorm(): List<String> {
            val groupsList =
                (get("groups") as? List<*>)
                    ?.mapNotNull {
                        it?.toString()?.trim()
                    }
                    ?.flatMap {
                        expandForumGroupAliases(it)
                    }
                    .orEmpty()

            return buildList {
                addAll(groupsList)

                addAll(
                    splitTokensNorm(
                        getString("primaryGroup")
                    ).flatMap {
                        expandForumGroupAliases(it)
                    }
                )

                addAll(
                    splitTokensNorm(
                        getString("groupKey")
                    ).flatMap {
                        expandForumGroupAliases(it)
                    }
                )

                addAll(
                    splitTokensNorm(
                        getString("group_key")
                    ).flatMap {
                        expandForumGroupAliases(it)
                    }
                )

                addAll(
                    splitTokensNorm(
                        getString("group")
                    ).flatMap {
                        expandForumGroupAliases(it)
                    }
                )

                addAll(
                    splitTokensNorm(
                        getString("groupName")
                    ).flatMap {
                        expandForumGroupAliases(it)
                    }
                )

                addAll(
                    splitTokensNorm(
                        getString("groupsCsv")
                    ).flatMap {
                        expandForumGroupAliases(it)
                    }
                )

                addAll(
                    splitTokensNorm(
                        getString("groupCsv")
                    ).flatMap {
                        expandForumGroupAliases(it)
                    }
                )

                addAll(
                    splitTokensNorm(
                        getString("age_group")
                    ).flatMap {
                        expandForumGroupAliases(it)
                    }
                )
            }
                .filter { it.isNotBlank() }
                .distinct()
        }

        /*
         * null פירושו שלמשתמש אין עדיין את המבנה החדש,
         * ולכן צריך להשתמש בשדות הישנים.
         *
         * true/false פירושם שהמבנה החדש קיים והוא
         * מקור האמת היחיד עבור השיוך.
         */
        fun DocumentSnapshot
                .matchesNewForumAssignment(
            branchCandidates: Set<String>,
            groupCandidates: Set<String>
        ): Boolean? {

            val rawAssignments =
                get("coachBranchAssignments")
                        as? List<*>
                    ?: return null

            if (rawAssignments.isEmpty()) {
                return null
            }

            return rawAssignments.any {
                    rawAssignment ->

                val assignmentMap =
                    rawAssignment as? Map<*, *>
                        ?: return@any false

                val assignmentBranch =
                    assignmentMap["branch"]
                        ?.toString()
                        ?.normForum()
                        .orEmpty()

                val assignmentGroups =
                    (
                            assignmentMap["groups"]
                                    as? List<*>
                            )
                        ?.asSequence()
                        ?.mapNotNull {
                            it?.toString()
                        }
                        ?.flatMap {
                            expandForumGroupAliases(it)
                                .asSequence()
                        }
                        ?.map {
                            it.normForum()
                        }
                        ?.filter {
                            it.isNotBlank()
                        }
                        ?.toSet()
                        .orEmpty()

                val branchMatches =
                    assignmentBranch.isNotBlank() &&
                            branchCandidates.any {
                                    candidate ->

                                candidate == assignmentBranch
                            }

                val groupMatches =
                    groupCandidates.isEmpty() ||
                            assignmentGroups.any {
                                it in groupCandidates
                            }

                branchMatches && groupMatches
            }
        }

        fun matchesForumGroup(
            tokens: List<String>,
            candidates: Set<String>
        ): Boolean {
            if (candidates.isEmpty()) return true

            // אם למשתמש אין שדה קבוצה בכלל, לא נכניס אותו לחדר קבוצה ספציפי.
            if (tokens.isEmpty()) return false

            return tokens.any { tok ->
                tok in candidates ||
                        candidates.any { cand ->
                            cand.length >= 2 &&
                                    tok.length >= 2 &&
                                    (tok.contains(cand) || cand.contains(tok))
                        }
            }
        }

        fun matchesBranch(tokens: List<String>, candidates: Set<String>): Boolean {
            if (tokens.isEmpty() || candidates.isEmpty()) return false

            return tokens.any { tok ->
                tok in candidates ||
                        candidates.any { cand ->
                            cand.length >= 4 &&
                                    tok.length >= 4 &&
                                    (tok.contains(cand) || cand.contains(tok))
                        }
            }
        }

        suspend fun fetchUsersFor(branchValue: String): List<DocumentSnapshot> {
            val col = db.collection("users")
            val out = mutableListOf<DocumentSnapshot>()

            runCatching {
                out.addAll(
                    col.whereArrayContains("branches", branchValue)
                        .get()
                        .await()
                        .documents
                )
            }

            runCatching {
                out.addAll(
                    col.whereEqualTo("branchesCsv", branchValue)
                        .get()
                        .await()
                        .documents
                )
            }

            runCatching {
                out.addAll(
                    col.whereEqualTo("branch", branchValue)
                        .get()
                        .await()
                        .documents
                )
            }

            runCatching {
                out.addAll(
                    col.whereEqualTo("activeBranch", branchValue)
                        .get()
                        .await()
                        .documents
                )
            }

            runCatching {
                out.addAll(
                    col.whereEqualTo("active_branch", branchValue)
                        .get()
                        .await()
                        .documents
                )
            }

            return out
        }

        val branchCandidates = listOf(
            branch,
            branch.swapDash('-'),
            branch.swapDash('–'),
            branch.swapDash('—'),
            branch.swapDash('־'),
            branch.replace("  ", " ")
        ).map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        val groupCandidates = expandForumGroupAliases(groupKey)
            .flatMap { g ->
                listOf(
                    g,
                    g.replace("-", "–"),
                    g.replace("–", "-"),
                    g.replace(" + ", " ו"),
                    g.replace(" ו", " + ")
                )
            }
            .map { it.normForum() }
            .filter { it.isNotBlank() }
            .distinct()

        scope.launch {
            try {
                var docs = branchCandidates
                    .flatMap { cand -> fetchUsersFor(cand) }
                    .distinctBy { it.id }

                if (docs.isEmpty()) {
                    val all = mutableListOf<DocumentSnapshot>()
                    val col = db.collection("users")

                    var last: DocumentSnapshot? = null

                    while (true) {
                        var q = col
                            .orderBy(FieldPath.documentId())
                            .limit(1000)

                        last?.let { lastDocument ->
                            q = q.startAfter(
                                lastDocument
                            )
                        }

                        val snap = q.get().await()
                        val page = snap.documents

                        if (page.isEmpty()) break

                        all.addAll(page)
                        last = page.last()

                        if (all.size >= 5000) break
                    }

                    val candNorm = branchCandidates.map { it.normForum() }.toSet()

                    val groupNorm = groupCandidates.toSet()

                    docs =
                        all.filter { doc ->
                            doc.matchesNewForumAssignment(
                                branchCandidates = candNorm,
                                groupCandidates = groupNorm
                            )
                                ?: (
                                        matchesBranch(
                                            doc.branchTokensNorm(),
                                            candNorm
                                        ) &&
                                                matchesForumGroup(
                                                    doc.groupTokensNorm(),
                                                    groupNorm
                                                )
                                        )
                        }
                            .distinctBy {
                                it.id
                            }
                }

                fun normalizeParticipantName(value: String): String {
                    return value
                        .trim()
                        .lowercase()
                        .replace("‏", "")
                        .replace("יובל פולק", "יובל פולק")
                        .replace(Regex("\\s+"), " ")
                }

                fun DocumentSnapshot.participantUniqueKey(): String {
                    val docUid = getString("uid")
                        ?: getString("authUid")
                        ?: ""

                    val docEmail = (
                            getString("email")
                                ?: getString("emailLower")
                                ?: getString("userEmail")
                                ?: getString("user_email")
                                ?: ""
                            )
                        .trim()
                        .lowercase()

                    val docPhone = (
                            getString("phone")
                                ?: getString("phoneNumber")
                                ?: getString("phone_number")
                                ?: getString("mobile")
                                ?: ""
                            )
                        .filter { it.isDigit() }
                        .let { digits ->
                            when {
                                digits.startsWith("972") && digits.length >= 11 -> "0" + digits.drop(3)
                                digits.startsWith("05") -> digits
                                digits.length == 9 && digits.startsWith("5") -> "0$digits"
                                else -> digits
                            }
                        }

                    val docName = userNameOrNull().orEmpty()

                    return when {
                        docEmail.isNotBlank() -> "email:$docEmail"
                        docPhone.isNotBlank() -> "phone:$docPhone"
                        docUid.isNotBlank() -> "uid:${docUid.trim()}"
                        docName.isNotBlank() -> "name:${normalizeParticipantName(docName)}"
                        else -> "doc:${id}"
                    }
                }

                val groupNorm = groupCandidates.toSet()

                val branchNorm =
                    branchCandidates
                        .map {
                            it.normForum()
                        }
                        .toSet()

                val realParticipants =
                    docs
                        .asSequence()
                        .filter {
                            it.isAllowedForumRole()
                        }
                        .filter {
                            it.userNameOrNull()
                                ?.isNotBlank() == true
                        }
                        .filter { doc ->
                            doc.matchesNewForumAssignment(
                                branchCandidates =
                                    branchNorm,
                                groupCandidates =
                                    groupNorm
                            )
                                ?: (
                                        matchesBranch(
                                            doc.branchTokensNorm(),
                                            branchNorm
                                        ) &&
                                                matchesForumGroup(
                                                    doc.groupTokensNorm(),
                                                    groupNorm
                                                )
                                        )
                        }
                        .groupBy {
                            it.participantUniqueKey()
                        }
                    .values
                    .mapNotNull { samePersonDocs ->
                        val doc = samePersonDocs.firstOrNull() ?: return@mapNotNull null
                        val cleanName = doc.userNameOrNull() ?: return@mapNotNull null

                        val docEmail = doc.getString("email").orEmpty().trim()
                        val docUid =
                            doc.getString("uid")
                                ?: doc.getString("authUid")
                                ?: doc.id

                        ForumParticipantUi(
                            id = docUid.ifBlank { doc.id },
                            name = cleanName,
                            isMe = (
                                    (currentUid != null && docUid == currentUid) ||
                                            (currentEmail.isNotBlank() && docEmail == currentEmail) ||
                                            (currentName.isNotBlank() && cleanName.trim() == currentName)
                                    )
                        )
                    }
                    .distinctBy {
                        normalizeParticipantName(it.name)
                    }
                    .sortedBy { it.name }
                    .toList()

                participantsByUsers = realParticipants
                isParticipantsLoading = false
            } catch (_: Exception) {
                participantsByUsers = emptyList()
                isParticipantsLoading = false
            }
        }
    }

    // ---------- בוררי מדיה ----------
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            attachedUri = uri
            attachedMediaType = "image"
        }
    }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            attachedUri = uri
            attachedMediaType = "video"
        }
    }

    val currentFirebaseUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    val isCurrentUserForumParticipant = remember(
        participantsByUsers,
        currentFirebaseUid,
        email,
        fullName,
        isManagerOverride
    ) {
        if (isManagerOverride) {
            true
        } else {
            participantsByUsers.any { p ->
                p.isMe ||
                        (currentFirebaseUid.isNotBlank() && p.id == currentFirebaseUid) ||
                        (email.isNotBlank() && p.id.equals(email, ignoreCase = true)) ||
                        (fullName.isNotBlank() && p.name.trim() == fullName.trim())
            }
        }
    }

    // ---------- שליחת/עדכון הודעה ----------
    suspend fun sendMessageInternal() {
        try {
            val text = (if (editingMessage != null) editText else input).trim()
            val auth = FirebaseAuth.getInstance()
            val currentUser = auth.currentUser
            val currentUid = currentUser?.uid

            if (text.isEmpty() && attachedUri == null) return
            if (branch.isBlank() || groupKey.isBlank()) return

            if (currentUser == null || currentUid.isNullOrBlank()) {
                Toast.makeText(
                    ctx,
                    forumTr(
                        isEnglish,
                        "לא ניתן לשלוח הודעה לפני התחברות משתמש.",
                        "You must be signed in before sending a message."
                    ),
                    Toast.LENGTH_LONG
                ).show()
                return
            }

            if (!isCurrentUserForumParticipant) {
                Toast.makeText(
                    ctx,
                    forumTr(
                        isEnglish,
                        "אין הרשאה לשלוח הודעות בחדר הקבוצה הזה.",
                        "You do not have permission to send messages in this group room."
                    ),
                    Toast.LENGTH_LONG
                ).show()
                return
            }

            // העלאת מדיה (אם יש)
            var mediaUrl: String? = null
            val mediaType = attachedMediaType

            if (attachedUri != null && mediaType != null) {
                val path =
                    "forum_media/${forumSafeDocId(branch)}/${forumSafeDocId(groupKey)}/$currentUid/${System.currentTimeMillis()}"
                val ref = storage.reference.child(path)
                ref.putFile(attachedUri!!).await()
                mediaUrl = ref.downloadUrl.await().toString()
            }

            val messagePreview = when {
                text.isNotBlank() -> text.take(120)
                mediaType == "image" -> forumTr(isEnglish, "תמונה חדשה", "New image")
                mediaType == "video" -> forumTr(isEnglish, "סרטון חדש", "New video")
                else -> forumTr(isEnglish, "הודעה חדשה", "New message")
            }

            // דאטה בסיסי להודעה
            val safeAuthorName = fullName
                .ifBlank { userSp.getString("displayName", "").orEmpty() }
                .ifBlank { userSp.getString("name", "").orEmpty() }
                .ifBlank { email }
                .ifBlank { forumTr(isEnglish, "משתתף", "Participant") }

            // מבטיח שחדר הפורום קיים כמסמך אמת בשרת.
            // חשוב:
            // אם חוקי Firestore לא מאפשרים למשתמש רגיל לעדכן את מסמך החדר,
            // לא נפיל את שליחת ההודעה. ננסה לעדכן את החדר, ואם אין הרשאה —
            // נמשיך לשמירת ההודעה עצמה.
            val roomRef = db.collection("branches")
                .document(branch)
                .collection("forumRooms")
                .document(forumRoomId)

            val canWriteRoomMetadata = runCatching {
                roomRef.set(
                    mapOf(
                        "roomId" to forumRoomId,
                        "branch" to branch,
                        "groupKey" to groupKey,
                        "participantCount" to participantsByUsers.size,
                        "participantIds" to participantsByUsers.map { it.id }.take(200),
                        "participantNames" to participantsByUsers.map { it.name }.take(200),
                        "participantSource" to "users_by_branch_and_group",
                        "pushEnabled" to true,
                        "pushTarget" to "forum_room_participants",
                        "updatedAt" to FieldValue.serverTimestamp(),
                        "updatedAtMillis" to System.currentTimeMillis(),
                        "lastMessagePreview" to messagePreview,
                        "lastMessageAuthorUid" to currentUid,
                        "lastMessageAuthorName" to safeAuthorName,
                        "source" to "android_forum"
                    ),
                    SetOptions.merge()
                ).await()

                true
            }.getOrElse { error ->
                if (
                    error is FirebaseFirestoreException &&
                    error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
                ) {
                    false
                } else {
                    throw error
                }
            }

            val expiresAtDate = Date(
                System.currentTimeMillis() + FORUM_MESSAGE_RETENTION_MILLIS
            )

            val baseData = mutableMapOf<String, Any?>(
                "roomId" to forumRoomId,
                "branch" to branch,
                "groupKey" to groupKey,
                "authorName" to safeAuthorName,
                "authorEmail" to email,
                "authorUid" to currentUid,
                "authorIsManager" to isManagerOverride,
                "text" to text,
                "messagePreview" to messagePreview,
                "hasMedia" to (mediaUrl != null),
                "mediaType" to mediaType,
                "expiresAt" to com.google.firebase.Timestamp(expiresAtDate),
                "retentionDays" to FORUM_MESSAGE_RETENTION_DAYS,
                "isPinned" to false,
                "pushStatus" to "pending",
                "pushCreatedBy" to "android_forum",
                "source" to "android_forum"
            )

            if (mediaUrl != null) {
                baseData["mediaUrl"] = mediaUrl
                baseData["mediaType"] = mediaType
            }

            if (editingMessage == null) {
                // הודעה חדשה — מוגדרת למחיקה אוטומטית אחרי 90 יום דרך Firestore TTL
                val nowMillis = System.currentTimeMillis()
                val messageRef = db.collection("branches")
                    .document(branch)
                    .collection("forumRooms")
                    .document(forumRoomId)
                    .collection("messages")
                    .document()

                baseData["messageId"] = messageRef.id
                baseData["createdAt"] = FieldValue.serverTimestamp()
                baseData["createdAtMillis"] = nowMillis
                baseData["updatedAtMillis"] = nowMillis

                messageRef
                    .set(baseData.filterValues { it != null }, SetOptions.merge())
                    .await()

                if (canWriteRoomMetadata) {
                    runCatching {
                        roomRef.set(
                            mapOf(
                                "lastMessageId" to messageRef.id,
                                "lastMessagePreview" to messagePreview,
                                "lastMessageAuthorUid" to currentUid,
                                "lastMessageAuthorName" to safeAuthorName,
                                "lastMessageAt" to FieldValue.serverTimestamp(),
                                "lastMessageAtMillis" to nowMillis,
                                "lastMessageHasMedia" to (mediaUrl != null),
                                "lastMessageMediaType" to mediaType,
                                "pendingPushMessageId" to messageRef.id,
                                "pendingPushAuthorUid" to currentUid,
                                "pendingPushPreview" to messagePreview,
                                "pendingPushAt" to FieldValue.serverTimestamp(),
                                "pendingPushAtMillis" to nowMillis,
                                "updatedAt" to FieldValue.serverTimestamp(),
                                "updatedAtMillis" to nowMillis
                            ),
                            SetOptions.merge()
                        ).await()
                    }
                }
            } else {
                val msg = editingMessage ?: return
                val canEditThisMessage = msg.isMine || isManagerOverride

                if (!canEditThisMessage) {
                    Toast.makeText(
                        ctx,
                        forumTr(
                            isEnglish,
                            "אין הרשאה לערוך הודעה זו.",
                            "You do not have permission to edit this message."
                        ),
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }

                val nowMillis = System.currentTimeMillis()

                // עדכון הודעה קיימת — לא מאריכים את expiresAt בעריכה
                baseData.remove("expiresAt")
                baseData.remove("retentionDays")
                baseData.remove("isPinned")
                baseData["messageId"] = msg.messageId
                baseData["updatedAt"] = FieldValue.serverTimestamp()
                baseData["updatedAtMillis"] = nowMillis
                baseData["edited"] = true

                db.collection("branches")
                    .document(branch)
                    .collection("forumRooms")
                    .document(forumRoomId)
                    .collection("messages")
                    .document(msg.id)
                    .set(
                        baseData.filterValues { it != null },
                        SetOptions.merge()
                    )
                    .await()

                if (canWriteRoomMetadata) {
                    runCatching {
                        roomRef.set(
                            mapOf(
                                "lastMessagePreview" to messagePreview,
                                "lastMessageAuthorUid" to currentUid,
                                "lastMessageAuthorName" to safeAuthorName,
                                "lastMessageEditedAt" to FieldValue.serverTimestamp(),
                                "lastMessageEditedAtMillis" to nowMillis,
                                "updatedAt" to FieldValue.serverTimestamp(),
                                "updatedAtMillis" to nowMillis
                            ),
                            SetOptions.merge()
                        ).await()
                    }
                }
            }

            // ניקוי מצב אחרי שליחה / עדכון
            input = ""
            editText = ""
            editingMessage = null
            attachedUri = null
            attachedMediaType = null

            // ✅ אחרי שליחה סוגרים מקלדת ומחזירים שדה ראייה למסך
            focusManager.clearFocus(force = true)
            keyboardController?.hide()

        } catch (_: Exception) {
            Toast.makeText(
                ctx,
                forumTr(
                    isEnglish,
                    "לא ניתן לשמור את ההודעה כרגע. בדוק את החיבור ונסה שוב.",
                    "The message cannot be saved right now. Check your connection and try again."
                ),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun formatInstant(instant: Instant): String {
        val df = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
        val date = Date(instant.toEpochMilliseconds())
        return df.format(date)
    }

    val screenBackgroundBrush =
        kmiScreenBackgroundBrush()

    val forumHeaderColor =
        MaterialTheme.colorScheme.surface.copy(
            alpha = 0.94f
        )

    val forumHeaderBorder =
        MaterialTheme.colorScheme.outline.copy(
            alpha = 0.72f
        )

    val participantsText =
        MaterialTheme.colorScheme.onSurfaceVariant

    val myBubbleColor =
        MaterialTheme.colorScheme.primaryContainer

    val otherBubbleColor =
        MaterialTheme.colorScheme.surface.copy(
            alpha = 0.96f
        )

    val myBubbleText =
        MaterialTheme.colorScheme.onPrimaryContainer

    val otherBubbleText =
        MaterialTheme.colorScheme.onSurface

    val inputSurfaceColor =
        MaterialTheme.colorScheme.surface

    val inputTextColor =
        MaterialTheme.colorScheme.onSurface

    val inputPlaceholderColor =
        MaterialTheme.colorScheme.onSurfaceVariant.copy(
            alpha = 0.76f
        )

    val inputIconTint =
        MaterialTheme.colorScheme.onSurfaceVariant

    val attachmentChipColor =
        MaterialTheme.colorScheme.surfaceVariant.copy(
            alpha = 0.94f
        )

    val attachmentChipText =
        MaterialTheme.colorScheme.onSurfaceVariant

    Scaffold(
        topBar = {
            il.kmi.app.ui.KmiTopBar(
                title =
                    forumTr(
                        isEnglish,
                        "פורום הסניף",
                        "Branch Forum"
                    ),
                onBack = onBack,
                onOpenExercise = onOpenExercise,
                onHome = onGoHome,
                onSearch = { },
                showTopHome = false,
                showTopSearch = false,
                lockSearch = false,
                showBottomActions = true,
                showBottomShare = true,
                onShare = {
                    runCatching {
                        val pdfFile =
                            createForumPdf(
                                context = ctx,
                                branch = branch,
                                groupKey = groupKey,
                                messages = messages,
                                participants = participantsByUsers,
                                isEnglish = isEnglish
                            )

                        shareForumPdf(
                            context = ctx,
                            pdfFile = pdfFile,
                            isEnglish = isEnglish
                        )
                    }.onFailure {
                        Toast.makeText(
                            ctx,
                            forumTr(
                                isEnglish,
                                "לא ניתן ליצור את קובץ ה־PDF",
                                "Unable to create the PDF file"
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                onOpenAi = { showAiDialog = true }
            )
        },
        containerColor = Color.Transparent,
        // ⬅️ רק סטטוס־בר וצדדים; בלי מרווח בתחתית מה-Scaffold
        contentWindowInsets = WindowInsets.systemBars.only(
            WindowInsetsSides.Top + WindowInsetsSides.Start + WindowInsetsSides.End
        )
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(screenBackgroundBrush)
                .padding(padding)
        ) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {

                // 🔒 קודם כל – נעילת מסך הפורום לפי מנוי / ניסיון
                if (!canUseExtras) {
                    val lockText =
                        if (isTrial) {
                            forumTr(
                                isEnglish,
                                "במהלך תקופת הניסיון מסך הפורום נעול.\nאחרי רכישת מנוי המסך ייפתח עבורך.",
                                "During the trial period, the forum is locked.\nAfter purchasing a subscription, this screen will be available."
                            )
                        } else {
                            forumTr(
                                isEnglish,
                                "מסך הפורום זמין למנויים בלבד.\nכדי להמשיך יש לרכוש מנוי פעיל.",
                                "The forum is available to subscribers only.\nTo continue, please purchase an active subscription."
                            )
                        }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            // ✅ מוריד את כרטיס הנעילה מעט למטה כדי שלא יתנגש
                            // עם הידית/סרגל האייקונים הנסתר של KmiTopBar.
                            .padding(top = 24.dp),
                        color =
                            MaterialTheme.colorScheme.surface.copy(
                                alpha = 0.96f
                            ),
                        shape = RoundedCornerShape(20.dp),
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                        border = BorderStroke(
                            width = 1.dp,
                            color =
                                MaterialTheme.colorScheme.outline.copy(
                                    alpha = 0.52f
                                )
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 26.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = RoundedCornerShape(22.dp),
                                color =
                                    MaterialTheme.colorScheme.primaryContainer.copy(
                                        alpha = 0.72f
                                    ),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color =
                                        MaterialTheme.colorScheme.primary.copy(
                                            alpha = 0.28f
                                        )
                                ),
                                modifier = Modifier.size(
                                    scaledIconSize(64.dp)
                                )
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Lock,
                                        contentDescription = null,
                                        tint =
                                            MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(
                                            scaledIconSize(30.dp)
                                        )
                                    )
                                }
                            }

                            Spacer(Modifier.height(14.dp))

                            Text(
                                text = forumTr(
                                    isEnglish,
                                    "גישה לפורום",
                                    "Forum Access"
                                ),
                                color =
                                    MaterialTheme.colorScheme.primary,
                                style = KmiTypography.sectionTitle,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(Modifier.height(12.dp))

                            Text(
                                text = lockText,
                                color =
                                    MaterialTheme.colorScheme.onSurface,
                                style = KmiTypography.body,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(Modifier.height(18.dp))

                            Button(
                                onClick = onOpenSubscription,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Text(
                                    text = forumTr(
                                        isEnglish,
                                        "עבור למסך המנוי",
                                        "Go to Subscription"
                                    ),
                                    style = KmiTypography.action
                                )
                            }

                            Spacer(Modifier.height(12.dp))

                            Text(
                                text = forumTr(
                                    isEnglish,
                                    "ניתן לחזור תמיד למסך זה לאחר רכישת מנוי.",
                                    "You can always return to this screen after purchasing a subscription."
                                ),
                                color =
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                style = KmiTypography.caption,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // לא מציירים שורת כתיבה כשאין גישה
                    return@Column
                }

                // רק אם יש גישה – בודקים שהמשתמש משויך לסניף/קבוצה
                if (branch.isBlank() || groupKey.isBlank()) {
                    Text(
                        text = forumTr(
                            isEnglish,
                            "לא אותרו סניף/קבוצה במשתמש.\nודאו ש־\"branch\" ו־\"groupKey\" מוגדרים בפרופיל.",
                            "No branch/group was found for this user.\nPlease make sure \"branch\" and \"groupKey\" are set in the profile."
                        ),
                        color =
                            MaterialTheme.colorScheme.error,
                        style = KmiTypography.body,
                        textAlign = screenTextAlign,
                        modifier = Modifier.fillMaxWidth()
                    )
                    return@Column
                }

                // ===== כרטיס שליטה עליון מאוחד: חדר פורום + משתתפים =====
                val participants = participantsByUsers

                if (isForumControlsCollapsed) {
                    ForumControlsMiniHandle(
                        isDarkMode = isDarkMode,
                        isEnglish = isEnglish,
                        text = if (selectedForumBranch.isNotBlank() || selectedForumGroup.isNotBlank()) {
                            "${selectedForumBranch.ifBlank { "—" }} • ${selectedForumGroup.ifBlank { "—" }}"
                        } else {
                            forumTr(
                                isEnglish,
                                "לחץ לבחירת חדר הפורום ומשתתפים",
                                "Tap to choose forum room and participants"
                            )
                        },
                        onClick = {
                            isForumControlsCollapsed = false
                            isRoomPickerExpanded = true
                            isParticipantsExpanded = false
                        }
                    )

                    Spacer(Modifier.height(5.dp))
                } else {
                    ForumPremiumControlCard(
                        branches = availableForumBranches,
                        groups = availableForumGroups,
                        selectedBranch = selectedForumBranch,
                        selectedGroup = selectedForumGroup,
                        participants = participants,
                        participantsText = participantsText,
                        roomTitle = forumTr(isEnglish, "בחירת חדר פורום", "Forum room"),
                        roomSubtitle = forumTr(
                            isEnglish,
                            "${selectedForumBranch.ifBlank { "—" }} • ${selectedForumGroup.ifBlank { "—" }}",
                            "${selectedForumBranch.ifBlank { "—" }} • ${selectedForumGroup.ifBlank { "—" }}"
                        ),
                        participantsTitle = when {
                            isParticipantsLoading -> forumTr(
                                isEnglish,
                                "טוען משתתפים...",
                                "Loading participants..."
                            )

                            participants.isNotEmpty() -> forumTr(
                                isEnglish,
                                "משתתפים בפורום (${participants.size})",
                                "Forum participants (${participants.size})"
                            )

                            else -> forumTr(
                                isEnglish,
                                "משתתפים בפורום",
                                "Forum participants"
                            )
                        },
                        participantsSubtitle = when {
                            isParticipantsLoading -> forumTr(
                                isEnglish,
                                "בודק מי רשום לחדר הזה",
                                "Checking who belongs to this room"
                            )

                            participants.isNotEmpty() -> ""

                            else -> forumTr(
                                isEnglish,
                                "אין משתתפים רשומים בקבוצה הזו עדיין",
                                "No registered participants in this group yet"
                            )
                        },
                        isRoomExpanded = isRoomPickerExpanded,
                        isParticipantsExpanded = isParticipantsExpanded,
                        canOpenRoomPicker = availableForumBranches.size > 1 || availableForumGroups.size > 1,
                        canOpenParticipants = !isParticipantsLoading && participants.isNotEmpty(),
                        isDarkMode = isDarkMode,
                        isEnglish = isEnglish,
                        onCollapseAll = {
                            isRoomPickerExpanded = false
                            isParticipantsExpanded = false
                            isForumControlsCollapsed = true
                        },
                        onRoomClick = {
                            if (availableForumBranches.size > 1 || availableForumGroups.size > 1) {
                                isRoomPickerExpanded = true
                                isParticipantsExpanded = false
                            }
                        },
                        onParticipantsClick = {
                            if (!isParticipantsLoading && participants.isNotEmpty()) {
                                isParticipantsExpanded = !isParticipantsExpanded
                                if (isParticipantsExpanded) {
                                    isRoomPickerExpanded = false
                                }
                            }
                        },
                        onBranchSelected = {
                            selectedForumBranch = it

                            isRoomPickerExpanded = false
                            isParticipantsExpanded = false
                            isForumControlsCollapsed = true
                        },
                        onGroupSelected = {
                            selectedForumGroup = it

                            isRoomPickerExpanded = false
                            isParticipantsExpanded = false
                            isForumControlsCollapsed = true
                        }
                    )

                    Spacer(Modifier.height(8.dp))
                }

                if (
                    isMessagesLoading ||
                    isParticipantsLoading
                ) {
                    Surface(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    bottom = 8.dp
                                ),
                        shape =
                            RoundedCornerShape(18.dp),
                        color = forumHeaderColor,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                        border =
                            BorderStroke(
                                width = 1.dp,
                                color = forumHeaderBorder
                            )
                    ) {
                        KmiLoadingRings(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = 16.dp,
                                        vertical = 16.dp
                                    ),
                            text =
                                if (isMessagesLoading) {
                                    forumTr(
                                        isEnglish,
                                        "טוען הודעות מהפורום...",
                                        "Loading forum messages..."
                                    )
                                } else {
                                    forumTr(
                                        isEnglish,
                                        "טוען את משתתפי הקבוצה...",
                                        "Loading group participants..."
                                    )
                                }
                        )
                    }
                }

                // ================= רשימת הודעות =================
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    reverseLayout = true,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (messages.isEmpty()) {
                        item(key = "empty_forum_room") {
                            EmptyForumRoomCard(
                                branch = branch,
                                groupKey = groupKey,
                                isEnglish = isEnglish
                            )
                        }
                    }

                    items(
                        items = messages,
                        key = { it.id }
                    ) { msg ->
                        val bubbleColor =
                            if (msg.isMine) myBubbleColor else otherBubbleColor
                        val textColor =
                            if (msg.isMine) myBubbleText else otherBubbleText

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp),
                            horizontalArrangement = if (msg.isMine) Arrangement.End else Arrangement.Start
                        ) {
                            Box {
                                Surface(
                                    color = bubbleColor,
                                    shape = RoundedCornerShape(
                                        topStart = 18.dp,
                                        topEnd = 18.dp,
                                        bottomStart = if (msg.isMine) 18.dp else 6.dp,
                                        bottomEnd = if (msg.isMine) 6.dp else 18.dp
                                    ),
                                    tonalElevation = 0.dp,
                                    shadowElevation = 0.dp
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .widthIn(max = 260.dp)
                                            .padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        val participantNameByUid = msg.authorUid
                                            ?.let { uid ->
                                                participantsByUsers.firstOrNull { it.id == uid }?.name
                                            }
                                            .orEmpty()

                                        val realMessageAuthorName =
                                            msg.authorName
                                                .ifBlank {
                                                    participantNameByUid
                                                }
                                                .ifBlank {
                                                    msg.authorEmail
                                                }

                                        val messageAuthorDemoIndex =
                                            participantsByUsers
                                                .indexOfFirst { participant ->
                                                    (
                                                            !msg.authorUid.isNullOrBlank() &&
                                                                    participant.id == msg.authorUid
                                                            ) ||
                                                            (
                                                                    msg.authorEmail.isNotBlank() &&
                                                                            participant.id.equals(
                                                                                msg.authorEmail,
                                                                                ignoreCase = true
                                                                            )
                                                                    ) ||
                                                            (
                                                                    realMessageAuthorName.isNotBlank() &&
                                                                            participant.name.trim().equals(
                                                                                realMessageAuthorName.trim(),
                                                                                ignoreCase = true
                                                                            )
                                                                    )
                                                }
                                                .takeIf { index ->
                                                    index >= 0
                                                }

                                        val messageAuthorName =
                                            forumDisplayPersonName(
                                                realName =
                                                    realMessageAuthorName,
                                                stableKey =
                                                    msg.authorUid
                                                        ?.takeIf {
                                                            it.isNotBlank()
                                                        }
                                                        ?: msg.authorEmail
                                                            .takeIf {
                                                                it.isNotBlank()
                                                            }
                                                        ?: msg.id,
                                                demoIndex =
                                                    messageAuthorDemoIndex,
                                                isEnglish =
                                                    isEnglish
                                            )

                                        Text(
                                            text = messageAuthorName,
                                            color = textColor.copy(alpha = 0.78f),
                                            style = KmiTypography.caption.copy(
                                                fontWeight = FontWeight.Black
                                            ),
                                            textAlign = screenTextAlign,
                                            maxLines = 1,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        if (msg.text.isNotBlank()) {
                                            Text(
                                                text = msg.text,
                                                color = textColor,
                                                style = KmiTypography.body.copy(
                                                    fontWeight = FontWeight.SemiBold
                                                ),
                                                textAlign = screenTextAlign,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }

                                        msg.mediaUrl?.let { url ->
                                            Spacer(Modifier.height(4.dp))
                                            when (msg.mediaType) {
                                                "image" -> {
                                                    Surface(
                                                        shape = RoundedCornerShape(14.dp),
                                                        color = Color.Black.copy(alpha = 0.16f)
                                                    ) {
                                                        AsyncImage(
                                                            model = url,
                                                            contentDescription = forumTr(isEnglish, "תמונה מצורפת", "Attached image"),
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .heightIn(min = 120.dp, max = 220.dp)
                                                        )
                                                    }
                                                }

                                                "video" -> {
                                                    val context = LocalContext.current
                                                    Surface(
                                                        shape = RoundedCornerShape(14.dp),
                                                        color = Color.Black.copy(alpha = 0.28f),
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(108.dp)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Column(
                                                                modifier = Modifier.weight(1f),
                                                                horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
                                                            ) {
                                                                Text(
                                                                    text = forumTr(
                                                                        isEnglish,
                                                                        "סרטון מצורף",
                                                                        "Attached video"
                                                                    ),
                                                                    color = Color.White,
                                                                    style = KmiTypography.body.copy(
                                                                        fontWeight = FontWeight.SemiBold
                                                                    ),
                                                                    textAlign = screenTextAlign,
                                                                    modifier = Modifier.fillMaxWidth()
                                                                )

                                                                Text(
                                                                    text = forumTr(
                                                                        isEnglish,
                                                                        "לחיצה לפתיחה בנגן",
                                                                        "Tap to open in player"
                                                                    ),
                                                                    color = Color.White.copy(
                                                                        alpha = 0.78f
                                                                    ),
                                                                    style = KmiTypography.caption,
                                                                    textAlign = screenTextAlign,
                                                                    modifier = Modifier.fillMaxWidth()
                                                                )
                                                            }
                                                            FilledTonalButton(
                                                                onClick = {
                                                                    val videoUri =
                                                                        url.toUri()

                                                                    val intent =
                                                                        Intent(
                                                                            Intent.ACTION_VIEW,
                                                                            videoUri
                                                                        ).apply {
                                                                            setDataAndType(
                                                                                videoUri,
                                                                                "video/*"
                                                                            )
                                                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                                    }
                                                                    context.startActivity(intent)
                                                                }
                                                            ) {
                                                                Icon(
                                                                    imageVector =
                                                                        Icons.Filled.VideoLibrary,
                                                                    contentDescription = null,
                                                                    modifier = Modifier.size(
                                                                        KmiIconSize.medium
                                                                    )
                                                                )

                                                                Spacer(Modifier.width(6.dp))

                                                                Text(
                                                                    text = forumTr(
                                                                        isEnglish,
                                                                        "פתח",
                                                                        "Open"
                                                                    ),
                                                                    style = KmiTypography.action
                                                                )
                                                            }
                                                        }
                                                    }
                                                }

                                                else -> {}
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = if (isEnglish) {
                                                Arrangement.End
                                            } else {
                                                Arrangement.Start
                                            },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val canModifyMessage = msg.isMine || isManagerOverride

                                            if (canModifyMessage) {
                                                IconButton(
                                                    onClick = {
                                                        editingMessage = msg
                                                        editText = msg.text
                                                        input = ""
                                                        attachedUri = null
                                                        attachedMediaType = null
                                                    },
                                                    modifier = Modifier.size(scaledIconSize(18.dp))
                                                ) {
                                                    Icon(
                                                        Icons.Filled.Edit,
                                                        contentDescription = forumTr(isEnglish, "עריכת הודעה", "Edit message"),
                                                        tint = textColor.copy(alpha = 0.72f)
                                                    )
                                                }
                                                Spacer(Modifier.width(2.dp))
                                                IconButton(
                                                    onClick = {
                                                        scope.launch {
                                                            val roomIdForMessage = msg.groupKey
                                                                .takeIf { it.isNotBlank() }
                                                                ?.let { forumRoomDocId(msg.branch, it) }
                                                                ?: forumRoomId

                                                            db.collection("branches")
                                                                .document(msg.branch)
                                                                .collection("forumRooms")
                                                                .document(roomIdForMessage)
                                                                .collection("messages")
                                                                .document(msg.id)
                                                                .delete()
                                                                .await()

                                                            val deleteAtMillis = System.currentTimeMillis()

                                                            db.collection("branches")
                                                                .document(msg.branch)
                                                                .collection("forumRooms")
                                                                .document(roomIdForMessage)
                                                                .set(
                                                                    mapOf(
                                                                        "updatedAt" to FieldValue.serverTimestamp(),
                                                                        "updatedAtMillis" to deleteAtMillis,
                                                                        "lastModerationAction" to "message_deleted",
                                                                        "lastModerationByUid" to FirebaseAuth.getInstance().currentUser?.uid.orEmpty(),
                                                                        "lastDeletedMessageId" to msg.messageId,
                                                                        "lastDeletedAt" to FieldValue.serverTimestamp(),
                                                                        "lastDeletedAtMillis" to deleteAtMillis
                                                                    ),
                                                                    SetOptions.merge()
                                                                )
                                                                .await()
                                                        }
                                                    },
                                                    modifier = Modifier.size(scaledIconSize(18.dp))
                                                ) {
                                                    Icon(
                                                        Icons.Filled.Delete,
                                                        contentDescription = forumTr(isEnglish, "מחיקת הודעה", "Delete message"),
                                                        tint = textColor.copy(alpha = 0.72f)
                                                    )
                                                }
                                                Spacer(Modifier.width(4.dp))
                                            }

                                            Text(
                                                text = formatInstant(
                                                    msg.createdAt
                                                ),
                                                style = KmiTypography.caption,
                                                color = textColor.copy(
                                                    alpha = 0.62f
                                                ),
                                                textAlign = screenTextAlign
                                            )
                                        }
                                    }
                                }

                                Canvas(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .align(
                                            if (msg.isMine) Alignment.BottomEnd else Alignment.BottomStart
                                        )
                                ) {
                                    val path = Path()

                                    if (msg.isMine) {
                                        path.moveTo(size.width, size.height)
                                        path.lineTo(0f, size.height)
                                        path.lineTo(size.width, 0f)
                                    } else {
                                        path.moveTo(0f, size.height)
                                        path.lineTo(size.width, size.height)
                                        path.lineTo(0f, 0f)
                                    }

                                    drawPath(
                                        path = path,
                                        color = bubbleColor,
                                        style = Fill
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // צ'יפ למדיה מצורפת (אם יש)
                if (attachedUri != null && attachedMediaType != null) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = attachmentChipColor,
                        border = BorderStroke(1.dp, forumHeaderBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = when (attachedMediaType) {
                                    "image" -> forumTr(isEnglish, "תמונה מצורפת לשליחה", "Image attached")
                                    "video" -> forumTr(isEnglish, "סרטון מצורף לשליחה", "Video attached")
                                    else -> forumTr(isEnglish, "קובץ מצורף", "Attachment")
                                },
                                color = attachmentChipText,
                                style = KmiTypography.secondary
                            )
                            TextButton(
                                onClick = {
                                    attachedUri = null
                                    attachedMediaType = null
                                }
                            ) {
                                Text(
                                    text = forumTr(
                                        isEnglish,
                                        "הסר",
                                        "Remove"
                                    ),
                                    style = KmiTypography.action
                                )
                            }
                        }
                    }
                }

                // ================= שורת שליחה =================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp)
                        .padding(
                            top = 4.dp,
                            bottom = 4.dp
                        )
                        .imePadding()
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp),
                        shape = RoundedCornerShape(28.dp),
                        color = inputSurfaceColor,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                        border = BorderStroke(
                            width = 1.dp,
                            color = forumHeaderBorder
                        )
                    ) {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(
                                        min = 52.dp,
                                        max = 84.dp
                                    )
                                    .padding(
                                        horizontal = 6.dp,
                                        vertical = 2.dp
                                    ),
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { imagePicker.launch("image/*") },
                                modifier = Modifier.size(
                                    scaledIconSize(36.dp)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Add,
                                    contentDescription = forumTr(
                                        isEnglish,
                                        "צרף תמונה",
                                        "Attach image"
                                    ),
                                    tint = inputIconTint,
                                    modifier = Modifier.size(
                                        KmiIconSize.medium
                                    )
                                )
                            }

                            BasicTextField(
                                value = if (editingMessage != null) editText else input,
                                onValueChange = {
                                    if (editingMessage != null) {
                                        editText = it
                                    } else {
                                        input = it
                                    }
                                },
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .heightIn(
                                            min = 52.dp,
                                            max = 84.dp
                                        ),
                                textStyle =
                                    KmiTypography.body.copy(
                                    color = inputTextColor,
                                    textAlign = screenTextAlign,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                cursorBrush =
                                    SolidColor(
                                        MaterialTheme.colorScheme.primary
                                    ),
                                singleLine = true,
                                decorationBox = { innerTextField ->
                                    Box(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .heightIn(
                                                    min = 52.dp,
                                                    max = 84.dp
                                                )
                                                .padding(
                                                    horizontal = 8.dp
                                                ),
                                        contentAlignment =
                                            if (isEnglish) {
                                                Alignment.CenterStart
                                            } else {
                                                Alignment.CenterEnd
                                            }
                                    ) {
                                        val currentText =
                                            if (editingMessage != null) editText else input

                                        if (currentText.isBlank()) {
                                            Text(
                                                text = if (!isCurrentUserForumParticipant) {
                                                    forumTr(
                                                        isEnglish,
                                                        "אין הרשאה לשלוח בחדר זה",
                                                        "No permission to send in this room"
                                                    )
                                                } else if (editingMessage != null) {
                                                    forumTr(isEnglish, "עריכת הודעה...", "Editing message...")
                                                } else {
                                                    forumTr(isEnglish, "הודעה", "Message")
                                                },
                                                color = inputPlaceholderColor,
                                                textAlign = screenTextAlign,
                                                style = KmiTypography.body,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }

                                        Box(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentAlignment = if (isEnglish) Alignment.CenterStart else Alignment.CenterEnd
                                        ) {
                                            innerTextField()
                                        }
                                    }
                                }
                            )

                            IconButton(
                                onClick = { videoPicker.launch("video/*") },
                                modifier = Modifier.size(
                                    scaledIconSize(36.dp)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.VideoLibrary,
                                    contentDescription = forumTr(
                                        isEnglish,
                                        "צרף וידאו",
                                        "Attach video"
                                    ),
                                    tint = inputIconTint,
                                    modifier = Modifier.size(
                                        KmiIconSize.medium
                                    )
                                )
                            }
                        }
                    }

                    val canSendContent =
                        (if (editingMessage != null) editText else input).trim().isNotEmpty() || attachedUri != null

                    val canSend = canSendContent && isCurrentUserForumParticipant

                    Surface(
                        onClick = {
                            if (canSend) {
                                scope.launch { sendMessageInternal() }
                            }
                        },
                        shape = RoundedCornerShape(999.dp),
                        color =
                            if (canSend) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                        modifier = Modifier.size(
                            scaledIconSize(48.dp)
                        ),
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector =
                                    if (canSend) {
                                        Icons.AutoMirrored.Filled.Send
                                    } else {
                                        Icons.Outlined.Mic
                                    },
                                contentDescription = if (canSend) {
                                    if (editingMessage != null) {
                                        forumTr(isEnglish, "עדכן הודעה", "Update message")
                                    } else {
                                        forumTr(isEnglish, "שלח", "Send")
                                    }
                                } else {
                                    forumTr(isEnglish, "הקלטה", "Voice recording")
                                },
                                tint =
                                    if (canSend) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                modifier = Modifier.size(
                                    KmiIconSize.medium
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ForumControlsMiniHandle(
    isDarkMode: Boolean,
    isEnglish: Boolean,
    text: String,
    onClick: () -> Unit
) {
    val accentColor =
        MaterialTheme.colorScheme.primary

    val textColor =
        MaterialTheme.colorScheme.onSurface

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 32.dp)
                .clickable { onClick() },
            shape = RoundedCornerShape(999.dp),
            color =
                MaterialTheme.colorScheme.surface.copy(
                    alpha = 0.96f
                ),
            border = BorderStroke(
                1.dp,
                accentColor.copy(alpha = if (isDarkMode) 0.42f else 0.24f)
            ),
            shadowElevation = 0.dp,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    accentColor.copy(
                                        alpha =
                                            if (isDarkMode) {
                                                0.15f
                                            } else {
                                                0.08f
                                            }
                                    ),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(
                            horizontal = 12.dp,
                            vertical = 8.dp
                        ),
                verticalAlignment =
                    Alignment.CenterVertically,
                horizontalArrangement = if (isEnglish) {
                    Arrangement.Start
                } else {
                    Arrangement.End
                }
            ) {
                if (isEnglish) {
                    ForumHandleLines(accentColor = accentColor)
                    Spacer(Modifier.width(8.dp))
                }

                Text(
                    text = text,
                    color = textColor,
                    style = KmiTypography.caption.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = forumTextAlign(isEnglish),
                    maxLines = 1
                )

                if (!isEnglish) {
                    Spacer(Modifier.width(8.dp))
                    ForumHandleLines(accentColor = accentColor)
                }
            }
        }
    }
}

@Composable
private fun ForumHandleLines(
    accentColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .width(32.dp)
                    .height(2.dp)
                    .background(
                        color = accentColor.copy(alpha = 0.82f),
                        shape = RoundedCornerShape(999.dp)
                    )
            )

            if (it < 2) {
                Spacer(Modifier.height(2.dp))
            }
        }
    }
}

@Composable
private fun ForumPremiumControlCard(
    branches: List<String>,
    groups: List<String>,
    selectedBranch: String,
    selectedGroup: String,
    participants: List<ForumParticipantUi>,
    participantsText: Color,
    roomTitle: String,
    roomSubtitle: String,
    participantsTitle: String,
    participantsSubtitle: String,
    isRoomExpanded: Boolean,
    isParticipantsExpanded: Boolean,
    canOpenRoomPicker: Boolean,
    canOpenParticipants: Boolean,
    isDarkMode: Boolean,
    isEnglish: Boolean,
    onCollapseAll: () -> Unit,
    onRoomClick: () -> Unit,
    onParticipantsClick: () -> Unit,
    onBranchSelected: (String) -> Unit,
    onGroupSelected: (String) -> Unit
) {
    val titleColor =
        MaterialTheme.colorScheme.onSurface

    val subtitleColor =
        MaterialTheme.colorScheme.onSurfaceVariant

    val mutedColor =
        MaterialTheme.colorScheme.onSurfaceVariant.copy(
            alpha = 0.62f
        )

    val blueAccent =
        MaterialTheme.colorScheme.primary

    val purpleAccent =
        MaterialTheme.colorScheme.tertiary

    val cardBrush =
        Brush.linearGradient(
            colors =
                listOf(
                    MaterialTheme.colorScheme.surface,
                    MaterialTheme.colorScheme.surfaceVariant.copy(
                        alpha = 0.68f
                    )
                )
        )

    val borderBrush =
        Brush.linearGradient(
            colors =
                listOf(
                    blueAccent.copy(
                        alpha = 0.34f
                    ),
                    purpleAccent.copy(
                        alpha = 0.30f
                    )
                )
        )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 7.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, borderBrush),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBrush)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ForumPremiumControlRow(
                iconText = "⌂",
                title = roomTitle,
                subtitle = roomSubtitle,
                accentColor = blueAccent,
                titleColor = titleColor,
                subtitleColor = subtitleColor,
                mutedColor = mutedColor,
                isExpanded = isRoomExpanded,
                enabled = canOpenRoomPicker,
                isDarkMode = isDarkMode,
                isEnglish = isEnglish,
                onClick = onRoomClick
            )

            if (isRoomExpanded && canOpenRoomPicker) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp, bottom = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (branches.size > 1) {
                        KmiPremiumDropdown(
                            title =
                                forumTr(
                                    isEnglish,
                                    "סניף",
                                    "Branch"
                                ),
                            options = branches,
                            selectedValue = selectedBranch,
                            isEnglish = isEnglish,
                            placeholder =
                                forumTr(
                                    isEnglish,
                                    "בחר סניף",
                                    "Select branch"
                                ),
                            onSelected = { selectedBranchValue ->
                                onBranchSelected(
                                    selectedBranchValue
                                )
                            }
                        )
                    }

                    if (groups.size > 1) {
                        KmiPremiumDropdown(
                            title =
                                forumTr(
                                    isEnglish,
                                    "קבוצה",
                                    "Group"
                                ),
                            options = groups,
                            selectedValue = selectedGroup,
                            isEnglish = isEnglish,
                            placeholder =
                                forumTr(
                                    isEnglish,
                                    "בחר קבוצה",
                                    "Select group"
                                ),
                            onSelected = { selectedGroupValue ->
                                onGroupSelected(
                                    selectedGroupValue
                                )
                            }
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors =
                                listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.outline.copy(
                                        alpha = 0.46f
                                    ),
                                    Color.Transparent
                                )
                        )
                    )
            )

            ForumPremiumControlRow(
                iconText = "👥",
                title = participantsTitle,
                subtitle = participantsSubtitle,
                accentColor = purpleAccent,
                titleColor = titleColor,
                subtitleColor = subtitleColor,
                mutedColor = mutedColor,
                isExpanded = isParticipantsExpanded,
                enabled = canOpenParticipants,
                isDarkMode = isDarkMode,
                isEnglish = isEnglish,
                onClick = onParticipantsClick
            )

            if (isParticipantsExpanded && participants.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp, bottom = 3.dp),
                    shape = RoundedCornerShape(17.dp),
                    color =
                        MaterialTheme.colorScheme.surfaceVariant.copy(
                            alpha = 0.72f
                        ),
                    border = BorderStroke(
                        1.dp,
                        purpleAccent.copy(alpha = if (isDarkMode) 0.28f else 0.18f)
                    ),
                    shadowElevation = 0.dp,
                    tonalElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 11.dp, vertical = 7.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        participants.forEachIndexed { index, participant ->
                            val participantDisplayName =
                                forumDisplayPersonName(
                                    realName =
                                        participant.name,
                                    stableKey =
                                        participant.id,
                                    demoIndex =
                                        index,
                                    isEnglish =
                                        isEnglish
                                )

                            Text(
                                text =
                                    if (participant.isMe) {
                                        if (isEnglish) {
                                            "$participantDisplayName (me)"
                                        } else {
                                            "$participantDisplayName (אני)"
                                        }
                                    } else {
                                        participantDisplayName
                                    },
                                color = participantsText,
                                style = KmiTypography.secondary.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                textAlign = forumTextAlign(isEnglish),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .size(width = 86.dp, height = 22.dp)
                        .clickable { onCollapseAll() },
                    shape = RoundedCornerShape(999.dp),
                    color =
                        MaterialTheme.colorScheme.surfaceVariant.copy(
                            alpha = 0.62f
                        ),
                    border = BorderStroke(
                        width = 1.dp,
                        color =
                            MaterialTheme.colorScheme.outline.copy(
                                alpha = 0.52f
                            )
                    ),
                    shadowElevation = 0.dp,
                    tonalElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        repeat(3) {
                            Box(
                                modifier = Modifier
                                    .width(36.dp)
                                    .height(2.dp)
                                    .background(
                                        color =
                                            MaterialTheme.colorScheme.primary.copy(
                                                alpha = 0.78f
                                            ),
                                        shape = RoundedCornerShape(999.dp)
                                    )
                            )

                            if (it < 2) {
                                Spacer(Modifier.height(2.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ForumPremiumControlRow(
    iconText: String,
    title: String,
    subtitle: String,
    accentColor: Color,
    titleColor: Color,
    subtitleColor: Color,
    mutedColor: Color,
    isExpanded: Boolean,
    enabled: Boolean,
    isDarkMode: Boolean,
    isEnglish: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 2.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Surface(
            modifier = Modifier.size(scaledIconSize(34.dp)),
            shape = RoundedCornerShape(14.dp),
            color = accentColor.copy(alpha = if (isDarkMode) 0.18f else 0.11f),
            border = BorderStroke(
                1.dp,
                accentColor.copy(alpha = if (isDarkMode) 0.38f else 0.26f)
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = iconText,
                    color = accentColor,
                    style = KmiTypography.action,
                    fontWeight = FontWeight.Black
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
        ) {
            Text(
                text = title,
                color = titleColor,
                style = KmiTypography.secondary,
                fontWeight = FontWeight.ExtraBold,
                textAlign = forumTextAlign(isEnglish),
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2
            )

            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isEnglish) {
                        Arrangement.Absolute.Left
                    } else {
                        Arrangement.Absolute.Right
                    }
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = accentColor.copy(alpha = if (isDarkMode) 0.15f else 0.09f),
                        border = BorderStroke(
                            1.dp,
                            accentColor.copy(alpha = if (isDarkMode) 0.28f else 0.18f)
                        )
                    ) {
                        Text(
                            text = subtitle,
                            color =
                                if (enabled) {
                                    subtitleColor
                                } else {
                                    mutedColor
                                },
                            style = KmiTypography.caption,
                            fontWeight = FontWeight.Bold,
                            textAlign =
                                forumTextAlign(isEnglish),
                            modifier =
                                Modifier.padding(
                                    horizontal = 8.dp,
                                    vertical = 3.dp
                                ),
                            maxLines = 2
                        )
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.size(scaledIconSize(30.dp)),
            shape = RoundedCornerShape(13.dp),
            color =
                if (enabled) {
                    accentColor.copy(
                        alpha =
                            if (isDarkMode) {
                                0.18f
                            } else {
                                0.10f
                            }
                    )
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(
                        alpha = 0.54f
                    )
                },
            border = BorderStroke(
                width = 1.dp,
                color =
                    if (enabled) {
                        accentColor.copy(
                            alpha =
                                if (isDarkMode) {
                                    0.34f
                                } else {
                                    0.22f
                                }
                        )
                    } else {
                        MaterialTheme.colorScheme.outline.copy(
                            alpha = 0.34f
                        )
                    }
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text =
                        if (isExpanded) {
                            "⌃"
                        } else {
                            "⌄"
                        },
                    color =
                        if (enabled) {
                            accentColor
                        } else {
                            mutedColor
                        },
                    style = KmiTypography.action,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
private fun EmptyForumRoomCard(
    branch: String,
    groupKey: String,
    isEnglish: Boolean
) {
    val align = forumTextAlign(isEnglish)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 18.dp),
        shape = RoundedCornerShape(24.dp),
        color =
            MaterialTheme.colorScheme.surface.copy(
                alpha = 0.96f
            ),
        border = BorderStroke(
            width = 1.dp,
            color =
                MaterialTheme.colorScheme.outline.copy(
                    alpha = 0.52f
                )
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors =
                            listOf(
                                MaterialTheme.colorScheme.surfaceVariant.copy(
                                    alpha = 0.54f
                                ),
                                MaterialTheme.colorScheme.surface
                            )
                    )
                )
                .padding(horizontal = 18.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color =
                    MaterialTheme.colorScheme.primaryContainer.copy(
                        alpha = 0.72f
                    ),
                border = BorderStroke(
                    width = 1.dp,
                    color =
                        MaterialTheme.colorScheme.primary.copy(
                            alpha = 0.30f
                        )
                )
            ) {
                Box(
                    modifier = Modifier.size(scaledIconSize(50.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint =
                            MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(scaledIconSize(26.dp))
                    )
                }
            }

            Text(
                text = forumTr(
                    isEnglish,
                    "אין עדיין הודעות בחדר הזה",
                    "No messages in this room yet"
                ),
                color =
                    MaterialTheme.colorScheme.onSurface,
                style = KmiTypography.sectionTitle,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = forumTr(
                    isEnglish,
                    "הפורום מחובר לשרת. הודעות שתשלחו כאן יישמרו בחדר הקבוצה ויופיעו לכל המשתתפים המורשים.",
                    "This forum is connected to the server. Messages sent here will be saved in the group room and shown to authorized participants."
                ),
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant,
                style = KmiTypography.caption,
                textAlign = align,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = forumTr(
                    isEnglish,
                    "סניף: ${branch.ifBlank { "—" }}  •  קבוצה: ${groupKey.ifBlank { "—" }}",
                    "Branch: ${branch.ifBlank { "—" }}  •  Group: ${groupKey.ifBlank { "—" }}"
                ),
                color =
                    MaterialTheme.colorScheme.primary,
                style = KmiTypography.secondary.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                textAlign = align,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
