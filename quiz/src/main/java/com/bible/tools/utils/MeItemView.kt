package com.bible.tools.utils

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.getResourceIdOrThrow
import androidx.core.view.isVisible
import com.quran.quranaudio.quiz.R
import com.quran.quranaudio.quiz.databinding.LayoutItemMeBinding


class MeItemView : ConstraintLayout {
    val binding = LayoutItemMeBinding.inflate(LayoutInflater.from(context), this)

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    @SuppressLint("CustomViewStyleable")
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        attrs?.let {
            val ob = context.obtainStyledAttributes(it, R.styleable.MeItemVIew)
            val isShow = ob.getBoolean(R.styleable.MeItemVIew_itemLeftVisibility, true)
            binding.itemLeftIconIv.isVisible = isShow
            binding.holdSpace.isVisible = isShow
            if (isShow) {
                if (ob.hasValue(R.styleable.MeItemVIew_itemLeftDrawable)) {
                    binding.itemLeftIconIv.setImageResource(ob.getResourceIdOrThrow(R.styleable.MeItemVIew_itemLeftDrawable))
                }
            }

            if (ob.hasValue(R.styleable.MeItemVIew_android_text)) {
                binding.itemTitleTv.text = ob.getText(R.styleable.MeItemVIew_android_text)
            }
            if (ob.hasValue(R.styleable.MeItemVIew_itemRightDrawable)) {
                binding.itemRightIconIv.setImageResource(ob.getResourceIdOrThrow(R.styleable.MeItemVIew_itemRightDrawable))
            }
            ob.recycle()

        }
    }

    fun setRightVisible(isShow: Boolean) {
        binding.itemRightIconIv.isVisible = isShow
    }
}
