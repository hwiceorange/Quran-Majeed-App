package com.quran.quranaudio.online.quran_module.adapters;

import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import com.quran.quranaudio.online.quran_module.activities.ActivityReader;
import com.quran.quranaudio.online.quran_module.activities.readerSettings.Activity_Quran_Settings;
import com.quran.quranaudio.online.quran_module.components.quran.QuranMeta;
import com.quran.quranaudio.online.quran_module.components.quran.subcomponents.Verse;
import com.quran.quranaudio.online.quran_module.components.reader.ReaderRecyclerItemModel;
import com.quran.quranaudio.online.quran_module.components.utility.CardMessageParams;
import com.quran.quranaudio.online.quran_module.reader_managers.ReaderParams;
import com.quran.quranaudio.online.R;
import com.quran.quranaudio.online.databinding.LytReaderIsVotdBinding;
import com.quran.quranaudio.online.quran_module.views.CardMessage;
import com.quran.quranaudio.online.quran_module.views.reader.BismillahView;
import com.quran.quranaudio.online.quran_module.views.reader.ChapterTitleView;
import com.quran.quranaudio.online.quran_module.views.reader.ReaderFooter;
import com.quran.quranaudio.online.quran_module.views.reader.VerseView;

import java.util.ArrayList;

public class ADPReader extends RecyclerView.Adapter<ADPReader.VHReader> {
    private final QuranMeta.ChapterMeta mChapterInfoMeta;
    private final ActivityReader mActivity;
    private final ArrayList<ReaderRecyclerItemModel> mModels;

    public ADPReader(ActivityReader activity, QuranMeta.ChapterMeta chapterInfoMeta, ArrayList<ReaderRecyclerItemModel> models) {
        mActivity = activity;
        mChapterInfoMeta = chapterInfoMeta;
        mModels = models;

        if (chapterInfoMeta != null) {
            models.add(0, new ReaderRecyclerItemModel().setViewType(ReaderParams.RecyclerItemViewType.CHAPTER_INFO));
        }
        models.add(models.size(), new ReaderRecyclerItemModel().setViewType(ReaderParams.RecyclerItemViewType.READER_FOOTER));

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
        return getViewType(position);
    }

    private int getViewType(int position) {
        return mModels.get(position).getViewType();
    }

    public ReaderRecyclerItemModel getItem(int position) {
        return mModels.get(position);
    }

    public void highlightVerseOnScroll(int position) {
        getItem(position).setScrollHighlightPending(true);
        notifyItemChanged(position);
    }

    @NonNull
    @Override
    public VHReader onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final View view;
        if (viewType == ReaderParams.RecyclerItemViewType.IS_VOTD) {
            view = makeIsVotdView(mActivity, parent);
        } else if (viewType == ReaderParams.RecyclerItemViewType.NO_TRANSL_SELECTED) {
            view = prepareNoTranslMessageView(mActivity);
        } else if (viewType == ReaderParams.RecyclerItemViewType.BISMILLAH) {
            view = new BismillahView(mActivity);
        } else if (viewType == ReaderParams.RecyclerItemViewType.VERSE) {
            view = new VerseView(mActivity, parent, null, false);
        } else if (viewType == ReaderParams.RecyclerItemViewType.CHAPTER_TITLE) {
            view = new ChapterTitleView(mActivity);
        } else if (viewType == ReaderParams.RecyclerItemViewType.READER_FOOTER) {
            final ReaderFooter footer = mActivity.mNavigator.readerFooter;
            footer.clearParent();
            view = footer;
        } else {
            view = new View(mActivity);
        }

        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params == null) {
            params = new ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        } else {
            params.width = MATCH_PARENT;
        }
        view.setLayoutParams(params);

        return new VHReader(view);
    }

    private View makeIsVotdView(ActivityReader activity, ViewGroup parent) {
        LytReaderIsVotdBinding binding = LytReaderIsVotdBinding.inflate(activity.getLayoutInflater(), parent, false);
        return binding.getRoot();
    }

    private View prepareNoTranslMessageView(ActivityReader activity) {
        CardMessage msgView = new CardMessage(activity);
        msgView.setMessage(activity.str(R.string.strMsgTranslNoneSelected));
        msgView.setElevation(activity.dp2px(4));
        msgView.setMessageStyle(CardMessageParams.STYLE_WARNING);
        msgView.setActionText(activity.str(R.string.strTitleSettings),
            () -> activity.mBinding.readerHeader.openReaderSetting(Activity_Quran_Settings.SETTINGS_TRANSLATION));
        return msgView;
    }

    @Override
    public void onBindViewHolder(@NonNull VHReader holder, int position) {
        final ReaderRecyclerItemModel model = mModels.get(position);
        holder.bind(model);
    }

    public class VHReader extends RecyclerView.ViewHolder {
        public VHReader(@NonNull View itemView) {
            super(itemView);
        }

        public void bind(ReaderRecyclerItemModel model) {
            final int position = getAdapterPosition();

            int viewType = getViewType(position);

            switch (viewType) {

                case ReaderParams.RecyclerItemViewType.VERSE: setupVerseView(model);
                    break;
                case ReaderParams.RecyclerItemViewType.CHAPTER_TITLE: setupTitleView(model);
                    break;
            }
        }

        private void setupVerseView(ReaderRecyclerItemModel model) {
            if (!(itemView instanceof VerseView)) {
                return;
            }

            final Verse verse = model.getVerse();

            VerseView verseView = (VerseView) itemView;
            verseView.setVerse(verse);

            if (model.isScrollHighlightPending()) {
                verseView.highlightOnScroll();
                model.setScrollHighlightPending(false);
            }

            if (mActivity.mPlayer != null) {
                verseView.onRecite(mActivity.mPlayer.isReciting(verse.chapterNo, verse.verseNo));
            }
        }

        private void setupTitleView(ReaderRecyclerItemModel model) {
            if (!(itemView instanceof ChapterTitleView)) {
                return;
            }

            ChapterTitleView chapterTitleView = (ChapterTitleView) itemView;
            chapterTitleView.setChapterNumber(model.getChapterNo());
        }
    }
}
