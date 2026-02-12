@file:OptIn(ExperimentalMaterial3Api::class)

package il.kmi.app.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun SplashScreen(onContinue: () -> Unit, onSettings: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Button(onClick = onContinue) {
            Text("מסך פתיחה (המשך)")
        }
    }
}
