package com.quran.quranaudio.online.prayertimes.ui.timingtable;

import android.os.Bundle;

import java.time.LocalDate;

/**
 * Author: Rai Adnan
 * Whatsapp: +923002375907
 * Email: officialshaheendevelopers@gmail.com
 * Portfolio: https://codecanyon.net/user/shaheendevelopers/portfolio
 */
public class NextTimingTableFragment extends TimingTableBaseFragment {

    public NextTimingTableFragment() {
    }

    public static NextTimingTableFragment newInstance() {
        NextTimingTableFragment fragment = new NextTimingTableFragment();
        Bundle args = new Bundle();

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected void setTableDate() {
        this.tableLocalDate = LocalDate.now().plusMonths(1);
    }

    @Override
    protected void setScrollAndSelect() {
    }
}