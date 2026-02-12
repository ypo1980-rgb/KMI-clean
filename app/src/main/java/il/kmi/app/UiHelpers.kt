package il.kmi.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * רכיב קרדיט קטן שמוצג בתחתית המסך
 */
@Composable
fun MadeByBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "פותח באהבה ❤ על ידי יובל",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(4.dp)
        )
    }
}

/**
 * כפתור אייקון לצליל (on/off)
 */
@Composable
fun SoundIconButton(
    enabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onToggle,
        modifier = modifier
    ) {
        if (enabled) {
            Icon(Icons.Filled.VolumeUp, contentDescription = "צליל פעיל")
        } else {
            Icon(Icons.Filled.VolumeOff, contentDescription = "צליל כבוי")
        }
    }
}

/**
 * פס ניווט תחתון רספונסיבי
 */
@Composable
fun ResponsiveBottomBar(
    items: List<String>,
    selectedItem: String? = null,
    onItemSelected: (String) -> Unit
) {
    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                selected = (item == selectedItem),
                onClick = { onItemSelected(item) },
                icon = {
                    when (item) {
                        "בית" -> Icon(Icons.Filled.Home, contentDescription = item)
                        "הגדרות" -> Icon(Icons.Filled.Settings, contentDescription = item)
                        "חזרה" -> Icon(Icons.Filled.ArrowBack, contentDescription = item)
                        else   -> Icon(Icons.Filled.Home, contentDescription = item)
                    }
                },
                label = { Text(item) }
            )
        }
    }
}
