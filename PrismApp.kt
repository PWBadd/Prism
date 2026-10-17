// Prism — author: PWBad's | planning: Claude (Anthropic) | code: DeepSeek
package com.shtuchki.glassbox

import android.app.Application
import com.shtuchki.glassbox.data.Repo
import com.shtuchki.glassbox.perf.PerformanceManager

/** Запускается один раз при старте процесса: загружает данные и следит за нагревом. */
class PrismApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Repo.init(this)
        PerformanceManager.init(this)
    }
}
