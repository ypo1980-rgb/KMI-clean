package il.kmi.app.screens.coach

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import il.kmi.app.privacy.DemoPrivacy
import il.kmi.app.privacy.TraineeDisplayNameMapper
import il.kmi.app.screens.registration.CoachBranchAssignmentsCodec
import il.kmi.app.ui.KmiPremiumDropdown
import il.kmi.app.ui.KmiTypography
import il.kmi.app.ui.loading.KmiLoadingRings
import il.kmi.app.ui.pdf.KmiPdfFooter
import il.kmi.app.ui.pdf.KmiPdfHeader
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager
import il.yuval.ui.theme.kmiScreenBackgroundBrush
import il.yuval.ui.theme.kmiSectionHeaderBackground
import il.yuval.ui.theme.kmiSuccessColor
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream

//======================================================================

private fun coachBroadcastTr(isEnglish: Boolean, he: String, en: String): String {
    return if (isEnglish) en else he
}

private fun coachBroadcastTextAlign(isEnglish: Boolean): TextAlign {
    return if (isEnglish) TextAlign.Left else TextAlign.Right
}

fun persistCoachBroadcast(
    region: String,
    branch: String,
    message: String,
    targetUids: List<String>,
    targetGroups: List<String> = emptyList(),
    onResult: (Boolean, Throwable?) -> Unit = { _, _ -> }
) {
    val currentUid =
        FirebaseAuth
            .getInstance()
            .currentUser
            ?.uid
            ?.trim()
            .orEmpty()

    if (currentUid.isBlank()) {
        onResult(
            false,
            IllegalStateException(
                "No logged-in user"
            )
        )
        return
    }

    val cleanRegion =
        region.trim()

    val cleanBranch =
        branch.trim()

    val cleanMessage =
        message.trim()

    val cleanTargetUids =
        targetUids
            .map {
                it.trim()
            }
            .filter {
                it.isNotBlank()
            }
            .distinct()

    val cleanTargetGroups =
        targetGroups
            .map {
                it.trim()
            }
            .filter {
                it.isNotBlank()
            }
            .distinct()

    if (
        cleanBranch.isBlank() ||
        cleanMessage.isBlank()
    ) {
        onResult(
            false,
            IllegalStateException(
                "Missing branch or message"
            )
        )
        return
    }

    if (cleanTargetUids.isEmpty()) {
        onResult(
            false,
            IllegalStateException(
                "No selected recipients"
            )
        )
        return
    }

    FirebaseFunctions
        .getInstance()
        .getHttpsCallable(
            "createSecureCoachBroadcast"
        )
        .call(
            mapOf(
                "region" to
                        cleanRegion,

                "branch" to
                        cleanBranch,

                "message" to
                        cleanMessage,

                "groups" to
                        cleanTargetGroups,

                "targetUids" to
                        cleanTargetUids
            )
        )
        .addOnSuccessListener {
            onResult(
                true,
                null
            )
        }
        .addOnFailureListener { error ->
            onResult(
                false,
                error
            )
        }
}

// ייצוג נמען אחד ברשימת הקבוצה
private data class CoachRecipient(
    val uid: String,
    val name: String,
    val phone: String,
    val email: String,
    val selected: Boolean
)

//======================================================================
// Coach Broadcast PDF
//======================================================================

private fun shareCoachBroadcastPdf(
    context: Context,
    region: String,
    branch: String,
    groups: List<String>,
    message: String,
    recipients: List<CoachRecipient>,
    totalRecipients: Int,
    isEnglish: Boolean,
    demoPrivacyEnabled: Boolean
) {
    val file =
        createCoachBroadcastPdf(
            context = context,
            region = region,
            branch = branch,
            groups = groups,
            message = message,
            recipients = recipients,
            totalRecipients =
                totalRecipients,
            isEnglish = isEnglish,
            demoPrivacyEnabled =
                demoPrivacyEnabled
        )

    val uri =
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

    val intent =
        Intent(
            Intent.ACTION_SEND
        ).apply {
            type = "application/pdf"

            putExtra(
                Intent.EXTRA_SUBJECT,
                if (isEnglish) {
                    "KAMI Broadcast Message"
                } else {
                    "שידור הודעה לקבוצה"
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

private fun createCoachBroadcastPdf(
    context: Context,
    region: String,
    branch: String,
    groups: List<String>,
    message: String,
    recipients: List<CoachRecipient>,
    totalRecipients: Int,
    isEnglish: Boolean,
    demoPrivacyEnabled: Boolean
): File {

    val pageWidth = 595
    val pageHeight = 842
    val margin = 32f

    val document =
        PdfDocument()

    // צבע ההדגשה משמש לתוכן הדוח בלבד.
    // צבעי הכותרת והלוגו מנוהלים ברכיב הגלובלי.
    val mediumBlue =
        android.graphics.Color.rgb(
            36,
            103,
            158
        )

    val textDark =
        android.graphics.Color.rgb(
            15,
            23,
            42
        )

    val boxBackground =
        android.graphics.Color.rgb(
            248,
            250,
            252
        )

    val boxBorder =
        android.graphics.Color.rgb(
            226,
            232,
            240
        )

    val regularTypeface =
        Typeface.create(
            Typeface.SANS_SERIF,
            Typeface.NORMAL
        )

    val boldTypeface =
        Typeface.create(
            Typeface.SANS_SERIF,
            Typeface.BOLD
        )

    fun textPaint(
        size: Float,
        color: Int = textDark,
        bold: Boolean = false,
        align: Paint.Align =
            if (isEnglish) {
                Paint.Align.LEFT
            } else {
                Paint.Align.RIGHT
            }
    ): Paint {
        return Paint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {
            textSize = size
            this.color = color
            typeface =
                if (bold) {
                    boldTypeface
                } else {
                    regularTypeface
                }

            textAlign = align
        }
    }

    var pageNumber = 0
    var page: PdfDocument.Page? = null
    lateinit var canvas: android.graphics.Canvas
    var y = 0f

    fun startPage() {
        page?.let { currentPage ->
            KmiPdfFooter.draw(
                canvas = currentPage.canvas,
                pageWidth = pageWidth,
                pageHeight = pageHeight,
                pageNumber = pageNumber,
                isEnglish = isEnglish
            )

            document.finishPage(currentPage)
        }

        pageNumber++

        val info =
            PdfDocument.PageInfo
                .Builder(
                    pageWidth,
                    pageHeight,
                    pageNumber
                )
                .create()

        val newPage =
            document.startPage(info)

        page = newPage
        canvas = newPage.canvas

        KmiPdfHeader.draw(
            context = context,
            canvas = canvas,
            pageWidth = pageWidth,
            isEnglish = isEnglish,
            titleHebrew = "שידור הודעה לקבוצה",
            titleEnglish = "Broadcast Message",
            subtitleHebrew = "דו״ח תקשורת מאמן",
            subtitleEnglish = "Coach communication report"
        )

        y = KmiPdfHeader.CONTENT_TOP + 24f
    }

    fun ensureSpace(
        required: Float
    ) {
        val contentBottom =
            pageHeight -
                    KmiPdfFooter.CONTENT_BOTTOM_PADDING

        if (y + required > contentBottom) {
            startPage()
        }
    }

    fun drawInfoRow(
        label: String,
        value: String
    ) {
        if (value.isBlank()) {
            return
        }

        ensureSpace(46f)

        val right =
            pageWidth.toFloat() - margin

        val rect =
            android.graphics.RectF(
                margin,
                y,
                right,
                y + 38f
            )

        val backgroundPaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                color = boxBackground
                style = Paint.Style.FILL
            }

        val borderPaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                color = boxBorder
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }

        canvas.drawRoundRect(
            rect,
            8f,
            8f,
            backgroundPaint
        )

        canvas.drawRoundRect(
            rect,
            8f,
            8f,
            borderPaint
        )

        val x =
            if (isEnglish) {
                margin + 12f
            } else {
                right - 12f
            }

        val rowPaint =
            textPaint(size = 11f)

        val rowText =
            "$label: $value"

        val availableWidth =
            right - margin - 24f

        val visibleText =
            if (rowPaint.measureText(rowText) <= availableWidth) {
                rowText
            } else {
                val ellipsis = "…"

                val count = rowPaint.breakText(
                    rowText,
                    true,
                    availableWidth -
                            rowPaint.measureText(ellipsis),
                    null
                )

                rowText.take(count) + ellipsis
            }

        canvas.drawText(
            visibleText,
            x,
            y + 24f,
            rowPaint
        )

        y += 46f
    }

    fun wrappedLines(
        text: String,
        paint: Paint,
        maxWidth: Float
    ): List<String> {
        if (text.isBlank()) return emptyList()

        val lines = mutableListOf<String>()

        text.replace("\r\n", "\n")
            .split("\n")
            .forEach { paragraph ->

                if (paragraph.isBlank()) {
                    lines.add("")
                    return@forEach
                }

                var currentLine = ""

                paragraph.trim()
                    .split(Regex("\\s+"))
                    .forEach { word ->

                        val candidate =
                            if (currentLine.isEmpty()) {
                                word
                            } else {
                                "$currentLine $word"
                            }

                        if (paint.measureText(candidate) <= maxWidth) {
                            currentLine = candidate
                        } else {
                            if (currentLine.isNotEmpty()) {
                                lines.add(currentLine)
                                currentLine = ""
                            }

                            var remaining = word

                            while (
                                remaining.isNotEmpty() &&
                                paint.measureText(remaining) > maxWidth
                            ) {
                                val count = paint.breakText(
                                    remaining,
                                    true,
                                    maxWidth,
                                    null
                                ).coerceAtLeast(1)

                                lines.add(remaining.take(count))
                                remaining = remaining.drop(count)
                            }

                            currentLine = remaining
                        }
                    }

                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine)
                }
            }

        return lines
    }

    startPage()

    canvas.drawText(
        if (isEnglish) {
            "Broadcast details"
        } else {
            "פרטי השידור"
        },
        if (isEnglish) {
            margin
        } else {
            pageWidth.toFloat() - margin
        },
        y,
        textPaint(
            size = 16f,
            color = mediumBlue,
            bold = true
        )
    )

    y += 22f

    drawInfoRow(
        label =
            if (isEnglish) {
                "Region"
            } else {
                "אזור"
            },
        value = region.trim()
    )

    drawInfoRow(
        label =
            if (isEnglish) {
                "Branch"
            } else {
                "סניף"
            },
        value = branch.trim()
    )

    drawInfoRow(
        label =
            if (isEnglish) {
                "Groups"
            } else {
                "קבוצות"
            },
        value =
            groups.joinToString(", ")
    )

    drawInfoRow(
        label =
            if (isEnglish) {
                "Selected recipients"
            } else {
                "נמענים שנבחרו"
            },
        value =
            totalRecipients.toString()
    )

    ensureSpace(70f)

    canvas.drawText(
        if (isEnglish) {
            "Message"
        } else {
            "תוכן ההודעה"
        },
        if (isEnglish) {
            margin
        } else {
            pageWidth.toFloat() - margin
        },
        y,
        textPaint(
            size = 14f,
            color = mediumBlue,
            bold = true
        )
    )

    y += 20f

    val messagePaint = textPaint(size = 11f)

    val messageWidth =
        pageWidth.toFloat() - margin * 2f

    wrappedLines(
        text = message,
        paint = messagePaint,
        maxWidth = messageWidth
    ).forEach { line ->

        ensureSpace(20f)

        if (line.isNotEmpty()) {
            canvas.drawText(
                line,
                if (isEnglish) {
                    margin
                } else {
                    pageWidth.toFloat() - margin
                },
                y,
                messagePaint
            )
        }

        y += 18f
    }

    y += 14f

    if (recipients.isNotEmpty()) {

        ensureSpace(42f)

        canvas.drawText(
            if (isEnglish) {
                "Recipients"
            } else {
                "רשימת נמענים"
            },
            if (isEnglish) {
                margin
            } else {
                pageWidth.toFloat() - margin
            },
            y,
            textPaint(
                size = 14f,
                color = mediumBlue,
                bold = true
            )
        )

        y += 22f

        recipients.forEachIndexed {
                index,
                recipient ->

            ensureSpace(30f)

            val displayLine =
                buildString {
                    append(
                        "${index + 1}. "
                    )

                    append(
                        recipient.name
                            .ifBlank {
                                if (isEnglish) {
                                    "Unnamed trainee"
                                } else {
                                    "מתאמן ללא שם"
                                }
                            }
                    )

                    if (
                        !demoPrivacyEnabled &&
                        recipient.phone.isNotBlank()
                    ) {
                        append(
                            " · ${recipient.phone}"
                        )
                    }
                }

            val recipientPaint =
                textPaint(size = 10.5f)

            val recipientWidth =
                pageWidth.toFloat() - margin * 2f - 16f

            val recipientLines =
                wrappedLines(
                    text = displayLine,
                    paint = recipientPaint,
                    maxWidth = recipientWidth
                )

            recipientLines.forEach { line ->
                ensureSpace(20f)

                if (line.isNotBlank()) {
                    canvas.drawText(
                        line,
                        if (isEnglish) {
                            margin + 8f
                        } else {
                            pageWidth.toFloat() - margin - 8f
                        },
                        y,
                        recipientPaint
                    )
                }

                y += 18f
            }

            y += 4f
        }
    }

    page?.let { currentPage ->
        KmiPdfFooter.draw(
            canvas = currentPage.canvas,
            pageWidth = pageWidth,
            pageHeight = pageHeight,
            pageNumber = pageNumber,
            isEnglish = isEnglish
        )

        document.finishPage(currentPage)
    }

    val directory =
        File(
            context.cacheDir,
            "shared_pdfs"
        ).apply {
            mkdirs()
        }

    /*
     * שם קבוע:
     * כל יצירה חדשה מחליפה את הקובץ הקודם.
     */
    val fileName =
        if (isEnglish) {
            "Broadcast Message.pdf"
        } else {
            "שידור הודעה.pdf"
        }

    val file =
        File(
            directory,
            fileName
        )

    try {
        FileOutputStream(
            file,
            false
        ).use { output ->
            document.writeTo(output)
        }
    } finally {
        document.close()
    }

    return file
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoachBroadcastScreen(
    branchesByRegion: Map<String, List<String>>,
    defaultRegion: String?,
    defaultBranch: String?,
    onBack: () -> Unit,
    onHome: () -> Unit,

    // ✅ פעולות פלטפורמה
    onOpenSms: (numbers: List<String>, message: String) -> Unit = { _, _ -> },

    @Suppress("UNUSED_PARAMETER")
    onShareText: (message: String) -> Unit = {}
) {
    val contextLang = LocalContext.current
    val langManager = remember { AppLanguageManager(contextLang) }

    var currentLanguage by remember {
        mutableStateOf(langManager.getCurrentLanguage())
    }

    val isEnglish =
        currentLanguage ==
                AppLanguage.ENGLISH

    val screenTextAlign =
        coachBroadcastTextAlign(isEnglish)

    val layoutDirection =
        if (isEnglish) {
            LayoutDirection.Ltr
        } else {
            LayoutDirection.Rtl
        }

    /*
     * הקריאה מתבצעת מתוך Composition ולכן
     * שינוי המתג בהגדרות מעדכן את המסך מיד.
     */
    val demoPrivacyEnabled =
        DemoPrivacy.isEnabled()

    val userSp =
        remember(contextLang) {
            contextLang.getSharedPreferences(
                "kmi_user",
                Context.MODE_PRIVATE
            )
        }

    /*
     * המבנה החדש:
     *
     * כל סניף מחזיק רק את הקבוצות שאליהן
     * המאמן משויך באותו סניף.
     */
    val coachBranchAssignments =
        remember(userSp) {
            CoachBranchAssignmentsCodec.decode(
                userSp.getString(
                    "coach_branch_assignments_json",
                    ""
                )
            )
        }

    fun normalizedAssignmentValue(
        value: String
    ): String {
        return value
            .trim()
            .replace('־', '-')
            .replace('–', '-')
            .replace('—', '-')
            .replace(
                Regex("\\s+"),
                " "
            )
            .lowercase()
    }

    val assignedBranchNames =
        remember(coachBranchAssignments) {
            CoachBranchAssignmentsCodec
                .flattenBranches(
                    coachBranchAssignments
                )
                .map {
                    it.trim()
                }
                .filter {
                    it.isNotBlank()
                }
                .distinct()
        }

    /*
     * אם קיים המבנה החדש, מסננים את רשימת
     * הסניפים לפי השיוכים האמיתיים של המאמן.
     *
     * אם המבנה החדש עדיין ריק, נשמר fallback
     * למידע הישן כדי לא לפגוע במשתמשים קיימים.
     */
    val visibleBranchesByRegion =
        remember(
            branchesByRegion,
            assignedBranchNames
        ) {
            if (assignedBranchNames.isEmpty()) {
                branchesByRegion
            } else {
                val assignedNormalized =
                    assignedBranchNames
                        .map(::normalizedAssignmentValue)
                        .toSet()

                branchesByRegion
                    .mapValues { (_, branches) ->
                        branches.filter { candidate ->
                            normalizedAssignmentValue(
                                candidate
                            ) in assignedNormalized
                        }
                    }
                    .filterValues {
                        it.isNotEmpty()
                    }
            }
        }

    fun readCoachGroupKey(): String {
        return userSp.getString("active_group", null)
            ?: userSp.getString("activeGroup", null)
            ?: userSp.getString("primaryGroup", null)
            ?: userSp.getString("groupKey", null)
            ?: userSp.getString("group_key", null)
            ?: userSp.getString("age_group", null)
            ?: userSp.getString("group", null)
            ?: ""
    }

    var coachGroupKey by remember {
        mutableStateOf(readCoachGroupKey())
    }

    androidx.compose.runtime.DisposableEffect(userSp) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (
                key == "active_group" ||
                key == "activeGroup" ||
                key == "primaryGroup" ||
                key == "groupKey" ||
                key == "group_key" ||
                key == "age_group" ||
                key == "group"
            ) {
                coachGroupKey = readCoachGroupKey()
            }
        }

        userSp.registerOnSharedPreferenceChangeListener(listener)

        onDispose {
            userSp.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    var sendScope by remember { mutableStateOf("groups") }

    var availableBranchGroups by remember {
        mutableStateOf<List<String>>(emptyList())
    }

    var availableBranchGroupCounts by remember {
        mutableStateOf<Map<String, Int>>(emptyMap())
    }

    var selectedTargetGroups by remember {
        mutableStateOf<Set<String>>(emptySet())
    }

    val effectiveGroupKeys = remember(sendScope, selectedTargetGroups) {
        if (sendScope == "branch") {
            emptyList()
        } else {
            selectedTargetGroups
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
        }
    }

    val initialRegion =
        remember(
            visibleBranchesByRegion,
            defaultRegion
        ) {
            visibleBranchesByRegion
                .keys
                .singleOrNull()
                .orEmpty()
        }

    val initialBranch =
        remember(
            visibleBranchesByRegion,
            initialRegion,
            defaultBranch
        ) {
            visibleBranchesByRegion[
                initialRegion
            ]
                .orEmpty()
                .singleOrNull()
                .orEmpty()
        }

    var region by remember(
        visibleBranchesByRegion
    ) {
        mutableStateOf(initialRegion)
    }

    var branch by remember(
        visibleBranchesByRegion,
        initialRegion
    ) {
        mutableStateOf(initialBranch)
    }

    var message by remember {
        mutableStateOf("")
    }

    var isSending by remember {
        mutableStateOf(false)
    }

    /*
     * הקבוצות המותרות נקראות מהשיוך של
     * הסניף שנבחר בלבד.
     */
    val assignedGroupsForSelectedBranch =
        remember(
            coachBranchAssignments,
            branch
        ) {
            val normalizedBranch =
                normalizedAssignmentValue(
                    branch
                )

            coachBranchAssignments
                .firstOrNull { assignment ->
                    normalizedAssignmentValue(
                        assignment.branch
                    ) == normalizedBranch
                }
                ?.groups
                ?.map {
                    it.trim()
                }
                ?.filter {
                    it.isNotBlank()
                }
                ?.distinct()
                .orEmpty()
        }

    // רשימת הנמענים מהקבוצה (נשלפת מפיירסטור)
    var recipients by remember {
        mutableStateOf<List<CoachRecipient>>(emptyList())
    }

    var isRecipientsLoading by remember {
        mutableStateOf(false)
    }

    var recipientsLoadFailed by remember {
        mutableStateOf(false)
    }

    // Snackbar לפידבק
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // ===== טעינת נמענים מאובטחת דרך Cloud Function =====
    LaunchedEffect(
        region,
        branch,
        sendScope,
        selectedTargetGroups,
        coachGroupKey,
        coachBranchAssignments,
        assignedGroupsForSelectedBranch
    ) {
        val cleanRegion =
            region.trim()

        val cleanBranch =
            branch.trim()

        if (
            cleanRegion.isBlank() ||
            cleanBranch.isBlank()
        ) {
            availableBranchGroups = emptyList()
            availableBranchGroupCounts = emptyMap()
            selectedTargetGroups = emptySet()
            recipients = emptyList()
            isRecipientsLoading = false
            recipientsLoadFailed = false

            return@LaunchedEffect
        }

        recipientsLoadFailed = false
        isRecipientsLoading = true

        /*
         * שומרים את מצב הבחירה הנוכחי כדי שרענון
         * מהשרת לא יאפס checkbox שכבר שונה ע"י המאמן.
         */
        val currentSelectionByUid =
            recipients.associate {
                it.uid to it.selected
            }

        val currentSelectionByPhone =
            recipients
                .filter { it.phone.isNotBlank() }
                .associate {
                    it.phone to it.selected
                }

        val requestedGroups =
            if (sendScope == "branch") {
                emptyList()
            } else {
                selectedTargetGroups
                    .map {
                        it.trim()
                    }
                    .filter {
                        it.isNotBlank()
                    }
                    .distinct()
            }

        try {

            val result =
                FirebaseFunctions
                    .getInstance()
                    .getHttpsCallable(
                        "loadSecureCoachBroadcastRecipients"
                    )
                    .call(
                        mapOf(
                            "branch" to
                                    cleanBranch,
                            "groups" to
                                    requestedGroups
                        )
                    )
                    .await()

            val payload =
                result.data as? Map<*, *>

            /*
             * קבוצות מורשות בסניף.
             */
            val serverGroups =
                (
                        payload
                            ?.get("availableGroups")
                                as? List<*>
                        )
                    ?.mapNotNull { rawGroup ->
                        rawGroup
                            ?.toString()
                            ?.trim()
                            ?.takeIf {
                                it.isNotBlank()
                            }
                    }
                    ?.distinct()
                    .orEmpty()

            /*
             * מספר מתאמנים בכל קבוצה.
             */
            val rawGroupCounts =
                payload
                    ?.get("groupCounts")
                        as? Map<*, *>

            val serverGroupCounts =
                rawGroupCounts
                    ?.mapNotNull { entry ->

                        val groupName =
                            entry.key
                                ?.toString()
                                ?.trim()
                                .orEmpty()

                        val count =
                            when (
                                val rawValue =
                                    entry.value
                            ) {
                                is Number ->
                                    rawValue.toInt()

                                else ->
                                    rawValue
                                        ?.toString()
                                        ?.toIntOrNull()
                            }
                                ?: 0

                        if (groupName.isBlank()) {
                            null
                        } else {
                            groupName to count
                        }
                    }
                    ?.toMap()
                    .orEmpty()

            availableBranchGroups =
                serverGroups

            availableBranchGroupCounts =
                serverGroupCounts

            /*
             * אם קבוצה שסומנה כבר אינה מורשית/קיימת,
             * מסירים אותה מהבחירה.
             */
            if (
                sendScope != "branch" &&
                selectedTargetGroups.isNotEmpty()
            ) {

                val validSelectedGroups =
                    selectedTargetGroups
                        .filter {
                            it in serverGroups
                        }
                        .toSet()

                if (
                    validSelectedGroups !=
                    selectedTargetGroups
                ) {
                    selectedTargetGroups =
                        validSelectedGroups

                    recipients =
                        emptyList()

                    isRecipientsLoading =
                        false

                    return@LaunchedEffect
                }
            }

            val rawRecipients =
                payload
                    ?.get("recipients")
                        as? List<*>
                    ?: emptyList<Any>()

            val serverRecipients =
                rawRecipients
                    .mapNotNull { rawRecipient ->

                        val recipientMap =
                            rawRecipient as? Map<*, *>
                                ?: return@mapNotNull null

                        val uid =
                            recipientMap["uid"]
                                ?.toString()
                                ?.trim()
                                .orEmpty()

                        val name =
                            recipientMap["name"]
                                ?.toString()
                                ?.trim()
                                .orEmpty()

                        val phone =
                            recipientMap["phone"]
                                ?.toString()
                                ?.trim()
                                .orEmpty()

                        val email =
                            recipientMap["email"]
                                ?.toString()
                                ?.trim()
                                .orEmpty()

                        if (
                            uid.isBlank()
                        ) {
                            return@mapNotNull null
                        }

                        CoachRecipient(
                            uid = uid,

                            name =
                                name.ifBlank {
                                    email.ifBlank {
                                        phone
                                    }
                                },

                            phone =
                                phone,

                            email =
                                email,

                            selected =
                                currentSelectionByUid[uid]
                                    ?: phone
                                        .takeIf { it.isNotBlank() }
                                        ?.let { currentSelectionByPhone[it] }
                                    ?: true
                        )
                    }
                    .distinctBy {
                        it.uid
                    }
                    .sortedBy {
                        it.name
                    }

            /*
             * במצב קבוצות, כל עוד המאמן עדיין
             * לא סימן אף קבוצה — מציגים רק את
             * רשימת הקבוצות, לא את כל המתאמנים.
             *
             * במצב branch אפשר להציג את כל נמעני הסניף.
             */
            recipients =
                if (
                    sendScope == "branch" ||
                    requestedGroups.isNotEmpty()
                ) {
                    serverRecipients
                } else {
                    emptyList()
                }

            isRecipientsLoading =
                false

        } catch (error: Exception) {

            if (error is kotlinx.coroutines.CancellationException) {
                throw error
            }

            recipientsLoadFailed = true

            availableBranchGroups = emptyList()
            availableBranchGroupCounts = emptyMap()
            recipients = emptyList()

            isRecipientsLoading = false
        }
    }

    /*
     * הרשימה האמיתית נשארת בזיכרון לצורך שליחה.
     * החלפת השם מתבצעת בשכבת התצוגה בלבד.
     */
    val uiRecipients =
        remember(
            recipients,
            demoPrivacyEnabled,
            isEnglish
        ) {
            recipients.mapIndexed {
                    index,
                    recipient ->

                recipient.copy(
                    name =
                        TraineeDisplayNameMapper
                            .displayName(
                                realName =
                                    recipient.name,
                                stableKey =
                                    recipient.uid
                                        .ifBlank {
                                            recipient.email
                                                .ifBlank {
                                                    recipient.phone
                                                }
                                        },
                                demoIndex = index + 1,
                                isEnglish =
                                    isEnglish
                            )
                            .ifBlank {
                                coachBroadcastTr(
                                    isEnglish,
                                    "מתאמן ללא שם",
                                    "Unnamed trainee"
                                )
                            }
                )
            }
        }

    // נמענים שנבחרו (גם טלפונים וגם UIDs)
    val selectedRecipients =
        recipients.filter {
            it.selected
        }

    /*
     * רשימת תצוגה בלבד.
     *
     * במצב הדגמה היא כוללת:
     * מתאמן 1, מתאמן 2 וכו'.
     *
     * הרשימה האמיתית נשארת ב-selectedRecipients
     * לצורך SMS / Push / Firestore בלבד.
     */
    val selectedUiRecipients =
        uiRecipients.filter {
            it.selected
        }

    val selectedUids =
        selectedRecipients
        .map { it.uid.trim() }
        .filter { it.isNotBlank() }
        .distinct()

    val allSelected = recipients.isNotEmpty() && recipients.all { it.selected }

    val sendButtonText = when {
        selectedUids.isEmpty() -> coachBroadcastTr(
            isEnglish,
            "בחר מתאמנים לשליחה",
            "Select trainees to send"
        )

        allSelected -> coachBroadcastTr(
            isEnglish,
            "שליחת הודעה לכל המתאמנים",
            "Send message to all trainees"
        )

        selectedUids.size == 1 -> coachBroadcastTr(
            isEnglish,
            "שליחת הודעה למתאמן שנבחר",
            "Send message to selected trainee"
        )

        else -> coachBroadcastTr(
            isEnglish,
            "שליחת הודעה ל-${selectedUids.size} מתאמנים",
            "Send message to ${selectedUids.size} trainees"
        )
    }

    // שליחת הודעה מאובטחת דרך Firebase / Push בלבד.
    fun saveBroadcast(
        messageToSend: String,
        uidsToSend: List<String>,
        groupsToSend: List<String>
    ) {
        val regionToSend = region.trim()
        val branchToSend = branch.trim()

        persistCoachBroadcast(
            region = regionToSend,
            branch = branchToSend,
            message = messageToSend,
            targetUids = uidsToSend,
            targetGroups = groupsToSend,
            onResult = { ok, _ ->

                scope.launch {
                    if (!ok) {
                        isSending = false

                        snackbarHostState.showSnackbar(
                            coachBroadcastTr(
                                isEnglish,
                                "שמירת ההודעה נכשלה. בדוק את החיבור לאינטרנט ונסה שוב.",
                                "Failed to save the message. Check your internet connection and try again."
                            )
                        )

                        return@launch
                    }

                    // Firebase אישרה שמירה.
                    // אין צורך להמתין להשלמת עיבוד Push.
                    if (message == messageToSend) {
                        message = ""
                    }

                    isSending = false

                    snackbarHostState.showSnackbar(
                        coachBroadcastTr(
                            isEnglish,
                            "ההודעה נשמרה ונשלחה לעיבוד Push עבור ${uidsToSend.size} נמענים.",
                            "The message was saved and queued for Push processing for ${uidsToSend.size} recipients."
                        )
                    )
                }
            }
        )
    }

    // צבעי שדות ממקור האמת הגלובלי
    val fieldContainerColor =
        MaterialTheme.colorScheme.surface

    val fieldTextColor =
        MaterialTheme.colorScheme.onSurface

    val fieldLabelColor =
        MaterialTheme.colorScheme.onSurfaceVariant

    val fieldBorderColor =
        MaterialTheme.colorScheme.outlineVariant

    val fieldColors =
        OutlinedTextFieldDefaults.colors(
            focusedContainerColor = fieldContainerColor,
            unfocusedContainerColor = fieldContainerColor,
            disabledContainerColor = fieldContainerColor,

            focusedTextColor = fieldTextColor,
            unfocusedTextColor = fieldTextColor,
            disabledTextColor = fieldTextColor,

            focusedLabelColor = fieldLabelColor,
            unfocusedLabelColor = fieldLabelColor,
            disabledLabelColor = fieldLabelColor,

            focusedBorderColor = fieldBorderColor,
            unfocusedBorderColor = fieldBorderColor,
            disabledBorderColor = fieldBorderColor,

            cursorColor = fieldTextColor
        )

    // צבעי כרטיסים ממקור האמת הגלובלי
    val cardContainerColor =
        MaterialTheme.colorScheme.surfaceVariant

    val cardBorderColor =
        MaterialTheme.colorScheme.outlineVariant

    val primaryContentColor =
        MaterialTheme.colorScheme.onSurface

    val secondaryContentColor =
        MaterialTheme.colorScheme.onSurfaceVariant

    // צבעי רשימת המתאמנים
    val recipientListColor =
        MaterialTheme.colorScheme.surface

    val selectedRecipientColor =
        MaterialTheme.colorScheme.secondaryContainer

    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalLayoutDirection provides layoutDirection
    ) {
        Scaffold(
            topBar = {
                il.kmi.app.ui.KmiTopBar(
                    title = coachBroadcastTr(
                        isEnglish,
                        "שידור הודעה לקבוצה",
                        "Broadcast Message"
                    ),
                    titleScale = 0.95f,
                    onBack = onBack,
                    onHome = onHome,
                    onOpenDrawer = {
                        il.kmi.app.ui.DrawerBridge.open()
                    },
                    showRoleStatus = false,
                    lockSearch = false,

                    // ✅ לא נועל את הבית בסרגל האייקונים התחתון.
                    // ✅ רק מסתיר את אייקון הבית הקטן ליד הכותרת העליונה.
                    lockHome = false,
                    showTopHome = false,

                    showBottomActions = true,

                    showTopShare = true,
                    onShare = {
                        runCatching {
                            shareCoachBroadcastPdf(
                                context = contextLang,
                                region = region,
                                branch = branch,
                                groups = effectiveGroupKeys,
                                message = if (demoPrivacyEnabled) {
                                    coachBroadcastTr(
                                        isEnglish,
                                        "תוכן ההודעה מוסתר במצב הדגמה",
                                        "Message content hidden in demo mode"
                                    )
                                } else {
                                    message
                                },
                                recipients = selectedUiRecipients,
                                totalRecipients = selectedRecipients.size,
                                isEnglish = isEnglish,
                                demoPrivacyEnabled = demoPrivacyEnabled
                            )
                        }.onFailure {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    coachBroadcastTr(
                                        isEnglish,
                                        "לא ניתן ליצור או לשתף את קובץ ה־PDF. נסה שוב.",
                                        "Unable to create or share the PDF. Please try again."
                                    )
                                )
                            }
                        }
                    },

                    currentLang =
                        if (isEnglish) {
                            "en"
                        } else {
                            "he"
                        },

                    onToggleLanguage = {
                    val newLang =
                        if (currentLanguage == AppLanguage.HEBREW) {
                            AppLanguage.ENGLISH
                        } else {
                            AppLanguage.HEBREW
                        }

                    langManager.setLanguage(newLang)
                    currentLanguage = newLang
                    (contextLang as? Activity)?.recreate()
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(left = 0)
        ) { pad ->
            Column(
                modifier = Modifier
                    .padding(pad)
                    .fillMaxSize()
                    .background(kmiScreenBackgroundBrush())
                    .imePadding()
                    .navigationBarsPadding()
            ) {

                // =====================================================
                // תת־כותרת קבועה — זהה לשאר המסכים
                // =====================================================

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .kmiSectionHeaderBackground(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = coachBroadcastTr(
                            isEnglish,
                            "פרטי ההודעה",
                            "Message details"
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        style = KmiTypography.sectionTitle.copy(
                            fontWeight =
                                androidx.compose.ui.text.font.FontWeight.ExtraBold
                        ),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }

                // =====================================================
                // רק התוכן שמתחת נגלל
                // =====================================================

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
                    color = cardContainerColor,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    border = BorderStroke(
                        width = 1.dp,
                        color = cardBorderColor
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // ===== אזור =====
                        KmiPremiumDropdown(
                            title =
                                coachBroadcastTr(
                                    isEnglish,
                                    "אזור",
                                    "Region"
                                ),
                            options =
                                visibleBranchesByRegion
                                    .keys
                                    .toList(),
                            selectedValue = region,
                            isEnglish = isEnglish,
                            placeholder =
                                coachBroadcastTr(
                                    isEnglish,
                                    "בחר אזור",
                                    "Select region"
                                ),
                            onSelected = { selectedRegion ->
                                region = selectedRegion

                                /*
                                 * אם קיים סניף יחיד באזור הוא נבחר
                                 * אוטומטית. אחרת נדרשת בחירה ידנית.
                                 */
                                branch =
                                    visibleBranchesByRegion[
                                        selectedRegion
                                    ]
                                        .orEmpty()
                                        .singleOrNull()
                                        .orEmpty()

                                selectedTargetGroups = emptySet()
                                availableBranchGroups = emptyList()
                                availableBranchGroupCounts = emptyMap()
                                recipients = emptyList()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // ===== סניף =====
                        if (region.isNotBlank()) {
                            KmiPremiumDropdown(
                                title =
                                    coachBroadcastTr(
                                        isEnglish,
                                        "סניף",
                                        "Branch"
                                    ),
                                options =
                                    visibleBranchesByRegion[
                                        region
                                    ].orEmpty(),
                                selectedValue = branch,
                                isEnglish = isEnglish,
                                placeholder =
                                    coachBroadcastTr(
                                        isEnglish,
                                        "בחר סניף",
                                        "Select branch"
                                    ),
                                onSelected = { selectedBranch ->
                                    branch = selectedBranch

                                    // מעבר לסניף אחר מאפס רק מידע
                                    // שתלוי בסניף הקודם.
                                    selectedTargetGroups = emptySet()
                                    availableBranchGroups = emptyList()
                                    availableBranchGroupCounts = emptyMap()
                                    recipients = emptyList()
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // ===== טקסט ההודעה =====
                        OutlinedTextField(
                            value = message,
                            onValueChange = { message = it },
                            label = {
                                Text(
                                    coachBroadcastTr(isEnglish, "טקסט ההודעה", "Message text"),
                                    textAlign = screenTextAlign
                                )
                            },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle =
                                KmiTypography.body.copy(
                                    textAlign =
                                        screenTextAlign,
                                    color =
                                        fieldTextColor
                                ),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                            colors = fieldColors
                        )
                    }
                }

                if (
                    branch.isNotBlank() &&
                    availableBranchGroups.isNotEmpty()
                ) {
                    Surface(
                        modifier =
                            Modifier.fillMaxWidth(),
                        shape =
                            androidx.compose.foundation
                                .shape
                                .RoundedCornerShape(22.dp),
                        color = cardContainerColor,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                        border =
                            BorderStroke(
                                width = 1.dp,
                                color = cardBorderColor
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text =
                                    coachBroadcastTr(
                                        isEnglish,
                                        "בחירת קבוצות לשליחה",
                                        "Select groups to include"
                                    ),
                                color =
                                    primaryContentColor,
                                style =
                                    KmiTypography.cardTitle,
                                fontWeight =
                                    androidx.compose.ui.text
                                        .font
                                        .FontWeight.ExtraBold,
                                textAlign =
                                    screenTextAlign,
                                modifier =
                                    Modifier.fillMaxWidth()
                            )

                            val areAllGroupsSelected =
                                availableBranchGroups.isNotEmpty() &&
                                        selectedTargetGroups.containsAll(availableBranchGroups)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Surface(
                                    onClick = {
                                        sendScope = "groups"

                                        selectedTargetGroups =
                                            if (areAllGroupsSelected) {
                                                emptySet()
                                            } else {
                                                availableBranchGroups.toSet()
                                            }

                                        recipients = emptyList()
                                        isRecipientsLoading = true
                                    },
                                    modifier =
                                        Modifier
                                            .widthIn(
                                                min = 210.dp,
                                                max = 280.dp
                                            )
                                            .heightIn(
                                                min = 58.dp
                                            ),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                                    color = if (areAllGroupsSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.secondary
                                    },
                                    shadowElevation = 0.dp,
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (areAllGroupsSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.secondary
                                        }
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(
                                                horizontal = 10.dp,
                                                vertical = 5.dp
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text =
                                                    if (
                                                        areAllGroupsSelected
                                                    ) {
                                                        "↩️"
                                                    } else {
                                                        "☑️"
                                                    },
                                                style =
                                                    KmiTypography.action,
                                                textAlign =
                                                    TextAlign.Center,
                                                maxLines = 1
                                            )

                                            Spacer(Modifier.width(7.dp))

                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    text = if (areAllGroupsSelected) {
                                                        coachBroadcastTr(
                                                            isEnglish,
                                                            "בטל סימון לכולם",
                                                            "Unselect all"
                                                        )
                                                    } else if (availableBranchGroups.size == 1) {
                                                        coachBroadcastTr(
                                                            isEnglish,
                                                            "בחר את קבוצת ${availableBranchGroups.first()}",
                                                            "Select ${availableBranchGroups.first()}"
                                                        )
                                                    } else {
                                                        coachBroadcastTr(
                                                            isEnglish,
                                                            "סמן את כולם",
                                                            "Select all"
                                                        )
                                                    },
                                                    textAlign =
                                                        TextAlign.Center,
                                                    color =
                                                        if (areAllGroupsSelected) {
                                                            MaterialTheme.colorScheme.onPrimary
                                                        } else {
                                                            MaterialTheme.colorScheme.onSecondary
                                                        },
                                                    style =
                                                        KmiTypography.action,
                                                    fontWeight =
                                                        androidx.compose.ui.text.font.FontWeight.ExtraBold,
                                                    maxLines = 2,
                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                )

                                                Text(
                                                    text = if (areAllGroupsSelected) {
                                                        coachBroadcastTr(
                                                            isEnglish,
                                                            "הקבוצה מסומנת לשליחה",
                                                            "Group selected for sending"
                                                        )
                                                    } else if (availableBranchGroups.size == 1) {
                                                        coachBroadcastTr(
                                                            isEnglish,
                                                            "לחץ כאן או סמן את הריבוע למטה",
                                                            "Tap here or tick the checkbox below"
                                                        )
                                                    } else {
                                                        coachBroadcastTr(
                                                            isEnglish,
                                                            "${availableBranchGroups.size} קבוצות · ${
                                                                availableBranchGroups.sumOf { availableBranchGroupCounts[it] ?: 0 }
                                                            } מתאמנים",
                                                            "${availableBranchGroups.size} groups · ${
                                                                availableBranchGroups.sumOf { availableBranchGroupCounts[it] ?: 0 }
                                                            } trainees"
                                                        )
                                                    },
                                                    textAlign = TextAlign.Center,
                                                    color = if (areAllGroupsSelected) {
                                                        MaterialTheme.colorScheme.onPrimary
                                                    } else {
                                                        MaterialTheme.colorScheme.onSecondary
                                                    },
                                                    style =
                                                        KmiTypography.caption,
                                                    fontWeight =
                                                        androidx.compose.ui.text.font.FontWeight.SemiBold,
                                                    maxLines = 2,
                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                availableBranchGroups.forEach { groupName ->
                                    val isSelected = sendScope != "branch" &&
                                            selectedTargetGroups.contains(groupName)

                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                sendScope = "groups"

                                                selectedTargetGroups =
                                                    if (isSelected) {
                                                        selectedTargetGroups - groupName
                                                    } else {
                                                        selectedTargetGroups + groupName
                                                    }

                                                recipients = emptyList()
                                                isRecipientsLoading = true
                                            },
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.secondaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        },
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.secondary
                                            } else {
                                                MaterialTheme.colorScheme.outlineVariant
                                            }
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                horizontalAlignment = if (isEnglish) {
                                                    Alignment.Start
                                                } else {
                                                    Alignment.End
                                                },
                                                verticalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Text(
                                                    text = groupName,
                                                    color = if (isSelected) {
                                                        MaterialTheme.colorScheme.onSecondaryContainer
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurface
                                                    },
                                                    style =
                                                        KmiTypography.cardTitle,
                                                    fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                                                    textAlign = screenTextAlign,
                                                    modifier = Modifier.fillMaxWidth()
                                                )

                                                Text(
                                                    text = coachBroadcastTr(
                                                        isEnglish,
                                                        "${availableBranchGroupCounts[groupName] ?: 0} מתאמנים",
                                                        "${availableBranchGroupCounts[groupName] ?: 0} trainees"
                                                    ),
                                                    color = if (isSelected) {
                                                        MaterialTheme.colorScheme.onSecondaryContainer
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                    },
                                                    style = KmiTypography.caption,
                                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                                    textAlign = screenTextAlign,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = if (isSelected) {
                                                        coachBroadcastTr(
                                                            isEnglish,
                                                            "נבחר",
                                                            "Selected"
                                                        )
                                                    } else {
                                                        coachBroadcastTr(
                                                            isEnglish,
                                                            "בחר",
                                                            "Select"
                                                        )
                                                    },
                                                    color = if (isSelected) {
                                                        kmiSuccessColor()
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                    },
                                                    style =
                                                        KmiTypography.secondary,
                                                    fontWeight =
                                                        androidx.compose.ui.text.font.FontWeight.ExtraBold,
                                                    maxLines = 2
                                                )

                                                Surface(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .clickable {
                                                            sendScope = "groups"

                                                            selectedTargetGroups =
                                                                if (isSelected) {
                                                                    selectedTargetGroups - groupName
                                                                } else {
                                                                    selectedTargetGroups + groupName
                                                                }

                                                            recipients = emptyList()
                                                            isRecipientsLoading = true
                                                        },
                                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
                                                    color = if (isSelected) {
                                                        MaterialTheme.colorScheme.primary
                                                    } else {
                                                        MaterialTheme.colorScheme.surface
                                                    },
                                                    border = BorderStroke(
                                                        width = 1.4.dp,
                                                        color = if (isSelected) {
                                                            MaterialTheme.colorScheme.primary
                                                        } else {
                                                            MaterialTheme.colorScheme.outline
                                                        }
                                                    )
                                                ) {
                                                    Box(
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        if (isSelected) {
                                                            Text(
                                                                text = "✓",
                                                                color =
                                                                    MaterialTheme.colorScheme.onPrimary,
                                                                style =
                                                                    KmiTypography.action,
                                                                fontWeight =
                                                                    androidx.compose.ui.text.font.FontWeight.ExtraBold,
                                                                textAlign =
                                                                    TextAlign.Center
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Text(
                                text = if (selectedTargetGroups.isEmpty()) {
                                    coachBroadcastTr(
                                        isEnglish,
                                        "סמן את הריבוע ליד הקבוצה כדי להציג את המתאמנים ולשלוח הודעה.",
                                        "Tick the group checkbox to show trainees and send a message."
                                    )
                                } else {
                                    coachBroadcastTr(
                                        isEnglish,
                                        "ההודעה תישלח רק למתאמנים בקבוצות שסומנו.",
                                        "The message will be sent only to trainees in the selected groups."
                                    )
                                },
                                color =
                                    if (selectedTargetGroups.isEmpty()) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        secondaryContentColor
                                    },
                                style =
                                    KmiTypography.caption,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                                textAlign = screenTextAlign,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // ===== רשימת נמענים מהקבוצה =====
                if (isRecipientsLoading) {
                    Surface(
                        modifier =
                            Modifier.fillMaxWidth(),
                        shape =
                            androidx.compose.foundation
                                .shape
                                .RoundedCornerShape(18.dp),
                        color = cardContainerColor,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                        border =
                            BorderStroke(
                                width = 1.dp,
                                color = cardBorderColor
                            )
                    ) {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = 16.dp,
                                        vertical = 18.dp
                                    ),
                            horizontalAlignment =
                                Alignment.CenterHorizontally,
                            verticalArrangement =
                                Arrangement.spacedBy(10.dp)
                        ) {
                            KmiLoadingRings(
                                size = 64.dp
                            )

                            Text(
                                text =
                                    coachBroadcastTr(
                                        isEnglish,
                                        if (
                                            selectedTargetGroups
                                                .isEmpty()
                                        ) {
                                            "טוען קבוצות בסניף..."
                                        } else {
                                            "טוען מתאמנים בקבוצות שנבחרו..."
                                        },
                                        if (
                                            selectedTargetGroups
                                                .isEmpty()
                                        ) {
                                            "Loading branch groups..."
                                        } else {
                                            "Loading trainees in selected groups..."
                                        }
                                    ),
                                color = primaryContentColor,
                                style =
                                    KmiTypography.secondary,
                                fontWeight =
                                    androidx.compose.ui.text
                                        .font
                                        .FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else if (recipients.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isEnglish) Arrangement.Start else Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = {
                                val newValue = !allSelected
                                recipients = recipients.map { it.copy(selected = newValue) }
                            },
                            modifier =
                                Modifier.widthIn(
                                    min = 200.dp,
                                    max = 280.dp
                                ),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                            border = BorderStroke(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline
                            ),
                            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Text(
                                text =
                                    if (allSelected) {
                                        coachBroadcastTr(
                                            isEnglish,
                                            "בטל סימון לכולם",
                                            "Unselect all"
                                        )
                                    } else {
                                        coachBroadcastTr(
                                            isEnglish,
                                            "סמן את כל חברי הקבוצה",
                                            "Select all group members"
                                        )
                                    },
                                style =
                                    KmiTypography.action,
                                fontWeight =
                                    androidx.compose.ui.text.font.FontWeight.Bold,
                                textAlign =
                                    TextAlign.Center
                            )
                        }
                    }

                    Spacer(Modifier.height(2.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = recipientListColor,
                                shape =
                                    androidx.compose.foundation
                                        .shape
                                        .RoundedCornerShape(18.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = cardBorderColor,
                                shape =
                                    androidx.compose.foundation
                                        .shape
                                        .RoundedCornerShape(18.dp)
                            )
                            .padding(8.dp),
                        horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
                    ) {
                        uiRecipients.forEach { r ->
                            val isSelected = r.selected

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                                color =
                                    if (isSelected) {
                                        selectedRecipientColor
                                    } else {
                                        Color.Transparent
                                    },
                                tonalElevation = 0.dp,
                                border = if (isSelected) {
                                    BorderStroke(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                } else null
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
                                    ) {
                                        Text(
                                            text = r.name,
                                            color =
                                                if (isSelected) {
                                                    MaterialTheme.colorScheme.onSecondaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.onSurface
                                                },
                                            textAlign = screenTextAlign,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Text(
                                            text =
                                                if (
                                                    demoPrivacyEnabled
                                                ) {
                                                    coachBroadcastTr(
                                                        isEnglish,
                                                        "מספר מוסתר",
                                                        "Hidden number"
                                                    )
                                                } else {
                                                    r.phone.ifBlank {
                                                        coachBroadcastTr(
                                                            isEnglish,
                                                            "ללא מספר טלפון",
                                                            "No phone number"
                                                        )
                                                    }
                                                },
                                            style =
                                                KmiTypography.caption,
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.onSecondaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            textAlign = screenTextAlign,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    Checkbox(
                                        checked = r.selected,
                                        onCheckedChange = { checked ->
                                            recipients = recipients.map {
                                                if (it.uid == r.uid) it.copy(selected = checked) else it
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                } else if (region.isNotBlank() && branch.isNotBlank()) {
                    Text(
                        text = if (recipientsLoadFailed) {
                            coachBroadcastTr(
                                isEnglish,
                                "טעינת הנתונים נכשלה. בדוק את החיבור לאינטרנט ונסה שוב.",
                                "Failed to load data. Check your internet connection and try again."
                            )
                        } else {
                            coachBroadcastTr(
                                isEnglish,
                                if (selectedTargetGroups.isEmpty()) {
                                    "לא נבחרו קבוצות לשליחה."
                                } else {
                                    "לא נמצאו מתאמנים פעילים לקבוצות שנבחרו בסניף הזה."
                                },
                                if (selectedTargetGroups.isEmpty()) {
                                    "No groups were selected for sending."
                                } else {
                                    "No active trainees were found for the selected groups in this branch."
                                }
                            )
                        },
                        color = if (recipientsLoadFailed) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onBackground
                        },
                        style = KmiTypography.body,
                        textAlign = screenTextAlign,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(10.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text =
                            coachBroadcastTr(
                                isEnglish,
                                "מתאמנים בקבוצות שנבחרו: ${recipients.size}",
                                "Trainees in selected groups: ${recipients.size}"
                            ),
                        color = MaterialTheme.colorScheme.onSurface,
                        style =
                            KmiTypography.cardTitle,
                        fontWeight =
                            androidx.compose.ui.text
                                .font
                                .FontWeight.Bold,
                        textAlign = screenTextAlign,
                        modifier =
                            Modifier.fillMaxWidth()
                    )

                    Text(
                        text =
                            coachBroadcastTr(
                                isEnglish,
                                "מתאמנים נבחרים: ${selectedUids.size}",
                                "Selected trainees: ${selectedUids.size}"
                            ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style =
                            KmiTypography.cardTitle,
                        fontWeight =
                            androidx.compose.ui.text
                                .font
                                .FontWeight.Bold,
                        textAlign = screenTextAlign,
                        modifier =
                            Modifier.fillMaxWidth()
                    )
                }

                    // שליחת Push בלבד דרך Cloud Function.
                    androidx.compose.material3.Button(
                    onClick = {
                        scope.launch {
                            if (isSending) {
                                return@launch
                            }

                            when {
                                message.isBlank() -> {
                                    snackbarHostState.showSnackbar(
                                        coachBroadcastTr(
                                            isEnglish,
                                            "נא לכתוב טקסט להודעה",
                                            "Please write a message"
                                        )
                                    )
                                }

                                selectedUids.isEmpty() -> {
                                    snackbarHostState.showSnackbar(
                                        coachBroadcastTr(
                                            isEnglish,
                                            "לא נבחרו נמענים – סמן לפחות מתאמן אחד",
                                            "No recipients selected — select at least one trainee"
                                        )
                                    )
                                }
                                else -> {
                                    isSending = true

                                    saveBroadcast(
                                        messageToSend = message.trim(),
                                        uidsToSend = selectedUids.toList(),
                                        groupsToSend = effectiveGroupKeys.toList()
                                    )
                                }
                            }
                        }
                    },
                    enabled =
                        !isSending &&
                                !isRecipientsLoading &&
                                !recipientsLoadFailed &&
                                branch.isNotBlank() &&
                                message.isNotBlank() &&
                                selectedUids.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 58.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    elevation =
                        androidx.compose.material3
                            .ButtonDefaults
                            .buttonElevation(
                                defaultElevation = 0.dp,
                                pressedElevation = 0.dp,
                                focusedElevation = 0.dp,
                                hoveredElevation = 0.dp,
                                disabledElevation = 0.dp
                            ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    if (isSending) {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                            horizontalArrangement =
                                Arrangement.Center,
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {
                            KmiLoadingRings(
                                size = 30.dp
                            )

                            Spacer(
                                Modifier.width(10.dp)
                            )

                            Text(
                                text =
                                    coachBroadcastTr(
                                        isEnglish,
                                        "שולח הודעה...",
                                        "Sending message..."
                                    ),
                                color = MaterialTheme.colorScheme.onPrimary,
                                style =
                                    KmiTypography.action,
                                fontWeight =
                                    androidx.compose.ui.text
                                        .font
                                        .FontWeight.Bold
                            )
                        }
                    } else {
                        Text(
                            text = sendButtonText,
                            color = if (
                                !isRecipientsLoading &&
                                !recipientsLoadFailed &&
                                branch.isNotBlank() &&
                                message.isNotBlank() &&
                                selectedUids.isNotEmpty()
                            ) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            style = KmiTypography.action,
                            fontWeight =
                                androidx.compose.ui.text.font.FontWeight.Bold,
                            maxLines = 2,
                            textAlign =
                                TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(Modifier.height(84.dp))
                }
            }
        }
    }
}