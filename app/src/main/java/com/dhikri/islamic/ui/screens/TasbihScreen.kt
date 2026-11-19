package com.dhikri.islamic.ui.screens

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.SoundEffectConstants
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhikri.islamic.R
import com.dhikri.islamic.ui.theme.EmeraldDark
import com.dhikri.islamic.ui.theme.EmeraldPrimary
import com.dhikri.islamic.ui.theme.SoftGold
import com.dhikri.islamic.ui.viewmodel.DhikriViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasbihScreen(
    viewModel: DhikriViewModel,
    onBack: () -> Unit
) {
    val presets by viewModel.tasbihPresets.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val todayTotal by viewModel.todayTasbihTotal.collectAsState()

    var selectedId by remember(settings.lastTasbihPresetId) { mutableIntStateOf(settings.lastTasbihPresetId) }
    val activePreset = presets.firstOrNull { it.id == selectedId } ?: presets.firstOrNull()

    var showAddDialog by remember { mutableStateOf(false) }
    var customPhrase by remember { mutableStateOf("") }
    var customTarget by remember { mutableStateOf("33") }

    val context = LocalContext.current
    val view = LocalView.current
    val toneGenerator = remember {
        runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 85) }.getOrNull()
    }
    DisposableEffect(Unit) {
        onDispose { toneGenerator?.release() }
    }

    fun triggerFeedback() {
        if (settings.vibrationEnabled) {
            @Suppress("DEPRECATION")
            val vib = context.getSystemService(Vibrator::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib?.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vib?.vibrate(25)
            }
        }
        if (settings.soundEnabled) {
            runCatching { toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 45) }
            runCatching { view.playSoundEffect(SoundEffectConstants.CLICK) }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Subtle sunrise mosque ambient backdrop matching 1000000481.png
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
                        colors = listOf(
                            Color(0xD8FAF5E8),
                            Color(0xEBFAF5E8),
                            Color(0xF5F4ECD9)
                        )
                    )
                )
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    title = { Text("عداد التسبيح", fontWeight = FontWeight.Bold, color = EmeraldDark) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = EmeraldDark)
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.setVibrationEnabled(!settings.vibrationEnabled) }) {
                            Icon(
                                if (settings.vibrationEnabled) Icons.Default.Vibration else Icons.Default.DoNotDisturbOn,
                                contentDescription = "الاهتزاز",
                                tint = EmeraldDark
                            )
                        }
                        IconButton(onClick = { viewModel.setSoundEnabled(!settings.soundEnabled) }) {
                            Icon(
                                if (settings.soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                contentDescription = "صوت النقر",
                                tint = EmeraldDark
                            )
                        }
                        IconButton(onClick = { showAddDialog = true }) {
                            Icon(Icons.Default.AddCircleOutline, contentDescription = "إضافة ذكر مخصص", tint = EmeraldDark)
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Emerald & Gold Header Plaque (matching mockup)
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = EmeraldDark,
                    border = BorderStroke(2.dp, SoftGold),
                    shadowElevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = activePreset?.phraseArabic ?: "سبحان الله",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFF2C6),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = activePreset?.sourceNote ?: "سبحان الله .. والحمد لله .. والله أكبر",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.82f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // 2. Large Center Golden-Rimmed Counter + Encouragement Badge
                activePreset?.let { preset ->
                    val ringProgress = (preset.currentCount.toFloat() / preset.targetCount.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                    val goalDone = todayTotal >= settings.dailyTasbihGoal

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        // Main Golden Ring Counter Button
                        Box(
                            modifier = Modifier
                                .size(230.dp)
                                .shadow(12.dp, CircleShape)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFFF3D47E), Color(0xFFB88625), Color(0xFFE7C25D))
                                    )
                                )
                                .clickable {
                                    viewModel.incrementTasbih(preset)
                                    triggerFeedback()
                                }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(Color(0xFFFBF5E6)),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                                    drawArc(
                                        color = Color(0xFFE5D8B8),
                                        startAngle = -90f,
                                        sweepAngle = 360f,
                                        useCenter = false,
                                        style = Stroke(width = 10f, cap = StrokeCap.Round)
                                    )
                                    drawArc(
                                        color = EmeraldPrimary,
                                        startAngle = -90f,
                                        sweepAngle = 360f * ringProgress,
                                        useCenter = false,
                                        style = Stroke(width = 10f, cap = StrokeCap.Round)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${preset.currentCount}",
                                        fontSize = 68.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = EmeraldDark
                                    )
                                    Text(
                                        text = "مرة  •  الهدف ${preset.targetCount}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color(0xFF4A3B1B),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        // Floating Warm-Gold Encouragement Badge (matching "أحسنت — مستمر في الذكر")
                        if (preset.currentCount > 0 || todayTotal > 0) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .offset(x = 2.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFF6D99A),
                                border = BorderStroke(1.2.dp, Color(0xFFC59332)),
                                shadowElevation = 4.dp
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = if (goalDone) "ما شاء الله!" else "أحسنت",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = EmeraldDark
                                    )
                                    Text(
                                        text = if (goalDone) "أتممت هدفك" else "مستمر في\\nالذكر",
                                        fontSize = 11.sp,
                                        lineHeight = 14.sp,
                                        color = Color(0xFF3A2D12),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    // 3. Three Circular Buttons: (+)  (إعادة)  (-)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // (+) Button
                        Box(
                            modifier = Modifier
                                .size(62.dp)
                                .shadow(5.dp, CircleShape)
                                .clip(CircleShape)
                                .background(Color(0xFFF5D89B))
                                .border(2.dp, SoftGold, CircleShape)
                                .clickable {
                                    viewModel.incrementTasbih(preset)
                                    triggerFeedback()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "زيادة", tint = EmeraldDark, modifier = Modifier.size(30.dp))
                        }

                        // Center (إعادة) Reset Button
                        Box(
                            modifier = Modifier
                                .size(78.dp)
                                .shadow(6.dp, CircleShape)
                                .clip(CircleShape)
                                .background(EmeraldDark)
                                .border(2.5.dp, SoftGold, CircleShape)
                                .clickable { viewModel.resetTasbih(preset) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Refresh, contentDescription = "إعادة التصفير", tint = Color.White, modifier = Modifier.size(26.dp))
                                Text("إعادة", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // (-) Button
                        Box(
                            modifier = Modifier
                                .size(62.dp)
                                .shadow(5.dp, CircleShape)
                                .clip(CircleShape)
                                .background(Color(0xFFF5D89B))
                                .border(2.dp, SoftGold, CircleShape)
                                .clickable { viewModel.decrementTasbih(preset) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "إنقاص", tint = EmeraldDark, modifier = Modifier.size(30.dp))
                        }
                    }
                }

                // 4. Below Counter: Dhikr Selector (اختيار الذكر)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFCF4)),
                    border = BorderStroke(1.dp, SoftGold.copy(alpha = 0.5f))
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("اختيار الذكر", fontWeight = FontWeight.Bold, color = EmeraldDark)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(presets, key = { it.id }) { p ->
                                FilterChip(
                                    selected = p.id == activePreset?.id,
                                    onClick = {
                                        selectedId = p.id
                                        viewModel.setLastTasbihPresetId(p.id)
                                    },
                                    label = { Text(p.phraseArabic.take(24)) }
                                )
                            }
                        }
                    }
                }

                // 5. Below Counter: Daily Goal & Progress (الهدف اليومي)
                val dailyProgress = (todayTotal.toFloat() / settings.dailyTasbihGoal.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFCF4)),
                    border = BorderStroke(1.dp, SoftGold.copy(alpha = 0.5f))
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("الهدف اليومي للتسبيح", fontWeight = FontWeight.Bold, color = EmeraldDark)
                            Text("$todayTotal / ${settings.dailyTasbihGoal}", fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                        }
                        LinearProgressIndicator(
                            progress = { dailyProgress },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = EmeraldPrimary,
                            trackColor = Color(0xFFE7DEC8)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(33, 100, 300, 1000).forEach { goal ->
                                FilterChip(
                                    selected = settings.dailyTasbihGoal == goal,
                                    onClick = { viewModel.setDailyTasbihGoal(goal) },
                                    label = { Text("$goal") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        activePreset?.let { preset ->
                            Text(
                                "إجمالي هذا الذكر: ${preset.totalCount} تسبيحة • يتم الحفظ تلقائياً دون إنترنت",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("إضافة تسبيح مخصص") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = customPhrase,
                        onValueChange = { customPhrase = it },
                        label = { Text("نص الذكر") }
                    )
                    OutlinedTextField(
                        value = customTarget,
                        onValueChange = { customTarget = it.filter(Char::isDigit) },
                        label = { Text("العدد المستهدف") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val t = customTarget.toIntOrNull() ?: 33
                    if (customPhrase.isNotBlank()) {
                        viewModel.addCustomTasbih(customPhrase, t)
                        customPhrase = ""
                        showAddDialog = false
                    }
                }) { Text("حفظ") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("إلغاء") }
            }
        )
    }
}
