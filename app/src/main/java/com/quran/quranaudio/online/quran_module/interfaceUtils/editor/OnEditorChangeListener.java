

package com.quran.quranaudio.online.quran_module.interfaceUtils.editor;

import com.quran.quranaudio.online.quran_module.components.editor.EditorBG;
import com.quran.quranaudio.online.quran_module.components.editor.EditorFG;
import com.quran.quranaudio.online.quran_module.components.quran.subcomponents.TranslationBook;

public interface OnEditorChangeListener {
    void onBGChange(EditorBG nBg);

    void onBGAlphaColorChange(int alphaColor);

    void onFGChange(EditorFG nFg);

    void onArSizeChange(float mult);

    void onTranslSizeChange(float mult);

    void onOptionChange(boolean showAr, boolean showTrans, boolean showRef);

    void onTranslChange(TranslationBook book);
}