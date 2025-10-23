package com.quran.quranaudio.online.quests.ui;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavAction;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.quran.quranaudio.online.R;
import com.quran.quranaudio.online.quests.data.DailyProgressModel;
import com.quran.quranaudio.online.quests.data.StreakStats;
import com.quran.quranaudio.online.quests.data.UserQuestConfig;
import com.quran.quranaudio.online.quests.repository.QuestRepository;
import com.quran.quranaudio.online.quests.viewmodel.HomeQuestsViewModel;
import com.quran.quranaudio.online.quran_module.utils.reader.factory.ReaderFactory;

import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.Job;

/**
 * Manager class for Daily Quests feature integration in Home screen (FragMain).
 * 
 * Responsibilities:
 * - Check if user has created a learning plan
 * - Show create plan card OR streak + quests cards
 * - Observe Firebase data and update UI
 * - Handle navigation and click events
 */
public class DailyQuestsManager {
    
    private static final String TAG = "DailyQuestsManager";
    
    private final Fragment fragment;
    private final View rootView;
    private final LifecycleOwner lifecycleOwner;
    private final QuestRepository questRepository;
    
    // View containers
    private View createCardContainer;
    private View questsCardsContainer;
    private CardView streakCard;
    private View todayQuestsCard;
    
    // Create Card Views
    private CardView dailyQuestsCreateCard;
    
    // Streak Card Views
    private TextView tvStreakDays;
    private TextView tvMonthlyProgress;
    private ProgressBar pbMonthlyProgress;
    private ImageView ivStreakSettings;
    
    // Today's Quests Views
    private CardView questTask1Card;
    private CardView questTask2Card;
    private CardView questTask3Card;
    
    private TextView tvTask1Title;
    private TextView tvTask1Description;
    private Button btnTask1Go;
    private ImageView ivTask1Completed;
    
    private TextView tvTask2Title;
    private TextView tvTask2Description;
    private Button btnTask2Go;
    private ImageView ivTask2Completed;
    
    private TextView tvTask3Title;
    private TextView tvTask3Description;
    private Button btnTask3Go;
    private ImageView ivTask3Completed;
    
    private HomeQuestsViewModel viewModel;
    
    public DailyQuestsManager(Fragment fragment, View rootView, QuestRepository questRepository) {
        this.fragment = fragment;
        this.rootView = rootView;
        this.lifecycleOwner = fragment.getViewLifecycleOwner();
        this.questRepository = questRepository;
    }
    
    /**
     * Initializes the Daily Quests feature.
     * Finds views and sets up observers.
     * 
     * Note: Create Card is shown to ALL users (logged in or not).
     * Login is only required when saving the configuration.
     */
    public void initialize() {
        try {
            // Find view containers first
            findViewContainers();
            
            // Check if user is logged in
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            String userId = (currentUser != null) ? currentUser.getUid() : null;
            
            // Create ViewModel
            viewModel = new ViewModelProvider(fragment).get(HomeQuestsViewModel.class);
            
            if (userId != null) {
                // User is logged in - initialize repository and observe data
                Log.d(TAG, "User logged in - initializing quest data");
                viewModel.initializeRepository(questRepository);
                
                // Observe quest configuration (will show Create Card or Quests Cards)
                observeQuestConfig();
                
                // Trigger cross-day streak check
                viewModel.checkAndResetStreak();
            } else {
                // User not logged in - show Create Card only
                Log.d(TAG, "User not logged in - showing Create Card for exploration");
                showCreateCard();
                hideQuestsCards();
            }
            
            Log.d(TAG, "Daily Quests initialized successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Daily Quests", e);
        }
    }
    
    /**
     * Finds view containers in the root view
     */
    private void findViewContainers() {
        createCardContainer = rootView.findViewById(R.id.daily_quests_create_card);
        questsCardsContainer = rootView.findViewById(R.id.daily_quests_cards_container);
        
        if (createCardContainer == null) {
            Log.w(TAG, "Create card container not found - feature may not be visible");
        }
        
        if (questsCardsContainer == null) {
            Log.w(TAG, "Quests cards container not found - feature may not be visible");
        }
    }
    
    /**
     * Observes quest configuration from Firebase
     */
    private void observeQuestConfig() {
        viewModel.getQuestConfig().observe(lifecycleOwner, config -> {
            if (config == null) {
                // No learning plan created - show create card
                showCreateCard();
                hideQuestsCards();
                Log.d(TAG, "No learning plan found - showing create card");
            } else {
                // Learning plan exists - show quests cards
                Log.d(TAG, "=== Quest Config Received from Firestore ===");
                Log.d(TAG, "Reading Pages: " + config.getDailyReadingPages());
                Log.d(TAG, "Recitation Minutes: " + config.getRecitationMinutes());
                Log.d(TAG, "Tasbih Enabled: " + config.getTasbihReminderEnabled());
                Log.d(TAG, "Tasbih Count: " + config.getTasbihCount());
                Log.d(TAG, "Challenge Days: " + config.getTotalChallengeDays());
                Log.d(TAG, "==========================================");
                
                hideCreateCard();
                showQuestsCards(config);
                Log.d(TAG, "Learning plan found - showing quests cards");
                
                // Observe streak stats and today's progress
                observeStreakStats();
                observeTodayProgress(config);
            }
        });
    }
    
    /**
     * Shows the create plan card
     */
    private void showCreateCard() {
        if (createCardContainer != null) {
            createCardContainer.setVisibility(View.VISIBLE);
            
            // Initialize the card view and button
            dailyQuestsCreateCard = createCardContainer.findViewById(R.id.daily_quests_create_card_root);
            View btnCreatePlan = createCardContainer.findViewById(R.id.btn_create_plan);
            
            setupCreateCardClickListener();
            
            // Also set click listener on the button
            if (btnCreatePlan != null) {
                btnCreatePlan.setOnClickListener(v -> {
                    navigateToLearningPlanSetup("Button");
                });
            }
        }
    }
    
    /**
     * Hides the create plan card
     */
    private void hideCreateCard() {
        if (createCardContainer != null) {
            createCardContainer.setVisibility(View.GONE);
        }
    }
    
    /**
     * Shows the quests cards (streak + today's quests)
     */
    private void showQuestsCards(UserQuestConfig config) {
        if (questsCardsContainer != null) {
            questsCardsContainer.setVisibility(View.VISIBLE);
            findQuestsCardViews();
            setupQuestsCardClickListeners(config);
        }
    }
    
    /**
     * Hides the quests cards
     */
    private void hideQuestsCards() {
        if (questsCardsContainer != null) {
            questsCardsContainer.setVisibility(View.GONE);
        }
    }
    
    /**
     * Hides all quest views (for non-logged-in users)
     */
    private void hideAllQuestViews() {
        hideCreateCard();
        hideQuestsCards();
    }
    
    /**
     * Finds views in streak card and today's quests card
     */
    private void findQuestsCardViews() {
        if (questsCardsContainer == null) {
            Log.e(TAG, "questsCardsContainer is NULL!");
            return;
        }
        
        Log.d(TAG, "Finding quests card views...");
        
        // Streak Card Views - Use include tag's ID, not root element ID
        streakCard = questsCardsContainer.findViewById(R.id.streak_card);
        if (streakCard != null) {
            Log.d(TAG, "streak_card found successfully");
            tvStreakDays = streakCard.findViewById(R.id.tv_streak_days);
            tvMonthlyProgress = streakCard.findViewById(R.id.tv_monthly_progress);
            pbMonthlyProgress = streakCard.findViewById(R.id.pb_monthly_progress);
            ivStreakSettings = streakCard.findViewById(R.id.iv_streak_settings);
            
            if (ivStreakSettings == null) {
                Log.e(TAG, "Failed to find ivStreakSettings!");
            } else {
                Log.d(TAG, "ivStreakSettings found successfully");
            }
        } else {
            Log.e(TAG, "Failed to find streak_card!");
        }
        
        // Today's Quests Card Views - Use include tag's ID, not root element ID
        todayQuestsCard = questsCardsContainer.findViewById(R.id.today_quests_card);
        if (todayQuestsCard != null) {
            Log.d(TAG, "today_quests_card found successfully");
            
            // Task 1
            questTask1Card = todayQuestsCard.findViewById(R.id.quest_task_1_card);
            tvTask1Title = todayQuestsCard.findViewById(R.id.tv_task_1_title);
            tvTask1Description = todayQuestsCard.findViewById(R.id.tv_task_1_description);
            btnTask1Go = todayQuestsCard.findViewById(R.id.btn_task_1_go);
            ivTask1Completed = todayQuestsCard.findViewById(R.id.iv_task_1_completed);
            
            if (btnTask1Go == null) {
                Log.e(TAG, "Failed to find btnTask1Go!");
            } else {
                Log.d(TAG, "btnTask1Go found successfully");
            }
            
            // Task 2
            questTask2Card = todayQuestsCard.findViewById(R.id.quest_task_2_card);
            tvTask2Title = todayQuestsCard.findViewById(R.id.tv_task_2_title);
            tvTask2Description = todayQuestsCard.findViewById(R.id.tv_task_2_description);
            btnTask2Go = todayQuestsCard.findViewById(R.id.btn_task_2_go);
            ivTask2Completed = todayQuestsCard.findViewById(R.id.iv_task_2_completed);
            
            if (btnTask2Go == null) {
                Log.e(TAG, "Failed to find btnTask2Go!");
            } else {
                Log.d(TAG, "btnTask2Go found successfully");
            }
            
            // Task 3
            questTask3Card = todayQuestsCard.findViewById(R.id.quest_task_3_card);
            tvTask3Title = todayQuestsCard.findViewById(R.id.tv_task_3_title);
            tvTask3Description = todayQuestsCard.findViewById(R.id.tv_task_3_description);
            btnTask3Go = todayQuestsCard.findViewById(R.id.btn_task_3_go);
            ivTask3Completed = todayQuestsCard.findViewById(R.id.iv_task_3_completed);
            
            if (btnTask3Go == null) {
                Log.e(TAG, "Failed to find btnTask3Go!");
            } else {
                Log.d(TAG, "btnTask3Go found successfully");
            }
        } else {
            Log.e(TAG, "Failed to find today_quests_card!");
        }
        
        Log.d(TAG, "Quests card views found and initialized");
    }
    
    /**
     * Sets up click listener for create card
     */
    private void setupCreateCardClickListener() {
        if (dailyQuestsCreateCard != null) {
            dailyQuestsCreateCard.setOnClickListener(v -> {
                navigateToLearningPlanSetup("Card");
            });
        } else {
            Log.w(TAG, "dailyQuestsCreateCard is null, cannot set click listener");
        }
    }
    
    /**
     * Safely navigates to Learning Plan Setup with comprehensive error handling
     * @param source The source of the navigation (for logging)
     */
    private void navigateToLearningPlanSetup(String source) {
        try {
            Log.d(TAG, "📱 " + source + " clicked - attempting navigation");
            
            // Check if fragment is still attached and active
            if (!fragment.isAdded() || fragment.getContext() == null) {
                Log.w(TAG, "⚠️ Fragment is detached, cannot navigate");
                return;
            }
            
            // Check if fragment is currently visible
            if (!fragment.isVisible()) {
                Log.w(TAG, "⚠️ Fragment is not visible, skipping navigation");
                return;
            }
            
            // Get NavController safely using NavHostFragment
            NavController navController = null;
            try {
                navController = NavHostFragment.findNavController(fragment);
            } catch (IllegalStateException e) {
                Log.e(TAG, "❌ Fragment is not attached to NavHostFragment", e);
                showToast("Navigation error. Please try again.");
                return;
            }
            
            if (navController == null) {
                Log.e(TAG, "❌ NavController is null");
                showToast("Navigation error. Please try again.");
                return;
            }
            
            // Check current destination
            NavDestination currentDestination = navController.getCurrentDestination();
            if (currentDestination == null) {
                Log.e(TAG, "❌ Current destination is null");
                return;
            }
            
            // Check if action exists
            NavAction action = currentDestination.getAction(R.id.action_nav_home_to_learning_plan_setup);
            if (action == null) {
                Log.e(TAG, "❌ Navigation action not found in nav graph");
                Log.e(TAG, "Current destination ID: " + currentDestination.getId());
                Log.e(TAG, "Current destination label: " + currentDestination.getLabel());
                showToast("Navigation setup error. Please restart the app.");
                return;
            }
            
            // Perform navigation
            navController.navigate(R.id.action_nav_home_to_learning_plan_setup);
            Log.d(TAG, "✅ Successfully navigated to Learning Plan Setup from " + source);
            
        } catch (IllegalStateException e) {
            Log.e(TAG, "❌ Navigation failed - IllegalStateException", e);
            showToast("Navigation error. Please try again.");
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "❌ Navigation failed - Invalid destination", e);
            showToast("Navigation error. Please check your setup.");
        } catch (Exception e) {
            Log.e(TAG, "❌ Unexpected error during navigation", e);
            showToast("An error occurred. Please try again.");
        }
    }
    
    /**
     * Safely shows a toast message
     * @param message The message to show
     */
    private void showToast(String message) {
        try {
            if (fragment.isAdded() && fragment.getContext() != null) {
                android.widget.Toast.makeText(
                    fragment.requireContext(),
                    message,
                    android.widget.Toast.LENGTH_SHORT
                ).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to show toast", e);
        }
    }
    
    /**
     * Sets up click listeners for quests cards
     */
    private void setupQuestsCardClickListeners(UserQuestConfig config) {
        Log.d(TAG, "Setting up quests card click listeners");
        
        // Settings icon
        if (ivStreakSettings != null) {
            Log.d(TAG, "Setting up click listener for Streak Settings icon");
            ivStreakSettings.setOnClickListener(v -> {
                try {
                    Log.d(TAG, "Streak Settings icon clicked!");
                    // Navigate to Learning Plan Setup (edit mode)
                    NavController navController = Navigation.findNavController(v);
                    navController.navigate(R.id.action_nav_home_to_learning_plan_setup);
                    Log.d(TAG, "Navigating to Learning Plan Setup (edit)");
                } catch (Exception e) {
                    Log.e(TAG, "Failed to navigate from Settings icon", e);
                    Toast.makeText(fragment.requireContext(), "Failed to open settings: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Log.e(TAG, "ivStreakSettings is NULL! Cannot set click listener");
        }
        
        // Task 1: Quran Reading - click to jump to Quran Reader
        if (btnTask1Go != null) {
            Log.d(TAG, "Setting up click listener for Task 1 Go button");
            btnTask1Go.setOnClickListener(v -> {
                try {
                    Log.d(TAG, "Task 1 Go button clicked!");
                    Context context = fragment.requireContext();
                    ReaderFactory.startEmptyReader(context);
                    Log.d(TAG, "Launching Quran Reader for Task 1");
                } catch (Exception e) {
                    Log.e(TAG, "Failed to launch Quran Reader", e);
                    Toast.makeText(fragment.requireContext(), "Failed to open Quran Reader: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Log.e(TAG, "btnTask1Go is NULL! Cannot set click listener");
        }
        
        // Task 2: Quran Listening - click to jump to Quran Reader with auto-play
        if (btnTask2Go != null) {
            Log.d(TAG, "Setting up click listener for Task 2 Go button");
            btnTask2Go.setOnClickListener(v -> {
                try {
                    Log.d(TAG, "Task 2 (Quran Listening) Go button clicked!");
                    Context context = fragment.requireContext();
                    
                    // 从 Firestore 获取上次阅读位置
                    fetchUserLearningStateAndStartReader(context, config);
                    
                } catch (Exception e) {
                    Log.e(TAG, "Failed to launch Quran Reader for listening", e);
                    Toast.makeText(fragment.requireContext(), "Failed to start Quran listening: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Log.e(TAG, "btnTask2Go is NULL! Cannot set click listener");
        }
        
        // Task 3: Tasbih (Dhikr) - click to jump to Tasbih page
        if (btnTask3Go != null) {
            Log.d(TAG, "Setting up click listener for Task 3 Go button");
            btnTask3Go.setOnClickListener(v -> {
                try {
                    Log.d(TAG, "Task 3 Go button clicked!");
                    NavController navController = Navigation.findNavController(v);
                    navController.navigate(R.id.nav_tasbih);
                    Log.d(TAG, "Navigating to Tasbih page for Task 3");
                } catch (Exception e) {
                    Log.e(TAG, "Failed to navigate to Tasbih", e);
                    Toast.makeText(fragment.requireContext(), "Failed to open Tasbih: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Log.e(TAG, "btnTask3Go is NULL! Cannot set click listener");
        }
        
        Log.d(TAG, "Quests card click listeners setup completed");
    }
    
    /**
     * Observes streak statistics
     */
    private void observeStreakStats() {
        viewModel.getStreakStats().observe(lifecycleOwner, stats -> {
            if (stats != null) {
                updateStreakCard(stats);
            }
        });
    }
    
    /**
     * Observes today's progress
     */
    private void observeTodayProgress(UserQuestConfig config) {
        viewModel.getTodayProgress().observe(lifecycleOwner, progress -> {
            if (progress != null) {
                updateTodayQuestsCard(config, progress);
            } else {
                // No progress yet today - show all tasks as incomplete
                updateTodayQuestsCard(config, new DailyProgressModel());
            }
        });
    }
    
    /**
     * Updates streak card UI
     */
    private void updateStreakCard(StreakStats stats) {
        if (tvStreakDays != null) {
            String daysText = fragment.getString(R.string.x_days, stats.getCurrentStreak());
            tvStreakDays.setText(daysText);
        }
        
        if (tvMonthlyProgress != null) {
            String progressText = fragment.getString(R.string.progress_format, 
                stats.getMonthlyProgress(), stats.getMonthlyGoal());
            tvMonthlyProgress.setText(progressText);
        }
        
        if (pbMonthlyProgress != null) {
            int progress = (int) ((stats.getMonthlyProgress() / (float) stats.getMonthlyGoal()) * 100);
            pbMonthlyProgress.setProgress(progress);
        }
        
        Log.d(TAG, "Streak card updated: " + stats.getCurrentStreak() + " days, " + 
                   stats.getMonthlyProgress() + "/" + stats.getMonthlyGoal());
    }
    
    /**
     * Updates today's quests card UI
     */
    private void updateTodayQuestsCard(UserQuestConfig config, DailyProgressModel progress) {
        Log.d(TAG, "Updating today's quests with config: Unit=" + config.getReadingGoalUnit() + 
                   ", Goal=" + config.getDailyReadingGoal() + 
                   ", Recitation=" + config.getRecitationEnabled() + ":" + config.getRecitationMinutes() + "min" +
                   ", Tasbih=" + config.getTasbihReminderEnabled());
        
        // Task 1: Quran Reading - 根据readingGoalUnit动态显示单位
        if (tvTask1Description != null) {
            String unitName = getReadingUnitDisplayName(config.getReadingGoalUnit());
            String task1Desc = fragment.getString(R.string.read_x_verses, config.getDailyReadingGoal());
            // 如果unitName不是"verses"，则需要替换
            if (!config.getReadingGoalUnit().equals("verses")) {
                task1Desc = fragment.getString(R.string.practice_x_minutes, config.getDailyReadingGoal()) + " " + unitName;
            }
            tvTask1Description.setText(task1Desc);
            Log.d(TAG, "Task 1 description updated: " + task1Desc);
        }
        updateTaskCompletionStatus(
            btnTask1Go, 
            ivTask1Completed, 
            progress.getTask1ReadCompleted()
        );
        
        // Task 2: Quran Listening/Recitation - 根据recitationEnabled决定是否显示
        if (questTask2Card != null) {
            if (config.getRecitationEnabled() && config.getRecitationMinutes() > 0) {
                questTask2Card.setVisibility(View.VISIBLE);
                if (tvTask2Description != null) {
                    String task2Desc = fragment.getString(R.string.listen_x_minutes, config.getRecitationMinutes());
                    tvTask2Description.setText(task2Desc);
                    Log.d(TAG, "Task 2 description updated: " + task2Desc);
                }
                updateTaskCompletionStatus(
                    btnTask2Go, 
                    ivTask2Completed, 
                    progress.getTask2TajweedCompleted()
                );
            } else {
                questTask2Card.setVisibility(View.GONE);
                Log.d(TAG, "Task 2 (Recitation) hidden - disabled or 0 minutes");
            }
        }
        
        // Task 3: Dhikr (show only if enabled)
        if (questTask3Card != null) {
            if (config.getTasbihReminderEnabled()) {
                questTask3Card.setVisibility(View.VISIBLE);
                Log.d(TAG, "Task 3 (Tasbih) visible - count: " + config.getTasbihCount());
                updateTaskCompletionStatus(
                    btnTask3Go, 
                    ivTask3Completed, 
                    progress.getTask3TasbihCompleted()
                );
            } else {
                questTask3Card.setVisibility(View.GONE);
                Log.d(TAG, "Task 3 (Tasbih) hidden");
            }
        }
        
        Log.d(TAG, "Today's quests completion status - Task1: " + progress.getTask1ReadCompleted() + 
                   ", Task2: " + progress.getTask2TajweedCompleted() + 
                   ", Task3: " + progress.getTask3TasbihCompleted());
    }
    
    /**
     * 获取阅读单位的显示名称（小写复数形式）
     */
    private String getReadingUnitDisplayName(String unitString) {
        if (unitString == null || unitString.isEmpty()) {
            return "pages"; // 默认
        }
        
        switch (unitString.toUpperCase()) {
            case "PAGES":
                return "pages";
            case "VERSES":
                return "verses";
            case "JUZ":
                return "Juz'";
            default:
                return "pages";
        }
    }
    
    /**
     * Updates task completion status (Go button vs Completed icon)
     */
    private void updateTaskCompletionStatus(Button goButton, ImageView completedIcon, boolean isCompleted) {
        if (goButton != null && completedIcon != null) {
            if (isCompleted) {
                goButton.setVisibility(View.GONE);
                completedIcon.setVisibility(View.VISIBLE);
            } else {
                goButton.setVisibility(View.VISIBLE);
                completedIcon.setVisibility(View.GONE);
            }
        }
    }
    
    /**
     * 从 Firestore 获取用户学习状态并启动 Quran Reader
     */
    private void fetchUserLearningStateAndStartReader(Context context, UserQuestConfig config) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "User not logged in, using default position (Surah 1, Ayah 1)");
            startQuranReaderWithAudio(context, 1, 1, config);
            return;
        }
        
        String userId = currentUser.getUid();
        Log.d(TAG, "Fetching UserLearningState from Firestore for user: " + userId);
        
        // 从 Firestore 异步获取学习状态
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .collection("learningState")
            .document("current")
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    // 解析学习状态
                    Integer lastReadSurah = documentSnapshot.getLong("lastReadSurah") != null 
                        ? documentSnapshot.getLong("lastReadSurah").intValue() : 1;
                    Integer lastReadAyah = documentSnapshot.getLong("lastReadAyah") != null 
                        ? documentSnapshot.getLong("lastReadAyah").intValue() : 1;
                    
                    Log.d(TAG, "UserLearningState found: Surah " + lastReadSurah + ", Ayah " + lastReadAyah);
                    startQuranReaderWithAudio(context, lastReadSurah, lastReadAyah, config);
                } else {
                    Log.d(TAG, "UserLearningState not found, using default position (Surah 1, Ayah 1)");
                    startQuranReaderWithAudio(context, 1, 1, config);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to fetch UserLearningState", e);
                Toast.makeText(context, "Failed to load reading position, starting from Surah 1", Toast.LENGTH_SHORT).show();
                startQuranReaderWithAudio(context, 1, 1, config);
            });
    }
    
    /**
     * 启动 Quran Reader 并传递音频自动播放参数
     */
    private void startQuranReaderWithAudio(Context context, int surah, int ayah, UserQuestConfig config) {
        try {
            Log.d(TAG, "Starting Quran Reader for listening: Surah " + surah + ", Ayah " + ayah);
            
            // 使用 ReaderFactory 启动 Reader 并定位到指定位置
            Intent intent = ReaderFactory.prepareSingleVerseIntent(surah, ayah);
            intent.setClass(context, com.quran.quranaudio.online.quran_module.activities.ActivityReader.class);
            
            // 传递自动播放参数
            intent.putExtra("AUTO_PLAY_AUDIO", true);
            intent.putExtra("LISTENING_MODE", true);
            intent.putExtra("TARGET_MINUTES", config.getRecitationMinutes());
            
            context.startActivity(intent);
            Log.d(TAG, "Quran Reader started successfully for Quran Listening task");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to start Quran Reader", e);
            Toast.makeText(context, "Failed to start Quran Reader: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Cleans up resources
     */
    public void onDestroy() {
        // Remove observers if needed
        Log.d(TAG, "Daily Quests manager destroyed");
    }
}


