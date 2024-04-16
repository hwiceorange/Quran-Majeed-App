package com.quran.quranaudio.online.prayertimes.ui.timingtable.tableview.holder;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.evrencoskun.tableview.adapter.recyclerview.holder.AbstractSorterViewHolder;
import com.quran.quranaudio.online.R;
import com.quran.quranaudio.online.prayertimes.ui.timingtable.tableview.model.ColumnHeader;

import java.util.Objects;

/**
 * Author: Rai Adnan
 * Whatsapp: +923002375907
 * Email: officialshaheendevelopers@gmail.com
 * Portfolio: https://codecanyon.net/user/shaheendevelopers/portfolio
 */
public class ColumnHeaderViewHolder extends AbstractSorterViewHolder {

    @NonNull
    private final LinearLayout column_header_container;
    @NonNull
    private final TextView column_header_textview;

    public ColumnHeaderViewHolder(@NonNull View itemView) {
        super(itemView);
        column_header_textview = itemView.findViewById(R.id.column_header_textView);
        column_header_container = itemView.findViewById(R.id.column_header_container);
    }

    public void setColumnHeader(@Nullable ColumnHeader columnHeader) {
        column_header_textview.setText(String.valueOf(Objects.requireNonNull(columnHeader).getData()));


        column_header_container.getLayoutParams().width = LinearLayout.LayoutParams.WRAP_CONTENT;
        column_header_textview.requestLayout();
    }
}
