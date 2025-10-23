package com.quran.quranaudio.online.activities;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.quran.quranaudio.online.R;
import com.quran.quranaudio.online.features.kalma.KalmaFragment;


public class SixKalmasActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 设置状态栏透明，并使用统一的主题色
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));
            
            // 设置状态栏图标为亮色（白色）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.getDecorView().setSystemUiVisibility(0);
            }
        }
        
        setContentView(R.layout.activity_kalmas);
        loadFragment(new KalmaFragment());

        // 为工具栏添加状态栏高度的顶部padding
        FrameLayout customToolbar = findViewById(R.id.custom_toolbar);
        if (customToolbar != null) {
            ViewCompat.setOnApplyWindowInsetsListener(customToolbar, (v, insets) -> {
                int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
                v.setPadding(
                    v.getPaddingLeft(),
                    statusBarHeight,
                    v.getPaddingRight(),
                    v.getPaddingBottom()
                );
                return insets;
            });
        }

        ImageView imgFavorite = findViewById(R.id.back);
        imgFavorite.setOnClickListener(v -> finish());
    }

    private boolean loadFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.names99_fragment_container, fragment)
                    .commit();
            return true;
        }
        return false;
    }


}
