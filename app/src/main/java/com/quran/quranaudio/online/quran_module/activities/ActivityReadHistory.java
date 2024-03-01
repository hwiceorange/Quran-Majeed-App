package com.quran.quranaudio.online.quran_module.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import com.peacedesign.android.utils.WindowUtils;
import com.quran.quranaudio.online.quran_module.activities.base.BaseActivity;
import com.quran.quranaudio.online.quran_module.components.quran.QuranMeta;
import com.quran.quranaudio.online.quran_module.components.readHistory.ReadHistoryModel;
import com.quran.quranaudio.online.quran_module.interfaceUtils.ReadHistoryCallbacks;
import com.quran.quranaudio.online.R;
import com.quran.quranaudio.online.databinding.ActivityQuranBookmarkBinding;
import com.quran.quranaudio.online.quran_module.adapters.ADPReadHistory;
import com.quran.quranaudio.online.quran_module.components.readHistory.ReadHistoryModel;
import com.quran.quranaudio.online.quran_module.db.readHistory.ReadHistoryDBHelper;
import com.quran.quranaudio.online.quran_module.utils.Logger;
import com.quran.quranaudio.online.quran_module.utils.reader.factory.ReaderFactory;
import com.quran.quranaudio.online.quran_module.utils.thread.runner.CallableTaskRunner;
import com.quran.quranaudio.online.quran_module.utils.thread.tasks.BaseCallableTask;
import com.quran.quranaudio.online.quran_module.views.BoldHeader;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class ActivityReadHistory extends BaseActivity implements ReadHistoryCallbacks {
    public final AtomicReference<QuranMeta> quranMetaRef = new AtomicReference<>();
    private final CallableTaskRunner<List<ReadHistoryModel>> taskRunner = new CallableTaskRunner<>();
    private ActivityQuranBookmarkBinding mBinding;
    private ReadHistoryDBHelper mReadHistoryDBHelper;
    private ADPReadHistory mAdapter;

    @Override
    protected void onDestroy() {
        if (mReadHistoryDBHelper != null) {
            mReadHistoryDBHelper.close();
        }
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mReadHistoryDBHelper != null && mAdapter != null) {
            refreshHistories();
        }
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_quran_bookmark;
    }

    @Override
    protected void preActivityInflate(@Nullable Bundle savedInstanceState) {
        super.preActivityInflate(savedInstanceState);
        mReadHistoryDBHelper = new ReadHistoryDBHelper(this);
    }

    @Override
    protected boolean shouldInflateAsynchronously() {
        return true;
    }

    @Override
    protected void onActivityInflated(@NonNull View activityView, @Nullable Bundle savedInstanceState) {
        mBinding = ActivityQuranBookmarkBinding.bind(activityView);

        initHeader(mBinding.header);

        QuranMeta.prepareInstance(this, quranMeta -> {
            quranMetaRef.set(quranMeta);
            init();
        });
    }

    private void init() {
        mBinding.noItemsIcon.setImageResource(R.drawable.dr_icon_history);
        mBinding.noItemsText.setText(R.string.strMsgReadHistoryNoItems);
        mBinding.loader.setVisibility(View.VISIBLE);
        refreshHistories();
    }

    private void initHeader(BoldHeader header) {
        header.setCallback(this::onBackPressed);
        header.setTitleText(R.string.strTitleReadHistory);

        header.setLeftIconRes(R.drawable.dr_icon_arrow_left);
        header.setShowRightIcon(false);

        header.setBGColor(R.color.colorBGPage);
    }

    private void refreshHistories() {
        taskRunner.callAsync(new ReadHistoryFetcher());
    }

    private void setupAdapter(List<ReadHistoryModel> models) {
        if (mAdapter == null && models.size() > 0) {
            Context context = this;

            int spanCount = WindowUtils.isLandscapeMode(context) ? 2 : 1;
            GridLayoutManager layoutManager = new GridLayoutManager(context, spanCount);
            mBinding.list.setLayoutManager(layoutManager);

            mAdapter = new ADPReadHistory(this, quranMetaRef.get(), models, MATCH_PARENT);
            mBinding.list.setAdapter(mAdapter);
        } else if (mAdapter != null) {
            mAdapter.updateModels(models);
        }

        mBinding.noItems.setVisibility(models.size() == 0 ? View.VISIBLE : View.GONE);
    }

    public void onOpen(ReadHistoryModel model) {
        Intent intent = ReaderFactory.prepareVerseRangeIntent(model.getChapterNo(), model.getFromVerseNo(),
            model.getToVerseNo());
        intent.setClass(this, ActivityReader.class);
        startActivity(intent);
    }

    @Override
    public void onReadHistoryRemoved(ReadHistoryModel model) {
    }

    @Override
    public void onReadHistoryAdded(ReadHistoryModel model) {

    }

    private class ReadHistoryFetcher extends BaseCallableTask<List<ReadHistoryModel>> {
        @Override
        public List<ReadHistoryModel> call() {
            return mReadHistoryDBHelper.getAllHistories(-1);
        }

        @Override
        public void onComplete(@NonNull List<ReadHistoryModel> models) {
            setupAdapter(models);
        }

        @Override
        public void onFailed(@NonNull Exception e) {
            e.printStackTrace();
            Logger.reportError(e);
        }

        @Override
        public void postExecute() {
            mBinding.list.post(() -> mBinding.loader.setVisibility(View.GONE));
        }
    }
}