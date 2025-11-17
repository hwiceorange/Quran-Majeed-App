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
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return updateResourcesLocale(context, locale);
        }
        return updateResourcesLocaleLegacy(context, locale);
    }

    /**
     * 🌐 Update resources for Android N and above
     */
    private Context updateResourcesLocale(Context context, Locale locale) {
        Configuration configuration = new Configuration(context.getResources().getConfiguration());
        configuration.setLocale(locale);
        return context.createConfigurationContext(configuration);
    }

    /**
     * 🌐 Update resources for Android M and below
     */
    private Context updateResourcesLocaleLegacy(Context context, Locale locale) {
        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();
        configuration.locale = locale;
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
        return context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Configure status bar before setContentView for better visual effect
        setupStatusBar();
        
        setContentView(R.layout.activity_qibla_direction);
        loadFragment(new QiblaFragment()); // 使用增强版Fragment，移除地图依赖

        // 🔄 统一设计风格：使用 Toolbar 的导航按钮
        setupToolbar();
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