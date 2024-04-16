/**
 * Author: Rai Adnan
 * Whatsapp: +923002375907
 * Email: officialshaheendevelopers@gmail.com
 * Portfolio: https://codecanyon.net/user/shaheendevelopers/portfolio
 */

package com.quran.quranaudio.online.quran_module.widgets.chapterCard;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ChapterCardWithoutIcon extends ChapterCard {
    public ChapterCardWithoutIcon(@NonNull Context context) {
        super(context);
    }

    @Nullable
    @Override
    protected View createRightView() {
        return null;
    }

    @Nullable
    @Override
    protected View createFavIcon() {
        return null;
    }
}
