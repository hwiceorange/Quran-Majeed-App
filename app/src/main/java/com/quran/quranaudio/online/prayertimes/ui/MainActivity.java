package com.quran.quranaudio.online.prayertimes.ui;

import android.Manifest;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;

import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.navigation.NavController;
import androidx.navigation.NavGraph;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.quran.quranaudio.online.App;
import com.quran.quranaudio.online.Utils.GifImageView;
import com.quran.quranaudio.online.prayertimes.job.WorkCreator;
import com.quran.quranaudio.online.prayertimes.preferences.PreferencesHelper;
import com.quran.quranaudio.online.prayertimes.ui.home.HomeViewModel;
import com.quran.quranaudio.online.R;
import com.quran.quranaudio.quiz.utils.RxBus;
import com.quran.quranaudio.quiz.base.MainTabChangeEvent;

import javax.inject.Inject;

import androidx.lifecycle.ViewModelProvider;


public class MainActivity extends BaseActivity {

   /* ActivityResultLauncher<String[]> mPermissionResultLauncher;
    private boolean isLocationPermissionGranted = false;*/

    @Inject
    PreferencesHelper preferencesHelper;

    private PrayerDataPreloader prayerDataPreloader;
    private NavController navController;
    private BottomNavigationView navView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ((App) getApplicationContext())
                .defaultComponent
                .inject(this);

        // Inject PrayerDataPreloader from HomeComponent
        prayerDataPreloader = ((App) getApplicationContext())
                .appComponent
                .homeComponent()
                .create()
                .getPrayerDataPreloader();

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        navView = findViewById(R.id.nav_view);

     /*   //PermissionStart

        mPermissionResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), new ActivityResultCallback<Map<String, Boolean>>() {
            @Override
            public void onActivityResult(Map<String, Boolean> result) {
                if (result.get(Manifest.permission.ACCESS_FINE_LOCATION) !=null) {
                    isLocationPermissionGranted = result.get(Manifest.permission.ACCESS_FINE_LOCATION);
                }
            }
        });

        //Permission End*/

     //   requestPermission();


        navController = Navigation.findNavController(this, R.id.home_host_fragment);
        NavigationUI.setupWithNavController(navView, navController);
        
        // 设置统一的白色状态栏 + 深色图标（所有页面统一效果）
        setupUnifiedStatusBar();
        android.util.Log.e("MainActivity", "✅ 初始化：设置统一白色状态栏");
        
        // Add navigation item selection listener with logging
        navView.setOnItemSelectedListener(item -> {
            android.util.Log.d("MainActivity", "Bottom nav item clicked: " + item.getTitle() + " (ID: " + item.getItemId() + ")");
            
            // Let NavigationUI handle all navigation items (including Quran)
            boolean handled = NavigationUI.onNavDestinationSelected(item, navController);
            
            if (handled) {
                android.util.Log.d("MainActivity", "Navigation handled by NavigationUI");
            } else {
                android.util.Log.w("MainActivity", "Navigation NOT handled by NavigationUI, trying manual navigation");
                // Fallback: manually navigate
                try {
                    navController.navigate(item.getItemId());
                    android.util.Log.d("MainActivity", "Manual navigation successful");
                    return true;
                } catch (Exception e) {
                    android.util.Log.e("MainActivity", "Manual navigation failed", e);
                    return false;
                }
            }
            
            return handled;
        });

        NavGraph navGraph = navController.getNavInflater().inflate(R.navigation.nav_graphmain);

        // Set correct start destination: Home page for normal launch
        if (displaySettingsScreenFirst()) {
            navGraph.setStartDestination(R.id.navigation_settings);
        } else {
            navGraph.setStartDestination(R.id.nav_home);  // Fixed: Start at Home page, not Learn page
        }

        navController.setGraph(navGraph);
        preferencesHelper.setFirstTimeLaunch(false);

        WorkCreator.schedulePeriodicPrayerUpdater(this);

        // Preload HomeViewModel at app startup to fetch prayer data in background
        // This ensures data is ready when user navigates to Home page
        preloadPrayerData();
        
        // Register RxBus listener for MainTabChangeEvent (from quiz result page)
        registerQuizResultListener();
    }

    /**
     * Preload prayer data at app startup for faster Home page display
     * Delegates to PrayerDataPreloader which creates HomeViewModel in background
     */
    private void preloadPrayerData() {
        if (prayerDataPreloader != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            prayerDataPreloader.preloadPrayerData(this);
        }
    }

    /**
     * Register RxBus listener to handle navigation from quiz result page to Discover tab
     */
    private void registerQuizResultListener() {
        RxBus.INSTANCE().register(this, MainTabChangeEvent.class, event -> {
            if (MainTabChangeEvent.TO_QUIZ.equals(event.toType)) {
                android.util.Log.d("MainActivity", "📱 Received MainTabChangeEvent.TO_QUIZ - navigating to Discover tab");
                
                // Navigate to Discover tab (nav_name_99)
                if (navController != null && navView != null) {
                    try {
                        navController.navigate(R.id.nav_name_99);
                        navView.setSelectedItemId(R.id.nav_name_99);
                        android.util.Log.d("MainActivity", "✅ Successfully navigated to Discover tab");
                    } catch (Exception e) {
                        android.util.Log.e("MainActivity", "❌ Failed to navigate to Discover tab", e);
                    }
                }
            }
        });
        android.util.Log.d("MainActivity", "✅ Quiz result listener registered");
    }
/*
    private void requestPermission(){
        isLocationPermissionGranted = ContextCompat.checkSelfPermission(
                this,
              Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;

        List<String> permissionRequest = new ArrayList<String>();

        if (!isLocationPermissionGranted) {
            permissionRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (!permissionRequest.isEmpty()) {

            mPermissionResultLauncher.launch(permissionRequest.toArray(new String[0]));
        }
    }
*/

    private boolean displaySettingsScreenFirst() {
        // Always start at Home page for new users
        // The Welcome dialog will guide them to grant location permission
        // Only show Settings first if explicitly needed (currently never)
        return false;
    }
    
    /**
     * 设置统一的白色状态栏（所有页面统一效果）
     * 白色背景 + 深色图标 + 内容不延伸到状态栏下方
     */
    private void setupUnifiedStatusBar() {
        try {
            Window window = getWindow();
            View decorView = window.getDecorView();
            
            // 确保内容不延伸到状态栏下方（非沉浸式）
            WindowCompat.setDecorFitsSystemWindows(window, true);
            
            // 设置状态栏为白色
            window.setStatusBarColor(0xFFFFFFFF);
            
            // 设置图标为深色（lightStatusBar = true 表示浅色背景需要深色图标）
            WindowInsetsControllerCompat wic = new WindowInsetsControllerCompat(window, decorView);
            wic.setAppearanceLightStatusBars(true);
            
            android.util.Log.e("MainActivity", "✅ 统一状态栏设置: 白色背景 + 深色图标");
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "❌ 设置统一状态栏失败", e);
        }
    }

    public void onBackPressed() {
        // Rate us dialog removed - directly finish the app
        finish();
    }

}
