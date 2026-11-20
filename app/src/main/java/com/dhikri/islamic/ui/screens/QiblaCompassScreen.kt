package com.dhikri.islamic.ui.screens

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.dhikri.islamic.R
import com.dhikri.islamic.ui.components.DhikriCompactTopBar
import com.dhikri.islamic.ui.theme.EmeraldDark
import com.dhikri.islamic.ui.theme.SoftGold
import kotlin.math.*

private data class CompassCity(
    val nameArabic: String,
    val latitude: Double,
    val longitude: Double
)

private val compassCities = listOf(
    CompassCity("عمّان", 31.9539, 35.9106),
    CompassCity("إربد", 32.5568, 35.8469),
    CompassCity("الزرقاء", 32.0728, 36.0880),
    CompassCity("العقبة", 29.5321, 35.0063),
    CompassCity("القدس", 31.7683, 35.2137),
    CompassCity("مكة المكرمة", 21.4225, 39.8262),
    CompassCity("المدينة المنورة", 24.4672, 39.6112),
    CompassCity("الرياض", 24.7136, 46.6753),
    CompassCity("القاهرة", 30.0444, 31.2357),
    CompassCity("دمشق", 33.5138, 36.2765),
    CompassCity("بيروت", 33.8938, 35.5018),
    CompassCity("بغداد", 33.3152, 44.3661),
    CompassCity("دبي", 25.2048, 55.2708),
    CompassCity("الدوحة", 25.2854, 51.5310)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QiblaCompassScreen(onBack: (() -> Unit)? = null) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var selectedCity by remember { mutableStateOf(compassCities.first()) }
    var azimuthDeg by remember { mutableFloatStateOf(0f) }
    var hasSensor by remember { mutableStateOf(true) }
    var wasAligned by remember { mutableStateOf(false) }

    // Great-circle bearing to Kaaba (21.4225° N, 39.8262° E)
    val qiblaBearing = remember(selectedCity) {
        val lat1 = Math.toRadians(selectedCity.latitude)
        val lon1 = Math.toRadians(selectedCity.longitude)
        val lat2 = Math.toRadians(21.4225)
        val lon2 = Math.toRadians(39.8262)
        val dLon = lon2 - lon1
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        ((Math.toDegrees(atan2(y, x)) + 360.0) % 360.0).toFloat()
    }

    // Battery-efficient lifecycle-bound compass sensor
    DisposableEffect(context, lifecycleOwner) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rotSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (rotSensor == null) {
            hasSensor = false
            onDispose { }
        } else {
            var lastEmitted = -999f
            val listener = object : SensorEventListener {
                val rotMatrix = FloatArray(9)
                val orient = FloatArray(3)
                override fun onSensorChanged(event: SensorEvent) {
                    SensorManager.getRotationMatrixFromVector(rotMatrix, event.values)
                    SensorManager.getOrientation(rotMatrix, orient)
                    val nextDeg = ((Math.toDegrees(orient[0].toDouble()) + 360.0) % 360.0).toFloat()
                    if (abs(nextDeg - lastEmitted) >= 0.8f) {
                        lastEmitted = nextDeg
                        azimuthDeg = nextDeg
                    }
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> sensorManager.registerListener(listener, rotSensor, SensorManager.SENSOR_DELAY_UI)
                    Lifecycle.Event.ON_PAUSE -> sensorManager.unregisterListener(listener)
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                sensorManager.registerListener(listener, rotSensor, SensorManager.SENSOR_DELAY_UI)
            }
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                sensorManager.unregisterListener(listener)
            }
        }
    }

    val diffToQibla = ((qiblaBearing - azimuthDeg + 540f) % 360f) - 180f // -180..+180
    val isAligned = abs(diffToQibla) <= 6f

    // Subtle haptic confirmation once when phone aligns with Kaaba
    LaunchedEffect(isAligned) {
        if (isAligned && !wasAligned && hasSensor) {
            runCatching {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(45L, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(45L)
                }
            }
        }
        wasAligned = isAligned
    }

    // Smooth continuous rotation (avoids 0/360 snap jump)
    var continuousDialAngle by remember { mutableFloatStateOf(-azimuthDeg) }
    LaunchedEffect(azimuthDeg) {
        val target = -azimuthDeg
        var delta = ((target - continuousDialAngle + 540f) % 360f) - 180f
        if (delta < -180f) delta += 360f
        continuousDialAngle += delta
    }
    val smoothDialRotation by animateFloatAsState(
        targetValue = continuousDialAngle,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 220f),
        label = "qibla_dial"
    )

    val alignedAccent = if (isAligned) Color(0xFF4ADE80) else SoftGold

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF061121), EmeraldDark, Color(0xFF132D54))
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                DhikriCompactTopBar(
                    title = "اتجاه القبلة",
                    subtitle = "${selectedCity.nameArabic} • زاوية القبلة ${qiblaBearing.toInt()}°",
                    onBack = onBack
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. Sleek City Selector Strip
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(compassCities, key = { it.nameArabic }) { city ->
                        val selected = city == selectedCity
                        Surface(
                            modifier = Modifier.clickable { selectedCity = city },
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) SoftGold else Color(0xFF152D4F).copy(alpha = 0.85f),
                            border = BorderStroke(
                                1.dp,
                                if (selected) Color(0xFFFFE8A3) else SoftGold.copy(alpha = 0.35f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (selected) {
                                    Icon(
                                        Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = Color(0xFF1F1604),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    text = city.nameArabic,
                                    color = if (selected) Color(0xFF1F1604) else Color.White,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                // 2. Live Alignment Status Pill
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (isAligned) Color(0xFF14472A).copy(alpha = 0.9f) else Color(0xFF122847).copy(alpha = 0.9f),
                    border = BorderStroke(1.2.dp, alignedAccent.copy(alpha = 0.85f)),
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isAligned) Icons.Default.CheckCircle else Icons.Default.Explore,
                            contentDescription = null,
                            tint = alignedAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = when {
                                !hasSensor -> "زاوية القبلة لـ ${selectedCity.nameArabic}: ${qiblaBearing.toInt()}° من الشمال"
                                isAligned -> "أنت متجه الآن نحو القبلة الشريفة"
                                diffToQibla > 0 -> "أدِر الهاتف قليلاً نحو اليمين (${abs(diffToQibla).toInt()}°)"
                                else -> "أدِر الهاتف قليلاً نحو اليسار (${abs(diffToQibla).toInt()}°)"
                            },
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                // 3. Modern Luxury Precision Compass Dial
                Box(
                    modifier = Modifier.size(308.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Fixed top alignment pointer triangle
                    Canvas(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .size(22.dp)
                    ) {
                        val tri = Path().apply {
                            moveTo(size.width / 2f, size.height)
                            lineTo(0f, 0f)
                            lineTo(size.width, 0f)
                            close()
                        }
                        drawPath(tri, color = alignedAccent)
                    }

                    // Outer metallic glowing bezel
                    Box(
                        modifier = Modifier
                            .size(282.dp)
                            .shadow(18.dp, CircleShape)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(Color(0xFF193C6E), Color(0xFF0B1F3B), Color(0xFF06101F))
                                )
                            )
                            .border(
                                width = if (isAligned) 3.dp else 2.dp,
                                brush = Brush.sweepGradient(
                                    listOf(alignedAccent, Color(0xFFFFE8A3), alignedAccent.copy(alpha = 0.55f), alignedAccent)
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Rotating Compass Rose + Degree Ticks + Qibla Needle
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .rotate(smoothDialRotation),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                                val cx = size.width / 2f
                                val cy = size.height / 2f
                                val outerR = size.minDimension / 2f

                                // Inner subtle concentric rings
                                drawCircle(
                                    color = SoftGold.copy(alpha = 0.22f),
                                    radius = outerR * 0.78f,
                                    style = Stroke(width = 1.2.dp.toPx())
                                )
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.08f),
                                    radius = outerR * 0.45f,
                                    style = Stroke(width = 1.dp.toPx())
                                )

                                // 72 precision degree ticks (every 5 degrees)
                                for (deg in 0 until 360 step 5) {
                                    val rad = Math.toRadians(deg.toDouble() - 90.0)
                                    val isMajor = deg % 30 == 0
                                    val isCardinal = deg % 90 == 0
                                    val innerR = outerR * when {
                                        isCardinal -> 0.80f
                                        isMajor -> 0.85f
                                        else -> 0.91f
                                    }
                                    val start = Offset(
                                        cx + (cos(rad) * innerR).toFloat(),
                                        cy + (sin(rad) * innerR).toFloat()
                                    )
                                    val end = Offset(
                                        cx + (cos(rad) * outerR).toFloat(),
                                        cy + (sin(rad) * outerR).toFloat()
                                    )
                                    drawLine(
                                        color = when {
                                            deg == 0 -> Color(0xFFEF4444)
                                            isMajor -> SoftGold
                                            else -> Color.White.copy(alpha = 0.35f)
                                        },
                                        start = start,
                                        end = end,
                                        strokeWidth = if (isCardinal) 3.dp.toPx() else if (isMajor) 2.dp.toPx() else 1.dp.toPx(),
                                        cap = StrokeCap.Round
                                    )
                                }
                            }

                            // Arabic Cardinal Directions (N, S, E, W)
                            Text("ش", color = Color(0xFFF87171), fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, modifier = Modifier.align(Alignment.TopCenter).padding(top = 34.dp))
                            Text("ج", color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 34.dp))
                            Text("ق", color = SoftGold, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 36.dp))
                            Text("غ", color = SoftGold, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.align(Alignment.CenterStart).padding(start = 36.dp))

                            // Qibla Pointer Needle & Glowing Kaaba Medallion (rotated to exact qiblaBearing)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .rotate(qiblaBearing),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val cx = size.width / 2f
                                    val cy = size.height / 2f
                                    val tipY = cy - (size.minDimension * 0.33f)
                                    val tailY = cy + (size.minDimension * 0.24f)

                                    // Golden pointer beam toward Kaaba
                                    val needle = Path().apply {
                                        moveTo(cx, tipY)
                                        lineTo(cx - 8.dp.toPx(), cy)
                                        lineTo(cx, tailY)
                                        lineTo(cx + 8.dp.toPx(), cy)
                                        close()
                                    }
                                    drawPath(
                                        path = needle,
                                        brush = Brush.verticalGradient(
                                            colors = listOf(alignedAccent, SoftGold, Color(0xFF7A5610))
                                        )
                                    )
                                }

                                // Circular Kaaba Medallion at needle tip
                                Image(
                                    painter = painterResource(id = R.drawable.kaaba_icon),
                                    contentDescription = "الكعبة المشرفة",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(top = 18.dp)
                                        .size(44.dp)
                                        .shadow(8.dp, CircleShape)
                                        .clip(CircleShape)
                                        .border(2.dp, alignedAccent, CircleShape)
                                )
                            }
                        }

                        // Center Golden Pivot Jewel
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .shadow(6.dp, CircleShape)
                                .clip(CircleShape)
                                .background(Brush.radialGradient(listOf(Color(0xFFFFF2B8), SoftGold, Color(0xFF8B6510))))
                                .border(2.dp, EmeraldDark, CircleShape)
                        )
                    }
                }

                // 4. Bottom Readout & Calibration Tip Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF122847).copy(alpha = 0.90f),
                    border = BorderStroke(1.dp, SoftGold.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("اتجاه القبلة", color = SoftGold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text("${qiblaBearing.toInt()}°", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                            }
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(32.dp)
                                    .background(Color.White.copy(alpha = 0.18f))
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("اتجاه الهاتف", color = SoftGold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text("${azimuthDeg.toInt()}°", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                        Text(
                            text = "لأفضل دقة، ضع الهاتف بشكل مستوٍ وأبعده عن المعادن والمغناطيس",
                            color = Color.White.copy(alpha = 0.72f),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
