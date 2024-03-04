package com.raiadnan.quranreader.prayertimes.prayers.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.raiadnan.quranreader.R;
import com.raiadnan.quranreader.prayertimes.prayers.helper.PrayerTimeSettingsPref;
import com.raiadnan.quranreader.prayertimes.prayers.helper.ToneHelper;
import com.raiadnan.quranreader.prayertimes.prayers.model.PrayItem;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public abstract class PrayerAdapter extends RecyclerView.Adapter<PrayerAdapter.ViewHolder> {
    private Context context;
    private SimpleDateFormat format = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private ArrayList<PrayItem> prayItems;

    public abstract void OnItemClick(int i);

    public PrayerAdapter(Context context2, ArrayList<PrayItem> arrayList) {
        this.context = context2;
        this.prayItems = arrayList;
    }

    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new ViewHolder(LayoutInflater.from(this.context).inflate((int) R.layout.item_prayer_time, viewGroup, false));
    }

    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        viewHolder.tvTitle.setText(this.prayItems.get(i).getTitle());
        viewHolder.icon.setImageResource(this.prayItems.get(i).getIcon());
        viewHolder.tvTime.setText(this.format.format(Long.valueOf(this.prayItems.get(i).getTime())));
//        viewHolder.iconTone.setImageResource(ToneHelper.get().getToneIconFromKey(this.prayItems.get(i).getTitle()));
        viewHolder.tvTone.setText(ToneHelper.get().getToneStringFromPosition(PrayerTimeSettingsPref.get().getTone(this.prayItems.get(i).getTitle())));
        if (this.prayItems.get(i).isSelect()) {
            viewHolder.bg.setBackgroundResource(R.drawable.prayer_item_select);
            viewHolder.icon.setColorFilter(-1);
//            viewHolder.iconTone.setColorFilter(-1);
            viewHolder.tvTitle.setTextColor(-1);
            viewHolder.tvTone.setTextColor(Color.parseColor("#ffffff"));
            viewHolder.tvTime.setTextColor(-1);
            viewHolder.tvTime.setTextColor(-1);
            return;
        }
        viewHolder.bg.setBackgroundResource(0);
        viewHolder.icon.setColorFilter(0);
        viewHolder.tvTitle.setTextColor(Color.parseColor("#222222"));
        viewHolder.tvTone.setTextColor(Color.parseColor("#555555"));
        viewHolder.tvTime.setTextColor(Color.parseColor("#222222"));
//        viewHolder.iconTone.setColorFilter(0);
    }

    public void setData(ArrayList<PrayItem> arrayList) {
        this.prayItems = arrayList;
        notifyDataSetChanged();
    }

    public int getItemCount() {
        return this.prayItems.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        
        public View bg;
        
        public ImageView icon;
        
        public ImageView iconTone;
        
        public TextView tvTime;
        
        public TextView tvTitle;
        
        public TextView tvTone;

        public ViewHolder(@NonNull View view) {
            super(view);
            this.tvTime = (TextView) view.findViewById(R.id.tv_time);
            this.tvTitle = (TextView) view.findViewById(R.id.tv_prayer_title);
            this.icon = (ImageView) view.findViewById(R.id.img_icon);
            this.tvTone = (TextView) view.findViewById(R.id.tv_prayer_tone);
            this.iconTone = (ImageView) view.findViewById(R.id.img_tone);
            this.bg = view.findViewById(R.id.view_bg);
          /*  view.setOnClickListener(new View.OnClickListener() {
                public final void onClick(View view) {
                    PrayerAdapter.this.OnItemClick(ViewHolder.this.getAdapterPosition());
                }
            });*/
        }
    }
}
