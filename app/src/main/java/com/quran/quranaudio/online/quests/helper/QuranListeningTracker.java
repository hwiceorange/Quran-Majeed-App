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
 * Helper class to track Quran listening progress for Daily Quests.
 * 
 * Tracks actual playback time (excluding pauses) to ensure accurate
 * listening duration for daily quest completion.
 */
public class QuranListeningTracker {
    
    private static final String TAG = "QuranListeningTracker";
    private static final String PREFS_NAME = "QuranListeningQuestPrefs";
    private static final String KEY_MINUTES_LISTENED_TODAY = "minutes_listened_today";
    private static final String KEY_SECONDS_LISTENED_TODAY = "seconds_listened_today";
    private static final String KEY_LAST_DATE = "last_date";
    private static final String KEY_TASK_COMPLETED_TODAY = "task_completed_today";
    private static final String KEY_SESSION_START_TIME = "session_start_time";
    
    private final Context context;
    private final SharedPreferences prefs;
    private final QuestRepository questRepository;
    
    // Session tracking
    private long sessionStartTime = 0;
    private long totalPausedDuration = 0;
    private long lastPauseTime = 0;
    private boolean isPaused = false;
    
    public QuranListeningTracker(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.questRepository = new QuestRepository(FirebaseFirestore.getInstance());
    }
    
    /**
     * Starts a listening session
     */
    public void startListening() {
        if (sessionStartTime == 0) {
            sessionStartTime = System.currentTimeMillis();
            totalPausedDuration = 0;
            isPaused = false;
            
            prefs.edit()
                .putLong(KEY_SESSION_START_TIME, sessionStartTime)
                .apply();
            
            Log.d(TAG, "🎧 Listening session started");
        }
    }
    
    /**
     * Pauses the listening session
     */
    public void pauseListening() {
        if (!isPaused && sessionStartTime > 0) {
            lastPauseTime = System.currentTimeMillis();
            isPaused = true;
            Log.d(TAG, "⏸️ Listening paused");
        }
    }
    
    /**
     * Resumes the listening session
     */
    public void resumeListening() {
        if (isPaused && lastPauseTime > 0) {
            long pauseDuration = System.currentTimeMillis() - lastPauseTime;
            totalPausedDuration += pauseDuration;
            isPaused = false;
            lastPauseTime = 0;
            Log.d(TAG, "▶️ Listening resumed (paused for " + (pauseDuration / 1000) + "s)");
        }
    }
    
    /**
     * Stops the listening session and records the duration
     */
    public void stopListening() {
        if (sessionStartTime > 0) {
            // If still paused, add that duration
            if (isPaused && lastPauseTime > 0) {
                totalPausedDuration += System.currentTimeMillis() - lastPauseTime;
            }
            
            // Calculate effective listening duration (excluding pauses)
            long totalDuration = System.currentTimeMillis() - sessionStartTime;
            long effectiveDuration = totalDuration - totalPausedDuration;
            int secondsListened = (int) (effectiveDuration / 1000);
            
            recordSecondsListened(secondsListened);
            
            // Reset session
            sessionStartTime = 0;
            totalPausedDuration = 0;
            lastPauseTime = 0;
            isPaused = false;
            
            prefs.edit()
                .remove(KEY_SESSION_START_TIME)
                .apply();
            
            Log.d(TAG, "🛑 Listening session stopped: " + secondsListened + "s effective");
        }
    }
    
    /**
     * Records listened seconds and accumulates to daily total
     */
    private void recordSecondsListened(int seconds) {
        if (seconds <= 0) {
            return;
        }
        
        String today = getTodayDateString();
        int currentSeconds = getTodaySecondsListened();
        int newTotal = currentSeconds + seconds;
        int newMinutes = newTotal / 60;
        
        prefs.edit()
            .putInt(KEY_SECONDS_LISTENED_TODAY, newTotal)
            .putInt(KEY_MINUTES_LISTENED_TODAY, newMinutes)
            .putString(KEY_LAST_DATE, today)
            .apply();
        
        Log.d(TAG, "🎵 Recorded " + seconds + "s (" + (seconds/60) + "m). Total today: " + newMinutes + " minutes");
    }
    
    /**
     * Gets the number of seconds listened today
     */
    public int getTodaySecondsListened() {
        String today = getTodayDateString();
        String savedDate = prefs.getString(KEY_LAST_DATE, "");
        
        // If it's a new day, reset
        if (!today.equals(savedDate)) {
            resetDailyProgress();
            return 0;
        }
        
        return prefs.getInt(KEY_SECONDS_LISTENED_TODAY, 0);
    }
    
    /**
     * Gets the number of minutes listened today
     */
    public int getTodayMinutesListened() {
        String today = getTodayDateString();
        String savedDate = prefs.getString(KEY_LAST_DATE, "");
        
        // If it's a new day, reset
        if (!today.equals(savedDate)) {
            resetDailyProgress();
            return 0;
        }
        
        return prefs.getInt(KEY_MINUTES_LISTENED_TODAY, 0);
    }
    
    /**
     * Checks if the daily quest target has been reached and marks task as complete if needed.
     * 
     * @param targetMinutes The target listening duration in minutes (from user's quest config)
     */
    public void checkAndMarkComplete(int targetMinutes) {
        int minutesListened = getTodayMinutesListened();
        
        Log.d(TAG, "📊 Checking completion: " + minutesListened + " / " + targetMinutes + " minutes");
        
        if (minutesListened >= targetMinutes) {
            // Check if already marked complete today
            String today = getTodayDateString();
            boolean alreadyCompleted = prefs.getBoolean(KEY_TASK_COMPLETED_TODAY, false);
            String completedDate = prefs.getString("completed_date", "");
            
            Log.d(TAG, "🔍 Completion check: alreadyCompleted=" + alreadyCompleted + 
                ", completedDate=" + completedDate + ", today=" + today);
            
            if (alreadyCompleted && today.equals(completedDate)) {
                Log.d(TAG, "Task already completed today - skipping Firebase update");
                return; // Already marked today
            }
            
            Log.d(TAG, "🚀 Marking task as complete for the first time today...");
            
            // Mark as complete
            markTaskComplete();
            
            // Save completion flag
            prefs.edit()
                .putBoolean(KEY_TASK_COMPLETED_TODAY, true)
                .putString("completed_date", today)
                .apply();
            
            Log.d(TAG, "✅ Daily Quran Listening Quest completed! (" + minutesListened + "/" + targetMinutes + " minutes)");
        } else {
            Log.d(TAG, "🎧 Keep listening: " + minutesListened + "/" + targetMinutes + " minutes completed");
        }
    }
    
    /**
     * Marks the Quran Listening task as complete in Firebase.
     */
    private void markTaskComplete() {
        // Check if user is logged in
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Log.w(TAG, "⚠️ User not logged in - cannot mark task complete");
            return;
        }
        
        Log.d(TAG, "🔥 Calling markTaskComplete() - starting Firebase update...");
        
        // Update task completion in Firebase (Task 2: Quran Listening)
        new Thread(() -> {
            try {
                Log.d(TAG, "🔥 Thread started - calling questRepository.updateTaskCompletion()");
                questRepository.updateTaskCompletion("task2TajweedCompleted", true);
                Log.d(TAG, "✅ updateTaskCompletion() called successfully (async operation may still be in progress)");
            } catch (Exception e) {
                Log.e(TAG, "❌ Failed to call updateTaskCompletion", e);
                e.printStackTrace();
            }
        }).start();
    }
    
    /**
     * Resets daily progress (called when a new day starts).
     */
    private void resetDailyProgress() {
        String today = getTodayDateString();
        prefs.edit()
            .putInt(KEY_SECONDS_LISTENED_TODAY, 0)
            .putInt(KEY_MINUTES_LISTENED_TODAY, 0)
            .putString(KEY_LAST_DATE, today)
            .putBoolean(KEY_TASK_COMPLETED_TODAY, false)
            .apply();
    }
    
    /**
     * Gets today's date as a string (YYYY-MM-DD).
     */
    private String getTodayDateString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return sdf.format(new Date());
    }
    
    /**
     * Gets the current effective listening duration for this session (in seconds)
     */
    public int getCurrentSessionSeconds() {
        if (sessionStartTime == 0) {
            return 0;
        }
        
        long currentPausedDuration = totalPausedDuration;
        if (isPaused && lastPauseTime > 0) {
            currentPausedDuration += System.currentTimeMillis() - lastPauseTime;
        }
        
        long totalDuration = System.currentTimeMillis() - sessionStartTime;
        long effectiveDuration = totalDuration - currentPausedDuration;
        return (int) (effectiveDuration / 1000);
    }
}

