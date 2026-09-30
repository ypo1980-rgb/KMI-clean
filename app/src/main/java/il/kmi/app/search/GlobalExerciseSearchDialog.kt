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
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material.icons.filled.ChevronRight
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
import il.kmi.app.domain.ContentRepo
import il.kmi.app.ui.KmiIconSize
import il.kmi.app.ui.KmiTypography
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
        containerColor = Color.Transparent,
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
                    .background(
                        brush =
                            Brush.verticalGradient(
                                colors =
                                    listOf(
                                        MaterialTheme.colorScheme.surface,
                                        MaterialTheme.colorScheme.surfaceVariant
                                            .copy(alpha = 0.96f),
                                        MaterialTheme.colorScheme.primaryContainer
                                            .copy(alpha = 0.30f),
                                        MaterialTheme.colorScheme.surface
                                    )
                            )
                    )
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(
                        horizontal = 18.dp,
                        vertical = 8.dp
                    ),
                verticalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border =
                        BorderStroke(
                            width = 1.dp,
                            color =
                                MaterialTheme.colorScheme
                                    .outlineVariant
                        ),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Column(
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {
                        SearchHeader(
                            isEnglish = isEnglish,
                            textAlign = textAlign
                        )

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
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            horizontal = 10.dp,
                                            vertical = 4.dp
                                        ),
                                color =
                                    MaterialTheme.colorScheme.error,
                                style =
                                    KmiTypography.caption.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                textAlign = textAlign
                            )
                        }

                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                        ) {
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
                        }
                    }
                }

                SearchBottomActions(
                    isEnglish = isEnglish,
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
        }
    }
}

@Composable
private fun SearchHeader(
    isEnglish: Boolean,
    textAlign: TextAlign
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    brush =
                        Brush.horizontalGradient(
                            colors =
                                listOf(
                                    Color(0xFF174A73),
                                    Color(0xFF286185),
                                    Color(0xFF526F85)
                                )
                        )
                )
                .padding(
                    horizontal = 16.dp,
                    vertical = 6.dp
                ),
        verticalArrangement =
            Arrangement.spacedBy(1.dp)
    ) {
        Text(
            text =
                if (isEnglish) {
                    "Search exercise"
                } else {
                    "חיפוש תרגיל"
                },
            modifier =
                Modifier.fillMaxWidth(),
            color = Color.White,
            style =
                KmiTypography.sectionTitle.copy(
                    fontWeight =
                        FontWeight.Black
                ),
            textAlign = textAlign,
            maxLines = 1,
            overflow =
                TextOverflow.Ellipsis
        )

        Text(
            text =
                if (isEnglish) {
                    "Search all exercises"
                } else {
                    "חיפוש בכל התרגילים"
                },
            modifier =
                Modifier.fillMaxWidth(),
            color =
                Color.White.copy(
                    alpha = 0.82f
                ),
            style =
                KmiTypography.caption.copy(
                    fontWeight =
                        FontWeight.SemiBold
                ),
            textAlign = textAlign,
            maxLines = 1,
            overflow =
                TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SearchBottomActions(
    isEnglish: Boolean,
    isListening: Boolean,
    onMicrophoneClick: () -> Unit,
    onClose: () -> Unit
) {
    val microphoneTransition =
        rememberInfiniteTransition(
            label = "exercise_search_microphone"
        )

    val microphoneScale by
    microphoneTransition.animateFloat(
        initialValue = 1f,
        targetValue =
            if (isListening) {
                1.06f
            } else {
                1f
            },
        animationSpec =
            infiniteRepeatable(
                animation =
                    tween(
                        durationMillis = 650
                    ),
                repeatMode =
                    RepeatMode.Reverse
            ),
        label =
            "exercise_search_microphone_scale"
    )

    Row(
        modifier =
            Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.spacedBy(10.dp),
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Surface(
            onClick = onMicrophoneClick,
            modifier =
                Modifier
                    .weight(1f)
                    .height(44.dp)
                    .graphicsLayer {
                        scaleX =
                            microphoneScale
                        scaleY =
                            microphoneScale
                    },
            shape =
                RoundedCornerShape(14.dp),
            color =
                if (isListening) {
                    MaterialTheme
                        .colorScheme
                        .error
                } else {
                    MaterialTheme
                        .colorScheme
                        .primaryContainer
                },
            border =
                BorderStroke(
                    width = 1.dp,
                    color =
                        if (isListening) {
                            MaterialTheme
                                .colorScheme
                                .error
                        } else {
                            MaterialTheme
                                .colorScheme
                                .primary
                                .copy(alpha = 0.34f)
                        }
                ),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 10.dp
                        ),
                horizontalArrangement =
                    Arrangement.Center,
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Icon(
                    imageVector =
                        if (isListening) {
                            Icons.Default.Stop
                        } else {
                            Icons.Default.Mic
                        },
                    contentDescription =
                        if (isListening) {
                            if (isEnglish) {
                                "Stop listening"
                            } else {
                                "עצור האזנה"
                            }
                        } else {
                            if (isEnglish) {
                                "Voice search"
                            } else {
                                "חיפוש קולי"
                            }
                        },
                    tint =
                        if (isListening) {
                            MaterialTheme
                                .colorScheme
                                .onError
                        } else {
                            MaterialTheme
                                .colorScheme
                                .primary
                        },
                    modifier =
                        Modifier.size(
                            KmiIconSize.tiny
                        )
                )

                Spacer(
                    Modifier.width(6.dp)
                )

                Text(
                    text =
                        if (isListening) {
                            if (isEnglish) {
                                "Listening…"
                            } else {
                                "מאזין…"
                            }
                        } else {
                            if (isEnglish) {
                                "Voice search"
                            } else {
                                "חיפוש קולי"
                            }
                        },
                    color =
                        if (isListening) {
                            MaterialTheme
                                .colorScheme
                                .onError
                        } else {
                            MaterialTheme
                                .colorScheme
                                .primary
                        },
                    style =
                        KmiTypography.action.copy(
                            fontWeight =
                                FontWeight.ExtraBold
                        ),
                    maxLines = 1
                )
            }
        }

        Surface(
            onClick = onClose,
            modifier =
                Modifier
                    .weight(1f)
                    .height(44.dp),
            shape =
                RoundedCornerShape(14.dp),
            color =
                MaterialTheme
                    .colorScheme
                    .surfaceVariant,
            border =
                BorderStroke(
                    width = 1.dp,
                    color =
                        MaterialTheme
                            .colorScheme
                            .outlineVariant
                ),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Box(
                modifier =
                    Modifier.fillMaxWidth(),
                contentAlignment =
                    Alignment.Center
            ) {
                Text(
                    text =
                        if (isEnglish) {
                            "Close"
                        } else {
                            "סגור"
                        },
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurface,
                    style =
                        KmiTypography.action.copy(
                            fontWeight =
                                FontWeight.ExtraBold
                        ),
                    textAlign =
                        TextAlign.Center,
                    maxLines = 1
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

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier =
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(
                    horizontal = 10.dp
                )
                .focusRequester(focusRequester),
        singleLine = true,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(
                onSearch = { onDone() },
                onDone = { onDone() },
                onGo = { onDone() },
                onSend = { onDone() }
            ),
            textStyle = KmiTypography.body.copy(
                fontWeight = FontWeight.SemiBold,
                textAlign = textAlign
            ),
            placeholder = {
                Text(
                    text = label,
                    modifier = Modifier.fillMaxWidth(),
                    style = KmiTypography.body,
                    textAlign = textAlign,
                    color = MaterialTheme.colorScheme
                        .onSurfaceVariant
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.primary,
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
                        },
                        modifier = Modifier.size(
                            KmiIconSize.medium
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription =
                                if (isEnglish) {
                                    "Clear"
                                } else {
                                    "נקה"
                                },
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(
                                KmiIconSize.small
                            )
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
                style = KmiTypography.body.copy(
                    fontWeight = FontWeight.Bold
                )
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
                    style = KmiTypography.body.copy(
                        fontWeight = FontWeight.Bold
                    )
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
        subtitle = result.subtitle,
        defaultColor = MaterialTheme.colorScheme.onSurface
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = 12.dp,
                vertical = 5.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector =
                if (isEnglish) {
                    Icons.Filled.ChevronRight
                } else {
                    Icons.Filled.ChevronLeft
                },
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(
                KmiIconSize.tiny
            )
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
                style = KmiTypography.secondary.copy(
                    fontWeight = FontWeight.ExtraBold
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = titleColor
            )

            if (!result.subtitle.isNullOrBlank()) {
                Text(
                    text = result.subtitle,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = textAlign,
                    style = KmiTypography.caption.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
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

    val numberNormalizedQuery =
        cleanQuery
            .replace(
                Regex("""(?<!\S)(אחת|אחד)(?!\S)"""),
                "1"
            )
            .replace(
                Regex("""(?<!\S)(שתיים|שניים)(?!\S)"""),
                "2"
            )
            .replace(
                Regex("""(?<!\S)(שלוש|שלושה)(?!\S)"""),
                "3"
            )
            .replace(
                Regex("""(?<!\S)(ארבע|ארבעה)(?!\S)"""),
                "4"
            )
            .replace(
                Regex("""(?<!\S)(חמש|חמישה)(?!\S)"""),
                "5"
            )
            .replace(
                Regex("""\s+"""),
                " "
            )
            .trim()

    /*
     * תומכים גם בצורה המלאה "מספר 1"
     * וגם בצורה שבה שמות התרגילים נשמרו: "מס' 1".
     */
    val numberLabelVariants =
        linkedSetOf(
            cleanQuery,
            numberNormalizedQuery,

            numberNormalizedQuery.replace(
                Regex("""\bמספר\s+([1-5])"""),
                "מס' $1"
            ),

            numberNormalizedQuery.replace(
                Regex("""\bמספר\s+([1-5])"""),
                "מס׳ $1"
            )
        )

    /*
     * יוצרים גם וריאנטים של צואר / צוואר.
     */
    val neckSpellingVariants =
        linkedSetOf<String>().apply {
            numberLabelVariants.forEach { value ->
                add(value)

                add(
                    value.replace(
                        oldValue = "צוואר",
                        newValue = "צואר",
                        ignoreCase = true
                    )
                )

                add(
                    value.replace(
                        oldValue = "צואר",
                        newValue = "צוואר",
                        ignoreCase = true
                    )
                )
            }
        }

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
    subtitle: String?,
    defaultColor: Color
): Color {
    val resolvedBelt = runCatching {
        ContentRepo.resolveItemKey(id)?.belt
    }.getOrNull()

    if (resolvedBelt != null) {
        return searchResultBeltColor(
            belt = resolvedBelt,
            defaultColor = defaultColor
        )
    }

    val searchableText =
        "$subtitle $id".lowercase()

    return when {
        "צהובה" in searchableText ||
                "yellow" in searchableText ->
            searchResultBeltColor(
                belt = Belt.YELLOW,
                defaultColor = defaultColor
            )

        "כתומה" in searchableText ||
                "orange" in searchableText ->
            searchResultBeltColor(
                belt = Belt.ORANGE,
                defaultColor = defaultColor
            )

        "ירוקה" in searchableText ||
                "green" in searchableText ->
            searchResultBeltColor(
                belt = Belt.GREEN,
                defaultColor = defaultColor
            )

        "כחולה" in searchableText ||
                "blue" in searchableText ->
            searchResultBeltColor(
                belt = Belt.BLUE,
                defaultColor = defaultColor
            )

        "חומה" in searchableText ||
                "brown" in searchableText ->
            searchResultBeltColor(
                belt = Belt.BROWN,
                defaultColor = defaultColor
            )

        "שחורה" in searchableText ||
                "black" in searchableText ->
            searchResultBeltColor(
                belt = Belt.BLACK,
                defaultColor = defaultColor
            )

        else ->
            defaultColor
    }
}

private fun searchResultBeltColor(
    belt: Belt,
    defaultColor: Color
): Color {
    return when (belt) {
        Belt.YELLOW -> Color(0xFFFFC107)
        Belt.ORANGE -> Color(0xFFFF9800)
        Belt.GREEN -> Color(0xFF2E7D32)
        Belt.BLUE -> Color(0xFF1E88E5)
        Belt.BROWN -> Color(0xFF6D4C41)
        Belt.BLACK -> Color.Black

        else -> defaultColor
    }
}