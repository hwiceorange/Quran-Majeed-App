/**
 * Author: Rai Adnan
 * Whatsapp: +923002375907
 * Email: officialshaheendevelopers@gmail.com
 * Portfolio: https://codecanyon.net/user/shaheendevelopers/portfolio
 */

package com.quran.quranaudio.online.quran_module.views.reader.swipe;

import android.content.Context;
import android.widget.TextView;

import com.peacedesign.android.utils.Dimen;
import com.quran.quranaudio.online.quran_module.reader_managers.ReaderParams;
import com.quran.quranaudio.online.R;

import me.dkzwm.widget.srl.extra.IRefreshView;

public class ViewSwipeToPrevious extends BaseViewSwipeTo {
    public ViewSwipeToPrevious(Context context) {
        super(context);
    }

    @Override
    protected TextView createTitleView(Context ctx) {
        TextView titleView = super.createTitleView(ctx);
        LayoutParams p = (LayoutParams) titleView.getLayoutParams();
        p.bottomMargin = Dimen.dp2px(ctx, 5);
        titleView.setLayoutParams(p);
        return titleView;
    }

    @Override
    protected String getTitle(int readType) {
        switch (readType) {
            case ReaderParams.READER_READ_TYPE_CHAPTER:
                return getContext().getString(R.string.strLabelPreviousChapter);
            case ReaderParams.READER_READ_TYPE_JUZ:
                return getContext().getString(R.string.strLabelPreviousJuz);
            default:
                return null;
        }
    }

    @Override
    protected String getNoFurtherTitle(int readType) {
        switch (readType) {
            case ReaderParams.READER_READ_TYPE_CHAPTER:
                return getContext().getString(R.string.strLabelNoPreviousChapter);
            case ReaderParams.READER_READ_TYPE_JUZ:
                return getContext().getString(R.string.strLabelNoPreviousJuz);
            default:
                return null;
        }
    }

    @Override
    protected int getArrowResource() {
        return R.drawable.ic_back;
    }

    @Override
    protected int getTitleIndex() {
        return 0;
    }

    @Override
    protected int getArrowIndex() {
        return 1;
    }

    @Override
    protected float getRotationBeforeReach() {
        return -90;
    }

    @Override
    public int getType() {
        return IRefreshView.TYPE_HEADER;
    }
}
