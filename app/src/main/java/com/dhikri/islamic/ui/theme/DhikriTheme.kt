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

val EmeraldPrimary = Color(0xFF0B5D46)
val EmeraldDark = Color(0xFF063D2D)
val EmeraldLight = Color(0xFF2E8B6E)
val SoftGold = Color(0xFFC9A24B)
val SoftGoldContainer = Color(0xFFF6EBD1)
val IvoryBackground = Color(0xFFFAF7F0)
val IvorySurface = Color(0xFFFFFDF8)
val IvorySurfaceVariant = Color(0xFFF0EADF)
val DeepNightBackground = Color(0xFF0D1412)
val DeepNightSurface = Color(0xFF141F1C)
val DeepNightCard = Color(0xFF1B2A26)
val NightReadingParchment = Color(0xFF121816)

val HafsFontFamily = FontFamily(
    Font(R.font.hafs_20, FontWeight.Normal)
)

private val LightColors = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD4EFE5),
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
    onSurfaceVariant = Color(0xFF495450),
    outline = Color(0xFFD5CCBC)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF5CC4A1),
    onPrimary = Color(0xFF003828),
    primaryContainer = EmeraldPrimary,
    onPrimaryContainer = Color(0xFFD4EFE5),
    secondary = Color(0xFFE0C06D),
    onSecondary = Color(0xFF332707),
    secondaryContainer = Color(0xFF4A3A12),
    onSecondaryContainer = SoftGoldContainer,
    background = DeepNightBackground,
    onBackground = Color(0xFFEAECE8),
    surface = DeepNightSurface,
    onSurface = Color(0xFFEAECE8),
    surfaceVariant = DeepNightCard,
    onSurfaceVariant = Color(0xFFB5C4BE),
    outline = Color(0xFF2D403A)
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
