package com.bible.tools.reader;

import android.text.TextUtils;

import java.io.Serializable;

public class ReadBook implements Serializable {

    public int bookId;
    public int chapter;
    public int verse;
    private String bookName;

    public ReadBook(int bookId, int chapter, int verse) {
        this.bookId = bookId;
        this.chapter = chapter;
        this.verse = verse;
    }

    @Override
    public String toString() {
        return "ReadBook{" +
                "bookId=" + bookId +
                ", chapter=" + chapter +
                ", verse=" + verse +
                ", bookName='" + bookName + '\'' +
                '}';
    }

    public int getBookId() {
        return bookId;
    }

    public void setBookId(int bookId) {
        this.bookId = bookId;
    }

    public int getChapter() {
        return chapter;
    }

    public void setChapter(int chapter) {
        this.chapter = chapter;
    }

    public int getVerse() {
        return verse;
    }

    public void setVerse(int verse) {
        this.verse = verse;
    }

    public String getBookName() {
        if (TextUtils.isEmpty(this.bookName)) {
            bookName = ReadManager.getInstance().getBookName(this.bookId);
        }
        return bookName;
    }

    public void setBookName(String bookName) {
        this.bookName = bookName;
    }
}
