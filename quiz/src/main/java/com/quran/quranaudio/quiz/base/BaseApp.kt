package com.quran.quranaudio.quiz.base

import android.app.Application
import android.content.Context
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

        SPTools.init(this)
        FireBaseConfigManager.initCloud(this)
        AppConfig.setLanguage()
        initPlanAndQuiz()
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