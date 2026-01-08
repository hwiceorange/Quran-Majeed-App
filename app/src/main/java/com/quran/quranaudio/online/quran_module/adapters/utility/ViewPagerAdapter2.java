package com.quran.quranaudio.online.quran_module.adapters.utility;

import android.os.Bundle;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewPager2 Adapter with proper state management
 * 
 * 🔥 修复崩溃：FragmentStateAdapter state restoration conflict
 * - 不再直接保存 Fragment 实例
 * - 使用 Fragment class 信息重新创建 Fragment
 * - 正确实现 getItemId 和 containsItem
 */
public class ViewPagerAdapter2 extends FragmentStateAdapter {
    // 🔥 保存 Fragment 的类信息和参数，而不是实例
    private final List<FragmentInfo> fragmentInfos = new ArrayList<>();
    private final List<String> fragmentTitles = new ArrayList<>();
    
    // 🔥 缓存已创建的 Fragment 实例（仅用于 getFragment 方法）
    private final List<Fragment> cachedFragments = new ArrayList<>();

    public ViewPagerAdapter2(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    /**
     * 添加 Fragment
     * 🔥 修复：不再直接保存 Fragment 实例，而是保存创建信息
     */
    public void addFragment(@NonNull Fragment fragment, @Nullable String title) {
        fragmentInfos.add(new FragmentInfo(fragment));
        fragmentTitles.add(title);
        cachedFragments.add(null); // 占位，实际实例由 FragmentStateAdapter 管理
    }

    public List<Fragment> getFragments() {
        return cachedFragments;
    }

    public Fragment getFragment(int index) {
        return cachedFragments.get(index);
    }

    public Fragment getFragmentSafely(int index) {
        try {
            return cachedFragments.get(index);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @NonNull
    public CharSequence getPageTitle(int position) {
        return fragmentTitles.get(position);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // 🔥 修复：每次都创建新的 Fragment 实例
        Fragment fragment = fragmentInfos.get(position).createFragment();
        
        // 缓存实例供外部访问
        if (position < cachedFragments.size()) {
            cachedFragments.set(position, fragment);
        }
        
        return fragment;
    }

    @Override
    public int getItemCount() {
        return fragmentInfos.size();
    }
    
    /**
     * 🔥 修复崩溃：提供稳定的 itemId
     * 这样 FragmentStateAdapter 就能正确跟踪和恢复 Fragment
     */
    @Override
    public long getItemId(int position) {
        return fragmentInfos.get(position).hashCode();
    }
    
    /**
     * 🔥 修复崩溃：正确判断 itemId 是否存在
     */
    @Override
    public boolean containsItem(long itemId) {
        for (FragmentInfo info : fragmentInfos) {
            if (info.hashCode() == itemId) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Fragment 创建信息
     * 保存 Fragment 的类和参数，用于重新创建
     */
    private static class FragmentInfo {
        private final Class<? extends Fragment> fragmentClass;
        private final Bundle arguments;
        private final int stableId;
        
        FragmentInfo(@NonNull Fragment fragment) {
            this.fragmentClass = fragment.getClass();
            this.arguments = fragment.getArguments() != null 
                    ? new Bundle(fragment.getArguments()) 
                    : null;
            // 🔥 使用 class + arguments 生成稳定 ID
            this.stableId = generateStableId(fragmentClass, arguments);
        }
        
        @NonNull
        Fragment createFragment() {
            try {
                Fragment fragment = fragmentClass.newInstance();
                if (arguments != null) {
                    fragment.setArguments(new Bundle(arguments));
                }
                return fragment;
            } catch (IllegalAccessException | InstantiationException e) {
                throw new RuntimeException("Failed to create fragment: " + fragmentClass.getName(), e);
            }
        }
        
        private static int generateStableId(Class<? extends Fragment> clazz, Bundle args) {
            int result = clazz.getName().hashCode();
            if (args != null && !args.isEmpty()) {
                // 包含参数信息生成更唯一的 ID
                result = 31 * result + args.toString().hashCode();
            }
            return result;
        }
        
        @Override
        public int hashCode() {
            return stableId;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof FragmentInfo)) return false;
            FragmentInfo other = (FragmentInfo) obj;
            return stableId == other.stableId;
        }
    }
}