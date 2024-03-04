package com.raiadnan.quranreader.prayertimes.prayers.views;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import com.raiadnan.quranreader.R;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class PrayerSunView extends FrameLayout {
    private SimpleDateFormat format = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private ImageView[] imageViews = new ImageView[6];
    private SunView sunView;
    private TextView[] textViews = new TextView[6];
    private View vLine;
    private View[] views = new View[6];

    public PrayerSunView(@NonNull Context context) {
        super(context);
        init();
    }

    public PrayerSunView(@NonNull Context context, @Nullable AttributeSet attributeSet) {
        super(context, attributeSet);
        init();
    }

    public PrayerSunView(@NonNull Context context, @Nullable AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        init();
    }

    private void init() {
        addView(LayoutInflater.from(getContext()).inflate((int) R.layout.prayer_sun_view, (ViewGroup) null));
        this.views[0] = findViewById(R.id.view_fajr);
        this.views[1] = findViewById(R.id.view_sunrise);
        this.views[2] = findViewById(R.id.view_dhuhr);
        this.views[3] = findViewById(R.id.view_asr);
        this.views[4] = findViewById(R.id.view_maghrib);
        this.views[5] = findViewById(R.id.view_isha);
        this.imageViews[0] = (ImageView) findViewById(R.id.img_fajr);
        this.imageViews[1] = (ImageView) findViewById(R.id.img_sunrise);
        this.imageViews[2] = (ImageView) findViewById(R.id.img_dhuhr);
        this.imageViews[3] = (ImageView) findViewById(R.id.img_asr);
        this.imageViews[4] = (ImageView) findViewById(R.id.img_maghrib);
        this.imageViews[5] = (ImageView) findViewById(R.id.img_isha);
        this.textViews[0] = (TextView) findViewById(R.id.tv_fajr);
        this.textViews[1] = (TextView) findViewById(R.id.tv_sunrise);
        this.textViews[2] = (TextView) findViewById(R.id.tv_dhuhr);
        this.textViews[3] = (TextView) findViewById(R.id.tv_asr);
        this.textViews[4] = (TextView) findViewById(R.id.tv_maghrib);
        this.textViews[5] = (TextView) findViewById(R.id.tv_isha);
        this.sunView = (SunView) findViewById(R.id.sunView);
        this.vLine = findViewById(R.id.view_line);
    }

    public void set(final Date[] dateArr) {
        boolean z;
        int i = 0;
        while (true) {
            TextView[] textViewArr = this.textViews;
            if (i >= textViewArr.length) {
                break;
            }
            textViewArr[i].setText(this.format.format(dateArr[i]));
            i++;
        }
        for (ImageView colorFilter : this.imageViews) {
            colorFilter.setColorFilter((int) ViewCompat.MEASURED_STATE_MASK);
        }
        for (TextView textColor : this.textViews) {
            textColor.setTextColor(Color.parseColor("#C9C9C9"));
        }
        this.vLine.post(new Runnable() {

            public final void run() {
                long time = dateArr[5].getTime() - dateArr[0].getTime();
                int width = PrayerSunView.this.vLine.getWidth();
                long j = (long) width;
                PrayerSunView.this.views[1].setTranslationX((float) (((dateArr[1].getTime() - dateArr[0].getTime()) * j) / time));
                View[] viewArr = PrayerSunView.this.views;
                viewArr[2].setTranslationX((float) ((((dateArr[2].getTime() - dateArr[0].getTime()) * j) / time) - ((long) viewArr[2].getWidth())));
                View[] viewArr2 = PrayerSunView.this.views;
                viewArr2[3].setTranslationX((float) ((((dateArr[3].getTime() - dateArr[0].getTime()) * j) / time) - ((long) viewArr2[3].getWidth())));
                long time2 = (j * (dateArr[4].getTime() - dateArr[0].getTime())) / time;
                View[] viewArr3 = PrayerSunView.this.views;
                viewArr3[4].setTranslationX((float) (time2 - ((long) viewArr3[4].getWidth())));
            }
        });
        Calendar instance = Calendar.getInstance();
        Calendar instance2 = Calendar.getInstance();
        instance.setTimeInMillis(dateArr[0].getTime());
        if (instance.get(5) == instance2.get(5) && instance.get(2) == instance2.get(2) && instance.get(1) == instance2.get(1)) {
            int i2 = 0;
            while (true) {
                if (i2 >= dateArr.length) {
                    break;
                } else if (dateArr[i2].getTime() > instance2.getTimeInMillis()) {
                    this.imageViews[i2].setColorFilter(getResources().getColor(R.color.sun_color));
                    this.textViews[i2].setTextColor(getResources().getColor(R.color.sun_color));
                    z = true;
                    break;
                } else {
                    i2++;
                }
            }
        }
        z = false;
        if (z) {
          this.sunView.setArcSolidColor(Color.parseColor("#C3F8E85D"));
        } else {
            this.sunView.setArcSolidColor(0);
        }
        this.sunView.setHideWeatherDrawble(z);
        this.sunView.setStartTime(this.format.format(dateArr[0]));
        this.sunView.setEndTime(this.format.format(dateArr[5]));
        this.sunView.setCurrentTime(this.format.format(new Date()));
        this.sunView.invalidate();
    }
}
