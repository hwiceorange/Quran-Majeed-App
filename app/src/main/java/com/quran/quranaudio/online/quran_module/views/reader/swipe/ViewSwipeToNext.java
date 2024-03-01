
package com.quran.quranaudio.online.quran_module.views.reader.swipe;

import android.content.Context;
import android.widget.TextView;

import com.peacedesign.android.utils.Dimen;
import com.quran.quranaudio.online.quran_module.reader_managers.ReaderParams;
import com.quran.quranaudio.online.R;

import me.dkzwm.widget.srl.SmoothRefreshLayout;
import me.dkzwm.widget.srl.extra.IRefreshView;

public class ViewSwipeToNext extends BaseViewSwipeTo {
    public ViewSwipeToNext(Context context) {
        super(context);
    }

    @Override
    protected TextView createTitleView(Context ctx) {
        TextView titleView = super.createTitleView(ctx);
        LayoutParams p = (LayoutParams) titleView.getLayoutParams();
        p.topMargin = Dimen.dp2px(ctx, 5);
        titleView.setLayoutParams(p);
        return titleView;
    }

    @Override
    protected String getTitle(int readType) {
        switch (readType) {
            case ReaderParams.READER_READ_TYPE_CHAPTER:
                return getContext().getString(R.string.strLabelNextChapter);
            case ReaderParams.READER_READ_TYPE_JUZ:
                return getContext().getString(R.string.strLabelNextJuz);
            default:
                return null;
        }
    }

    @Override
    protected String getNoFurtherTitle(int readType) {
        switch (readType) {
            case ReaderParams.READER_READ_TYPE_CHAPTER:
                return getContext().getString(R.string.strLabelNoNextChapter);
            case ReaderParams.READER_READ_TYPE_JUZ:
                return getContext().getString(R.string.strLabelNoNextJuz);
            default:
                return null;
        }
    }

    @Override
    public void onRefreshComplete(SmoothRefreshLayout layout, boolean isSuccessful) {
        setVisibility(GONE);

        if (mSwipeDisabled) {
            return;
        }

        mJumpAnimation.cancel();
        mArrow.clearAnimation();
        relayout();
    }

    @Override
    protected int getArrowResource() {
        return R.drawable.dr_icon_chevron_right;
    }

    @Override
    protected int getTitleIndex() {
        return 1;
    }

    @Override
    protected int getArrowIndex() {
        return 0;
    }

    @Override
    protected float getRotationBeforeReach() {
        return -90;
    }

    @Override
    public int getType() {
        return IRefreshView.TYPE_FOOTER;
    }
}
