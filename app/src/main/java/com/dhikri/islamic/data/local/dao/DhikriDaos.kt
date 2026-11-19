package com.dhikri.islamic.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dhikri.islamic.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface QuranDao {
    @Query("SELECT * FROM surahs ORDER BY id ASC")
    fun observeAllSurahs(): Flow<List<SurahEntity>>

    @Query("SELECT * FROM surahs WHERE id = :surahId LIMIT 1")
    suspend fun getSurahById(surahId: Int): SurahEntity?

    @Query("SELECT * FROM ayahs WHERE page_number = :pageNumber ORDER BY id ASC")
    fun observeAyahsByPage(pageNumber: Int): Flow<List<AyahEntity>>

    @Query("SELECT * FROM ayahs WHERE surah_id = :surahId ORDER BY ayah_number ASC")
    fun observeAyahsBySurah(surahId: Int): Flow<List<AyahEntity>>

    @Query("SELECT * FROM ayahs WHERE text_search_clean LIKE '%' || :normalizedQuery || '%' ORDER BY id ASC LIMIT 120")
    suspend fun searchAyahs(normalizedQuery: String): List<AyahEntity>

    @Query("SELECT * FROM juz_metadata ORDER BY juz_number ASC")
    fun observeAllJuzs(): Flow<List<JuzMetadataEntity>>

    @Query("SELECT * FROM hizb_quarters ORDER BY rub_number ASC")
    fun observeAllHizbQuarters(): Flow<List<HizbQuarterEntity>>

    @Query("SELECT * FROM ayahs WHERE is_sajdah = 1 ORDER BY id ASC")
    fun observeSajdahAyahs(): Flow<List<AyahEntity>>

    @Query("SELECT * FROM reading_history WHERE id = 1 LIMIT 1")
    fun observeLastRead(): Flow<ReadingHistoryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveLastRead(history: ReadingHistoryEntity)

    @Query("SELECT * FROM bookmarks ORDER BY created_at DESC")
    fun observeBookmarks(): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Delete
    suspend fun deleteBookmark(bookmark: BookmarkEntity)

    @Query("SELECT * FROM favorite_ayahs ORDER BY created_at DESC")
    fun observeFavoriteAyahs(): Flow<List<FavoriteAyahEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavoriteAyah(fav: FavoriteAyahEntity)

    @Query("DELETE FROM favorite_ayahs WHERE ayah_id = :ayahId")
    suspend fun removeFavoriteAyah(ayahId: Int)

    @Query("SELECT COUNT(*) FROM surahs")
    suspend fun countSurahs(): Int

    @Query("SELECT COUNT(*) FROM ayahs")
    suspend fun countAyahs(): Int
}

@Dao
interface TafsirDao {
    @Query("SELECT * FROM tafsir_books ORDER BY category ASC, title_arabic ASC")
    fun observeBooks(): Flow<List<TafsirBookEntity>>

    @Query("SELECT * FROM tafsir_entries WHERE surah_id = :surahId AND ayah_number = :ayahNumber ORDER BY book_key ASC")
    suspend fun getEntriesForAyah(surahId: Int, ayahNumber: Int): List<TafsirEntryEntity>

    @Query("SELECT * FROM tafsir_entries WHERE text_content LIKE '%' || :query || '%' LIMIT 80")
    suspend fun searchTafsir(query: String): List<TafsirEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<TafsirEntryEntity>)

    @Query("DELETE FROM tafsir_entries WHERE book_key = :bookKey")
    suspend fun deleteEntriesForBook(bookKey: String)

    @Query("UPDATE tafsir_books SET is_downloaded = :downloaded, entry_count = :count, version = :version WHERE book_key = :bookKey")
    suspend fun updateBookStatus(bookKey: String, downloaded: Int, count: Int, version: String)
}

@Dao
interface AdhkarDao {
    @Query("SELECT * FROM adhkar_categories ORDER BY sort_order ASC, id ASC")
    fun observeCategories(): Flow<List<AdhkarCategoryEntity>>

    @Query("SELECT * FROM adhkar_items WHERE category_id = :categoryId ORDER BY id ASC")
    fun observeItemsByCategory(categoryId: Int): Flow<List<AdhkarItemEntity>>

    @Query("SELECT * FROM adhkar_items WHERE is_favorite = 1 ORDER BY id ASC")
    fun observeFavoriteAdhkar(): Flow<List<AdhkarItemEntity>>

    @Query("UPDATE adhkar_items SET is_favorite = :favorite WHERE id = :itemId")
    suspend fun setFavorite(itemId: Int, favorite: Int)
}

@Dao
interface StoryDao {
    @Query("SELECT * FROM stories ORDER BY id ASC")
    fun observeAllStories(): Flow<List<StoryEntity>>

    @Query("SELECT * FROM stories WHERE id = :storyId LIMIT 1")
    suspend fun getStoryById(storyId: Int): StoryEntity?

    @Query("UPDATE stories SET is_favorite = :favorite WHERE id = :storyId")
    suspend fun setFavorite(storyId: Int, favorite: Int)
}

@Dao
interface TasbihDao {
    @Query("SELECT * FROM tasbih_presets ORDER BY id ASC")
    fun observePresets(): Flow<List<TasbihPresetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: TasbihPresetEntity): Long

    @Update
    suspend fun updatePreset(preset: TasbihPresetEntity)

    @Delete
    suspend fun deletePreset(preset: TasbihPresetEntity)

    @Query("SELECT * FROM tasbih_daily_logs ORDER BY date_iso DESC, updated_at DESC LIMIT 60")
    fun observeDailyLogs(): Flow<List<TasbihDailyLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyLog(log: TasbihDailyLogEntity)

    @Query("SELECT COALESCE(SUM(count_added), 0) FROM tasbih_daily_logs WHERE date_iso = :dateIso")
    fun observeTodayTotal(dateIso: String): Flow<Int>
}

@Dao
interface DownloadPackageDao {
    @Query("SELECT * FROM download_packages ORDER BY is_core DESC, category ASC, title_arabic ASC")
    fun observeAllPackages(): Flow<List<DownloadPackageEntity>>

    @Query("SELECT * FROM download_packages WHERE package_key = :key LIMIT 1")
    suspend fun getPackage(key: String): DownloadPackageEntity?

    @Query("UPDATE download_packages SET status = :status, progress_percent = :progress, sha256_checksum = :checksum WHERE package_key = :key")
    suspend fun updateProgress(key: String, status: String, progress: Int, checksum: String)
}
