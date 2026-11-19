package com.dhikri.islamic.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.dhikri.islamic.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.URL
import javax.net.ssl.HttpsURLConnection

data class AppUpdateInfo(
    val latestVersion: String,
    val apkUrl: String,
    val releaseNotes: String
)

object AppUpdater {
    private const val RELEASES_API =
        "https://api.github.com/repos/nabelmuh525-design/dhikri-app/releases/latest"
    private const val ALLOWED_APK_HOST = "github.com"

    suspend fun checkForUpdate(): AppUpdateInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = (URL(RELEASES_API).openConnection() as? HttpsURLConnection) ?: return@withContext null
            conn.apply {
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "DHIKRI-Android/${BuildConfig.VERSION_NAME}")
            }
            if (conn.responseCode != 200) {
                conn.disconnect()
                return@withContext null
            }
            val body = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            conn.disconnect()

            val json = JSONObject(body)
            val tag = json.optString("tag_name").trim().removePrefix("v")
            if (tag.isBlank() || !isNewerVersion(tag, BuildConfig.VERSION_NAME)) {
                return@withContext null
            }

            val assets = json.optJSONArray("assets") ?: return@withContext null
            var apkUrl: String? = null
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = asset.optString("name")
                val url = asset.optString("browser_download_url")
                if (name.endsWith(".apk", ignoreCase = true) && url.startsWith("https://$ALLOWED_APK_HOST/")) {
                    apkUrl = url
                    break
                }
            }
            if (apkUrl == null) return@withContext null

            AppUpdateInfo(
                latestVersion = tag,
                apkUrl = apkUrl,
                releaseNotes = json.optString("body").ifBlank { "تحسينات وميزات جديدة لتطبيق ذِكري." }
            )
        }.getOrNull()
    }

    suspend fun downloadAndInstall(
        context: Context,
        apkUrl: String,
        onProgress: (Int) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val outFile = File(updatesDir, "dhikri-update.apk")

            val conn = (URL(apkUrl).openConnection() as HttpsURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 30_000
                instanceFollowRedirects = true
            }
            if (conn.responseCode != 200) {
                conn.disconnect()
                error("HTTP ${conn.responseCode}")
            }
            val totalBytes = conn.contentLengthLong.coerceAtLeast(1L)
            conn.inputStream.use { input ->
                outFile.outputStream().use { output ->
                    val buf = ByteArray(16_384)
                    var downloaded = 0L
                    while (true) {
                        val read = input.read(buf)
                        if (read == -1) break
                        output.write(buf, 0, read)
                        downloaded += read
                        onProgress(((downloaded * 100) / totalBytes).toInt().coerceIn(0, 100))
                    }
                }
            }
            conn.disconnect()

            withContext(Dispatchers.Main) {
                launchApkInstaller(context, outFile)
            }
        }
    }

    private fun launchApkInstaller(context: Context, apkFile: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            val permIntent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(permIntent)
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(installIntent)
    }

    fun isNewerVersion(remote: String, current: String): Boolean {
        val rParts = remote.split(".").map { it.toIntOrNull() ?: 0 }
        val cParts = current.split(".").map { it.toIntOrNull() ?: 0 }
        val maxLen = maxOf(rParts.size, cParts.size)
        for (i in 0 until maxLen) {
            val r = rParts.getOrElse(i) { 0 }
            val c = cParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }
}
