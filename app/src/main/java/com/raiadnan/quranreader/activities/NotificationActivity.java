package com.raiadnan.quranreader.activities;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.location.Location;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.view.View;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.raiadnan.quranreader.R;
import com.raiadnan.quranreader.compass.helper.Compass;
import com.raiadnan.quranreader.compass.helper.CompassManager;
import com.raiadnan.quranreader.compass.view.CalibrateCompassDialog;
import com.raiadnan.quranreader.prayertimes.locations.helper.LocationSave;
import com.raiadnan.quranreader.prayertimes.prayers.fragment.PrayerFragment;
import com.raiadnan.quranreader.prayertimes.prayers.helper.PrayerTimeSettingsPref;
import com.raiadnan.quranreader.prayertimes.prayers.helper.ToneHelper;
import com.raiadnan.quranreader.prayertimes.utils.Utils;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class NotificationActivity extends AppCompatActivity {
    private Compass compass;
    private MediaPlayer mediaPlayer;

    public void onBackPressed() {
    }

    /* access modifiers changed from: protected */
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        getWindow().addFlags(524288);
        getWindow().addFlags(4194304);
        try {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } catch (Exception unused) {
        }
        setContentView((int) R.layout.activity_notification);
      //  FlurryAgent.logEvent(getClass().getSimpleName());
        String stringExtra = getIntent().getStringExtra("key");
        long longExtra = getIntent().getLongExtra("time", 0);
        ((TextView) findViewById(R.id.tv_time)).setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(Long.valueOf(longExtra)));
        ((TextView) findViewById(R.id.tv_date)).setText(new SimpleDateFormat("E, dd MMM yyyy", Locale.getDefault()).format(Long.valueOf(longExtra)));
        ((TextView) findViewById(R.id.tv_prayer)).setText(stringExtra);
        ((ImageView) findViewById(R.id.img_prayer)).setImageResource(getIcon(stringExtra));
        int tone = PrayerTimeSettingsPref.get().getTone(stringExtra);
        if (tone == 2) {
            RingtoneManager.getRingtone(this, RingtoneManager.getDefaultUri(2)).play();
        } else if (tone > 2) {
            this.mediaPlayer = MediaPlayer.create(this, ToneHelper.get().getToneResoureFromPosition(tone));
            this.mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                public final void onPrepared(MediaPlayer mediaPlayer) {
                    NotificationActivity.this.mediaPlayer.start();
                }
            });
        }
        if (PrayerTimeSettingsPref.get().getVibrate()) {
            Utils.vibratorLong();
        }
        findViewById(R.id.bt_close).setOnClickListener(new View.OnClickListener() {
            public final void onClick(View view) {
                NotificationActivity.this.finish();
            }
        });
        initCompass();
    }

    private void initCompass() {
        final Location location = new Location("dummyprovider");
        location.setLatitude(LocationSave.getLat());
        location.setLongitude(LocationSave.getLon());
        this.compass = new Compass(this);
        final ImageView imageView = (ImageView) findViewById(R.id.compass);
        final ImageView imageView2 = (ImageView) findViewById(R.id.compass_k);
        imageView.setImageResource(CompassManager.getCompass());
        imageView2.setImageResource(CompassManager.getCompassK());
        this.compass.setListener(new Compass.CompassListener() {
            private float currentAzimuth;
            private double result;

            public void onNewAzimuth(float f) {
                fetchGps();
                adjustGambarDial(f);
                adjustArrowQiblat(f);
            }

            public void onAccuracyChanged(String str) {
                new CalibrateCompassDialog(NotificationActivity.this, str).show();
            }

            /* access modifiers changed from: package-private */
            public void adjustGambarDial(float f) {
                RotateAnimation rotateAnimation = new RotateAnimation(-this.currentAzimuth, -f, 1, 0.5f, 1, 0.5f);
                this.currentAzimuth = f;
                rotateAnimation.setDuration(500);
                rotateAnimation.setRepeatCount(0);
                rotateAnimation.setFillAfter(true);
                imageView.startAnimation(rotateAnimation);
            }

            /* access modifiers changed from: package-private */
            public void adjustArrowQiblat(float f) {
                double d = (double) (-this.currentAzimuth);
                double d2 = this.result;
                Double.isNaN(d);
                RotateAnimation rotateAnimation = new RotateAnimation((float) (d + d2), -f, 1, 0.5f, 1, 0.5f);
                this.currentAzimuth = f;
                rotateAnimation.setDuration(500);
                rotateAnimation.setRepeatCount(0);
                rotateAnimation.setFillAfter(true);
                imageView2.startAnimation(rotateAnimation);
                if (this.result > 0.0d) {
                    imageView2.setVisibility(View.VISIBLE);
                    return;
                }
                imageView2.setVisibility(View.INVISIBLE);
                imageView2.setVisibility(View.GONE);
            }

            @SuppressLint({"SetTextI18n"})
            private void fetchGps() {
                double radians = Math.toRadians(21.42251d);
                double radians2 = Math.toRadians(location.getLatitude());
                double radians3 = Math.toRadians(39.82616d - location.getLongitude());
                this.result = (Math.toDegrees(Math.atan2(Math.sin(radians3) * Math.cos(radians), (Math.cos(radians2) * Math.sin(radians)) - ((Math.sin(radians2) * Math.cos(radians)) * Math.cos(radians3)))) + 360.0d) % 360.0d;
            }
        });
    }

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

    public void onResume() {
        super.onResume();
        Compass compass2 = this.compass;
        if (compass2 != null) {
            compass2.start(this);
        }
    }

    public void onPause() {
        super.onPause();
        Compass compass2 = this.compass;
        if (compass2 != null) {
            compass2.stop();
        }
    }

    /* access modifiers changed from: protected */
    public void onDestroy() {
        super.onDestroy();
        MediaPlayer mediaPlayer2 = this.mediaPlayer;
        if (mediaPlayer2 != null) {
            mediaPlayer2.release();
        }
    }
}
