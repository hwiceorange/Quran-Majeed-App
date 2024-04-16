package com.quran.quranaudio.online.prayertimes.ui.timingtable.tableview.holder;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.evrencoskun.tableview.adapter.recyclerview.holder.AbstractViewHolder;
import com.quran.quranaudio.online.R;
import com.quran.quranaudio.online.prayertimes.ui.timingtable.tableview.model.Cell;

import java.util.Objects;

/**
 * Author: Rai Adnan
 * Whatsapp: +923002375907
 * Email: officialshaheendevelopers@gmail.com
 * Portfolio: https://codecanyon.net/user/shaheendevelopers/portfolio
 */
public class CellViewHolder extends AbstractViewHolder {
    @NonNull
    private final TextView cell_textview;
    @NonNull
    private final LinearLayout cell_container;

    public CellViewHolder(@NonNull View itemView) {
        super(itemView);
        cell_textview = itemView.findViewById(R.id.cell_data);
        cell_container = itemView.findViewById(R.id.cell_container);
    }

    public void setCell(@Nullable Cell cell) {
        cell_textview.setText(String.valueOf(Objects.requireNonNull(cell).getData()));

        cell_container.getLayoutParams().width = LinearLayout.LayoutParams.WRAP_CONTENT;
        cell_textview.requestLayout();
    }
}
