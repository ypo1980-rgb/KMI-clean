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

private fun parseExerciseExplanationForUi(raw: String): AnnotatedString {
    val startTag = "[[RED_BOLD]]"
    val endTag = "[[/RED_BOLD]]"

    val builder = AnnotatedString.Builder()
    var remaining = raw

    while (remaining.isNotEmpty()) {
        val startIndex = remaining.indexOf(startTag)

        if (startIndex == -1) {
            builder.append(remaining)
            break
        }

        builder.append(remaining.substring(0, startIndex))

        val contentStart = startIndex + startTag.length
        val endIndex = remaining.indexOf(endTag, contentStart)

        if (endIndex == -1) {
            builder.append(remaining.substring(startIndex))
            break
        }

        val highlightedText = remaining.substring(contentStart, endIndex)

        builder.pushStyle(
            SpanStyle(
                color = Color(0xFFDC2626),
                fontWeight = FontWeight.Black
            )
        )
        builder.append(highlightedText)
        builder.pop()

        remaining = remaining.substring(endIndex + endTag.length)

        // אם הנקודה נמצאת מחוץ להדגשה:
        // [[RED_BOLD]]עמידת מוצא רגילה[[/RED_BOLD]]. המשך...
        if (remaining.startsWith(". ")) {
            builder.append(".\n")
            remaining = remaining.removePrefix(". ")
        } else if (remaining.startsWith(".")) {
            builder.append(".\n")
            remaining = remaining.removePrefix(".")

            // אם הנקודה כבר נמצאת בתוך ההדגשה:
            // [[RED_BOLD]]עמידת מוצא רגילה.[[/RED_BOLD]] המשך...
        } else if (remaining.startsWith(" ")) {
            builder.append("\n")
            remaining = remaining.trimStart()
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