// Prism — author: PWBad's | planning: Claude (Anthropic) | code: DeepSeek
package com.shtuchki.glassbox.perf

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.shtuchki.glassbox.data.Repo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Следит за нагревом и батареей. Когда телефон горячий, садится или включён экономный режим,
 * lite = true, и приложение отключает украшения (вращение обложки, мягкие пятна, длинные затухания).
 */
object PerformanceManager {
    private val _lite = MutableStateFlow(false)
    val lite: StateFlow<Boolean> = _lite.asStateFlow()

    private val _warm = MutableStateFlow(false)
    val warm: StateFlow<Boolean> = _warm.asStateFlow()

    private var started = false
    private var thermalHot = false
    private var powerSave = false
    private var batteryLow = false
    private var coolJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun init(app: Application) {
        if (started) return
        started = true
        val pm = app.getSystemService(Context.POWER_SERVICE) as PowerManager
        powerSave = pm.isPowerSaveMode

        pm.addThermalStatusListener(app.mainExecutor) { status ->
            if (status >= PowerManager.THERMAL_STATUS_MODERATE) {
                coolJob?.cancel()
                thermalHot = true
                recompute()
            } else if (thermalHot) {
                // Возвращаем эффекты не сразу, а через 20 секунд (чтобы не мигало)
                coolJob?.cancel()
                coolJob = scope.launch {
                    delay(20_000)
                    thermalHot = false
                    recompute()
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }
        ContextCompat.registerReceiver(app, object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                powerSave = pm.isPowerSaveMode
                if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 100)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
                    batteryLow = !charging && level * 100 / scale < 15
                }
                recompute()
            }
        }, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        scope.launch {
            Repo.data.map { it.perfMode }.distinctUntilChanged().collect { recompute() }
        }
    }

    private fun recompute() {
        val mode = Repo.data.value.perfMode
        _warm.value = thermalHot && mode != "max"
        _lite.value = when (mode) {
            "eco" -> true
            "max" -> false
            else -> thermalHot || powerSave || batteryLow
        }
    }
}
