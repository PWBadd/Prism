// Glassbox — author: PWBad's | planning: Claude (Anthropic) | code: DeepSeek
package com.shtuchki.glassbox.util

/** Время в формате 3:25 или 1:02:03 */
fun formatTime(ms: Long): String {
    val total = ms.coerceAtLeast(0L) / 1000L
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%d:%02d", m, s)
}
