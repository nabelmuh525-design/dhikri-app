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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dhikri.islamic.ui.theme.EmeraldPrimary
import com.dhikri.islamic.ui.theme.ReadingThemes
import com.dhikri.islamic.ui.theme.SoftGold
import com.dhikri.islamic.BuildConfig
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
            TopAppBar(
                title = { Text("القائمة والمزيد", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Group 1: العبادات
            item { GroupHeader("العبادات") }
            item { MoreRow("ختمة القرآن الكريم", "إنشاء خطة ختمة ومتابعة التقدم اليومي", Icons.Default.AutoAwesome, onOpenKhatmah) }
            item { MoreRow("الاستماع للقرآن الكريم", "تلاوات القراء مع إمكانية التحميل أوفلاين", Icons.Default.Headphones, onOpenRecitations) }
            item { MoreRow("الرُّقية الشرعية", "آيات وأدعية موثقة من القرآن والصحيحين", Icons.Default.HealthAndSafety, onOpenRuqyah) }
            item { MoreRow("أسماء الله الحسنى", "الـ 99 اسماً ومعانيها الموثقة أوفلاين", Icons.Default.Stars, onOpenAsmaUlHusna) }
            item { MoreRow("حصن المسلم والأذكار", "134 باباً مصنفاً من الأذكار والأدعية", Icons.Default.MenuBook, onOpenHisnAlMuslim) }
            item { MoreRow("السبحة الإلكترونية", "عداد تسبيح، أهداف مخصصة، وإحصائيات يومية", Icons.Default.TouchApp, onOpenTasbih) }

            // Group 2: الأدوات الإسلامية
            item { Spacer(Modifier.height(4.dp)); GroupHeader("الأدوات الإسلامية") }
            item { MoreRow("اتجاه القبلة", "بوصلة القبلة وحساب الزاوية دون إنترنت", Icons.Default.Explore, onOpenQibla) }

            // Group 3: التخصيص
            item { Spacer(Modifier.height(4.dp)); GroupHeader("التخصيص وإدارة المحتوى") }
            item { MoreRow("مركز التنزيلات وإدارة المساحة", "حالة الحزم (محمّل / غير محمّل) وحجم الملفات", Icons.Default.CloudDownload, onOpenDownloads) }
            item { MoreRow("المفضلة والعلامات", "الآيات والأذكار والقصص المحفوظة", Icons.Default.Favorite, onOpenFavorites) }
            item { MoreRow("الإعدادات والتنبيهات", "ألوان القراءة لراحة العين، الخط، والوضع الليلي", Icons.Default.Settings, onOpenSettings) }

            // Group 4: التطبيق
            item { Spacer(Modifier.height(4.dp)); GroupHeader("التطبيق") }
            item {
                MoreRow("مشاركة تطبيق ذِكري", "انشر الخير وشارك التطبيق مع أحبابك", Icons.Default.Share) {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "تطبيق ذِكري | DHIKRI — المصحف الشريف، التفسير، الأذكار، الختمة، والسبحة دون إنترنت.")
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "مشاركة تطبيق ذِكري"))
                }
            }
            item { MoreRow("حول التطبيق والمصادر والتراخيص", "Tanzil وQuranEnc وحصن المسلم وسياسة الخصوصية", Icons.Default.VerifiedUser, onOpenSources) }
        }
    }
}

@Composable
private fun GroupHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    )
}

@Composable
private fun MoreRow(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(0.6.dp, SoftGold.copy(alpha = 0.35f))
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
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
            TopAppBar(
                title = { Text("مركز التنزيلات") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
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
                                color = if (pkg.status == "INSTALLED") EmeraldPrimary.copy(alpha = 0.14f) else SoftGold.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    statusArabic,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (pkg.status == "INSTALLED") EmeraldPrimary else Color(0xFF7A5311),
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
    onOpenPage: (Int) -> Unit,
    onOpenStory: (Int) -> Unit
) {
    val favAdhkar by viewModel.favoriteAdhkar.collectAsState()
    val stories by viewModel.stories.collectAsState()
    val favStories = stories.filter { it.isFavorite == 1 }
    val bookmarks by viewModel.bookmarks.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("المفضلة والعلامات") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
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
            item { Text("القصص المفضلة (${favStories.size})", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) }
            items(favStories, key = { it.id }) { s ->
                Card(Modifier.fillMaxWidth().clickable { onOpenStory(s.id) }) { Text(s.titleArabic, Modifier.padding(14.dp)) }
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
            TopAppBar(
                title = { Text("الإعدادات") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
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
            item {
                SettingSwitch("الوضع الليلي للتطبيق", settings.darkMode) { viewModel.setDarkMode(it) }
            }
            item {
                Card(shape = RoundedCornerShape(14.dp)) {
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
                                        Box(
                                            Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(theme.accentColor)
                                        )
                                        Text(theme.labelArabic, color = theme.textColor, style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item {
                Card(shape = RoundedCornerShape(14.dp)) {
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
            item { MoreRow("إدارة التنزيلات والتخزين", "حذف الحزم الإضافية دون المساس ببياناتك", Icons.Default.Storage, onOpenDownloads) }
            item {
                Card(shape = RoundedCornerShape(16.dp), border = BorderStroke(0.8.dp, SoftGold.copy(alpha = 0.5f))) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("تحديث تطبيق ذِكري (الإصدار الحالي: v${BuildConfig.VERSION_NAME})", fontWeight = FontWeight.Bold)
                        updateStatusMsg?.let { msg ->
                            Text(msg, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        updateProgress?.let { pct ->
                            LinearProgressIndicator(progress = { pct / 100f }, modifier = Modifier.fillMaxWidth())
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { viewModel.checkForAppUpdate(silent = false) }) {
                                Text("التحقق من التحديثات")
                            }
                            if (availableUpdate != null) {
                                Button(
                                    onClick = { viewModel.installAppUpdate(context) },
                                    enabled = updateProgress == null
                                ) {
                                    Text("تحديث إلى v${availableUpdate?.latestVersion}")
                                }
                            }
                        }
                    }
                }
            }
            item { MoreRow("المصادر والتراخيص وسياسة الخصوصية", "حول تطبيق ذِكري v2.9.0", Icons.Default.Info, onOpenSources) }
        }
    }
}

@Composable
private fun SettingSwitch(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Card(shape = RoundedCornerShape(14.dp)) {
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
            TopAppBar(
                title = { Text("المصادر والتراخيص والخصوصية") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
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
