package com.quran.quranaudio.online.quran_module.reader_managers;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.Nullable;
import static com.quran.quranaudio.online.quran_module.reader_managers.ReaderParams.READER_READ_TYPE_CHAPTER;
import static com.quran.quranaudio.online.quran_module.reader_managers.ReaderParams.READER_READ_TYPE_JUZ;
import static com.quran.quranaudio.online.quran_module.reader_managers.ReaderParams.READER_READ_TYPE_VERSES;

import com.quran.quranaudio.online.quran_module.activities.ActivityReader;
import com.quran.quranaudio.online.quran_module.activities.ReaderPossessingActivity;
import com.quran.quranaudio.online.quran_module.components.quran.QuranMeta;
import com.quran.quranaudio.online.quran_module.components.quran.subcomponents.Chapter;
import com.quran.quranaudio.online.quran_module.components.quran.subcomponents.Footnote;
import com.quran.quranaudio.online.quran_module.components.quran.subcomponents.Verse;
import com.quran.quranaudio.online.quran_module.interfaceUtils.BookmarkCallbacks;
import com.quran.quranaudio.online.quran_module.interfaceUtils.Destroyable;
import com.quran.quranaudio.online.quran_module.utils.Logger;
import com.quran.quranaudio.online.quran_module.utils.quran.QuranUtils;
import com.quran.quranaudio.online.quran_module.utils.reader.factory.ReaderFactory;
import com.quran.quranaudio.online.quran_module.utils.thread.runner.RunnableTaskRunner;
import com.quran.quranaudio.online.quran_module.views.reader.dialogs.FootnotePresenter;
import com.quran.quranaudio.online.quran_module.views.reader.dialogs.QuickReference;
import com.quran.quranaudio.online.quran_module.widgets.dialog.loader.PeaceProgressDialog;

import java.util.Set;

import kotlin.Pair;

public class ActionController implements Destroyable {
    private final ReaderPossessingActivity mActivity;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final RunnableTaskRunner taskRunner = new RunnableTaskRunner(handler);
  //  private final VerseOptionsDialog mVOD = new VerseOptionsDialog(); //+923470036634
    private final FootnotePresenter mFootnotePresenter = new FootnotePresenter();
    private final QuickReference mQuickReference = new QuickReference();
    private final PeaceProgressDialog mProgressDialog;

    public ActionController(ReaderPossessingActivity readerPossessingActivity) {
        mActivity = readerPossessingActivity;

        mProgressDialog = new PeaceProgressDialog(mActivity);
        mProgressDialog.setDimAmount(0);
        mProgressDialog.setElevation(0);
    }

    public void showFootnote(Verse verse, Footnote footnote, boolean isUrduSlug) {
        mFootnotePresenter.present(mActivity, verse, footnote, isUrduSlug);
    }

    public void showFootnotes(Verse verse) {
        mFootnotePresenter.present(mActivity, verse);
    }

    public void openVerseOptionDialog(Verse verse, @Nullable BookmarkCallbacks verseViewCallbacks) {
     //   mVOD.open(mActivity, verse, verseViewCallbacks);
    }

    public void openShareDialog(int chapterNo, int verseNo) {
    //    mVOD.openShareDialog(mActivity, chapterNo, verseNo); //+923470036634
    }

    public void dismissShareDialog() {
     //   mVOD.dismissShareDialog(); //+923470036634
    }

    public void showVerseReference(Set<String> translSlugs, int chapterNo, String verses) {
        try {
            mQuickReference.show(mActivity, translSlugs, chapterNo, verses);
        } catch (Exception e) {
            e.printStackTrace();
            Logger.reportError(e);
        }
    }

    public void showReferenceSingleVerseOrRange(
        Set<String> translSlugs,
        int chapterNo,
        Pair<Integer, Integer> verseRange
    ) {
        mQuickReference.showSingleVerseOrRange(mActivity, translSlugs, chapterNo, verseRange);
    }

    public void openVerseReference(int chapterNo, Pair<Integer, Integer> verseRange) {
        if (mActivity instanceof ActivityReader) {
            openVerseReferenceWithinReader((ActivityReader) mActivity, chapterNo, verseRange);
        } else {
            ReaderFactory.startVerseRange(mActivity, chapterNo, verseRange);
        }

        closeDialogs(true);
    }

    private void openVerseReferenceWithinReader(ActivityReader activity, int chapterNo, Pair<Integer, Integer> verseRange) {
        ReaderParams readerParams = activity.mReaderParams;
        if (!QuranMeta.isChapterValid(chapterNo) || verseRange == null) {
            return;
        }

        Chapter chapter = mActivity.mQuranRef.get().getChapter(chapterNo);

        final boolean initNewRange;
        boolean isReferencedVerseSingle = QuranUtils.doesRangeDenoteSingle(verseRange);

        if (isReferencedVerseSingle) {
            if (readerParams.readType == READER_READ_TYPE_JUZ) {
                initNewRange = !mActivity.mQuranMetaRef.get().isVerseValid4Juz(readerParams.currJuzNo, chapterNo,
                    verseRange.getFirst());
            } else if (readerParams.readType == READER_READ_TYPE_CHAPTER || readerParams.isSingleVerse()) {
                initNewRange = !chapter.equals(readerParams.currChapter);
            } else if (readerParams.readType == READER_READ_TYPE_VERSES) {
                initNewRange = !QuranUtils.isVerseInRange(verseRange.getFirst(), readerParams.verseRange);
            } else {
                initNewRange = true;
            }
        } else {
            initNewRange = true;
        }

        if (initNewRange) {
            activity.initVerseRange(chapter, verseRange);
        } else {
            activity.mNavigator.jumpToVerse(chapterNo, verseRange.getFirst(), false);
        }
    }

    public void onVerseRecite(int chapterNo, int verseNo, boolean isReciting) {
      //  mVOD.onVerseRecite(chapterNo, verseNo, isReciting); //+923470036634
    }

    public void showLoader() {
        mProgressDialog.show();
    }

    public void dismissLoader() {
        mProgressDialog.dismiss();
    }

    public void closeDialogs(boolean closeForReal) {
        mProgressDialog.dismiss();

        if (closeForReal || mActivity.isFinishing()) {
            mQuickReference.dismiss();
         //   mVOD.dismiss();
         //   mVOD.dismissShareDialog();
            mFootnotePresenter.dismiss();
        }
    }

    @Override
    public void destroy() {
        taskRunner.cancel();

        closeDialogs(false);
    }
}
