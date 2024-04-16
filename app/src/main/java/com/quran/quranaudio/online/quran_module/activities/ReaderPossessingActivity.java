package com.quran.quranaudio.online.quran_module.activities;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.Toast;
import androidx.activity.result.ActivityResult;
import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import static android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;

import com.peacedesign.android.utils.span.TypefaceSpan2;
import com.quran.quranaudio.online.quran_module.activities.base.BaseActivity;
import com.quran.quranaudio.online.quran_module.components.bookmark.BookmarkModel;
import com.quran.quranaudio.online.quran_module.components.quran.Quran;
import com.quran.quranaudio.online.quran_module.components.quran.QuranMeta;
import com.quran.quranaudio.online.quran_module.components.quran.subcomponents.Footnote;
import com.quran.quranaudio.online.quran_module.components.quran.subcomponents.QuranTranslBookInfo;
import com.quran.quranaudio.online.quran_module.components.quran.subcomponents.Translation;
import com.quran.quranaudio.online.quran_module.components.quran.subcomponents.Verse;
import com.quran.quranaudio.online.quran_module.interfaceUtils.BookmarkCallbacks;
import com.quran.quranaudio.online.quran_module.interfaceUtils.OnResultReadyCallback;
import com.quran.quranaudio.online.quran_module.reader_managers.ActionController;
import com.quran.quranaudio.online.quran_module.reader_managers.ReaderVerseDecorator;
import com.quran.quranaudio.online.quran_module.suppliments.BookmarkViewer;
import com.quran.quranaudio.online.R;
import com.quran.quranaudio.online.quran_module.components.bookmark.BookmarkModel;
import com.quran.quranaudio.online.quran_module.components.quran.subcomponents.Footnote;
import com.quran.quranaudio.online.quran_module.components.quran.subcomponents.QuranTranslBookInfo;
import com.quran.quranaudio.online.quran_module.components.quran.subcomponents.Translation;
import com.quran.quranaudio.online.quran_module.db.bookmark.BookmarkDBHelper;
import com.quran.quranaudio.online.quran_module.interfaceUtils.OnResultReadyCallback;
import com.quran.quranaudio.online.quran_module.reader_managers.ReaderVerseDecorator;
import com.quran.quranaudio.online.quran_module.utils.parser.HtmlParser;
import com.quran.quranaudio.online.quran_module.utils.reader.ReferenceTagHandler;
import com.quran.quranaudio.online.quran_module.utils.reader.TranslUtils;
import com.quran.quranaudio.online.quran_module.utils.reader.factory.QuranTranslationFactory;
import com.quran.quranaudio.online.quran_module.utils.verse.VerseUtils;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import kotlin.Pair;

public abstract class ReaderPossessingActivity extends BaseActivity implements BookmarkCallbacks {
    public final AtomicReference<QuranMeta> mQuranMetaRef = new AtomicReference<>(); //don't initialize QuranMeta as it is being null checked in getQuranMetaSafely.
    public final AtomicReference<Quran> mQuranRef = new AtomicReference<>();
    public QuranTranslationFactory mTranslFactory;

    private final Set<OnResultReadyCallback<QuranMeta>> mPendingMetaRequesters = new HashSet<>();

    public ReaderVerseDecorator mVerseDecorator;
    public ActionController mActionController;

    public int mColorSecondary;
    public int mVerseHighlightedBGColor;
    public int mVerseUnhighlightedBGColor = Color.TRANSPARENT;
    public int mRefHighlightTxtColor;
    public int mRefHighlightBGColor;
    public int mRefHighlightBGColorPres;
    public int mAuthorTextSize;
    public Typeface mUrduTypeface;

    private BookmarkDBHelper mBookmarkDBHelper;
    private BookmarkViewer mBookmarkViewer;
    private BookmarkCallbacks mLastBookmarkCallback;

    @Override
    protected void onDestroy() {
        if (mTranslFactory != null) {
            mTranslFactory.close();
        }

        if (mBookmarkDBHelper != null) {
            mBookmarkDBHelper.close();
        }

        if (mBookmarkViewer != null) {
            mBookmarkViewer.destroy();
        }

        if (mActionController != null) {
            mActionController.destroy();
        }

        mVerseDecorator = null;

        super.onDestroy();
    }

    @CallSuper
    @Override
    protected void preActivityInflate(@Nullable Bundle savedInstanceState) {
        mTranslFactory = new QuranTranslationFactory(this);

        mBookmarkDBHelper = new BookmarkDBHelper(this);
        mBookmarkViewer = new BookmarkViewer(this, mQuranMetaRef, mBookmarkDBHelper, this);
        mVerseDecorator = new ReaderVerseDecorator(this);
        mActionController = new ActionController(this);

        mColorSecondary = color(R.color.colorSecondary);
        mVerseHighlightedBGColor = color(R.color.colorBGReaderVerseSelected);
        mRefHighlightTxtColor = color(R.color.colorSecondary);
        mRefHighlightBGColor = color(R.color.colorPrimaryAlpha10);
        mRefHighlightBGColorPres = color(R.color.colorPrimaryAlpha50);
        mAuthorTextSize = dimen(R.dimen.dmnCommonSize3);
        mUrduTypeface = font(R.font.font_urdu);
    }

    @Override
    protected final void onActivityInflated(@NonNull View activityView, Bundle savedInstanceState) {
        preReaderReady(activityView, getIntent(), savedInstanceState);

        QuranMeta.prepareInstance(this, quranMeta -> {
            mQuranMetaRef.set(quranMeta);
            onQuranMetaAvailable(quranMeta);

            Quran.prepareInstance(this, quranMeta, quran -> {
                mQuranRef.set(quran);
                onReaderReady(getIntent(), savedInstanceState);
            });
        });
    }

    protected abstract void preReaderReady(@NonNull View activityView, @NonNull Intent intent, @Nullable Bundle savedInstanceState);

    protected abstract void onReaderReady(@NonNull Intent intent, @Nullable Bundle savedInstanceState);

    protected void reparseQuran() {
        Quran.prepareInstance(this, mQuranMetaRef.get(), quran -> {
            mQuranRef.set(quran);
            onQuranReParsed(quran);
        });
    }

    protected void onQuranReParsed(Quran quran) {
    }

    @CallSuper
    @Override
    protected void onActivityResult2(ActivityResult result) {
        if (result.getResultCode() == RESULT_OK) {
            mActionController.dismissShareDialog();
        }
    }

    public synchronized void getQuranMetaSafely(OnResultReadyCallback<QuranMeta> readyCallback) {
        if (mQuranMetaRef.get() != null) {
            readyCallback.onReady(mQuranMetaRef.get());
        } else {
            mPendingMetaRequesters.add(readyCallback);
        }
    }

    private void onQuranMetaAvailable(QuranMeta quranMeta) {
        for (OnResultReadyCallback<QuranMeta> requester : mPendingMetaRequesters) {
            requester.onReady(quranMeta);
        }
        mPendingMetaRequesters.clear();
    }

    public CharSequence prepareTranslSpannable(Verse verse, List<Translation> translations, Map<String, QuranTranslBookInfo> bookInfos) {
        SpannableStringBuilder sb = new SpannableStringBuilder();

        for (int i = 0, l = translations.size(), l2 = l - 1; i < l; i++) {
            Translation translation = translations.get(i);
            sb.append(prepareSingleTranslationText(verse, translation));
            sb.append("\n");

            String bookSlug = translation.getBookSlug();
            QuranTranslBookInfo bookInfo = bookInfos.get(bookSlug);
            if (bookInfo != null) {
                String author = bookInfo.getDisplayName(false);
                Typeface authorFont = translation.isUrdu() ? mUrduTypeface : Typeface.SANS_SERIF;
                sb.append(VerseUtils.prepareTranslAuthorText(
                    author,
                    mColorSecondary,
                    mAuthorTextSize,
                    authorFont,
                    TranslUtils.isTransliteration(bookSlug)
                ));
            }

            sb.append(i < l2 ? "\n\n" : "\n");
        }

        return sb;
    }

    private CharSequence prepareSingleTranslationText(Verse verse, Translation translation) {
        CharSequence translationText = translation.getText();
        if (TextUtils.isEmpty(translationText)) {
            return null;
        }

        Set<String> translSlugs = Collections.singleton(translation.getBookSlug());

        ReferenceTagHandler tagHandler = new ReferenceTagHandler(translSlugs, mRefHighlightTxtColor,
            mRefHighlightBGColor,
            mRefHighlightBGColorPres, this::showVerseReference,
            footnoteNo -> showFootnote(verse, translation.getFootnote(footnoteNo), translation.isUrdu()));

        // to prevent clickable span to be invoked on empty space.
        translationText += " ";

        Spanned spanned = HtmlParser.buildSpannedText(translationText.toString(), tagHandler);
        SpannableStringBuilder sb = new SpannableStringBuilder(spanned);

        Typeface typeface = translation.isUrdu() ? mUrduTypeface : Typeface.SANS_SERIF;
        sb.setSpan(new TypefaceSpan2(typeface), 0, sb.length(), SPAN_EXCLUSIVE_EXCLUSIVE);

        if (TranslUtils.isTransliteration(translation.getBookSlug())) {
            sb.setSpan(new StyleSpan(Typeface.ITALIC), 0, sb.length(), SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return sb;
    }

    public void onBookmarkView(int chapterNo, int fromVerse, int toVerse, BookmarkCallbacks callbacks) {
        mLastBookmarkCallback = callbacks;
        mBookmarkViewer.view(chapterNo, fromVerse, toVerse);
    }

    public boolean isBookmarked(int chapterNo, int fromVerse, int toVerse) {
        return mBookmarkDBHelper.isBookmarked(chapterNo, fromVerse, toVerse);
    }

    public void addVerseToBookmark(int chapterNo, int fromVerse, int toVerse, @NonNull BookmarkCallbacks callbacks) {
        mLastBookmarkCallback = callbacks;

        mBookmarkDBHelper.addToBookmark(chapterNo, fromVerse, toVerse, null, model -> {
            mBookmarkViewer.edit(model);
            callbacks.onBookmarkAdded(model);
        });
    }

    public void openVerseOptionDialog(Verse verse, @Nullable BookmarkCallbacks verseViewCallbacks) {
        mActionController.openVerseOptionDialog(verse, verseViewCallbacks);
    }

    public void showFootnote(Verse verse, Footnote footnote, boolean isUrduSlug) {
        if (footnote != null) {
            mActionController.showFootnote(verse, footnote, isUrduSlug);
        } else {
            Toast.makeText(this, "Footnote not found, please report us.", Toast.LENGTH_LONG).show();
        }
    }

    public void showFootnotes(Verse verse) {
        mActionController.showFootnotes(verse);
    }

    public void showVerseReference(Set<String> translSlug, int chapterNo, String verses) {
        mActionController.showVerseReference(translSlug, chapterNo, verses);
    }

    public void showReferenceSingleVerseOrRange(
        Set<String> translSlugs,
        int chapterNo,
        Pair<Integer, Integer> verseRange
    ) {
        mActionController.showReferenceSingleVerseOrRange(translSlugs, chapterNo, verseRange);
    }

    public void startQuickEditShare(Verse verse) {
        /*ReaderFactory.startQuickEditShare(this, verse.getChapterNo(), verse.getVerseNo());*/
    }

    @Override
    public void onBookmarkRemoved(BookmarkModel model) {
        if (mLastBookmarkCallback != null) {
            mLastBookmarkCallback.onBookmarkRemoved(model);
        }
    }

    @Override
    public void onBookmarkAdded(BookmarkModel model) {
        if (mLastBookmarkCallback != null) {
            mLastBookmarkCallback.onBookmarkAdded(model);
        }
    }

    @Override
    public void onBookmarkUpdated(BookmarkModel model) {
        if (mLastBookmarkCallback != null) {
            mLastBookmarkCallback.onBookmarkUpdated(model);
        }
    }
}
