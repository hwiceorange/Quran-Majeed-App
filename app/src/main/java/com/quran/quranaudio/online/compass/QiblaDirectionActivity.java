package com.quran.quranaudio.online.compass;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;

import com.github.kayvannj.permission_utils.PermissionUtil;
import com.quran.quranaudio.online.compass.fragment.QiblaFragment;
import com.quran.quranaudio.online.compass.fragment.QiblaMapFragment;
import com.quran.quranaudio.online.R;
import com.quran.quranaudio.online.quran_module.utils.sharedPrefs.SPAppConfigs;

import java.util.Locale;


@SuppressWarnings("deprecation")
public class QiblaDirectionActivity extends AppCompatActivity {

    public PermissionUtil.PermissionRequestObject mRequestObject;

    /**
     * 🌐 Override attachBaseContext to apply language settings
     * This ensures the Activity displays content in the user's selected language
     */
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(updateBaseContextLocale(base));
    }

    /**
     * 🌐 Update Context with user's selected language
     */
    private Context updateBaseContextLocale(Context context) {
        String language = SPAppConfigs.getLocale(context);
        
        // If no language is set, use default
        if (language == null || language.isEmpty()) {
            return context;
        }
        
        Locale locale = new Locale(language);
        Locale.setDefault(locale);
        
        return updateResourcesLocale(context, locale);
    }

    /**
     * 🌐 Update resources for Android N and above
     */
    private Context updateResourcesLocale(Context context, Locale locale) {
        Configuration configuration = new Configuration(context.getResources().getConfiguration());
        configuration.setLocale(locale);
        return context.createConfigurationContext(configuration);
    }

@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Configure status bar before setContentView for better visual effect
        setupStatusBar();
        
        setContentView(R.layout.activity_qibla_direction);

        // 🔄 统一设计风格：使用 Toolbar 的导航按钮
        setupToolbar();

        // 🧭 Map / Compass 两 Tab
        setupTabs();
    }

    /**
     * 设置 Map / Compass 两个 Tab。
     *
     * 默认落哪个 Tab 取决于设备是否有可用磁力计：
     * - 无磁力计（大量 T3 低端机：Tecno/Infinix/itel 等）→ 默认 Map，避免落到不可用的罗盘；
     * - 有磁力计 → 默认 Compass（传统体验，指哪拜哪更直观）。
     */
    private void setupTabs() {
        com.google.android.material.tabs.TabLayout tabLayout = findViewById(R.id.qibla_tab_layout);

        boolean hasMagnetometer = deviceHasMagnetometer();
        // Tab 顺序：0=Map，1=Compass
        int defaultTab = hasMagnetometer ? 1 : 0;

        showTab(defaultTab);
        showFirstTimeHintIfNeeded(hasMagnetometer);
        if (tabLayout != null) {
            com.google.android.material.tabs.TabLayout.Tab tab = tabLayout.getTabAt(defaultTab);
            if (tab != null) {
                tab.select();
            }
            tabLayout.addOnTabSelectedListener(
                    new com.google.android.material.tabs.TabLayout.OnTabSelectedListener() {
                        @Override
                        public void onTabSelected(com.google.android.material.tabs.TabLayout.Tab tab) {
                            showTab(tab.getPosition());
                        }

                        @Override
                        public void onTabUnselected(com.google.android.material.tabs.TabLayout.Tab tab) {
                        }

                        @Override
                        public void onTabReselected(com.google.android.material.tabs.TabLayout.Tab tab) {
                        }
                    });
        }
    }

    private void showTab(int position) {
        // 0=Map，1=Compass
        // 缓存两个 Fragment 实例，用 show/hide 而非 replace，避免每次切 Tab 地图重载闪烁
        androidx.fragment.app.FragmentManager fm = getSupportFragmentManager();
        androidx.fragment.app.FragmentTransaction tx = fm.beginTransaction();

        String targetTag = position == 0 ? TAG_MAP : TAG_COMPASS;
        String otherTag = position == 0 ? TAG_COMPASS : TAG_MAP;

        Fragment other = fm.findFragmentByTag(otherTag);
        if (other != null) {
            tx.hide(other);
        }

        Fragment target = fm.findFragmentByTag(targetTag);
        if (target == null) {
            target = position == 0 ? new QiblaMapFragment() : new QiblaFragment();
            tx.add(R.id.qibla_fragment_container, target, targetTag);
        } else {
            tx.show(target);
        }
        tx.commit();
    }

    private static final String TAG_MAP = "qibla_map";
    private static final String TAG_COMPASS = "qibla_compass";

    /**
     * 首次进入时一次性说明两个 Tab 的差异，帮助用户理解何时用地图、何时用罗盘。
     * 通过 SharedPreferences 门控，只弹一次；无磁力计设备额外提示"本机建议用地图"。
     */
    private void showFirstTimeHintIfNeeded(boolean hasMagnetometer) {
        try {
            android.content.SharedPreferences sp =
                    getSharedPreferences("QIBLA_PREFS", MODE_PRIVATE);
            if (sp.getBoolean("hint_shown", false)) {
                return;
            }
            sp.edit().putBoolean("hint_shown", true).apply();

            String msg = getString(R.string.qibla_intro_message);
            if (!hasMagnetometer) {
                msg = msg + "\n\n" + getString(R.string.qibla_intro_no_compass);
            }
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle(R.string.qibla_intro_title)
                    .setMessage(msg)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        } catch (Exception e) {
            // 引导失败绝不影响功能
        }
    }

    private boolean deviceHasMagnetometer() {
        try {
            android.hardware.SensorManager sm =
                    (android.hardware.SensorManager) getSystemService(SENSOR_SERVICE);
            return sm != null
                    && sm.getDefaultSensor(android.hardware.Sensor.TYPE_MAGNETIC_FIELD) != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Configure status bar to ensure system icons are visible
     * Status bar color matches the toolbar color (#4B9B76 green)
     */
    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            
            // 🔄 统一设计风格：状态栏颜色与 Toolbar 一致
            window.setStatusBarColor(0xFF4B9B76); // #4B9B76
            
            // For API 23+, set light/dark status bar icons based on background color
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                WindowCompat.setDecorFitsSystemWindows(window, true);
                WindowInsetsControllerCompat insetsController = 
                    WindowCompat.getInsetsController(window, window.getDecorView());
                if (insetsController != null) {
                    // Use light status bar icons (dark icons) for light backgrounds
                    // Use dark status bar icons (light icons) for dark backgrounds
                    // Since #4B9B76 is green (dark), we want light icons
                    insetsController.setAppearanceLightStatusBars(false);
                }
            }
        }
    }
    
    /**
     * 🔄 统一设计风格：设置 Toolbar 和返回按钮
     */
    private void setupToolbar() {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.custom_toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
            }
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    public void onRequestPermissionsResult(int i, @NonNull String[] strArr, @NonNull int[] iArr) {
        super.onRequestPermissionsResult(i, strArr, iArr);
        PermissionUtil.PermissionRequestObject permissionRequestObject = this.mRequestObject;
        if (permissionRequestObject != null) {
            permissionRequestObject.onRequestPermissionsResult(i, strArr, iArr);
        }
    }
    private boolean loadFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.qibla_fragment_container, fragment)
                    .commit();
            return true;
        }
        return false;
    }


}