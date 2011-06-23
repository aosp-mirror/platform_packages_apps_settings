/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.MathUtils;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import com.android.settings.R;
import com.google.common.base.Preconditions;

/**
 * Sweep across a {@link ChartView} at a specific {@link ChartAxis} value, which
 * a user can drag.
 */
public class ChartSweepView extends FrameLayout {

    // TODO: paint label when requested

    private Drawable mSweep;
    private int mFollowAxis;
    private boolean mShowLabel;

    private ChartAxis mAxis;
    private long mValue;

    public static final int HORIZONTAL = 0;
    public static final int VERTICAL = 1;

    public interface OnSweepListener {
        public void onSweep(ChartSweepView sweep, boolean sweepDone);
    }

    private OnSweepListener mListener;
    private MotionEvent mTracking;

    public ChartSweepView(Context context) {
        this(context, null, 0);
    }

    public ChartSweepView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ChartSweepView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        final TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.ChartSweepView, defStyle, 0);

        setSweepDrawable(a.getDrawable(R.styleable.ChartSweepView_sweepDrawable));
        setFollowAxis(a.getInt(R.styleable.ChartSweepView_followAxis, -1));
        setShowLabel(a.getBoolean(R.styleable.ChartSweepView_showLabel, false));

        a.recycle();

        setClipToPadding(false);
        setClipChildren(false);
        setWillNotDraw(false);
    }

    void init(ChartAxis axis) {
        mAxis = Preconditions.checkNotNull(axis, "missing axis");
    }

    public int getFollowAxis() {
        return mFollowAxis;
    }

    public void getExtraMargins(Rect rect) {
        mSweep.getPadding(rect);
    }

    public void addOnSweepListener(OnSweepListener listener) {
        mListener = listener;
    }

    private void dispatchOnSweep(boolean sweepDone) {
        if (mListener != null) {
            mListener.onSweep(this, sweepDone);
        }
    }

    public void setSweepDrawable(Drawable sweep) {
        if (mSweep != null) {
            mSweep.setCallback(null);
            unscheduleDrawable(mSweep);
        }

        if (sweep != null) {
            sweep.setCallback(this);
            if (sweep.isStateful()) {
                sweep.setState(getDrawableState());
            }
            sweep.setVisible(getVisibility() == VISIBLE, false);
            mSweep = sweep;
        } else {
            mSweep = null;
        }

        invalidate();
    }

    public void setFollowAxis(int followAxis) {
        mFollowAxis = followAxis;
    }

    public void setShowLabel(boolean showLabel) {
        mShowLabel = showLabel;
        invalidate();
    }

    @Override
    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();
        if (mSweep != null) {
            mSweep.jumpToCurrentState();
        }
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (mSweep != null) {
            mSweep.setVisible(visibility == VISIBLE, false);
        }
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return who == mSweep || super.verifyDrawable(who);
    }

    public ChartAxis getAxis() {
        return mAxis;
    }

    public void setValue(long value) {
        mValue = value;
    }

    public long getValue() {
        return mValue;
    }

    public float getPoint() {
        return mAxis.convertToPoint(mValue);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) return false;

        final View parent = (View) getParent();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                mTracking = event.copy();
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                getParent().requestDisallowInterceptTouchEvent(true);

                if (mFollowAxis == VERTICAL) {
                    final float chartHeight = parent.getHeight() - parent.getPaddingTop()
                            - parent.getPaddingBottom();
                    final float translationY = MathUtils.constrain(
                            event.getRawY() - mTracking.getRawY(), -getTop(),
                            chartHeight - getTop());
                    setTranslationY(translationY);
                    final float point = (getTop() + getTranslationY() + (getHeight() / 2))
                            - parent.getPaddingTop();
                    mValue = mAxis.convertToValue(point);
                    dispatchOnSweep(false);
                } else {
                    final float chartWidth = parent.getWidth() - parent.getPaddingLeft()
                            - parent.getPaddingRight();
                    final float translationX = MathUtils.constrain(
                            event.getRawX() - mTracking.getRawX(), -getLeft(),
                            chartWidth - getLeft());
                    setTranslationX(translationX);
                    final float point = (getLeft() + getTranslationX() + (getWidth() / 2))
                            - parent.getPaddingLeft();
                    mValue = mAxis.convertToValue(point);
                    dispatchOnSweep(false);
                }
                return true;
            }
            case MotionEvent.ACTION_UP: {
                mTracking = null;
                setTranslationX(0);
                setTranslationY(0);
                requestLayout();
                dispatchOnSweep(true);
                return true;
            }
            default: {
                return false;
            }
        }
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        if (mSweep.isStateful()) {
            mSweep.setState(getDrawableState());
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(mSweep.getIntrinsicWidth(), mSweep.getIntrinsicHeight());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        final int width = getWidth();
        final int height = getHeight();

        mSweep.setBounds(0, 0, width, height);
        mSweep.draw(canvas);
    }

}
