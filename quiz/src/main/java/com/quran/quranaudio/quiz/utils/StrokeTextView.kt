package com.quran.quranaudio.quiz.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.quran.quranaudio.quiz.R


class StrokeTextView: AppCompatTextView {

    //region Constructors

    constructor(ctx: Context) : super(ctx, null)
    constructor(ctx: Context, attr: AttributeSet?) : super(ctx, attr, 0) {
        getStyledAttributes(attr)
    }

    constructor(ctx: Context, attr: AttributeSet?, defStyleAttr: Int) : super(ctx, attr, defStyleAttr) {
        getStyledAttributes(attr)
    }

    private var calcWidth = 0

    var strokeWidth = 4f
    var strokeColor = Color.RED

    /**
     * Static layout values are not mutable so we need to initialize it after text is set
     */
    private lateinit var staticLayout: StaticLayout

    private val staticLayoutPaint by lazy {
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = this@StrokeTextView.textSize
            typeface = this@StrokeTextView.typeface
        }
    }

    private fun getStyledAttributes(attr: AttributeSet?) {
        context.obtainStyledAttributes(attr, R.styleable.StrokeTextView).apply {
            strokeWidth = getDimensionPixelSize(R.styleable.StrokeTextView_strokeThickness, 4).toFloat()
            strokeColor = getColor(R.styleable.StrokeTextView_strokeColor, Color.RED)
            recycle()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        calcWidth = (MeasureSpec.getSize(widthMeasureSpec) - paddingStart - paddingEnd)
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

     override fun onDraw(canvas: Canvas) {
        if (canvas == null) return

        reinitialzieStaticLayout()
        with(canvas) {
            save()
            translate(paddingStart.toFloat(), 0f)

            staticLayoutPaint.configureForStroke()
            staticLayout.draw(this)

            staticLayoutPaint.configureForFill()
            staticLayout.draw(this)

            restore()
        }
    }

    private fun reinitialzieStaticLayout() {
        staticLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder
                .obtain(text, 0, text.length, staticLayoutPaint, calcWidth)
                .setLineSpacing(lineSpacingExtra, lineSpacingMultiplier)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .build()
        } else {
            StaticLayout(
                text,
                staticLayoutPaint,
                calcWidth,
                Layout.Alignment.ALIGN_CENTER,
                lineSpacingMultiplier,
                lineSpacingExtra,
                true
            )
        }
    }

    private fun Paint.configureForFill() {
        style = Paint.Style.FILL
        color = textColors.defaultColor
    }

    private fun Paint.configureForStroke() {
        style = Paint.Style.STROKE
        color = strokeColor
        strokeWidth = this@StrokeTextView.strokeWidth
    }
}