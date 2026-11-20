package com.dhikri.islamic.ui.screens

import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.dhikri.islamic.ui.components.DhikriCompactTopBar
import com.dhikri.islamic.ui.theme.EmeraldDark
import com.dhikri.islamic.ui.theme.HafsFontFamily
import com.dhikri.islamic.ui.theme.ReadingThemes
import com.dhikri.islamic.ui.theme.SoftGold
import com.dhikri.islamic.ui.theme.readingThemeByKey
import com.dhikri.islamic.ui.viewmodel.DhikriViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

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

    var selectedAyah by remember { mutableStateOf<AyahEntity?>(null) }
    var selectedTafsirEntries by remember { mutableStateOf<List<TafsirEntryEntity>>(emptyList()) }
    var showThemePicker by remember { mutableStateOf(false) }
    var isFullScreen by remember { mutableStateOf(false) }
    var isPlayingAyahAudio by remember { mutableStateOf(false) }
    var audioStatusText by remember { mutableStateOf<String?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val currentView = LocalView.current
    val palette = readingThemeByKey(settings.readingThemeKey)

    DisposableEffect(currentView) {
        currentView.keepScreenOn = true
        onDispose { currentView.keepScreenOn = false }
    }

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

    // Fast cached single-ayah pronunciation (64kbps -> ~35KB, downloads in ~0.3s and plays from local cache!)
    fun playAyahPronunciation(surahId: Int, ayahNumber: Int, globalAyahId: Int) {
        stopAllAudio()
        val s3 = "%03d".format(surahId)
        val a3 = "%03d".format(ayahNumber)
        audioStatusText = "جاري تجهيز النطق المجوّد..."

        scope.launch {
            val cacheDir = File(context.cacheDir, "ayah_audio").apply { mkdirs() }
            val localFile = File(cacheDir, "${s3}_${a3}.mp3")

            val ready = if (localFile.exists() && localFile.length() > 1500L) {
                true
            } else {
                withContext(Dispatchers.IO) {
                    val candidates = listOf(
                        "https://verses.quran.foundation/Husary/mp3/$s3$a3.mp3",
                        "https://cdn.islamic.network/quran/audio/128/ar.husary/$globalAyahId.mp3",
                        "https://cdn.islamic.network/quran/audio/64/ar.husary/$globalAyahId.mp3",
                        "https://everyayah.com/data/Husary_64kbps/$s3$a3.mp3"
                    )
                    var success = false
                    for (urlStr in candidates) {
                        val downloaded = runCatching {
                            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                                connectTimeout = 7_000
                                readTimeout = 9_000
                                instanceFollowRedirects = true
                                setRequestProperty("User-Agent", "Mozilla/5.0 (Android)")
                            }
                            if (conn.responseCode == 200) {
                                conn.inputStream.use { inp ->
                                    localFile.outputStream().use { out -> inp.copyTo(out) }
                                }
                                conn.disconnect()
                                localFile.length() > 1500L
                            } else {
                                conn.disconnect()
                                false
                            }
                        }.getOrDefault(false)
                        if (downloaded) {
                            success = true
                            break
                        }
                    }
                    success
                }
            }

            if (!ready) {
                isPlayingAyahAudio = false
                audioStatusText = "تحتاج اتصال إنترنت سريع لسماع نطق الآية"
                return@launch
            }

            runCatching {
                val mp = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    setDataSource(localFile.absolutePath)
                    prepare()
                    setOnCompletionListener {
                        isPlayingAyahAudio = false
                        audioStatusText = null
                    }
                    start()
                }
                mediaPlayer = mp
                isPlayingAyahAudio = true
                audioStatusText = "يُقرأ الآن بصوت الشيخ الحصري"
            }.onFailure {
                isPlayingAyahAudio = false
                audioStatusText = "تعذّر تشغيل الصوت"
            }
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
            selectedAyah != null -> {
                stopAllAudio()
                selectedAyah = null
            }
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
                        IconButton(onClick = { viewModel.setMushafFontSize(settings.mushafFontSizeSp - 2f) }) {
                            Icon(Icons.Default.TextDecrease, contentDescription = "تصغير الخط")
                        }
                        IconButton(onClick = { viewModel.setMushafFontSize(settings.mushafFontSizeSp + 2f) }) {
                            Icon(Icons.Default.TextIncrease, contentDescription = "تكبير الخط")
                        }
                        IconButton(onClick = { showThemePicker = true }) {
                            Icon(Icons.Default.Palette, contentDescription = "ألوان القراءة لراحة العين")
                        }
                        IconButton(onClick = { isFullScreen = true }) {
                            Icon(Icons.Default.Fullscreen, contentDescription = "ملء الشاشة")
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (!isFullScreen) {
                Surface(color = palette.surface, tonalElevation = 3.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
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
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
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
                            lineHeight = (settings.mushafFontSizeSp * 1.9f).sp,
                            color = palette.textColor,
                            textAlign = TextAlign.Justify,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (isFav || ayah.isSajdah == 1) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (ayah.isSajdah == 1) {
                                    Text("۩ موضع سجدة", style = MaterialTheme.typography.labelMedium, color = palette.accentColor)
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

    if (showThemePicker) {
        ModalBottomSheet(onDismissRequest = { showThemePicker = false }) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("اختر لون القراءة لراحة العين", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                ReadingThemes.forEach { theme ->
                    val isSelected = theme.key == palette.key
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable {
                            viewModel.setReadingTheme(theme.key)
                            showThemePicker = false
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = theme.background),
                        border = BorderStroke(if (isSelected) 2.5.dp else 1.dp, if (isSelected) theme.accentColor else theme.dividerColor)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(Modifier.size(22.dp).clip(CircleShape).background(theme.accentColor))
                                Text(theme.labelArabic, color = theme.textColor, fontWeight = FontWeight.Bold)
                            }
                            if (isSelected) Icon(Icons.Default.CheckCircle, contentDescription = null, tint = theme.accentColor)
                        }
                    }
                }
            }
        }
    }

    selectedAyah?.let { ayah ->
        val surahName = surahs.firstOrNull { it.id == ayah.surahId }?.nameArabic ?: ""
        val isFav = favoriteAyahs.any { it.ayahId == ayah.id }

        ModalBottomSheet(
            onDismissRequest = {
                stopAllAudio()
                selectedAyah = null
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("سورة $surahName — آية ${ayah.ayahNumber}", style = MaterialTheme.typography.titleLarge, color = SoftGold)
                Text(ayah.textHafsV20, fontFamily = HafsFontFamily, fontSize = 22.sp, lineHeight = 38.sp)

                // Fast Play Pronunciation Button
                Button(
                    onClick = {
                        if (isPlayingAyahAudio) stopAllAudio()
                        else playAyahPronunciation(ayah.surahId, ayah.ayahNumber, ayah.id)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark, contentColor = SoftGold)
                ) {
                    Icon(if (isPlayingAyahAudio) Icons.Default.StopCircle else Icons.Default.VolumeUp, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isPlayingAyahAudio) "إيقاف النطق" else "اسمع النطق المجوّد للآية", fontWeight = FontWeight.Bold)
                }

                audioStatusText?.let { msg ->
                    Text(msg, fontSize = 12.sp, color = SoftGold, fontWeight = FontWeight.SemiBold)
                }

                // Sleek Icon + 1-Line Label Quick Actions Row (zero broken Arabic text)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AyahActionChip(
                        icon = Icons.Default.BookmarkAdd,
                        label = "علامة",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.addBookmark(ayah.surahId, ayah.ayahNumber, currentPage, "سورة $surahName (آية ${ayah.ayahNumber})") }
                    )
                    AyahActionChip(
                        icon = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        label = if (isFav) "محفوظة" else "مفضلة",
                        highlighted = isFav,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.toggleFavoriteAyah(ayah, isFav) }
                    )
                    AyahActionChip(
                        icon = Icons.Default.ContentCopy,
                        label = "نسخ",
                        modifier = Modifier.weight(1f),
                        onClick = { clipboard.setText(AnnotatedString("${ayah.textHafsV20} [سورة $surahName: ${ayah.ayahNumber}]")) }
                    )
                    AyahActionChip(
                        icon = Icons.Default.Share,
                        label = "مشاركة",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val share = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "﴿${ayah.textHafsV20}﴾ [سورة $surahName: ${ayah.ayahNumber}]\nعبر تطبيق ذِكري")
                            }
                            context.startActivity(Intent.createChooser(share, "مشاركة الآية"))
                        }
                    )
                }

                HorizontalDivider()

                if (selectedTafsirEntries.isEmpty()) {
                    Text("التفسير غير محمّل لهذه الآية. يمكنك تنزيله من مركز التنزيلات.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(onClick = {
                        stopAllAudio()
                        selectedAyah = null
                        onOpenDownloads()
                    }) { Text("فتح مركز التنزيلات") }
                } else {
                    selectedTafsirEntries.forEach { entry ->
                        val label = when (entry.bookKey) {
                            "arabic_moyassar" -> "التفسير الميسر"
                            "arabic_seraj" -> "معاني الكلمات (السراج)"
                            "arabic_mokhtasar" -> "المختصر في التفسير"
                            else -> entry.bookKey
                        }
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(0.8.dp, SoftGold.copy(alpha = 0.3f))
                        ) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(label, fontWeight = FontWeight.Bold, color = SoftGold)
                                    IconButton(onClick = { speakArabicMeaning(entry.textContent) }, modifier = Modifier.size(30.dp)) {
                                        Icon(Icons.Default.RecordVoiceOver, contentDescription = "نطق الشرح", tint = SoftGold, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Text(entry.textContent, lineHeight = 24.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AyahActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    highlighted: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (highlighted) SoftGold.copy(alpha = 0.22f) else EmeraldDark.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, if (highlighted) SoftGold else SoftGold.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = label, tint = SoftGold, modifier = Modifier.size(20.dp))
            Text(
                text = label,
                color = Color.White,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
