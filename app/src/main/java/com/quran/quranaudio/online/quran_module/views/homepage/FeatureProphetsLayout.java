/**
 * Author: Rai Adnan
 * Whatsapp: +923002375907
 * Email: officialshaheendevelopers@gmail.com
 * Portfolio: https://codecanyon.net/user/shaheendevelopers/portfolio
 */

package com.quran.quranaudio.online.quran_module.views.homepage;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.peacedesign.android.utils.Dimen;
import com.quran.quranaudio.online.quran_module.activities.ActivityProphets;
import com.quran.quranaudio.online.quran_module.adapters.ADPProphets;
import com.quran.quranaudio.online.quran_module.components.quran.QuranMeta;
import com.quran.quranaudio.online.quran_module.components.quran.QuranProphet;
import com.quran.quranaudio.online.R;
import com.quran.quranaudio.online.quran_module.components.quran.QuranProphet;

import kotlin.Unit;

public class FeatureProphetsLayout extends HomepageCollectionLayoutBase {
    public FeatureProphetsLayout(@NonNull Context context) {
        this(context, null);
    }

    public FeatureProphetsLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FeatureProphetsLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected int getHeaderTitle() {
        return R.string.strTitleFeaturedProphets;
    }

    @Override
    protected int getHeaderIcon() {
        return R.drawable.prophets;
    }

    @Override
    protected boolean showViewAllBtn() {
        return true;
    }

    @Override
    protected void onViewAllClick(Context context) {
        context.startActivity(new Intent(context, ActivityProphets.class));
    }

    private void refreshFeatured(Context ctx, QuranMeta quranMeta) {
        showLoader();
        QuranProphet.Companion.prepareInstance(getContext(), quranMeta, quranProphet -> {
            hideLoader();
            ADPProphets prophetsAdapter = new ADPProphets(ctx, Dimen.dp2px(ctx, 300), 10);
            prophetsAdapter.setProphets(quranProphet.getProphets());
            resolveListView().setAdapter(prophetsAdapter);

            return Unit.INSTANCE;
        });
    }

    public void refresh(QuranMeta quranMeta) {
        refreshFeatured(getContext(), quranMeta);
    }
}
