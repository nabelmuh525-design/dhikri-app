package com.dhikri.islamic.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dhikri.islamic.ui.viewmodel.DhikriViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranIndexScreen(
    viewModel: DhikriViewModel,
    onOpenPage: (Int) -> Unit,
    onOpenSearch: () -> Unit
) {
    val surahs by viewModel.surahs.collectAsState()
    val juzs by viewModel.juzs.collectAsState()
    val quarters by viewModel.hizbQuarters.collectAsState()
    val sajdahs by viewModel.sajdahs.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()
    val lastRead by viewModel.lastRead.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showJumpDialog by remember { mutableStateOf(false) }
    var pageInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("القرآن الكريم") },
                actions = {
                    TextButton(onClick = { showJumpDialog = true }) {
                        Text("صفحة")
                    }
                    IconButton(onClick = onOpenSearch) {
                        Icon(Icons.Default.Search, contentDescription = "بحث في القرآن")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            lastRead?.let { lr ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clickable { onOpenPage(lr.pageNumber) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Bookmark, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            "آخر موضع قراءة: صفحة ${lr.pageNumber} — سورة رقم ${lr.surahId} (آية ${lr.ayahNumber})",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 12.dp) {
                listOf("السور (114)", "الأجزاء (30)", "الأحزاب (60)", "السجدات (15)", "العلامات").forEachIndexed { idx, label ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        text = { Text(label) }
                    )
                }
            }

            when (selectedTab) {
                0 -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(surahs, key = { it.id }) { surah ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onOpenPage(surah.startPage) },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("${surah.id}. سورة ${surah.nameArabic}", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "${surah.revelationType} • ${surah.ayahCount} آية • ترتيب النزول: ${surah.revelationOrder}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    "ص ${surah.startPage}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                1 -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(juzs, key = { it.juzNumber }) { juz ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onOpenPage(juz.startPage) },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(juz.nameArabic, style = MaterialTheme.typography.titleMedium)
                                Text("ص ${juz.startPage}", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
                2 -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(quarters, key = { it.rubNumber }) { q ->
                        val quarterLabel = when (q.quarterInHizb) {
                            1 -> "بداية الحزب ${q.hizbNumber}"
                            2 -> "ربع الحزب ${q.hizbNumber}"
                            3 -> "نصف الحزب ${q.hizbNumber}"
                            else -> "ثلاثة أرباع الحزب ${q.hizbNumber}"
                        }
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onOpenPage(q.startPage) },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(quarterLabel, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "الجزء ${q.juzNumber} • الربع ${q.rubNumber} من 240",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text("ص ${q.startPage}", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
                3 -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sajdahs, key = { it.id }) { ayah ->
                        val sName = surahs.firstOrNull { it.id == ayah.surahId }?.nameArabic ?: "${ayah.surahId}"
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onOpenPage(ayah.pageNumber) },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("سورة $sName — الآية ${ayah.ayahNumber} (سجدة ${ayah.sajdahType ?: "مستحبة"})", fontWeight = FontWeight.Bold)
                                Text("صفحة ${ayah.pageNumber} • الجزء ${ayah.juzNumber}", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (bookmarks.isEmpty()) {
                        item {
                            Text(
                                "لا توجد علامات مرجعية بعد. اضغط على أي آية داخل المصحف لحفظ علامة مرجعية.",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        items(bookmarks, key = { it.id }) { bm ->
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable { onOpenPage(bm.pageNumber) },
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(bm.label, style = MaterialTheme.typography.titleMedium)
                                        Text("صفحة ${bm.pageNumber} • آية ${bm.ayahNumber}", style = MaterialTheme.typography.bodyMedium)
                                    }
                                    TextButton(onClick = { viewModel.deleteBookmark(bm) }) {
                                        Text("حذف")
                                    }
                                }
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
            title = { Text("الانتقال إلى صفحة (1 – 604)") },
            text = {
                OutlinedTextField(
                    value = pageInput,
                    onValueChange = { pageInput = it.filter(Char::isDigit) },
                    label = { Text("رقم الصفحة") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val p = pageInput.toIntOrNull()
                    if (p != null && p in 1..604) {
                        showJumpDialog = false
                        pageInput = ""
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
