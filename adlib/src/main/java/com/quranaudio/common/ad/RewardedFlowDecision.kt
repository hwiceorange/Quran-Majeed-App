package com.quranaudio.common.ad

internal enum class RewardedFlowNext {
    SHOW_REWARDED,
    WAIT,
    SHOW_FALLBACK,
    UNAVAILABLE
}

internal fun decideRewardedFlowNext(
    rewardedReady: Boolean,
    fallbackReady: Boolean,
    elapsedMs: Long,
    timeoutMs: Long = RewardedAdFlowCoordinator.REWARDED_WAIT_TIMEOUT_MS
): RewardedFlowNext = when {
    rewardedReady -> RewardedFlowNext.SHOW_REWARDED
    elapsedMs < timeoutMs -> RewardedFlowNext.WAIT
    fallbackReady -> RewardedFlowNext.SHOW_FALLBACK
    else -> RewardedFlowNext.UNAVAILABLE
}
