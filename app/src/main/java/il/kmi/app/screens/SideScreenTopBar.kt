@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)

package il.kmi.app.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import il.kmi.app.ui.KmiTopBar
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import shareCurrentScreen
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager

//========================================================================

/**
 * TopBar למסכי צד:
 * - כותרת ממורכזת
 * - כפתור X לסגירה (onClose)
 * - בלי המבורגר/Back
 * - משאיר את הסרגל התחתון (BottomActionsBarEdgeToEdge)
 * - חיפוש נעול עד רישום (lockSearch=true), בית לא נעול (lockHome=false)
 */
@Composable
fun SideScreenTopBar(
    title: String,
    onClose: () -> Unit
) {
    var showAiDialog by rememberSaveable { mutableStateOf(false) }
    KmiTopBar(
        title = title,
        centerTitle = true,
        showMenu = false,
        onBack = null,
        showBottomActions = true,
        showRoleBadge = false,
        showModePill = false,
        lockSearch = true,
        lockHome = false,
        // כפתור X בצד ימין
        extraActions = {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "סגור",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

/* =========================================================
   ☆ סרגל האייקונים התחתון — מופרד מקובץ KmiTopBar הגדול ☆
   ========================================================= */

@Composable
fun BottomActionsBarEdgeToEdge(
    onHome: (() -> Unit)?,
    onSearch: (() -> Unit)?,
    onToggleLanguage: (() -> Unit)? = null,      // שמור לעתיד אם צריך
    onSettings: (() -> Unit)?,
    currentLang: String = "he",
    onShare: (() -> Unit)?,
    onShareWhatsApp: (() -> Unit)? = null,       // לא בשימוש כאן (אפשר לחבר בהמשך)
    onTts: (() -> Unit)? = null,                 // נגישות קולית (אופציונלי)
    onFont: (() -> Unit)? = null,                // לוח נגישות (אופציונלי)
    onNext: (() -> Unit)? = null,                // לא בשימוש כאן (מסכים רציפים)
    whatsAppIconRes: Int? = null,                // לא בשימוש כאן
    accessibilityIconRes: Int? = null,           // אייקון חלופי לנגישות אם קיים
    searchProvider: ((String) -> List<il.kmi.app.domain.ContentRepo.SearchHit>)? = null,
    onPickSearchResult: ((String) -> Unit)? = null,
    onOpenAi: (() -> Unit)? = null,
    showCoachBroadcastAction: Boolean = false,   // true → מציג אייקון "שידור" (מאמן)
    onCoachBroadcastClick: (() -> Unit)? = null, // ייפתח ע"י הקומפוזבל העוטף (KmiTopBar)
    // מצב
    isRegistered: Boolean = true,
    homeEnabled: Boolean = true,
    homeDisabledToast: String? = null
) {
    val ctx = LocalContext.current
    val isEnglish = currentLang.equals("en", ignoreCase = true)

    val tSearch = if (isEnglish) "Search" else "חיפוש"
    val tHome = if (isEnglish) "Home" else "בית"
    val tSettings = if (isEnglish) "Settings" else "הגדרות"
    val tAssistant = if (isEnglish) "Assistant" else "עוזר"
    val tShare = if (isEnglish) "Share" else "שתף"
    val tBroadcast = if (isEnglish) "Broadcast" else "שידור"

    val tSearchLocked = if (isEnglish) {
        "Search is available after login/registration"
    } else {
        "החיפוש זמין לאחר כניסה/רישום"
    }

    val tHomeLocked = homeDisabledToast ?: if (isEnglish) {
        "Home screen will be available after login/registration"
    } else {
        "מסך הבית יהיה זמין לאחר כניסה/רישום"
    }

    val tBroadcastLocked = if (isEnglish) {
        "Broadcast will be available after login/registration"
    } else {
        "שידור יהיה זמין לאחר כניסה/רישום"
    }

    val tSearchExercise = if (isEnglish) {
        "Search exercise (e.g. kick, defense)"
    } else {
        "חפש תרגיל (למשל: בעיטה, הגנה)"
    }

    val tClearSearch = if (isEnglish) "Clear search" else "נקה חיפוש"

    val tSearchErrorPrefix = if (isEnglish) {
        "Search error:"
    } else {
        "שגיאה בעת חיפוש:"
    }

    val tSearchHint = if (isEnglish) {
        "Type a word or phrase to see matching exercises."
    } else {
        "הקלד מילה או ביטוי כדי לראות תרגילים תואמים."
    }

    val tNoExercisesFound = if (isEnglish) {
        "No exercises found for"
    } else {
        "לא נמצאו תרגילים עבור"
    }

    val tShareSubject = if (isEnglish) {
        "K.M.I – Krav Magen Israeli"
    } else {
        "ק.מ.י – קרב מגן ישראלי"
    }

    val tShareChooser = if (isEnglish) "Share via" else "שתף באמצעות"
    /* --- state לחיפוש (כולל שכבה מודאלית) --- */
    var showSearch by rememberSaveable(isRegistered) { mutableStateOf(false) }
    LaunchedEffect(isRegistered) {
        if (!isRegistered && showSearch) showSearch = false
    }

    var query by remember { mutableStateOf("") }          // ערך אחרי debounce
    var rawQuery by remember { mutableStateOf("") }       // מה שמוקלד כרגע
    var debounceJob by remember { mutableStateOf<Job?>(null) }

    data class SearchUiState(
        val results: List<il.kmi.app.domain.ContentRepo.SearchHit> = emptyList(),
        val error: String? = null
    )
    val uiState: SearchUiState = remember(query, searchProvider) {
        val q = query.trim()
        if (q.isEmpty()) SearchUiState()
        else runCatching { searchProvider?.invoke(q) ?: emptyList() }
            .fold(
                onSuccess = { SearchUiState(results = it) },
                onFailure = { SearchUiState(error = it.message ?: "אירעה שגיאה") }
            )
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val barColor = Color.White
    val contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    val disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp),
        shape = RectangleShape,
        color = barColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        val iconSize = 24.dp

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .height(64.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // 1) חיפוש (נעול עד רישום)
            val canSearch = isRegistered
            BarAction(
                label = tSearch,
                onClick = {
                    if (!canSearch) {
                        android.widget.Toast
                            .makeText(ctx, tSearchLocked, android.widget.Toast.LENGTH_SHORT)
                            .show()
                        return@BarAction
                    }
                    showSearch = true
                    onSearch?.invoke()
                },
                enabled = true // משאיר פעיל כדי שיופיע טוסט גם כשהוא "נעול"
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = tSearch,
                    tint = if (canSearch) contentColor else disabledContentColor,
                    modifier = Modifier.size(iconSize)
                )
            }

            // 2) בית
            BarAction(
                label = tHome,
                onClick = {
                    if (!homeEnabled) {
                        val msg = tHomeLocked
                        android.widget.Toast
                            .makeText(ctx, msg, android.widget.Toast.LENGTH_SHORT)
                            .show()
                        return@BarAction
                    }
                    onHome?.invoke()
                },
                enabled = true // טוסט גם כשהוא נעול
            ) {
                Icon(
                    imageVector = Icons.Filled.Home,
                    contentDescription = tHome,
                    tint = if (homeEnabled) contentColor else disabledContentColor,
                    modifier = Modifier.size(iconSize)
                )
            }

            // 3) הגדרות
            BarAction(
                label = tSettings,
                onClick = {
                    if (onSettings != null) {
                        onSettings()
                    } else {
                        android.widget.Toast
                            .makeText(
                                ctx,
                                if (isEnglish) "Settings is not connected yet" else "מסך ההגדרות עדיין לא מחובר",
                                android.widget.Toast.LENGTH_SHORT
                            )
                            .show()
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = tSettings,
                    tint = contentColor,
                    modifier = Modifier.size(iconSize)
                )
            }

            // 4) עוזר חכם (AI) ✅ חדש
            if (onOpenAi != null) {
                BarAction(
                    label = tAssistant,
                    onClick = { onOpenAi() }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lightbulb,
                        contentDescription = tAssistant,
                        tint = contentColor,
                        modifier = Modifier.size(iconSize)
                    )
                }
            }

            // 5) שתף (שיתוף צילום מסך או fallback לטקסט)
            BarAction(tShare, {
                if (onShare != null) {
                    onShare()
                } else {
                    val activity = (ctx as? Activity)
                    val root = activity?.window?.decorView?.rootView
                    if (root != null) {
                        runCatching {
                            shareCurrentScreen(
                                context = ctx,
                                rootView = root,
                                targetPackage = null,
                                subject = "ק.מ.י – קרב מגן ישראלי"
                            )
                        }.onFailure { shareAppDefault(ctx) }
                    } else {
                        shareAppDefault(ctx)
                    }
                }
            }) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = tShare,
                    tint = contentColor,
                    modifier = Modifier.size(iconSize)
                )
            }

            // 6) שידור (מאמן בלבד)
            if (showCoachBroadcastAction) {
                val canBroadcast = isRegistered
                BarAction(
                    label = tBroadcast,
                    onClick = {
                        if (!canBroadcast) {
                            android.widget.Toast
                                .makeText(ctx, "שידור יהיה זמין לאחר כניסה/רישום", android.widget.Toast.LENGTH_SHORT)
                                .show()
                            return@BarAction
                        }
                        onCoachBroadcastClick?.invoke()
                    },
                    enabled = true
                ) {
                    Icon(
                        imageVector = Icons.Filled.Chat,
                        contentDescription = tBroadcast,
                        tint = if (canBroadcast) contentColor else disabledContentColor,
                        modifier = Modifier.size(iconSize)
                    )
                }
            }

            // 7) החלפת שפה
            if (onToggleLanguage != null) {

                val nextLang = if (currentLang == "he") "EN" else "עב"

                BarAction(
                    label = "",
                    onClick = { onToggleLanguage() }
                ) {

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        modifier = Modifier.size(32.dp)
                    ) {

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {

                            Text(
                                text = nextLang,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                        }
                    }
                }
            }
        }
    }

    // ===== שכבת חיפוש =====
    if (showSearch && isRegistered) {
        val scope = rememberCoroutineScope()
        val focusRequester = remember { FocusRequester() }
        val focusManager = LocalFocusManager.current

        LaunchedEffect(Unit) {
            rawQuery = ""
            query = ""
            debounceJob?.cancel()
        }
        LaunchedEffect(showSearch) {
            if (showSearch) {
                delay(100)
                focusRequester.requestFocus()
            }
        }

        ModalBottomSheet(
            onDismissRequest = { showSearch = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                var isSearching by remember { mutableStateOf(false) }

                OutlinedTextField(
                    value = rawQuery,
                    onValueChange = { newText ->
                        rawQuery = newText
                        isSearching = true
                        debounceJob?.cancel()
                        debounceJob = scope.launch {
                            delay(180)
                            query = newText
                            isSearching = false
                        }
                    },
                    singleLine = true,
                    label = { Text(tSearchExercise) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            debounceJob?.cancel()
                            query = rawQuery
                            isSearching = false
                            focusManager.clearFocus()
                        }
                    ),
                    trailingIcon = {
                        androidx.compose.animation.Crossfade(
                            targetState = Pair(isSearching, rawQuery.isNotBlank())
                        ) { (loading, hasText) ->
                            when {
                                loading -> {
                                    CircularProgressIndicator(
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                hasText -> {
                                    IconButton(onClick = {
                                        debounceJob?.cancel()
                                        rawQuery = ""
                                        query = ""
                                        isSearching = false
                                        scope.launch {
                                            delay(10)
                                            focusRequester.requestFocus()
                                        }
                                    }) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = tClearSearch
                                        )
                                    }
                                }
                            }
                        }
                    }
                )

                AnimatedVisibility(visible = isSearching) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                    )
                }

                Spacer(Modifier.height(12.dp))

                val results = uiState.results
                when {
                    uiState.error != null -> {
                        Text(
                            "שגיאה בעת חיפוש: ${uiState.error}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    query.isBlank() -> {
                        Text(
                            tSearchHint,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    results.isEmpty() -> {
                        Text(
                            "$tNoExercisesFound: $query",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = results,
                                key  = { it.id ?: it.title }
                            ) { hit ->
                                val key = hit.id ?: hit.title
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            // נחזיר מפתח "יציב" אם יש resolve
                                            val resolved = il.kmi.app.domain.ContentRepo.resolveItemKey(key.trim())
                                            val stableKey: String = if (resolved != null) {
                                                val fixedSub: String =
                                                    il.kmi.app.domain.ContentRepo.findSubTopicTitleForItem(
                                                        belt = resolved.belt,
                                                        topicTitle = resolved.topicTitle,
                                                        itemTitle = resolved.itemTitle
                                                    ) ?: (resolved.subTopicTitle ?: "")
                                                listOf(
                                                    resolved.belt.name,
                                                    resolved.topicTitle,
                                                    fixedSub,
                                                    resolved.itemTitle
                                                ).joinToString("::")
                                            } else key.trim()

                                            scope.launch {
                                                runCatching { sheetState.hide() }
                                                showSearch = false
                                                onPickSearchResult?.invoke(stableKey)
                                            }
                                        }
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)
                                        )
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = hit.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (!hit.subtitle.isNullOrBlank()) {
                                        Text(
                                            text = hit.subtitle!!,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Divider()
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

/* ----------------- Helpers ----------------- */

@Composable
fun BarAction(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 0.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(36.dp)) { content() }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 0.8f else 0.4f)
        )
    }
}

@Composable
fun BarAction(
    icon: ImageVector,
    contentDescription: String? = null,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = modifier) {
        Icon(imageVector = icon, contentDescription = contentDescription)
    }
}

private fun shareAppDefault(ctx: Context, text: String? = null) {
    val isEnglish = AppLanguageManager(ctx).getCurrentLanguage() == AppLanguage.ENGLISH

    val shareText = text ?: if (isEnglish) {
        "Download the KMI app"
    } else {
        "הורידו את אפליקציית ק.מ.י"
    }

    val chooserTitle = if (isEnglish) {
        "Share via"
    } else {
        "שתף באמצעות"
    }

    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    val chooser = Intent.createChooser(send, chooserTitle)
    if (ctx !is Activity) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    ctx.startActivity(chooser)
}
