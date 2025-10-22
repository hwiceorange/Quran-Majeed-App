package com.quran.quranaudio.online.quests.utils;

import android.app.Activity;
import android.content.Intent;

/**
 * Java 包装类用于在 ActivityReader 中集成 Listening Mode
 * 
 * 使用方法：
 * 1. 在 ActivityReader.onCreate() 中调用 onReaderStarted()
 * 2. 在 ActivityReader.onDestroy() 中调用 onReaderDestroyed()
 * 3. 当用户完成收听时调用 onListeningComplete()
 */
public class ListeningModeIntegration {
    
    private static boolean isListeningModeActive = false;
    
    /**
     * 当 Reader 启动时调用（在 onCreate 中）
     * 
     * @param activity ActivityReader 实例
     * @param intent 启动 Intent
     * @param surah 当前 Surah 编号
     * @param ayah 当前 Ayah 编号
     */
    public static void onReaderStarted(Activity activity, Intent intent, int surah, int ayah) {
        isListeningModeActive = ListeningModeHelper.INSTANCE.checkAndStartListeningMode(
            activity, intent, surah, ayah, 1, 1
        );
    }
    
    /**
     * 当 Reader 销毁时调用（在 onDestroy 中）
     * 
     * @param activity ActivityReader 实例
     * @param surah 当前 Surah 编号
     * @param ayah 当前 Ayah 编号
     */
    public static void onReaderDestroyed(Activity activity, int surah, int ayah) {
        if (isListeningModeActive) {
            ListeningModeHelper.INSTANCE.stopListeningMode(activity, surah, ayah, 1, 1);
            isListeningModeActive = false;
        }
    }
    
    /**
     * 当用户手动完成收听时调用
     * 
     * @param activity ActivityReader 实例
     */
    public static void onListeningComplete(Activity activity) {
        if (isListeningModeActive) {
            ListeningModeHelper.INSTANCE.completeListeningTask(activity);
        }
    }
    
    /**
     * 检查是否为 Listening Mode
     */
    public static boolean isListeningMode() {
        return isListeningModeActive;
    }
}

