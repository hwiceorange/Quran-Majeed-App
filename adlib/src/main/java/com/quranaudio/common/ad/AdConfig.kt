package com.quranaudio.common.ad

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.quran.quranaudio.common.ad.BuildConfig

object AdConfig {
    var isTest = false
    const val AD_CACHE_MAX_TIME = 55 * 60 * 1000L //广告缓存有效时间
    const val AD_LOADING_TIME_OUT = 60 * 1000L  //一分钟加载超时
    const val AD_APP_OPEN_CACHE_MAX_TIME = 3 * 60 * 60 * 1000 + 55 * 60 * 1000L //开屏广告缓存有效时间

    const val AD_APPOPEN = "appopen"
    const val AD_INTERS = "inter_ad"
    const val AD_NEW_USER_INTERS = "inter_new_first"
    const val AD_NATIVE = "native_ad"
    const val AD_BANNER = "banner_ad"
    const val AD_QUIZ_INTERS = "inters_ad_quiz" //quiz插屏id
    const val AD_QUIZ_REWARD = "reward_ad_quiz"  //quiz激励id

    private const val AD_TEST_APPOPEN_ID = "ca-app-pub-3940256099942544/9257395921"
    private const val AD_TEST_INTERS_ID = "ca-app-pub-3940256099942544/1033173712"
    private const val AD_TEST_NATIVE_ID = "ca-app-pub-3940256099942544/2247696110"
    private const val AD_TEST_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
    private const val AD_TEST_REWARD_ID = "ca-app-pub-3940256099942544/5224354917"

    private const val AD_QUIZ_INTERS_ID = "ca-app-pub-3966802724737141/2182661506"
    private const val AD_QUIZ_REWARD_ID = "ca-app-pub-3966802724737141/2186558832"
    private const val AD_APPOPEN_ID = "ca-app-pub-3966802724737141/3298687654"
    private const val AD_NEW_USER_INTERS_ID = "ca-app-pub-3966802724737141/7804176008"
    private const val AD_INTERS_ID = "ca-app-pub-3966802724737141/2182661506"
    private const val AD_NATIVE_ID = "ca-app-pub-3966802724737141/1300824672"
    private const val AD_BANNER_ID = "ca-app-pub-3966802724737141/1386840185"


    fun getAdIdByPosition(position: String): String {
        if (!isTest) {
            val adId = FirebaseRemoteConfig.getInstance().getString("${position}_admob")
            if (adId.isNotBlank()) return adId
        }
        return when (position) {
            AD_APPOPEN -> if (useTestAD()) AD_TEST_APPOPEN_ID else AD_APPOPEN_ID
            AD_NEW_USER_INTERS -> if (useTestAD()) AD_TEST_INTERS_ID else AD_NEW_USER_INTERS_ID
            AD_INTERS -> if (useTestAD()) AD_TEST_INTERS_ID else AD_INTERS_ID
            AD_NATIVE -> if (useTestAD()) AD_TEST_NATIVE_ID else AD_NATIVE_ID
            AD_BANNER -> if (useTestAD()) AD_TEST_BANNER_ID else AD_BANNER_ID
            AD_QUIZ_INTERS -> if (useTestAD()) AD_TEST_INTERS_ID else AD_QUIZ_INTERS_ID
            AD_QUIZ_REWARD -> if (useTestAD()) AD_TEST_REWARD_ID else AD_QUIZ_REWARD_ID
            else -> ""
        }
    }

    private fun useTestAD(): Boolean {
        return BuildConfig.DEBUG
    }
}