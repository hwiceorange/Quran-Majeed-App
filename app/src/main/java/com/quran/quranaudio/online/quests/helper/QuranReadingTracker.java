package com.quran.quranaudio.online.quests.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.quran.quranaudio.online.quests.repository.QuestRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Helper class to track Quran reading progress for Daily Quests.
 * 
 * Since converting Surah/Ayah to precise page numbers requires a Mushaf mapping table,
 * this simplified version tracks reading sessions and estimates pages based on reading time/activity.
 * 
 * Future improvement: Implement full Mushaf page mapping for precise page tracking.
 */
public class QuranReadingTracker {
    
    private static final String TAG = "QuranReadingTracker";
    private static final String PREFS_NAME = "QuranReadingQuestPrefs";
    private static final String KEY_PAGES_READ_TODAY = "pages_read_today";
    private static final String KEY_VERSES_READ_TODAY = "verses_read_today";  // 🔥 新增：verses计数
    private static final String KEY_LAST_DATE = "last_date";
    private static final String KEY_TASK_COMPLETED_TODAY = "task_completed_today";
    
    private final Context context;
    private final SharedPreferences prefs;
    private final QuestRepository questRepository;
    
    public QuranReadingTracker(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.questRepository = new QuestRepository(FirebaseFirestore.getInstance());
    }
    
    /**
     * Records that the user has read some pages of the Quran.
     * This should be called when user exits the Quran Reader or after significant reading activity.
     * 
     * @param pagesRead Number of pages read in this session
     */
    public void recordPagesRead(int pagesRead) {
        if (pagesRead <= 0) {
            return;
        }
        
        String today = getTodayDateString();
        int currentPages = getTodayPagesRead();
        int newTotal = currentPages + pagesRead;
        
        prefs.edit()
            .putInt(KEY_PAGES_READ_TODAY, newTotal)
            .putString(KEY_LAST_DATE, today)
            .apply();
        
        Log.d(TAG, "Recorded " + pagesRead + " pages. Total today: " + newTotal);
    }
    
    /**
     * 🔥 新方法：记录实际阅读的页码范围（更精确）
     * 
     * @param startPage 开始页码
     * @param endPage 结束页码
     */
    public void recordPageRange(int startPage, int endPage) {
        if (startPage <= 0 || endPage <= 0 || endPage < startPage) {
            Log.w(TAG, "Invalid page range: " + startPage + " to " + endPage);
            return;
        }
        
        int pagesRead = endPage - startPage + 1;
        recordPagesRead(pagesRead);
        Log.d(TAG, "Recorded page range: " + startPage + "-" + endPage + " (" + pagesRead + " pages)");
    }
    
    /**
     * 🔥 新方法：记录实际阅读的经文数量
     * 
     * @param versesRead 阅读的经文数
     */
    public void recordVersesRead(int versesRead) {
        if (versesRead <= 0) {
            return;
        }
        
        String today = getTodayDateString();
        int currentVerses = getTodayVersesRead();
        int newTotal = currentVerses + versesRead;
        
        prefs.edit()
            .putInt(KEY_VERSES_READ_TODAY, newTotal)
            .putString(KEY_LAST_DATE, today)
            .apply();
        
        Log.d(TAG, "✅ Recorded " + versesRead + " verses. Total today: " + newTotal);
        
        // 🔥 修复：移除向后兼容的page转换，避免混淆不同单位的计数
        // 如果用户配置是VERSES单位，就只记录VERSES，不再同时更新PAGES
        // 这样可以确保计数的准确性
    }
    
    /**
     * 获取今天已阅读的经文数量
     */
    private int getTodayVersesRead() {
        String today = getTodayDateString();
        String lastDate = prefs.getString(KEY_LAST_DATE, "");
        
        if (!today.equals(lastDate)) {
            return 0;  // 新的一天，重置计数
        }
        
        return prefs.getInt(KEY_VERSES_READ_TODAY, 0);
    }
    
    /**
     * 🔥 新增：获取今天已阅读的经文数量（公共方法，用于调试）
     */
    public int getTodayVersesReadPublic() {
        return getTodayVersesRead();
    }
    
    /**
     * 🔥 新增：打印当前计数状态（用于调试）
     */
    public void logCurrentProgress() {
        String today = getTodayDateString();
        int versesRead = getTodayVersesRead();
        int pagesRead = getTodayPagesRead();
        boolean isCompleted = prefs.getBoolean(KEY_TASK_COMPLETED_TODAY, false);
        
        Log.d(TAG, "═══════════════════════════════════════");
        Log.d(TAG, "📊 当前阅读进度统计");
        Log.d(TAG, "───────────────────────────────────────");
        Log.d(TAG, "📅 日期: " + today);
        Log.d(TAG, "📖 今日已读Verses: " + versesRead);
        Log.d(TAG, "📄 今日已读Pages: " + pagesRead);
        Log.d(TAG, "✅ 任务完成状态: " + (isCompleted ? "已完成" : "未完成"));
        Log.d(TAG, "═══════════════════════════════════════");
    }
    
    /**
     * 🔥 新方法：记录Juz阅读进度（用于JUZ类型任务）
     * 
     * @param juzNo Juz编号 (1-30)
     * @param pagesInJuz 在该Juz内阅读的页数
     */
    public void recordJuzProgress(int juzNo, int pagesInJuz) {
        if (juzNo < 1 || juzNo > 30 || pagesInJuz <= 0) {
            Log.w(TAG, "Invalid Juz progress: Juz " + juzNo + ", " + pagesInJuz + " pages");
            return;
        }
        
        recordPagesRead(pagesInJuz);
        Log.d(TAG, "Recorded Juz " + juzNo + " progress: " + pagesInJuz + " pages");
    }
    
    /**
     * Gets the number of pages read today.
     * Automatically resets if it's a new day.
     */
    public int getTodayPagesRead() {
        String today = getTodayDateString();
        String savedDate = prefs.getString(KEY_LAST_DATE, "");
        
        // If it's a new day, reset
        if (!today.equals(savedDate)) {
            resetDailyProgress();
            return 0;
        }
        
        return prefs.getInt(KEY_PAGES_READ_TODAY, 0);
    }
    
    /**
     * Checks if the daily quest target has been reached and marks task as complete if needed.
     * 🔥 已弃用：请使用 checkAndMarkCompleteAsync() 来自动获取用户配置
     * 
     * @param targetPages The target number of pages (from user's quest config)
     * @deprecated Use {@link #checkAndMarkCompleteAsync()} instead
     */
    @Deprecated
    public void checkAndMarkComplete(int targetPages) {
        int pagesRead = getTodayPagesRead();
        
        if (pagesRead >= targetPages) {
            // Check if already marked complete today
            String today = getTodayDateString();
            boolean alreadyCompleted = prefs.getBoolean(KEY_TASK_COMPLETED_TODAY, false);
            String completedDate = prefs.getString("completed_date", "");
            
            if (alreadyCompleted && today.equals(completedDate)) {
                Log.d(TAG, "Task already completed today");
                return; // Already marked today
            }
            
            // Mark as complete
            markTaskComplete();
            
            // Save completion flag
            prefs.edit()
                .putBoolean(KEY_TASK_COMPLETED_TODAY, true)
                .putString("completed_date", today)
                .apply();
            
            Log.d(TAG, "Daily Quran Reading Quest completed! (" + pagesRead + "/" + targetPages + " pages)");
        }
    }
    
    /**
     * 异步检查并标记任务完成 - 自动从Firebase获取用户配置
     * 这会根据用户的实际目标（Pages/Verses/Juz）来判断是否完成
     */
    public void checkAndMarkCompleteAsync() {
        new Thread(() -> {
            try {
                // 检查用户是否登录
                if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                    Log.w(TAG, "User not logged in - cannot check quest completion");
                    return;
                }
                
                // 获取用户配置
                com.quran.quranaudio.online.quests.data.UserQuestConfig config = 
                    questRepository.getUserQuestConfigSync();
                
                if (config == null) {
                    Log.w(TAG, "No quest config found - using fallback check");
                    checkAndMarkComplete(10); // 回退到默认10页
                    return;
                }
                
                // 根据配置的阅读单位判断
                int targetGoal = config.getDailyReadingGoal();
                String unit = config.getReadingGoalUnit();
                
                // 🔥 修复：根据配置单位选择正确的进度值进行比较
                int currentProgress;
                if ("VERSES".equalsIgnoreCase(unit) || "verses".equals(unit)) {
                    currentProgress = getTodayVersesRead();
                    Log.d(TAG, "📖 Checking completion: " + currentProgress + " verses read / " + targetGoal + " verses target");
                } else if ("PAGES".equalsIgnoreCase(unit) || "pages".equals(unit)) {
                    currentProgress = getTodayPagesRead();
                    Log.d(TAG, "📖 Checking completion: " + currentProgress + " pages read / " + targetGoal + " pages target");
                } else if ("JUZ".equalsIgnoreCase(unit) || "juz".equals(unit)) {
                    // JUZ 目前还是转换为 pages 进行比较（1 Juz = 20 pages）
                    currentProgress = getTodayPagesRead();
                    targetGoal = targetGoal * 20;
                    Log.d(TAG, "📖 Checking completion: " + currentProgress + " pages read / " + targetGoal + " pages (from Juz) target");
                } else {
                    // 默认使用 pages
                    currentProgress = getTodayPagesRead();
                    Log.d(TAG, "📖 Checking completion (default): " + currentProgress + " pages read / " + targetGoal + " target");
                }
                
                // 直接比较，不再转换
                boolean isCompleted = currentProgress >= targetGoal;
                
                if (isCompleted) {
                    Log.d(TAG, "✅ Reading goal achieved! " + currentProgress + " >= " + targetGoal);
                    // Check if already marked complete today
                    String today = getTodayDateString();
                    boolean alreadyCompleted = prefs.getBoolean(KEY_TASK_COMPLETED_TODAY, false);
                    String completedDate = prefs.getString("completed_date", "");
                    
                    if (alreadyCompleted && today.equals(completedDate)) {
                        Log.d(TAG, "Task already completed today");
                        return;
                    }
                    
                    // Mark as complete
                    markTaskComplete();
                    
                    // Save completion flag
                    prefs.edit()
                        .putBoolean(KEY_TASK_COMPLETED_TODAY, true)
                        .putString("completed_date", today)
                        .apply();
                    
                    Log.d(TAG, "✅ Daily Quran Reading Quest completed! (" + 
                          currentProgress + " pages ≈ " + targetGoal + " " + unit + ")");
                } else {
                    Log.d(TAG, "📚 Keep reading: " + currentProgress + "/" + targetGoal + " completed");
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to check quest completion", e);
            }
        }).start();
    }
    
    /**
     * Marks the Quran Reading task as complete in Firebase.
     */
    private void markTaskComplete() {
        // Check if user is logged in
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Log.w(TAG, "User not logged in - cannot mark task complete");
            return;
        }
        
        // Note: Ad will be shown when user exits ActivityReader, not here
        // to avoid interrupting the user while they are still reading
        
        // Update task completion in Firebase (Task 1: Quran Reading)
        new Thread(() -> {
            try {
                // 🔥 修复：使用正确的TaskId
                questRepository.updateTaskCompletion("task1ReadCompleted", true);
                Log.d(TAG, "Task 1 (Quran Reading) marked as complete in Firebase");
            } catch (Exception e) {
                Log.e(TAG, "Failed to mark task as complete", e);
            }
        }).start();
    }
    
    /**
     * Resets daily progress (called when a new day starts).
     */
    private void resetDailyProgress() {
        String today = getTodayDateString();
        prefs.edit()
            .putInt(KEY_PAGES_READ_TODAY, 0)
            .putInt(KEY_VERSES_READ_TODAY, 0)  // 🔥 新增：重置 verses 计数
            .putString(KEY_LAST_DATE, today)
            .putBoolean(KEY_TASK_COMPLETED_TODAY, false)
            .apply();
    }
    
    /**
     * Gets today's date as a string (YYYY-MM-DD).
     * 使用设备本地时区，确保与用户感知的"今天"一致
     */
    private String getTodayDateString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        // 显式使用设备默认时区，与用户所在时区保持一致
        sdf.setTimeZone(java.util.TimeZone.getDefault());
        return sdf.format(new Date());
    }
    
    /**
     * 将不同阅读单位转换为等效的pages数
     * 
     * 转换规则（基于古兰经标准Mushaf）：
     * - PAGES: 1:1 直接使用
     * - VERSES: 约12 ayahs = 1 page（粗略估算）
     * - JUZ: 1 juz = 20 pages
     * 
     * @param goal 目标值
     * @param unit 单位（PAGES/VERSES/JUZ）
     * @return 等效的pages数
     */
    private int convertToPages(int goal, String unit) {
        if (unit == null || unit.isEmpty()) {
            return goal; // 默认视为pages
        }
        
        switch (unit.toUpperCase()) {
            case "VERSES":
                // 粗略估算：12 verses ≈ 1 page
                // 为了让小目标更容易完成，使用10 verses = 1 page
                return Math.max(1, goal / 10);
                
            case "JUZ":
                // 1 Juz' = 20 pages
                return goal * 20;
                
            case "PAGES":
            default:
                return goal;
        }
    }
}




