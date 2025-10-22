package com.quran.quranaudio.online.tasbih.fragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.drawable.AnimationDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.quran.quranaudio.online.fragments.BaseFragment;
import com.quran.quranaudio.online.quests.constants.FirestoreConstants;
import com.quran.quranaudio.online.quests.data.UserQuestConfig;
import com.quran.quranaudio.online.quests.repository.QuestRepository;
import com.quran.quranaudio.online.tasbih.helper.TasbihManager;
import com.quran.quranaudio.online.R;
import com.quran.quranaudio.online.tasbih.utils.Utils;


public class TasbihFragment extends BaseFragment {
    private static final String TAG = "TasbihFragment";
    private static final int DEFAULT_TASBIH_TARGET = 50; // Default if config not available
    private static final long MIN_CLICK_INTERVAL_MS = 300; // 防作弊：最小点击间隔 300ms
    
    private int dailyQuestTarget = DEFAULT_TASBIH_TARGET; // 动态从 Firestore 读取
    private long lastClickTime = 0; // 防作弊：记录上次点击时间
    
    private ImageView btn33;
    private ImageView btnRefresh;
    private ImageView btnSpeak;
    private ImageView btnPrevious;
    private boolean is33;
    private int speakStatus;
    private ImageView tasbihView;
    private int total;
    private TextView tv33;
    private TextView tvCount;
    private TextView tvTotal;
    
    // Daily Quest integration
    private QuestRepository questRepository;
    private boolean dailyQuestCompleted = false;

    public int getLayoutId() {
        return R.layout.fragment_tasbih;
    }

    public static TasbihFragment newInstance() {
        Bundle bundle = new Bundle();
        TasbihFragment tasbihFragment = new TasbihFragment();
        tasbihFragment.setArguments(bundle);
        return tasbihFragment;
    }

    public void onViewCreated(@NonNull View view, @Nullable Bundle bundle) {
        super.onViewCreated(view, bundle);
        initView();
        initToolbar();
        initTasbih();
        initDailyQuest();
    }
    
    /**
     * Initialize Daily Quest integration
     */
    private void initDailyQuest() {
        // Initialize Quest Repository
        questRepository = new QuestRepository(FirebaseFirestore.getInstance());
        
        // 从 Firestore 读取用户配置
        fetchUserQuestConfig();
        
        // Check current daily count
        int dailyCount = TasbihManager.get().getDailyCount();
        if (dailyCount >= dailyQuestTarget) {
            dailyQuestCompleted = true;
        }
        
        Log.d(TAG, "Daily Quest initialized - Current count: " + dailyCount + "/" + dailyQuestTarget);
    }
    
    /**
     * 从 Firestore 读取用户配置（tasbihCount 和 tasbihReminderEnabled）
     */
    private void fetchUserQuestConfig() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Log.w(TAG, "User not logged in, using default Tasbih target: " + DEFAULT_TASBIH_TARGET);
            return;
        }
        
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String configPath = "users/" + userId + "/learningPlan/config";
        
        FirebaseFirestore.getInstance().document(configPath)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    try {
                        UserQuestConfig config = documentSnapshot.toObject(UserQuestConfig.class);
                        if (config != null) {
                            // 检查是否启用 Tasbih Reminder
                            if (config.getTasbihReminderEnabled()) {
                                dailyQuestTarget = config.getTasbihCount();
                                Log.d(TAG, "Tasbih config loaded - Target: " + dailyQuestTarget + " (enabled)");
                            } else {
                                Log.d(TAG, "Tasbih Reminder is disabled in user config");
                                // 可选：显示提示
                                if (getActivity() != null) {
                                    Toast.makeText(getActivity(), 
                                        "Tasbih task is not enabled in your learning plan", 
                                        Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to parse UserQuestConfig", e);
                        dailyQuestTarget = DEFAULT_TASBIH_TARGET;
                    }
                } else {
                    Log.w(TAG, "UserQuestConfig not found, using default: " + DEFAULT_TASBIH_TARGET);
                    dailyQuestTarget = DEFAULT_TASBIH_TARGET;
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to fetch UserQuestConfig", e);
                dailyQuestTarget = DEFAULT_TASBIH_TARGET;
            });
    }

    private void initTasbih() {
        this.tasbihView.setOnClickListener(new View.OnClickListener() {
            public final void onClick(View view) {
                TasbihFragment.this.tasbihClick();
            }
        });
    }

    
    public void tasbihClick() {
        // 防作弊检查：限制点击间隔
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastClickTime < MIN_CLICK_INTERVAL_MS) {
            Log.w(TAG, "Click too fast (" + (currentTime - lastClickTime) + "ms), ignored (anti-cheat)");
            return; // 忽略过快的点击
        }
        lastClickTime = currentTime;
        
        // 正常计数逻辑
        AnimationDrawable animationDrawable = (AnimationDrawable) getResources().getDrawable(R.drawable.tasbih_animation);
        this.tasbihView.setImageDrawable(animationDrawable);
        animationDrawable.start();
        this.total++;
        TasbihManager.get().putTotal(this.total);
        
        // Daily Quest: Increment daily count
        int dailyCount = TasbihManager.get().incrementDailyCount();
        Log.d(TAG, "Tasbih clicked - Daily count: " + dailyCount + "/" + dailyQuestTarget);
        
        // Check if daily quest completed
        if (!dailyQuestCompleted && dailyCount >= dailyQuestTarget) {
            dailyQuestCompleted = true;
            onDailyQuestCompleted();
        }
        
        updateText(true);
    }
    
    /**
     * Called when user completes the daily Tasbih quest
     */
    private void onDailyQuestCompleted() {
        Log.d(TAG, "Daily Tasbih Quest completed!");
        
        int dailyCount = TasbihManager.get().getDailyCount();
        
        // Show celebration toast
        Toast.makeText(this.activity, 
            "🎉 Daily Tasbih Quest completed! (" + dailyCount + "/" + dailyQuestTarget + ")", 
            Toast.LENGTH_LONG).show();
        
        // Mark task as complete in Firebase (if user is logged in)
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            // 使用 AsyncTask 或 Handler 来异步处理（避免阻塞 UI 线程）
            new Thread(() -> {
                try {
                    // 注意：task ID 必须与 FirestoreConstants 一致
                    questRepository.updateTaskCompletion(
                        FirestoreConstants.TaskIds.TASK_3_TASBIH, 
                        true
                    );
                    Log.d(TAG, "Task 3 (Tasbih) marked as complete in Firebase");
                    
                    // 可选：在主线程显示同步成功提示
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getActivity(), 
                                "Progress synced! ✓", 
                                Toast.LENGTH_SHORT).show();
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to mark Tasbih task as complete", e);
                    
                    // 在主线程显示错误提示
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getActivity(), 
                                "Sync failed, will retry later", 
                                Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            }).start();
        } else {
            Log.w(TAG, "User not logged in, task completion not synced");
        }
    }

    @SuppressLint({"SetTextI18n"})
    private void updateText(boolean z) {
        int i;
        this.tv33.setText(this.is33 ? "33" : "99");
        TextView textView = this.tvTotal;
        textView.setText(this.total + "");
        if (this.is33) {
            int i2 = this.total;
            i = i2 % 33;
            if (i == 0 && i2 > 0) {
                if (z) {
                    Utils.vibrator();
                }
                i = 33;
            }
        } else {
            int i3 = this.total;
            i = i3 % 99;
            if (i == 0 && i3 > 0) {
                if (z) {
                    Utils.vibrator();
                }
                i = 99;
            }
        }
        TextView textView2 = this.tvCount;
        textView2.setText(i + "");
        if (z) {
            int i4 = this.speakStatus;
            if (i4 == 0) {
                playSound();
            } else if (i4 == 1) {
                Utils.vibrator();
            }
        }
    }

    private void playSound() {
        MediaPlayer create = MediaPlayer.create(this.activity, (int) R.raw.tasbih_sound);
        if (create != null) {
            create.start();
            create.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mp.release();
                }
            });

        }
    }

    private void initView() {
        this.btnPrevious = (ImageView) this.view.findViewById(R.id.bt_previous);
        this.btnSpeak = (ImageView) this.view.findViewById(R.id.bt_speak);
        this.btn33 = (ImageView) this.view.findViewById(R.id.bt_tasbih_count);
        this.btnRefresh = (ImageView) this.view.findViewById(R.id.bt_refresh);
        this.tasbihView = (ImageView) this.view.findViewById(R.id.tasbih_view);
        this.tvCount = (TextView) this.view.findViewById(R.id.tv_tasbih_count);
        this.tv33 = (TextView) this.view.findViewById(R.id.tv_tasbih_33);
        this.tvTotal = (TextView) this.view.findViewById(R.id.tv_tasbih_total);
    }

    private void initToolbar() {
        // Setup back button to navigate to home
        if (this.btnPrevious != null) {
            this.btnPrevious.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        Log.d(TAG, "Back button clicked - navigating to home");
                        androidx.navigation.Navigation.findNavController(v).popBackStack();
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to navigate back", e);
                        // Fallback: try to finish activity
                        if (getActivity() != null) {
                            getActivity().onBackPressed();
                        }
                    }
                }
            });
        }
        
        this.total = TasbihManager.get().getTotal();
        this.speakStatus = TasbihManager.get().getSpeak();
        checkSpeak();
        this.btnSpeak.setOnClickListener(new View.OnClickListener() {
            public final void onClick(View view) {
                TasbihFragment.lambda$initToolbar$1(TasbihFragment.this, view);
            }
        });
        this.is33 = TasbihManager.get().is33();
        check33();
        this.btn33.setOnClickListener(new View.OnClickListener() {
            public final void onClick(View view) {
                TasbihFragment.lambda$initToolbar$2(TasbihFragment.this, view);
            }
        });
        this.btnRefresh.setOnClickListener(new View.OnClickListener() {
            public final void onClick(View view) {
                TasbihFragment.lambda$initToolbar$4(TasbihFragment.this, view);
            }
        });
        updateText(false);
    }

    public static /* synthetic */ void lambda$initToolbar$1(TasbihFragment tasbihFragment, View view) {
        int i = tasbihFragment.speakStatus;
        if (i == 0) {
            tasbihFragment.speakStatus = 1;
            TasbihManager.get().putSpeak(1);
            Utils.vibrator();
        } else if (i == 1) {
            tasbihFragment.speakStatus = 2;
            TasbihManager.get().putSpeak(2);
        } else {
            tasbihFragment.speakStatus = 0;
            TasbihManager.get().putSpeak(0);
        }
        tasbihFragment.checkSpeak();
    }

    public static /* synthetic */ void lambda$initToolbar$2(TasbihFragment tasbihFragment, View view) {
        if (tasbihFragment.is33) {
            tasbihFragment.is33 = false;
            TasbihManager.get().put33(false);
        } else {
            tasbihFragment.is33 = true;
            TasbihManager.get().put33(true);
        }
        tasbihFragment.check33();
        tasbihFragment.updateText(false);
    }

    public static /* synthetic */ void lambda$initToolbar$4(final TasbihFragment tasbihFragment, View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(tasbihFragment.activity);
        builder.setMessage("Reset your current and total tasbih counts to zezo?");
        builder.setPositiveButton("RESET", new DialogInterface.OnClickListener() {
            public final void onClick(DialogInterface dialogInterface, int i) {
                tasbihFragment.total = 0;
                tasbihFragment.is33 = true;
                tasbihFragment.speakStatus = 0;
                TasbihManager.get().put33(true);
                TasbihManager.get().putSpeak(0);
                TasbihManager.get().putTotal(0);
                tasbihFragment.check33();
                tasbihFragment.checkSpeak();
                tasbihFragment.updateText(false);
            }
        });
        builder.setNegativeButton("CANCEL", (DialogInterface.OnClickListener) null);
        builder.show();
    }



    private void check33() {
        if (this.is33) {
            this.btn33.setImageResource(R.drawable.ic_33);
        } else {
            this.btn33.setImageResource(R.drawable.ic_99);
        }
    }

    private void checkSpeak() {
        int i = this.speakStatus;
        if (i == 0) {
            this.btnSpeak.setImageResource(R.drawable.ic_volume);
        } else if (i == 1) {
            this.btnSpeak.setImageResource(R.drawable.ic_vibration);
        } else {
            this.btnSpeak.setImageResource(R.drawable.ic_volume_off);
        }
    }
}
