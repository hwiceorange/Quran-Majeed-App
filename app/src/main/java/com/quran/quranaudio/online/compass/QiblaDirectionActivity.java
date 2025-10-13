package com.quran.quranaudio.online.compass;

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


public class QiblaDirectionActivity extends AppCompatActivity {

    public PermissionUtil.PermissionRequestObject mRequestObject;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Configure status bar before setContentView for better visual effect
        setupStatusBar();
        
        setContentView(R.layout.activity_qibla_direction);
        loadFragment(new QiblaFragment()); // 使用增强版Fragment，移除地图依赖



        ImageView imgFavorite = (ImageView) findViewById(R.id.back);
        imgFavorite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                QiblaDirectionActivity.this.finish();
            }
        });
    }
    
    /**
     * Configure status bar to ensure system icons are visible
     * Status bar color matches the toolbar color (colorAccent/green)
     */
    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            
            // Set status bar color to match toolbar (colorAccent - green)
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.colorAccent));
            
            // For API 23+, set light/dark status bar icons based on background color
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                WindowCompat.setDecorFitsSystemWindows(window, true);
                WindowInsetsControllerCompat insetsController = 
                    WindowCompat.getInsetsController(window, window.getDecorView());
                if (insetsController != null) {
                    // Use light status bar icons (dark icons) for light backgrounds
                    // Use dark status bar icons (light icons) for dark backgrounds
                    // Since colorAccent is green (dark), we want light icons
                    insetsController.setAppearanceLightStatusBars(false);
                }
            }
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