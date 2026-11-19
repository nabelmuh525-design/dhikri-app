package com.dhikri.islamic.data.download

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dhikri.islamic.data.local.DhikriDatabase
import com.dhikri.islamic.data.local.entity.TafsirEntryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.URL
import java.security.MessageDigest
import javax.net.ssl.HttpsURLConnection

class TafsirDownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val bookKey = inputData.getString(KEY_BOOK_KEY) ?: return@withContext Result.failure()
        if (bookKey !in ALLOWED_BOOK_KEYS) {
            return@withContext Result.failure()
        }

        val db = DhikriDatabase.getInstance(applicationContext)
        val pkgDao = db.downloadPackageDao()
        val tafsirDao = db.tafsirDao()

        try {
            val existingMaxSurah = tafsirDao.getMaxDownloadedSurah(bookKey)
            val startSurah = if (existingMaxSurah in 1..113) existingMaxSurah + 1 else {
                if (existingMaxSurah >= 114) {
                    val count = tafsirDao.countEntriesForBook(bookKey)
                    tafsirDao.updateBookStatus(bookKey, 1, count, "QuranEnc-Live")
                    pkgDao.updateProgress(bookKey, "INSTALLED", 100, "VERIFIED")
                    return@withContext Result.success()
                }
                tafsirDao.deleteEntriesForBook(bookKey)
                1
            }

            pkgDao.updateProgress(bookKey, "DOWNLOADING", ((startSurah - 1) * 100) / 114, "")
            val digest = MessageDigest.getInstance("SHA-256")

            for (sura in startSurah..114) {
                if (isStopped) {
                    pkgDao.updateProgress(bookKey, "PAUSED", ((sura - 1) * 100) / 114, "")
                    return@withContext Result.success()
                }
                val endpoint = "https://quranenc.com/api/v1/translation/sura/$bookKey/$sura"
                val conn = (URL(endpoint).openConnection() as? HttpsURLConnection)
                    ?: return@withContext Result.failure()
                conn.apply {
                    connectTimeout = 15_000
                    readTimeout = 20_000
                    requestMethod = "GET"
                    instanceFollowRedirects = false
                }
                val code = conn.responseCode
                if (code != 200) {
                    conn.disconnect()
                    pkgDao.updateProgress(bookKey, "FAILED", ((sura - 1) * 100) / 114, "")
                    return@withContext Result.retry()
                }

                val rawBytes = conn.inputStream.use { input ->
                    val out = ByteArrayOutputStream()
                    val buf = ByteArray(8192)
                    var total = 0
                    while (true) {
                        val read = input.read(buf)
                        if (read == -1) break
                        total += read
                        if (total > MAX_SURA_RESPONSE_BYTES) {
                            throw SecurityException("Response exceeds safety limit")
                        }
                        out.write(buf, 0, read)
                    }
                    out.toByteArray()
                }
                conn.disconnect()
                digest.update(rawBytes)

                val body = rawBytes.toString(Charsets.UTF_8)
                val resultArray = JSONObject(body).getJSONArray("result")
                val batch = ArrayList<TafsirEntryEntity>(resultArray.length())
                for (i in 0 until resultArray.length()) {
                    val obj = resultArray.getJSONObject(i)
                    batch.add(
                        TafsirEntryEntity(
                            bookKey = bookKey,
                            surahId = obj.getString("sura").toInt(),
                            ayahNumber = obj.getString("aya").toInt(),
                            textContent = obj.getString("translation"),
                            footnotes = obj.optString("footnotes").ifBlank { null }
                        )
                    )
                }
                tafsirDao.deleteSurahEntries(bookKey, sura)
                tafsirDao.insertEntries(batch)
                pkgDao.updateProgress(bookKey, "DOWNLOADING", (sura * 100) / 114, "")
            }

            val shaHex = digest.digest().joinToString("") { "%02x".format(it) }
            val totalCount = tafsirDao.countEntriesForBook(bookKey)
            tafsirDao.updateBookStatus(bookKey, 1, totalCount, "QuranEnc-Live")
            pkgDao.updateProgress(bookKey, "INSTALLED", 100, shaHex)
            Result.success()
        } catch (_: Exception) {
            val done = tafsirDao.getMaxDownloadedSurah(bookKey)
            pkgDao.updateProgress(bookKey, "PAUSED", (done * 100) / 114, "")
            Result.failure()
        }
    }

    companion object {
        const val KEY_BOOK_KEY = "book_key"
        private const val MAX_SURA_RESPONSE_BYTES = 5 * 1024 * 1024
        private val ALLOWED_BOOK_KEYS = setOf(
            "arabic_moyassar",
            "arabic_seraj",
            "arabic_mokhtasar"
        )
    }
}
