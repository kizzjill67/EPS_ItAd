package com.example.videodownloader.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.videodownloader.data.local.Converters

/**
 * Entity для хранения информации о видео в базе данных Room
 */
@Entity(tableName = "videos")
@TypeConverters(Converters::class)
data class Video(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Название видео (может быть изменено пользователем)
    val title: String,
    
    // Оригинальное название (при загрузке)
    val originalTitle: String? = null,
    
    // URL источника
    val url: String? = null,
    
    // Путь к файлу на устройстве
    val filePath: String,
    
    // Дата добавления/загрузки
    val dateAdded: Long = System.currentTimeMillis(),
    
    // Длительность видео в секундах
    val duration: Long? = null,
    
    // Размер файла в байтах
    val fileSize: Long? = null,
    
    // Теги для категоризации
    val tags: List<String> = emptyList(),
    
    // Статус загрузки
    val downloadStatus: DownloadStatus = DownloadStatus.COMPLETED,
    
    // Прогресс загрузки (0-100)
    val downloadProgress: Int = 100,
    
    // ID задачи загрузки (для отмены)
    val downloadJobId: String? = null,
    
    // Путь к миниатюре
    val thumbnailPath: String? = null,
    
    // Было ли видео добавлено из галереи
    val isFromGallery: Boolean = false
)

/**
 * Статус загрузки видео
 */
enum class DownloadStatus {
    PENDING,      // Ожидает начала загрузки
    DOWNLOADING,  // В процессе загрузки
    COMPLETED,    // Загрузка завершена успешно
    FAILED,       // Загрузка не удалась
    CANCELLED     // Загрузка отменена пользователем
}
