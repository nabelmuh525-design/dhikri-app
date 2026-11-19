package com.dhikri.islamic.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhikri.islamic.data.local.entity.AyahEntity
import com.dhikri.islamic.data.local.entity.TafsirEntryEntity
import com.dhikri.islamic.ui.theme.HafsFontFamily
import com.dhikri.islamic.ui.theme.NightReadingParchment
import com.dhikri.islamic.ui.theme.SoftGold
import com.dhikri.islamic.ui.viewmodel.DhikriViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MushafReaderScreen(
    initialPage: Int,
    viewModel: DhikriViewModel,
    onBack: () -> Unit,
    onOpenDownloads: () -> Unit
) {
    var currentPage by remember { mutableIntStateOf(initialPage.coerceIn(1, 604)) }
    val ayahs by viewModel.observePageAyahs(currentPage).collectAsState(initial = emptyList())
    val surahs by viewModel.surahs.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val favoriteAyahs by viewModel.favoriteAyahs.collectAsState()
    val tafsirBooks by viewModel.tafsirBooks.collectAsState()

    var selectedAyah by remember { mutableStateOf<AyahEntity?>(null) }
    var selectedTafsirEntries by remember { mutableStateOf<List<TafsirEntryEntity>>(emptyList()) }

    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    // Save reading position automatically whenever page loads
    LaunchedEffect(currentPage, ayahs) {
        ayahs.firstOrNull()?.let { first ->
            viewModel.saveLastRead(first.surahId, first.ayahNumber, currentPage)
        }
    }

    // Load tafsir when user taps an ayah
    LaunchedEffect(selectedAyah) {
        val a = selectedAyah
        selectedTafsirEntries = if (a != null) {
            viewModel.getTafsirForAyah(a.surahId, a.ayahNumber)
        } else {
            emptyList()
        }
    }

    // Predictive Back: if Tafsir sheet is open, close it first before popping Mushaf screen
    BackHandler(enabled = selectedAyah != null) {
        selectedAyah = null
    }

    val isNight = settings.nightReadingMode
    val bgColor = if (isNight) NightReadingParchment else MaterialTheme.colorScheme.background
    val txtColor = if (isNight) Color(0xFFE7E2D4) else MaterialTheme.colorScheme.onBackground

    val firstAyah = ayahs.firstOrNull()
    val headerSurah = surahs.firstOrNull { it.id == (firstAyah?.surahId ?: 1) }

    Scaffold(
        containerColor = bgColor,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = headerSurah?.let { "سورة ${it.nameArabic}" } ?: "المصحف الشريف",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "صفحة $currentPage / 604 • الجزء ${firstAyah?.juzNumber ?: 1} • الحزب ${firstAyah?.hizbNumber ?: 1}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.setMushafFontSize(settings.mushafFontSizeSp - 2f) }) {
                        Icon(Icons.Default.TextDecrease, contentDescription = "تصغير الخط")
                    }
                    IconButton(onClick = { viewModel.setMushafFontSize(settings.mushafFontSizeSp + 2f) }) {
                        Icon(Icons.Default.TextIncrease, contentDescription = "تكبير الخط")
                    }
                    IconButton(onClick = { viewModel.setNightReadingMode(!isNight) }) {
                        Icon(
                            if (isNight) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "القراءة الليلية"
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { if (currentPage > 1) currentPage-- },
                        enabled = currentPage > 1
                    ) { Text("الصفحة السابقة") }

                    Text("$currentPage / 604", fontWeight = FontWeight.Bold)

                    Button(
                        onClick = { if (currentPage < 604) currentPage++ },
                        enabled = currentPage < 604
                    ) { Text("الصفحة التالية") }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 18.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(ayahs, key = { it.id }) { ayah ->
                val surah = surahs.firstOrNull { it.id == ayah.surahId }
                if (ayah.ayahNumber == 1 && surah != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("سورة ${surah.nameArabic}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                            Text(
                                "${surah.revelationType} • ${surah.ayahCount} آية",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                    if (ayah.surahId != 1 && ayah.surahId != 9) {
                        Text(
                            text = "بِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ",
                            fontFamily = HafsFontFamily,
                            fontSize = (settings.mushafFontSizeSp - 2f).sp,
                            color = SoftGold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        )
                    }
                }

                val isFav = favoriteAyahs.any { it.ayahId == ayah.id }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedAyah = ayah }
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "${ayah.textHafsV20} ﴿${ayah.ayahNumber}﴾" + if (ayah.isSajdah == 1) " ۩" else "",
                        fontFamily = HafsFontFamily,
                        fontSize = settings.mushafFontSizeSp.sp,
                        lineHeight = (settings.mushafFontSizeSp * 1.85f).sp,
                        color = txtColor,
                        textAlign = TextAlign.Justify,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (ayah.isSajdah == 1 || isFav) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (ayah.isSajdah == 1) {
                                Text(
                                    "موضع سجدة (${ayah.sajdahType ?: "مستحبة"})",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SoftGold
                                )
                            }
                            if (isFav) {
                                Text("★ في المفضلة", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            }
        }
    }

    // Ayah Actions + Tafsir & Word Meanings Sheet
    selectedAyah?.let { ayah ->
        val surahName = surahs.firstOrNull { it.id == ayah.surahId }?.nameArabic ?: "${ayah.surahId}"
        val isFav = favoriteAyahs.any { it.ayahId == ayah.id }
        ModalBottomSheet(onDismissRequest = { selectedAyah = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "سورة $surahName — الآية ${ayah.ayahNumber}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AssistChip(
                        onClick = {
                            viewModel.addBookmark(ayah.surahId, ayah.ayahNumber, ayah.pageNumber, "سورة $surahName : ${ayah.ayahNumber}")
                        },
                        label = { Text("علامة") },
                        leadingIcon = { Icon(Icons.Default.BookmarkAdd, contentDescription = null) }
                    )
                    AssistChip(
                        onClick = { viewModel.toggleFavoriteAyah(ayah, isFav) },
                        label = { Text(if (isFav) "إزالة مفضلة" else "مفضلة") },
                        leadingIcon = { Icon(if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = null) }
                    )
                    AssistChip(
                        onClick = {
                            clipboard.setText(AnnotatedString("${ayah.textHafsV20} [سورة $surahName: ${ayah.ayahNumber}]"))
                        },
                        label = { Text("نسخ") },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) }
                    )
                    AssistChip(
                        onClick = {
                            val shareText = "﴿${ayah.textHafsV20}﴾
[سورة $surahName — الآية ${ayah.ayahNumber}]
عبر تطبيق ذِكري"
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(intent, "مشاركة الآية"))
                        },
                        label = { Text("مشاركة") },
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
                    )
                }

                HorizontalDivider()
                Text("التفسير ومعاني الكلمات", style = MaterialTheme.typography.titleMedium)

                if (selectedTafsirEntries.isEmpty()) {
                    Text(
                        "لم يتم تنزيل حزمة التفسير الميسر أو معاني الكلمات بعد. يمكنك تنزيلهما من مركز التنزيلات (المصدر الرسمي: QuranEnc.com).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = {
                        selectedAyah = null
                        onOpenDownloads()
                    }) {
                        Text("فتح مركز التنزيلات")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(selectedTafsirEntries) { entry ->
                            val book = tafsirBooks.firstOrNull { it.bookKey == entry.bookKey }
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        book?.titleArabic ?: entry.bookKey,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        "المؤلف: ${book?.authorArabic ?: "—"} • المصدر: ${book?.publisher ?: "QuranEnc.com"} • الإصدار: ${book?.version ?: "—"}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(entry.textContent, style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
