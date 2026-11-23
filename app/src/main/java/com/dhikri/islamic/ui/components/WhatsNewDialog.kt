package com.dhikri.islamic.ui.components

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.dhikri.islamic.BuildConfig
import com.dhikri.islamic.ui.theme.EmeraldDark
import com.dhikri.islamic.ui.theme.SoftGold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

object WhatsNewTracker {
    private const val PREFS_NAME = "dhikri_whats_new_prefs"
    private const val KEY_SEEN_VERSION = "last_seen_version_name"
    private const val KEY_DYNAMIC_VERSION = "dynamic_notes_version"
    private const val KEY_DYNAMIC_NOTES = "dynamic_notes_body"
    private const val RELEASE_TAG_API =
        "https://api.github.com/repos/nabelmuh525-design/dhikri-app/releases/tags/v"

    fun shouldShowForCurrentVersion(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val seen = prefs.getString(KEY_SEEN_VERSION, null)
        return seen != BuildConfig.VERSION_NAME
    }

    fun markCurrentVersionSeen(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SEEN_VERSION, BuildConfig.VERSION_NAME).apply()
    }

    fun saveReleaseNotes(context: Context, versionName: String, rawBody: String) {
        val cleanVer = versionName.trim().removePrefix("v")
        if (cleanVer.isBlank() || rawBody.isBlank()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_DYNAMIC_VERSION, cleanVer)
            .putString(KEY_DYNAMIC_NOTES, rawBody)
            .apply()
    }

    fun parseBullets(rawBody: String): List<String> {
        // Strictly accept ONLY explicit markdown bullet lines (- / * / •) so generic 1-line CI release banners
        // never overwrite the real version highlights!
        return rawBody.lines()
            .map { it.trim() }
            .filter { it.startsWith("- ") || it.startsWith("* ") || it.startsWith("• ") }
            .map { it.drop(2).trim() }
            .filter {
                it.length >= 8 &&
                    !it.contains("التحديث التلقائي الموثّق") &&
                    !it.startsWith("SHA-256", ignoreCase = true) &&
                    !it.startsWith("http", ignoreCase = true)
            }
            .take(5)
    }

    fun highlightsForCurrentVersion(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedVer = prefs.getString(KEY_DYNAMIC_VERSION, null)
        val savedBody = prefs.getString(KEY_DYNAMIC_NOTES, null)
        if (savedVer == BuildConfig.VERSION_NAME && !savedBody.isNullOrBlank()) {
            val parsed = parseBullets(savedBody)
            if (parsed.isNotEmpty()) return parsed
        }
        return currentHighlights()
    }

    /**
     * Automatically fetches the live GitHub Release notes for the exact installed version
     * (works whether updated in-app, via GitHub, APKPure, or Uptodown) and caches them locally.
     */
    suspend fun fetchLiveHighlightsForVersion(
        context: Context,
        versionName: String = BuildConfig.VERSION_NAME
    ): List<String>? = withContext(Dispatchers.IO) {
        val cleanVer = versionName.trim().removePrefix("v")
        runCatching {
            val conn = (URL("$RELEASE_TAG_API$cleanVer").openConnection() as? HttpsURLConnection)
                ?: return@withContext null
            conn.apply {
                connectTimeout = 7_000
                readTimeout = 7_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "DHIKRI-Android/$cleanVer")
            }
            if (conn.responseCode != 200) {
                conn.disconnect()
                return@withContext null
            }
            val text = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            conn.disconnect()
            val body = JSONObject(text).optString("body").trim()
            val parsed = parseBullets(body)
            if (parsed.isNotEmpty()) {
                saveReleaseNotes(context, cleanVer, body)
                parsed
            } else null
        }.getOrNull()
    }

    fun currentHighlights(): List<String> = listOf(
        "ميزة التشغيل التلقائي للسورة التالية في التلاوات وللحلقة التالية في البودكاست أثناء القيادة أو المشي وحتى عند قفل الشاشة",
        "إضافة مفتاح اختياري لتفعيل أو إيقاف التشغيل التلقائي في كل من شاشة التلاوات وشاشة البودكاست مع دعم أزرار التالي والسابق من سماعات البلوتوث والسيارة",
        "إضافة زر «آخر استماع» في الشريط السفلي لاستئناف آخر سورة أو حلقة بودكاست من نفس الدقيقة بضغطة واحدة، وإغلاق المشغّل الشفاف تلقائياً عند الخروج من التطبيق"
    )
}

@Composable
fun WhatsNewDialog(
    version: String = BuildConfig.VERSION_NAME,
    highlights: List<String>? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var displayHighlights by remember(version, highlights) {
        mutableStateOf(highlights ?: WhatsNewTracker.highlightsForCurrentVersion(context))
    }

    // Automatically fetch live release notes for this exact version from GitHub Releases
    LaunchedEffect(version, highlights) {
        if (highlights == null) {
            val live = WhatsNewTracker.fetchLiveHighlightsForVersion(context, version)
            if (!live.isNullOrEmpty()) {
                displayHighlights = live
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = EmeraldDark,
            border = BorderStroke(1.5.dp, SoftGold),
            shadowElevation = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(SoftGold.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = SoftGold,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "ما الجديد في ذِكري",
                            color = SoftGold,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "الإصدار v$version",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp
                        )
                    }
                }

                HorizontalDivider(color = SoftGold.copy(alpha = 0.28f))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    displayHighlights.forEach { item ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SoftGold,
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(18.dp)
                            )
                            Text(
                                text = item,
                                color = Color.White.copy(alpha = 0.94f),
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SoftGold,
                        contentColor = Color(0xFF1F1604)
                    )
                ) {
                    Text(
                        text = "رائع، ابدأ الاستخدام",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}
