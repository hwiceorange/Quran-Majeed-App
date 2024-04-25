package com.quran.quranaudio.quiz.utils

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.XmlResourceParser
import android.util.Log
import com.quran.quranaudio.quiz.base.BaseApp
import com.quran.quranaudio.quiz.extension.getTodayDate
import com.quran.quranaudio.quiz.extension.loge
import com.blankj.utilcode.util.TimeUtils
import com.quran.quranaudio.quiz.R
import org.xmlpull.v1.XmlPullParser
import java.util.Locale

data class AppConfigInfo(
    var internalPrefix: String = "",
    var internalLocale: String = "",
    var internalShortName: String = "",
    var internalLongName: String = "",
    var internalPresetName: String = "",
)

object AppConfig {
    val TAG = AppConfig::class.java.simpleName
    private lateinit var lastAppConfig: AppConfigInfo
    private var lan: String = "en"
    fun isIDLan() = "id" == lan
    fun isEsLan() = "es" == lan
    fun setLanguage() {
        var locale = BaseApp.instance!!.resources.configuration.locale
        if (locale == null) {
            locale = Locale.getDefault()
        }
        lan = locale.language
        when(lan) {
            "in" -> lan = "id"
            "ji"-> lan="yi"
            "he"-> lan="iw"
        }
        loge("当前语言: " + lan, "lan_config")
        initAppConfigInfo()
    }

    @JvmStatic
    fun get(): AppConfigInfo {
        if (AppConfig::lastAppConfig.isInitialized) {
            return lastAppConfig
        }
        initAppConfigInfo()
        return lastAppConfig
    }

    private fun initAppConfigInfo(){

    }

    @Throws(Exception::class)
    private fun loadConfig(parser: XmlResourceParser): AppConfigInfo {
        val res = AppConfigInfo()

        return res
    }

    @JvmStatic
    val dirName: String
        get() {
            Log.e("lan_config", "lan: " + lan)
            val dirName: String = when (lan) {
                "pt" -> {
                    "bible_pt/"
                }

                "es" -> {
                    "bible_es/"
                }

                else -> {
                    "bible_en/"
                }
            }
            return dirName
        }

//    val zipName: String
//        get() {
//            val dirName: String = when (lan) {
//                "pt" -> {
//                    "bible_pt.zip"
//                }
//
//                "es" -> {
//                    "bible_es.zip"
//                }
//
//                else -> {
//                    "bible_en.zip"
//                }
//            }
//            return dirName
//        }
    private var firstInstallTime: Long = -1

    /**
     * 获取安装天数
     */
    val installDay: Int
        get() {
            if (firstInstallTime < 0) {
                var packageInfo: PackageInfo? = null
                try {
                    packageInfo = BaseApp.instance!!.packageManager.getPackageInfo(
                        BaseApp.instance!!.packageName,
                        0
                    )
                    firstInstallTime = packageInfo.firstInstallTime
                } catch (e: PackageManager.NameNotFoundException) {
                    e.printStackTrace()
                    return 0
                }
            }
            val currentTimeMillis = System.currentTimeMillis()
            return ((currentTimeMillis - firstInstallTime) / (24 * 60 * 60 * 1000)).toInt()
        }


    val isInstallFirstDay:Boolean
        get() {
            if (firstInstallTime < 0) {
                var packageInfo: PackageInfo? = null
                try {
                    packageInfo = BaseApp.instance!!.packageManager.getPackageInfo(
                        BaseApp.instance!!.packageName,
                        0
                    )
                    firstInstallTime = packageInfo.firstInstallTime
                } catch (e: PackageManager.NameNotFoundException) {
                    e.printStackTrace()
                    return false
                }
            }
            val installDate = TimeUtils.millis2String(firstInstallTime, "yyyyMMdd")
            return installDate == getTodayDate()
        }
}