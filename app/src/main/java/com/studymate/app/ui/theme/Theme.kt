package com.studymate.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// StudyMate brand palette.
private val BrandBlue = Color(0xFF3F51B5)
private val BrandBlueDark = Color(0xFF303F9F)
private val Accent = Color(0xFF03DAC5)
private val UserBubble = Color(0xFF3F51B5)
private val AssistantBubble = Color(0xFF1E1E2E)
private val AssistantBubbleDark = Color(0xFFE8EAF6)

val LightColors = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC5CAE9),
    onPrimaryContainer = Color(0xFF1A237E),
    secondary = Accent,
    background = Color(0xFFF5F5F7),
    surface = Color.White,
    onSurface = Color(0xFF1C1B1F)
)

val DarkColors = darkColorScheme(
    primary = Color(0xFF8C9EFF),
    onPrimary = Color(0xFF1A237E),
    primaryContainer = BrandBlueDark,
    secondary = Accent,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE6E6E6)
)

/** App-wide Compose theme. Dark/light follows the system setting. */
@Composable
fun StudyMateTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}

// Re-export bubble colors for chat UI.
val ChatUserBubbleColor: Color get() = UserBubble
val ChatAssistantBubbleColor: Color
    @Composable get() = if (isSystemInDarkTheme()) AssistantBubbleDark else AssistantBubble
