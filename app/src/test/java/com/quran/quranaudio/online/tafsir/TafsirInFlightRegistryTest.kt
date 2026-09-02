package com.quran.quranaudio.online.tafsir

import com.quran.quranaudio.online.quran_module.utils.tafsir.TafsirInFlightRegistry
import com.quran.quranaudio.online.quran_module.utils.tafsir.resolveTafsirRequestSlug
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class TafsirInFlightRegistryTest {
    @Test
    fun activeRequestIsShared() {
        val registry = TafsirInFlightRegistry<Result<String>>()
        val first = CompletableDeferred<Result<String>>()

        val started = registry.getOrStart("en-1-1") { first }
        val joined = registry.getOrStart("en-1-1") {
            CompletableDeferred(Result.success("duplicate"))
        }

        assertTrue(started.started)
        assertFalse(joined.started)
        assertSame(first, joined.deferred)
        assertTrue(registry.contains("en-1-1"))
    }

    @Test
    fun completedFailureIsEvictedBeforeNextForegroundRequest() {
        val registry = TafsirInFlightRegistry<Result<String>>()
        val failed = CompletableDeferred(Result.failure<String>(IllegalStateException("cold manifest")))

        val first = registry.getOrStart("en-1-1") { failed }
        assertTrue(first.started)
        assertFalse(registry.contains("en-1-1"))

        val replacement = CompletableDeferred<Result<String>>()
        val second = registry.getOrStart("en-1-1") { replacement }

        assertTrue(second.started)
        assertNotSame(first.deferred, second.deferred)
        assertSame(replacement, second.deferred)
    }

    @Test
    fun completedSuccessIsNotReusedForARefresh() {
        val registry = TafsirInFlightRegistry<Result<String>>()
        val completed = CompletableDeferred(Result.success("old"))

        registry.getOrStart("en-1-1") { completed }
        val replacement = CompletableDeferred<Result<String>>()
        val refreshed = registry.getOrStart("en-1-1") { replacement }

        assertTrue(refreshed.started)
        assertSame(replacement, refreshed.deferred)
    }

    @Test
    fun savedKeyIsSafeFallbackUntilManifestSlugIsReady() {
        assertEquals("en-tafisr-ibn-kathir", resolveTafsirRequestSlug(null, "en-tafisr-ibn-kathir"))
        assertEquals("manifest-slug", resolveTafsirRequestSlug("manifest-slug", "saved-key"))
        assertEquals(null, resolveTafsirRequestSlug(null, ""))
    }
}
