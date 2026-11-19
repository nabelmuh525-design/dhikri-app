package com.dhikri.islamic.ui.screens

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val toneGenerator = remember {
        runCatching { ToneGenerator(AudioManager.STREAM_SYSTEM, 40) }.getOrNull()
    }
    DisposableEffect(Unit) {
        onDispose { toneGenerator?.release() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("السبحة الإلكترونية") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "إضافة تسبيح مخصص")
                    }
                    activePreset?.let { preset ->
                        IconButton(onClick = { viewModel.resetTasbih(preset) }) {
                            Icon(Icons.Default.Refresh, contentDescription = "تصفير العداد")
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(presets, key = { it.id }) { p ->
                        FilterChip(
                            selected = p.id == activePreset?.id,
                            onClick = {
                                selectedId = p.id
                                viewModel.setLastTasbihPresetId(p.id)
                            },
                            label = { Text(p.phraseArabic.take(22)) }
                        )
                    }
                }

                activePreset?.let { preset ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                preset.phraseArabic,
                                style = MaterialTheme.typography.titleLarge,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "المصدر: ${preset.sourceNote}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Main interactive circular Tasbih button
            activePreset?.let { preset ->
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .clip(CircleShape)
                        .background(EmeraldPrimary)
                        .clickable {
                            viewModel.incrementTasbih(preset)
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
                                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 35)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${preset.currentCount}",
                            fontSize = 54.sp,
                            fontWeight = FontWeight.Bold,
                            color = SoftGold
                        )
                        Text(
                            text = "الهدف: ${preset.targetCount}",
                            color = Color.White.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatBadge("إجمالي اليوم", "$todayTotal")
                    StatBadge("إجمالي هذا الذكر", "${preset.totalCount}")
                }
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

@Composable
private fun StatBadge(label: String, value: String) {
    Card(shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}
