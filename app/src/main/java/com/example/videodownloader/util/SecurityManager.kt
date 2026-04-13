package com.example.videodownloader.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

/**
 * Менеджер для безопасного хранения пароля и настроек безопасности
 * Использует EncryptedSharedPreferences для шифрования данных
 */
class SecurityManager(private val context: Context) {
    
    private val masterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }
    
    private val encryptedPrefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            "secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    companion object {
        private const val KEY_PASSWORD_HASH = "password_hash"
        private const val KEY_SECURITY_QUESTION = "security_question"
        private const val KEY_SECURITY_ANSWER_HASH = "security_answer_hash"
        private const val KEY_IS_PASSWORD_SET = "is_password_set"
    }
    
    /**
     * Хэшировать строку с помощью SHA-256
     */
    private fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Проверить, установлен ли пароль
     */
    fun isPasswordSet(): Boolean {
        return encryptedPrefs.getBoolean(KEY_IS_PASSWORD_SET, false)
    }
    
    /**
     * Установить новый пароль
     */
    suspend fun setPassword(password: String): Unit = withContext(Dispatchers.IO) {
        val hash = hashString(password)
        encryptedPrefs.edit().apply {
            putString(KEY_PASSWORD_HASH, hash)
            putBoolean(KEY_IS_PASSWORD_SET, true)
            apply()
        }
    }
    
    /**
     * Проверить правильность пароля
     */
    suspend fun verifyPassword(password: String): Boolean = withContext(Dispatchers.IO) {
        val storedHash = encryptedPrefs.getString(KEY_PASSWORD_HASH, null)
        val inputHash = hashString(password)
        storedHash == inputHash
    }
    
    /**
     * Установить контрольный вопрос и ответ
     */
    suspend fun setSecurityQuestion(question: String, answer: String): Unit = withContext(Dispatchers.IO) {
        val answerHash = hashString(answer.lowercase().trim())
        encryptedPrefs.edit().apply {
            putString(KEY_SECURITY_QUESTION, question)
            putString(KEY_SECURITY_ANSWER_HASH, answerHash)
            apply()
        }
    }
    
    /**
     * Получить контрольный вопрос
     */
    fun getSecurityQuestion(): String? {
        return encryptedPrefs.getString(KEY_SECURITY_QUESTION, null)
    }
    
    /**
     * Проверить ответ на контрольный вопрос
     */
    suspend fun verifySecurityAnswer(answer: String): Boolean = withContext(Dispatchers.IO) {
        val storedHash = encryptedPrefs.getString(KEY_SECURITY_ANSWER_HASH, null)
        val inputHash = hashString(answer.lowercase().trim())
        storedHash == inputHash
    }
    
    /**
     * Сбросить пароль (после успешной проверки контрольного вопроса)
     */
    suspend fun resetPassword(newPassword: String): Unit = withContext(Dispatchers.IO) {
        setPassword(newPassword)
    }
    
    /**
     * Очистить все данные безопасности (для теста или сброса)
     */
    suspend fun clearAll(): Unit = withContext(Dispatchers.IO) {
        encryptedPrefs.edit().clear().apply()
    }
}
