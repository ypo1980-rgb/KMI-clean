package il.kmi.app.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenWithSettings(
    title: String,
    onOpenSettings: () -> Unit = {},
    onBack: (() -> Unit)? = null,   // ⬅️ פרמטר אופציונלי
    content: @Composable () -> Unit
) {
    Scaffold(
        topBar = {
            il.kmi.app.ui.KmiTopBar(
                title = title,
                onBack = onBack,
                extraActions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "הגדרות"
                        )
                    }
                },
            )
        }
    ) { innerPadding ->

        Surface(
            modifier = Modifier.padding(innerPadding)
        ) {
            content()
        }
    }
}
