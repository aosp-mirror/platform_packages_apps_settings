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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.view.MotionEvent;
import android.view.View;

import com.google.common.base.Preconditions;

/**
 * Sweep across a {@link ChartView} at a specific {@link ChartAxis} value, which
 * a user can drag.
 */
public class ChartSweepView extends View {

    private final Paint mPaintSweep;
    private final Paint mPaintSweepDisabled;
    private final Paint mPaintShadow;

    private final ChartAxis mAxis;
    private long mValue;

    public interface OnSweepListener {
        public void onSweep(ChartSweepView sweep, boolean sweepDone);
    }

    private OnSweepListener mListener;

    private boolean mHorizontal;
    private MotionEvent mTracking;

    public ChartSweepView(Context context, ChartAxis axis, long value, int color) {
        super(context);

        mAxis = Preconditions.checkNotNull(axis, "missing axis");
        mValue = value;

        mPaintSweep = new Paint();
        mPaintSweep.setColor(color);
        mPaintSweep.setStrokeWidth(3.0f);
        mPaintSweep.setStyle(Style.FILL_AND_STROKE);
        mPaintSweep.setAntiAlias(true);

        mPaintSweepDisabled = new Paint();
        mPaintSweepDisabled.setColor(color);
        mPaintSweepDisabled.setStrokeWidth(1.5f);
        mPaintSweepDisabled.setStyle(Style.FILL_AND_STROKE);
        mPaintSweepDisabled.setPathEffect(new DashPathEffect(new float[] { 5, 5 }, 0));
        mPaintSweepDisabled.setAntiAlias(true);

        mPaintShadow = new Paint();
        mPaintShadow.setColor(Color.BLACK);
        mPaintShadow.setStrokeWidth(6.0f);
        mPaintShadow.setStyle(Style.FILL_AND_STROKE);
        mPaintShadow.setAntiAlias(true);

    }

    public void addOnSweepListener(OnSweepListener listener) {
        mListener = listener;
    }

    private void dispatchOnSweep(boolean sweepDone) {
        if (mListener != null) {
            mListener.onSweep(this, sweepDone);
        }
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

                if (mHorizontal) {
                    setTranslationY(event.getRawY() - mTracking.getRawY());
                    final float point = (getTop() + getTranslationY() + (getHeight() / 2))
                            - parent.getPaddingTop();
                    mValue = mAxis.convertToValue(point);
                    dispatchOnSweep(false);
                } else {
                    setTranslationX(event.getRawX() - mTracking.getRawX());
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
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // need at least 50px in each direction for grippies
        // TODO: provide this value through params
        setMeasuredDimension(50, 50);
    }

    @Override
    protected void onDraw(Canvas canvas) {

        // draw line across larger dimension
        final int width = getWidth();
        final int height = getHeight();

        mHorizontal = width > height;

        final Paint linePaint = isEnabled() ? mPaintSweep : mPaintSweepDisabled;

        if (mHorizontal) {
            final int centerY = height / 2;
            final int endX = width - height;

            canvas.drawLine(0, centerY, endX, centerY, mPaintShadow);
            canvas.drawLine(0, centerY, endX, centerY, linePaint);
            canvas.drawCircle(endX, centerY, 4.0f, mPaintShadow);
            canvas.drawCircle(endX, centerY, 4.0f, mPaintSweep);
        } else {
            final int centerX = width / 2;
            final int endY = height - width;

            canvas.drawLine(centerX, 0, centerX, endY, mPaintShadow);
            canvas.drawLine(centerX, 0, centerX, endY, linePaint);
            canvas.drawCircle(centerX, endY, 4.0f, mPaintShadow);
            canvas.drawCircle(centerX, endY, 4.0f, mPaintSweep);
        }
    }

}
