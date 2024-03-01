package com.quran.quranaudio.online.quran_module.components.reader;

import androidx.annotation.NonNull;

import com.quran.quranaudio.online.quran_module.reader_managers.ReaderParams;
import com.quran.quranaudio.online.quran_module.components.quran.subcomponents.Verse;

public class ReaderRecyclerItemModel {
    @ReaderParams.RecyclerItemViewTypeConst
    private int viewType;
    private Verse verse;
    private int chapterNo;
    private boolean scrollHighlightPending;

    public ReaderRecyclerItemModel setViewType(@ReaderParams.RecyclerItemViewTypeConst int viewType) {
        this.viewType = viewType;
        return this;
    }

    @ReaderParams.RecyclerItemViewTypeConst
    public int getViewType() {
        return viewType;
    }

    public ReaderRecyclerItemModel setVerse(Verse verse) {
        this.verse = verse;
        return this;
    }

    public Verse getVerse() {
        return verse;
    }

    public int getChapterNo() {
        return chapterNo;
    }

    public ReaderRecyclerItemModel setChapterNo(int chapterNo) {
        this.chapterNo = chapterNo;
        return this;
    }

    @NonNull
    @Override
    public String toString() {
        String viewTypeStr;
        switch (viewType) {
            case ReaderParams.RecyclerItemViewType.BISMILLAH: viewTypeStr = "BISMILLAH";
                break;
            case ReaderParams.RecyclerItemViewType.VERSE: viewTypeStr = "VERSE";
                break;
            case ReaderParams.RecyclerItemViewType.CHAPTER_TITLE: viewTypeStr = "CHAPTER_TITLE";
                break;
            case ReaderParams.RecyclerItemViewType.READER_FOOTER: viewTypeStr = "READER_FOOTER";
                break;
            case ReaderParams.RecyclerItemViewType.CHAPTER_INFO:
                viewTypeStr = "CHAPTER_INFO";
                break;
            case ReaderParams.RecyclerItemViewType.IS_VOTD:
                viewTypeStr = "IS_VOTD";
                break;
            case ReaderParams.RecyclerItemViewType.NO_TRANSL_SELECTED:
                viewTypeStr = "NO_TRANSL_SELECTED";
                break;
            case ReaderParams.RecyclerItemViewType.READER_PAGE:
                viewTypeStr = "READER_PAGE";
                break;
            default: viewTypeStr = "NONE";
        }
        return "ReaderRecyclerItemModel: VIEW_TYPE: " + viewTypeStr;
    }

    public void setScrollHighlightPending(boolean pending) {
        scrollHighlightPending = pending;
    }

    public boolean isScrollHighlightPending() {
        return scrollHighlightPending;
    }
}
