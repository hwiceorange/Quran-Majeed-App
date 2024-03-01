package com.quran.quranaudio.online.prayertimes.ui.timingtable;

import android.os.Bundle;

import java.time.LocalDate;


public class CurrentTimingTableFragment extends TimingTableBaseFragment {

    public CurrentTimingTableFragment() {
    }

    public static CurrentTimingTableFragment newInstance() {
        CurrentTimingTableFragment fragment = new CurrentTimingTableFragment();
        Bundle args = new Bundle();

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected void setTableDate() {
        this.tableLocalDate = LocalDate.now();
    }

    @Override
    protected void setScrollAndSelect() {
        mTableView.getSelectionHandler().setSelectedRowPosition(LocalDate.now().getDayOfMonth() - 1);
        mTableView.scrollToRowPosition(LocalDate.now().getDayOfMonth() - 1, 2);
    }
}