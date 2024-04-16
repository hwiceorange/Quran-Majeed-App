package com.quran.quranaudio.online.prayertimes.ui.timingtable;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.evrencoskun.tableview.TableView;
import com.quran.quranaudio.online.App;
import com.quran.quranaudio.online.prayertimes.common.ComplementaryTimingEnum;
import com.quran.quranaudio.online.prayertimes.common.PrayerEnum;
import com.quran.quranaudio.online.prayertimes.timings.DayPrayer;
import com.quran.quranaudio.online.prayertimes.ui.timingtable.tableview.TableViewAdapter;
import com.quran.quranaudio.online.prayertimes.ui.timingtable.tableview.model.Cell;
import com.quran.quranaudio.online.prayertimes.ui.timingtable.tableview.model.ColumnHeader;
import com.quran.quranaudio.online.prayertimes.ui.timingtable.tableview.model.RowHeader;
import com.quran.quranaudio.online.prayertimes.utils.UiUtils;
import com.quran.quranaudio.online.R;

import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;

/**
 * Author: Rai Adnan
 * Whatsapp: +923002375907
 * Email: officialshaheendevelopers@gmail.com
 * Portfolio: https://codecanyon.net/user/shaheendevelopers/portfolio
 */
public abstract class TimingTableBaseFragment extends Fragment {

    private View loadingLinearLayout;

    protected TableView mTableView;
    protected LocalDate tableLocalDate;

    @Inject
    ViewModelProvider.Factory viewModelFactory;

    @Override
    public void onAttach(@NonNull Context context) {
        ((App) context.getApplicationContext())
                .appComponent
                .timingTableComponent()
                .create()
                .inject(this);

        super.onAttach(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_timing_table, container, false);

        TimingTableViewModel timingTableViewModel = viewModelFactory.create(TimingTableViewModel.class);

        mTableView = rootView.findViewById(R.id.tableview);
        loadingLinearLayout = rootView.findViewById(R.id.loading_linear_layout);

        // Force direction to LTR until Table view RTL support released
        ConstraintLayout tableConstraintLayout = rootView.findViewById(R.id.table_constraint_layout);
        ViewCompat.setLayoutDirection(tableConstraintLayout, ViewCompat.LAYOUT_DIRECTION_LTR);

        loadingLinearLayout.setVisibility(View.VISIBLE);

        setTableDate();
        timingTableViewModel.getCalendar().observe(getViewLifecycleOwner(), this::initializeTableView);
        timingTableViewModel.processData(tableLocalDate, requireContext());

        return rootView;
    }

    protected abstract void setTableDate();

    protected abstract void setScrollAndSelect();

    private void initializeTableView(List<DayPrayer> calendar) {
        loadingLinearLayout.setVisibility(View.INVISIBLE);

        String month = tableLocalDate.getMonth().getDisplayName(TextStyle.SHORT, Locale.getDefault());

        TableViewAdapter tableViewAdapter = new TableViewAdapter(StringUtils.capitalize(month));

        mTableView.setAdapter(tableViewAdapter);
        tableViewAdapter.setAllItems(getColumnHeaderList(), getRowHeaderList(calendar), getCellList(calendar));

        setScrollAndSelect();
    }

    @NonNull
    private List<RowHeader> getRowHeaderList(List<DayPrayer> calendar) {
        List<RowHeader> rowHeaders = new ArrayList<>();

        for (int index = 0; index < calendar.size(); index++) {
            DayPrayer dayPrayer = calendar.get(index);

            rowHeaders.add(new RowHeader(String.valueOf(dayPrayer.getGregorianDay()), String.valueOf(dayPrayer.getGregorianDay())));
        }

        return rowHeaders;
    }

    @NonNull
    private List<ColumnHeader> getColumnHeaderList() {
        ColumnHeader[] columnHeaders = {
                new ColumnHeader(getResources().getString(R.string.hijri), getResources().getString(R.string.hijri)),
                new ColumnHeader(getResources().getString(R.string.SHORT_FAJR), getResources().getString(R.string.SHORT_FAJR)),
                new ColumnHeader(getResources().getString(R.string.SUNRISE), getResources().getString(R.string.SUNRISE)),
                new ColumnHeader(getResources().getString(R.string.SHORT_DHOHR), getResources().getString(R.string.SHORT_DHOHR)),
                new ColumnHeader(getResources().getString(R.string.SHORT_ASR), getResources().getString(R.string.SHORT_ASR)),
                new ColumnHeader(getResources().getString(R.string.SHORT_MAGHRIB), getResources().getString(R.string.SHORT_MAGHRIB)),
                new ColumnHeader(getResources().getString(R.string.SHORT_ICHA), getResources().getString(R.string.SHORT_ICHA))
        };

        return Arrays.asList(columnHeaders);
    }

    @NonNull
    private List<List<Cell>> getCellList(List<DayPrayer> calendar) {
        List<List<Cell>> list = new ArrayList<>();

        for (int index = 0; index < calendar.size(); index++) {
            List<Cell> cellList = new ArrayList<>();
            DayPrayer dayPrayer = calendar.get(index);
            Map<PrayerEnum, LocalDateTime> timings = dayPrayer.getTimings();
            Map<ComplementaryTimingEnum, LocalDateTime> complementaryTiming = dayPrayer.getComplementaryTiming();

            cellList.add(new Cell(index + "-0", getHijriShortDate(dayPrayer)));
            cellList.add(new Cell(index + "-1", UiUtils.formatTiming(Objects.requireNonNull(timings.get(PrayerEnum.FAJR)))));
            cellList.add(new Cell(index + "-2", UiUtils.formatTiming(Objects.requireNonNull(complementaryTiming.get(ComplementaryTimingEnum.SUNRISE)))));
            cellList.add(new Cell(index + "-3", UiUtils.formatTiming(Objects.requireNonNull(timings.get(PrayerEnum.DHOHR)))));
            cellList.add(new Cell(index + "-4", UiUtils.formatTiming(Objects.requireNonNull(timings.get(PrayerEnum.ASR)))));
            cellList.add(new Cell(index + "-5", UiUtils.formatTiming(Objects.requireNonNull(timings.get(PrayerEnum.MAGHRIB)))));
            cellList.add(new Cell(index + "-6", UiUtils.formatTiming(Objects.requireNonNull(timings.get(PrayerEnum.ICHA)))));

            list.add(cellList);
        }

        return list;
    }

    private String getHijriShortDate(DayPrayer dayPrayer) {
        String hijriMonth = getResources().getString(
                getResources().getIdentifier("hijri_month_" + dayPrayer.getHijriMonthNumber(), "string", requireActivity().getPackageName()));

        return UiUtils.formatShortHijriDate(
                dayPrayer.getHijriDay(),
                hijriMonth
        );
    }
}