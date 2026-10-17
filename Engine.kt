// Prism — author: PWBad's | planning: Claude (Anthropic) | code: DeepSeek
package com.shtuchki.glassbox.player

import kotlinx.coroutines.flow.MutableStateFlow

/** Общая «шина» между приложением и сервисом (они в одном процессе). */
object Engine {
    /** Время (мс), когда таймер сна остановит музыку. null = таймер выключен. */
    val sleepEndAt = MutableStateFlow<Long?>(null)
}
