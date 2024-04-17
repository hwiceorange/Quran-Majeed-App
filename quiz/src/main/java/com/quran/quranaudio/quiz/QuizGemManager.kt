package com.quranaudio.quiz.quiz

import com.quran.quranaudio.quiz.base.Constants
import com.quran.quranaudio.quiz.extension.SPTools
import com.quran.quranaudio.quiz.utils.RxBus

/**
 * quiz 金币管理
 */
object QuizGemManager {
    // 用户默认金币数量
    const val DEFAULT_COUNT = 30

    // 隐藏错误选项，消耗金币数量
    const val HIDE_ERROR_OPTION = 10

    // 加时消耗金币数量
    const val ADD_TIME = 10

    // 复活消耗金币数量
    const val RESTART_PLAY = 8

    // 看广告奖励金币数量
    const val WATCH_AD_REWARD = 5
    // 看签到广告奖励金币数量
    const val WATCH_DAILY_AD_REWARD = 30
    // 新用户赠送道具使用次数
    const val HIDE_ERROR_OPTION_PROP_COUNT = 2
    // 新用户赠送加时道具使用次数
    const val ADD_TIME_PROP_COUNT = 2

    /**
     * 获取金币
     */
    fun getGemCount(): Int {
        return SPTools.getInt(Constants.KEY_QUIZ_GEN_COUNT, DEFAULT_COUNT)
    }

    /**
     * 添加金币
     */
    fun addCount(count: Int) {
        val totalCount = SPTools.getInt(Constants.KEY_QUIZ_GEN_COUNT, DEFAULT_COUNT)
        SPTools.put(Constants.KEY_QUIZ_GEN_COUNT, totalCount + count)
        RxBus.INSTANCE().post(QuizGemChange())
    }

    /**
     * 消耗金币
     */
    fun consumeCount(count: Int): Boolean {
        val totalCount = SPTools.getInt(Constants.KEY_QUIZ_GEN_COUNT, DEFAULT_COUNT)
        return if (totalCount - count < 0) {
            false
        } else {
            SPTools.put(Constants.KEY_QUIZ_GEN_COUNT, totalCount - count)
            RxBus.INSTANCE().post(QuizGemChange())
            true
        }
    }

    /**
     * 获取隐藏选项道具数量
     */
    fun getHidePropCount():Int{
        return SPTools.getInt(Constants.KEY_HIDE_ERROR_OPTION_COUNT, HIDE_ERROR_OPTION_PROP_COUNT)
    }
    /**
     * 是否有隐藏选项道具
     */
    fun isEnableHideProp(): Boolean {
        val totalCount = SPTools.getInt(Constants.KEY_HIDE_ERROR_OPTION_COUNT, HIDE_ERROR_OPTION_PROP_COUNT)
        return totalCount > 0
    }

    /**
     * 消耗隐藏选项道具
     */
    fun consumeHideOptionPropCount(){
        val totalCount = SPTools.getInt(Constants.KEY_HIDE_ERROR_OPTION_COUNT, HIDE_ERROR_OPTION_PROP_COUNT)
        SPTools.put(Constants.KEY_HIDE_ERROR_OPTION_COUNT, totalCount - 1)
        RxBus.INSTANCE().post(QuizPropChange())
    }


    /**
     * 获取隐藏选项道具数量
     */
    fun getAddTimePropCount():Int{
        return SPTools.getInt(Constants.KEY_ADD_TIME_COUNT, ADD_TIME_PROP_COUNT)
    }

    /**
     * 是否有隐藏选项道具
     */
    fun isEnableAddTimeProp(): Boolean {
        val totalCount = SPTools.getInt(Constants.KEY_ADD_TIME_COUNT, ADD_TIME_PROP_COUNT)
        return totalCount > 0
    }

    /**
     * 消耗隐藏选项道具
     */
    fun consumeAddTimeOptionPropCount(){
        val totalCount = SPTools.getInt(Constants.KEY_ADD_TIME_COUNT, ADD_TIME_PROP_COUNT)
        SPTools.put(Constants.KEY_ADD_TIME_COUNT, totalCount - 1)
        RxBus.INSTANCE().post(QuizPropChange())
    }

}

class QuizGemChange {

}
class QuizPropChange {

}