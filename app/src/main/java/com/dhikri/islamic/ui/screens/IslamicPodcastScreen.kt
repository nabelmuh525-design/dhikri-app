package com.dhikri.islamic.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhikri.islamic.data.podcast.IslamicPodcastRepository
import com.dhikri.islamic.data.podcast.PodcastCategory
import com.dhikri.islamic.data.podcast.PodcastEpisode
import com.dhikri.islamic.data.podcast.PodcastPlaybackService
import com.dhikri.islamic.data.podcast.PodcastShow
import com.dhikri.islamic.data.podcast.PodcastStorage
import com.dhikri.islamic.ui.components.DhikriCompactTopBar
import com.dhikri.islamic.ui.theme.EmeraldDark
import com.dhikri.islamic.ui.theme.SoftGold
import kotlinx.coroutines.launch

@Composable
fun IslamicPodcastScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val playback by PodcastPlaybackService.state.collectAsState()

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val cardBg = if (isDark) Color(0xFF132642) else Color.White
    val primaryText = if (isDark) Color.White else EmeraldDark
    val secondaryText = if (isDark) Color.White.copy(alpha = 0.72f) else EmeraldDark.copy(alpha = 0.72f)

    var selectedCategory by remember { mutableStateOf(PodcastCategory.ALL) }
    var selectedShow by remember { mutableStateOf<PodcastShow?>(null) }
    var episodes by remember { mutableStateOf<List<PodcastEpisode>>(emptyList()) }
    var isLoadingEpisodes by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var episodeQuery by remember { mutableStateOf("") }
    var offlineOnlyFilter by remember { mutableStateOf(false) }
    var newestFirst by remember { mutableStateOf(false) }
    val episodeListState = rememberLazyListState()

    BackHandler(enabled = selectedShow != null) {
        selectedShow = null
    }

    // Download progress map (episodeId -> 0..100) and refresh trigger for downloaded status
    val downloadProgress = remember { mutableStateMapOf<String, Int>() }
    var storageVersion by remember { mutableIntStateOf(0) }

    fun loadShowEpisodes(show: PodcastShow, forceRefresh: Boolean = false) {
        selectedShow = show
        episodeQuery = ""
        offlineOnlyFilter = false
        newestFirst = false
        loadError = null
        isLoadingEpisodes = true
        scope.launch {
            IslamicPodcastRepository.fetchEpisodes(show, forceRefresh)
                .onSuccess { list ->
                    episodes = list
                    isLoadingEpisodes = false
                }
                .onFailure {
                    // Graceful offline fallback: show any downloaded episodes for this show
                    val offlineList = PodcastStorage.getDownloadedEpisodesForShow(context, show.id)
                    episodes = offlineList
                    loadError = if (offlineList.isEmpty()) {
                        "تعذر جلب الحلقات الآن — تأكد من الاتصال بالإنترنت وحاول مرة أخرى."
                    } else null
                    isLoadingEpisodes = false
                }
        }
    }

    // Auto-open playing podcast show and scroll directly to the active episode when tapped from Home mini-player
    LaunchedEffect(Unit) {
        if (selectedShow == null && playback.episodeId.isNotBlank() && !playback.showId.startsWith("quran_")) {
            val activeShow = IslamicPodcastRepository.shows.firstOrNull { it.id == playback.showId }
            if (activeShow != null) {
                loadShowEpisodes(activeShow)
            }
        }
    }

    LaunchedEffect(episodes, playback.episodeId) {
        if (selectedShow != null && playback.episodeId.isNotBlank() && episodes.isNotEmpty()) {
            val idx = episodes.indexOfFirst { it.id == playback.episodeId }
            if (idx >= 0) {
                runCatching { episodeListState.scrollToItem(idx) }
            }
        }
    }

    val filteredShows = remember(selectedCategory) {
        if (selectedCategory == PodcastCategory.ALL) {
            IslamicPodcastRepository.shows
        } else {
            IslamicPodcastRepository.shows.filter { it.category == selectedCategory }
        }
    }

    val filteredEpisodes = remember(episodes, episodeQuery, offlineOnlyFilter, newestFirst, storageVersion) {
        val q = episodeQuery.trim()
        val ordered = if (newestFirst) episodes.reversed() else episodes
        ordered.filter { ep ->
            val matchesQuery = q.isEmpty() || ep.title.contains(q, ignoreCase = true)
            val matchesOffline = !offlineOnlyFilter || PodcastStorage.isDownloaded(context, ep.id)
            matchesQuery && matchesOffline
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DhikriCompactTopBar(
                title = selectedShow?.titleArabic ?: "بودكاست",
                subtitle = selectedShow?.hostArabic ?: "تشغيل بالخلفية وتنزيل أوفلاين",
                onBack = {
                    if (selectedShow != null) {
                        selectedShow = null
                    } else {
                        onBack()
                    }
                },
                actions = {
                    selectedShow?.let { current ->
                        IconButton(onClick = { loadShowEpisodes(current, forceRefresh = true) }) {
                            Icon(Icons.Default.Refresh, contentDescription = "تحديث الحلقات", tint = SoftGold)
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (playback.episodeId.isNotBlank()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = EmeraldDark,
                    border = BorderStroke(1.2.dp, SoftGold),
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(Color(0xFF14345E), EmeraldDark)))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (playback.isOfflineFile) "${playback.showTitle} • أوفلاين" else playback.showTitle,
                                    color = SoftGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = playback.episodeTitle,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(
                                    onClick = { PodcastPlaybackService.sendAction(context, PodcastPlaybackService.ACTION_CYCLE_SPEED) },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("${playback.playbackSpeed}x", color = SoftGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                IconButton(
                                    onClick = { PodcastPlaybackService.sendAction(context, PodcastPlaybackService.ACTION_STOP) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "إغلاق المشغّل", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        playback.errorMessage?.let { err ->
                            Text(err, color = Color(0xFFFFB4AB), fontSize = 11.sp)
                        }

                        if (playback.durationMs > 0) {
                            Slider(
                                value = playback.currentPositionMs.toFloat().coerceIn(0f, playback.durationMs.toFloat()),
                                onValueChange = { newVal ->
                                    PodcastPlaybackService.seekTo(context, newVal.toInt())
                                },
                                valueRange = 0f..playback.durationMs.toFloat(),
                                colors = SliderDefaults.colors(
                                    thumbColor = SoftGold,
                                    activeTrackColor = SoftGold,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                                ),
                                modifier = Modifier.height(26.dp)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(formatMillis(playback.currentPositionMs), color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)

                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                IconButton(
                                    onClick = { PodcastPlaybackService.sendAction(context, PodcastPlaybackService.ACTION_REWIND) },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(Icons.Default.Replay10, contentDescription = "ترجيع 15 ثانية", tint = Color.White)
                                }

                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(SoftGold)
                                        .clickable {
                                            PodcastPlaybackService.sendAction(context, PodcastPlaybackService.ACTION_TOGGLE)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (playback.isBuffering) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(22.dp),
                                            strokeWidth = 2.5.dp,
                                            color = EmeraldDark
                                        )
                                    } else {
                                        Icon(
                                            imageVector = if (playback.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = if (playback.isPlaying) "إيقاف مؤقت" else "تشغيل",
                                            tint = EmeraldDark,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { PodcastPlaybackService.sendAction(context, PodcastPlaybackService.ACTION_FORWARD) },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(Icons.Default.Forward10, contentDescription = "تقديم 15 ثانية", tint = Color.White)
                                }
                            }
                            }

                            Text(
                                text = if (playback.durationMs > 0) formatMillis(playback.durationMs) else playback.durationLabel,
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (selectedShow == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(PodcastCategory.entries) { cat ->
                        val active = cat == selectedCategory
                        Surface(
                            modifier = Modifier.clickable { selectedCategory = cat },
                            shape = RoundedCornerShape(14.dp),
                            color = if (active) EmeraldDark else cardBg,
                            border = BorderStroke(1.dp, if (active) SoftGold else SoftGold.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = cat.labelArabic,
                                color = if (active) SoftGold else primaryText,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.5.sp,
                                modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(filteredShows, key = { it.id }) { show ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { loadShowEpisodes(show) },
                            shape = RoundedCornerShape(18.dp),
                            color = cardBg,
                            border = BorderStroke(1.dp, SoftGold.copy(alpha = 0.38f)),
                            shadowElevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Brush.linearGradient(listOf(Color(0xFF28588A), EmeraldDark))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Podcasts,
                                        contentDescription = null,
                                        tint = SoftGold,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(
                                        text = show.titleArabic,
                                        color = primaryText,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = show.hostArabic,
                                        color = if (isDark) SoftGold else Color(0xFF8C691B),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = show.descriptionArabic,
                                        color = secondaryText,
                                        fontSize = 11.5.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronLeft,
                                    contentDescription = "عرض الحلقات",
                                    tint = SoftGold
                                )
                            }
                        }
                    }
                }
            }
        } else {
            val activeShow = selectedShow!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = episodeQuery,
                    onValueChange = { episodeQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("ابحث في حلقات ${activeShow.titleArabic}...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SoftGold) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "الحلقات: ${filteredEpisodes.size}",
                        color = secondaryText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.clickable {
                                newestFirst = !newestFirst
                                scope.launch { episodeListState.scrollToItem(0) }
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (newestFirst) EmeraldDark else cardBg,
                            border = BorderStroke(1.dp, SoftGold.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapVert,
                                    contentDescription = "تبديل الترتيب",
                                    tint = if (newestFirst) SoftGold else primaryText,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = if (newestFirst) "الأحدث أولاً" else "من الأولى",
                                    color = if (newestFirst) SoftGold else primaryText,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Surface(
                            modifier = Modifier.clickable { offlineOnlyFilter = !offlineOnlyFilter },
                            shape = RoundedCornerShape(12.dp),
                            color = if (offlineOnlyFilter) EmeraldDark else cardBg,
                            border = BorderStroke(1.dp, SoftGold.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OfflinePin,
                                    contentDescription = null,
                                    tint = if (offlineOnlyFilter) SoftGold else primaryText,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "أوفلاين",
                                    color = if (offlineOnlyFilter) SoftGold else primaryText,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                when {
                    isLoadingEpisodes -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(color = SoftGold)
                                Text("جاري جلب أحدث الحلقات...", color = secondaryText, fontSize = 13.sp)
                            }
                        }
                    }
                    loadError != null -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Text(loadError!!, color = primaryText, fontSize = 13.5.sp)
                                Button(
                                    onClick = { loadShowEpisodes(activeShow, forceRefresh = true) },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark, contentColor = SoftGold)
                                ) {
                                    Text("إعادة المحاولة")
                                }
                            }
                        }
                    }
                    filteredEpisodes.isEmpty() -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (offlineOnlyFilter) "لم تقم بتنزيل أي حلقة من هذا البرنامج بعد." else "لا توجد حلقات مطابقة لبحثك.",
                                color = secondaryText,
                                fontSize = 13.5.sp
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            state = episodeListState,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 18.dp)
                        ) {
                            items(filteredEpisodes, key = { it.id }) { ep ->
                                val isCurrent = playback.episodeId == ep.id
                                val downloaded = remember(ep.id, storageVersion) {
                                    PodcastStorage.isDownloaded(context, ep.id)
                                }
                                val savedPosMs = remember(ep.id, playback.currentPositionMs, playback.isPlaying) {
                                    if (isCurrent) playback.currentPositionMs else PodcastStorage.getSavedPosition(context, ep.id)
                                }
                                val dlPct = downloadProgress[ep.id]

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { PodcastPlaybackService.playEpisode(context, activeShow, ep) },
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isCurrent) EmeraldDark else cardBg,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isCurrent) SoftGold else SoftGold.copy(alpha = 0.32f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(if (isCurrent) SoftGold else EmeraldDark),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (isCurrent && playback.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = "تشغيل",
                                                tint = if (isCurrent) EmeraldDark else SoftGold,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                            Text(
                                                text = ep.title,
                                                color = if (isCurrent) Color.White else primaryText,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            val metaParts = mutableListOf<String>()
                                            if (ep.publishedLabel.isNotBlank()) metaParts.add(ep.publishedLabel)
                                            if (ep.durationLabel.isNotBlank()) metaParts.add(ep.durationLabel)
                                            if (savedPosMs >= 3_000) metaParts.add("متابعة من ${formatMillis(savedPosMs)}")
                                            if (metaParts.isNotEmpty()) {
                                                Text(
                                                    text = metaParts.joinToString(" • "),
                                                    color = if (isCurrent) SoftGold else secondaryText,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                        // Offline Download / Progress / Delete button
                                        when {
                                            dlPct != null -> {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(22.dp),
                                                        strokeWidth = 2.dp,
                                                        color = SoftGold
                                                    )
                                                    Text("$dlPct%", fontSize = 9.sp, color = if (isCurrent) SoftGold else secondaryText)
                                                }
                                            }
                                            downloaded -> {
                                                IconButton(
                                                    onClick = {
                                                        PodcastStorage.deleteDownload(context, ep.id)
                                                        storageVersion++
                                                    },
                                                    modifier = Modifier.size(34.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.DownloadDone,
                                                        contentDescription = "محفوظة أوفلاين (اضغط للحذف)",
                                                        tint = Color(0xFF4ADE80),
                                                        modifier = Modifier.size(21.dp)
                                                    )
                                                }
                                            }
                                            else -> {
                                                IconButton(
                                                    onClick = {
                                                        downloadProgress[ep.id] = 0
                                                        scope.launch {
                                                            PodcastStorage.downloadEpisode(context, activeShow.id, ep) { pct ->
                                                                downloadProgress[ep.id] = pct
                                                            }
                                                            downloadProgress.remove(ep.id)
                                                            storageVersion++
                                                        }
                                                    },
                                                    modifier = Modifier.size(34.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Download,
                                                        contentDescription = "تنزيل للاستماع بدون إنترنت",
                                                        tint = SoftGold,
                                                        modifier = Modifier.size(21.dp)
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
        }
    }
}

private fun formatMillis(ms: Int): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
