package com.dhikri.islamic.ui.screens

import android.icu.text.DateFormat
import android.icu.util.Calendar
import android.icu.util.ULocale
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhikri.islamic.R
import com.dhikri.islamic.domain.util.PrayerTimesCalculator
import com.dhikri.islamic.ui.theme.EmeraldDark
import com.dhikri.islamic.ui.theme.HafsFontFamily
import com.dhikri.islamic.ui.theme.SoftGold
import com.dhikri.islamic.ui.viewmodel.DhikriViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: DhikriViewModel,
    onContinueReading: (Int) -> Unit,
    onOpenQuran: () -> Unit,
    onOpenAdhkarTab: () -> Unit,
    onOpenRecitations: () -> Unit,
    onOpenTasbih: () -> Unit,
    onOpenQibla: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenMore: () -> Unit,
    onOpenHijri: () -> Unit
) {
    val lastRead by viewModel.lastRead.collectAsState()
    val surahs by viewModel.surahs.collectAsState()
    val todayTasbih by viewModel.todayTasbihTotal.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val availableUpdate by viewModel.availableUpdate.collectAsState()
    val updateProgress by viewModel.updateProgress.collectAsState()
    val context = LocalContext.current

    val systemDark = isSystemInDarkTheme()
    val effectiveDark = when (settings.themeMode) {
        "dark" -> true
        "light" -> false
        else -> systemDark
    }

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showCityPicker by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1_000L)
        }
    }
    val prayerSchedule = remember(settings.prayerCityName, nowMillis / 1000L) {
        PrayerTimesCalculator.computeSchedule(settings.prayerCityName, nowMillis)
    }

    val lastSurahName = surahs.firstOrNull { it.id == (lastRead?.surahId ?: 1) }?.nameArabic ?: "الفاتحة"
    val lastPage = lastRead?.pageNumber ?: 1
    val lastAyah = lastRead?.ayahNumber ?: 1
    val datePillText = remember {
        val now = Date()
        val dayName = SimpleDateFormat("EEEE", Locale("ar")).format(now)
        val hijri = runCatching {
            val uLocale = ULocale("ar@calendar=islamic-umalqura")
            val cal = Calendar.getInstance(uLocale)
            DateFormat.getDateInstance(DateFormat.MEDIUM, uLocale).format(cal)
        }.getOrElse { "" }
        val greg = SimpleDateFormat("d MMMM yyyy", Locale("ar")).format(now)
        if (hijri.isNotBlank()) "$dayName • $hijri • $greg" else "$dayName • $greg"
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
                .height(335.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.home_header_bg),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(285.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(285.dp)
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
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
                    onClick = { viewModel.setDarkMode(!effectiveDark) },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(EmeraldDark.copy(alpha = 0.85f))
                ) {
                    Icon(
                        imageVector = if (effectiveDark) Icons.Default.DarkMode else Icons.Default.WbSunny,
                        contentDescription = if (effectiveDark) "الوضع الداكن" else "الوضع الفاتح",
                        tint = SoftGold
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 56.dp),
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

            // Clean Verse Banner
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .shadow(6.dp, RoundedCornerShape(22.dp)),
                shape = RoundedCornerShape(22.dp),
                color = if (effectiveDark) Color(0xFF132642) else Color(0xFFFFF8E8),
                border = BorderStroke(1.2.dp, SoftGold.copy(alpha = 0.75f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "﴿ أَلَا بِذِكۡرِ ٱللَّهِ تَطۡمَئِنُّ ٱلۡقُلُوبُ ﴾",
                        fontFamily = HafsFontFamily,
                        fontSize = 22.sp,
                        color = if (effectiveDark) Color(0xFFFFF3D1) else Color(0xFF2B2112),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // In-App Update Notification Banner
        availableUpdate?.let { update ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 10.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFFFF4D6),
                border = BorderStroke(1.5.dp, SoftGold),
                shadowElevation = 4.dp
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = EmeraldDark)
                            Column {
                                Text(
                                    "تحديث جديد متاح (v${update.latestVersion})",
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldDark
                                )
                                Text(
                                    "اضغط للتحديث المباشر دون حذف بياناتك",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFF5A4519)
                                )
                            }
                        }
                        Button(
                            onClick = { viewModel.installAppUpdate(context) },
                            enabled = updateProgress == null,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark, contentColor = SoftGold)
                        ) {
                            Text(if (updateProgress != null) "${updateProgress}%" else "تحديث الآن")
                        }
                    }
                    updateProgress?.let { pct ->
                        LinearProgressIndicator(
                            progress = { pct / 100f },
                            modifier = Modifier.fillMaxWidth(),
                            color = EmeraldDark
                        )
                    }
                }
            }
        }

        // Dedicated Continue Reading Card
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

        Spacer(Modifier.height(12.dp))

        // Prayer Times & Live Countdown Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .shadow(4.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            color = EmeraldDark,
            border = BorderStroke(1.dp, SoftGold.copy(alpha = 0.65f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color(0xFF14345E), EmeraldDark)))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "الصلاة القادمة: ${prayerSchedule.nextPrayer.nameArabic} • ${prayerSchedule.nextPrayer.formatted12h()}",
                            color = SoftGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1
                        )
                        Text(
                            text = "متبقي ${PrayerTimesCalculator.formatCountdown(prayerSchedule.millisRemaining)}",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White.copy(alpha = 0.12f),
                            modifier = Modifier.clickable { showCityPicker = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(Icons.Default.LocationOn, contentDescription = "المدينة", tint = SoftGold, modifier = Modifier.size(14.dp))
                                Text(prayerSchedule.city.nameArabic, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        IconButton(
                            onClick = { viewModel.setPreAdhanReminderEnabled(context, !settings.preAdhanReminderEnabled) },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    if (settings.preAdhanReminderEnabled) SoftGold.copy(alpha = 0.25f)
                                    else Color.White.copy(alpha = 0.10f)
                                )
                        ) {
                            Icon(
                                imageVector = if (settings.preAdhanReminderEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                                contentDescription = "تنبيه قبل الأذان",
                                tint = if (settings.preAdhanReminderEnabled) SoftGold else Color.White.copy(alpha = 0.75f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    prayerSchedule.prayers.forEach { p ->
                        val isNext = p.nameArabic == prayerSchedule.nextPrayer.nameArabic
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isNext) SoftGold.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.07f),
                            border = if (isNext) BorderStroke(1.dp, SoftGold) else null
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = p.nameArabic,
                                    fontSize = 10.sp,
                                    fontWeight = if (isNext) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isNext) SoftGold else Color.White.copy(alpha = 0.85f),
                                    maxLines = 1
                                )
                                Text(
                                    text = p.formatted12h().replace(" ص", "").replace(" م", ""),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showCityPicker) {
            AlertDialog(
                onDismissRequest = { showCityPicker = false },
                title = { Text("اختر مدينتك لمواقيت الصلاة", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PrayerTimesCalculator.cities.forEach { city ->
                            val selected = city.nameArabic == settings.prayerCityName
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.setPrayerCityName(context, city.nameArabic)
                                        showCityPicker = false
                                    },
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) EmeraldDark else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = city.nameArabic,
                                    color = if (selected) SoftGold else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showCityPicker = false }) { Text("إغلاق") }
                }
            )
        }

        Spacer(Modifier.height(14.dp))

        // 2. 2x3 Feature Grid (All Adhkar consolidated under one "الأذكار" button)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HomeGradientCard(
                    title = "الأذكار",
                    subtitle = "الصباح، المساء، وكل الأذكار",
                    icon = Icons.Default.VolunteerActivism,
                    colors = listOf(Color(0xFF8B7AD4), Color(0xFF4F3E8E)),
                    contentColor = Color.White,
                    iconTint = Color(0xFFFFF4D1),
                    modifier = Modifier.weight(1f),
                    onClick = onOpenAdhkarTab
                )
                HomeGradientCard(
                    title = "الاستماع للقرآن",
                    subtitle = "تلاوات خاشعة أوفلاين",
                    icon = Icons.Default.Headphones,
                    colors = listOf(Color(0xFF4CA5C2), Color(0xFF1C6B8A)),
                    contentColor = Color.White,
                    iconTint = Color.White,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenRecitations
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HomeGradientCard(
                    title = "المفضلة",
                    subtitle = "أذكارك وعلاماتك دائماً",
                    icon = Icons.Default.Star,
                    colors = listOf(Color(0xFFF6CC80), Color(0xFFC99136)),
                    contentColor = Color(0xFF2B1F09),
                    iconTint = Color(0xFF7A5311),
                    modifier = Modifier.weight(1f),
                    onClick = onOpenFavorites
                )
                HomeGradientCard(
                    title = "اتجاه القبلة",
                    subtitle = "بوصلة الكعبة بدقة",
                    icon = Icons.Default.Explore,
                    colors = listOf(Color(0xFF28588A), Color(0xFF0E294D)),
                    contentColor = Color.White,
                    iconTint = SoftGold,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenQibla
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
            color = if (effectiveDark) Color(0xFF132642) else Color(0xFFF5EFE3),
            border = BorderStroke(1.dp, SoftGold.copy(alpha = 0.45f))
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
                    tint = if (effectiveDark) SoftGold else EmeraldDark,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "كل ذكر لله .. يقرّبك من الله",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (effectiveDark) Color.White else EmeraldDark
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
