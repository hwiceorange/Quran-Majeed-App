/**
 * Author: Rai Adnan
 * Whatsapp: +923002375907
 * Email: officialshaheendevelopers@gmail.com
 * Portfolio: https://codecanyon.net/user/shaheendevelopers/portfolio
 */

package com.quran.quranaudio.online.quran_module.interfaceUtils;

import com.quran.quranaudio.online.quran_module.components.readHistory.ReadHistoryModel;

public interface ReadHistoryCallbacks {
    void onReadHistoryRemoved(ReadHistoryModel model);

    void onReadHistoryAdded(ReadHistoryModel model);
}
