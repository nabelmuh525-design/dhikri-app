package com.dhikri.islamic.ui.screens

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.icu.text.DateFormat
import android.icu.util.Calendar
import android.icu.util.ULocale
import android.media.MediaPlayer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.dhikri.islamic.R
import com.dhikri.islamic.ui.theme.EmeraldDark
import org.json.JSONObject
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
    val isAligned = needleRotation <= 6f || needleRotation >= 354f

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.home_header_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xDCFAF5E8), Color(0xF0FAF5E8), Color(0xF8F3EBD8))
                    )
                )
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    title = { Text("اتجاه القبلة", fontWeight = FontWeight.Bold, color = EmeraldDark) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = EmeraldDark)
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // City Selector
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(IslamicToolsData.qiblaCities) { city ->
                        FilterChip(
                            selected = city == selectedCity,
                            onClick = { selectedCity = city },
                            label = { Text(city.nameArabic) }
                        )
                    }
                }

                // Emerald-Gold Alignment Status Banner
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = if (isAligned) EmeraldDark else Color(0xFFFFFBF0),
                    border = BorderStroke(1.5.dp, SoftGold),
                    shadowElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = if (isAligned) "أنت متجه نحو الكعبة المشرفة تماماً ✓"
                            else "زاوية القبلة من ${selectedCity.nameArabic}: ${qiblaBearing.toInt()}°",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isAligned) SoftGold else EmeraldDark,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "مكة المكرمة — 21.4225° N, 39.8262° E",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isAligned) Color.White.copy(alpha = 0.85f) else Color(0xFF6B5833)
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Ornate Golden-Rimmed Compass Dial with Kaaba Image on Pointer Tip
                Box(
                    modifier = Modifier
                        .size(290.dp)
                        .shadow(12.dp, CircleShape)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(Color(0xFFFFFDF7), Color(0xFFF3E9D2), Color(0xFFE3D2AC))
                            )
                        )
                        .border(5.dp, SoftGold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    // Cardinal Labels
                    Text("ش", fontWeight = FontWeight.Bold, color = EmeraldDark, modifier = Modifier.align(Alignment.TopCenter).padding(top = 14.dp))
                    Text("ج", fontWeight = FontWeight.Bold, color = Color(0xFF6B5833), modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp))
                    Text("ق", fontWeight = FontWeight.Bold, color = Color(0xFF6B5833), modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp))
                    Text("غ", fontWeight = FontWeight.Bold, color = Color(0xFF6B5833), modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp))

                    // Compass Tick Marks & Inner Decorative Ring
                    Canvas(modifier = Modifier.fillMaxSize().padding(18.dp)) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val r = size.minDimension / 2f
                        drawCircle(
                            color = EmeraldPrimary.copy(alpha = 0.12f),
                            radius = r * 0.78f,
                            center = center
                        )
                        drawCircle(
                            color = SoftGold.copy(alpha = 0.65f),
                            radius = r * 0.78f,
                            center = center,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
                        )
                        for (deg in 0 until 360 step 10) {
                            val rad = Math.toRadians(deg.toDouble())
                            val len = if (deg % 30 == 0) 14f else 7f
                            val start = Offset(
                                center.x + (r * 0.96f * sin(rad)).toFloat(),
                                center.y - (r * 0.96f * cos(rad)).toFloat()
                            )
                            val end = Offset(
                                center.x + ((r * 0.96f - len) * sin(rad)).toFloat(),
                                center.y - ((r * 0.96f - len) * cos(rad)).toFloat()
                            )
                            drawLine(
                                color = if (deg % 90 == 0) EmeraldDark else SoftGold.copy(alpha = 0.7f),
                                start = start,
                                end = end,
                                strokeWidth = if (deg % 30 == 0) 3.5f else 2f
                            )
                        }
                    }

                    // Rotating Qibla Pointer + Kaaba Image Directly on the Arrow Tip!
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { rotationZ = needleRotation },
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val tipY = cy - size.minDimension * 0.30f
                            val tailY = cy + size.minDimension * 0.18f
                            drawLine(
                                color = if (isAligned) SoftGold else EmeraldDark,
                                start = Offset(cx, tailY),
                                end = Offset(cx, tipY),
                                strokeWidth = 12f,
                                cap = StrokeCap.Round
                            )
                            drawCircle(color = SoftGold, radius = 14f, center = Offset(cx, cy))
                            drawCircle(color = EmeraldDark, radius = 7f, center = Offset(cx, cy))
                        }

                        // Holy Kaaba Medallion positioned right at the tip of the arrow
                        Image(
                            painter = painterResource(id = R.drawable.kaaba_icon),
                            contentDescription = "الكعبة المشرفة",
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 28.dp)
                                .size(58.dp)
                                .shadow(6.dp, CircleShape)
                                .clip(CircleShape)
                                .border(2.dp, SoftGold, CircleShape)
                        )
                    }
                }

                Text(
                    text = if (hasSensor) "ضع الهاتف بشكل أفقي بعيداً عن المعادن، وحرّكه حتى تصبح الكعبة المشرفة في أعلى البوصلة."
                    else "جهازك لا يحتوي على حساس بوصلة، يمكنك الاعتماد على زاوية القبلة الموضّحة أعلاه.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = EmeraldDark.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HijriCalendarScreen(onBack: () -> Unit) {
    val hijriFormatted = remember {
        runCatching {
            val locale = ULocale("ar@calendar=islamic-umalqura")
            val cal = Calendar.getInstance(locale)
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
                title = { Text("التقويم والمناسبات الإسلامية") },
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
            item {
                Text(
                    "المناسبات والأيام الدينية الإسلامية",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            items(
                listOf(
                    "1 محرّم" to "رأس السنة الهجرية",
                    "10 محرّم" to "يوم عاشوراء (يُستحب صيامه)",
                    "12 ربيع الأول" to "ذكرى المولد النبوي الشريف",
                    "1 رمضان" to "غرة شهر رمضان المبارك",
                    "21 – 29 رمضان" to "العشر الأواخر وليلة القدر",
                    "1 شوّال" to "عيد الفطر المبارك",
                    "1 – 9 ذو الحجة" to "العشر الأوائل من ذي الحجة",
                    "9 ذو الحجة" to "يوم عرفة (صيامه يكفّر سنتين)",
                    "10 ذو الحجة" to "عيد الأضحى المبارك وأيام التشريق"
                )
            ) { (hijriDate, occasion) ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(0.8.dp, SoftGold.copy(alpha = 0.45f))
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(occasion, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                hijriDate,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(4.dp)); Text("الأشهر الهجرية (12 شهراً)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
            items(months) { m ->
                Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(m, Modifier.padding(14.dp), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

private data class ReciterInfo(
    val key: String,
    val nameArabic: String,
    val mp3QuranSearchHint: String,
    val fallbackBaseUrl: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioRecitationsScreen(
    viewModel: DhikriViewModel,
    onBack: () -> Unit
) {
    val surahs by viewModel.surahs.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val reciters = remember {
        listOf(
            ReciterInfo("ayyub", "محمد أيوب", "محمد أيوب", "https://server8.mp3quran.net/ayyub/"),
            ReciterInfo("ajm", "أحمد بن علي العجمي", "العجمي", "https://server10.mp3quran.net/ajm/"),
            ReciterInfo("basit", "عبد الباسط عبد الصمد", "عبد الباسط", "https://server7.mp3quran.net/basit/"),
            ReciterInfo("husr", "محمود خليل الحصري", "الحصري", "https://server13.mp3quran.net/husr/"),
            ReciterInfo("minsh", "محمد صديق المنشاوي", "المنشاوي", "https://server10.mp3quran.net/minsh/")
        )
    }

    var selectedReciter by remember { mutableStateOf(reciters.first()) }
    val resolvedServers = remember { mutableStateMapOf<String, String>() }
    var playingSurahId by remember { mutableStateOf<Int?>(null) }
    var downloadingSurahId by remember { mutableStateOf<Int?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    val cacheDir = remember { File(context.filesDir, "recitations").apply { mkdirs() } }

    // Fetch live verified MP3Quran API v3 server URLs once when online
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            runCatching {
                val conn = (URL("https://www.mp3quran.net/api/v3/reciters?language=ar").openConnection() as HttpsURLConnection).apply {
                    connectTimeout = 10_000
                    readTimeout = 12_000
                }
                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    val arr = JSONObject(body).getJSONArray("reciters")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val name = obj.optString("name")
                        val moshafArr = obj.optJSONArray("moshaf") ?: continue
                        if (moshafArr.length() == 0) continue
                        val server = moshafArr.getJSONObject(0).optString("server")
                        if (!server.startsWith("https://")) continue
                        reciters.forEach { rec ->
                            if (name.contains(rec.mp3QuranSearchHint) && !resolvedServers.containsKey(rec.key)) {
                                resolvedServers[rec.key] = if (server.endsWith("/")) server else "$server/"
                            }
                        }
                    }
                }
                conn.disconnect()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { mediaPlayer?.stop(); mediaPlayer?.release() }
        }
    }

    fun localFile(reciterKey: String, surahId: Int): File =
        File(cacheDir, "${reciterKey}_${"%03d".format(surahId)}.mp3")

    fun surahUrl(reciter: ReciterInfo, surahId: Int): String {
        val base = resolvedServers[reciter.key] ?: reciter.fallbackBaseUrl
        return "${base}${"%03d".format(surahId)}.mp3"
    }

    fun playSurah(surahId: Int) {
        runCatching { mediaPlayer?.stop(); mediaPlayer?.release() }
        val file = localFile(selectedReciter.key, surahId)
        val urlOrPath = if (file.exists() && file.length() > 1000) {
            file.absolutePath
        } else {
            surahUrl(selectedReciter, surahId)
        }
        statusMessage = if (file.exists()) "جاري التشغيل أوفلاين..." else "جاري التشغيل بصوت ${selectedReciter.nameArabic}..."
        val mp = MediaPlayer()
        runCatching {
            mp.setDataSource(urlOrPath)
            mp.setOnPreparedListener {
                it.start()
                playingSurahId = surahId
                statusMessage = "قيد التشغيل الآن — ${selectedReciter.nameArabic}"
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
                items(reciters, key = { it.key }) { r ->
                    FilterChip(
                        selected = r == selectedReciter,
                        onClick = { selectedReciter = r },
                        label = { Text(r.nameArabic) }
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
                    val file = localFile(selectedReciter.key, surah.id)
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
                                                        val urlStr = surahUrl(selectedReciter, surah.id)
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
