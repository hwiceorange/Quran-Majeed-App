package com.quran.quranaudio.online.quests.utils

import android.app.Activity
import android.content.Intent
import android.util.Log
import com.quran.quranaudio.online.quests.service.ListeningTimerService

/**
 * 辅助类用于处理 Quran Listening Mode
 * 
 * 功能：
 * - 检测是否为 Listening Mode
 * - 启动计时器 Service
 * - 保存学习状态
 */
object ListeningModeHelper {
    
    private const val TAG = "ListeningModeHelper"
    
    /**
     * 从 Intent 检查是否为 Listening Mode 并启动计时器
     * 
     * @param activity ActivityReader 实例
     * @param intent 启动 Intent
     * @param currentSurah 当前 Surah 编号
     * @param currentAyah 当前 Ayah 编号
     * @param currentPage 当前页码 (可选)
     * @param currentJuz 当前 Juz (可选)
     * @return true 如果是 Listening Mode
     */
    fun checkAndStartListeningMode(
        activity: Activity,
        intent: Intent?,
        currentSurah: Int,
        currentAyah: Int,
        currentPage: Int = 1,
        currentJuz: Int = 1
    ): Boolean {
        if (intent == null) return false
        
        val isListeningMode = intent.getBooleanExtra("LISTENING_MODE", false)
        val autoPlayAudio = intent.getBooleanExtra("AUTO_PLAY_AUDIO", false)
        
        if (!isListeningMode && !autoPlayAudio) {
            return false
        }
        
        val targetMinutes = intent.getIntExtra("TARGET_MINUTES", 15)
        
        Log.d(TAG, "Listening Mode detected: target=$targetMinutes min, Surah $currentSurah, Ayah $currentAyah")
        
        // 启动计时器 Service
        ListeningTimerService.startListeningTimer(
            context = activity,
            targetMinutes = targetMinutes,
            surah = currentSurah,
            ayah = currentAyah,
            page = currentPage,
            juz = currentJuz
        )
        
        Log.d(TAG, "Listening timer started")
        return true
    }
    
    /**
     * 停止计时器并保存学习状态
     * 
     * @param activity ActivityReader 实例
     * @param currentSurah 当前 Surah 编号
     * @param currentAyah 当前 Ayah 编号
     * @param currentPage 当前页码 (可选)
     * @param currentJuz 当前 Juz (可选)
     */
    fun stopListeningMode(
        activity: Activity,
        currentSurah: Int,
        currentAyah: Int,
        currentPage: Int = 1,
        currentJuz: Int = 1
    ) {
        Log.d(TAG, "Stopping listening mode: Surah $currentSurah, Ayah $currentAyah")
        
        ListeningTimerService.stopListeningTimer(
            context = activity,
            surah = currentSurah,
            ayah = currentAyah,
            page = currentPage,
            juz = currentJuz
        )
    }
    
    /**
     * 手动标记任务完成
     * 
     * @param activity ActivityReader 实例
     */
    fun completeListeningTask(activity: Activity) {
        Log.d(TAG, "Manually completing listening task")
        ListeningTimerService.completeTask(activity)
    }
}

