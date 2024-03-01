package com.quran.quranaudio.online.quran_module.adapters;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import com.peacedesign.android.utils.Dimen;
import com.quran.quranaudio.online.quran_module.activities.ActivityReader;
import com.quran.quranaudio.online.quran_module.components.quran.QuranMeta;
import com.quran.quranaudio.online.quran_module.components.reader.QuranPageModel;
import com.quran.quranaudio.online.quran_module.components.reader.QuranPageSectionModel;
import com.quran.quranaudio.online.quran_module.reader_managers.ReaderParams;
import com.quran.quranaudio.online.quran_module.utils.extensions.LayoutParamsKt;
import com.quran.quranaudio.online.quran_module.views.reader.QuranPageView;
import com.quran.quranaudio.online.quran_module.views.reader.ReaderFooter;

import java.util.ArrayList;

public class ADPQuranPages extends RecyclerView.Adapter<ADPQuranPages.VHQuranPage> {
    private final QuranMeta.ChapterMeta mChapterInfoMeta;
    private final ArrayList<QuranPageModel> mModels;
    private final ActivityReader mActivity;

    public ADPQuranPages(ActivityReader activity, QuranMeta.ChapterMeta chapterInfoMeta, ArrayList<QuranPageModel> models) {
        mActivity = activity;
        mChapterInfoMeta = chapterInfoMeta;
        mModels = models;

        if (chapterInfoMeta != null) {
            models.add(0, new QuranPageModel().setViewType(ReaderParams.RecyclerItemViewType.CHAPTER_INFO));
        }
        models.add(models.size(), new QuranPageModel().setViewType(ReaderParams.RecyclerItemViewType.READER_FOOTER));

        for (int i = 0; i < models.size(); i++) {
            QuranPageModel model = models.get(i);
            if (model.getViewType() == ReaderParams.RecyclerItemViewType.READER_PAGE) {
                for (QuranPageSectionModel section : model.getSections()) {
                    section.parentIndexInAdapter = i;
                }
            }
        }

        setHasStableIds(true);
    }

    @Override
    public int getItemCount() {
        return mModels.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    private int getViewType(int position) {
        return mModels.get(position).getViewType();
    }

    public void highlightVerseOnScroll(int position, int chapterNo, int verseNo) {
        QuranPageModel pageModel = getPageModel(position);
        pageModel.setScrollHighlightPendingChapterNo(chapterNo);
        pageModel.setScrollHighlightPendingVerseNo(verseNo);
        notifyItemChanged(position);
    }

    @NonNull
    @Override
    public VHQuranPage onCreateViewHolder(@NonNull ViewGroup parent, int position) {
        int viewType = getViewType(position);
        View view;

        if (viewType == ReaderParams.RecyclerItemViewType.READER_PAGE) {
            final QuranPageView quranPageView = new QuranPageView(mActivity);
            final ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(MATCH_PARENT, WRAP_CONTENT);
            LayoutParamsKt.updateMargins(params, Dimen.dp2px(parent.getContext(), 3));
            quranPageView.setLayoutParams(params);
            view = quranPageView;
        } else if (viewType == ReaderParams.RecyclerItemViewType.READER_FOOTER) {
            final ReaderFooter footer = mActivity.mNavigator.readerFooter;
            footer.clearParent();
            view = footer;
        } else {
            view = new View(parent.getContext());
        }

        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params == null) {
            params = new ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        } else {
            params.width = MATCH_PARENT;
        }
        view.setLayoutParams(params);

        return new VHQuranPage(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VHQuranPage holder, int position) {
        holder.bind(mModels.get(position));
    }

    public QuranPageModel getPageModel(int position) {
        return mModels.get(position);
    }

    class VHQuranPage extends RecyclerView.ViewHolder {

        public VHQuranPage(@NonNull View itemView) {
            super(itemView);
        }

        public void bind(QuranPageModel pageModel) {
            final int position = getAdapterPosition();

            int viewType = getViewType(position);

            if (viewType == ReaderParams.RecyclerItemViewType.READER_PAGE) {
                ((QuranPageView) itemView).setPageModel(pageModel);
            }
        }
    }
}
