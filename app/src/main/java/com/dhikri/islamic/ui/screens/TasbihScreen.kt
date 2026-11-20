package com.dhikri.islamic.ui.screens

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.BackHandler
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

private object TasbihBeadSoundPlayer {
    private const val SAMPLE_RATE = 22050
    private val beadSamples: ShortArray by lazy { synthesizeBeadClick() }

    private fun synthesizeBeadClick(): ShortArray {
        val durationSec = 0.028
        val count = (SAMPLE_RATE * durationSec).toInt()
        val buf = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toDouble() / SAMPLE_RATE
            val env1 = exp(-t * 165.0)
            val env2 = exp(-t * 420.0)
            val f1 = 760.0 + 280.0 * exp(-t * 220.0)
            val f2 = 1950.0
            val sample = (0.68 * sin(2.0 * PI * f1 * t) * env1) +
                (0.32 * sin(2.0 * PI * f2 * t) * env2)
            buf[i] = (sample * 22000.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        return buf
    }

    fun play() {
        runCatching {
            val minBuf = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufBytes = maxOf(minBuf, beadSamples.size * 2)
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufBytes)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(beadSamples, 0, beadSamples.size)
            track.play()
            Thread {
                runCatching {
                    Thread.sleep(60)
                    track.stop()
                    track.release()
                }
            }.start()
        }
    }
}

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

    var selectedPresetId by remember(settings.lastTasbihPresetId) { mutableIntStateOf(settings.lastTasbihPresetId) }
    val activePreset = presets.firstOrNull { it.id == selectedPresetId } ?: presets.firstOrNull()

    var showAddDialog by remember { mutableStateOf(false) }
    var showGoalDialog by remember { mutableStateOf(false) }
    var customPhrase by remember { mutableStateOf("") }
    var customTarget by remember { mutableStateOf("33") }
    var goalInput by remember(settings.dailyTasbihGoal) { mutableStateOf(settings.dailyTasbihGoal.toString()) }
    var isPressed by remember { mutableStateOf(false) }

    BackHandler(enabled = showAddDialog || showGoalDialog) {
        showAddDialog = false
        showGoalDialog = false
    }

    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 500f),
        label = "tasbih_scale"
    )

    val target = (activePreset?.targetCount ?: 33).coerceAtLeast(1)
    val current = activePreset?.currentCount ?: 0
    val cycleProgress = ((current % target).let { if (it == 0 && current > 0) target else it }).toFloat() / target.toFloat()
    val animatedProgress by animateFloatAsState(
        targetValue = cycleProgress.coerceIn(0f, 1f),
        animationSpec = tween(220),
        label = "tasbih_ring"
    )
    val completedRounds = current / target

    fun triggerFeedback(isRoundComplete: Boolean) {
        if (settings.soundEnabled) {
            scope.launch(Dispatchers.IO) { TasbihBeadSoundPlayer.play() }
        }
        if (settings.vibrationEnabled) {
            runCatching {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                }
                val duration = if (isRoundComplete) 85L else 22L
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(duration)
                }
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
                        IconButton(onClick = { showAddDialog = true }) {
                            Icon(Icons.Default.AddCircleOutline, contentDescription = "إضافة ذكر", tint = SoftGold)
                        }
                    }
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
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
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

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, SoftGold.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = activePreset?.phraseArabic ?: "سبحان الله",
                            color = primaryText,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "الهدف: $target  •  الدورات المكتملة: $completedRounds",
                            color = goldAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(258.dp)
                        .scale(buttonScale)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            activePreset?.let { p ->
                                isPressed = true
                                val nextCount = p.currentCount + 1
                                triggerFeedback(nextCount % target == 0)
                                viewModel.incrementTasbih(p)
                                scope.launch {
                                    delay(95)
                                    isPressed = false
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                        drawArc(
                            color = if (isDarkTheme) Color.White.copy(alpha = 0.12f) else EmeraldDark.copy(alpha = 0.12f),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                        )
                        drawArc(
                            brush = Brush.sweepGradient(listOf(SoftGold, Color(0xFFFFE599), Color(0xFFB88A1E), SoftGold)),
                            startAngle = -90f,
                            sweepAngle = 360f * animatedProgress,
                            useCenter = false,
                            style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(204.dp)
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
                            Text(text = "$current", color = Color.White, fontSize = 56.sp, fontWeight = FontWeight.ExtraBold)
                            Text(text = "اضغط للتسبيح", color = SoftGold, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TasbihGlassButton(Icons.Default.Remove, "إنقاص", cardBg, primaryText, goldAccent) {
                        activePreset?.let { viewModel.decrementTasbih(it) }
                    }
                    TasbihGlassButton(Icons.Default.Refresh, "تصفير", cardBg, primaryText, goldAccent, highlighted = true) {
                        activePreset?.let { viewModel.resetTasbih(it) }
                    }
                    TasbihGlassButton(Icons.Default.Add, "زيادة", cardBg, primaryText, goldAccent) {
                        activePreset?.let { p ->
                            triggerFeedback((p.currentCount + 1) % target == 0)
                            viewModel.incrementTasbih(p)
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
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("مجموع اليوم: $todayTotal تسبيحة", color = primaryText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
