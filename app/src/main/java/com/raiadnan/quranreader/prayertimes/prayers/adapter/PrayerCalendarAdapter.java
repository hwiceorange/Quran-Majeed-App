package com.raiadnan.quranreader.prayertimes.prayers.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.raiadnan.quranreader.R;
import com.raiadnan.quranreader.prayertimes.prayers.model.PrayerCalendarItem;
import java.util.ArrayList;

public class PrayerCalendarAdapter extends RecyclerView.Adapter<PrayerCalendarAdapter.ViewHolder> {
    private Context context;
    private ArrayList<PrayerCalendarItem> prayerCalendarItems;

    public PrayerCalendarAdapter(Context context2, ArrayList<PrayerCalendarItem> arrayList) {
        this.context = context2;
        this.prayerCalendarItems = arrayList;
    }

    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new ViewHolder(LayoutInflater.from(this.context).inflate((int) R.layout.item_prayer_calender, viewGroup, false));
    }

    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        PrayerCalendarItem prayerCalendarItem = this.prayerCalendarItems.get(i);
        viewHolder.tvTime.setText(prayerCalendarItem.getTime());
        viewHolder.tvFajr.setText(prayerCalendarItem.getFajr());
        viewHolder.tvSunrise.setText(prayerCalendarItem.getSunrise());
        viewHolder.tvDhuhr.setText(prayerCalendarItem.getDhuhr());
        viewHolder.tvAsr.setText(prayerCalendarItem.getAsr());
        viewHolder.tvMaghrib.setText(prayerCalendarItem.getMaghrib());
        viewHolder.tvIsha.setText(prayerCalendarItem.getIsha());
        if (i % 2 == 0) {
            viewHolder.itemView.setBackgroundColor(Color.parseColor("#E2E2E2"));
        } else {
            viewHolder.itemView.setBackgroundColor(Color.parseColor("#FAFAFA"));
        }
    }

    public int getItemCount() {
        return this.prayerCalendarItems.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        
        public TextView tvAsr;
        
        public TextView tvDhuhr;
        
        public TextView tvFajr;
        
        public TextView tvIsha;
        
        public TextView tvMaghrib;
        
        public TextView tvSunrise;
        
        public TextView tvTime;

        public ViewHolder(@NonNull View view) {
            super(view);
            this.tvTime = (TextView) view.findViewById(R.id.tv_time);
            this.tvFajr = (TextView) view.findViewById(R.id.tv_1);
            this.tvSunrise = (TextView) view.findViewById(R.id.tv_2);
            this.tvDhuhr = (TextView) view.findViewById(R.id.tv_3);
            this.tvAsr = (TextView) view.findViewById(R.id.tv_4);
            this.tvMaghrib = (TextView) view.findViewById(R.id.tv_5);
            this.tvIsha = (TextView) view.findViewById(R.id.tv_6);
        }
    }
}
