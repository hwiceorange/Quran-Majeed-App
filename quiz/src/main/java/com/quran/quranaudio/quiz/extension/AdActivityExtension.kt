package com.quran.quranaudio.quiz.extension

import android.app.Activity
import com.quran.quranaudio.quiz.ad.ExternalAdConfig
import com.quran.quranaudio.quiz.base.CloudManager
import com.quranaudio.quiz.quiz.QuizGemManager
import com.quran.quranaudio.quiz.utils.LoadingDialog
import com.quran.quranaudio.quiz.utils.Tasks
import com.blankj.utilcode.util.ToastUtils
import com.quranaudio.common.ad.AdConfig
import com.quranaudio.common.ad.AdFactory
import com.quranaudio.common.ad.AdShowCallback
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
    showCallback: (() -> Unit)? = null
) {
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
        if (this.isValid()) {
            if (!hasInterAdByPool()) {
                wrapCallback.invoke(false)
                return
            }
            val loadingDialog = LoadingDialog(this, R.string.bible_loading_ad.getResString())
            loadingDialog.show()
            Tasks.postDelayedByUI({
                if (this.isValid()) {
                    loadingDialog.dismiss()
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
                }
            },1000L)
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
) {
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
            if (!hasRewardAdByPool(adPosition)) {
                callbacks.invoke(false)
                return
            }
            val loadingDialog = LoadingDialog(this, R.string.bible_loading_ad.getResString())
            loadingDialog.show()
            Tasks.postDelayedByUI({
                if (this.isValid()) {
                    loadingDialog.dismiss()
                    AdFactory
                        .showRewardAd(this, adPosition, functionTag, object : AdShowCallback {
                            override fun onAdImpression(p0: AdItem?) {
                                CloudManager.adLastShowTime = System.currentTimeMillis()
                            }

                            override fun onAdClicked(p0: AdItem?) {
                            }

                            override fun onUserEarnedReward(p0: AdItem?, p1: RewardItem?) {
                                rewardCallback?.invoke()
                            }

                            override fun onAdClosed(p0: AdItem?) {
                                callbacks.invoke(true)
                            }

                            override fun onShow(p0: AdItem?) {
                                showCallback?.invoke()
                                CloudManager.adLastShowTime = System.currentTimeMillis()
                            }

                            override fun onShowFail() {
                                callbacks.invoke(false)
                            }
                        })
                }
            },1000L)
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
    if (CloudManager.isShowQuizUseInterAd()){
        if (hasInterAdByPool(ExternalAdConfig.AD_QUIZ_INTERS)){
            showInterAdByPoolNew(ExternalAdConfig.AD_QUIZ_INTERS, intersFunction, 0, {
                reportEnterFunShowEvent(pageName, fromPageName, it, intersFunction, ExternalAdConfig.AD_QUIZ_INTERS)
            }, {
                if (it) {
                    nextSetup?.invoke()
                }else {
                    ToastUtils.showLong(R.string.bible_no_ad_tips)
                }
            }, {
                reportEvent(intersFunction,"quiz_reward_gem","$rewardGems")
                if (rewardGems > 0) QuizGemManager.addCount(rewardGems)
                reloadQuizInterstitial()
            })
        }else {
            ToastUtils.showLong(R.string.bible_no_ad_tips)
            reloadQuizInterstitial()
            return false
        }
    }else {
        if (hasRewardAdByPool(ExternalAdConfig.AD_QUIZ_REWARD)) {
            showRewardAd(ExternalAdConfig.AD_QUIZ_REWARD, rewardFunction, 0, {
                reportEnterFunShowEvent(pageName, fromPageName, it, rewardFunction, ExternalAdConfig.AD_QUIZ_REWARD)
            }, {
                if (it) {
                    nextSetup?.invoke()
                }else {
                    ToastUtils.showLong(R.string.bible_no_ad_tips)
                }
            }, {
            }, {
                reportEvent(rewardFunction,"quiz_reward_gem","$rewardGems")
                if (rewardGems > 0) QuizGemManager.addCount(rewardGems)
            })
        } else {
            ToastUtils.showLong(R.string.bible_no_ad_tips)
            reloadQuizRewardAd()
            return false
        }
    }
    return true
}

fun Activity.reloadQuizRewardAd(){
    AdFactory.loadRewardAd(this, ExternalAdConfig.AD_QUIZ_REWARD, null)
}

fun Activity.reloadQuizInterstitial(){
    AdFactory.loadInterstitialAd(this, ExternalAdConfig.AD_QUIZ_INTERS, null)
}