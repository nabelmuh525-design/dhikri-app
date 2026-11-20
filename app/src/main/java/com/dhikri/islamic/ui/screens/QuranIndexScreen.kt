package com.dhikri.islamic.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhikri.islamic.ui.components.DhikriCompactTopBar
import com.dhikri.islamic.ui.theme.EmeraldDark
import com.dhikri.islamic.ui.theme.SoftGold
import com.dhikri.islamic.ui.viewmodel.DhikriViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranIndexScreen(
    viewModel: DhikriViewModel,
    onOpenPage: (Int) -> Unit,
    onOpenSearch: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    val surahs by viewModel.surahs.collectAsState()
    val juzs by viewModel.juzs.collectAsState()
    val hizbs by viewModel.hizbQuarters.collectAsState()
    val sajdahs by viewModel.sajdahs.collectAsState()
    val lastRead by viewModel.lastRead.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showJumpDialog by remember { mutableStateOf(false) }
    var jumpInput by remember { mutableStateOf("") }

    val tabTitles = listOf("السور (114)", "الأجزاء (30)", "الأحزاب", "السجدات")

    BackHandler(enabled = showJumpDialog) { showJumpDialog = false }

    Scaffold(
        topBar = {
            DhikriCompactTopBar(
                title = "القرآن الكريم",
                onBack = onBack,
                actions = {
                    TextButton(onClick = { showJumpDialog = true }) {
                        Text("انتقال لصفحة", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                    IconButton(onClick = onOpenSearch) {
                        Icon(Icons.Default.Search, contentDescription = "بحث في القرآن", tint = SoftGold)
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            lastRead?.let { lr ->
                val surahName = surahs.firstOrNull { it.id == lr.surahId }?.nameArabic ?: "الفاتحة"
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .clickable { onOpenPage(lr.pageNumber) },
                    shape = RoundedCornerShape(16.dp),
                    color = EmeraldDark,
                    border = BorderStroke(1.dp, SoftGold.copy(alpha = 0.75f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Bookmark, contentDescription = null, tint = SoftGold)
                        Column(Modifier.weight(1f)) {
                            Text("متابعة القراءة", color = SoftGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("سورة $surahName • صفحة ${lr.pageNumber} (آية ${lr.ayahNumber})", color = Color.White, fontSize = 13.sp)
                        }
                        Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = SoftGold)
                    }
                }
            }

            ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 12.dp) {
                tabTitles.forEachIndexed { idx, title ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        text = { Text(title, fontWeight = if (selectedTab == idx) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            when (selectedTab) {
                0 -> LazyColumn(
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(surahs, key = { it.id }) { surah ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onOpenPage(surah.startPage) },
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(0.7.dp, SoftGold.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldDark),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("${surah.id}", color = SoftGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                Column(Modifier.weight(1f)) {
                                    Text("سورة ${surah.nameArabic}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(
                                        "${surah.revelationType} • ${surah.ayahCount} آية • صفحة ${surah.startPage}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = SoftGold)
                            }
                        }
                    }
                }

                1 -> LazyColumn(
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(juzs, key = { it.juzNumber }) { juz ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onOpenPage(juz.startPage) },
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("الجزء ${juz.juzNumber}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("صفحة ${juz.startPage}", color = SoftGold, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                2 -> LazyColumn(
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(hizbs, key = { it.quarterNumber }) { q ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onOpenPage(q.startPage) },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("الحزب ${q.hizbNumber} — الربع ${q.quarterNumber}", fontWeight = FontWeight.SemiBold)
                                Text("صفحة ${q.startPage}", color = SoftGold)
                            }
                        }
                    }
                }

                3 -> LazyColumn(
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sajdahs, key = { it.id }) { ayah ->
                        val sName = surahs.firstOrNull { it.id == ayah.surahId }?.nameArabic ?: ""
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onOpenPage(ayah.pageNumber) },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("سورة $sName — آية ${ayah.ayahNumber} ۩", fontWeight = FontWeight.Bold)
                                Text("صفحة ${ayah.pageNumber}", color = SoftGold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showJumpDialog) {
        AlertDialog(
            onDismissRequest = { showJumpDialog = false },
            title = { Text("الانتقال إلى صفحة (1 - 604)", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = jumpInput,
                    onValueChange = { jumpInput = it.filter { ch -> ch.isDigit() } },
                    label = { Text("رقم الصفحة") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    val p = jumpInput.toIntOrNull()
                    if (p != null && p in 1..604) {
                        showJumpDialog = false
                        jumpInput = ""
                        onOpenPage(p)
                    }
                }) { Text("انتقال") }
            },
            dismissButton = {
                TextButton(onClick = { showJumpDialog = false }) { Text("إلغاء") }
            }
        )
    }
}
