package com.quran.quranaudio.quiz.extension

import android.app.Activity
import com.quran.quranaudio.quiz.ad.ExternalAdConfig
import com.quran.quranaudio.quiz.base.CloudManager
import com.quran.quranaudio.quiz.base.Constants
import com.quranaudio.quiz.quiz.QuizGemManager
import com.quran.quranaudio.quiz.utils.AppConfig
import com.quran.quranaudio.quiz.utils.LoadingDialog
import com.quran.quranaudio.quiz.utils.Tasks
import com.quran.quranaudio.quiz.utils.UserInfoUtils
import com.quranaudio.common.ad.AdConfig
import com.quranaudio.common.ad.AdFactory
import com.quranaudio.common.ad.AdShowCallback
import com.quranaudio.common.ad.RewardedAdFlowCoordinator
import com.quranaudio.common.ad.model.AdItem
import com.quranaudio.common.ad.model.RewardItem
import com.quran.quranaudio.quiz.R

fun hasOpenAd(): Boolean {
    return AdFactory.hasAppOpenAd(AdConfig.AD_APPOPEN)
}
/**
 * 判断Activity是否可用
 */
fun <T : Activity?> T.isValid(): Boolean {
    return this != null && !this.isFinishing && !this.isDestroyed
}

fun Activity.showOpenAdByPool(
    callbacks: Function1<Boolean, Unit>,
    showCallback: (() -> Unit)? = null
) {
    AdFactory.showAppOpenAd(this, AdConfig.AD_APPOPEN, object : AdShowCallback {
        override fun onAdImpression(p0: AdItem?) {
        }

        override fun onAdClicked(p0: AdItem?) {
        }

        override fun onUserEarnedReward(p0: AdItem?, p1: RewardItem?) {
        }

        override fun onAdClosed(p0: AdItem?) {
            callbacks.invoke(true)
        }

        override fun onShow(p0: AdItem?) {
            showCallback?.invoke()
        }

        override fun onShowFail() {
            callbacks.invoke(false)
        }
    })
}

fun hasInterAdByPool(adPosition: String? = null): Boolean {
    return AdFactory.hasInterAd(adPosition?: AdConfig.AD_INTERS)
}

fun Activity.hasInterAdOrNativeFallback(adPosition: String? = null): Boolean =
    hasInterAdByPool(adPosition) || AdFactory.hasFullScreenNativeFallback(this)

fun hasRewardAdByPool(adPosition: String): Boolean {
    return AdFactory.hasRewardAd(adPosition)
}

// 展示高价值广告（新用户广告）
fun Activity.showInterAd(
    adPosition: String,
    callbacks: Function1<Boolean, Unit>,
    showCallback: (() -> Unit)? = null,
) {
    if (this.isValid()) {
        AdFactory.showInterstitialAd(this, adPosition, "", object : AdShowCallback {
            override fun onAdImpression(p0: AdItem?) {
            }

            override fun onAdClicked(p0: AdItem?) {
            }

            override fun onUserEarnedReward(p0: AdItem?, p1: RewardItem?) {
            }

            override fun onAdClosed(p0: AdItem?) {
                callbacks.invoke(true)
            }

            override fun onShow(p0: AdItem?) {
                showCallback?.invoke()
            }

            override fun onShowFail() {
                callbacks.invoke(false)
            }
        })
    }
}

fun Activity.showInterAdByPoolNew(
    adPosition: String,
    functionTag: String,
    level: Int = 0,
    beforeShowCallbacks: ((Boolean) -> Unit)?,
    callbacks: Function1<Boolean, Unit>,
) {
    showInterAdByPoolNew(adPosition, functionTag, level, beforeShowCallbacks, callbacks, null)
}

fun Activity.showInterAdByPoolNew(
    adPosition: String,
    functionTag: String,
    level: Int = 0,
    beforeShowCallbacks: ((Boolean) -> Unit)?,
    callbacks: Function1<Boolean, Unit>,
    showCallback: (() -> Unit)? = null,
    skipNewUserCheck: Boolean = false  // 🎯 答题模块专用：跳过新用户检查
) {
    // ⭐ 新用户首日不展示广告（避免浪费和展示率异常）
    // 🔧 答题模块例外：不受新老用户限制，直接展示广告
    val isNewUserFirstDay = UserInfoUtils.isNewUser() && AppConfig.isInstallFirstDay
    if (isNewUserFirstDay && !skipNewUserCheck) {
        android.util.Log.d("AdExtension", "🚫 New user first day - skipping interstitial ad")
        beforeShowCallbacks?.invoke(false)
        callbacks.invoke(false)
        return
    }
    
    var canShow = CloudManager.adShowPercent(level)
    //在原有百分比基础上额外添加控制逻辑，shenghe控制和organic控制
    if (canShow) {
        if (CloudManager.isSysViewTime()) {
            canShow = CloudManager.isShowAdBySysView()
        }
    }

    val wrapCallback: (Boolean) -> Unit = {
        if (!it) {
            callbacks.invoke(false)
        } else {
            callbacks.invoke(true)
        }
    }
    beforeShowCallbacks?.invoke(canShow)
    if (canShow) {
        // ✅ 只在开始时检查一次生命周期
        if (this.isValid()) {
            if (!hasInterAdOrNativeFallback(adPosition)) {
                wrapCallback.invoke(false)
                return
            }
            val loadingDialog = LoadingDialog(this, R.string.quran_loading_ad.getResString())
            loadingDialog.show()
            
            // ⚡ 优化延迟时间：从 1000ms 缩短至 500ms
            // 原因：缩短延迟窗口期，减少用户快速操作导致的广告丢失，同时保持视觉流畅
            Tasks.postDelayedByUI({
                // ✅ 移除第二次 isValid() 检查，避免广告展示率下降
                // 原因：1秒延迟期间用户可能快速操作导致Activity状态变化，但广告仍应展示
                // AdFactory.showInterstitialAd 内部会处理Activity生命周期问题
                
                // ✅ 优化 Loading 对话框 dismiss 时机，避免 Race Condition
                // 检查对话框是否正在显示，避免 "View not attached to window manager" 异常
                try {
                    if (loadingDialog.isShowing) {
                    loadingDialog.dismiss()
                    }
                } catch (e: Exception) {
                    android.util.Log.w("AdExtension", "⚠️ Failed to dismiss loading dialog", e)
                }
                
                    AdFactory
                        .showInterstitialAd(this, adPosition, functionTag, object : AdShowCallback {
                            override fun onAdImpression(p0: AdItem?) {
                                CloudManager.adLastShowTime = System.currentTimeMillis()
                            }

                            override fun onAdClicked(p0: AdItem?) {
                            }

                            override fun onUserEarnedReward(p0: AdItem?, p1: RewardItem?) {
                            }

                            override fun onAdClosed(p0: AdItem?) {
                                wrapCallback.invoke(true)
                            }

                            override fun onShow(p0: AdItem?) {
                                showCallback?.invoke()
                                CloudManager.adLastShowTime = System.currentTimeMillis()
                            }

                            override fun onShowFail() {
                                wrapCallback.invoke(false)
                            }
                        })
            }, 500L)
        } else {
            // ✅ 第一次检查失败，确保回调
            android.util.Log.w("AdExtension", "⚠️ Activity not valid, skipping interstitial ad")
            wrapCallback.invoke(false)
        }

    } else {
        wrapCallback.invoke(false)
    }
}

fun Activity.showRewardAd(
    adPosition: String,
    functionTag: String,
    level: Int = 0,
    beforeShowCallbacks: ((Boolean) -> Unit)?,
    callbacks: ((Boolean) -> Unit),
    showCallback: (() -> Unit)? = null,
    rewardCallback: (() -> Unit)? = null,
    skipNewUserCheck: Boolean = false  // 🎯 答题模块专用：跳过新用户检查
) {
    // ⭐ 新用户首日不展示激励广告（避免浪费和展示率异常）
    // 🔧 答题模块例外：不受新老用户限制，直接展示广告
    val isNewUserFirstDay = UserInfoUtils.isNewUser() && AppConfig.isInstallFirstDay
    if (isNewUserFirstDay && !skipNewUserCheck) {
        android.util.Log.d("AdExtension", "🚫 New user first day - skipping reward ad")
        beforeShowCallbacks?.invoke(false)
        callbacks.invoke(false)
        return
    }
    
    var canShow = CloudManager.adShowPercent(level)
    //在原有百分比基础上额外添加控制逻辑，shenghe控制和organic控制
    if (canShow) {
        if (CloudManager.isSysViewTime()) {
            canShow = CloudManager.isShowAdBySysView()
        }
    }

    beforeShowCallbacks?.invoke(canShow)
    if (canShow) {
        if (this.isValid()) {
            var earned = false
            RewardedAdFlowCoordinator.request(
                this,
                adPosition,
                functionTag,
                getString(R.string.quran_reward),
                object : AdShowCallback {
                    override fun onAdImpression(p0: AdItem?) {
                        CloudManager.adLastShowTime = System.currentTimeMillis()
                    }

                    override fun onAdClicked(p0: AdItem?) = Unit

                    override fun onUserEarnedReward(p0: AdItem?, p1: RewardItem?) {
                        if (earned) return
                        earned = true
                        rewardCallback?.invoke()
                    }

                    override fun onAdClosed(p0: AdItem?) {
                        // Closing an ad without earning never grants or continues value.
                        callbacks.invoke(earned)
                    }

                    override fun onShow(p0: AdItem?) {
                        showCallback?.invoke()
                        CloudManager.adLastShowTime = System.currentTimeMillis()
                    }

                    override fun onShowFail() {
                        callbacks.invoke(false)
                    }
                }
            )
        } else {
            android.util.Log.w("AdExtension", "⚠️ Activity not valid, skipping reward ad")
            callbacks.invoke(false)
        }

    } else {
        callbacks.invoke(false)
    }
}

fun Activity.showGemAd(
    intersFunction: String,
    rewardFunction: String,
    rewardGems: Int,
    pageName: String,
    fromPageName: String,
    nextSetup: (() -> Unit)? = null,
): Boolean {
    // ✅ AdMob 合规：发放奖励(宝石)必须来自激励(rewarded)广告，不得奖励普通插屏。
    // 插屏仅用于无奖励的「继续下一关」过渡(见 nextLevelTv → AD_INTERS)。
    showRewardAd(
            adPosition = ExternalAdConfig.AD_QUIZ_REWARD,
            functionTag = rewardFunction,
            level = 0,
            beforeShowCallbacks = {
                reportEnterFunShowEvent(pageName, fromPageName, it, rewardFunction, ExternalAdConfig.AD_QUIZ_REWARD)
            },
            callbacks = {
                if (it) {
                    nextSetup?.invoke()
                }
            },
            showCallback = {
            },
            rewardCallback = {
                reportEvent(rewardFunction, "quiz_reward_gem", "$rewardGems")
                if (rewardGems > 0) QuizGemManager.addCount(rewardGems)
            },
            skipNewUserCheck = true  // 🎯 答题模块：不受新用户限制
        )
    return true
}

fun Activity.reloadQuizRewardAd(){
    RewardedAdFlowCoordinator.preload(this, ExternalAdConfig.AD_QUIZ_REWARD)
}

fun Activity.reloadQuizInterstitial(){
    AdFactory.loadInterstitialAd(this, ExternalAdConfig.AD_QUIZ_INTERS, null)
}
