package com.quran.quranaudio.online.compass.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.quran.quranaudio.online.R;

public abstract class CompassAdapter extends RecyclerView.Adapter<CompassAdapter.ViewHolder> {
    
    public int[] compass = {R.drawable.compass_1, R.drawable.compass_2, R.drawable.compass_3, R.drawable.compass_4, R.drawable.compass_5};
    
    public int[] compassK = {R.drawable.compass_1_k, R.drawable.compass_2_k, R.drawable.compass_3_k, R.drawable.compass_4_k, R.drawable.compass_5_k};
    
    public Context context;

    public abstract void OnItemClick(int i, int i2);

    public CompassAdapter(Context context2) {
        this.context = context2;
    }

    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new ViewHolder(LayoutInflater.from(this.context).inflate((int) R.layout.item_compass, viewGroup, false));
    }

    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        viewHolder.imgCompass.setImageResource(this.compass[i]);
        viewHolder.imgCompassK.setImageResource(this.compassK[i]);
    }

    public int getItemCount() {
        return this.compass.length;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        
        public ImageView imgCompass;
        
        public ImageView imgCompassK;

        public ViewHolder(@NonNull View view) {
            super(view);
            this.imgCompass = (ImageView) view.findViewById(R.id.img_compass);
            this.imgCompassK = (ImageView) view.findViewById(R.id.img_compass_k);
            view.setOnClickListener(new View.OnClickListener() {
                public final void onClick(View view) {
                    CompassAdapter compassAdapter = CompassAdapter.this;
                    compassAdapter.OnItemClick(compassAdapter.compass[ViewHolder.this.getAdapterPosition()],
                            CompassAdapter.this.compassK[ViewHolder.this.getAdapterPosition()]);
                }
            });
            view.getLayoutParams().width = CompassAdapter.this.context.getResources().getDisplayMetrics().widthPixels / 5;
        }
    }
}
