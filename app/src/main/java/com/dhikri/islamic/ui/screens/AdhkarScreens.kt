package com.dhikri.islamic.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhikri.islamic.ui.components.DhikriCompactTopBar
import com.dhikri.islamic.ui.theme.EmeraldDark
import com.dhikri.islamic.ui.theme.EmeraldPrimary
import com.dhikri.islamic.ui.theme.SoftGold
import com.dhikri.islamic.ui.viewmodel.DhikriViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class DisplayDhikrCard(
    val uniqueKey: String,
    val dbItemId: Int,
    val titleBadge: String? = null,
    val textArabic: String,
    val repeatCount: Int,
    val sourceReference: String,
    val isFavorite: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdhkarListScreen(
    viewModel: DhikriViewModel,
    onOpenCategory: (Int, String) -> Unit,
    onBack: (() -> Unit)? = null
) {
    val categories by viewModel.adhkarCategories.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val otherCategories = remember(categories, searchQuery) {
        val base = categories.filterNot { it.id == 27 }
        if (searchQuery.isBlank()) base
        else base.filter { it.titleArabic.contains(searchQuery.trim(), ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            DhikriCompactTopBar(
                title = "الأذكار",
                subtitle = "أذكار الصباح والمساء وحصن المسلم — دون إنترنت",
                onBack = onBack
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. أذكار الصباح
            if (searchQuery.isBlank() || "أذكار الصباح".contains(searchQuery.trim())) {
                item {
                    PinnedAdhkarBanner(
                        title = "أذكار الصباح",
                        subtitle = "ابدأ يومك بحفظ الله وطمأنينة القلب",
                        icon = Icons.Default.WbSunny,
                        gradient = listOf(Color(0xFFE09F3E), Color(0xFFB56E18)),
                        iconTint = Color(0xFFFFF7D6),
                        onClick = { onOpenCategory(27, "morning") }
                    )
                }
            }

            // 2. أذكار المساء
            if (searchQuery.isBlank() || "أذكار المساء".contains(searchQuery.trim())) {
                item {
                    PinnedAdhkarBanner(
                        title = "أذكار المساء",
                        subtitle = "حصّن مساءك بذكر الله قبل الغروب وبعده",
                        icon = Icons.Default.NightsStay,
                        gradient = listOf(Color(0xFF1A3C6E), EmeraldDark),
                        iconTint = SoftGold,
                        onClick = { onOpenCategory(27, "evening") }
                    )
                }
            }

            // 3. شريط البحث
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("ابحث في باقي الأذكار والأدعية...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "مسح")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Text(
                    text = "باقي الأذكار والأدعية (${otherCategories.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            items(otherCategories, key = { it.id }) { cat ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenCategory(cat.id, "all") },
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(0.7.dp, SoftGold.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(EmeraldPrimary.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = categoryIconFor(cat.titleArabic),
                                contentDescription = null,
                                tint = SoftGold,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                        Text(
                            text = cat.titleArabic,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
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
private fun PinnedAdhkarBanner(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradient: List<Color>,
    iconTint: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.2.dp, SoftGold.copy(alpha = 0.7f)),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(gradient))
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(28.dp)
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.90f),
                    fontSize = 12.5.sp
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

private fun categoryIconFor(title: String): ImageVector = when {
    title.contains("النوم") || title.contains("الاستيقاظ") -> Icons.Default.Bedtime
    title.contains("المسجد") || title.contains("الصلاة") || title.contains("الأذان") -> Icons.Default.Mosque
    title.contains("السفر") || title.contains("الركوب") -> Icons.Default.FlightTakeoff
    title.contains("الطعام") || title.contains("الأكل") -> Icons.Default.Restaurant
    title.contains("المنزل") || title.contains("البيت") -> Icons.Default.Home
    else -> Icons.Default.VolunteerActivism
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
    val scope = rememberCoroutineScope()

    // Filter Morning/Evening & split combined Mu'awwidhat (Al-Ikhlas, Al-Falaq, An-Nas) into 3 separate cards
    val displayCards = remember(rawItems, categoryId, mode) {
        val filtered = if (categoryId == 27 && mode == "morning") {
            rawItems.filterNot { it.textArabic.contains("إذا أمسى)") || it.textArabic.contains("إذا أمسى).") }
        } else if (categoryId == 27 && mode == "evening") {
            rawItems.filterNot { it.textArabic.contains("إذا أصبحَ") || it.textArabic.contains("إذا أصبح)") }
        } else {
            rawItems
        }

        buildList {
            filtered.forEach { item ->
                val cleaned = if (categoryId == 27 && mode == "morning") {
                    item.textArabic.replace(Regex("""\[\s*و?إذا أمسى.*?\]"""), "").trim()
                } else {
                    item.textArabic
                }

                // Detect combined 3 Quls (Al-Ikhlas + Al-Falaq + An-Nas) and split into 3 individual cards
                val hasIkhlas = cleaned.contains("قُلْ هُوَ") || cleaned.contains("قل هو")
                val hasFalaq = cleaned.contains("بِرَبِّ الْفَلَقِ") || cleaned.contains("بِرَبِّ الْفَلَقِ") || cleaned.contains("الفلق")
                val hasNas = cleaned.contains("بِرَبِّ النَّاسِ") || cleaned.contains("بِرَبِّ النَّاسِ") || cleaned.contains("الناس")

                if (hasIkhlas && hasFalaq && hasNas) {
                    val parts = cleaned.split(Regex("""(?=بسم الله الرحمن الرحيم)"""))
                        .map { it.trim().replace(Regex("""\(ثلاث\s+مر[ّٰ]?اتٍ?\)\.?"""), "").trim() }
                        .filter { it.isNotBlank() }

                    if (parts.size == 3) {
                        val surahLabels = listOf("سورة الإخلاص", "سورة الفلق", "سورة الناس")
                        parts.forEachIndexed { idx, surahText ->
                            add(
                                DisplayDhikrCard(
                                    uniqueKey = "${item.id}_qul_$idx",
                                    dbItemId = item.id,
                                    titleBadge = surahLabels[idx],
                                    textArabic = surahText,
                                    repeatCount = item.repeatCount.coerceAtLeast(3),
                                    sourceReference = item.sourceReference,
                                    isFavorite = item.isFavorite
                                )
                            )
                        }
                    } else {
                        add(
                            DisplayDhikrCard(
                                uniqueKey = "${item.id}",
                                dbItemId = item.id,
                                textArabic = cleaned,
                                repeatCount = item.repeatCount,
                                sourceReference = item.sourceReference,
                                isFavorite = item.isFavorite
                            )
                        )
                    }
                } else {
                    add(
                        DisplayDhikrCard(
                            uniqueKey = "${item.id}",
                            dbItemId = item.id,
                            textArabic = cleaned,
                            repeatCount = item.repeatCount,
                            sourceReference = item.sourceReference,
                            isFavorite = item.isFavorite
                        )
                    )
                }
            }
        }.let { cards ->
            if (categoryId == 27 && mode == "morning") {
                val subhanIdx = cards.indexOfFirst { c ->
                    val plain = c.textArabic.replace(Regex("[\u064B-\u065F\u0670]"), "")
                    plain.contains("سبحان الله وبحمده") && plain.length < 120
                }
                if (subhanIdx != -1) {
                    cards.take(subhanIdx + 1)
                } else {
                    cards + DisplayDhikrCard(
                        uniqueKey = "morning_subhanallah_end",
                        dbItemId = 999100,
                        textArabic = "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ.",
                        repeatCount = 100,
                        sourceReference = "من قالها حين يصبح وحين يمسي مائة مرة لم يأتِ أحدٌ يوم القيامة بأفضل مما جاء به — رواه مسلم",
                        isFavorite = 0
                    )
                }
            } else {
                cards
            }
        }
    }

    val screenTitle = when {
        categoryId == 27 && mode == "morning" -> "أذكار الصباح"
        categoryId == 27 && mode == "evening" -> "أذكار المساء"
        else -> categories.firstOrNull { it.id == categoryId }?.titleArabic ?: "الأذكار"
    }

    val progressMap = remember { mutableStateMapOf<String, Int>() }
    val hiddenMap = remember { mutableStateMapOf<String, Boolean>() }
    var adhkarFontSizeSp by remember { mutableFloatStateOf(20f) }
    var isFullScreen by remember { mutableStateOf(false) }

    val completedCount = displayCards.count { (progressMap[it.uniqueKey] ?: 0) >= it.repeatCount }
    val allCompleted = displayCards.isNotEmpty() && displayCards.all { hiddenMap[it.uniqueKey] == true }
    val context = LocalContext.current
    val currentView = LocalView.current

    DisposableEffect(currentView) {
        currentView.keepScreenOn = true
        onDispose { currentView.keepScreenOn = false }
    }
    BackHandler(enabled = isFullScreen) { isFullScreen = false }

    fun handleCardTap(card: DisplayDhikrCard) {
        val currentTapped = progressMap[card.uniqueKey] ?: 0
        if (currentTapped >= card.repeatCount) return
        val nextTapped = currentTapped + 1
        progressMap[card.uniqueKey] = nextTapped
        if (nextTapped >= card.repeatCount) {
            scope.launch {
                // Brief 130ms visual checkmark beat before silky slide-out disappearance
                delay(130)
                hiddenMap[card.uniqueKey] = true
            }
        }
    }

    Scaffold(
        topBar = {
            if (!isFullScreen) {
                DhikriCompactTopBar(
                    title = screenTitle,
                    subtitle = if (displayCards.isNotEmpty()) "المتبقي ${displayCards.size - completedCount} من ${displayCards.size}" else null,
                    onBack = onBack,
                    actions = {
                        IconButton(onClick = { adhkarFontSizeSp = (adhkarFontSizeSp - 2f).coerceAtLeast(15f) }) {
                            Icon(Icons.Default.TextDecrease, contentDescription = "تصغير الخط")
                        }
                        IconButton(onClick = { adhkarFontSizeSp = (adhkarFontSizeSp + 2f).coerceAtMost(34f) }) {
                            Icon(Icons.Default.TextIncrease, contentDescription = "تكبير الخط")
                        }
                        IconButton(onClick = {
                            progressMap.clear()
                            hiddenMap.clear()
                        }) {
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(Modifier.fillMaxSize()) {
                if (displayCards.isNotEmpty() && !isFullScreen) {
                    LinearProgressIndicator(
                        progress = { completedCount.toFloat() / displayCards.size.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp),
                        color = SoftGold,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                if (allCompleted) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = EmeraldDark,
                            border = BorderStroke(1.5.dp, SoftGold),
                            shadowElevation = 8.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = SoftGold,
                                    modifier = Modifier.size(64.dp)
                                )
                                Text(
                                    text = "تقبّل الله ذكرك!",
                                    color = SoftGold,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "أتممت جميع $screenTitle بنجاح.",
                                    color = Color.White.copy(alpha = 0.92f),
                                    fontSize = 15.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(6.dp))
                                Button(
                                    onClick = {
                                        progressMap.clear()
                                        hiddenMap.clear()
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SoftGold,
                                        contentColor = Color(0xFF1F1604)
                                    )
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("إعادة القراءة من البداية", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)
                    ) {
                        items(displayCards, key = { it.uniqueKey }) { card ->
                            val tapped = progressMap[card.uniqueKey] ?: 0
                            val remaining = (card.repeatCount - tapped).coerceAtLeast(0)
                            val isVisible = hiddenMap[card.uniqueKey] != true

                            // Silky-smooth disappear motion + upward placement animation for remaining cards
                            androidx.compose.animation.AnimatedVisibility(
                                modifier = Modifier.animateItem(),
                                    visible = isVisible,
                                    exit = slideOutHorizontally(
                                        targetOffsetX = { -it },
                                        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
                                    ) + scaleOut(
                                        targetScale = 0.92f,
                                        animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing)
                                    ) + fadeOut(
                                        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                                    ) + shrinkVertically(
                                        shrinkTowards = Alignment.Top,
                                        animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing)
                                    )
                                ) {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 14.dp)
                                            .shadow(4.dp, RoundedCornerShape(20.dp))
                                            .clickable { handleCardTap(card) },
                                        shape = RoundedCornerShape(20.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(
                                            if (remaining == 0) 1.5.dp else 1.dp,
                                            if (remaining == 0) Color(0xFF4ADE80) else SoftGold.copy(alpha = 0.45f)
                                        )
                                    ) {
                                        Column(Modifier.fillMaxWidth()) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp, vertical = 16.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                card.titleBadge?.let { badge ->
                                                    Surface(
                                                        shape = RoundedCornerShape(10.dp),
                                                        color = SoftGold.copy(alpha = 0.18f),
                                                        border = BorderStroke(0.8.dp, SoftGold.copy(alpha = 0.65f))
                                                    ) {
                                                        Text(
                                                            text = badge,
                                                            color = SoftGold,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 13.sp,
                                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = card.textArabic,
                                                    fontSize = adhkarFontSizeSp.sp,
                                                    lineHeight = (adhkarFontSizeSp * 1.75f).sp,
                                                    textAlign = TextAlign.Center,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                                if (card.sourceReference.isNotBlank()) {
                                                    Text(
                                                        text = card.sourceReference,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                            }

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(
                                                        Brush.horizontalGradient(
                                                            listOf(Color(0xFF163A69), EmeraldDark)
                                                        )
                                                    )
                                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clickable { handleCardTap(card) }
                                                ) {
                                                    Text(
                                                        text = "التكرار",
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 15.sp
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .size(34.dp)
                                                            .clip(CircleShape)
                                                            .background(if (remaining == 0) Color(0xFF4ADE80) else SoftGold),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        if (remaining == 0) {
                                                            Icon(
                                                                Icons.Default.Check,
                                                                contentDescription = "مكتمل",
                                                                tint = Color(0xFF062814),
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        } else {
                                                            Text(
                                                                text = "$remaining",
                                                                color = Color(0xFF1F1604),
                                                                fontWeight = FontWeight.ExtraBold,
                                                                fontSize = 15.sp
                                                            )
                                                        }
                                                    }
                                                }

                                                Box(
                                                    modifier = Modifier
                                                        .width(1.dp)
                                                        .height(24.dp)
                                                        .background(Color.White.copy(alpha = 0.25f))
                                                )

                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    modifier = Modifier.padding(start = 10.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(10.dp))
                                                            .clickable {
                                                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                                    type = "text/plain"
                                                                    putExtra(
                                                                        Intent.EXTRA_TEXT,
                                                                        "${card.textArabic}\n\nالمصدر: ${card.sourceReference}\nعبر تطبيق ذِكري"
                                                                    )
                                                                }
                                                                context.startActivity(Intent.createChooser(shareIntent, "مشاركة الذكر"))
                                                            }
                                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                                    ) {
                                                        Text(
                                                            text = "مشاركة",
                                                            color = Color.White,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 14.sp
                                                        )
                                                        Icon(
                                                            imageVector = Icons.Default.Share,
                                                            contentDescription = "مشاركة",
                                                            tint = SoftGold,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = { viewModel.toggleAdhkarFavorite(card.dbItemId, card.isFavorite == 0) },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = if (card.isFavorite == 1) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                            contentDescription = "مفضلة",
                                                            tint = if (card.isFavorite == 1) SoftGold else Color.White.copy(alpha = 0.8f),
                                                            modifier = Modifier.size(19.dp)
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
                        Icon(
                            Icons.Default.FullscreenExit,
                            contentDescription = "إظهار الأشرطة",
                            tint = SoftGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Text("إظهار الأشرطة", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
