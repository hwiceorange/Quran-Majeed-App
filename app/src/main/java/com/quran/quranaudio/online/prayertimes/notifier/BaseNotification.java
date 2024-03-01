package com.quran.quranaudio.online.prayertimes.notifier;

import android.content.Context;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;

import com.quran.quranaudio.online.prayertimes.preferences.PreferencesConstants;
import com.quran.quranaudio.online.prayertimes.preferences.PreferencesHelper;
import com.quran.quranaudio.online.R;


public class BaseNotification {

    protected final Context context;
    protected final PreferencesHelper preferencesHelper;

    public BaseNotification(PreferencesHelper preferencesHelper, Context context) {
        this.preferencesHelper = preferencesHelper;
        this.context = context;
    }

    @DrawableRes
    protected int getActionIcon() {
        return R.drawable.ic_notifications_on_24dp_blue;
    }

    @DrawableRes
    protected int getNotificationIcon() {
        return R.drawable.ic_notifications_on_24dp_blue;
    }

    @ColorInt
    protected int getNotificationColor() {
        switch (preferencesHelper.getThemePreference()) {
            case PreferencesConstants.THEME_PREFERENCE_NAME_THEME_DARK_ORANGE:
                return ContextCompat.getColor(context, R.color.orange);
            case PreferencesConstants.THEME_PREFERENCE_NAME_THEME_DARK_TURQUOISE:
                return ContextCompat.getColor(context, R.color.turquoise_blue);
            case PreferencesConstants.THEME_PREFERENCE_NAME_THEME_WHITE_BLUE:
            default:
                return ContextCompat.getColor(context, R.color.dodger_blue);
        }
    }
}
