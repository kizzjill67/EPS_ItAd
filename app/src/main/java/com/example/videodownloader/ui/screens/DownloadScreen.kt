package com.example.videodownloader.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Экран загрузки видео с полем ввода URL и отображением прогресса
 */
@Composable
fun DownloadScreen(
    urls: String,
    onUrlsChange: (String) -> Unit,
    onStartDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    isDownloading: Boolean,
    downloadProgress: Int,
    downloadStatus: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Заголовок
        Text(
            text = "Загрузка видео",
            style = MaterialTheme.typography.headlineMedium
        )
        
        // Поле ввода URL
        OutlinedTextField(
            value = urls,
            onValueChange = onUrlsChange,
            label = { Text("Ссылка на видео") },
            placeholder = { Text("Вставьте ссылку (YouTube, Vimeo, TikTok и др.)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 5
        )
        
        // Кнопка начала загрузки
        Button(
            onClick = onStartDownload,
            enabled = urls.isNotBlank() && !isDownloading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.Download,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isDownloading) "Загрузка..." else "Начать загрузку")
        }
        
        Divider()
        
        // Прогресс загрузки
        if (isDownloading || downloadProgress > 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = downloadStatus,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "$downloadProgress%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    LinearProgressIndicator(
                        progress = downloadProgress / 100f,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Кнопка отмены
                    if (isDownloading) {
                        OutlinedButton(
                            onClick = onCancelDownload,
                            modifier = Modifier.align(Alignment.End),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Close,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Отменить")
                        }
                    }
                }
            }
        }
        
        // Информация о поддерживаемых сайтах
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Поддерживаемые сайты:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "YouTube, Vimeo, TikTok, Instagram, Twitter, Facebook и более 1000 других сайтов благодаря yt-dlp",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
