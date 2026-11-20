package com.dhikri.islamic.ui.components

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.dhikri.islamic.BuildConfig
import com.dhikri.islamic.ui.theme.EmeraldDark
import com.dhikri.islamic.ui.theme.SoftGold

object WhatsNewTracker {
    private const val PREFS_NAME = "dhikri_whats_new_prefs"
    private const val KEY_SEEN_VERSION = "last_seen_version_name"

    fun shouldShowForCurrentVersion(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val seen = prefs.getString(KEY_SEEN_VERSION, null)
        return seen != BuildConfig.VERSION_NAME
    }

    fun markCurrentVersionSeen(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SEEN_VERSION, BuildConfig.VERSION_NAME).apply()
    }

    fun currentHighlights(): List<String> = listOf(
        "إعادة تصميم شاشة التقويم والمناسبات الإسلامية بالكامل بواجهة عصرية متوافقة مع الوضعين النهاري والليلي",
        "إضافة تقويم شهري هجري تفاعلي (شبكة أيام 1-30) مع تمييز اليوم والأيام البيض وعرض التاريخ الميلادي الموافق عند اللمس",
        "إضافة عدّاد تنازلي تلقائي لأقرب مناسبة إسلامية قادمة وتمييز الشهر الحالي والأشهر الحرم"
    )
}

@Composable
fun WhatsNewDialog(
    version: String = BuildConfig.VERSION_NAME,
    highlights: List<String> = WhatsNewTracker.currentHighlights(),
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = EmeraldDark,
            border = BorderStroke(1.5.dp, SoftGold),
            shadowElevation = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(SoftGold.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = SoftGold,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "ما الجديد في ذِكري",
                            color = SoftGold,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "الإصدار v$version",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp
                        )
                    }
                }

                HorizontalDivider(color = SoftGold.copy(alpha = 0.28f))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    highlights.forEach { item ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SoftGold,
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(18.dp)
                            )
                            Text(
                                text = item,
                                color = Color.White.copy(alpha = 0.94f),
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SoftGold,
                        contentColor = Color(0xFF1F1604)
                    )
                ) {
                    Text(
                        text = "رائع، ابدأ الاستخدام",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}
