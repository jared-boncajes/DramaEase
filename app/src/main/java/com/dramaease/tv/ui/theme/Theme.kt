package com.dramaease.tv.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import com.dramaease.tv.data.AppSettings
import com.dramaease.tv.ui.Strings

data class DramaColors(
    val background: Color,
    val surface: Color,
    val surfaceFocused: Color,
    val text: Color,
    val textDim: Color,
    val accent: Color,
    val onAccent: Color,
    val focusRing: Color,
    val progress: Color,
    val error: Color,
)

val StandardColors = DramaColors(
    background = Color(0xFF16181C), surface = Color(0xFF262A31), surfaceFocused = Color(0xFF3A404A),
    text = Color(0xFFF4F4F5), textDim = Color(0xFFB8BEC8), accent = Color(0xFFFFB02E), onAccent = Color(0xFF1A1200),
    focusRing = Color(0xFFFFFFFF), progress = Color(0xFFFFB02E), error = Color(0xFFFF8A80),
)

val HighContrastColors = DramaColors(
    background = Color(0xFF000000), surface = Color(0xFF1B1B1B), surfaceFocused = Color(0xFF2E2E2E),
    text = Color(0xFFFFFFFF), textDim = Color(0xFFE0E0E0), accent = Color(0xFFFFD54F), onAccent = Color(0xFF000000),
    focusRing = Color(0xFFFFEB3B), progress = Color(0xFFFFD54F), error = Color(0xFFFF9E9E),
)

object DramaType {
    val headline = TextStyle(fontSize = 44.sp, fontWeight = FontWeight.Bold)
    val title = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
    val section = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold)
    val body = TextStyle(fontSize = 22.sp)
    val label = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Medium)
    val button = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold)
}

val LocalDramaColors = staticCompositionLocalOf { StandardColors }
val LocalStrings = staticCompositionLocalOf { Strings(com.dramaease.tv.data.AppLanguage.ENGLISH) }

/** Layout constants (TV-safe margins: 5% of a 1920x1080 screen = 96x54 dp-ish; we use 48dp/27dp baseline in dp). */
object Dimens {
    val safeHorizontal = androidx.compose.ui.unit.Dp(48f)
    val safeVertical = androidx.compose.ui.unit.Dp(27f)
}

@Composable
fun DramaEaseTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val colors = if (settings.highContrast) HighContrastColors else StandardColors
    val base = LocalDensity.current
    // Text size is applied by scaling the font scale, so every sp-based text grows consistently.
    val scaled = Density(base.density, base.fontScale * settings.textSize.scale)
    CompositionLocalProvider(
        LocalDensity provides scaled,
        LocalDramaColors provides colors,
        LocalStrings provides Strings(settings.language),
    ) {
        MaterialTheme(
            colorScheme = darkColorScheme(
                background = colors.background, surface = colors.surface,
                primary = colors.accent, onPrimary = colors.onAccent,
                onSurface = colors.text, onBackground = colors.text,
            ),
            content = content,
        )
    }
}
