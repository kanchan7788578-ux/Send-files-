package com.example.shared.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.shared.model.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.beamLangDataStore by preferencesDataStore(name = "beamtv_lang_prefs")

class LanguagePrefsRepository(private val context: Context) {

    private object Keys {
        val APP_LANGUAGE = stringPreferencesKey("app_language")
    }

    val languageFlow: Flow<AppLanguage> = context.beamLangDataStore.data.map { prefs ->
        when (prefs[Keys.APP_LANGUAGE]) {
            AppLanguage.ENGLISH.name -> AppLanguage.ENGLISH
            else -> AppLanguage.BENGALI
        }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.beamLangDataStore.edit { prefs ->
            prefs[Keys.APP_LANGUAGE] = language.name
        }
    }
}
