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
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
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
import com.google.android.material.button.MaterialButton;
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
import com.quran.quranaudio.online.home.quiz.QuizQuestion;
import com.quran.quranaudio.online.home.quiz.QuizRepository;
import com.quran.quranaudio.quiz.QuestionBean;
import com.quran.quranaudio.quiz.base.Constants;
import com.quranaudio.quiz.quiz.QuranQuizNotifyResultActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.quranaudio.common.ad.NativeAdHelper;
import android.widget.FrameLayout;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
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
    private CountDownTimer prayerCardTimer;

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
    private ImageView btnPremium;
    // 折扣挽回角标（折扣窗口有效期内显示 -50% 与倒计时）
    private View discountBadge;
    private TextView tvDiscountBadgePercent;
    private TextView tvDiscountBadgeTimer;
    private final android.os.Handler discountBadgeHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable discountBadgeTicker;
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
    private FrameLayout votdNativeAdContainer;  // 🔥 原生广告容器
    private FrameLayout homeNativeAdContainer;
    private int votdChapterNo = -1;
    private int votdVerseNo = -1;

    // Live Stream Card Views
    private CardView meccaLiveCard;
    private TextView tvMeccaTitle;
    private TextView tvMeccaDescription;
    private CardView medinaLiveCard;
    private TextView tvMedinaTitle;
    private TextView tvMedinaDescription;

    // Daily Quran Quiz Views
    private View quizEntryView;
    private TextView quizQuestionTextView;
    private List<MaterialButton> quizOptionButtons;
    private QuizRepository quizRepository;
    private QuizQuestion currentQuizQuestion;

    private GoogleAuthManager googleAuthManager;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    private CircularProgressBar circularProgressBar;
    private Skeleton skeleton;
    private boolean prayerTimesValueReported;

    @Override
    public void onAttach(@NonNull Context context) {
        android.util.Log.d("NATIVE_AD_TRACK", "═══════════════════════════════════════════════");
        android.util.Log.d("NATIVE_AD_TRACK", "🎯 HomeFragment.onAttach() CALLED");
        android.util.Log.d("NATIVE_AD_TRACK", "   context: " + (context != null ? context.getClass().getSimpleName() : "NULL"));
        android.util.Log.d("NATIVE_AD_TRACK", "═══════════════════════════════════════════════");
        
        ((App) requireContext().getApplicationContext())
                .appComponent
                .homeComponent()
                .create()
                .inject(this);

        super.onAttach(context);
        
        android.util.Log.d("NATIVE_AD_TRACK", "✅ HomeFragment.onAttach() COMPLETED");
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        android.util.Log.d("NATIVE_AD_TRACK", "═══════════════════════════════════════════════");
        android.util.Log.d("NATIVE_AD_TRACK", "🎯 HomeFragment.onCreateView() START");
        android.util.Log.d("NATIVE_AD_TRACK", "   inflater: " + (inflater != null ? "NOT NULL" : "NULL"));
        android.util.Log.d("NATIVE_AD_TRACK", "   container: " + (container != null ? "NOT NULL" : "NULL"));
        android.util.Log.d("NATIVE_AD_TRACK", "   savedInstanceState: " + (savedInstanceState != null ? "NOT NULL" : "NULL"));
        android.util.Log.d("NATIVE_AD_TRACK", "═══════════════════════════════════════════════");

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

        quizRepository = new QuizRepository(requireContext());

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

        android.util.Log.d("NATIVE_AD_TRACK", "═══════════════════════════════════════════════");
        android.util.Log.d("NATIVE_AD_TRACK", "🎯 HomeFragment.onCreateView() START");
        android.util.Log.d("NATIVE_AD_TRACK", "═══════════════════════════════════════════════");
        
        initializeViews(rootView);
        View quietReadingEntry = rootView.findViewById(R.id.quiet_reading_entry);
        boolean alreadyAdFree = com.quranaudio.common.ad.SubscriptionChecker.shouldHideAds(requireContext());
        quietReadingEntry.setVisibility(alreadyAdFree ? View.GONE : View.VISIBLE);
        quietReadingEntry.setOnClickListener(v -> {
            if (getActivity() != null && !getActivity().isFinishing()) {
                new com.quran.quranaudio.online.subscription.AdFreeDialog(getActivity()).show();
            }
        });
        android.util.Log.d("NATIVE_AD_TRACK", "✅ initializeViews() completed");
        
        initializeQuizEntry(rootView);
        android.util.Log.d("NATIVE_AD_TRACK", "✅ initializeQuizEntry() completed");
        
        initializeHeaderListeners();
        android.util.Log.d("NATIVE_AD_TRACK", "✅ initializeHeaderListeners() completed");
        
        initializePrayerCardListeners();
        android.util.Log.d("NATIVE_AD_TRACK", "✅ initializePrayerCardListeners() completed");
        
        android.util.Log.d("NATIVE_AD_TRACK", "→ Calling initializeVerseOfDayCard()...");
        initializeVerseOfDayCard();
        android.util.Log.d("NATIVE_AD_TRACK", "✅ initializeVerseOfDayCard() completed");
        
        initializeLiveStreamCards();
        android.util.Log.d("NATIVE_AD_TRACK", "✅ initializeLiveStreamCards() completed");
        
        updateHeaderUI();
        android.util.Log.d("NATIVE_AD_TRACK", "✅ updateHeaderUI() completed");
        
        android.util.Log.d("NATIVE_AD_TRACK", "═══════════════════════════════════════════════");
        android.util.Log.d("NATIVE_AD_TRACK", "✅ HomeFragment.onCreateView() COMPLETED");
        android.util.Log.d("NATIVE_AD_TRACK", "═══════════════════════════════════════════════");

        //PermissionStart

        mPermissionResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), new ActivityResultCallback<Map<String, Boolean>>() {
            @Override
            public void onActivityResult(Map<String, Boolean> result) {
                if (result.containsKey(Manifest.permission.ACCESS_FINE_LOCATION)
                        || result.containsKey(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    isLocationPermissionGranted = Boolean.TRUE.equals(
                            result.get(Manifest.permission.ACCESS_FINE_LOCATION))
                            || Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));
                    
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
                            homeViewModel.forceRefreshLocation();
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
            if (dayPrayer == null || dayPrayer.getTimings() == null || dayPrayer.getTimings().isEmpty()) {
                return;
            }
            updateDatesTextViews(dayPrayer);
            updateNextPrayerViews(dayPrayer);
            updateTimingsTextViews(dayPrayer);
            updateHeaderPrayerInfo(dayPrayer);
            updatePrayerCard(dayPrayer);  // Update Prayer Card with real-time data
            startPrayerSchedulerWork(dayPrayer);

            // 只有真实礼拜时间数据已经返回并可展示，才记为用户获得了礼拜价值。
            com.quran.quranaudio.online.analytics.RetentionFunnel.firstValue(
                    requireContext(), "prayer_times_loaded");
            if (!prayerTimesValueReported) {
                prayerTimesValueReported = true;
                com.quran.quranaudio.online.analytics.RetentionFunnel.valueAction(
                        requireContext(), "prayer_times_viewed", 0L, dayPrayer.getTimings().size());
            }

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
        setupContinueReading(rootView);
        return rootView;
    }

    // 首页"继续阅读"卡片相关视图
    private View continueReadingCard;
    private android.widget.TextView continueReadingPosition;

    /**
     * 初始化"继续阅读"卡片：从本地读取上次阅读位置(同步)，解析章名后展示，
     * 点击直达阅读器该位置。无记录则整卡隐藏。这是古兰经 App 最强的日活回归钩子。
     */
    private com.google.android.material.progressindicator.CircularProgressIndicator juzProgressRing;
    private android.widget.TextView juzNumberText;
    private android.widget.TextView juzProgressLabel;

    // Khatmah 计划卡片视图
    private View khatmahCard;
    private android.widget.TextView khatmahStatus;
    private android.widget.TextView khatmahCta;
    private View khatmahActiveGroup;
    private android.widget.TextView khatmahDay;
    private com.google.android.material.progressindicator.LinearProgressIndicator khatmahProgress;

    private void setupContinueReading(View rootView) {
        try {
            // 注意：<include android:id> 会覆盖被包含布局的根 id，故根卡片用 _include id
            continueReadingCard = rootView.findViewById(R.id.continue_reading_card_include);
            continueReadingPosition = rootView.findViewById(R.id.tv_continue_reading_position);
            juzProgressRing = rootView.findViewById(R.id.juz_progress_ring);
            juzNumberText = rootView.findViewById(R.id.tv_juz_number);
            juzProgressLabel = rootView.findViewById(R.id.tv_juz_progress_label);

            khatmahCard = rootView.findViewById(R.id.khatmah_card_include);
            khatmahStatus = rootView.findViewById(R.id.tv_khatmah_status);
            khatmahCta = rootView.findViewById(R.id.khatmah_cta);
            khatmahActiveGroup = rootView.findViewById(R.id.khatmah_active_group);
            khatmahDay = rootView.findViewById(R.id.tv_khatmah_day);
            khatmahProgress = rootView.findViewById(R.id.khatmah_progress);

            refreshContinueReading();
        } catch (Exception e) {
            android.util.Log.w("HomeFragment", "setupContinueReading failed", e);
        }
    }

    /**
     * 由(章,节)计算当前 Juz(1-30)。遍历该章所属的 Juz，命中节所在范围者即当前 Juz。
     * 返回 0 表示无法判定(不显示进度环)。
     */
    private int computeJuz(com.quran.quranaudio.online.quran_module.components.quran.QuranMeta quranMeta,
                           int surah, int ayah) {
        try {
            java.util.ArrayList<Integer> juzs = quranMeta.getChapterJuzs(surah);
            if (juzs == null || juzs.isEmpty()) return 0;
            for (Integer juz : juzs) {
                kotlin.Pair<Integer, Integer> range =
                        quranMeta.getVerseRangeOfChapterInJuz(juz, surah);
                if (range != null && ayah >= range.getFirst() && ayah <= range.getSecond()) {
                    return juz;
                }
            }
            // 兜底：返回该章第一个 Juz
            return juzs.get(0);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Khatmah 通读计划卡片：
     * - 有计划：显示第几天/目标、已读 Juz、进度条、领先/落后状态；
     * - 无计划但已有阅读：显示"开始 30 天读完古兰经" CTA；
     * - 无计划且无阅读：整卡隐藏(不打扰 day-0 用户)。
     * 全部基于 SharedPreferences 计划 + 已算好的 actualJuz，不碰数据库。
     */
    private void refreshKhatmahUI(int actualJuz) {
        if (khatmahCard == null || getContext() == null) return;
        try {
            final android.content.Context ctx = requireContext();

            // 记录今日进度(供每日提醒判断"今天是否已读"，已读则不再打扰)
            com.quran.quranaudio.online.quran_module.utils.KhatmahPlanManager
                    .recordProgress(ctx, actualJuz);

            final boolean active = com.quran.quranaudio.online.quran_module.utils
                    .KhatmahPlanManager.isActive(ctx);

            if (!active && actualJuz <= 0) {
                khatmahCard.setVisibility(View.GONE);
                return;
            }
            khatmahCard.setVisibility(View.VISIBLE);

            if (!active) {
                // 无计划：CTA，点击开始 30 天计划
                khatmahCta.setVisibility(View.VISIBLE);
                khatmahActiveGroup.setVisibility(View.GONE);
                khatmahStatus.setText("");
                khatmahCard.setOnClickListener(v -> {
                    com.quran.quranaudio.online.quran_module.utils.KhatmahPlanManager
                            .startPlan(ctx, com.quran.quranaudio.online.quran_module.utils
                                    .KhatmahPlanManager.DEFAULT_TARGET_DAYS);
                    // 开始计划即排下一次温和提醒(Fajr+3h)
                    com.quran.quranaudio.online.dailyverse.KhatmahReminderScheduler.scheduleNext(ctx);
                    refreshKhatmahUI(actualJuz);
                });
                return;
            }

            // 有计划：进度 + 状态
            khatmahCta.setVisibility(View.GONE);
            khatmahActiveGroup.setVisibility(View.VISIBLE);
            khatmahCard.setOnClickListener(null);

            int target = com.quran.quranaudio.online.quran_module.utils.KhatmahPlanManager.getTargetDays(ctx);
            int day = com.quran.quranaudio.online.quran_module.utils.KhatmahPlanManager.getElapsedDay(ctx);
            khatmahDay.setText(getString(R.string.khatmah_day_format, Math.min(day, target), target, actualJuz, 30));
            khatmahProgress.setProgress(actualJuz);

            com.quran.quranaudio.online.quran_module.utils.KhatmahPlanManager.Status status =
                    com.quran.quranaudio.online.quran_module.utils.KhatmahPlanManager.getStatus(ctx, actualJuz);
            switch (status) {
                case COMPLETED:
                    khatmahStatus.setText(getString(R.string.khatmah_completed));
                    khatmahStatus.setTextColor(0xFF4E8545);
                    break;
                case AHEAD:
                    khatmahStatus.setText(getString(R.string.khatmah_ahead));
                    khatmahStatus.setTextColor(0xFF4E8545);
                    break;
                case ON_TRACK:
                    khatmahStatus.setText(getString(R.string.khatmah_on_track));
                    khatmahStatus.setTextColor(0xFF4E8545);
                    break;
                case BEHIND:
                    int behind = com.quran.quranaudio.online.quran_module.utils
                            .KhatmahPlanManager.getBehindBy(ctx, actualJuz);
                    khatmahStatus.setText(getString(R.string.khatmah_behind, behind));
                    khatmahStatus.setTextColor(0xFFD9822B);
                    break;
            }
        } catch (Exception e) {
            android.util.Log.w("HomeFragment", "refreshKhatmahUI failed", e);
            if (khatmahCard != null) khatmahCard.setVisibility(View.GONE);
        }
    }

    private void refreshContinueReading() {
        if (continueReadingCard == null || getContext() == null) return;
        try {
            final int surah = com.quran.quranaudio.online.features.Helper.LastSurahAndAyahHelper
                    .getLastSurah(requireContext());
            final int ayah = com.quran.quranaudio.online.features.Helper.LastSurahAndAyahHelper
                    .getLastAyah(requireContext());

            final boolean hasReading = surah > 0 && ayah > 0;

            // 始终准备 QuranMeta 以计算 Juz(Khatmah 卡在无阅读记录时也需展示"开始计划")
            com.quran.quranaudio.online.quran_module.components.quran.QuranMeta.prepareInstance(
                    requireContext(),
                    quranMeta -> {
                        // 回调可能在冷启动时异步/延后触发：此时 Fragment 可能已分离，
                        // 必须用 isAdded() 守卫 + try-catch，避免 requireContext() 抛异常导致崩溃
                        if (!isAdded() || getActivity() == null || continueReadingPosition == null) return;
                        try {
                        final int juz = hasReading ? computeJuz(quranMeta, surah, ayah) : 0;
                        final String text = hasReading
                                ? getString(R.string.continue_reading_position_format,
                                        quranMeta.getChapterName(requireContext(), surah), ayah)
                                : "";
                        getActivity().runOnUiThread(() -> {
                            // Khatmah 计划卡(独立于继续阅读卡)
                            refreshKhatmahUI(juz);

                            if (!hasReading) {
                                continueReadingCard.setVisibility(View.GONE);
                                return;
                            }

                            continueReadingPosition.setText(text);

                            // Khatmah 进度环：当前 Juz / 30
                            if (juz > 0 && juzProgressRing != null) {
                                juzProgressRing.setProgress(juz);
                                if (juzNumberText != null) {
                                    juzNumberText.setText(String.valueOf(juz));
                                }
                                if (juzProgressLabel != null) {
                                    juzProgressLabel.setText(
                                            getString(R.string.juz_progress_format, juz, 30));
                                    juzProgressLabel.setVisibility(View.VISIBLE);
                                }
                                juzProgressRing.setVisibility(View.VISIBLE);
                                if (juzNumberText != null) juzNumberText.setVisibility(View.VISIBLE);
                            }

                            continueReadingCard.setVisibility(View.VISIBLE);
                            // 点击：打开整章并定位到上次的节(而非孤立单节视图)，
                            // 让用户能从该节继续往下读——这才是"继续阅读"的正确语义
                            continueReadingCard.setOnClickListener(v -> openContinueReading(quranMeta, surah, ayah));
                        });
                        } catch (Exception e) {
                            android.util.Log.w("HomeFragment", "continue-reading callback failed", e);
                        }
                    });
        } catch (Exception e) {
            continueReadingCard.setVisibility(View.GONE);
        }
    }

    /**
     * 打开整章并滚动到指定节。用 prepareLastVersesIntent(CHAPTER 模式 + PENDING_SCROLL)，
     * 使用户可从该节继续向下阅读，而非只看孤立一节。失败时回退到单节视图。
     */
    private void openContinueReading(
            com.quran.quranaudio.online.quran_module.components.quran.QuranMeta quranMeta,
            int surah, int ayah) {
        try {
            android.content.Intent intent =
                    com.quran.quranaudio.online.quran_module.utils.reader.factory.ReaderFactory
                            .prepareLastVersesIntent(quranMeta, 0, surah, ayah, ayah,
                                    com.quran.quranaudio.online.quran_module.reader_managers
                                            .ReaderParams.READER_READ_TYPE_CHAPTER, -1);
            if (intent != null) {
                intent.setClass(requireContext(),
                        com.quran.quranaudio.online.quran_module.activities.ActivityReader.class);
                startActivity(intent);
            } else {
                com.quran.quranaudio.online.quran_module.utils.reader.factory.ReaderFactory
                        .startVerse(requireContext(), surah, ayah);
            }
        } catch (Exception e) {
            android.util.Log.w("HomeFragment", "open continue reading failed", e);
        }
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
            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        return isLocationPermissionGranted;
    }
    private void showPermissionWarning(){
        dialogWarning=new AlertDialog.Builder(getActivity()).setView(R.layout.layout_dialog_location_warning).create();
        TextView skip=dialogWarning.findViewById(R.id.btn_skip);
        MaterialButton enable=dialogWarning.findViewById(R.id.btn_enable_location);
        skip.setOnClickListener(dialogListener);
        enable.setOnClickListener(dialogListener);
        
        // Set transparent background for modern card design
        if (dialogWarning.getWindow() != null) {
            dialogWarning.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        dialogWarning.show();
    }
    private void requestPermission(){


        List<String> permissionRequest = new ArrayList<String>();

        if (!isLocationPermissionGranted) {
            permissionRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
            permissionRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION);
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
        android.util.Log.d("NATIVE_AD_TRACK", "═══════════════════════════════════════════════");
        android.util.Log.d("NATIVE_AD_TRACK", "🎯 HomeFragment.onResume() CALLED");
        android.util.Log.d("NATIVE_AD_TRACK", "   isAdded: " + isAdded());
        android.util.Log.d("NATIVE_AD_TRACK", "   isVisible: " + isVisible());
        android.util.Log.d("NATIVE_AD_TRACK", "   isResumed: " + isResumed());
        android.util.Log.d("NATIVE_AD_TRACK", "═══════════════════════════════════════════════");
        
        super.onResume();
        if (isAdded() && checkLocationPermission()) {
            HomeViewModel homeViewModel = new ViewModelProvider(requireActivity(), viewModelFactory)
                    .get(HomeViewModel.class);
            homeViewModel.refreshLocationIfStale();
        }
        //Initialize();
        if(allowRefresh){
            allowRefresh=false;
            //call your initialization code here
        }

        // 用户可能在其他页面读过经，回到首页刷新"继续阅读"位置
        refreshContinueReading();
        updatePremiumEntryVisibility();
        startDiscountBadgeTicker();

        // 💳 情境化订阅触发：仅对"已读过经文(产生价值)的非订阅用户"在自然节点软性提示，
        // 替代此前"仅首装硬弹付费墙"(价值前收费、转化极低)。内部有频控与会话门控。
        try {
            if (getContext() != null) {
                boolean hasReadingValue = com.quran.quranaudio.online.features.Helper
                        .LastSurahAndAyahHelper.getLastSurah(requireContext()) > 0;
                com.quran.quranaudio.online.subscription.SubscriptionHelper.INSTANCE
                        .maybeShowContextualPrompt(requireContext(), hasReadingValue);
            }
        } catch (Exception e) {
            android.util.Log.w("HomeFragment", "contextual subscription prompt failed", e);
        }
        bindCurrentQuizQuestion();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (!allowRefresh)
            allowRefresh = true;
        stopDiscountBadgeTicker();
    }

    private void initializeQuizEntry(View rootView) {
        quizEntryView = rootView.findViewById(R.id.quiz_entry_view);
        if (quizEntryView == null) {
            return;
        }

        quizQuestionTextView = quizEntryView.findViewById(R.id.tv_question_text);

        MaterialButton optionA = quizEntryView.findViewById(R.id.btn_option_a);
        MaterialButton optionB = quizEntryView.findViewById(R.id.btn_option_b);
        MaterialButton optionC = quizEntryView.findViewById(R.id.btn_option_c);
        MaterialButton optionD = quizEntryView.findViewById(R.id.btn_option_d);

        quizOptionButtons = Arrays.asList(optionA, optionB, optionC, optionD);

        bindCurrentQuizQuestion();
    }

    private void initializeViews(View rootView) {
        skeleton = rootView.findViewById(R.id.skeletonLayout);

        // Initialize Header Views
        View headerView = rootView.findViewById(R.id.home_header);
        if (headerView != null) {
            tvGreeting = headerView.findViewById(R.id.tv_greeting);
            tvUserName = headerView.findViewById(R.id.tv_user_name);
            btnSearch = headerView.findViewById(R.id.btn_search);
            btnPremium = headerView.findViewById(R.id.btn_premium);
            discountBadge = headerView.findViewById(R.id.discount_badge);
            tvDiscountBadgePercent = headerView.findViewById(R.id.tv_discount_badge_percent);
            tvDiscountBadgeTimer = headerView.findViewById(R.id.tv_discount_badge_timer);
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
            
            android.util.Log.d("HomeFragment", "Prayer Card initialized - tvTimeRemaining is " + (tvTimeRemaining == null ? "NULL" : "NOT NULL"));
        } else {
            android.util.Log.e("HomeFragment", "Prayer Card View is NULL!");
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
            votdNativeAdContainer = verseOfDayCardView.findViewById(R.id.votd_native_ad_container);  // 🔥 原生广告容器
        }

        homeNativeAdContainer = rootView.findViewById(R.id.home_native_ad_container);
        if (homeNativeAdContainer != null && getActivity() != null) {
            com.quranaudio.common.ad.NativeAdHelper.INSTANCE.displayNativeAdWithAutoLoad(
                getActivity(),
                homeNativeAdContainer,
                com.quran.quranaudio.quiz.R.layout.layout_ad_native_small_wrapper
            );
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

        bindCurrentQuizQuestion();

    }

    private void bindCurrentQuizQuestion() {
        if (quizEntryView == null || quizRepository == null) {
            return;
        }

        if (!isQuizSupportedLanguage()) {
            quizEntryView.setVisibility(View.GONE);
            return;
        }

        currentQuizQuestion = quizRepository.getCurrentQuestion();

        if (currentQuizQuestion == null) {
            quizEntryView.setVisibility(View.GONE);
            return;
        }

        quizEntryView.setVisibility(View.VISIBLE);

        if (quizQuestionTextView != null) {
            quizQuestionTextView.setText(currentQuizQuestion.getQuestionText());
            int quizColor = ContextCompat.getColor(requireContext(), R.color.quran_quiz_green_dark);
            quizQuestionTextView.setTextColor(quizColor);
        }

        if (quizOptionButtons == null || quizOptionButtons.isEmpty()) {
            return;
        }

        List<String> options = currentQuizQuestion.getOptions();
        for (int i = 0; i < quizOptionButtons.size(); i++) {
            MaterialButton button = quizOptionButtons.get(i);
            if (i < options.size()) {
                button.setVisibility(View.VISIBLE);
                button.setText(getOptionPrefix(i) + " " + options.get(i));
                final int index = i;
                button.setOnClickListener(v -> handleQuizOptionSelected(index));
            } else {
                button.setVisibility(View.GONE);
            }
        }
    }

    @SuppressWarnings("deprecation")
    private boolean isQuizSupportedLanguage() {
        Locale activeLocale;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            activeLocale = requireContext().getResources().getConfiguration().getLocales().get(0);
        } else {
            activeLocale = requireContext().getResources().getConfiguration().locale;
        }

        String languageCode = activeLocale != null ? activeLocale.getLanguage() : null;

        if (TextUtils.isEmpty(languageCode)) {
            languageCode = Locale.getDefault().getLanguage();
        }

        if (TextUtils.isEmpty(languageCode)) {
            return false;
        }

        languageCode = languageCode.toLowerCase(Locale.US);

        return "en".equals(languageCode) || "in".equals(languageCode) || "id".equals(languageCode);
    }

    private String getOptionPrefix(int index) {
        switch (index) {
            case 0:
                return "A";
            case 1:
                return "B";
            case 2:
                return "C";
            case 3:
                return "D";
            default:
                return "";
        }
    }

    private void handleQuizOptionSelected(int selectedIndex) {
        if (currentQuizQuestion == null) {
            return;
        }

        quizRepository.markQuestionAnswered(currentQuizQuestion.getId());

        // Launch QuranQuizNotifyResultActivity (result page) directly
        launchQuizResultPage(selectedIndex);
    }

    private void launchQuizResultPage(int selectedIndex) {
        try {
            if (currentQuizQuestion == null) {
                return;
            }

            QuestionBean questionBean = buildQuestionBeanForQuiz(currentQuizQuestion);
            if (questionBean == null) {
                return;
            }

            // Get the selected answer key (A, B, C, or D)
            String selectedAnswerKey = getOptionPrefix(selectedIndex);

            Bundle bundle = new Bundle();
            bundle.putParcelable(Constants.INTENT_NOTIFY_QUIZ_BEAN, questionBean);
            bundle.putString(Constants.INTENT_NOTIFY_QUIZ_SELECT_ANSWER, selectedAnswerKey);

            QuranQuizNotifyResultActivity.Companion.open(requireContext(), bundle);

        } catch (Exception e) {
            android.util.Log.e("HomeFragment", "Failed to launch quiz result page", e);
        }
    }

    private QuestionBean buildQuestionBeanForQuiz(QuizQuestion quizQuestion) {
        try {
            if (quizQuestion == null) {
                return null;
            }

            java.util.List<String> options = quizQuestion.getOptions();
            if (options == null || options.isEmpty()) {
                return null;
            }

            TreeMap<String, String> optionMap = new TreeMap<>();
            String[] keys = {"A", "B", "C", "D"};
            int count = Math.min(options.size(), keys.length);
            for (int i = 0; i < count; i++) {
                optionMap.put(keys[i], options.get(i));
            }

            String correctKey = getOptionPrefix(quizQuestion.getCorrectAnswerIndex());

            return new QuestionBean(
                String.valueOf(quizQuestion.getId()),  // Convert int to String
                quizQuestion.getQuestionText(),
                optionMap,
                0,
                correctKey,
                "",  // Category
                "",  // Subclass
                0,   // surah_id
                0,   // ayah_id
                "",  // tafsir_brief
                "",  // tafsir_detailed
                ""   // explanation
            );

        } catch (Exception e) {
            android.util.Log.e("HomeFragment", "Failed to build QuestionBean", e);
            return null;
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

        LocalDateTime sunriseTiming = dayPrayer.getComplementaryTiming().get(ComplementaryTimingEnum.SUNRISE);
        LocalDateTime sunsetTiming = dayPrayer.getComplementaryTiming().get(ComplementaryTimingEnum.SUNSET);

        sunriseTimingTextView.setText(UiUtils.formatTiming(Objects.requireNonNull(sunriseTiming)));
        sunsetTimingTextView.setText(UiUtils.formatTiming(Objects.requireNonNull(sunsetTiming)));
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

        prayerNametextView.setText (getString(R.string.intro_next) + ": " + prayerName);
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

    }

    private float getProgressBarPercentage(long timeRemaining, long timeBetween) {
        return 100 - ((float) (timeRemaining * 100) / (timeBetween));
    }

    private void startAnimationTimer(final long timeRemaining, final long timeBetween, final DayPrayer dayPrayer) {
        circularProgressBar.setProgressWithAnimation(getProgressBarPercentage(timeRemaining, timeBetween), 1000L);
        TimeRemainingCTimer = new CountDownTimer(timeRemaining, 1000L) {
            public void onTick(long millisUntilFinished) {
                // Check if fragment is still attached to avoid crashes
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
        if (prayerCardTimer != null)
            prayerCardTimer.cancel();
    }


    private void startPrayerSchedulerWork(DayPrayer dayPrayer) {
        WorkCreator.scheduleOneTimePrayerUpdater(requireContext(), dayPrayer);
    }

    /**
     * Start countdown timer for Prayer Card
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startPrayerCardTimer(final long timeRemaining, final DayPrayer dayPrayer) {
        if (prayerCardTimer != null) {
            prayerCardTimer.cancel();
        }
        
        prayerCardTimer = new CountDownTimer(timeRemaining, 1000L) {
            public void onTick(long millisUntilFinished) {
                if (tvTimeRemaining != null) {
                    tvTimeRemaining.setText(getString(R.string.remaining) + ": " + UiUtils.formatTimeForTimer(millisUntilFinished));
                }
            }

            @RequiresApi(api = Build.VERSION_CODES.O)
            public void onFinish() {
                updatePrayerCard(dayPrayer);
            }
        };
        prayerCardTimer.start();
    }

    /**
     * Initialize Header click listeners
     */
    private void initializeHeaderListeners() {
        if (btnPremium != null) {
            btnPremium.setOnClickListener(v -> {
                // 折扣窗口有效期内，点击 Premium 入口直接进对应方案的折扣挽回页；否则进常规订阅页。
                com.quran.quranaudio.online.subscription.DiscountManager.Plan active =
                        getContext() == null ? null
                        : com.quran.quranaudio.online.subscription.DiscountManager.INSTANCE
                                .activePlan(requireContext());
                if (active != null) {
                    startActivity(new Intent(getContext(),
                            com.quran.quranaudio.online.subscription.DiscountActivity.class)
                            .putExtra(com.quran.quranaudio.online.subscription.DiscountActivity.EXTRA_PLAN,
                                    active.getKey()));
                } else {
                    com.quran.quranaudio.online.subscription.SubscriptionHelper.INSTANCE
                            .launchSubscriptionPage(requireContext(), "home_header");
                }
            });
        }
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

    private void updatePremiumEntryVisibility() {
        if (btnPremium != null && getContext() != null) {
            btnPremium.setVisibility(
                    com.quran.quranaudio.online.subscription.SubscriptionHelper.INSTANCE
                            .isUserSubscribed(requireContext()) ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * 刷新折扣角标：折扣窗口有效期内显示 -50% 与 MM:SS 倒计时，过期或未订阅态变化时自动隐藏。
     * 每秒由 discountBadgeTicker 驱动。
     */
    private void updateDiscountBadge() {
        if (discountBadge == null || getContext() == null) return;

        com.quran.quranaudio.online.subscription.DiscountManager mgr =
                com.quran.quranaudio.online.subscription.DiscountManager.INSTANCE;
        boolean subscribed = com.quran.quranaudio.online.subscription.SubscriptionHelper.INSTANCE
                .isUserSubscribed(requireContext());

        com.quran.quranaudio.online.subscription.DiscountManager.Plan active =
                mgr.activePlan(requireContext());
        long remaining = active == null ? 0 : mgr.remainingMillis(requireContext(), active);
        if (subscribed || active == null || remaining <= 0) {
            discountBadge.setVisibility(View.GONE);
            return;
        }

        int percent = mgr.cachedDiscountPercent(requireContext(), active);
        if (tvDiscountBadgePercent != null) {
            tvDiscountBadgePercent.setText(percent > 0
                    ? getString(R.string.discount_percent_value, percent)
                    : getString(R.string.discount_badge_percent));
        }
        if (tvDiscountBadgeTimer != null) {
            long minutes = remaining / 60000L;
            long seconds = (remaining / 1000L) % 60L;
            tvDiscountBadgeTimer.setText(String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds));
        }
        discountBadge.setVisibility(View.VISIBLE);
    }

    private void startDiscountBadgeTicker() {
        stopDiscountBadgeTicker();
        discountBadgeTicker = new Runnable() {
            @Override
            public void run() {
                updateDiscountBadge();
                discountBadgeHandler.postDelayed(this, 1000L);
            }
        };
        discountBadgeHandler.post(discountBadgeTicker);
    }

    private void stopDiscountBadgeTicker() {
        if (discountBadgeTicker != null) {
            discountBadgeHandler.removeCallbacks(discountBadgeTicker);
            discountBadgeTicker = null;
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
                .setMessage(R.string.logout_message)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    googleAuthManager.signOut(() -> {
                        updateHeaderUI();
                        if (getContext() != null) {
                            android.widget.Toast.makeText(getContext(), 
                                getString(R.string.logged_out_successfully), 
                                android.widget.Toast.LENGTH_SHORT).show();
                        }
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

        // 顺手缓存今日礼拜时间戳，供折扣挽回页做功修避让（±10 分钟内不打扰）。
        try {
            if (getContext() == null || dayPrayer == null || dayPrayer.getTimings() == null) return;
            java.util.List<Long> epochs = new java.util.ArrayList<>();
            for (java.time.LocalDateTime t : dayPrayer.getTimings().values()) {
                if (t != null) {
                    epochs.add(t.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
                }
            }
            com.quran.quranaudio.online.subscription.DiscountManager.INSTANCE
                    .cacheTodayPrayerEpochs(requireContext(), epochs);
        } catch (Exception e) {
            android.util.Log.w("HomeFragment", "cache prayer epochs failed", e);
        }
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
        android.util.Log.d("HomeFragment", "updatePrayerCard called");
        android.util.Log.d("HomeFragment", "tvTimeRemaining is " + (tvTimeRemaining == null ? "NULL" : "NOT NULL"));
        
        if (tvNextPrayerName == null || tvNextPrayerTimeCard == null || tvLocationPrayer == null) {
            android.util.Log.e("HomeFragment", "Prayer Card views are null, skipping update");
            return;
        }

        Map<PrayerEnum, LocalDateTime> timings = dayPrayer.getTimings();
        PrayerEnum nextPrayerKey = PrayerUtils.getNextPrayer(timings, LocalDateTime.now());
        PrayerEnum previousPrayerKey = PrayerUtils.getPreviousPrayerKey(nextPrayerKey);

        if (nextPrayerKey != null) {
            // Update prayer name
            String prayerName = requireContext().getResources().getString(
                    getResources().getIdentifier(nextPrayerKey.toString(), "string", requireContext().getPackageName()));
            tvNextPrayerName.setText(getString(R.string.prayer_label) + " " + prayerName);

            // Update prayer time
            LocalDateTime nextPrayerTime = timings.get(nextPrayerKey);
            if (nextPrayerTime != null) {
                tvNextPrayerTimeCard.setText(UiUtils.formatTiming(nextPrayerTime));

                // Calculate and update remaining time
                long timeRemaining = TimingUtils.getTimeBetweenTwoPrayer(LocalDateTime.now(), nextPrayerTime);
                android.util.Log.d("HomeFragment", "Time remaining: " + timeRemaining + "ms");
                
                if (tvTimeRemaining != null) {
                    tvTimeRemaining.setVisibility(View.VISIBLE);
                    String remainingText = getString(R.string.remaining) + ": " + UiUtils.formatTimeForTimer(timeRemaining);
                    tvTimeRemaining.setText(remainingText);
                    android.util.Log.d("HomeFragment", "Set tvTimeRemaining text to: " + remainingText);
                    
                    // Start countdown timer for Prayer Card
                    startPrayerCardTimer(timeRemaining, dayPrayer);
                } else {
                    android.util.Log.e("HomeFragment", "tvTimeRemaining is NULL - cannot update!");
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
        android.util.Log.d("NATIVE_AD_TRACK", "═══════════════════════════════════════════════");
        android.util.Log.d("NATIVE_AD_TRACK", "🎯 HomeFragment.initializeVerseOfDayCard() CALLED");
        android.util.Log.d("NATIVE_AD_TRACK", "   verseOfDayCard: " + (verseOfDayCard != null ? "NOT NULL" : "NULL"));
        android.util.Log.d("NATIVE_AD_TRACK", "   votdNativeAdContainer: " + (votdNativeAdContainer != null ? "NOT NULL" : "NULL"));
        android.util.Log.d("NATIVE_AD_TRACK", "═══════════════════════════════════════════════");
        
        android.util.Log.d("DIAGNOSE", "→→ HomeFragment.initializeVerseOfDayCard() called");
        android.util.Log.d("DIAGNOSE", "→→ verseOfDayCard: " + (verseOfDayCard != null ? "NOT NULL" : "NULL"));
        
        if (verseOfDayCard == null) {
            android.util.Log.e("DIAGNOSE_ERROR", "❌ verseOfDayCard is NULL, returning");
            return;
        }

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
        android.util.Log.d("DIAGNOSE", "→→ Calling loadVerseOfTheDay()...");
        loadVerseOfTheDay();
        
        // 🔥 Load native ad at bottom of card
        android.util.Log.d("NATIVE_AD_TRACK", "→ Calling loadVOTDNativeAd()...");
        android.util.Log.d("DIAGNOSE", "→→ Calling loadVOTDNativeAd()...");
        loadVOTDNativeAd();
        android.util.Log.d("NATIVE_AD_TRACK", "✅ loadVOTDNativeAd() returned");
        android.util.Log.d("DIAGNOSE", "✅ initializeVerseOfDayCard() completed");
        android.util.Log.d("NATIVE_AD_TRACK", "✅ HomeFragment.initializeVerseOfDayCard() COMPLETED");
        android.util.Log.d("NATIVE_AD_TRACK", "═══════════════════════════════════════════════");
    }
    
    /**
     * 🔥 加载 Verse of the Day 卡片底部的原生广告
     * 复用 Quiz Review & Learn 的样式和逻辑
     */
    private void loadVOTDNativeAd() {
        android.util.Log.d("NATIVE_AD_TRACK", "═══════════════════════════════════════════════");
        android.util.Log.d("NATIVE_AD_TRACK", "🎯 HomeFragment.loadVOTDNativeAd() CALLED");
        android.util.Log.d("NATIVE_AD_TRACK", "   votdNativeAdContainer: " + (votdNativeAdContainer != null ? "NOT NULL" : "NULL"));
        android.util.Log.d("NATIVE_AD_TRACK", "   getActivity(): " + (getActivity() != null ? getActivity().getClass().getSimpleName() : "NULL"));
        android.util.Log.d("NATIVE_AD_TRACK", "   isAdded: " + isAdded());
        android.util.Log.d("NATIVE_AD_TRACK", "═══════════════════════════════════════════════");
        
        android.util.Log.d("DIAGNOSE", "→→ HomeFragment.loadVOTDNativeAd() called");
        android.util.Log.d("DIAGNOSE", "→→ votdNativeAdContainer: " + (votdNativeAdContainer != null ? "NOT NULL" : "NULL"));
        android.util.Log.d("DIAGNOSE", "→→ getActivity(): " + (getActivity() != null ? getActivity().getClass().getSimpleName() : "NULL"));
        
        if (votdNativeAdContainer == null) {
            android.util.Log.e("DIAGNOSE_ERROR", "❌ votdNativeAdContainer is NULL!");
            return;
        }
        
        if (getActivity() == null) {
            android.util.Log.e("NATIVE_AD_TRACK", "❌ getActivity() is NULL!");
            android.util.Log.e("DIAGNOSE_ERROR", "❌ getActivity() is NULL!");
            return;
        }
        
        // 检查订阅状态
        boolean isSubscribed = com.quranaudio.common.ad.SubscriptionChecker.INSTANCE.isUserSubscribed(getActivity());
        android.util.Log.d("NATIVE_AD_TRACK", "→ Checking subscription: " + isSubscribed);
        android.util.Log.d("DIAGNOSE", "→→ User subscribed: " + isSubscribed);
        
        if (isSubscribed) {
            android.util.Log.d("NATIVE_AD_TRACK", "❌ User is subscribed, hiding VOTD ad");
            android.util.Log.d("DIAGNOSE", "→→ User is subscribed, hiding VOTD ad container");
            votdNativeAdContainer.setVisibility(android.view.View.GONE);
            return;
        }
        
        try {
            android.util.Log.d("NATIVE_AD_TRACK", "→ Calling NativeAdHelper.displayNativeAdWithAutoLoad()...");
            android.util.Log.d("NATIVE_AD_TRACK", "   Activity: " + getActivity().getClass().getSimpleName());
            android.util.Log.d("NATIVE_AD_TRACK", "   Container: " + votdNativeAdContainer.getClass().getSimpleName());
            android.util.Log.d("NATIVE_AD_TRACK", "   Layout: com.quran.quranaudio.quiz.R.layout.layout_ad_native_small_wrapper");
            android.util.Log.d("DIAGNOSE", "→→ Calling NativeAdHelper.displayNativeAdWithAutoLoad for VOTD...");
            android.util.Log.d("HomeFragment", "📡 Loading native ad for VOTD card...");
            
            // 使用 NativeAdHelper 加载原生广告
            // 复用 Quiz 的布局样式
            com.quranaudio.common.ad.NativeAdHelper.INSTANCE.displayNativeAdWithAutoLoad(
                getActivity(),
                votdNativeAdContainer,
                com.quran.quranaudio.quiz.R.layout.layout_ad_native_small_wrapper
            );
            
            android.util.Log.d("NATIVE_AD_TRACK", "✅ displayNativeAdWithAutoLoad() call completed");
            android.util.Log.d("DIAGNOSE", "✅ NativeAdHelper.displayNativeAdWithAutoLoad returned for VOTD");
            android.util.Log.d("HomeFragment", "✅ Native ad load initiated for VOTD");
        } catch (Exception e) {
            android.util.Log.e("NATIVE_AD_TRACK", "❌ Exception in loadVOTDNativeAd()", e);
            android.util.Log.e("DIAGNOSE_ERROR", "❌ Failed to load VOTD native ad", e);
            android.util.Log.e("HomeFragment", "❌ Failed to load VOTD native ad: " + e.getMessage(), e);
            votdNativeAdContainer.setVisibility(android.view.View.GONE);
        }
        
        android.util.Log.d("NATIVE_AD_TRACK", "✅ HomeFragment.loadVOTDNativeAd() COMPLETED");
        android.util.Log.d("NATIVE_AD_TRACK", "═══════════════════════════════════════════════");
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
