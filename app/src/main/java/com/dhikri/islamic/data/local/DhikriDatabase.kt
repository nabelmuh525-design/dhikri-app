package com.dhikri.islamic.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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
    version = 1,
    exportSchema = false
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
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
