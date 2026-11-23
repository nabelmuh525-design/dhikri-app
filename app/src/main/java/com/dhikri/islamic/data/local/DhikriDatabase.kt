package com.dhikri.islamic.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dhikri.islamic.data.local.dao.*
import com.dhikri.islamic.data.local.entity.*

@Database(
    entities = [
        SurahEntity::class,
        AyahEntity::class,
        JuzMetadataEntity::class,
        HizbQuarterEntity::class,
        TafsirBookEntity::class,
        TafsirEntryEntity::class,
        AdhkarCategoryEntity::class,
        AdhkarItemEntity::class,
        StoryEntity::class,
        BookmarkEntity::class,
        FavoriteAyahEntity::class,
        ReadingHistoryEntity::class,
        TasbihPresetEntity::class,
        TasbihDailyLogEntity::class,
        DownloadPackageEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class DhikriDatabase : RoomDatabase() {
    abstract fun quranDao(): QuranDao
    abstract fun tafsirDao(): TafsirDao
    abstract fun adhkarDao(): AdhkarDao
    abstract fun storyDao(): StoryDao
    abstract fun tasbihDao(): TasbihDao
    abstract fun downloadPackageDao(): DownloadPackageDao

    companion object {
        private const val DB_NAME = "dhikri_database.db"

        /**
         * Non-destructive migration from v1 (v3.8.4) to v2 (v4.0.0):
         * - Preserves 100% of user bookmarks, favorites, reading_history, tasbih_presets, and tasbih_daily_logs.
         * - Corrects Story #7 mismatched title to match its Prophet Ayyub (AS) Quranic verses & Bukhari source.
         * - Updates Sajdah fiqh classification to accurately state the scholarly difference
         *   (Sunnah Mu'akkadah per majority/Shafi'i/Maliki/Hanbali & Jordan Iftaa; Wajib per Hanafi).
         * - Never alters any Quranic verse text (text_hafs_v20 / text_tanzil_uthmani).
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    UPDATE stories
                    SET title_arabic = 'قصة صبر نبي الله أيوب عليه السلام والشكر عند البلاء'
                    WHERE id = 7 AND title_arabic LIKE '%الغلام وصاحب البستان%'
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    UPDATE ayahs
                    SET sajdah_type = 'سنة مؤكدة عند الجمهور (واجبة عند الحنفية)'
                    WHERE is_sajdah = 1 AND NOT (surah_id = 22 AND ayah_number = 77)
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    UPDATE ayahs
                    SET sajdah_type = 'سنة مؤكدة عند الشافعية والحنابلة (خلاف فقهي)'
                    WHERE is_sajdah = 1 AND surah_id = 22 AND ayah_number = 77
                    """.trimIndent()
                )
            }
        }

        @Volatile
        private var INSTANCE: DhikriDatabase? = null

        fun getInstance(context: Context): DhikriDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    DhikriDatabase::class.java,
                    DB_NAME
                )
                    .createFromAsset("databases/$DB_NAME")
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
