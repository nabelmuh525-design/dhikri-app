package com.dhikri.islamic.ui.screens

import com.dhikri.islamic.ui.components.DhikriCompactTopBar

import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.speech.tts.TextToSpeech
import java.util.Locale
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhikri.islamic.data.local.entity.AyahEntity
import com.dhikri.islamic.data.local.entity.TafsirEntryEntity
import com.dhikri.islamic.ui.theme.HafsFontFamily
import com.dhikri.islamic.ui.theme.SoftGold
import com.dhikri.islamic.ui.theme.ReadingThemes
import com.dhikri.islamic.ui.theme.readingThemeByKey
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
    val listState = rememberLazyListState()
    val cachedInitial = remember(currentPage) { viewModel.getCachedPageAyahs(currentPage) }
    val ayahs by viewModel.observePageAyahs(currentPage).collectAsState(initial = cachedInitial)
    val surahs by viewModel.surahs.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val favoriteAyahs by viewModel.favoriteAyahs.collectAsState()
    val tafsirBooks by viewModel.tafsirBooks.collectAsState()

    var selectedAyah by remember { mutableStateOf<AyahEntity?>(null) }
    var selectedTafsirEntries by remember { mutableStateOf<List<TafsirEntryEntity>>(emptyList()) }
    var showThemePicker by remember { mutableStateOf(false) }
    var isFullScreen by remember { mutableStateOf(false) }
    var isPlayingAyahAudio by remember { mutableStateOf(false) }
    var audioStatusText by remember { mutableStateOf<String?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

    val context = LocalContext.current
    val currentView = LocalView.current
    DisposableEffect(currentView) {
        currentView.keepScreenOn = true
        onDispose { currentView.keepScreenOn = false }
    }
    val clipboard = LocalClipboardManager.current

    val palette = readingThemeByKey(settings.readingThemeKey)

    DisposableEffect(context) {
        onDispose {
            runCatching { mediaPlayer?.stop(); mediaPlayer?.release() }
            runCatching { tts?.stop(); tts?.shutdown() }
        }
    }

    fun speakArabicMeaning(text: String) {
        val existing = tts
        if (existing != null) {
            existing.speak(text, TextToSpeech.QUEUE_FLUSH, null, "word_meaning")
            return
        }
        var created: TextToSpeech? = null
        created = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                created?.language = Locale("ar")
                created?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "word_meaning")
            }
        }
        tts = created
    }

    fun stopAllAudio() {
        runCatching { mediaPlayer?.stop(); mediaPlayer?.release() }
        mediaPlayer = null
        isPlayingAyahAudio = false
        audioStatusText = null
        runCatching { tts?.stop() }
    }

    fun playAyahPronunciation(surahId: Int, ayahNumber: Int) {
        stopAllAudio()
        val s3 = "%03d".format(surahId)
        val a3 = "%03d".format(ayahNumber)
        val url = "https://everyayah.com/data/Husary_128kbps/$s3$a3.mp3"
        audioStatusText = "جاري تشغيل النطق المجوّد (الشيخ الحصري)..."
        val mp = MediaPlayer()
        runCatching {
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            mp.setDataSource(context, Uri.parse(url), mapOf("User-Agent" to "Mozilla/5.0 (Android)"))
            mp.setOnPreparedListener {
                it.start()
                isPlayingAyahAudio = true
                audioStatusText = "يُقرأ الآن بصوت الشيخ الحصري (المعلّم)"
            }
            mp.setOnCompletionListener {
                isPlayingAyahAudio = false
                audioStatusText = null
            }
            mp.setOnErrorListener { _, _, _ ->
                isPlayingAyahAudio = false
                audioStatusText = "تحتاج اتصال إنترنت لسماع التلاوة المجوّدة للآية"
                true
            }
            mp.prepareAsync()
            mediaPlayer = mp
        }.onFailure {
            isPlayingAyahAudio = false
            audioStatusText = "تعذّر تشغيل الصوت"
        }
    }

    LaunchedEffect(currentPage) {
        listState.scrollToItem(0)
        viewModel.prefetchAdjacentPages(currentPage)
    }

    LaunchedEffect(currentPage, ayahs) {
        ayahs.firstOrNull()?.let { first ->
            viewModel.saveLastRead(first.surahId, first.ayahNumber, currentPage)
        }
    }

    LaunchedEffect(selectedAyah) {
        val a = selectedAyah
        selectedTafsirEntries = if (a != null) {
            viewModel.getTafsirForAyah(a.surahId, a.ayahNumber)
        } else {
            emptyList()
        }
    }

    BackHandler(enabled = selectedAyah != null || showThemePicker || isFullScreen) {
        when {
            showThemePicker -> showThemePicker = false
            selectedAyah != null -> selectedAyah = null
            isFullScreen -> isFullScreen = false
        }
    }

    val firstAyah = ayahs.firstOrNull()
    val headerSurah = surahs.firstOrNull { it.id == (firstAyah?.surahId ?: 1) }

    Scaffold(
        containerColor = palette.background,
        topBar = {
            if (!isFullScreen) {
            DhikriCompactTopBar(
                title = headerSurah?.let { "سورة ${it.nameArabic}" } ?: "المصحف الشريف",
                subtitle = "صفحة $currentPage / 604 • الجزء ${firstAyah?.juzNumber ?: 1} • الحزب ${firstAyah?.hizbNumber ?: 1}",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { viewModel.setMushafFontSize(settings.mushafFontSizeSp - 2f) }
            ) {
                        Icon(Icons.Default.TextDecrease, contentDescription = "تصغير الخط")
                    }
                    IconButton(onClick = { viewModel.setMushafFontSize(settings.mushafFontSizeSp + 2f) }) {
                        Icon(Icons.Default.TextIncrease, contentDescription = "تكبير الخط")
                    }
                    IconButton(onClick = { showThemePicker = true }) {
                        Icon(Icons.Default.Palette, contentDescription = "ألوان القراءة لراحة العين")
                    }
                }
            )
        },
        bottomBar = {
            Surface(color = palette.surface, tonalElevation = 3.dp) {
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

                    Text("$currentPage / 604", fontWeight = FontWeight.Bold, color = palette.textColor)

                    Button(
                        onClick = { if (currentPage < 604) currentPage++ },
                        enabled = currentPage < 604
                    ) { Text("الصفحة التالية") }
                }
            }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
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
                        colors = CardDefaults.cardColors(containerColor = palette.surface),
                        border = BorderStroke(1.dp, palette.accentColor.copy(alpha = 0.45f)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("سورة ${surah.nameArabic}", style = MaterialTheme.typography.titleLarge, color = palette.accentColor)
                            Text(
                                "${surah.revelationType} • ${surah.ayahCount} آية",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.textColor.copy(alpha = 0.75f)
                            )
                        }
                    }
                    if (ayah.surahId != 1 && ayah.surahId != 9) {
                        Text(
                            text = "بِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ",
                            fontFamily = HafsFontFamily,
                            fontSize = (settings.mushafFontSizeSp - 2f).sp,
                            color = palette.accentColor,
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
                        color = palette.textColor,
                        textAlign = TextAlign.Justify,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (ayah.isSajdah == 1 || isFav) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (ayah.isSajdah == 1) {
                                Text(
                                    "موضع سجدة (${ayah.sajdahType ?: "مستحبة"})",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.accentColor
                                )
                            }
                            if (isFav) {
                                Text("★ في المفضلة", style = MaterialTheme.typography.labelMedium, color = palette.accentColor)
                            }
                        }
                    }
                }
                HorizontalDivider(color = palette.dividerColor.copy(alpha = 0.55f))
            }
        }
    }

    // Eye-Comfort Reading Theme Picker Sheet
    if (showThemePicker) {
        ModalBottomSheet(onDismissRequest = { showThemePicker = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "اختر لون القراءة لراحة العين",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                ReadingThemes.forEach { theme ->
                    val isSelected = theme.key == palette.key
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setReadingTheme(theme.key)
                                showThemePicker = false
                            },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = theme.background),
                        border = BorderStroke(
                            width = if (isSelected) 2.5.dp else 1.dp,
                            color = if (isSelected) theme.accentColor else theme.dividerColor
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(theme.surface)
                                        .border(2.dp, theme.accentColor, CircleShape)
                                )
                                Column {
                                    Text(theme.labelArabic, color = theme.textColor, fontWeight = FontWeight.Bold)
                                    Text(
                                        "بِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ",
                                        fontFamily = HafsFontFamily,
                                        color = theme.textColor.copy(alpha = 0.85f),
                                        fontSize = 15.sp
                                    )
                                }
                            }
                            if (isSelected) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "مُختار", tint = theme.accentColor)
                            }
                        }
                    }
                }
            }
        }
    }

    // Ayah Actions + Tafsir & Word Meanings Sheet
    selectedAyah?.let { ayah ->
        val surahName = surahs.firstOrNull { it.id == ayah.surahId }?.nameArabic ?: "${ayah.surahId}"
        val isFav = favoriteAyahs.any { it.ayahId == ayah.id }
        ModalBottomSheet(onDismissRequest = { stopAllAudio(); selectedAyah = null }) {
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = {
                            if (isPlayingAyahAudio) stopAllAudio()
                            else playAyahPronunciation(ayah.surahId, ayah.ayahNumber)
                        },
                        label = { Text(if (isPlayingAyahAudio) "إيقاف النطق" else "اسمع النطق") },
                        leadingIcon = {
                            Icon(
                                if (isPlayingAyahAudio) Icons.Default.StopCircle else Icons.Default.VolumeUp,
                                contentDescription = null
                            )
                        }
                    )
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
                            val shareText = "﴿${ayah.textHafsV20}﴾\n[سورة $surahName — الآية ${ayah.ayahNumber}]\nعبر تطبيق ذِكري"
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
                audioStatusText?.let { status ->
                    Text(status, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
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
                                    if (entry.textContent.isBlank()) {
                                        Text(
                                            "لا توجد مفردات غريبة مشروحة لهذه الآية في هذا الكتاب.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        Text(entry.textContent, style = MaterialTheme.typography.bodyLarge)
                                        if (entry.bookKey == "arabic_seraj") {
                                            TextButton(
                                                onClick = {
                                                    speakArabicMeaning(entry.textContent)
                                                }
                                            ) {
                                                Icon(Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text("استماع لشرح الكلمات الصعبة (أوفلاين)")
                                            }
                                        }
                                    }
                                    entry.footnotes?.takeIf { it.isNotBlank() }?.let { notes ->
                                        Text(
                                            notes,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
