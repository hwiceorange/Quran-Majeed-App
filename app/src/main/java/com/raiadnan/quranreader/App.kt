package com.raiadnan.quranreader

import android.app.Application
import android.content.Context
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.FormatStrategy
import com.orhanobut.logger.Logger
import com.orhanobut.logger.PrettyFormatStrategy

import com.raiadnan.quranreader.ConnectivityStateHolder.registerConnectivityBroadcaster
import timber.log.Timber


class App : Application() {

    companion object {
        var context: Context? = null
        lateinit var application: App
    }

    override fun onCreate() {
        super.onCreate()

        context = this;
        application = this
        registerConnectivityBroadcaster()
        val formatStrategy:FormatStrategy = PrettyFormatStrategy.newBuilder()
            .showThreadInfo(true)
            .methodCount(1)
            .methodOffset(5)
            .tag("")
            .build()

        Logger.addLogAdapter(AndroidLogAdapter(formatStrategy))
        Timber.plant(object : Timber.DebugTree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                Logger.log(priority,"-$tag",message,t)
            }
        })

    }

}