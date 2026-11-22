package com.dhikri.islamic.ui.screens

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhikri.islamic.ui.components.DhikriCompactTopBar
import com.dhikri.islamic.ui.theme.EmeraldDark
import com.dhikri.islamic.ui.theme.SoftGold
import com.dhikri.islamic.ui.viewmodel.DhikriViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

private object TasbihBeadSoundPlayer {
    private const val SAMPLE_RATE = 22050
    @Volatile private var soundPool: SoundPool? = null
    @Volatile private var soundId: Int = 0
    @Volatile private var isReady: Boolean = false

    fun init(context: Context) {
        if (soundPool != null) return
        synchronized(this) {
            if (soundPool != null) return
            runCatching {
                val wavFile = File(context.cacheDir, "tasbih_bead_click.wav")
                if (!wavFile.exists() || wavFile.length() < 200L) {
                    writeBeadWav(wavFile)
                }
                val sp = SoundPool.Builder()
                    .setMaxStreams(6)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .build()
                sp.setOnLoadCompleteListener { _, _, status ->
                    if (status == 0) isReady = true
                }
                soundId = sp.load(wavFile.absolutePath, 1)
                soundPool = sp
            }
        }
    }

    private fun writeBeadWav(file: File) {
        val durationSec = 0.026
        val numSamples = (SAMPLE_RATE * durationSec).toInt()
        val dataSize = numSamples * 2
        val totalSize = 36 + dataSize
        val header = ByteArray(44)

        fun putIntLE(offset: Int, value: Int) {
            header[offset] = (value and 0xFF).toByte()
            header[offset + 1] = ((value shr 8) and 0xFF).toByte()
            header[offset + 2] = ((value shr 16) and 0xFF).toByte()
            header[offset + 3] = ((value shr 24) and 0xFF).toByte()
        }

        fun putShortLE(offset: Int, value: Int) {
            header[offset] = (value and 0xFF).toByte()
            header[offset + 1] = ((value shr 8) and 0xFF).toByte()
        }

        "RIFF".toByteArray().copyInto(header, 0)
        putIntLE(4, totalSize)
        "WAVEfmt ".toByteArray().copyInto(header, 8)
        putIntLE(16, 16)
        putShortLE(20, 1)
        putShortLE(22, 1)
        putIntLE(24, SAMPLE_RATE)
        putIntLE(28, SAMPLE_RATE * 2)
        putShortLE(32, 2)
        putShortLE(34, 16)
        "data".toByteArray().copyInto(header, 36)
        putIntLE(40, dataSize)

        val pcm = ByteArray(dataSize)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val env1 = exp(-t * 170.0)
            val env2 = exp(-t * 430.0)
            val f1 = 760.0 + 280.0 * exp(-t * 220.0)
            val f2 = 1950.0
            val sample = (0.68 * sin(2.0 * PI * f1 * t) * env1) +
                (0.32 * sin(2.0 * PI * f2 * t) * env2)
            val s = (sample * 22000.0).toInt().coerceIn(-32767, 32767)
            pcm[i * 2] = (s and 0xFF).toByte()
            pcm[i * 2 + 1] = ((s shr 8) and 0xFF).toByte()
        }

        FileOutputStream(file).use { out ->
            out.write(header)
            out.write(pcm)
        }
    }

    fun play() {
        val sp = soundPool ?: return
        if (isReady && soundId != 0) {
            sp.play(soundId, 0.9f, 0.9f, 1, 0, 1.0f)
        }
    }
}

private val ISTIGHFAR_TARGETS = listOf(100, 500, 1000, 5000, 10000, 0)

private fun formatLargeCount(value: Int): String =
    NumberFormat.getIntegerInstance(Locale.US).format(value)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasbihScreen(
    viewModel: DhikriViewModel,
    onBack: () -> Unit
) {
    val presets by viewModel.tasbihPresets.collectAsState()
    val todayTotal by viewModel.todayTasbihTotal.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val bgGradient = if (isDarkTheme) {
        listOf(Color(0xFF071428), EmeraldDark, Color(0xFF132D54))
    } else {
        listOf(Color(0xFFF8F4EC), Color(0xFFF0EADF), Color(0xFFE7DFD0))
    }
    val cardBg = if (isDarkTheme) Color(0xFF122847).copy(alpha = 0.88f) else Color.White.copy(alpha = 0.96f)
    val primaryText = if (isDarkTheme) Color.White else EmeraldDark
    val goldAccent = if (isDarkTheme) SoftGold else Color(0xFF9C7826)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val istighfarPrefs = remember { context.getSharedPreferences("dhikri_istighfar_prefs", Context.MODE_PRIVATE) }

    // Pre-initialize SoundPool & Vibrator once off the tap hot-path for zero-lag rapid tapping
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) { TasbihBeadSoundPlayer.init(context.applicationContext) }
    }
    val vibrator = remember {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
        }.getOrNull()
    }
    val tapEffect = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            VibrationEffect.createOneShot(18L, VibrationEffect.DEFAULT_AMPLITUDE)
        } else null
    }
    val roundEffect = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            VibrationEffect.createOneShot(75L, VibrationEffect.DEFAULT_AMPLITUDE)
        } else null
    }

    var isIstighfarMode by remember { mutableStateOf(false) }
    var istighfarCount by remember { mutableIntStateOf(istighfarPrefs.getInt("count", 0)) }
    var istighfarTarget by remember { mutableIntStateOf(istighfarPrefs.getInt("target", 1000)) }

    var selectedPresetId by remember(settings.lastTasbihPresetId) { mutableIntStateOf(settings.lastTasbihPresetId) }
    val activePreset = presets.firstOrNull { it.id == selectedPresetId } ?: presets.firstOrNull()

    var showAddDialog by remember { mutableStateOf(false) }
    var showGoalDialog by remember { mutableStateOf(false) }
    var customPhrase by remember { mutableStateOf("") }
    var customTarget by remember { mutableStateOf("33") }
    var goalInput by remember(settings.dailyTasbihGoal) { mutableStateOf(settings.dailyTasbihGoal.toString()) }
    var isPressed by remember { mutableStateOf(false) }
    var pressResetJob by remember { mutableStateOf<Job?>(null) }

    fun saveIstighfar(count: Int = istighfarCount, target: Int = istighfarTarget) {
        istighfarPrefs.edit()
            .putInt("count", count)
            .putInt("target", target)
            .apply()
    }

    BackHandler(enabled = showAddDialog || showGoalDialog || isIstighfarMode) {
        when {
            showAddDialog || showGoalDialog -> {
                showAddDialog = false
                showGoalDialog = false
            }
            isIstighfarMode -> isIstighfarMode = false
        }
    }

    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessHigh),
        label = "tasbih_scale"
    )

    val normalTarget = (activePreset?.targetCount ?: 33).coerceAtLeast(1)
    val normalCurrent = activePreset?.currentCount ?: 0

    val activeCount = if (isIstighfarMode) istighfarCount else normalCurrent
    val activeTarget = if (isIstighfarMode) istighfarTarget else normalTarget

    val cycleProgress = if (isIstighfarMode && activeTarget == 0) {
        if (activeCount > 0) 0.85f else 0f
    } else {
        val safeTarget = activeTarget.coerceAtLeast(1)
        ((activeCount % safeTarget).let { if (it == 0 && activeCount > 0) safeTarget else it }).toFloat() / safeTarget.toFloat()
    }
    val animatedProgress by animateFloatAsState(
        targetValue = cycleProgress.coerceIn(0f, 1f),
        animationSpec = tween(90),
        label = "tasbih_ring"
    )
    val completedRounds = if (activeTarget > 0) activeCount / activeTarget else 0

    fun triggerFeedback(isRoundComplete: Boolean) {
        if (settings.soundEnabled) {
            TasbihBeadSoundPlayer.play()
        }
        if (settings.vibrationEnabled && vibrator != null) {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = if (isRoundComplete) roundEffect else tapEffect
                    if (effect != null) vibrator.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(if (isRoundComplete) 75L else 18L)
                }
            }
        }
    }

    fun performIncrement() {
        isPressed = true
        pressResetJob?.cancel()
        pressResetJob = scope.launch {
            delay(75)
            isPressed = false
        }

        if (isIstighfarMode) {
            val prevRounds = if (istighfarTarget > 0) istighfarCount / istighfarTarget else 0
            val next = istighfarCount + 1
            istighfarCount = next
            saveIstighfar(count = next)
            val completedNow = istighfarTarget > 0 && (next / istighfarTarget) > prevRounds
            triggerFeedback(completedNow)
        } else {
            activePreset?.let { p ->
                val nextCount = p.currentCount + 1
                triggerFeedback(nextCount % normalTarget == 0)
                viewModel.incrementTasbih(p)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = bgGradient))
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                DhikriCompactTopBar(
                    title = "المسبحة الإلكترونية",
                    onBack = onBack,
                    actions = {
                        IconButton(onClick = { viewModel.setVibrationEnabled(!settings.vibrationEnabled) }) {
                            Icon(
                                if (settings.vibrationEnabled) Icons.Default.Vibration else Icons.Default.DoNotDisturbOn,
                                contentDescription = "الاهتزاز",
                                tint = SoftGold
                            )
                        }
                        IconButton(onClick = { viewModel.setSoundEnabled(!settings.soundEnabled) }) {
                            Icon(
                                if (settings.soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                contentDescription = "صوت خرز السبحة",
                                tint = SoftGold
                            )
                        }
                        if (!isIstighfarMode) {
                            IconButton(onClick = { showAddDialog = true }) {
                                Icon(Icons.Default.AddCircleOutline, contentDescription = "إضافة ذكر", tint = SoftGold)
                            }
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
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Dedicated High-Count Istighfar Toggle Button
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isIstighfarMode = !isIstighfarMode },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isIstighfarMode) SoftGold else cardBg,
                    border = BorderStroke(
                        1.4.dp,
                        if (isIstighfarMode) Color(0xFFFFE599) else SoftGold.copy(alpha = 0.72f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = if (isIstighfarMode) Color(0xFF1F1604) else SoftGold,
                                modifier = Modifier.size(19.dp)
                            )
                            Text(
                                text = "الاستغفار (أعداد كبيرة)",
                                color = if (isIstighfarMode) Color(0xFF1F1604) else primaryText,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = if (isIstighfarMode) Color(0xFF1F1604).copy(alpha = 0.16f) else SoftGold.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = if (isIstighfarMode) "مُفعّل • عودة للتسبيح" else "فتح الاستغفار",
                                color = if (isIstighfarMode) Color(0xFF1F1604) else goldAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // Presets Row (Large Targets in Istighfar mode OR normal Tasbih presets)
                if (isIstighfarMode) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(ISTIGHFAR_TARGETS, key = { it }) { targetOption ->
                            val selected = targetOption == istighfarTarget
                            val label = if (targetOption == 0) "مفتوح ∞" else formatLargeCount(targetOption)
                            Surface(
                                modifier = Modifier.clickable {
                                    istighfarTarget = targetOption
                                    saveIstighfar(target = targetOption)
                                },
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) SoftGold else cardBg,
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) Color(0xFFFFE599) else SoftGold.copy(alpha = 0.45f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "الهدف",
                                        color = if (selected) Color(0xFF1F1604) else primaryText,
                                        fontSize = 12.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                    Surface(
                                        shape = CircleShape,
                                        color = if (selected) Color(0xFF1F1604).copy(alpha = 0.16f) else SoftGold.copy(alpha = 0.22f)
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (selected) Color(0xFF1F1604) else goldAccent,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(presets, key = { it.id }) { preset ->
                            val selected = preset.id == activePreset?.id
                            Surface(
                                modifier = Modifier.clickable {
                                    selectedPresetId = preset.id
                                    viewModel.setLastTasbihPresetId(preset.id)
                                },
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) SoftGold else cardBg,
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) Color(0xFFFFE599) else SoftGold.copy(alpha = 0.45f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = preset.phraseArabic,
                                        color = if (selected) Color(0xFF1F1604) else primaryText,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Surface(
                                        shape = CircleShape,
                                        color = if (selected) Color(0xFF1F1604).copy(alpha = 0.16f) else SoftGold.copy(alpha = 0.22f)
                                    ) {
                                        Text(
                                            text = "${preset.targetCount}",
                                            color = if (selected) Color(0xFF1F1604) else goldAccent,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Active Phrase Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, SoftGold.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = if (isIstighfarMode) "أستغفر الله العظيم وأتوب إليه" else (activePreset?.phraseArabic ?: "سبحان الله"),
                            color = primaryText,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = when {
                                isIstighfarMode && activeTarget == 0 -> "عداد مفتوح بلا حد"
                                isIstighfarMode -> "الهدف: ${formatLargeCount(activeTarget)}  •  الدورات المكتملة: $completedRounds"
                                else -> "الهدف: $normalTarget  •  الدورات المكتملة: $completedRounds"
                            },
                            color = goldAccent,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Central Circular Progress + Tap Orb
                Box(
                    modifier = Modifier
                        .size(236.dp)
                        .scale(buttonScale)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { performIncrement() },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                        drawArc(
                            color = if (isDarkTheme) Color.White.copy(alpha = 0.12f) else EmeraldDark.copy(alpha = 0.12f),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 13.dp.toPx(), cap = StrokeCap.Round)
                        )
                        drawArc(
                            brush = Brush.sweepGradient(listOf(SoftGold, Color(0xFFFFE599), Color(0xFFB88A1E), SoftGold)),
                            startAngle = -90f,
                            sweepAngle = 360f * animatedProgress,
                            useCenter = false,
                            style = Stroke(width = 13.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(188.dp)
                            .shadow(14.dp, CircleShape)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(Color(0xFF1B4173), Color(0xFF0C2140), Color(0xFF071326))
                                )
                            )
                            .border(2.dp, SoftGold.copy(alpha = 0.78f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = formatLargeCount(activeCount),
                                color = Color.White,
                                fontSize = if (activeCount >= 10_000) 38.sp else 50.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = if (isIstighfarMode) "اضغط للاستغفار" else "اضغط للتسبيح",
                                color = SoftGold,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }


                // Glass Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isIstighfarMode) {
                        TasbihGlassButton(Icons.Default.Remove, "إنقاص", cardBg, primaryText, goldAccent) {
                            activePreset?.let { viewModel.decrementTasbih(it) }
                        }
                    }
                    TasbihGlassButton(Icons.Default.Refresh, "تصفير", cardBg, primaryText, goldAccent, highlighted = true) {
                        if (isIstighfarMode) {
                            istighfarCount = 0
                            saveIstighfar(count = 0)
                        } else {
                            activePreset?.let { viewModel.resetTasbih(it) }
                        }
                    }
                    if (!isIstighfarMode) {
                        TasbihGlassButton(Icons.Default.Add, "زيادة", cardBg, primaryText, goldAccent) {
                            performIncrement()
                        }
                    }
                }

                val goalProgress = (todayTotal.toFloat() / settings.dailyTasbihGoal.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { showGoalDialog = true },
                    shape = RoundedCornerShape(18.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, SoftGold.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isIstighfarMode) "مجموع الاستغفار: ${formatLargeCount(istighfarCount)}" else "مجموع اليوم: $todayTotal تسبيحة",
                                color = primaryText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            )
                            Text("الهدف اليومي: ${settings.dailyTasbihGoal} ✎", color = goldAccent, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                        LinearProgressIndicator(
                            progress = { goalProgress },
                            modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
                            color = SoftGold,
                            trackColor = if (isDarkTheme) Color.White.copy(alpha = 0.16f) else EmeraldDark.copy(alpha = 0.14f)
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("إضافة ذكر مخصص", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = customPhrase, onValueChange = { customPhrase = it }, label = { Text("صيغة الذكر") }, singleLine = true)
                    OutlinedTextField(value = customTarget, onValueChange = { customTarget = it.filter { ch -> ch.isDigit() } }, label = { Text("العدد المستهدف (مثلاً 33 أو 100)") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val t = customTarget.toIntOrNull()?.coerceIn(1, 10_000) ?: 33
                    if (customPhrase.isNotBlank()) {
                        viewModel.addCustomTasbih(customPhrase.trim(), t)
                        customPhrase = ""
                        showAddDialog = false
                    }
                }) { Text("إضافة") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("إلغاء") } }
        )
    }

    if (showGoalDialog) {
        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            title = { Text("تعديل الهدف اليومي للتسبيح", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(value = goalInput, onValueChange = { goalInput = it.filter { ch -> ch.isDigit() } }, label = { Text("عدد التسبيحات اليومي") }, singleLine = true)
            },
            confirmButton = {
                Button(onClick = {
                    goalInput.toIntOrNull()?.let { viewModel.setDailyTasbihGoal(it) }
                    showGoalDialog = false
                }) { Text("حفظ") }
            },
            dismissButton = { TextButton(onClick = { showGoalDialog = false }) { Text("إلغاء") } }
        )
    }
}

@Composable
private fun TasbihGlassButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    cardBg: Color,
    textColor: Color,
    iconTint: Color,
    highlighted: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.width(96.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (highlighted) SoftGold.copy(alpha = 0.22f) else cardBg,
        border = BorderStroke(1.dp, SoftGold.copy(alpha = if (highlighted) 0.85f else 0.42f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = label, tint = iconTint, modifier = Modifier.size(18.dp))
            Text(label, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}
