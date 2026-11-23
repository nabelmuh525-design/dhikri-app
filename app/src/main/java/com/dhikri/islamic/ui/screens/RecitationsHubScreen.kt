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
import com.dhikri.islamic.data.podcast.PlaybackQueueItem
import com.dhikri.islamic.data.podcast.PodcastPlaybackService
import com.dhikri.islamic.ui.components.DhikriCompactTopBar
import com.dhikri.islamic.ui.theme.EmeraldDark
import com.dhikri.islamic.ui.theme.SoftGold
import com.dhikri.islamic.ui.viewmodel.DhikriViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

private data class QuranReciterCard(
    val id: String,
    val nameArabic: String,
    val styleArabic: String,
    val requiredArabicTokens: List<String>,
    val quranicAudioPathHint: String,
    val quranicAudioDirectUrl: String,
    val mp3QuranFallbackUrl: String,
    val gradient: List<Color>
)

private val recitersCatalog = listOf(
    QuranReciterCard(
        id = "ayyub",
        nameArabic = "محمد أيوب",
        styleArabic = "مرتّل — الحرم النبوي",
        requiredArabicTokens = listOf("محمد", "ايوب"),
        quranicAudioPathHint = "muhammad_ayyoob",
        quranicAudioDirectUrl = "https://download.quranicaudio.com/quran/muhammad_ayyoob/",
        mp3QuranFallbackUrl = "https://server8.mp3quran.net/ayyub/",
        gradient = listOf(Color(0xFF1A437A), Color(0xFF0B1F3B))
    ),
    QuranReciterCard(
        id = "ajm",
        nameArabic = "أحمد بن علي العجمي",
        styleArabic = "مرتّل — تلاوة خاشعة",
        requiredArabicTokens = listOf("احمد", "العجمي"),
        quranicAudioPathHint = "ahmed_ibn_3ali_al-3ajamy",
        quranicAudioDirectUrl = "https://download.quranicaudio.com/quran/ahmed_ibn_3ali_al-3ajamy/",
        mp3QuranFallbackUrl = "https://server10.mp3quran.net/ajm/",
        gradient = listOf(Color(0xFF2E6B4F), Color(0xFF133B28))
    ),
    QuranReciterCard(
        id = "basit",
        nameArabic = "عبد الباسط عبد الصمد",
        styleArabic = "المصحف المرتّل",
        requiredArabicTokens = listOf("عبد", "الباسط"),
        quranicAudioPathHint = "abdul_basit_murattal",
        quranicAudioDirectUrl = "https://download.quranicaudio.com/quran/abdul_basit_murattal/",
        mp3QuranFallbackUrl = "https://server7.mp3quran.net/basit/",
        gradient = listOf(Color(0xFF8C6B1F), Color(0xFF4A360B))
    ),
    QuranReciterCard(
        id = "husr",
        nameArabic = "محمود خليل الحصري",
        styleArabic = "المصحف المرتّل",
        requiredArabicTokens = listOf("محمود", "الحصري"),
        quranicAudioPathHint = "mahmood_khaleel_al-husaree",
        quranicAudioDirectUrl = "https://download.quranicaudio.com/quran/mahmood_khaleel_al-husaree/",
        mp3QuranFallbackUrl = "https://server13.mp3quran.net/husr/",
        gradient = listOf(Color(0xFF1F6A8A), Color(0xFF0E3E56))
    ),
    QuranReciterCard(
        id = "minsh",
        nameArabic = "محمد صديق المنشاوي",
        styleArabic = "المصحف المرتّل",
        requiredArabicTokens = listOf("محمد", "المنشاوي"),
        quranicAudioPathHint = "muhammad_siddeeq_al-minshaawee",
        quranicAudioDirectUrl = "https://download.quranicaudio.com/quran/muhammad_siddeeq_al-minshaawee/",
        mp3QuranFallbackUrl = "https://server10.mp3quran.net/minsh/",
        gradient = listOf(Color(0xFF564494), Color(0xFF2B2054))
    ),
    QuranReciterCard(
        id = "maher",
        nameArabic = "ماهر المعيقلي",
        styleArabic = "إمام الحرم المكي — مرتّل",
        requiredArabicTokens = listOf("ماهر", "المعيقلي"),
        quranicAudioPathHint = "maher_256",
        quranicAudioDirectUrl = "https://download.quranicaudio.com/quran/maher_256/",
        mp3QuranFallbackUrl = "https://server12.mp3quran.net/maher/",
        gradient = listOf(Color(0xFF215B63), Color(0xFF0D2E33))
    ),
    QuranReciterCard(
        id = "yasser",
        nameArabic = "ياسر الدوسري",
        styleArabic = "إمام الحرم المكي — مرتّل",
        requiredArabicTokens = listOf("ياسر", "الدوسري"),
        quranicAudioPathHint = "yasser_ad-dussary",
        quranicAudioDirectUrl = "https://download.quranicaudio.com/quran/yasser_ad-dussary/",
        mp3QuranFallbackUrl = "https://server11.mp3quran.net/yasser/",
        gradient = listOf(Color(0xFF7B435B), Color(0xFF411D2D))
    ),
    QuranReciterCard(
        id = "s_gmd",
        nameArabic = "سعد الغامدي",
        styleArabic = "مرتّل — تلاوة هادئة",
        requiredArabicTokens = listOf("سعد", "الغامدي"),
        quranicAudioPathHint = "sa3d_al-ghaamidi",
        quranicAudioDirectUrl = "https://download.quranicaudio.com/quran/sa3d_al-ghaamidi/complete/",
        mp3QuranFallbackUrl = "https://server7.mp3quran.net/s_gmd/",
        gradient = listOf(Color(0xFF3E6947), Color(0xFF1B3621))
    ),
    QuranReciterCard(
        id = "shur",
        nameArabic = "سعود الشريم",
        styleArabic = "إمام الحرم المكي — مرتّل",
        requiredArabicTokens = listOf("سعود", "الشريم"),
        quranicAudioPathHint = "sa3ood_al-shuraym",
        quranicAudioDirectUrl = "https://download.quranicaudio.com/quran/sa3ood_al-shuraym/",
        mp3QuranFallbackUrl = "https://server7.mp3quran.net/shur/",
        gradient = listOf(Color(0xFF274C77), Color(0xFF102540))
    ),
    QuranReciterCard(
        id = "frs_a",
        nameArabic = "فارس عباد",
        styleArabic = "مرتّل — تلاوة نقية",
        requiredArabicTokens = listOf("فارس", "عباد"),
        quranicAudioPathHint = "fares",
        quranicAudioDirectUrl = "https://download.quranicaudio.com/quran/fares/",
        mp3QuranFallbackUrl = "https://server8.mp3quran.net/frs_a/",
        gradient = listOf(Color(0xFF7A5622), Color(0xFF3E290D))
    ),
    QuranReciterCard(
        id = "ali_jaber",
        nameArabic = "علي جابر",
        styleArabic = "إمام الحرم المكي — مرتّل",
        requiredArabicTokens = listOf("علي", "جابر"),
        quranicAudioPathHint = "abdullaah_alee_jaabir_studio",
        quranicAudioDirectUrl = "https://download.quranicaudio.com/quran/abdullaah_alee_jaabir_studio/",
        mp3QuranFallbackUrl = "https://server11.mp3quran.net/a_jbr/",
        gradient = listOf(Color(0xFF1B4965), Color(0xFF0D283A))
    )
)

private fun normalizeArabicForMatch(input: String): String {
    return input
        .replace(Regex("[\\u064B-\\u065F\\u0670\\u0640]"), "")
        .replace('أ', 'ا')
        .replace('إ', 'ا')
        .replace('آ', 'ا')
        .replace('ى', 'ي')
        .replace('ة', 'ه')
        .lowercase()
        .trim()
}

private fun isExcludedStyle(text: String): Boolean {
    val norm = normalizeArabicForMatch(text)
    val banned = listOf(
        "mujawwad", "mojawwad", "مجود",
        "muallim", "معلم",
        "children", "اطفال",
        "warsh", "ورش",
        "qaloon", "قالون",
        "doori", "الدوري",
        "soosi", "السوسي",
        "shubah", "شعبه",
        "translation", "ترجمه",
        "taraweeh", "تراويح"
    )
    return banned.any { norm.contains(it) }
}

private fun fetchQuranicAudioServers(): Map<String, String> {
    val result = mutableMapOf<String, String>()
    runCatching {
        val conn = (URL("https://quranicaudio.com/api/qaris").openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 8_000
            setRequestProperty("User-Agent", "DHIKRI-Android")
        }
        if (conn.responseCode == 200) {
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val arr = JSONArray(body)
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val sectionId = obj.optInt("section_id", 1)
                if (sectionId != 1) continue
                val relPath = obj.optString("relative_path").trim().trimStart('/')
                if (relPath.isBlank()) continue
                val enName = obj.optString("name")
                val arName = obj.optString("arabic_name")
                val combined = "$enName $arName $relPath"
                if (isExcludedStyle(combined)) continue

                val normAr = normalizeArabicForMatch(arName)
                val lowerPath = relPath.lowercase()
                val normalizedBase = "https://download.quranicaudio.com/quran/" +
                    if (relPath.endsWith("/")) relPath else "$relPath/"

                recitersCatalog.forEach { card ->
                    val pathMatches = lowerPath.startsWith(card.quranicAudioPathHint.lowercase())
                    val arabicMatches = normAr.isNotBlank() &&
                        card.requiredArabicTokens.all { tok -> normAr.contains(tok) }
                    if (pathMatches) {
                        result[card.id] = normalizedBase
                    } else if (arabicMatches && card.id !in result) {
                        result[card.id] = normalizedBase
                    }
                }
            }
        }
        conn.disconnect()
    }
    return result
}

private fun fetchMp3QuranMurattalServers(): Map<String, String> {
    val result = mutableMapOf<String, String>()
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
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val normName = normalizeArabicForMatch(obj.optString("name"))
                    val moshafs = obj.optJSONArray("moshaf") ?: continue
                    if (moshafs.length() == 0) continue

                    // Pick strictly Hafs an Asim - Murattal (complete 114 surahs, never Mujawwad/Muallim)
                    var bestServer: String? = null
                    var bestScore = -1
                    for (m in 0 until moshafs.length()) {
                        val mObj = moshafs.optJSONObject(m) ?: continue
                        val server = mObj.optString("server")
                        if (!server.startsWith("https://")) continue
                        val mName = normalizeArabicForMatch(mObj.optString("name"))
                        if (isExcludedStyle(mName)) continue
                        val surahTotal = mObj.optInt("surah_total", 0)
                        var score = 0
                        if (surahTotal >= 114) score += 10
                        if (mName.contains("حفص")) score += 5
                        if (mName.contains("مرتل")) score += 5
                        if (score > bestScore) {
                            bestScore = score
                            bestServer = if (server.endsWith("/")) server else "$server/"
                        }
                    }
                    val chosen = bestServer ?: continue

                    recitersCatalog.forEach { card ->
                        if (card.id !in result && card.requiredArabicTokens.all { tok -> normName.contains(tok) }) {
                            result[card.id] = chosen
                        }
                    }
                }
            }
        }
        conn.disconnect()
    }
    return result
}

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
    var quranicAudioServers by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var mp3QuranServers by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    val bgPlayback by PodcastPlaybackService.state.collectAsState()
    val playingSurahId: Int? = if (bgPlayback.showId.startsWith("quran_") && (bgPlayback.isPlaying || bgPlayback.isBuffering)) {
        bgPlayback.episodeId.substringAfterLast("_").toIntOrNull()
    } else null
    val playingSurahName: String? = if (playingSurahId != null) bgPlayback.episodeTitle else null
    var downloadingSurahId by remember { mutableStateOf<Int?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var refreshTick by remember { mutableIntStateOf(0) }
    var searchSurah by remember { mutableStateOf("") }

    // Resolve live official QuranicAudio.com (Quran.com) + strict MP3Quran Murattal fallbacks once
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val qa = fetchQuranicAudioServers()
            val mp3 = fetchMp3QuranMurattalServers()
            quranicAudioServers = qa
            mp3QuranServers = mp3
        }
    }

    fun stopPlayback() {
        PodcastPlaybackService.sendAction(context, PodcastPlaybackService.ACTION_STOP)
        statusMessage = null
    }

    // Keep Quran recitation playing smoothly in background & when screen is locked!
    BackHandler(enabled = selectedReciter != null) {
        selectedReciter = null
        searchSurah = ""
    }

    fun localAudioFile(reciter: QuranReciterCard, surahId: Int): File {
        val dir = File(context.filesDir, "recitations_v2/${reciter.id}").apply { mkdirs() }
        return File(dir, "%03d.mp3".format(surahId))
    }

    fun candidateUrls(reciter: QuranReciterCard, surahId: Int): List<String> {
        val fileName = "%03d.mp3".format(surahId)
        val bases = listOfNotNull(
            quranicAudioServers[reciter.id],
            reciter.quranicAudioDirectUrl,
            mp3QuranServers[reciter.id],
            reciter.mp3QuranFallbackUrl
        ).distinct()
        return bases.map { base ->
            val clean = if (base.endsWith("/")) base else "$base/"
            "$clean$fileName"
        }
    }

    fun resolveWorkingAudioUrl(reciter: QuranReciterCard, surahId: Int): String {
        val candidates = candidateUrls(reciter, surahId)
        for (candidate in candidates) {
            val finalUrl = runCatching {
                val c = (URL(candidate).openConnection() as HttpURLConnection).apply {
                    requestMethod = "HEAD"
                    instanceFollowRedirects = true
                    connectTimeout = 6_000
                    readTimeout = 6_000
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Android)")
                }
                val code = c.responseCode
                val resolved = c.url.toString()
                c.disconnect()
                if (code in 200..299) resolved else null
            }.getOrNull()
            if (finalUrl != null) return finalUrl
        }
        return candidates.first()
    }

    fun playSurah(reciter: QuranReciterCard, surahId: Int, surahName: String) {
        val trackId = "quran_${reciter.id}_$surahId"
        if (bgPlayback.episodeId == trackId && (bgPlayback.isPlaying || bgPlayback.isBuffering)) {
            stopPlayback()
            return
        }
        statusMessage = "جاري تشغيل سورة $surahName (تعمل بالخلفية وعند قفل الشاشة)..."

        scope.launch {
            val localFile = localAudioFile(reciter, surahId)
            val resolvedTarget: String = if (localFile.exists() && localFile.length() > 10_000L) {
                localFile.absolutePath
            } else {
                withContext(Dispatchers.IO) {
                    resolveWorkingAudioUrl(reciter, surahId)
                }
            }

            val resolvedBase = if (resolvedTarget.startsWith("http")) {
                resolvedTarget.substringBeforeLast("/") + "/"
            } else {
                (quranicAudioServers[reciter.id] ?: reciter.quranicAudioDirectUrl).let {
                    if (it.endsWith("/")) it else "$it/"
                }
            }
            val orderedSurahs = surahs.sortedBy { it.id }
            val startIdx = orderedSurahs.indexOfFirst { it.id == surahId }.coerceAtLeast(0)
            val queueItems = orderedSurahs.map { s ->
                val f = localAudioFile(reciter, s.id)
                val targetUrlOrPath = if (s.id == surahId) {
                    resolvedTarget
                } else if (f.exists() && f.length() > 10_000L) {
                    f.absolutePath
                } else {
                    "$resolvedBase%03d.mp3".format(s.id)
                }
                PlaybackQueueItem(
                    showId = "quran_${reciter.id}",
                    showTitle = "القرآن الكريم • ${reciter.nameArabic}",
                    trackId = "quran_${reciter.id}_${s.id}",
                    title = "سورة ${s.nameArabic}",
                    audioPathOrUrl = targetUrlOrPath,
                    saveProgress = false
                )
            }
            PodcastPlaybackService.playDirectQueue(context, queueItems, startIdx)
            statusMessage = "يُتلى الآن: سورة $surahName — ${reciter.nameArabic}"
        }
    }

    fun downloadSurah(reciter: QuranReciterCard, surahId: Int, surahName: String) {
        if (downloadingSurahId != null) return
        downloadingSurahId = surahId
        statusMessage = "جاري تحميل سورة $surahName للاستماع أوفلاين..."
        scope.launch(Dispatchers.IO) {
            val target = localAudioFile(reciter, surahId)
            val workingUrl = resolveWorkingAudioUrl(reciter, surahId)
            val ok = runCatching {
                val conn = (URL(workingUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15_000
                    readTimeout = 30_000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Android)")
                }
                if (conn.responseCode !in 200..299) {
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
        // STAGE 1: 2-Column Square Gradient Cards Grid for all 11 Reciters
        Scaffold(
            topBar = {
                DhikriCompactTopBar(
                    title = "الاستماع للقرآن الكريم",
                    subtitle = "اختر القارئ للاستماع أو التحميل أوفلاين (${recitersCatalog.size} قارئاً)",
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

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "تشغيل السورة التالية تلقائياً",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Switch(
                        checked = bgPlayback.autoPlayNext,
                        onCheckedChange = { PodcastPlaybackService.setAutoPlayNext(context, it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = EmeraldDark,
                            checkedTrackColor = SoftGold
                        )
                    )
                }

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
