package com.quran.quranaudio.online.tafsir

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Process-only Tafsir native-ad state. Nothing is persisted: closing the process resets both the
 * subscription-first funnel and an earned Tafsir-session reward.
 */
object TafsirSessionAdFreeManager {
    private val active = AtomicBoolean(false)
    private val rewardedAlternativeEnabled = AtomicBoolean(false)

    fun isActive(): Boolean = active.get()

    fun activate() {
        active.set(true)
    }

    fun enableRewardedAlternative() {
        rewardedAlternativeEnabled.set(true)
    }

    fun shouldOfferRewardedAlternative(): Boolean =
        rewardedAlternativeEnabled.get() && !active.get()

    internal fun resetForTests() {
        active.set(false)
        rewardedAlternativeEnabled.set(false)
    }
}
