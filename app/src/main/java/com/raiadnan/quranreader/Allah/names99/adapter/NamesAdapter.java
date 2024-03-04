package com.raiadnan.quranreader.Allah.names99.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.raiadnan.quranreader.Allah.names99.model.NamesModel;
import com.raiadnan.quranreader.R;

import java.util.ArrayList;

public abstract class NamesAdapter extends RecyclerView.Adapter<NamesAdapter.ViewHolder> {
    private Context context;
    private int highlightPosition = -1;
    private ArrayList<NamesModel> namesModels;

    public abstract void OnItemClick(int i);

    public NamesAdapter(Context context2, ArrayList<NamesModel> arrayList) {
        this.context = context2;
        this.namesModels = arrayList;
    }


    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new ViewHolder(LayoutInflater.from(this.context).inflate((int) R.layout.item_names99, viewGroup, false));
    }

    @SuppressLint({"SetTextI18n"})
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        TextView access$100 = viewHolder.tvSurahNo;
        access$100.setText("" + (i + 1));
        viewHolder.tvMakkiMadni.setText(this.namesModels.get(i).getMeaning().trim());
        viewHolder.tvNameArabic.setText(this.namesModels.get(i).getArabic());
        viewHolder.tvNameEnglish.setText(this.namesModels.get(i).getEng());
        viewHolder.ayahRow.setBackgroundColor(Color.parseColor("#FAFAFA"));
        if (i == this.highlightPosition) {
            viewHolder.tvMakkiMadni.setTextColor(Color.parseColor("#ffffff"));
            viewHolder.tvNameArabic.setTextColor(Color.parseColor("#ffffff"));
            viewHolder.tvNameEnglish.setTextColor(Color.parseColor("#ffffff"));
            viewHolder.ayahRow.setBackgroundColor(Color.parseColor("#799999CA"));
            return;
        }
        viewHolder.ayahRow.setBackgroundColor(Color.parseColor("#FAFAFA"));
        viewHolder.tvMakkiMadni.setTextColor(Color.parseColor("#555555"));
        viewHolder.tvNameArabic.setTextColor(Color.parseColor("#9C9CCF"));
        viewHolder.tvNameEnglish.setTextColor(Color.parseColor("#9C9CCF"));
    }

    public void movePosition(int i) {
        this.highlightPosition = i;
        notifyDataSetChanged();
    }

    public int getItemCount() {
        return this.namesModels.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        
        public RelativeLayout ayahRow;
        
        public TextView tvMakkiMadni;
        
        public TextView tvNameArabic;
        
        public TextView tvNameEnglish;
        
        public TextView tvSurahNo;

        private ViewHolder(@NonNull View view) {
            super(view);
            this.tvSurahNo = (TextView) view.findViewById(R.id.tv_surah_no);
            this.tvMakkiMadni = (TextView) view.findViewById(R.id.maki_madni);
            this.tvNameArabic = (TextView) view.findViewById(R.id.arabic_name);
            this.tvNameEnglish = (TextView) view.findViewById(R.id.tvEnglishName);
            this.ayahRow = (RelativeLayout) view.findViewById(R.id.index_row);
            view.setOnClickListener(new View.OnClickListener() {
                public final void onClick(View view) {
                    NamesAdapter.this.OnItemClick(ViewHolder.this.getAdapterPosition());
                }
            });
        }
    }
}
