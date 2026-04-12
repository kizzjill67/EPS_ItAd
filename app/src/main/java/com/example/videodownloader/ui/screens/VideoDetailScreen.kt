package com.example.videodownloader.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.videodownloader.data.model.Video

/**
 * Экран деталей видео с возможностью редактирования названия и тегов
 */
@Composable
fun VideoDetailScreen(
    video: Video,
    onNavigateBack: () -> Unit,
    onUpdateVideo: (Video) -> Unit,
    onDeleteVideo: (Long) -> Unit,
    onAddTag: (Long, String) -> Unit,
    onRemoveTag: (Long, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf(video.title) }
    var newTag by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Заголовок
        Text(
            text = "Настройки видео",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Поле для переименования
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Название") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = false,
            maxLines = 3
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(
            onClick = {
                if (title.isNotBlank() && title != video.title) {
                    onUpdateVideo(video.copy(title = title))
                }
            },
            enabled = title.isNotBlank() && title != video.title,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Сохранить название")
        }
        
        Divider(modifier = Modifier.padding(vertical = 16.dp))
        
        // Секция тегов
        Text(
            text = "Теги",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Список текущих тегов
        if (video.tags.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                video.tags.forEach { tag ->
                    AssistChip(
                        onClick = { onRemoveTag(video.id, tag) },
                        label = { Text(tag) },
                        trailingIcon = {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Close,
                                contentDescription = "Удалить тег",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }
        } else {
            Text(
                text = "Нет тегов",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Добавление нового тега
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newTag,
                onValueChange = { newTag = it },
                label = { Text("Новый тег") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            
            Button(
                onClick = {
                    if (newTag.isNotBlank()) {
                        onAddTag(video.id, newTag.trim())
                        newTag = ""
                    }
                },
                enabled = newTag.isNotBlank()
            ) {
                Text("Добавить")
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Кнопка удаления
        Button(
            onClick = { showDeleteDialog = true },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.Delete,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Удалить видео")
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Кнопка назад
        OutlinedButton(
            onClick = onNavigateBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Назад")
        }
    }
    
    // Диалог подтверждения удаления
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удаление видео") },
            text = { Text("Вы уверены, что хотите удалить это видео? Это действие нельзя отменить.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteVideo(video.id)
                        showDeleteDialog = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}
