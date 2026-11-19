package com.dhikri.islamic.ui.screens

import com.dhikri.islamic.ui.components.DhikriCompactTopBar

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
        topBar = {
            DhikriCompactTopBar(title = "القائمة والأدوات", onBack = onBack)
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: العبادات (Modern 2-column grid tiles)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    GroupHeader("العبادات")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ModernGridTile(
                            title = "ختمة القرآن",
                            subtitle = "خطة ومتابعة يومية",
                            icon = Icons.Default.AutoAwesome,
                            colors = listOf(Color(0xFF1A4378), EmeraldDark),
                            modifier = Modifier.weight(1f),
                            onClick = onOpenKhatmah
                        )
                        ModernGridTile(
                            title = "الاستماع للقرآن",
                            subtitle = "5 قراء مع التحميل",
                            icon = Icons.Default.Headphones,
                            colors = listOf(Color(0xFF28598A), Color(0xFF123254)),
                            modifier = Modifier.weight(1f),
                            onClick = onOpenRecitations
                        )
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ModernGridTile(
                            title = "الرُّقية الشرعية",
                            subtitle = "آيات وأدعية موثقة",
                            icon = Icons.Default.HealthAndSafety,
                            colors = listOf(Color(0xFF2E6B5E), Color(0xFF134036)),
                            modifier = Modifier.weight(1f),
                            onClick = onOpenRuqyah
                        )
                        ModernGridTile(
                            title = "أسماء الله الحسنى",
                            subtitle = "99 اسماً ومعانيها",
                            icon = Icons.Default.Stars,
                            colors = listOf(Color(0xFF8C6924), Color(0xFF594010)),
                            modifier = Modifier.weight(1f),
                            onClick = onOpenAsmaUlHusna
                        )
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ModernGridTile(
                            title = "حصن المسلم",
                            subtitle = "134 باباً مصنفاً",
                            icon = Icons.Default.MenuBook,
                            colors = listOf(Color(0xFF4F468C), Color(0xFF2B2459)),
                            modifier = Modifier.weight(1f),
                            onClick = onOpenHisnAlMuslim
                        )
                        ModernGridTile(
                            title = "عداد التسبيح",
                            subtitle = "سبحة وأهداف يومية",
                            icon = Icons.Default.TouchApp,
                            colors = listOf(Color(0xFF1E5743), Color(0xFF0E3326)),
                            modifier = Modifier.weight(1f),
                            onClick = onOpenTasbih
                        )
                    }
                }
            }

            // Section 2: الأدوات والتخصيص (Sleek grouped card container)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GroupHeader("الأدوات والتخصيص")
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(0.9.dp, SoftGold.copy(alpha = 0.45f))
                    ) {
                        Column {
                            GroupedMenuItem("اتجاه القبلة", "بوصلة الكعبة المشرفة أوفلاين", Icons.Default.Explore, onOpenQibla)
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), modifier = Modifier.padding(horizontal = 16.dp))
                            GroupedMenuItem("مركز التنزيلات", "إدارة التفاسير والمساحة (محمّل / غير محمّل)", Icons.Default.CloudDownload, onOpenDownloads)
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), modifier = Modifier.padding(horizontal = 16.dp))
                            GroupedMenuItem("المفضلة والعلامات", "الآيات والأذكار والقصص المحفوظة", Icons.Default.Favorite, onOpenFavorites)
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), modifier = Modifier.padding(horizontal = 16.dp))
                            GroupedMenuItem("الإعدادات وتحديث التطبيق", "ألوان القراءة، الخط، التنبيهات، والتحديثات", Icons.Default.Settings, onOpenSettings)
                        }
                    }
                }
            }

            // Section 3: التطبيق
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GroupHeader("التطبيق")
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(0.9.dp, SoftGold.copy(alpha = 0.45f))
                    ) {
                        Column {
                            GroupedMenuItem("مشاركة تطبيق ذِكري", "شارك الأجر مع أهلك وأصدقائك", Icons.Default.Share) {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "تطبيق ذِكري | DHIKRI — المصحف الشريف، التفسير، الأذكار، الختمة، والسبحة دون إنترنت.")
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "مشاركة تطبيق ذِكري"))
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), modifier = Modifier.padding(horizontal = 16.dp))
                            GroupedMenuItem("المصادر والتراخيص والخصوصية", "الإصدار v${BuildConfig.VERSION_NAME} • Tanzil وQuranEnc", Icons.Default.VerifiedUser, onOpenSources)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupHeader(title: String) {
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
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun ModernGridTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(106.dp)
            .shadow(4.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(colors))
            .clickable(onClick = onClick)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = title, tint = SoftGold, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(6.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center, maxLines = 1)
            Text(subtitle, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.82f), textAlign = TextAlign.Center, maxLines = 1)
        }
    }
}

@Composable
private fun GroupedMenuItem(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(EmeraldDark),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = title, tint = SoftGold, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadCenterScreen(viewModel: DhikriViewModel, onBack: () -> Unit) {
    val packages by viewModel.downloadPackages.collectAsState()
    val installedCount = packages.count { it.status == "INSTALLED" }
    val installedMb = packages.filter { it.status == "INSTALLED" }.sumOf { it.sizeBytes } / 1_048_576.0

    Scaffold(
        topBar = {
            DhikriCompactTopBar(title = "مركز التنزيلات", onBack = onBack)
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("ملخص المحتوى المحمّل دون إنترنت", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Text("الحزم المحمّلة: $installedCount من ${packages.size} • المساحة المستخدمة: ${String.format("%.2f MB", installedMb)}")
                    }
                }
            }

            items(packages, key = { it.packageKey }) { pkg ->
                val sizeMb = String.format("%.2f MB", pkg.sizeBytes / 1_048_576.0)
                val statusArabic = when (pkg.status) {
                    "INSTALLED" -> "محمّل ✅"
                    "DOWNLOADING" -> "جاري التنزيل (${pkg.progressPercent}%)"
                    "PAUSED" -> "متوقف مؤقتاً"
                    "FAILED" -> "فشل التنزيل"
                    else -> "غير محمّل ⬇️"
                }
                Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(pkg.titleArabic, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (pkg.status == "INSTALLED") Color(0xFF1B5E20).copy(alpha = 0.22f) else SoftGold.copy(alpha = 0.24f)
                            ) {
                                Text(
                                    statusArabic,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (pkg.status == "INSTALLED") Color(0xFF4CAF50) else SoftGold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Text(pkg.descriptionArabic, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("الإصدار: ${pkg.version} • الحجم: $sizeMb", style = MaterialTheme.typography.labelMedium)
                        if (pkg.sha256Checksum.isNotBlank() && pkg.sha256Checksum != "CORE_BUNDLED") {
                            Text("التحقق SHA-256: ${pkg.sha256Checksum.take(18)}...", style = MaterialTheme.typography.labelMedium)
                        }
                        if (pkg.status == "DOWNLOADING" || pkg.status == "PAUSED") {
                            LinearProgressIndicator(
                                progress = { pkg.progressPercent / 100f },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (pkg.isCore == 1) {
                                AssistChip(onClick = {}, label = { Text("مدمج أساسي (لا يُحذف)") })
                            } else {
                                when (pkg.status) {
                                    "INSTALLED" -> OutlinedButton(onClick = { viewModel.deleteOptionalPackage(pkg.packageKey) }) {
                                        Text("حذف الحزمة")
                                    }
                                    "DOWNLOADING", "PAUSED", "FAILED" -> {
                                        OutlinedButton(
                                            onClick = { viewModel.pauseOrCancelDownload(pkg.packageKey) },
                                            enabled = pkg.status == "DOWNLOADING"
                                        ) {
                                            Text("إيقاف مؤقت")
                                        }
                                        Button(
                                            onClick = { viewModel.startPackageDownload(pkg.packageKey) },
                                            enabled = pkg.status != "DOWNLOADING"
                                        ) {
                                            Text("إكمال التحميل")
                                        }
                                    }
                                    else -> Button(onClick = { viewModel.startPackageDownload(pkg.packageKey) }) {
                                        Text("تنزيل أوفلاين")
                                    }
                                }
                            }
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
        topBar = {
            DhikriCompactTopBar(title = "المفضلة والعلامات", onBack = onBack)
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Text("العلامات المرجعية (${bookmarks.size})", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) }
            items(bookmarks, key = { it.id }) { bm ->
                Card(Modifier.fillMaxWidth().clickable { onOpenPage(bm.pageNumber) }) {
                    Text("${bm.label} — صفحة ${bm.pageNumber}", Modifier.padding(14.dp))
                }
            }
            item { Text("الأذكار المفضلة (${favAdhkar.size})", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) }
            items(favAdhkar, key = { it.id }) { d ->
                Card(Modifier.fillMaxWidth()) { Text(d.textArabic.take(140), Modifier.padding(14.dp)) }
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
        topBar = {
            DhikriCompactTopBar(title = "الإعدادات", onBack = onBack)
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Clean, Organized App Update Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, SoftGold.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = SoftGold)
                                Text("تحديث تطبيق ذِكري", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    "v${BuildConfig.VERSION_NAME}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        updateStatusMsg?.let { msg ->
                            Text(msg, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        updateProgress?.let { pct ->
                            LinearProgressIndicator(progress = { pct / 100f }, modifier = Modifier.fillMaxWidth())
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.checkForAppUpdate(silent = false) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("فحص التحديثات")
                            }
                            if (availableUpdate != null) {
                                Button(
                                    onClick = { viewModel.installAppUpdate(context) },
                                    enabled = updateProgress == null,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("تحديث v${availableUpdate?.latestVersion}")
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(0.8.dp, SoftGold.copy(alpha = 0.35f))
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("مظهر التطبيق (الوضع الليلي)", fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val options = listOf(
                                Triple("system", "تلقائي (النظام)", Icons.Default.BrightnessAuto),
                                Triple("light", "فاتح", Icons.Default.WbSunny),
                                Triple("dark", "داكن", Icons.Default.DarkMode)
                            )
                            options.forEach { (key, label, icon) ->
                                val selected = settings.themeMode == key
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { viewModel.setThemeMode(key) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selected) EmeraldDark else MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(
                                        if (selected) 1.5.dp else 0.8.dp,
                                        if (selected) SoftGold else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
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
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("لون القراءة في المصحف (لراحة العين)", fontWeight = FontWeight.SemiBold)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(ReadingThemes, key = { it.key }) { theme ->
                                val selected = settings.readingThemeKey == theme.key
                                Surface(
                                    modifier = Modifier.clickable { viewModel.setReadingTheme(theme.key) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = theme.background,
                                    border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) theme.accentColor else theme.dividerColor)
                                ) {
                                    Row(
                                        Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(Modifier.size(16.dp).clip(CircleShape).background(theme.accentColor))
                                        Text(theme.labelArabic, color = theme.textColor, style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("حجم خط المصحف: ${settings.mushafFontSizeSp.toInt()} sp", fontWeight = FontWeight.SemiBold)
                        Slider(
                            value = settings.mushafFontSizeSp,
                            onValueChange = { viewModel.setMushafFontSize(it) },
                            valueRange = 18f..42f
                        )
                    }
                }
            }
            item { SettingSwitch("الاهتزاز في السبحة الإلكترونية", settings.vibrationEnabled) { viewModel.setVibrationEnabled(it) } }
            item { SettingSwitch("صوت النقر في السبحة الإلكترونية", settings.soundEnabled) { viewModel.setSoundEnabled(it) } }
            item { SettingSwitch("التنبيهات اليومية للأذكار", settings.notificationsEnabled) { viewModel.setNotificationsEnabled(it) } }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(0.8.dp, SoftGold.copy(alpha = 0.4f))
                ) {
                    Column {
                        GroupedMenuItem("إدارة التنزيلات والتخزين", "حذف الحزم الإضافية دون المساس ببياناتك", Icons.Default.Storage, onOpenDownloads)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), modifier = Modifier.padding(horizontal = 16.dp))
                        GroupedMenuItem("المصادر والتراخيص والخصوصية", "حول تطبيق ذِكري v${BuildConfig.VERSION_NAME}", Icons.Default.Info, onOpenSources)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingSwitch(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourcesAndLicensesScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            DhikriCompactTopBar(title = "المصادر والتراخيص والخصوصية", onBack = onBack)
        }
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
                    body = "أذكار الكتاب والسنة من تأليف الشيخ سعيد بن علي بن وهف القحطاني، عبر قاعدة البيانات المفتوحة (رخصة MIT): github.com/YousefAsalya/Islamic-Pro-azkar-API.\nللتخريج الحديثي التفصيلي: الموسوعة الحديثية — الدرر السنية (https://dorar.net/hadith)."
                )
            }
            item {
                SourceCard(
                    title = "4. سياسة الخصوصية",
                    body = "تطبيق ذِكري يعمل دون إنترنت (Offline-First)، ولا يجمع أي بيانات شخصية، ولا يرسل أي تتبع أو تحليلات خارجية، ولا يحتوي على إعلانات. جميع العلامات المرجعية والمفضلة وسجل التسبيح والختمة تُحفظ محلياً على جهازك فقط."
                )
            }
        }
    }
}

@Composable
private fun SourceCard(title: String, body: String) {
    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text(body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
