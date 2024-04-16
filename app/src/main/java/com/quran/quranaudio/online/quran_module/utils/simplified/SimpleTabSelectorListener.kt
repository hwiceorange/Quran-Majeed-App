package com.quran.quranaudio.online.quran_module.utils.simplified

import com.google.android.material.tabs.TabLayout

abstract class SimpleTabSelectorListener : TabLayout.OnTabSelectedListener {
    override fun onTabSelected(tab: TabLayout.Tab) {}
    override fun onTabUnselected(tab: TabLayout.Tab) {}
    override fun onTabReselected(tab: TabLayout.Tab) {}
}
