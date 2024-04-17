package com.quran.quranaudio.quiz.base

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.firebase.remoteconfig.FirebaseRemoteConfigValue
import com.quranaudio.common.ad.AdConfig
import com.quran.quranaudio.quiz.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Date


object FireBaseConfigManager {

    val KEY_WHITELIST_DEVICE:String="ad_test_device_gaids";
    //打开悬浮窗权限弹窗
    const val SYSTEM_PERMISSION_KEY = "open_system_permission"

    private var isInited = false
    private var localInitTask: Task<Void>? = null
    fun initCloud(aContext: Context) {
        if (isInited) {
            return
        }
        isInited = true
        val mContext=aContext
        initRemoteConfig()
        localInitTask =
            FirebaseRemoteConfig.getInstance().setDefaultsAsync(R.xml.remote_rconfig_defaults)
        //autoRefreshConfig()
        FirebaseRemoteConfig.getInstance().fetchAndActivate().addOnCompleteListener {
            val entries: Set<Map.Entry<String, FirebaseRemoteConfigValue>> =
                FirebaseRemoteConfig.getInstance().all.entries
            for ((key, value) in entries) {
                Log.d("firebase_remote","initRemoteConfig: key : " + key + " ; value : " + value.asString())
            }
            logCloudControlComplete(it.isSuccessful, it.exception)
            //获取到配置及时更新白名单设备

            Log.d("whitelist",String.format("isWhiteListDevice fetch %b " , AdConfig.isTest))
        }
        FirebaseInstallations.getInstance().getToken(false)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("firebase_remote", "Installation auth token: " + task.result?.token)
                } else {
                    Log.e("firebase_remote", "Unable to get Installation auth token")
                }
            }
        FirebaseRemoteConfig.getInstance().addOnConfigUpdateListener(
            object : ConfigUpdateListener{
                override fun onUpdate(configUpdate: ConfigUpdate) {
                    //获取到配置及时更新白名单设备
                    if(configUpdate.updatedKeys.contains(KEY_WHITELIST_DEVICE)){

                        Log.d("whitelist",String.format("isWhiteListDevice update %b " , AdConfig.isTest))

                    }

                }

                override fun onError(error: FirebaseRemoteConfigException) {
                   Log.e("whitelist","onError")
                }
            }
        )

    }

    /**
     * firebase默认配置
     */
    private fun initRemoteConfig() {
        val firebaseRemoteConfig = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(10*60)//最小请求间隔 10分钟
            .build()
        FirebaseRemoteConfig.getInstance().setConfigSettingsAsync(firebaseRemoteConfig)
        FirebaseRemoteConfig.getInstance().ensureInitialized()
            .continueWith { this }


    }

    fun isInitSuccess(): Boolean {
        return localInitTask?.isSuccessful == true
    }


    private fun getDifferenceNaturalDay(startTime: Long, endTime: Long): Int {
        if(endTime <= startTime) return 0
        val startCalendar = Calendar.getInstance()
        startCalendar.time = Date(startTime)
        val endCalendar = Calendar.getInstance()
        endCalendar.time = Date(endTime)
        val betweenDays =
            ((endCalendar.time.time - startCalendar.time.time) / (1000 * 60 * 60 * 24)).toInt()
        endCalendar.add(Calendar.DAY_OF_MONTH, -betweenDays)
        endCalendar.add(Calendar.DAY_OF_MONTH, -1)
        return if (startCalendar.get(Calendar.DAY_OF_MONTH) == endCalendar.get(Calendar.DAY_OF_MONTH)) {
            betweenDays + 1
        } else {
            betweenDays
        }
    }


    /**
     * 云控获取boolean
     */
    fun getBoolean(key: String): Boolean {
        return FirebaseRemoteConfig.getInstance().getBoolean(key)
    }

    /**
     * 云控获取Double
     */
    fun getDouble(key: String): Double {
        return FirebaseRemoteConfig.getInstance().getDouble(key)
    }

    /**
     * 云控获取Long
     */
    fun getLong(key: String): Long {
        return FirebaseRemoteConfig.getInstance().getLong(key)
    }


    /**
     * 云控获取string
     */
    @JvmStatic
    fun getString(key: String, defaultValue: String? = null): String {
        val cloudValue = FirebaseRemoteConfig.getInstance().getString(key)
        if (FirebaseRemoteConfig.DEFAULT_VALUE_FOR_STRING == cloudValue && defaultValue != null) {
            return defaultValue
        }
        return cloudValue
    }

    /**
     * 循环拉取firebase配置
     */
    private fun autoRefreshConfig() { //每30分钟刷新一次。
        MainScope().launch {
            while (true) {
                withContext(Dispatchers.IO) {
                    FirebaseRemoteConfig.getInstance().fetchAndActivate().addOnCompleteListener {
                        logCloudControlComplete(it.isSuccessful, it.exception)
                    }
                }
                delay(30L * 1000 * 60)
            }
        }
    }

    private fun logCloudControlComplete(success: Boolean, exception: Exception?) {
        Log.e("xxx", "logCloudControlComplete success: $success   exception:$exception")
    }

}