package com.dhikri.islamic.data.podcast

import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

enum class PodcastCategory(val labelArabic: String) {
    ALL("الكل"),
    SEERAH("السيرة النبوية"),
    FAITH("وعي وتزكية"),
    HISTORY("التاريخ والأنبياء"),
    SHARIA("فكر وشرعي")
}

data class PodcastShow(
    val id: String,
    val titleArabic: String,
    val hostArabic: String,
    val descriptionArabic: String,
    val rssUrl: String,
    val category: PodcastCategory
)

data class PodcastEpisode(
    val id: String,
    val title: String,
    val audioUrl: String,
    val durationLabel: String,
    val publishedLabel: String,
    val summary: String
)

object IslamicPodcastRepository {

    val shows: List<PodcastShow> = listOf(
        PodcastShow(
            id = "fahem",
            titleArabic = "بودكاست فاهم",
            hostArabic = "حوارات إسلامية وتوعوية شبابية",
            descriptionArabic = "حوارات عميقة ومبسطة تناقش التحديات الإيمانية والفكرية والاجتماعية للشباب المسلم.",
            rssUrl = "https://feeds.simplecast.com/flyig0Ev",
            category = PodcastCategory.FAITH
        ),
        PodcastShow(
            id = "tarweeha",
            titleArabic = "بودكاست ترويحة",
            hostArabic = "تأملات إيمانية وقصص من السيرة",
            descriptionArabic = "رحلة تأملية هادئة تتناول حكم الخالق في أحداث الحياة ومواقف السيرة النبوية.",
            rssUrl = "https://feeds.simplecast.com/BPaX8PyE",
            category = PodcastCategory.FAITH
        ),
        PodcastShow(
            id = "fiqh_alnafs",
            titleArabic = "فقه النفس / مكاني",
            hostArabic = "د. عبد الرحمن ذاكر الهاشمي",
            descriptionArabic = "جلسات في فقه النفس في القرآن والتزكية وبناء الوعي النفسي والتربوي.",
            rssUrl = "https://anchor.fm/s/ef921ef0/podcast/rss",
            category = PodcastCategory.FAITH
        ),
        PodcastShow(
            id = "noor_seerah",
            titleArabic = "نور — السيرة النبوية",
            hostArabic = "مصطفى حسني",
            descriptionArabic = "حلقات متسلسلة في السيرة النبوية العطرة وأثر محبة النبي ﷺ في حياتنا اليومية.",
            rssUrl = "https://anchor.fm/s/ef0fe82c/podcast/rss",
            category = PodcastCategory.SEERAH
        ),
        PodcastShow(
            id = "majalis_seerah",
            titleArabic = "مجالس السيرة النبوية",
            hostArabic = "د. محمد إلهامي",
            descriptionArabic = "مجالس تحليلية وتاريخية تستقرئ الدروس والسنن من أحداث السيرة النبوية.",
            rssUrl = "https://anchor.fm/s/10c25d534/podcast/rss",
            category = PodcastCategory.SEERAH
        ),
        PodcastShow(
            id = "awfi_seerah",
            titleArabic = "السيرة النبوية",
            hostArabic = "الشيخ إسماعيل العوفي",
            descriptionArabic = "سلسلة صوتية مرتّبة في السيرة النبوية بأسلوب هادئ وواضح.",
            rssUrl = "https://anchor.fm/s/6e9b9510/podcast/rss",
            category = PodcastCategory.SEERAH
        ),
        PodcastShow(
            id = "prophets_stories",
            titleArabic = "قصص الأنبياء",
            hostArabic = "سلسلة قصص الأنبياء الموثقة",
            descriptionArabic = "حكايات الأنبياء عليهم السلام كما وردت في القرآن الكريم والسنة النبوية.",
            rssUrl = "https://feeds.simplecast.com/_1G3PqYA",
            category = PodcastCategory.HISTORY
        ),
        PodcastShow(
            id = "ask_history",
            titleArabic = "اسألوا التاريخ",
            hostArabic = "التاريخ الإسلامي والإنساني",
            descriptionArabic = "حلقات سردية مشوّقة تسلط الضوء على محطات التاريخ الإسلامي والعالمي.",
            rssUrl = "https://feeds.simplecast.com/1h1JA2Dr",
            category = PodcastCategory.HISTORY
        ),
        PodcastShow(
            id = "shari_forum",
            titleArabic = "بودكاست شرعي",
            hostArabic = "منتدى العلماء",
            descriptionArabic = "منصة حوار شرعية تعالج القضايا المعاصرة التي تهم المسلم والأمة بتأصيل وواقعية.",
            rssUrl = "https://anchor.fm/s/fc725a18/podcast/rss",
            category = PodcastCategory.SHARIA
        ),
        PodcastShow(
            id = "ghorba",
            titleArabic = "بودكاست غربة",
            hostArabic = "حوارات الهوية والفكر الإسلامي",
            descriptionArabic = "حوارات شبابية تنطلق من تساؤلات الهوية والانتماء والفكر الإسلامي المعاصر.",
            rssUrl = "https://anchor.fm/s/ecb5c54c/podcast/rss",
            category = PodcastCategory.SHARIA
        )
    )

    private val episodeCache = ConcurrentHashMap<String, List<PodcastEpisode>>()
    private val htmlTagRegex = Regex("<[^>]*>")
    private val whitespaceRegex = Regex("\\s+")

    suspend fun fetchEpisodes(
        show: PodcastShow,
        forceRefresh: Boolean = false
    ): Result<List<PodcastEpisode>> = withContext(Dispatchers.IO) {
        if (!forceRefresh) {
            episodeCache[show.id]?.let { cached ->
                if (cached.isNotEmpty()) return@withContext Result.success(cached)
            }
        }
        runCatching {
            val stream = openHttpsStream(show.rssUrl)
            stream.use { input ->
                val parsed = parseRss(input)
                if (parsed.isNotEmpty()) {
                    episodeCache[show.id] = parsed
                }
                parsed
            }
        }
    }

    private fun openHttpsStream(initialUrl: String): InputStream {
        var currentUrl = initialUrl
        repeat(5) {
            val url = URL(currentUrl)
            require(url.protocol.equals("https", ignoreCase = true)) {
                "Only HTTPS podcast feeds are permitted"
            }
            val conn = (url.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = 15_000
                readTimeout = 20_000
                setRequestProperty("User-Agent", "DHIKRI-Android/4.1.0")
                setRequestProperty("Accept", "application/rss+xml, application/xml, text/xml")
            }
            val code = conn.responseCode
            if (code in 301..308) {
                val loc = conn.getHeaderField("Location")
                    ?: throw IllegalStateException("Redirect without Location header")
                conn.disconnect()
                currentUrl = URL(url, loc).toString()
            } else if (code in 200..299) {
                return conn.inputStream
            } else {
                conn.disconnect()
                throw IllegalStateException("HTTP $code while loading podcast feed")
            }
        }
        throw IllegalStateException("Too many redirects while loading podcast feed")
    }

    private fun parseRss(input: InputStream): List<PodcastEpisode> {
        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            setInput(input, "UTF-8")
        }
        val episodes = mutableListOf<PodcastEpisode>()
        var inItem = false
        var title = ""
        var audioUrl = ""
        var duration = ""
        var pubDate = ""
        var description = ""
        var guid = ""

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT && episodes.size < 200) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    val tag = parser.name.lowercase()
                    if (tag == "item") {
                        inItem = true
                        title = ""
                        audioUrl = ""
                        duration = ""
                        pubDate = ""
                        description = ""
                        guid = ""
                    } else if (inItem) {
                        when (tag) {
                            "title" -> title = safeNextText(parser).trim()
                            "guid" -> guid = safeNextText(parser).trim()
                            "pubdate" -> pubDate = formatShortDate(safeNextText(parser).trim())
                            "itunes:duration", "duration" -> duration = formatDurationText(safeNextText(parser).trim())
                            "description", "itunes:summary" -> {
                                if (description.isBlank()) {
                                    description = cleanSummary(safeNextText(parser))
                                }
                            }
                            "enclosure" -> {
                                val u = parser.getAttributeValue(null, "url").orEmpty().trim()
                                if (u.startsWith("https://", ignoreCase = true) || u.startsWith("http://", ignoreCase = true)) {
                                    audioUrl = u
                                }
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name.equals("item", ignoreCase = true) && inItem) {
                        inItem = false
                        if (title.isNotBlank() && audioUrl.isNotBlank()) {
                            episodes.add(
                                PodcastEpisode(
                                    id = guid.ifBlank { audioUrl },
                                    title = title,
                                    audioUrl = audioUrl,
                                    durationLabel = duration,
                                    publishedLabel = pubDate,
                                    summary = description
                                )
                            )
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        return episodes
    }

    private fun safeNextText(parser: XmlPullParser): String {
        return runCatching {
            if (parser.next() == XmlPullParser.TEXT) parser.text.orEmpty() else ""
        }.getOrDefault("")
    }

    private fun cleanSummary(raw: String): String {
        val stripped = htmlTagRegex.replace(raw, " ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
        return whitespaceRegex.replace(stripped, " ").trim().take(160)
    }

    private fun formatShortDate(raw: String): String {
        // RSS pubDate is typically "Mon, 18 May 2026 10:00:00 +0000" -> "18 May 2026"
        val parts = raw.split(" ").filter { it.isNotBlank() }
        return if (parts.size >= 4) "${parts[1]} ${parts[2]} ${parts[3]}" else raw.take(16)
    }

    private fun formatDurationText(raw: String): String {
        if (raw.isBlank()) return ""
        if (raw.contains(":")) return raw
        val totalSec = raw.toLongOrNull() ?: return raw
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    }
}
