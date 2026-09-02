package com.quran.quranaudio.online.quran_module.utils.tafsir

import kotlinx.coroutines.Deferred

/**
 * Small single-flight registry whose entries remove themselves at completion.
 *
 * Removal cannot depend on an awaiting UI/preload caller reaching `finally`: a
 * foreground caller may otherwise join a completed failed prefetch during that
 * gap and fail until the user opens Tafsir a second time.
 */
internal class TafsirInFlightRegistry<T> {
    data class Acquisition<T>(
        val deferred: Deferred<T>,
        val started: Boolean
    )

    private val lock = Any()
    private val entries = mutableMapOf<String, Deferred<T>>()

    fun getOrStart(key: String, starter: () -> Deferred<T>): Acquisition<T> = synchronized(lock) {
        entries[key]?.let { existing ->
            // The completion callback normally removes this entry. Checking here
            // too closes the tiny race where a foreground caller acquires the
            // lock before that callback does.
            if (!existing.isCompleted) {
                return@synchronized Acquisition(existing, false)
            }
            entries.remove(key)
        }

        val created = starter()
        entries[key] = created
        created.invokeOnCompletion {
            synchronized(lock) {
                if (entries[key] === created) {
                    entries.remove(key)
                }
            }
        }
        Acquisition(created, true)
    }

    internal fun contains(key: String): Boolean = synchronized(lock) {
        entries.containsKey(key)
    }
}

internal fun resolveTafsirRequestSlug(manifestSlug: String?, tafsirKey: String): String? =
    manifestSlug?.takeIf { it.isNotBlank() }
        ?: tafsirKey.takeIf { it.isNotBlank() }
