package com.dhikri.islamic.data.podcast

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

data class LastPlayedTrack(
    val showId: String,
    val showTitle: String,
    val trackId: String,
    val title: String,
    val audioPathOrUrl: String,
    val durationLabel: String,
    val saveProgress: Boolean
)

object PodcastStorage {

    private const val PREFS_PROGRESS = "dhikri_podcast_progress"
    private const val PREFS_OFFLINE_META = "dhikri_podcast_offline_meta"
    private const val PODCAST_DIR = "podcasts"
    private const val PREFS_LAST_PLAYED = "dhikri_last_played_track"

    private fun keyHash(episodeId: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(episodeId.toByteArray(Charsets.UTF_8))
        return digest.take(12).joinToString("") { "%02x".format(it) }
    }


    // --- Last Played Track (Podcast or Quran Recitation) ---

    fun saveLastPlayed(context: Context, track: LastPlayedTrack) {
        if (track.trackId.isBlank() || track.audioPathOrUrl.isBlank()) return
        context.applicationContext.getSharedPreferences(PREFS_LAST_PLAYED, Context.MODE_PRIVATE)
            .edit()
            .putString("showId", track.showId)
            .putString("showTitle", track.showTitle)
            .putString("trackId", track.trackId)
            .putString("title", track.title)
            .putString("audioPathOrUrl", track.audioPathOrUrl)
            .putString("durationLabel", track.durationLabel)
            .putBoolean("saveProgress", track.saveProgress)
            .apply()
    }

    fun getLastPlayed(context: Context): LastPlayedTrack? {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_LAST_PLAYED, Context.MODE_PRIVATE)
        val trackId = prefs.getString("trackId", null).orEmpty()
        val audio = prefs.getString("audioPathOrUrl", null).orEmpty()
        if (trackId.isBlank() || audio.isBlank()) return null
        return LastPlayedTrack(
            showId = prefs.getString("showId", "").orEmpty(),
            showTitle = prefs.getString("showTitle", "").orEmpty(),
            trackId = trackId,
            title = prefs.getString("title", "").orEmpty(),
            audioPathOrUrl = audio,
            durationLabel = prefs.getString("durationLabel", "").orEmpty(),
            saveProgress = prefs.getBoolean("saveProgress", true)
        )
    }

    // --- Resume Position Persistence ---

    fun savePosition(context: Context, episodeId: String, positionMs: Int, durationMs: Int) {
        if (episodeId.isBlank()) return
        val prefs = context.applicationContext.getSharedPreferences(PREFS_PROGRESS, Context.MODE_PRIVATE)
        val key = "pos_${keyHash(episodeId)}"
        // Clear saved position if finished (within last 10 seconds)
        if (durationMs > 20_000 && positionMs >= durationMs - 10_000) {
            prefs.edit().remove(key).apply()
        } else if (positionMs >= 3_000) {
            prefs.edit().putInt(key, positionMs).apply()
        }
    }

    fun getSavedPosition(context: Context, episodeId: String): Int {
        if (episodeId.isBlank()) return 0
        val prefs = context.applicationContext.getSharedPreferences(PREFS_PROGRESS, Context.MODE_PRIVATE)
        return prefs.getInt("pos_${keyHash(episodeId)}", 0).coerceAtLeast(0)
    }

    // --- Offline Episode Downloads ---

    fun localAudioFile(context: Context, episodeId: String): File {
        val dir = File(context.applicationContext.filesDir, PODCAST_DIR)
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "ep_${keyHash(episodeId)}.mp3")
    }

    fun isDownloaded(context: Context, episodeId: String): Boolean {
        val f = localAudioFile(context, episodeId)
        return f.exists() && f.length() > 16_384L
    }

    fun deleteDownload(context: Context, episodeId: String): Boolean {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_OFFLINE_META, Context.MODE_PRIVATE)
        prefs.edit().remove("meta_${keyHash(episodeId)}").apply()
        val f = localAudioFile(context, episodeId)
        return !f.exists() || f.delete()
    }

    fun getDownloadedEpisodesForShow(context: Context, showId: String): List<PodcastEpisode> {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_OFFLINE_META, Context.MODE_PRIVATE)
        val result = mutableListOf<PodcastEpisode>()
        prefs.all.forEach { (_, value) ->
            val raw = value as? String ?: return@forEach
            val parts = raw.split("\t")
            if (parts.size >= 6 && parts[0] == showId) {
                val ep = PodcastEpisode(
                    id = parts[1],
                    title = parts[2],
                    audioUrl = parts[3],
                    durationLabel = parts[4],
                    publishedLabel = parts[5],
                    summary = parts.getOrElse(6) { "" }
                )
                if (isDownloaded(context, ep.id)) {
                    result.add(ep)
                }
            }
        }
        return result
    }

    private fun saveOfflineMeta(context: Context, showId: String, ep: PodcastEpisode) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_OFFLINE_META, Context.MODE_PRIVATE)
        val clean = listOf(
            showId,
            ep.id,
            ep.title,
            ep.audioUrl,
            ep.durationLabel,
            ep.publishedLabel,
            ep.summary
        ).joinToString("\t") { it.replace("\t", " ").replace("\n", " ") }
        prefs.edit().putString("meta_${keyHash(ep.id)}", clean).apply()
    }

    suspend fun downloadEpisode(
        context: Context,
        showId: String,
        episode: PodcastEpisode,
        onProgress: (Int) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val target = localAudioFile(context, episode.id)
        val temp = File(target.parentFile, "${target.name}.part")
        runCatching {
            var currentUrl = episode.audioUrl.replaceFirst("http://", "https://", ignoreCase = true)
            var conn: HttpURLConnection? = null
            repeat(6) {
                val url = URL(currentUrl)
                require(url.protocol.equals("https", ignoreCase = true)) {
                    "Only HTTPS audio downloads are permitted"
                }
                val c = (url.openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = false
                    connectTimeout = 20_000
                    readTimeout = 30_000
                    setRequestProperty("User-Agent", "DHIKRI-Android/4.1.0")
                }
                val code = c.responseCode
                if (code in 301..308) {
                    val loc = c.getHeaderField("Location")
                        ?: throw IllegalStateException("Redirect without Location")
                    c.disconnect()
                    currentUrl = URL(url, loc).toString().replaceFirst("http://", "https://", ignoreCase = true)
                } else if (code in 200..299) {
                    conn = c
                    return@repeat
                } else {
                    c.disconnect()
                    throw IllegalStateException("HTTP $code")
                }
            }
            val activeConn = conn ?: throw IllegalStateException("Too many redirects")
            try {
                val totalBytes = activeConn.contentLengthLong
                var copied = 0L
                var lastPct = -1
                activeConn.inputStream.use { input ->
                    temp.outputStream().use { output ->
                        val buf = ByteArray(16 * 1024)
                        while (true) {
                            val read = input.read(buf)
                            if (read <= 0) break
                            output.write(buf, 0, read)
                            copied += read
                            if (totalBytes > 0L) {
                                val pct = ((copied * 100L) / totalBytes).toInt().coerceIn(0, 100)
                                if (pct != lastPct) {
                                    lastPct = pct
                                    onProgress(pct)
                                }
                            }
                        }
                    }
                }
                if (target.exists()) target.delete()
                check(temp.renameTo(target)) { "Failed to finalize downloaded episode" }
                saveOfflineMeta(context, showId, episode)
                onProgress(100)
                target
            } finally {
                activeConn.disconnect()
            }
        }.onFailure {
            temp.delete()
        }
    }
}
