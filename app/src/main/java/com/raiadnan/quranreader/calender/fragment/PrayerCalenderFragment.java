package com.raiadnan.quranreader.calender.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.raiadnan.quranreader.Constant;
import com.raiadnan.quranreader.R;
import com.raiadnan.quranreader.prayertimes.App;
import com.raiadnan.quranreader.fragments.BaseFragment;
import com.raiadnan.quranreader.prayertimes.locations.helper.LocationSave;
import com.raiadnan.quranreader.prayertimes.prayers.adapter.PrayerCalendarAdapter;
import com.raiadnan.quranreader.prayertimes.prayers.helper.CalculatePrayerTime;
import com.raiadnan.quranreader.prayertimes.prayers.helper.Dates;
import com.raiadnan.quranreader.prayertimes.prayers.helper.HGDate;
import com.raiadnan.quranreader.prayertimes.prayers.model.PrayerCalendarItem;
import com.raiadnan.quranreader.prayertimes.utils.Utils;
import com.shaheendevelopers.ads.sdk.format.BannerAd;

import java.util.ArrayList;

public class PrayerCalenderFragment extends BaseFragment {
    BannerAd.Builder bannerAd;

    public int getLayoutId() {
        return R.layout.fragment_prayer_calendar;
    }

    public static PrayerCalenderFragment newInstance() {
        Bundle bundle = new Bundle();
        PrayerCalenderFragment prayerCalenderFragment = new PrayerCalenderFragment();
        prayerCalenderFragment.setArguments(bundle);
        return prayerCalenderFragment;
    }

    @SuppressLint({"SetTextI18n"})
    public void onViewCreated(@NonNull View view, @Nullable Bundle bundle) {
        super.onViewCreated(view, bundle);
        ((TextView) view.findViewById(R.id.tv_city)).setText(LocationSave.getCity());
        HGDate hGDate = new HGDate();
        hGDate.toHigri();
        String trim = Dates.islamicMonthName(getContext(), hGDate.getMonth() - 1).trim();
        ((TextView) view.findViewById(R.id.tv_month)).setText(trim + " " + hGDate.getYear());
        ((TextView) view.findViewById(R.id.tv_title)).setText(trim + " " + hGDate.getYear());
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.rcv_prayer_calender);
        NestedScrollView nestedScrollView = (NestedScrollView) view.findViewById(R.id.view_capture);
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setLayoutManager(new LinearLayoutManager(this.activity));
        recyclerView.setAdapter(new PrayerCalendarAdapter(this.activity, getItems()));
        view.findViewById(R.id.bt_share).setOnClickListener(new View.OnClickListener() {
            public final void onClick(View view) {
                final ProgressDialog progressDialog = new ProgressDialog(PrayerCalenderFragment.this.activity);
                progressDialog.setMessage("Loading...");
                progressDialog.setCancelable(false);
                progressDialog.show();

                Utils.shareBitmap(PrayerCalenderFragment.this.activity,
                        PrayerCalenderFragment.this.getBitmapFromView(nestedScrollView,
                                nestedScrollView.getChildAt(0).getHeight(), nestedScrollView.getChildAt(0).getWidth()),
                        "share via", new App.SimpleCallback() {


                    public final void callback(Object obj) {
                        progressDialog.dismiss();
                    }
                });




            }
        });
        loadBannerAd();
    }

    private void loadBannerAd() {
        bannerAd = new BannerAd.Builder((Activity) getContext())
                .setAdStatus(Constant.AD_STATUS)
                .setAdNetwork(Constant.AD_NETWORK)
                .setBackupAdNetwork(Constant.BACKUP_AD_NETWORK)
                .setAdMobBannerId(Constant.ADMOB_BANNER_ID)
                .setGoogleAdManagerBannerId(Constant.GOOGLE_AD_MANAGER_BANNER_ID)
                .setFanBannerId(Constant.FAN_BANNER_ID)
                .setUnityBannerId(Constant.UNITY_BANNER_ID)
                .setAppLovinBannerId(Constant.APPLOVIN_BANNER_ID)
                .setAppLovinBannerZoneId(Constant.APPLOVIN_BANNER_ZONE_ID)
                .setDarkTheme(false)
                .build();
    }

    private Bitmap getBitmapFromView(View view, int i, int i2) {
        Bitmap createBitmap = Bitmap.createBitmap(i2, i, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(createBitmap);
        Drawable background = view.getBackground();
        if (background != null) {
            background.draw(canvas);
        } else {
            canvas.drawColor(-1);
        }
        view.draw(canvas);
        return createBitmap;
    }

    private ArrayList<PrayerCalendarItem> getItems() {
        ArrayList<PrayerCalendarItem> arrayList = new ArrayList<>();
        HGDate hGDate = new HGDate();
        hGDate.toHigri();
        hGDate.setHigri(hGDate.getYear(), hGDate.getMonth(), 1);
        int month = hGDate.getMonth();
        do {
            int day = hGDate.getDay();
            hGDate.toGregorian();
            String str = day + ". (" + (hGDate.getMonth() + "/" + hGDate.getDay() + "/" + hGDate.getYear()) + ")";
            ArrayList<String> NamazTimings = new CalculatePrayerTime(this.activity).NamazTimings(hGDate, LocationSave.getLat(), LocationSave.getLon());
            arrayList.add(new PrayerCalendarItem(str, NamazTimings.get(0), NamazTimings.get(1), NamazTimings.get(2), NamazTimings.get(3), NamazTimings.get(4), NamazTimings.get(5)));
            hGDate.nextDay();
            hGDate.toHigri();
        } while (hGDate.getMonth() == month);
        return arrayList;
    }
}
