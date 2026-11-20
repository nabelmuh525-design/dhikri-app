package com.dhikri.islamic.ui.screens

import android.icu.text.DateFormat
import android.icu.util.Calendar
import android.icu.util.ULocale
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhikri.islamic.ui.components.DhikriCompactTopBar
import com.dhikri.islamic.ui.theme.EmeraldDark
import com.dhikri.islamic.ui.theme.SoftGold
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class HijriMonthMeta(
    val number: Int,
    val name: String,
    val note: String,
    val isSacred: Boolean = false
)

private data class IslamicOccasionItem(
    val title: String,
    val subtitle: String,
    val hijriMonth: Int, // 1..12
    val hijriDay: Int,
    val dateLabel: String
)

private data class HijriDayCell(
    val dayNumber: Int,
    val timeMillis: Long,
    val dayOfWeek: Int // Calendar.SUNDAY..SATURDAY
)

private val hijriMonthsList = listOf(
    HijriMonthMeta(1, "محرّم", "أول أشهر السنة الهجرية", isSacred = true),
    HijriMonthMeta(2, "صَفَر", "الشهر الثاني"),
    HijriMonthMeta(3, "ربيع الأول", "شهر الهجرة النبوية"),
    HijriMonthMeta(4, "ربيع الآخر", "الشهر الرابع"),
    HijriMonthMeta(5, "جمادى الأولى", "الشهر الخامس"),
    HijriMonthMeta(6, "جمادى الآخرة", "الشهر السادس"),
    HijriMonthMeta(7, "رجب", "من الأشهر الحرم", isSacred = true),
    HijriMonthMeta(8, "شعبان", "تُرفع فيه الأعمال"),
    HijriMonthMeta(9, "رمضان", "شهر الصيام والقرآن"),
    HijriMonthMeta(10, "شوّال", "عيد الفطر وصيام الست"),
    HijriMonthMeta(11, "ذو القعدة", "من الأشهر الحرم", isSacred = true),
    HijriMonthMeta(12, "ذو الحجة", "موسم الحج وعرفة والأضحى", isSacred = true)
)

private val islamicOccasionsList = listOf(
    IslamicOccasionItem("غرة شهر رمضان المبارك", "بداية شهر الصيام والقيام والقرآن", 9, 1, "1 رمضان"),
    IslamicOccasionItem("العشر الأواخر وليلة القدر", "خير من ألف شهر — التمسوها في الوتر", 9, 21, "21 - 29 رمضان"),
    IslamicOccasionItem("عيد الفطر المبارك", "يوم الفرح والجائزة بعد إتمام الصيام", 10, 1, "1 شوّال"),
    IslamicOccasionItem("العشر الأوائل من ذي الحجة", "أفضل أيام الدنيا للعمل الصالح", 12, 1, "1 - 9 ذو الحجة"),
    IslamicOccasionItem("يوم عرفة", "صيامه يكفّر سنتين (الماضية والقادمة)", 12, 9, "9 ذو الحجة"),
    IslamicOccasionItem("عيد الأضحى وأيام التشريق", "شعيرة الأضحية وأيام الذكر والشكر", 12, 10, "10 ذو الحجة"),
    IslamicOccasionItem("رأس السنة الهجرية", "بداية العام الهجري الجديد", 1, 1, "1 محرّم"),
    IslamicOccasionItem("يوم عاشوراء", "يُستحب صيامه ويكفّر السنة الماضية", 1, 10, "10 محرّم")
)

private fun computeDaysUntilOccasion(uLocale: ULocale, targetMonth1Based: Int, targetDay: Int): Int {
    val today = Calendar.getInstance(uLocale)
    val todayYear = today.get(Calendar.YEAR)
    val todayStart = (today.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }

    fun targetCal(year: Int): Calendar = Calendar.getInstance(uLocale).apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, targetMonth1Based - 1)
        set(Calendar.DAY_OF_MONTH, targetDay)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }

    var target = targetCal(todayYear)
    if (target.timeInMillis < todayStart.timeInMillis) {
        target = targetCal(todayYear + 1)
    }
    return ((target.timeInMillis - todayStart.timeInMillis) / 86_400_000L).toInt().coerceAtLeast(0)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HijriCalendarModernScreen(onBack: () -> Unit) {
    val uLocale = remember { ULocale("ar@calendar=islamic-umalqura") }
    val todayCal = remember { Calendar.getInstance(uLocale) }
    val todayYear = remember { todayCal.get(Calendar.YEAR) }
    val todayMonth = remember { todayCal.get(Calendar.MONTH) } // 0-based
    val todayDay = remember { todayCal.get(Calendar.DAY_OF_MONTH) }

    val todayHijriFull = remember {
        val dayName = SimpleDateFormat("EEEE", Locale("ar")).format(Date())
        val formatted = runCatching {
            DateFormat.getDateInstance(DateFormat.LONG, uLocale).format(todayCal)
        }.getOrElse { "$todayDay ${hijriMonthsList.getOrNull(todayMonth)?.name ?: ""} $todayYear هـ" }
        "$dayName، $formatted"
    }
    val todayGregFull = remember {
        SimpleDateFormat("EEEE، d MMMM yyyy", Locale("ar")).format(Date())
    }

    // Nearest upcoming occasion for countdown hero
    val occasionsWithDays = remember {
        islamicOccasionsList.map { occ ->
            occ to computeDaysUntilOccasion(uLocale, occ.hijriMonth, occ.hijriDay)
        }.sortedBy { it.second }
    }
    val nearestOccasion = occasionsWithDays.firstOrNull()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = التقويم الشهري, 1 = المناسبات والأشهر
    var displayedYear by remember { mutableIntStateOf(todayYear) }
    var displayedMonth by remember { mutableIntStateOf(todayMonth) } // 0..11
    var selectedDayNum by remember { mutableIntStateOf(todayDay) }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val goldAccent = if (isDark) SoftGold else Color(0xFF9C7826)

    // Compute cells for displayed Hijri month
    val monthData = remember(displayedYear, displayedMonth) {
        val cal = Calendar.getInstance(uLocale).apply {
            set(Calendar.YEAR, displayedYear)
            set(Calendar.MONTH, displayedMonth)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 12); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
        }
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH).coerceIn(29, 30)
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1=Sun .. 7=Sat
        val cells = (1..daysInMonth).map { d ->
            val c = (cal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, d) }
            HijriDayCell(d, c.timeInMillis, c.get(Calendar.DAY_OF_WEEK))
        }
        Triple(firstDayOfWeek - 1, daysInMonth, cells) // leadingBlanks, daysCount, cells
    }

    val leadingBlanks = monthData.first
    val cells = monthData.third
    val selectedCell = cells.firstOrNull { it.dayNumber == selectedDayNum } ?: cells.firstOrNull()

    Scaffold(
        topBar = {
            DhikriCompactTopBar(
                title = "التقويم والمناسبات الإسلامية",
                subtitle = "تقويم أم القرى الهجري",
                onBack = onBack
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Modern Royal Navy & Gold Today + Nearest Occasion Countdown Hero Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth().shadow(6.dp, RoundedCornerShape(22.dp)),
                    shape = RoundedCornerShape(22.dp),
                    color = Color.Transparent,
                    border = BorderStroke(1.2.dp, SoftGold.copy(alpha = 0.75f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(Color(0xFF163A69), EmeraldDark)))
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("التاريخ الهجري اليوم (أم القرى)", color = SoftGold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text(todayHijriFull, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                                Text("الموافق ميلادياً: $todayGregFull", color = Color.White.copy(alpha = 0.82f), fontSize = 12.5.sp)
                            }
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(SoftGold.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = SoftGold, modifier = Modifier.size(25.dp))
                            }
                        }

                        nearestOccasion?.let { (occ, daysLeft) ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White.copy(alpha = 0.10f),
                                border = BorderStroke(0.8.dp, SoftGold.copy(alpha = 0.55f))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = SoftGold, modifier = Modifier.size(16.dp))
                                        Text(
                                            text = "المناسبة القادمة: ${occ.title}",
                                            color = Color.White,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = SoftGold
                                    ) {
                                        Text(
                                            text = if (daysLeft == 0) "اليوم!" else "بعد $daysLeft يوم",
                                            color = Color(0xFF1F1604),
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Clean 2-Tab Segmented Switcher
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val tabs = listOf(
                        Pair(0, "التقويم الشهري"),
                        Pair(1, "المناسبات والأشهر")
                    )
                    tabs.forEach { (idx, label) ->
                        val selected = selectedTab == idx
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedTab = idx },
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) EmeraldDark else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                if (selected) 1.5.dp else 0.9.dp,
                                if (selected) SoftGold else SoftGold.copy(alpha = 0.35f)
                            )
                        ) {
                            Text(
                                text = label,
                                color = if (selected) SoftGold else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 11.dp)
                            )
                        }
                    }
                }
            }

            if (selectedTab == 0) {
                // TAB 1: Interactive Monthly Hijri Calendar Grid
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, SoftGold.copy(alpha = 0.4f)),
                        shadowElevation = 3.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Month navigation bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        if (displayedMonth == 0) {
                                            displayedMonth = 11
                                            displayedYear -= 1
                                        } else {
                                            displayedMonth -= 1
                                        }
                                        selectedDayNum = 1
                                    },
                                    modifier = Modifier.size(36.dp).clip(CircleShape).background(EmeraldDark)
                                ) {
                                    Icon(Icons.Default.ChevronRight, contentDescription = "الشهر السابق", tint = SoftGold)
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${hijriMonthsList[displayedMonth].name} $displayedYear هـ",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = goldAccent
                                    )
                                    if (displayedYear != todayYear || displayedMonth != todayMonth) {
                                        Text(
                                            text = "العودة لليوم",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.clickable {
                                                displayedYear = todayYear
                                                displayedMonth = todayMonth
                                                selectedDayNum = todayDay
                                            }
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        if (displayedMonth == 11) {
                                            displayedMonth = 0
                                            displayedYear += 1
                                        } else {
                                            displayedMonth += 1
                                        }
                                        selectedDayNum = 1
                                    },
                                    modifier = Modifier.size(36.dp).clip(CircleShape).background(EmeraldDark)
                                ) {
                                    Icon(Icons.Default.ChevronLeft, contentDescription = "الشهر التالي", tint = SoftGold)
                                }
                            }

                            // Weekday Headers
                            val weekDays = listOf("أحد", "إثنين", "ثلاثاء", "أربعاء", "خميس", "جمعة", "سبت")
                            Row(modifier = Modifier.fillMaxWidth()) {
                                weekDays.forEach { wd ->
                                    Text(
                                        text = wd,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Center,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            HorizontalDivider(color = SoftGold.copy(alpha = 0.2f))

                            // Calendar Rows (7 columns)
                            val totalSlots = leadingBlanks + cells.size
                            val rows = (totalSlots + 6) / 7
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                for (r in 0 until rows) {
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        for (c in 0 until 7) {
                                            val slotIdx = r * 7 + c
                                            val cellIdx = slotIdx - leadingBlanks
                                            if (cellIdx in cells.indices) {
                                                val cell = cells[cellIdx]
                                                val isToday = displayedYear == todayYear && displayedMonth == todayMonth && cell.dayNumber == todayDay
                                                val isSelected = cell.dayNumber == selectedDayNum
                                                val isWhiteDay = cell.dayNumber in 13..15

                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .aspectRatio(1f)
                                                        .padding(2.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            when {
                                                                isToday -> SoftGold
                                                                isSelected -> EmeraldDark
                                                                isWhiteDay -> SoftGold.copy(alpha = 0.16f)
                                                                else -> Color.Transparent
                                                            }
                                                        )
                                                        .clickable { selectedDayNum = cell.dayNumber },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text(
                                                            text = "${cell.dayNumber}",
                                                            fontSize = 13.5.sp,
                                                            fontWeight = if (isToday || isSelected || isWhiteDay) FontWeight.ExtraBold else FontWeight.Medium,
                                                            color = when {
                                                                isToday -> Color(0xFF1F1604)
                                                                isSelected -> SoftGold
                                                                else -> MaterialTheme.colorScheme.onSurface
                                                            }
                                                        )
                                                        if (isWhiteDay && !isToday) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(4.dp)
                                                                    .clip(CircleShape)
                                                                    .background(SoftGold)
                                                            )
                                                        }
                                                    }
                                                }
                                            } else {
                                                Spacer(Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }

                            // Legend
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Box(Modifier.size(10.dp).clip(CircleShape).background(SoftGold))
                                    Text("اليوم", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Box(Modifier.size(10.dp).clip(CircleShape).background(SoftGold.copy(alpha = 0.35f)))
                                    Text("الأيام البيض (13 - 15)", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                // Selected Day Details Card (Gregorian equivalent + Fasting recommendation)
                selectedCell?.let { cell ->
                    item {
                        val gregStr = SimpleDateFormat("EEEE، d MMMM yyyy", Locale("ar")).format(Date(cell.timeMillis))
                        val isWhiteDay = cell.dayNumber in 13..15
                        val isMonThu = cell.dayOfWeek == Calendar.MONDAY || cell.dayOfWeek == Calendar.THURSDAY
                        val fastingHint = when {
                            isWhiteDay && isMonThu -> "من الأيام البيض ويوم إثنين/خميس — يُستحب صيامه"
                            isWhiteDay -> "من الأيام البيض — يُستحب صيامه"
                            isMonThu -> "يُستحب صيام الاثنين والخميس"
                            cell.dayOfWeek == Calendar.FRIDAY -> "يوم الجمعة — لا تنسَ سورة الكهف والصلاة على النبي ﷺ"
                            else -> null
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, SoftGold.copy(alpha = 0.45f))
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "${cell.dayNumber} ${hijriMonthsList[displayedMonth].name} $displayedYear هـ",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = goldAccent
                                )
                                Text(
                                    text = "يوافقه ميلادياً: $gregStr",
                                    fontSize = 13.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                fastingHint?.let { hint ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = SoftGold.copy(alpha = 0.16f)
                                    ) {
                                        Text(
                                            text = "✦ $hint",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = goldAccent,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // TAB 2: Islamic Occasions + 12 Hijri Months Modern Grid
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.width(4.dp).height(18.dp).clip(RoundedCornerShape(2.dp)).background(SoftGold))
                        Text("المناسبات والأيام الدينية القادمة", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = goldAccent)
                    }
                }

                items(occasionsWithDays, key = { it.first.title }) { (occ, daysLeft) ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(0.9.dp, SoftGold.copy(alpha = 0.38f)),
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(42.dp).clip(CircleShape).background(EmeraldDark),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.NightsStay, contentDescription = null, tint = SoftGold, modifier = Modifier.size(20.dp))
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(occ.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(occ.subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = EmeraldDark
                                ) {
                                    Text(
                                        text = occ.dateLabel,
                                        color = SoftGold,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Text(
                                    text = if (daysLeft == 0) "اليوم" else "بعد $daysLeft يوم",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = goldAccent
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.width(4.dp).height(18.dp).clip(RoundedCornerShape(2.dp)).background(SoftGold))
                        Text("الأشهر الهجرية (12 شهراً)", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = goldAccent)
                    }
                }

                // 2-Column Modern Grid of the 12 Hijri Months
                items(hijriMonthsList.chunked(2)) { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        pair.forEach { m ->
                            val isCurrentMonth = (m.number - 1) == todayMonth
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        displayedMonth = m.number - 1
                                        displayedYear = todayYear
                                        selectedDayNum = if (isCurrentMonth) todayDay else 1
                                        selectedTab = 0
                                    },
                                shape = RoundedCornerShape(18.dp),
                                color = if (isCurrentMonth) EmeraldDark else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    if (isCurrentMonth) 1.5.dp else 0.8.dp,
                                    if (isCurrentMonth) SoftGold else SoftGold.copy(alpha = 0.35f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(if (isCurrentMonth) SoftGold else EmeraldDark),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${m.number}",
                                                color = if (isCurrentMonth) Color(0xFF1F1604) else SoftGold,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 12.sp
                                            )
                                        }
                                        if (isCurrentMonth) {
                                            Surface(shape = RoundedCornerShape(8.dp), color = SoftGold.copy(alpha = 0.22f)) {
                                                Text(
                                                    "الشهر الحالي",
                                                    color = SoftGold,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                                )
                                            }
                                        } else if (m.isSacred) {
                                            Surface(shape = RoundedCornerShape(8.dp), color = SoftGold.copy(alpha = 0.16f)) {
                                                Text(
                                                    "شهر حرام",
                                                    color = goldAccent,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = m.name,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        color = if (isCurrentMonth) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = m.note,
                                        fontSize = 11.5.sp,
                                        color = if (isCurrentMonth) Color.White.copy(alpha = 0.82f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
