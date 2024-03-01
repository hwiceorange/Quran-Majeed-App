package com.quran.quranaudio.online.quran_module.db.bookmark;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.widget.Toast;
import androidx.annotation.NonNull;

import com.quran.quranaudio.online.quran_module.components.bookmark.BookmarkModel;
import com.quran.quranaudio.online.quran_module.interfaceUtils.OnResultReadyCallback;
import com.quran.quranaudio.online.quran_module.utils.univ.DBUtils;
import com.quran.quranaudio.online.quran_module.utils.univ.DateUtils;
import com.quran.quranaudio.online.R;
import com.quran.quranaudio.online.quran_module.components.bookmark.BookmarkModel;
import com.quran.quranaudio.online.quran_module.interfaceUtils.OnResultReadyCallback;
import com.quran.quranaudio.online.quran_module.utils.univ.DBUtils;

import java.util.ArrayList;

public class BookmarkDBHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "Bookmark.db";
    public static final int DB_VERSION = 1;
    private final Context mContext;


    public BookmarkDBHelper(@NonNull Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase DB) {
        final String CREATE_TABLE = "CREATE TABLE " + BookmarkContract.BookmarkEntry.TABLE_NAME + " (" +
            BaseColumns._ID + " INTEGER PRIMARY KEY," +
            BookmarkContract.BookmarkEntry.COL_CHAPTER_NO + " INTEGER," +
            BookmarkContract.BookmarkEntry.COL_FROM_VERSE_NO + " INTEGER," +
            BookmarkContract.BookmarkEntry.COL_TO_VERSE_NO + " INTEGER," +
            BookmarkContract.BookmarkEntry.COL_DATETIME + " TEXT," +
            BookmarkContract.BookmarkEntry.COL_NOTE + " TEXT)";
        DB.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase DB, int oldVersion, int newVersion) {
        final String DELETE_TABLE = "DROP TABLE IF EXISTS " + BookmarkContract.BookmarkEntry.TABLE_NAME;
        DB.execSQL(DELETE_TABLE);
        onCreate(DB);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public void addToBookmark(int chapterNo, int fromVerse, int toVerse, String note, OnResultReadyCallback<BookmarkModel> callback) {
        if (isBookmarked(chapterNo, fromVerse, toVerse)) {
            Toast.makeText(mContext, R.string.strMsgBookmarkAddedAlready, Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = getWritableDatabase();

        String dateTime = DateUtils.getDateTimeNow();
        ContentValues values = new ContentValues();
        values.put(BookmarkContract.BookmarkEntry.COL_CHAPTER_NO, chapterNo);
        values.put(BookmarkContract.BookmarkEntry.COL_FROM_VERSE_NO, fromVerse);
        values.put(BookmarkContract.BookmarkEntry.COL_TO_VERSE_NO, toVerse);
        values.put(BookmarkContract.BookmarkEntry.COL_DATETIME, dateTime);
        values.put(BookmarkContract.BookmarkEntry.COL_NOTE, note);

        long rowId = db.insert(BookmarkContract.BookmarkEntry.TABLE_NAME, null, values);
        boolean inserted = rowId != -1;

        final int msg;
        if (inserted) {
            if (callback != null) {
                BookmarkModel model = new BookmarkModel(rowId, chapterNo, fromVerse, toVerse, dateTime);
                model.setNote(note);
                callback.onReady(model);
            }
            msg = R.string.strMsgBookmarkAdded;
        } else {
            msg = R.string.strMsgBookmarkAddFailed;
        }
        Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
    }

    public void updateBookmark(int chapterNo, int fromVerse, int toVerse, String note, OnResultReadyCallback<BookmarkModel> callback) {
        SQLiteDatabase db = getWritableDatabase();

        String whereClause = BookmarkContract.BookmarkEntry.COL_CHAPTER_NO + "=? AND " + BookmarkContract.BookmarkEntry.COL_FROM_VERSE_NO + "=? AND " + BookmarkContract.BookmarkEntry.COL_TO_VERSE_NO + "=?";
        String[] whereArgs = {String.valueOf(chapterNo), String.valueOf(fromVerse), String.valueOf(toVerse)};

        String dateTime = DateUtils.getDateTimeNow();

        ContentValues values = new ContentValues();
        values.put(BookmarkContract.BookmarkEntry.COL_CHAPTER_NO, chapterNo);
        values.put(BookmarkContract.BookmarkEntry.COL_FROM_VERSE_NO, fromVerse);
        values.put(BookmarkContract.BookmarkEntry.COL_TO_VERSE_NO, toVerse);
        values.put(BookmarkContract.BookmarkEntry.COL_DATETIME, dateTime);
        values.put(BookmarkContract.BookmarkEntry.COL_NOTE, note);

        long rowId = db.update(BookmarkContract.BookmarkEntry.TABLE_NAME, values, whereClause, whereArgs);
        boolean updated = rowId != -1;

        if (updated) {
            if (callback != null) {
                BookmarkModel model = new BookmarkModel(rowId, chapterNo, fromVerse, toVerse, dateTime);
                model.setNote(note);
                callback.onReady(model);
            }
        }
    }

    public void removeFromBookmark(int chapterNo, int fromVerse, int toVerse, Runnable runOnSucceed) {
        SQLiteDatabase db = getWritableDatabase();

        String whereClause = DBUtils.createDBSelection(BookmarkContract.BookmarkEntry.COL_CHAPTER_NO, BookmarkContract.BookmarkEntry.COL_FROM_VERSE_NO, BookmarkContract.BookmarkEntry.COL_TO_VERSE_NO);
        String[] whereArgs = {String.valueOf(chapterNo), String.valueOf(fromVerse), String.valueOf(toVerse)};

        int rowsAffected = db.delete(BookmarkContract.BookmarkEntry.TABLE_NAME, whereClause, whereArgs);

        boolean deleted = rowsAffected >= 1;

        final int msg;
        if (deleted) {
            if (runOnSucceed != null) {
                runOnSucceed.run();
            }
            msg = R.string.strMsgBookmarkRemoved;
        } else {
            msg = R.string.strMsgBookmarkRemoveFailed;
        }
        Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
    }

    public void removeBookmarksBulk(long[] ids, Runnable runOnSucceed) {
        SQLiteDatabase db = getWritableDatabase();

        int rowsAffected = 0;
        for (long id : ids) {
            String whereClause = BaseColumns._ID + "=?";
            String[] whereArgs = {String.valueOf(id)};

            rowsAffected += db.delete(BookmarkContract.BookmarkEntry.TABLE_NAME, whereClause, whereArgs);
        }

        boolean deleted = rowsAffected >= 1;
        final int msg;
        if (deleted) {
            if (runOnSucceed != null) {
                runOnSucceed.run();
            }
            msg = R.string.strMsgBookmarkRemoved;
        } else {
            msg = R.string.strMsgBookmarkRemoveFailed;
        }
        Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
    }

    public boolean isBookmarked(int chapterNo, int fromVerse, int toVerse) {
        SQLiteDatabase db = getReadableDatabase();

        String selection = DBUtils.createDBSelection(BookmarkContract.BookmarkEntry.COL_CHAPTER_NO, BookmarkContract.BookmarkEntry.COL_FROM_VERSE_NO, BookmarkContract.BookmarkEntry.COL_TO_VERSE_NO);
        String[] selectionArgs = {
            String.valueOf(chapterNo),
            String.valueOf(fromVerse),
            String.valueOf(toVerse)
        };

        long count = DatabaseUtils.queryNumEntries(db, BookmarkContract.BookmarkEntry.TABLE_NAME, selection, selectionArgs);

        return count > 0;
    }

    public void removeAllBookmarks() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(BookmarkContract.BookmarkEntry.TABLE_NAME, null, null);
    }

    public BookmarkModel getBookmark(int chapNo, int fromVerse, int toVerse) {
        SQLiteDatabase db = getReadableDatabase();

        String selection = DBUtils.createDBSelection(BookmarkContract.BookmarkEntry.COL_CHAPTER_NO, BookmarkContract.BookmarkEntry.COL_FROM_VERSE_NO, BookmarkContract.BookmarkEntry.COL_TO_VERSE_NO);
        String[] selectionArgs = {String.valueOf(chapNo), String.valueOf(fromVerse), String.valueOf(toVerse)};
        String sortOrder = BaseColumns._ID + " DESC";

        BookmarkModel model = null;

        Cursor cursor = db.query(BookmarkContract.BookmarkEntry.TABLE_NAME, null, selection, selectionArgs, null, null, sortOrder);
        while (cursor.moveToNext()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(BaseColumns._ID));
            int chapterNo = cursor.getInt(cursor.getColumnIndexOrThrow(BookmarkContract.BookmarkEntry.COL_CHAPTER_NO));
            int fromVerseNo = cursor.getInt(cursor.getColumnIndexOrThrow(BookmarkContract.BookmarkEntry.COL_FROM_VERSE_NO));
            int toVerseNo = cursor.getInt(cursor.getColumnIndexOrThrow(BookmarkContract.BookmarkEntry.COL_TO_VERSE_NO));
            String date = cursor.getString(cursor.getColumnIndexOrThrow(BookmarkContract.BookmarkEntry.COL_DATETIME));
            String note = cursor.getString(cursor.getColumnIndexOrThrow(BookmarkContract.BookmarkEntry.COL_NOTE));

            model = new BookmarkModel(id, chapterNo, fromVerseNo, toVerseNo, date);
            model.setNote(note);
        }
        cursor.close();

        return model;
    }

    public ArrayList<BookmarkModel> getBookmarks() {
        SQLiteDatabase db = getReadableDatabase();

        String sortOrder = BaseColumns._ID + " DESC";

        ArrayList<BookmarkModel> verses = new ArrayList<>();

        Cursor cursor = db.query(BookmarkContract.BookmarkEntry.TABLE_NAME, null, null, null, null, null, sortOrder);
        while (cursor.moveToNext()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(BaseColumns._ID));
            int chapterNo = cursor.getInt(cursor.getColumnIndexOrThrow(BookmarkContract.BookmarkEntry.COL_CHAPTER_NO));
            int fromVerseNo = cursor.getInt(cursor.getColumnIndexOrThrow(BookmarkContract.BookmarkEntry.COL_FROM_VERSE_NO));
            int toVerseNo = cursor.getInt(cursor.getColumnIndexOrThrow(BookmarkContract.BookmarkEntry.COL_TO_VERSE_NO));
            String date = cursor.getString(cursor.getColumnIndexOrThrow(BookmarkContract.BookmarkEntry.COL_DATETIME));
            String note = cursor.getString(cursor.getColumnIndexOrThrow(BookmarkContract.BookmarkEntry.COL_NOTE));

            BookmarkModel model = new BookmarkModel(id, chapterNo, fromVerseNo, toVerseNo, date);
            model.setNote(note);
            verses.add(model);
        }
        cursor.close();

        return verses;
    }
}
