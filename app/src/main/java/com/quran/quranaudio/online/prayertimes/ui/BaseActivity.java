package com.quran.quranaudio.online.prayertimes.ui;

import static com.quran.quranaudio.online.quran_module.utils.sharedPrefs.SPAppConfigs.LOCALE_DEFAULT;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;

import androidx.appcompat.app.AppCompatActivity;

import com.quran.quranaudio.online.App;
import com.quran.quranaudio.online.prayertimes.preferences.PreferencesHelper;
import com.quran.quranaudio.online.quran_module.utils.sharedPrefs.SPAppConfigs;
import com.quran.quranaudio.online.quran_module.utils.sharedPrefs.SPAppConfigs;

import java.util.Locale;

import javax.inject.Inject;


public class BaseActivity extends AppCompatActivity {

    @Inject
    PreferencesHelper preferencesHelper;

    @Override
    protected void attachBaseContext(Context base) {
          super.attachBaseContext(initBeforeBaseAttach(base));
    }


    private Context initBeforeBaseAttach(Context base) {
        return updateBaseContextLocale(base);
    }

    private Context updateBaseContextLocale(Context context) {
        String language = SPAppConfigs.getLocale(context);

        // 只检查空值，不再跳过默认语言的处理
        if (language == null || language.isEmpty()) {
            return context;
        }

        Locale locale = new Locale(language);
        Locale.setDefault(locale);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
            return updateResourcesLocale(context, locale);
        }
        return updateResourcesLocaleLegacy(context, locale);
    }

    @TargetApi(Build.VERSION_CODES.N_MR1)
    private Context updateResourcesLocale(Context context, Locale locale) {
        Configuration configuration = new Configuration(context.getResources().getConfiguration());
        configuration.setLocale(locale);
        return context.createConfigurationContext(configuration);
    }

    private Context updateResourcesLocaleLegacy(Context context, Locale locale) {
        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();
        configuration.locale = locale;
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
        return context;
    }

    @Override
    public void applyOverrideConfiguration(Configuration overrideConfiguration) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
            setLocale(overrideConfiguration.locale);
        }
        super.applyOverrideConfiguration(overrideConfiguration);
    }

    protected void setLocale(Locale locale) {
        SPAppConfigs.setLocale(this, locale.toLanguageTag());
        Resources resources = getResources();
        Configuration configuration = resources.getConfiguration();
        configuration.setLocale(locale);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
            getApplicationContext().createConfigurationContext(configuration);
        } else {
            DisplayMetrics displayMetrics = resources.getDisplayMetrics();
            resources.updateConfiguration(configuration, displayMetrics);
        }
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ((App) getApplicationContext())
                .defaultComponent
                .inject(this);

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
            int theme = preferencesHelper.getThemePreferenceId();
            setTheme(theme);
        }


        super.onCreate(savedInstanceState);
    }
}