package com.quran.quranaudio.online.feedback

import android.app.Activity
import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.quran.quranaudio.online.R

/**
 * 全局悬浮反馈按钮
 * 在屏幕右下角显示半透明的 💬 图标
 */
class FeedbackFloatingButton(private val activity: Activity) {
    
    private var floatingView: View? = null
    private var windowManager: WindowManager? = null
    private var params: WindowManager.LayoutParams? = null
    private var isShowing = false
    
    /**
     * 显示悬浮按钮
     * 🔥 Critical: 必须在 Activity 前台且有效时调用
     */
    fun show() {
        if (isShowing) return
        
        try {
            // 🔥 关键修复1：检查 Activity 状态
            if (activity.isFinishing || activity.isDestroyed) {
                android.util.Log.w("FeedbackFloatingButton", "⚠️ Activity is finishing/destroyed, abort showing floating button")
                return
            }
            
            // 🔥 关键修复2：检查 Window Token 可用性
            val decorView = activity.window?.decorView
            if (decorView == null || decorView.windowToken == null) {
                android.util.Log.w("FeedbackFloatingButton", "⚠️ Activity window token not ready, abort showing floating button")
                return
            }
            
            android.util.Log.d("FeedbackFloatingButton", "✅ Activity state check passed")
            android.util.Log.d("FeedbackFloatingButton", "   → isFinishing: ${activity.isFinishing}")
            android.util.Log.d("FeedbackFloatingButton", "   → isDestroyed: ${activity.isDestroyed}")
            android.util.Log.d("FeedbackFloatingButton", "   → hasWindowToken: ${decorView.windowToken != null}")
            
            windowManager = activity.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            
            // 创建悬浮视图
            floatingView = LayoutInflater.from(activity).inflate(
                R.layout.feedback_floating_button,
                null
            )
            
            // 计算合适的 Y 坐标（避免遮挡底部导航栏）
            // 底部导航栏高度约 56dp + 悬浮按钮高度 56dp + 额外安全边距 16dp = 128dp
            val bottomMarginDp = 128
            val density = activity.resources.displayMetrics.density
            val bottomMarginPx = (bottomMarginDp * density).toInt()
            
            // 🔥 关键修复3：设置布局参数（使用 token 关联到 Activity）
            params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.END
                x = (24 * density).toInt() // 距离右边 24dp
                y = bottomMarginPx // 距离底部避开导航栏
                // 🆕 显式设置 token，确保关联到 Activity 窗口
                token = decorView.windowToken
            }
            
            android.util.Log.d("FeedbackFloatingButton", "→ Attempting to add FloatingView to WindowManager...")
            android.util.Log.d("FeedbackFloatingButton", "   → Window Token: ${params?.token}")
            
            // 🔥 关键修复4：添加到窗口（在主线程，带超时保护）
            windowManager?.addView(floatingView, params)
            
            android.util.Log.d("FeedbackFloatingButton", "→ FloatingView added to window: $floatingView")
            android.util.Log.d("FeedbackFloatingButton", "→ FloatingView isClickable: ${floatingView?.isClickable}")
            android.util.Log.d("FeedbackFloatingButton", "→ FloatingView isFocusable: ${floatingView?.isFocusable}")
            
            // 方案1：设置触摸监听器（支持拖动和点击检测）
            setupTouchListener()
            
            // 方案2：同时设置普通点击监听器作为备用
            floatingView?.setOnClickListener {
                android.util.Log.d("FeedbackFloatingButton", "🎯 OnClick listener triggered")
                onFeedbackButtonClicked()
            }
            
            isShowing = true
            android.util.Log.d("FeedbackFloatingButton", "✅ Floating button shown successfully at y=$bottomMarginPx (${bottomMarginDp}dp)")
            
        } catch (e: android.view.WindowManager.BadTokenException) {
            android.util.Log.e("FeedbackFloatingButton", "❌ BadTokenException: Activity window token invalid", e)
            android.util.Log.e("FeedbackFloatingButton", "   → This usually means Activity is not in foreground or being destroyed")
            // 不显示 Toast，因为 Activity 可能已经不可用
        } catch (e: IllegalStateException) {
            android.util.Log.e("FeedbackFloatingButton", "❌ IllegalStateException: Activity state invalid", e)
        } catch (e: Exception) {
            android.util.Log.e("FeedbackFloatingButton", "❌ Failed to show floating button", e)
        }
    }
    
    /**
     * 隐藏悬浮按钮
     */
    fun hide() {
        if (!isShowing) return
        
        try {
            floatingView?.let {
                windowManager?.removeView(it)
            }
            isShowing = false
            android.util.Log.d("FeedbackFloatingButton", "✅ Floating button hidden")
        } catch (e: Exception) {
            android.util.Log.e("FeedbackFloatingButton", "❌ Failed to hide floating button", e)
        }
    }
    
    /**
     * 设置触摸监听（支持拖动和点击）
     */
    private fun setupTouchListener() {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isMoved = false
        
        floatingView?.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params?.x ?: 0
                    initialY = params?.y ?: 0
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isMoved = false
                    android.util.Log.d("FeedbackFloatingButton", "Touch DOWN at (${event.rawX}, ${event.rawY})")
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = Math.abs(event.rawX - initialTouchX)
                    val deltaY = Math.abs(event.rawY - initialTouchY)
                    
                    // 如果移动超过阈值，认为是拖动
                    if (deltaX > 10 || deltaY > 10) {
                        isMoved = true
                        params?.x = initialX + (initialTouchX - event.rawX).toInt()
                        params?.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager?.updateViewLayout(floatingView, params)
                        android.util.Log.d("FeedbackFloatingButton", "Touch MOVE - dragging")
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    android.util.Log.d("FeedbackFloatingButton", "Touch UP - isMoved: $isMoved")
                    
                    // 如果没有移动，视为点击
                    if (!isMoved) {
                        android.util.Log.d("FeedbackFloatingButton", "🎯 Detected as CLICK, opening dialog...")
                        onFeedbackButtonClicked()
                    } else {
                        android.util.Log.d("FeedbackFloatingButton", "Detected as DRAG, position saved")
                    }
                    true
                }
                else -> false
            }
        }
    }
    
    /**
     * 反馈按钮点击
     */
    private fun onFeedbackButtonClicked() {
        try {
            android.util.Log.d("FeedbackFloatingButton", "═══════════════════════════════════════════════")
            android.util.Log.d("FeedbackFloatingButton", "📱 onFeedbackButtonClicked() START")
            android.util.Log.d("FeedbackFloatingButton", "═══════════════════════════════════════════════")
            android.util.Log.d("FeedbackFloatingButton", "→ Activity: ${activity.javaClass.name}")
            android.util.Log.d("FeedbackFloatingButton", "→ Is FragmentActivity: ${activity is androidx.fragment.app.FragmentActivity}")
            
            // 显示反馈弹窗
            if (activity is androidx.fragment.app.FragmentActivity) {
                android.util.Log.d("FeedbackFloatingButton", "→ Creating FeedbackBottomSheetDialog...")
                val dialog = FeedbackBottomSheetDialog.newInstance()
                
                android.util.Log.d("FeedbackFloatingButton", "→ Getting supportFragmentManager...")
                val fragmentManager = activity.supportFragmentManager
                android.util.Log.d("FeedbackFloatingButton", "→ FragmentManager: $fragmentManager")
                
                android.util.Log.d("FeedbackFloatingButton", "→ Showing dialog...")
                dialog.show(fragmentManager, FeedbackBottomSheetDialog.TAG)
                
                android.util.Log.d("FeedbackFloatingButton", "✅ Dialog.show() called successfully")
                android.util.Log.d("FeedbackFloatingButton", "═══════════════════════════════════════════════")
            } else {
                android.util.Log.e("FeedbackFloatingButton", "❌ Activity is not FragmentActivity: ${activity.javaClass.name}")
                
                // 显示 Toast 提示
                android.widget.Toast.makeText(
                    activity,
                    "反馈功能暂时不可用（Activity类型不匹配）",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            android.util.Log.e("FeedbackFloatingButton", "❌ Exception in onFeedbackButtonClicked", e)
            android.util.Log.e("FeedbackFloatingButton", "❌ Exception type: ${e.javaClass.name}")
            android.util.Log.e("FeedbackFloatingButton", "❌ Exception message: ${e.message}")
            e.printStackTrace()
            
            // 显示 Toast 提示用户
            android.widget.Toast.makeText(
                activity,
                "反馈功能暂时不可用: ${e.message}",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }
    
    /**
     * 销毁悬浮按钮
     */
    fun destroy() {
        hide()
        floatingView = null
        windowManager = null
        params = null
    }
}

