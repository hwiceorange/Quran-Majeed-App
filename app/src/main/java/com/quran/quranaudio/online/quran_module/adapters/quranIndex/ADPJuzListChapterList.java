

package com.quran.quranaudio.online.quran_module.adapters.quranIndex;

import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.collect.ImmutableList;
import com.quran.quranaudio.online.quran_module.components.quran.QuranMeta;
import com.quran.quranaudio.online.R;
import com.quran.quranaudio.online.quran_module.frags.readerindex.BaseFragReaderIndex;
import com.quran.quranaudio.online.quran_module.utils.reader.factory.ReaderFactory;
import com.quran.quranaudio.online.quran_module.widgets.chapterCard.ChapterCardJuz;

import kotlin.Pair;

public class ADPJuzListChapterList extends RecyclerView.Adapter<ADPJuzListChapterList.VHJuzChapter> {
    private final BaseFragReaderIndex mFragment;
    private final ImmutableList<QuranMeta.ChapterMeta> mChapterMetas;
    private final int mJuzNo;
    private final String mVersesStr;

    public ADPJuzListChapterList(BaseFragReaderIndex fragment, ImmutableList<QuranMeta.ChapterMeta> mChapterMetas, int juzNo) {
        mFragment = fragment;
        this.mChapterMetas = mChapterMetas;
        mJuzNo = juzNo;
        mVersesStr = fragment.getString(R.string.strLabelVersesText);
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return mChapterMetas.size();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @NonNull
    @Override
    public VHJuzChapter onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VHJuzChapter(new ChapterCardJuz(parent.getContext()));
    }

    @Override
    public void onBindViewHolder(@NonNull VHJuzChapter holder, int position) {
        holder.bind(mChapterMetas.get(position));
    }

    class VHJuzChapter extends RecyclerView.ViewHolder {
        public VHJuzChapter(@NonNull ChapterCardJuz chapterCard) {
            super(chapterCard);
            chapterCard.setBackgroundResource(R.drawable.dr_bg_hover);
        }

        public void bind(QuranMeta.ChapterMeta chapterMeta) {
            if (!(itemView instanceof ChapterCardJuz)) {
                return;
            }

            ChapterCardJuz chapterCard = (ChapterCardJuz) itemView;
            chapterCard.setChapterNumber(chapterMeta.chapterNo);
            chapterCard.setName(chapterMeta.getName(), chapterMeta.getNameTranslation());

            Pair<Integer, Integer> versesInJuz = mFragment.getQuranMeta().getVerseRangeOfChapterInJuz(mJuzNo,
                chapterMeta.chapterNo);
            if (versesInJuz != null) {
                chapterCard.setVersesCount(mVersesStr, versesInJuz.getFirst(), versesInJuz.getSecond());
                chapterCard.setOnClickListener(
                    v -> ReaderFactory.startVerseRange(v.getContext(), chapterMeta.chapterNo, versesInJuz));
            }
        }
    }
}
