package com.quran.quranaudio.online.prayertimes.ui.timingtable.di;

import com.quran.quranaudio.online.prayertimes.ui.timingtable.TimingTableBaseFragment;

import dagger.Subcomponent;


@Subcomponent(modules = {TimingTableModule.class})
public interface TimingTableComponent {

    @Subcomponent.Factory
    interface Factory {
        TimingTableComponent create();
    }

    void inject(TimingTableBaseFragment timingTableBaseFragment);
}
