package il.kmi.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign

private data class BeltTextColor(
    val labels: List<String>,
    val color: Color
)

private val beltTextColors =
    listOf(
        BeltTextColor(
            labels = listOf(
                "חגורה לבנה",
                "White belt"
            ),
            /*
             * אפור כהה כדי שהחגורה הלבנה תהיה קריאה
             * גם על רקע לבן.
             */
            color = Color(0xFF64748B)
        ),
        BeltTextColor(
            labels = listOf(
                "חגורה צהובה",
                "Yellow belt"
            ),
            color = Color(0xFFD59A00)
        ),
        BeltTextColor(
            labels = listOf(
                "חגורה כתומה",
                "Orange belt"
            ),
            color = Color(0xFFF97316)
        ),
        BeltTextColor(
            labels = listOf(
                "חגורה ירוקה",
                "Green belt"
            ),
            color = Color(0xFF16A34A)
        ),
        BeltTextColor(
            labels = listOf(
                "חגורה כחולה",
                "Blue belt"
            ),
            color = Color(0xFF2563EB)
        ),
        BeltTextColor(
            labels = listOf(
                "חגורה חומה",
                "Brown belt"
            ),
            color = Color(0xFF92400E)
        ),
        BeltTextColor(
            labels = listOf(
                "חגורה שחורה",
                "Black belt"
            ),
            color = Color(0xFF111827)
        )
    )

private fun applyBeltColors(
    source: AnnotatedString
): AnnotatedString {
    val builder =
        AnnotatedString.Builder().apply {
            /*
             * append של AnnotatedString שומר גם את
             * עיצובי RED_BOLD ו־BLUE_BOLD הקיימים.
             */
            append(source)
        }

    beltTextColors.forEach { beltStyle ->
        beltStyle.labels.forEach { label ->
            var searchFrom = 0

            while (searchFrom < source.text.length) {
                val start =
                    source.text.indexOf(
                        string = label,
                        startIndex = searchFrom,
                        ignoreCase = true
                    )

                if (start < 0) {
                    break
                }

                val end =
                    start + label.length

                builder.addStyle(
                    style = SpanStyle(
                        color = beltStyle.color,
                        fontWeight = FontWeight.Black
                    ),
                    start = start,
                    end = end
                )

                searchFrom = end
            }
        }
    }

    return builder.toAnnotatedString()
}

private fun parseExerciseExplanationForUi(
    raw: String
): AnnotatedString {
    val redStartTag = "[[RED_BOLD]]"
    val redEndTag = "[[/RED_BOLD]]"

    val blueStartTag = "[[BLUE_BOLD]]"
    val blueEndTag = "[[/BLUE_BOLD]]"

    val builder = AnnotatedString.Builder()
    var remaining = raw

    while (remaining.isNotEmpty()) {
        val redStartIndex =
            remaining.indexOf(redStartTag)

        val blueStartIndex =
            remaining.indexOf(blueStartTag)

        /*
         * בוחרים את תגית העיצוב הקרובה ביותר.
         */
        val useRedTag =
            redStartIndex >= 0 &&
                    (
                            blueStartIndex < 0 ||
                                    redStartIndex <
                                    blueStartIndex
                            )

        val useBlueTag =
            blueStartIndex >= 0 &&
                    (
                            redStartIndex < 0 ||
                                    blueStartIndex <
                                    redStartIndex
                            )

        if (!useRedTag && !useBlueTag) {
            builder.append(remaining)
            break
        }

        val startTag =
            if (useRedTag) {
                redStartTag
            } else {
                blueStartTag
            }

        val endTag =
            if (useRedTag) {
                redEndTag
            } else {
                blueEndTag
            }

        val startIndex =
            if (useRedTag) {
                redStartIndex
            } else {
                blueStartIndex
            }

        builder.append(
            remaining.substring(
                startIndex = 0,
                endIndex = startIndex
            )
        )

        val contentStart =
            startIndex +
                    startTag.length

        val endIndex =
            remaining.indexOf(
                string = endTag,
                startIndex = contentStart
            )

        /*
         * אם תגית לא נסגרה, משאירים אותה כטקסט
         * כדי שלא יאבד חלק מההסבר.
         */
        if (endIndex < 0) {
            builder.append(
                remaining.substring(
                    startIndex
                )
            )
            break
        }

        val highlightedText =
            remaining.substring(
                startIndex = contentStart,
                endIndex = endIndex
            )

        builder.pushStyle(
            SpanStyle(
                color =
                    if (useRedTag) {
                        Color(0xFFDC2626)
                    } else {
                        Color(0xFF2563EB)
                    },
                fontWeight =
                    FontWeight.Black
            )
        )

        builder.append(
            highlightedText
        )

        builder.pop()

        remaining =
            remaining.substring(
                endIndex +
                        endTag.length
            )

        /*
         * RED_BOLD ממשיך להציג את עמידת המוצא
         * בשורה נפרדת.
         */
        if (useRedTag) {
            when {
                remaining.startsWith(". ") -> {
                    builder.append(".\n")

                    remaining =
                        remaining.removePrefix(
                            ". "
                        )
                }

                remaining.startsWith(".") -> {
                    builder.append(".\n")

                    remaining =
                        remaining.removePrefix(
                            "."
                        )
                }

                remaining.startsWith(" ") -> {
                    builder.append("\n")

                    remaining =
                        remaining.trimStart()
                }
            }
        }
    }

    /*
     * לאחר עיבוד RED_BOLD ו־BLUE_BOLD מוסיפים
     * את צבעי החגורות לאותו AnnotatedString.
     */
    return applyBeltColors(
        builder.toAnnotatedString()
    )
}

@Composable
fun StyledExplanationText(
    raw: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    color: Color = Color(0xFF1F2937),
    textAlign: TextAlign = TextAlign.Right
) {
    Text(
        text = parseExerciseExplanationForUi(raw),
        modifier = modifier,
        style = style,
        color = color,
        textAlign = textAlign
    )
}