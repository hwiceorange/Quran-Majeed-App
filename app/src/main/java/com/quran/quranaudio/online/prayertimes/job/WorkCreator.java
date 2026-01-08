package com.quran.quranaudio.online.prayertimes.job;

import android.content.Context;

import androidx.work.BackoffPolicy;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.gson.Gson;
import com.quran.quranaudio.online.prayertimes.timings.DayPrayer;

import java.util.concurrent.TimeUnit;


public final class WorkCreator {

    private WorkCreator() {
    }
    
    /**
     * 调度 WorkManager 数据库清理任务
     * 
     * 🔥 修复崩溃：防止 WorkManager 数据库过大导致 CursorWindowAllocationException
     * - 每7天自动执行一次清理
     * - 清理已完成/失败/取消的工作记录
     * - 使用 KEEP 策略，避免重复调度
     */
    public static void scheduleWorkManagerCleanup(Context context) {
        PeriodicWorkRequest cleanupRequest = new PeriodicWorkRequest.Builder(
                WorkManagerCleanupWorker.class,
                7, TimeUnit.DAYS,  // 每7天执行一次
                1, TimeUnit.DAYS   // 允许1天的弹性窗口
        )
        .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                1, TimeUnit.HOURS
        )
        .build();
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "WORKMANAGER_DATABASE_CLEANUP",
                ExistingPeriodicWorkPolicy.KEEP,  // 保持现有调度，避免重复
                cleanupRequest
        );
        
        android.util.Log.d("WorkCreator", "✅ WorkManager cleanup task scheduled (every 7 days)");
    }

    public static void schedulePeriodicPrayerUpdater(Context context) {

        PeriodicWorkRequest periodicWorkRequest =
                new PeriodicWorkRequest
                        .Builder(PrayerUpdater.class, 30, TimeUnit.MINUTES, 15, TimeUnit.MINUTES)
                        .setBackoffCriteria(
                                BackoffPolicy.LINEAR,
                                10,
                                TimeUnit.MINUTES)
                        .build();

        WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork("QURAN_READER_UPDATER", ExistingPeriodicWorkPolicy.KEEP, periodicWorkRequest);
    }

    public static void scheduleOneTimePrayerUpdater(Context context, DayPrayer dayPrayer) {
        Gson gson = new Gson();
        String dayPrayerString = gson.toJson(dayPrayer);

        Data data = new Data.Builder()
                .putString("DAY_PRAYER_PARAM",dayPrayerString)
                .build();

        OneTimeWorkRequest oneTimeWorkRequest = new OneTimeWorkRequest
                .Builder(InstantPrayerScheduler.class)
                .setInputData(data)
                .build();

        // 🔔 使用 REPLACE 策略，确保每次设置更改都会立即重新调度
        // KEEP 会忽略新的工作，导致设置更改无法生效
        WorkManager.getInstance(context)
                .enqueueUniqueWork(
                        "ONE_TIME_QURAN_READER_UPDATER",
                        ExistingWorkPolicy.REPLACE,
                        oneTimeWorkRequest
                );
    }
}
