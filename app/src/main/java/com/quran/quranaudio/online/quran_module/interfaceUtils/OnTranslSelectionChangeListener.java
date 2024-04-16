/**
 * Author: Rai Adnan
 * Whatsapp: +923002375907
 * Email: officialshaheendevelopers@gmail.com
 * Portfolio: https://codecanyon.net/user/shaheendevelopers/portfolio
 */

package com.quran.quranaudio.online.quran_module.interfaceUtils;

import android.content.Context;

public interface OnTranslSelectionChangeListener<R> {
    boolean onSelectionChanged(Context ctx, R r, boolean isSelected);
}