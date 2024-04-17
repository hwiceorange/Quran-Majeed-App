package com.quran.quranaudio.quiz.base

import com.quran.quranaudio.quiz.BuildConfig

object Constants {
    /* key */
    const val DAILY_REWARD_NUM = "daily_reward_num" //宝箱已领取天数
    const val LAST_RECEIVED_BOX_TIME = "last_received_box_time" //宝箱奖励获取的最后时
    const val KEY_QUIZ_TIPS_GUIDE = "key_quiz_tips_guide"
    const val KEY_MAIN_NOTIFY_SELECT_MOOD_DATE = "key_main_notify_select_mood_date"
    const val KEY_LAST_QUESTION_LEVEL = "key_last_question_level"
    const val KEY_LAST_QUESTION_START_INDEX = "key_last_question_start_index"
    const val KEY_MAIN_NOTIFY_SELECT_MOOD_INDEX = "key_main_notify_select_mood_index"
    const val KEY_MAIN_NOTIFY_PRAYER_WITH_GOD_CLICK_DATE = "key_main_notify_prayer_with_god_click_date"
    const val KEY_MAIN_NOTIFY_PRAYER_NOW_CLICK_DATE = "key_main_notify_prayer_now_click_date"
    const val KEY_ALARM_NIGHT_TIME = "key_alarm_night_time"
    const val KEY_ALARM_NIGHT_OPEN = "key_alarm_night_open"
    const val KEY_ALARM_MORNING_TIME = "key_alarm_morning_time"
    const val KEY_ALARM_MORNING_OPEN = "key_alarm_morning_open"
    const val KEY_IS_BIBLE_AUTO_SCROLL = "key_is_bible_auto_scroll"
    const val KEY_MARK_AS_READ_TIME: String = "key_mark_as_read_time"
    const val KEY_RECORD_SELECT_TIMER = "key_record_select_timer"
    const val KEY_HIGHLIGHT_COLOR = "key_highlight_color"
    const val KEY_IS_NEW_USER = "key_is_new_user"
    const val KEY_BIBLE_MOOD_CHOICE_DATE = "key_bible_mood_choice_date"
    const val KEY_DAY_DATE = "key_day_date"
    const val KEY_NIGHT_DATE = "key_night_date"
    //plan学习计划key
    const val KEY_STUDY_PLAN = "key_study_plan"
    //plan计划通知最后展示时间
    const val KEY_PLAN_NOTIFICATION_TIME = "key_plan_notification_time"
    //quiz答题通知最后展示时间
    const val KEY_QUIZ_NOTIFICATION_TIME = "key_quiz_notification_time"
    //通用通知最后展示时间
    const val KEY_UNIVERSAL_NOTIFICATION_TIME = "key_universal_notification_time"
    //push click 点击的日期
    const val KEY_CLICK_PUSH_DATE = "key_click_push_date"

    //悬浮窗权限弹窗最后弹出时间
    const val KEY_SYSTEM_PERMISSION_TIME = "key_system_permission_time"
    //悬浮窗amen额外弹窗最后弹出时间
    const val KEY_SYSTEM_AMEN_PERMISSION_TIME = "key_system_amen_permission_time"
    //通知权限弹窗最后弹出时间
    const val KEY_NOTIFY_PERMISSION_TIME = "key_notify_permission_time"
    //是否打开全屏的悬浮窗权限页面
    const val KEY_CAN_OPEN_SYSTEM = "key_can_open_system"
    //广告测试白名单
    const val KEY_REMOTE_AD_TEST_GAIDS = "key_remote_ad_test_gaids"
    //今天 quiz 通过一个 level
    const val KEY_THOUGHT_QUIZ_LEVEL_DATE = "key_thought_quiz_level_date"
    //今天完成过一天的计划
    const val KEY_FINISH_PLAN_DATE = "key_finish_plan_date"
    //已经展示过的push id列表
    const val KEY_ALREADY_SHOW_PUSH_QUIZ_ID = "key_already_show_push_quiz_id"
    const val KEY_OPEN_DEBUG_TIME = "key_open_debug_time"
    //宝石数量
    const val KEY_QUIZ_GEN_COUNT = "key_quiz_gen_count"
    //隐藏错误答案道具
    const val KEY_HIDE_ERROR_OPTION_COUNT = "key_hide_error_option_count"
    const val KEY_ADD_TIME_COUNT = "key_add_time_count"


    /* intent */
    const val INTENT_VERSE_INFO = "intent_verse_info"
    const val INTENT_AMEN_DATE = "intent_amen_date"
    const val INTENT_MOOD_FROM = "intent_mood_from"
    const val INTENT_MOOD_KEY = "intent_mood_key"
    const val INTENT_MOOD_DATE = "intent_mood_date"
    const val INTENT_SPLASH_INTO = "intent_splash_into"
    const val INTENT_SPLASH_PRAYER = "intent_splash_prayer"
    const val INTENT_SEARCH_INFORMATION_READ = "intent_search_information_read"
    const val INTENT_NOTIFY_QUIZ_BEAN = "intent_notify_quiz_bean"
    const val INTENT_NOTIFY_QUIZ_SELECT_ANSWER = "intent_notify_quiz_select_answer"
    const val INTENT_NOTIFY_PLAN_ID= "planId"


    const val ACTION_ATTRIBUTE_MAP_CHANGED_FROM_POPUP = "yuku.alkitab.action.ATTRIBUTE_MAP_CHANGED_FROM_POPUP"
    const val ACTION_UNCHECKED_VERSES: String = BuildConfig.LIBRARY_PACKAGE_NAME + ".action.unchecked.verses"
    const val ACTION_ATTRIBUTE_MAP_CHANGED = BuildConfig.LIBRARY_PACKAGE_NAME + ".action.reload.verses"

}