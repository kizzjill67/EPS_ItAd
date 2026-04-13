package com.example.videodownloader.data.local

import androidx.room.*
import com.example.videodownloader.data.model.DownloadStatus
import com.example.videodownloader.data.model.Video
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object для работы с видео в базе данных
 */
@Dao
interface VideoDao {
    
    /**
     * Получить все видео в виде Flow (автоматическое обновление при изменении БД)
     */
    @Query("SELECT * FROM videos ORDER BY dateAdded DESC")
    fun getAllVideos(): Flow<List<Video>>
    
    /**
     * Получить видео по ID
     */
    @Query("SELECT * FROM videos WHERE id = :id")
    suspend fun getVideoById(id: Long): Video?
    
    /**
     * Получить видео по статусу загрузки
     */
    @Query("SELECT * FROM videos WHERE downloadStatus = :status ORDER BY dateAdded DESC")
    fun getVideosByStatus(status: DownloadStatus): Flow<List<Video>>
    
    /**
     * Поиск видео по названию
     */
    @Query("SELECT * FROM videos WHERE title LIKE :query OR originalTitle LIKE :query ORDER BY dateAdded DESC")
    fun searchVideos(query: String): Flow<List<Video>>
    
    /**
     * Получить видео с определённым тегом
     */
    @Query("SELECT * FROM videos")
    fun getVideosWithTag(tag: String): Flow<List<Video>>
    
    /**
     * Получить все уникальные теги
     */
    @Query("SELECT DISTINCT tags FROM videos WHERE tags != '[]'")
    suspend fun getAllTagsRaw(): List<String>
    
    /**
     * Вставить новое видео
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: Video): Long
    
    /**
     * Обновить информацию о видео
     */
    @Update
    suspend fun updateVideo(video: Video)
    
    /**
     * Обновить статус загрузки и прогресс
     */
    @Query("UPDATE videos SET downloadStatus = :status, downloadProgress = :progress WHERE id = :id")
    suspend fun updateDownloadStatus(id: Long, status: DownloadStatus, progress: Int)
    
    /**
     * Удалить видео по ID
     */
    @Query("DELETE FROM videos WHERE id = :id")
    suspend fun deleteVideoById(id: Long)
    
    /**
     * Удалить все видео
     */
    @Query("DELETE FROM videos")
    suspend fun deleteAllVideos()
    
    /**
     * Получить количество видео
     */
    @Query("SELECT COUNT(*) FROM videos")
    suspend fun getVideoCount(): Int
}
