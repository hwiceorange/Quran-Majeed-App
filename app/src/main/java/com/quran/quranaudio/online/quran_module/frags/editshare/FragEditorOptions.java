/**
 * Author: Rai Adnan
 * Whatsapp: +923002375907
 * Email: officialshaheendevelopers@gmail.com
 * Portfolio: https://codecanyon.net/user/shaheendevelopers/portfolio
 */

package com.quran.quranaudio.online.quran_module.frags.editshare;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import com.quran.quranaudio.online.quran_module.widgets.checkbox.PeaceCheckBox;
import com.quran.quranaudio.online.quran_module.widgets.compound.PeaceCompoundButton;
import com.quran.quranaudio.online.R;
import com.quran.quranaudio.online.quran_module.utils.extensions.ContextKt;
import com.quran.quranaudio.online.quran_module.utils.extensions.ViewPaddingKt;
import com.quran.quranaudio.online.quran_module.widgets.checkbox.PeaceCheckBox;
import com.quran.quranaudio.online.quran_module.widgets.compound.PeaceCompoundButton;

import kotlin.Unit;
import kotlin.jvm.functions.Function2;

public class FragEditorOptions extends FragEditorBase {
    private boolean mInitShowAr = true;
    private boolean mInitShowTrans = true;
    private boolean mInitShowRef = true;
    private PeaceCheckBox mIncludeAr;
    private PeaceCheckBox mIncludeTrans;
    private PeaceCheckBox mIncludeRef;

    public static FragEditorOptions newInstance() {
        return new FragEditorOptions();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        NestedScrollView scrollView = new NestedScrollView(inflater.getContext());
        scrollView.setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));

        LinearLayout layout = new LinearLayout(inflater.getContext());
        layout.setOrientation(LinearLayout.VERTICAL);

        scrollView.addView(layout, new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        return scrollView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init((LinearLayout) ((ViewGroup) view).getChildAt(0));
    }

    private void init(LinearLayout container) {
        mIncludeAr = makeCheckbox(container, R.string.strLabelIncludeArabic,
            (buttonView, isChecked) -> dispatchOptions(isChecked, mIncludeTrans.isChecked(), mIncludeRef.isChecked()));
        mIncludeTrans = makeCheckbox(container, R.string.strLabelIncludeTranslation,
            (buttonView, isChecked) -> dispatchOptions(mIncludeAr.isChecked(), isChecked, mIncludeRef.isChecked()));
        mIncludeRef = makeCheckbox(container, R.string.strLabelIncludeReference,
            (buttonView, isChecked) -> dispatchOptions(mIncludeAr.isChecked(), mIncludeTrans.isChecked(), isChecked));
        setOptionsVisibility(mInitShowAr, mInitShowTrans, mInitShowRef);
    }

    private PeaceCheckBox makeCheckbox(LinearLayout container, int txtRes, Function2<PeaceCompoundButton, Boolean, Unit> l) {
        Context context = container.getContext();

        PeaceCheckBox checkBox = new PeaceCheckBox(context);
        checkBox.setTextAppearance(R.style.TextAppearanceCommonTitle);
        checkBox.setBackgroundResource(R.drawable.dr_bg_action);
        ViewPaddingKt.updatePaddings(checkBox, ContextKt.dp2px(context, 15), ContextKt.dp2px(context, 10));
        checkBox.setText(txtRes);

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        p.bottomMargin = ContextKt.dp2px(context, 5);
        container.addView(checkBox, p);

        checkBox.setChecked(false);
        checkBox.setOnCheckChangedListener(l);

        return checkBox;
    }

    public void setOptionsVisibility(boolean showAr, boolean showTrans, boolean showRef) {
        mInitShowAr = showAr;
        mInitShowTrans = showTrans;
        mInitShowRef = showRef;

        if (mIncludeAr == null || mIncludeTrans == null || mIncludeRef == null) {
            return;
        }

        // if mIncludeAr check state is different then options are dispatched via the check listener.
        // And if mIncludeAr check state is same then options are dispatched via dispatchOptions().
        // We are calling dispatchOptions() only in first logic, because in either of the above cases,
        // options are ultimately dispatched.

        if (mIncludeAr.isChecked() != showAr) {
            mIncludeAr.setChecked(showAr);
        } else {
            dispatchOptions(showAr, showTrans, showRef);
        }

        mIncludeTrans.setChecked(showTrans);
        mIncludeRef.setChecked(showRef);
    }

    private Unit dispatchOptions(boolean showAr, boolean showTrans, boolean showRef) {
        if (mVerseEditor == null) {
            return Unit.INSTANCE;
        }

        mVerseEditor.getListener().onOptionChange(showAr, showTrans, showRef);

        return Unit.INSTANCE;
    }
}
