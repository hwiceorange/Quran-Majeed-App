package com.quran.quranaudio.online.prayertimes.job;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import androidx.work.WorkManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * WorkManager 数据库清理 Worker
 * 
 * 功能：
 * - 定期清理 WorkManager 已完成/失败/取消的工作记录
 * - 防止数据库过大导致 CursorWindowAllocationException
 * - 每7天自动执行一次
 * 
 * 修复崩溃：
 * - Fatal Exception: android.database.CursorWindowAllocationException
 * - Could not allocate CursorWindow of size 2097152 due to error -12
 */
public class WorkManagerCleanupWorker extends Worker {
    
    private static final String TAG = "WorkManagerCleanup";
    private static final String PREFS_NAME = "workmanager_cleanup_prefs";
    private static final String KEY_LAST_CLEANUP_TIME = "last_cleanup_time";
    private static final long CLEANUP_INTERVAL_DAYS = 7; // 每7天清理一次
    
    public WorkManagerCleanupWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }
    
    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "═══════════════════════════════════════════════");
        Log.d(TAG, "🧹 WorkManager Database Cleanup Started");
        Log.d(TAG, "═══════════════════════════════════════════════");
        
        try {
            // 检查是否需要清理（避免频繁清理）
            if (!shouldCleanup()) {
                Log.d(TAG, "⏭️ Skipping cleanup - cleaned recently");
                return Result.success();
            }
            
            // 执行清理
            Log.d(TAG, "→ Pruning completed/failed/cancelled work records...");
            WorkManager workManager = WorkManager.getInstance(getApplicationContext());
            workManager.pruneWork();
            
            // 记录清理时间
            recordCleanupTime();
            
            Log.d(TAG, "✅ WorkManager database cleaned successfully");
            Log.d(TAG, "═══════════════════════════════════════════════");
            
            return Result.success();
            
        } catch (Exception e) {
            Log.e(TAG, "❌ WorkManager cleanup failed", e);
            Log.e(TAG, "═══════════════════════════════════════════════");
            
            // 清理失败不应该阻止下次尝试
            return Result.retry();
        }
    }
    
    /**
     * 检查是否应该执行清理
     * 避免过于频繁的清理操作
     */
    private boolean shouldCleanup() {
        SharedPreferences prefs = getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        long lastCleanupTime = prefs.getLong(KEY_LAST_CLEANUP_TIME, 0);
        long currentTime = System.currentTimeMillis();
        long daysSinceLastCleanup = TimeUnit.MILLISECONDS.toDays(currentTime - lastCleanupTime);
        
        Log.d(TAG, "Last cleanup: " + formatTime(lastCleanupTime));
        Log.d(TAG, "Days since last cleanup: " + daysSinceLastCleanup);
        
        return daysSinceLastCleanup >= CLEANUP_INTERVAL_DAYS;
    }
    
    /**
     * 记录清理时间
     */
    private void recordCleanupTime() {
        SharedPreferences prefs = getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        long currentTime = System.currentTimeMillis();
        prefs.edit()
                .putLong(KEY_LAST_CLEANUP_TIME, currentTime)
                .apply();
        
        Log.d(TAG, "Cleanup time recorded: " + formatTime(currentTime));
    }
    
    /**
     * 格式化时间戳为可读字符串
     */
    private String formatTime(long timestamp) {
        if (timestamp == 0) {
            return "Never";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        return sdf.format(new Date(timestamp));
    }
}

