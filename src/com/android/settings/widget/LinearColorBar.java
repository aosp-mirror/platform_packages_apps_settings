/*
 * Copyright (C) 2018 The Android Open Source Project
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
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.widget.LinearLayout;

import com.android.settings.Utils;

/**
 * @Deprecated Use {@link android.widget.ProgressBar} instead.
 */
public class LinearColorBar extends LinearLayout {

    static final int RIGHT_COLOR = 0xffced7db;
    static final int GRAY_COLOR = 0xff555555;
    static final int WHITE_COLOR = 0xffffffff;

    private float mRedRatio;
    private float mYellowRatio;
    private float mGreenRatio;

    private int mLeftColor;
    private int mMiddleColor;
    private int mRightColor = RIGHT_COLOR;

    private int mColoredRegions = REGION_RED | REGION_YELLOW | REGION_GREEN;

    final Rect mRect = new Rect();
    final Paint mPaint = new Paint();

    int mLineWidth;

    int mLastRegion;

    final Paint mColorGradientPaint = new Paint();
    final Paint mEdgeGradientPaint = new Paint();

    public static final int REGION_RED = 1 << 0;
    public static final int REGION_YELLOW = 1 << 1;
    public static final int REGION_GREEN = 1 << 2;

    public LinearColorBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        mPaint.setStyle(Paint.Style.FILL);
        mColorGradientPaint.setStyle(Paint.Style.FILL);
        mColorGradientPaint.setAntiAlias(true);
        mEdgeGradientPaint.setStyle(Paint.Style.STROKE);
        mLineWidth = getResources().getDisplayMetrics().densityDpi >= DisplayMetrics.DENSITY_HIGH
                ? 2 : 1;
        mEdgeGradientPaint.setStrokeWidth(mLineWidth);
        mEdgeGradientPaint.setAntiAlias(true);
        mLeftColor = mMiddleColor = Utils.getColorAccent(context);
    }

    public void setRatios(float red, float yellow, float green) {
        mRedRatio = red;
        mYellowRatio = yellow;
        mGreenRatio = green;
        invalidate();
    }

    public void setColors(int red, int yellow, int green) {
        mLeftColor = red;
        mMiddleColor = yellow;
        mRightColor = green;
        updateIndicator();
        invalidate();
    }

    private void updateIndicator() {
        int off = getPaddingTop() - getPaddingBottom();
        if (off < 0) off = 0;
        mRect.top = off;
        mRect.bottom = getHeight();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateIndicator();
    }

    @Override
    protected void dispatchSetPressed(boolean pressed) {
        invalidate();
    }

    private int pickColor(int color, int region) {
        if (isPressed() && (mLastRegion & region) != 0) {
            return WHITE_COLOR;
        }
        if ((mColoredRegions & region) == 0) {
            return GRAY_COLOR;
        }
        return color;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        final int width = getWidth();

        if (!isLayoutRtl()) {
            drawLtr(canvas, width);
        } else {
            drawRtl(canvas, width);
        }
    }

    private void drawLtr(Canvas canvas, int width) {
        int start = 0;
        int end = start + (int) (width * mRedRatio);
        int end2 = end + (int) (width * mYellowRatio);

        if (start < end) {
            mRect.left = start;
            mRect.right = end;
            mPaint.setColor(pickColor(mLeftColor, REGION_RED));
            canvas.drawRect(mRect, mPaint);
            start = end;
        }

        end = end2;

        if (start < end) {
            mRect.left = start;
            mRect.right = end;
            mPaint.setColor(pickColor(mMiddleColor, REGION_YELLOW));
            canvas.drawRect(mRect, mPaint);
            start = end;
        }

        end = width;
        if (start < end) {
            mRect.left = start;
            mRect.right = end;
            mPaint.setColor(pickColor(mRightColor, REGION_GREEN));
            canvas.drawRect(mRect, mPaint);
        }
    }

    private void drawRtl(Canvas canvas, int width) {
        int start = width;
        int end = start - (int) (width * mRedRatio);
        int end2 = end - (int) (width * mYellowRatio);

        if (start > end) {
            mRect.left = end;
            mRect.right = start;
            mPaint.setColor(pickColor(mLeftColor, REGION_RED));
            canvas.drawRect(mRect, mPaint);
            start = end;
        }

        end = end2;

        if (start > end) {
            mRect.left = end;
            mRect.right = start;
            mPaint.setColor(pickColor(mMiddleColor, REGION_YELLOW));
            canvas.drawRect(mRect, mPaint);
            start = end;
        }

        end = 0;
        if (start > end) {
            mRect.left = end;
            mRect.right = start;
            mPaint.setColor(pickColor(mRightColor, REGION_GREEN));
            canvas.drawRect(mRect, mPaint);
        }
    }
}