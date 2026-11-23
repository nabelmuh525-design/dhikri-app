package com.dhikri.islamic.ui.screens

import android.content.Context
import android.hardware.GeomagneticField
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.dhikri.islamic.R
import com.dhikri.islamic.domain.util.PrayerCity
import com.dhikri.islamic.domain.util.PrayerTimesCalculator
import com.dhikri.islamic.domain.util.QiblaMath
import com.dhikri.islamic.ui.components.DhikriCompactTopBar
import com.dhikri.islamic.ui.theme.EmeraldDark
import com.dhikri.islamic.ui.theme.SoftGold
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QiblaCompassScreen(onBack: (() -> Unit)? = null) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cities = remember { PrayerTimesCalculator.cities }
    var selectedCity by remember { mutableStateOf(cities.first()) }
    val activeLat = selectedCity.latitude
    val activeLon = selectedCity.longitude

    // Normalized true geographic azimuth [0, 360) for readouts & alignment check
    var trueAzimuthDeg by remember { mutableFloatStateOf(0f) }
    // Continuous unwrapped azimuth for smooth rotation without 359° -> 0° spin glitch
    var continuousAzimuthDeg by remember { mutableFloatStateOf(0f) }
    var hasSensor by remember { mutableStateOf(true) }
    var wasAligned by remember { mutableStateOf(false) }

    // True geographic Qibla bearing & distance using verified WGS-84 Kaaba coordinates
    val qiblaBearing = remember(activeLat, activeLon) {
        QiblaMath.calculateQiblaBearing(activeLat, activeLon).toFloat()
    }
    val distanceKm = remember(activeLat, activeLon) {
        QiblaMath.distanceToKaabaKm(activeLat, activeLon).roundToInt()
    }
    // Magnetic declination (degrees East positive) to convert Magnetic North -> True Geographic North
    val magneticDeclination = remember(activeLat, activeLon) {
        runCatching {
            GeomagneticField(
                activeLat.toFloat(),
                activeLon.toFloat(),
                0f,
                System.currentTimeMillis()
            ).declination
        }.getOrDefault(0f)
    }

    // Lifecycle-bound compass sensor with ROTATION_VECTOR + ACCEL/MAG fallback, tilt compensation & continuous unwrapping
    DisposableEffect(context, lifecycleOwner, magneticDeclination) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rotSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        val useRotVector = rotSensor != null

        if (!useRotVector && (accelSensor == null || magSensor == null)) {
            hasSensor = false
            onDispose { }
        } else {
            var smoothedAzimuth = -1f
            var unwrappedAzimuth = 0f
            val gravity = FloatArray(3)
            val geomagnetic = FloatArray(3)
            var hasGravity = false
            var hasGeomagnetic = false

            fun publishTrueAzimuth(trueDeg: Float) {
                if (smoothedAzimuth < 0f) {
                    smoothedAzimuth = trueDeg
                    unwrappedAzimuth = trueDeg
                    trueAzimuthDeg = trueDeg
                    continuousAzimuthDeg = trueDeg
                    return
                }
                val nextSmoothed = QiblaMath.smoothAzimuth(smoothedAzimuth, trueDeg, alpha = 0.15f)
                val step = QiblaMath.shortestAngleDelta(nextSmoothed, smoothedAzimuth)
                if (abs(step) >= 0.35f) {
                    smoothedAzimuth = nextSmoothed
                    unwrappedAzimuth += step
                    trueAzimuthDeg = nextSmoothed
                    continuousAzimuthDeg = unwrappedAzimuth
                }
            }

            val listener = object : SensorEventListener {
                val rotMatrix = FloatArray(9)
                val remappedMatrix = FloatArray(9)
                val orient = FloatArray(3)

                override fun onSensorChanged(event: SensorEvent) {
                    val gotMatrix = when (event.sensor.type) {
                        Sensor.TYPE_ROTATION_VECTOR -> {
                            // Truncate to 4 floats for ColorOS/Xiaomi/Samsung HAL compatibility
                            val safeVec = if (event.values.size > 4) {
                                floatArrayOf(event.values[0], event.values[1], event.values[2], event.values[3])
                            } else {
                                event.values
                            }
                            SensorManager.getRotationMatrixFromVector(rotMatrix, safeVec)
                            true
                        }
                        Sensor.TYPE_ACCELEROMETER -> {
                            val a = 0.2f
                            for (i in 0..2) {
                                gravity[i] = if (hasGravity) gravity[i] + a * (event.values[i] - gravity[i]) else event.values[i]
                            }
                            hasGravity = true
                            hasGeomagnetic && SensorManager.getRotationMatrix(rotMatrix, null, gravity, geomagnetic)
                        }
                        Sensor.TYPE_MAGNETIC_FIELD -> {
                            val a = 0.2f
                            for (i in 0..2) {
                                geomagnetic[i] = if (hasGeomagnetic) geomagnetic[i] + a * (event.values[i] - geomagnetic[i]) else event.values[i]
                            }
                            hasGeomagnetic = true
                            hasGravity && SensorManager.getRotationMatrix(rotMatrix, null, gravity, geomagnetic)
                        }
                        else -> false
                    }

                    if (!gotMatrix) return

                    SensorManager.getOrientation(rotMatrix, orient)
                    val pitchDeg = Math.toDegrees(orient[1].toDouble())
                    // If user holds the phone tilted upright (> 55°), remap Z axis so heading stays accurate
                    val activeMatrix = if (abs(pitchDeg) > 55.0) {
                        SensorManager.remapCoordinateSystem(
                            rotMatrix,
                            SensorManager.AXIS_X,
                            SensorManager.AXIS_Z,
                            remappedMatrix
                        )
                        remappedMatrix
                    } else {
                        rotMatrix
                    }
                    SensorManager.getOrientation(activeMatrix, orient)
                    val magneticDeg = ((Math.toDegrees(orient[0].toDouble()) + 360.0) % 360.0).toFloat()
                    val trueDeg = QiblaMath.applyMagneticDeclination(magneticDeg, magneticDeclination)
                    publishTrueAzimuth(trueDeg)
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }

            fun registerSensors() {
                if (useRotVector) {
                    sensorManager.registerListener(listener, rotSensor, SensorManager.SENSOR_DELAY_GAME)
                } else {
                    sensorManager.registerListener(listener, accelSensor, SensorManager.SENSOR_DELAY_GAME)
                    sensorManager.registerListener(listener, magSensor, SensorManager.SENSOR_DELAY_GAME)
                }
            }

            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> registerSensors()
                    Lifecycle.Event.ON_PAUSE -> sensorManager.unregisterListener(listener)
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                registerSensors()
            }
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                sensorManager.unregisterListener(listener)
            }
        }
    }

    val diffToQibla = QiblaMath.shortestAngleDelta(qiblaBearing, trueAzimuthDeg)
    val isAligned = abs(diffToQibla) <= 5f

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

    // Continuous rotation values prevent 359° -> 0° reverse spinning
    val dialRotation by animateFloatAsState(
        targetValue = -continuousAzimuthDeg,
        animationSpec = spring(dampingRatio = 0.88f, stiffness = 220f),
        label = "qibla_dial"
    )
    val needleRotation by animateFloatAsState(
        targetValue = qiblaBearing - continuousAzimuthDeg,
        animationSpec = spring(dampingRatio = 0.88f, stiffness = 220f),
        label = "qibla_needle"
    )

    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val bgGradient = if (isDarkTheme) {
        listOf(Color(0xFF061124), EmeraldDark, Color(0xFF10284B))
    } else {
        listOf(Color(0xFFF8F4EC), Color(0xFFF0EADF), Color(0xFFE7DFD0))
    }
    val cardBg = if (isDarkTheme) Color(0xFF122847).copy(alpha = 0.9f) else Color.White.copy(alpha = 0.96f)
    val primaryText = if (isDarkTheme) Color.White else EmeraldDark
    val secondaryText = if (isDarkTheme) Color.White.copy(alpha = 0.72f) else EmeraldDark.copy(alpha = 0.72f)
    val goldAccent = if (isDarkTheme) SoftGold else Color(0xFF9C7826)
    val activeLocationLabel = selectedCity.nameArabic

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(bgGradient))
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                DhikriCompactTopBar(
                    title = "اتجاه القبلة",
                    subtitle = "$activeLocationLabel • ${qiblaBearing.roundToInt()}° • $distanceKm كم عن الكعبة",
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
                // Unified City Selector Strip
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(cities, key = { it.nameArabic }) { city: PrayerCity ->
                        val isSelected = city.nameArabic == selectedCity.nameArabic
                        Surface(
                            modifier = Modifier.clickable {
                                selectedCity = city
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) SoftGold else cardBg,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFFFFE599) else SoftGold.copy(alpha = 0.45f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (isSelected) {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF1F1604), modifier = Modifier.size(14.dp))
                                }
                                Text(
                                    text = city.nameArabic,
                                    color = if (isSelected) Color(0xFF1F1604) else primaryText,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                // Alignment Status Pill
                val statusText = when {
                    !hasSensor -> "لا يتوفر حساس بوصلة — اتجه نحو الزاوية ${qiblaBearing.roundToInt()}° من الشمال"
                    isAligned -> "أنت الآن باتجاه القبلة تماماً"
                    diffToQibla > 0 -> "أدِر الهاتف قليلاً نحو اليمين (${abs(diffToQibla).roundToInt()}°)"
                    else -> "أدِر الهاتف قليلاً نحو اليسار (${abs(diffToQibla).roundToInt()}°)"
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isAligned && hasSensor) Color(0xFF135E3A) else cardBg,
                    border = BorderStroke(1.2.dp, if (isAligned && hasSensor) Color(0xFF5BE39D) else SoftGold.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isAligned && hasSensor) Icons.Default.CheckCircle else Icons.Default.Explore,
                            contentDescription = null,
                            tint = if (isAligned && hasSensor) Color(0xFF6EF5AE) else goldAccent,
                            modifier = Modifier.size(19.dp)
                        )
                        Text(
                            text = statusText,
                            color = if (isAligned && hasSensor) Color.White else primaryText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Luxury Compass Dial — MUST be LTR so East (ق) is physically RIGHT (90°) and West (غ) is LEFT (270°)
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Box(
                        modifier = Modifier.size(286.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val outerR = size.minDimension / 2f - 10.dp.toPx()

                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0xFF153461), Color(0xFF0A1D38), Color(0xFF050F1F)),
                                    center = Offset(cx, cy),
                                    radius = outerR
                                ),
                                radius = outerR
                            )
                            drawCircle(
                                color = if (isAligned && hasSensor) Color(0xFF5BE39D) else SoftGold,
                                radius = outerR,
                                style = Stroke(width = 3.dp.toPx())
                            )
                            drawCircle(
                                color = SoftGold.copy(alpha = 0.28f),
                                radius = outerR * 0.72f,
                                style = Stroke(width = 1.dp.toPx())
                            )
                        }

                        // Rotating Degree Ring & Cardinal Directions (True Geographic North)
                        Box(
                            modifier = Modifier
                                .size(262.dp)
                                .rotate(dialRotation),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val cx = size.width / 2f
                                val cy = size.height / 2f
                                val r = size.minDimension / 2f - 6.dp.toPx()
                                for (deg in 0 until 360 step 5) {
                                    val rad = Math.toRadians((deg - 90).toDouble())
                                    val isMajor = deg % 90 == 0
                                    val isMid = deg % 30 == 0
                                    val len = when {
                                        isMajor -> 16.dp.toPx()
                                        isMid -> 11.dp.toPx()
                                        else -> 6.dp.toPx()
                                    }
                                    val start = Offset(cx + (r - len) * cos(rad).toFloat(), cy + (r - len) * sin(rad).toFloat())
                                    val end = Offset(cx + r * cos(rad).toFloat(), cy + r * sin(rad).toFloat())
                                    drawLine(
                                        color = when {
                                            deg == 0 -> Color(0xFFFF6B6B)
                                            isMajor -> SoftGold
                                            isMid -> SoftGold.copy(alpha = 0.65f)
                                            else -> Color.White.copy(alpha = 0.25f)
                                        },
                                        start = start,
                                        end = end,
                                        strokeWidth = if (isMajor) 2.5.dp.toPx() else 1.2.dp.toPx(),
                                        cap = StrokeCap.Round
                                    )
                                }
                            }

                            // Physically verified in LTR: North=Top, South=Bottom, East(ق)=Right, West(غ)=Left
                            Text("ش", color = Color(0xFFFF6B6B), fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, modifier = Modifier.align(Alignment.TopCenter).padding(top = 24.dp))
                            Text("ج", color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp))
                            Text("ق", color = SoftGold, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 24.dp))
                            Text("غ", color = SoftGold, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.align(Alignment.CenterStart).padding(start = 24.dp))
                        }

                        // Rotating Qibla Needle + Kaaba Badge
                        Box(
                            modifier = Modifier
                                .size(236.dp)
                                .rotate(needleRotation),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val cx = size.width / 2f
                                val cy = size.height / 2f
                                val tipY = 30.dp.toPx()
                                val path = Path().apply {
                                    moveTo(cx, tipY)
                                    lineTo(cx - 10.dp.toPx(), cy)
                                    lineTo(cx + 10.dp.toPx(), cy)
                                    close()
                                }
                                drawPath(
                                    path = path,
                                    brush = Brush.verticalGradient(
                                        listOf(
                                            if (isAligned && hasSensor) Color(0xFF5BE39D) else Color(0xFFFFE599),
                                            if (isAligned && hasSensor) Color(0xFF1F8A56) else SoftGold
                                        )
                                    )
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 2.dp)
                                    .size(38.dp)
                                    .shadow(6.dp, CircleShape)
                                    .clip(CircleShape)
                                    .background(EmeraldDark)
                                    .border(1.5.dp, if (isAligned && hasSensor) Color(0xFF5BE39D) else SoftGold, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.kaaba_icon),
                                    contentDescription = "الكعبة المشرفة",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(SoftGold)
                                    .border(2.dp, EmeraldDark, CircleShape)
                            )
                        }
                    }
                }

                // Bottom Telemetry Card (True North corrected)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, SoftGold.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("اتجاه القبلة", color = goldAccent, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                Text("${qiblaBearing.roundToInt()}°", color = primaryText, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("الشمال الحقيقي", color = goldAccent, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                Text("${trueAzimuthDeg.roundToInt()}°", color = primaryText, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("الانحراف", color = goldAccent, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                Text("${"%+.1f".format(magneticDeclination)}°", color = primaryText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text(
                            text = "مُصحّح للشمال الجغرافي الحقيقي (GeomagneticField) • ضع الهاتف بشكل مستوٍ بعيداً عن المعادن",
                            color = secondaryText,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
