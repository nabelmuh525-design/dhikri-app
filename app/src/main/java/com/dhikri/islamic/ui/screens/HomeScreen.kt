package com.dhikri.islamic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dhikri.islamic.ui.theme.EmeraldDark
import com.dhikri.islamic.ui.theme.EmeraldPrimary
import com.dhikri.islamic.ui.theme.SoftGold
import com.dhikri.islamic.ui.viewmodel.DhikriViewModel

@Composable
fun HomeScreen(
    viewModel: DhikriViewModel,
    onContinueReading: (Int) -> Unit,
    onOpenQuran: () -> Unit,
    onOpenAdhkarCategory: (Int) -> Unit,
    onOpenTasbih: () -> Unit,
    onOpenStories: () -> Unit
) {
    val lastRead by viewModel.lastRead.collectAsState()
    val surahs by viewModel.surahs.collectAsState()
    val todayTasbih by viewModel.todayTasbihTotal.collectAsState()

    val lastSurahName = surahs.firstOrNull { it.id == (lastRead?.surahId ?: 1) }?.nameArabic ?: "الفاتحة"
    val lastPage = lastRead?.pageNumber ?: 1

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(listOf(EmeraldDark, EmeraldPrimary))
                    )
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "ذِكري | DHIKRI",
                        style = MaterialTheme.typography.headlineMedium,
                        color = SoftGold
                    )
                    Text(
                        text = "رفيقك اليومي للقرآن الكريم والأذكار والتسبيح — يعمل بالكامل دون إنترنت",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.92f)
                    )
                    Spacer(Modifier.height(6.dp))
                    Button(
                        onClick = { onContinueReading(lastPage) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SoftGold,
                            contentColor = Color(0xFF1F1604)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Bookmark, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("متابعة القراءة: سورة $lastSurahName — صفحة $lastPage", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text(
                text = "الوصول السريع",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(
                    title = "المصحف الشريف",
                    subtitle = "114 سورة • 604 صفحة",
                    icon = Icons.Default.MenuBook,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenQuran
                )
                QuickActionCard(
                    title = "السبحة الإلكترونية",
                    subtitle = "اليوم: $todayTasbih تسبيحة",
                    icon = Icons.Default.TouchApp,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenTasbih
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(
                    title = "أذكار الصباح والمساء",
                    subtitle = "حصن المسلم",
                    icon = Icons.Default.WbSunny,
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenAdhkarCategory(27) }
                )
                QuickActionCard(
                    title = "أذكار النوم",
                    subtitle = "قبل المنام",
                    icon = Icons.Default.NightsStay,
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenAdhkarCategory(28) }
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(
                    title = "أذكار بعد الصلاة",
                    subtitle = "عقب السلام",
                    icon = Icons.Default.Mosque,
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenAdhkarCategory(25) }
                )
                QuickActionCard(
                    title = "الحكايات الإسلامية",
                    subtitle = "قصص موثقة بالمصادر",
                    icon = Icons.Default.AutoStories,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenStories
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "﴿أَلَا بِذِكۡرِ ٱللَّهِ تَطۡمَئِنُّ ٱلۡقُلُوبُ﴾",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "سورة الرعد — الآية 28",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
