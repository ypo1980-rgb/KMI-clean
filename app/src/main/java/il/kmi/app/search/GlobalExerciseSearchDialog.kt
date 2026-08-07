@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)

package il.kmi.app.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import il.kmi.app.domain.ContentRepo
import il.kmi.app.ui.KmiIconSize
import il.kmi.shared.domain.Belt
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer

/**
 * חלון החיפוש הגלובלי של תרגילי ק.מ.י.
 *
 * החיפוש עצמו מתבצע ב-[GlobalExerciseSearchEngine]. המסך אחראי
 * רק לקלט, להצגת התוצאות ולדיווח על התרגיל שנבחר.
 */
@Composable
fun GlobalExerciseSearchDialog(
    isEnglish: Boolean,
    onDismiss: () -> Unit,
    onExerciseSelected:
        (GlobalExerciseSearchEngine.Result) -> Unit,
    modifier: Modifier = Modifier,
    initialQuery: String = ""
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    val keyboardController =
        LocalSoftwareKeyboardController.current

    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    val isKeyboardVisible =
        WindowInsets.isImeVisible

    var query by rememberSaveable(initialQuery) {
        mutableStateOf(initialQuery)
    }

    var speechError by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    val results = remember(
        query,
        isEnglish
    ) {
        searchExercisesWithHebrewVariants(
            query = query,
            isEnglish = isEnglish
        )
    }

    val direction =
        if (isEnglish) {
            LayoutDirection.Ltr
        } else {
            LayoutDirection.Rtl
        }

    val textAlign =
        if (isEnglish) {
            TextAlign.Left
        } else {
            TextAlign.Right
        }

    fun finishTyping() {
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
    }

    val speechState =
        rememberExerciseSearchSpeechState(
            isEnglish = isEnglish,
            onPartialResult = { recognizedText ->
                query =
                    GlobalExerciseSearchEngine
                        .normalizeSpokenQuery(
                            recognizedText
                        )
                        .replace("\n", " ")
                        .replace("\r", " ")
                        .replace(
                            Regex("""\s+"""),
                            " "
                        )
                        .trim()

                speechError = null
            },
            onResult = { recognizedText ->
                val finalQuery =
                    GlobalExerciseSearchEngine
                        .normalizeSpokenQuery(
                            recognizedText
                        )
                        .replace("\n", " ")
                        .replace("\r", " ")
                        .replace(
                            Regex("""\s+"""),
                            " "
                        )
                        .trim()

                query = finalQuery
                speechError = null
                finishTyping()

                val finalResults =
                    searchExercisesWithHebrewVariants(
                        query = finalQuery,
                        isEnglish = isEnglish
                    )

                if (finalResults.size == 1) {
                    onExerciseSelected(
                        finalResults.single()
                    )
                }
            },
            onError = { message ->
                speechError = message
            }
        )

    /*
     * ה־ModalBottomSheet אינו מנהל בעצמו את לחצן החזור.
     *
     * כך לחיצה ראשונה בזמן שהמקלדת פתוחה סוגרת רק אותה,
     * ולחיצה נוספת, לאחר שהמקלדת נסגרה, סוגרת את מסך החיפוש.
     */
    BackHandler {
        if (isKeyboardVisible) {
            finishTyping()
        } else {
            speechState.stopListening()
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            /*
             * הקריאה הזו מגיעה מלחיצה מחוץ ל־sheet
             * או מגרירתו כלפי מטה — לא מלחצן החזור.
             */
            finishTyping()
            speechState.stopListening()
            onDismiss()
        },
        sheetState = sheetState,
        properties = ModalBottomSheetProperties(
            shouldDismissOnBackPress = false
        ),
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 6.dp)
                    .size(
                        width = 46.dp,
                        height = 5.dp
                    )
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                            .copy(alpha = 0.32f)
                    )
            )
        }
    ) {
        CompositionLocalProvider(
            LocalLayoutDirection provides direction
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(
                        horizontal = 18.dp,
                        vertical = 12.dp
                    ),
                verticalArrangement =
                    Arrangement.spacedBy(14.dp)
            ) {
                if (!isKeyboardVisible) {
                    SearchHeader(
                        isEnglish = isEnglish,
                        textAlign = textAlign,
                        isListening = speechState.isListening,
                        onMicrophoneClick = {
                            speechError = null
                            finishTyping()
                            speechState.toggleListening()
                        },
                        onClose = {
                            finishTyping()
                            speechState.stopListening()
                            onDismiss()
                        }
                    )
                }

                SearchInput(
                    query = query,
                    isEnglish = isEnglish,
                    textAlign = textAlign,
                    focusRequester = focusRequester,
                    onQueryChange = { value ->
                        query = value
                            .replace("\n", " ")
                            .replace("\r", " ")
                            .replace(
                                Regex("""\s+"""),
                                " "
                            )
                    },
                    onDone = ::finishTyping
                )

                speechError?.takeIf {
                    it.isNotBlank()
                }?.let { message ->
                    Text(
                        text = message,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = textAlign
                    )
                }

                SearchContent(
                    query = query,
                    results = results,
                    isEnglish = isEnglish,
                    textAlign = textAlign,
                    onExerciseSelected = { result ->
                        finishTyping()
                        onExerciseSelected(result)
                    }
                )

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SearchHeader(
    isEnglish: Boolean,
    textAlign: TextAlign,
    isListening: Boolean,
    onMicrophoneClick: () -> Unit,
    onClose: () -> Unit
) {
    val microphoneTransition =
        rememberInfiniteTransition(
            label = "exercise_search_microphone"
        )

    val microphoneScale by microphoneTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.16f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650),
            repeatMode = RepeatMode.Reverse
        ),
        label = "exercise_search_microphone_scale"
    )

    val microphoneAlpha by microphoneTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 0.68f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650),
            repeatMode = RepeatMode.Reverse
        ),
        label = "exercise_search_microphone_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF10294A),
                        Color(0xFF173B68),
                        Color(0xFF6250C7)
                    )
                )
            )
            .padding(
                horizontal = 16.dp,
                vertical = 12.dp
            )
    ) {
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(30.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription =
                    if (isEnglish) {
                        "Close search"
                    } else {
                        "סגור חיפוש"
                    },
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(18.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 30.dp,
                    end = 30.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isEnglish) {
                    "Search exercise"
                } else {
                    "חיפוש תרגיל"
                },
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                textAlign = textAlign
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (isEnglish) {
                    "Type or say a word to search all exercises."
                } else {
                    "הקלד או אמור מילה כדי לחפש בכל התרגילים."
                },
                modifier = Modifier.fillMaxWidth(),
                color = Color.White.copy(alpha = 0.82f),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = textAlign
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                onClick = onMicrophoneClick,
                modifier = Modifier
                    .size(40.dp)
                    .graphicsLayer {
                        scaleX = microphoneScale
                        scaleY = microphoneScale
                        alpha = microphoneAlpha
                    },
                shape = CircleShape,
                color = if (isListening) {
                    Color(0xFFE53935)
                } else {
                    Color.White.copy(alpha = 0.18f)
                },
                border = BorderStroke(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.42f)
                )
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isListening) {
                            Icons.Default.Stop
                        } else {
                            Icons.Default.Mic
                        },
                        contentDescription = if (isListening) {
                            if (isEnglish) {
                                "Stop listening"
                            } else {
                                "עצור האזנה"
                            }
                        } else {
                            if (isEnglish) {
                                "Search by voice"
                            } else {
                                "חיפוש קולי"
                            }
                        },
                        tint = Color.White,
                        modifier = Modifier.size(21.dp)
                    )
                }
            }

            if (isListening) {
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (isEnglish) {
                        "Listening…"
                    } else {
                        "מאזין…"
                    },
                    color = Color(0xFFFFCDD2),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SearchInput(
    query: String,
    isEnglish: Boolean,
    textAlign: TextAlign,
    focusRequester: FocusRequester,
    onQueryChange: (String) -> Unit,
    onDone: () -> Unit
) {
    val label =
        if (isEnglish) {
            "Search exercise"
        } else {
            "חפש תרגיל"
        }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        tonalElevation = 0.dp,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 62.dp)
                .padding(
                    horizontal = 10.dp,
                    vertical = 6.dp
                )
                .focusRequester(focusRequester),
            singleLine = false,
            minLines = 1,
            maxLines = 4,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(
                onSearch = { onDone() },
                onDone = { onDone() },
                onGo = { onDone() },
                onSend = { onDone() }
            ),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                textAlign = textAlign
            ),
            label = {
                Text(
                    text = label,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = textAlign,
                    fontWeight = FontWeight.SemiBold
                )
            },
            placeholder = {
                Text(
                    text = label,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = textAlign,
                    color = MaterialTheme.colorScheme
                        .onSurfaceVariant
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = label,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(
                        KmiIconSize.medium
                    )
                )
            },
            trailingIcon = {
                if (query.isNotBlank()) {
                    IconButton(
                        onClick = {
                            onQueryChange("")
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription =
                                if (isEnglish) {
                                    "Clear"
                                } else {
                                    "נקה"
                                },
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor =
                    MaterialTheme.colorScheme.surface,
                unfocusedContainerColor =
                    MaterialTheme.colorScheme.surface,
                focusedBorderColor =
                    MaterialTheme.colorScheme.primary,
                unfocusedBorderColor =
                    MaterialTheme.colorScheme.outlineVariant,
                focusedLabelColor =
                    MaterialTheme.colorScheme.primary,
                unfocusedLabelColor =
                    MaterialTheme.colorScheme.onSurfaceVariant,
                cursorColor =
                    MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
private fun SearchContent(
    query: String,
    results: List<GlobalExerciseSearchEngine.Result>,
    isEnglish: Boolean,
    textAlign: TextAlign,
    onExerciseSelected:
        (GlobalExerciseSearchEngine.Result) -> Unit
) {
    when {
        query.trim().length < 2 -> {
            Text(
                text =
                    if (isEnglish) {
                        "Type or say at least two characters."
                    } else {
                        "הקלד או אמור לפחות שני תווים."
                    },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                textAlign = textAlign,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.Bold
            )
        }

        results.isEmpty() -> {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                tonalElevation = 0.dp
            ) {
                Text(
                    text =
                        if (isEnglish) {
                            "No results found: ${query.trim()}"
                        } else {
                            "לא נמצאו תוצאות: ${query.trim()}"
                        },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 16.dp,
                            vertical = 14.dp
                        ),
                    textAlign = textAlign,
                    color = MaterialTheme.colorScheme
                        .onErrorContainer,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        else -> {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        MaterialTheme.colorScheme.surface
                    )
            ) {
                itemsIndexed(
                    items = results,
                    key = { _, result -> result.id }
                ) { index, result ->
                    SearchResultRow(
                        result = result,
                        isEnglish = isEnglish,
                        textAlign = textAlign,
                        onClick = {
                            onExerciseSelected(result)
                        }
                    )

                    if (index != results.lastIndex) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme
                                .outlineVariant,
                            thickness = 0.8.dp,
                            modifier = Modifier.padding(
                                horizontal = 12.dp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    result: GlobalExerciseSearchEngine.Result,
    isEnglish: Boolean,
    textAlign: TextAlign,
    onClick: () -> Unit
) {
    val titleColor = resultTitleColor(
        id = result.id,
        subtitle = result.subtitle
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = 12.dp,
                vertical = 10.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.ChevronLeft,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(KmiIconSize.tiny)
        )

        Spacer(Modifier.width(8.dp))

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment =
                if (isEnglish) {
                    Alignment.Start
                } else {
                    Alignment.End
                }
        ) {
            Text(
                text = result.title,
                modifier = Modifier.fillMaxWidth(),
                textAlign = textAlign,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = titleColor
            )

            if (!result.subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))

                Text(
                    text = result.subtitle.orEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = textAlign,
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme
                        .onSurfaceVariant
                )
            }
        }
    }
}

/**
 * חיפוש תרגילים עם תמיכה בכתיבים עבריים חלופיים.
 *
 * החיפוש מתבצע גם לפי הטקסט המקורי וגם לפי
 * החלפת "צואר" ו־"צוואר", בלי תלות בכתיב
 * שבו נשמר שם התרגיל במאגר.
 */
private fun searchExercisesWithHebrewVariants(
    query: String,
    isEnglish: Boolean
): List<GlobalExerciseSearchEngine.Result> {
    val cleanQuery =
        query
            .replace("\n", " ")
            .replace("\r", " ")
            .replace(
                Regex("""\s+"""),
                " "
            )
            .trim()

    if (cleanQuery.isBlank()) {
        return emptyList()
    }

    /*
     * תחילה יוצרים את שני הכתיבים האפשריים
     * של "צואר" ו־"צוואר".
     */
    val neckSpellingVariants =
        linkedSetOf(
            cleanQuery,
            cleanQuery.replace(
                oldValue = "צוואר",
                newValue = "צואר",
                ignoreCase = true
            ),
            cleanQuery.replace(
                oldValue = "צואר",
                newValue = "צוואר",
                ignoreCase = true
            )
        )

    /*
     * עבור כל כתיב יוצרים גם גרסה ללא מקפים.
     *
     * נתמכים:
     * מקף רגיל: -
     * מקף עברי: ־
     * en dash: –
     * em dash: —
     */
    val queryVariants =
        linkedSetOf<String>().apply {
            neckSpellingVariants.forEach { spellingVariant ->
                add(spellingVariant)

                add(
                    spellingVariant
                        .replace(
                            Regex("""\s*[-־–—]\s*"""),
                            " "
                        )
                        .replace(
                            Regex("""\s+"""),
                            " "
                        )
                        .trim()
                )
            }
        }
            .map { value ->
                value.trim()
            }
            .filter { value ->
                value.isNotBlank()
            }

    return queryVariants
        .flatMap { queryVariant ->
            GlobalExerciseSearchEngine.search(
                query = queryVariant,
                isEnglish = isEnglish
            )
        }
        .distinctBy { result ->
            result.id
        }
}

private fun resultTitleColor(
    id: String,
    subtitle: String?
): Color {
    val resolvedBelt = runCatching {
        ContentRepo.resolveItemKey(id)?.belt
    }.getOrNull()

    return when (resolvedBelt) {
        Belt.YELLOW -> Color(0xFFF59E0B)
        Belt.ORANGE -> Color(0xFFFF9800)
        Belt.GREEN -> Color(0xFF2E7D32)
        Belt.BLUE -> Color(0xFF1E88E5)
        Belt.BROWN -> Color(0xFF6D4C41)
        Belt.BLACK -> Color(0xFF64748B)
        else -> {
            val searchableText =
                "$subtitle $id".lowercase()

            when {
                "צהובה" in searchableText ||
                        "yellow" in searchableText ->
                    Color(0xFFF59E0B)

                "כתומה" in searchableText ||
                        "orange" in searchableText ->
                    Color(0xFFFF9800)

                "ירוקה" in searchableText ||
                        "green" in searchableText ->
                    Color(0xFF2E7D32)

                "כחולה" in searchableText ||
                        "blue" in searchableText ->
                    Color(0xFF1E88E5)

                "חומה" in searchableText ||
                        "brown" in searchableText ->
                    Color(0xFF6D4C41)

                else -> Color(0xFF334155)
            }
        }
    }
}
