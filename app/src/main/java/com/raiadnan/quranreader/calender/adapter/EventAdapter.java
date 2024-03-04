package com.raiadnan.quranreader.calender.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import com.raiadnan.quranreader.R;
import com.raiadnan.quranreader.calender.fragment.EventDetailFragment;
import com.raiadnan.quranreader.calender.model.Event;
import com.raiadnan.quranreader.fragments.BaseFragment;
import com.raiadnan.quranreader.prayertimes.prayers.helper.Dates;
import com.raiadnan.quranreader.prayertimes.prayers.helper.HGDate;
import com.raiadnan.quranreader.prayertimes.prayers.helper.NumbersLocal;
import com.raiadnan.quranreader.activities.HomeActivity;

import java.util.ArrayList;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.ViewHolder> {
    
    public Context context;
    
    public ArrayList<Event> events;

    public EventAdapter(Context context2, ArrayList<Event> arrayList) {
        this.context = context2;
        this.events = arrayList;
    }

    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new ViewHolder(LayoutInflater.from(this.context).inflate((int) R.layout.item_event, viewGroup, false));
    }

    @SuppressLint({"SetTextI18n"})
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        Event event = this.events.get(i);
        String[] split = event.getHejriDate().split("/");
        HGDate hGDate = new HGDate();
        hGDate.setHigri(Integer.valueOf(split[2]).intValue(), Integer.valueOf(split[1]).intValue(), Integer.valueOf(split[0]).intValue());
        viewHolder.tvTitle.setText(event.getEventName());
        TextView access$100 = viewHolder.tvDateMuslim;
        access$100.setText(NumbersLocal.convertNumberType(this.context, split[0]) + " " + Dates.islamicMonthName(this.context, Integer.valueOf(split[1]).intValue() - 1) + " " + NumbersLocal.convertNumberType(this.context, split[2]));
        hGDate.toGregorian();
        TextView access$200 = viewHolder.tvDate;
        StringBuilder sb = new StringBuilder();
        Context context2 = this.context;
        sb.append(NumbersLocal.convertNumberType(context2, hGDate.getDay() + ""));
        sb.append(" ");
        sb.append(Dates.gregorianMonthName(this.context, hGDate.getMonth() - 1));
        sb.append(" ");
        Context context3 = this.context;
        sb.append(NumbersLocal.convertNumberType(context3, hGDate.getYear() + ""));
        access$200.setText(sb.toString());
    }

    public int getItemCount() {
        return this.events.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        
        public TextView tvDate;
        
        public TextView tvDateMuslim;
        
        public TextView tvTitle;

        public ViewHolder(@NonNull View view) {
            super(view);
            this.tvDate = (TextView) view.findViewById(R.id.tv_date);
            this.tvDateMuslim = (TextView) view.findViewById(R.id.tv_date_muslim);
            this.tvTitle = (TextView) view.findViewById(R.id.tv_title);
            view.setOnClickListener(new View.OnClickListener() {
                public final void onClick(View view) {


                    if (EventAdapter.this.context instanceof HomeActivity) {

                        EventDetailFragment newInstance = EventDetailFragment.newInstance();
                        newInstance.setEvent((Event) EventAdapter.this.events.get(ViewHolder.this.getAdapterPosition()));
                        BaseFragment.addFragment((AppCompatActivity) EventAdapter.this.context, newInstance);
                    }


                }
            });
        }

    }
}
