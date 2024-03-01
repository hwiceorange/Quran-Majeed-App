

package com.quran.quranaudio.online.quran_module.interfaceUtils;

import com.quran.quranaudio.online.quran_module.components.readHistory.ReadHistoryModel;

public interface ReadHistoryCallbacks {
    void onReadHistoryRemoved(ReadHistoryModel model);

    void onReadHistoryAdded(ReadHistoryModel model);
}
