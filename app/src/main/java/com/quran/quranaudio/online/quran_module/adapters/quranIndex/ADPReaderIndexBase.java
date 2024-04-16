/**
 * Author: Rai Adnan
 * Whatsapp: +923002375907
 * Email: officialshaheendevelopers@gmail.com
 * Portfolio: https://codecanyon.net/user/shaheendevelopers/portfolio
 */

package com.quran.quranaudio.online.quran_module.adapters.quranIndex;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;

import com.quran.quranaudio.online.quran_module.frags.readerindex.BaseFragReaderIndex;

public abstract class ADPReaderIndexBase<VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {
    protected final BaseFragReaderIndex mFragment;
    private final boolean mReversed;

    protected ADPReaderIndexBase(BaseFragReaderIndex fragment, boolean reverse) {
        mFragment = fragment;
        mReversed = reverse;
    }

    protected void initADP(Context ctx) {
        prepareList(ctx, mReversed);
        setHasStableIds(true);
    }

    protected abstract void prepareList(Context ctx, boolean reverse);

    @Override
    public long getItemId(int position) {
        return position;
    }
}
