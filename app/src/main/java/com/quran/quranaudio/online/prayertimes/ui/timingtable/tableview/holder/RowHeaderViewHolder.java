package com.quran.quranaudio.online.prayertimes.ui.timingtable.tableview.holder;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.evrencoskun.tableview.adapter.recyclerview.holder.AbstractViewHolder;
import com.quran.quranaudio.online.R;

/**
 * Author: Rai Adnan
 * Whatsapp: +923002375907
 * Email: officialshaheendevelopers@gmail.com
 * Portfolio: https://codecanyon.net/user/shaheendevelopers/portfolio
 */
public class RowHeaderViewHolder extends AbstractViewHolder {
    @NonNull
    public final TextView row_header_textview;

    public RowHeaderViewHolder(@NonNull View itemView) {
        super(itemView);
        row_header_textview = itemView.findViewById(R.id.row_header_textview);
    }
}
