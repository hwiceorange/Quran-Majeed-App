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
 * 
 * 🔥 Updated: Added getItemId and containsItem for proper state management
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
    
    /**
     * 🔥 修复崩溃：提供稳定的 itemId，防止状态恢复错误
     */
    @Override
    public long getItemId(int position) {
        // 使用 position 作为稳定 ID（因为 Fragment 类型固定）
        return position;
    }
    
    /**
     * 🔥 修复崩溃：正确判断 itemId 是否存在
     */
    @Override
    public boolean containsItem(long itemId) {
        return itemId >= 0 && itemId < 2;
    }
}
