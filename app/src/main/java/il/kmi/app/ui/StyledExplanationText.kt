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
         * בוחרים את התגית הקרובה ביותר בטקסט.
         * כך ניתן לשלב כמה קטעים אדומים וכחולים
         * באותו הסבר ובכל סדר.
         */
        val useRedTag =
            redStartIndex >= 0 &&
                    (
                            blueStartIndex < 0 ||
                                    redStartIndex < blueStartIndex
                            )

        val useBlueTag =
            blueStartIndex >= 0 &&
                    (
                            redStartIndex < 0 ||
                                    blueStartIndex < redStartIndex
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
            startIndex + startTag.length

        val endIndex =
            remaining.indexOf(
                string = endTag,
                startIndex = contentStart
            )

        /*
         * תגית שלא נסגרה נשארת כטקסט רגיל,
         * כדי שלא ייעלם חלק מההסבר.
         */
        if (endIndex < 0) {
            builder.append(
                remaining.substring(startIndex)
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
                fontWeight = FontWeight.Black
            )
        )

        builder.append(highlightedText)
        builder.pop()

        remaining =
            remaining.substring(
                endIndex + endTag.length
            )

        /*
         * שומרים את ההתנהגות הקיימת של RED_BOLD:
         * עמידת המוצא מוצגת בשורה נפרדת.
         *
         * BLUE_BOLD אינו משנה שורות בעצמו.
         * ירידת שורה כחולה נקבעת במחרוזת באמצעות \n.
         */
        if (useRedTag) {
            if (remaining.startsWith(". ")) {
                builder.append(".\n")
                remaining =
                    remaining.removePrefix(". ")
            } else if (remaining.startsWith(".")) {
                builder.append(".\n")
                remaining =
                    remaining.removePrefix(".")
            } else if (remaining.startsWith(" ")) {
                builder.append("\n")
                remaining =
                    remaining.trimStart()
            }
        }
    }

    return builder.toAnnotatedString()
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