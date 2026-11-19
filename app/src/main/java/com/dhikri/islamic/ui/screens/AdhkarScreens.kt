package com.dhikri.islamic.ui.screens

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dhikri.islamic.ui.viewmodel.DhikriViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdhkarListScreen(
    viewModel: DhikriViewModel,
    onOpenCategory: (Int) -> Unit
) {
    val categories by viewModel.adhkarCategories.collectAsState()
    var selectedGroup by remember { mutableStateOf("all") }
    var searchQuery by remember { mutableStateOf("") }

    val groups = listOf(
        "all" to "الكل",
        "morning" to "الصباح والمساء",
        "sleep" to "النوم",
        "waking" to "الاستيقاظ",
        "prayer" to "الصلاة",
        "after_prayer" to "بعد الصلاة",
        "quranic_duas" to "الأدعية القرآنية",
        "prophets_duas" to "أدعية الأنبياء",
        "home" to "المنزل",
        "food" to "الطعام",
        "travel" to "السفر",
        "ruqyah" to "الرقية والتحصين"
    )

    val filtered = categories.filter { cat ->
        (selectedGroup == "all" || cat.groupKey == selectedGroup) &&
            (searchQuery.isBlank() || cat.titleArabic.contains(searchQuery.trim()))
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("الأذكار والأدعية") }) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("ابحث في أبواب حصن المسلم والأدعية...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(groups) { (key, label) ->
                    FilterChip(
                        selected = selectedGroup == key,
                        onClick = { selectedGroup = key },
                        label = { Text(label) }
                    )
                }
            }
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.id }) { cat ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onOpenCategory(cat.id) },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(cat.titleArabic, style = MaterialTheme.typography.titleMedium)
                            Text(
                                cat.sourceBook,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdhkarDetailScreen(
    categoryId: Int,
    viewModel: DhikriViewModel,
    onBack: () -> Unit
) {
    val categories by viewModel.adhkarCategories.collectAsState()
    val itemsList by viewModel.observeAdhkarItems(categoryId).collectAsState(initial = emptyList())
    val categoryTitle = categories.firstOrNull { it.id == categoryId }?.titleArabic ?: "الأذكار"
    val progressMap = remember { mutableStateMapOf<Int, Int>() }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(categoryTitle, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = { progressMap.clear() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "إعادة التصفير")
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
            items(itemsList, key = { it.id }) { item ->
                val count = progressMap[item.id] ?: 0
                val done = count >= item.repeatCount
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (count < item.repeatCount) progressMap[item.id] = count + 1
                        },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (done) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(item.textArabic, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "المصدر: ${item.sourceReference}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        item.hadithGrade?.let { grade ->
                            Text(
                                grade,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    if (count < item.repeatCount) progressMap[item.id] = count + 1
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    if (done) "تمت القراءة (${item.repeatCount}/${item.repeatCount})"
                                    else "التكرار: $count / ${item.repeatCount}",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row {
                                IconButton(onClick = {
                                    viewModel.toggleAdhkarFavorite(item.id, item.isFavorite == 0)
                                }) {
                                    Icon(
                                        if (item.isFavorite == 1) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = "مفضلة"
                                    )
                                }
                                IconButton(onClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, "${item.textArabic}

المصدر: ${item.sourceReference}
عبر تطبيق ذِكري")
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "مشاركة الذكر"))
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
}
