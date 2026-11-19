package com.dhikri.islamic.ui.screens

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dhikri.islamic.ui.theme.HafsFontFamily
import com.dhikri.islamic.ui.viewmodel.DhikriViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoriesListScreen(
    viewModel: DhikriViewModel,
    onOpenStory: (Int) -> Unit
) {
    val stories by viewModel.stories.collectAsState()
    var selectedCategory by remember { mutableStateOf("all") }
    var search by remember { mutableStateOf("") }

    val cats = listOf(
        "all" to "الكل",
        "prophets" to "قصص الأنبياء",
        "companions" to "قصص الصحابة",
        "seerah" to "السيرة النبوية",
        "quranic" to "القصص القرآنية",
        "sunnah" to "السنة الصحيحة",
        "lessons" to "المواقف والعبر"
    )

    val filtered = stories.filter { s ->
        (selectedCategory == "all" || s.categoryKey == selectedCategory) &&
            (search.isBlank() || s.titleArabic.contains(search.trim()) || s.summaryArabic.contains(search.trim()))
    }

    Scaffold(topBar = { TopAppBar(title = { Text("الحكايات الإسلامية الموثقة") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                label = { Text("ابحث في القصص...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(cats) { (k, label) ->
                    FilterChip(selected = selectedCategory == k, onClick = { selectedCategory = k }, label = { Text(label) })
                }
            }
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered, key = { it.id }) { story ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onOpenStory(story.id) },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(story.categoryArabic, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Text(story.titleArabic, style = MaterialTheme.typography.titleMedium)
                            Text(story.summaryArabic, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryDetailScreen(
    storyId: Int,
    viewModel: DhikriViewModel,
    onBack: () -> Unit
) {
    val stories by viewModel.stories.collectAsState()
    val story = stories.firstOrNull { it.id == storyId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(story?.titleArabic ?: "القصة", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    story?.let { s ->
                        IconButton(onClick = { viewModel.toggleStoryFavorite(s.id, s.isFavorite == 0) }) {
                            Icon(
                                if (s.isFavorite == 1) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "مفضلة"
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (story == null) {
            Box(Modifier.fillMaxSize().padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("النص القرآني المستند إليه", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(story.quranicVerses, fontFamily = HafsFontFamily, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
                item {
                    Card(shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("السرد التوضيحي", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(story.narrationArabic, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("الأحاديث النبوية المستند إليها", fontWeight = FontWeight.Bold)
                            Text(story.hadithReferences, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                item {
                    Card(shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("الدروس والعبر", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(story.lessonsArabic, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
                item {
                    Text(
                        "المصادر والتوثيق: ${story.sourcesArabic}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
