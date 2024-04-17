package com.quran.quranaudio.quiz.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.TextView
import androidx.core.view.isVisible


object AnimatorUtils {

    fun animatorTranslateByCenter(
        sourceView: View,
        targetView: View,
        duration: Long = 1000,
        startDelay: Long = 0,
        onEnd: (() -> Unit)? = null
    ) {
        sourceView.translationX = 0f
        sourceView.translationY = 0f
        sourceView.isVisible = true
        // 获取 sourceView 的当前位置
        val startPosition = IntArray(2)
        sourceView.getLocationOnScreen(startPosition)
        val startX: Int = startPosition[0] + sourceView.getWidth() / 2
        val startY: Int = startPosition[1] + sourceView.getHeight() / 2

        // 获取 targetView 的中心位置
        val endPosition = IntArray(2)
        targetView.getLocationOnScreen(endPosition)
        val endX = endPosition[0] + targetView.width / 2
        val endY = endPosition[1] + targetView.height / 2

        // 创建属性动画，将视图从一个位置飞到另一个位置
        val animatorX: ObjectAnimator =
            ObjectAnimator.ofFloat(sourceView, View.TRANSLATION_X, 0f, (endX - startX).toFloat())
        val animatorY: ObjectAnimator =
            ObjectAnimator.ofFloat(sourceView, View.TRANSLATION_Y, 0f, (endY - startY).toFloat())

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(animatorX, animatorY)
        animatorSet.interpolator = AccelerateDecelerateInterpolator()
        animatorSet.duration = duration // 设置动画持续时间
        animatorSet.startDelay = startDelay


        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                // 动画结束后，可以执行额外的操作
                // 例如，隐藏或删除飞行视图
                onEnd?.invoke()
            }
        })

        // 启动动画
        animatorSet.start()
    }
    fun animatorScale(
        targetView: View,
        vararg values: Float,
        duration: Long = 1000,
        startDelay: Long = 0,
        onEnd: (() -> Unit)? = null
    ) {
        val animator = ValueAnimator.ofFloat(*values) // 从 1 倍放大到 2 倍，可以根据需要更改目标缩放比例
        animator.duration = duration // 动画持续时间，以毫秒为单位
        animator.startDelay = startDelay
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.addUpdateListener { animation ->
            val scale = animation.animatedValue as Float
            // 设置视图的缩放比例
            targetView.scaleX = scale
            targetView.scaleY = scale
        }
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                onEnd?.invoke()
            }
        })
        animator.start()
    }

    fun animatorText(
        textView: TextView, startValue: Int, endValue: Int,
        duration: Long = 1000,
        startDelay: Long = 0,
        onEnd: (() -> Unit)? = null
    ) {
        val animator = ValueAnimator.ofInt(startValue, endValue) // 从 0 变换到 100，可以根据需要更改目标数字
        animator.duration = duration // 动画持续时间，以毫秒为单位
        animator.startDelay = startDelay
        animator.addUpdateListener { animation ->
            val animatedValue = animation.animatedValue as Int
            // 将动画的当前值设置到 TextView 上
            textView.text = animatedValue.toString()
        }
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                onEnd?.invoke()
            }
        })
        // 启动动画
        animator.start()
    }
}