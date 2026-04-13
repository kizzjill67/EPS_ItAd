package com.example.videodownloader

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.videodownloader.ui.screens.*
import com.example.videodownloader.ui.theme.VideoDownloaderTheme
import com.example.videodownloader.ui.viewmodel.AuthState
import com.example.videodownloader.ui.viewmodel.MainViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Главная активность приложения
 */
class MainActivity : ComponentActivity() {
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Обработка результатов запроса разрешений
        val allGranted = permissions.all { it.value }
        if (!allGranted) {
            // Некоторые разрешения не предоставлены
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Запрос разрешений при запуске
        checkAndRequestPermissions()
        
        setContent {
            VideoDownloaderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
    
    /**
     * Проверка и запрос необходимых разрешений
     */
    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()
        
        // Интернет (обычно всегда есть в манифесте)
        // Для Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_VIDEO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
            }
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            // Для старых версий Android
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
        
        if (permissions.isNotEmpty()) {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }
    }
}

/**
 * Навигация по приложению
 */
@Composable
fun AppNavigation(
    viewModel: MainViewModel = viewModel()
) {
    val navController = rememberNavController()
    val authState by viewModel.authState.collectAsState()
    
    NavHost(
        navController = navController,
        startDestination = when (authState) {
            AuthState.NotSet -> "setPassword"
            AuthState.Locked -> "login"
            AuthState.Unlocked -> "main"
        }
    ) {
        // Экран установки пароля (первый запуск)
        composable("setPassword") {
            SetPasswordScreen(
                onSave = { password, question, answer ->
                    viewModel.setPassword(password, question, answer)
                    navController.navigate("main") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        
        // Экран входа
        composable("login") {
            var showForgotPassword by remember { mutableStateOf(false) }
            
            if (showForgotPassword) {
                ForgotPasswordScreen(
                    securityQuestion = viewModel.getSecurityQuestion(),
                    onSubmit = { answer ->
                        viewModel.verifySecurityAnswer(answer) { isValid ->
                            if (isValid) {
                                // Показать диалог для нового пароля
                                // Упрощённо - просто сбрасываем
                                navController.navigate("setPassword") {
                                    popUpTo(0) { inclusive = true }
                                }
                            } else {
                                showForgotPassword = false
                            }
                        }
                    },
                    onCancel = { showForgotPassword = false }
                )
            } else {
                LoginScreen(
                    onLogin = { password ->
                        viewModel.login(password) {
                            navController.navigate("main") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    },
                    onForgotPassword = { showForgotPassword = true }
                )
            }
        }
        
        // Главный экран с навигацией
        composable("main") {
            MainScreen(
                viewModel = viewModel,
                onNavigateToDetails = { videoId ->
                    navController.navigate("videoDetail/$videoId")
                }
            )
        }
        
        // Экран деталей видео
        composable(
            route = "videoDetail/{videoId}",
            arguments = listOf(navArgument("videoId") { type = NavType.LongType })
        ) { backStackEntry ->
            val videoId = backStackEntry.arguments?.getLong("videoId") ?: return@composable
            
            // Получаем видео из ViewModel (упрощённо)
            // В реальном приложении нужно передавать данные через SavedStateHandle
            VideoDetailPlaceholder(
                videoId = videoId,
                onNavigateBack = { navController.popBackStack() },
                onUpdateVideo = { viewModel.updateVideo(it) },
                onDeleteVideo = { viewModel.deleteVideo(it) },
                onAddTag = { id, tag -> viewModel.addTagToVideo(id, tag) },
                onRemoveTag = { id, tag -> viewModel.removeTagFromVideo(id, tag) }
            )
        }
    }
}

/**
 * Главный экран с нижней навигацией
 */
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onNavigateToDetails: (Long) -> Unit
) {
    val videos by viewModel.searchedVideos.collectAsState(initial = emptyList())
    val allTags by viewModel.allTags.collectAsState(initial = emptyList())
    val selectedTag by viewModel.selectedTag.collectAsState(initial = null)
    
    var selectedTab by remember { mutableStateOf(0) }
    var downloadUrls by remember { mutableStateOf("") }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0) }
    var downloadStatus by remember { mutableStateOf("") }
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.VideoLibrary, contentDescription = null) },
                    label = { Text("Видео") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Download, contentDescription = null) },
                    label = { Text("Загрузка") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    label = { Text("Из галереи") },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 }
                )
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                0 -> {
                    // Список видео
                    VideoListScreen(
                        videos = videos,
                        allTags = allTags,
                        selectedTag = selectedTag,
                        onVideoClick = onNavigateToDetails,
                        onTagSelected = viewModel::selectTag
                    )
                }
                1 -> {
                    // Экран загрузки
                    DownloadScreen(
                        urls = downloadUrls,
                        onUrlsChange = { downloadUrls = it },
                        onStartDownload = {
                            isDownloading = true
                            // Здесь должна быть логика запуска загрузки через сервис
                        },
                        onCancelDownload = {
                            isDownloading = false
                            downloadProgress = 0
                        },
                        isDownloading = isDownloading,
                        downloadProgress = downloadProgress,
                        downloadStatus = downloadStatus
                    )
                }
                2 -> {
                    // Кнопка добавления из галереи
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        Button(onClick = { /* Открыть галерею */ }) {
                            Text("Выбрать из галереи")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Заглушка для экрана деталей (для демонстрации)
 */
@Composable
fun VideoDetailPlaceholder(
    videoId: Long,
    onNavigateBack: () -> Unit,
    onUpdateVideo: (com.example.videodownloader.data.model.Video) -> Unit,
    onDeleteVideo: (Long) -> Unit,
    onAddTag: (Long, String) -> Unit,
    onRemoveTag: (Long, String) -> Unit
) {
    // В реальном приложении здесь нужно получить данные о видео из ViewModel
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text("Экран настроек видео #$videoId")
        Button(onClick = onNavigateBack, modifier = Modifier.padding(top = 16.dp)) {
            Text("Назад")
        }
    }
}
