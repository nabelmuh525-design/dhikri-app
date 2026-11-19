package com.dhikri.islamic.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "surahs")
data class SurahEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "name_arabic") val nameArabic: String,
    @ColumnInfo(name = "name_transliteration") val nameTransliteration: String,
    @ColumnInfo(name = "name_english") val nameEnglish: String,
    @ColumnInfo(name = "revelation_type") val revelationType: String,
    @ColumnInfo(name = "ayah_count") val ayahCount: Int,
    @ColumnInfo(name = "revelation_order") val revelationOrder: Int,
    @ColumnInfo(name = "rukus_count") val rukusCount: Int,
    @ColumnInfo(name = "start_ayah_index") val startAyahIndex: Int,
    @ColumnInfo(name = "start_page") val startPage: Int
)

@Entity(
    tableName = "ayahs",
    foreignKeys = [
        ForeignKey(
            entity = SurahEntity::class,
            parentColumns = ["id"],
            childColumns = ["surah_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["surah_id"], name = "index_ayahs_surah_id"),
        Index(value = ["page_number"], name = "index_ayahs_page_number"),
        Index(value = ["juz_number"], name = "index_ayahs_juz_number"),
        Index(value = ["hizb_number"], name = "index_ayahs_hizb_number")
    ]
)
data class AyahEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "surah_id") val surahId: Int,
    @ColumnInfo(name = "ayah_number") val ayahNumber: Int,
    @ColumnInfo(name = "text_hafs_v20") val textHafsV20: String,
    @ColumnInfo(name = "text_tanzil_uthmani") val textTanzilUthmani: String,
    @ColumnInfo(name = "text_search_clean") val textSearchClean: String,
    @ColumnInfo(name = "page_number") val pageNumber: Int,
    @ColumnInfo(name = "juz_number") val juzNumber: Int,
    @ColumnInfo(name = "hizb_number") val hizbNumber: Int,
    @ColumnInfo(name = "rub_number") val rubNumber: Int,
    @ColumnInfo(name = "is_sajdah") val isSajdah: Int,
    @ColumnInfo(name = "sajdah_type") val sajdahType: String?
)

@Entity(tableName = "juz_metadata")
data class JuzMetadataEntity(
    @PrimaryKey @ColumnInfo(name = "juz_number") val juzNumber: Int,
    @ColumnInfo(name = "start_surah_id") val startSurahId: Int,
    @ColumnInfo(name = "start_ayah_number") val startAyahNumber: Int,
    @ColumnInfo(name = "start_page") val startPage: Int,
    @ColumnInfo(name = "name_arabic") val nameArabic: String
)

@Entity(tableName = "hizb_quarters")
data class HizbQuarterEntity(
    @PrimaryKey @ColumnInfo(name = "rub_number") val rubNumber: Int,
    @ColumnInfo(name = "hizb_number") val hizbNumber: Int,
    @ColumnInfo(name = "quarter_in_hizb") val quarterInHizb: Int,
    @ColumnInfo(name = "juz_number") val juzNumber: Int,
    @ColumnInfo(name = "start_surah_id") val startSurahId: Int,
    @ColumnInfo(name = "start_ayah_number") val startAyahNumber: Int,
    @ColumnInfo(name = "start_page") val startPage: Int
)

@Entity(tableName = "tafsir_books")
data class TafsirBookEntity(
    @PrimaryKey @ColumnInfo(name = "book_key") val bookKey: String,
    @ColumnInfo(name = "title_arabic") val titleArabic: String,
    @ColumnInfo(name = "author_arabic") val authorArabic: String,
    @ColumnInfo(name = "publisher") val publisher: String,
    @ColumnInfo(name = "source_url") val sourceUrl: String,
    @ColumnInfo(name = "version") val version: String,
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "is_downloaded") val isDownloaded: Int,
    @ColumnInfo(name = "entry_count") val entryCount: Int,
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long
)

@Entity(
    tableName = "tafsir_entries",
    indices = [
        Index(
            value = ["book_key", "surah_id", "ayah_number"],
            name = "index_tafsir_entries_book_key_surah_id_ayah_number"
        )
    ]
)
data class TafsirEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "book_key") val bookKey: String,
    @ColumnInfo(name = "surah_id") val surahId: Int,
    @ColumnInfo(name = "ayah_number") val ayahNumber: Int,
    @ColumnInfo(name = "text_content") val textContent: String,
    @ColumnInfo(name = "footnotes") val footnotes: String?
)

@Entity(tableName = "adhkar_categories")
data class AdhkarCategoryEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "title_arabic") val titleArabic: String,
    @ColumnInfo(name = "group_key") val groupKey: String,
    @ColumnInfo(name = "source_book") val sourceBook: String,
    @ColumnInfo(name = "sort_order") val sortOrder: Int
)

@Entity(
    tableName = "adhkar_items",
    foreignKeys = [
        ForeignKey(
            entity = AdhkarCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["category_id"], name = "index_adhkar_items_category_id")
    ]
)
data class AdhkarItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "category_id") val categoryId: Int,
    @ColumnInfo(name = "text_arabic") val textArabic: String,
    @ColumnInfo(name = "repeat_count") val repeatCount: Int,
    @ColumnInfo(name = "source_reference") val sourceReference: String,
    @ColumnInfo(name = "hadith_grade") val hadithGrade: String?,
    @ColumnInfo(name = "is_favorite") val isFavorite: Int
)

@Entity(tableName = "stories")
data class StoryEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "title_arabic") val titleArabic: String,
    @ColumnInfo(name = "category_key") val categoryKey: String,
    @ColumnInfo(name = "category_arabic") val categoryArabic: String,
    @ColumnInfo(name = "summary_arabic") val summaryArabic: String,
    @ColumnInfo(name = "quranic_verses") val quranicVerses: String,
    @ColumnInfo(name = "hadith_references") val hadithReferences: String,
    @ColumnInfo(name = "narration_arabic") val narrationArabic: String,
    @ColumnInfo(name = "lessons_arabic") val lessonsArabic: String,
    @ColumnInfo(name = "sources_arabic") val sourcesArabic: String,
    @ColumnInfo(name = "is_favorite") val isFavorite: Int,
    @ColumnInfo(name = "is_downloaded") val isDownloaded: Int
)

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "surah_id") val surahId: Int,
    @ColumnInfo(name = "ayah_number") val ayahNumber: Int,
    @ColumnInfo(name = "page_number") val pageNumber: Int,
    @ColumnInfo(name = "label") val label: String,
    @ColumnInfo(name = "created_at") val createdAt: Long
)

@Entity(tableName = "favorite_ayahs")
data class FavoriteAyahEntity(
    @PrimaryKey @ColumnInfo(name = "ayah_id") val ayahId: Int,
    @ColumnInfo(name = "surah_id") val surahId: Int,
    @ColumnInfo(name = "ayah_number") val ayahNumber: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long
)

@Entity(tableName = "reading_history")
data class ReadingHistoryEntity(
    @PrimaryKey val id: Int = 1,
    @ColumnInfo(name = "surah_id") val surahId: Int,
    @ColumnInfo(name = "ayah_number") val ayahNumber: Int,
    @ColumnInfo(name = "page_number") val pageNumber: Int,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)

@Entity(tableName = "tasbih_presets")
data class TasbihPresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "phrase_arabic") val phraseArabic: String,
    @ColumnInfo(name = "target_count") val targetCount: Int,
    @ColumnInfo(name = "current_count") val currentCount: Int,
    @ColumnInfo(name = "total_count") val totalCount: Int,
    @ColumnInfo(name = "is_custom") val isCustom: Int,
    @ColumnInfo(name = "source_note") val sourceNote: String
)

@Entity(tableName = "tasbih_daily_logs")
data class TasbihDailyLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "date_iso") val dateIso: String,
    @ColumnInfo(name = "preset_id") val presetId: Int,
    @ColumnInfo(name = "phrase_arabic") val phraseArabic: String,
    @ColumnInfo(name = "count_added") val countAdded: Int,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)

@Entity(tableName = "download_packages")
data class DownloadPackageEntity(
    @PrimaryKey @ColumnInfo(name = "package_key") val packageKey: String,
    @ColumnInfo(name = "title_arabic") val titleArabic: String,
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "description_arabic") val descriptionArabic: String,
    @ColumnInfo(name = "version") val version: String,
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long,
    @ColumnInfo(name = "sha256_checksum") val sha256Checksum: String,
    @ColumnInfo(name = "source_url") val sourceUrl: String,
    @ColumnInfo(name = "status") val status: String,
    @ColumnInfo(name = "progress_percent") val progressPercent: Int,
    @ColumnInfo(name = "is_core") val isCore: Int
)
