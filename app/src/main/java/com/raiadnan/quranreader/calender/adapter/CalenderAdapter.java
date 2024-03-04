package com.raiadnan.quranreader.calender.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.raiadnan.quranreader.R;
import com.raiadnan.quranreader.calender.model.CalendarCell;
import com.raiadnan.quranreader.prayertimes.prayers.helper.HGDate;
import com.raiadnan.quranreader.prayertimes.prayers.helper.NumbersLocal;
import java.util.ArrayList;

public abstract class CalenderAdapter extends RecyclerView.Adapter<CalenderAdapter.ViewHolder> {
    private ArrayList<CalendarCell> calendarCells;
    private Context context;
    private String[] events = {"1-1", "10-1", "12-3", "27-7", "15-8", "1-9", "1-10", "9-12", "10-12"};

    public abstract void OnItemClick(int i);

    public CalenderAdapter(Context context2, ArrayList<CalendarCell> arrayList) {
        this.context = context2;
        this.calendarCells = arrayList;
    }

    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new ViewHolder(LayoutInflater.from(this.context).inflate((int) R.layout.item_calender_cell, viewGroup, false));
    }

    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        CalendarCell calendarCell = this.calendarCells.get(i);
        if (calendarCell.isSelect()) {
            viewHolder.vSelect.setBackgroundResource(R.drawable.bg_select);
        } else {
            viewHolder.vSelect.setBackgroundColor(0);
        }
        if (calendarCell.getHijriDay() == -1) {
            viewHolder.tvDate.setVisibility(View.GONE);
            viewHolder.tvDateMuslim.setVisibility(View.GONE);
        } else {
            viewHolder.tvDate.setVisibility(View.VISIBLE);
            viewHolder.tvDateMuslim.setVisibility(View.VISIBLE);
        }
        String[] strArr = this.events;
        int length = strArr.length;
        int i2 = 0;
        while (true) {
            if (i2 >= length) {
                break;
            }
            String[] split = strArr[i2].split("-");
            if (calendarCell.getHijriDay() == Integer.parseInt(split[0].trim()) && calendarCell.getHijriMonth() == Integer.parseInt(split[1].trim())) {
                viewHolder.vEvent.setVisibility(View.VISIBLE);
                break;
            } else {
                viewHolder.vEvent.setVisibility(View.GONE);
                i2++;
            }
        }
        HGDate hGDate = new HGDate();
        if (calendarCell.getGeorgianMonth() == hGDate.getMonth() && calendarCell.getGeorgianDay() == hGDate.getDay() && calendarCell.getGeorgianYear() == hGDate.getYear()) {
            viewHolder.tvDate.setTextColor(Color.parseColor("#0c7264"));
            viewHolder.tvDateMuslim.setTextColor(Color.parseColor("#0c7264"));
            viewHolder.vBg.setBackgroundColor(Color.parseColor("#3C257065"));
        } else {
            viewHolder.tvDate.setTextColor(Color.parseColor("#666666"));
            viewHolder.tvDateMuslim.setTextColor(Color.parseColor("#666666"));
            viewHolder.vBg.setBackgroundColor(Color.parseColor("#FAFAFA"));
            int i3 = i % 7;
            if (i3 == 0) {
                viewHolder.tvDateMuslim.setTextColor(Color.parseColor("#D50000"));
                viewHolder.tvDate.setTextColor(Color.parseColor("#D50000"));
            }
            if (i3 == 6) {
                viewHolder.tvDateMuslim.setTextColor(Color.parseColor("#2962FF"));
                viewHolder.tvDate.setTextColor(Color.parseColor("#2962FF"));
            }
        }
        TextView access$100 = viewHolder.tvDate;
        Context context2 = this.context;
        access$100.setText(NumbersLocal.convertNumberType(context2, calendarCell.getGeorgianDay() + ""));
        TextView access$200 = viewHolder.tvDateMuslim;
        Context context3 = this.context;
        access$200.setText(NumbersLocal.convertNumberType(context3, calendarCell.getHijriDay() + ""));
    }

    public int getItemCount() {
        return this.calendarCells.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        
        public TextView tvDate;
        
        public TextView tvDateMuslim;
        
        public View vBg;
        
        public View vEvent;
        
        public View vSelect;

        public ViewHolder(@NonNull View view) {
            super(view);
            this.tvDate = (TextView) view.findViewById(R.id.tv_date);
            this.tvDateMuslim = (TextView) view.findViewById(R.id.tv_date_muslim);
            this.vBg = view.findViewById(R.id.view_bg);
            this.vEvent = view.findViewById(R.id.view_event);
            this.vSelect = view.findViewById(R.id.view_select);
            view.setOnClickListener(new View.OnClickListener() {
                public final void onClick(View view) {
                    CalenderAdapter.this.OnItemClick(ViewHolder.this.getAdapterPosition());
                }
            });
        }
    }
}
