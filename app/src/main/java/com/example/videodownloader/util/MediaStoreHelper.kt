package com.example.videodownloader.util

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.example.videodownloader.data.model.Video
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Helper для работы с MediaStore API
 * Позволяет сохранять видео в публичную папку Movies и делать их видимыми в галерее
 */
class MediaStoreHelper(private val context: Context) {
    
    companion object {
        private const val APP_FOLDER_NAME = "MyVideoApp"
    }
    
    /**
     * Получить путь к приватной папке приложения для загрузок
     */
    fun getDownloadDirectory(): File {
        val downloadDir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_MOVIES),
            APP_FOLDER_NAME
        )
        if (!downloadDir.exists()) {
            downloadDir.mkdirs()
        }
        return downloadDir
    }
    
    /**
     * Сохранить видео из приватной папки в публичную Movies через MediaStore
     * После этого видео появится в системной галерее
     */
    suspend fun copyToPublicMovies(videoFile: File, title: String): Uri? {
        return try {
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Video.Media.DISPLAY_NAME, title)
                put(android.provider.MediaStore.Video.Media.MIME_TYPE, "video/*")
                put(
                    android.provider.MediaStore.Video.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_MOVIES + "/" + APP_FOLDER_NAME
                )
                put(android.provider.MediaStore.Video.Media.IS_PENDING, 1)
            }
            
            val resolver = context.contentResolver
            val collection = android.provider.MediaStore.Video.Media.getContentUri(
                android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY
            )
            
            val uri = resolver.insert(collection, contentValues) ?: return null
            
            // Копирование файла
            resolver.openOutputStream(uri)?.use { outputStream ->
                FileInputStream(videoFile).use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            
            // Помечаем как готовое
            contentValues.clear()
            contentValues.put(android.provider.MediaStore.Video.Media.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
            
            uri
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Удалить видео из MediaStore
     */
    suspend fun deleteFromMediaStore(uri: Uri): Boolean {
        return try {
            context.contentResolver.delete(uri, null, null) > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Открыть файл через FileProvider для совместимости с Android 7+
     */
    fun getFileUri(file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
}
