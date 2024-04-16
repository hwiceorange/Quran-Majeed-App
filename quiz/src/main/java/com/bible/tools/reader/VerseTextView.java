package com.bible.tools.reader;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;


public class VerseTextView extends AppCompatTextView {
    public static final String TAG = VerseTextView.class.getSimpleName();

    public boolean isChecked;
    Rect rect = new Rect();
    Paint paint = new Paint();

    public VerseTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private int color;

    public void setPaintFilterColor(int color) {
        if (color != 0) {
            this.color = color ;//ColorUtil.getColorWithAlpha(S.verseHighlightAlpha, color);
        } else {
            this.color = color;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {

        if (getLayout() == null) {
            return;
        }
        //夜间与白天颜色不一样
        paint.setColor(color);
        if (color == 0) {
            canvas.drawColor(color);
        } else {
            int lineCount = getLineCount();
            drawColorBg(canvas, lineCount);
        }

        super.onDraw(canvas);
    }

    private void drawColorBg(Canvas canvas, int lineCount) {
        for (int i = 0; i < lineCount; i++) {
            rect.top = (getLayout().getLineTop(i));
            rect.left = (int) getLayout().getLineLeft(i);
            rect.right = (int) getLayout().getLineRight(i);
            rect.bottom = getLayout().getLineBottom(i) - ((i + 1 == lineCount) ? 0 : dpToPx(getContext(), 3));
            canvas.drawRect(rect, paint);
        }
    }
    public static int dpToPx(Context ctx, int dp) {
        if (ctx == null) {
            return 0;
        }
        final float scale = ctx.getResources().getDisplayMetrics().density;
        return Math.round(dp * scale);
    }
}
