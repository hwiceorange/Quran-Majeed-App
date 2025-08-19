package com.raiadnan.quranreader

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.raiadnan.quranreader.Utils.StatusBarUtils

/**
 * 基础Activity类 - 处理状态栏适配
 * 适配Android 35及以上版本的Edge-to-Edge要求
 */
abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 设置Edge-to-Edge和状态栏适配
        setupStatusBar()
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
     * 子类可以重写此方法来自定义状态栏行为
     */
    protected open fun setupStatusBar() {
        // 默认设置为浅色状态栏
        StatusBarUtils.setupEdgeToEdge(this, isLightStatusBar())
    }

    /**
     * 应用系统栏内边距
     * 子类可以重写此方法来自定义内边距行为
     */
    protected open fun applySystemBarInsets() {
        val rootView = findViewById<View>(android.R.id.content)
        
        // 如果是特殊页面（如全屏页面），则跳过内边距处理
        if (isExcludedFromSystemBarInsets()) {
            return
        }
        
        // 为根视图应用系统栏内边距
        StatusBarUtils.applySystemBarInsets(
            rootView,
            applyTop = shouldApplyTopInset(),
            applyBottom = shouldApplyBottomInset(),
            applyLeft = shouldApplyLeftInset(),
            applyRight = shouldApplyRightInset()
        )
    }

    /**
     * 是否使用浅色状态栏
     * 子类可以重写此方法来自定义状态栏颜色
     */
    protected open fun isLightStatusBar(): Boolean {
        return true
    }

    /**
     * 是否排除在系统栏内边距处理之外
     * 子类可以重写此方法来排除特殊页面（如全屏页面）
     */
    protected open fun isExcludedFromSystemBarInsets(): Boolean {
        return false
    }

    /**
     * 是否应用顶部内边距（状态栏）
     */
    protected open fun shouldApplyTopInset(): Boolean {
        return true
    }

    /**
     * 是否应用底部内边距（导航栏）
     */
    protected open fun shouldApplyBottomInset(): Boolean {
        return true
    }

    /**
     * 是否应用左侧内边距
     */
    protected open fun shouldApplyLeftInset(): Boolean {
        return false
    }

    /**
     * 是否应用右侧内边距
     */
    protected open fun shouldApplyRightInset(): Boolean {
        return false
    }

    /**
     * 手动为指定View应用系统栏内边距
     * 用于需要特殊处理的View
     */
    protected fun applySystemBarInsetsToView(
        view: View,
        applyTop: Boolean = true,
        applyBottom: Boolean = true,
        applyLeft: Boolean = false,
        applyRight: Boolean = false
    ) {
        StatusBarUtils.applySystemBarInsets(view, applyTop, applyBottom, applyLeft, applyRight)
    }

    /**
     * 手动为指定View应用系统栏margin
     * 用于需要使用margin而非padding的View
     */
    protected fun applySystemBarMarginsToView(
        view: View,
        applyTop: Boolean = true,
        applyBottom: Boolean = true,
        applyLeft: Boolean = false,
        applyRight: Boolean = false
    ) {
        StatusBarUtils.applySystemBarMargins(view, applyTop, applyBottom, applyLeft, applyRight)
    }
}
