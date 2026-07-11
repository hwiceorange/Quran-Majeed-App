package com.quranaudio.quiz.quiz

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.LinearLayoutCompat
import com.quran.quranaudio.quiz.extension.logd
import com.quran.quranaudio.quiz.extension.reportClickEvent
import com.quran.quranaudio.quiz.QuestionBean

class QuestionOptionView : LinearLayoutCompat {
    private val mOptionListView = arrayOfNulls<QuestionOptionItemView>(4)
    var mQuestionBean: QuestionBean? = null
    private var mAnswerResult: ((Boolean, String) -> Unit)? = null

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    init {
        orientation = VERTICAL
        for (i in 0..3) {
            val questionOptionItemView = QuestionOptionItemView(context)
            questionOptionItemView.setOnClickListener {
                reportClickEvent("quiz_option")
                for (optionItemView in mOptionListView) {
                    optionItemView?.setNoClick()
                }
                mQuestionBean?.run {
                    // 使用随机化后的正确答案进行验证
                    val isRight = it.tag == tempShuffledAnswer
                    if (isRight) {
                        questionOptionItemView.setRightStyle()
                    } else {
                        questionOptionItemView.setWrongStyle()
                    }
                    mAnswerResult?.invoke(isRight, questionOptionItemView.getAnswerText())
                }
            }
            mOptionListView[i] = questionOptionItemView
            addView(questionOptionItemView)
        }
    }

    fun setAnswerResultListener(answerResult: (Boolean, String) -> Unit) {
        this.mAnswerResult = answerResult
    }

    fun setData(questionBean: QuestionBean) {
        mQuestionBean = questionBean
        
        // ✅ CRITICAL FIX: Handle questions with incorrect number of options
        // Some questions may have 5 options (A, B, C, D, E) or empty options
        // Filter out empty options and take only first 4 valid options
        val validOptions = questionBean.options.filter { it.value.isNotBlank() }
        
        if (validOptions.isEmpty()) {
            android.util.Log.e("QuestionOptionView", "❌ No valid options found for question: ${questionBean.question}")
            throw IllegalAccessException("question content is error, no valid options found")
        }
        
        // If more than 4 options, take first 4
        val optionsToUse = if (validOptions.size > mOptionListView.size) {
            android.util.Log.w("QuestionOptionView", "⚠️ Question has ${validOptions.size} options, using first 4")
            validOptions.entries.take(mOptionListView.size).associate { it.key to it.value }
        } else if (validOptions.size < mOptionListView.size) {
            android.util.Log.e("QuestionOptionView", "❌ Question has only ${validOptions.size} options, expected 4")
            throw IllegalAccessException("question content is error, option count is ${validOptions.size}, expected 4")
        } else {
            validOptions
        }
        
        // 🎲 随机化选项内容，保持ABCD顺序
        val (shuffledOptions, newAnswerKey) = questionBean.getShuffledQuestion()
        
        // 更新题目的正确答案键（用于验证）
        // 注意：这里我们需要临时存储新的answer，因为原始questionBean不能修改
        tempShuffledAnswer = newAnswerKey
        
        // 按ABCD顺序显示，但内容已被随机打乱
        shuffledOptions.keys.forEachIndexed { index, key ->
            if (index < mOptionListView.size) {
                mOptionListView[index]?.run {
                    resetStyle()
                    tag = key  // tag保存键名(A/B/C/D)
                    setData(key, shuffledOptions[key] ?: "")
                }
            }
        }
    }
    
    private var tempShuffledAnswer: String = ""  // 临时存储随机化后的正确答案
    
    /**
     * 获取随机化后的正确答案键（用于测试模式显示）
     */
    fun getShuffledAnswer(): String {
        return tempShuffledAnswer
    }

    fun hideOption(noGemCallBack:()->Unit) {
        mQuestionBean?.run {
            val noHideList =
                mOptionListView.filter { it?.tag != tempShuffledAnswer && it?.isHideStatus() == false }
            if (noHideList.isNotEmpty()) {
                if (QuizGemManager.isEnableHideProp()) {
                    QuizGemManager.consumeHideOptionPropCount()
                    noHideList.random()?.showHideLine()
                    return
                }
                if (QuizGemManager.consumeCount(QuizGemManager.HIDE_ERROR_OPTION)) {
                    noHideList.random()?.showHideLine()
                } else {
                    noGemCallBack.invoke()
                }
            } else {
                logd("no enable hide option")
            }
        }
    }
}