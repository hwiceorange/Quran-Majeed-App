

package com.quran.quranaudio.online.quran_module.activities;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.quran.quranaudio.online.quran_module.activities.base.BaseActivity;
import com.quran.quranaudio.online.R;
import com.quran.quranaudio.online.databinding.ActivityDownloadsBinding;
import com.quran.quranaudio.online.quran_module.views.BoldHeader;

public class ActivityDownloads extends BaseActivity {
    private ActivityDownloadsBinding mBinding;

    @Override
    protected boolean shouldInflateAsynchronously() {
        return true;
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_downloads;
    }

    @Override
    protected void onActivityInflated(@NonNull View activityView, @Nullable Bundle savedInstanceState) {
        mBinding = ActivityDownloadsBinding.bind(activityView);
        initHeader(mBinding.header);
    }

    private void initHeader(BoldHeader header) {
        header.setTitleText("Downloads");
        header.setCallback(this::finish);
        header.setBackgroundColor(color(R.color.colorBGPage));
    }
}
