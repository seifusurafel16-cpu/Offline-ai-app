package com.studymate.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.studymate.app.ui.StudyMateApp

/**
 * Single-activity host. The app is fully Jetpack Compose; this activity only installs the
 * content and enables edge-to-edge. Heavy models are NOT loaded here — they load lazily
 * the first time the user asks a question (see [LlmManager] / [StudyMateApp]).
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Surface(modifier = Modifier.fillMaxSize()) {
                StudyMateApp()
            }
        }
    }
}
