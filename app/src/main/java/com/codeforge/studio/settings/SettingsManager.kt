package com.codeforge.studio.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("codeforge_settings", Context.MODE_PRIVATE)

    private val _darkTheme = MutableStateFlow(prefs.getBoolean("dark_theme", true))
    val darkTheme: StateFlow<Boolean> = _darkTheme.asStateFlow()

    private val _fontSizeSp = MutableStateFlow(prefs.getInt("font_size", 14))
    val fontSizeSp: StateFlow<Int> = _fontSizeSp.asStateFlow()

    private val _tabWidth = MutableStateFlow(prefs.getInt("tab_width", 4))
    val tabWidth: StateFlow<Int> = _tabWidth.asStateFlow()

    private val _wordWrap = MutableStateFlow(prefs.getBoolean("word_wrap", false))
    val wordWrap: StateFlow<Boolean> = _wordWrap.asStateFlow()

    private val _autoSaveEnabled = MutableStateFlow(prefs.getBoolean("autosave", true))
    val autoSaveEnabled: StateFlow<Boolean> = _autoSaveEnabled.asStateFlow()

    private val _showHiddenFiles = MutableStateFlow(prefs.getBoolean("show_hidden_files", false))
    val showHiddenFiles: StateFlow<Boolean> = _showHiddenFiles.asStateFlow()

    private val _remoteExecutionUrl = MutableStateFlow(prefs.getString("remote_url", "") ?: "")
    val remoteExecutionUrl: StateFlow<String> = _remoteExecutionUrl.asStateFlow()

    fun setDarkTheme(enabled: Boolean) {
        _darkTheme.value = enabled
        prefs.edit().putBoolean("dark_theme", enabled).apply()
    }

    fun setFontSize(sizeSp: Int) {
        val clamped = sizeSp.coerceIn(10, 28)
        _fontSizeSp.value = clamped
        prefs.edit().putInt("font_size", clamped).apply()
    }

    fun setTabWidth(width: Int) {
        val clamped = width.coerceIn(2, 8)
        _tabWidth.value = clamped
        prefs.edit().putInt("tab_width", clamped).apply()
    }

    fun setWordWrap(enabled: Boolean) {
        _wordWrap.value = enabled
        prefs.edit().putBoolean("word_wrap", enabled).apply()
    }

    fun setAutoSave(enabled: Boolean) {
        _autoSaveEnabled.value = enabled
        prefs.edit().putBoolean("autosave", enabled).apply()
    }

    fun setShowHiddenFiles(enabled: Boolean) {
        _showHiddenFiles.value = enabled
        prefs.edit().putBoolean("show_hidden_files", enabled).apply()
    }

    fun setRemoteExecutionUrl(url: String) {
        _remoteExecutionUrl.value = url.trim()
        prefs.edit().putString("remote_url", url.trim()).apply()
    }
}
