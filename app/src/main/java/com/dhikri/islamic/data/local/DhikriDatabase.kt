package com.dhikri.islamic.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dhikri.islamic.data.local.dao.*
import com.dhikri.islamic.data.local.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

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
        private val BUNDLED_TAFSIR_KEYS = listOf(
            "arabic_moyassar",
            "arabic_seraj",
            "arabic_mokhtasar"
        )

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

        /**
         * Ensures existing users upgrading from older versions automatically receive
         * all 3 offline-bundled tafsirs (Al-Muyassar, As-Siraj, Al-Mokhtasar) marked as core
         * without touching any user bookmarks, favorites, reading history, or tasbih logs.
         */
        fun syncBundledTafsirsInBackground(context: Context, database: DhikriDatabase) {
            val appCtx = context.applicationContext
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                runCatching {
                    val liveDb = database.openHelper.writableDatabase
                    val missingKeys = BUNDLED_TAFSIR_KEYS.filter { key ->
                        val bookReady = liveDb.query(
                            "SELECT is_downloaded, entry_count FROM tafsir_books WHERE book_key = ?",
                            arrayOf(key)
                        ).use { c ->
                            c.moveToFirst() && c.getInt(0) == 1 && c.getInt(1) > 0
                        }
                        val pkgCore = liveDb.query(
                            "SELECT is_core, status FROM download_packages WHERE package_key = ?",
                            arrayOf(key)
                        ).use { c ->
                            c.moveToFirst() && c.getInt(0) == 1 && c.getString(1) == "INSTALLED"
                        }
                        !(bookReady && pkgCore)
                    }
                    if (missingKeys.isEmpty()) return@runCatching

                    val tempAssetDb = File(appCtx.cacheDir, "dhikri_asset_sync.db")
                    try {
                        appCtx.assets.open("databases/$DB_NAME").use { input ->
                            tempAssetDb.outputStream().use { output -> input.copyTo(output) }
                        }
                        val assetDb = SQLiteDatabase.openDatabase(
                            tempAssetDb.absolutePath,
                            null,
                            SQLiteDatabase.OPEN_READONLY
                        )
                        try {
                            var updatedAny = false
                            for (key in missingKeys) {
                                val assetCount = assetDb.rawQuery(
                                    "SELECT COUNT(*) FROM tafsir_entries WHERE book_key = ?",
                                    arrayOf(key)
                                ).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }

                                val liveCount = liveDb.query(
                                    "SELECT COUNT(*) FROM tafsir_entries WHERE book_key = ?",
                                    arrayOf(key)
                                ).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }

                                if (assetCount == 0 && liveCount == 0) continue

                                liveDb.beginTransaction()
                                try {
                                    if (assetCount > 0 && liveCount < assetCount) {
                                        liveDb.execSQL("DELETE FROM tafsir_entries WHERE book_key = ?", arrayOf(key))
                                        val insertStmt = liveDb.compileStatement(
                                            "INSERT INTO tafsir_entries (book_key, surah_id, ayah_number, text_content, footnotes) VALUES (?,?,?,?,?)"
                                        )
                                        assetDb.rawQuery(
                                            "SELECT surah_id, ayah_number, text_content, footnotes FROM tafsir_entries WHERE book_key = ?",
                                            arrayOf(key)
                                        ).use { cursor ->
                                            while (cursor.moveToNext()) {
                                                insertStmt.clearBindings()
                                                insertStmt.bindString(1, key)
                                                insertStmt.bindLong(2, cursor.getLong(0))
                                                insertStmt.bindLong(3, cursor.getLong(1))
                                                insertStmt.bindString(4, cursor.getString(2))
                                                if (cursor.isNull(3)) {
                                                    insertStmt.bindNull(5)
                                                } else {
                                                    insertStmt.bindString(5, cursor.getString(3))
                                                }
                                                insertStmt.executeInsert()
                                            }
                                        }
                                        insertStmt.close()
                                    }

                                    val finalCount = maxOf(assetCount, liveCount)
                                    assetDb.rawQuery(
                                        "SELECT version, size_bytes FROM tafsir_books WHERE book_key = ?",
                                        arrayOf(key)
                                    ).use { c ->
                                        if (c.moveToFirst()) {
                                            liveDb.execSQL(
                                                "UPDATE tafsir_books SET is_downloaded = 1, entry_count = ?, version = ?, size_bytes = ? WHERE book_key = ?",
                                                arrayOf(finalCount, c.getString(0), c.getLong(1), key)
                                            )
                                        }
                                    }
                                    assetDb.rawQuery(
                                        "SELECT version, size_bytes, sha256_checksum FROM download_packages WHERE package_key = ?",
                                        arrayOf(key)
                                    ).use { c ->
                                        if (c.moveToFirst()) {
                                            liveDb.execSQL(
                                                "UPDATE download_packages SET status = 'INSTALLED', progress_percent = 100, is_core = 1, version = ?, size_bytes = ?, sha256_checksum = ? WHERE package_key = ?",
                                                arrayOf(c.getString(0), c.getLong(1), c.getString(2), key)
                                            )
                                        } else {
                                            liveDb.execSQL(
                                                "UPDATE download_packages SET status = 'INSTALLED', progress_percent = 100, is_core = 1 WHERE package_key = ?",
                                                arrayOf(key)
                                            )
                                        }
                                    }
                                    liveDb.setTransactionSuccessful()
                                    updatedAny = true
                                } finally {
                                    liveDb.endTransaction()
                                }
                            }
                            if (updatedAny) {
                                database.invalidationTracker.refreshVersionsAsync()
                            }
                        } finally {
                            assetDb.close()
                        }
                    } finally {
                        tempAssetDb.delete()
                    }
                }
            }
        }
    }
}
