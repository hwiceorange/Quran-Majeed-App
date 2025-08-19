package com.quran.quranaudio.online.features.base

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.raiadnan.quranreader.Utils.StatusBarUtils

abstract class BaseActivity : AppCompatActivity() {

    abstract fun observeViewModel()
    protected abstract fun initViewBinding()
    protected abstract fun setupViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 设置Edge-to-Edge和状态栏适配
        setupStatusBar()
        setupViewModel()
        initViewBinding()
        observeViewModel()
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        // 在setContentView后应用系统栏内边距
        applySystemBarInsets()
    }

    override fun setContentView(view: View?) {
        super.setContentView(view)
        // 在setContentView后应用系统栏内边距
        applySystemBarInsets()
    }

    /**
     * 设置状态栏样式
     */
    protected open fun setupStatusBar() {
        StatusBarUtils.setupEdgeToEdge(this, isLightStatusBar())
    }

    /**
     * 应用系统栏内边距
     */
    protected open fun applySystemBarInsets() {
        val rootView = findViewById<View>(android.R.id.content)
        
        if (isExcludedFromSystemBarInsets()) {
            return
        }
        
        StatusBarUtils.applySystemBarInsets(
            rootView,
            applyTop = shouldApplyTopInset(),
            applyBottom = shouldApplyBottomInset(),
            applyLeft = shouldApplyLeftInset(),
            applyRight = shouldApplyRightInset()
        )
    }

    protected open fun isLightStatusBar(): Boolean = true
    protected open fun isExcludedFromSystemBarInsets(): Boolean = false
    protected open fun shouldApplyTopInset(): Boolean = true
    protected open fun shouldApplyBottomInset(): Boolean = true
    protected open fun shouldApplyLeftInset(): Boolean = false
    protected open fun shouldApplyRightInset(): Boolean = false
}