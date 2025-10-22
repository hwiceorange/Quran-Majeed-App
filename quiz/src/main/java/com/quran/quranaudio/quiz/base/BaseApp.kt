package com.quran.quranaudio.quiz.base

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Process
import android.util.Log
import androidx.multidex.MultiDexApplication
import com.quran.quranaudio.quiz.extension.SPTools
import com.quranaudio.quiz.quiz.QuestionTools
import com.blankj.utilcode.util.ThreadUtils
import com.quran.quranaudio.quiz.utils.AppConfig

/**
create by microspark 4/14/24
 **/
open class BaseApp: MultiDexApplication() {
    companion object{
        var instance:Application?=null
    }
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        instance =this
    }

    override fun onCreate() {
        super.onCreate()
        
        // 🔧 关键修复：检查是否在主进程中
        val isMainProcess = isInMainProcess()
        
        Log.d("BaseApp", "onCreate - isMainProcess: $isMainProcess")

        // 通用初始化（所有进程都需要）
        SPTools.init(this)
        AppConfig.setLanguage()
        
        // 🚨 Firebase 初始化仅在主进程执行
        // 这样可以避免在 :error_activity 等独立进程中崩溃
        if (isMainProcess) {
            Log.d("BaseApp", "✅ Running in MAIN process - initializing Firebase")
            FireBaseConfigManager.initCloud(this)
            initPlanAndQuiz()
        } else {
            Log.d("BaseApp", "⚠️ Running in CHILD process - skipping Firebase initialization")
        }
    }
    
    /**
     * 检查是否在主进程中运行
     * @return true 如果在主进程，false 如果在子进程（如 :error_activity）
     */
    private fun isInMainProcess(): Boolean {
        return try {
            val pid = Process.myPid()
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val currentProcessName = activityManager?.runningAppProcesses?.find { it.pid == pid }?.processName
            currentProcessName == null || currentProcessName == applicationContext.packageName
        } catch (e: Exception) {
            Log.e("BaseApp", "Failed to check process, assuming main process", e)
            true // 出错时默认为主进程
        }
    }

    private fun initPlanAndQuiz() {
        ThreadUtils.executeByIo(object : ThreadUtils.SimpleTask<Boolean>() {
            override fun doInBackground(): Boolean {
//                FileUtil.unZipBible(this@SplashActivity, AppConfig.zipName)
                QuestionTools.unZipBibleQuiz()
                return true
            }

            override fun onSuccess(result: Boolean?) {

            }

            override fun onDone() {
                super.onDone()

            }
        })
    }
}