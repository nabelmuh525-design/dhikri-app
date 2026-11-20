package com.dhikri.islamic.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhikri.islamic.BuildConfig
import com.dhikri.islamic.ui.components.DhikriCompactTopBar
import com.dhikri.islamic.ui.theme.EmeraldDark
import com.dhikri.islamic.ui.theme.EmeraldPrimary
import com.dhikri.islamic.ui.theme.ReadingThemes
import com.dhikri.islamic.ui.theme.SoftGold
import com.dhikri.islamic.ui.viewmodel.DhikriViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    onOpenKhatmah: () -> Unit,
    onOpenRecitations: () -> Unit,
    onOpenRuqyah: () -> Unit,
    onOpenAsmaUlHusna: () -> Unit,
    onOpenTasbih: () -> Unit,
    onOpenHisnAlMuslim: () -> Unit,
    onOpenQibla: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSources: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        topBar = { DhikriCompactTopBar(title = "القائمة والأدوات", onBack = onBack) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ModernSectionHeader("العبادات")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ModernFeatureTile("ختمة القرآن", "متابعة الورد اليومي", Icons.Default.MenuBook, listOf(Color(0xFF1A437A), EmeraldDark), Modifier.weight(1f), onOpenKhatmah)
                        ModernFeatureTile("الاستماع للقرآن", "10 قراء وتلاوات أوفلاين", Icons.Default.Headphones, listOf(Color(0xFF1F6A8A), Color(0xFF0E3E56)), Modifier.weight(1f), onOpenRecitations)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ModernFeatureTile("الرُّقية الشرعية", "آيات وأدعية موثقة", Icons.Default.HealthAndSafety, listOf(Color(0xFF2E6B4F), Color(0xFF163D2B)), Modifier.weight(1f), onOpenRuqyah)
                        ModernFeatureTile("أسماء الله الحسنى", "الأسماء الـ 99 ومعانيها", Icons.Default.AutoAwesome, listOf(Color(0xFF8C6B1F), Color(0xFF4F3A0C)), Modifier.weight(1f), onOpenAsmaUlHusna)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ModernFeatureTile("المسبحة الإلكترونية", "عدّاد ذكي وصوت خرز", Icons.Default.TouchApp, listOf(Color(0xFF4E408C), Color(0xFF281F52)), Modifier.weight(1f), onOpenTasbih)
                        ModernFeatureTile("حصن المسلم", "الأذكار والأدعية اليومية", Icons.Default.VerifiedUser, listOf(Color(0xFF1E4E75), Color(0xFF0D2840)), Modifier.weight(1f), onOpenHisnAlMuslim)
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ModernSectionHeader("الأدوات والتخصيص")
                    ModernGroupedCard {
                        ModernMenuRow("اتجاه القبلة", "بوصلة الكعبة المشرفة الدقيقة", Icons.Default.Explore, onOpenQibla)
                        ModernDivider()
                        ModernMenuRow("مركز التنزيلات", "إدارة حزم التفسير والمعاني أوفلاين", Icons.Default.CloudDownload, onOpenDownloads)
                        ModernDivider()
                        ModernMenuRow("المفضلة والعلامات", "علامات المصحف والأذكار المحفوظة", Icons.Default.Favorite, onOpenFavorites)
                        ModernDivider()
                        ModernMenuRow("الإعدادات", "المظهر، الخطوط، التحديثات، والتنبيهات", Icons.Default.Settings, onOpenSettings)
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ModernSectionHeader("التطبيق")
                    ModernGroupedCard {
                        ModernMenuRow("المصادر والتراخيص والخصوصية", "تطبيق ذِكري v${BuildConfig.VERSION_NAME}", Icons.Default.Info, onOpenSources)
                        ModernDivider()
                        ModernMenuRow("مشاركة تطبيق ذِكري", "شارك الأجر مع أهلك وأصدقائك", Icons.Default.Share) {
                            val share = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "تطبيق ذِكري | DHIKRI — مصحف شريف، أذكار، مواقيت الصلاة، بوصلة القبلة، ومسبحة إلكترونية بدون إنترنت وبدون إعلانات:\nhttps://github.com/nabelmuh525-design/dhikri-app/releases/latest"
                                )
                            }
                            context.startActivity(Intent.createChooser(share, "مشاركة تطبيق ذِكري"))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernSectionHeader(title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(SoftGold)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = SoftGold
        )
    }
}

@Composable
private fun ModernFeatureTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(106.dp)
            .shadow(4.dp, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, SoftGold.copy(alpha = 0.45f)),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(colors))
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = title, tint = SoftGold, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(6.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, textAlign = TextAlign.Center)
            Text(subtitle, color = Color.White.copy(alpha = 0.82f), fontSize = 11.sp, maxLines = 1, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ModernGroupedCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, SoftGold.copy(alpha = 0.35f)),
        shadowElevation = 3.dp
    ) {
        Column(content = content)
    }
}

@Composable
private fun ModernDivider() {
    HorizontalDivider(
        color = SoftGold.copy(alpha = 0.18f),
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
private fun ModernMenuRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
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
            Icon(icon, contentDescription = null, tint = SoftGold, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = SoftGold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadCenterScreen(viewModel: DhikriViewModel, onBack: () -> Unit) {
    val allPackages by viewModel.downloadPackages.collectAsState()
    // Hide removed Stories package cleanly
    val packages = allPackages.filterNot { it.packageKey.contains("stories", ignoreCase = true) || it.titleArabic.contains("الحكايات") }
    val installedCount = packages.count { it.status == "INSTALLED" }
    val installedMb = packages.filter { it.status == "INSTALLED" }.sumOf { it.sizeBytes } / 1_048_576.0

    Scaffold(
        topBar = { DhikriCompactTopBar(title = "مركز التنزيلات", onBack = onBack) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Modern Royal Navy & Gold Summary Hero Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth().shadow(6.dp, RoundedCornerShape(22.dp)),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.2.dp, SoftGold.copy(alpha = 0.7f)),
                    color = Color.Transparent
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.horizontalGradient(listOf(Color(0xFF163A69), EmeraldDark)))
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(SoftGold.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CloudDone, contentDescription = null, tint = SoftGold, modifier = Modifier.size(26.dp))
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("المحتوى المحمّل دون إنترنت", color = SoftGold, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                            Text(
                                "الحزم المثبّتة: $installedCount من ${packages.size}  •  الحجم: ${String.format("%.2f MB", installedMb)}",
                                color = Color.White.copy(alpha = 0.92f),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            items(packages, key = { it.packageKey }) { pkg ->
                val sizeMb = String.format("%.2f MB", pkg.sizeBytes / 1_048_576.0)
                val isInstalled = pkg.status == "INSTALLED"
                val statusArabic = when (pkg.status) {
                    "INSTALLED" -> "محمّل"
                    "DOWNLOADING" -> "جاري التنزيل (${pkg.progressPercent}%)"
                    "PAUSED" -> "متوقف مؤقتاً"
                    "FAILED" -> "فشل التنزيل"
                    else -> "متاح للتنزيل"
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        1.dp,
                        if (isInstalled) Color(0xFF4ADE80).copy(alpha = 0.45f) else SoftGold.copy(alpha = 0.35f)
                    ),
                    shadowElevation = 3.dp
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldDark),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (pkg.isCore == 1) Icons.Default.Verified else Icons.Default.LibraryBooks,
                                        contentDescription = null,
                                        tint = SoftGold,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                                Text(pkg.titleArabic, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                            }
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isInstalled) Color(0xFF14532D).copy(alpha = 0.35f) else SoftGold.copy(alpha = 0.2f),
                                border = BorderStroke(0.8.dp, if (isInstalled) Color(0xFF4ADE80) else SoftGold)
                            ) {
                                Text(
                                    text = statusArabic,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isInstalled) Color(0xFF4ADE80) else SoftGold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(pkg.descriptionArabic, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 20.sp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("الإصدار ${pkg.version} • $sizeMb", fontSize = 12.sp, color = SoftGold, fontWeight = FontWeight.SemiBold)

                            if (pkg.isCore == 1) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = EmeraldDark.copy(alpha = 0.65f),
                                    border = BorderStroke(0.8.dp, SoftGold.copy(alpha = 0.35f))
                                ) {
                                    Text(
                                        "مدمج أساسي",
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            } else {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    when (pkg.status) {
                                        "INSTALLED" -> OutlinedButton(
                                            onClick = { viewModel.deleteOptionalPackage(pkg.packageKey) },
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, SoftGold.copy(alpha = 0.5f))
                                        ) {
                                            Text("حذف الحزمة", fontSize = 12.sp)
                                        }
                                        "DOWNLOADING", "PAUSED", "FAILED" -> {
                                            OutlinedButton(
                                                onClick = { viewModel.pauseOrCancelDownload(pkg.packageKey) },
                                                enabled = pkg.status == "DOWNLOADING",
                                                shape = RoundedCornerShape(12.dp)
                                            ) { Text("إيقاف", fontSize = 12.sp) }
                                            Button(
                                                onClick = { viewModel.startPackageDownload(pkg.packageKey) },
                                                enabled = pkg.status != "DOWNLOADING",
                                                shape = RoundedCornerShape(12.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = SoftGold, contentColor = Color(0xFF1F1604))
                                            ) { Text("إكمال", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                                        }
                                        else -> Button(
                                            onClick = { viewModel.startPackageDownload(pkg.packageKey) },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = SoftGold, contentColor = Color(0xFF1F1604))
                                        ) {
                                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("تنزيل أوفلاين", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        if (pkg.status == "DOWNLOADING" || pkg.status == "PAUSED") {
                            LinearProgressIndicator(
                                progress = { pkg.progressPercent / 100f },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(4.dp)),
                                color = SoftGold
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
fun FavoritesScreen(
    viewModel: DhikriViewModel,
    onBack: () -> Unit,
    onOpenPage: (Int) -> Unit
) {
    val favAdhkar by viewModel.favoriteAdhkar.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()

    Scaffold(
        topBar = { DhikriCompactTopBar(title = "المفضلة والعلامات", onBack = onBack) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { ModernSectionHeader("العلامات المرجعية (${bookmarks.size})") }
            if (bookmarks.isEmpty()) {
                item {
                    Text("لا توجد علامات مرجعية محفوظة بعد.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
            } else {
                items(bookmarks, key = { it.id }) { bm ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { onOpenPage(bm.pageNumber) },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(0.8.dp, SoftGold.copy(alpha = 0.4f))
                    ) {
                        Row(
                            Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${bm.label} — صفحة ${bm.pageNumber}", fontWeight = FontWeight.SemiBold)
                            Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = SoftGold)
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(4.dp))
                ModernSectionHeader("الأذكار المفضلة (${favAdhkar.size})")
            }
            if (favAdhkar.isEmpty()) {
                item {
                    Text("اضغط على أيقونة القلب في أي ذكر لحفظه هنا.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
            } else {
                items(favAdhkar, key = { it.id }) { d ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(0.8.dp, SoftGold.copy(alpha = 0.4f))
                    ) {
                        Text(d.textArabic.take(180), Modifier.padding(14.dp), lineHeight = 24.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: DhikriViewModel,
    onBack: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenSources: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val availableUpdate by viewModel.availableUpdate.collectAsState()
    val updateProgress by viewModel.updateProgress.collectAsState()
    val updateStatusMsg by viewModel.updateStatusMsg.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = { DhikriCompactTopBar(title = "الإعدادات", onBack = onBack) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Modern App Update Hero Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth().shadow(5.dp, RoundedCornerShape(22.dp)),
                    shape = RoundedCornerShape(22.dp),
                    color = Color.Transparent,
                    border = BorderStroke(1.2.dp, SoftGold.copy(alpha = 0.75f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(Color(0xFF163A69), EmeraldDark)))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier.size(38.dp).clip(CircleShape).background(SoftGold.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = SoftGold, modifier = Modifier.size(20.dp))
                                }
                                Column {
                                    Text("تحديث تطبيق ذِكري", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(
                                        updateStatusMsg ?: "أنت تستخدم أحدث إصدار من التطبيق",
                                        color = Color.White.copy(alpha = 0.80f),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = SoftGold.copy(alpha = 0.22f),
                                border = BorderStroke(0.8.dp, SoftGold)
                            ) {
                                Text(
                                    "v${BuildConfig.VERSION_NAME}",
                                    color = SoftGold,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        updateProgress?.let { pct ->
                            LinearProgressIndicator(
                                progress = { pct / 100f },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(4.dp)),
                                color = SoftGold,
                                trackColor = Color.White.copy(alpha = 0.2f)
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.checkForAppUpdate(silent = false) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, SoftGold.copy(alpha = 0.7f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                Text("فحص التحديثات", fontWeight = FontWeight.SemiBold)
                            }
                            if (availableUpdate != null) {
                                Button(
                                    onClick = { viewModel.installAppUpdate(context) },
                                    enabled = updateProgress == null,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SoftGold, contentColor = Color(0xFF1F1604))
                                ) {
                                    Text("تحديث v${availableUpdate?.latestVersion}", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // 2. Appearance & Reading Section
            item { ModernSectionHeader("المظهر والقراءة") }

            item {
                ModernGroupedCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("مظهر التطبيق (الوضع الليلي)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val options = listOf(
                                Triple("system", "تلقائي (النظام)", Icons.Default.BrightnessAuto),
                                Triple("light", "فاتح", Icons.Default.WbSunny),
                                Triple("dark", "داكن", Icons.Default.DarkMode)
                            )
                            options.forEach { (key, label, icon) ->
                                val selected = settings.themeMode == key
                                Surface(
                                    modifier = Modifier.weight(1f).clickable { viewModel.setThemeMode(key) },
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (selected) EmeraldDark else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                    border = BorderStroke(
                                        if (selected) 1.5.dp else 0.8.dp,
                                        if (selected) SoftGold else SoftGold.copy(alpha = 0.25f)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = label,
                                            tint = if (selected) SoftGold else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = SoftGold.copy(alpha = 0.18f))

                        Text("لون القراءة في المصحف (لراحة العين)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(ReadingThemes, key = { it.key }) { theme ->
                                val selected = settings.readingThemeKey == theme.key
                                Surface(
                                    modifier = Modifier.clickable { viewModel.setReadingTheme(theme.key) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = theme.background,
                                    border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) SoftGold else theme.dividerColor)
                                ) {
                                    Row(
                                        Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(Modifier.size(16.dp).clip(CircleShape).background(theme.accentColor))
                                        Text(theme.labelArabic, color = theme.textColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = SoftGold.copy(alpha = 0.18f))

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("حجم خط المصحف", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SoftGold.copy(alpha = 0.18f)
                            ) {
                                Text(
                                    "${settings.mushafFontSizeSp.toInt()} sp",
                                    color = SoftGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Slider(
                            value = settings.mushafFontSizeSp,
                            onValueChange = { viewModel.setMushafFontSize(it) },
                            valueRange = 18f..42f,
                            colors = SliderDefaults.colors(thumbColor = SoftGold, activeTrackColor = SoftGold)
                        )
                    }
                }
            }

            // 3. Feedback & Notifications Section
            item { ModernSectionHeader("التفاعل والتنبيهات") }

            item {
                ModernGroupedCard {
                    ModernSettingToggleRow(
                        title = "الاهتزاز في السبحة والأذكار",
                        subtitle = "اهتزاز خفيف عند كل نقرة تسبيح أو ذكر",
                        icon = Icons.Default.Vibration,
                        checked = settings.vibrationEnabled,
                        onCheckedChange = { viewModel.setVibrationEnabled(it) }
                    )
                    ModernDivider()
                    ModernSettingToggleRow(
                        title = "صوت خرز السبحة الإلكترونية",
                        subtitle = "صوت نقر خشبي طبيعي أثناء التسبيح",
                        icon = Icons.Default.VolumeUp,
                        checked = settings.soundEnabled,
                        onCheckedChange = { viewModel.setSoundEnabled(it) }
                    )
                    ModernDivider()
                    ModernSettingToggleRow(
                        title = "تنبيهات الأذكار اليومية",
                        subtitle = "تذكير لطيف بأذكار الصباح والمساء",
                        icon = Icons.Default.NotificationsActive,
                        checked = settings.notificationsEnabled,
                        onCheckedChange = { viewModel.setNotificationsEnabled(it) }
                    )
                }
            }

            // 4. Storage & About Section
            item { ModernSectionHeader("التخزين والمعلومات") }

            item {
                ModernGroupedCard {
                    ModernMenuRow("إدارة التنزيلات والتخزين", "حذف أو تنزيل حزم التفسير دون المساس ببياناتك", Icons.Default.Storage, onOpenDownloads)
                    ModernDivider()
                    ModernMenuRow("المصادر والتراخيص والخصوصية", "حول تطبيق ذِكري v${BuildConfig.VERSION_NAME}", Icons.Default.Info, onOpenSources)
                }
            }
        }
    }
}

@Composable
private fun ModernSettingToggleRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(38.dp).clip(CircleShape).background(EmeraldDark),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = SoftGold, modifier = Modifier.size(19.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF1F1604),
                checkedTrackColor = SoftGold
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourcesAndLicensesScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = { DhikriCompactTopBar(title = "المصادر والتراخيص والخصوصية", onBack = onBack) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SourceCard(
                    title = "1. مشروع تنزيل (Tanzil Project)",
                    body = "بيانات المصحف الشريف (quran-data.xml v1.0) والنص العثماني (quran-uthmani.txt v1.1) دون أي تعديل.\nحقوق النشر: © 2007-2026 Tanzil Project — رخصة Creative Commons Attribution 3.0.\nالموقع الرسمي: https://tanzil.net"
                )
            }
            item {
                SourceCard(
                    title = "2. موسوعة القرآن الكريم (QuranEnc.com)",
                    body = "حزمة خط حفص العثماني والنص المتوافق معه (Hafs_20.zip — مجمع الملك فهد لطباعة المصحف الشريف)، والتفسير الميسر (arabic_moyassar)، ومعاني الكلمات — السراج في بيان غريب القرآن (arabic_seraj) دون تعديل.\nالموقع الرسمي: https://quranenc.com"
                )
            }
            item {
                SourceCard(
                    title = "3. الأذكار والأدعية — كتاب حصن المسلم",
                    body = "أذكار الكتاب والسنة — الشيخ سعيد بن علي بن وهف القحطاني رحمه الله، مع الأدعية القرآنية وأدعية الأنبياء الموثقة."
                )
            }
            item {
                SourceCard(
                    title = "4. سياسة الخصوصية",
                    body = "تطبيق ذِكري يعمل أوفلاين بالكامل، ولا يجمع أي بيانات شخصية، ولا يحتوي على إعلانات أو أدوات تتبّع. جميع علاماتك وختماتك وإعداداتك محفوظة محلياً على جهازك فقط."
                )
            }
        }
    }
}

@Composable
private fun SourceCard(title: String, body: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.9.dp, SoftGold.copy(alpha = 0.4f))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = SoftGold, fontSize = 15.sp)
            Text(body, fontSize = 13.5.sp, lineHeight = 22.sp, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
