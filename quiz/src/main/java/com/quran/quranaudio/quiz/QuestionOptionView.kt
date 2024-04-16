package com.bible.tools.quiz

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.LinearLayoutCompat
import com.bible.tools.extension.logd
import com.bible.tools.extension.reportClickEvent
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
                    val isRight = it.tag == this.answer
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
        if (questionBean.options.size != mOptionListView.size) {
            throw IllegalAccessException("question content is error, option is not 4, is ${questionBean.options}")
        }
        questionBean.options.keys.forEachIndexed { index, s ->
            mOptionListView[index]?.run {
                resetStyle()
                tag = s
                setData(s, questionBean.options[s] ?: "")
            }
        }
    }

    fun hideOption(noGemCallBack:()->Unit) {
        mQuestionBean?.run {
            val noHideList =
                mOptionListView.filter { it?.tag != this.answer && it?.isHideStatus() == false }
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