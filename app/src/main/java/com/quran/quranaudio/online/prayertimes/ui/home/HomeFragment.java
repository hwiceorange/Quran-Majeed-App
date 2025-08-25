package com.quran.quranaudio.online.prayertimes.ui.home;

import android.Manifest;
import android.annotation.SuppressLint;
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

import com.faltenreich.skeletonlayout.Skeleton;
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
import com.quran.quranaudio.online.R;
import com.quran.quranaudio.online.activities.LiveActivity;
import com.quran.quranaudio.online.prayertimes.ui.MainActivity;
import com.quran.quranaudio.online.quran_module.activities.ActivityReaderIndexPage;

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

        HomeViewModel homeViewModel = viewModelFactory.create(HomeViewModel.class);

        View rootView = inflater.inflate(R.layout.fragment_home, container, false);

        initializeViews(rootView);

        //PermissionStart

        mPermissionResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), new ActivityResultCallback<Map<String, Boolean>>() {
            @Override
            public void onActivityResult(Map<String, Boolean> result) {
                if (result.get(Manifest.permission.ACCESS_FINE_LOCATION) !=null) {
                    isLocationPermissionGranted = result.get(Manifest.permission.ACCESS_FINE_LOCATION);
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
                    "https://www.youtube.com/watch?v=4s4XX-qaNgg", // 新的Medina Live YouTube直播（优先）
                    "https://www.youtube.com/watch?v=0lg0XeJ2gAU", // 备用Medina Live YouTube直播
                    "http://m.live.net.sa:1935/live/sunnah/playlist.m3u8",
                    "https://ythls.armelin.one/channel/UCJr4gikBowJ8I-iUXs7CkMg.m3u8",
                    "https://www.youtube.com/watch?v=4Ar8JHRCdSE"
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

}