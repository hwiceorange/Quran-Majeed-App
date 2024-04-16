package com.bible.tools.reader;

import com.bible.tools.reader.Book;
import com.bible.tools.reader.ReadManager;
import com.bible.tools.utils.NoProguard;

public class ReadSort implements NoProguard {
    public int bookId;
    public boolean empty;

    public ReadSort(int bookId) {
        this.bookId = bookId;
    }

    public boolean isReadNoComplete() {
        Book book = ReadManager.getInstance().getAllKJVBook()[this.bookId];
        return book.isReadNoComplete();
    }
}
