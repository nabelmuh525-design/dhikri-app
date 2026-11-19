package com.dhikri.islamic.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import android.icu.text.DateFormat
import android.icu.util.Calendar
import android.icu.util.ULocale
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhikri.islamic.R
import com.dhikri.islamic.ui.theme.EmeraldDark
import com.dhikri.islamic.ui.theme.HafsFontFamily
import com.dhikri.islamic.ui.theme.SoftGold
import com.dhikri.islamic.ui.viewmodel.DhikriViewModel

@Composable
fun HomeScreen(
    viewModel: DhikriViewModel,
    onContinueReading: (Int) -> Unit,
    onOpenQuran: () -> Unit,
    onOpenAdhkarTab: () -> Unit,
    onOpenAdhkarCategory: (Int) -> Unit,
    onOpenTasbih: () -> Unit,
    onOpenStories: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenMore: () -> Unit,
    onOpenHijri: () -> Unit
) {
    val lastRead by viewModel.lastRead.collectAsState()
    val surahs by viewModel.surahs.collectAsState()
    val todayTasbih by viewModel.todayTasbihTotal.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val lastSurahName = surahs.firstOrNull { it.id == (lastRead?.surahId ?: 1) }?.nameArabic ?: "الفاتحة"
    val lastPage = lastRead?.pageNumber ?: 1
    val lastAyah = lastRead?.ayahNumber ?: 1
    val datePillText = remember {
        val hijri = runCatching {
            val uLocale = ULocale("ar@calendar=islamic-umalqura")
            val cal = Calendar.getInstance(uLocale)
            DateFormat.getDateInstance(DateFormat.MEDIUM, uLocale).format(cal)
        }.getOrElse { "" }
        val greg = SimpleDateFormat("d MMMM yyyy", Locale("ar")).format(Date())
        if (hijri.isNotBlank()) "$hijri  •  $greg" else greg
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Sunrise Mosque Hero Header + Overlapping Verse Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(315.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.home_header_bg),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(265.dp)
            )
            // Soft warm gradient veil for crisp logo readability
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(265.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0x300B1F3B),
                                Color(0x14FFF8E7),
                                MaterialTheme.colorScheme.background.copy(alpha = 0.85f)
                            )
                        )
                    )
            )

            // Top bar: 3-lines Menu (Right/Start) | Clickable Hijri+Greg Date (Center) | Sun/Moon Theme Toggle (Left/End)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onOpenMore,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(EmeraldDark.copy(alpha = 0.85f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "القائمة والمزيد",
                        tint = SoftGold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xECFFF8E8),
                    border = BorderStroke(1.dp, SoftGold.copy(alpha = 0.8f)),
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .clickable { onOpenHijri() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "التقويم الهجري والمناسبات",
                            tint = EmeraldDark,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = datePillText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = EmeraldDark,
                            maxLines = 1,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                IconButton(
                    onClick = { viewModel.setDarkMode(!settings.darkMode) },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(EmeraldDark.copy(alpha = 0.85f))
                ) {
                    Icon(
                        imageVector = if (settings.darkMode) Icons.Default.DarkMode else Icons.Default.WbSunny,
                        contentDescription = if (settings.darkMode) "الوضع الداكن" else "الوضع الفاتح",
                        tint = SoftGold
                    )
                }
            }

            // Center Brand Title: ذِكري + tagline
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "ذِكْري",
                    fontSize = 54.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = EmeraldDark,
                    lineHeight = 64.sp
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(
                        modifier = Modifier.width(34.dp),
                        thickness = 1.dp,
                        color = EmeraldDark.copy(alpha = 0.55f)
                    )
                    Text(
                        text = "رفيقك في طريق الطمأنينة",
                        style = MaterialTheme.typography.titleMedium,
                        color = EmeraldDark,
                        fontWeight = FontWeight.Bold
                    )
                    HorizontalDivider(
                        modifier = Modifier.width(34.dp),
                        thickness = 1.dp,
                        color = EmeraldDark.copy(alpha = 0.55f)
                    )
                }
            }

            // Overlapping Cream-Gold Verse Banner
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .shadow(6.dp, RoundedCornerShape(22.dp))
                    .clickable { onContinueReading(lastPage) },
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFFFFF8E8),
                border = BorderStroke(1.2.dp, SoftGold.copy(alpha = 0.75f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "﴿ أَلَا بِذِكۡرِ ٱللَّهِ تَطۡمَئِنُّ ٱلۡقُلُوبُ ﴾",
                        fontFamily = HafsFontFamily,
                        fontSize = 22.sp,
                        color = Color(0xFF2B2112),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "[ الرعد: 28 ]  •  متابعة القراءة: $lastSurahName (ص $lastPage)",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF6B5630),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Dedicated Prominent Continue Reading Card (منفصلة وواضحة)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .shadow(4.dp, RoundedCornerShape(18.dp))
                .clickable { onContinueReading(lastPage) },
            shape = RoundedCornerShape(18.dp),
            color = EmeraldDark,
            border = BorderStroke(1.2.dp, SoftGold)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(SoftGold.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = null,
                            tint = SoftGold,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "متابعة القراءة",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SoftGold
                        )
                        Text(
                            text = "سورة $lastSurahName • صفحة $lastPage (آية $lastAyah)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.92f)
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SoftGold
                ) {
                    Text(
                        text = "أكمل",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F1604),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // 2. 2x3 Colorful Gradient Feature Grid (matching exact reference layout)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Row 1 (RTL: Tasbih on right, Mushaf on left)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HomeGradientCard(
                    title = "عداد التسبيح",
                    subtitle = if (todayTasbih > 0) "اليوم: $todayTasbih تسبيحة" else "سبح .. واحصل على الأجر",
                    icon = Icons.Default.TouchApp,
                    colors = listOf(Color(0xFF4F8B5F), Color(0xFF1F5733)),
                    contentColor = Color.White,
                    iconTint = Color(0xFFF6D97A),
                    modifier = Modifier.weight(1f),
                    onClick = onOpenTasbih
                )
                HomeGradientCard(
                    title = "المصحف الكريم",
                    subtitle = "قراءة وتفسير ومعاني",
                    icon = Icons.Default.MenuBook,
                    colors = listOf(Color(0xFFF3E2BC), Color(0xFFD6B36D)),
                    contentColor = Color(0xFF281E0E),
                    iconTint = Color(0xFF4A3310),
                    modifier = Modifier.weight(1f),
                    onClick = onOpenQuran
                )
            }

            // Row 2 (RTL: Varied Duas on right, Morning/Evening Adhkar on left)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HomeGradientCard(
                    title = "أدعية متنوعة",
                    subtitle = "في كل وقت وحين",
                    icon = Icons.Default.VolunteerActivism,
                    colors = listOf(Color(0xFF5CB1CC), Color(0xFF1E7494)),
                    contentColor = Color.White,
                    iconTint = Color.White,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenAdhkarTab
                )
                HomeGradientCard(
                    title = "أذكار الصباح والمساء",
                    subtitle = "ابدأ يومك بذكر الله",
                    icon = Icons.Default.EditNote,
                    colors = listOf(Color(0xFF9387D4), Color(0xFF5A4E94)),
                    contentColor = Color.White,
                    iconTint = Color.White,
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenAdhkarCategory(27) }
                )
            }

            // Row 3 (RTL: Favorites on right, Stories on left)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HomeGradientCard(
                    title = "المفضلة",
                    subtitle = "أذكارك المفضلة دائماً",
                    icon = Icons.Default.Star,
                    colors = listOf(Color(0xFFF6CC80), Color(0xFFC99136)),
                    contentColor = Color(0xFF2B1F09),
                    iconTint = Color(0xFF7A5311),
                    modifier = Modifier.weight(1f),
                    onClick = onOpenFavorites
                )
                HomeGradientCard(
                    title = "الحكايات الإسلامية",
                    subtitle = "قصص موثقة وعبر",
                    icon = Icons.Default.Mosque,
                    colors = listOf(Color(0xFF88B884), Color(0xFF447C4A)),
                    contentColor = Color.White,
                    iconTint = Color(0xFFFFF8E8),
                    modifier = Modifier.weight(1f),
                    onClick = onOpenStories
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // 3. Bottom Inspirational Pill
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 4.dp),
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFFF5EFE3),
            border = BorderStroke(1.dp, SoftGold.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = EmeraldDark,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "كل ذكر لله .. يقرّبك من الله",
                    style = MaterialTheme.typography.titleMedium,
                    color = EmeraldDark
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Spa,
                    contentDescription = null,
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun HomeGradientCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    colors: List<Color>,
    contentColor: Color,
    iconTint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(126.dp)
            .shadow(4.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(colors))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(34.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor.copy(alpha = 0.88f),
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}
