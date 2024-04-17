package com.quranaudio.quiz.quiz

import android.content.Context
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.quran.quranaudio.quiz.R
import com.quran.quranaudio.quiz.extension.getResDrawable
import com.quran.quranaudio.quiz.extension.gone
import com.quran.quranaudio.quiz.extension.visible
import com.quran.quranaudio.quiz.databinding.LayoutQuestionOptionBinding

class QuestionOptionItemView : ConstraintLayout {

    val binding = LayoutQuestionOptionBinding.inflate(LayoutInflater.from(context), this, true)
    private val rightDrawable: Drawable? = R.drawable.bg_quiz_option_right.getResDrawable()
    private val wrongDrawable: Drawable? = R.drawable.bg_quiz_option_wrong.getResDrawable()
    private val normalDrawable: Drawable? = R.drawable.bg_quiz_option_noraml.getResDrawable()
    private var isHideStatus = false

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    init {
        background = normalDrawable
    }

    fun getAnswerText() = binding.optionAnswerTv.text.toString()

    fun setData(optionTitle: String, answer: String) {
        binding.optionTitleTv.text = optionTitle
        binding.optionAnswerTv.text = answer
    }

    fun setWrongStyle() {
        binding.optionResultIv.visible()
        binding.optionResultIv.setImageResource(R.drawable.vector_quiz_wrong)
        background = wrongDrawable
    }

    fun setRightStyle() {
        binding.optionResultIv.visible()
        binding.optionResultIv.setImageResource(R.drawable.vector_quiz_right)
        background = rightDrawable
    }

    fun setNoClick() {
        isEnabled = false
    }

    fun isHideStatus() = isHideStatus
    fun showHideLine() {
        isHideStatus = true
        binding.optionAnswerTv.paintFlags = Paint.STRIKE_THRU_TEXT_FLAG
        binding.optionTitleTv.paintFlags = Paint.STRIKE_THRU_TEXT_FLAG
        alpha = 0.3f
        setNoClick()
    }

    fun resetStyle() {
        isHideStatus = false
        isEnabled = true
        alpha = 1f
        binding.optionResultIv.gone()
        background = normalDrawable
        binding.optionAnswerTv.paintFlags = Paint.ANTI_ALIAS_FLAG
        binding.optionTitleTv.paintFlags = Paint.ANTI_ALIAS_FLAG
    }
}