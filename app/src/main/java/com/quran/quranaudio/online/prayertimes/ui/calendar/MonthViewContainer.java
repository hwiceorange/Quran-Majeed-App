package com.quran.quranaudio.online.prayertimes.ui.calendar;

import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.quran.quranaudio.online.R;
import com.kizitonwose.calendarview.ui.ViewContainer;


public class MonthViewContainer extends ViewContainer {

    private final LinearLayout legendLayout;

    public MonthViewContainer(@NonNull View view) {
        super(view);
        legendLayout = view.findViewById(R.id.legendLayout);
    }

    public LinearLayout getLegendLayout() {
        return legendLayout;
    }
}
