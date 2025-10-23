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
import com.quran.quranaudio.online.features.zakat.ZakatFragment;


public class ZakatCalculatorActivity extends AppCompatActivity {

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
                window.getDecorView().setSystemUiVisibility(0); // 清除亮色模式，使用深色背景+白色图标
            }
        }
        
        setContentView(R.layout.activity_zakat_calculator);
        loadFragment(new ZakatFragment());

        // 为工具栏添加状态栏高度的顶部padding
        FrameLayout customToolbar = findViewById(R.id.custom_toolbar);
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
