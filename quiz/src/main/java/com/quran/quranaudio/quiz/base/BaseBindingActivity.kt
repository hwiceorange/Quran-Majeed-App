package com.quran.quranaudio.quiz.base

import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import com.quran.quranaudio.quiz.extension.reportEnterFunEvent

abstract class BaseBindingActivity<VB : ViewBinding>(val block: (LayoutInflater) -> VB) :
    AppCompatActivity() {

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