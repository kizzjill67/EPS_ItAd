package com.example.videodownloader.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.videodownloader.data.model.Video

/**
 * Экран списка видео с поддержкой фильтрации по тегам
 */
@Composable
fun VideoListScreen(
    videos: List<Video>,
    allTags: List<String>,
    selectedTag: String?,
    onVideoClick: (Video) -> Unit,
    onTagSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Фильтр по тегам
        if (allTags.isNotEmpty()) {
            TagFilterBar(
                tags = allTags,
                selectedTag = selectedTag,
                onTagSelected = onTagSelected
            )
        }
        
        // Список видео
        if (videos.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Нет видео",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(videos, key = { it.id }) { video ->
                    VideoItem(
                        video = video,
                        onClick = { onVideoClick(video) }
                    )
                }
            }
        }
    }
}

/**
 * Компонент для отображения одного видео в списке
 */
@Composable
fun VideoItem(
    video: Video,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = video.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Теги
            if (video.tags.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    video.tags.take(3).forEach { tag ->
                        AssistChip(
                            onClick = { },
                            label = { Text(tag, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                    if (video.tags.size > 3) {
                        Text(
                            text = "+${video.tags.size - 3}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Дополнительная информация
            Text(
                text = buildString {
                    if (video.isFromGallery) {
                        append("Из галереи")
                    } else {
                        append("Загружено")
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Панель фильтров по тегам
 */
@Composable
fun TagFilterBar(
    tags: List<String>,
    selectedTag: String?,
    onTagSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(vertical = 8.dp)) {
        Text(
            text = "Фильтр по тегам:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Кнопка "Все"
            FilterChip(
                selected = selectedTag == null,
                onClick = { onTagSelected(null) },
                label = { Text("Все") }
            )
            
            // Остальные теги
            tags.forEach { tag ->
                FilterChip(
                    selected = selectedTag == tag,
                    onClick = { onTagSelected(if (selectedTag == tag) null else tag) },
                    label = { Text(tag) }
                )
            }
        }
    }
}
