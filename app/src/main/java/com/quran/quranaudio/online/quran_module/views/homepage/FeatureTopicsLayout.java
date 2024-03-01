

package com.quran.quranaudio.online.quran_module.views.homepage;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.peacedesign.android.utils.Dimen;
import com.quran.quranaudio.online.quran_module.activities.ActivityTopics;
import com.quran.quranaudio.online.quran_module.adapters.ADPTopics;
import com.quran.quranaudio.online.quran_module.components.quran.QuranMeta;
import com.quran.quranaudio.online.quran_module.components.quran.QuranTopic;
import com.quran.quranaudio.online.R;
import com.quran.quranaudio.online.databinding.LytHomepageTitledItemTitleBinding;
import com.quran.quranaudio.online.quran_module.utils.thread.runner.CallableTaskRunner;
import com.quran.quranaudio.online.quran_module.utils.thread.tasks.BaseCallableTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FeatureTopicsLayout extends HomepageCollectionLayoutBase {
    private final CallableTaskRunner<List<QuranTopic.Topic>> taskRunner = new CallableTaskRunner<>();
    private boolean mFirstTime = true;

    public FeatureTopicsLayout(@NonNull Context context) {
        this(context, null);
    }

    public FeatureTopicsLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FeatureTopicsLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected int getHeaderTitle() {
        return R.string.strTitleFeaturedTopics;
    }

    @Override
    protected int getHeaderIcon() {
        return R.drawable.dr_icon_hash;
    }

    @Override
    protected boolean showViewAllBtn() {
        return true;
    }

    @Override
    protected void setupHeader(Context context, LytHomepageTitledItemTitleBinding header) {
        header.titleIcon.setColorFilter(Color.parseColor("#DA4646"));
    }

    @Override
    protected void onViewAllClick(Context context) {
        context.startActivity(new Intent(context, ActivityTopics.class));
    }

    private void refreshFeatured(Context ctx, QuranTopic quranTopic) {
        taskRunner.callAsync(new BaseCallableTask<List<QuranTopic.Topic>>() {
            @Override
            public List<QuranTopic.Topic> call() {
                List<QuranTopic.Topic> topics = new ArrayList<>();

                for (Map.Entry<Integer, QuranTopic.Topic> entry : quranTopic.getTopics().entrySet()) {
                    QuranTopic.Topic topic = entry.getValue();
                    if (topic.isFeatured) {
                        topics.add(topic);
                    }
                }

                return topics;
            }

            @Override
            public void onComplete(List<QuranTopic.Topic> topics) {
                hideLoader();
                ADPTopics adpTopics = new ADPTopics(ctx, Dimen.dp2px(ctx, 200));
                adpTopics.setTopics(topics);
                resolveListView().setAdapter(adpTopics);
            }
        });
    }

    public void refresh(QuranMeta quranMeta) {
        if (mFirstTime) {
            showLoader();
            mFirstTime = false;
        }

        QuranTopic.prepareInstance(getContext(), quranMeta, quranTopic -> refreshFeatured(getContext(), quranTopic));
    }
}
