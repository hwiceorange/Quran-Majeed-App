package com.bible.tools.reader;

import android.text.TextUtils;
import android.util.Log;

import com.bible.tools.extension.SPTools;
import com.blankj.utilcode.util.GsonUtils;
import com.google.android.gms.common.util.CollectionUtils;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class ReadManager {

    private final HashSet<String> readBookList = new HashSet<>();
    /**
     * 阅读顺序
     */
    private final List<ReadSort> readBookSortList = new ArrayList<>();
    public static final String READ_BOOK_LIST = "read_book_list";
    public static final String READ_SORT_LIST = "read_sort_list";

    private final List<ReadBook> listReadBook = new ArrayList<>();
    private Book[] allActiveBook = null;


    private ReadManager() {
        String json = SPTools.INSTANCE.getString(READ_BOOK_LIST, "");

        HashSet<String> result = GsonUtils.fromJson(json, new TypeToken<HashSet<String>>() {
        }.getType());
        if (result == null) {
            result = new HashSet<>();
        }
        if (!CollectionUtils.isEmpty(result)) {
            for (String str : result) {
                if (!TextUtils.isEmpty(str) && str.startsWith(S.activeVersionId)) {
                    readBookList.add(str);
                }
            }
        }

    }

    private static final class SReadManagerHolder {
        private static final ReadManager sReadManager = new ReadManager();
    }

    public static ReadManager getInstance() {
        return SReadManagerHolder.sReadManager;
    }

    public List<ReadBook> getListReadBook() {
        return listReadBook;
    }

    /**
     * @param book 指定书
     * @return 获取指定书籍读了几章
     */
    public int getReadBookChapterCount(Book book) {
        int count = 0;
        if (book == null) {
            return count;
        }

        for (int i = 0; i < book.chapter_count; i++) {
            if (isReadBook(book.bookId, i)) {
                ++count;
            }
        }
        return count;
    }

    /**
     * @return 获取所有书籍
     */
    public Book[] getAllKJVBook() {
        if (allActiveBook == null || allActiveBook.length == 0) {
            if (S.activeVersion == null) {
              //  S.activeVersion = Version(1)// VersionImpl.getInternalVersion();
            }
            allActiveBook = S.activeVersion.getConsecutiveBooks();
        }
        return allActiveBook;
    }

    public void clearBookVersion(){
        allActiveBook = null;
        S.activeVersion = null;
      //  VersionImpl.clearInternalVersion();
    }


    /**
     * @param bookId  书id
     * @param chapter 章节id
     * @return 是否读过指定章节
     */
    public boolean isReadBook(int bookId, int chapter) {
        String activeVersionId = S.activeVersionId;
        String result = activeVersionId + ":" + bookId + ":" + chapter;
        return readBookList.contains(result);
    }

    /**
     * @param bookId 书 id
     * @return 获取书名
     */
    public String getBookName(int bookId) {
        if (getAllKJVBook() == null) {
            return "";
        }
        for (Book book : getAllKJVBook()) {
            if (book.bookId == bookId) {
                return book.shortName;
            }
        }
        return "";
    }

    /**
     * 保存阅读mark
     *
     * @param bookId 书id
     * @param chapter 章节id
     */
    public void setReadBook(int bookId, int chapter) {
        String activeVersionId = S.activeVersionId;
        String result = activeVersionId + ":" + bookId + ":" + chapter;
        readBookList.add(result);
        String json = GsonUtils.toJson(readBookList);
        SPTools.INSTANCE.put(READ_BOOK_LIST, json);


        //阅读顺序
        ReadSort readSort = new ReadSort(bookId);
        int position = -1;
        for (int i = 0; i < readBookSortList.size(); i++) {
            if (readBookSortList.get(i).bookId == readSort.bookId) {
                position = i;
                break;
            }
        }
        if (position != -1) {
            readBookSortList.remove(position);
        }
        readBookSortList.add(0, readSort);

        Log.e("read",bookId + ":" + chapter);

        String readBookSort = GsonUtils.toJson(readBookSortList);
        SPTools.INSTANCE.put(READ_SORT_LIST, readBookSort);
    }


    public int loadVersion(final MVersion mv, int openAri) {
        try {

            if (S.activeVersion == null) {
               // S.activeVersion = VersionImpl.getInternalVersion();
            }

            final Version version = mv.getVersion();
            // we already have some other version loaded, so make the new version openResultFromQuiz the same book
            if (S.mActiveBook != null) {
                int bookId = S.mActiveBook.bookId;
                if (version != null) {
                    Book book = version.getBook(bookId);
                    if (book != null) {
                        S.mActiveBook = book;
                    } else {
                        S.mActiveBook = version.getFirstBook();
                    }
                }
            } else {
                S.mActiveBook = S.activeVersion.getFirstBook();
            }

            S.activeVersion = version;
            S.activeVersionId = mv.getVersionId();

            S.mActiveBook = S.activeVersion.getBook(Ari.toBook(openAri));
            allActiveBook = S.activeVersion.getConsecutiveBooks();

            readBookList.clear();
            String json = SPTools.INSTANCE.getString(READ_BOOK_LIST, "");
            HashSet<String> result = GsonUtils.fromJson(json, new TypeToken<HashSet<String>>() {
            }.getType());
            if (result == null) {
                result = new HashSet<>();
            }
            if (!CollectionUtils.isEmpty(result)) {
                for (String str : result) {
                    if (!TextUtils.isEmpty(str) && str.startsWith(S.activeVersionId)) {
                        readBookList.add(str);
                    }
                }
            }

            final int lastBookId = Ari.toBook(openAri);
            final int lastChapter = Ari.toChapter(openAri);
            final int lastVerse = Ari.toVerse(openAri);

            int initPos = 0;

            listReadBook.clear();
            if (getAllKJVBook() != null) {
                for (Book book : getAllKJVBook()) {
                    if (book == null) {
                        continue;
                    }

                    for (int j = 1; j <= book.chapter_count; j++) {
                        if (lastBookId == book.bookId && lastChapter == j) {
                            ReadBook read = new ReadBook(book.bookId, j, lastVerse);
                            listReadBook.add(read);
                            initPos = listReadBook.indexOf(read);
                        } else {
                            ReadBook read = new ReadBook(book.bookId, j, 1);
                            listReadBook.add(read);
                        }
                    }
                }
            }

            return initPos;
        } catch (Throwable e) {
            // so we don't crash on the beginning of the app
            Log.e("read","Error opening main version " + e);
            e.printStackTrace();
            return -1;
        }
    }


    public static void saveBibleIndex(int bookId, int chapterId, int verseId) {
        SPTools.INSTANCE.put(Prefkey.lastBookId.toString(), bookId);
        SPTools.INSTANCE.put(Prefkey.lastChapter.toString(), chapterId);
        SPTools.INSTANCE.put(Prefkey.lastVerse.toString(), verseId);
    }
}
