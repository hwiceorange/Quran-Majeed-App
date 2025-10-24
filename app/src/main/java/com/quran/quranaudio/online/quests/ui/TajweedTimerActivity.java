package com.quran.quranaudio.online.quests.ui;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.quran.quranaudio.online.R;
import com.quran.quranaudio.online.quests.repository.QuestRepository;

import java.util.Locale;

/**
 * Activity for Tajweed Practice Timer (Task 2 of Daily Quests).
 * 
 * Features:
 * - Displays target practice time (from user's quest config)
 * - Timer with start/pause/stop functionality
 * - Progress bar showing completion percentage
 * - Automatically marks task as complete when target time is reached
 * - Instructions for Tajweed practice
 */
public class TajweedTimerActivity extends AppCompatActivity {
    
    private static final String TAG = "TajweedTimerActivity";
    private static final String EXTRA_TARGET_MINUTES = "target_minutes";
    
    // Views
    private TextView tvGoalTime;
    private TextView tvTimer;
    private TextView tvTimerStatus;
    private ProgressBar progressTimer;
    private Button btnStartPause;
    private Button btnStop;
    
    // Timer state
    private int targetMinutes = 15; // Default
    private long targetMillis;
    private long elapsedMillis = 0;
    private boolean isRunning = false;
    private long startTimeMillis;
    private Handler timerHandler;
    private Runnable timerRunnable;
    
    // Quest Repository
    private QuestRepository questRepository;
    private boolean taskMarkedComplete = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tajweed_timer);
        
        // Initialize Quest Repository
        questRepository = new QuestRepository(FirebaseFirestore.getInstance());
        
        // Get target time from intent
        if (getIntent().hasExtra(EXTRA_TARGET_MINUTES)) {
            targetMinutes = getIntent().getIntExtra(EXTRA_TARGET_MINUTES, 15);
        }
        targetMillis = targetMinutes * 60 * 1000L;
        
        // Initialize views
        initViews();
        
        // Set up timer
        setupTimer();
        
        Log.d(TAG, "TajweedTimerActivity created with target: " + targetMinutes + " minutes");
    }
    
    private void initViews() {
        findViewById(R.id.btn_back).setOnClickListener(v -> onBackPressed());
        
        tvGoalTime = findViewById(R.id.tv_goal_time);
        tvTimer = findViewById(R.id.tv_timer);
        tvTimerStatus = findViewById(R.id.tv_timer_status);
        progressTimer = findViewById(R.id.progress_timer);
        btnStartPause = findViewById(R.id.btn_start_pause);
        btnStop = findViewById(R.id.btn_stop);
        
        // Set goal time
        tvGoalTime.setText(targetMinutes + " minutes");
        
        // Set up button listeners
        btnStartPause.setOnClickListener(v -> toggleTimer());
        btnStop.setOnClickListener(v -> stopTimer());
    }
    
    private void setupTimer() {
        timerHandler = new Handler();
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    long currentMillis = System.currentTimeMillis();
                    elapsedMillis += (currentMillis - startTimeMillis);
                    startTimeMillis = currentMillis;
                    
                    updateTimerDisplay();
                    
                    // Check if target reached
                    if (elapsedMillis >= targetMillis) {
                        onTargetReached();
                    } else {
                        timerHandler.postDelayed(this, 1000); // Update every second
                    }
                }
            }
        };
    }
    
    private void toggleTimer() {
        if (isRunning) {
            pauseTimer();
        } else {
            startTimer();
        }
    }
    
    private void startTimer() {
        isRunning = true;
        startTimeMillis = System.currentTimeMillis();
        
        btnStartPause.setText("Pause");
        btnStop.setEnabled(true);
        tvTimerStatus.setText("Practice in progress...");
        
        timerHandler.post(timerRunnable);
        
        Log.d(TAG, "Timer started");
    }
    
    private void pauseTimer() {
        isRunning = false;
        
        btnStartPause.setText("Resume");
        tvTimerStatus.setText("Paused");
        
        timerHandler.removeCallbacks(timerRunnable);
        
        Log.d(TAG, "Timer paused");
    }
    
    private void stopTimer() {
        isRunning = false;
        elapsedMillis = 0;
        
        btnStartPause.setText("Start Practice");
        btnStop.setEnabled(false);
        tvTimerStatus.setText("Ready to begin");
        
        updateTimerDisplay();
        timerHandler.removeCallbacks(timerRunnable);
        
        Log.d(TAG, "Timer stopped");
    }
    
    private void updateTimerDisplay() {
        int totalSeconds = (int) (elapsedMillis / 1000);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        
        tvTimer.setText(String.format(Locale.US, "%02d:%02d", minutes, seconds));
        
        int progress = (int) ((elapsedMillis * 100) / targetMillis);
        progressTimer.setProgress(Math.min(progress, 100));
    }
    
    private void onTargetReached() {
        isRunning = false;
        timerHandler.removeCallbacks(timerRunnable);
        
        // Update UI
        tvTimer.setText(String.format(Locale.US, "%02d:00", targetMinutes));
        progressTimer.setProgress(100);
        tvTimerStatus.setText("✅ Goal completed!");
        btnStartPause.setEnabled(false);
        btnStop.setEnabled(false);
        
        // Mark task as complete in Firebase
        markTaskComplete();
        
        // Show completion message
        Toast.makeText(this, "🎉 Tajweed Practice completed! Well done!", Toast.LENGTH_LONG).show();
        
        // Auto-close after 3 seconds
        new Handler().postDelayed(() -> {
            finish();
        }, 3000);
        
        Log.d(TAG, "Target time reached - Task completed");
    }
    
    private void markTaskComplete() {
        if (taskMarkedComplete) {
            return; // Already marked
        }
        
        // Check if user is logged in
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Log.w(TAG, "User not logged in - cannot mark task complete");
            return;
        }
        
        taskMarkedComplete = true;
        
        // Update task completion in Firebase (Task 2: Tajweed Practice)
        new Thread(() -> {
            try {
                questRepository.updateTaskCompletion("task_2_tajweed", true);
                Log.d(TAG, "Task 2 (Tajweed) marked as complete in Firebase");
            } catch (Exception e) {
                Log.e(TAG, "Failed to mark task as complete", e);
            }
        }).start();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        if (isRunning) {
            pauseTimer();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timerHandler != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
    }
}









