package com.quran.quranaudio.online.prayertimes.ui.timingtable.tableview.holder;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.evrencoskun.tableview.adapter.recyclerview.holder.AbstractViewHolder;
import com.quran.quranaudio.online.R;


public class RowHeaderViewHolder extends AbstractViewHolder {
    @NonNull
    public final TextView row_header_textview;

    public RowHeaderViewHolder(@NonNull View itemView) {
        super(itemView);
        row_header_textview = itemView.findViewById(R.id.row_header_textview);
    }
}
