package com.quran.quranaudio.online.home

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Process-only state for the bottom home native ad. It is intentionally separate from Tafsir and
 * is never persisted, so the rewarded benefit exactly matches the copy shown to the user.
 */
object HomeSessionAdFreeManager {
    private val active = AtomicBoolean(false)
    private val rewardedAlternativeEnabled = AtomicBoolean(false)

    @JvmStatic
    fun isActive(): Boolean = active.get()

    @JvmStatic
    fun activate() {
        active.set(true)
    }

    @JvmStatic
    fun enableRewardedAlternative() {
        rewardedAlternativeEnabled.set(true)
    }

    @JvmStatic
    fun shouldOfferRewardedAlternative(): Boolean =
        rewardedAlternativeEnabled.get() && !active.get()

    internal fun resetForTests() {
        active.set(false)
        rewardedAlternativeEnabled.set(false)
    }
}
