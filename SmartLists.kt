// Prism — author: PWBad's | planning: Claude (Anthropic) | code: DeepSeek
package com.shtuchki.glassbox.data

/** Умные списки: считаются из библиотеки и статистики прослушивания. */
object SmartLists {
    private const val MONTH_MS = 30L * 24 * 60 * 60 * 1000

    fun recentlyAdded(all: List<Track>): List<Track> =
        all.filter { !it.isVideo }.sortedByDescending { it.dateAdded }.take(50)

    fun mostPlayed(all: List<Track>, data: AppData): List<Track> =
        all.filter { (data.playCounts[it.mediaId] ?: 0) > 0 }
            .sortedByDescending { data.playCounts[it.mediaId] ?: 0 }
            .take(50)

    fun notPlayedForAWhile(all: List<Track>, data: AppData): List<Track> {
        val limit = System.currentTimeMillis() - MONTH_MS
        return all.filter { !it.isVideo && (data.lastPlayed[it.mediaId] ?: 0L) < limit }
            .sortedBy { data.lastPlayed[it.mediaId] ?: 0L }
            .take(50)
    }
}
