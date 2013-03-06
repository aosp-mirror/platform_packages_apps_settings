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
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewDebug;
import android.widget.FrameLayout;

import com.android.internal.util.Preconditions;
import com.android.settings.R;

/**
 * Container for two-dimensional chart, drawn with a combination of
 * {@link ChartGridView}, {@link ChartNetworkSeriesView} and {@link ChartSweepView}
 * children. The entire chart uses {@link ChartAxis} to map between raw values
 * and screen coordinates.
 */
public class ChartView extends FrameLayout {
    // TODO: extend something that supports two-dimensional scrolling

    private static final int SWEEP_GRAVITY = Gravity.TOP | Gravity.START;

    ChartAxis mHoriz;
    ChartAxis mVert;

    @ViewDebug.ExportedProperty
    private int mOptimalWidth = -1;
    private float mOptimalWidthWeight = 0;

    private Rect mContent = new Rect();

    public ChartView(Context context) {
        this(context, null, 0);
    }

    public ChartView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ChartView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        final TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.ChartView, defStyle, 0);
        setOptimalWidth(a.getDimensionPixelSize(R.styleable.ChartView_optimalWidth, -1),
                a.getFloat(R.styleable.ChartView_optimalWidthWeight, 0));
        a.recycle();

        setClipToPadding(false);
        setClipChildren(false);
    }

    void init(ChartAxis horiz, ChartAxis vert) {
        mHoriz = Preconditions.checkNotNull(horiz, "missing horiz");
        mVert = Preconditions.checkNotNull(vert, "missing vert");
    }

    public void setOptimalWidth(int optimalWidth, float optimalWidthWeight) {
        mOptimalWidth = optimalWidth;
        mOptimalWidthWeight = optimalWidthWeight;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        final int slack = getMeasuredWidth() - mOptimalWidth;
        if (mOptimalWidth > 0 && slack > 0) {
            final int targetWidth = (int) (mOptimalWidth + (slack * mOptimalWidthWeight));
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(targetWidth, MeasureSpec.EXACTLY);
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        mContent.set(getPaddingLeft(), getPaddingTop(), r - l - getPaddingRight(),
                b - t - getPaddingBottom());
        final int width = mContent.width();
        final int height = mContent.height();

        // no scrolling yet, so tell dimensions to fill exactly
        mHoriz.setSize(width);
        mVert.setSize(height);

        final Rect parentRect = new Rect();
        final Rect childRect = new Rect();

        for (int i = 0; i < getChildCount(); i++) {
            final View child = getChildAt(i);
            final LayoutParams params = (LayoutParams) child.getLayoutParams();

            parentRect.set(mContent);

            if (child instanceof ChartNetworkSeriesView || child instanceof ChartGridView) {
                // series are always laid out to fill entire graph area
                // TODO: handle scrolling for series larger than content area
                Gravity.apply(params.gravity, width, height, parentRect, childRect);
                child.layout(childRect.left, childRect.top, childRect.right, childRect.bottom);

            } else if (child instanceof ChartSweepView) {
                layoutSweep((ChartSweepView) child, parentRect, childRect);
                child.layout(childRect.left, childRect.top, childRect.right, childRect.bottom);
            }
        }
    }

    protected void layoutSweep(ChartSweepView sweep) {
        final Rect parentRect = new Rect(mContent);
        final Rect childRect = new Rect();

        layoutSweep(sweep, parentRect, childRect);
        sweep.layout(childRect.left, childRect.top, childRect.right, childRect.bottom);
    }

    protected void layoutSweep(ChartSweepView sweep, Rect parentRect, Rect childRect) {
        final Rect sweepMargins = sweep.getMargins();

        // sweep is always placed along specific dimension
        if (sweep.getFollowAxis() == ChartSweepView.VERTICAL) {
            parentRect.top += sweepMargins.top + (int) sweep.getPoint();
            parentRect.bottom = parentRect.top;
            parentRect.left += sweepMargins.left;
            parentRect.right += sweepMargins.right;
            Gravity.apply(SWEEP_GRAVITY, parentRect.width(), sweep.getMeasuredHeight(),
                    parentRect, childRect);

        } else {
            parentRect.left += sweepMargins.left + (int) sweep.getPoint();
            parentRect.right = parentRect.left;
            parentRect.top += sweepMargins.top;
            parentRect.bottom += sweepMargins.bottom;
            Gravity.apply(SWEEP_GRAVITY, sweep.getMeasuredWidth(), parentRect.height(),
                    parentRect, childRect);
        }
    }

}
