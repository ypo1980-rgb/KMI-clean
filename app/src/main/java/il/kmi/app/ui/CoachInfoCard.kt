package il.kmi.app.ui

import android.content.SharedPreferences
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

// מפתחות ברירת-מחדל ב-SharedPreferences (לנוחות)
const val KEY_COACH_NAME = "coach_name"
const val KEY_BRANCH     = "branch"
const val KEY_REGION     = "region"    // עיר/אזור
const val KEY_AGE_GROUP  = "age_group"

/**
 * כרטיס מידע קומפקטי על הקבוצה: מאמן, עיר·סניף וקבוצה.
 */
@Composable
fun CoachInfoCard(
    coachName: String?,
    branchName: String?,
    city: String?,
    groupName: String?,
    onOpenProfile: () -> Unit = {},
    modifier: Modifier = Modifier
) {
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
                // מאמן
                Text(
                    text = "מאמן: ${coachName.orEmpty()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // עיר · סניף
                val branchLine = buildString {
                    if (!city.isNullOrBlank()) append(city).append(" · ")
                    append(branchName.orEmpty())
                }
                Text(
                    text = branchLine,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // קבוצה
                Text(
                    text = "קבוצה: ${groupName.orEmpty()}",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onOpenProfile) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = "פרופיל",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * עטיפה נוחה שמושכת נתונים מ-SharedPreferences ומציגה את הכרטיס.
 */
@Composable
fun CoachInfoCardFromPrefs(
    sp: SharedPreferences,
    modifier: Modifier = Modifier,
    onOpenProfile: () -> Unit = {}
) {
    CoachInfoCard(
        coachName  = sp.getString(KEY_COACH_NAME, null),
        branchName = sp.getString(KEY_BRANCH,     null),
        city       = sp.getString(KEY_REGION,     null),
        groupName  = sp.getString(KEY_AGE_GROUP,  null),
        onOpenProfile = onOpenProfile,
        modifier = modifier
    )
}
