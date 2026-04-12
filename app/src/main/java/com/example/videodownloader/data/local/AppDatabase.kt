package com.example.videodownloader.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.videodownloader.data.model.Video

/**
 * База данных Room для хранения информации о видео
 */
@Database(
    entities = [Video::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    /**
     * DAO для работы с видео
     */
    abstract fun videoDao(): VideoDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        /**
         * Получить экземпляр базы данных (Singleton pattern)
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "video_database"
                )
                .fallbackToDestructiveMigration() // При изменении схемы - пересоздать БД
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
