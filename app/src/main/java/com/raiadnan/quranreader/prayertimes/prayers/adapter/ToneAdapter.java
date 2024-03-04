package com.raiadnan.quranreader.prayertimes.prayers.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.raiadnan.quranreader.R;
import com.raiadnan.quranreader.prayertimes.prayers.helper.PrayerTimeSettingsPref;
import com.raiadnan.quranreader.prayertimes.prayers.model.Tone;
import java.util.ArrayList;

public abstract class ToneAdapter extends RecyclerView.Adapter<ToneAdapter.ViewHolder> {
    private Context context;
    
    public String key;
    private ArrayList<Tone> tones;

    public abstract void OnPlayClick(int i);

    public ToneAdapter(Context context2, ArrayList<Tone> arrayList, String str) {
        this.context = context2;
        this.tones = arrayList;
        this.key = str;
    }

    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new ViewHolder(LayoutInflater.from(this.context).inflate((int) R.layout.item_tone, viewGroup, false));
    }

    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        Tone tone = this.tones.get(i);
        viewHolder.title.setText(tone.getTitle());
        viewHolder.icon.setImageResource(tone.getIcon());
        if (i == 0 || i == 1) {
            viewHolder.play.setVisibility(View.INVISIBLE);
        } else {
            viewHolder.play.setVisibility(View.VISIBLE);
        }
        if (i == PrayerTimeSettingsPref.get().getTone(this.key)) {
            viewHolder.check.setVisibility(View.VISIBLE);
        } else {
            viewHolder.check.setVisibility(View.INVISIBLE);
        }
    }

    public int getItemCount() {
        return this.tones.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        
        public ImageView check;
        
        public ImageView icon;
        
        public ImageView play;
        
        public TextView title;

        public ViewHolder(@NonNull View view) {
            super(view);
            this.title = (TextView) view.findViewById(R.id.tv_title);
            this.icon = (ImageView) view.findViewById(R.id.img_tone_type);
            this.check = (ImageView) view.findViewById(R.id.img_tick);
            this.play = (ImageView) view.findViewById(R.id.img_play);
            this.play.setOnClickListener(new View.OnClickListener() {
                public final void onClick(View view) {
                    ToneAdapter.this.OnPlayClick(ViewHolder.this.getAdapterPosition());
                }
            });
            view.setOnClickListener(new View.OnClickListener() {
                public final void onClick(View view) {
                  //  ViewHolder.lambda$new$1(ViewHolder.this, view);
                    int tone = PrayerTimeSettingsPref.get().getTone(ToneAdapter.this.key);
                    PrayerTimeSettingsPref.get().setTone(ToneAdapter.this.key, ViewHolder.this.getAdapterPosition());
                    ToneAdapter.this.notifyItemChanged(tone);
                    ToneAdapter.this.notifyItemChanged(ViewHolder.this.getAdapterPosition());
                }
            });
        }


    }
}
