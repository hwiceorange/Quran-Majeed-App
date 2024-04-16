package com.quran.quranaudio.online.prayertimes.ui.timingtable;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.Objects;

/**
 * Author: Rai Adnan
 * Whatsapp: +923002375907
 * Email: officialshaheendevelopers@gmail.com
 * Portfolio: https://codecanyon.net/user/shaheendevelopers/portfolio
 */
public class TimingTablePagerAdapter extends FragmentStateAdapter {

    public TimingTablePagerAdapter(@NonNull FragmentManager fragmentManager,
                                   @NonNull Lifecycle lifecycle) {
        super(fragmentManager, lifecycle);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Fragment fragment = null;
        switch (position) {
            case 0:
                fragment = CurrentTimingTableFragment.newInstance();
                break;
            case 1:
                fragment = NextTimingTableFragment.newInstance();
                break;
        }
        return Objects.requireNonNull(fragment);
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
