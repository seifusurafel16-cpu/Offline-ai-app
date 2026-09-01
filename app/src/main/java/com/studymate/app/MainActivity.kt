package com.studymate.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studymate.app.ui.StudyMateApp
import com.studymate.app.ui.theme.StudyMateTheme

/**
 * Single-activity host. The app is fully Jetpack Compose; this activity only installs the
 * content and enables edge-to-edge. Heavy models are NOT loaded here — they load lazily
 * the first time the user asks a question (see [LlmManager] / [StudyMateApp]).
 *
 * `setContent` is wrapped so that if the Compose tree throws, we fall back to a plain
 * error screen instead of crashing the whole app (helps diagnose device-specific issues).
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            enableEdgeToEdge()
        } catch (e: Exception) {
            // Edge-to-edge is cosmetic; ignore failures on exotic devices/themes.
        }
        try {
            setContent {
                StudyMateTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        StudyMateApp()
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("StudyMate/MainActivity", "Compose content failed", e)
            showError(e)
        }
    }

    private fun showError(e: Throwable) {
        try {
            setContent { ErrorScreen(e) }
        } catch (_: Exception) {
            // Last resort: leave a blank activity rather than crashing.
        }
    }
}

@Composable
private fun ErrorScreen(e: Throwable) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("StudyMate could not start.", fontSize = 18.sp, color = MaterialTheme.colorScheme.error)
            Text(
                text = e.message ?: e.javaClass.name,
                modifier = Modifier.padding(top = 12.dp),
                fontSize = 13.sp
            )
        }
    }
}
