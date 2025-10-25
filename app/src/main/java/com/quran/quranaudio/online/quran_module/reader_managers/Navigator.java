package com.quran.quranaudio.online.quran_module.reader_managers;

import android.view.View;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.quran.quranaudio.online.R;
import com.quran.quranaudio.online.quran_module.activities.ActivityReader;
import com.quran.quranaudio.online.quran_module.adapters.ADPQuranPages;
import com.quran.quranaudio.online.quran_module.adapters.ADPReader;
import com.quran.quranaudio.online.quran_module.components.quran.QuranMeta;
import com.quran.quranaudio.online.quran_module.components.quran.subcomponents.Verse;
import com.quran.quranaudio.online.quran_module.components.reader.QuranPageModel;
import com.quran.quranaudio.online.quran_module.components.reader.QuranPageSectionModel;
import com.quran.quranaudio.online.quran_module.components.reader.ReaderRecyclerItemModel;
import com.quran.quranaudio.online.databinding.ActivityReaderBinding;
import com.quran.quranaudio.online.quran_module.utils.quran.QuranUtils;
import com.quran.quranaudio.online.quran_module.utils.span.VerseArabicHighlightSpan;
import com.quran.quranaudio.online.quran_module.views.reader.QuranPageView;
import com.quran.quranaudio.online.quran_module.views.reader.QuranPageView.QuranPageSectionView;
import com.quran.quranaudio.online.quran_module.views.reader.ReaderFooter;
import com.quran.quranaudio.online.quran_module.views.reader.swipe.ViewSwipeToNext;
import com.quran.quranaudio.online.quran_module.views.reader.swipe.ViewSwipeToPrevious;

import kotlin.Pair;
import me.dkzwm.widget.srl.SmoothRefreshLayout;

public class Navigator {
    private final ReaderParams mReaderParams;
    private final ActivityReader mActivity;
    public int[] pendingScrollVerse = {-1, -1};
    public boolean pendingScrollVerseHighlight = true;
    public ReaderFooter readerFooter;
    private ViewSwipeToPrevious mViewSwipeToPrevious;
    private ViewSwipeToNext mViewSwipeToNext;
    private boolean mSwipeDisabled;

    public Navigator(ActivityReader activityReader) {
        mActivity = activityReader;
        mReaderParams = activityReader.mReaderParams;
        init();
    }

    private void init() {
        readerFooter = new ReaderFooter(mActivity, this);
        initSwipes(mActivity.mBinding.swipeLayout);
    }

    private void initSwipes(SmoothRefreshLayout swipeLayout) {
        swipeLayout.setDisableLoadMoreWhenContentNotFull(true);
        swipeLayout.setOnRefreshListener(new SmoothRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefreshing() {
                if (mSwipeDisabled) {
                    swipeLayout.refreshComplete();
                    return;
                }

                switch (mReaderParams.readType) {
                    case ReaderParams.READER_READ_TYPE_CHAPTER:
                        if (QuranMeta.isChapterValid(getPrevChapterNo())) {
                            previousChapter();
                        }
                        break;
                    case ReaderParams.READER_READ_TYPE_JUZ:
                        if (QuranMeta.isJuzValid(getPrevJuzNo())) {
                            previousJuz();
                        }
                        break;
                    case ReaderParams.READER_READ_TYPE_VERSES:
                    default:
                        break;
                }

                swipeLayout.refreshComplete(200);
            }

            @Override
            public void onLoadingMore() {
                if (mSwipeDisabled) {
                    swipeLayout.refreshComplete();
                    return;
                }

                switch (mReaderParams.readType) {
                    case ReaderParams.READER_READ_TYPE_CHAPTER:
                        if (QuranMeta.isChapterValid(getNextChapterNo())) {
                            nextChapter();
                        }
                        break;
                    case ReaderParams.READER_READ_TYPE_JUZ:
                        if (QuranMeta.isJuzValid(getNextJuzNo())) {
                            nextJuz();
                        }
                        break;
                    case ReaderParams.READER_READ_TYPE_VERSES:
                    default:
                        break;
                }

                swipeLayout.refreshComplete();
            }
        });

        mViewSwipeToPrevious = new ViewSwipeToPrevious(mActivity);
        swipeLayout.setHeaderView(mViewSwipeToPrevious);

        mViewSwipeToNext = new ViewSwipeToNext(mActivity);
        swipeLayout.setFooterView(mViewSwipeToNext);
    }

    private void setupSwipe() {
        switch (mReaderParams.readType) {
            case ReaderParams.READER_READ_TYPE_CHAPTER:
                setupForChapter();
                break;
            case ReaderParams.READER_READ_TYPE_JUZ:
                setupFooterForJuz();
                break;
            case ReaderParams.READER_READ_TYPE_VERSES:
            default:
                setSwipeDisabled(true);
                break;
        }
    }

    private void setupForChapter() {
        setSwipeDisabled(false);

        if (QuranMeta.isChapterValid(getPrevChapterNo())) {
            QuranMeta quranMeta = mActivity.mQuranMetaRef.get();
            String chapterName = quranMeta.getChapterName(mActivity, getPrevChapterNo(), true);
            mViewSwipeToPrevious.setName(ReaderParams.READER_READ_TYPE_CHAPTER, chapterName);
        } else {
            mViewSwipeToPrevious.setNoFurther(ReaderParams.READER_READ_TYPE_CHAPTER);
        }

        if (QuranMeta.isChapterValid(getNextChapterNo())) {
            QuranMeta quranMeta = mActivity.mQuranMetaRef.get();
            String chapterName = quranMeta.getChapterName(mActivity, getNextChapterNo(), true);
            mViewSwipeToNext.setName(ReaderParams.READER_READ_TYPE_CHAPTER, chapterName);
        } else {
            mViewSwipeToNext.setNoFurther(ReaderParams.READER_READ_TYPE_CHAPTER);
        }
    }

    private void setupFooterForJuz() {
        setSwipeDisabled(false);

        if (QuranMeta.isJuzValid(getPrevJuzNo())) {
            String juzName = mActivity.str(R.string.strLabelJuzNo, getPrevJuzNo());
            mViewSwipeToPrevious.setName(ReaderParams.READER_READ_TYPE_JUZ, juzName);
        } else {
            mViewSwipeToPrevious.setNoFurther(ReaderParams.READER_READ_TYPE_JUZ);
        }

        if (QuranMeta.isJuzValid(getNextJuzNo())) {
            String juzName = mActivity.str(R.string.strLabelJuzNo, getNextJuzNo());
            mViewSwipeToNext.setName(ReaderParams.READER_READ_TYPE_JUZ, juzName);
        } else {
            mViewSwipeToNext.setNoFurther(ReaderParams.READER_READ_TYPE_JUZ);
        }
    }

    private void setSwipeDisabled(boolean disabled) {
        mSwipeDisabled = disabled;
        mViewSwipeToPrevious.setSwipeDisabled(disabled);
        mViewSwipeToNext.setSwipeDisabled(disabled);

        /*SmoothRefreshLayout swipeLayout = mActivity.mBinding.swipeLayout;
        swipeLayout.setDisableRefresh(disabled);
        swipeLayout.setDisableLoadMore(disabled);*/
    }

    public int getCurrJuzNo() {
        return mReaderParams.currJuzNo;
    }

    public int getCurrJuzIndex() {
        return getCurrJuzNo() - 1;
    }

    public int getPrevJuzNo() {
        return getCurrJuzNo() - 1;
    }

    public int getNextJuzNo() {
        return getCurrJuzNo() + 1;
    }

    public int getCurrChapterNo() {
        return mReaderParams.currChapter.getChapterNumber();
    }

    public int getCurrChapterIndex() {
        return getCurrChapterNo() - 1;
    }

    public int getPrevChapterNo() {
        return getCurrChapterNo() - 1;
    }

    public int getNextChapterNo() {
        return getCurrChapterNo() + 1;
    }

    public int getCurrVerseNo() {
        return mReaderParams.currChapter.getCurrentVerseNo();
    }

    public int getCurrVerseIndexInSpinner() {
        return getCurrVerseNo() - 1;
    }

    public int getPrevVerseNo() {
        return getCurrVerseNo() - 1;
    }

    public int getNextVerseNo() {
        return getCurrVerseNo() + 1;
    }

    public void goToJuz(int juzNumber) {
        if (!QuranMeta.isJuzValid(juzNumber)) {
            return;
        }

        mReaderParams.readType = ReaderParams.READER_READ_TYPE_JUZ;

        scrollToTop();
        mActivity.initJuz(juzNumber);
    }

    public void previousJuz() {
        mActivity.initJuz(getPrevJuzNo());
    }

    public void nextJuz() {
        mActivity.initJuz(getNextJuzNo());
    }

    public void previousChapter() {
        goToChapter(getPrevChapterNo());
    }

    public void nextChapter() {
        goToChapter(getNextChapterNo());
    }

    public void goToChapter(int chapterNumber) {
        pendingScrollVerse = new int[]{-1, -1};

        if (!QuranMeta.isChapterValid(chapterNumber)) {
            return;
        }

        mReaderParams.readType = ReaderParams.READER_READ_TYPE_CHAPTER;

        scrollToTop();
        mActivity.initChapter(mActivity.mQuranRef.get().getChapter(chapterNumber));
    }

    public void readFullChapter() {
        mActivity.initChapter(mReaderParams.currChapter);
    }

    public void continueChapter() {
        pendingScrollVerse = new int[]{mReaderParams.currChapter.getChapterNumber(), mReaderParams.verseRange.getSecond()};
        mActivity.initChapter(mReaderParams.currChapter);
    }

    public void previousVerse() {
        // 🔥 Daily Quest: 支持跨章节导航（向前）
        int currentChapter = getCurrChapterNo();
        int currentVerse = getCurrVerseNo();
        int prevVerse = getPrevVerseNo();
        
        QuranMeta quranMeta = mActivity.mQuranMetaRef.get();
        
        // 检查是否需要跨章节导航（当前是第1节，需要跳到上一章的最后一节）
        if (prevVerse < 1 && currentChapter > 1) {
            // 跳转到上一章的最后一节
            int prevChapter = currentChapter - 1;
            int lastVerseOfPrevChapter = quranMeta.getChapterVerseCount(prevChapter);
            android.util.Log.d("Navigator", "🔄 跨章节导航（向前）: 从 Surah " + currentChapter + " Verse 1 → Surah " + prevChapter + " Verse " + lastVerseOfPrevChapter);
            jumpToVerseAcrossChapter(prevChapter, lastVerseOfPrevChapter, true);
        } else {
            jumpToVerse(currentChapter, prevVerse, true);
        }
    }

    public void nextVerse() {
        // 🔥 Daily Quest: 支持跨章节导航（向后）
        int currentChapter = getCurrChapterNo();
        int currentVerse = getCurrVerseNo();
        int nextVerse = getNextVerseNo();
        
        QuranMeta quranMeta = mActivity.mQuranMetaRef.get();
        int maxVerseInCurrentChapter = quranMeta.getChapterVerseCount(currentChapter);
        
        // 检查是否需要跨章节导航（当前已经是最后一节）
        if (nextVerse > maxVerseInCurrentChapter) {
            // 检查是否是古兰经的最后一节（Surah 114, Ayat 6）
            if (currentChapter == 114 && currentVerse == 6) {
                android.util.Log.d("Navigator", "📖 已到达古兰经最后一节，无法继续");
                // TODO: 可以显示完成提示对话框
                return;
            }
            
            // 跳转到下一章的第一节
            int nextChapter = currentChapter + 1;
            android.util.Log.d("Navigator", "🔄 跨章节导航（向后）: 从 Surah " + currentChapter + " Verse " + currentVerse + " → Surah " + nextChapter + " Verse 1");
            jumpToVerseAcrossChapter(nextChapter, 1, true);
        } else {
            jumpToVerse(currentChapter, nextVerse, true);
        }
    }
    
    /**
     * 🔥 Daily Quest: 跨章节跳转到指定的Verse
     * 此方法会切换到新的章节，然后导航到指定的verse
     */
    private void jumpToVerseAcrossChapter(int targetChapter, int targetVerse, boolean invokePlayer) {
        android.util.Log.d("Navigator", "🔄 jumpToVerseAcrossChapter called: targetChapter=" + targetChapter + ", targetVerse=" + targetVerse);
        
        if (mActivity.mQuranRef == null || mActivity.mQuranRef.get() == null) {
            android.util.Log.e("Navigator", "❌ mActivity.mQuranRef is null");
            return;
        }
        
        QuranMeta quranMeta = mActivity.mQuranMetaRef.get();
        if (!QuranMeta.isChapterValid(targetChapter)) {
            android.util.Log.e("Navigator", "❌ 无效的章节: " + targetChapter);
            return;
        }
        
        if (!quranMeta.isVerseValid4Chapter(targetChapter, targetVerse)) {
            android.util.Log.e("Navigator", "❌ 无效的节号: Surah " + targetChapter + " Verse " + targetVerse);
            return;
        }
        
        // 获取目标章节
        var targetChapterObj = mActivity.mQuranRef.get().getChapter(targetChapter);
        if (targetChapterObj == null) {
            android.util.Log.e("Navigator", "❌ Cannot get chapter object for " + targetChapter);
            return;
        }
        
        android.util.Log.d("Navigator", "✅ Target chapter object obtained: " + targetChapterObj.getName());
        android.util.Log.d("Navigator", "🔍 Current readType: " + mReaderParams.readType + " (VERSES=" + ReaderParams.READER_READ_TYPE_VERSES + ")");
        
        // 🔥 关键：在单Verse模式下(READER_READ_TYPE_VERSES)，切换章节时需要初始化新的单Verse范围
        if (mReaderParams.readType == ReaderParams.READER_READ_TYPE_VERSES) {
            // 使用单Verse模式初始化新章节的指定verse
            android.util.Log.d("Navigator", "📖 使用单Verse模式初始化: Surah " + targetChapter + " Verse " + targetVerse);
            
            // 🔥 立即更新 currChapter 以防止后续操作使用旧的章节信息
            mReaderParams.currChapter = targetChapterObj;
            
            mActivity.initVerseRange(targetChapterObj, new Pair<>(targetVerse, targetVerse));
            
            // 更新播放器（如果需要）
            if (invokePlayer) {
                mActivity.onVerseJump(targetChapter, targetVerse);
            }
            
            // 更新Verse编号显示
            mActivity.updateVerseNumber(targetChapter, targetVerse);
            
            android.util.Log.d("Navigator", "✅ 跨章节导航完成");
        } else {
            // 对于其他模式（章节模式、Juz模式），使用标准跳转
            android.util.Log.d("Navigator", "📖 非单Verse模式，使用标准跳转");
            jumpToVerse(targetChapter, targetVerse, invokePlayer);
        }
    }

    public void jumpToVerse(int chapterNo, int verseNo, boolean invokePlayer) {
        if (mReaderParams.currChapter == null) return;

        QuranMeta quranMeta = mActivity.mQuranMetaRef.get();

        if (mReaderParams.readType != ReaderParams.READER_READ_TYPE_JUZ
            && !quranMeta.isVerseValid4Chapter(getCurrChapterNo(), verseNo)) {
            return;
        }

        if (invokePlayer) {
            mActivity.onVerseJump(chapterNo, verseNo);
        }

        switch (mReaderParams.readType) {
            case ReaderParams.READER_READ_TYPE_VERSES: {
                Pair<Integer, Integer> range = mReaderParams.verseRange;
                if (QuranUtils.doesRangeDenoteSingle(range)) {
                    if (range.getFirst() != verseNo) {
                        mActivity.initVerseRange(mReaderParams.currChapter, new Pair<>(verseNo, verseNo));
                    }
                } else {
                    if (QuranUtils.isVerseInRange(verseNo, range)) {
                        scrollToVerse(chapterNo, verseNo, true);
                    } else {
                        pendingScrollVerse = new int[]{chapterNo, verseNo};
                        mActivity.persistProgressDialog4PendingTask = true;
                        readFullChapter();
                    }
                }
            }
            break;
            case ReaderParams.READER_READ_TYPE_JUZ:
            case ReaderParams.READER_READ_TYPE_CHAPTER: {
                scrollToVerse(chapterNo, verseNo, true);
            }
            break;
        }

        mActivity.updateVerseNumber(chapterNo, verseNo);
    }

    public void scrollToTop() {
        mActivity.mBinding.readerVerses.scrollToPosition(0);
        if (mActivity.mPlayer != null) {
            mActivity.mPlayer.reveal();
        }
    }

    public void scrollToVerse(int chapterNo, int verseNo, boolean highlight) {
        final RecyclerView.Adapter<?> adp = mActivity.mBinding.readerVerses.getAdapter();
        
        // 🔍 日志 C：scrollToVerse 被调用
        int adapterItemCount = adp != null ? adp.getItemCount() : 0;
        String adapterType = adp != null ? adp.getClass().getSimpleName() : "null";
        android.util.Log.d("🔍 SCROLL_DEBUG", "📍 日志 C: scrollToVerse() 被调用 | Surah " + chapterNo + ", Ayah " + verseNo + 
                           " | Adapter类型 = " + adapterType + " | ItemCount = " + adapterItemCount);
        
        if (adp instanceof ADPReader) {
            scrollToVerseWithOffset(chapterNo, verseNo, 0, highlight);
        } else if (adp instanceof ADPQuranPages) {
            scrollToVerseOnPage(chapterNo, verseNo, highlight);
        }
    }

    public void scrollToVerseWithOffset(int chapterNo, int verseNo, int offset, boolean highlight) {
        final RecyclerView.Adapter<?> adp = mActivity.mBinding.readerVerses.getAdapter();
        if (!(adp instanceof ADPReader)) {
            return;
        }

        final ADPReader adapter = (ADPReader) adp;
        for (int i = 0, l = adapter.getItemCount(); i < l; i++) {
            final ReaderRecyclerItemModel item = adapter.getItem(i);

            if (item == null || item.getViewType() != ReaderParams.RecyclerItemViewType.VERSE) {
                continue;
            }

            final Verse verse = item.getVerse();

            if (verse.chapterNo != chapterNo || verse.verseNo != verseNo) {
                continue;
            }

            // 🔍 日志 C2：找到目标 Verse，准备滚动
            android.util.Log.d("🔍 SCROLL_DEBUG", "📍 日志 C2: 找到目标 Verse，准备滚动 RecyclerView | Position = " + i + 
                               " | Surah " + chapterNo + ", Ayah " + verseNo);
            
            mActivity.mLayoutManager.scrollToPositionWithOffset(i, offset);

            if (highlight) {
                adapter.highlightVerseOnScroll(i);
            }
            
            // 🔍 日志 C3：滚动完成
            android.util.Log.d("🔍 SCROLL_DEBUG", "📍 日志 C3: scrollToPositionWithOffset() 调用完成");
            

            break;
        }
    }

    private void scrollToVerseOnPage(int chapterNo, int verseNo, boolean highlight) {
        RecyclerView.Adapter<?> adp = mActivity.mBinding.readerVerses.getAdapter();
        if (!(adp instanceof ADPQuranPages)) {
            return;
        }

        ADPQuranPages adapter = (ADPQuranPages) adp;
        outer:
        for (int pos = 0, l = adapter.getItemCount(); pos < l; pos++) {
            QuranPageModel pageModel = adapter.getPageModel(pos);

            if (pageModel == null || pageModel.getViewType() != ReaderParams.RecyclerItemViewType.READER_PAGE) {
                continue;
            }

            if (!pageModel.hasChapter(chapterNo)) {
                continue;
            }

            for (QuranPageSectionModel section : pageModel.getSections()) {
                if (section.getChapterNo() != chapterNo || !section.hasVerse(verseNo)) {
                    continue;
                }

                View viewByPosition = mActivity.mLayoutManager.findViewByPosition(pos);
                scrollToVerseOnPageValidate(pos, verseNo, viewByPosition, section, highlight);

                break outer;
            }
        }
    }

    public void scrollToVerseOnPageValidate(int pos, int verseNo, View viewToScrollTo, QuranPageSectionModel section, boolean highlight) {
        LinearLayoutManager lm = mActivity.mLayoutManager;
        ActivityReaderBinding binding = mActivity.mBinding;

        if (viewToScrollTo == null) {
            lm.scrollToPosition(pos);

            binding.readerVerses.post(() -> {
                View viewToScrollToFinal = lm.findViewByPosition(pos);
                if (viewToScrollToFinal instanceof QuranPageView && section.getSectionView() != null) {
                    scrollToVerseOnPageWithOffset(pos, verseNo, section.getSectionView(), lm, highlight);
                }
            });
        } else {
            binding.readerVerses.post(() -> {
                if (viewToScrollTo instanceof QuranPageView && section.getSectionView() != null) {
                    scrollToVerseOnPageWithOffset(pos, verseNo, section.getSectionView(), lm, highlight);
                }
            });
        }
    }

    private void scrollToVerseOnPageWithOffset(int position, int verseNo, QuranPageSectionView sectionView, LinearLayoutManager lm, boolean highlight) {
        VerseArabicHighlightSpan verseSpan = sectionView.findVerseSpan(verseNo);
        if (verseSpan != null) {
            int off = sectionView.getVerseTopOffset(verseSpan);
            lm.scrollToPositionWithOffset(position, -off);
            if (highlight) {
                sectionView.highlightVerseSpan(verseSpan);
            }
        }
    }

    public void setupNavigator() {
        setupSwipe();
        readerFooter.setupBottomNavigator();
    }
}
