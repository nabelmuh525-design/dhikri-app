package com.dhikri.islamic.ui.screens

import com.dhikri.islamic.ui.components.DhikriCompactTopBar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dhikri.islamic.ui.viewmodel.DhikriViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranSearchScreen(
    viewModel: DhikriViewModel,
    onBack: () -> Unit,
    onOpenPage: (Int) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var searchInTafsir by remember { mutableStateOf(false) }
    val ayahResults by viewModel.searchResults.collectAsState()
    val tafsirResults by viewModel.tafsirSearchResults.collectAsState()
    val surahs by viewModel.surahs.collectAsState()

    Scaffold(
        topBar = {
            DhikriCompactTopBar(title = "البحث في القرآن والتفسير", onBack = onBack)
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    if (searchInTafsir) viewModel.searchTafsir(it) else viewModel.searchQuran(it)
                },
                label = { Text(if (searchInTafsir) "ابحث داخل التفسير..." else "ابحث عن كلمة أو آية (بدون تشكيل)...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !searchInTafsir,
                    onClick = {
                        searchInTafsir = false
                        viewModel.searchQuran(query)
                    },
                    label = { Text("آيات القرآن") }
                )
                FilterChip(
                    selected = searchInTafsir,
                    onClick = {
                        searchInTafsir = true
                        viewModel.searchTafsir(query)
                    },
                    label = { Text("التفسير المحمّل") }
                )
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!searchInTafsir) {
                    items(ayahResults, key = { it.id }) { ayah ->
                        val sName = surahs.firstOrNull { it.id == ayah.surahId }?.nameArabic ?: "${ayah.surahId}"
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onOpenPage(ayah.pageNumber) },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("سورة $sName — آية ${ayah.ayahNumber} (ص ${ayah.pageNumber})", color = MaterialTheme.colorScheme.primary)
                                Text(ayah.textHafsV20, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                } else {
                    items(tafsirResults, key = { it.id }) { entry ->
                        val sName = surahs.firstOrNull { it.id == entry.surahId }?.nameArabic ?: "${entry.surahId}"
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("سورة $sName — آية ${entry.ayahNumber} (${entry.bookKey})", color = MaterialTheme.colorScheme.primary)
                                Text(entry.textContent, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}
