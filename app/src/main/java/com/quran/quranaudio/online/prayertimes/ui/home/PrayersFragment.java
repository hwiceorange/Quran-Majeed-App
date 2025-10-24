package com.quran.quranaudio.online.prayertimes.ui.home;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
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

import com.google.android.material.button.MaterialButton;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.quran.quranaudio.online.Utils.GoogleAuthManager;

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
import com.mikhaellopez.circularprogressbar.CircularProgressBar;

import org.apache.commons.lang3.StringUtils;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import cl.jesualex.stooltip.Position;
import cl.jesualex.stooltip.Tooltip;


public class PrayersFragment extends Fragment {

    @Inject
    ViewModelProvider.Factory viewModelFactory;

    private SalahViewModel salahViewModel;
    private GoogleAuthManager googleAuthManager;
    private ActivityResultLauncher<Intent> signInLauncher;
    
    // Salah track buttons
    private MaterialButton fajrTrackButton;
    private MaterialButton dhuhrTrackButton;
    private MaterialButton asrTrackButton;
    private MaterialButton maghribTrackButton;
    private MaterialButton ishaTrackButton;
    
    // ⭐ Location permission tracking
    private static final String PREFS_NAME = "LocationPermissionPrefs";
    private static final String KEY_PERMISSION_REQUEST_COUNT = "permission_request_count";
    private static final int MAX_PERMISSION_REQUESTS = 2;

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
                        Toast.makeText(requireContext(), "Login cancelled", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.e("PrayersFragment", "Error handling sign-in result", e);
                }
            }
        );
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
        
        // Initialize Salah track buttons
        fajrTrackButton = rootView.findViewById(R.id.fajr_track_button);
        dhuhrTrackButton = rootView.findViewById(R.id.dhuhr_track_button);
        asrTrackButton = rootView.findViewById(R.id.asr_track_button);
        maghribTrackButton = rootView.findViewById(R.id.maghrib_track_button);
        ishaTrackButton = rootView.findViewById(R.id.isha_track_button);
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
     * Handles click on a salah track button.
     * 未登录用户点击时触发Google登录，登录后自动保存状态
     */
    private void onSalahTrackClicked(SalahName salahName, MaterialButton button) {
        Log.d("PrayersFragment", "🔘 Track button clicked: " + salahName.getDisplayName());
        
        // Check if user is logged in
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Log.d("PrayersFragment", "❌ User not logged in, showing login dialog");
            // Show login dialog for unauthenticated users
            showLoginDialog(salahName, button);
            return;
        }
        
        // User is logged in, proceed with toggle
        Log.d("PrayersFragment", "✅ User logged in, toggling status for: " + salahName.getDisplayName());
        
        // Disable button temporarily to prevent double-clicks
        button.setEnabled(false);
        
        salahViewModel.toggleSalahStatus(salahName);
        
        // Re-enable after a short delay
        button.postDelayed(() -> {
            button.setEnabled(true);
            Log.d("PrayersFragment", "🔓 Button re-enabled for: " + salahName.getDisplayName());
        }, 500);
    }
    
    /**
     * Shows login dialog when unauthenticated user clicks Track button
     */
    private void showLoginDialog(SalahName salahName, MaterialButton button) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Login Required")
            .setMessage("Please login with your Google account to track your prayers and sync across devices.")
            .setPositiveButton("Login with Google", (dialog, which) -> {
                dialog.dismiss();
                initiateGoogleSignIn();
            })
            .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
            .setCancelable(true)
            .show();
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
        if (salahViewModel == null) return;
        
        // Observe salah record changes and update button states
        salahViewModel.getTodaySalahRecord().observe(getViewLifecycleOwner(), record -> {
            Log.d("PrayersFragment", "📝 Salah record received: " + (record != null ? record.getTotalCompleted() + "/5 completed" : "null"));
            
            if (record != null) {
                // Update all button states
                updateTrackButton(fajrTrackButton, record.getFajr());
                updateTrackButton(dhuhrTrackButton, record.getDhuhr());
                updateTrackButton(asrTrackButton, record.getAsr());
                updateTrackButton(maghribTrackButton, record.getMaghrib());
                updateTrackButton(ishaTrackButton, record.getIsha());
            } else {
                // Record is null (no data yet), keep default Track state
                Log.d("PrayersFragment", "📝 No salah record found, keeping default Track state");
            }
        });
    }
    
    /**
     * Updates the track button appearance based on completion status.
     */
    private void updateTrackButton(MaterialButton button, boolean isCompleted) {
        if (button == null) {
            Log.w("PrayersFragment", "⚠️ Button is null, cannot update");
            return;
        }
        
        String buttonId = getResources().getResourceEntryName(button.getId());
        Log.d("PrayersFragment", "🎨 Updating button " + buttonId + " to " + (isCompleted ? "✓ (completed)" : "Track (uncompleted)"));
        
        if (isCompleted) {
            button.setText(getString(R.string.button_completed));
            button.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.green)
            ));
        } else {
            button.setText(getString(R.string.button_track));
            button.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.salah_track_button)
            ));
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

        ZonedDateTime zonedDateTime = TimingUtils.getZonedDateTimeFromTimestamps(dayPrayer.getTimestamp(), dayPrayer.getTimezone());
        String nameOfTheDay = zonedDateTime.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.getDefault());

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
                updateNextPrayerViews(dayPrayer);
            }
        };
        TimeRemainingCTimer.start();
    }

    private void cancelTimer() {
        if (TimeRemainingCTimer != null)
            TimeRemainingCTimer.cancel();
    }

    private void initializeImageViewIcon(ConstraintLayout adhanCallConstraintLayout, ImageView adhanCallImageView, PrayerEnum prayerEnum) {
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences(adhanCallsPreferences, MODE_PRIVATE);
        String callPreferenceKey = prayerEnum.toString() + adhanCallKeyPart;

        boolean adhanCallEnabled = sharedPreferences.getBoolean(callPreferenceKey, false);

        adhanCallImageView.setImageResource(adhanCallEnabled ? R.drawable.ic_notifications_on_24dp : R.drawable.ic_notifications_off_24dp);

        setNotifImgOnClickListener(adhanCallConstraintLayout, adhanCallImageView, callPreferenceKey);
    }

    private void setNotifImgOnClickListener(ConstraintLayout adhanCallConstraintLayout, ImageView imageView, String callPreferenceKey) {
        adhanCallConstraintLayout.setOnClickListener(view -> {
            SharedPreferences sharedPreferences = requireContext().getSharedPreferences(adhanCallsPreferences, MODE_PRIVATE);

            Vibrator vibe = (Vibrator) requireContext().getSystemService(Context.VIBRATOR_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibe.vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibe.vibrate(10);
            }

            boolean adhanCallEnabled = sharedPreferences.getBoolean(callPreferenceKey, false);

            imageView.setImageResource(adhanCallEnabled ? R.drawable.ic_notifications_off_24dp : R.drawable.ic_notifications_on_24dp);

            SharedPreferences.Editor edit = sharedPreferences.edit();

            edit.putBoolean(callPreferenceKey, !adhanCallEnabled);
            edit.apply();
        });
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
}