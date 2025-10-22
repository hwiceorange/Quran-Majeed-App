package com.quran.quranaudio.online.prayertimes.ui.home;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.faltenreich.skeletonlayout.Skeleton;
import com.google.firebase.auth.FirebaseUser;
import com.mikhaellopez.circularprogressbar.CircularProgressBar;
import com.quran.quranaudio.online.App;
import com.quran.quranaudio.online.activities.LiveActivity;
import com.quran.quranaudio.online.activities.SixKalmasActivity;
import com.quran.quranaudio.online.activities.ZakatCalculatorActivity;
import com.quran.quranaudio.online.compass.QiblaDirectionActivity;
import com.quran.quranaudio.online.hadith.HadithActivity;
import com.quran.quranaudio.online.prayertimes.common.ComplementaryTimingEnum;
import com.quran.quranaudio.online.prayertimes.common.PrayerEnum;
import com.quran.quranaudio.online.prayertimes.job.WorkCreator;
import com.quran.quranaudio.online.prayertimes.timings.DayPrayer;
import com.quran.quranaudio.online.prayertimes.ui.calendar.CalendarActivity;
import com.quran.quranaudio.online.prayertimes.ui.timingtable.TimingTableActivity;
import com.quran.quranaudio.online.prayertimes.utils.AlertHelper;
import com.quran.quranaudio.online.prayertimes.utils.PrayerUtils;
import com.quran.quranaudio.online.prayertimes.utils.TimingUtils;
import com.quran.quranaudio.online.prayertimes.utils.UiUtils;
import com.quran.quranaudio.online.quran_module.activities.ActivityReaderIndexPage;
import com.quran.quranaudio.online.quran_module.activities.ActivityQuran_Search;
import com.quran.quranaudio.online.R;
import com.quran.quranaudio.online.activities.HomeActivity;
import com.quran.quranaudio.online.prayertimes.ui.MainActivity;
import com.quran.quranaudio.online.Utils.GoogleAuthManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.apache.commons.lang3.StringUtils;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;


public class HomeFragment extends Fragment implements View.OnClickListener {

    private boolean allowRefresh = false;
    ActivityResultLauncher<String[]> mPermissionResultLauncher;
    private boolean isLocationPermissionGranted = false;

    @Inject
    ViewModelProvider.Factory viewModelFactory;

    private LocalDateTime todayDate;
    private CountDownTimer TimeRemainingCTimer;

    private TextView locationTextView;
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

    // Header Views
    private TextView tvGreeting;
    private TextView tvUserName;
    private ImageView btnSearch;
    private CardView cardAvatar;
    private ImageView imgAvatarDefault;
    private ImageView imgAvatarUser;

    // Prayer Card Views
    private CardView prayerCard;
    private TextView tvNextPrayerName;
    private TextView tvNextPrayerTimeCard;
    private TextView tvTimeRemaining;
    private TextView tvLocationPrayer;
    private LinearLayout locationContainer;
    private LinearLayout btnNavPrayer;
    private LinearLayout btnNavQuran;
    private LinearLayout btnNavLearn;
    private LinearLayout btnNavTools;

    // Verse of the Day Card Views
    private CardView verseOfDayCard;
    private TextView tvArabicText;
    private TextView tvTranslationText;
    private TextView tvVerseInfo;
    private ImageView btnShare;
    private ImageView btnBookmark;
    private ProgressBar loadingIndicator;
    private int votdChapterNo = -1;
    private int votdVerseNo = -1;

    // Live Stream Card Views
    private CardView meccaLiveCard;
    private TextView tvMeccaTitle;
    private TextView tvMeccaDescription;
    private CardView medinaLiveCard;
    private TextView tvMedinaTitle;
    private TextView tvMedinaDescription;

    private GoogleAuthManager googleAuthManager;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    private CircularProgressBar circularProgressBar;
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

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        todayDate = LocalDateTime.now();

        TypedArray typedArray = requireContext().getTheme().obtainStyledAttributes(R.styleable.mainStyles);
        int navigationBackgroundStartColor = typedArray.getColor(R.styleable.mainStyles_navigationBackgroundStartColor, ContextCompat.getColor(requireContext(), R.color.alabaster));
        int navigationBackgroundEndColor = typedArray.getColor(R.styleable.mainStyles_navigationBackgroundEndColor, ContextCompat.getColor(requireContext(), R.color.alabaster));
        typedArray.recycle();

        // Use Activity scope to share ViewModel with MainActivity preload and other fragments
        // This ensures data is loaded once and shared across all fragments
        HomeViewModel homeViewModel = new ViewModelProvider(requireActivity(), viewModelFactory)
                .get(HomeViewModel.class);

        View rootView = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize Google Auth Manager
        googleAuthManager = new GoogleAuthManager(requireContext());
        
        // Register Google Sign-In Launcher
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        googleAuthManager.handleSignInResult(result.getData(), new GoogleAuthManager.AuthCallback() {
                            @Override
                            public void onSuccess(FirebaseUser user) {
                                updateHeaderUI();
                            }

                            @Override
                            public void onFailure(String error) {
                                // Handle error - could show a toast or snackbar
                            }
                        });
                    }
                }
        );

        initializeViews(rootView);
        initializeHeaderListeners();
        initializePrayerCardListeners();
        initializeVerseOfDayCard();
        initializeLiveStreamCards();
        updateHeaderUI();

        //PermissionStart

        mPermissionResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), new ActivityResultCallback<Map<String, Boolean>>() {
            @Override
            public void onActivityResult(Map<String, Boolean> result) {
                if (result.get(Manifest.permission.ACCESS_FINE_LOCATION) !=null) {
                    isLocationPermissionGranted = result.get(Manifest.permission.ACCESS_FINE_LOCATION);
                    
                    if (isLocationPermissionGranted) {
                        // Permission granted - close warning dialog and refresh data
                        if (dialogWarning != null && dialogWarning.isShowing()) {
                            dialogWarning.dismiss();
                        }
                        
                        // Trigger location and prayer time refresh
                        if (isAdded() && getActivity() != null) {
                            // Force ViewModel to reload with new location permission
                            HomeViewModel homeViewModel = new ViewModelProvider(requireActivity(), viewModelFactory)
                                    .get(HomeViewModel.class);
                            // The ViewModel will automatically observe location changes
                        }
                    }
                }
            }
        });

        if(!checkLocationPermission()){
            showPermissionWarning();
        }
        //Permission End

        homeViewModel
                .getError()
                .observe(
                        getViewLifecycleOwner(),
                        error -> {
                            // Only show error dialog if location permission is NOT granted
                            // If permission is granted but there's still an error (e.g., GPS off),
                            // don't show intrusive dialog - let the error state show in UI
                            if (error != null && !isLocationPermissionGranted) {
                                AlertHelper.displayLocationErrorDialog(requireActivity(),
                                        getResources().getString(R.string.location_alert_title),
                                        error);
                            }
                            // If permission is granted but there's still an error (GPS off, etc.),
                            // don't show dialog - error is already visible in UI
                        });

        homeViewModel.getDayPrayers().observe(getViewLifecycleOwner(), dayPrayer -> {
            updateDatesTextViews(dayPrayer);
            updateNextPrayerViews(dayPrayer);
            updateTimingsTextViews(dayPrayer);
            updateHeaderPrayerInfo(dayPrayer);
            updatePrayerCard(dayPrayer);  // Update Prayer Card with real-time data
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

        setOnClick(rootView);
        return rootView;
    }

    View.OnClickListener dialogListener=new View.OnClickListener() {
        @Override public void onClick(View v) {
            if(v.getId()==R.id.btn_skip){
                dialogWarning.dismiss();
            } else if(v.getId()==R.id.btn_enable_location){
                dialogWarning.dismiss();
                requestPermission();
            }
        }
    };
    Dialog dialogWarning;
    private boolean checkLocationPermission(){
        isLocationPermissionGranted = ContextCompat.checkSelfPermission(requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;
        return isLocationPermissionGranted;
    }
    private void showPermissionWarning(){
        dialogWarning=new AlertDialog.Builder(getActivity()).setView(R.layout.layout_dialog_location_warning).create();
        TextView skip=dialogWarning.findViewById(R.id.btn_skip);
        Button enable=dialogWarning.findViewById(R.id.btn_enable_location);
        skip.setOnClickListener(dialogListener);
        enable.setOnClickListener(dialogListener);
        dialogWarning.show();
    }
    private void requestPermission(){


        List<String> permissionRequest = new ArrayList<String>();

        if (!isLocationPermissionGranted) {
            permissionRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (!permissionRequest.isEmpty()) {

            mPermissionResultLauncher.launch(permissionRequest.toArray(new String[0]));
        }
    }

    public static HomeFragment newInstance() {
        Bundle bundle = new Bundle();
        HomeFragment homeFragment = new HomeFragment();
        homeFragment.setArguments(bundle);
        return homeFragment;
    }

    private void setOnClick(@NonNull View view) {
        CardView quran = view.findViewById(R.id.read_quran);
        quran.setOnClickListener(this);
        CardView liveMedina = view.findViewById(R.id.medina_live);
        liveMedina.setOnClickListener(this);
        CardView hadithBooks = view.findViewById(R.id.hadith_books);
        hadithBooks.setOnClickListener(this);
        CardView liveMecca = view.findViewById(R.id.mecca_live);
        liveMecca.setOnClickListener(this);
        CardView qiblaDirection = view.findViewById(R.id.qibla_direction);
        qiblaDirection.setOnClickListener(this);
        CardView prayerCalender = view.findViewById(R.id.prayer_Calender);
        prayerCalender.setOnClickListener(this);
        CardView sixKalmas = view.findViewById(R.id.six_kalmas);
        sixKalmas.setOnClickListener(this);
        CardView zakatCalculator = view.findViewById(R.id.zakat_calculator);
        zakatCalculator.setOnClickListener(this);
        CardView prayers_time = view.findViewById(R.id.prayers_time);
        prayers_time.setOnClickListener(this);
        ImageView islamicCalender = view.findViewById(R.id.islamic_Calender);
        islamicCalender.setOnClickListener(this);
        CardView newAzkar = view.findViewById(R.id.new_azkar);
        newAzkar.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Fragment fragment;
        switch (v.getId()){
            case R.id.read_quran:
                startActivity(new Intent(getActivity(), ActivityReaderIndexPage.class));
                break;

            case R.id.medina_live:
                String[] medinaLiveUrls = {
                    "http://m.live.net.sa:1935/live/sunnah/playlist.m3u8", // HLS流媒体（优先，应用内播放）
                    "https://ythls.armelin.one/channel/UCJr4gikBowJ8I-iUXs7CkMg.m3u8", // YouTube转HLS
                    "https://www.youtube.com/watch?v=4s4XX-qaNgg", // YouTube直播备用1
                    "https://www.youtube.com/watch?v=0lg0XeJ2gAU", // YouTube直播备用2
                    "https://www.youtube.com/watch?v=4Ar8JHRCdSE" // YouTube直播备用3
                };
                Intent intent = new Intent(getActivity(), LiveActivity.class);
                intent.putExtra("live", medinaLiveUrls[0]);
                intent.putExtra("backup_urls", medinaLiveUrls);
                startActivity(intent);
                break;

            case R.id.hadith_books:
                startActivity(new Intent(getActivity(), HadithActivity.class));
                break;

            case R.id.mecca_live:
                String[] meccaLiveUrls = {
                    "http://m.live.net.sa:1935/live/quran/playlist.m3u8",
                    "https://ythls.armelin.one/channel/UCos52-JmjOoBnBOnxJCWAQA.m3u8",
                    "https://www.youtube.com/watch?v=e85tJVzKwDU",
                    "https://www.youtube.com/watch?v=yd19lGSibQ4"
                };
                intent = new Intent(getActivity(), LiveActivity.class);
                intent.putExtra("live", meccaLiveUrls[0]);
                intent.putExtra("backup_urls", meccaLiveUrls);
                startActivity(intent);
                break;

            case R.id.qibla_direction:
                startActivity(new Intent(getActivity(), QiblaDirectionActivity.class));
                break;

            case R.id.prayer_Calender:
                startActivity(new Intent(getActivity(), CalendarActivity.class));
                break;

            case R.id.six_kalmas:
                startActivity(new Intent(getActivity(), SixKalmasActivity.class));
                break;
            case R.id.zakat_calculator:
                startActivity(new Intent(getActivity(), ZakatCalculatorActivity.class));
                break;
            case R.id.prayers_time:
                startActivity(new Intent(getActivity(), MainActivity.class));
                break;

            case R.id.islamic_Calender:
             //   startActivity(new Intent(getActivity(), Activity_Quran_Settings.class));

                 startActivity(new Intent(getActivity(), TimingTableActivity.class));
                break;

            case R.id.new_azkar:
              //  startActivity(new Intent(getActivity(), AzkarActivity.class));
                break;


        }
    }



    private void loadFragment(Fragment fragment){
        FragmentTransaction fragmentTransaction = getParentFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.home_host_fragment, fragment);
      //  getParentFragmentManager().beginTransaction().detach(HomeFragment.this).attach(HomeFragment.this).commit();
        fragmentTransaction.addToBackStack(null).commit();

    }



    @Override
    public void onDestroy() {

        cancelTimer();
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        //Initialize();
        if(allowRefresh){
            allowRefresh=false;
            //call your initialization code here
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (!allowRefresh)
            allowRefresh = true;
    }

    private void initializeViews(View rootView) {
        skeleton = rootView.findViewById(R.id.skeletonLayout);

        // Initialize Header Views
        View headerView = rootView.findViewById(R.id.home_header);
        if (headerView != null) {
            tvGreeting = headerView.findViewById(R.id.tv_greeting);
            tvUserName = headerView.findViewById(R.id.tv_user_name);
            btnSearch = headerView.findViewById(R.id.btn_search);
            cardAvatar = headerView.findViewById(R.id.card_avatar);
            imgAvatarDefault = headerView.findViewById(R.id.img_avatar_default);
            imgAvatarUser = headerView.findViewById(R.id.img_avatar_user);
        }

        // Initialize Prayer Card Views
        View prayerCardView = rootView.findViewById(R.id.prayer_card);
        if (prayerCardView != null) {
            prayerCard = (CardView) prayerCardView;
            tvNextPrayerName = prayerCardView.findViewById(R.id.tv_next_prayer_name);
            tvNextPrayerTimeCard = prayerCardView.findViewById(R.id.tv_next_prayer_time);
            tvTimeRemaining = prayerCardView.findViewById(R.id.tv_time_remaining);
            tvLocationPrayer = prayerCardView.findViewById(R.id.tv_location_prayer);
            locationContainer = prayerCardView.findViewById(R.id.location_container);
            btnNavPrayer = prayerCardView.findViewById(R.id.btn_nav_prayer);
            btnNavQuran = prayerCardView.findViewById(R.id.btn_nav_quran);
            btnNavLearn = prayerCardView.findViewById(R.id.btn_nav_learn);
            btnNavTools = prayerCardView.findViewById(R.id.btn_nav_tools);
        }

        // Initialize Verse of the Day Card Views (using correct IDs from layout_verse_of_day_card.xml)
        View verseOfDayCardView = rootView.findViewById(R.id.verse_of_day_card);
        if (verseOfDayCardView != null) {
            verseOfDayCard = (CardView) verseOfDayCardView;
            tvArabicText = verseOfDayCardView.findViewById(R.id.votd_content_text);  // Changed to votd_content_text
            // tvTranslationText is no longer separate - combined with Arabic in votd_content_text
            tvVerseInfo = verseOfDayCardView.findViewById(R.id.votd_verse_reference);
            btnShare = verseOfDayCardView.findViewById(R.id.votd_share);
            btnBookmark = verseOfDayCardView.findViewById(R.id.votd_bookmark);
            // loadingIndicator = verseOfDayCardView.findViewById(R.id.loading_indicator);  // Not in new layout
        }

        // Initialize Mecca Live Card Views
        View meccaLiveCardView = rootView.findViewById(R.id.mecca_live_card);
        if (meccaLiveCardView != null) {
            meccaLiveCard = (CardView) meccaLiveCardView;
            tvMeccaTitle = meccaLiveCardView.findViewById(R.id.tv_live_title);
            tvMeccaDescription = meccaLiveCardView.findViewById(R.id.tv_live_description);
        }

        // Initialize Medina Live Card Views
        View medinaLiveCardView = rootView.findViewById(R.id.medina_live_card);
        if (medinaLiveCardView != null) {
            medinaLiveCard = (CardView) medinaLiveCardView;
            tvMedinaTitle = medinaLiveCardView.findViewById(R.id.tv_live_title);
            tvMedinaDescription = medinaLiveCardView.findViewById(R.id.tv_live_description);
        }

        locationTextView = rootView.findViewById(R.id.location_text_view);
        todayDateTextView = rootView.findViewById(R.id.todayDateTextView);
        prayerNametextView = rootView.findViewById(R.id.prayerNametextView);
        prayerTimetextView = rootView.findViewById(R.id.prayerTimetextView);
        timeRemainingTextView = rootView.findViewById(R.id.timeRemainingTextView);
        circularProgressBar = rootView.findViewById(R.id.circularProgressBar);
        fajrTimingTextView = rootView.findViewById(R.id.fajr_timing_text_view);



        dohrTimingTextView = rootView.findViewById(R.id.dohr_timing_text_view);
        asrTimingTextView = rootView.findViewById(R.id.asr_timing_text_view);
        maghribTimingTextView = rootView.findViewById(R.id.maghreb_timing_text_view);
        ichaTimingTextView = rootView.findViewById(R.id.icha_timing_text_view);
        sunriseTimingTextView = rootView.findViewById(R.id.sunrise_timing_text_view);
        sunsetTimingTextView = rootView.findViewById(R.id.sunset_timing_text_view);

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void updateTimingsTextViews(DayPrayer dayPrayer) {
        Map<PrayerEnum, LocalDateTime> timings = dayPrayer.getTimings();

        LocalDateTime fajrTiming = timings.get(PrayerEnum.FAJR);
        LocalDateTime dohrTiming = timings.get(PrayerEnum.DHOHR);
        LocalDateTime asrTiming = timings.get(PrayerEnum.ASR);
        LocalDateTime maghribTiming = timings.get(PrayerEnum.MAGHRIB);
        LocalDateTime ichaTiming = timings.get(PrayerEnum.ICHA);

        LocalDateTime sunriseTiming = dayPrayer.getComplementaryTiming().get(ComplementaryTimingEnum.SUNRISE);
        LocalDateTime sunsetTiming = dayPrayer.getComplementaryTiming().get(ComplementaryTimingEnum.SUNSET);

        sunriseTimingTextView.setText(UiUtils.formatTiming(Objects.requireNonNull(sunriseTiming)));
        sunsetTimingTextView.setText(UiUtils.formatTiming(Objects.requireNonNull(sunsetTiming)));
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

        prayerNametextView.setText ("Next: " + prayerName);
        prayerTimetextView.setText(UiUtils.formatTiming(Objects.requireNonNull(timings.get(nextPrayerKey))));
        timeRemainingTextView.setText(UiUtils.formatTimeForTimer(timeRemaining));

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

    }

    private float getProgressBarPercentage(long timeRemaining, long timeBetween) {
        return 100 - ((float) (timeRemaining * 100) / (timeBetween));
    }

    private void startAnimationTimer(final long timeRemaining, final long timeBetween, final DayPrayer dayPrayer) {
        circularProgressBar.setProgressWithAnimation(getProgressBarPercentage(timeRemaining, timeBetween), 1000L);
        TimeRemainingCTimer = new CountDownTimer(timeRemaining, 1000L) {
            public void onTick(long millisUntilFinished) {
                timeRemainingTextView.setText(UiUtils.formatTimeForTimer(millisUntilFinished));
                circularProgressBar.setProgress(getProgressBarPercentage(timeRemaining, timeBetween));
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


    private void startPrayerSchedulerWork(DayPrayer dayPrayer) {
        WorkCreator.scheduleOneTimePrayerUpdater(requireContext(), dayPrayer);
    }

    /**
     * Initialize Header click listeners
     */
    private void initializeHeaderListeners() {
        if (btnSearch != null) {
            btnSearch.setOnClickListener(v -> {
                // Navigate to Global Search
                Intent searchIntent = new Intent(getActivity(), ActivityQuran_Search.class);
                startActivity(searchIntent);
            });
        }

        if (cardAvatar != null) {
            cardAvatar.setOnClickListener(v -> {
                if (googleAuthManager.isUserSignedIn()) {
                    // Show logout dialog
                    showLogoutDialog();
                } else {
                    // Start Google Sign-In
                    Intent signInIntent = googleAuthManager.getSignInIntent();
                    googleSignInLauncher.launch(signInIntent);
                }
            });
        }
    }

    /**
     * Update Header UI based on authentication state
     */
    private void updateHeaderUI() {
        if (googleAuthManager == null) return;

        if (googleAuthManager.isUserSignedIn()) {
            // User is signed in
            String userName = googleAuthManager.getUserDisplayName();
            
            if (tvUserName != null) {
                tvUserName.setText(userName);
                tvUserName.setVisibility(View.VISIBLE);
            }

            // Load user avatar
            if (imgAvatarUser != null && imgAvatarDefault != null) {
                android.net.Uri photoUrl = googleAuthManager.getUserPhotoUrl();
                if (photoUrl != null) {
                    Glide.with(this)
                            .load(photoUrl)
                            .circleCrop()
                            .into(imgAvatarUser);
                    imgAvatarUser.setVisibility(View.VISIBLE);
                    imgAvatarDefault.setVisibility(View.GONE);
                } else {
                    imgAvatarUser.setVisibility(View.GONE);
                    imgAvatarDefault.setVisibility(View.VISIBLE);
                }
            }
        } else {
            // User is not signed in
            if (tvUserName != null) {
                tvUserName.setVisibility(View.GONE);
            }

            if (imgAvatarUser != null && imgAvatarDefault != null) {
                imgAvatarUser.setVisibility(View.GONE);
                imgAvatarDefault.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * Show logout confirmation dialog
     */
    private void showLogoutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.logout)
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    googleAuthManager.signOut(() -> {
                        updateHeaderUI();
                    });
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    /**
     * Update header prayer time information
     * NOTE: This method is no longer used - Prayer info now displayed in Prayer Card instead of Header
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void updateHeaderPrayerInfo(DayPrayer dayPrayer) {
        // Removed: Prayer info now displayed in separate Prayer Card
        // Header only shows greeting, user name, search icon, and avatar
    }

    /**
     * Initialize Prayer Card click listeners
     */
    private void initializePrayerCardListeners() {
        // Prayer Card click - Navigate to Prayer Times (Salat Page)
        if (prayerCard != null) {
            prayerCard.setOnClickListener(v -> navigateToSalatPage());
        }

        // Location click - Edit location
        if (locationContainer != null) {
            locationContainer.setOnClickListener(v -> editLocation());
        }

        // Prayer button - Navigate to Salat Page
        if (btnNavPrayer != null) {
            btnNavPrayer.setOnClickListener(v -> navigateToSalatPage());
        }

        // Quran button - Navigate to Quran Reader
        if (btnNavQuran != null) {
            btnNavQuran.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), ActivityReaderIndexPage.class);
                startActivity(intent);
            });
        }

        // Learn button - Navigate to Discover (Names99)
        if (btnNavLearn != null) {
            btnNavLearn.setOnClickListener(v -> {
                // Navigate to bottom navigation "Discover" tab
                        if (getActivity() instanceof HomeActivity) {
                            BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottom_nav);
                            if (bottomNav != null) {
                                bottomNav.setSelectedItemId(R.id.nav_name_99);
                            }
                        }
            });
        }

        // Tools button - Show tools menu
        if (btnNavTools != null) {
            btnNavTools.setOnClickListener(v -> showToolsMenu());
        }
    }

    /**
     * Navigate to Salat (Prayer Times) Page
     */
    private void navigateToSalatPage() {
        if (getActivity() instanceof HomeActivity) {
            BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottom_nav);
            if (bottomNav != null) {
                bottomNav.setSelectedItemId(R.id.nav_namaz);
            }
        }
    }

    /**
     * Edit location - Navigate to location settings
     */
    private void editLocation() {
        // Navigate to MainActivity (Settings tab)
        if (getActivity() != null) {
            Intent intent = new Intent(getActivity(), MainActivity.class);
            startActivity(intent);
        }
    }

    /**
     * Show Tools floating menu
     */
    private void showToolsMenu() {
        if (getContext() == null) return;

        String[] toolsItems = {
                getString(R.string.hadith_btn),
                getString(R.string.time_settings),
                "Six Kalmas",
                "Zakat Calculator"
        };

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.tools_menu)
                .setItems(toolsItems, (dialog, which) -> {
                    Intent intent;
                    switch (which) {
                        case 0: // Hadith Books
                            intent = new Intent(getActivity(), HadithActivity.class);
                            startActivity(intent);
                            break;
                        case 1: // Calendar
                            intent = new Intent(getActivity(), CalendarActivity.class);
                            startActivity(intent);
                            break;
                        case 2: // Six Kalmas
                            intent = new Intent(getActivity(), SixKalmasActivity.class);
                            startActivity(intent);
                            break;
                        case 3: // Zakat Calculator
                            intent = new Intent(getActivity(), ZakatCalculatorActivity.class);
                            startActivity(intent);
                            break;
                    }
                })
                .show();
    }

    /**
     * Update Prayer Card with real-time data
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void updatePrayerCard(DayPrayer dayPrayer) {
        if (tvNextPrayerName == null || tvNextPrayerTimeCard == null || tvLocationPrayer == null) {
            return;
        }

        Map<PrayerEnum, LocalDateTime> timings = dayPrayer.getTimings();
        PrayerEnum nextPrayerKey = PrayerUtils.getNextPrayer(timings, LocalDateTime.now());
        PrayerEnum previousPrayerKey = PrayerUtils.getPreviousPrayerKey(nextPrayerKey);

        if (nextPrayerKey != null) {
            // Update prayer name
            String prayerName = requireContext().getResources().getString(
                    getResources().getIdentifier(nextPrayerKey.toString(), "string", requireContext().getPackageName()));
            tvNextPrayerName.setText("Shalat " + prayerName);

            // Update prayer time
            LocalDateTime nextPrayerTime = timings.get(nextPrayerKey);
            if (nextPrayerTime != null) {
                tvNextPrayerTimeCard.setText(UiUtils.formatTiming(nextPrayerTime));

                // Calculate and update remaining time
                long timeRemaining = TimingUtils.getTimeBetweenTwoPrayer(LocalDateTime.now(), nextPrayerTime);
                if (tvTimeRemaining != null) {
                    tvTimeRemaining.setText(getString(R.string.remaining) + ": " + UiUtils.formatTimeForTimer(timeRemaining));
                }
            }
        }

        // Update location
        String locationText;
        if (dayPrayer.getCity() != null) {
            locationText = StringUtils.capitalize(dayPrayer.getCity());
        } else {
            locationText = getString(R.string.common_offline);
        }
        tvLocationPrayer.setText(locationText);
    }

    /**
     * Initialize Verse of the Day Card
     */
    private void initializeVerseOfDayCard() {
        if (verseOfDayCard == null) return;

        // Card click - Navigate to verse detail
        verseOfDayCard.setOnClickListener(v -> {
            if (votdChapterNo > 0 && votdVerseNo > 0) {
                openVerseDetail();
            }
        });

        // Share button
        if (btnShare != null) {
            btnShare.setOnClickListener(v -> shareVerse());
        }

        // Bookmark button
        if (btnBookmark != null) {
            btnBookmark.setOnClickListener(v -> toggleBookmark());
        }

        // Load verse of the day
        loadVerseOfTheDay();
    }

    /**
     * Load Verse of the Day content
     */
    private void loadVerseOfTheDay() {
        if (getContext() == null || tvArabicText == null) return;

        // Show loading indicator
        if (loadingIndicator != null) {
            loadingIndicator.setVisibility(View.VISIBLE);
        }
        if (tvArabicText != null) tvArabicText.setVisibility(View.GONE);
        if (tvTranslationText != null) tvTranslationText.setVisibility(View.GONE);

        // Get QuranMeta from ViewModel or create new instance
        com.quran.quranaudio.online.quran_module.components.quran.QuranMeta.prepareInstance(
            requireContext(),
            quranMeta -> {
                // Get Verse of the Day
                com.quran.quranaudio.online.quran_module.utils.verse.VerseUtils.getVOTD(
                    requireContext(),
                    quranMeta,
                    null,
                    (chapterNo, verseNo) -> {
                        votdChapterNo = chapterNo;
                        votdVerseNo = verseNo;
                        loadVerseContent(quranMeta, chapterNo, verseNo);
                    }
                );
            }
        );
    }

    /**
     * Load verse content (Arabic text and translation)
     */
    private void loadVerseContent(com.quran.quranaudio.online.quran_module.components.quran.QuranMeta quranMeta, int chapterNo, int verseNo) {
        if (getContext() == null) return;

        // Prepare Quran instance
        com.quran.quranaudio.online.quran_module.components.quran.Quran.prepareInstance(
            requireContext(),
            quranMeta,
            quran -> {
                com.quran.quranaudio.online.quran_module.components.quran.subcomponents.Verse verse = 
                    quran.getVerse(chapterNo, verseNo);
                com.quran.quranaudio.online.quran_module.components.quran.subcomponents.Chapter chapter = 
                    quran.getChapter(chapterNo);

                // Update UI on main thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        displayVerseContent(verse, chapter, chapterNo, verseNo);
                    });
                }
            }
        );
    }

    /**
     * Display verse content in the UI
     */
    private void displayVerseContent(
        com.quran.quranaudio.online.quran_module.components.quran.subcomponents.Verse verse,
        com.quran.quranaudio.online.quran_module.components.quran.subcomponents.Chapter chapter,
        int chapterNo,
        int verseNo
    ) {
        if (tvArabicText == null || tvVerseInfo == null) return;

        // Hide loading, show content
        if (loadingIndicator != null) {
            loadingIndicator.setVisibility(View.GONE);
        }
        if (tvArabicText != null) {
            tvArabicText.setVisibility(View.VISIBLE);
            tvArabicText.setText(verse.arabicText);
        }

        // Set verse info
        if (tvVerseInfo != null) {
            String info = chapter.getName() + " " + chapterNo + ":" + verseNo;
            tvVerseInfo.setText(info);
        }

        // Load and display translation
        loadTranslation(chapterNo, verseNo);

        // Update bookmark icon
        updateBookmarkIcon();
    }

    /**
     * Load translation text
     */
    private void loadTranslation(int chapterNo, int verseNo) {
        if (getContext() == null || tvTranslationText == null) return;

        new Thread(() -> {
            com.quran.quranaudio.online.quran_module.utils.reader.factory.QuranTranslationFactory factory = 
                new com.quran.quranaudio.online.quran_module.utils.reader.factory.QuranTranslationFactory(requireContext());

            try {
                // Get first available translation
                java.util.Set<String> savedTranslations = 
                    com.quran.quranaudio.online.quran_module.utils.sharedPrefs.SPReader.getSavedTranslations(requireContext());

                String slug = null;
                for (String savedSlug : savedTranslations) {
                    if (!com.quran.quranaudio.online.quran_module.utils.reader.TranslUtils.isTransliteration(savedSlug)) {
                        slug = savedSlug;
                        break;
                    }
                }

                if (slug == null) {
                    slug = com.quran.quranaudio.online.quran_module.utils.reader.TranslUtils.TRANSL_SLUG_DEFAULT;
                }

                com.quran.quranaudio.online.quran_module.components.quran.subcomponents.Translation translation = 
                    factory.getTranslationsSingleSlugVerse(slug, chapterNo, verseNo);

                if (translation != null && getActivity() != null) {
                    String translText = com.quran.quranaudio.online.quran_module.utils.univ.StringUtils.removeHTML(
                        translation.getText(), false
                    );

                    getActivity().runOnUiThread(() -> {
                        if (tvTranslationText != null) {
                            tvTranslationText.setVisibility(View.VISIBLE);
                            tvTranslationText.setText(translText);
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                factory.close();
            }
        }).start();
    }

    /**
     * Open verse detail in Reader activity
     */
    private void openVerseDetail() {
        if (getContext() == null || votdChapterNo <= 0 || votdVerseNo <= 0) return;

        com.quran.quranaudio.online.quran_module.utils.reader.factory.ReaderFactory.startVerse(
            requireContext(),
            votdChapterNo,
            votdVerseNo
        );
    }

    /**
     * Share verse using Android system share
     */
    private void shareVerse() {
        if (getContext() == null || votdChapterNo <= 0 || votdVerseNo <= 0) return;

        // Get verse content asynchronously
        com.quran.quranaudio.online.quran_module.components.quran.QuranMeta.prepareInstance(
            requireContext(),
            quranMeta -> {
                com.quran.quranaudio.online.quran_module.components.quran.Quran.prepareInstance(
                    requireContext(),
                    quranMeta,
                    quran -> {
                        new Thread(() -> {
                            try {
                                StringBuilder sb = new StringBuilder();

                                com.quran.quranaudio.online.quran_module.components.quran.subcomponents.Verse verse = 
                                    quran.getVerse(votdChapterNo, votdVerseNo);
                                com.quran.quranaudio.online.quran_module.components.quran.subcomponents.Chapter chapter = 
                                    quran.getChapter(votdChapterNo);

                // Add verse info
                sb.append("Quran ").append(votdChapterNo).append(":").append(votdVerseNo).append("\n\n");

                // Add Arabic text
                sb.append(verse.arabicText).append("\n\n");

                // Add translation
                com.quran.quranaudio.online.quran_module.utils.reader.factory.QuranTranslationFactory factory = 
                    new com.quran.quranaudio.online.quran_module.utils.reader.factory.QuranTranslationFactory(requireContext());
                
                java.util.Set<String> savedTranslations = 
                    com.quran.quranaudio.online.quran_module.utils.sharedPrefs.SPReader.getSavedTranslations(requireContext());

                String slug = null;
                for (String savedSlug : savedTranslations) {
                    if (!com.quran.quranaudio.online.quran_module.utils.reader.TranslUtils.isTransliteration(savedSlug)) {
                        slug = savedSlug;
                        break;
                    }
                }

                if (slug == null) {
                    slug = com.quran.quranaudio.online.quran_module.utils.reader.TranslUtils.TRANSL_SLUG_DEFAULT;
                }

                com.quran.quranaudio.online.quran_module.components.quran.subcomponents.Translation translation = 
                    factory.getTranslationsSingleSlugVerse(slug, votdChapterNo, votdVerseNo);

                if (translation != null) {
                    String translText = com.quran.quranaudio.online.quran_module.utils.univ.StringUtils.removeHTML(
                        translation.getText(), false
                    );
                    sb.append(translText).append("\n\n");
                }

                sb.append("- ").append(chapter.getName()).append(" ").append(votdChapterNo).append(":").append(votdVerseNo);

                factory.close();

                                // Share on main thread
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() -> {
                                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                                        shareIntent.setType("text/plain");
                                        shareIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());
                                        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Verse of the Day");
                                        startActivity(Intent.createChooser(shareIntent, getString(R.string.share)));
                                    });
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }).start();
                    }
                );
            }
        );
    }

    /**
     * Toggle bookmark for the verse
     */
    private void toggleBookmark() {
        if (getContext() == null || votdChapterNo <= 0 || votdVerseNo <= 0) return;

        com.quran.quranaudio.online.quran_module.db.bookmark.BookmarkDBHelper bookmarkDBHelper = 
            new com.quran.quranaudio.online.quran_module.db.bookmark.BookmarkDBHelper(requireContext());

        boolean isBookmarked = bookmarkDBHelper.isBookmarked(votdChapterNo, votdVerseNo, votdVerseNo);

        if (isBookmarked) {
            // Remove bookmark
            bookmarkDBHelper.removeFromBookmark(votdChapterNo, votdVerseNo, votdVerseNo, () -> {
                updateBookmarkIcon();
            });
        } else {
            // Add bookmark
            bookmarkDBHelper.addToBookmark(votdChapterNo, votdVerseNo, votdVerseNo, null, model -> {
                updateBookmarkIcon();
            });
        }

        bookmarkDBHelper.close();
    }

    /**
     * Update bookmark icon based on current state
     */
    private void updateBookmarkIcon() {
        if (btnBookmark == null || votdChapterNo <= 0 || votdVerseNo <= 0) return;

        com.quran.quranaudio.online.quran_module.db.bookmark.BookmarkDBHelper bookmarkDBHelper = 
            new com.quran.quranaudio.online.quran_module.db.bookmark.BookmarkDBHelper(requireContext());

        boolean isBookmarked = bookmarkDBHelper.isBookmarked(votdChapterNo, votdVerseNo, votdVerseNo);

        int iconRes = isBookmarked ? R.drawable.dr_icon_bookmark_added : R.drawable.dr_icon_bookmark_outlined;
        btnBookmark.setImageResource(iconRes);

        bookmarkDBHelper.close();
    }

    /**
     * Initialize Live Stream Cards (Mecca & Medina)
     */
    private void initializeLiveStreamCards() {
        // Setup Mecca Live Card
        if (tvMeccaTitle != null) {
            tvMeccaTitle.setText(R.string.mecca_live);
        }
        if (tvMeccaDescription != null) {
            tvMeccaDescription.setText(R.string.mecca_live_description);
        }
        if (meccaLiveCard != null) {
            meccaLiveCard.setOnClickListener(v -> openMeccaLive());
        }

        // Setup Medina Live Card
        if (tvMedinaTitle != null) {
            tvMedinaTitle.setText(R.string.madina_live);
        }
        if (tvMedinaDescription != null) {
            tvMedinaDescription.setText(R.string.medina_live_description);
        }
        if (medinaLiveCard != null) {
            medinaLiveCard.setOnClickListener(v -> openMedinaLive());
        }
    }

    /**
     * Open Mecca Live stream
     */
    private void openMeccaLive() {
        if (getActivity() == null) return;

        // Mecca Live URLs (same as existing implementation)
        String[] meccaLiveUrls = {
            "http://m.live.net.sa:1935/live/quran/playlist.m3u8", // HLS stream (preferred)
            "https://ythls.armelin.one/channel/UCos52-JmjOoBnBOnxJCWAQA.m3u8", // YouTube to HLS
            "https://www.youtube.com/watch?v=e85tJVzKwDU", // YouTube backup 1
            "https://www.youtube.com/watch?v=yd19lGSibQ4"  // YouTube backup 2
        };

        Intent intent = new Intent(getActivity(), LiveActivity.class);
        intent.putExtra("live", meccaLiveUrls[0]);
        intent.putExtra("backup_urls", meccaLiveUrls);
        startActivity(intent);
    }

    /**
     * Open Medina Live stream
     */
    private void openMedinaLive() {
        if (getActivity() == null) return;

        // Medina Live URLs (same as existing implementation)
        String[] medinaLiveUrls = {
            "http://m.live.net.sa:1935/live/sunnah/playlist.m3u8", // HLS stream (preferred)
            "https://ythls.armelin.one/channel/UCJr4gikBowJ8I-iUXs7CkMg.m3u8", // YouTube to HLS
            "https://www.youtube.com/watch?v=4s4XX-qaNgg", // YouTube backup 1
            "https://www.youtube.com/watch?v=0lg0XeJ2gAU", // YouTube backup 2
            "https://www.youtube.com/watch?v=4Ar8JHRCdSE"  // YouTube backup 3
        };

        Intent intent = new Intent(getActivity(), LiveActivity.class);
        intent.putExtra("live", medinaLiveUrls[0]);
        intent.putExtra("backup_urls", medinaLiveUrls);
        startActivity(intent);
    }

}