package com.dhikri.islamic.ui.screens

import com.dhikri.islamic.ui.components.DhikriCompactTopBar

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhikri.islamic.ui.theme.EmeraldDark
import com.dhikri.islamic.ui.theme.EmeraldPrimary
import com.dhikri.islamic.ui.theme.SoftGold
import com.dhikri.islamic.ui.viewmodel.DhikriViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdhkarListScreen(
    viewModel: DhikriViewModel,
    onOpenCategory: (Int, String) -> Unit
) {
    val categories by viewModel.adhkarCategories.collectAsState()
    var selectedGroup by remember { mutableStateOf("all") }
    var searchQuery by remember { mutableStateOf("") }

    val groups = listOf(
        "all" to "الكل",
        "waking" to "الاستيقاظ",
        "prayer" to "الصلاة",
        "quranic_duas" to "الأدعية القرآنية",
        "prophets_duas" to "أدعية الأنبياء",
        "home" to "المنزل",
        "food" to "الطعام",
        "travel" to "السفر",
        "ruqyah" to "التحصين",
        "daily" to "أذكار يومية"
    )

    // Exclude combined Morning/Evening (id=27) from the lower list since we show dedicated Morning & Evening cards on top
    val filtered = categories.filter { cat ->
        cat.id != 27 &&
            (selectedGroup == "all" || cat.groupKey == selectedGroup) &&
            (searchQuery.isBlank() || cat.titleArabic.contains(searchQuery.trim()))
    }

    Scaffold(
        topBar = {
            DhikriCompactTopBar(
                title = "الأذكار والأدعية",
                subtitle = "حصن المسلم والأدعية القرآنية — يعمل دون إنترنت"
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Search bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("ابحث عن ذكر أو دعاء...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 2x2 Hero Grid: Morning SEPARATED from Evening + Sleep + After Prayer
            if (searchQuery.isBlank()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AdhkarHeroCard(
                                title = "أذكار الصباح",
                                subtitle = "بداية يومك بالخير",
                                icon = Icons.Default.WbSunny,
                                colors = listOf(Color(0xFFF5C96B), Color(0xFFC88A29)),
                                contentColor = Color(0xFF2B1D06),
                                modifier = Modifier.weight(1f),
                                onClick = { onOpenCategory(27, "morning") }
                            )
                            AdhkarHeroCard(
                                title = "أذكار المساء",
                                subtitle = "حصنك حتى الصباح",
                                icon = Icons.Default.NightsStay,
                                colors = listOf(Color(0xFF4B5E9E), Color(0xFF212E5E)),
                                contentColor = Color.White,
                                modifier = Modifier.weight(1f),
                                onClick = { onOpenCategory(27, "evening") }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AdhkarHeroCard(
                                title = "أذكار النوم",
                                subtitle = "قبل المنام",
                                icon = Icons.Default.Bedtime,
                                colors = listOf(Color(0xFF7D6FC5), Color(0xFF4A3D87)),
                                contentColor = Color.White,
                                modifier = Modifier.weight(1f),
                                onClick = { onOpenCategory(28, "all") }
                            )
                            AdhkarHeroCard(
                                title = "أذكار بعد الصلاة",
                                subtitle = "عقب السلام",
                                icon = Icons.Default.Mosque,
                                colors = listOf(Color(0xFF3E8463), Color(0xFF165238)),
                                contentColor = Color.White,
                                modifier = Modifier.weight(1f),
                                onClick = { onOpenCategory(25, "all") }
                            )
                        }
                    }
                }
            }

            // Category Filter Chips
            item {
                Text(
                    "أبواب حصن المسلم والأدعية",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(groups) { (key, label) ->
                        FilterChip(
                            selected = selectedGroup == key,
                            onClick = { selectedGroup = key },
                            label = { Text(label) }
                        )
                    }
                }
            }

            // Clean Category List
            items(filtered, key = { it.id }) { cat ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onOpenCategory(cat.id, "all") },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(0.7.dp, SoftGold.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (cat.groupKey) {
                                    "quranic_duas", "prophets_duas" -> Icons.Default.MenuBook
                                    "prayer" -> Icons.Default.Mosque
                                    "travel" -> Icons.Default.FlightTakeoff
                                    "food" -> Icons.Default.Restaurant
                                    "home" -> Icons.Default.Home
                                    else -> Icons.Default.VolunteerActivism
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = cat.titleArabic,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            Icons.Default.ChevronLeft,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdhkarHeroCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    colors: List<Color>,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(108.dp)
            .shadow(4.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(colors))
            .clickable(onClick = onClick)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = title, tint = contentColor, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(6.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = contentColor, textAlign = TextAlign.Center)
            Text(subtitle, style = MaterialTheme.typography.labelMedium, color = contentColor.copy(alpha = 0.85f), textAlign = TextAlign.Center)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdhkarDetailScreen(
    categoryId: Int,
    mode: String = "all",
    viewModel: DhikriViewModel,
    onBack: () -> Unit
) {
    val categories by viewModel.adhkarCategories.collectAsState()
    val rawItems by viewModel.observeAdhkarItems(categoryId).collectAsState(initial = emptyList())

    // Separate Morning vs Evening cleanly when categoryId == 27
    val itemsList = remember(rawItems, categoryId, mode) {
        if (categoryId == 27 && mode == "morning") {
            rawItems.filterNot { it.textArabic.contains("إذا أمسى)") || it.textArabic.contains("إذا أمسى).") }
        } else if (categoryId == 27 && mode == "evening") {
            rawItems.filterNot { it.textArabic.contains("إذا أصبحَ") || it.textArabic.contains("إذا أصبح)") }
        } else {
            rawItems
        }
    }

    val screenTitle = when {
        categoryId == 27 && mode == "morning" -> "أذكار الصباح"
        categoryId == 27 && mode == "evening" -> "أذكار المساء"
        else -> categories.firstOrNull { it.id == categoryId }?.titleArabic ?: "الأذكار"
    }

    val progressMap = remember { mutableStateMapOf<Int, Int>() }
    var adhkarFontSizeSp by remember { mutableFloatStateOf(19f) }
    var isFullScreen by remember { mutableStateOf(false) }
    val currentView = LocalView.current
    DisposableEffect(currentView) {
        currentView.keepScreenOn = true
        onDispose { currentView.keepScreenOn = false }
    }
    BackHandler(enabled = isFullScreen) { isFullScreen = false }
    val completedCount = itemsList.count { (progressMap[it.id] ?: 0) >= it.repeatCount }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            if (!isFullScreen) {
            DhikriCompactTopBar(
                title = screenTitle,
                subtitle = if (itemsList.isNotEmpty()) "أتممت $completedCount من ${itemsList.size}" else null,
                onBack = onBack,
                actions = {
                    IconButton(onClick = { adhkarFontSizeSp = (adhkarFontSizeSp - 2f).coerceAtLeast(15f) }) {
                        Icon(Icons.Default.TextDecrease, contentDescription = "تصغير الخط")
                    }
                    IconButton(onClick = { adhkarFontSizeSp = (adhkarFontSizeSp + 2f).coerceAtMost(34f) }) {
                        Icon(Icons.Default.TextIncrease, contentDescription = "تكبير الخط")
                    }
                    IconButton(onClick = { progressMap.clear() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "إعادة التصفير")
                    }
                    IconButton(onClick = { isFullScreen = true }) {
                        Icon(Icons.Default.Fullscreen, contentDescription = "ملء الشاشة")
                    }
                }
            )
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
        Column(Modifier.fillMaxSize()) {
            if (itemsList.isNotEmpty()) {
                LinearProgressIndicator(
                    progress = { completedCount.toFloat() / itemsList.size.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(5.dp),
                    color = EmeraldPrimary
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(itemsList, key = { it.id }) { item ->
                    val count = progressMap[item.id] ?: 0
                    val done = count >= item.repeatCount
                    val displayText = remember(item.textArabic, mode) {
                        if (categoryId == 27 && mode == "morning") {
                            item.textArabic.replace(Regex("""\[\s*و?إذا أمسى.*?\]"""), "").trim()
                        } else {
                            item.textArabic
                        }
                    }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (count < item.repeatCount) progressMap[item.id] = count + 1
                            },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (done) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(0.8.dp, if (done) EmeraldPrimary else SoftGold.copy(alpha = 0.4f))
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(displayText, fontSize = adhkarFontSizeSp.sp, lineHeight = (adhkarFontSizeSp * 1.7f).sp)
                            Text(
                                "المصدر: ${item.sourceReference}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        if (count < item.repeatCount) progressMap[item.id] = count + 1
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        if (done) "تمت القراءة ✓ (${item.repeatCount}/${item.repeatCount})"
                                        else "اضغط للعد: $count / ${item.repeatCount}",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Row {
                                    IconButton(onClick = {
                                        viewModel.toggleAdhkarFavorite(item.id, item.isFavorite == 0)
                                    }) {
                                        Icon(
                                            if (item.isFavorite == 1) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = "مفضلة"
                                        )
                                    }
                                    IconButton(onClick = {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, "$displayText\n\nالمصدر: ${item.sourceReference}\nعبر تطبيق ذِكري")
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "مشاركة الذكر"))
                                    }) {
                                        Icon(Icons.Default.Share, contentDescription = "مشاركة")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (isFullScreen) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(10.dp)
                    .clickable { isFullScreen = false },
                shape = RoundedCornerShape(14.dp),
                color = EmeraldDark.copy(alpha = 0.82f),
                border = BorderStroke(1.dp, SoftGold.copy(alpha = 0.7f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.FullscreenExit, contentDescription = "إظهار الأشرطة", tint = SoftGold, modifier = Modifier.size(16.dp))
                    Text("إظهار الأشرطة", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        }
    }
}
