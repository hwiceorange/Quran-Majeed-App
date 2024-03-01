

package com.quran.quranaudio.online.quran_module.interfaceUtils;

import com.quran.quranaudio.online.quran_module.components.bookmark.BookmarkModel;

public interface BookmarkCallbacks {
    void onBookmarkRemoved(BookmarkModel model);

    default void onBookmarkAdded(BookmarkModel model) {}

    default void onBookmarkUpdated(BookmarkModel model) {}
}
