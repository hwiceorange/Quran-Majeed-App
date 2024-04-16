package com.bible.tools.extension

import com.bible.tools.ad.ExternalAdConfig
import com.hydra.common.ad.AdConfig
import com.hydra.common.ad.AdFactory

fun reportAnimStartAdEvent(name: String, fromResource: String, hasAd: Boolean) {
//    ReportHelper.uploadData(
//        Report.Builder()
//            .actionParam(name)
//            .action("animation_start")
//            .progress(
//                if (hasAd) {
//                    "true"
//                } else {
//                    "fasle"
//                }
//            )
//            .refer(fromResource)
//            .build()
//    )
}

fun reportPushClickEvent(itemId:String, isOngoing: Boolean = false) {
//    ReportHelper.uploadData(
//        Report.Builder()
//            .actionParam("localPush")
//            .refer(if (isOngoing) "referrer_push_ongoing" else "referrer_push")
//            .action("action_push_click")
//            .itemId(itemId)
//            .sid(sessionId.toString())
//            .build()
//    )
}

fun reportPopupPushClickEvent(itemId:String) {
//    ReportHelper.uploadData(
//        Report.Builder()
//            .actionParam("popup")
//            .refer("referrer_popup")
//            .action("action_popup_click")
//            .itemId(itemId)
//            .sid(sessionId.toString())
//            .build()
//    )
}

fun reportEnterFunEvent(name: String, formResource: String) {
    val hasInterAd = AdFactory.hasInterAd(AdConfig.AD_INTERS)
//    ReportHelper.uploadData(
//        Report.Builder()
//            .actionParam(name) //页面
//            .action("enter_fun_page")
//            .refer(formResource) //功能
//            .followTopics(if (hasInterAd) "true" else "false") //是否有缓存
//            .top(if(android.app.ActivityManager.isUserAMonkey())"true" else "false")
//            .build()
//    )
}

fun reportEnterFunShowEvent(name: String, formResource: String, needShowed: Boolean, adFunctionTag: String, from: String? = null)  {
    val hasCache = if (from.isNullOrEmpty()){
        AdFactory.hasInterAd(AdConfig.AD_INTERS)
    } else if (from == ExternalAdConfig.AD_QUIZ_REWARD) {
        AdFactory.hasRewardAd(from)
    } else {
        AdFactory.hasInterAd(from)
    }
//    ReportHelper.uploadData(
//        Report.Builder()
//            .actionParam(name) //页面添加 、历史
//            .action("enter_fun_page_show") //进入功能展示广告
//            .refer(formResource) //功能 血压、心率
//            .progress(needShowed.toString()) //是否需要展示
//            .sourceType(from)//来源 打开方式
//            .top(adFunctionTag)
//            .followTopics(if (hasCache) "true" else "false") //是否有缓存
//            .build()
//    )
}
fun reportClickEvent(name: String) {
//    ReportHelper.uploadData(
//        Report.Builder()
//            .actionParam(name)
//            .action("function_click")
//            .progress(if(AdFactory.hasInterAd(AdConfig.AD_INTERS)) "true" else "false")
//            .build()
//    )
}

fun reportClickEvent(name: String, refer: String = "") {
//    ReportHelper.uploadData(
//        Report.Builder()
//            .actionParam(name)
//            .action("function_click")
//            .refer(refer)
//            .progress(if(AdFactory.hasInterAd(AdConfig.AD_INTERS)) "true" else "false")
//            .build()
//    )
}

fun reportEvent(name:String, action:String) {
    reportEvent(name, action, "")
}

fun reportEvent(name:String, action:String, refer: String = "") {
//    ReportHelper.uploadData(
//        Report.Builder()
//            .actionParam(name) //页面
//            .action(action) //进入功能展示广告
//            .refer(refer)
//            .build()
//    )
}

fun reportFunCompletedEvent(name: String, fromResource: String, hasAd: Boolean, type: String? = null) {
    loge("action=function_completed, actionParam = $name, fromResource=$fromResource")
//    ReportHelper.uploadData(
//        Report.Builder()
//            .actionParam(name)
//            .action("function_completed")
//            .progress(if (hasAd) "true" else "false")
//            .sid(sessionId.toString())
//            .refer(fromResource)
//            .itemId(type)
//            .build()
//    )
}

fun reportPreShowFirstAd(name: String, fromResource: String, hasAd: Boolean, type: String? = null) {
    loge("action=function_completed, actionParam = $name, fromResource=$fromResource")
//    ReportHelper.uploadData(
//        Report.Builder()
//            .actionParam(name) //页面
//            .action("preshow_first_ad") //展示新用户首个广告
//            .progress(if (hasAd) "true" else "false")
//            .itemId(type)
//            .refer(fromResource)
//            .build()
//    )
}

fun reportSessionStartEvent(fromResource: String) {
//    ReportHelper.uploadData(
//        Report.Builder()
//            .actionParam(if (AdFactory.hasAppOpenAd(AdConfig.AD_APPOPEN)) "true" else "false")
//            .action("session_start")
//            .sid(sessionId.toString())
//            .refer(fromResource)
//            .progress(if (AdFactory.hasInterAd(AdConfig.AD_INTERS)) "true" else "false")
//            .build()
//    )
}

fun reportExitFunShowEvent(name: String, fromResource: String, needShowed: Boolean, adFunctionTag:String, from: String? = null) {
//    ReportHelper.uploadData(
//        Report.Builder()
//            .actionParam(name) //页面
//            .action("exit_fun_page_show")//退出功能展示广告
//            .refer(fromResource) //功能
//            .top(adFunctionTag)
//            .progress(needShowed.toString()) //是否需要展示
//            .followTopics(if (AdFactory.hasInterAd(AdConfig.AD_INTERS)) "true" else "false")
//            .sourceType(from)
//            .build()
//    )
}

fun reportQuizLevelUp(level: Int) {
//    ReportHelper.uploadData(
//        Report.Builder()
//            .actionParam(level.toString())
//            .action("level_up")
//            .build()
//    )
}

fun reportQuizTipsEvent(level: Int, refer: String) {
//    ReportHelper.uploadData(
//        Report.Builder()
//            .refer(refer)
//            .actionParam(level.toString())
//            .action("quiz_tips")
//            .build()
//    )
}

fun reportPlanEvent(action: String, planId: String, day: String = "") {
//    ReportHelper.uploadData(
//        Report.Builder()
//            .actionParam("PlanDetail") //页面
//            .action(action)
//            .refer(planId)
//            .top(day)
//            .followTopics(
//                if (AdFactory.hasInterAd(AdConfig.AD_INTERS)) "true" else "false"
//            )
//            .build()
//    )
}

fun reportSystemPermissionEvent(action: String, readTime: String, clickButton: String = "") {
//    ReportHelper.uploadData(
//        Report.Builder()
//            .action(action)
//            .refer(readTime)
//            .top(clickButton)
//            .build()
//    )
}

fun reportThreeEvent(action: String, actionParam: String, refer: String) {
//    ReportHelper.uploadData(
//        Report.Builder()
//            .action(action)
//            .actionParam(actionParam)
//            .refer(refer)
//            .build()
//    )
}