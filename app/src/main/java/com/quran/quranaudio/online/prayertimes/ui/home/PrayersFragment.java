package com.quran.quranaudio.online.prayertimes.ui.home;

import static android.content.Context.MODE_PRIVATE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ProgressBar;

import java.lang.ref.WeakReference;

import com.google.android.material.button.MaterialButton;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.quran.quranaudio.online.Utils.GoogleAuthManager;
import com.quran.quranaudio.online.common.rate.RatePromptManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.faltenreich.skeletonlayout.Skeleton;
import com.google.firebase.auth.FirebaseAuth;
import com.quran.quranaudio.online.App;
import com.quran.quranaudio.online.prayertimes.common.ComplementaryTimingEnum;
import com.quran.quranaudio.online.prayertimes.common.PrayerEnum;
import com.quran.quranaudio.online.prayertimes.job.WorkCreator;
import com.quran.quranaudio.online.prayertimes.preferences.PreferencesConstants;
import com.quran.quranaudio.online.prayertimes.timings.DayPrayer;
import com.quran.quranaudio.online.prayertimes.utils.AlertHelper;
import com.quran.quranaudio.online.prayertimes.utils.PrayerUtils;
import com.quran.quranaudio.online.prayertimes.utils.TimingUtils;
import com.quran.quranaudio.online.prayertimes.utils.UiUtils;
import com.quran.quranaudio.online.R;
import com.quran.quranaudio.online.quests.data.SalahName;
import com.quran.quranaudio.online.quests.viewmodel.SalahViewModel;
import com.quran.quranaudio.online.prayertimes.repository.PrayerLogRepository;
import com.quran.quranaudio.online.prayertimes.models.PrayerLog;
import com.mikhaellopez.circularprogressbar.CircularProgressBar;

import org.apache.commons.lang3.StringUtils;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import cl.jesualex.stooltip.Position;
import cl.jesualex.stooltip.Tooltip;


public class PrayersFragment extends Fragment implements com.quran.quranaudio.online.prayertimes.ui.PrayerLogBottomSheet.OnPrayerLoggedListener {

    @Inject
    ViewModelProvider.Factory viewModelFactory;

    private SalahViewModel salahViewModel;
    private GoogleAuthManager googleAuthManager;
    private ActivityResultLauncher<Intent> signInLauncher;
    private PrayerLogRepository prayerLogRepository;
    
    // 🔔 用于接收通知设置页面的返回结果
    private ActivityResultLauncher<Intent> notificationSettingsLauncher;
    
    // Salah track buttons
    private MaterialButton fajrTrackButton;
    private MaterialButton dhuhrTrackButton;
    private MaterialButton asrTrackButton;
    private MaterialButton maghribTrackButton;
    private MaterialButton ishaTrackButton;
    
    // Status icons (ImageView) for displaying different states
    private ImageView fajrStatusIcon;
    private ImageView dhuhrStatusIcon;
    private ImageView asrStatusIcon;
    private ImageView maghribStatusIcon;
    private ImageView ishaStatusIcon;
    
    // Store current prayer log status and IDs for each prayer
    private java.util.Map<String, PrayerLog> todayPrayerLogs = new java.util.HashMap<>();
    
    // Store today's prayer times for comparison
    private DayPrayer currentDayPrayer = null;
    private final Set<String> autoMissInProgress = new HashSet<>();

    // Qada summary UI
    private View qadaSummaryCard;
    private TextView qadaCountTextView;
    private ProgressBar qadaProgressBar;

    private boolean fajrCompletedLast;
    private boolean dhuhrCompletedLast;
    private boolean asrCompletedLast;
    private boolean maghribCompletedLast;
    private boolean ishaCompletedLast;
    private boolean completionStatesInitialized;
    
    // ⭐ Location permission tracking
    private static final String PREFS_NAME = "LocationPermissionPrefs";
    private static final String KEY_PERMISSION_REQUEST_COUNT = "permission_request_count";
    private static final int MAX_PERMISSION_REQUESTS = 2;
    
    // 🔔 Notification permission tracking
    private static final String NOTIFICATION_PREFS_NAME = "NotificationPermissionPrefs";
    private static final String KEY_FIRST_ENTRY_SHOWN = "notification_first_entry_shown";
    private static final int NOTIFICATION_PERMISSION_DELAY_MS = 3000; // 3 seconds
    private Handler notificationPermissionHandler;
    private Runnable notificationPermissionRunnable;
    private ActivityResultLauncher<String> notificationPermissionLauncher;

    private LocalDateTime todayDate;
    private CountDownTimer TimeRemainingCTimer;

    private TextView locationTextView;
    private TextView calculationMethodTextView;
    //   private TextView holidayIndicatorTextView;
    private TextView todayDateTextView;
    private TextView prayerNametextView;
    private TextView prayerTimetextView;
    private TextView timeRemainingTextView;
    private TextView fajrTimingTextView;
    private TextView dohrTimingTextView;
    private TextView asrTimingTextView;
    private TextView maghribTimingTextView;
    private TextView ichaTimingTextView;
    private TextView sunriseTimingTextView;
    private TextView sunsetTimingTextView;
    private TextView fajrLabel;
    private TextView dohrLabel;
    private TextView asrLabel;
    private TextView maghribLabel;
    private TextView ichaLabel;

    private ImageView islamic_cal;



    private CircularProgressBar circularProgressBar;
    private String adhanCallsPreferences;
    private String adhanCallKeyPart;
    private Skeleton skeleton;

    @Override
    public void onAttach(@NonNull Context context) {
        ((App) requireContext().getApplicationContext())
                .appComponent
                .homeComponent()
                .create()
                        .inject(this);

        super.onAttach(context);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 🔔 初始化通知设置页面返回结果监听器
        notificationSettingsLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Log.d("PrayersFragment", "🔔 Notification settings changed, rescheduling alarms");
                    
                    // 重新调度闹钟（复用现有的调度逻辑）
                    if (currentDayPrayer != null) {
                        startPrayerSchedulerWork(currentDayPrayer);
                        Log.d("PrayersFragment", "✅ Alarms rescheduled successfully");
                    } else {
                        Log.w("PrayersFragment", "⚠️ currentDayPrayer is null, cannot reschedule");
                    }
                    
                    // 刷新通知图标
                    refreshAllNotificationIcons();
                } else {
                    Log.d("PrayersFragment", "ℹ️ Notification settings not changed");
                }
            }
        );
        
        // Initialize Google Auth Manager for login support
        try {
            googleAuthManager = new GoogleAuthManager(requireContext());
            Log.d("PrayersFragment", "GoogleAuthManager initialized successfully");
        } catch (Exception e) {
            Log.e("PrayersFragment", "Failed to initialize GoogleAuthManager", e);
        }
        
        // Initialize Sign-In Launcher
        signInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                try {
                    if (!isAdded() || getContext() == null) {
                        Log.w("PrayersFragment", "Fragment not attached, ignoring sign-in result");
                        return;
                    }
                    
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        handleSignInResult(result.getData());
                    } else {
                        Log.w("PrayersFragment", "Sign-in canceled or failed");
                        if (googleAuthManager != null) {
                            googleAuthManager.logSignInDiagnostics(result.getData(), "PrayersFragment-CANCELLED");
                        }
                        Toast.makeText(requireContext(), "Login cancelled", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.e("PrayersFragment", "Error handling sign-in result", e);
                }
            }
        );
        
        // 🔔 Initialize Notification Permission Launcher (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        Log.d("PrayersFragment", "✅ Notification permission granted");
                        Toast.makeText(requireContext(), "Notification enabled", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.d("PrayersFragment", "❌ Notification permission denied");
                    }
                }
            );
        }
    }

    private boolean getPreviousCompletionState(SalahName salahName) {
        switch (salahName) {
            case FAJR:
                return fajrCompletedLast;
            case DHUHR:
                return dhuhrCompletedLast;
            case ASR:
                return asrCompletedLast;
            case MAGHRIB:
                return maghribCompletedLast;
            case ISHA:
                return ishaCompletedLast;
            default:
                return false;
        }
    }

    private void setPreviousCompletionState(SalahName salahName, boolean value) {
        switch (salahName) {
            case FAJR:
                fajrCompletedLast = value;
                break;
            case DHUHR:
                dhuhrCompletedLast = value;
                break;
            case ASR:
                asrCompletedLast = value;
                break;
            case MAGHRIB:
                maghribCompletedLast = value;
                break;
            case ISHA:
                ishaCompletedLast = value;
                break;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        todayDate = LocalDateTime.now();

        adhanCallsPreferences = PreferencesConstants.ADTHAN_CALLS_SHARED_PREFERENCES;
        adhanCallKeyPart = PreferencesConstants.ADTHAN_CALL_ENABLED_KEY;

        TypedArray typedArray = requireContext().getTheme().obtainStyledAttributes(R.styleable.mainStyles);
        int navigationBackgroundStartColor = typedArray.getColor(R.styleable.mainStyles_navigationBackgroundStartColor, ContextCompat.getColor(requireContext(), R.color.alabaster));
        int navigationBackgroundEndColor = typedArray.getColor(R.styleable.mainStyles_navigationBackgroundEndColor, ContextCompat.getColor(requireContext(), R.color.alabaster));
        typedArray.recycle();

        // Use Activity scope to share ViewModel with MainActivity preload and other fragments
        // This ensures data is loaded once and shared across all fragments
        HomeViewModel homeViewModel = new ViewModelProvider(requireActivity(), viewModelFactory)
                .get(HomeViewModel.class);

        // Initialize SalahViewModel for prayer tracking
        salahViewModel = new ViewModelProvider(this).get(SalahViewModel.class);

        View rootView = inflater.inflate(R.layout.fragment_prayers, container, false);

        initializeViews(rootView);
        initializeSalahRecording();

        homeViewModel
                .getError()
                .observe(
                        getViewLifecycleOwner(),
                        error -> AlertHelper.displayLocationErrorDialog(requireActivity(),
                                getResources().getString(R.string.location_alert_title),
                                error));

        homeViewModel.getDayPrayers().observe(getViewLifecycleOwner(), dayPrayer -> {
            // Store the day prayer for time checking
            currentDayPrayer = dayPrayer;
            
            updateDatesTextViews(dayPrayer);
            updateNextPrayerViews(dayPrayer);
            updateTimingsTextViews(dayPrayer);
            startPrayerSchedulerWork(dayPrayer);

            skeleton.showOriginal();


        });

        ViewTreeObserver observer = rootView.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        
        // 🔔 延迟 3 秒请求通知权限（首次进入 Salat 页面）
        scheduleNotificationPermissionRequest();

        return rootView;
    }

    public static PrayersFragment newInstance() {
        Bundle bundle = new Bundle();
        PrayersFragment prayersFragment = new PrayersFragment();
        prayersFragment.setArguments(bundle);
        return prayersFragment;
    }




    @Override
    public void onDestroy() {
        cancelTimer();
        
        // 🔔 Clean up notification permission handler
        cleanupNotificationPermissionHandler();
        
        // Clean up Google Auth Manager
        try {
            if (googleAuthManager != null) {
                googleAuthManager = null;
                Log.d("PrayersFragment", "GoogleAuthManager cleaned up");
            }
        } catch (Exception e) {
            Log.e("PrayersFragment", "Error cleaning up GoogleAuthManager", e);
        }
        
        super.onDestroy();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        
        // 🔄 刷新祷告状态（用户可能在其他页面记录了祷告）
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            Log.d("PrayersFragment", "🔄 onResume: Reloading prayer logs");
            loadTodayPrayerLogs();
        }
        
        // 刷新所有祷告时间的通知图标（用户可能从通知设置页面返回）
        refreshAllNotificationIcons();

        // 延迟3秒后检查通知权限
        scheduleNotificationPermissionRequest();

        // 🏠 桌面 Widget 促活：第3次访问祈祷页（aha时刻）弹一次系统级添加引导；
        // 内部与上面的通知权限请求流互斥、一生仅一次、Widget已添加时静默
        if (getActivity() != null) {
            com.quran.quranaudio.online.prayertimes.widget.PrayerWidgetPromoHelper
                    .onPrayerTabVisible(getActivity());
        }
    }
    
    /**
     * Refreshes all prayer notification icons based on current settings
     * Called when returning from notification settings page
     */
    private void refreshAllNotificationIcons() {
        if (getView() != null) {
            // Get all ImageView references again and update them
            ImageView fajrCallImageView = getView().findViewById(R.id.fajr_call_image_view);
            ImageView dohrCallImageView = getView().findViewById(R.id.dohr_call_image_view);
            ImageView asrCallImageView = getView().findViewById(R.id.asr_call_image_view);
            ImageView maghrebCallImageView = getView().findViewById(R.id.maghreb_call_image_view);
            ImageView ichaCallImageView = getView().findViewById(R.id.icha_call_image_view);
            
            // Update each icon based on current notification settings
            if (fajrCallImageView != null) updateNotificationIcon(fajrCallImageView, PrayerEnum.FAJR);
            if (dohrCallImageView != null) updateNotificationIcon(dohrCallImageView, PrayerEnum.DHOHR);
            if (asrCallImageView != null) updateNotificationIcon(asrCallImageView, PrayerEnum.ASR);
            if (maghrebCallImageView != null) updateNotificationIcon(maghrebCallImageView, PrayerEnum.MAGHRIB);
            if (ichaCallImageView != null) updateNotificationIcon(ichaCallImageView, PrayerEnum.ICHA);
        }
    }
    
    /**
     * Updates a single notification icon based on current settings
     */
    private void updateNotificationIcon(ImageView imageView, PrayerEnum prayerEnum) {
        SharedPreferences notificationPrefs = requireContext().getSharedPreferences(PreferencesConstants.ADTHAN_CALLS_SHARED_PREFERENCES, MODE_PRIVATE);
        String notificationTypeKey = prayerEnum.toString() + "_NOTIFICATION_TYPE";
        String notificationType = notificationPrefs.getString(notificationTypeKey, "none");
        
        int iconResource = getNotificationIconForType(notificationType);
        imageView.setImageResource(iconResource);
    }

    private void initializeViews(View rootView) {
        skeleton = rootView.findViewById(R.id.skeletonLayout);

        locationTextView = rootView.findViewById(R.id.location_text_view);
        todayDateTextView = rootView.findViewById(R.id.todayDateTextView);
        //    holidayIndicatorTextView = rootView.findViewById(R.id.holiday_indicator_text_view);
        prayerNametextView = rootView.findViewById(R.id.prayerNametextView);
        prayerTimetextView = rootView.findViewById(R.id.prayerTimetextView);
        timeRemainingTextView = rootView.findViewById(R.id.timeRemainingTextView);
        circularProgressBar = rootView.findViewById(R.id.circularProgressBar);
        calculationMethodTextView = rootView.findViewById(R.id.calculation_method_text_view);

        qadaSummaryCard = rootView.findViewById(R.id.card_qada_summary);
        qadaCountTextView = rootView.findViewById(R.id.tv_total_qada_count);
        qadaProgressBar = rootView.findViewById(R.id.progress_qada_summary);

        if (qadaSummaryCard != null) {
            qadaSummaryCard.setOnClickListener(v -> onOutstandingQadaClicked());
        }

        applyDefaultQadaSummary();


        fajrTimingTextView = rootView.findViewById(R.id.fajr_timing_text_view);

        ImageView fajrCallImageView = rootView.findViewById(R.id.fajr_call_image_view);
        ConstraintLayout fajrCallConstraintLayout = rootView.findViewById(R.id.fajr_call_constraint_layout);
        initializeImageViewIcon(fajrCallConstraintLayout, fajrCallImageView, PrayerEnum.FAJR);

        ImageView dohrCallImageView = rootView.findViewById(R.id.dohr_call_image_view);
        ConstraintLayout dohrCallConstraintLayout = rootView.findViewById(R.id.dohr_call_constraint_layout);
        initializeImageViewIcon(dohrCallConstraintLayout, dohrCallImageView, PrayerEnum.DHOHR);

        ImageView asrCallImageView = rootView.findViewById(R.id.asr_call_image_view);
        ConstraintLayout asrCallConstraintLayout = rootView.findViewById(R.id.asr_call_constraint_layout);
        initializeImageViewIcon(asrCallConstraintLayout, asrCallImageView, PrayerEnum.ASR);

        ImageView maghrebCallImageView = rootView.findViewById(R.id.maghreb_call_image_view);
        ConstraintLayout maghrebCallConstraintLayout = rootView.findViewById(R.id.maghreb_call_constraint_layout);
        initializeImageViewIcon(maghrebCallConstraintLayout, maghrebCallImageView, PrayerEnum.MAGHRIB);

        ImageView ichaCallImageView = rootView.findViewById(R.id.icha_call_image_view);
        ConstraintLayout ichaCallConstraintLayout = rootView.findViewById(R.id.icha_call_constraint_layout);
        initializeImageViewIcon(ichaCallConstraintLayout, ichaCallImageView, PrayerEnum.ICHA);


        dohrTimingTextView = rootView.findViewById(R.id.dohr_timing_text_view);
        asrTimingTextView = rootView.findViewById(R.id.asr_timing_text_view);
        maghribTimingTextView = rootView.findViewById(R.id.maghreb_timing_text_view);
        ichaTimingTextView = rootView.findViewById(R.id.icha_timing_text_view);

        sunriseTimingTextView = rootView.findViewById(R.id.sunrise_timing_text_view);
        sunsetTimingTextView = rootView.findViewById(R.id.sunset_timing_text_view);

        fajrLabel = rootView.findViewById(R.id.fajr_label_text_view);
        dohrLabel = rootView.findViewById(R.id.dohr_label_text_view);
        asrLabel = rootView.findViewById(R.id.asr_label_text_view);
        maghribLabel = rootView.findViewById(R.id.maghrib_label_text_view);
        ichaLabel = rootView.findViewById(R.id.icha_label_text_view);
        
        // Feature buttons click listeners
        rootView.findViewById(R.id.btn_qibla_direction).setOnClickListener(v -> {
            // ⭐ 点击Qibla功能时检查位置权限
            if (checkLocationPermission()) {
                // 有权限，直接打开Qibla页面
                Log.d("PrayersFragment", "✅ Location permission granted, launching Qibla Direction");
                startActivity(new Intent(requireContext(), com.quran.quranaudio.online.compass.QiblaDirectionActivity.class));
            } else {
                // 没有权限，检查是否还能弹出权限请求
                int requestCount = getPermissionRequestCount();
                if (requestCount < MAX_PERMISSION_REQUESTS) {
                    Log.d("PrayersFragment", "⚠️ No location permission, showing permission dialog for Qibla feature (count: " + (requestCount + 1) + "/" + MAX_PERMISSION_REQUESTS + ")");
                    Toast.makeText(requireContext(), 
                        "Location permission is required to use Qibla Direction", 
                        Toast.LENGTH_SHORT).show();
                    showPermissionWarningAndIncrementCount();
                } else {
                    Log.d("PrayersFragment", "⚠️ Max permission requests reached, cannot show dialog");
                    Toast.makeText(requireContext(), 
                        "Please enable location permission in Settings to use Qibla Direction", 
                        Toast.LENGTH_LONG).show();
                }
            }
        });
        
        rootView.findViewById(R.id.btn_wudu_guide).setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), com.quran.quranaudio.online.wudu.WuduGuideActivity.class));
        });
        
        // Tasbih Counter Card click listener
        View tasbihCard = rootView.findViewById(R.id.tasbih_counter_card);
        com.google.android.material.button.MaterialButton btnGetStart = rootView.findViewById(R.id.btn_get_start);
        
        if (tasbihCard != null) {
            tasbihCard.setOnClickListener(v -> {
                Log.d("PrayersFragment", "📿 Tasbih card clicked");
                navigateToTasbihPage();
            });
        } else {
            Log.w("PrayersFragment", "⚠️ Tasbih card not found");
        }
        
        if (btnGetStart != null) {
            btnGetStart.setOnClickListener(v -> {
                Log.d("PrayersFragment", "📿 Get Start Now button clicked");
                navigateToTasbihPage();
            });
        } else {
            Log.w("PrayersFragment", "⚠️ Get Start Now button not found");
        }
        
        // Initialize Salah track buttons
        fajrTrackButton = rootView.findViewById(R.id.fajr_track_button);
        dhuhrTrackButton = rootView.findViewById(R.id.dhuhr_track_button);
        asrTrackButton = rootView.findViewById(R.id.asr_track_button);
        maghribTrackButton = rootView.findViewById(R.id.maghrib_track_button);
        ishaTrackButton = rootView.findViewById(R.id.isha_track_button);
        
        // Initialize status icons (ImageView) - using completed icon views for now
        fajrStatusIcon = rootView.findViewById(R.id.fajr_completed_icon);
        dhuhrStatusIcon = rootView.findViewById(R.id.dhuhr_completed_icon);
        asrStatusIcon = rootView.findViewById(R.id.asr_completed_icon);
        maghribStatusIcon = rootView.findViewById(R.id.maghrib_completed_icon);
        ishaStatusIcon = rootView.findViewById(R.id.isha_completed_icon);
        
        // Make status icons clickable
        if (fajrStatusIcon != null) {
            fajrStatusIcon.setClickable(true);
            fajrStatusIcon.setOnClickListener(v -> onSalahTrackClicked(SalahName.FAJR, fajrTrackButton));
        }
        if (dhuhrStatusIcon != null) {
            dhuhrStatusIcon.setClickable(true);
            dhuhrStatusIcon.setOnClickListener(v -> onSalahTrackClicked(SalahName.DHUHR, dhuhrTrackButton));
        }
        if (asrStatusIcon != null) {
            asrStatusIcon.setClickable(true);
            asrStatusIcon.setOnClickListener(v -> onSalahTrackClicked(SalahName.ASR, asrTrackButton));
        }
        if (maghribStatusIcon != null) {
            maghribStatusIcon.setClickable(true);
            maghribStatusIcon.setOnClickListener(v -> onSalahTrackClicked(SalahName.MAGHRIB, maghribTrackButton));
        }
        if (ishaStatusIcon != null) {
            ishaStatusIcon.setClickable(true);
            ishaStatusIcon.setOnClickListener(v -> onSalahTrackClicked(SalahName.ISHA, ishaTrackButton));
        }
        
        // Initialize PrayerLogRepository
        prayerLogRepository = new PrayerLogRepository();
    }
    
    /**
     * Initializes Salah recording feature.
     * Sets up click listeners and observes status updates.
     * Buttons are visible immediately (from XML) and update asynchronously.
     * 
     * KEY: XML已设置默认可见状态和"Track"样式，此处只负责：
     * 1. 设置点击监听器（始终显示按钮，未登录时点击触发登录）
     * 2. 观察Firebase数据并更新状态（仅限登录用户）
     */
    private void initializeSalahRecording() {
        Log.d("PrayersFragment", "🔧 Initializing Salah recording feature");
        
        // ✅ 按钮始终可见，登录状态在点击时检查
        if (fajrTrackButton != null) {
            fajrTrackButton.setOnClickListener(v -> onSalahTrackClicked(SalahName.FAJR, fajrTrackButton));
        }
        if (dhuhrTrackButton != null) {
            dhuhrTrackButton.setOnClickListener(v -> onSalahTrackClicked(SalahName.DHUHR, dhuhrTrackButton));
        }
        if (asrTrackButton != null) {
            asrTrackButton.setOnClickListener(v -> onSalahTrackClicked(SalahName.ASR, asrTrackButton));
        }
        if (maghribTrackButton != null) {
            maghribTrackButton.setOnClickListener(v -> onSalahTrackClicked(SalahName.MAGHRIB, maghribTrackButton));
        }
        if (ishaTrackButton != null) {
            ishaTrackButton.setOnClickListener(v -> onSalahTrackClicked(SalahName.ISHA, ishaTrackButton));
        }
        
        // ✅ 只在登录时观察Firebase数据
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            Log.d("PrayersFragment", "✅ User logged in, starting to observe Salah records");
            startObservingSalahRecords();
        } else {
            Log.d("PrayersFragment", "ℹ️ User not logged in, buttons will show login dialog on click");
        }
    }
    
    /**
     * Handles click on a salah track button or status icon.
     * 根据当前状态执行不同操作
     */
    private void onSalahTrackClicked(SalahName salahName, MaterialButton button) {
        Log.d("PrayersFragment", "🔘 Prayer clicked: " + salahName.getDisplayName());
        
        // ✅ 【修复】如果用户未登录，尝试自动匿名登录
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Log.w("PrayersFragment", "⚠️ User not logged in, attempting automatic anonymous sign-in...");
            ensureUserAuthenticated(new Runnable() {
                @Override
                public void run() {
                    // 登录成功后，重新执行点击逻辑
                    handleSalahTrackClick(salahName, button);
                }
            });
            return;
        }
        
        handleSalahTrackClick(salahName, button);
    }
    
    /**
     * 实际的祷告点击处理逻辑（从 onSalahTrackClicked 中提取）
     */
    private void handleSalahTrackClick(SalahName salahName, MaterialButton button) {
        String prayerName = salahName.getDisplayName();
        PrayerLog existingLog = todayPrayerLogs.get(prayerName);
        
        if (existingLog == null) {
            // Pending state: Show new log dialog (default to Ada')
            Log.d("PrayersFragment", "📝 Pending state - showing new log dialog (default: Ada')");
            showPrayerLogBottomSheet(prayerName, null, PrayerLog.PrayerStatus.ADA);
        } else {
            // Has existing log: Check status
            PrayerLog.PrayerStatus status = existingLog.getStatus();
            if (status == PrayerLog.PrayerStatus.ADA) {
                // Ada': Edit mode (can change to Qada')
                Log.d("PrayersFragment", "✅ Ada' state - showing edit dialog");
                showPrayerLogBottomSheet(prayerName, existingLog.getId(), null);
            } else if (status == PrayerLog.PrayerStatus.QADA) {
                // Qada': Edit mode (can modify time/notes)
                Log.d("PrayersFragment", "⚠️ Qada' state - showing edit dialog");
                showPrayerLogBottomSheet(prayerName, existingLog.getId(), null);
            } else if (status == PrayerLog.PrayerStatus.MISSED) {
                // Missed: Create Qada' log (default to Qada' status)
                Log.d("PrayersFragment", "❌ Missed state - showing Qada' log dialog");
                showPrayerLogBottomSheet(prayerName, null, PrayerLog.PrayerStatus.QADA);
            }
        }
    }
    
    /**
     * 确保用户已认证（自动匿名登录）
     */
    private void ensureUserAuthenticated(Runnable onSuccess) {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            // 已登录
            onSuccess.run();
            return;
        }
        
        // 尝试匿名登录
        Log.d("PrayersFragment", "🔓 Attempting anonymous sign-in...");
        
        if (googleAuthManager == null) {
            Log.e("PrayersFragment", "❌ GoogleAuthManager is null, cannot authenticate");
            showErrorToast("Authentication service unavailable");
            return;
        }
        
        googleAuthManager.signInAnonymously(new com.quran.quranaudio.online.Utils.GoogleAuthManager.AuthCallback() {
            @Override
            public void onSuccess(com.google.firebase.auth.FirebaseUser user) {
                Log.d("PrayersFragment", "✅ Anonymous sign-in successful: " + user.getUid());
                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(onSuccess);
                }
            }
            
            @Override
            public void onFailure(String error) {
                Log.e("PrayersFragment", "❌ Anonymous sign-in failed: " + error);
                if (isAdded() && getContext() != null) {
                    showErrorToast("Failed to authenticate: " + error);
                }
            }
        });
    }
    
    /**
     * 显示错误提示
     */
    private void showErrorToast(String message) {
        if (isAdded() && getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 显示祷告记录 Bottom Sheet
     * @param prayerName 祷告名称
     * @param existingLogId 现有记录 ID（编辑模式），null 表示新建
     * @param initialStatus 初始状态（用于 Missed -> Qada 转换）
     */
    private void showPrayerLogBottomSheet(String prayerName, String existingLogId, PrayerLog.PrayerStatus initialStatus) {
        // 🔥 修复崩溃：检查 Fragment 状态，避免在 onSaveInstanceState 后执行 Fragment 事务
        if (!isAdded()) {
            Log.w("PrayersFragment", "⚠️ Cannot show bottom sheet: Fragment not added");
            return;
        }
        
        // 🔥 检查 FragmentManager 状态
        if (getChildFragmentManager().isStateSaved()) {
            Log.w("PrayersFragment", "⚠️ Cannot show bottom sheet: FragmentManager state already saved");
            return;
        }
        
        com.quran.quranaudio.online.prayertimes.ui.PrayerLogBottomSheet bottomSheet;
        
        if (existingLogId != null) {
            // Edit mode
            bottomSheet = com.quran.quranaudio.online.prayertimes.ui.PrayerLogBottomSheet.Companion.newInstanceForEdit(prayerName, existingLogId);
        } else {
            // New mode
            bottomSheet = com.quran.quranaudio.online.prayertimes.ui.PrayerLogBottomSheet.Companion.newInstance(prayerName, initialStatus, null);
        }
        
        try {
            bottomSheet.show(getChildFragmentManager(), "PrayerLogBottomSheet");
        } catch (IllegalStateException e) {
            // 🔥 额外保护：即使检查通过，仍可能在极端情况下失败（极端边界条件）
            Log.e("PrayersFragment", "❌ Failed to show bottom sheet: " + e.getMessage());
        }
    }
    
    /**
     * @deprecated No longer needed with anonymous login support
     * Shows login dialog when unauthenticated user clicks Track button
     */
    @Deprecated
    private void showLoginDialog(SalahName salahName, MaterialButton button) {
        // 已弃用 - 应用现在支持匿名登录，不再强制要求 Google 登录
        // 如果需要提示用户升级账户，请使用 AccountUpgradeDialog
        Log.w("PrayersFragment", "⚠️ showLoginDialog() is deprecated - using anonymous auth instead");
    }
    
    /**
     * @deprecated No longer needed with anonymous login support
     * Shows generic login dialog (for Qada tracker access)
     */
    @Deprecated
    private void showGenericLoginDialog() {
        // 已弃用 - 应用现在支持匿名登录，不再强制要求 Google 登录
        Log.w("PrayersFragment", "⚠️ showGenericLoginDialog() is deprecated - using anonymous auth instead");
    }
    
    /**
     * Initiates Google Sign-In flow
     */
    private void initiateGoogleSignIn() {
        try {
            if (!isAdded() || getContext() == null) {
                Log.w("PrayersFragment", "Fragment not attached, cannot initiate sign-in");
                return;
            }
            
            if (googleAuthManager == null) {
                Log.e("PrayersFragment", "GoogleAuthManager is null!");
                Toast.makeText(requireContext(), "Login is not available", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (signInLauncher == null) {
                Log.e("PrayersFragment", "SignInLauncher is null!");
                Toast.makeText(requireContext(), "Login is not available", Toast.LENGTH_SHORT).show();
                return;
            }
            
            Intent signInIntent = googleAuthManager.getSignInIntent();
            if (signInIntent == null) {
                Log.e("PrayersFragment", "Sign-in intent is null!");
                Toast.makeText(requireContext(), "Failed to create sign-in intent", Toast.LENGTH_SHORT).show();
                return;
            }
            
            signInLauncher.launch(signInIntent);
            Log.d("PrayersFragment", "Google Sign-In intent launched successfully");
        } catch (Exception e) {
            Log.e("PrayersFragment", "Failed to launch Google Sign-In", e);
            if (isAdded() && getContext() != null) {
                Toast.makeText(requireContext(), "Failed to launch sign-in: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    /**
     * Handles the result from Google Sign-In
     */
    private void handleSignInResult(Intent data) {
        if (!isAdded() || getContext() == null) {
            Log.w("PrayersFragment", "Fragment not attached, cannot handle sign-in result");
            return;
        }
        
        if (googleAuthManager == null) {
            Log.e("PrayersFragment", "GoogleAuthManager is null, cannot handle sign-in result");
            Toast.makeText(requireContext(), "Login system unavailable", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (data == null) {
            Log.w("PrayersFragment", "Sign-in data is null");
            Toast.makeText(requireContext(), "Login failed: No data received", Toast.LENGTH_SHORT).show();
            return;
        }
        
        googleAuthManager.handleSignInResult(data, new GoogleAuthManager.AuthCallback() {
            @Override
            public void onSuccess(com.google.firebase.auth.FirebaseUser user) {
                Log.d("PrayersFragment", "Firebase authentication successful: " + (user != null ? user.getEmail() : "unknown"));
                if (isAdded() && getContext() != null) {
                    Toast.makeText(requireContext(), "Login successful! ✅", Toast.LENGTH_SHORT).show();
                    
                    // Start observing salah records now that user is logged in
                    startObservingSalahRecords();
                }
            }
            
            @Override
            public void onFailure(String error) {
                Log.e("PrayersFragment", "Firebase authentication failed: " + error);
                if (isAdded() && getContext() != null) {
                    Toast.makeText(requireContext(), "Login failed: " + error, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    
    /**
     * Starts observing salah records from Firebase.
     * Called after successful login or when fragment is created for logged-in users.
     */
    private void startObservingSalahRecords() {
        if (salahViewModel == null || prayerLogRepository == null) return;
        
        // Load prayer logs and update UI
        loadTodayPrayerLogs();
        
        // Still observe salah records for backwards compatibility
        salahViewModel.getTodaySalahRecord().observe(getViewLifecycleOwner(), record -> {
            Log.d("PrayersFragment", "📝 Salah record received: " + (record != null ? record.getTotalCompleted() + "/5 completed" : "null"));
            // Note: We now use prayer_logs collection instead
        });
    }
    
    /**
     * Load today's prayer logs and update UI
     */
    private void loadTodayPrayerLogs() {
        Log.d("PrayersFragment", "🔍 loadTodayPrayerLogs() called");
        
        if (prayerLogRepository == null) {
            Log.e("PrayersFragment", "❌ PrayerLogRepository is null!");
            return;
        }
        
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Log.d("PrayersFragment", "ℹ️ User not logged in, resetting to Pending");
            resetAllPrayersToPending();
            applyDefaultQadaSummary();
            return;
        }
        
        Log.d("PrayersFragment", "📡 Querying prayer logs from Firestore...");
        
        // Load all prayer logs at once using callback
        prayerLogRepository.getTodayPrayerLogsAsync(new PrayerLogRepository.PrayerLogsCallback() {
            @Override
            public void onResult(java.util.Map<String, PrayerLog> logs) {
                Log.d("PrayersFragment", "📥 Callback received with " + logs.size() + " logs");
                
                if (getActivity() == null) {
                    Log.w("PrayersFragment", "⚠️ Activity is null, cannot update UI");
                    return;
                }
                
                getActivity().runOnUiThread(() -> {
                    Log.d("PrayersFragment", "🔄 Updating UI on main thread");
                    todayPrayerLogs.clear();
                    todayPrayerLogs.putAll(logs);
                    
                    // Log all received logs
                    for (java.util.Map.Entry<String, PrayerLog> entry : logs.entrySet()) {
                        Log.d("PrayersFragment", "  📝 " + entry.getKey() + " -> " + entry.getValue().getStatus());
                    }
                    
                    // Update UI for each prayer
                    updatePrayerStatusUI(SalahName.FAJR, logs.get("Fajr"));
                    updatePrayerStatusUI(SalahName.DHUHR, logs.get("Dhuhr"));
                    updatePrayerStatusUI(SalahName.ASR, logs.get("Asr"));
                    updatePrayerStatusUI(SalahName.MAGHRIB, logs.get("Maghrib"));
                    updatePrayerStatusUI(SalahName.ISHA, logs.get("Isha"));
                    
                    Log.d("PrayersFragment", "✅ UI update completed");

                    loadQadaSummary();
                });
            }
        });
    }
    
    /**
     * Reset all prayers to Pending state (show Track buttons)
     */
    private void resetAllPrayersToPending() {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            updatePrayerStatusUI(SalahName.FAJR, null);
            updatePrayerStatusUI(SalahName.DHUHR, null);
            updatePrayerStatusUI(SalahName.ASR, null);
            updatePrayerStatusUI(SalahName.MAGHRIB, null);
            updatePrayerStatusUI(SalahName.ISHA, null);
        });
    }

    private void loadQadaSummary() {
        if (!isAdded()) {
            return;
        }

        if (prayerLogRepository == null) {
            Log.e("PrayersFragment", "❌ PrayerLogRepository is null when loading Qada summary");
            return;
        }

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            applyDefaultQadaSummary();
            return;
        }

        Log.d("PrayersFragment", "📊 Loading Qada summary with actual prayer times");
        // Pass current prayer times to ensure consistent calculation with Qada Tracker
        prayerLogRepository.getQadaSummaryAsync(currentDayPrayer, new PrayerLogRepository.QadaSummaryCallback() {
            @Override
            public void onResult(PrayerLogRepository.QadaSummary summary) {
                if (!isAdded() || getActivity() == null) {
                    return;
                }

                getActivity().runOnUiThread(() -> updateQadaSummaryUI(summary));
            }
        });
    }

    private void updateQadaSummaryUI(PrayerLogRepository.QadaSummary summary) {
        if (!isAdded()) {
            return;
        }

        if (qadaCountTextView == null || qadaProgressBar == null) {
            return;
        }

        int outstanding = 0;
        int completed = 0;

        if (summary != null) {
            outstanding = Math.max(0, summary.getOutstandingCount());
            completed = Math.max(0, summary.getCompletedCount());
        }

        int total = Math.max(outstanding + completed, 0);
        
        // 🔍 诊断日志：Salat 页面的 Qada 计数
        android.util.Log.d("QadaDiagnosis", "═══════════════════════════════════════════════");
        android.util.Log.d("QadaDiagnosis", "📊 【统一计算规则】Salat Page - Total Qada Count");
        android.util.Log.d("QadaDiagnosis", "   ✅ Calculation Source: PrayerLogRepository.getQadaSummary()");
        android.util.Log.d("QadaDiagnosis", "   ❌ Outstanding (Missed+Pending): " + outstanding);
        android.util.Log.d("QadaDiagnosis", "   ✅ Completed (Qada'): " + completed);
        android.util.Log.d("QadaDiagnosis", "   🔢 Total: " + total);
        android.util.Log.d("QadaDiagnosis", "   📌 This is the same calculation used by QadaTracker");
        android.util.Log.d("QadaDiagnosis", "═══════════════════════════════════════════════");

        if (outstanding > 0) {
            String formatted = NumberFormat.getIntegerInstance().format(outstanding);
            String displayText = getString(R.string.qada_count_prayers, formatted);
            qadaCountTextView.setText(displayText);
            qadaCountTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.qada_alert_red));
        } else {
            qadaCountTextView.setText(getString(R.string.qada_count_zero));
            qadaCountTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.bottom_nav_selected));
        }

        int progressValue = 0;
        if (outstanding == 0 && total > 0) {
            // Qada' 总数为 0 时，显示 100% 绿色进度条 (Alhamdulillah!)
            progressValue = 100;
            qadaProgressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.qada_progress_complete)
            ));
        } else if (total > 0) {
            progressValue = Math.round((completed * 100f) / total);
            progressValue = Math.max(0, Math.min(100, progressValue));
            qadaProgressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.qada_progress_fill)
            ));
        } else {
            // No qada start date configured or no prayers yet
            qadaProgressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.qada_progress_fill)
            ));
        }

        qadaProgressBar.setProgress(progressValue);
    }

    private void applyDefaultQadaSummary() {
        if (qadaCountTextView != null) {
            CharSequence zeroText = qadaCountTextView.getResources().getString(R.string.qada_count_zero);
            qadaCountTextView.setText(zeroText);
            Context context = qadaCountTextView.getContext();
            if (context != null) {
                qadaCountTextView.setTextColor(ContextCompat.getColor(context, R.color.bottom_nav_selected));
            }
        }

        if (qadaProgressBar != null) {
            qadaProgressBar.setProgress(0);
        }
    }

    private void onOutstandingQadaClicked() {
        if (!isAdded()) {
            return;
        }

        Log.d("PrayersFragment", "📊 Outstanding Qada card clicked");

        // ✅ 【修复】如果用户未登录，尝试自动匿名登录
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Log.w("PrayersFragment", "⚠️ User not logged in, attempting automatic anonymous sign-in...");
            ensureUserAuthenticated(new Runnable() {
                @Override
                public void run() {
                    // 登录成功后，继续执行 Qada 逻辑
                    proceedToQadaTracker();
                }
            });
            return;
        }

        proceedToQadaTracker();
    }
    
    /**
     * 继续执行 Qada Tracker 逻辑（从 onOutstandingQadaClicked 中提取）
     */
    private void proceedToQadaTracker() {
        // Show loading feedback to user
        if (getContext() != null) {
            android.widget.Toast.makeText(getContext(), 
                getString(R.string.loading), 
                android.widget.Toast.LENGTH_SHORT).show();
        }

        // Check if user has configured Qada start date
        checkAndShowQadaOnboarding();
    }
    
    /**
     * Check if user has configured Qada start date
     * If not, show onboarding dialog
     */
    private void checkAndShowQadaOnboarding() {
        if (prayerLogRepository == null) {
            return;
        }
        
        kotlinx.coroutines.CoroutineScope scope = kotlinx.coroutines.CoroutineScopeKt.CoroutineScope(
            kotlinx.coroutines.Dispatchers.getIO()
        );
        
        prayerLogRepository.getQadaStartDateAsync(new PrayerLogRepository.QadaStartDateCallback() {
            @Override
            public void onSuccess(String startDate) {
                if (getActivity() == null) return;
                
                getActivity().runOnUiThread(() -> {
                    if (startDate == null || startDate.isEmpty()) {
                        // Show onboarding dialog
                        Log.d("PrayersFragment", "📅 No Qada start date configured, showing onboarding");
                        showQadaOnboardingDialog();
                    } else {
                        // Already configured, open Qada Tracker Activity
                        Log.d("PrayersFragment", "📅 Qada start date already configured: " + startDate);
                        openQadaTrackerActivity();
                    }
                });
            }
            
            @Override
            public void onError(Exception e) {
                Log.e("PrayersFragment", "Error checking Qada start date", e);
            }
        });
    }
    
    /**
     * Open Qada Tracker Activity
     */
    private void openQadaTrackerActivity() {
        Intent intent = new Intent(requireContext(), com.quran.quranaudio.online.prayertimes.ui.QadaTrackerActivity.class);
        
        // ✅ 传递今天的祷告时间数据，用于精确的祷告窗口判断
        if (currentDayPrayer != null && currentDayPrayer.getTimings() != null) {
            Log.d("PrayersFragment", "📤 Passing today's prayer times to QadaTracker");
            // 传递序列化的祷告时间数据
            java.util.Map<com.quran.quranaudio.online.prayertimes.common.PrayerEnum, java.time.LocalDateTime> timings = currentDayPrayer.getTimings();
            if (timings != null) {
                for (java.util.Map.Entry<com.quran.quranaudio.online.prayertimes.common.PrayerEnum, java.time.LocalDateTime> entry : timings.entrySet()) {
                    if (entry.getValue() != null) {
                        String key = "prayer_time_" + entry.getKey().name();
                        String value = entry.getValue().toString();
                        intent.putExtra(key, value);
                        Log.d("PrayersFragment", "   " + entry.getKey() + ": " + value);
                    }
                }
            }
        } else {
            Log.w("PrayersFragment", "⚠️ currentDayPrayer or timings is null, QadaTracker will use fallback times");
        }
        
        startActivity(intent);
    }
    
    /**
     * Show Qada onboarding dialog
     */
    private void showQadaOnboardingDialog() {
        if (!isAdded() || getContext() == null) {
            return;
        }
        
        com.quran.quranaudio.online.prayertimes.ui.QadaOnboardingDialog dialog = 
            new com.quran.quranaudio.online.prayertimes.ui.QadaOnboardingDialog(
                requireContext(),
                startDate -> {
                    Log.d("PrayersFragment", "✅ Qada start date configured: " + startDate);
                    // Reload Qada summary
                    loadQadaSummary();
                    // Open Qada Tracker Activity
                    openQadaTrackerActivity();
                    return null;
                }
            );
        
        dialog.show();
    }
    
    /**
     * Updates prayer status UI based on prayer log
     * @param salahName The prayer name
     * @param log The prayer log (null means Pending)
     */
    private void updatePrayerStatusUI(SalahName salahName, PrayerLog log) {
        Log.d("PrayersFragment", "🎨 updatePrayerStatusUI called for " + salahName + ", log=" + (log != null ? log.getStatus() : "null"));
        
        MaterialButton button = getTrackButton(salahName);
        ImageView statusIcon = getStatusIcon(salahName);
        
        if (button == null || statusIcon == null) {
            Log.e("PrayersFragment", "❌ Button or icon is null for " + salahName + " (button=" + button + ", icon=" + statusIcon + ")");
            return;
        }
        
        Log.d("PrayersFragment", "  📍 Button visibility before: " + (button.getVisibility() == android.view.View.VISIBLE ? "VISIBLE" : "GONE"));
        Log.d("PrayersFragment", "  📍 Icon visibility before: " + (statusIcon.getVisibility() == android.view.View.VISIBLE ? "VISIBLE" : "GONE"));
        
        if (log == null) {
            // No log: Check if prayer time has passed
            boolean isPrayerTimePassed = isPrayerTimePassed(salahName);
            
            if (isPrayerTimePassed) {
                ensureMissedLogRecorded(salahName);
                // Prayer time has passed without logging: Show as Missed ❌
                button.setVisibility(android.view.View.GONE);
                statusIcon.setVisibility(android.view.View.VISIBLE);
                statusIcon.setImageResource(R.drawable.ic_error);
                statusIcon.setColorFilter(0xFFF44336, android.graphics.PorterDuff.Mode.SRC_IN); // Red
                Log.d("PrayersFragment", "❌ " + salahName + ": Missed (time passed, no log) - UPDATED");
            } else {
                // Prayer time not yet passed: Show Track button (Pending)
                button.setVisibility(android.view.View.VISIBLE);
                statusIcon.setVisibility(android.view.View.GONE);
                Log.d("PrayersFragment", "📝 " + salahName + ": Pending (Track button) - UPDATED");
            }
        } else {
            // Has log: Hide button, show appropriate icon
            button.setVisibility(android.view.View.GONE);
            statusIcon.setVisibility(android.view.View.VISIBLE);
            
            // Set icon based on status
            PrayerLog.PrayerStatus status = log.getStatus();
            if (status == PrayerLog.PrayerStatus.ADA) {
                // Ada' (准时完成): Green check circle ✅
                statusIcon.setImageResource(R.drawable.ic_check_circle);
                statusIcon.setColorFilter(null); // Clear any color filter (icon has built-in color)
                Log.d("PrayersFragment", "✅ " + salahName + ": Ada' (green check circle) - UPDATED");
            } else if (status == PrayerLog.PrayerStatus.QADA) {
                // Qada' (已弥补): Orange warning ⚠️
                statusIcon.setImageResource(R.drawable.ic_warning);
                statusIcon.setColorFilter(0xFFFF9800, android.graphics.PorterDuff.Mode.SRC_IN); // Orange
                Log.d("PrayersFragment", "⚠️ " + salahName + ": Qada' (orange warning) - UPDATED");
            } else if (status == PrayerLog.PrayerStatus.MISSED) {
                // Missed (错过): Red error ❌
                statusIcon.setImageResource(R.drawable.ic_error);
                statusIcon.setColorFilter(0xFFF44336, android.graphics.PorterDuff.Mode.SRC_IN); // Red
                Log.d("PrayersFragment", "❌ " + salahName + ": Missed (red error) - UPDATED");
            }
            
            // Log icon resource for debugging
            try {
                String resourceName = getResources().getResourceEntryName(statusIcon.getDrawable().getConstantState().hashCode());
                Log.d("PrayersFragment", "  🎨 Icon resource: " + resourceName);
            } catch (Exception e) {
                Log.d("PrayersFragment", "  🎨 Icon resource: [unable to determine]");
            }
        }
        
        Log.d("PrayersFragment", "  📍 Button visibility after: " + (button.getVisibility() == android.view.View.VISIBLE ? "VISIBLE" : "GONE"));
        Log.d("PrayersFragment", "  📍 Icon visibility after: " + (statusIcon.getVisibility() == android.view.View.VISIBLE ? "VISIBLE" : "GONE"));
    }
    
    /**
     * Check if prayer time has passed
     * Returns true if the prayer time has passed and the next prayer's time has also passed
     */
    private boolean isPrayerTimePassed(SalahName salahName) {
        if (currentDayPrayer == null) {
            Log.d("PrayersFragment", "⚠️ isPrayerTimePassed(" + salahName + "): currentDayPrayer is null, returning false");
            return false;
        }
        
        try {
            LocalDateTime prayerTime = getPrayerTime(salahName);
            if (prayerTime == null) {
                Log.d("PrayersFragment", "⚠️ isPrayerTimePassed(" + salahName + "): prayerTime is null, returning false");
                return false;
            }

            ZoneId zoneId = resolveZoneId();
            ZonedDateTime now = ZonedDateTime.now(zoneId);
            ZonedDateTime prayerDateTime = prayerTime.atZone(zoneId);

            boolean hasPassed = !now.isBefore(prayerDateTime);

            Log.d(
                "PrayersFragment",
                "✅ isPrayerTimePassed(" + salahName + "): " + hasPassed +
                    " (now=" + now + ", prayer=" + prayerDateTime + ")"
            );

            return hasPassed;
            
        } catch (Exception e) {
            Log.e("PrayersFragment", "❌ Error in isPrayerTimePassed: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Get the LocalDateTime for a specific prayer
     */
    private LocalDateTime getPrayerTime(SalahName salahName) {
        if (currentDayPrayer == null || currentDayPrayer.getTimings() == null) {
            return null;
        }
        
        Map<PrayerEnum, LocalDateTime> timings = currentDayPrayer.getTimings();
        
        switch (salahName) {
            case FAJR:
                return timings.get(PrayerEnum.FAJR);
            case DHUHR:
                return timings.get(PrayerEnum.DHOHR);
            case ASR:
                return timings.get(PrayerEnum.ASR);
            case MAGHRIB:
                return timings.get(PrayerEnum.MAGHRIB);
            case ISHA:
                return timings.get(PrayerEnum.ICHA);
            default:
                return null;
        }
    }

    private ZoneId resolveZoneId() {
        if (currentDayPrayer != null && currentDayPrayer.getTimezone() != null) {
            try {
                return ZoneId.of(currentDayPrayer.getTimezone());
            } catch (Exception ignored) {
                Log.w("PrayersFragment", "⚠️ Invalid timezone from DayPrayer: " + currentDayPrayer.getTimezone());
            }
        }
        return ZoneId.systemDefault();
    }

    private void ensureMissedLogRecorded(SalahName salahName) {
        if (prayerLogRepository == null || FirebaseAuth.getInstance().getCurrentUser() == null) {
            return;
        }

        String prayerName = salahName.getDisplayName();
        if (autoMissInProgress.contains(prayerName)) {
            return;
        }

        autoMissInProgress.add(prayerName);

        String todayDate = getTodayDateString();
        prayerLogRepository.markPrayerAsMissedIfNeededAsync(prayerName, todayDate, new PrayerLogRepository.MarkMissedCallback() {
            @Override
            public void onComplete(boolean created) {
                autoMissInProgress.remove(prayerName);
                Log.d("PrayersFragment", "markPrayerAsMissedIfNeededAsync completed for " + prayerName + ", created=" + created);
                if (created) {
                    loadTodayPrayerLogs();
                }
            }

            @Override
            public void onError(Exception e) {
                autoMissInProgress.remove(prayerName);
                Log.e("PrayersFragment", "Failed to auto-mark " + prayerName + " as Missed", e);
            }
        });
    }

    private String getTodayDateString() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return java.time.LocalDate.now().toString();
        } else {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            return formatter.format(new Date());
        }
    }
    
    /**
     * Get the next prayer after the given prayer
     */
    private SalahName getNextPrayer(SalahName current) {
        switch (current) {
            case FAJR:
                return SalahName.DHUHR;
            case DHUHR:
                return SalahName.ASR;
            case ASR:
                return SalahName.MAGHRIB;
            case MAGHRIB:
                return SalahName.ISHA;
            case ISHA:
                return null; // Last prayer of the day
            default:
                return null;
        }
    }
    
    /**
     * Get track button for a prayer
     */
    private MaterialButton getTrackButton(SalahName salahName) {
        switch (salahName) {
            case FAJR: return fajrTrackButton;
            case DHUHR: return dhuhrTrackButton;
            case ASR: return asrTrackButton;
            case MAGHRIB: return maghribTrackButton;
            case ISHA: return ishaTrackButton;
            default: return null;
        }
    }
    
    /**
     * Get status icon for a prayer
     */
    private ImageView getStatusIcon(SalahName salahName) {
        switch (salahName) {
            case FAJR: return fajrStatusIcon;
            case DHUHR: return dhuhrStatusIcon;
            case ASR: return asrStatusIcon;
            case MAGHRIB: return maghribStatusIcon;
            case ISHA: return ishaStatusIcon;
            default: return null;
        }
    }
    
    /**
     * Updates the track button and completed icon visibility based on completion status.
     * ✅ Completed: Hide Track button, show ic_correct.png ImageView
     * ⭕ Not completed: Show "Track" button, hide completed icon
     * @deprecated Use updatePrayerStatusUI instead
     */
    @Deprecated
    private void updateTrackButton(SalahName salahName, MaterialButton button, ImageView completedIcon, boolean isCompleted) {
        // Legacy method - kept for backwards compatibility
        if (button == null || completedIcon == null) {
            Log.w("PrayersFragment", "⚠️ Button or icon is null, cannot update");
            return;
        }

        if (isCompleted) {
            button.setVisibility(android.view.View.GONE);
            completedIcon.setVisibility(android.view.View.VISIBLE);
        } else {
            button.setVisibility(android.view.View.VISIBLE);
            completedIcon.setVisibility(android.view.View.GONE);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void updateTimingsTextViews(DayPrayer dayPrayer) {
        Map<PrayerEnum, LocalDateTime> timings = dayPrayer.getTimings();

        LocalDateTime fajrTiming = timings.get(PrayerEnum.FAJR);
        LocalDateTime dohrTiming = timings.get(PrayerEnum.DHOHR);
        LocalDateTime asrTiming = timings.get(PrayerEnum.ASR);
        LocalDateTime maghribTiming = timings.get(PrayerEnum.MAGHRIB);
        LocalDateTime ichaTiming = timings.get(PrayerEnum.ICHA);


        fajrTimingTextView.setText(UiUtils.formatTiming(fajrTiming));
        dohrTimingTextView.setText(UiUtils.formatTiming(dohrTiming));
        asrTimingTextView.setText(UiUtils.formatTiming(asrTiming));
        maghribTimingTextView.setText(UiUtils.formatTiming(maghribTiming));
        ichaTimingTextView.setText(UiUtils.formatTiming(ichaTiming));

        LocalDateTime sunriseTiming = dayPrayer.getComplementaryTiming().get(ComplementaryTimingEnum.SUNRISE);
        LocalDateTime sunsetTiming = dayPrayer.getComplementaryTiming().get(ComplementaryTimingEnum.SUNSET);

        sunriseTimingTextView.setText(UiUtils.formatTiming(Objects.requireNonNull(sunriseTiming)));
        sunsetTimingTextView.setText(UiUtils.formatTiming(Objects.requireNonNull(sunsetTiming)));

        fajrLabel.setText(R.string.FAJR);
        dohrLabel.setText(R.string.DHOHR);
        asrLabel.setText(R.string.ASR);
        maghribLabel.setText(R.string.MAGHRIB);
        ichaLabel.setText(R.string.ICHA);
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    private void updateNextPrayerViews(DayPrayer dayPrayer) {
        // Check if fragment is still attached to avoid crashes
        if (!isAdded() || getContext() == null) {
            return;
        }
        
        Map<PrayerEnum, LocalDateTime> timings = dayPrayer.getTimings();

        PrayerEnum nextPrayerKey = PrayerUtils.getNextPrayer(timings, LocalDateTime.now());
        PrayerEnum previousPrayerKey = PrayerUtils.getPreviousPrayerKey(nextPrayerKey);

        long timeRemaining = TimingUtils.getTimeBetweenTwoPrayer(todayDate, Objects.requireNonNull(timings.get(nextPrayerKey)));
        long timeBetween = TimingUtils.getTimeBetweenTwoPrayer(Objects.requireNonNull(timings.get(previousPrayerKey)), Objects.requireNonNull(timings.get(nextPrayerKey)));

        String prayerName = requireContext().getResources().getString(
                getResources().getIdentifier(nextPrayerKey.toString(), "string", requireContext().getPackageName()));

        prayerNametextView.setText(prayerName);
        prayerTimetextView.setText(UiUtils.formatTiming(Objects.requireNonNull(timings.get(nextPrayerKey))));
        timeRemainingTextView.setText(getString(R.string.remaining) + ": " + UiUtils.formatTimeForTimer(timeRemaining));

        startAnimationTimer(timeRemaining, timeBetween, dayPrayer);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @SuppressLint("ClickableViewAccessibility")
    private void updateDatesTextViews(DayPrayer dayPrayer) {
        //holidayIndicatorTextView.setVisibility(View.INVISIBLE);

        // 🌐 使用应用设置的语言，而非系统语言
        String appLanguageCode = com.quran.quranaudio.online.quran_module.utils.sharedPrefs.SPAppConfigs.getLocale(requireContext());
        Locale appLocale = new Locale(appLanguageCode);
        
        ZonedDateTime zonedDateTime = TimingUtils.getZonedDateTimeFromTimestamps(dayPrayer.getTimestamp(), dayPrayer.getTimezone());
        String nameOfTheDay = zonedDateTime.getDayOfWeek().getDisplayName(TextStyle.FULL, appLocale);

        String hijriMonth = requireContext().getResources().getString(
                getResources().getIdentifier("hijri_month_" + dayPrayer.getHijriMonthNumber(), "string", requireContext().getPackageName()));

        String hijriDate = UiUtils.formatFullHijriDate(
                nameOfTheDay,
                dayPrayer.getHijriDay(),
                hijriMonth,
                dayPrayer.getHijriYear()
        );

        String gregorianDate = UiUtils.formatReadableGregorianDate(zonedDateTime);
        String timezone = UiUtils.formatReadableTimezone(zonedDateTime);

        todayDateTextView.setText(StringUtils.capitalize(hijriDate));

        todayDateTextView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (todayDateTextView.getText().equals(StringUtils.capitalize(hijriDate))) {
                    todayDateTextView.setText(StringUtils.capitalize(gregorianDate));
                } else {
                    todayDateTextView.setText(StringUtils.capitalize(hijriDate));
                }
            }
            return false;
        });

        String locationText;
        if (dayPrayer.getCity() != null) {
            locationText = StringUtils.capitalize(dayPrayer.getCity());
        } else {
            locationText = getString(R.string.common_offline);
        }

        if (dayPrayer.getCountry() != null) {
            locationText += StringUtils.capitalize(" - " + dayPrayer.getCountry() + " (" + timezone + ")");
        } else {
            locationText += StringUtils.capitalize(" (" + timezone + ")");
        }

        locationTextView.setText(locationText);

        String methodKey = String.valueOf(dayPrayer.getCalculationMethodEnum()).toLowerCase();
        String fajrAngle = dayPrayer.getCalculationMethodEnum().getFajrAngle();
        String ichaAngle = dayPrayer.getCalculationMethodEnum().getIchaAngle();
        boolean isIchaAngleInMinute = dayPrayer.getCalculationMethodEnum().isIchaAngleInMinute();
        String tooltipText = formatCalculationMethodAngle(fajrAngle, ichaAngle, isIchaAngleInMinute);


        int id = getResources().getIdentifier("short_method_" + methodKey, "string", requireContext().getPackageName());

        if (id != 0) {
            String methodName = getResources().getString(id);
            calculationMethodTextView.setText(methodName);
        }

        TypedArray typedArray = requireContext().getTheme().obtainStyledAttributes(R.styleable.tooltipStyle);
        int toolTipBackgroundColor = typedArray.getColor(R.styleable.tooltipStyle_tooltipBackgroundColor, ContextCompat.getColor(requireContext(), R.color.alabaster));
        int toolTipTextColor = typedArray.getColor(R.styleable.tooltipStyle_tooltipTextColor, ContextCompat.getColor(requireContext(), R.color.mine_shaft));
        typedArray.recycle();

        calculationMethodTextView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                Tooltip.on(calculationMethodTextView)
                        .text(tooltipText)
                        .textColor(toolTipTextColor)
                        .textSize(13)
                        .color(toolTipBackgroundColor)
                        .border(toolTipTextColor, 1f)
                        .clickToHide(true)
                        .arrowSize(0, 0)
                        .corner(10)
                        .position(Position.END)
                        .show(5000);
            }
            return false;
        });

//        HijriHoliday holiday = HijriHoliday.getHoliday(dayPrayer.getHijriDay(), dayPrayer.getHijriMonthNumber());
//
//        if (holiday != null) {
//            String holidayName = getResources().getString(
//                    getResources().getIdentifier(holiday.toString(), "string", requireContext().getPackageName()));
//
//                holidayIndicatorTextView.setText(holidayName);
//                holidayIndicatorTextView.setVisibility(View.VISIBLE);
//        }
    }

    private float getProgressBarPercentage(long timeRemaining, long timeBetween) {
        return 100 - ((float) (timeRemaining * 100) / (timeBetween));
    }

    private void startAnimationTimer(final long timeRemaining, final long timeBetween, final DayPrayer dayPrayer) {
        circularProgressBar.setProgressWithAnimation(getProgressBarPercentage(timeRemaining, timeBetween), 1000L);
        TimeRemainingCTimer = new CountDownTimer(timeRemaining, 1000L) {
            public void onTick(long millisUntilFinished) {
                // 检查Fragment是否还attached，避免崩溃
                if (isAdded() && getContext() != null) {
                    timeRemainingTextView.setText(getString(R.string.remaining) + ": " + UiUtils.formatTimeForTimer(millisUntilFinished));
                    circularProgressBar.setProgress(getProgressBarPercentage(timeRemaining, timeBetween));
                }
            }

            @RequiresApi(api = Build.VERSION_CODES.O)
            public void onFinish() {
                // Check if fragment is still attached before updating views
                if (isAdded() && getContext() != null) {
                    updateNextPrayerViews(dayPrayer);
                }
            }
        };
        TimeRemainingCTimer.start();
    }

    private void cancelTimer() {
        if (TimeRemainingCTimer != null)
            TimeRemainingCTimer.cancel();
    }

    private void initializeImageViewIcon(ConstraintLayout adhanCallConstraintLayout, ImageView adhanCallImageView, PrayerEnum prayerEnum) {
        // Read notification type from our new notification settings
        SharedPreferences notificationPrefs = requireContext().getSharedPreferences(PreferencesConstants.ADTHAN_CALLS_SHARED_PREFERENCES, MODE_PRIVATE);
        String notificationTypeKey = prayerEnum.toString() + "_NOTIFICATION_TYPE";
        String notificationType = notificationPrefs.getString(notificationTypeKey, "none");
        
        // Set icon based on notification type
        int iconResource = getNotificationIconForType(notificationType);
        adhanCallImageView.setImageResource(iconResource);

        setNotifImgOnClickListener(adhanCallConstraintLayout, adhanCallImageView, prayerEnum);
    }
    
    /**
     * Returns the appropriate icon resource for each notification type
     */
    private int getNotificationIconForType(String notificationType) {
        switch (notificationType) {
            case "none":
                return R.drawable.ic_notifications_off_24dp;
            case "azan":
                return R.drawable.ic_volume;
            case "vibrate":
                return R.drawable.ic_vibration;
            case "silent":
                return R.drawable.ic_notifications_on_24dp;
            case "text_tone":
                return R.drawable.ic_volume;
            case "clock":
                return R.drawable.ic_alarm_clock;
            default:
                return R.drawable.ic_notifications_off_24dp;
        }
    }

    private void setNotifImgOnClickListener(ConstraintLayout adhanCallConstraintLayout, ImageView imageView, PrayerEnum prayerEnum) {
        adhanCallConstraintLayout.setOnClickListener(view -> {
            // Get prayer name string resource
            String prayerName = getPrayerNameString(prayerEnum);
            
            // 🔔 使用 ActivityResultLauncher 启动通知设置页面，以接收返回结果
            Intent intent = new Intent(requireContext(), com.quran.quranaudio.online.prayertimes.ui.PrayerNotificationSettingsActivity.class);
            intent.putExtra(com.quran.quranaudio.online.prayertimes.ui.PrayerNotificationSettingsActivity.EXTRA_PRAYER_NAME, prayerName);
            intent.putExtra(com.quran.quranaudio.online.prayertimes.ui.PrayerNotificationSettingsActivity.EXTRA_PRAYER_ENUM, prayerEnum.toString());
            notificationSettingsLauncher.launch(intent);
        });
    }
    
    private String getPrayerNameString(PrayerEnum prayerEnum) {
        switch (prayerEnum) {
            case FAJR:
                return getString(R.string.FAJR);
            case DHOHR:
                return getString(R.string.DHOHR);
            case ASR:
                return getString(R.string.ASR);
            case MAGHRIB:
                return getString(R.string.MAGHRIB);
            case ICHA:
                return getString(R.string.ICHA);
            default:
                return "Prayer";
        }
    }

    private void startPrayerSchedulerWork(DayPrayer dayPrayer) {
        WorkCreator.scheduleOneTimePrayerUpdater(requireContext(), dayPrayer);
    }

    private String formatCalculationMethodAngle(String fajrAngle, String ichaAngle, boolean isAngleInMinute) {
        NumberFormat numberFormat = NumberFormat.getInstance(Locale.getDefault());
        StringBuilder stringBuilder = new StringBuilder();

        String formattedIchaAngle;
        if (isAngleInMinute) {
            String[] result = new String[2];
            Pattern p = Pattern.compile("([0-9]{1,2})");
            Matcher m = p.matcher(ichaAngle);
            if (m.find()) {
                result[0] = m.group(1);
            }
            formattedIchaAngle = numberFormat.format(Float.parseFloat(Objects.requireNonNull(result[0])));
        } else {
            formattedIchaAngle = numberFormat.format(Float.parseFloat(Objects.requireNonNull(ichaAngle)));
        }

        stringBuilder
                .append(requireContext().getString(R.string.method_fajr_angle))
                .append(" : ")
                .append(numberFormat.format(Float.parseFloat(fajrAngle)))
                .append("° - ")
                .append(requireContext().getString(R.string.method_ichaa_angle))
                .append(" : ")
                .append(formattedIchaAngle)
                .append(isAngleInMinute ? " " + requireContext().getString(R.string.common_minutes) : "°");

        return stringBuilder.toString();
    }
    
    // ⭐ Location permission helper methods
    
    /**
     * Check if location permission is granted
     */
    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }
    
    /**
     * Get the number of times permission has been requested
     */
    private int getPermissionRequestCount() {
        if (getActivity() == null) return 0;
        SharedPreferences prefs = getActivity().getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int count = prefs.getInt(KEY_PERMISSION_REQUEST_COUNT, 0);
        Log.d("PrayersFragment", "📊 Current permission request count: " + count);
        return count;
    }
    
    /**
     * Increment the permission request count
     */
    private void incrementPermissionRequestCount() {
        if (getActivity() == null) return;
        SharedPreferences prefs = getActivity().getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int currentCount = prefs.getInt(KEY_PERMISSION_REQUEST_COUNT, 0);
        int newCount = currentCount + 1;
        prefs.edit().putInt(KEY_PERMISSION_REQUEST_COUNT, newCount).apply();
        Log.d("PrayersFragment", "📈 Permission request count incremented: " + currentCount + " → " + newCount);
    }
    
    /**
     * Show permission warning dialog and increment count
     */
    private void showPermissionWarningAndIncrementCount() {
        incrementPermissionRequestCount();
        showPermissionWarning();
    }
    
    /**
     * Show permission warning dialog
     */
    @SuppressWarnings("deprecation")
    private void showPermissionWarning() {
        if (getActivity() == null) return;
        
        new android.app.AlertDialog.Builder(getActivity())
            .setTitle("Location Permission Required")
            .setMessage("This app needs location permission to show accurate prayer times and Qibla direction for your area.")
            .setPositiveButton("Grant Permission", (dialog, which) -> {
                requestPermissions(
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    1001
                );
            })
            .setNegativeButton("Not Now", (dialog, which) -> {
                dialog.dismiss();
            })
            .create()
            .show();
    }
    
    // ============ 🔔 Notification Permission Methods ============
    
    /**
     * 🔔 延迟请求通知权限（首次进入时）
     * 低端机适配：使用 WeakReference 避免内存泄漏，延迟 3 秒后检查 Fragment 状态
     */
    private void scheduleNotificationPermissionRequest() {
        // Only for Android 13+ (TIRAMISU)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Log.d("PrayersFragment", "📱 Android version < 13, no need for notification permission");
            return;
        }
        
        // Check if already shown
        if (hasShownNotificationPermission()) {
            Log.d("PrayersFragment", "ℹ️ Notification permission already shown before");
            return;
        }
        
        // Check if already granted
        if (checkNotificationPermission()) {
            Log.d("PrayersFragment", "✅ Notification permission already granted");
            markNotificationPermissionShown();
            return;
        }
        
        // 使用 WeakReference 避免内存泄漏（低端机适配）
        final WeakReference<PrayersFragment> weakFragment = new WeakReference<>(this);
        
        // 延迟 3 秒后请求权限
        notificationPermissionHandler = new Handler(Looper.getMainLooper());
        notificationPermissionRunnable = () -> {
            PrayersFragment fragment = weakFragment.get();
            if (fragment != null && fragment.isAdded() && fragment.getContext() != null) {
                Log.d("PrayersFragment", "🔔 Requesting notification permission after 3s delay");
                requestNotificationPermission();
            } else {
                Log.d("PrayersFragment", "⚠️ Fragment not attached, skipping notification permission request");
            }
        };
        
        notificationPermissionHandler.postDelayed(
            notificationPermissionRunnable, 
            NOTIFICATION_PERMISSION_DELAY_MS
        );
        
        Log.d("PrayersFragment", "⏱️ Notification permission scheduled in 3 seconds");
    }
    
    /**
     * 🔔 检查通知权限是否已授予
     */
    private boolean checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                requireContext(), 
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED;
        }
        return true; // Android 13 以下默认有权限
    }
    
    /**
     * 🔔 请求通知权限
     */
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (notificationPermissionLauncher != null) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                markNotificationPermissionShown();
            }
        }
    }
    
    /**
     * 🔔 检查是否已经显示过通知权限请求
     */
    private boolean hasShownNotificationPermission() {
        if (getActivity() == null) return true;
        SharedPreferences prefs = getActivity().getSharedPreferences(
            NOTIFICATION_PREFS_NAME, MODE_PRIVATE
        );
        return prefs.getBoolean(KEY_FIRST_ENTRY_SHOWN, false);
    }
    
    /**
     * 🔔 标记已显示通知权限请求
     */
    private void markNotificationPermissionShown() {
        if (getActivity() == null) return;
        SharedPreferences prefs = getActivity().getSharedPreferences(
            NOTIFICATION_PREFS_NAME, MODE_PRIVATE
        );
        prefs.edit().putBoolean(KEY_FIRST_ENTRY_SHOWN, true).apply();
        Log.d("PrayersFragment", "✅ Marked notification permission as shown");
    }
    
    /**
     * 🔔 清理通知权限 Handler（避免内存泄漏）
     */
    private void cleanupNotificationPermissionHandler() {
        if (notificationPermissionHandler != null && notificationPermissionRunnable != null) {
            notificationPermissionHandler.removeCallbacks(notificationPermissionRunnable);
            notificationPermissionHandler = null;
            notificationPermissionRunnable = null;
            Log.d("PrayersFragment", "🧹 Notification permission handler cleaned up");
        }
    }
    
    /**
     * 📿 Navigate to Tasbih page
     */
    private void navigateToTasbihPage() {
        try {
            // 方法1：尝试通过 NavController 导航
            androidx.navigation.NavController navController = androidx.navigation.Navigation.findNavController(requireView());
            if (navController != null) {
                navController.navigate(R.id.nav_tasbih);
                Log.d("PrayersFragment", "📿 Navigating to Tasbih via NavController");
                return;
            }
        } catch (Exception e) {
            Log.w("PrayersFragment", "⚠️ NavController navigation failed, trying BottomNav", e);
        }
        
        try {
            // 方法2：通过 BottomNavigationView 导航（兼容方案）
            if (getActivity() != null) {
                com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = 
                    getActivity().findViewById(R.id.bottom_nav);
                if (bottomNav != null) {
                    bottomNav.setSelectedItemId(R.id.nav_tasbih);
                    Log.d("PrayersFragment", "📿 Navigating to Tasbih via BottomNav");
                } else {
                    // 方法3：通过 nav_view（MainActivity 中的另一个可能 ID）
                    bottomNav = getActivity().findViewById(R.id.nav_view);
                    if (bottomNav != null) {
                        bottomNav.setSelectedItemId(R.id.nav_tasbih);
                        Log.d("PrayersFragment", "📿 Navigating to Tasbih via nav_view");
                    } else {
                        Log.e("PrayersFragment", "❌ BottomNavigationView not found (tried both IDs)");
                    }
                }
            }
        } catch (Exception e) {
            Log.e("PrayersFragment", "❌ Error navigating to Tasbih page", e);
        }
    }
    
    /**
     * Callback from PrayerLogBottomSheet when prayer is successfully logged
     * ⚡ 乐观更新：立即根据参数更新本地 UI，不重新查询 Firestore
     */
    @Override
    public void onPrayerLogged(String prayerName, String date, int newStatus, String logId) {
        long timestamp = System.currentTimeMillis();
        Log.d("PrayersFragment", "⚡ [OPTIMISTIC-" + timestamp + "] onPrayerLogged callback received");
        Log.d("PrayersFragment", "   Prayer: " + prayerName);
        Log.d("PrayersFragment", "   Date: " + date);
        Log.d("PrayersFragment", "   Status: " + newStatus);
        Log.d("PrayersFragment", "   LogId: " + logId);
        
        // ⚡ 立即更新本地 UI（不等待 Firestore）
        updatePrayerButtonStateOptimistic(prayerName, newStatus, logId);
        
        // 🔄 后台刷新数据（确保最终一致性）
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isAdded() && getActivity() != null) {
                loadQadaSummary();
            }
        }, 500); // 500ms 后后台同步，确保 Firestore 写入完成
        
        // Also update SalahRecord for backwards compatibility
        if (salahViewModel != null) {
            try {
                SalahName salahName = SalahName.valueOf(prayerName.toUpperCase(Locale.US));
                // Mark as completed if status is ADA or QADA (not MISSED)
                PrayerLog log = todayPrayerLogs.get(prayerName);
                if (log != null && (log.getStatus() == PrayerLog.PrayerStatus.ADA || log.getStatus() == PrayerLog.PrayerStatus.QADA)) {
                    salahViewModel.setSalahStatus(salahName, true);
                }
            } catch (IllegalArgumentException e) {
                Log.e("PrayersFragment", "❌ Invalid prayer name: " + prayerName, e);
            }
        }
        
        // 🎯 Record daily check-in for streak tracking (if Ada' or Qada')
        // newStatus: 0=ADA, 1=QADA, 2=MISSED
        if (newStatus == 0 || newStatus == 1) {
            // Run streak tracking in background (non-blocking)
            new Thread(() -> {
                try {
                    Log.d("PrayersFragment", "🔥 Recording daily check-in for streak tracking...");
                    
                    // Note: StreakManager.recordCheckIn is a Kotlin suspend function
                    // We cannot call it directly from Java without coroutines
                    // For now, we'll skip the streak tracking in prayer logging
                    // It's already tracked in Learning Plan which is the primary entry point
                    
                    Log.d("PrayersFragment", "ℹ️ Streak tracking via Prayer logging is optional");
                    Log.d("PrayersFragment", "→ Primary streak tracking is done via Learning Plan");
                    
                } catch (Exception e) {
                    Log.e("PrayersFragment", "❌ Failed to record check-in", e);
                }
            }).start();
        } else {
            Log.d("PrayersFragment", "ℹ️ Prayer status is MISSED (status=" + newStatus + "), not recording check-in");
        }
    }
    
    @Override
    public void onQadaCountChanged(int delta) {
        long timestamp = System.currentTimeMillis();
        Log.d("PrayersFragment", "⚡ [OPTIMISTIC-" + timestamp + "] onQadaCountChanged callback received: delta=" + delta);
        
        // ⚡ 立即更新本地 Qada 计数（不等待 Firestore）
        updateQadaTotalOptimistic(delta);
        
        // 🔄 后台刷新数据（确保最终一致性）
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isAdded() && getActivity() != null) {
                loadQadaSummary();
            }
        }, 500); // 500ms 后后台同步，确保 Firestore 写入完成
    }
    
    /**
     * ⚡ 乐观更新：立即更新祷告按钮状态
     * @param prayerName 祷告名称（英文或本地化）
     * @param newStatus 新状态 (0=Ada, 1=Qada, 2=Missed)
     * @param logId 记录 ID
     */
    private void updatePrayerButtonStateOptimistic(String prayerName, int newStatus, String logId) {
        if (!isAdded() || getActivity() == null) {
            return;
        }
        
        getActivity().runOnUiThread(() -> {
            try {
                // 转换祷告名称为英文（确保匹配）
                String englishName = com.quran.quranaudio.online.prayertimes.models.PrayerName.toEnglishName(
                    prayerName, requireContext()
                );
                
                Log.d("PrayersFragment", "⚡ Updating button for prayer: " + englishName + " (status: " + newStatus + ", logId: " + logId + ")");
                
                // 根据祷告名称找到对应的按钮
                MaterialButton button = null;
                String prayerKey = null;
                
                if (fajrTrackButton != null && (englishName.equalsIgnoreCase("Fajr") || englishName.equalsIgnoreCase("Subuh"))) {
                    button = fajrTrackButton;
                    prayerKey = "Fajr";
                } else if (dhuhrTrackButton != null && englishName.equalsIgnoreCase("Dhuhr")) {
                    button = dhuhrTrackButton;
                    prayerKey = "Dhuhr";
                } else if (asrTrackButton != null && englishName.equalsIgnoreCase("Asr")) {
                    button = asrTrackButton;
                    prayerKey = "Asr";
                } else if (maghribTrackButton != null && englishName.equalsIgnoreCase("Maghrib")) {
                    button = maghribTrackButton;
                    prayerKey = "Maghrib";
                } else if (ishaTrackButton != null && (englishName.equalsIgnoreCase("Isha") || englishName.equalsIgnoreCase("Isya"))) {
                    button = ishaTrackButton;
                    prayerKey = "Isha";
                }
                
                // 更新本地缓存
                if (prayerKey != null) {
                    if (newStatus == 2) { // Missed
                        todayPrayerLogs.remove(prayerKey);
                    } else {
                        // 使用 Kotlin 构造函数创建 PrayerLog（Kotlin data class 所有字段都是 val）
                        PrayerLog log = new PrayerLog(
                            logId,  // id
                            "",     // userId
                            prayerKey,  // prayerName
                            PrayerLog.PrayerStatus.values()[newStatus],  // status
                            null,   // performedAt
                            null,   // loggedAt
                            "",     // notes
                            "",     // date
                            false,  // isToday
                            java.util.Collections.emptyList()  // tags
                        );
                        todayPrayerLogs.put(prayerKey, log);
                    }
                }
                
                // ⚡ 重用现有的 UI 更新逻辑（避免重复代码和资源ID错误）
                if (prayerKey != null) {
                    // 找到对应的 SalahName
                    SalahName salahName = null;
                    if (prayerKey.equals("Fajr")) salahName = SalahName.FAJR;
                    else if (prayerKey.equals("Dhuhr")) salahName = SalahName.DHUHR;
                    else if (prayerKey.equals("Asr")) salahName = SalahName.ASR;
                    else if (prayerKey.equals("Maghrib")) salahName = SalahName.MAGHRIB;
                    else if (prayerKey.equals("Isha")) salahName = SalahName.ISHA;
                    
                    if (salahName != null) {
                        PrayerLog updatedLog = todayPrayerLogs.get(prayerKey);
                        updatePrayerStatusUI(salahName, updatedLog);
                        Log.d("PrayersFragment", "✅ Prayer status UI updated immediately via existing method");
                    }
                } else {
                    Log.w("PrayersFragment", "⚠️ Prayer key not found for: " + englishName);
                }
            } catch (Exception e) {
                Log.e("PrayersFragment", "❌ Error updating button state", e);
            }
        });
    }
    
    /**
     * ⚡ 乐观更新：立即增减 Total Qada 计数
     * @param delta 增减量（正数增加，负数减少）
     */
    private void updateQadaTotalOptimistic(int delta) {
        if (!isAdded() || getActivity() == null || qadaCountTextView == null) {
            return;
        }
        
        getActivity().runOnUiThread(() -> {
            try {
                // 获取当前显示的 Total Qada 数值
                // qadaCountTextView 的格式是 "X Prayers" 或 "No outstanding prayers"
                String currentText = qadaCountTextView.getText().toString();
                int currentTotal = 0;
                
                try {
                    // 尝试从文本中提取数字（例如 "5 Prayers" -> 5）
                    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)");
                    java.util.regex.Matcher matcher = pattern.matcher(currentText);
                    if (matcher.find()) {
                        currentTotal = Integer.parseInt(matcher.group(1));
                    }
                } catch (NumberFormatException e) {
                    Log.w("PrayersFragment", "⚠️ Failed to parse current Qada total: " + currentText);
                }
                
                // 计算新的总数
                int newTotal = Math.max(0, currentTotal + delta); // 确保不为负数
                
                Log.d("PrayersFragment", "⚡ Qada total: " + currentTotal + " → " + newTotal + " (delta: " + delta + ")");
                
                // 立即更新 UI（使用与 updateQadaSummaryUI 相同的格式）
                if (newTotal > 0) {
                    String formatted = NumberFormat.getIntegerInstance().format(newTotal);
                    String displayText = getString(R.string.qada_count_prayers, formatted);
                    qadaCountTextView.setText(displayText);
                    qadaCountTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.qada_alert_red));
                } else {
                    qadaCountTextView.setText(getString(R.string.qada_count_zero));
                    qadaCountTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.bottom_nav_selected));
                }
                
                Log.d("PrayersFragment", "✅ Qada total updated immediately");
            } catch (Exception e) {
                Log.e("PrayersFragment", "❌ Error updating Qada total", e);
            }
        });
    }
}