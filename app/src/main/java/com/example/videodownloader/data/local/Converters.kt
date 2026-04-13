package com.example.videodownloader.data.local

import androidx.room.TypeConverter
import com.example.videodownloader.data.model.DownloadStatus
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Конвертеры типов для Room (для хранения сложных типов в базе данных)
 */
class Converters {
    
    private val json = Json { ignoreUnknownKeys = true }
    
    /**
     * Конвертация списка строк (тегов) в JSON строку для хранения в БД
     */
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return json.encodeToString(value)
    }
    
    /**
     * Конвертация JSON строки обратно в список строк
     */
    @TypeConverter
    fun toStringList(value: String): List<String> {
        return try {
            json.decodeFromString(value)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Конвертация статуса загрузки в строку
     */
    @TypeConverter
    fun fromDownloadStatus(status: DownloadStatus): String {
        return status.name
    }
    
    /**
     * Конвертация строки обратно в статус загрузки
     */
    @TypeConverter
    fun toDownloadStatus(value: String): DownloadStatus {
        return try {
            DownloadStatus.valueOf(value)
        } catch (e: Exception) {
            DownloadStatus.PENDING
        }
    }
}
