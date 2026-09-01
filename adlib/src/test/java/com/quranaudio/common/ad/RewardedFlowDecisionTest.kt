package com.quranaudio.common.ad

import org.junit.Assert.assertEquals
import org.junit.Test

class RewardedFlowDecisionTest {
    @Test
    fun rewardedCacheAlwaysWins() {
        assertEquals(
            RewardedFlowNext.SHOW_REWARDED,
            decideRewardedFlowNext(true, true, 8_000L)
        )
    }

    @Test
    fun waitsForRewardBeforeDeadline() {
        assertEquals(
            RewardedFlowNext.WAIT,
            decideRewardedFlowNext(false, true, 7_999L)
        )
    }

    @Test
    fun usesRewardedInterstitialAtDeadline() {
        assertEquals(
            RewardedFlowNext.SHOW_FALLBACK,
            decideRewardedFlowNext(false, true, 8_000L)
        )
    }

    @Test
    fun exposesRetryWhenNeitherFormatIsReady() {
        assertEquals(
            RewardedFlowNext.UNAVAILABLE,
            decideRewardedFlowNext(false, false, 8_000L)
        )
    }
}
