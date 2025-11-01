package ru.izhxx.aichallenge.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.izhxx.aichallenge.data.preferences.ApiKeyStore

/**
 * ViewModel для экрана настроек
 */
class SettingsViewModel(private val apiKeyStore: ApiKeyStore) : ViewModel() {
    
    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()
    
    init {
        loadApiKey()
    }
    
    /**
     * Загружает API ключ из хранилища
     */
    private fun loadApiKey() {
        viewModelScope.launch {
            try {
                val apiKey = apiKeyStore.getApiKey()
                _state.update { it.copy(apiKey = apiKey, isLoading = false) }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        error = "Не удалось загрузить API ключ: ${e.message}",
                        isLoading = false
                    )
                }
            }
        }
    }
    
    /**
     * Обновляет значение API ключа в state
     */
    fun updateApiKey(apiKey: String) {
        _state.update { it.copy(apiKey = apiKey) }
    }
    
    /**
     * Сохраняет API ключ в хранилище
     */
    fun saveApiKey() {
        val apiKey = state.value.apiKey.trim()
        if (apiKey.isBlank()) {
            _state.update { 
                it.copy(error = "API ключ не может быть пустым")
            }
            return
        }
        
        _state.update { it.copy(isLoading = true, isSaved = false, error = null) }
        
        viewModelScope.launch {
            try {
                apiKeyStore.saveApiKey(apiKey)
                _state.update { 
                    it.copy(isLoading = false, isSaved = true)
                }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        error = "Не удалось сохранить API ключ: ${e.message}",
                        isLoading = false
                    )
                }
            }
        }
    }
    
    /**
     * Очищает состояние сохранения
     */
    fun clearSavedState() {
        _state.update { it.copy(isSaved = false) }
    }
    
    /**
     * Очищает ошибку
     */
    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}

/**
 * Состояние для экрана настроек
 */
data class SettingsState(
    val apiKey: String = "",
    val isLoading: Boolean = true,
    val isSaved: Boolean = false,
    val error: String? = null
)
