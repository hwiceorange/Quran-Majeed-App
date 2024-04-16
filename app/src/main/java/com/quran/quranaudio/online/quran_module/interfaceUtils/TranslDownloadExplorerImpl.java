/**
 * Author: Rai Adnan
 * Whatsapp: +923002375907
 * Email: officialshaheendevelopers@gmail.com
 * Portfolio: https://codecanyon.net/user/shaheendevelopers/portfolio
 */
package com.quran.quranaudio.online.quran_module.interfaceUtils;

import android.view.View;

import com.quran.quranaudio.online.quran_module.adapters.transl.ADPDownloadTranslations;
import com.quran.quranaudio.online.quran_module.components.transls.TranslModel;

public interface TranslDownloadExplorerImpl {
    void onDownloadAttempt(ADPDownloadTranslations adapter, ADPDownloadTranslations.VHDownloadTransl vhTransl, View referencedView, TranslModel model);
}
