package com.dhikri.islamic.data.podcast

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaMetadata
import android.media.MediaPlayer
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.dhikri.islamic.MainActivity
import com.dhikri.islamic.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PodcastPlaybackState(
    val showId: String = "",
    val showTitle: String = "",
    val episodeId: String = "",
    val episodeTitle: String = "",
    val audioUrl: String = "",
    val durationLabel: String = "",
    val isOfflineFile: Boolean = false,
    val saveProgress: Boolean = true,
    val isBuffering: Boolean = false,
    val isPlaying: Boolean = false,
    val currentPositionMs: Int = 0,
    val durationMs: Int = 0,
    val playbackSpeed: Float = 1.0f,
    val errorMessage: String? = null
)

class PodcastPlaybackService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var mediaSession: MediaSession? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastSaveTickMs = 0L

    private val progressRunnable = object : Runnable {
        override fun run() {
            val mp = mediaPlayer
            val cur = _state.value
            if (mp != null && cur.isPlaying) {
                runCatching {
                    val pos = mp.currentPosition.coerceAtLeast(0)
                    val dur = if (mp.duration > 0) mp.duration else cur.durationMs
                    _state.value = cur.copy(currentPositionMs = pos, durationMs = dur)
                    val now = System.currentTimeMillis()
                    if (now - lastSaveTickMs >= 3_000L) {
                        lastSaveTickMs = now
                        if (cur.saveProgress) PodcastStorage.savePosition(this@PodcastPlaybackService, cur.episodeId, pos, dur)
                        updateMediaSessionPlaybackState()
                    }
                }
                mainHandler.postDelayed(this, 500L)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
        mediaSession = MediaSession(this, "DhikriPodcastSession").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() = togglePlayPause(forcePlay = true)
                override fun onPause() = togglePlayPause(forcePause = true)
                override fun onFastForward() = seekBy(15_000)
                override fun onRewind() = seekBy(-15_000)
                override fun onSeekTo(pos: Long) = seekTo(pos.toInt())
                override fun onStop() = stopAndClose()
            })
            isActive = true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_EPISODE -> {
                val showId = intent.getStringExtra(EXTRA_SHOW_ID).orEmpty()
                val showTitle = intent.getStringExtra(EXTRA_SHOW_TITLE).orEmpty()
                val epId = intent.getStringExtra(EXTRA_EPISODE_ID).orEmpty()
                val epTitle = intent.getStringExtra(EXTRA_EPISODE_TITLE).orEmpty()
                val audioUrl = intent.getStringExtra(EXTRA_AUDIO_URL).orEmpty()
                val durLabel = intent.getStringExtra(EXTRA_DURATION_LABEL).orEmpty()
                if (epId.isNotBlank() && audioUrl.isNotBlank()) {
                    startTrack(showId, showTitle, epId, epTitle, audioUrl, durLabel, directLocalPath = null, saveProgress = true)
                }
            }
            ACTION_PLAY_DIRECT -> {
                val showId = intent.getStringExtra(EXTRA_SHOW_ID).orEmpty()
                val showTitle = intent.getStringExtra(EXTRA_SHOW_TITLE).orEmpty()
                val trackId = intent.getStringExtra(EXTRA_EPISODE_ID).orEmpty()
                val title = intent.getStringExtra(EXTRA_EPISODE_TITLE).orEmpty()
                val target = intent.getStringExtra(EXTRA_AUDIO_URL).orEmpty()
                val isLocal = !target.startsWith("http", ignoreCase = true)
                if (trackId.isNotBlank() && target.isNotBlank()) {
                    startTrack(
                        showId = showId,
                        showTitle = showTitle,
                        episodeId = trackId,
                        episodeTitle = title,
                        audioUrl = target,
                        durationLabel = "",
                        directLocalPath = if (isLocal) target else null,
                        saveProgress = false
                    )
                }
            }
            ACTION_TOGGLE -> togglePlayPause()
            ACTION_REWIND -> seekBy(-15_000)
            ACTION_FORWARD -> seekBy(15_000)
            ACTION_SEEK_TO -> seekTo(intent.getIntExtra(EXTRA_SEEK_MS, 0))
            ACTION_CYCLE_SPEED -> cycleSpeed()
            ACTION_STOP -> stopAndClose()
        }
        return START_NOT_STICKY
    }

    private fun startTrack(
        showId: String,
        showTitle: String,
        episodeId: String,
        episodeTitle: String,
        audioUrl: String,
        durationLabel: String,
        directLocalPath: String?,
        saveProgress: Boolean
    ) {
        val current = _state.value
        if (current.episodeId == episodeId && mediaPlayer != null) {
            togglePlayPause()
            return
        }

        // Persist previous episode position before switching
        persistCurrentPosition()
        releasePlayer()

        val localFile = if (directLocalPath != null) java.io.File(directLocalPath) else PodcastStorage.localAudioFile(this, episodeId)
        val offlineAvailable = localFile.exists() && localFile.length() > 10_000L
        val resumeMs = if (saveProgress) PodcastStorage.getSavedPosition(this, episodeId) else 0
        val safeUrl = if (audioUrl.startsWith("http", ignoreCase = true)) {
            audioUrl.replaceFirst("http://", "https://", ignoreCase = true)
        } else audioUrl

        _state.value = PodcastPlaybackState(
            showId = showId,
            showTitle = showTitle,
            episodeId = episodeId,
            episodeTitle = episodeTitle,
            audioUrl = safeUrl,
            durationLabel = durationLabel,
            isOfflineFile = offlineAvailable,
            saveProgress = saveProgress,
            isBuffering = true,
            isPlaying = false,
            currentPositionMs = resumeMs,
            durationMs = 0,
            playbackSpeed = if (saveProgress) current.playbackSpeed else 1.0f,
            errorMessage = null
        )

        updateForegroundNotification()

        val mp = MediaPlayer().apply {
            setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(if (saveProgress) AudioAttributes.CONTENT_TYPE_SPEECH else AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setOnPreparedListener { prepared ->
                val dur = prepared.duration.coerceAtLeast(0)
                if (resumeMs in 1_000 until (dur - 5_000).coerceAtLeast(1_000)) {
                    runCatching { prepared.seekTo(resumeMs) }
                }
                prepared.start()
                applySpeed(prepared, _state.value.playbackSpeed)
                _state.value = _state.value.copy(
                    isBuffering = false,
                    isPlaying = true,
                    durationMs = dur,
                    currentPositionMs = prepared.currentPosition.coerceAtLeast(0)
                )
                updateMediaSessionMetadata()
                updateMediaSessionPlaybackState()
                updateForegroundNotification()
                mainHandler.removeCallbacks(progressRunnable)
                mainHandler.post(progressRunnable)
            }
            setOnCompletionListener {
                val st = _state.value
                if (st.saveProgress) PodcastStorage.savePosition(this@PodcastPlaybackService, st.episodeId, st.durationMs, st.durationMs)
                _state.value = st.copy(isPlaying = false, currentPositionMs = st.durationMs)
                updateMediaSessionPlaybackState()
                updateForegroundNotification()
            }
            setOnErrorListener { _, _, _ ->
                _state.value = _state.value.copy(
                    isBuffering = false,
                    isPlaying = false,
                    errorMessage = "تعذر تشغيل الحلقة حالياً — تحقق من الاتصال أو نزّلها للاستماع أوفلاين"
                )
                updateMediaSessionPlaybackState()
                updateForegroundNotification()
                true
            }
        }
        mediaPlayer = mp

        runCatching {
            if (offlineAvailable) {
                mp.setDataSource(localFile.absolutePath)
            } else {
                mp.setDataSource(safeUrl)
            }
            mp.prepareAsync()
        }.onFailure {
            _state.value = _state.value.copy(
                isBuffering = false,
                isPlaying = false,
                errorMessage = "تعذر فتح ملف الصوت لهذه الحلقة"
            )
            updateForegroundNotification()
        }
    }

    private fun togglePlayPause(forcePlay: Boolean = false, forcePause: Boolean = false) {
        val mp = mediaPlayer ?: return
        runCatching {
            if ((mp.isPlaying && !forcePlay) || forcePause) {
                mp.pause()
                val pos = mp.currentPosition.coerceAtLeast(0)
                val dur = mp.duration.coerceAtLeast(_state.value.durationMs)
                if (_state.value.saveProgress) PodcastStorage.savePosition(this, _state.value.episodeId, pos, dur)
                _state.value = _state.value.copy(isPlaying = false, currentPositionMs = pos, durationMs = dur)
                mainHandler.removeCallbacks(progressRunnable)
            } else {
                mp.start()
                applySpeed(mp, _state.value.playbackSpeed)
                _state.value = _state.value.copy(isPlaying = true)
                mainHandler.removeCallbacks(progressRunnable)
                mainHandler.post(progressRunnable)
            }
            updateMediaSessionPlaybackState()
            updateForegroundNotification()
        }
    }

    private fun seekBy(deltaMs: Int) {
        val mp = mediaPlayer ?: return
        runCatching {
            val maxDur = mp.duration.coerceAtLeast(_state.value.durationMs)
            val target = (mp.currentPosition + deltaMs).coerceIn(0, maxDur.coerceAtLeast(0))
            mp.seekTo(target)
            if (_state.value.saveProgress) PodcastStorage.savePosition(this, _state.value.episodeId, target, maxDur)
            _state.value = _state.value.copy(currentPositionMs = target, durationMs = maxDur)
            updateMediaSessionPlaybackState()
        }
    }

    private fun seekTo(targetMs: Int) {
        val mp = mediaPlayer ?: return
        runCatching {
            val maxDur = mp.duration.coerceAtLeast(_state.value.durationMs)
            val safe = targetMs.coerceIn(0, maxDur.coerceAtLeast(0))
            mp.seekTo(safe)
            if (_state.value.saveProgress) PodcastStorage.savePosition(this, _state.value.episodeId, safe, maxDur)
            _state.value = _state.value.copy(currentPositionMs = safe, durationMs = maxDur)
            updateMediaSessionPlaybackState()
        }
    }

    private fun cycleSpeed() {
        val next = when (_state.value.playbackSpeed) {
            1.0f -> 1.25f
            1.25f -> 1.5f
            1.5f -> 1.75f
            else -> 1.0f
        }
        _state.value = _state.value.copy(playbackSpeed = next)
        mediaPlayer?.let { mp ->
            if (mp.isPlaying) applySpeed(mp, next)
        }
        updateMediaSessionPlaybackState()
    }

    private fun applySpeed(mp: MediaPlayer, speed: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            runCatching {
                mp.playbackParams = mp.playbackParams.setSpeed(speed)
            }
        }
    }

    private fun stopAndClose() {
        persistCurrentPosition()
        releasePlayer()
        _state.value = PodcastPlaybackState()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun persistCurrentPosition() {
        val st = _state.value
        val mp = mediaPlayer
        if (st.saveProgress && st.episodeId.isNotBlank() && mp != null) {
            runCatching {
                val pos = mp.currentPosition.coerceAtLeast(0)
                val dur = mp.duration.coerceAtLeast(st.durationMs)
                PodcastStorage.savePosition(this, st.episodeId, pos, dur)
            }
        }
    }

    private fun releasePlayer() {
        mainHandler.removeCallbacks(progressRunnable)
        runCatching {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        }
        mediaPlayer = null
    }

    private fun updateMediaSessionMetadata() {
        val st = _state.value
        val meta = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, st.episodeTitle)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, st.showTitle)
            .putString(MediaMetadata.METADATA_KEY_ALBUM, "ذِكري • البودكاست الإسلامي")
            .putLong(MediaMetadata.METADATA_KEY_DURATION, st.durationMs.toLong())
            .build()
        mediaSession?.setMetadata(meta)
    }

    private fun updateMediaSessionPlaybackState() {
        val st = _state.value
        val pbState = when {
            st.isBuffering -> PlaybackState.STATE_BUFFERING
            st.isPlaying -> PlaybackState.STATE_PLAYING
            st.episodeId.isNotBlank() -> PlaybackState.STATE_PAUSED
            else -> PlaybackState.STATE_STOPPED
        }
        val stateObj = PlaybackState.Builder()
            .setActions(
                PlaybackState.ACTION_PLAY or
                    PlaybackState.ACTION_PAUSE or
                    PlaybackState.ACTION_PLAY_PAUSE or
                    PlaybackState.ACTION_REWIND or
                    PlaybackState.ACTION_FAST_FORWARD or
                    PlaybackState.ACTION_SEEK_TO or
                    PlaybackState.ACTION_STOP
            )
            .setState(pbState, st.currentPositionMs.toLong(), if (st.isPlaying) st.playbackSpeed else 0f)
            .build()
        mediaSession?.setPlaybackState(stateObj)
    }

    private fun updateForegroundNotification() {
        val st = _state.value
        val openAppIntent = PendingIntent.getActivity(
            this,
            100,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val rewindPi = servicePendingIntent(ACTION_REWIND, 101)
        val togglePi = servicePendingIntent(ACTION_TOGGLE, 102)
        val forwardPi = servicePendingIntent(ACTION_FORWARD, 103)
        val stopPi = servicePendingIntent(ACTION_STOP, 104)

        val playPauseIcon = if (st.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val playPauseLabel = if (st.isPlaying) "إيقاف مؤقت" else "تشغيل"

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        val notif = builder
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(st.episodeTitle.ifBlank { "البودكاست الإسلامي" })
            .setContentText(
                if (st.isOfflineFile) "${st.showTitle} • بدون إنترنت" else st.showTitle
            )
            .setContentIntent(openAppIntent)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(st.isPlaying || st.isBuffering)
            .addAction(Notification.Action.Builder(android.R.drawable.ic_media_rew, "-15ث", rewindPi).build())
            .addAction(Notification.Action.Builder(playPauseIcon, playPauseLabel, togglePi).build())
            .addAction(Notification.Action.Builder(android.R.drawable.ic_media_ff, "+15ث", forwardPi).build())
            .addAction(Notification.Action.Builder(android.R.drawable.ic_menu_close_clear_cancel, "إغلاق", stopPi).build())
            .setStyle(
                Notification.MediaStyle()
                    .setMediaSession(mediaSession?.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIF_ID, notif)
        }
    }

    private fun servicePendingIntent(action: String, reqCode: Int): PendingIntent {
        val intent = Intent(this, PodcastPlaybackService::class.java).setAction(action)
        return PendingIntent.getService(
            this,
            reqCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val ch = NotificationChannel(
                    CHANNEL_ID,
                    "مشغّل البودكاست الإسلامي",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "عناصر التحكم بالبودكاست في الخلفية وشاشة القفل"
                    setShowBadge(false)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
                nm.createNotificationChannel(ch)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        persistCurrentPosition()
        releasePlayer()
        mediaSession?.isActive = false
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "dhikri_podcast_playback"
        private const val NOTIF_ID = 4101

        const val ACTION_PLAY_EPISODE = "com.dhikri.islamic.podcast.PLAY_EPISODE"
        const val ACTION_PLAY_DIRECT = "com.dhikri.islamic.podcast.PLAY_DIRECT"
        const val ACTION_TOGGLE = "com.dhikri.islamic.podcast.TOGGLE"
        const val ACTION_REWIND = "com.dhikri.islamic.podcast.REWIND"
        const val ACTION_FORWARD = "com.dhikri.islamic.podcast.FORWARD"
        const val ACTION_SEEK_TO = "com.dhikri.islamic.podcast.SEEK_TO"
        const val ACTION_CYCLE_SPEED = "com.dhikri.islamic.podcast.CYCLE_SPEED"
        const val ACTION_STOP = "com.dhikri.islamic.podcast.STOP"

        private const val EXTRA_SHOW_ID = "show_id"
        private const val EXTRA_SHOW_TITLE = "show_title"
        private const val EXTRA_EPISODE_ID = "episode_id"
        private const val EXTRA_EPISODE_TITLE = "episode_title"
        private const val EXTRA_AUDIO_URL = "audio_url"
        private const val EXTRA_DURATION_LABEL = "duration_label"
        private const val EXTRA_SEEK_MS = "seek_ms"

        private val _state = MutableStateFlow(PodcastPlaybackState())
        val state: StateFlow<PodcastPlaybackState> = _state.asStateFlow()

        fun playEpisode(context: Context, show: PodcastShow, episode: PodcastEpisode) {
            val intent = Intent(context, PodcastPlaybackService::class.java).apply {
                action = ACTION_PLAY_EPISODE
                putExtra(EXTRA_SHOW_ID, show.id)
                putExtra(EXTRA_SHOW_TITLE, show.titleArabic)
                putExtra(EXTRA_EPISODE_ID, episode.id)
                putExtra(EXTRA_EPISODE_TITLE, episode.title)
                putExtra(EXTRA_AUDIO_URL, episode.audioUrl)
                putExtra(EXTRA_DURATION_LABEL, episode.durationLabel)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun sendAction(context: Context, action: String) {
            val intent = Intent(context, PodcastPlaybackService::class.java).setAction(action)
            context.startService(intent)
        }

        fun playDirectAudio(
            context: Context,
            groupId: String,
            subtitle: String,
            trackId: String,
            title: String,
            audioPathOrUrl: String
        ) {
            val intent = Intent(context, PodcastPlaybackService::class.java).apply {
                action = ACTION_PLAY_DIRECT
                putExtra(EXTRA_SHOW_ID, groupId)
                putExtra(EXTRA_SHOW_TITLE, subtitle)
                putExtra(EXTRA_EPISODE_ID, trackId)
                putExtra(EXTRA_EPISODE_TITLE, title)
                putExtra(EXTRA_AUDIO_URL, audioPathOrUrl)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun seekTo(context: Context, positionMs: Int) {
            val intent = Intent(context, PodcastPlaybackService::class.java).apply {
                action = ACTION_SEEK_TO
                putExtra(EXTRA_SEEK_MS, positionMs)
            }
            context.startService(intent)
        }
    }
}
