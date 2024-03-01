/**
 * Author: le cheng
 * Whatsapp: +4407803311518
 * Email: lecheng2019@gmail.com
 */

package com.peacedesign.android.utils.span;

import android.text.TextPaint;
import android.text.style.SuperscriptSpan;

import androidx.annotation.NonNull;

public class SuperscriptSpan2 extends SuperscriptSpan {
    @Override
    public void updateDrawState(@NonNull TextPaint textPaint) {
        textPaint.baselineShift += (int) ((textPaint.ascent()) / 3);
    }

    @Override
    public void updateMeasureState(@NonNull TextPaint textPaint) {
        textPaint.baselineShift += (int) ((textPaint.ascent()) / 3);
    }
}
