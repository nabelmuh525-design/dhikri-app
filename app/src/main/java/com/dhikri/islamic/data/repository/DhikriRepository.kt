package com.dhikri.islamic.data.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.dhikri.islamic.data.download.TafsirDownloadWorker
import com.dhikri.islamic.data.local.DhikriDatabase
import com.dhikri.islamic.data.local.entity.*
import com.dhikri.islamic.domain.util.ArabicTextNormalizer
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DhikriRepository(
    private val context: Context,
    private val db: DhikriDatabase
) {
    // Quran
    fun observeAllSurahs(): Flow<List<SurahEntity>> = db.quranDao().observeAllSurahs()
    fun observeAyahsByPage(page: Int): Flow<List<AyahEntity>> = db.quranDao().observeAyahsByPage(page)
    fun observeAllJuzs(): Flow<List<JuzMetadataEntity>> = db.quranDao().observeAllJuzs()
    fun observeAllHizbQuarters(): Flow<List<HizbQuarterEntity>> = db.quranDao().observeAllHizbQuarters()
    fun observeSajdahAyahs(): Flow<List<AyahEntity>> = db.quranDao().observeSajdahAyahs()
    fun observeLastRead(): Flow<ReadingHistoryEntity?> = db.quranDao().observeLastRead()
    fun observeBookmarks(): Flow<List<BookmarkEntity>> = db.quranDao().observeBookmarks()
    fun observeFavoriteAyahs(): Flow<List<FavoriteAyahEntity>> = db.quranDao().observeFavoriteAyahs()

    suspend fun getSurahById(id: Int): SurahEntity? = db.quranDao().getSurahById(id)

    suspend fun searchQuran(rawQuery: String): List<AyahEntity> {
        val normalized = ArabicTextNormalizer.normalizeForSearch(rawQuery)
        if (normalized.length < 2) return emptyList()
        return db.quranDao().searchAyahs(normalized)
    }

    suspend fun saveLastRead(surahId: Int, ayahNumber: Int, pageNumber: Int) {
        db.quranDao().saveLastRead(
            ReadingHistoryEntity(
                id = 1,
                surahId = surahId,
                ayahNumber = ayahNumber,
                pageNumber = pageNumber,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun addBookmark(surahId: Int, ayahNumber: Int, pageNumber: Int, label: String) {
        db.quranDao().insertBookmark(
            BookmarkEntity(
                surahId = surahId,
                ayahNumber = ayahNumber,
                pageNumber = pageNumber,
                label = label,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteBookmark(bookmark: BookmarkEntity) = db.quranDao().deleteBookmark(bookmark)

    suspend fun toggleFavoriteAyah(ayah: AyahEntity, isCurrentlyFav: Boolean) {
        if (isCurrentlyFav) {
            db.quranDao().removeFavoriteAyah(ayah.id)
        } else {
            db.quranDao().addFavoriteAyah(
                FavoriteAyahEntity(
                    ayahId = ayah.id,
                    surahId = ayah.surahId,
                    ayahNumber = ayah.ayahNumber,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    // Tafsir & Word Meanings
    fun observeTafsirBooks(): Flow<List<TafsirBookEntity>> = db.tafsirDao().observeBooks()
    suspend fun getTafsirForAyah(surahId: Int, ayahNumber: Int): List<TafsirEntryEntity> =
        db.tafsirDao().getEntriesForAyah(surahId, ayahNumber)
    suspend fun searchTafsir(query: String): List<TafsirEntryEntity> =
        if (query.trim().length < 2) emptyList() else db.tafsirDao().searchTafsir(query.trim())

    // Adhkar
    fun observeAdhkarCategories(): Flow<List<AdhkarCategoryEntity>> = db.adhkarDao().observeCategories()
    fun observeAdhkarItems(categoryId: Int): Flow<List<AdhkarItemEntity>> = db.adhkarDao().observeItemsByCategory(categoryId)
    fun observeFavoriteAdhkar(): Flow<List<AdhkarItemEntity>> = db.adhkarDao().observeFavoriteAdhkar()
    suspend fun toggleAdhkarFavorite(itemId: Int, isFavorite: Boolean) =
        db.adhkarDao().setFavorite(itemId, if (isFavorite) 1 else 0)

    // Stories
    fun observeStories(): Flow<List<StoryEntity>> = db.storyDao().observeAllStories()
    suspend fun getStoryById(id: Int): StoryEntity? = db.storyDao().getStoryById(id)
    suspend fun toggleStoryFavorite(storyId: Int, isFavorite: Boolean) =
        db.storyDao().setFavorite(storyId, if (isFavorite) 1 else 0)

    // Tasbih
    fun observeTasbihPresets(): Flow<List<TasbihPresetEntity>> = db.tasbihDao().observePresets()
    fun observeTasbihDailyLogs(): Flow<List<TasbihDailyLogEntity>> = db.tasbihDao().observeDailyLogs()
    fun observeTodayTasbihTotal(): Flow<Int> = db.tasbihDao().observeTodayTotal(todayIso())

    suspend fun incrementTasbih(preset: TasbihPresetEntity) {
        val nextCount = if (preset.currentCount + 1 >= preset.targetCount) 0 else preset.currentCount + 1
        db.tasbihDao().updatePreset(
            preset.copy(
                currentCount = nextCount,
                totalCount = preset.totalCount + 1
            )
        )
        db.tasbihDao().insertDailyLog(
            TasbihDailyLogEntity(
                dateIso = todayIso(),
                presetId = preset.id,
                phraseArabic = preset.phraseArabic,
                countAdded = 1,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun decrementTasbih(preset: TasbihPresetEntity) {
        if (preset.currentCount > 0) {
            db.tasbihDao().updatePreset(
                preset.copy(
                    currentCount = preset.currentCount - 1,
                    totalCount = (preset.totalCount - 1).coerceAtLeast(0)
                )
            )
        }
    }

    suspend fun resetTasbihCounter(preset: TasbihPresetEntity) {
        db.tasbihDao().updatePreset(preset.copy(currentCount = 0))
    }

    suspend fun addCustomTasbih(phrase: String, target: Int) {
        db.tasbihDao().insertPreset(
            TasbihPresetEntity(
                phraseArabic = phrase.trim(),
                targetCount = target.coerceAtLeast(1),
                currentCount = 0,
                totalCount = 0,
                isCustom = 1,
                sourceNote = "ذكر مخصص مضاف من المستخدم"
            )
        )
    }

    suspend fun deleteCustomTasbih(preset: TasbihPresetEntity) {
        if (preset.isCustom == 1) db.tasbihDao().deletePreset(preset)
    }

    // Download Center
    fun observeDownloadPackages(): Flow<List<DownloadPackageEntity>> =
        db.downloadPackageDao().observeAllPackages()

    fun enqueueTafsirDownload(bookKey: String) {
        val request = OneTimeWorkRequestBuilder<TafsirDownloadWorker>()
            .setInputData(workDataOf(TafsirDownloadWorker.KEY_BOOK_KEY to bookKey))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork("download_$bookKey", ExistingWorkPolicy.REPLACE, request)
    }

    suspend fun pauseDownload(bookKey: String) {
        WorkManager.getInstance(context).cancelUniqueWork("download_$bookKey")
        val done = db.tafsirDao().getMaxDownloadedSurah(bookKey)
        db.downloadPackageDao().updateProgress(bookKey, "PAUSED", (done * 100) / 114, "")
    }

    suspend fun deleteOptionalPackage(packageKey: String) {
        val pkg = db.downloadPackageDao().getPackage(packageKey) ?: return
        if (pkg.isCore == 1) return
        db.tafsirDao().deleteEntriesForBook(packageKey)
        db.tafsirDao().updateBookStatus(packageKey, 0, 0, pkg.version)
        db.downloadPackageDao().updateProgress(packageKey, "AVAILABLE", 0, "")
    }

    private fun todayIso(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
}
