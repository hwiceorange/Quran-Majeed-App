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
     */
    fun show() {
        if (isShowing) return
        
        try {
            windowManager = activity.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            
            // 创建悬浮视图
            floatingView = LayoutInflater.from(activity).inflate(
                R.layout.feedback_floating_button,
                null
            )
            
            // 计算合适的 Y 坐标（避免遮挡底部导航栏）
            // 底部导航栏高度约 56dp + 额外安全边距 16dp = 72dp
            val bottomNavHeightDp = 72
            val density = activity.resources.displayMetrics.density
            val bottomNavHeightPx = (bottomNavHeightDp * density).toInt()
            
            // 设置布局参数
            params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.END
                x = (24 * density).toInt() // 距离右边 24dp
                y = bottomNavHeightPx // 距离底部避开导航栏
            }
            
            // 添加到窗口
            windowManager?.addView(floatingView, params)
            
            // 设置触摸拖动（内部处理点击和拖动）
            setupTouchListener()
            
            isShowing = true
            android.util.Log.d("FeedbackFloatingButton", "✅ Floating button shown at y=$bottomNavHeightPx")
            
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
            android.util.Log.d("FeedbackFloatingButton", "📱 onFeedbackButtonClicked() called")
            
            // 显示反馈弹窗
            if (activity is androidx.fragment.app.FragmentActivity) {
                val dialog = FeedbackBottomSheetDialog.newInstance()
                dialog.show(activity.supportFragmentManager, FeedbackBottomSheetDialog.TAG)
                
                android.util.Log.d("FeedbackFloatingButton", "✅ Feedback dialog opened successfully")
            } else {
                android.util.Log.e("FeedbackFloatingButton", "❌ Activity is not FragmentActivity: ${activity.javaClass.name}")
            }
        } catch (e: Exception) {
            android.util.Log.e("FeedbackFloatingButton", "❌ Failed to open feedback dialog", e)
            
            // 显示 Toast 提示用户
            android.widget.Toast.makeText(
                activity,
                "反馈功能暂时不可用",
                android.widget.Toast.LENGTH_SHORT
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

