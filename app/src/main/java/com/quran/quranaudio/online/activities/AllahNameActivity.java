package com.quran.quranaudio.online.activities;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.quran.quranaudio.online.Allah.names99.fragments.Name99Fragment;
import com.quran.quranaudio.online.R;
import com.quran.quranaudio.online.quran_module.utils.sharedPrefs.SPAppConfigs;

import java.util.Locale;

/**
 * 99 圣名(Asma ul Husna)宿主页。
 * 复用 SixKalmasActivity 的宿主模式（activity_kalmas 布局 + names99_fragment_container + custom_toolbar），
 * 装载既有的 {@link Name99Fragment}（圣名列表 + 诵读音频）。
 */
public class AllahNameActivity extends AppCompatActivity {

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
        Configuration configuration = new Configuration(context.getResources().getConfiguration());
        configuration.setLocale(locale);
        return context.createConfigurationContext(configuration);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 状态栏与 Toolbar 同色，统一设计风格
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(0xFF4B9B76);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.getDecorView().setSystemUiVisibility(0);
            }
        }

        setContentView(R.layout.activity_allah_names);
        loadFragment(Name99Fragment.newInstance());
        setupToolbar();
    }

    private void setupToolbar() {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.custom_toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(R.string.asma_ul_husna);
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
