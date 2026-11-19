package com.dhikri.islamic.ui.screens

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.icu.text.DateFormat
import android.icu.util.IslamicUmalquraCalendar
import android.icu.util.ULocale
import android.media.MediaPlayer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhikri.islamic.data.local.IslamicToolsData
import com.dhikri.islamic.ui.theme.EmeraldPrimary
import com.dhikri.islamic.ui.theme.HafsFontFamily
import com.dhikri.islamic.ui.theme.SoftGold
import com.dhikri.islamic.ui.viewmodel.DhikriViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.net.ssl.HttpsURLConnection
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KhatmahScreen(
    viewModel: DhikriViewModel,
    onBack: () -> Unit,
    onOpenPage: (Int) -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val dailyPages = ceil(604.0 / settings.khatmahDays.coerceAtLeast(1)).toInt()
    val progress = (settings.khatmahCurrentPage.coerceIn(1, 604) / 604f)
    val percent = (progress * 100).toInt()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ختمة القرآن الكريم") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            if (settings.khatmahActive) "خطتك الحالية: ختمة خلال ${settings.khatmahDays} يوماً"
                            else "اختر مدة الختمة للبدء",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text("الورد اليومي المقترح: $dailyPages صفحة يومياً (حوالي ${max(1, dailyPages / 5)} صفحات بعد كل صلاة)")
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(10.dp)
                        )
                        Text(
                            "التقدم: صفحة ${settings.khatmahCurrentPage} من 604 ($percent%)",
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { onOpenPage(settings.khatmahCurrentPage) },
                                modifier = Modifier.weight(1f)
                            ) { Text("اقرأ ورد اليوم") }
                            OutlinedButton(
                                onClick = {
                                    val next = (settings.khatmahCurrentPage + dailyPages).coerceAtMost(604)
                                    viewModel.updateKhatmahPage(next)
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("أتممت ورد اليوم") }
                        }
                    }
                }
            }

            item {
                Text("إنشاء أو تغيير خطة الختمة", style = MaterialTheme.typography.titleMedium)
            }

            item {
                val plans = listOf(
                    15 to "ختمة مكثفة (15 يوماً — 41 صفحة يومياً)",
                    30 to "ختمة شهرية (30 يوماً — 21 صفحة يومياً)",
                    60 to "ختمة ميسّرة (60 يوماً — 11 صفحة يومياً)",
                    90 to "ختمة هادئة (90 يوماً — 7 صفحات يومياً)"
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    plans.forEach { (days, label) ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { viewModel.startKhatmah(days) },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                if (settings.khatmahActive && settings.khatmahDays == days) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }

            if (settings.khatmahActive) {
                item {
                    TextButton(onClick = { viewModel.resetKhatmah() }) {
                        Text("تصفير التقدم والبدء من جديد")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuqyahScreen(onBack: () -> Unit) {
    val progressMap = remember { mutableStateMapOf<Int, Int>() }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الرُّقية الشرعية الموثقة") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = { progressMap.clear() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "تصفير")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(IslamicToolsData.ruqyahItems.indices.toList()) { idx ->
                val item = IslamicToolsData.ruqyahItems[idx]
                val count = progressMap[idx] ?: 0
                val done = count >= item.repeatCount
                Card(
                    modifier = Modifier.fillMaxWidth().clickable {
                        if (count < item.repeatCount) progressMap[idx] = count + 1
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (done) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(item.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Text(
                            item.text,
                            fontFamily = if (item.text.startsWith("﴿")) HafsFontFamily else null,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text("المصدر: ${item.source}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(onClick = { if (count < item.repeatCount) progressMap[idx] = count + 1 }) {
                                Text(if (done) "تمت القراءة (${item.repeatCount}/${item.repeatCount})" else "التكرار: $count / ${item.repeatCount}")
                            }
                            IconButton(onClick = {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "${item.title}\n${item.text}\nالمصدر: ${item.source}\nعبر تطبيق ذِكري")
                                }
                                context.startActivity(Intent.createChooser(intent, "مشاركة الرقية"))
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "مشاركة")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AsmaUlHusnaScreen(onBack: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val filtered = IslamicToolsData.asmaUlHusna.filter {
        query.isBlank() || it.name.contains(query.trim()) || it.meaning.contains(query.trim())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("أسماء الله الحسنى (99)") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("ابحث عن اسم أو معنى...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.number }) { item ->
                    Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("${item.number}. ${item.name}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                            Text(item.meaning, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QiblaScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var selectedCity by remember { mutableStateOf(IslamicToolsData.qiblaCities.first()) }
    var azimuthDeg by remember { mutableFloatStateOf(0f) }
    var hasSensor by remember { mutableStateOf(true) }

    // Great-circle bearing from selected city to Kaaba (21.4225, 39.8262)
    val qiblaBearing = remember(selectedCity) {
        val lat1 = Math.toRadians(selectedCity.lat)
        val lon1 = Math.toRadians(selectedCity.lon)
        val lat2 = Math.toRadians(21.4225)
        val lon2 = Math.toRadians(39.8262)
        val dLon = lon2 - lon1
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        ((Math.toDegrees(atan2(y, x)) + 360.0) % 360.0).toFloat()
    }

    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rotSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (rotSensor == null) {
            hasSensor = false
            onDispose { }
        } else {
            val listener = object : SensorEventListener {
                val rotMatrix = FloatArray(9)
                val orient = FloatArray(3)
                override fun onSensorChanged(event: SensorEvent) {
                    SensorManager.getRotationMatrixFromVector(rotMatrix, event.values)
                    SensorManager.getOrientation(rotMatrix, orient)
                    azimuthDeg = ((Math.toDegrees(orient[0].toDouble()) + 360.0) % 360.0).toFloat()
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
            sensorManager.registerListener(listener, rotSensor, SensorManager.SENSOR_DELAY_UI)
            onDispose { sensorManager.unregisterListener(listener) }
        }
    }

    val needleRotation = (qiblaBearing - azimuthDeg + 360f) % 360f

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("اتجاه القبلة") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(IslamicToolsData.qiblaCities) { city ->
                    FilterChip(
                        selected = city == selectedCity,
                        onClick = { selectedCity = city },
                        label = { Text(city.nameArabic) }
                    )
                }
            }

            Text(
                "زاوية القبلة من ${selectedCity.nameArabic}: ${qiblaBearing.toInt()}° من الشمال",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )

            Box(
                modifier = Modifier.size(250.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = size.minDimension / 2.2f
                    drawCircle(color = EmeraldPrimary.copy(alpha = 0.14f), radius = radius, center = center)
                    drawCircle(color = SoftGold, radius = radius, center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f))
                    rotate(degrees = needleRotation, pivot = center) {
                        drawLine(
                            color = EmeraldPrimary,
                            start = center,
                            end = Offset(center.x, center.y - radius * 0.82f),
                            strokeWidth = 14f,
                            cap = StrokeCap.Round
                        )
                        drawCircle(color = SoftGold, radius = 14f, center = Offset(center.x, center.y - radius * 0.85f))
                    }
                    drawCircle(color = EmeraldPrimary, radius = 12f, center = center)
                }
            }

            Text(
                if (hasSensor) "وجّه الهاتف أفقياً وحرّكه حتى يشير السهم الذهبي إلى الأعلى تماماً (اتجاه الكعبة المشرفة)."
                else "جهازك لا يدعم حساس البوصلة، يمكنك الاعتماد على زاوية القبلة الموضّحة أعلاه.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HijriCalendarScreen(onBack: () -> Unit) {
    val hijriFormatted = remember {
        runCatching {
            val locale = ULocale("ar@calendar=islamic-umalqura")
            val cal = IslamicUmalquraCalendar(locale)
            DateFormat.getDateInstance(DateFormat.FULL, locale).format(cal)
        }.getOrElse { "التقويم الهجري — أم القرى" }
    }
    val gregorianFormatted = remember {
        SimpleDateFormat("EEEE، d MMMM yyyy", Locale("ar")).format(Date())
    }
    val months = listOf(
        "1. محرّم (شهر الله المحرّم — يوم عاشوراء)",
        "2. صَفَر",
        "3. ربيع الأول",
        "4. ربيع الآخر",
        "5. جمادى الأولى",
        "6. جمادى الآخرة",
        "7. رجب (من الأشهر الحرم)",
        "8. شعبان",
        "9. رمضان (شهر الصيام والقرآن)",
        "10. شوّال (عيد الفطر وصيام الست من شوال)",
        "11. ذو القعدة (من الأشهر الحرم)",
        "12. ذو الحجة (موسم الحج وعرفة وعيد الأضحى)"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("التقويم الهجري") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("التاريخ الهجري اليوم (أم القرى)", style = MaterialTheme.typography.labelMedium)
                        Text(hijriFormatted, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                        Text("الموافق ميلادياً: $gregorianFormatted", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            item { Text("الأشهر الهجرية", style = MaterialTheme.typography.titleMedium) }
            items(months) { m ->
                Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(m, Modifier.padding(14.dp), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioRecitationsScreen(
    viewModel: DhikriViewModel,
    onBack: () -> Unit
) {
    val surahs by viewModel.surahs.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val reciters = listOf(
        "Alafasy_128kbps" to "مشاري راشد العفاسي",
        "Husary_128kbps" to "محمود خليل الحصري",
        "Minshawy_Murattal_128kbps" to "محمد صديق المنشاوي"
    )
    var selectedReciter by remember { mutableStateOf(reciters.first()) }
    var playingSurahId by remember { mutableStateOf<Int?>(null) }
    var downloadingSurahId by remember { mutableStateOf<Int?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    val cacheDir = remember { File(context.filesDir, "recitations").apply { mkdirs() } }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { mediaPlayer?.stop(); mediaPlayer?.release() }
        }
    }

    fun localFile(reciterKey: String, surahId: Int): File =
        File(cacheDir, "${reciterKey}_${"%03d".format(surahId)}.mp3")

    fun playSurah(surahId: Int) {
        runCatching { mediaPlayer?.stop(); mediaPlayer?.release() }
        val file = localFile(selectedReciter.first, surahId)
        val urlOrPath = if (file.exists() && file.length() > 1000) {
            file.absolutePath
        } else {
            val code = "%03d".format(surahId)
            when (selectedReciter.first) {
                "Alafasy_128kbps" -> "https://server8.mp3quran.net/afs/$code.mp3"
                "Husary_128kbps" -> "https://server13.mp3quran.net/husr/$code.mp3"
                else -> "https://server10.mp3quran.net/minsh/$code.mp3"
            }
        }
        statusMessage = if (file.exists()) "جاري التشغيل أوفلاين..." else "جاري التشغيل عبر الإنترنت..."
        val mp = MediaPlayer()
        runCatching {
            mp.setDataSource(urlOrPath)
            mp.setOnPreparedListener {
                it.start()
                playingSurahId = surahId
                statusMessage = "قيد التشغيل الآن"
            }
            mp.setOnCompletionListener { playingSurahId = null; statusMessage = null }
            mp.setOnErrorListener { _, _, _ ->
                playingSurahId = null
                statusMessage = "تعذّر التشغيل — تأكد من الاتصال بالإنترنت أو حمّل السورة أولاً"
                true
            }
            mp.prepareAsync()
            mediaPlayer = mp
        }.onFailure {
            statusMessage = "تعذّر بدء التشغيل"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الاستماع للقرآن الكريم") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    if (playingSurahId != null) {
                        IconButton(onClick = {
                            runCatching { mediaPlayer?.stop(); mediaPlayer?.release() }
                            mediaPlayer = null
                            playingSurahId = null
                            statusMessage = null
                        }) {
                            Icon(Icons.Default.Stop, contentDescription = "إيقاف")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(reciters) { r ->
                    FilterChip(
                        selected = r == selectedReciter,
                        onClick = { selectedReciter = r },
                        label = { Text(r.second) }
                    )
                }
            }
            statusMessage?.let { msg ->
                Text(
                    msg,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(surahs, key = { it.id }) { surah ->
                    val file = localFile(selectedReciter.first, surah.id)
                    val isDownloaded = file.exists() && file.length() > 1000
                    val isPlaying = playingSurahId == surah.id
                    Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("${surah.id}. سورة ${surah.nameArabic}", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    if (isDownloaded) "محمّلة أوفلاين (${String.format("%.1f MB", file.length() / 1_048_576.0)})"
                                    else "غير محمّلة — متاحة للاستماع أو التحميل",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isDownloaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row {
                                IconButton(onClick = {
                                    if (isPlaying) {
                                        runCatching { mediaPlayer?.stop() }
                                        playingSurahId = null
                                    } else {
                                        playSurah(surah.id)
                                    }
                                }) {
                                    Icon(
                                        if (isPlaying) Icons.Default.StopCircle else Icons.Default.PlayCircle,
                                        contentDescription = "تشغيل",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (isDownloaded) {
                                    IconButton(onClick = {
                                        file.delete()
                                        statusMessage = "تم حذف تلاوة سورة ${surah.nameArabic}"
                                    }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "حذف")
                                    }
                                } else {
                                    IconButton(
                                        enabled = downloadingSurahId == null,
                                        onClick = {
                                            downloadingSurahId = surah.id
                                            statusMessage = "جاري تحميل سورة ${surah.nameArabic}..."
                                            scope.launch {
                                                val ok = withContext(Dispatchers.IO) {
                                                    runCatching {
                                                        val code = "%03d".format(surah.id)
                                                        val urlStr = when (selectedReciter.first) {
                                                            "Alafasy_128kbps" -> "https://server8.mp3quran.net/afs/$code.mp3"
                                                            "Husary_128kbps" -> "https://server13.mp3quran.net/husr/$code.mp3"
                                                            else -> "https://server10.mp3quran.net/minsh/$code.mp3"
                                                        }
                                                        val conn = (URL(urlStr).openConnection() as HttpsURLConnection).apply {
                                                            connectTimeout = 15_000
                                                            readTimeout = 30_000
                                                        }
                                                        conn.inputStream.use { inp ->
                                                            file.outputStream().use { out -> inp.copyTo(out) }
                                                        }
                                                        conn.disconnect()
                                                        true
                                                    }.getOrDefault(false)
                                                }
                                                downloadingSurahId = null
                                                statusMessage = if (ok) "تم تحميل سورة ${surah.nameArabic} بنجاح" else "فشل التحميل — تحقق من الإنترنت"
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = "تحميل")
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
