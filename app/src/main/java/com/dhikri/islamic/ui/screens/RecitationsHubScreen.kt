package com.dhikri.islamic.ui.screens

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhikri.islamic.ui.components.DhikriCompactTopBar
import com.dhikri.islamic.ui.theme.EmeraldDark
import com.dhikri.islamic.ui.theme.SoftGold
import com.dhikri.islamic.ui.viewmodel.DhikriViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

private data class QuranReciterCard(
    val id: String,
    val nameArabic: String,
    val styleArabic: String,
    val searchKeyword: String,
    val fallbackBaseUrl: String,
    val gradient: List<Color>
)

private val recitersCatalog = listOf(
    QuranReciterCard("ayyub", "محمد أيوب", "مرتّل — الحرم النبوي", "محمد أيوب", "https://server8.mp3quran.net/ayyub/", listOf(Color(0xFF1A437A), Color(0xFF0B1F3B))),
    QuranReciterCard("ajm", "أحمد بن علي العجمي", "تلاوة خاشعة مؤثرة", "العجمي", "https://server10.mp3quran.net/ajm/", listOf(Color(0xFF2E6B4F), Color(0xFF133B28))),
    QuranReciterCard("basit", "عبد الباسط عبد الصمد", "مرتّل — صوت ذهبي", "عبد الباسط", "https://server7.mp3quran.net/basit/", listOf(Color(0xFF8C6B1F), Color(0xFF4A360B))),
    QuranReciterCard("husr", "محمود خليل الحصري", "المصحف المعلّم والمرتّل", "الحصري", "https://server13.mp3quran.net/husr/", listOf(Color(0xFF1F6A8A), Color(0xFF0E3E56))),
    QuranReciterCard("minsh", "محمد صديق المنشاوي", "تلاوة روحانية خالدة", "المنشاوي", "https://server10.mp3quran.net/minsh/", listOf(Color(0xFF564494), Color(0xFF2B2054))),
    QuranReciterCard("maher", "ماهر المعيقلي", "إمام الحرم المكي", "المعيقلي", "https://server12.mp3quran.net/maher/", listOf(Color(0xFF215B63), Color(0xFF0D2E33))),
    QuranReciterCard("yasser", "ياسر الدوسري", "إمام الحرم المكي", "الدوسري", "https://server11.mp3quran.net/yasser/", listOf(Color(0xFF7B435B), Color(0xFF411D2D))),
    QuranReciterCard("s_gmd", "سعد الغامدي", "تلاوة هادئة ومريحة", "الغامدي", "https://server7.mp3quran.net/s_gmd/", listOf(Color(0xFF3E6947), Color(0xFF1B3621))),
    QuranReciterCard("shur", "سعود الشريم", "إمام الحرم المكي", "الشريم", "https://server7.mp3quran.net/shur/", listOf(Color(0xFF274C77), Color(0xFF102540))),
    QuranReciterCard("frs_a", "فارس عباد", "تلاوة نقية خاشعة", "فارس عباد", "https://server8.mp3quran.net/frs_a/", listOf(Color(0xFF7A5622), Color(0xFF3E290D)))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecitationsHubScreen(
    viewModel: DhikriViewModel,
    onBack: () -> Unit
) {
    val surahs by viewModel.surahs.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedReciter by remember { mutableStateOf<QuranReciterCard?>(null) }
    var resolvedServers by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var playingSurahId by remember { mutableStateOf<Int?>(null) }
    var playingSurahName by remember { mutableStateOf<String?>(null) }
    var downloadingSurahId by remember { mutableStateOf<Int?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var refreshTick by remember { mutableIntStateOf(0) }
    var searchSurah by remember { mutableStateOf("") }

    // Fetch live MP3Quran server map once in background
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            runCatching {
                val conn = (URL("https://www.mp3quran.net/api/v3/reciters?language=ar").openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8_000
                    readTimeout = 8_000
                    setRequestProperty("User-Agent", "DHIKRI-Android")
                }
                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val arr = JSONObject(body).optJSONArray("reciters")
                    if (arr != null) {
                        val map = mutableMapOf<String, String>()
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            val rName = obj.optString("name")
                            val moshafs = obj.optJSONArray("moshaf") ?: continue
                            if (moshafs.length() == 0) continue
                            val server = moshafs.getJSONObject(0).optString("server")
                            if (!server.startsWith("https://")) continue
                            recitersCatalog.forEach { card ->
                                if (card.id !in map && rName.contains(card.searchKeyword)) {
                                    map[card.id] = if (server.endsWith("/")) server else "$server/"
                                }
                            }
                        }
                        resolvedServers = map
                    }
                }
                conn.disconnect()
            }
        }
    }

    fun stopPlayback() {
        runCatching { mediaPlayer?.stop(); mediaPlayer?.release() }
        mediaPlayer = null
        playingSurahId = null
        playingSurahName = null
        statusMessage = null
    }

    DisposableEffect(Unit) {
        onDispose { stopPlayback() }
    }

    BackHandler(enabled = selectedReciter != null) {
        stopPlayback()
        selectedReciter = null
        searchSurah = ""
    }

    fun localAudioFile(reciter: QuranReciterCard, surahId: Int): File {
        val dir = File(context.filesDir, "recitations/${reciter.id}").apply { mkdirs() }
        return File(dir, "%03d.mp3".format(surahId))
    }

    fun surahUrl(reciter: QuranReciterCard, surahId: Int): String {
        val base = resolvedServers[reciter.id] ?: reciter.fallbackBaseUrl
        return "$base%03d.mp3".format(surahId)
    }

    fun playSurah(reciter: QuranReciterCard, surahId: Int, surahName: String) {
        if (playingSurahId == surahId) {
            stopPlayback()
            return
        }
        stopPlayback()
        playingSurahId = surahId
        playingSurahName = "سورة $surahName"
        statusMessage = "جاري تشغيل سورة $surahName..."

        scope.launch {
            val localFile = localAudioFile(reciter, surahId)
            val resolvedTarget: String = if (localFile.exists() && localFile.length() > 10_000L) {
                localFile.absolutePath
            } else {
                withContext(Dispatchers.IO) {
                    val primary = surahUrl(reciter, surahId)
                    runCatching {
                        val c = (URL(primary).openConnection() as HttpURLConnection).apply {
                            requestMethod = "HEAD"
                            instanceFollowRedirects = true
                            connectTimeout = 7_000
                            readTimeout = 7_000
                            setRequestProperty("User-Agent", "Mozilla/5.0 (Android)")
                        }
                        c.connect()
                        val finalUrl = c.url.toString()
                        c.disconnect()
                        finalUrl
                    }.getOrDefault(primary)
                }
            }

            runCatching {
                val mp = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    if (resolvedTarget.startsWith("http")) {
                        setDataSource(context, Uri.parse(resolvedTarget), mapOf("User-Agent" to "Mozilla/5.0 (Android)"))
                    } else {
                        setDataSource(resolvedTarget)
                    }
                    setOnPreparedListener {
                        it.start()
                        statusMessage = "يُتلى الآن: سورة $surahName — ${reciter.nameArabic}"
                    }
                    setOnCompletionListener {
                        playingSurahId = null
                        playingSurahName = null
                        statusMessage = null
                    }
                    setOnErrorListener { _, _, _ ->
                        playingSurahId = null
                        statusMessage = "تعذّر تشغيل التلاوة — تأكد من الاتصال بالإنترنت"
                        true
                    }
                    prepareAsync()
                }
                mediaPlayer = mp
            }.onFailure {
                playingSurahId = null
                statusMessage = "تعذّر بدء التشغيل"
            }
        }
    }

    fun downloadSurah(reciter: QuranReciterCard, surahId: Int, surahName: String) {
        if (downloadingSurahId != null) return
        downloadingSurahId = surahId
        statusMessage = "جاري تحميل سورة $surahName للاستماع أوفلاين..."
        scope.launch(Dispatchers.IO) {
            val target = localAudioFile(reciter, surahId)
            val ok = runCatching {
                val conn = (URL(surahUrl(reciter, surahId)).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15_000
                    readTimeout = 30_000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Android)")
                }
                if (conn.responseCode != 200) {
                    conn.disconnect()
                    return@runCatching false
                }
                conn.inputStream.use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                }
                conn.disconnect()
                target.length() > 10_000L
            }.getOrDefault(false)

            withContext(Dispatchers.Main) {
                downloadingSurahId = null
                refreshTick++
                statusMessage = if (ok) "تم تحميل سورة $surahName بنجاح (تعمل أوفلاين)" else "فشل التحميل — تأكد من الإنترنت"
            }
        }
    }

    val currentReciter = selectedReciter
    if (currentReciter == null) {
        // STAGE 1: 2-Column Square Gradient Cards Grid for all 10 Reciters (matching HomeScreen cards!)
        Scaffold(
            topBar = {
                DhikriCompactTopBar(
                    title = "الاستماع للقرآن الكريم",
                    subtitle = "اختر القارئ للاستماع أو التحميل أوفلاين (10 قراء)",
                    onBack = onBack
                )
            }
        ) { padding ->
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(recitersCatalog, key = { it.id }) { reciter ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.08f)
                            .shadow(5.dp, RoundedCornerShape(22.dp))
                            .clickable { selectedReciter = reciter },
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(1.2.dp, SoftGold.copy(alpha = 0.6f)),
                        color = Color.Transparent
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.verticalGradient(reciter.gradient))
                                .padding(14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(SoftGold.copy(alpha = 0.22f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.RecordVoiceOver,
                                        contentDescription = reciter.nameArabic,
                                        tint = SoftGold,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = reciter.nameArabic,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = reciter.styleArabic,
                                    color = Color.White.copy(alpha = 0.82f),
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    } else {
        // STAGE 2: Dedicated Surah List for the Selected Reciter
        val filteredSurahs = remember(surahs, searchSurah) {
            if (searchSurah.isBlank()) surahs
            else surahs.filter { it.nameArabic.contains(searchSurah.trim()) }
        }

        Scaffold(
            topBar = {
                DhikriCompactTopBar(
                    title = currentReciter.nameArabic,
                    subtitle = currentReciter.styleArabic,
                    onBack = {
                        stopPlayback()
                        selectedReciter = null
                        searchSurah = ""
                    },
                    actions = {
                        if (playingSurahId != null) {
                            IconButton(onClick = { stopPlayback() }) {
                                Icon(Icons.Default.StopCircle, contentDescription = "إيقاف", tint = SoftGold)
                            }
                        }
                    }
                )
            },
            bottomBar = {
                if (statusMessage != null) {
                    Surface(
                        color = EmeraldDark,
                        border = BorderStroke(1.dp, SoftGold.copy(alpha = 0.6f)),
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = SoftGold)
                                Text(
                                    text = statusMessage ?: "",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (playingSurahId != null) {
                                IconButton(onClick = { stopPlayback() }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "إيقاف", tint = SoftGold)
                                }
                            }
                        }
                    }
                }
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                OutlinedTextField(
                    value = searchSurah,
                    onValueChange = { searchSurah = it },
                    placeholder = { Text("ابحث عن سورة...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredSurahs, key = { "${it.id}_$refreshTick" }) { surah ->
                        val isPlaying = playingSurahId == surah.id
                        val isDownloaded = localAudioFile(currentReciter, surah.id).let { it.exists() && it.length() > 10_000L }
                        val isDownloading = downloadingSurahId == surah.id

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { playSurah(currentReciter, surah.id, surah.nameArabic) },
                            shape = RoundedCornerShape(18.dp),
                            color = if (isPlaying) EmeraldDark else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                if (isPlaying) 1.5.dp else 0.8.dp,
                                if (isPlaying) SoftGold else SoftGold.copy(alpha = 0.35f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(if (isPlaying) SoftGold else EmeraldDark),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${surah.id}",
                                        color = if (isPlaying) Color(0xFF1F1604) else SoftGold,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }

                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = "سورة ${surah.nameArabic}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.5.sp,
                                        color = if (isPlaying) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = when {
                                            isPlaying -> "جاري التشغيل الآن..."
                                            isDownloaded -> "محمّلة — تعمل دون إنترنت"
                                            else -> "${surah.ayahCount} آية • اضغط للاستماع"
                                        },
                                        fontSize = 12.sp,
                                        color = if (isPlaying) SoftGold else if (isDownloaded) Color(0xFF4ADE80) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                IconButton(onClick = { playSurah(currentReciter, surah.id, surah.nameArabic) }) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.StopCircle else Icons.Default.PlayCircle,
                                        contentDescription = if (isPlaying) "إيقاف" else "تشغيل",
                                        tint = SoftGold,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }

                                if (isDownloading) {
                                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = SoftGold)
                                } else {
                                    IconButton(onClick = { downloadSurah(currentReciter, surah.id, surah.nameArabic) }) {
                                        Icon(
                                            imageVector = if (isDownloaded) Icons.Default.CheckCircle else Icons.Default.Download,
                                            contentDescription = "تحميل أوفلاين",
                                            tint = if (isDownloaded) Color(0xFF4ADE80) else MaterialTheme.colorScheme.onSurfaceVariant
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
