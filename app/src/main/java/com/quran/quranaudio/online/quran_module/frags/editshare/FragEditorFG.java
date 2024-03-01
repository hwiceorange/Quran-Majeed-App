

package com.quran.quranaudio.online.quran_module.frags.editshare;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import com.quran.quranaudio.online.quran_module.adapters.editor.ADPEditorFG;
import com.quran.quranaudio.online.quran_module.components.editor.VerseEditor;
import com.quran.quranaudio.online.quran_module.utils.extended.GapedItemDecoration;
import com.quran.quranaudio.online.quran_module.utils.extensions.ContextKt;
import com.quran.quranaudio.online.quran_module.utils.extensions.ViewPaddingKt;

public class FragEditorFG extends FragEditorBase {
    private ADPEditorFG mAdapter;
    private RecyclerView mRecyclerView;

    public static FragEditorFG newInstance() {
        return new FragEditorFG();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Context ctx = inflater.getContext();
        RecyclerView view = new RecyclerView(ctx);
        ViewPaddingKt.updatePaddingHorizontal(view, ContextKt.dp2px(ctx, 8));
        ViewPaddingKt.updatePaddingVertical(view, ContextKt.dp2px(ctx, 10));
        view.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        view.setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRecyclerView = (RecyclerView) view;
        if (mAdapter == null && mVerseEditor != null) {
            initializeBGs(mVerseEditor);
        }
    }

    private void initializeBGs(VerseEditor editor) {
        mAdapter = new ADPEditorFG(editor);

        GridLayoutManager lm = new GridLayoutManager(getContext(), 5);
        mRecyclerView.setLayoutManager(lm);
        mRecyclerView.addItemDecoration(new GapedItemDecoration(ContextKt.dp2px(mRecyclerView.getContext(), 3)));
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setItemAnimator(null);
    }

    public int getSelectedItemPos() {
        if (mAdapter == null) {
            return -1;
        }
        return mAdapter.getSelectedItemPos();
    }

    public void select(int index) {
        if (mAdapter == null) {
            return;
        }

        mAdapter.select(index);
    }
}
