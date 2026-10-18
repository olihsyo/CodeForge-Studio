package com.codeforge.studio

import android.content.SharedPreferences
import com.codeforge.studio.settings.SettingsManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsManagerTest {

    private class FakeSharedPreferences : SharedPreferences {
        private val data = mutableMapOf<String, Any>()

        override fun getAll(): MutableMap<String, *> = data
        override fun getString(key: String?, defValue: String?): String? = (data[key] as? String) ?: defValue
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = defValues
        override fun getInt(key: String?, defValue: Int): Int = (data[key] as? Int) ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = (data[key] as? Long) ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = (data[key] as? Float) ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = (data[key] as? Boolean) ?: defValue
        override fun contains(key: String?): Boolean = data.containsKey(key)
        override fun edit(): SharedPreferences.Editor = FakeEditor(data)
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        private class FakeEditor(private val data: MutableMap<String, Any>) : SharedPreferences.Editor {
            private val pending = mutableMapOf<String, Any?>()
            private var clearAll = false

            override fun putString(key: String?, value: String?): SharedPreferences.Editor = apply { key?.let { pending[it] = value } }
            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor = apply { key?.let { pending[it] = values } }
            override fun putInt(key: String?, value: Int): SharedPreferences.Editor = apply { key?.let { pending[it] = value } }
            override fun putLong(key: String?, value: Long): SharedPreferences.Editor = apply { key?.let { pending[it] = value } }
            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = apply { key?.let { pending[it] = value } }
            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = apply { key?.let { pending[it] = value } }
            override fun remove(key: String?): SharedPreferences.Editor = apply { key?.let { pending[it] = null } }
            override fun clear(): SharedPreferences.Editor = apply { clearAll = true }
            override fun commit(): Boolean {
                if (clearAll) data.clear()
                pending.forEach { (k, v) ->
                    if (v == null) data.remove(k) else data[k] = v
                }
                pending.clear()
                return true
            }
            override fun apply() { commit() }
        }
    }

    @Test
    fun testFontSizeClamping() {
        val fakePrefs = FakeSharedPreferences()
        val settings = SettingsManager(fakePrefs)

        settings.setFontSize(5)
        assertEquals(10, settings.fontSizeSp.value)
        assertEquals(10, fakePrefs.getInt("font_size", 0))

        settings.setFontSize(40)
        assertEquals(28, settings.fontSizeSp.value)
        assertEquals(28, fakePrefs.getInt("font_size", 0))

        settings.setFontSize(16)
        assertEquals(16, settings.fontSizeSp.value)
        assertEquals(16, fakePrefs.getInt("font_size", 0))
    }

    @Test
    fun testTabWidthClamping() {
        val fakePrefs = FakeSharedPreferences()
        val settings = SettingsManager(fakePrefs)

        settings.setTabWidth(1)
        assertEquals(2, settings.tabWidth.value)

        settings.setTabWidth(12)
        assertEquals(8, settings.tabWidth.value)

        settings.setTabWidth(4)
        assertEquals(4, settings.tabWidth.value)
    }

    @Test
    fun testThemeAndAutoSaveToggle() {
        val fakePrefs = FakeSharedPreferences()
        val settings = SettingsManager(fakePrefs)

        assertTrue(settings.darkTheme.value)
        settings.setDarkTheme(false)
        assertFalse(settings.darkTheme.value)
        assertFalse(fakePrefs.getBoolean("dark_theme", true))

        assertTrue(settings.autoSaveEnabled.value)
        settings.setAutoSave(false)
        assertFalse(settings.autoSaveEnabled.value)
        assertFalse(fakePrefs.getBoolean("autosave", true))
    }

    @Test
    fun testGitConfig() {
        val fakePrefs = FakeSharedPreferences()
        val settings = SettingsManager(fakePrefs)

        settings.setGitConfig("Alice Dev", "alice@example.com")
        assertEquals("Alice Dev", settings.gitUserName.value)
        assertEquals("alice@example.com", settings.gitUserEmail.value)
        assertEquals("Alice Dev", fakePrefs.getString("git_user_name", ""))
        assertEquals("alice@example.com", fakePrefs.getString("git_user_email", ""))
    }
}
