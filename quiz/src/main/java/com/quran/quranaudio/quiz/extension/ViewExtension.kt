package com.quran.quranaudio.quiz.extension

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.Paint
import android.view.View
import android.widget.TextView
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes


fun View.playScaleXY(startDelayTime: Long, durationTime: Long, vararg values: Float): AnimatorSet {
    val animatorSet = AnimatorSet().apply {
        playTogether(
            ObjectAnimator.ofFloat(
                this@playScaleXY, View.SCALE_X, *values
            ).apply {
                repeatCount = ObjectAnimator.INFINITE
            },
            ObjectAnimator.ofFloat(
                this@playScaleXY, View.SCALE_Y, *values
            ).apply {
                repeatCount = ObjectAnimator.INFINITE
            }
        )
        this.startDelay = startDelayTime
        this.duration = durationTime
    }
    animatorSet.start()
    return animatorSet
}



fun View.playScaleXY(startDelayTime: Long, durationTime: Long, repeatCount:Int,vararg values: Float): AnimatorSet {
    val animatorSet = AnimatorSet().apply {
        playTogether(
            ObjectAnimator.ofFloat(
                this@playScaleXY, View.SCALE_X, *values
            ).apply {
                this.repeatCount = repeatCount
            },
            ObjectAnimator.ofFloat(
                this@playScaleXY, View.SCALE_Y, *values
            ).apply {
                this.repeatCount = repeatCount
            }
        )
        this.startDelay = startDelayTime
        this.duration = durationTime
    }
    animatorSet.start()
    return animatorSet
}


fun TextView.setUnderLine(){
    paint.flags = Paint.UNDERLINE_TEXT_FLAG
    paint.isAntiAlias = true
}

fun TextView.setDrawableLeft(@DrawableRes drawableId:Int,@DimenRes sizeId:Int ) {
    var drawable = drawableId.getResDrawable()
    if (drawable == null) {
        drawable = null
    } else {
        val height: Int = sizeId.getDimension().toInt()
        drawable.setBounds(0, 0, height, height)
    }
    this.setCompoundDrawables(drawable, null, null, null)
}

fun TextView.setDrawableRight(@DrawableRes drawableId:Int,@DimenRes sizeId:Int ) {
    var drawable = drawableId.getResDrawable()
    if (drawable == null) {
        drawable = null
    } else {
        val height: Int = sizeId.getDimension().toInt()
        drawable.setBounds(0, 0, height, height)
    }
    this.setCompoundDrawables(null, null, drawable, null)
}

fun View.gone() = run { visibility = View.GONE }

fun View.visible() = run { visibility = View.VISIBLE }

fun View.invisible() = run { visibility = View.INVISIBLE }