package com.bible.tools.base;

import androidx.fragment.app.Fragment;

import com.bible.tools.extension.ReportExtensionKt;

abstract public class BaseFragment extends Fragment {
    private boolean isFirstInto = true;
    private boolean isVisibleFragment = false;
    private boolean isHideChangeShow = false;

    protected abstract String getPageName();
    protected abstract String getFromPageName();

    protected void fragmentVisibleListener(boolean visible) {

    }

    @Override
    public void onResume() {
        super.onResume();
        if (isFirstInto) {
            isHideChangeShow = true;
            isVisibleFragment = true;
            isFirstInto = false;
        } else {
            isVisibleFragment = isHideChangeShow;
        }
        if (isVisibleFragment) {
            reportFragmentVisible();
            fragmentVisibleListener(isVisibleFragment);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (isVisibleFragment) {
            isVisibleFragment = false;
            fragmentVisibleListener(false);
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        isHideChangeShow = !hidden;
        isVisibleFragment = isHideChangeShow;
        fragmentVisibleListener(isVisibleFragment);
        if (isVisibleFragment) {
            reportFragmentVisible();
        }
    }

    private void reportFragmentVisible() {
        ReportExtensionKt.reportEnterFunEvent(getPageName(), getFromPageName());

    }
}
