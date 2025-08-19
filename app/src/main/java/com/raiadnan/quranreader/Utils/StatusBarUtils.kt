package com.raiadnan.quranreader.Utils

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.annotation.ColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * 状态栏工具类 - 适配Android 35及以上版本
 * 解决状态栏与应用界面重叠问题
 */
object StatusBarUtils {

    /**
     * 获取状态栏高度
     */
    fun getStatusBarHeight(context: Context): Int {
        var result = 0
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = context.resources.getDimensionPixelSize(resourceId)
        }
        return result
    }

    /**
     * 获取导航栏高度
     */
    fun getNavigationBarHeight(context: Context): Int {
        var result = 0
        val resourceId = context.resources.getIdentifier("navigation_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = context.resources.getDimensionPixelSize(resourceId)
        }
        return result
    }

    /**
     * 设置Activity为Edge-to-Edge模式并适配系统栏
     */
    fun setupEdgeToEdge(activity: Activity, isLightStatusBar: Boolean = true) {
        val window = activity.window
        
        // 启用Edge-to-Edge
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // 设置系统栏样式
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+
            window.setDecorFitsSystemWindows(false)
            val controller = window.insetsController
            if (controller != null) {
                if (isLightStatusBar) {
                    controller.setSystemBarsAppearance(
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    )
                } else {
                    controller.setSystemBarsAppearance(
                        0,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    )
                }
            }
        } else {
            // Android 6-10
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            controller.isAppearanceLightStatusBars = isLightStatusBar
        }
        
        // 设置状态栏和导航栏透明
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        
        // 确保窗口标志正确设置
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
    }

    /**
     * 为View设置系统栏内边距
     */
    fun applySystemBarInsets(view: View, 
                           applyTop: Boolean = true, 
                           applyBottom: Boolean = true,
                           applyLeft: Boolean = false,
                           applyRight: Boolean = false) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            val paddingLeft = if (applyLeft) systemBars.left else v.paddingLeft
            val paddingTop = if (applyTop) systemBars.top else v.paddingTop
            val paddingRight = if (applyRight) systemBars.right else v.paddingRight
            val paddingBottom = if (applyBottom) systemBars.bottom else v.paddingBottom
            
            v.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom)
            insets
        }
    }

    /**
     * 为View设置系统栏margin（用于需要margin而非padding的情况）
     */
    fun applySystemBarMargins(view: View,
                            applyTop: Boolean = true,
                            applyBottom: Boolean = true,
                            applyLeft: Boolean = false,
                            applyRight: Boolean = false) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            val layoutParams = v.layoutParams
            if (layoutParams is android.view.ViewGroup.MarginLayoutParams) {
                if (applyLeft) layoutParams.leftMargin = systemBars.left
                if (applyTop) layoutParams.topMargin = systemBars.top
                if (applyRight) layoutParams.rightMargin = systemBars.right
                if (applyBottom) layoutParams.bottomMargin = systemBars.bottom
                v.layoutParams = layoutParams
            }
            insets
        }
    }

    /**
     * 设置状态栏颜色和亮度
     */
    fun setStatusBarColor(activity: Activity, @ColorInt color: Int, isLightStatusBar: Boolean = true) {
        val window = activity.window
        window.statusBarColor = color
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val controller = window.insetsController
            if (controller != null) {
                if (isLightStatusBar) {
                    controller.setSystemBarsAppearance(
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    )
                } else {
                    controller.setSystemBarsAppearance(
                        0,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    )
                }
            }
        } else {
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            controller.isAppearanceLightStatusBars = isLightStatusBar
        }
    }

    /**
     * 隐藏系统栏（全屏模式）
     */
    fun hideSystemBars(activity: Activity) {
        val window = activity.window
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        
        // 隐藏状态栏和导航栏
        controller.hide(WindowInsetsCompat.Type.systemBars())
        
        // 设置沉浸式模式
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    /**
     * 显示系统栏
     */
    fun showSystemBars(activity: Activity) {
        val window = activity.window
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        
        // 显示状态栏和导航栏
        controller.show(WindowInsetsCompat.Type.systemBars())
    }

    /**
     * 检查是否为刘海屏或打孔屏
     */
    fun hasDisplayCutout(activity: Activity): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val decorView = activity.window.decorView
            val rootWindowInsets = decorView.rootWindowInsets
            rootWindowInsets?.displayCutout != null
        } else {
            false
        }
    }
}
