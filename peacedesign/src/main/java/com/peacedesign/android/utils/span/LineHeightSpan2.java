/**
 * Author: le cheng
 * Whatsapp: +4407803311518
 * Email: lecheng2019@gmail.com
 */

package com.peacedesign.android.utils.span;

import android.graphics.Paint;
import android.text.style.LineHeightSpan;

public class LineHeightSpan2 implements LineHeightSpan {
    private final int mHeight;
    private final boolean mIncludeTop;
    private final boolean mIncludeBottom;

    public LineHeightSpan2(int height) {
        this(height, true);
    }

    public LineHeightSpan2(int height, boolean includeVertical) {
        this(height, includeVertical, includeVertical);
    }

    public LineHeightSpan2(int height, boolean includeTop, boolean includeBottom) {
        mHeight = height;
        mIncludeTop = includeTop;
        mIncludeBottom = includeBottom;
    }

    @Override
    public void chooseHeight(CharSequence text, int start, int end, int spanstartv, int v,
                             Paint.FontMetricsInt fm) {
        if (mIncludeTop) {
            fm.top -= mHeight;
            fm.ascent -= mHeight;
        }
        if (mIncludeBottom) {
            fm.bottom += mHeight;
            fm.descent += mHeight;
        }
    }
}