package com.quran.quranaudio.quiz.utils;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.View;

import com.quran.quranaudio.quiz.R;

import androidx.annotation.Nullable;


/**
 * @author yangdongke
 * 圆形进度条
 */
public class CircleProgress extends View {

    private Paint mPaint;
    private float progressPercent;
    private float radius;
    private RectF rectF;
    private int bgColor, progressColor;
    private int startColor,midColor, endColor;
    private SweepGradient gradient;
    private boolean isGradient;
    //是否逆时针展示进度
    private boolean isReverse = false;

    public CircleProgress(Context context) {
        this(context,null);
    }

    public CircleProgress(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public CircleProgress(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CircleProgress);
        bgColor = typedArray.getColor(R.styleable.CircleProgress_circleBgColor, getResources().getColor(R.color.white));
        progressColor = typedArray.getColor(R.styleable.CircleProgress_circleProgressColor, getResources().getColor(R.color.color_0BC9B2));
        radius = typedArray.getDimension(R.styleable.CircleProgress_circleRadius, getResources().getDimension(R.dimen.dp_4));
        isGradient = typedArray.getBoolean(R.styleable.CircleProgress_circleIsGradient, false);
        isReverse = typedArray.getBoolean(R.styleable.CircleProgress_circleIsReverse, false);
        startColor = typedArray.getColor(R.styleable.CircleProgress_circleStartColor, getResources().getColor(R.color.color_0BC9B2));
        midColor = typedArray.getColor(R.styleable.CircleProgress_circleMidColor, 0);
        endColor = typedArray.getColor(R.styleable.CircleProgress_circleEndColor, getResources().getColor(R.color.color_0BC9B2));
        typedArray.recycle();

        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setAntiAlias(true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        //自定义的View能够使用wrap_content或者是match_parent的属性
        setMeasuredDimension(getMeasuredWidth(), getMeasuredWidth());
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (midColor == 0) {
            gradient = new SweepGradient(getWidth() / 2f, getHeight() / 2f,
                    new int[]{startColor, endColor},
                    null);
        } else {
            gradient = new SweepGradient(getWidth() / 2f, getHeight() / 2f,
                    new int[]{startColor, midColor, endColor},
                    null);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // 1、绘制背景灰色圆环
        float centerX = (float) (getWidth() / 2.0);
        mPaint.setShader(null);//必须设置为null，否则背景也会加上渐变色
        mPaint.setStrokeWidth(radius); //设置画笔的大小
        mPaint.setColor(bgColor);
        canvas.drawCircle(centerX, centerX, centerX - radius / 2, mPaint);
        // 2、绘制比例弧
        if (rectF == null) {//外切正方形
            rectF = new RectF(radius / 2, radius / 2, 2 * centerX - radius / 2, 2 * centerX - radius / 2);
        }
        //3、是否绘制渐变色
        if (isGradient) {
            mPaint.setShader(gradient);//设置线性渐变
        } else {
            mPaint.setColor(progressColor);
        }
        canvas.save();
        canvas.rotate(-90, getWidth() / 2f, getHeight() / 2f);
        //Paint.Cap.ROUND 与 SweepGradient 混用导致异常问题处理
        float angleOffset =
                (float) Math.toDegrees(Math.asin(radius / 2f / (centerX - radius / 2)));
        if (isReverse) {
            canvas.drawArc(rectF, -angleOffset, -3.6f * progressPercent, false, mPaint);//画比例圆弧
        } else {
            canvas.drawArc(rectF, angleOffset, 3.6f * progressPercent, false, mPaint);//画比例圆弧
        }
        canvas.restore();
    }

    public void setPercentage(float percentage) {
        this.progressPercent = percentage;
        invalidate();
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public void setBgColor(int bgColor) {
        this.bgColor = bgColor;
    }

    public void setProgressColor(int progressColor) {
        this.progressColor = progressColor;
    }

    public void setStartColor(int startColor) {
        this.startColor = startColor;
    }

    public void setEndColor(int endColor) {
        this.endColor = endColor;
    }

    public void setGradient(boolean gradient) {
        isGradient = gradient;
    }
}
