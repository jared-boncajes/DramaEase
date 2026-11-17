package com.dramaease.tv.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dramaease.tv.provider.ContentSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class TextSize(val scale: Float) { NORMAL(1.0f), LARGE(1.25f), EXTRA_LARGE(1.5f) }
enum class AppLanguage { ENGLISH, FILIPINO }

data class AppSettings(
    val easyMode: Boolean = true,
    val textSize: TextSize = TextSize.LARGE,
    val highContrast: Boolean = true,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val subtitleSize: TextSize = TextSize.LARGE,
    val source: ContentSource = ContentSource.DEMO,
)

private val Context.dataStore by preferencesDataStore(name = "dramaease_settings")

class SettingsRepository(private val context: Context) {
    private object K {
        val easy = booleanPreferencesKey("easy_mode")
        val text = stringPreferencesKey("text_size")
        val contrast = booleanPreferencesKey("high_contrast")
        val lang = stringPreferencesKey("language")
        val subs = stringPreferencesKey("subtitle_size")
        val source = stringPreferencesKey("source")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        val d = AppSettings()
        AppSettings(
            easyMode = p[K.easy] ?: d.easyMode,
            textSize = p[K.text].toEnum(d.textSize),
            highContrast = p[K.contrast] ?: d.highContrast,
            language = p[K.lang].toEnum(d.language),
            subtitleSize = p[K.subs].toEnum(d.subtitleSize),
            source = p[K.source].toEnum(d.source),
        )
    }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { p ->
            val cur = AppSettings(
                easyMode = p[K.easy] ?: true,
                textSize = p[K.text].toEnum(TextSize.LARGE),
                highContrast = p[K.contrast] ?: true,
                language = p[K.lang].toEnum(AppLanguage.ENGLISH),
                subtitleSize = p[K.subs].toEnum(TextSize.LARGE),
                source = p[K.source].toEnum(ContentSource.DEMO),
            )
            val n = transform(cur)
            p[K.easy] = n.easyMode
            p[K.text] = n.textSize.name
            p[K.contrast] = n.highContrast
            p[K.lang] = n.language.name
            p[K.subs] = n.subtitleSize.name
            p[K.source] = n.source.name
        }
    }
}

private inline fun <reified E : Enum<E>> String?.toEnum(default: E): E =
    enumValues<E>().firstOrNull { it.name == this } ?: default
