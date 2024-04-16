package com.quran.quranaudio.online.prayertimes.ui.calendar;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.quran.quranaudio.online.R;

import java.util.List;

/**
 * Author: Rai Adnan
 * Whatsapp: +923002375907
 * Email: officialshaheendevelopers@gmail.com
 * Portfolio: https://codecanyon.net/user/shaheendevelopers/portfolio
 */
public class HolidayAdapter extends RecyclerView.Adapter<HolidayAdapter.HolidayViewHolder> {

    private List<String> holidays;

    public static class HolidayViewHolder extends RecyclerView.ViewHolder {
        public TextView textView;

        public HolidayViewHolder(View view) {
            super(view);
            textView = view.findViewById(R.id.item_holiday_text_view);
        }
    }

    public HolidayAdapter(List<String> holidays) {
        this.holidays = holidays;
    }

    @NonNull
    @Override
    public HolidayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.holiday_item_view, parent, false);

        return new HolidayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HolidayViewHolder holder, int position) {
        if (getItemCount() == 0) {
            holder.textView.setVisibility(View.INVISIBLE);
        } else {
            holder.textView.setText(holidays.get(position));
            holder.textView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return holidays.size();
    }
}
