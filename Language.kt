// Glassbox — author: PWBad's | planning: Claude (Anthropic) | code: DeepSeek
package com.shtuchki.glassbox.util

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/** Возвращает "system", "ru" или "en". */
fun currentLanguage(): String {
    val list = AppCompatDelegate.getApplicationLocales()
    return if (list.isEmpty()) "system" else (list.get(0)?.language ?: "system")
}

/** Меняет язык приложения сразу (активность пересоздаётся). */
fun setLanguage(tag: String) {
    val locales = if (tag == "system") {
        LocaleListCompat.getEmptyLocaleList()
    } else {
        LocaleListCompat.forLanguageTags(tag)
    }
    AppCompatDelegate.setApplicationLocales(locales)
}
