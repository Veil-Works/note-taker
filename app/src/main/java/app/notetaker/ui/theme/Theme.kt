package app.notetaker.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Color(0xFF466B8C), onPrimary = Color.White,
    primaryContainer = Color(0xFFD9EEFF), onPrimaryContainer = Color(0xFF003B68),
    secondary = Color(0xFF4C7091), onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCECF8), onSecondaryContainer = Color(0xFF163B59),
    background = Color(0xFFF0F6FC), onBackground = Color(0xFF132638),
    surface = Color(0xFFF5FAFF), onSurface = Color(0xFF152B3D),
    surfaceContainer = Color(0xFFE4EFF9), surfaceContainerLow = Color(0xFFEFF7FD),
    surfaceContainerHighest = Color(0xFFDCE9F5), surfaceContainerLowest = Color.White,
    surfaceContainerHigh = Color(0xFFDEEDF9), surfaceVariant = Color(0xFFE2EDF7),
    onSurfaceVariant = Color(0xFF526D84), outline = Color(0xFF708BA1), outlineVariant = Color(0xFFC4D9EA),
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFF7294B6), onPrimary = Color(0xFF001727),
    primaryContainer = Color(0xFF263C50), onPrimaryContainer = Color(0xFFCBEBFF),
    secondary = Color(0xFF9CBEDF), onSecondary = Color(0xFF082438),
    secondaryContainer = Color(0xFF132D43), onSecondaryContainer = Color(0xFFD2E9FA),
    background = Color(0xFF101820), onBackground = Color(0xFFEAF3FF),
    surface = Color(0xFF0A1722), onSurface = Color(0xFFEAF3FF),
    surfaceContainer = Color(0xFF0B1B29), surfaceContainerLow = Color(0xFF07131F),
    surfaceContainerHighest = Color(0xFF152C3F), surfaceContainerLowest = Color(0xFF02070D),
    surfaceContainerHigh = Color(0xFF102335), surfaceVariant = Color(0xFF142A3C),
    onSurfaceVariant = Color(0xFF9CB6CF), outline = Color(0xFF587890), outlineVariant = Color(0xFF203E56),
)
@Composable
fun NoteTakerTheme(mode: String = "System", content: @Composable () -> Unit) {
    val dark = when (mode) { "Light" -> false; "Dark" -> true; else -> isSystemInDarkTheme() }
    val view = LocalView.current
    SideEffect {
        (view.context as? Activity)?.window?.let {
            WindowCompat.getInsetsController(it, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors,
        typography = Typography(
            headlineLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal, fontSize = 32.sp, letterSpacing = (-1).sp),
            titleLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal, fontSize = 25.sp, letterSpacing = (-0.6).sp),
            bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
        ), content = content)
}
