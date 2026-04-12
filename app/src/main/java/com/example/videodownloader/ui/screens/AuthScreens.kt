package com.example.videodownloader.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Экран входа с вводом пароля
 */
@Composable
fun LoginScreen(
    onLogin: (String) -> Unit,
    onForgotPassword: () -> Unit,
    modifier: Modifier = Modifier
) {
    var password by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = androidx.compose.material.icons.Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Введите пароль",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        OutlinedTextField(
            value = password,
            onValueChange = { 
                password = it
                isError = false
            },
            label = { Text("Пароль") },
            visualTransformation = androidx.compose.foundation.text.input.PasswordVisualTransformation(),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onDone = { onLogin(password) }
            ),
            singleLine = true,
            isError = isError,
            modifier = Modifier.fillMaxWidth()
        )
        
        if (isError) {
            Text(
                text = "Неверный пароль",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { onLogin(password) },
            enabled = password.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Войти")
        }
        
        TextButton(onClick = onForgotPassword) {
            Text("Забыли пароль?")
        }
    }
}

/**
 * Экран установки пароля (первый запуск)
 */
@Composable
fun SetPasswordScreen(
    onSave: (password: String, question: String, answer: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var securityQuestion by remember { mutableStateOf("") }
    var securityAnswer by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(androidx.compose.foundation.verticalScroll.rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Установка безопасности",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Text(
            text = "Придумайте пароль для защиты приложения и контрольный вопрос для восстановления доступа",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Divider()
        
        // Пароль
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Пароль") },
            visualTransformation = androidx.compose.foundation.text.input.PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Подтвердите пароль") },
            visualTransformation = androidx.compose.foundation.text.input.PasswordVisualTransformation(),
            singleLine = true,
            isError = password != confirmPassword && confirmPassword.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        )
        
        Divider()
        
        // Контрольный вопрос
        OutlinedTextField(
            value = securityQuestion,
            onValueChange = { securityQuestion = it },
            label = { Text("Контрольный вопрос") },
            placeholder = { Text("Например: Кличка вашего первого питомца?") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        
        OutlinedTextField(
            value = securityAnswer,
            onValueChange = { securityAnswer = it },
            label = { Text("Ответ на вопрос") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        
        error?.let { err ->
            Text(
                text = err,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = {
                when {
                    password.isEmpty() -> error = "Введите пароль"
                    password.length < 4 -> error = "Пароль должен быть не менее 4 символов"
                    password != confirmPassword -> error = "Пароли не совпадают"
                    securityQuestion.isEmpty() -> error = "Введите контрольный вопрос"
                    securityAnswer.isEmpty() -> error = "Введите ответ на вопрос"
                    else -> {
                        error = null
                        onSave(password, securityQuestion, securityAnswer)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Сохранить")
        }
    }
}

/**
 * Экран восстановления пароля через контрольный вопрос
 */
@Composable
fun ForgotPasswordScreen(
    securityQuestion: String?,
    onSubmit: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var answer by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = androidx.compose.material.icons.Icons.Default.Help,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Восстановление пароля",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (securityQuestion != null) {
            Text(
                text = securityQuestion,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            OutlinedTextField(
                value = answer,
                onValueChange = { 
                    answer = it
                    isError = false
                },
                label = { Text("Ваш ответ") },
                singleLine = true,
                isError = isError,
                modifier = Modifier.fillMaxWidth()
            )
            
            if (isError) {
                Text(
                    text = "Неверный ответ",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { onSubmit(answer) },
                enabled = answer.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Проверить")
            }
        } else {
            Text(
                text = "Контрольный вопрос не установлен",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
        }
        
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Отмена")
        }
    }
}
