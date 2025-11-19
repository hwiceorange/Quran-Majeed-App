package com.quran.quranaudio.quiz.base

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import com.quran.quranaudio.quiz.extension.reportEnterFunEvent
import java.util.Locale

abstract class BaseBindingActivity<VB : ViewBinding>(val block: (LayoutInflater) -> VB) :
    AppCompatActivity() {

    /**
     * 🔧 统一语言设置：所有答题相关 Activity 都会应用用户选择的语言
     * 确保 Android 资源系统使用正确的语言文件夹（values-ar, values-in 等）
     */
    override fun attachBaseContext(newBase: Context) {
        // 获取用户设置的语言
        com.quran.quranaudio.quiz.utils.AppConfig.setLanguage()
        val language = com.quran.quranaudio.quiz.utils.AppConfig.lan
        
        android.util.Log.d("BaseBindingActivity", "🌐 Setting language to: $language")
        
        // 创建带有正确 Locale 的 Context
        val locale = Locale(language)
        Locale.setDefault(locale)
        
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        
        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }

    protected val binding by lazy {
        block(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initIntent(savedInstanceState)
        initView()
        initData()
    }

    protected open fun initIntent(savedInstanceState: Bundle?){}

    protected open fun initView() {}

    protected open fun initData() {}
    protected abstract fun getPageName(): String
    protected abstract fun getFormPageName(): String

    override fun onResume() {
        super.onResume()
        reportEnterFunEvent(getPageName(), getFormPageName())
    }
}