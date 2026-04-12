package com.example.videodownloader.data.repository

import android.content.Context
import android.net.Uri
import com.example.videodownloader.data.local.VideoDao
import com.example.videodownloader.data.model.DownloadStatus
import com.example.videodownloader.data.model.Video
import com.example.videodownloader.util.MediaStoreHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Репозиторий для работы с видео
 * Инкапсулирует логику работы с базой данных и файловой системой
 */
class VideoRepository(
    private val videoDao: VideoDao,
    private val mediaStoreHelper: MediaStoreHelper,
    private val context: Context
) {
    
    /**
     * Получить все видео в виде Flow (автоматическое обновление)
     */
    fun getAllVideos(): Flow<List<Video>> = videoDao.getAllVideos()
    
    /**
     * Получить видео по ID
     */
    suspend fun getVideoById(id: Long): Video? = withContext(Dispatchers.IO) {
        videoDao.getVideoById(id)
    }
    
    /**
     * Поиск видео по названию
     */
    fun searchVideos(query: String): Flow<List<Video>> = videoDao.searchVideos("%$query%")
    
    /**
     * Добавить новое видео в базу данных
     */
    suspend fun addVideo(video: Video): Long = withContext(Dispatchers.IO) {
        videoDao.insertVideo(video)
    }
    
    /**
     * Обновить информацию о видео (название, теги)
     */
    suspend fun updateVideo(video: Video): Unit = withContext(Dispatchers.IO) {
        videoDao.updateVideo(video)
    }
    
    /**
     * Обновить статус загрузки
     */
    suspend fun updateDownloadStatus(id: Long, status: DownloadStatus, progress: Int): Unit = withContext(Dispatchers.IO) {
        videoDao.updateDownloadStatus(id, status, progress)
    }
    
    /**
     * Удалить видео из базы данных и файловой системы
     */
    suspend fun deleteVideo(id: Long): Boolean = withContext(Dispatchers.IO) {
        val video = videoDao.getVideoById(id) ?: return@withContext false
        
        // Удалить файл
        val file = File(video.filePath)
        if (file.exists()) {
            file.delete()
        }
        
        // Удалить запись из БД
        videoDao.deleteVideoById(id)
        true
    }
    
    /**
     * Добавить видео из галереи
     */
    suspend fun addVideoFromGallery(uri: Uri, title: String): Long? = withContext(Dispatchers.IO) {
        try {
            // Создать копию файла в приватной папке приложения
            val downloadDir = mediaStoreHelper.getDownloadDirectory()
            val outputFile = File(downloadDir, "${title}_${System.currentTimeMillis()}.mp4")
            
            // Копирование файла
            context.contentResolver.openInputStream(uri)?.use { input ->
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            // Создать объект видео
            val video = Video(
                title = title,
                originalTitle = title,
                filePath = outputFile.absolutePath,
                isFromGallery = true,
                downloadStatus = DownloadStatus.COMPLETED,
                downloadProgress = 100
            )
            
            // Сохранить в БД
            videoDao.insertVideo(video)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Сохранить загруженное видео в публичную папку Movies
     */
    suspend fun saveToPublicMovies(videoId: Long): Uri? = withContext(Dispatchers.IO) {
        val video = videoDao.getVideoById(videoId) ?: return@withContext null
        val file = File(video.filePath)
        
        if (!file.exists()) {
            return@withContext null
        }
        
        mediaStoreHelper.copyToPublicMovies(file, video.title)
    }
    
    /**
     * Получить все уникальные теги из всех видео
     */
    suspend fun getAllTags(): List<String> = withContext(Dispatchers.IO) {
        val allVideos = videoDao.getAllVideos().firstOrNull() ?: emptyList()
        allVideos.flatMap { it.tags }.distinct().sorted()
    }
    
    /**
     * Фильтровать видео по тегу
     */
    fun getVideosByTag(tag: String): Flow<List<Video>> = videoDao.getAllVideos()
        .let { flow ->
            // Фильтрация на уровне репозитория (можно оптимизировать через SQL)
            kotlinx.coroutines.flow.map(flow) { videos ->
                videos.filter { tag in it.tags }
            }
        }
    
    /**
     * Добавить тег к видео
     */
    suspend fun addTagToVideo(videoId: Long, tag: String): Boolean = withContext(Dispatchers.IO) {
        val video = videoDao.getVideoById(videoId) ?: return@withContext false
        
        if (tag in video.tags) {
            return@withContext false // Тег уже существует
        }
        
        val updatedVideo = video.copy(tags = video.tags + tag)
        videoDao.updateVideo(updatedVideo)
        true
    }
    
    /**
     * Удалить тег из видео
     */
    suspend fun removeTagFromVideo(videoId: Long, tag: String): Boolean = withContext(Dispatchers.IO) {
        val video = videoDao.getVideoById(videoId) ?: return@withContext false
        
        if (tag !in video.tags) {
            return@withContext false // Тега не существует
        }
        
        val updatedVideo = video.copy(tags = video.tags - tag)
        videoDao.updateVideo(updatedVideo)
        true
    }
}
