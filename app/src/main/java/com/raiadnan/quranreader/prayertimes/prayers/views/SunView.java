package com.raiadnan.quranreader.prayertimes.prayers.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import com.raiadnan.quranreader.R;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SunView extends View {
    private static final String DATE_FORMAT = "HH:mm";
    private boolean hideWeatherDrawble = false;
    private int mArcColor;
    private float mArcDashGapWidth;
    private float mArcDashHeight;
    private float mArcDashWidth;
    private float mArcOffsetAngle;
    private float mArcRadius;
    private int mArcSolidColor;
    private int mBottomLineColor;
    private float mBottomLineHeight;
    private String mCurrentTime;
    private SimpleDateFormat mDateFormat;
    private float mDefaultWeatherIconSize;
    private String mEndTime;
    private Paint mPaint;
    private String mStartTime;
    private int mSunColor;
    private float mTextPadding;
    private int mTimeTextColor;
    private float mTimeTextSize;
    private Drawable mWeatherDrawable;
    private float offsetX;
    private float offsetY;

    public SunView(Context context) {
        super(context);
        init();
    }

    public SunView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        initAttrs(context, attributeSet);
    }

    public SunView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        initAttrs(context, attributeSet);
    }

    public void setArcSolidColor(int i) {
        this.mArcSolidColor = i;
    }

    public void setArcColor(int i) {
        this.mArcColor = i;
    }

    public void setBottomLineColor(int i) {
        this.mBottomLineColor = i;
    }

    public void setTimeTextColor(int i) {
        this.mTimeTextColor = i;
    }

    public void setSunColor(int i) {
        this.mSunColor = i;
    }

    @SuppressLint("ResourceType")
    private void initAttrs(Context context, AttributeSet attributeSet) {
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.WeatherViewStyle);
        this.mStartTime = obtainStyledAttributes.getString(11);
        this.mEndTime = obtainStyledAttributes.getString(10);
        this.mCurrentTime = obtainStyledAttributes.getString(9);
        this.mTimeTextSize = obtainStyledAttributes.getDimension(15, getResources().getDimension(com.raiadnan.quranreader.R.dimen.default_text_size));
        this.mTimeTextColor = obtainStyledAttributes.getColor(14, getResources().getColor(com.raiadnan.quranreader.R.color.default_text_color));
        this.mWeatherDrawable = obtainStyledAttributes.getDrawable(16);
        this.mBottomLineHeight = obtainStyledAttributes.getDimension(8, getResources().getDimension(com.raiadnan.quranreader.R.dimen.default_bottom_line_height));
        this.mBottomLineColor = obtainStyledAttributes.getColor(7, getResources().getColor(com.raiadnan.quranreader.R.color.default_bottom_line_color));
        this.mArcColor = obtainStyledAttributes.getColor(0, getResources().getColor(com.raiadnan.quranreader.R.color.default_arc_color));
        this.mArcSolidColor = obtainStyledAttributes.getColor(6, getResources().getColor(R.color.sun_highlight));
        this.mArcDashWidth = obtainStyledAttributes.getDimension(3, getResources().getDimension(com.raiadnan.quranreader.R.dimen.default_arc_dash_width));
        this.mArcDashGapWidth = obtainStyledAttributes.getDimension(1, getResources().getDimension(com.raiadnan.quranreader.R.dimen.default_arc_dash_gap_width));
        this.mArcDashHeight = obtainStyledAttributes.getDimension(2, getResources().getDimension(com.raiadnan.quranreader.R.dimen.default_arc_dash_height));
        this.mArcRadius = obtainStyledAttributes.getDimension(5, 0.0f);
        this.mArcOffsetAngle = (float) obtainStyledAttributes.getInteger(4, 0);
        this.mSunColor = obtainStyledAttributes.getColor(12, getResources().getColor(com.raiadnan.quranreader.R.color.default_sun_color));
        this.mTextPadding = obtainStyledAttributes.getDimension(13, 0.0f);
        obtainStyledAttributes.recycle();
        this.mDefaultWeatherIconSize = getResources().getDimension(com.raiadnan.quranreader.R.dimen.default_weather_icon_size);
        this.mPaint = new Paint();
        this.mPaint.setAntiAlias(true);
        setDefaultTime();
    }

    public void setHideWeatherDrawble(boolean z) {
        this.hideWeatherDrawble = z;
    }

    private void init() {
        this.mPaint = new Paint();
        this.mPaint.setAntiAlias(true);
        this.mTimeTextSize = getResources().getDimension(com.raiadnan.quranreader.R.dimen.default_text_size);
        this.mTimeTextColor = getResources().getColor(com.raiadnan.quranreader.R.color.default_text_color);
        this.mBottomLineHeight = getResources().getDimension(com.raiadnan.quranreader.R.dimen.default_bottom_line_height);
        this.mBottomLineColor = getResources().getColor(com.raiadnan.quranreader.R.color.default_bottom_line_color);
        this.mArcColor = getResources().getColor(com.raiadnan.quranreader.R.color.default_arc_color);
        this.mArcSolidColor = getResources().getColor(R.color.sun_highlight);
        this.mArcDashWidth = getResources().getDimension(com.raiadnan.quranreader.R.dimen.default_arc_dash_width);
        this.mArcDashGapWidth = getResources().getDimension(com.raiadnan.quranreader.R.dimen.default_arc_dash_gap_width);
        this.mArcDashHeight = getResources().getDimension(com.raiadnan.quranreader.R.dimen.default_arc_dash_height);
        this.mDefaultWeatherIconSize = getResources().getDimension(com.raiadnan.quranreader.R.dimen.default_weather_icon_size);
        this.mSunColor = getResources().getColor(com.raiadnan.quranreader.R.color.default_sun_color);
        setDefaultTime();
    }

    public void setStartTime(String str) {
        this.mStartTime = str;
    }

    public void setEndTime(String str) {
        this.mEndTime = str;
    }

    public void setCurrentTime(String str) {
        String[] split = this.mStartTime.split(":");
        String[] split2 = str.split(":");
        if ((Integer.parseInt(split2[0]) * 60) + Integer.parseInt(split2[1]) < (Integer.parseInt(split[0]) * 60) + Integer.parseInt(split[1])) {
            this.mArcSolidColor = getResources().getColor(R.color.sun_highlight);
            str = this.mStartTime;
        }
        this.mCurrentTime = str;
    }

    private void setDefaultTime() {
        this.mDateFormat = new SimpleDateFormat(DATE_FORMAT);
        if (TextUtils.isEmpty(this.mStartTime)) {
            this.mStartTime = getResources().getString(com.raiadnan.quranreader.R.string.default_start_time);
        }
        if (TextUtils.isEmpty(this.mEndTime)) {
            this.mEndTime = getResources().getString(com.raiadnan.quranreader.R.string.default_end_time);
        }
        if (TextUtils.isEmpty(this.mCurrentTime)) {
            this.mCurrentTime = this.mDateFormat.format(new Date());
        }
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int i, int i2) {
        int i3;
        int i4;
        int mode = MeasureSpec.getMode(i);
        int mode2 = MeasureSpec.getMode(i2);
        int size = MeasureSpec.getSize(i);
        int size2 = MeasureSpec.getSize(i2);
        if (mode == Integer.MIN_VALUE && mode2 == Integer.MIN_VALUE) {
            float f = this.mArcRadius;
            if (f == 0.0f) {
                setMeasuredDimension(size, size2);
            } else {
                setMeasuredDimension((int) ((f * 2.0f) + ((float) getWidthGap())), (int) (this.mArcRadius + ((float) getHeightGap())));
            }
        } else if (mode == Integer.MIN_VALUE) {
            float f2 = this.mArcRadius;
            if (f2 == 0.0f) {
                i4 = ((size2 - getHeightGap()) * 2) + getWidthGap();
            } else {
                i4 = (int) ((f2 * 2.0f) + ((float) getWidthGap()));
            }
            setMeasuredDimension(i4, size2);
        } else if (mode2 == Integer.MIN_VALUE) {
            float f3 = this.mArcRadius;
            if (f3 == 0.0f) {
                i3 = ((size - getWidthGap()) / 2) + getHeightGap();
            } else {
                i3 = (int) (f3 + ((float) getHeightGap()));
            }
            setMeasuredDimension(size, i3);
        } else {
            setMeasuredDimension(size, size2);
        }
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (getBackground() != null) {
            getBackground().draw(canvas);
        } else {
            canvas.drawColor(-1);
        }
        drawLine(canvas);
        drawArc(canvas);
    }

    private void drawArc(Canvas canvas) {
        getRadius();
        this.mPaint.setStyle(Paint.Style.STROKE);
        this.mPaint.setColor(this.mArcColor);
        this.mPaint.setStrokeWidth(this.mArcDashHeight);
        float paddingLeft = ((float) getPaddingLeft()) + ((((float) ((getWidth() - getPaddingLeft()) - getPaddingRight())) - (this.mArcRadius * 2.0f)) / 2.0f);
        float height = (((float) getHeight()) - this.mArcRadius) - ((float) getBottomHeightGap());
        float f = this.mArcRadius;
        RectF rectF = new RectF(paddingLeft, height, (f * 2.0f) + paddingLeft, (f * 2.0f) + height);
        PointF calcArcEndPointXY = calcArcEndPointXY(rectF.centerX(), rectF.centerY(), rectF.width() / 2.0f, this.mArcOffsetAngle + 180.0f);
        this.offsetX = calcArcEndPointXY.x - paddingLeft;
        this.offsetY = rectF.centerY() - calcArcEndPointXY.y;
        rectF.top += this.offsetY;
        rectF.bottom += this.offsetY;
        float f2 = this.mArcDashWidth;
        float f3 = this.mArcDashGapWidth;
        this.mPaint.setPathEffect(new DashPathEffect(new float[]{f2, f3, f2, f3}, 0.0f));
        float f4 = this.mArcOffsetAngle;
        canvas.drawArc(rectF, f4 + 180.0f, 180.0f - (f4 * 2.0f), false, this.mPaint);
        drawSolidArc(canvas, rectF);
    }

    private void getRadius() {
        if (this.mArcRadius == 0.0f) {
            int width = getWidth() - getWidthGap();
            int height = getHeight() - getHeightGap();
            int i = width / 2;
            if (i > height) {
                this.mArcRadius = (float) height;
            } else {
                this.mArcRadius = (float) i;
            }
        }
    }

    private void drawWeatherDrawable(Canvas canvas, PointF pointF) {
        if (!this.hideWeatherDrawble) {
            Drawable drawable = this.mWeatherDrawable;
            if (drawable != null) {
                int intrinsicWidth = drawable.getIntrinsicWidth() == 0 ? (int) this.mDefaultWeatherIconSize : this.mWeatherDrawable.getIntrinsicWidth();
                int intrinsicHeight = this.mWeatherDrawable.getIntrinsicHeight() == 0 ? (int) this.mDefaultWeatherIconSize : this.mWeatherDrawable.getIntrinsicHeight();
                Rect rect = new Rect();
                rect.left = (int) (pointF.x - ((float) (intrinsicWidth / 2)));
                rect.top = (int) (pointF.y - ((float) (intrinsicHeight / 2)));
                rect.right = rect.left + intrinsicWidth;
                rect.bottom = rect.top + intrinsicHeight;
                this.mWeatherDrawable.setBounds(rect);
                this.mWeatherDrawable.draw(canvas);
                return;
            }
            drawSun(canvas, pointF);
        }
    }

    private void drawSun(Canvas canvas, PointF pointF) {
        this.mPaint.setColor(this.mSunColor);
        this.mPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(pointF.x, pointF.y, this.mDefaultWeatherIconSize / 2.0f, this.mPaint);
        this.mPaint.setStyle(Paint.Style.STROKE);
        this.mPaint.setStrokeWidth(this.mDefaultWeatherIconSize / 3.0f);
        double d = (double) (this.mDefaultWeatherIconSize * 2.0f);
        Double.isNaN(d);
        float f = (((float) (d * 3.141592653589793d)) - 60.0f) / 12.0f;
        float f2 = (float) 3;
        this.mPaint.setPathEffect(new DashPathEffect(new float[]{f2, f, f2, f}, 0.0f));
        canvas.drawCircle(pointF.x, pointF.y, this.mDefaultWeatherIconSize, this.mPaint);
    }

    private void drawSolidArc(Canvas canvas, RectF rectF) {
        int i;
        try {
            long time = this.mDateFormat.parse(this.mStartTime).getTime();
            float time2 = (((float) (this.mDateFormat.parse(this.mCurrentTime).getTime() - time)) * 1.0f) / ((float) (this.mDateFormat.parse(this.mEndTime).getTime() - time));
            if (time2 > 1.0f) {
                i = (int) (180.0f - (this.mArcOffsetAngle * 2.0f));
            } else {
                i = (int) (time2 * (180.0f - (this.mArcOffsetAngle * 2.0f)));
            }
        } catch (ParseException e) {
            e.printStackTrace();
            i = 0;
        }
        this.mPaint.setStyle(Paint.Style.FILL);
        this.mPaint.setColor(this.mArcSolidColor);
        float f = (float) i;
        canvas.drawArc(rectF, this.mArcOffsetAngle + 180.0f, f, false, this.mPaint);
        PointF calcArcEndPointXY = calcArcEndPointXY(rectF.centerX(), rectF.centerY(), rectF.width() / 2.0f, this.mArcOffsetAngle + 180.0f + f);
        drawTriangle(canvas, rectF, calcArcEndPointXY);
        drawWeatherDrawable(canvas, calcArcEndPointXY);
        drawText(canvas, rectF);
    }

    private void drawText(Canvas canvas, RectF rectF) {
        this.mPaint.setColor(this.mTimeTextColor);
        this.mPaint.setTextSize(this.mTimeTextSize);
        int textWidth = getTextWidth(this.mPaint, this.mStartTime);
        int textWidth2 = getTextWidth(this.mPaint, this.mEndTime);
        this.mPaint.setPathEffect(null);
        this.mPaint.setStyle(Paint.Style.FILL);
        float textHeight = (float) (getTextHeight() + 15);
        canvas.drawText(this.mStartTime, (rectF.left - ((float) (textWidth / 2))) + this.offsetX, (rectF.centerY() - this.offsetY) + textHeight + this.mTextPadding, this.mPaint);
        canvas.drawText(this.mEndTime, ((rectF.right - ((float) (textWidth2 / 2))) - 2.0f) - (this.offsetX * 2.0f), (rectF.centerY() - this.offsetY) + textHeight + this.mTextPadding, this.mPaint);
    }

    private int getTextHeight() {
        this.mPaint.setTextSize(this.mTimeTextSize);
        Paint.FontMetrics fontMetrics = this.mPaint.getFontMetrics();
        return (int) Math.ceil((double) (fontMetrics.descent - fontMetrics.ascent));
    }

    private void drawTriangle(Canvas canvas, RectF rectF, PointF pointF) {
        Path path = new Path();
        path.moveTo((rectF.left - 1.0f) + this.offsetX, rectF.centerY() - this.offsetY);
        path.lineTo(pointF.x - 1.0f, pointF.y - 1.0f);
        path.lineTo(pointF.x - 1.0f, rectF.centerY() - this.offsetY);
        path.close();
        canvas.drawPath(path, this.mPaint);
    }

    private void drawLine(Canvas canvas) {
        this.mPaint.setColor(this.mBottomLineColor);
        this.mPaint.setStrokeWidth(this.mBottomLineHeight);
        this.mPaint.setStyle(Paint.Style.FILL);
        canvas.drawLine((float) getPaddingLeft(), (float) (getHeight() - getBottomHeightGap()), (float) (getWidth() - getPaddingRight()), (float) (getHeight() - getBottomHeightGap()), this.mPaint);
    }

    private int getWidthGap() {
        return getPaddingLeft() + getPaddingRight() + (getTextWidth(this.mStartTime) / 2) + (getTextWidth(this.mEndTime) / 2);
    }

    private int getHeightGap() {
        return ((int) (((float) (getPaddingTop() + getPaddingBottom())) + this.mTextPadding + this.mBottomLineHeight + ((float) (getWeatherHeight() / 2)))) + getTextHeight();
    }

    private int getBottomHeightGap() {
        return ((int) (((float) (getPaddingBottom() + getTextHeight())) + this.mTextPadding)) + 10;
    }

    private int getWeatherHeight() {
        Drawable drawable = this.mWeatherDrawable;
        if (drawable == null) {
            return ((int) this.mDefaultWeatherIconSize) * 2;
        }
        if (drawable.getIntrinsicHeight() == 0) {
            return ((int) this.mDefaultWeatherIconSize) * 2;
        }
        return this.mWeatherDrawable.getIntrinsicHeight();
    }

    private int getWeatherWidth() {
        Drawable drawable = this.mWeatherDrawable;
        if (drawable == null) {
            return ((int) this.mDefaultWeatherIconSize) * 2;
        }
        if (drawable.getIntrinsicWidth() == 0) {
            return ((int) this.mDefaultWeatherIconSize) * 2;
        }
        return this.mWeatherDrawable.getIntrinsicWidth();
    }

    public PointF calcArcEndPointXY(float f, float f2, float f3, float f4) {
        PointF pointF = new PointF();
        double d = (double) f4;
        Double.isNaN(d);
        float f5 = (float) ((d * 3.141592653589793d) / 180.0d);
        if (f4 < 90.0f) {
            double d2 = (double) f5;
            pointF.x = f + (((float) Math.cos(d2)) * f3);
            pointF.y = f2 + (((float) Math.sin(d2)) * f3);
        } else if (f4 == 90.0f) {
            pointF.x = f;
            pointF.y = f2 + f3;
        } else if (f4 > 90.0f && f4 < 180.0f) {
            double d3 = (double) (180.0f - f4);
            Double.isNaN(d3);
            double d4 = (double) ((float) ((d3 * 3.141592653589793d) / 180.0d));
            pointF.x = f - (((float) Math.cos(d4)) * f3);
            pointF.y = f2 + (((float) Math.sin(d4)) * f3);
        } else if (f4 == 180.0f) {
            pointF.x = f - f3;
            pointF.y = f2;
        } else if (f4 > 180.0f && f4 < 270.0f) {
            double d5 = (double) (f4 - 180.0f);
            Double.isNaN(d5);
            double d6 = (double) ((float) ((d5 * 3.141592653589793d) / 180.0d));
            pointF.x = f - (((float) Math.cos(d6)) * f3);
            pointF.y = f2 - (((float) Math.sin(d6)) * f3);
        } else if (f4 == 270.0f) {
            pointF.x = f;
            pointF.y = f2 - f3;
        } else {
            double d7 = (double) (360.0f - f4);
            Double.isNaN(d7);
            double d8 = (double) ((float) ((d7 * 3.141592653589793d) / 180.0d));
            pointF.x = f + (((float) Math.cos(d8)) * f3);
            pointF.y = f2 - (((float) Math.sin(d8)) * f3);
        }
        return pointF;
    }

    public int getTextWidth(Paint paint, String str) {
        if (str == null || str.length() <= 0) {
            return 0;
        }
        int length = str.length();
        float[] fArr = new float[length];
        paint.getTextWidths(str, fArr);
        int i = 0;
        for (int i2 = 0; i2 < length; i2++) {
            i += (int) Math.ceil((double) fArr[i2]);
        }
        return i;
    }

    public int getTextWidth(String str) {
        this.mPaint.setTextSize(this.mTimeTextSize);
        return getTextWidth(this.mPaint, str);
    }
}
