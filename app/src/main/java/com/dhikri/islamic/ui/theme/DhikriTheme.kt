package com.dhikri.islamic.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.dhikri.islamic.R

val EmeraldPrimary = Color(0xFF14345E)
val EmeraldDark = Color(0xFF0B1F3B)
val EmeraldLight = Color(0xFF2B5A94)
val SoftGold = Color(0xFFD4AF37)
val SoftGoldContainer = Color(0xFFF6EBD1)
val IvoryBackground = Color(0xFFF8F4EC)
val IvorySurface = Color(0xFFFFFDF8)
val IvorySurfaceVariant = Color(0xFFF0EADF)
val DeepNightBackground = Color(0xFF08111F)
val DeepNightSurface = Color(0xFF0F1C30)
val DeepNightCard = Color(0xFF172842)

val HafsFontFamily = FontFamily(
    Font(R.font.hafs_20, FontWeight.Normal)
)

data class ReadingThemePalette(
    val key: String,
    val labelArabic: String,
    val background: Color,
    val surface: Color,
    val textColor: Color,
    val accentColor: Color,
    val dividerColor: Color,
    val isDark: Boolean
)

val ReadingThemes: List<ReadingThemePalette> = listOf(
    ReadingThemePalette(
        key = "parchment",
        labelArabic = "ورق المصحف الكريمي",
        background = Color(0xFFFBF6E8),
        surface = Color(0xFFF2E8D0),
        textColor = Color(0xFF211C14),
        accentColor = Color(0xFF9C7826),
        dividerColor = Color(0xFFDCCEA8),
        isDark = false
    ),
    ReadingThemePalette(
        key = "classic",
        labelArabic = "أبيض مريح",
        background = Color(0xFFFAFBFA),
        surface = Color(0xFFEEF3F0),
        textColor = Color(0xFF141A17),
        accentColor = Color(0xFF0B5D46),
        dividerColor = Color(0xFFD4DED9),
        isDark = false
    ),
    ReadingThemePalette(
        key = "sage",
        labelArabic = "أخضر هادئ للعين",
        background = Color(0xFFEAF4EE),
        surface = Color(0xFFD9EBDF),
        textColor = Color(0xFF13261C),
        accentColor = Color(0xFF0B5D46),
        dividerColor = Color(0xFFBED8C8),
        isDark = false
    ),
    ReadingThemePalette(
        key = "sepia_dark",
        labelArabic = "ليلي دافئ (سيبيا)",
        background = Color(0xFF1B1712),
        surface = Color(0xFF272119),
        textColor = Color(0xFFE8DCC6),
        accentColor = Color(0xFFD8B55B),
        dividerColor = Color(0xFF3E3529),
        isDark = true
    ),
    ReadingThemePalette(
        key = "amoled_dark",
        labelArabic = "ليلي داكن (AMOLED)",
        background = Color(0xFF0B100E),
        surface = Color(0xFF141E1B),
        textColor = Color(0xFFE3E8E5),
        accentColor = Color(0xFF5CC4A1),
        dividerColor = Color(0xFF253530),
        isDark = true
    )
)

fun readingThemeByKey(key: String): ReadingThemePalette =
    ReadingThemes.firstOrNull { it.key == key } ?: ReadingThemes.first()

private val LightColors = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE7F5),
    onPrimaryContainer = EmeraldDark,
    secondary = SoftGold,
    onSecondary = Color(0xFF2B2108),
    secondaryContainer = SoftGoldContainer,
    onSecondaryContainer = Color(0xFF47350C),
    background = IvoryBackground,
    onBackground = Color(0xFF18201D),
    surface = IvorySurface,
    onSurface = Color(0xFF18201D),
    surfaceVariant = IvorySurfaceVariant,
    onSurfaceVariant = Color(0xFF3A4440),
    outline = Color(0xFFD5CCBC)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8AB8EB),
    onPrimary = Color(0xFF081C36),
    primaryContainer = EmeraldPrimary,
    onPrimaryContainer = Color(0xFFDCE7F5),
    secondary = Color(0xFFE0C06D),
    onSecondary = Color(0xFF332707),
    secondaryContainer = Color(0xFF4A3A12),
    onSecondaryContainer = SoftGoldContainer,
    background = DeepNightBackground,
    onBackground = Color(0xFFF3F6FA),
    surface = DeepNightSurface,
    onSurface = Color(0xFFF3F6FA),
    surfaceVariant = DeepNightCard,
    onSurfaceVariant = Color(0xFFCBD7E8),
    outline = Color(0xFF263B59)
)

private val DhikriTypography = Typography(
    headlineMedium = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 34.sp),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 28.sp),
    titleMedium = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 22.sp),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, lineHeight = 16.sp)
)

@Composable
fun DhikriTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = DhikriTypography,
        content = content
    )
}
