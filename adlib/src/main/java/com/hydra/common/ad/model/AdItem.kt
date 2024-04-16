package com.hydra.common.ad.model

import android.os.SystemClock
import com.google.android.gms.ads.AdListener
import com.hydra.common.ad.AdConfig.AD_CACHE_MAX_TIME
import com.hydra.common.ad.AdConfig.AD_LOADING_TIME_OUT

data class AdItem(
    val id: String,
    var loadingState: LoadingState = LoadingState.LOADING,
    var ad: Any? = null,
    private val cacheAt: Long = SystemClock.elapsedRealtime(),
    var listener: AdListener? = null
) {
    fun isValid(maxCacheTime: Long = AD_CACHE_MAX_TIME) =
        SystemClock.elapsedRealtime() - cacheAt <= maxCacheTime && loadingState == LoadingState.LOADED

    /**
     * 是否超时
     */
    fun isTimeOut() =
        loadingState == LoadingState.LOADING && SystemClock.elapsedRealtime() - cacheAt > AD_LOADING_TIME_OUT
}

enum class LoadingState {
    LOADING,
    LOADED,
    FAILED
}