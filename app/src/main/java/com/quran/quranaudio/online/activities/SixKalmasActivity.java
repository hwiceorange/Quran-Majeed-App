package com.quran.quranaudio.online.activities;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
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
import com.quran.quranaudio.online.quran_module.utils.sharedPrefs.SPAppConfigs;

import java.util.Locale;


public class SixKalmasActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(updateBaseContextLocale(base));
    }

    private Context updateBaseContextLocale(Context context) {
        String language = SPAppConfigs.getLocale(context);
        if (language == null || language.isEmpty()) {
            return context;
        }
        Locale locale = new Locale(language);
        Locale.setDefault(locale);
        return updateResourcesLocale(context, locale);
    }

    private Context updateResourcesLocale(Context context, Locale locale) {
        Configuration configuration = new Configuration(context.getResources().getConfiguration());
        configuration.setLocale(locale);
        return context.createConfigurationContext(configuration);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 🔄 统一设计风格：状态栏颜色与 Toolbar 一致
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(0xFF4B9B76); // #4B9B76 - 与 Toolbar 背景色一致
            
            // 设置状态栏图标为亮色（白色）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.getDecorView().setSystemUiVisibility(0);
            }
        }
        
        setContentView(R.layout.activity_kalmas);
        loadFragment(new KalmaFragment());

        // 🔄 统一设计风格：使用 Toolbar 的导航按钮
        setupToolbar();
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
