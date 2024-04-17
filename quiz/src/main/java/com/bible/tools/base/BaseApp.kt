package com.bible.tools.base

import android.app.Application
import android.content.Context
import androidx.multidex.MultiDexApplication
import com.bible.tools.extension.SPTools
import com.bible.tools.quiz.QuestionTools
import com.blankj.utilcode.util.ThreadUtils

/**
create by microspark 4/14/24
 **/
open class BaseApp: MultiDexApplication() {
    companion object{
        var instance:Application?=null
    }
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        instance=this
    }

    override fun onCreate() {
        super.onCreate()

        SPTools.init(this)
        FireBaseConfigManager.initCloud(this)
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