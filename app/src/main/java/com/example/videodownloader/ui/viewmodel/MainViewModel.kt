package com.example.videodownloader.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.videodownloader.data.local.AppDatabase
import com.example.videodownloader.data.model.Video
import com.example.videodownloader.data.repository.VideoRepository
import com.example.videodownloader.util.MediaStoreHelper
import com.example.videodownloader.util.SecurityManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel для главного экрана и управления состоянием приложения
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = AppDatabase.getDatabase(application)
    private val videoDao = database.videoDao()
    private val mediaStoreHelper = MediaStoreHelper(application)
    private val videoRepository = VideoRepository(videoDao, mediaStoreHelper, application)
    private val securityManager = SecurityManager(application)
    
    // Состояние авторизации
    private val _authState = MutableStateFlow<AuthState>(AuthState.NotSet)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    // Список всех видео
    val allVideos: Flow<List<Video>> = videoRepository.getAllVideos()
    
    // Поиск
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    val searchedVideos: Flow<List<Video>> = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                videoRepository.getAllVideos()
            } else {
                videoRepository.searchVideos(query)
            }
        }
    
    // Выбранный тег для фильтрации
    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag: StateFlow<String?> = _selectedTag.asStateFlow()
    
    val filteredVideos: Flow<List<Video>> = _selectedTag
        .flatMapLatest { tag ->
            if (tag == null) {
                videoRepository.getAllVideos()
            } else {
                videoRepository.getVideosByTag(tag)
            }
        }
    
    // Все уникальные теги
    private val _allTags = MutableStateFlow<List<String>>(emptyList())
    val allTags: StateFlow<List<String>> = _allTags.asStateFlow()
    
    init {
        checkAuthState()
        loadAllTags()
    }
    
    /**
     * Проверить состояние авторизации при запуске
     */
    private fun checkAuthState() {
        viewModelScope.launch {
            if (securityManager.isPasswordSet()) {
                _authState.value = AuthState.Locked
            } else {
                _authState.value = AuthState.NotSet
            }
        }
    }
    
    /**
     * Загрузить все теги
     */
    private fun loadAllTags() {
        viewModelScope.launch {
            _allTags.value = videoRepository.getAllTags()
        }
    }
    
    // ==================== АВТОРИЗАЦИЯ ====================
    
    /**
     * Установить новый пароль (первый запуск)
     */
    fun setPassword(password: String, securityQuestion: String, securityAnswer: String) {
        viewModelScope.launch {
            securityManager.setPassword(password)
            securityManager.setSecurityQuestion(securityQuestion, securityAnswer)
            _authState.value = AuthState.Unlocked
        }
    }
    
    /**
     * Войти с паролем
     */
    fun login(password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            if (securityManager.verifyPassword(password)) {
                _authState.value = AuthState.Unlocked
                onSuccess()
            } else {
                _authState.value = AuthState.Locked
            }
        }
    }
    
    /**
     * Выйти из приложения (заблокировать)
     */
    fun logout() {
        _authState.value = AuthState.Locked
    }
    
    /**
     * Получить контрольный вопрос для восстановления пароля
     */
    fun getSecurityQuestion(): String? {
        return securityManager.getSecurityQuestion()
    }
    
    /**
     * Проверить ответ на контрольный вопрос
     */
    fun verifySecurityAnswer(answer: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            val isValid = securityManager.verifySecurityAnswer(answer)
            callback(isValid)
        }
    }
    
    /**
     * Сбросить пароль после успешного ответа на контрольный вопрос
     */
    fun resetPassword(newPassword: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            securityManager.resetPassword(newPassword)
            _authState.value = AuthState.Unlocked
            callback(true)
        }
    }
    
    // ==================== ФИЛЬТРАЦИЯ И ПОИСК ====================
    
    /**
     * Установить поисковый запрос
     */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    /**
     * Выбрать тег для фильтрации
     */
    fun selectTag(tag: String?) {
        _selectedTag.value = tag
    }
    
    /**
     * Очистить фильтр по тегам
     */
    fun clearTagFilter() {
        _selectedTag.value = null
    }
    
    // ==================== УПРАВЛЕНИЕ ВИДЕО ====================
    
    /**
     * Обновить видео (после изменения названия или тегов)
     */
    fun updateVideo(video: Video) {
        viewModelScope.launch {
            videoRepository.updateVideo(video)
            loadAllTags()
        }
    }
    
    /**
     * Удалить видео
     */
    fun deleteVideo(id: Long) {
        viewModelScope.launch {
            videoRepository.deleteVideo(id)
            loadAllTags()
        }
    }
    
    /**
     * Добавить тег к видео
     */
    fun addTagToVideo(videoId: Long, tag: String) {
        viewModelScope.launch {
            videoRepository.addTagToVideo(videoId, tag)
            loadAllTags()
        }
    }
    
    /**
     * Удалить тег из видео
     */
    fun removeTagFromVideo(videoId: Long, tag: String) {
        viewModelScope.launch {
            videoRepository.removeTagFromVideo(videoId, tag)
            loadAllTags()
        }
    }
}

/**
 * Состояния авторизации
 */
sealed class AuthState {
    object NotSet : AuthState()      // Пароль ещё не установлен
    object Locked : AuthState()      // Требуется ввод пароля
    object Unlocked : AuthState()    // Пользователь авторизован
}
