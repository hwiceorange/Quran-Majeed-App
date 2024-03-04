package com.raiadnan.quranreader.prayertimes.quickaccess;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Build.VERSION;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityEvent;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;
import androidx.core.os.ParcelableCompat;
import androidx.core.os.ParcelableCompatCreatorCallbacks;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.MotionEventCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.customview.view.AbsSavedState;
import androidx.customview.widget.ViewDragHelper;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class SlidePanelCustom extends ViewGroup {
    private static final int DEFAULT_FADE_COLOR = -858993460;
    private static final int DEFAULT_OVERHANG_SIZE = 0;
    static final SlidingPanelLayoutImpl IMPL;
    private static final int MIN_FLING_VELOCITY = 400;
    private static final String TAG = "SlidePanelCustem";
    private boolean mCanSlide;
    private int mCoveredFadeColor;
    final ViewDragHelper mDragHelper;
    private boolean mFirstLayout;
    private float mInitialMotionX;
    private float mInitialMotionY;
    boolean mIsUnableToDrag;
    private final int mOverhangSize;
    private PanelSlideListener mPanelSlideListener;
    private int mParallaxBy;
    private float mParallaxOffset;
    final ArrayList mPostedRunnables;
    boolean mPreservedOpenState;
    private Drawable mShadowDrawableLeft;
    private Drawable mShadowDrawableRight;
    float mSlideOffset;
    int mSlideRange;
    View mSlideableView;
    private int mSliderFadeColor;
    private final Rect mTmpRect;

    static {
        int var0 = VERSION.SDK_INT;
        if(var0 >= 17) {
            IMPL = new SlidingPanelLayoutImplJBMR1();
        } else if(var0 >= 16) {
            IMPL = new SlidingPanelLayoutImplJB();
        } else {
            IMPL = new SlidingPanelLayoutImplBase();
        }

    }

    public SlidePanelCustom(Context var1) {
        this(var1, (AttributeSet)null);
    }

    public SlidePanelCustom(Context var1, AttributeSet var2) {
        this(var1, var2, 0);
    }

    public SlidePanelCustom(Context var1, AttributeSet var2, int var3) {
        super(var1, var2, var3);
        this.mSliderFadeColor = -858993460;
        this.mFirstLayout = true;
        this.mTmpRect = new Rect();
        this.mPostedRunnables = new ArrayList();
        float var4 = var1.getResources().getDisplayMetrics().density;
        this.mOverhangSize = (int)(0.0F * var4 + 0.5F);
        ViewConfiguration.get(var1);
        this.setWillNotDraw(false);
        ViewCompat.setAccessibilityDelegate(this, new AccessibilityDelegate());
        ViewCompat.setImportantForAccessibility(this, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);
        this.mDragHelper = ViewDragHelper.create(this, 0.5F, new DragHelperCallback());
        this.mDragHelper.setMinVelocity(var4 * 400.0F);
    }

    private boolean closePane(View var1, int var2) {
        if(!this.mFirstLayout && !this.smoothSlideTo(0.0F, var2)) {
            return false;
        } else {
            this.mPreservedOpenState = false;
            return true;
        }
    }

    private void dimChildView(View var1, float var2, int var3) {
        LayoutParams var5 = (LayoutParams)var1.getLayoutParams();
        if(var2 > 0.0F && var3 != 0) {
            int var4 = (int)((float)((-16777216 & var3) >>> 24) * var2);
            if(var5.dimPaint == null) {
                var5.dimPaint = new Paint();
            }

            var5.dimPaint.setColorFilter(new PorterDuffColorFilter(var4 << 24 | var3 & 16777215, Mode.SRC_OVER));
            if(ViewCompat.getLayerType(var1) != View.LAYER_TYPE_HARDWARE) {
                ViewCompat.setLayerType(var1, View.LAYER_TYPE_HARDWARE, var5.dimPaint);
            }

            this.invalidateChildRegion(var1);
        } else if(ViewCompat.getLayerType(var1) != View.LAYER_TYPE_NONE) {
            if(var5.dimPaint != null) {
                var5.dimPaint.setColorFilter((ColorFilter)null);
            }

            DisableLayerRunnable var6 = new DisableLayerRunnable(var1);
            this.mPostedRunnables.add(var6);
            ViewCompat.postOnAnimation(this, var6);
        }

    }

    private boolean openPane(View var1, int var2) {
        if(!this.mFirstLayout && !this.smoothSlideTo(1.0F, var2)) {
            return false;
        } else {
            this.mPreservedOpenState = true;
            return true;
        }
    }

    private void parallaxOtherViews(float var1) {
        int var4;
        boolean var8;
        boolean var12;
        label40: {
            var8 = this.isLayoutRtlSupport();
            LayoutParams var10 = (LayoutParams)this.mSlideableView.getLayoutParams();
            boolean var9 = var10.dimWhenOffset;
            var4 = 0;
            if(var9) {
                int var3;
                if(var8) {
                    var3 = var10.rightMargin;
                } else {
                    var3 = var10.leftMargin;
                }

                if(var3 <= 0) {
                    var12 = true;
                    break label40;
                }
            }

            var12 = false;
        }

        for(int var7 = this.getChildCount(); var4 < var7; ++var4) {
            View var11 = this.getChildAt(var4);
            if(var11 != this.mSlideableView) {
                float var2 = this.mParallaxOffset;
                int var5 = this.mParallaxBy;
                int var6 = (int)((1.0F - var2) * (float)var5);
                this.mParallaxOffset = var1;
                var6 -= (int)((1.0F - var1) * (float)var5);
                var5 = var6;
                if(var8) {
                    var5 = -var6;
                }

                var11.offsetLeftAndRight(var5);
                if(var12) {
                    if(var8) {
                        var2 = this.mParallaxOffset - 1.0F;
                    } else {
                        var2 = 1.0F - this.mParallaxOffset;
                    }

                    this.dimChildView(var11, var2, this.mCoveredFadeColor);
                }
            }
        }

    }

    private static boolean viewIsOpaque(View var0) {
        return var0.isOpaque();
    }

    protected boolean canScroll(View var1, boolean var2, int var3, int var4, int var5) {
        boolean var12 = var1 instanceof ViewGroup;
        boolean var11 = true;
        if(var12) {
            ViewGroup var14 = (ViewGroup)var1;
            int var8 = var1.getScrollX();
            int var7 = var1.getScrollY();

            for(int var6 = var14.getChildCount() - 1; var6 >= 0; --var6) {
                View var13 = var14.getChildAt(var6);
                int var9 = var4 + var8;
                if(var9 >= var13.getLeft() && var9 < var13.getRight()) {
                    int var10 = var5 + var7;
                    if(var10 >= var13.getTop() && var10 < var13.getBottom() && this.canScroll(var13, true, var3, var9 - var13.getLeft(), var10 - var13.getTop())) {
                        return true;
                    }
                }
            }
        }

        if(var2) {
            if(!this.isLayoutRtlSupport()) {
                var3 = -var3;
            }

            if(ViewCompat.canScrollHorizontally(var1, var3)) {
                var2 = var11;
                return var2;
            }
        }

        var2 = false;
        return var2;
    }

    @Deprecated
    public boolean canSlide() {
        return this.mCanSlide;
    }

    protected boolean checkLayoutParams(ViewGroup.LayoutParams var1) {
        boolean var2;
        if(var1 instanceof LayoutParams && super.checkLayoutParams(var1)) {
            var2 = true;
        } else {
            var2 = false;
        }

        return var2;
    }

    public boolean closePane() {
        return this.closePane(this.mSlideableView, 0);
    }

    public void computeScroll() {
        if(this.mDragHelper.continueSettling(true)) {
            if(!this.mCanSlide) {
                this.mDragHelper.abort();
                return;
            }

            ViewCompat.postInvalidateOnAnimation(this);
        }

    }

    void dispatchOnPanelClosed(View var1) {
        PanelSlideListener var2 = this.mPanelSlideListener;
        if(var2 != null) {
            var2.onPanelClosed(var1);
        }

        this.sendAccessibilityEvent(32);
    }

    void dispatchOnPanelOpened(View var1) {
        PanelSlideListener var2 = this.mPanelSlideListener;
        if(var2 != null) {
            var2.onPanelOpened(var1);
        }

        this.sendAccessibilityEvent(32);
    }

    void dispatchOnPanelSlide(View var1) {
        PanelSlideListener var2 = this.mPanelSlideListener;
        if(var2 != null) {
            var2.onPanelSlide(var1, this.mSlideOffset);
        }

    }

    public void draw(Canvas var1) {
        super.draw(var1);
        Drawable var7;
        if(this.isLayoutRtlSupport()) {
            var7 = this.mShadowDrawableRight;
        } else {
            var7 = this.mShadowDrawableLeft;
        }

        View var8;
        if(this.getChildCount() > 1) {
            var8 = this.getChildAt(1);
        } else {
            var8 = null;
        }

        if(var8 != null && var7 != null) {
            int var4 = var8.getTop();
            int var5 = var8.getBottom();
            int var6 = var7.getIntrinsicWidth();
            int var2;
            int var3;
            if(this.isLayoutRtlSupport()) {
                var3 = var8.getRight();
                var2 = var6 + var3;
            } else {
                var3 = var8.getLeft();
                var2 = var3;
                var3 -= var6;
            }

            var7.setBounds(var3, var4, var2, var5);
            var7.draw(var1);
        }
    }

    protected boolean drawChild(Canvas var1, View var2, long var3) {
        LayoutParams var7 = (LayoutParams)var2.getLayoutParams();
        int var5 = var1.save();
        if(this.mCanSlide && !var7.slideable && this.mSlideableView != null) {
            var1.getClipBounds(this.mTmpRect);
            Rect var8;
            if(this.isLayoutRtlSupport()) {
                var8 = this.mTmpRect;
                var8.left = Math.max(var8.left, this.mSlideableView.getRight());
            } else {
                var8 = this.mTmpRect;
                var8.right = Math.min(var8.right, this.mSlideableView.getLeft());
            }

            var1.clipRect(this.mTmpRect);
        }

        boolean var6 = super.drawChild(var1, var2, var3);
        var1.restoreToCount(var5);
        return var6;
    }

    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams();
    }

    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet var1) {
        return new LayoutParams(this.getContext(), var1);
    }

    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams var1) {
        LayoutParams var2;
        if(var1 instanceof MarginLayoutParams) {
            var2 = new LayoutParams((MarginLayoutParams)var1);
        } else {
            var2 = new LayoutParams(var1);
        }

        return var2;
    }

    @ColorInt
    public int getCoveredFadeColor() {
        return this.mCoveredFadeColor;
    }

    public int getParallaxDistance() {
        return this.mParallaxBy;
    }

    @ColorInt
    public int getSliderFadeColor() {
        return this.mSliderFadeColor;
    }

    void invalidateChildRegion(View var1) {
        IMPL.invalidateChildRegion(this, var1);
    }

    boolean isDimmed(View var1) {
        boolean var3 = false;
        if(var1 == null) {
            return false;
        } else {
            LayoutParams var4 = (LayoutParams)var1.getLayoutParams();
            boolean var2 = var3;
            if(this.mCanSlide) {
                var2 = var3;
                if(var4.dimWhenOffset) {
                    var2 = var3;
                    if(this.mSlideOffset > 0.0F) {
                        var2 = true;
                    }
                }
            }

            return var2;
        }
    }

    boolean isLayoutRtlSupport() {
        int var1 = ViewCompat.getLayoutDirection(this);
        boolean var2 = true;
        if(var1 != 1) {
            var2 = false;
        }

        return var2;
    }

    public boolean isOpen() {
        boolean var1;
        if(this.mCanSlide && this.mSlideOffset != 1.0F) {
            var1 = false;
        } else {
            var1 = true;
        }

        return var1;
    }

    public boolean isSlideable() {
        return this.mCanSlide;
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mFirstLayout = true;
    }

    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mFirstLayout = true;
        int var2 = this.mPostedRunnables.size();

        for(int var1 = 0; var1 < var2; ++var1) {
            ((DisableLayerRunnable)this.mPostedRunnables.get(var1)).run();
        }

        this.mPostedRunnables.clear();
    }

    public boolean onInterceptTouchEvent(MotionEvent var1) {
        int var4 = MotionEventCompat.getActionMasked(var1);
        boolean var5 = this.mCanSlide;
        boolean var6 = true;
        if(!var5 && var4 == 0 && this.getChildCount() > 1) {
            View var7 = this.getChildAt(1);
            if(var7 != null) {
                this.mPreservedOpenState = this.mDragHelper.isViewUnder(var7, (int)var1.getX(), (int)var1.getY()) ^ true;
            }
        }

        if(!this.mCanSlide || this.mIsUnableToDrag && var4 != 0) {
            this.mDragHelper.cancel();
            return super.onInterceptTouchEvent(var1);
        } else if(var4 != 3 && var4 != 1) {
            boolean var8;
            label47: {
                float var2;
                float var3;
                if(var4 != 0) {
                    if(var4 == 2) {
                        var2 = var1.getX();
                        var3 = var1.getY();
                        var2 = Math.abs(var2 - this.mInitialMotionX);
                        var3 = Math.abs(var3 - this.mInitialMotionY);
                        if(var2 > (float)this.mDragHelper.getTouchSlop() && var3 > var2) {
                            this.mDragHelper.cancel();
                            this.mIsUnableToDrag = true;
                            return false;
                        }
                    }
                } else {
                    this.mIsUnableToDrag = false;
                    var2 = var1.getX();
                    var3 = var1.getY();
                    this.mInitialMotionX = var2;
                    this.mInitialMotionY = var3;
                    if(this.mDragHelper.isViewUnder(this.mSlideableView, (int)var2, (int)var3) && this.isDimmed(this.mSlideableView)) {
                        var8 = true;
                        break label47;
                    }
                }

                var8 = false;
            }

            var5 = var6;
            if(!this.mDragHelper.shouldInterceptTouchEvent(var1)) {
                if(var8) {
                    var5 = var6;
                } else {
                    var5 = false;
                }
            }

            return var5;
        } else {
            this.mDragHelper.cancel();
            return false;
        }
    }

    protected void onLayout(boolean var1, int var2, int var3, int var4, int var5) {
        boolean var14 = this.isLayoutRtlSupport();
        if(var14) {
            this.mDragHelper.setEdgeTrackingEnabled(2);
        } else {
            this.mDragHelper.setEdgeTrackingEnabled(1);
        }

        int var9 = var4 - var2;
        if(var14) {
            var2 = this.getPaddingRight();
        } else {
            var2 = this.getPaddingLeft();
        }

        if(var14) {
            var5 = this.getPaddingLeft();
        } else {
            var5 = this.getPaddingRight();
        }

        int var11 = this.getPaddingTop();
        int var10 = this.getChildCount();
        if(this.mFirstLayout) {
            float var6;
            if(this.mCanSlide && this.mPreservedOpenState) {
                var6 = 1.0F;
            } else {
                var6 = 0.0F;
            }

            this.mSlideOffset = var6;
        }

        var3 = var2;

        for(int var7 = 0; var7 < var10; ++var7) {
            View var16 = this.getChildAt(var7);
            if(var16.getVisibility() != View.GONE) {
                LayoutParams var15 = (LayoutParams)var16.getLayoutParams();
                int var12 = var16.getMeasuredWidth();
                int var8;
                if(var15.slideable) {
                    var4 = var15.leftMargin;
                    int var13 = var15.rightMargin;
                    var8 = var9 - var5;
                    var13 = Math.min(var2, var8 - this.mOverhangSize) - var3 - (var4 + var13);
                    this.mSlideRange = var13;
                    if(var14) {
                        var4 = var15.rightMargin;
                    } else {
                        var4 = var15.leftMargin;
                    }

                    if(var3 + var4 + var13 + var12 / 2 > var8) {
                        var1 = true;
                    } else {
                        var1 = false;
                    }

                    var15.dimWhenOffset = var1;
                    var8 = (int)((float)var13 * this.mSlideOffset);
                    var3 += var4 + var8;
                    this.mSlideOffset = (float)var8 / (float)this.mSlideRange;
                    var4 = 0;
                } else {
                    label82: {
                        if(this.mCanSlide) {
                            var3 = this.mParallaxBy;
                            if(var3 != 0) {
                                var4 = (int)((1.0F - this.mSlideOffset) * (float)var3);
                                var3 = var2;
                                break label82;
                            }
                        }

                        var3 = var2;
                        var4 = 0;
                    }
                }

                if(var14) {
                    var8 = var9 - var3 + var4;
                    var4 = var8 - var12;
                } else {
                    var4 = var3 - var4;
                    var8 = var4 + var12;
                }

                var16.layout(var4, var11, var8, var16.getMeasuredHeight() + var11);
                var2 += var16.getWidth();
            }
        }

        if(this.mFirstLayout) {
            if(this.mCanSlide) {
                if(this.mParallaxBy != 0) {
                    this.parallaxOtherViews(this.mSlideOffset);
                }

                if(((LayoutParams)this.mSlideableView.getLayoutParams()).dimWhenOffset) {
                    this.dimChildView(this.mSlideableView, this.mSlideOffset, this.mSliderFadeColor);
                }
            } else {
                for(var2 = 0; var2 < var10; ++var2) {
                    this.dimChildView(this.getChildAt(var2), 0.0F, this.mSliderFadeColor);
                }
            }

            this.updateObscuredViewsVisibility(this.mSlideableView);
        }

        this.mFirstLayout = false;
    }

    protected void onMeasure(int var1, int var2) {
        int var8 = MeasureSpec.getMode(var1);
        int var5 = MeasureSpec.getSize(var1);
        int var6 = MeasureSpec.getMode(var2);
        var2 = MeasureSpec.getSize(var2);
        int var7;
        int var9;
        if(var8 != 1073741824) {
            if(!this.isInEditMode()) {
                throw new IllegalStateException("Width must have an exact value or MATCH_PARENT");
            }

            if(var8 == Integer.MIN_VALUE) {
                var7 = var5;
                var9 = var6;
                var1 = var2;
            } else {
                var7 = var5;
                var9 = var6;
                var1 = var2;
                if(var8 == 0) {
                    var7 = 300;
                    var9 = var6;
                    var1 = var2;
                }
            }
        } else {
            var7 = var5;
            var9 = var6;
            var1 = var2;
            if(var6 == 0) {
                if(!this.isInEditMode()) {
                    throw new IllegalStateException("Height must not be UNSPECIFIED");
                }

                var7 = var5;
                var9 = var6;
                var1 = var2;
                if(var6 == 0) {
                    var9 = Integer.MIN_VALUE;
                    var1 = 300;
                    var7 = var5;
                }
            }
        }

        if(var9 != Integer.MIN_VALUE) {
            if(var9 != 1073741824) {
                var1 = 0;
                var2 = 1;
            } else {
                var1 = var1 - this.getPaddingTop() - this.getPaddingBottom();
                var2 = var1;
            }
        } else {
            var2 = var1 - this.getPaddingTop() - this.getPaddingBottom();
            var1 = 0;
        }

        int var11 = var7 - this.getPaddingLeft() - this.getPaddingRight();
        int var12 = this.getChildCount();
        if(var12 > 2) {
            Log.e("SlidePanelCustem", "onMeasure: More than two child views are not supported.");
        }

        this.mSlideableView = null;
        var8 = var11;
        int var10 = 0;
        boolean var15 = false;
        float var4 = 0.0F;

        int var13;
        View var17;
        LayoutParams var18;
        for(var5 = var1; var10 < var12; var5 = var1) {
            var17 = this.getChildAt(var10);
            var18 = (LayoutParams)var17.getLayoutParams();
            float var3;
            if(var17.getVisibility() == View.GONE) {
                var18.dimWhenOffset = false;
                var3 = var4;
                var1 = var5;
            } else {
                label190: {
                    var3 = var4;
                    if(var18.weight > 0.0F) {
                        var4 += var18.weight;
                        var3 = var4;
                        if(var18.width == 0) {
                            var3 = var4;
                            var1 = var5;
                            break label190;
                        }
                    }

                    var1 = var18.leftMargin + var18.rightMargin;
                    if(var18.width == -2) {
                        var1 = MeasureSpec.makeMeasureSpec(var11 - var1, MeasureSpec.AT_MOST);
                    } else if(var18.width == -1) {
                        var1 = MeasureSpec.makeMeasureSpec(var11 - var1, MeasureSpec.EXACTLY);
                    } else {
                        var1 = MeasureSpec.makeMeasureSpec(var18.width, MeasureSpec.EXACTLY);
                    }

                    if(var18.height == -2) {
                        var6 = MeasureSpec.makeMeasureSpec(var2, MeasureSpec.AT_MOST);
                    } else if(var18.height == -1) {
                        var6 = MeasureSpec.makeMeasureSpec(var2, MeasureSpec.EXACTLY);
                    } else {
                        var6 = MeasureSpec.makeMeasureSpec(var18.height, MeasureSpec.EXACTLY);
                    }

                    var17.measure(var1, var6);
                    var6 = var17.getMeasuredWidth();
                    var13 = var17.getMeasuredHeight();
                    var1 = var5;
                    if(var9 == Integer.MIN_VALUE) {
                        var1 = var5;
                        if(var13 > var5) {
                            var1 = Math.min(var13, var2);
                        }
                    }

                    var8 -= var6;
                    boolean var16;
                    if(var8 < 0) {
                        var16 = true;
                    } else {
                        var16 = false;
                    }

                    var18.slideable = var16;
                    if(var18.slideable) {
                        this.mSlideableView = var17;
                    }

                    var15 |= var16;
                }
            }

            ++var10;
            var4 = var3;
        }

        if(var15 || var4 > 0.0F) {
            var6 = var11 - this.mOverhangSize;

            for(var9 = 0; var9 < var12; ++var9) {
                var17 = this.getChildAt(var9);
                if(var17.getVisibility() != View.GONE) {
                    var18 = (LayoutParams)var17.getLayoutParams();
                    if(var17.getVisibility() != View.GONE) {
                        boolean var19;
                        if(var18.width == 0 && var18.weight > 0.0F) {
                            var19 = true;
                        } else {
                            var19 = false;
                        }

                        if(var19) {
                            var10 = 0;
                        } else {
                            var10 = var17.getMeasuredWidth();
                        }

                        if(var15 && var17 != this.mSlideableView) {
                            if(var18.width < 0 && (var10 > var6 || var18.weight > 0.0F)) {
                                if(var19) {
                                    if(var18.height == -2) {
                                        var1 = MeasureSpec.makeMeasureSpec(var2, Integer.MIN_VALUE);
                                    } else if(var18.height == -1) {
                                        var1 = MeasureSpec.makeMeasureSpec(var2, 1073741824);
                                    } else {
                                        var1 = MeasureSpec.makeMeasureSpec(var18.height, 1073741824);
                                    }
                                } else {
                                    var1 = MeasureSpec.makeMeasureSpec(var17.getMeasuredHeight(), 1073741824);
                                }

                                var17.measure(MeasureSpec.makeMeasureSpec(var6, 1073741824), var1);
                            }
                        } else if(var18.weight > 0.0F) {
                            if(var18.width == 0) {
                                if(var18.height == -2) {
                                    var1 = MeasureSpec.makeMeasureSpec(var2, Integer.MIN_VALUE);
                                } else if(var18.height == -1) {
                                    var1 = MeasureSpec.makeMeasureSpec(var2, 1073741824);
                                } else {
                                    var1 = MeasureSpec.makeMeasureSpec(var18.height, 1073741824);
                                }
                            } else {
                                var1 = MeasureSpec.makeMeasureSpec(var17.getMeasuredHeight(), 1073741824);
                            }

                            if(var15) {
                                int var14 = var11 - (var18.leftMargin + var18.rightMargin);
                                var13 = MeasureSpec.makeMeasureSpec(var14, 1073741824);
                                if(var10 != var14) {
                                    var17.measure(var13, var1);
                                }
                            } else {
                                var13 = Math.max(0, var8);
                                var17.measure(MeasureSpec.makeMeasureSpec(var10 + (int)(var18.weight * (float)var13 / var4), 1073741824), var1);
                            }
                        }
                    }
                }
            }
        }

        this.setMeasuredDimension(var7, var5 + this.getPaddingTop() + this.getPaddingBottom());
        this.mCanSlide = var15;
        if(this.mDragHelper.getViewDragState() != 0 && !var15) {
            this.mDragHelper.abort();
        }

    }

    void onPanelDragged(int var1) {
        if(this.mSlideableView == null) {
            this.mSlideOffset = 0.0F;
        } else {
            boolean var4 = this.isLayoutRtlSupport();
            LayoutParams var5 = (LayoutParams)this.mSlideableView.getLayoutParams();
            int var3 = this.mSlideableView.getWidth();
            int var2 = var1;
            if(var4) {
                var2 = this.getWidth() - var1 - var3;
            }

            if(var4) {
                var1 = this.getPaddingRight();
            } else {
                var1 = this.getPaddingLeft();
            }

            if(var4) {
                var3 = var5.rightMargin;
            } else {
                var3 = var5.leftMargin;
            }

            this.mSlideOffset = (float)(var2 - (var1 + var3)) / (float)this.mSlideRange;
            if(this.mParallaxBy != 0) {
                this.parallaxOtherViews(this.mSlideOffset);
            }

            if(var5.dimWhenOffset) {
                this.dimChildView(this.mSlideableView, this.mSlideOffset, this.mSliderFadeColor);
            }

            this.dispatchOnPanelSlide(this.mSlideableView);
        }
    }

    protected void onRestoreInstanceState(Parcelable var1) {
        if(!(var1 instanceof SavedState)) {
            super.onRestoreInstanceState(var1);
        } else {
            SavedState var2 = (SavedState)var1;
            super.onRestoreInstanceState(var2.getSuperState());
            if(var2.isOpen) {
                this.openPane();
            } else {
                this.closePane();
            }

            this.mPreservedOpenState = var2.isOpen;
        }
    }

    protected Parcelable onSaveInstanceState() {
        SavedState var2 = new SavedState(super.onSaveInstanceState());
        boolean var1;
        if(this.isSlideable()) {
            var1 = this.isOpen();
        } else {
            var1 = this.mPreservedOpenState;
        }

        var2.isOpen = var1;
        return var2;
    }

    protected void onSizeChanged(int var1, int var2, int var3, int var4) {
        super.onSizeChanged(var1, var2, var3, var4);
        if(var1 != var3) {
            this.mFirstLayout = true;
        }

    }

    public boolean onTouchEvent(MotionEvent var1) {
        if(!this.mCanSlide) {
            return super.onTouchEvent(var1);
        } else {
            this.mDragHelper.processTouchEvent(var1);
            float var2;
            float var3;
            switch(var1.getAction() & 255) {
                case 0:
                    var2 = var1.getX();
                    var3 = var1.getY();
                    this.mInitialMotionX = var2;
                    this.mInitialMotionY = var3;
                    break;
                case 1:
                    if(this.isDimmed(this.mSlideableView)) {
                        float var4 = var1.getX();
                        float var5 = var1.getY();
                        var2 = var4 - this.mInitialMotionX;
                        var3 = var5 - this.mInitialMotionY;
                        int var6 = this.mDragHelper.getTouchSlop();
                        if(var2 * var2 + var3 * var3 < (float)(var6 * var6) && this.mDragHelper.isViewUnder(this.mSlideableView, (int)var4, (int)var5)) {
                            this.closePane(this.mSlideableView, 0);
                        }
                    }
            }

            return true;
        }
    }

    public boolean openPane() {
        return this.openPane(this.mSlideableView, 0);
    }

    public void requestChildFocus(View var1, View var2) {
        super.requestChildFocus(var1, var2);
        if(!this.isInTouchMode() && !this.mCanSlide) {
            boolean var3;
            if(var1 == this.mSlideableView) {
                var3 = true;
            } else {
                var3 = false;
            }

            this.mPreservedOpenState = var3;
        }

    }

    void setAllChildrenVisible() {
        int var2 = this.getChildCount();

        for(int var1 = 0; var1 < var2; ++var1) {
            View var3 = this.getChildAt(var1);
            if(var3.getVisibility() == 4) {
                var3.setVisibility(0);
            }
        }

    }

    public void setCoveredFadeColor(@ColorInt int var1) {
        this.mCoveredFadeColor = var1;
    }

    public void setPanelSlideListener(PanelSlideListener var1) {
        this.mPanelSlideListener = var1;
    }

    public void setParallaxDistance(int var1) {
        this.mParallaxBy = var1;
        this.requestLayout();
    }

    @Deprecated
    public void setShadowDrawable(Drawable var1) {
        this.setShadowDrawableLeft(var1);
    }

    public void setShadowDrawableLeft(Drawable var1) {
        this.mShadowDrawableLeft = var1;
    }

    public void setShadowDrawableRight(Drawable var1) {
        this.mShadowDrawableRight = var1;
    }

    @Deprecated
    public void setShadowResource(@DrawableRes int var1) {
        this.setShadowDrawable(this.getResources().getDrawable(var1));
    }

    public void setShadowResourceLeft(int var1) {
        this.setShadowDrawableLeft(ContextCompat.getDrawable(this.getContext(), var1));
    }

    public void setShadowResourceRight(int var1) {
        this.setShadowDrawableRight(ContextCompat.getDrawable(this.getContext(), var1));
    }

    public void setSliderFadeColor(@ColorInt int var1) {
        this.mSliderFadeColor = var1;
    }

    @Deprecated
    public void smoothSlideClosed() {
        this.closePane();
    }

    @Deprecated
    public void smoothSlideOpen() {
        this.openPane();
    }

    boolean smoothSlideTo(float var1, int var2) {
        if(!this.mCanSlide) {
            return false;
        } else {
            boolean var5 = this.isLayoutRtlSupport();
            LayoutParams var6 = (LayoutParams)this.mSlideableView.getLayoutParams();
            if(var5) {
                int var3 = this.getPaddingRight();
                int var4 = var6.rightMargin;
                var2 = this.mSlideableView.getWidth();
                var2 = (int)((float)this.getWidth() - ((float)(var3 + var4) + var1 * (float)this.mSlideRange + (float)var2));
            } else {
                var2 = (int)((float)(this.getPaddingLeft() + var6.leftMargin) + var1 * (float)this.mSlideRange);
            }

            ViewDragHelper var8 = this.mDragHelper;
            View var7 = this.mSlideableView;
            if(var8.smoothSlideViewTo(var7, var2, var7.getTop())) {
                this.setAllChildrenVisible();
                ViewCompat.postInvalidateOnAnimation(this);
                return true;
            } else {
                return false;
            }
        }
    }

    void updateObscuredViewsVisibility(View var1) {
        boolean var17 = this.isLayoutRtlSupport();
        int var2;
        if(var17) {
            var2 = this.getWidth() - this.getPaddingRight();
        } else {
            var2 = this.getPaddingLeft();
        }

        int var3;
        if(var17) {
            var3 = this.getPaddingLeft();
        } else {
            var3 = this.getWidth() - this.getPaddingRight();
        }

        int var12 = this.getPaddingTop();
        int var10 = this.getHeight();
        int var11 = this.getPaddingBottom();
        int var4;
        int var5;
        int var6;
        int var7;
        if(var1 != null && viewIsOpaque(var1)) {
            var6 = var1.getLeft();
            var5 = var1.getRight();
            var4 = var1.getTop();
            var7 = var1.getBottom();
        } else {
            var6 = 0;
            var5 = 0;
            var4 = 0;
            var7 = 0;
        }

        int var13 = this.getChildCount();

        for(int var8 = 0; var8 < var13; ++var8) {
            View var18 = this.getChildAt(var8);
            if(var18 == var1) {
                break;
            }

            if(var18.getVisibility() != 8) {
                int var9;
                if(var17) {
                    var9 = var3;
                } else {
                    var9 = var2;
                }

                int var15 = Math.max(var9, var18.getLeft());
                int var14 = Math.max(var12, var18.getTop());
                if(var17) {
                    var9 = var2;
                } else {
                    var9 = var3;
                }

                int var16 = Math.min(var9, var18.getRight());
                var9 = Math.min(var10 - var11, var18.getBottom());
                byte var19;
                if(var15 >= var6 && var14 >= var4 && var16 <= var5 && var9 <= var7) {
                    var19 = 4;
                } else {
                    var19 = 0;
                }

                var18.setVisibility(var19);
            }
        }

    }

    class AccessibilityDelegate extends AccessibilityDelegateCompat {
        private final Rect mTmpRect = new Rect();

        private void copyNodeInfoNoChildren(AccessibilityNodeInfoCompat var1, AccessibilityNodeInfoCompat var2) {
            Rect var3 = this.mTmpRect;
            var2.getBoundsInParent(var3);
            var1.setBoundsInParent(var3);
            var2.getBoundsInScreen(var3);
            var1.setBoundsInScreen(var3);
            var1.setVisibleToUser(var2.isVisibleToUser());
            var1.setPackageName(var2.getPackageName());
            var1.setClassName(var2.getClassName());
            var1.setContentDescription(var2.getContentDescription());
            var1.setEnabled(var2.isEnabled());
            var1.setClickable(var2.isClickable());
            var1.setFocusable(var2.isFocusable());
            var1.setFocused(var2.isFocused());
            var1.setAccessibilityFocused(var2.isAccessibilityFocused());
            var1.setSelected(var2.isSelected());
            var1.setLongClickable(var2.isLongClickable());
            var1.addAction(var2.getActions());
            var1.setMovementGranularities(var2.getMovementGranularities());
        }

        public boolean filter(View var1) {
            return SlidePanelCustom.this.isDimmed(var1);
        }

        public void onInitializeAccessibilityEvent(View var1, AccessibilityEvent var2) {
            super.onInitializeAccessibilityEvent(var1, var2);
            var2.setClassName(SlidePanelCustom.class.getName());
        }

        public void onInitializeAccessibilityNodeInfo(View var1, AccessibilityNodeInfoCompat var2) {
            AccessibilityNodeInfoCompat var5 = AccessibilityNodeInfoCompat.obtain(var2);
            super.onInitializeAccessibilityNodeInfo(var1, var5);
            this.copyNodeInfoNoChildren(var2, var5);
            var5.recycle();
            var2.setClassName(SlidePanelCustom.class.getName());
            var2.setSource(var1);
            ViewParent var6 = ViewCompat.getParentForAccessibility(var1);
            if(var6 instanceof View) {
                var2.setParent((View)var6);
            }

            int var4 = SlidePanelCustom.this.getChildCount();

            for(int var3 = 0; var3 < var4; ++var3) {
                var1 = SlidePanelCustom.this.getChildAt(var3);
                if(!this.filter(var1) && var1.getVisibility() == View.VISIBLE) {
                    ViewCompat.setImportantForAccessibility(var1, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);
                    var2.addChild(var1);
                }
            }

        }

        public boolean onRequestSendAccessibilityEvent(ViewGroup var1, View var2, AccessibilityEvent var3) {
            return !this.filter(var2)?super.onRequestSendAccessibilityEvent(var1, var2, var3):false;
        }
    }

    private class DisableLayerRunnable implements Runnable {
        final View mChildView;

        DisableLayerRunnable(View var2) {
            this.mChildView = var2;
        }

        public void run() {
            if(this.mChildView.getParent() == SlidePanelCustom.this) {
                ViewCompat.setLayerType(this.mChildView, 0, (Paint)null);
                SlidePanelCustom.this.invalidateChildRegion(this.mChildView);
            }

            SlidePanelCustom.this.mPostedRunnables.remove(this);
        }
    }

    private class DragHelperCallback extends ViewDragHelper.Callback {
        public int clampViewPositionHorizontal(View var1, int var2, int var3) {
            LayoutParams var5 = (LayoutParams)SlidePanelCustom.this.mSlideableView.getLayoutParams();
            int var4;
            if(SlidePanelCustom.this.isLayoutRtlSupport()) {
                var3 = SlidePanelCustom.this.getWidth() - (SlidePanelCustom.this.getPaddingRight() + var5.rightMargin + SlidePanelCustom.this.mSlideableView.getWidth());
                var4 = SlidePanelCustom.this.mSlideRange;
                var2 = Math.max(Math.min(var2, var3), var3 - var4);
            } else {
                var3 = SlidePanelCustom.this.getPaddingLeft() + var5.leftMargin;
                var4 = SlidePanelCustom.this.mSlideRange;
                var2 = Math.min(Math.max(var2, var3), var4 + var3);
            }

            return var2;
        }

        public int clampViewPositionVertical(View var1, int var2, int var3) {
            return var1.getTop();
        }

        public int getViewHorizontalDragRange(View var1) {
            return SlidePanelCustom.this.mSlideRange;
        }

        public void onEdgeDragStarted(int var1, int var2) {
            SlidePanelCustom.this.mDragHelper.captureChildView(SlidePanelCustom.this.mSlideableView, var2);
        }

        public void onViewCaptured(View var1, int var2) {
            SlidePanelCustom.this.setAllChildrenVisible();
        }

        public void onViewDragStateChanged(int var1) {
            if(SlidePanelCustom.this.mDragHelper.getViewDragState() == 0) {
                SlidePanelCustom var2;
                if(SlidePanelCustom.this.mSlideOffset == 0.0F) {
                    var2 = SlidePanelCustom.this;
                    var2.updateObscuredViewsVisibility(var2.mSlideableView);
                    var2 = SlidePanelCustom.this;
                    var2.dispatchOnPanelClosed(var2.mSlideableView);
                    SlidePanelCustom.this.mPreservedOpenState = false;
                } else {
                    var2 = SlidePanelCustom.this;
                    var2.dispatchOnPanelOpened(var2.mSlideableView);
                    SlidePanelCustom.this.mPreservedOpenState = true;
                }
            }

        }

        public void onViewPositionChanged(View var1, int var2, int var3, int var4, int var5) {
            SlidePanelCustom.this.onPanelDragged(var2);
            SlidePanelCustom.this.invalidate();
        }

        public void onViewReleased(View var1, float var2, float var3) {
            LayoutParams var6 = (LayoutParams)var1.getLayoutParams();
            int var4;
            int var5;
            if(SlidePanelCustom.this.isLayoutRtlSupport()) {
                label26: {
                    var5 = SlidePanelCustom.this.getPaddingRight() + var6.rightMargin;
                    if(var2 >= 0.0F) {
                        var4 = var5;
                        if(var2 != 0.0F) {
                            break label26;
                        }

                        var4 = var5;
                        if(SlidePanelCustom.this.mSlideOffset <= 0.5F) {
                            break label26;
                        }
                    }

                    var4 = var5 + SlidePanelCustom.this.mSlideRange;
                }

                var5 = SlidePanelCustom.this.mSlideableView.getWidth();
                var4 = SlidePanelCustom.this.getWidth() - var4 - var5;
            } else {
                label20: {
                    var4 = SlidePanelCustom.this.getPaddingLeft();
                    var5 = var6.leftMargin + var4;
                    if(var2 <= 0.0F) {
                        var4 = var5;
                        if(var2 != 0.0F) {
                            break label20;
                        }

                        var4 = var5;
                        if(SlidePanelCustom.this.mSlideOffset <= 0.5F) {
                            break label20;
                        }
                    }

                    var4 = var5 + SlidePanelCustom.this.mSlideRange;
                }
            }

            SlidePanelCustom.this.mDragHelper.settleCapturedViewAt(var4, var1.getTop());
            SlidePanelCustom.this.invalidate();
        }

        public boolean tryCaptureView(View var1, int var2) {
            return SlidePanelCustom.this.mIsUnableToDrag?false:((LayoutParams)var1.getLayoutParams()).slideable;
        }
    }

    public static class LayoutParams extends MarginLayoutParams {
        private static final int[] ATTRS = new int[]{16843137};
        Paint dimPaint;
        boolean dimWhenOffset;
        boolean slideable;
        public float weight = 0.0F;

        public LayoutParams() {
            super(-1, -1);
        }

        public LayoutParams(int var1, int var2) {
            super(var1, var2);
        }

        public LayoutParams(Context var1, AttributeSet var2) {
            super(var1, var2);
            TypedArray var3 = var1.obtainStyledAttributes(var2, ATTRS);
            this.weight = var3.getFloat(0, 0.0F);
            var3.recycle();
        }

        public LayoutParams(ViewGroup.LayoutParams var1) {
            super(var1);
        }

        public LayoutParams(MarginLayoutParams var1) {
            super(var1);
        }

        public LayoutParams(LayoutParams var1) {
            super(var1);
            this.weight = var1.weight;
        }
    }

    public interface PanelSlideListener {
        void onPanelClosed(View var1);

        void onPanelOpened(View var1);

        void onPanelSlide(View var1, float var2);
    }

    static class SavedState extends AbsSavedState {
        public static final Creator CREATOR = ParcelableCompat.newCreator(new ParcelableCompatCreatorCallbacks() {
            public SavedState createFromParcel(Parcel var1, ClassLoader var2) {
                return new SavedState(var1, var2);
            }

            public SavedState[] newArray(int var1) {
                return new SavedState[var1];
            }
        });
        boolean isOpen;

        SavedState(Parcel var1, ClassLoader var2) {
            super(var1, var2);
            boolean var3;
            if(var1.readInt() != 0) {
                var3 = true;
            } else {
                var3 = false;
            }

            this.isOpen = var3;
        }

        SavedState(Parcelable var1) {
            super(var1);
        }

        public void writeToParcel(Parcel var1, int var2) {
            throw new RuntimeException("d2j fail translate: java.lang.RuntimeException: can not merge I and Z\r\n\tat com.googlecode.dex2jar.ir.TypeClass.merge(TypeClass.java:100)\r\n\tat com.googlecode.dex2jar.ir.ts.TypeTransformer$TypeRef.updateTypeClass(TypeTransformer.java:174)\r\n\tat com.googlecode.dex2jar.ir.ts.TypeTransformer$TypeAnalyze.provideAs(TypeTransformer.java:783)\r\n\tat com.googlecode.dex2jar.ir.ts.TypeTransformer$TypeAnalyze.e1expr(TypeTransformer.java:496)\r\n\tat com.googlecode.dex2jar.ir.ts.TypeTransformer$TypeAnalyze.exExpr(TypeTransformer.java:716)\r\n\tat com.googlecode.dex2jar.ir.ts.TypeTransformer$TypeAnalyze.exExpr(TypeTransformer.java:706)\r\n\tat com.googlecode.dex2jar.ir.ts.TypeTransformer$TypeAnalyze.enexpr(TypeTransformer.java:701)\r\n\tat com.googlecode.dex2jar.ir.ts.TypeTransformer$TypeAnalyze.exExpr(TypeTransformer.java:722)\r\n\tat com.googlecode.dex2jar.ir.ts.TypeTransformer$TypeAnalyze.exExpr(TypeTransformer.java:706)\r\n\tat com.googlecode.dex2jar.ir.ts.TypeTransformer$TypeAnalyze.s1stmt(TypeTransformer.java:813)\r\n\tat com.googlecode.dex2jar.ir.ts.TypeTransformer$TypeAnalyze.sxStmt(TypeTransformer.java:843)\r\n\tat com.googlecode.dex2jar.ir.ts.TypeTransformer$TypeAnalyze.analyze(TypeTransformer.java:206)\r\n\tat com.googlecode.dex2jar.ir.ts.TypeTransformer.transform(TypeTransformer.java:44)\r\n\tat com.googlecode.d2j.dex.Dex2jar$2.optimize(Dex2jar.java:161)\r\n\tat com.googlecode.d2j.dex.Dex2Asm.convertCode(Dex2Asm.java:433)\r\n\tat com.googlecode.d2j.dex.ExDex2Asm.convertCode(ExDex2Asm.java:42)\r\n\tat com.googlecode.d2j.dex.Dex2jar$2.convertCode(Dex2jar.java:129)\r\n\tat com.googlecode.d2j.dex.Dex2Asm.convertMethod(Dex2Asm.java:528)\r\n\tat com.googlecode.d2j.dex.Dex2Asm.convertClass(Dex2Asm.java:425)\r\n\tat com.googlecode.d2j.dex.Dex2Asm.convertDex(Dex2Asm.java:441)\r\n\tat com.googlecode.d2j.dex.Dex2jar.doTranslate(Dex2jar.java:123)\r\n\tat com.googlecode.d2j.dex.Dex2jar.to(Dex2jar.java:271)\r\n\tat com.googlecode.dex2jar.tools.Dex2jarCmd.doCommandLine(Dex2jarCmd.java:107)\r\n\tat com.googlecode.dex2jar.tools.BaseCmd.doMain(BaseCmd.java:290)\r\n\tat com.googlecode.dex2jar.tools.Dex2jarCmd.main(Dex2jarCmd.java:33)\r\n\tat sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\r\n\tat sun.reflect.NativeMethodAccessorImpl.invoke(Unknown Source)\r\n\tat sun.reflect.DelegatingMethodAccessorImpl.invoke(Unknown Source)\r\n\tat java.lang.reflect.Method.invoke(Unknown Source)\r\n\tat com.exe4j.runtime.LauncherEngine.launch(Unknown Source)\r\n\tat com.exe4j.runtime.WinLauncher.main(Unknown Source)\r\n");
        }
    }

    public static class SimplePanelSlideListener implements PanelSlideListener {
        public void onPanelClosed(View var1) {
        }

        public void onPanelOpened(View var1) {
        }

        public void onPanelSlide(View var1, float var2) {
        }
    }

    interface SlidingPanelLayoutImpl {
        void invalidateChildRegion(SlidePanelCustom var1, View var2);
    }

    static class SlidingPanelLayoutImplBase implements SlidingPanelLayoutImpl {
        public void invalidateChildRegion(SlidePanelCustom var1, View var2) {
            ViewCompat.postInvalidateOnAnimation(var1, var2.getLeft(), var2.getTop(), var2.getRight(), var2.getBottom());
        }
    }

    static class SlidingPanelLayoutImplJB extends SlidingPanelLayoutImplBase {
        private Method mGetDisplayList;
        private Field mRecreateDisplayList;

        SlidingPanelLayoutImplJB() {
            try {
                this.mGetDisplayList = View.class.getDeclaredMethod("getDisplayList", (Class[])null);
            } catch (NoSuchMethodException var3) {
                Log.e("SlidePanelCustem", "Couldn\'t fetch getDisplayList method; dimming won\'t work right.", var3);
            }

            try {
                this.mRecreateDisplayList = View.class.getDeclaredField("mRecreateDisplayList");
                this.mRecreateDisplayList.setAccessible(true);
            } catch (NoSuchFieldException var2) {
                Log.e("SlidePanelCustem", "Couldn\'t fetch mRecreateDisplayList field; dimming will be slow.", var2);
            }

        }

        public void invalidateChildRegion(SlidePanelCustom var1, View var2) {
            if(this.mGetDisplayList != null) {
                Field var3 = this.mRecreateDisplayList;
                if(var3 != null) {
                    try {
                        var3.setBoolean(var2, true);
                        this.mGetDisplayList.invoke(var2, (Object[])null);
                    } catch (Exception var4) {
                        Log.e("SlidePanelCustem", "Error refreshing display list state", var4);
                    }

                    super.invalidateChildRegion(var1, var2);
                    return;
                }
            }

            var2.invalidate();
        }
    }

    static class SlidingPanelLayoutImplJBMR1 extends SlidingPanelLayoutImplBase {
        public void invalidateChildRegion(SlidePanelCustom var1, View var2) {
            ViewCompat.setLayerPaint(var2, ((LayoutParams)var2.getLayoutParams()).dimPaint);
        }
    }
}
