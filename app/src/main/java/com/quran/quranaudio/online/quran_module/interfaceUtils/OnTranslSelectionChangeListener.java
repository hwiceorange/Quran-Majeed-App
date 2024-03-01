

package com.quran.quranaudio.online.quran_module.interfaceUtils;

import android.content.Context;

public interface OnTranslSelectionChangeListener<R> {
    boolean onSelectionChanged(Context ctx, R r, boolean isSelected);
}