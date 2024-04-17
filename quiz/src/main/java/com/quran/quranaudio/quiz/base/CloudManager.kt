package com.quran.quranaudio.quiz.base

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.quran.quranaudio.quiz.extension.SPTools
import com.quran.quranaudio.quiz.extension.logi
import com.quran.quranaudio.quiz.utils.AbTestUtil
import java.util.Random

object CloudManager {

    var adLastShowTime = 0L
    fun isSysViewTime(): Boolean {

//        val version = FireBaseConfigManager.getLong("new_sys_view_version_name")
//        if (version <= 0) {
//            return false
//        }
//        return AppUtils.getAppVersionCode().toLong() > version
        return false
    }

    //shengcha期间，广告有冷却期3分钟，并且只有10%概率
    fun isShowAdBySysView(): Boolean {
        var time = FireBaseConfigManager.getLong("version_ad_cool_time")
        time = if (time <= 0L) 180 else time
        return if (System.currentTimeMillis() - adLastShowTime < time * 1000) {
            "isShowAdBySysView::  time=$time,lastShowTime=$adLastShowTime,System.currentTimeMillis()=${System.currentTimeMillis()}".logi()
            false
        } else {
            val percent = 10
            val randomNum = Random().nextInt(100) //按百分比开
            "isShowAdBySysView::  randomNum=$randomNum,percent=$percent".logi()
            randomNum <= percent
        }
    }

    //Organic用户，广告有冷却期1分钟，时间可配
    fun isShowAdByOrganic(): Boolean {
        var time = FireBaseConfigManager.getLong("organic_ad_cool_time")
        time = if (time <= 0L) 60 else time
        "isShowAdByOrganic::  time=$time,lastShowTime=$adLastShowTime,System.currentTimeMillis()=${System.currentTimeMillis()}".logi()
        return System.currentTimeMillis() - adLastShowTime > time * 1000
    }

    /**
     * 广告展示概率，level_0：100%，level_1：10%，默认100, level_2：默认0
     */
    fun adShowPercent(level: Int = 0): Boolean {
       return true
    }

    fun isShowDailyMood(): Boolean {
        if (!FireBaseConfigManager.isInitSuccess()) {
            return true
        }
        return FireBaseConfigManager.getBoolean("isShowDailyMood")
    }


    fun isShowPopupPermissionDialog(): Boolean {

        if (!FireBaseConfigManager.isInitSuccess()) {
            return true
        }
        return FireBaseConfigManager.getBoolean(FireBaseConfigManager.SYSTEM_PERMISSION_KEY)
    }

    fun mainNotifyShow(): Boolean {
        if (!FireBaseConfigManager.isInitSuccess()) {
            return false
        }
        return FireBaseConfigManager.getBoolean("new_main_notify_show")
    }

    fun getSplashTime(): Long {
        if (!FireBaseConfigManager.isInitSuccess()) {
            return 8 * 1000L
        }
        var time = FireBaseConfigManager.getLong("splash_loading_time")
        if (time <= 0L) {
            time = 10 * 1000L
        }
        return time
    }

    fun getPrayerAnimatorDuration(): Long {
        if (!FireBaseConfigManager.isInitSuccess()) {
            return 20000L
        }
        return FireBaseConfigManager.getLong("prayer_animator_duration")
    }

    fun isDebug():Boolean{
        return false
    }
    /**
     * plan计划通知是否在冷却期
     */
    fun isPlanCoolTime(): Boolean {
        val coolTime = if (isDebug()) {
            600 * 1000L
        } else if (!FireBaseConfigManager.isInitSuccess()) {
            2 * 60 * 60 * 1000L
        } else {
            FireBaseConfigManager.getLong("plan_notification_cool_time")
        }
        val lastShowTime = SPTools.getLong(Constants.KEY_PLAN_NOTIFICATION_TIME, 0)
        return System.currentTimeMillis() - lastShowTime < coolTime
    }

    /**
     * quiz答题通知是否在冷却期
     */
    fun isQuizCoolTime(): Boolean {
        val coolTime = if (isDebug()) {
            1200 * 1000L
        } else if (!FireBaseConfigManager.isInitSuccess()) {
            1 * 60 * 60 * 1000L
        } else {
            FireBaseConfigManager.getLong("quiz_notification_cool_time")
        }
        val lastShowTime = SPTools.getLong(Constants.KEY_QUIZ_NOTIFICATION_TIME, 0)
        return System.currentTimeMillis() - lastShowTime < coolTime
    }

    /**
     * 是否在通知权限弹窗冷却期，默认24小时
     */
    fun isNotifyPermissionCoolTime(): Boolean {
        val coolTime = if (isDebug()) {
            3 * 60 * 1000L
        } else if (!FireBaseConfigManager.isInitSuccess()) {
            24 * 60 * 60 * 1000L
        } else {
            FireBaseConfigManager.getLong("notify_permission_cool_time")
        }
        val lastShowTime = SPTools.getLong(Constants.KEY_NOTIFY_PERMISSION_TIME, 0)
        return System.currentTimeMillis() - lastShowTime < coolTime
    }

    /**
     * 是否在悬浮窗权限弹窗冷却期，默认24小时
     */
    fun isSystemPermissionCoolTime(): Boolean {
        val coolTime = if (isDebug()) {
            3 * 60 * 1000L
        } else if (!FireBaseConfigManager.isInitSuccess()) {
            10 * 60 * 1000L
        } else {
            FireBaseConfigManager.getLong("system_permission_cool_time")
        }
        val lastShowTime = SPTools.getLong(Constants.KEY_SYSTEM_PERMISSION_TIME, 0)
        return System.currentTimeMillis() - lastShowTime < coolTime
    }

    /**
     * 是否有悬浮窗权限
     */
    fun hasSystemPermission(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)
    }

    fun getUniversalCoolTime(): Long{
        val coolTime = if (isDebug()) {
            5 * 1000L
        } else if (!FireBaseConfigManager.isInitSuccess()) {
            2 * 60 * 60 * 1000L
        } else {
            FireBaseConfigManager.getLong("notify_universal_cool_time")
        }
        return coolTime
    }

    fun getFloatWindowTestGroupName(): String{
        if (!FireBaseConfigManager.isInitSuccess()) {
            return AbTestUtil.FloatWindowOriginName
        }
        return FireBaseConfigManager.getString(
            "float_window_guide_page_test",
            AbTestUtil.FloatWindowOriginName
        )
    }

    fun getNativeIntervalTime(): Long{
        if (!FireBaseConfigManager.isInitSuccess()) {
            return 60 * 1000
        }
        return FireBaseConfigManager.getLong("native_interval_time")
    }

    fun isShowQuizUseInterAd(): Boolean{
        if (!FireBaseConfigManager.isInitSuccess()) {
            return false
        }
        return true// FireBaseConfigManager.getBoolean("quiz_use_inter_ad")
    }
}