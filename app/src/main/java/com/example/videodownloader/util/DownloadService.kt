package com.example.videodownloader.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.videodownloader.MainActivity
import com.example.videodownloader.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

/**
 * Сервис для фоновой загрузки видео с использованием YouTubeDL-Boom
 */
class DownloadService : Service() {
    
    companion object {
        const val CHANNEL_ID = "download_channel"
        const val NOTIFICATION_ID = 1001
        
        // StateFlow для отслеживания прогресса загрузки
        val downloadProgress: MutableStateFlow<Map<String, Int>> = MutableStateFlow(emptyMap())
        val downloadStatus: MutableStateFlow<Map<String, DownloadState>> = MutableStateFlow(emptyMap())
        
        private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        private val activeDownloads = mutableMapOf<String, Job>()
        
        sealed class DownloadState {
            object Pending : DownloadState()
            object Downloading : DownloadState()
            object Completed : DownloadState()
            data class Failed(val error: String) : DownloadState()
            object Cancelled : DownloadState()
        }
    }
    
    private lateinit var notificationManager: NotificationManager
    
    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_DOWNLOAD -> {
                val url = intent.getStringExtra(EXTRA_URL) ?: return START_NOT_STICKY
                val videoId = intent.getStringExtra(EXTRA_VIDEO_ID) ?: UUID.randomUUID().toString()
                startDownload(url, videoId)
            }
            ACTION_CANCEL_DOWNLOAD -> {
                val videoId = intent.getStringExtra(EXTRA_VIDEO_ID) ?: return START_NOT_STICKY
                cancelDownload(videoId)
            }
        }
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
    
    /**
     * Создать канал уведомлений для Android 8+
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Загрузка видео",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Уведомления о прогрессе загрузки видео"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Начать загрузку видео
     */
    private fun startDownload(url: String, videoId: String) {
        val job = coroutineScope.launch {
            try {
                updateStatus(videoId, DownloadState.Downloading)
                showNotification(videoId, "Загрузка...", 0)
                
                // Здесь будет интеграция с YouTubeDL-Boom
                // Для демонстрации - имитация загрузки
                for (i in 0..100 step 5) {
                    delay(200)
                    updateProgress(videoId, i)
                    showNotification(videoId, "Загрузка: $i%", i)
                }
                
                updateStatus(videoId, DownloadState.Completed)
                updateProgress(videoId, 100)
                showNotification(videoId, "Загрузка завершена", 100)
                
            } catch (e: Exception) {
                updateStatus(videoId, DownloadState.Failed(e.message ?: "Ошибка"))
                showNotification(videoId, "Ошибка: ${e.message}", -1)
            } finally {
                activeDownloads.remove(videoId)
            }
        }
        activeDownloads[videoId] = job
    }
    
    /**
     * Отменить загрузку
     */
    private fun cancelDownload(videoId: String) {
        activeDownloads[videoId]?.cancel()
        updateStatus(videoId, DownloadState.Cancelled)
        stopForeground(STOP_FOREGROUND_REMOVE)
    }
    
    /**
     * Обновить прогресс загрузки
     */
    private fun updateProgress(videoId: String, progress: Int) {
        val current = downloadProgress.value.toMutableMap()
        current[videoId] = progress
        downloadProgress.value = current
    }
    
    /**
     * Обновить статус загрузки
     */
    private fun updateStatus(videoId: String, state: DownloadState) {
        val current = downloadStatus.value.toMutableMap()
        current[videoId] = state
        downloadStatus.value = current
    }
    
    /**
     * Показать уведомление о прогрессе
     */
    private fun showNotification(videoId: String, text: String, progress: Int) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Загрузка видео")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .apply {
                if (progress >= 0 && progress <= 100) {
                    setProgress(100, progress, false)
                }
                
                // Intent для открытия приложения при клике
                val contentIntent = Intent(this@DownloadService, MainActivity::class.java)
                setContentIntent(
                    PendingIntent.getActivity(
                        this@DownloadService,
                        0,
                        contentIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
                
                // Кнопка отмены
                if (progress < 100 && progress >= 0) {
                    val cancelIntent = Intent(this@DownloadService, DownloadService::class.java).apply {
                        action = ACTION_CANCEL_DOWNLOAD
                        putExtra(EXTRA_VIDEO_ID, videoId)
                    }
                    addAction(
                        android.R.drawable.ic_menu_close_clear_cancel,
                        "Отмена",
                        PendingIntent.getService(
                            this@DownloadService,
                            1,
                            cancelIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                    )
                }
            }
            .build()
        
        if (progress >= 0 && progress <= 100) {
            startForeground(NOTIFICATION_ID, notification)
        } else {
            notificationManager.notify(NOTIFICATION_ID, notification)
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
    }
    
    companion object {
        const val ACTION_START_DOWNLOAD = "com.example.videodownloader.START_DOWNLOAD"
        const val ACTION_CANCEL_DOWNLOAD = "com.example.videodownloader.CANCEL_DOWNLOAD"
        const val EXTRA_URL = "url"
        const val EXTRA_VIDEO_ID = "video_id"
        
        /**
         * Запустить сервис загрузки
         */
        fun start(context: Context, url: String, videoId: String) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_START_DOWNLOAD
                putExtra(EXTRA_URL, url)
                putExtra(EXTRA_VIDEO_ID, videoId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        /**
         * Отменить загрузку
         */
        fun cancel(context: Context, videoId: String) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_CANCEL_DOWNLOAD
                putExtra(EXTRA_VIDEO_ID, videoId)
            }
            context.startService(intent)
        }
    }
}
