package com.raiadnan.quranreader.prayertimes.prayers.fragment;

import static com.google.maps.android.Context.getApplicationContext;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.raiadnan.quranreader.R;
import com.raiadnan.quranreader.activities.PrayerCalendarActivity;
import com.raiadnan.quranreader.hadith.HadithActivity;
import com.raiadnan.quranreader.hadith.search.SearchActivity;
import com.raiadnan.quranreader.prayertimes.App;
import com.raiadnan.quranreader.fragments.BaseFragment;
import com.raiadnan.quranreader.prayertimes.locations.helper.LocationSave;
import com.raiadnan.quranreader.prayertimes.prayers.adapter.PrayerAdapter;
import com.raiadnan.quranreader.prayertimes.prayers.helper.CalculatePrayerTime;
import com.raiadnan.quranreader.prayertimes.prayers.helper.Dates;
import com.raiadnan.quranreader.prayertimes.prayers.helper.HGDate;
import com.raiadnan.quranreader.prayertimes.prayers.helper.NumbersLocal;
import com.raiadnan.quranreader.prayertimes.prayers.model.PrayItem;
import com.raiadnan.quranreader.prayertimes.prayers.views.PrayerSunView;
import com.raiadnan.quranreader.prayertimes.utils.Utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;

public class PrayerFragment extends BaseFragment {
    public static final String ASR = "Asr";
    public static final String DHUHR = "Dhuhr";
    public static final String FAJR = "Fajr";
    public static final String ISHA = "Isha'a";
    public static final String MAGHRIB = "Maghrib";
    public static final String SUNRISE = "Sunrise";
    private View btnBackDay;
    private View btnNextDay;
    private HGDate hgDate;
    
    public ArrayList<PrayItem> prayItems = new ArrayList<>();
    private PrayerAdapter prayerAdapter;
    private RecyclerView rcvPrayer;
    private PrayerSunView sunView;
    private TextView tvDate;
    private TextView tvPrayerDate;

    public int getLayoutId() {
        return R.layout.fragment_prayer;
    }

    public static PrayerFragment newInstance() {
        Bundle bundle = new Bundle();
        PrayerFragment prayerFragment = new PrayerFragment();
        prayerFragment.setArguments(bundle);
        return prayerFragment;
    }

    public void onViewCreated(@NonNull View view, @Nullable Bundle bundle) {
        super.onViewCreated(view, bundle);

        view.findViewById(R.id.prayer_time_calendar).setOnClickListener(new View.OnClickListener() {
            public final void onClick(View view) {

                Intent intent = new Intent(getContext(), PrayerCalendarActivity.class);
                startActivity(intent);

            }
        });
        this.hgDate = new HGDate();
        initView();
        prayer();
        click();
    }

    public void moveCurrentDay() {
        this.hgDate = new HGDate();
        updateTitleTime();
    }

    public void onResume() {
        super.onResume();
        updateTitleTime();
    }

    private void prayer() {
        this.prayerAdapter = new PrayerAdapter(this.activity, this.prayItems) {
            public void OnItemClick(int i) {

                        ToneNotificationFragment newInstance = ToneNotificationFragment.newInstance();
                        newInstance.setPrayItem(((PrayItem) PrayerFragment.this.prayItems.get(i)).getTitle());
                        newInstance.setCallback(new App.SimpleCallback() {

                            public final void callback(Object obj) {

                                prayerAdapter.notifyItemChanged(i);
                            }
                        });
                        BaseFragment.addFragment(PrayerFragment.this.activity, newInstance);

            }



        };
        this.rcvPrayer = (RecyclerView) this.view.findViewById(R.id.rcv_prayer);
        this.rcvPrayer.setLayoutManager(new LinearLayoutManager(this.activity));
        this.rcvPrayer.setAdapter(this.prayerAdapter);
    }

    private void click() {
        this.btnBackDay.setOnClickListener(new View.OnClickListener() {
            public final void onClick(View view) {

                PrayerFragment.this.hgDate.previousDay();
                PrayerFragment.this.updateTitleTime();
            }
        });
        this.btnNextDay.setOnClickListener(new View.OnClickListener() {
            public final void onClick(View view) {
                PrayerFragment.this.hgDate.nextDay();
                PrayerFragment.this.updateTitleTime();
            }
        });

    }

    @SuppressLint({"SetTextI18n"})
    public void updateTitleTime() {
        this.hgDate.toGregorian();
        String convertNumberType = NumbersLocal.convertNumberType(getContext(), this.hgDate.getYear() + "");
        TextView textView = this.tvDate;
        textView.setText(NumbersLocal.convertNumberType(getContext(), this.hgDate.getDay() + "") + " " + (Dates.gregorianMonthName(getContext(), this.hgDate.getMonth() + -1) + "") + " " + convertNumberType);
        this.hgDate.toHigri();
        String convertNumberType2 = NumbersLocal.convertNumberType(getContext(), String.valueOf(this.hgDate.getDay()).trim());
        String trim = Dates.islamicMonthName(getContext(), this.hgDate.getMonth() + -1).trim();
        String convertNumberType3 = NumbersLocal.convertNumberType(getContext(), this.hgDate.getYear() + "");
        this.tvPrayerDate.setText(convertNumberType2 + " " + trim + " " + convertNumberType3);
        getPrayer();
    }

    private void getPrayer() {
        this.hgDate.toGregorian();
        ArrayList<String> NamazTimings = new CalculatePrayerTime(this.activity).NamazTimings(this.hgDate, LocationSave.getLat(), LocationSave.getLon());
        this.prayItems.clear();
        Date[] date = getDate(NamazTimings);
        this.prayItems.add(new PrayItem(R.drawable.symbol_fajr, FAJR, date[0].getTime()));
        this.prayItems.add(new PrayItem(R.drawable.symbol_sunrise, SUNRISE, date[1].getTime()));
        this.prayItems.add(new PrayItem(R.drawable.symbol_dhuhr, DHUHR, date[2].getTime()));
        this.prayItems.add(new PrayItem(R.drawable.symbol_asr, ASR, date[3].getTime()));
        this.prayItems.add(new PrayItem(R.drawable.symbol_maghrib, MAGHRIB, date[4].getTime()));
        this.prayItems.add(new PrayItem(R.drawable.symbol_ishaa, ISHA, date[5].getTime()));
        Calendar instance = Calendar.getInstance();
        Calendar instance2 = Calendar.getInstance();
        instance.setTimeInMillis(date[0].getTime());
        if (instance.get(5) == instance2.get(5) && instance.get(2) == instance2.get(2) && instance.get(1) == instance2.get(1)) {
            Iterator<PrayItem> it = this.prayItems.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                PrayItem next = it.next();
                if (next.getTime() > instance2.getTimeInMillis()) {
                    next.setSelect(true);
                    break;
                }
            }
        }
        this.prayerAdapter.notifyDataSetChanged();
        this.sunView.set(date);
    }

    private Date[] getDate(ArrayList<String> arrayList) {
        Date[] dateArr = new Date[6];
        for (int i = 0; i < arrayList.size(); i++) {
            String[] split = arrayList.get(i).split(":");
            int parseInt = Integer.parseInt(split[0]);
            int parseInt2 = Integer.parseInt(split[1]);
            Calendar instance = Calendar.getInstance();
            instance.set(this.hgDate.getYear(), this.hgDate.getMonth() - 1, this.hgDate.getDay(), parseInt, parseInt2, 0);
            dateArr[i] = instance.getTime();
        }
        return dateArr;
    }

    private void initView() {
        this.tvDate = (TextView) this.view.findViewById(R.id.tv_day);
        this.tvPrayerDate = (TextView) this.view.findViewById(R.id.tv_prayer_day);
        this.btnBackDay = this.view.findViewById(R.id.bt_back_day);
        this.btnNextDay = this.view.findViewById(R.id.bt_next_day);
        this.rcvPrayer = (RecyclerView) this.view.findViewById(R.id.rcv_prayer);
        this.sunView = (PrayerSunView) this.view.findViewById(R.id.pray_sun_view);
    }
}
