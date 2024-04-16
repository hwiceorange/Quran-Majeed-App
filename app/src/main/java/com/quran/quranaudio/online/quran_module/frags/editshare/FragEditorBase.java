/**
 * Author: Rai Adnan
 * Whatsapp: +923002375907
 * Email: officialshaheendevelopers@gmail.com
 * Portfolio: https://codecanyon.net/user/shaheendevelopers/portfolio
 */

package com.quran.quranaudio.online.quran_module.frags.editshare;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.quran.quranaudio.online.quran_module.components.editor.VerseEditor;
import com.quran.quranaudio.online.R;
import com.quran.quranaudio.online.quran_module.frags.BaseFragment;
import com.quran.quranaudio.online.quran_module.utils.extensions.ContextKt;
import com.quran.quranaudio.online.quran_module.utils.extensions.ViewPaddingKt;

public class FragEditorBase extends BaseFragment {
    protected VerseEditor mVerseEditor;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Context context = view.getContext();

        view.setBackgroundColor(ContextKt.color(context, R.color.colorBGPage));
        ViewPaddingKt.updatePaddingVertical(view, ContextKt.dp2px(context, 10),
            ContextKt.getDimenPx(context, R.dimen.dmnPadBig));
    }

    public void setEditor(VerseEditor verseEditor) {
        mVerseEditor = verseEditor;
    }
}
