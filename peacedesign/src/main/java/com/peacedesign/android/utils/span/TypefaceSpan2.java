/**
 * Author: le cheng
 * Whatsapp: +4407803311518
 * Email: lecheng2019@gmail.com
 */

package com.peacedesign.android.utils.span;

import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.style.MetricAffectingSpan;

public class TypefaceSpan2 extends MetricAffectingSpan {
    private final Typeface typeface;

    public TypefaceSpan2(Typeface typeface) {
        this.typeface = typeface;
    }

    @Override
    public void updateDrawState(TextPaint drawState) {
        apply(drawState);
    }

    @Override
    public void updateMeasureState(TextPaint paint) {
        apply(paint);
    }

    private void apply(Paint paint) {
        Typeface oldTypeface = paint.getTypeface();
        int oldStyle = oldTypeface != null ? oldTypeface.getStyle() : 0;
        int fakeStyle = oldStyle & ~typeface.getStyle();

        if ((fakeStyle & Typeface.BOLD) != 0) {
            paint.setFakeBoldText(true);
        }

        if ((fakeStyle & Typeface.ITALIC) != 0) {
            paint.setTextSkewX(-0.25f);
        }

        paint.setTypeface(typeface);
    }
}