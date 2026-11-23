package com.dhikri.islamic.data.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import java.security.MessageDigest
import javax.net.ssl.HttpsURLConnection

data class AppUpdateInfo(
    val latestVersion: String,
    val apkUrl: String,
    val sha256Url: String? = null,
    val releaseNotes: String
)

object AppUpdater {
    private const val RELEASES_API =
        "https://api.github.com/repos/nabelmuh525-design/dhikri-app/releases/latest"
    private val ALLOWED_APK_HOSTS = setOf(
        "github.com",
        "objects.githubusercontent.com",
        "release-assets.githubusercontent.com"
    )
    private const val MAX_APK_SIZE_BYTES = 120L * 1024L * 1024L

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
            var sha256Url: String? = null
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = asset.optString("name")
                val url = asset.optString("browser_download_url")
                if (!isTrustedHttpsUrl(url)) continue
                if (name.endsWith(".apk", ignoreCase = true) && apkUrl == null) {
                    apkUrl = url
                } else if (name.endsWith(".sha256", ignoreCase = true) && sha256Url == null) {
                    sha256Url = url
                }
            }
            if (apkUrl == null) return@withContext null

            AppUpdateInfo(
                latestVersion = tag,
                apkUrl = apkUrl,
                sha256Url = sha256Url,
                releaseNotes = json.optString("body").ifBlank { "تحسينات وميزات جديدة لتطبيق ذِكري." }
            )
        }.getOrNull()
    }

    suspend fun downloadAndInstall(
        context: Context,
        apkUrl: String,
        sha256Url: String? = null,
        onProgress: (Int) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            require(isTrustedHttpsUrl(apkUrl)) { "مصدر التحديث غير موثوق" }

            val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val outFile = File(updatesDir, "dhikri-update.apk")
            if (outFile.exists()) outFile.delete()

            val conn = (URL(apkUrl).openConnection() as HttpsURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 30_000
                instanceFollowRedirects = true
            }
            if (conn.responseCode != 200) {
                conn.disconnect()
                error("HTTP ${conn.responseCode}")
            }
            val finalUrl = conn.url.toString()
            require(isTrustedHttpsUrl(finalUrl)) { "تم حظر تحويل إلى رابط غير موثوق" }

            val totalBytes = conn.contentLengthLong.coerceAtLeast(1L)
            require(totalBytes <= MAX_APK_SIZE_BYTES) { "حجم ملف التحديث يتجاوز الحد الآمن" }

            val digest = MessageDigest.getInstance("SHA-256")
            conn.inputStream.use { input ->
                outFile.outputStream().use { output ->
                    val buf = ByteArray(16_384)
                    var downloaded = 0L
                    while (true) {
                        val read = input.read(buf)
                        if (read == -1) break
                        downloaded += read
                        require(downloaded <= MAX_APK_SIZE_BYTES) { "تجاوز الحد الآمن للتنزيل" }
                        output.write(buf, 0, read)
                        digest.update(buf, 0, read)
                        onProgress(((downloaded * 100) / totalBytes).toInt().coerceIn(0, 100))
                    }
                }
            }
            conn.disconnect()

            val actualSha256 = digest.digest().joinToString("") { "%02x".format(it) }

            // 1. Verify SHA-256 checksum against official release .sha256 asset if available
            if (!sha256Url.isNullOrBlank() && isTrustedHttpsUrl(sha256Url)) {
                val expectedSha = fetchExpectedSha256(sha256Url)
                if (expectedSha != null && !expectedSha.equals(actualSha256, ignoreCase = true)) {
                    outFile.delete()
                    error("فشل التحقق من بصمة SHA-256 لملف التحديث")
                }
            }

            // 2. Verify downloaded APK package name & signing certificate match the currently installed app
            if (!verifyApkSignatureMatchesInstalled(context, outFile)) {
                outFile.delete()
                error("فشل التحقق من شهادة توقيع التحديث — تم إلغاء التثبيت حفاظاً على أمانك")
            }

            withContext(Dispatchers.Main) {
                launchApkInstaller(context, outFile)
            }
        }
    }

    fun isTrustedHttpsUrl(url: String): Boolean {
        return runCatching {
            val parsed = URL(url)
            parsed.protocol.equals("https", ignoreCase = true) &&
                parsed.host.lowercase() in ALLOWED_APK_HOSTS
        }.getOrDefault(false)
    }

    private fun fetchExpectedSha256(shaUrl: String): String? = runCatching {
        val conn = (URL(shaUrl).openConnection() as? HttpsURLConnection) ?: return@runCatching null
        conn.connectTimeout = 8_000
        conn.readTimeout = 8_000
        if (conn.responseCode != 200) {
            conn.disconnect()
            return@runCatching null
        }
        val text = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }.trim()
        conn.disconnect()
        val token = text.split(Regex("\\s+")).firstOrNull()?.trim() ?: return@runCatching null
        if (token.matches(Regex("^[a-fA-F0-9]{64}$"))) token else null
    }.getOrNull()

    private fun verifyApkSignatureMatchesInstalled(context: Context, apkFile: File): Boolean = runCatching {
        val pm = context.packageManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val installedInfo = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            val archiveInfo = pm.getPackageArchiveInfo(apkFile.absolutePath, PackageManager.GET_SIGNING_CERTIFICATES)
                ?: return@runCatching false
            if (archiveInfo.packageName != context.packageName) return@runCatching false
            val installedSigs = installedInfo.signingInfo?.apkContentsSigners?.map { it.toByteArray().contentHashCode() }?.toSet()
                ?: return@runCatching false
            val archiveSigs = archiveInfo.signingInfo?.apkContentsSigners?.map { it.toByteArray().contentHashCode() }?.toSet()
                ?: return@runCatching false
            installedSigs.isNotEmpty() && installedSigs == archiveSigs
        } else {
            @Suppress("DEPRECATION")
            val installedInfo = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
            @Suppress("DEPRECATION")
            val archiveInfo = pm.getPackageArchiveInfo(apkFile.absolutePath, PackageManager.GET_SIGNATURES)
                ?: return@runCatching false
            if (archiveInfo.packageName != context.packageName) return@runCatching false
            @Suppress("DEPRECATION")
            val installedSigs = installedInfo.signatures?.map { it.toByteArray().contentHashCode() }?.toSet()
                ?: return@runCatching false
            @Suppress("DEPRECATION")
            val archiveSigs = archiveInfo.signatures?.map { it.toByteArray().contentHashCode() }?.toSet()
                ?: return@runCatching false
            installedSigs.isNotEmpty() && installedSigs == archiveSigs
        }
    }.getOrDefault(false)

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
