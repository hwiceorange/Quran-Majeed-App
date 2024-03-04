package com.raiadnan.quranreader.activities;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.PowerManager;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import com.bumptech.glide.Glide;
import com.raiadnan.quranreader.R;
import com.raiadnan.quranreader.prayertimes.locations.helper.LocationSave;
import com.raiadnan.quranreader.prayertimes.prayers.fragment.PrayerFragment;
import com.raiadnan.quranreader.prayertimes.prayers.helper.CalculatePrayerTime;
import com.raiadnan.quranreader.prayertimes.prayers.helper.Dates;
import com.raiadnan.quranreader.prayertimes.prayers.helper.HGDate;
import com.raiadnan.quranreader.prayertimes.prayers.helper.NumbersLocal;
import com.raiadnan.quranreader.prayertimes.prayers.helper.ToneHelper;
import com.raiadnan.quranreader.prayertimes.quickaccess.SlidePanelCustom;
import com.raiadnan.quranreader.prayertimes.utils.Utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class QuickAccessActivity extends AppCompatActivity {
    private CountDownTimer count;
    private ImageView imgNotification;
    private ImageView imgPrayer;
    private int nextPostition = 0;
    
    public long time = 0;
    private TextView tvCity;
    private TextView tvDate;
    private TextView tvDateMuslim;
    private TextView tvNext;
    
    public TextView tvTimeDown;

    public void onBackPressed() {
    }

    /* access modifiers changed from: protected */
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setupClearView();
        setContentView((int) R.layout.activity_quick_access);
   //     FlurryAgent.logEvent(getClass().getSimpleName());
        initSlide();
        initView();
        Glide.with((FragmentActivity) this).load(Integer.valueOf((int) R.drawable.back)).into((ImageView) findViewById(R.id.bg));
        findViewById(R.id.view_click).setOnClickListener(new View.OnClickListener() {
            public final void onClick(View view) {
                QuickAccessActivity.lambda$onCreate$0(QuickAccessActivity.this, view);
            }
        });
    }

    public static /* synthetic */ void lambda$onCreate$0(QuickAccessActivity quickAccessActivity, View view) {
        Intent intent = new Intent(quickAccessActivity, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        //intent.addFlags(CrashUtils.ErrorDialogData.BINDER_CRASH);
        quickAccessActivity.startActivity(intent);
        quickAccessActivity.finish();
    }

    private void initView() {
        this.tvDateMuslim = (TextView) findViewById(R.id.tv_date_muslim);
        this.tvDate = (TextView) findViewById(R.id.tv_date);
        this.imgNotification = (ImageView) findViewById(R.id.img_notification);
        this.tvNext = (TextView) findViewById(R.id.tv_next);
        this.tvTimeDown = (TextView) findViewById(R.id.tv_time_down);
        this.tvCity = (TextView) findViewById(R.id.tv_city);
        this.imgPrayer = (ImageView) findViewById(R.id.img_icon_prayer);
    }

    private void initSlide() {
        final SlidePanelCustom slidePanelCustom = (SlidePanelCustom) findViewById(R.id.slide_unlock);
        slidePanelCustom.setSliderFadeColor(0);
        slidePanelCustom.closePane();
        slidePanelCustom.setPanelSlideListener(new SlidePanelCustom.PanelSlideListener() {
            public void onPanelClosed(View view) {
            }

            public void onPanelOpened(View view) {
            }

            public void onPanelSlide(View view, float f) {
                if (slidePanelCustom.isOpen()) {
                    QuickAccessActivity.this.finish();
                }
            }
        });
    }

    /* access modifiers changed from: protected */
    public void onResume() {
        super.onResume();
        clearBar();
        getTime();
    }

    /* access modifiers changed from: protected */
    public void onPause() {
        super.onPause();
        gim();
        CountDownTimer countDownTimer = this.count;
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    @SuppressLint({"SetTextI18n"})
    public void getTime() {
        CountDownTimer countDownTimer = this.count;
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        String str = "";
        HGDate hGDate = new HGDate();
        hGDate.toGregorian();
        String convertNumberType = NumbersLocal.convertNumberType(this, hGDate.getYear() + "");
        TextView textView = this.tvDate;
        textView.setText(NumbersLocal.convertNumberType(this, hGDate.getDay() + "") + " " + (Dates.gregorianMonthName(this, hGDate.getMonth() + -1) + "") + " " + convertNumberType);
        hGDate.toHigri();
        String convertNumberType2 = NumbersLocal.convertNumberType(this, String.valueOf(hGDate.getDay()).trim());
        String trim = Dates.islamicMonthName(this, hGDate.getMonth() + -1).trim();
        String convertNumberType3 = NumbersLocal.convertNumberType(this, hGDate.getYear() + "");
        this.tvDateMuslim.setText(convertNumberType2 + " " + trim + " " + convertNumberType3);
        hGDate.toGregorian();
        Date[] date = getDate(hGDate, new CalculatePrayerTime(this).NamazTimings(hGDate, LocationSave.getLat(), LocationSave.getLon()));
        if (System.currentTimeMillis() > date[date.length - 1].getTime()) {
            hGDate.nextDay();
            hGDate.toGregorian();
            date = getDate(hGDate, new CalculatePrayerTime(this).NamazTimings(hGDate, LocationSave.getLat(), LocationSave.getLon()));
            str = " ( tomorrow )";
        }
        int i = 0;
        while (true) {
            if (i >= date.length) {
                break;
            } else if (date[i].getTime() > System.currentTimeMillis()) {
                this.nextPostition = i;
                break;
            } else {
                i++;
            }
        }
        String str2 = "";
        switch (this.nextPostition) {
            case 0:
                str2 = PrayerFragment.FAJR;
                break;
            case 1:
                str2 = PrayerFragment.SUNRISE;
                break;
            case 2:
                str2 = PrayerFragment.DHUHR;
                break;
            case 3:
                str2 = PrayerFragment.ASR;
                break;
            case 4:
                str2 = PrayerFragment.MAGHRIB;
                break;
            case 5:
                str2 = PrayerFragment.ISHA;
                break;
        }
        this.tvNext.setText(str2 + "" + str + " at " + new SimpleDateFormat("HH:mm", Locale.getDefault()).format(date[this.nextPostition]));
        CountDownTimer countDownTimer2 = this.count;
        if (countDownTimer2 != null) {
            countDownTimer2.cancel();
        }
        this.time = date[this.nextPostition].getTime() - System.currentTimeMillis();
        this.count = new CountDownTimer(this.time, 1000) {
            public void onTick(long j) {
                QuickAccessActivity quickAccessActivity = QuickAccessActivity.this;
                long unused = quickAccessActivity.time = quickAccessActivity.time - 1000;
                if (QuickAccessActivity.this.time < 0) {
                    QuickAccessActivity.this.getTime();
                    cancel();
                    return;
                }
                TextView access$100 = QuickAccessActivity.this.tvTimeDown;
                access$100.setText(" " + Utils.milliSecondsToTimerDown(QuickAccessActivity.this.time));
            }

            public void onFinish() {
                QuickAccessActivity.this.getTime();
            }
        };
        this.count.start();
        this.tvCity.setText(" " + LocationSave.getCity());
        this.imgNotification.setImageResource(ToneHelper.get().getToneIconFromKey(str2));
        this.imgPrayer.setImageResource(getIcon(str2));
    }

    /* JADX INFO: Can't fix incorrect switch cases order, some code will duplicate */
    private int getIcon(String str) {
        char c;
        switch (str.hashCode()) {
            case -2095641571:
                if (str.equals(PrayerFragment.ISHA)) {
                    c = 3;
                    break;
                }
                c = 65535;
                break;
            case -191907083:
                if (str.equals(PrayerFragment.SUNRISE)) {
                    c = 4;
                    break;
                }
                c = 65535;
                break;
            case 66144:
                if (str.equals(PrayerFragment.ASR)) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case 2181987:
                if (str.equals(PrayerFragment.FAJR)) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case 66013467:
                if (str.equals(PrayerFragment.DHUHR)) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            default:
                c = 65535;
                break;
        }
        switch (c) {
            case 0:
                return R.drawable.ic_asr;
            case 1:
                return R.drawable.ic_dhuhr;
            case 2:
                return R.drawable.ic_faji;
            case 3:
                return R.drawable.ic_isha;
            case 4:
                return R.drawable.ic_sunrise;
            default:
                return R.drawable.ic_maghrib;
        }
    }

    private Date[] getDate(HGDate hGDate, ArrayList<String> arrayList) {
        Date[] dateArr = new Date[6];
        for (int i = 0; i < arrayList.size(); i++) {
            String[] split = arrayList.get(i).split(":");
            int parseInt = Integer.parseInt(split[0]);
            int parseInt2 = Integer.parseInt(split[1]);
            Calendar instance = Calendar.getInstance();
            instance.set(hGDate.getYear(), hGDate.getMonth() - 1, hGDate.getDay(), parseInt, parseInt2, 0);
            dateArr[i] = instance.getTime();
        }
        return dateArr;
    }

    @SuppressLint("MissingPermission")
    private void gim() {
        ActivityManager activityManager = (ActivityManager) getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager != null) {
            activityManager.moveTaskToFront(getTaskId(), 0);
        }
    }

    private void setupClearView() {
        try {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } catch (Exception unused) {
        }
        requestWindowFeature(1);
        getWindow().setFlags(512, 512);
        getWindow().addFlags(524288);
        getWindow().addFlags(4194304);
    }

    private void clearBar() {
        if (getWindow() != null) {
            getWindow().getDecorView().setSystemUiVisibility(3842);
        }
    }

    public void onStop() {
        super.onStop();
        if (isScreenOn(this)) {
            finish();
        }
    }

    private boolean isScreenOn(Context context) {
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (powerManager == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= 20) {
            return powerManager.isInteractive();
        }
        return powerManager.isScreenOn();
    }
}
