package com.quran.quranaudio.online.prayertimes.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.quran.quranaudio.online.R;
import com.quran.quranaudio.online.prayertimes.models.PrayerLog;
import com.quran.quranaudio.online.prayertimes.repository.PrayerLogRepository;
import com.quran.quranaudio.online.quran_module.utils.sharedPrefs.SPAppConfigs;

import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Qada' Tracker Activity - Salah History & Qada' Tracking
 * 
 * Weekly View: Shows prayer completion status for the current week
 * Monthly View: Shows prayer completion status for the current month
 */
public class QadaTrackerActivity extends AppCompatActivity {
    
    private static final String TAG = "QadaTrackerActivity";
    
    // Views
    private Toolbar toolbar;
    private MaterialButton btnTabWeekly, btnTabMonthly;
    private ImageButton btnPrevDate, btnNextDate;
    private TextView tvDateRange;
    private View weeklyView, monthlyView;
    
    // Firebase
    private FirebaseFirestore firestore;
    
    // Current view mode
    private enum ViewMode { WEEKLY, MONTHLY }
    private ViewMode currentMode = ViewMode.WEEKLY;
    
    // Date management
    private LocalDate currentDate;
    private String qadaStartDate = null; // Qada tracking start date (YYYY-MM-DD)
    
    // Data repository
    private PrayerLogRepository prayerLogRepository;
    
    // Prayer data cache (date -> prayerName -> PrayerLogData)
    private Map<String, Map<String, PrayerLogData>> weeklyData = new HashMap<>();
    private Map<String, Map<String, PrayerLogData>> monthlyData = new HashMap<>();
    
    /**
     * Simple data class to cache prayer log info
     */
    private static class PrayerLogData {
        String logId;
        PrayerLog.PrayerStatus status;
        
        PrayerLogData(String logId, PrayerLog.PrayerStatus status) {
            this.logId = logId;
            this.status = status;
        }
    }
    
    // 🌐 语言配置支持
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(updateBaseContextLocale(base));
    }
    
    @Override
    public void applyOverrideConfiguration(Configuration overrideConfiguration) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
            String language = SPAppConfigs.getLocale(this);
            if (language != null && !language.isEmpty()) {
                String resourceLanguage = "id".equals(language) ? "in" : language;
                setLocale(overrideConfiguration, resourceLanguage);
            }
        }
        super.applyOverrideConfiguration(overrideConfiguration);
    }
    
    private void setLocale(Configuration configuration, String language) {
        configuration.setLocale(new Locale(language));
    }
    
    private Context updateBaseContextLocale(Context context) {
        String language = SPAppConfigs.getLocale(context);
        
        if (language == null || language.isEmpty()) {
            return context;
        }
        
        // 🔄 资源目录映射：应用使用 "id"，但 Android 资源使用 "in"
        String resourceLanguage = "id".equals(language) ? "in" : language;
        
        Locale locale = new Locale(resourceLanguage);
        Locale.setDefault(locale);
        
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
            Configuration configuration = new Configuration(context.getResources().getConfiguration());
            configuration.setLocale(locale);
            return context.createConfigurationContext(configuration);
        } else {
            Resources resources = context.getResources();
            Configuration configuration = resources.getConfiguration();
            configuration.locale = locale;
            resources.updateConfiguration(configuration, resources.getDisplayMetrics());
            return context;
        }
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qada_tracker);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            currentDate = LocalDate.now();
        }
        
        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance();
        
        // Initialize repository
        prayerLogRepository = new PrayerLogRepository();
        
        initializeViews();
        setupListeners();
        setupStatusBar();
        
        // Load Qada start date
        loadQadaStartDate();
        
        // Default to Weekly view
        switchToWeeklyView();
    }
    
    private void initializeViews() {
        // Setup Toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        
        btnTabWeekly = findViewById(R.id.btn_tab_weekly);
        btnTabMonthly = findViewById(R.id.btn_tab_monthly);
        btnPrevDate = findViewById(R.id.btn_prev_date);
        btnNextDate = findViewById(R.id.btn_next_date);
        tvDateRange = findViewById(R.id.tv_date_range);
        weeklyView = findViewById(R.id.weekly_view);
        monthlyView = findViewById(R.id.monthly_view);
    }
    
    private void setupListeners() {
        // Toolbar navigation click
        toolbar.setNavigationOnClickListener(v -> finish());
        
        btnTabWeekly.setOnClickListener(v -> switchToWeeklyView());
        btnTabMonthly.setOnClickListener(v -> switchToMonthlyView());
        
        btnPrevDate.setOnClickListener(v -> navigatePrevious());
        btnNextDate.setOnClickListener(v -> navigateNext());
    }
    
    private void setupStatusBar() {
        // Status bar color matching Toolbar (green #4B9B76)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.parseColor("#4B9B76"));
        }
    }
    
    /**
     * Switch to Weekly View
     */
    private void switchToWeeklyView() {
        currentMode = ViewMode.WEEKLY;
        
        // Update tab selection
        btnTabWeekly.setSelected(true);
        btnTabMonthly.setSelected(false);
        
        // Show/hide views
        weeklyView.setVisibility(View.VISIBLE);
        monthlyView.setVisibility(View.GONE);
        
        // Update date range
        updateDateDisplay();
        
        // Load weekly data
        loadWeeklyData();
    }
    
    /**
     * Switch to Monthly View
     */
    private void switchToMonthlyView() {
        currentMode = ViewMode.MONTHLY;
        
        // Update tab selection
        btnTabWeekly.setSelected(false);
        btnTabMonthly.setSelected(true);
        
        // Show/hide views
        weeklyView.setVisibility(View.GONE);
        monthlyView.setVisibility(View.VISIBLE);
        
        // Update date range
        updateDateDisplay();
        
        // Load monthly data
        loadMonthlyData();
    }
    
    /**
     * Navigate to previous week/month
     */
    private void navigatePrevious() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (currentMode == ViewMode.WEEKLY) {
                currentDate = currentDate.minusWeeks(1);
                updateDateDisplay();
                loadWeeklyData();
            } else {
                currentDate = currentDate.minusMonths(1);
                updateDateDisplay();
                loadMonthlyData();
            }
        }
    }
    
    /**
     * Navigate to next week/month
     */
    private void navigateNext() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (currentMode == ViewMode.WEEKLY) {
                currentDate = currentDate.plusWeeks(1);
                updateDateDisplay();
                loadWeeklyData();
            } else {
                currentDate = currentDate.plusMonths(1);
                updateDateDisplay();
                loadMonthlyData();
            }
        }
    }
    
    /**
     * Update date display based on current mode
     */
    private void updateDateDisplay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (currentMode == ViewMode.WEEKLY) {
                // Get week start (Monday) and end (Sunday)
                LocalDate weekStart = currentDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                LocalDate weekEnd = currentDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
                
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd", Locale.getDefault());
                String dateStr = weekStart.format(formatter) + " - " + weekEnd.format(formatter);
                tvDateRange.setText(dateStr);
            } else {
                // Show month and year
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault());
                tvDateRange.setText(currentDate.format(formatter));
            }
        } else {
            // Fallback for older Android versions
            SimpleDateFormat formatter = new SimpleDateFormat(
                currentMode == ViewMode.WEEKLY ? "MMM dd" : "MMMM yyyy", 
                Locale.getDefault()
            );
            Calendar cal = Calendar.getInstance();
            tvDateRange.setText(formatter.format(cal.getTime()));
        }
    }
    
    /**
     * Build Weekly Prayer Grid
     */
    private void buildWeeklyPrayerGrid() {
        LinearLayout gridContainer = weeklyView.findViewById(R.id.prayer_breakdown_grid);
        if (gridContainer == null) return;
        
        gridContainer.removeAllViews();
        
        // Create header row (Mon, Tue, Wed, Thu, Fri, Sat, Sun)
        LinearLayout headerRow = createWeeklyHeaderRow();
        gridContainer.addView(headerRow);
        
        // Create prayer rows (Fajr, Dhuhr, Asr, Maghrib, Isha)
        String[] prayers = getPrayerNames();
        
        for (String prayer : prayers) {
            LinearLayout prayerRow = createWeeklyPrayerRow(prayer);
            gridContainer.addView(prayerRow);
        }
    }
    
    /**
     * Get localized prayer names
     */
    private String[] getPrayerNames() {
        return new String[] {
            getString(R.string.prayer_fajr),
            getString(R.string.prayer_dhuhr),
            getString(R.string.prayer_asr),
            getString(R.string.prayer_maghrib),
            getString(R.string.prayer_isha)
        };
    }
    
    /**
     * Get localized day abbreviations
     */
    private String[] getDayAbbreviations() {
        return new String[] {
            getString(R.string.day_mon),
            getString(R.string.day_tue),
            getString(R.string.day_wed),
            getString(R.string.day_thu),
            getString(R.string.day_fri),
            getString(R.string.day_sat),
            getString(R.string.day_sun)
        };
    }
    
    /**
     * Create header row for weekly view
     */
    private LinearLayout createWeeklyHeaderRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        rowParams.setMargins(0, 0, 0, dpToPx(8));
        row.setLayoutParams(rowParams);
        
        // Prayer name column (empty for header)
        TextView nameView = new TextView(this);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
            dpToPx(56),  // ✅ 从 70 缩小到 56（紧凑但明显区分）
            dpToPx(32)
        );
        nameParams.setMargins(0, 0, dpToPx(8), 0);  // ✅ 右边距 8dp，与 Mon 列明显区分
        nameView.setLayoutParams(nameParams);
        row.addView(nameView);
        
        // Day columns
        String[] days = getDayAbbreviations();
        for (String day : days) {
            TextView dayView = new TextView(this);
            LinearLayout.LayoutParams dayParams = new LinearLayout.LayoutParams(
                dpToPx(44),  // ✅ 从 36 增加到 44，与点击容器对齐
                dpToPx(32)
            );
            dayParams.setMargins(0, 0, 0, 0);  // ✅ 移除左右间距，更紧凑
            dayView.setLayoutParams(dayParams);
            dayView.setText(day);
            dayView.setTextSize(12);
            dayView.setTextColor(Color.parseColor("#999999"));
            dayView.setGravity(Gravity.CENTER);
            row.addView(dayView);
        }
        
        return row;
    }
    
    /**
     * Create prayer row for weekly view
     */
    private LinearLayout createWeeklyPrayerRow(String prayerName) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        rowParams.setMargins(0, 0, 0, dpToPx(8));
        row.setLayoutParams(rowParams);
        
        // Prayer name
        TextView nameView = new TextView(this);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
            dpToPx(56),  // ✅ 从 70 缩小到 56，与表头对齐
            dpToPx(44)
        );
        nameParams.setMargins(0, dpToPx(2), dpToPx(8), dpToPx(2));  // ✅ 右边距 8dp，与 Mon 列明显区分
        nameView.setLayoutParams(nameParams);
        nameView.setText(prayerName);
        nameView.setTextSize(14);
        nameView.setTextColor(Color.parseColor("#1A1A1A"));
        nameView.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(nameView);
        
        // Status dots for each day (7 days)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalDate weekStart = currentDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            
            for (int dayOffset = 0; dayOffset < 7; dayOffset++) {
                final LocalDate date = weekStart.plusDays(dayOffset);
                final String dateStr = date.toString();
                final String finalPrayerName = prayerName;
                int status = getPrayerStatus(dateStr, prayerName, true);
                
                // ✅ 使用容器包裹点，扩大点击区域
                LinearLayout dotContainer = new LinearLayout(this);
                LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                    dpToPx(44),  // ✅ 容器宽度 44dp，与表头星期列对齐
                    dpToPx(44)   // ✅ 容器高度 44dp
                );
                containerParams.setMargins(0, dpToPx(2), 0, dpToPx(2));  // ✅ 移除左右间距，更紧凑
                dotContainer.setLayoutParams(containerParams);
                dotContainer.setGravity(Gravity.CENTER);
                dotContainer.setClickable(true);  // ✅ 容器可点击
                
                View dotView = new View(this);
                LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dpToPx(12), dpToPx(12));
                dotView.setLayoutParams(dotParams);
                
                // Status: 0=Ada', 1=Qada', 2=Missed, -1=Pending/Not Logged
                if (status == -1) {
                    // Pending: show light grey
                    dotView.setBackgroundColor(Color.parseColor("#E0E0E0"));
                } else {
                    dotView.setBackgroundResource(
                        status == 0 ? R.drawable.bg_circle_green :
                        status == 1 ? R.drawable.bg_circle_orange :
                        R.drawable.bg_circle_red
                    );
                }
                
                // ✅ 为容器添加点击反馈效果
                dotContainer.setBackground(createRippleDrawable());
                
                // ✅ 将点击监听器设置在容器上，而不是点上
                final int finalStatus = status;
                dotContainer.setOnClickListener(v -> {
                    Log.d(TAG, "🖱️ Dot clicked: " + finalPrayerName + " on " + dateStr);
                    openPrayerLogModal(finalPrayerName, dateStr, finalStatus);
                });
                
                dotContainer.addView(dotView);
                row.addView(dotContainer);
            }
        }
        
        return row;
    }
    
    /**
     * Build Monthly Prayer Table
     */
    private void buildMonthlyPrayerTable() {
        LinearLayout tableContainer = monthlyView.findViewById(R.id.monthly_prayer_table);
        if (tableContainer == null) return;
        
        tableContainer.removeAllViews();
        
        // Create header row (DATE, FAJR, DHUHR, ASR, MAGHRIB, ISHA)
        LinearLayout headerRow = createMonthlyHeaderRow();
        tableContainer.addView(headerRow);
        
        // Create data rows for all days in the month
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalDate monthStart = currentDate.withDayOfMonth(1);
            int daysInMonth = currentDate.lengthOfMonth();
            
            for (int day = 1; day <= daysInMonth; day++) {
                LocalDate date = monthStart.withDayOfMonth(day);
                LinearLayout dataRow = createMonthlyDataRow(date);
                tableContainer.addView(dataRow);
            }
        }
    }
    
    /**
     * Create header row for monthly view
     */
    private LinearLayout createMonthlyHeaderRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        rowParams.setMargins(0, 0, 0, dpToPx(12));
        row.setLayoutParams(rowParams);
        
        String[] headers = {"DATE", "FAJR", "DHUHR", "ASR", "MAGHRIB", "ISHA"};
        for (int i = 0; i < headers.length; i++) {
            TextView headerView = new TextView(this);
            LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
                i == 0 ? dpToPx(50) : dpToPx(65),
                dpToPx(32)
            );
            headerParams.setMargins(dpToPx(4), 0, dpToPx(4), 0);
            headerView.setLayoutParams(headerParams);
            headerView.setText(headers[i]);
            headerView.setTextSize(11);
            headerView.setTextColor(Color.parseColor("#999999"));
            headerView.setGravity(Gravity.CENTER);
            headerView.setTypeface(null, android.graphics.Typeface.BOLD);
            row.addView(headerView);
        }
        
        return row;
    }
    
    /**
     * Create data row for monthly view
     */
    private LinearLayout createMonthlyDataRow(LocalDate date) {
        int day = date.getDayOfMonth();
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        rowParams.setMargins(0, 0, 0, dpToPx(12));
        row.setLayoutParams(rowParams);
        
        // Date column
        TextView dateView = new TextView(this);
        LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(
            dpToPx(50),
            dpToPx(44)  // ✅ 增加高度（从32增加到44，与祷告点容器高度一致）
        );
        dateParams.setMargins(dpToPx(4), dpToPx(2), dpToPx(4), dpToPx(2));  // ✅ 优化上下间距
        dateView.setLayoutParams(dateParams);
        dateView.setText(String.format(Locale.getDefault(), "%02d", day));
        dateView.setTextSize(14);
        dateView.setTextColor(Color.parseColor("#1A1A1A"));
        dateView.setGravity(Gravity.CENTER);
        row.addView(dateView);
        
        // Prayer columns (5 prayers) - use real data
        final String dateStr = date.toString();
        String[] prayerNames = getPrayerNames();
        
        for (String prayerName : prayerNames) {
            final String finalPrayerName = prayerName;
            int status = getPrayerStatus(dateStr, prayerName, false);
            LinearLayout dotContainer = new LinearLayout(this);
            LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                dpToPx(65),
                dpToPx(44)  // ✅ 增加高度（从32增加到44，增加37.5%）
            );
            containerParams.setMargins(dpToPx(4), dpToPx(2), dpToPx(4), dpToPx(2));  // ✅ 优化上下间距
            dotContainer.setLayoutParams(containerParams);
            dotContainer.setGravity(Gravity.CENTER);
            dotContainer.setClickable(true);  // ✅ 容器可点击
            
            View dotView = new View(this);
            LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dpToPx(12), dpToPx(12));
            dotView.setLayoutParams(dotParams);
            
            // Status: 0=Ada', 1=Qada', 2=Missed, -1=Pending/Not Logged
            if (status == -1) {
                // Pending: show light grey
                dotView.setBackgroundColor(Color.parseColor("#E0E0E0"));
            } else {
                dotView.setBackgroundResource(
                    status == 0 ? R.drawable.bg_circle_green :
                    status == 1 ? R.drawable.bg_circle_orange :
                    R.drawable.bg_circle_red
                );
            }
            
            // ✅ 为容器添加点击反馈效果
            dotContainer.setBackground(createRippleDrawable());
            
            // ✅ 将点击监听器设置在容器上，而不是点上
            final int finalStatus = status;
            dotContainer.setOnClickListener(v -> {
                Log.d(TAG, "🖱️ Dot clicked: " + finalPrayerName + " on " + dateStr);
                openPrayerLogModal(finalPrayerName, dateStr, finalStatus);
            });
            
            dotContainer.addView(dotView);
            row.addView(dotContainer);
        }
        
        return row;
    }
    
    /**
     * Open Prayer Log Modal based on status
     * 
     * @param prayerName Prayer name (Fajr, Dhuhr, etc.)
     * @param date Prayer date (YYYY-MM-DD)
     * @param status Current status (0=Ada', 1=Qada', 2=Missed, -1=Pending)
     */
    private void openPrayerLogModal(String prayerName, String date, int status) {
        Log.d(TAG, "📝 Opening Prayer Log Modal: " + prayerName + " on " + date + ", status=" + status);
        
        try {
            // 根据状态判断是新建还是编辑模式
            if (status == -1) {
                // Pending: 新建模式，默认 Ada'
                Log.d(TAG, "  Mode: New Log (Pending → Ada')");
                
                PrayerLogBottomSheet bottomSheet = PrayerLogBottomSheet.Companion.newInstance(
                    prayerName,
                    PrayerLog.PrayerStatus.ADA,
                    date  // originalDate for Qada tracking
                );
                
                // Set listener for refreshing data
                bottomSheet.setOnPrayerLoggedListener(new PrayerLogBottomSheet.OnPrayerLoggedListener() {
                    @Override
                    public void onPrayerLogged(String prayer) {
                        Log.d(TAG, "✅ Prayer logged callback: " + prayer);
                        // Refresh data
                        if (currentMode == ViewMode.WEEKLY) {
                            loadWeeklyData();
                        } else {
                            loadMonthlyData();
                        }
                    }
                    
                    @Override
                    public void onQadaCountChanged(int delta) {
                        Log.d(TAG, "🔢 Qada count changed: delta=" + delta);
                        // No need to refresh Qada summary in this Activity
                        // It will be handled in PrayersFragment
                    }
                });
                
                bottomSheet.show(getSupportFragmentManager(), "PrayerLogBottomSheet");
                
            } else if (status == 2) {
                // Missed: 新建模式，默认 Qada'（弥补场景）
                Log.d(TAG, "  Mode: New Qada Log (Missed → Qada')");
                
                PrayerLogBottomSheet bottomSheet = PrayerLogBottomSheet.Companion.newInstance(
                    prayerName,
                    PrayerLog.PrayerStatus.QADA,
                    date  // originalDate
                );
                
                bottomSheet.setOnPrayerLoggedListener(new PrayerLogBottomSheet.OnPrayerLoggedListener() {
                    @Override
                    public void onPrayerLogged(String prayer) {
                        Log.d(TAG, "✅ Qada logged callback: " + prayer);
                        if (currentMode == ViewMode.WEEKLY) {
                            loadWeeklyData();
                        } else {
                            loadMonthlyData();
                        }
                    }
                    
                    @Override
                    public void onQadaCountChanged(int delta) {
                        Log.d(TAG, "🔢 Qada count changed: delta=" + delta);
                    }
                });
                
                bottomSheet.show(getSupportFragmentManager(), "PrayerLogBottomSheet");
                
            } else {
                // Ada' (0) or Qada' (1): 编辑模式
                Log.d(TAG, "  Mode: Edit Existing Log");
                
                // ✅ 优化：直接从缓存获取 logId，避免重复查询 Firestore
                String logId = getLogIdFromCache(date, prayerName);
                
                if (logId != null) {
                    Log.d(TAG, "  ✅ Found log ID from cache: " + logId);
                    
                    PrayerLogBottomSheet bottomSheet = PrayerLogBottomSheet.Companion.newInstanceForEdit(
                        prayerName,
                        logId
                    );
                    
                    bottomSheet.setOnPrayerLoggedListener(new PrayerLogBottomSheet.OnPrayerLoggedListener() {
                        @Override
                        public void onPrayerLogged(String prayer) {
                            Log.d(TAG, "✅ Prayer updated callback: " + prayer);
                            if (currentMode == ViewMode.WEEKLY) {
                                loadWeeklyData();
                            } else {
                                loadMonthlyData();
                            }
                        }
                        
                        @Override
                        public void onQadaCountChanged(int delta) {
                            Log.d(TAG, "🔢 Qada count changed: delta=" + delta);
                        }
                    });
                    
                    bottomSheet.show(getSupportFragmentManager(), "PrayerLogBottomSheet");
                } else {
                    // Fallback: 如果缓存中没有，查询 Firestore（不应该发生，但保险起见）
                    Log.w(TAG, "  ⚠️ Log ID not in cache, querying Firestore...");
                    findExistingLogId(prayerName, date, new LogIdCallback() {
                        @Override
                        public void onFound(String logId) {
                            Log.d(TAG, "  Found existing log from Firestore: " + logId);
                            
                            PrayerLogBottomSheet bottomSheet = PrayerLogBottomSheet.Companion.newInstanceForEdit(
                                prayerName,
                                logId
                            );
                            
                            bottomSheet.setOnPrayerLoggedListener(new PrayerLogBottomSheet.OnPrayerLoggedListener() {
                                @Override
                                public void onPrayerLogged(String prayer) {
                                    Log.d(TAG, "✅ Prayer updated callback: " + prayer);
                                    if (currentMode == ViewMode.WEEKLY) {
                                        loadWeeklyData();
                                    } else {
                                        loadMonthlyData();
                                    }
                                }
                                
                                @Override
                                public void onQadaCountChanged(int delta) {
                                    Log.d(TAG, "🔢 Qada count changed: delta=" + delta);
                                }
                            });
                            
                            bottomSheet.show(getSupportFragmentManager(), "PrayerLogBottomSheet");
                        }
                        
                        @Override
                        public void onNotFound() {
                            Log.w(TAG, "  ⚠️ No existing log found, treating as new log");
                            android.widget.Toast.makeText(QadaTrackerActivity.this,
                                "No existing record found. Creating new log...",
                                android.widget.Toast.LENGTH_SHORT).show();
                            
                            // Fallback to new log mode
                            openPrayerLogModal(prayerName, date, -1);
                        }
                    });
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error opening Prayer Log Modal", e);
            android.widget.Toast.makeText(this,
                "Error: " + e.getMessage(),
                android.widget.Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Get log ID from cache (优化：避免重复查询 Firestore)
     * 
     * @param date Prayer date (YYYY-MM-DD)
     * @param prayerName Prayer name
     * @return log ID or null if not in cache
     */
    private String getLogIdFromCache(String date, String prayerName) {
        Map<String, Map<String, PrayerLogData>> dataSource = 
            (currentMode == ViewMode.WEEKLY) ? weeklyData : monthlyData;
        
        if (dataSource.containsKey(date)) {
            Map<String, PrayerLogData> dayData = dataSource.get(date);
            if (dayData != null && dayData.containsKey(prayerName)) {
                PrayerLogData logData = dayData.get(prayerName);
                if (logData != null) {
                    return logData.logId;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Callback interface for log ID lookup
     */
    private interface LogIdCallback {
        void onFound(String logId);
        void onNotFound();
    }
    
    /**
     * Find existing prayer log ID for editing
     */
    private void findExistingLogId(String prayerName, String date, LogIdCallback callback) {
        String currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            callback.onNotFound();
            return;
        }
        
        firestore.collection(PrayerLog.COLLECTION_NAME)
            .whereEqualTo("userId", currentUserId)
            .whereEqualTo("prayerName", prayerName)
            .whereEqualTo("date", date)
            .limit(1)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                if (!querySnapshot.isEmpty()) {
                    String logId = querySnapshot.getDocuments().get(0).getId();
                    callback.onFound(logId);
                } else {
                    callback.onNotFound();
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Error finding existing log", e);
                callback.onNotFound();
            });
    }
    
    /**
     * Get current user ID from Firebase Auth
     */
    private String getCurrentUserId() {
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        return (user != null) ? user.getUid() : null;
    }
    
    /**
     * Load Qada start date from Firestore
     */
    private void loadQadaStartDate() {
        prayerLogRepository.getQadaStartDateAsync(new PrayerLogRepository.QadaStartDateCallback() {
            @Override
            public void onSuccess(String startDate) {
                qadaStartDate = startDate;
                Log.d(TAG, "✅ Loaded Qada start date: " + qadaStartDate);
                
                // Refresh data with new start date
                runOnUiThread(() -> {
                    if (currentMode == ViewMode.WEEKLY) {
                        loadWeeklyData();
                    } else {
                        loadMonthlyData();
                    }
                });
            }
            
            @Override
            public void onError(Exception e) {
                Log.e(TAG, "❌ Failed to load Qada start date", e);
                qadaStartDate = null;
            }
        });
    }
    
    /**
     * Convert dp to pixels
     */
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
    
    /**
     * Load weekly prayer data from Firestore
     */
    private void loadWeeklyData() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.e(TAG, "User not authenticated");
            buildWeeklyPrayerGrid(); // Show empty grid
            return;
        }
        
        LocalDate weekStart = currentDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = currentDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        
        Log.d(TAG, "Loading weekly data from " + weekStart + " to " + weekEnd);
        
        // Load data from Firestore with log IDs
        prayerLogRepository.getPrayerLogsByDateRangeWithIdsAsync(
            weekStart.toString(),
            weekEnd.toString(),
            new PrayerLogRepository.DateRangeWithIdsCallback() {
                @Override
                public void onResult(Map<String, Map<String, PrayerLogRepository.PrayerLogInfo>> data) {
                    weeklyData.clear();
                    
                    // Convert PrayerLogInfo to PrayerLogData
                    for (Map.Entry<String, Map<String, PrayerLogRepository.PrayerLogInfo>> dateEntry : data.entrySet()) {
                        Map<String, PrayerLogData> dayData = new HashMap<>();
                        for (Map.Entry<String, PrayerLogRepository.PrayerLogInfo> prayerEntry : dateEntry.getValue().entrySet()) {
                            PrayerLogRepository.PrayerLogInfo info = prayerEntry.getValue();
                            dayData.put(prayerEntry.getKey(), new PrayerLogData(info.getLogId(), info.getStatus()));
                        }
                        weeklyData.put(dateEntry.getKey(), dayData);
                    }
                    
                    Log.d(TAG, "Loaded " + weeklyData.size() + " days of weekly data with log IDs");
                    
                    // Update UI on main thread
                    runOnUiThread(() -> {
                        buildWeeklyPrayerGrid();
                        updateWeeklyCompletion();
                    });
                }
            }
        );
    }
    
    /**
     * Load monthly prayer data from Firestore
     */
    private void loadMonthlyData() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.e(TAG, "User not authenticated");
            buildMonthlyPrayerTable(); // Show empty table
            return;
        }
        
        LocalDate monthStart = currentDate.withDayOfMonth(1);
        LocalDate monthEnd = currentDate.withDayOfMonth(currentDate.lengthOfMonth());
        
        Log.d(TAG, "Loading monthly data from " + monthStart + " to " + monthEnd);
        
        // Load data from Firestore with log IDs
        prayerLogRepository.getPrayerLogsByDateRangeWithIdsAsync(
            monthStart.toString(),
            monthEnd.toString(),
            new PrayerLogRepository.DateRangeWithIdsCallback() {
                @Override
                public void onResult(Map<String, Map<String, PrayerLogRepository.PrayerLogInfo>> data) {
                    monthlyData.clear();
                    
                    // Convert PrayerLogInfo to PrayerLogData
                    for (Map.Entry<String, Map<String, PrayerLogRepository.PrayerLogInfo>> dateEntry : data.entrySet()) {
                        Map<String, PrayerLogData> dayData = new HashMap<>();
                        for (Map.Entry<String, PrayerLogRepository.PrayerLogInfo> prayerEntry : dateEntry.getValue().entrySet()) {
                            PrayerLogRepository.PrayerLogInfo info = prayerEntry.getValue();
                            dayData.put(prayerEntry.getKey(), new PrayerLogData(info.getLogId(), info.getStatus()));
                        }
                        monthlyData.put(dateEntry.getKey(), dayData);
                    }
                    
                    Log.d(TAG, "Loaded " + monthlyData.size() + " days of monthly data with log IDs");
                    
                    // Update UI on main thread
                    runOnUiThread(() -> {
                        buildMonthlyPrayerTable();
                        updateMonthlyCompletion();
                    });
                }
            }
        );
    }
    
    /**
     * Get prayer status for a specific date and prayer name
     */
    private int getPrayerStatus(String date, String prayerName, boolean isWeekly) {
        Map<String, Map<String, PrayerLogData>> dataSource = 
            isWeekly ? weeklyData : monthlyData;
        
        if (dataSource.containsKey(date)) {
            Map<String, PrayerLogData> dayData = dataSource.get(date);
            
            if (dayData != null && dayData.containsKey(prayerName)) {
                PrayerLogData logData = dayData.get(prayerName);
                if (logData != null) {
                    PrayerLog.PrayerStatus status = logData.status;
                    
                    // Convert status to int (0=Ada', 1=Qada', 2=Missed)
                    if (status == PrayerLog.PrayerStatus.ADA) {
                        return 0;
                    } else if (status == PrayerLog.PrayerStatus.QADA) {
                        return 1;
                    } else if (status == PrayerLog.PrayerStatus.MISSED) {
                        return 2;
                    }
                }
            }
        }
        
        // No record found - check if prayer time has passed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalDate prayerDate = LocalDate.parse(date);
            LocalDate today = LocalDate.now();
            
            // ✅ Check Qada start date first
            // If date is before Qada tracking start date, show as grey (not tracked)
            if (qadaStartDate != null) {
                try {
                    LocalDate startDate = LocalDate.parse(qadaStartDate);
                    if (prayerDate.isBefore(startDate)) {
                        Log.d(TAG, "  Prayer date " + date + " is before Qada start date " + qadaStartDate + ", showing as Pending");
                        return -1; // Pending (grey) - not tracked yet
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing Qada start date: " + qadaStartDate, e);
                }
            }
            
            // If date is in the future, it's Pending
            if (prayerDate.isAfter(today)) {
                return -1; // Pending (grey)
            }
            
            // If date is today, check if prayer time has passed
            if (prayerDate.isEqual(today)) {
                if (isPrayerTimePassedForDate(prayerName)) {
                    return 2; // Missed (red)
                } else {
                    return -1; // Pending (grey)
                }
            }
            
            // If date is in the past, it's definitely Missed
            if (prayerDate.isBefore(today)) {
                return 2; // Missed (red)
            }
        }
        
        // Default: Pending
        return -1;
    }
    
    /**
     * Check if a prayer time has passed for today
     */
    private boolean isPrayerTimePassedForDate(String prayerName) {
        Calendar now = Calendar.getInstance();
        
        // Get next prayer after current
        String nextPrayer = getNextPrayerName(prayerName);
        
        // Special case: Isha is the last prayer
        if (nextPrayer == null) {
            // For Isha, check if we're past midnight
            Calendar midnight = Calendar.getInstance();
            midnight.set(Calendar.HOUR_OF_DAY, 23);
            midnight.set(Calendar.MINUTE, 59);
            midnight.set(Calendar.SECOND, 59);
            return now.after(midnight);
        }
        
        // Simplified time checking based on typical prayer times
        int currentHour = now.get(Calendar.HOUR_OF_DAY);
        
        switch (prayerName) {
            case "Fajr":
                return currentHour >= 11; // After Dhuhr time
            case "Dhuhr":
                return currentHour >= 15; // After Asr time
            case "Asr":
                return currentHour >= 18; // After Maghrib time
            case "Maghrib":
                return currentHour >= 20; // After Isha time
            case "Isha":
                return currentHour >= 23; // After midnight
            default:
                return false;
        }
    }
    
    /**
     * Get the next prayer name
     */
    private String getNextPrayerName(String current) {
        switch (current) {
            case "Fajr":
                return "Dhuhr";
            case "Dhuhr":
                return "Asr";
            case "Asr":
                return "Maghrib";
            case "Maghrib":
                return "Isha";
            case "Isha":
                return null; // Last prayer
            default:
                return null;
        }
    }
    
    /**
     * Update weekly completion statistics
     */
    private void updateWeeklyCompletion() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        
        LocalDate weekStart = currentDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = currentDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        LocalDate today = LocalDate.now();
        
        int totalPrayers = 0;
        int completedPrayers = 0; // Ada' + Qada'
        
        String[] prayerNames = getPrayerNames();
        
        // ✅ 解析 Qada 开始日期
        LocalDate qadaStart = null;
        if (qadaStartDate != null) {
            try {
                qadaStart = LocalDate.parse(qadaStartDate);
            } catch (Exception e) {
                Log.e(TAG, "Error parsing Qada start date: " + qadaStartDate, e);
            }
        }
        
        LocalDate date = weekStart;
        while (!date.isAfter(weekEnd)) {
            String dateStr = date.toString();
            
            // ✅ 只计入有效日期范围内的祷告：
            // 1. 日期 >= Qada 开始日期
            // 2. 日期 <= 今天
            boolean isValidDate = true;
            if (qadaStart != null && date.isBefore(qadaStart)) {
                isValidDate = false; // Qada 开始日期之前，不计入
            }
            if (date.isAfter(today)) {
                isValidDate = false; // 未来日期，不计入
            }
            
            if (isValidDate) {
                for (String prayerName : prayerNames) {
                    if (!shouldIncludePrayerInDenominator(date, prayerName)) {
                        continue;
                    }
                    
                    totalPrayers++;
                    
                    if (weeklyData.containsKey(dateStr)) {
                        Map<String, PrayerLogData> dayData = weeklyData.get(dateStr);
                        if (dayData != null && dayData.containsKey(prayerName)) {
                            PrayerLogData logData = dayData.get(prayerName);
                            if (logData != null) {
                                PrayerLog.PrayerStatus status = logData.status;
                                if (status == PrayerLog.PrayerStatus.ADA || status == PrayerLog.PrayerStatus.QADA) {
                                    completedPrayers++;
                                }
                            }
                        }
                    }
                }
            }
            
            date = date.plusDays(1);
        }
        
        int completionRate = totalPrayers > 0 ? (completedPrayers * 100 / totalPrayers) : 0;
        
        Log.d(TAG, "Weekly completion: " + completedPrayers + "/" + totalPrayers + " = " + completionRate + "%");
        
        // Update UI elements for weekly completion rate
        android.widget.ProgressBar circularProgress = weeklyView.findViewById(R.id.circular_progress);
        android.widget.TextView tvPercentage = weeklyView.findViewById(R.id.tv_completion_percentage);
        android.widget.TextView tvGrowth = weeklyView.findViewById(R.id.tv_weekly_growth);
        
        if (circularProgress != null) {
            circularProgress.setProgress(completionRate);
        }
        
        if (tvPercentage != null) {
            tvPercentage.setText(completionRate + "%");
        }
        
        if (tvGrowth != null) {
            // Calculate last week's completion for comparison
            calculateAndDisplayWeeklyGrowth(tvGrowth, completionRate);
        }
    }
    
    /**
     * Update monthly completion statistics
     */
    private void updateMonthlyCompletion() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        
        LocalDate monthStart = currentDate.withDayOfMonth(1);
        LocalDate monthEnd = currentDate.withDayOfMonth(currentDate.lengthOfMonth());
        LocalDate today = LocalDate.now();
        
        int totalPrayers = 0;
        int completedPrayers = 0; // Ada' + Qada'
        
        String[] prayerNames = getPrayerNames();
        
        // ✅ 解析 Qada 开始日期
        LocalDate qadaStart = null;
        if (qadaStartDate != null) {
            try {
                qadaStart = LocalDate.parse(qadaStartDate);
            } catch (Exception e) {
                Log.e(TAG, "Error parsing Qada start date: " + qadaStartDate, e);
            }
        }
        
        LocalDate date = monthStart;
        while (!date.isAfter(monthEnd)) {
            String dateStr = date.toString();
            
            // ✅ 只计入有效日期范围内的祷告：
            // 1. 日期 >= Qada 开始日期
            // 2. 日期 <= 今天
            boolean isValidDate = true;
            if (qadaStart != null && date.isBefore(qadaStart)) {
                isValidDate = false; // Qada 开始日期之前，不计入
            }
            if (date.isAfter(today)) {
                isValidDate = false; // 未来日期，不计入
            }
            
            if (isValidDate) {
                for (String prayerName : prayerNames) {
                    if (!shouldIncludePrayerInDenominator(date, prayerName)) {
                        continue;
                    }
                    
                    totalPrayers++;
                    
                    if (monthlyData.containsKey(dateStr)) {
                        Map<String, PrayerLogData> dayData = monthlyData.get(dateStr);
                        if (dayData != null && dayData.containsKey(prayerName)) {
                            PrayerLogData logData = dayData.get(prayerName);
                            if (logData != null) {
                                PrayerLog.PrayerStatus status = logData.status;
                                if (status == PrayerLog.PrayerStatus.ADA || status == PrayerLog.PrayerStatus.QADA) {
                                    completedPrayers++;
                                }
                            }
                        }
                    }
                }
            }
            
            date = date.plusDays(1);
        }
        
        int completionRate = totalPrayers > 0 ? (completedPrayers * 100 / totalPrayers) : 0;
        
        Log.d(TAG, "Monthly completion: " + completedPrayers + "/" + totalPrayers + " = " + completionRate + "%");
        
        // Update UI elements for monthly completion rate
        android.widget.ProgressBar circularProgress = monthlyView.findViewById(R.id.circular_progress_monthly);
        android.widget.TextView tvPercentage = monthlyView.findViewById(R.id.tv_completion_percentage_monthly);
        
        if (circularProgress != null) {
            circularProgress.setProgress(completionRate);
        }
        
        if (tvPercentage != null) {
            tvPercentage.setText(completionRate + "%");
        }
    }
    
    /**
     * 计算并显示周增长趋势
     */
    private void calculateAndDisplayWeeklyGrowth(final android.widget.TextView tvGrowth, final int currentRate) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        
        // Calculate last week's date range
        LocalDate lastWeekStart = currentDate.minusWeeks(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate lastWeekEnd = currentDate.minusWeeks(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        
        // Load last week's data
        prayerLogRepository.getPrayerLogsByDateRangeAsync(
            lastWeekStart.toString(),
            lastWeekEnd.toString(),
            new PrayerLogRepository.DateRangeCallback() {
                @Override
                public void onResult(Map<String, Map<String, PrayerLog.PrayerStatus>> data) {
                    // Calculate last week's completion rate
                    int totalPrayers = 0;
                    int completedPrayers = 0;
                    
                    String[] prayerNames = getPrayerNames();
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        LocalDate date = lastWeekStart;
                        while (!date.isAfter(lastWeekEnd)) {
                            String dateStr = date.toString();
                            
                            for (String prayerName : prayerNames) {
                                totalPrayers++;
                                
                                if (data.containsKey(dateStr)) {
                                    Map<String, PrayerLog.PrayerStatus> dayData = data.get(dateStr);
                                    if (dayData != null && dayData.containsKey(prayerName)) {
                                        PrayerLog.PrayerStatus status = dayData.get(prayerName);
                                        if (status == PrayerLog.PrayerStatus.ADA || status == PrayerLog.PrayerStatus.QADA) {
                                            completedPrayers++;
                                        }
                                    }
                                }
                            }
                            
                            date = date.plusDays(1);
                        }
                    }
                    
                    int lastWeekRate = totalPrayers > 0 ? (completedPrayers * 100 / totalPrayers) : 0;
                    int growth = currentRate - lastWeekRate;
                    
                    // Update UI on main thread
                    runOnUiThread(() -> {
                        String thisWeekText = getString(R.string.this_week_progress);
                        if (growth > 0) {
                            tvGrowth.setText(thisWeekText + " ↑ +" + growth + "%");
                            tvGrowth.setTextColor(android.graphics.Color.parseColor("#4CAF50")); // Green
                        } else if (growth < 0) {
                            tvGrowth.setText(thisWeekText + " ↓ " + growth + "%");
                            tvGrowth.setTextColor(android.graphics.Color.parseColor("#F44336")); // Red
                        } else {
                            tvGrowth.setText(thisWeekText + " → 0%");
                            tvGrowth.setTextColor(android.graphics.Color.parseColor("#999999")); // Grey
                        }
                        
                        Log.d(TAG, "Weekly growth: " + growth + "% (Current: " + currentRate + "%, Last: " + lastWeekRate + "%)");
                    });
                }
            }
        );
    }
    
    /**
     * ✅ 创建 Ripple 点击效果（圆角背景 + 波纹）
     * 提升用户点击反馈体验
     */
    private Drawable createRippleDrawable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // 圆角半径 (8dp)
            float cornerRadius = dpToPx(8);
            float[] radii = new float[8];
            for (int i = 0; i < 8; i++) {
                radii[i] = cornerRadius;
            }
            
            // 透明背景（圆角矩形）
            RoundRectShape roundRectShape = new RoundRectShape(radii, null, null);
            ShapeDrawable shapeDrawable = new ShapeDrawable(roundRectShape);
            shapeDrawable.getPaint().setColor(Color.TRANSPARENT);
            
            // Ripple 波纹颜色（浅灰色，20% 不透明度）
            ColorStateList rippleColor = ColorStateList.valueOf(Color.parseColor("#33000000"));
            
            return new RippleDrawable(rippleColor, shapeDrawable, null);
        } else {
            // API < 21: 使用简单的透明背景
            return null;
        }
    }

    /**
     * Determine if a prayer should be included in the completion denominator
     * based on whether its prayer window has started.
     */
    private boolean shouldIncludePrayerInDenominator(LocalDate date, String prayerName) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return true;
        }

        LocalDate today = LocalDate.now();
        if (date.isBefore(today)) {
            return true; // Past dates always counted
        }

        if (date.isAfter(today)) {
            return false; // Future dates excluded
        }

        // Same day: include only prayers whose window has started
        return hasPrayerWindowStarted(prayerName);
    }

    /**
     * Rough heuristic to determine whether a prayer's window has started today.
     * Uses general time anchors to avoid counting future prayers in the denominator.
     */
    private boolean hasPrayerWindowStarted(String prayerName) {
        Calendar now = Calendar.getInstance();
        int hour = now.get(Calendar.HOUR_OF_DAY);
        int minute = now.get(Calendar.MINUTE);

        switch (prayerName) {
            case "Fajr":
                return hour >= 4; // Typical dawn start
            case "Dhuhr":
                return hour >= 11; // Around midday
            case "Asr":
                return hour >= 15; // Mid-afternoon
            case "Maghrib":
                return hour >= 18; // Sunset
            case "Isha":
                if (hour > 20) {
                    return true;
                }
                if (hour == 20) {
                    return minute >= 0;
                }
                return false;
            default:
                return false;
        }
    }
}
