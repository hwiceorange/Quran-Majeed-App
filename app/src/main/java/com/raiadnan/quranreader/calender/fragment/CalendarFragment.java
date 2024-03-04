package com.raiadnan.quranreader.calender.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.raiadnan.quranreader.Constant;
import com.raiadnan.quranreader.R;
import com.raiadnan.quranreader.calender.adapter.CalenderAdapter;
import com.raiadnan.quranreader.calender.adapter.EventAdapter;
import com.raiadnan.quranreader.calender.model.CalendarCell;
import com.raiadnan.quranreader.calender.model.Event;
import com.raiadnan.quranreader.fragments.BaseFragment;
import com.raiadnan.quranreader.prayertimes.prayers.helper.Dates;
import com.raiadnan.quranreader.prayertimes.prayers.helper.HGDate;
import com.raiadnan.quranreader.prayertimes.prayers.helper.NumbersLocal;
import com.shaheendevelopers.ads.sdk.format.BannerAd;

import java.util.ArrayList;

public class CalendarFragment extends BaseFragment {

    BannerAd.Builder bannerAd;
    private CalenderAdapter adapter;
    private View btnLeft;
    private View btnRight;
    private int mainMonth;
    private int mainYear;
    
    public ArrayList<CalendarCell> monthList;
    private int space;
    private TextView tvMonth;
    private TextView tvMonthMuslim;

    /* access modifiers changed from: protected */
    public int getLayoutId() {
        return R.layout.fragment_calendar;
    }

    public static CalendarFragment newInstance() {
        Bundle bundle = new Bundle();
        CalendarFragment calendarFragment = new CalendarFragment();
        calendarFragment.setArguments(bundle);
        return calendarFragment;
    }

    public void onViewCreated(@NonNull View view, @Nullable Bundle bundle) {
        super.onViewCreated(view, bundle);
        initView();
        setup();
        initEvent();
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


    public void moveCurrentDay() {
        loadGregorianCalendar(new HGDate());
    }

    private void setup() {
        this.monthList = new ArrayList<>();
        this.adapter = new CalenderAdapter(getContext(), this.monthList) {
            public void OnItemClick(int i) {
                if (((CalendarCell) CalendarFragment.this.monthList.get(i)).getGeorgianDay() != -1) {
                    for (int i2 = 0; i2 < CalendarFragment.this.monthList.size(); i2++) {
                        if (((CalendarCell) CalendarFragment.this.monthList.get(i2)).isSelect()) {
                            ((CalendarCell) CalendarFragment.this.monthList.get(i2)).setSelect(false);
                            notifyItemChanged(i2);
                        }
                    }
                    ((CalendarCell) CalendarFragment.this.monthList.get(i)).setSelect(true);
                    notifyItemChanged(i);
                    CalendarFragment calendarFragment = CalendarFragment.this;
                    calendarFragment.getTitleDay(((CalendarCell) calendarFragment.monthList.get(i)).getGeorgianDay());
                }
            }
        };
        RecyclerView recyclerView = (RecyclerView) this.view.findViewById(R.id.rcv_calender);
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 7));
        recyclerView.setAdapter(this.adapter);
        loadGregorianCalendar(new HGDate());
        this.btnLeft.setOnClickListener(new View.OnClickListener() {
            public final void onClick(View view) {
                CalendarFragment calendarFragment=CalendarFragment.this;
                calendarFragment.mainMonth--;
                int i = calendarFragment.mainMonth;
                if (i <= 1) {
                    calendarFragment.mainMonth = 12;
                    calendarFragment.mainYear--;
                    calendarFragment.loadMonthsDayGregorian(calendarFragment.mainMonth, calendarFragment.mainYear);
                    return;
                }
                calendarFragment.loadMonthsDayGregorian(i, calendarFragment.mainYear);
            }
        });
        this.btnRight.setOnClickListener(new View.OnClickListener() {
            public final void onClick(View view) {
               CalendarFragment calendarFragment=CalendarFragment.this;
                calendarFragment.mainMonth++;
                int i = calendarFragment.mainMonth;
                if (i >= 13) {
                    calendarFragment.mainMonth = 1;
                    calendarFragment.mainYear++;
                    calendarFragment.loadMonthsDayGregorian(calendarFragment.mainMonth, calendarFragment.mainYear);
                    return;
                }
                calendarFragment.loadMonthsDayGregorian(i, calendarFragment.mainYear);



            }
        });
    }


    private void loadGregorianCalendar(HGDate hGDate) {
        this.mainMonth = hGDate.getMonth();
        this.mainYear = hGDate.getYear();
        loadMonthsDayGregorian(hGDate.getMonth(), hGDate.getYear());
    }

    @SuppressLint({"SetTextI18n"})
    private void loadMonthsDayGregorian(int i, int i2) {
        this.monthList.clear();
        HGDate hGDate = new HGDate();
        hGDate.setGregorian(i2, i, 1);
        boolean z = false;
        boolean z2 = true;
        while (i == hGDate.getMonth()) {
            HGDate hGDate2 = new HGDate(hGDate);
            hGDate2.toHigri();
            CalendarCell calendarCell = new CalendarCell(hGDate2.getDay(), hGDate.getDay(), hGDate2.getMonth(), i, hGDate2.weekDay() + 1, hGDate.getYear());
            if (z2) {
                this.space = hGDate.weekDay();
                z2 = false;
            }
            HGDate hGDate3 = new HGDate();
            if (calendarCell.getGeorgianMonth() == hGDate3.getMonth() && calendarCell.getGeorgianDay() == hGDate3.getDay() && calendarCell.getGeorgianYear() == hGDate3.getYear()) {
                getTitleDay(hGDate3.getDay());
                calendarCell.setSelect(true);
                z = true;
            }
            this.monthList.add(calendarCell);
            hGDate.nextDay();
        }
        if (!z) {
            getTitleDay(1);
            this.monthList.get(0).setSelect(true);
        }
        for (int i3 = 0; i3 < this.space; i3++) {
            this.monthList.add(0, new CalendarCell(-1, -1, -1, -1, -1, -1));
        }
        this.adapter.notifyDataSetChanged();
    }

    
    @SuppressLint({"SetTextI18n"})
    public void getTitleDay(int i) {
        HGDate hGDate = new HGDate();
        hGDate.setGregorian(this.mainYear, this.mainMonth, i);
        String convertNumberType = NumbersLocal.convertNumberType(getContext(), hGDate.getYear() + "");
        TextView textView = this.tvMonth;
        textView.setText(NumbersLocal.convertNumberType(getContext(), hGDate.getDay() + "") + " " + (Dates.gregorianMonthName(getContext(), hGDate.getMonth() + -1) + "") + " " + convertNumberType);
        hGDate.toHigri();
        String convertNumberType2 = NumbersLocal.convertNumberType(getContext(), String.valueOf(hGDate.getDay()).trim());
        String trim = Dates.islamicMonthName(getContext(), hGDate.getMonth() + -1).trim();
        String convertNumberType3 = NumbersLocal.convertNumberType(getContext(), hGDate.getYear() + "");
        this.tvMonthMuslim.setText(convertNumberType2 + " " + trim + " " + convertNumberType3);
    }

    private void initView() {
        this.btnLeft = this.view.findViewById(R.id.bt_left);
        this.btnRight = this.view.findViewById(R.id.bt_right);
        this.tvMonth = (TextView) this.view.findViewById(R.id.tv_month);
        this.tvMonthMuslim = (TextView) this.view.findViewById(R.id.tv_month_muslim);
    }

    private void initEvent() {
        RecyclerView recyclerView = (RecyclerView) this.view.findViewById(R.id.rcv_event);
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setLayoutManager(new LinearLayoutManager(this.activity));
        recyclerView.setAdapter(new EventAdapter(this.activity, getEvents()));
    }

    private ArrayList<Event> getEvents() {
        HGDate hGDate = new HGDate();
        hGDate.toHigri();
        ArrayList<Event> arrayList = new ArrayList<>();
        hGDate.setHigri(hGDate.getYear(), 1, 1);
        arrayList.add(new Event("Islamic New Year", hGDate.toString(), "https://en.wikipedia.org/wiki/Islamic_New_Year"));
        hGDate.setHigri(hGDate.getYear(), 1, 10);
        arrayList.add(new Event("Ashura", hGDate.toString(), "https://en.wikipedia.org/wiki/Ashura"));
        hGDate.setHigri(hGDate.getYear(), 3, 12);
        arrayList.add(new Event("Mawlid al-Nabi", hGDate.toString(), "https://en.wikipedia.org/wiki/Mawlid"));
        hGDate.setHigri(hGDate.getYear(), 7, 27);
        arrayList.add(new Event("Laylat al-Mi'raj", hGDate.toString(), "https://en.wikipedia.org/wiki/Isra_and_Mi%27raj"));
        hGDate.setHigri(hGDate.getYear(), 8, 15);
        arrayList.add(new Event("Laylat al-Bara'at", hGDate.toString(), "https://en.wikipedia.org/wiki/Mid-Sha%27ban"));
        hGDate.setHigri(hGDate.getYear(), 9, 1);
        arrayList.add(new Event("First day of Ramaḍān", hGDate.toString(), "https://en.wikipedia.org/wiki/Ramadan"));
        hGDate.setHigri(hGDate.getYear(), 10, 1);
        arrayList.add(new Event("Eid al-Fitr", hGDate.toString(), "https://en.wikipedia.org/wiki/Eid_al-Fitr"));
        hGDate.setHigri(hGDate.getYear(), 12, 9);
        arrayList.add(new Event("Day of Arafah", hGDate.toString(), "https://en.wikipedia.org/wiki/Day_of_Arafah"));
        hGDate.setHigri(hGDate.getYear(), 12, 10);
        arrayList.add(new Event("Eid al-Adha", hGDate.toString(), "https://en.wikipedia.org/wiki/Eid_al-Adha"));
        return arrayList;
    }
}
