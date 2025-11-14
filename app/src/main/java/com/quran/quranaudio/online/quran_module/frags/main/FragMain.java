package com.quran.quranaudio.online.quran_module.frags.main;

import static com.quran.quranaudio.online.prayertimes.notifier.PrayerAlarmScheduler.TAG;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.asynclayoutinflater.view.AsyncLayoutInflater;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.widget.ImageView;
import android.os.Build;
import androidx.annotation.RequiresApi;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import com.bumptech.glide.Glide;
import javax.inject.Inject;
import com.google.firebase.auth.FirebaseUser;
import com.quran.quranaudio.online.Utils.GoogleAuthManager;
import com.quran.quranaudio.online.prayertimes.common.PrayerEnum;
import com.quran.quranaudio.online.prayertimes.timings.DayPrayer;
import com.quran.quranaudio.online.prayertimes.ui.home.HomeViewModel;
import com.quran.quranaudio.online.prayertimes.utils.PrayerUtils;
import com.quran.quranaudio.online.prayertimes.utils.TimingUtils;
import com.quran.quranaudio.online.prayertimes.utils.UiUtils;
import com.quran.quranaudio.online.prayertimes.utils.AlertHelper;
import com.quran.quranaudio.online.quests.ui.DailyQuestsManager;
import com.quran.quranaudio.online.quests.repository.QuestRepository;
import com.google.firebase.firestore.FirebaseFirestore;
import com.quran.quranaudio.online.home.quiz.QuizRepository;
import com.quran.quranaudio.online.home.quiz.QuizQuestion;
import com.quran.quranaudio.quiz.QuestionBean;
import com.quran.quranaudio.quiz.base.Constants;
import com.quranaudio.quiz.quiz.QuranQuizNotifyResultActivity;
import org.apache.commons.lang3.StringUtils;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.TreeMap;

// Ad imports removed
// import com.raiadnan.ads.sdk.format.BannerAd;
// import com.raiadnan.ads.sdk.format.InterstitialAd;
import com.quran.quranaudio.online.activities.LiveActivity;
import com.quran.quranaudio.online.activities.SixKalmasActivity;
import com.quran.quranaudio.online.activities.ZakatCalculatorActivity;
// import com.quran.quranaudio.online.ads.data.Constant; // Ad constants import removed
import com.quran.quranaudio.online.compass.QiblaDirectionActivity;
import com.quran.quranaudio.online.hadith.HadithActivity;
import com.quran.quranaudio.online.quran_module.activities.ActivityReaderIndexPage;
import com.quran.quranaudio.online.quran_module.components.quran.QuranMeta;
import com.quran.quranaudio.online.quran_module.utils.app.UpdateManager;
import com.quran.quranaudio.online.quran_module.views.VOTDView;
import com.quran.quranaudio.online.R;
import com.quran.quranaudio.online.App;
import com.quran.quranaudio.online.activities.LiveActivity;
import com.quran.quranaudio.online.databinding.FragMainBinding;
import com.quran.quranaudio.online.prayertimes.ui.calendar.CalendarActivity;
import com.quran.quranaudio.online.quran_module.activities.ActivityReaderIndexPage;
import com.quran.quranaudio.online.quran_module.frags.BaseFragment;
import com.quran.quranaudio.online.quran_module.utils.app.UpdateManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FragMain extends BaseFragment {
    private FragMainBinding mBinding;
    private AsyncLayoutInflater mAsyncInflater;
    private VOTDView mVotdView;
    private UpdateManager mUpdateManager;

    // Ad-related variables removed

    // Google Sign-In
    private GoogleAuthManager googleAuthManager;
    private ActivityResultLauncher<Intent> googleSignInLauncher;
    
    // Header Views
    private TextView tvGreeting;
    private TextView tvUserName;
    private ImageView btnSearch;
    private CardView cardAvatar;
    private ImageView imgAvatarDefault;
    private ImageView imgAvatarUser;

    // Prayer Card Views
    private TextView tvNextPrayerName;
    private TextView tvNextPrayerTime;
    private TextView tvLocationPrayer;
    private TextView tvTimeRemaining;
    
    // Prayer Times ViewModel
    private HomeViewModel homeViewModel;
    
    // Countdown Timer
    private Handler countdownHandler;
    private Runnable countdownRunnable;
    private LocalDateTime nextPrayerDateTime;
    
    // Verse of The Day Card Views
    private TextView tvVotdContentText;
    private TextView tvVotdReference;
    private ImageView ivVotdShare;
    private ImageView ivVotdBookmark;
    private VOTDView votdViewEmbedded;  // Embedded VOTDView for data and formatting
    private int votdChapterNo = -1;
    private int votdVerseNo = -1;
    
    // Daily Quests Manager
    private DailyQuestsManager dailyQuestsManager;
    private QuestRepository questRepository;
    
    // Quiz Module Views
    private View quizEntryView;
    private TextView quizQuestionTextView;
    private List<com.google.android.material.button.MaterialButton> quizOptionButtons;
    private QuizRepository quizRepository;
    private QuizQuestion currentQuizQuestion;
    private static final String[] QUIZ_OPTION_KEYS = {"A", "B", "C", "D"};
    
    // Dagger injected ViewModelFactory for creating HomeViewModel with dependencies
    @Inject
    ViewModelProvider.Factory viewModelFactory;

    ActivityResultLauncher<String[]> mPermissionResultLauncher;
    ActivityResultLauncher<Intent> mSettingsLauncher; // For jumping to Settings
    private boolean isLocationPermissionGranted = false;
    private boolean userHasRespondedToLocationPermission = false; // Track if user has made a choice
    
    // CRITICAL FLAG: Prevent Dialog from being re-displayed after permission grant
    // This flag is set to true once permission is granted (either via system dialog or Settings)
    // and prevents any code path (onViewCreated, onResume, error observers) from showing the dialog again
    private boolean hasPermissionBeenGrantedThisSession = false;
    
    // 🔥 权限请求计数器：最多弹2次
    private static final String PREFS_NAME = "LocationPermissionPrefs";
    private static final String KEY_PERMISSION_REQUEST_COUNT = "permission_request_count";
    private static final int MAX_PERMISSION_REQUESTS = 2;
    
    // CRITICAL FLAG: Track if user is currently in Settings page
    // Prevents onResume() from interfering while user is granting permission in Settings
    private boolean isUserInSettingsForPermission = false;


    public FragMain() {
    }

    @Override
    public void onAttach(@NonNull Context context) {
        // CRITICAL: Inject Dagger dependencies (ViewModelFactory) before calling super
        // This ensures HomeViewModel gets proper dependencies (LocationHelper, AddressHelper, etc.)
        ((App) requireContext().getApplicationContext())
                .appComponent
                .homeComponent()
                .create()
                .inject(this);  // Now FragMain is supported in HomeComponent

        super.onAttach(context);
        Log.d(TAG, "Dagger injection completed in FragMain.onAttach()");
    }

    @Override
    public boolean networkReceiverRegistrable() {
        return true;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // CRITICAL: Register permission launcher in onCreate() (not in onViewCreated())
        // ActivityResultLauncher must be registered before the Fragment is CREATED
        mPermissionResultLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), 
            new ActivityResultCallback<Map<String, Boolean>>() {
                @Override
                public void onActivityResult(Map<String, Boolean> result) {
                    Log.d(TAG, "========================================");
                    Log.d(TAG, "📋 Permission request result received");
                    Log.d(TAG, "========================================");
                    Log.d(TAG, "Result map: " + result);
                    
                    if (result.get(Manifest.permission.ACCESS_FINE_LOCATION) != null) {
                        isLocationPermissionGranted = result.get(Manifest.permission.ACCESS_FINE_LOCATION);
                        userHasRespondedToLocationPermission = true; // User has made a choice
                        
                        Log.d(TAG, "📍 Location permission granted: " + isLocationPermissionGranted);
                        
                        if (isLocationPermissionGranted) {
                            Log.d(TAG, "✅ Permission GRANTED by user!");
                            
                            // CRITICAL: Set flag to prevent dialog from being re-shown
                            hasPermissionBeenGrantedThisSession = true;
                            Log.d(TAG, "🔒 hasPermissionBeenGrantedThisSession = true");
                            
                            // Permission granted - close warning dialog and refresh prayer card
                            Log.d(TAG, "🚪 Attempting to close permission dialog...");
                            Log.d(TAG, "   - dialogWarning is null: " + (dialogWarning == null));
                            
                            if (dialogWarning != null) {
                                Log.d(TAG, "   - dialogWarning.isShowing(): " + dialogWarning.isShowing());
                                
                                if (dialogWarning.isShowing()) {
                                    Log.d(TAG, "   - 🔄 Dismissing dialog...");
                                    dialogWarning.dismiss();
                                    Log.d(TAG, "   - ✅ Dialog dismissed successfully!");
                                }
                            }
                            
                            // Refresh prayer card location and times
                            if (isAdded() && getActivity() != null) {
                                Log.d(TAG, "🔄 Calling refreshPrayerCardData()...");
                                refreshPrayerCardData();
                            } else {
                                Log.w(TAG, "⚠️ Cannot refresh - Fragment not added or Activity is null");
                            }
                        } else {
                            Log.w(TAG, "❌ Location permission DENIED by user");
                            
                            // Check if user selected "Don't ask again" (permanent denial)
                            Activity activity = getActivity();
                            if (activity != null) {
                                boolean shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                                    activity, Manifest.permission.ACCESS_FINE_LOCATION);
                                
                                if (!shouldShowRationale) {
                                    // User checked "Don't ask again" - guide them to Settings
                                    Log.w(TAG, "⚠️ Permission PERMANENTLY DENIED (Don't ask again)");
                                    Log.d(TAG, "   Showing guidance to go to Settings...");
                                    
                                    // Keep Welcome dialog visible but update message
                                    if (isAdded() && getContext() != null) {
                                        Toast.makeText(getContext(), 
                                            "Please enable location permission in Settings: App Info > Permissions > Location",
                                            Toast.LENGTH_LONG).show();
                                        
                                        // Option: Auto-jump to Settings after showing Toast
                                        new android.os.Handler().postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (isAdded() && getContext() != null) {
                                                    Log.d(TAG, "🚀 Auto-jumping to Settings after permanent denial");
                                                    jumpToAppSettings();
                                                }
                                            }
                                        }, 1500); // 1.5 second delay to let user read the Toast
                                    }
                                } else {
                                    // User denied but can be asked again
                                    Log.d(TAG, "📱 User denied, but can be asked again (no 'Don't ask again')");
                                }
                            }
                        }
                    } else {
                        Log.w(TAG, "⚠️ ACCESS_FINE_LOCATION not found in result map");
                    }
                    Log.d(TAG, "========================================");
                }
            }
        );
        
        // Register Settings launcher for handling return from app settings
        mSettingsLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    Log.d(TAG, "========================================");
                    Log.d(TAG, "🔙 Returned from Settings");
                    Log.d(TAG, "========================================");
                    
                    // CRITICAL: Reset Settings flag immediately
                    isUserInSettingsForPermission = false;
                    Log.d(TAG, "🔓 isUserInSettingsForPermission = false");
                    
                    // Check if permission was granted in Settings
                    if (!isAdded() || getContext() == null) {
                        Log.w(TAG, "❌ Fragment not attached, skipping permission check");
                        return;
                    }
                    Log.d(TAG, "✅ Fragment is attached and has context");
                    
                    boolean isNowGranted = checkLocationPermission();
                    Log.d(TAG, "📍 Permission status after Settings: " + isNowGranted);
                    
                    if (isNowGranted) {
                        // Permission granted in Settings
                        Log.d(TAG, "✅ Location permission GRANTED in Settings!");
                        
                        // Flags are already set in jumpToAppSettings(), just confirm
                        Log.d(TAG, "🔒 Confirming flags:");
                        Log.d(TAG, "   - hasPermissionBeenGrantedThisSession: " + hasPermissionBeenGrantedThisSession);
                        Log.d(TAG, "   - userHasRespondedToLocationPermission: " + userHasRespondedToLocationPermission);
                        
                        // Dialog should already be dismissed in jumpToAppSettings()
                        // But double-check for safety
                        Log.d(TAG, "🚪 Dialog check:");
                        Log.d(TAG, "   - dialogWarning is null: " + (dialogWarning == null));
                        
                        if (dialogWarning != null && dialogWarning.isShowing()) {
                            Log.w(TAG, "   - ⚠️ Dialog still showing (shouldn't happen), dismissing...");
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    try {
                                        if (dialogWarning != null && dialogWarning.isShowing()) {
                                            dialogWarning.dismiss();
                                            dialogWarning = null;
                                            Log.d(TAG, "   - ✅ Dialog dismissed");
                                        }
                                    } catch (Exception e) {
                                        Log.e(TAG, "   - ❌ Exception dismissing dialog", e);
                                    }
                                });
                            }
                        } else {
                            Log.d(TAG, "   - ✅ Dialog already dismissed (as expected)");
                        }
                        
                        // Refresh prayer card data
                        Log.d(TAG, "🔄 Calling refreshPrayerCardData()...");
                        refreshPrayerCardData();
                    } else {
                        Log.w(TAG, "❌ Permission STILL NOT GRANTED after Settings");
                        // User didn't grant permission, reset flags
                        hasPermissionBeenGrantedThisSession = false;
                        Log.d(TAG, "🔓 hasPermissionBeenGrantedThisSession reset to false");
                    }
                    Log.d(TAG, "========================================");
                }
            }
        );
        
        Log.d(TAG, "Permission launcher and Settings launcher registered in onCreate()");
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG, "========================================");
        Log.d(TAG, "📱 onResume() called");
        Log.d(TAG, "========================================");
        
        // CRITICAL: If user is in Settings page, skip ALL logic and wait for callback
        if (isUserInSettingsForPermission) {
            Log.d(TAG, "🔄 User is currently in Settings page");
            Log.d(TAG, "   Skipping onResume logic, waiting for mSettingsLauncher callback");
            Log.d(TAG, "========================================");
            return;  // Exit early - mSettingsLauncher callback will handle everything
        }
        
        // Log current state for debugging
        boolean permissionGranted = checkLocationPermission();
        Log.d(TAG, "Current state:");
        Log.d(TAG, "  - checkLocationPermission(): " + permissionGranted);
        Log.d(TAG, "  - isLocationPermissionGranted: " + isLocationPermissionGranted);
        Log.d(TAG, "  - userHasRespondedToLocationPermission: " + userHasRespondedToLocationPermission);
        Log.d(TAG, "  - hasPermissionBeenGrantedThisSession: " + hasPermissionBeenGrantedThisSession);
        Log.d(TAG, "  - isUserInSettingsForPermission: " + isUserInSettingsForPermission);
        Log.d(TAG, "  - dialogWarning: " + (dialogWarning == null ? "null" : "exists"));
        if (dialogWarning != null) {
            Log.d(TAG, "  - dialogWarning.isShowing(): " + dialogWarning.isShowing());
        }
        
        // CRITICAL: Check if dialog would be re-shown (for debugging)
        if (!permissionGranted && !hasPermissionBeenGrantedThisSession) {
            Log.w(TAG, "⚠️ WARNING: Conditions exist to show permission dialog in onResume!");
            Log.w(TAG, "           But we're NOT showing it because onViewCreated handles this.");
            // DO NOT call showPermissionWarning() here - let onViewCreated handle it
        } else if (hasPermissionBeenGrantedThisSession) {
            Log.d(TAG, "✅ Permission granted this session - dialog will not be shown");
        }
        
        Log.d(TAG, "========================================");
        
        // Note: Permission status is now handled by ActivityResult callbacks
        // No need to manually check permission here - the callbacks will handle it

        Context context = getContext();
        if (context == null) {
            return;
        }

        QuranMeta.prepareInstance(context, quranMeta -> {
            // Removed: VOTD View refresh - Using new Daily Quests Card instead
            // if (mVotdView != null) {
            //     mVotdView.post(() -> mVotdView.refresh(quranMeta));
            // }

        });
    }

    @Override
    public void onDestroy() {
        // Removed: VOTD View cleanup - Using new Daily Quests Card instead
        // if (mVotdView != null) {
        //     mVotdView.destroy();
        // }


        super.onDestroy();
    }

    public static FragMain newInstance() {
        return new FragMain();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mAsyncInflater = new AsyncLayoutInflater(inflater.getContext());

        mBinding = FragMainBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }



    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (mBinding == null) {
            mBinding = FragMainBinding.bind(view);
        }

        // Initialize Google Sign-In
        initializeGoogleSignIn();
        
        // Initialize Header Views
        initializeHeaderViews();
        
        // Initialize Prayer Card Views
        initializePrayerCardViews();
        
        // Initialize Prayer Card Quick Navigation Buttons
        initializeQuickNavigationButtons();
        
        // Initialize Verse of The Day Card
        initializeVerseOfDayCard();
        
        // Initialize Quiz Module
        // Quiz module temporarily disabled
        // initializeQuizModule();
        
        // Initialize Mecca Live Card
        initializeMeccaLiveCard();
        
        // Initialize Medina Live Card
        initializeMedinaLiveCard();
        
        // Initialize Daily Quests Feature
        initializeDailyQuests();

        //PermissionStart
        // Permission launcher is now registered in onCreate() (see above)
        // ⭐ 优化：新用户首次进入主页时弹出权限请求，最多弹2次
        if (checkLocationPermission()) {
            // User has already granted permission
            userHasRespondedToLocationPermission = true;
            hasPermissionBeenGrantedThisSession = true;
            Log.d(TAG, "✅ onViewCreated: Location permission already granted");
        } else if (!userHasRespondedToLocationPermission && !hasPermissionBeenGrantedThisSession) {
            // 检查权限请求次数
            int requestCount = getPermissionRequestCount();
            if (requestCount < MAX_PERMISSION_REQUESTS) {
                // 未超过最大次数，弹出权限请求
                Log.d(TAG, "🚪 onViewCreated: Showing permission dialog (count: " + (requestCount + 1) + "/" + MAX_PERMISSION_REQUESTS + ")");
                showPermissionWarningAndIncrementCount();
            } else {
                // 已达到最大次数，不再弹出
                Log.d(TAG, "⚠️ onViewCreated: Max permission requests reached (" + requestCount + "), skipping dialog");
                userHasRespondedToLocationPermission = true;
            }
        } else {
            // 用户已经拒绝过，不再弹出
            Log.d(TAG, "ℹ️ onViewCreated: User already responded, skipping dialog");
        }
        //Permission End


        mUpdateManager = new UpdateManager(view.getContext(), mBinding.appUpdateContainer);
        // If update is not critical, proceed to load the rest of the content
        if (!mUpdateManager.check4Update()) {
            QuranMeta.prepareInstance(view.getContext(), quranMeta -> initContent(view, quranMeta));
        }

        mBinding.readQuran.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), ActivityReaderIndexPage.class));
                // Ad calls removed
            }
        });
        mBinding.hadithBooks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), HadithActivity.class));
                // Ad calls removed
            }
        });
        mBinding.qiblaDirection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // ⭐ 优化：点击Qibla功能时才检查位置权限
                if (checkLocationPermission()) {
                    // 有权限，直接打开Qibla页面
                    Log.d(TAG, "✅ Location permission granted, launching Qibla Direction");
                    startActivity(new Intent(getActivity(), QiblaDirectionActivity.class));
                } else {
                    // 没有权限，检查是否还能弹出权限请求
                    int requestCount = getPermissionRequestCount();
                    if (requestCount < MAX_PERMISSION_REQUESTS) {
                        Log.d(TAG, "⚠️ No location permission, showing permission dialog for Qibla feature (count: " + (requestCount + 1) + "/" + MAX_PERMISSION_REQUESTS + ")");
                        android.widget.Toast.makeText(getActivity(), 
                            "Location permission is required to use Qibla Direction", 
                            android.widget.Toast.LENGTH_SHORT).show();
                        showPermissionWarningAndIncrementCount();
                    } else {
                        Log.d(TAG, "⚠️ Max permission requests reached, cannot show dialog");
                        android.widget.Toast.makeText(getActivity(), 
                            "Please enable location permission in Settings to use Qibla Direction", 
                            android.widget.Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
        mBinding.prayerCalender.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), CalendarActivity.class));
                // Ad calls removed
            }
        });
        mBinding.sixKalmas.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), SixKalmasActivity.class));
                // Ad calls removed
            }
        });
        mBinding.zakatCalculator.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), ZakatCalculatorActivity.class));
                // Ad calls removed
            }
        });
        mBinding.meccaLive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Backup live stream URL list
                String[] meccaLiveUrls = {
                    "http://m.live.net.sa:1935/live/quran/playlist.m3u8", // Original URL
                    "https://ythls.armelin.one/channel/UCos52-JmjOoBnBOnxJCWAQA.m3u8", // Mecca Live YouTube to HLS
                    "https://www.youtube.com/watch?v=e85tJVzKwDU", // YouTube backup 1
                    "https://www.youtube.com/watch?v=yd19lGSibQ4"  // YouTube backup 2
                };
                
                String selectedUrl = meccaLiveUrls[0]; // Use first URL by default
                Log.d("FragMain", "Mecca Live URL: " + selectedUrl);
                Log.d("FragMain", "Available backup URLs: " + java.util.Arrays.toString(meccaLiveUrls));
                
                Intent intent = new Intent(getActivity(), LiveActivity.class);
                intent.putExtra("live", selectedUrl);
                intent.putExtra("backup_urls", meccaLiveUrls);
                startActivity(intent);
            }
        });
        mBinding.medinaLive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Backup live stream URL list (HLS streaming priority for in-app playback)
                String[] medinaLiveUrls = {
                    "http://m.live.net.sa:1935/live/sunnah/playlist.m3u8", // Original HLS URL (priority)
                    "https://ythls.armelin.one/channel/UCJr4gikBowJ8I-iUXs7CkMg.m3u8", // YouTube to HLS
                    "https://www.youtube.com/watch?v=4s4XX-qaNgg", // YouTube live backup 1
                    "https://www.youtube.com/watch?v=0lg0XeJ2gAU", // YouTube live backup 2
                    "https://www.youtube.com/watch?v=4Ar8JHRCdSE" // YouTube live backup 3
                };
                
                String selectedUrl = medinaLiveUrls[0]; // Use first URL by default (HLS streaming)
                Log.d("FragMain", "Medina Live URL: " + selectedUrl);
                Log.d("FragMain", "Available backup URLs: " + java.util.Arrays.toString(medinaLiveUrls));
                
                Intent intent = new Intent(getActivity(), LiveActivity.class);
                intent.putExtra("live", selectedUrl);
                intent.putExtra("backup_urls", medinaLiveUrls);
                startActivity(intent);
            }
        });

        // Ad code removed


    }

    // All ad methods removed

    View.OnClickListener dialogListener=new View.OnClickListener() {
        @Override public void onClick(View v) {
            // Check if Fragment is still attached before processing click
            if (!isAdded() || getContext() == null) {
                Log.w(TAG, "Fragment not attached, ignoring dialog button click");
                return;
            }
            
            if(v.getId()==R.id.btn_skip){
                userHasRespondedToLocationPermission = true; // User chose to skip
                if (dialogWarning != null && dialogWarning.isShowing()) {
                    dialogWarning.dismiss();
                }
            } else if(v.getId()==R.id.btn_enable_location){
                // Check if permission is already granted (user may have granted in Settings)
                if (checkLocationPermission()) {
                    Log.d(TAG, "Permission already granted, closing dialog and refreshing");
                    userHasRespondedToLocationPermission = true;
                    hasPermissionBeenGrantedThisSession = true;
                    if (dialogWarning != null && dialogWarning.isShowing()) {
                        dialogWarning.dismiss();
                    }
                    refreshPrayerCardData();
                } else {
                    // TWO-STAGE PERMISSION REQUEST (Best UX):
                    // Stage 1: Always try native Android dialog first (one-tap approval)
                    // Stage 2: If permanently denied, guide user to Settings
                    Log.d(TAG, "========================================");
                    Log.d(TAG, "🚀 User clicked Enable Location");
                    Log.d(TAG, "   📱 Showing native permission dialog");
                    Log.d(TAG, "========================================");
                    
                    // Always try native dialog first - provides best UX
                    // If user denied with "Don't ask again", mPermissionResultLauncher
                    // will handle it and guide user to Settings
                    requestPermission();
                }
            }
        }
    };
    Dialog dialogWarning;
    private boolean checkLocationPermission(){
        // Use getContext() instead of requireContext() to avoid crash when Fragment is detached
        Context context = getContext();
        if (context == null) {
            Log.w(TAG, "Fragment not attached, cannot check permission");
            return false;
        }
        
        isLocationPermissionGranted = ContextCompat.checkSelfPermission(context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;
        return isLocationPermissionGranted;
    }
    /**
     * 获取权限请求次数
     */
    private int getPermissionRequestCount() {
        if (getActivity() == null) return 0;
        android.content.SharedPreferences prefs = getActivity().getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE);
        int count = prefs.getInt(KEY_PERMISSION_REQUEST_COUNT, 0);
        Log.d(TAG, "📊 Current permission request count: " + count);
        return count;
    }
    
    /**
     * 增加权限请求次数
     */
    private void incrementPermissionRequestCount() {
        if (getActivity() == null) return;
        android.content.SharedPreferences prefs = getActivity().getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE);
        int currentCount = prefs.getInt(KEY_PERMISSION_REQUEST_COUNT, 0);
        int newCount = currentCount + 1;
        prefs.edit().putInt(KEY_PERMISSION_REQUEST_COUNT, newCount).apply();
        Log.d(TAG, "📈 Permission request count incremented: " + currentCount + " → " + newCount);
    }
    
    /**
     * 显示权限弹窗并增加计数
     */
    private void showPermissionWarningAndIncrementCount() {
        incrementPermissionRequestCount();
        showPermissionWarning();
    }
    
    private void showPermissionWarning(){
        Log.d(TAG, "========================================");
        Log.d(TAG, "🚪 showPermissionWarning() called");
        Log.d(TAG, "   Stack trace (to identify caller):");
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (int i = 3; i < Math.min(8, stackTrace.length); i++) {
            Log.d(TAG, "     " + stackTrace[i].toString());
        }
        Log.d(TAG, "========================================");
        
        View view=LayoutInflater.from(getActivity()).inflate(R.layout.layout_dialog_location_warning,null);
        dialogWarning=new AlertDialog.Builder(getActivity()).setView(view).create();
        TextView skip=view.findViewById(R.id.btn_skip);
        Button enable=view.findViewById(R.id.btn_enable_location);
        skip.setOnClickListener(dialogListener);
        enable.setOnClickListener(dialogListener);
        dialogWarning.setCanceledOnTouchOutside(false);
        
        // Set transparent background for modern card design
        if (dialogWarning.getWindow() != null) {
            dialogWarning.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        dialogWarning.show();
        
        Log.d(TAG, "✅ Permission warning dialog displayed");
    }
    /**
     * Set random background image for Verse of the Day card with blur effect
     * 随机选择背景图并应用高斯模糊，低透明度作为氛围衬托，保持绿色主色调
     */
    private void setVotdRandomBackground(ImageView backgroundImageView) {
        if (backgroundImageView == null || getContext() == null) {
            return;
        }
        
        try {
            // Array of background images
            int[] backgroundImages = {
                R.drawable.img_prayer,
                R.drawable.prayer_bg2,
                R.drawable.today_header,
                R.drawable.tasbih_bg
            };
            
            // Select random image
            int randomIndex = new java.util.Random().nextInt(backgroundImages.length);
            int selectedImage = backgroundImages[randomIndex];
            
            // Load image without blur, 30% opacity for subtle atmosphere
            // 无模糊 + 30%透明度，清晰的背景氛围效果
            com.bumptech.glide.Glide.with(this)
                .load(selectedImage)
                .transform(new com.bumptech.glide.load.resource.bitmap.CenterCrop())
                .into(backgroundImageView);
            
            // Set 30% alpha for subtle background atmosphere
            backgroundImageView.setAlpha(0.3f);
            
            Log.d(TAG, "✅ VOTD background set without blur at 30% opacity: " + selectedImage);
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error setting VOTD background", e);
            // Fallback: keep transparent
            backgroundImageView.setAlpha(0f);
        }
    }
    
    private void requestPermission(){
        List<String> permissionRequest = new ArrayList<String>();

        if (!isLocationPermissionGranted) {
            permissionRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (!permissionRequest.isEmpty()) {
            try {
                mPermissionResultLauncher.launch(permissionRequest.toArray(new String[0]));
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Jump to app settings page using ActivityResult API
     * This ensures we get a callback when user returns from Settings
     * CRITICAL: Sets flags and closes Dialog BEFORE jumping to prevent conflicts
     */
    private void jumpToAppSettings() {
        try {
            Context context = getContext();
            if (context == null) {
                Log.w(TAG, "Fragment not attached, cannot jump to Settings");
                return;
            }
            
            Log.d(TAG, "========================================");
            Log.d(TAG, "🚀 Preparing to jump to Settings");
            Log.d(TAG, "========================================");
            
            // CRITICAL: Set all flags BEFORE jumping to Settings
            isUserInSettingsForPermission = true;
            hasPermissionBeenGrantedThisSession = true;  // Pre-set to prevent dialog re-show
            userHasRespondedToLocationPermission = true;
            
            Log.d(TAG, "🔒 Flags set:");
            Log.d(TAG, "   - isUserInSettingsForPermission = true");
            Log.d(TAG, "   - hasPermissionBeenGrantedThisSession = true");
            Log.d(TAG, "   - userHasRespondedToLocationPermission = true");
            
            // CRITICAL: Close Dialog BEFORE jumping to prevent it from staying visible
            if (dialogWarning != null) {
                if (dialogWarning.isShowing()) {
                    dialogWarning.dismiss();
                    Log.d(TAG, "🚪 Dialog dismissed before jumping to Settings");
                }
                dialogWarning = null;  // Clear reference
                Log.d(TAG, "🚪 Dialog reference cleared");
            } else {
                Log.d(TAG, "🚪 Dialog was already null");
            }
            
            // Jump to App Settings (App Info page)
            // Note: User will need to tap "Permissions" -> "Location" to grant permission
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", context.getPackageName(), null);
            intent.setData(uri);
            
            // Use ActivityResult launcher to handle return from Settings
            mSettingsLauncher.launch(intent);
            Log.d(TAG, "✅ Launched App Settings page successfully");
            Log.d(TAG, "========================================");
        } catch (Exception e) {
            // If jump fails, reset flags
            isUserInSettingsForPermission = false;
            hasPermissionBeenGrantedThisSession = false;
            Log.e(TAG, "❌ Failed to jump to Settings, flags reset", e);
        }
    }

    private void initContent(View root, QuranMeta quranMeta) {
        // Removed: initVOTD(root, quranMeta); - Using new Daily Quests Card instead
        //        initReadHistory(root, quranMeta);
    }

    private void initVOTD(View root, QuranMeta quranMeta) {
        mVotdView = new VOTDView(root.getContext());
        mVotdView.setId(R.id.homepageVOTD);
        mBinding.container.addView(mVotdView, resolvePosReadHistory(root));
        mVotdView.refresh(quranMeta);
    }


    private void initFeaturedReading(View root, QuranMeta quranMeta) {

    }



    public static int resolvePosUpdateCont() {
        return 0;
    }

    private int resolvePosVOTD(View root) {
        int pos = resolvePosUpdateCont() + 1;
        if (root.findViewById(R.id.appUpdateContainer) == null) {
            pos--;
        }
        return pos;
    }

    private int resolvePosReadHistory(View root) {
        int pos = resolvePosVOTD(root) + 1;
        if (root.findViewById(R.id.homepageVOTD) == null) {
            pos--;
        }
        return pos;
    }

    // ===== STEP 5: Google Sign-In Integration =====

    /**
     * Initialize Google Sign-In Manager and Launcher
     * Optimized for low-end devices with proper threading and error handling
     */
    private void initializeGoogleSignIn() {
        try {
            // Initialize Google Auth Manager
            googleAuthManager = new GoogleAuthManager(requireContext());
            Log.d(TAG, "GoogleAuthManager initialized successfully");
            
            // Register Google Sign-In Launcher
            googleSignInLauncher = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        Log.d(TAG, "Google Sign-In result received - ResultCode: " + result.getResultCode());
                        
                        // Check result code
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            Intent data = result.getData();
                            if (data != null) {
                                Log.d(TAG, "Processing Google Sign-In data...");
                                
                                // Handle sign-in result with callback
                                googleAuthManager.handleSignInResult(data, new GoogleAuthManager.AuthCallback() {
                                    @Override
                                    public void onSuccess(FirebaseUser user) {
                                        Log.d(TAG, "Google Sign-In SUCCESS!");
                                        Log.d(TAG, "User Display Name: " + (user != null ? user.getDisplayName() : "null"));
                                        Log.d(TAG, "User Email: " + (user != null ? user.getEmail() : "null"));
                                        Log.d(TAG, "User Photo URL: " + (user != null ? user.getPhotoUrl() : "null"));
                                        
                                        // CRITICAL: Update UI on main thread
                                        if (getActivity() != null) {
                                            getActivity().runOnUiThread(() -> {
                                                try {
                                                    updateHeaderUI();
                                                    Log.d(TAG, "Header UI updated successfully");
                                                    
                                                    // 🔥 关键修复：登录成功后立即刷新今日任务状态
                                                    if (dailyQuestsManager != null) {
                                                        dailyQuestsManager.initialize();
                                                        Log.d(TAG, "✅ Daily Quests refreshed after login");
                                                    }
                                                    
                                                    // Show success toast
                                                    if (getContext() != null) {
                                                        android.widget.Toast.makeText(getContext(), 
                                                            "Welcome, " + (user != null ? user.getDisplayName() : "User") + "!", 
                                                            android.widget.Toast.LENGTH_SHORT).show();
                                                    }
                                                } catch (Exception e) {
                                                    Log.e(TAG, "Error updating UI after login", e);
                                                }
                                            });
                                        }
                                    }

                                    @Override
                                    public void onFailure(String error) {
                                        Log.e(TAG, "Google Sign-In FAILURE: " + error);
                                        
                                        // Show error on main thread
                                        if (getActivity() != null) {
                                            getActivity().runOnUiThread(() -> {
                                                if (getContext() != null) {
                                                    android.widget.Toast.makeText(getContext(), 
                                                        "Sign-in failed: " + error, 
                                                        android.widget.Toast.LENGTH_LONG).show();
                                                }
                                            });
                                        }
                                    }
                                });
                            } else {
                                Log.e(TAG, "Google Sign-In data is null");
                            }
                        } else if (result.getResultCode() == Activity.RESULT_CANCELED) {
                            Log.d(TAG, "Google Sign-In canceled by user");
                            if (googleAuthManager != null) {
                                googleAuthManager.logSignInDiagnostics(result.getData(), "FragMain-CANCELLED");
                            }
                            if (getContext() != null && getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    android.widget.Toast.makeText(getContext(), 
                                        "Sign-in canceled", 
                                        android.widget.Toast.LENGTH_SHORT).show();
                                });
                            }
                        } else {
                            Log.e(TAG, "Unexpected result code: " + result.getResultCode());
                            if (googleAuthManager != null) {
                                googleAuthManager.logSignInDiagnostics(result.getData(), "FragMain-UNEXPECTED");
                            }
                        }
                    }
            );
            
            Log.d(TAG, "Google Sign-In launcher registered successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Google Sign-In", e);
        }
    }

    /**
     * Initialize Header Views and Click Listeners
     */
    private void initializeHeaderViews() {
        View headerView = mBinding.getRoot().findViewById(R.id.home_header);
        if (headerView != null) {
            tvGreeting = headerView.findViewById(R.id.tv_greeting);
            tvUserName = headerView.findViewById(R.id.tv_user_name);
            btnSearch = headerView.findViewById(R.id.btn_search);
            cardAvatar = headerView.findViewById(R.id.card_avatar);
            imgAvatarDefault = headerView.findViewById(R.id.img_avatar_default);
            imgAvatarUser = headerView.findViewById(R.id.img_avatar_user);

            // Set up Search button click listener
            if (btnSearch != null) {
                btnSearch.setOnClickListener(v -> {
                    // Navigate to search page
                    startActivity(new Intent(getActivity(), 
                        com.quran.quranaudio.online.quran_module.activities.ActivityQuran_Search.class));
                });
            }

            // Set up Avatar click listener for Google Sign-In
            if (cardAvatar != null) {
                cardAvatar.setOnClickListener(v -> {
                    if (googleAuthManager != null) {
                        if (googleAuthManager.isUserSignedIn()) {
                            // User is signed in - show logout dialog
                            showLogoutDialog();
                        } else {
                            // User not signed in - initiate Google Sign-In
                            Intent signInIntent = googleAuthManager.getSignInIntent();
                            googleSignInLauncher.launch(signInIntent);
                        }
                    }
                });
            }

            // Initial UI update
            updateHeaderUI();
        }
    }

    /**
     * Update Header UI based on authentication state
     * Optimized for low-end devices with error handling
     */
    private void updateHeaderUI() {
        Log.d(TAG, "updateHeaderUI() called");
        
        if (googleAuthManager == null) {
            Log.e(TAG, "GoogleAuthManager is null, cannot update UI");
            return;
        }

        try {
            boolean isSignedIn = googleAuthManager.isUserSignedIn();
            Log.d(TAG, "User signed in status: " + isSignedIn);

            if (isSignedIn) {
                // User is signed in
                String userName = googleAuthManager.getUserDisplayName();
                android.net.Uri photoUrl = googleAuthManager.getUserPhotoUrl();
                
                Log.d(TAG, "Updating UI for logged in user:");
                Log.d(TAG, "  - Username: " + userName);
                Log.d(TAG, "  - Photo URL: " + photoUrl);
                Log.d(TAG, "  - tvUserName null: " + (tvUserName == null));
                Log.d(TAG, "  - imgAvatarUser null: " + (imgAvatarUser == null));
                Log.d(TAG, "  - imgAvatarDefault null: " + (imgAvatarDefault == null));

                // Update Username
                if (tvUserName != null) {
                    if (userName != null && !userName.isEmpty()) {
                        tvUserName.setText(userName);
                        tvUserName.setVisibility(View.VISIBLE);
                        Log.d(TAG, "Username updated and visible");
                    } else {
                        tvUserName.setVisibility(View.GONE);
                        Log.d(TAG, "Username is empty, hidden");
                    }
                }

                // Update Avatar
                if (imgAvatarUser != null && imgAvatarDefault != null) {
                    if (photoUrl != null) {
                        Log.d(TAG, "Loading user avatar from URL");
                        
                        // Optimized Glide loading for low-end devices
                        try {
                            Glide.with(this)
                                    .load(photoUrl)
                                    .circleCrop()
                                    .thumbnail(0.1f)  // Load thumbnail first (low-end optimization)
                                    .error(com.quran.quranaudio.online.R.drawable.dr_icon_user)  // Fallback
                                    .timeout(10000)  // 10 second timeout
                                    .into(imgAvatarUser);
                            
                            imgAvatarUser.setVisibility(View.VISIBLE);
                            imgAvatarDefault.setVisibility(View.GONE);
                            Log.d(TAG, "Avatar loaded successfully");
                        } catch (Exception e) {
                            Log.e(TAG, "Error loading avatar with Glide", e);
                            // Fallback to default icon
                            imgAvatarUser.setVisibility(View.GONE);
                            imgAvatarDefault.setVisibility(View.VISIBLE);
                        }
                    } else {
                        Log.d(TAG, "Photo URL is null, showing default avatar");
                        imgAvatarUser.setVisibility(View.GONE);
                        imgAvatarDefault.setVisibility(View.VISIBLE);
                    }
                }
            } else {
                // User is not signed in
                Log.d(TAG, "User not signed in, showing default UI");
                
                if (tvUserName != null) {
                    tvUserName.setVisibility(View.GONE);
                    Log.d(TAG, "Username hidden");
                }

                if (imgAvatarUser != null && imgAvatarDefault != null) {
                    imgAvatarUser.setVisibility(View.GONE);
                    imgAvatarDefault.setVisibility(View.VISIBLE);
                    Log.d(TAG, "Default avatar shown");
                }
            }
            
            // Force view refresh (helps on some low-end devices)
            if (cardAvatar != null) {
                cardAvatar.invalidate();
                cardAvatar.requestLayout();
            }
            
            Log.d(TAG, "updateHeaderUI() completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error in updateHeaderUI()", e);
        }
    }

    /**
     * Show logout confirmation dialog
     */
    private void showLogoutDialog() {
        if (getContext() == null) return;

        new AlertDialog.Builder(getContext())
                .setTitle(R.string.logout)
                .setMessage(R.string.logout_message)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    if (googleAuthManager != null) {
                        googleAuthManager.signOut(() -> {
                            // Update UI after logout
                            updateHeaderUI();
                            if (getContext() != null) {
                                android.widget.Toast.makeText(getContext(), 
                                    getString(R.string.logged_out_successfully), 
                                    android.widget.Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    /**
     * Initialize Prayer Card Views and ViewModel
     * Subscribes to live prayer time data from HomeViewModel
     */
    private void initializePrayerCardViews() {
        Log.d(TAG, "initializePrayerCardViews() called");
        
        try {
            // Find Prayer Card views from included layout
            // NOTE: Use 'prayer_card' ID which is the <include> tag's ID in frag_main.xml
            View prayerCardView = mBinding.getRoot().findViewById(R.id.prayer_card);
            if (prayerCardView != null) {
                tvNextPrayerName = prayerCardView.findViewById(R.id.tv_next_prayer_name);
                tvNextPrayerTime = prayerCardView.findViewById(R.id.tv_next_prayer_time);
                tvLocationPrayer = prayerCardView.findViewById(R.id.tv_location_prayer);
                tvTimeRemaining = prayerCardView.findViewById(R.id.tv_time_remaining);
                
                // Set initial loading state to avoid showing stale data
                if (tvNextPrayerName != null) {
                    tvNextPrayerName.setText("Loading...");
                    tvNextPrayerName.setAlpha(0.6f); // Slightly dimmed for loading effect
                }
                if (tvNextPrayerTime != null) {
                    tvNextPrayerTime.setText("--:--");
                    tvNextPrayerTime.setAlpha(0.6f);
                }
                if (tvLocationPrayer != null) {
                    tvLocationPrayer.setText("Locating...");
                    tvLocationPrayer.setAlpha(0.6f);
                }
                if (tvTimeRemaining != null) {
                    tvTimeRemaining.setText(getString(R.string.calculating));
                    tvTimeRemaining.setAlpha(0.6f);
                }
                
                Log.d(TAG, "Prayer Card views found and initialized with loading state");
            } else {
                Log.e(TAG, "Prayer Card root view not found (looking for R.id.prayer_card)");
                return;
            }
            
            // Initialize HomeViewModel using ViewModelProvider
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                initializePrayerViewModel();
            } else {
                Log.w(TAG, "Prayer times require Android O (API 26) or higher");
                // Set placeholder text for older Android versions
                if (tvNextPrayerName != null) tvNextPrayerName.setText("Prayer Times");
                if (tvNextPrayerTime != null) tvNextPrayerTime.setText("--:--");
                if (tvLocationPrayer != null) tvLocationPrayer.setText("Update Required");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Prayer Card views", e);
        }
    }

    /**
     * Initialize Prayer Times ViewModel and observe data
     * Requires Android O (API 26+) for LocalDateTime support
     * Uses Dagger-injected ViewModelFactory to create HomeViewModel with all dependencies
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void initializePrayerViewModel() {
        try {
            // CRITICAL: Check if viewModelFactory was injected by Dagger
            if (viewModelFactory == null) {
                Log.e(TAG, "ViewModelFactory is null! Dagger injection may have failed.");
                if (tvNextPrayerName != null) tvNextPrayerName.setText("Error");
                if (tvNextPrayerTime != null) tvNextPrayerTime.setText("--:--");
                if (tvLocationPrayer != null) tvLocationPrayer.setText("Injection failed");
                return;
            }

            // Create HomeViewModel at ACTIVITY scope (not Fragment scope)
            // This ensures:
            // 1. ViewModel survives Fragment recreation
            // 2. Data is shared across all fragments in the Activity
            // 3. Data is loaded ONCE and cached, not reloaded every time user returns to Home
            homeViewModel = new ViewModelProvider(requireActivity(), viewModelFactory)
                .get(HomeViewModel.class);
            Log.d(TAG, "HomeViewModel obtained (Activity scope - shared and cached)");

            // Observe prayer times data
            homeViewModel.getDayPrayers().observe(getViewLifecycleOwner(), dayPrayer -> {
                if (dayPrayer != null) {
                    Log.d(TAG, "Prayer data received - City: " + dayPrayer.getCity() + 
                        ", Timings count: " + (dayPrayer.getTimings() != null ? dayPrayer.getTimings().size() : 0));
                    updatePrayerCard(dayPrayer);
                } else {
                    Log.w(TAG, "Received null dayPrayer data");
                }
            });

            // Observe error messages
            homeViewModel.getError().observe(getViewLifecycleOwner(), error -> {
                Log.e(TAG, "Prayer data error: " + error);
                if (error != null) {
                    // Update UI to show error state instead of loading
                    showPrayerCardErrorState(error);
                    
                    // Only show error dialog if:
                    // 1. User has already responded to location permission request (to avoid double dialogs)
                    // 2. Permission is NOT granted
                    // 3. Error is related to location
                    // 4. CRITICAL: Permission has NOT been granted this session (prevents dialog after grant)
                    if (getActivity() != null && error.contains("location") 
                        && !isLocationPermissionGranted 
                        && userHasRespondedToLocationPermission
                        && !hasPermissionBeenGrantedThisSession) {  // NEW: Check session flag
                        Log.d(TAG, "⚠️ Showing location error dialog (all conditions met)");
                        AlertHelper.displayLocationErrorDialog(
                            requireActivity(),
                            getString(R.string.location_alert_title),
                            error
                        );
                    } else if (error != null && error.contains("location")) {
                        Log.w(TAG, "🔇 Suppressing location error dialog - permission granted: " + isLocationPermissionGranted + 
                            ", user responded: " + userHasRespondedToLocationPermission +
                            ", session granted: " + hasPermissionBeenGrantedThisSession);
                    }
                }
            });

            Log.d(TAG, "Prayer ViewModel observers registered successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error initializing Prayer ViewModel", e);
            // Set error state
            if (tvNextPrayerName != null) tvNextPrayerName.setText("Error");
            if (tvNextPrayerTime != null) tvNextPrayerTime.setText("--:--");
            if (tvLocationPrayer != null) tvLocationPrayer.setText("Init failed");
        }
    }

    /**
     * Show error state on Prayer Card when data fetch fails
     * Removes loading state and displays user-friendly error message
     */
    private void showPrayerCardErrorState(String errorMessage) {
        if (tvNextPrayerName == null || tvNextPrayerTime == null || tvLocationPrayer == null) {
            return;
        }

        try {
            // Restore full opacity (remove loading dimming effect)
            tvNextPrayerName.setAlpha(1.0f);
            tvNextPrayerTime.setAlpha(1.0f);
            tvLocationPrayer.setAlpha(1.0f);
            if (tvTimeRemaining != null) tvTimeRemaining.setAlpha(1.0f);

            // Show user-friendly error messages based on error type
            if (errorMessage != null && errorMessage.toLowerCase().contains("location")) {
                // Location permission or service error
                tvNextPrayerName.setText("Prayer Times");
                tvNextPrayerTime.setText("--:--");
                tvLocationPrayer.setText("Location Required");
                // Hide countdown timer in error state to avoid card height increase
                if (tvTimeRemaining != null) {
                    tvTimeRemaining.setVisibility(View.GONE);
                }
            } else {
                // General error
                tvNextPrayerName.setText("Prayer Times");
                tvNextPrayerTime.setText("--:--");
                tvLocationPrayer.setText("Unable to load");
                // Hide countdown timer in error state
                if (tvTimeRemaining != null) {
                    tvTimeRemaining.setVisibility(View.GONE);
                }
            }

            Log.d(TAG, "Prayer Card error state displayed");

        } catch (Exception e) {
            Log.e(TAG, "Error showing Prayer Card error state", e);
        }
    }

    /**
     * Refresh prayer card data after location permission is granted
     * CRITICAL: Force HomeViewModel to fetch current location and update prayer times
     */
    private void refreshPrayerCardData() {
        Log.d(TAG, "🔄 ===== Refreshing prayer card data after permission grant =====");
        
        try {
            // Set loading state to indicate refresh is in progress
            if (tvNextPrayerName != null) {
                tvNextPrayerName.setText("Updating...");
                tvNextPrayerName.setAlpha(0.6f);
                Log.d(TAG, "✅ Prayer name set to 'Updating...'");
            }
            if (tvLocationPrayer != null) {
                tvLocationPrayer.setText("Getting location...");
                tvLocationPrayer.setAlpha(0.6f);
                Log.d(TAG, "✅ Location text set to 'Getting location...'");
            }
            
            // CRITICAL: Force ViewModel to refresh location
            if (homeViewModel != null) {
                Log.d(TAG, "🔄 Calling homeViewModel.forceRefreshLocation()");
                homeViewModel.forceRefreshLocation();
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Log.d(TAG, "⚠️ homeViewModel is null, initializing for the first time");
                initializePrayerViewModel();
            } else {
                Log.e(TAG, "❌ Cannot refresh - homeViewModel is null and Android version < O");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error refreshing prayer card data", e);
        }
    }

    /**
     * Update Prayer Card with real-time prayer data
     * This method reuses the logic from prayertimes.ui.home.HomeFragment
     * 
     * @param dayPrayer The DayPrayer object containing prayer times and location
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void updatePrayerCard(DayPrayer dayPrayer) {
        if (tvNextPrayerName == null || tvNextPrayerTime == null || tvLocationPrayer == null) {
            Log.w(TAG, "Prayer Card views not initialized");
            return;
        }

        try {
            // Get prayer timings
            Map<PrayerEnum, LocalDateTime> timings = dayPrayer.getTimings();
            if (timings == null || timings.isEmpty()) {
                Log.w(TAG, "Prayer timings are empty");
                return;
            }

            // Find next prayer
            PrayerEnum nextPrayerKey = PrayerUtils.getNextPrayer(timings, LocalDateTime.now());

            if (nextPrayerKey != null) {
                // Update prayer name
                String prayerName = requireContext().getResources().getString(
                    getResources().getIdentifier(nextPrayerKey.toString(), "string", 
                        requireContext().getPackageName())
                );
                tvNextPrayerName.setText(prayerName);
                tvNextPrayerName.setAlpha(1.0f); // Restore full opacity
                Log.d(TAG, "Next prayer: " + prayerName);

                // Update prayer time
                nextPrayerDateTime = timings.get(nextPrayerKey);
                if (nextPrayerDateTime != null) {
                    String formattedTime = UiUtils.formatTiming(nextPrayerDateTime);
                    tvNextPrayerTime.setText(formattedTime);
                    tvNextPrayerTime.setAlpha(1.0f); // Restore full opacity
                    Log.d(TAG, "Next prayer time: " + formattedTime);

                    // Start countdown timer for automatic updates
                    startCountdownTimer();
                }
            } else {
                Log.w(TAG, "No next prayer found");
                tvNextPrayerName.setText("Prayer");
                tvNextPrayerTime.setText("--:--");
            }

            // Update location
            String locationText;
            if (dayPrayer.getCity() != null && !dayPrayer.getCity().isEmpty()) {
                locationText = StringUtils.capitalize(dayPrayer.getCity());
            } else {
                locationText = getString(R.string.common_offline);
            }
            tvLocationPrayer.setText(locationText);
            tvLocationPrayer.setAlpha(1.0f); // Restore full opacity
            Log.d(TAG, "Location updated: " + locationText);

        } catch (Exception e) {
            Log.e(TAG, "Error updating Prayer Card", e);
            // Set error state
            tvNextPrayerName.setText("Error");
            tvNextPrayerTime.setText("--:--");
            tvLocationPrayer.setText("Update failed");
        }
    }

    /**
     * Start countdown timer to update remaining time every minute
     * Automatically stops when fragment is destroyed
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startCountdownTimer() {
        // Stop any existing timer first
        stopCountdownTimer();
        
        if (tvTimeRemaining == null || nextPrayerDateTime == null) {
            Log.w(TAG, "Cannot start countdown: tvTimeRemaining=" + (tvTimeRemaining != null) + 
                ", nextPrayerDateTime=" + (nextPrayerDateTime != null));
            return;
        }

        // Initialize Handler if not already created
        if (countdownHandler == null) {
            countdownHandler = new Handler(Looper.getMainLooper());
        }

        Log.d(TAG, "Starting countdown timer for next prayer at " + nextPrayerDateTime);

        // Create runnable that updates countdown and reschedules itself
        countdownRunnable = new Runnable() {
            @Override
            public void run() {
                if (tvTimeRemaining != null && nextPrayerDateTime != null && isAdded()) {
                    try {
                        long timeRemaining = TimingUtils.getTimeBetweenTwoPrayer(
                            LocalDateTime.now(), nextPrayerDateTime
                        );
                        
                        if (timeRemaining > 0) {
                            // Display countdown time with "Remaining:" prefix (localized)
                            String remainingText = getString(R.string.remaining) + ": " + UiUtils.formatTimeForTimer(timeRemaining);
                            tvTimeRemaining.setText(remainingText);
                            tvTimeRemaining.setAlpha(1.0f); // Restore full opacity
                            tvTimeRemaining.setVisibility(View.VISIBLE);
                            
                            // Schedule next update in 1 second (1000 ms) for real-time countdown
                            if (countdownHandler != null) {
                                countdownHandler.postDelayed(this, 1000);
                            }
                        } else {
                            // Prayer time has passed, stop countdown
                            tvTimeRemaining.setText("0m");
                            stopCountdownTimer();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error updating countdown", e);
                    }
                }
            }
        };

        // Start the countdown immediately
        countdownRunnable.run();
    }

    /**
     * Stop countdown timer and clean up resources
     */
    private void stopCountdownTimer() {
        if (countdownHandler != null && countdownRunnable != null) {
            countdownHandler.removeCallbacks(countdownRunnable);
            Log.d(TAG, "Countdown timer stopped");
        }
    }

    /**
     * Initialize Quick Navigation Buttons on Prayer Card
     * Sets up click listeners for Prayer, Quran, Learn, and Tools buttons
     * 
     * IMPORTANT: For bottom nav items (Prayer/Learn), we trigger BottomNavigationView clicks
     * instead of using NavController.navigate() to avoid back stack conflicts.
     */
    private void initializeQuickNavigationButtons() {
        try {
            View rootView = mBinding.getRoot();
            
            // Find the Prayer Card view first
            View prayerCardView = rootView.findViewById(R.id.prayer_card);
            if (prayerCardView == null) {
                Log.e(TAG, "Prayer Card view not found");
                return;
            }
            
            // Find navigation buttons
            LinearLayout btnNavPrayer = prayerCardView.findViewById(R.id.btn_nav_prayer);
            LinearLayout btnNavQuran = prayerCardView.findViewById(R.id.btn_nav_quran);
            LinearLayout btnNavLearn = prayerCardView.findViewById(R.id.btn_nav_learn);
            LinearLayout btnNavTools = prayerCardView.findViewById(R.id.btn_nav_tools);
            
            // Prayer Icon - Trigger bottom nav Salat item
            if (btnNavPrayer != null) {
                btnNavPrayer.setOnClickListener(v -> {
                    try {
                        // Find BottomNavigationView at click time (not initialization time)
                        com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = 
                            requireActivity().findViewById(R.id.nav_view);
                        
                        if (bottomNav != null) {
                            // Trigger the bottom navigation item click for Salat
                            bottomNav.setSelectedItemId(R.id.nav_namaz);
                            Log.d(TAG, "Triggered bottom nav: Salat page");
                        } else {
                            Log.e(TAG, "Bottom navigation view not found");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error navigating to Salat page", e);
                    }
                });
                Log.d(TAG, "Prayer button click listener registered");
            } else {
                Log.w(TAG, "Prayer navigation button not found");
            }
            
            // Quran Icon - Launch Quran Index/Directory Activity (Surah/Juz selection)
            if (btnNavQuran != null) {
                btnNavQuran.setOnClickListener(v -> {
                    try {
                        // Launch the Quran Index page (Surah/Juz directory), NOT the reader
                        Intent intent = new Intent(requireActivity(), 
                            com.quran.quranaudio.online.quran_module.activities.ActivityReaderIndexPage.class);
                        startActivity(intent);
                        Log.d(TAG, "Launching Quran Index/Directory page");
                    } catch (Exception e) {
                        Log.e(TAG, "Error launching Quran Index", e);
                    }
                });
                Log.d(TAG, "Quran button click listener registered");
            } else {
                Log.w(TAG, "Quran navigation button not found");
            }
            
            // Learn Icon - Trigger bottom nav Discover/Learn item
            if (btnNavLearn != null) {
                btnNavLearn.setOnClickListener(v -> {
                    try {
                        // Find BottomNavigationView at click time (not initialization time)
                        com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = 
                            requireActivity().findViewById(R.id.nav_view);
                        
                        if (bottomNav != null) {
                            // Trigger the bottom navigation item click for Learn/Discover
                            bottomNav.setSelectedItemId(R.id.nav_name_99);
                            Log.d(TAG, "Triggered bottom nav: Learn/Discover page");
                        } else {
                            Log.e(TAG, "Bottom navigation view not found");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error navigating to Learn page", e);
                    }
                });
                Log.d(TAG, "Learn button click listener registered");
            } else {
                Log.w(TAG, "Learn navigation button not found");
            }
            
            // Tools Icon - Show tools menu bottom sheet
            if (btnNavTools != null) {
                btnNavTools.setOnClickListener(v -> {
                    try {
                        // Show tools menu bottom sheet
                        ToolsMenuBottomSheet toolsMenu = new ToolsMenuBottomSheet();
                        toolsMenu.show(getChildFragmentManager(), "TOOLS_MENU");
                        Log.d(TAG, "Showing tools menu bottom sheet");
                    } catch (Exception e) {
                        Log.e(TAG, "Error showing tools menu", e);
                    }
                });
                Log.d(TAG, "Tools button click listener registered");
            } else {
                Log.w(TAG, "Tools navigation button not found");
            }
            
            Log.d(TAG, "Quick navigation buttons initialized successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error initializing quick navigation buttons", e);
        }
    }

    /**
     * Initialize Verse of The Day Card
     * Properly reuses VOTDView for verse fetching and formatting
     */
    private void initializeVerseOfDayCard() {
        try {
            View rootView = mBinding.getRoot();
            View verseCard = rootView.findViewById(R.id.verse_of_day_card);
            
            if (verseCard == null) {
                Log.w(TAG, "Verse of The Day card not found");
                return;
            }
            
            // Find views
            tvVotdContentText = verseCard.findViewById(R.id.votd_content_text);
            tvVotdReference = verseCard.findViewById(R.id.votd_verse_reference);
            ivVotdShare = verseCard.findViewById(R.id.votd_share);
            ivVotdBookmark = verseCard.findViewById(R.id.votd_bookmark);
            votdViewEmbedded = verseCard.findViewById(R.id.votd_view_embedded);
            ImageView votdBackgroundImage = verseCard.findViewById(R.id.votd_background_image);
            
            if (votdViewEmbedded == null) {
                Log.w(TAG, "VOTDView embedded not found");
                return;
            }
            
            // Set random background image with blur effect
            setVotdRandomBackground(votdBackgroundImage);
            
            // Load QuranMeta and initialize VOTD
            QuranMeta.prepareInstance(requireContext(), quranMeta -> {
                if (votdViewEmbedded != null) {
                    // Refresh VOTDView to load today's verse
                    votdViewEmbedded.refresh(quranMeta);
                    
                    // Wait for VOTD to load, then extract and display content
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        extractAndDisplayVotdContent();
                        setupVotdActions();
                    }, 800);  // Increased delay to ensure data is loaded
                }
            });
            
            Log.d(TAG, "Verse of The Day card initialized");
            
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Verse of The Day card", e);
        }
    }
    
    /**
     * Extract content from embedded VOTDView and display in custom card layout
     * This preserves all formatting from ReaderVerseDecorator
     */
    private void extractAndDisplayVotdContent() {
        try {
            if (votdViewEmbedded == null) {
                Log.w(TAG, "VOTDView embedded is null");
                return;
            }
            
            // Find the content container from VOTDView
            View container = votdViewEmbedded.findViewById(R.id.container);
            if (container == null) {
                Log.w(TAG, "VOTD container not found");
                return;
            }
            
            // Find the text view with formatted verse content (Arabic + Translation)
            TextView votdText = container.findViewById(R.id.text);
            TextView votdInfo = container.findViewById(R.id.verseInfo);
            
            if (votdText != null && tvVotdContentText != null) {
                // Get the full formatted text (includes Arabic with proper font, Translation, etc.)
                CharSequence fullText = votdText.getText();
                
                if (fullText != null && fullText.length() > 0) {
                    // Copy the ENTIRE formatted text (preserves SpannableString formatting)
                    tvVotdContentText.setText(fullText, TextView.BufferType.SPANNABLE);
                    Log.d(TAG, "VOTD content copied with formatting preserved");
                } else {
                    tvVotdContentText.setText("Loading verse...");
                    Log.w(TAG, "VOTD text is empty, retrying...");
                    // Retry after another delay
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        extractAndDisplayVotdContent();
                    }, 500);
                }
            }
            
            // Get verse reference and add underline
            if (votdInfo != null && tvVotdReference != null) {
                String referenceText = votdInfo.getText().toString();
                android.text.SpannableString spannableReference = new android.text.SpannableString(referenceText);
                spannableReference.setSpan(
                    new android.text.style.UnderlineSpan(),
                    0,
                    referenceText.length(),
                    android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
                tvVotdReference.setText(spannableReference);
                Log.d(TAG, "VOTD reference set: " + referenceText);
            }
            
            // Extract chapter and verse numbers using reflection (to access private fields)
            try {
                java.lang.reflect.Field chapterField = VOTDView.class.getDeclaredField("mChapterNo");
                java.lang.reflect.Field verseField = VOTDView.class.getDeclaredField("mVerseNo");
                chapterField.setAccessible(true);
                verseField.setAccessible(true);
                votdChapterNo = (int) chapterField.get(votdViewEmbedded);
                votdVerseNo = (int) verseField.get(votdViewEmbedded);
                Log.d(TAG, "VOTD verse location: Chapter " + votdChapterNo + ", Verse " + votdVerseNo);
            } catch (Exception e) {
                Log.e(TAG, "Error extracting chapter/verse numbers", e);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error extracting VOTD content", e);
        }
    }
    
    /**
     * Setup Share, Bookmark, and Reference Click actions for VOTD
     */
    private void setupVotdActions() {
        // Share button
        if (ivVotdShare != null) {
            ivVotdShare.setOnClickListener(v -> shareVerseOfDay());
        }
        
        // Bookmark button
        if (ivVotdBookmark != null) {
            ivVotdBookmark.setOnClickListener(v -> toggleVotdBookmark());
        }
        
        // Reference text - Click to jump to verse
        if (tvVotdReference != null) {
            tvVotdReference.setOnClickListener(v -> jumpToVerseOfDay());
        }
    }
    
    /**
     * Jump to the Verse of The Day in Quran Reader
     */
    private void jumpToVerseOfDay() {
        try {
            if (votdChapterNo == -1 || votdVerseNo == -1) {
                Log.w(TAG, "Cannot jump to verse: chapter or verse number not set");
                return;
            }
            
            // Use ReaderFactory to start verse reading activity
            com.quran.quranaudio.online.quran_module.utils.reader.factory.ReaderFactory.startVerse(
                requireContext(), 
                votdChapterNo, 
                votdVerseNo
            );
            
            Log.d(TAG, "Jumping to VOTD: Chapter " + votdChapterNo + ", Verse " + votdVerseNo);
            
        } catch (Exception e) {
            Log.e(TAG, "Error jumping to Verse of The Day", e);
        }
    }
    
    /**
     * Share Verse of The Day using Android system share
     */
    private void shareVerseOfDay() {
        try {
            String contentText = tvVotdContentText != null ? tvVotdContentText.getText().toString() : "";
            String reference = tvVotdReference != null ? tvVotdReference.getText().toString() : "";
            
            String shareText = contentText + "\n\n" + "— " + reference;
            
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Verse of The Day");
            
            startActivity(Intent.createChooser(shareIntent, "Share Verse"));
            Log.d(TAG, "Verse of The Day shared");
            
        } catch (Exception e) {
            Log.e(TAG, "Error sharing Verse of The Day", e);
        }
    }
    
    /**
     * Toggle bookmark for Verse of The Day
     * Reuses VOTDView's bookmark functionality
     */
    private void toggleVotdBookmark() {
        try {
            if (votdViewEmbedded == null || ivVotdBookmark == null) {
                Log.w(TAG, "Cannot toggle bookmark: views not initialized");
                return;
            }
            
            // Find the bookmark button in the embedded VOTDView
            View container = votdViewEmbedded.findViewById(R.id.container);
            if (container != null) {
                ImageView bookmarkButton = container.findViewById(R.id.votdBookmark);
                if (bookmarkButton != null) {
                    // Trigger the bookmark action
                    bookmarkButton.performClick();
                    
                    // Update our icon to match
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        try {
                            // Check the drawable resource of the embedded bookmark button
                            android.graphics.drawable.Drawable drawable = bookmarkButton.getDrawable();
                            if (drawable != null) {
                                // Copy the same drawable to our button
                                ivVotdBookmark.setImageDrawable(drawable);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error updating bookmark icon", e);
                        }
                    }, 100);
                    
                    Log.d(TAG, "Verse of The Day bookmark toggled");
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error toggling Verse of The Day bookmark", e);
        }
    }

    /**
     * Initialize Mecca Live Card
     * Displays a static cover with live info and click to launch full-screen player
     */
    private void initializeMeccaLiveCard() {
        try {
            View rootView = mBinding.getRoot();
            View meccaCard = rootView.findViewById(R.id.mecca_live_card);
            
            if (meccaCard == null) {
                Log.w(TAG, "Mecca Live card not found");
                return;
            }
            
            // Find viewer count TextView
            TextView tvViewers = meccaCard.findViewById(R.id.mecca_live_viewers);
            
            // Generate random viewer count (795-13849)
            int viewerCount = 795 + new java.util.Random().nextInt(13849 - 795 + 1);
            if (tvViewers != null) {
                tvViewers.setText("👁 " + String.format(java.util.Locale.US, "%,d", viewerCount));
            }
            
            // Mecca Live URLs (HLS priority for better in-app playback)
            final String[] meccaLiveUrls = {
                "http://m.live.net.sa:1935/live/quran/playlist.m3u8",
                "https://ythls.armelin.one/channel/UCos1bXP9p_5ntw8HcjxsNBw.m3u8",
                "https://www.youtube.com/watch?v=4s4XX-qaNgg",
                "https://www.youtube.com/watch?v=jdF00WnAwVw",
                "https://www.youtube.com/watch?v=21rf6-horn4"
            };
            
            // Click entire card to launch LiveActivity
            meccaCard.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(requireActivity(), LiveActivity.class);
                    intent.putExtra("live", meccaLiveUrls[0]); // Use first HLS URL
                    intent.putExtra("backup_urls", meccaLiveUrls);
                    startActivity(intent);
                    Log.d(TAG, "Mecca Live started with " + viewerCount + " viewers");
                } catch (Exception e) {
                    Log.e(TAG, "Error starting Mecca Live", e);
                }
            });
            
            Log.d(TAG, "Mecca Live card initialized with " + viewerCount + " viewers");
            
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Mecca Live card", e);
        }
    }
    
    /**
     * Initialize Medina Live Card
     * Displays a static cover with live info and click to launch full-screen player
     */
    private void initializeMedinaLiveCard() {
        try {
            View rootView = mBinding.getRoot();
            View medinaCard = rootView.findViewById(R.id.medina_live_card);
            
            if (medinaCard == null) {
                Log.w(TAG, "Medina Live card not found");
                return;
            }
            
            // Find viewer count TextView
            TextView tvViewers = medinaCard.findViewById(R.id.medina_live_viewers);
            
            // Generate random viewer count (135-8523)
            int viewerCount = 135 + new java.util.Random().nextInt(8523 - 135 + 1);
            if (tvViewers != null) {
                tvViewers.setText("👁 " + String.format(java.util.Locale.US, "%,d", viewerCount));
            }
            
            // Medina Live URLs (HLS priority for better in-app playback)
            final String[] medinaLiveUrls = {
                "http://m.live.net.sa:1935/live/sunnah/playlist.m3u8",
                "https://ythls.armelin.one/channel/UCJr4gikBowJ8I-iUXs7CkMg.m3u8",
                "https://www.youtube.com/watch?v=4s4XX-qaNgg",
                "https://www.youtube.com/watch?v=0lg0XeJ2gAU",
                "https://www.youtube.com/watch?v=4Ar8JHRCdSE"
            };
            
            // Click entire card to launch LiveActivity
            medinaCard.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(requireActivity(), LiveActivity.class);
                    intent.putExtra("live", medinaLiveUrls[0]); // Use first HLS URL
                    intent.putExtra("backup_urls", medinaLiveUrls);
                    startActivity(intent);
                    Log.d(TAG, "Medina Live started with " + viewerCount + " viewers");
                } catch (Exception e) {
                    Log.e(TAG, "Error starting Medina Live", e);
                }
            });
            
            Log.d(TAG, "Medina Live card initialized with " + viewerCount + " viewers");
            
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Medina Live card", e);
        }
    }
    
    /**
     * Initialize Quiz Module
     * Shows daily Quran quiz card below Verse of the Day
     */
    private void initializeQuizModule() {
        try {
            // Initialize Quiz Repository
            quizRepository = new QuizRepository(requireContext());
            
            // Find Quiz Entry View
            quizEntryView = mBinding.getRoot().findViewById(R.id.quiz_entry_view);
            if (quizEntryView == null) {
                Log.w(TAG, "Quiz entry view not found");
                return;
            }
            
            // Find Quiz Views
            quizQuestionTextView = quizEntryView.findViewById(R.id.tv_question_text);
            
            com.google.android.material.button.MaterialButton optionA = quizEntryView.findViewById(R.id.btn_option_a);
            com.google.android.material.button.MaterialButton optionB = quizEntryView.findViewById(R.id.btn_option_b);
            com.google.android.material.button.MaterialButton optionC = quizEntryView.findViewById(R.id.btn_option_c);
            com.google.android.material.button.MaterialButton optionD = quizEntryView.findViewById(R.id.btn_option_d);
            
            quizOptionButtons = java.util.Arrays.asList(optionA, optionB, optionC, optionD);
            
            // Bind current quiz question
            bindCurrentQuizQuestion();
            
            Log.d(TAG, "Quiz module initialized successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Quiz module", e);
        }
    }
    
    /**
     * Bind current quiz question to the UI
     */
    private void bindCurrentQuizQuestion() {
        try {
            if (quizEntryView == null || quizRepository == null) {
                Log.w(TAG, "Quiz view or repository is null");
                return;
            }
            
            // Check if quiz is supported for current language
            if (!isQuizSupportedLanguage()) {
                quizEntryView.setVisibility(View.GONE);
                Log.d(TAG, "Quiz not supported for current language");
                return;
            }
            
            // Get current quiz question
            currentQuizQuestion = quizRepository.getCurrentQuestion();
            
            if (currentQuizQuestion == null) {
                quizEntryView.setVisibility(View.GONE);
                Log.w(TAG, "No quiz question available");
                return;
            }
            
            // Show quiz view
            quizEntryView.setVisibility(View.VISIBLE);
            
            // Set question text
            if (quizQuestionTextView != null) {
                quizQuestionTextView.setText(currentQuizQuestion.getQuestionText());
                int quizColor = ContextCompat.getColor(requireContext(), R.color.quran_quiz_green_dark);
                quizQuestionTextView.setTextColor(quizColor);
            }
            
            // Set options
            if (quizOptionButtons != null && !quizOptionButtons.isEmpty()) {
                List<String> options = currentQuizQuestion.getOptions();
                for (int i = 0; i < quizOptionButtons.size() && i < options.size(); i++) {
                    final int selectedIndex = i;
                    com.google.android.material.button.MaterialButton button = quizOptionButtons.get(i);
                    String optionPrefix = getQuizOptionPrefix(i);
                    button.setText(optionPrefix + " " + options.get(i));
                    button.setOnClickListener(v -> handleQuizOptionSelected(selectedIndex));
                }
            }
            
            Log.d(TAG, "Quiz question bound successfully: " + currentQuizQuestion.getQuestionText());
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to bind quiz question", e);
            if (quizEntryView != null) {
                quizEntryView.setVisibility(View.GONE);
            }
        }
    }
    
    /**
     * Check if quiz is supported for current language
     * Currently supports English (en) and Indonesian (in/id)
     */
    private boolean isQuizSupportedLanguage() {
        try {
            java.util.Locale activeLocale;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                activeLocale = requireContext().getResources().getConfiguration().getLocales().get(0);
            } else {
                activeLocale = requireContext().getResources().getConfiguration().locale;
            }
            
            String languageCode = activeLocale != null ? activeLocale.getLanguage() : null;
            
            if (TextUtils.isEmpty(languageCode)) {
                languageCode = java.util.Locale.getDefault().getLanguage();
            }
            
            if (TextUtils.isEmpty(languageCode)) {
                return false;
            }
            
            languageCode = languageCode.toLowerCase(java.util.Locale.US);
            
            boolean isSupported = "en".equals(languageCode) || "id".equals(languageCode);  // 统一使用 "id" 表示印尼语
            Log.d(TAG, "Quiz language check: " + languageCode + " -> " + (isSupported ? "supported" : "not supported"));
            
            return isSupported;
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking quiz language support", e);
            return false;
        }
    }
    
    /**
     * Get option prefix (A, B, C, D)
     */
    private String getQuizOptionPrefix(int index) {
        switch (index) {
            case 0: return "A";
            case 1: return "B";
            case 2: return "C";
            case 3: return "D";
            default: return "";
        }
    }
    
    /**
     * Handle quiz option selection - Launch QuranQuizNotifyResultActivity (result page) directly
     * This allows the user to see the result immediately and continue to Discover quiz module
     */
    private void handleQuizOptionSelected(int selectedIndex) {
        try {
            if (currentQuizQuestion == null) {
                Log.w(TAG, "Current quiz question is null");
                return;
            }
            
            // Mark question as answered
            quizRepository.markQuestionAnswered(currentQuizQuestion.getId());
            
            // Launch result page directly with the user's selected answer
            launchQuizResultPage(selectedIndex);
            
            Log.d(TAG, "Launching quiz result page for option: " + getQuizOptionPrefix(selectedIndex));
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch quiz result page", e);
            Toast.makeText(requireContext(), "Failed to start quiz", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Launch QuranQuizNotifyResultActivity (Discover-style result page) directly
     * This shows the correct/incorrect feedback and allows user to continue in Discover quiz module
     */
    private void launchQuizResultPage(int selectedIndex) {
        try {
            if (currentQuizQuestion == null) {
                Log.w(TAG, "Cannot launch quiz result page - question is null");
                return;
            }

            QuestionBean questionBean = buildQuestionBean(currentQuizQuestion);
            if (questionBean == null) {
                Log.w(TAG, "Failed to build QuestionBean");
                return;
            }

            // Get the selected answer key (A, B, C, or D)
            String selectedAnswerKey = getQuizOptionPrefix(selectedIndex);
            
            // Get correct answer for logging
            String correctAnswerKey = getQuizOptionPrefix(currentQuizQuestion.getCorrectAnswerIndex());

            Log.d(TAG, "========================================");
            Log.d(TAG, "Quiz Answer Debug Info:");
            Log.d(TAG, "Question: " + currentQuizQuestion.getQuestionText());
            Log.d(TAG, "Selected Index: " + selectedIndex + " (Key: " + selectedAnswerKey + ")");
            Log.d(TAG, "Correct Index: " + currentQuizQuestion.getCorrectAnswerIndex() + " (Key: " + correctAnswerKey + ")");
            Log.d(TAG, "Options: " + currentQuizQuestion.getOptions());
            Log.d(TAG, "Is Correct: " + (selectedIndex == currentQuizQuestion.getCorrectAnswerIndex()));
            Log.d(TAG, "========================================");

            Bundle bundle = new Bundle();
            bundle.putParcelable(Constants.INTENT_NOTIFY_QUIZ_BEAN, questionBean);
            bundle.putString(Constants.INTENT_NOTIFY_QUIZ_SELECT_ANSWER, selectedAnswerKey);

            com.quranaudio.quiz.quiz.QuranQuizNotifyResultActivity.Companion.open(requireContext(), bundle);

            Log.d(TAG, "Quiz result page launched with answer: " + selectedAnswerKey);

        } catch (Exception e) {
            Log.e(TAG, "Failed to launch quiz result page", e);
            Toast.makeText(requireContext(), "Unable to open quiz result", Toast.LENGTH_SHORT).show();
        }
    }

    private QuestionBean buildQuestionBean(QuizQuestion quizQuestion) {
        try {
            if (quizQuestion == null) {
                return null;
            }

            List<String> options = quizQuestion.getOptions();
            if (options == null || options.isEmpty()) {
                Log.w(TAG, "Quiz question options are empty");
                return null;
            }

            TreeMap<String, String> optionMap = new TreeMap<>();
            int optionCount = Math.min(options.size(), QUIZ_OPTION_KEYS.length);
            for (int i = 0; i < optionCount; i++) {
                optionMap.put(QUIZ_OPTION_KEYS[i], options.get(i));
            }

            String correctKey = getQuizOptionPrefix(quizQuestion.getCorrectAnswerIndex());
            if (!optionMap.containsKey(correctKey)) {
                Log.w(TAG, "Correct option key not found in option map: " + correctKey);
                return null;
            }

            return new QuestionBean(
                quizQuestion.getId(),
                quizQuestion.getQuestionText(),
                optionMap,
                0,
                correctKey
            );

        } catch (Exception e) {
            Log.e(TAG, "Failed to build QuestionBean", e);
            return null;
        }
    }
    
    /**
     * Initialize Daily Quests Feature (Streak Card + Today's Quests)
     * Shows create plan card if user hasn't created a learning plan yet
     * Otherwise shows streak statistics and daily tasks
     */
    private void initializeDailyQuests() {
        try {
            // Initialize Quest Repository
            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            questRepository = new QuestRepository(firestore);
            
            // Initialize Daily Quests Manager
            dailyQuestsManager = new DailyQuestsManager(
                this,                    // Fragment
                mBinding.getRoot(),      // Root View
                questRepository          // Quest Repository
            );
            
            // Initialize and start observing
            dailyQuestsManager.initialize();
            
            Log.d(TAG, "Daily Quests feature initialized");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Daily Quests feature", e);
        }
    }

    @Override
    public void onDestroyView() {
        // Stop countdown timer to prevent memory leaks
        stopCountdownTimer();
        
        // CRITICAL: Clean up Dialog to prevent memory leaks
        if (dialogWarning != null) {
            if (dialogWarning.isShowing()) {
                dialogWarning.dismiss();
                Log.d(TAG, "Dialog dismissed in onDestroyView()");
            }
            dialogWarning = null;
        }
        
        // Clean up VOTD view
        if (votdViewEmbedded != null) {
            votdViewEmbedded.destroy();
            votdViewEmbedded = null;
        }
        
        // Cleanup Daily Quests
        if (dailyQuestsManager != null) {
            dailyQuestsManager.onDestroy();
        }
        
        super.onDestroyView();
    }


}
