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

package com.android.settings.wifi.qrcode;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import com.android.settings.R;

/**
 * Draws the lines at the corner of the inner frame.
 */
public class QrDecorateView extends View {
    private static final float CORNER_STROKE_WIDTH = 3f;    // 3dp
    private static final float CORNER_LINE_LENGTH = 20f;    // 20dp

    final private Paint mPaint;
    private RectF mFrame;
    private boolean mFocused;

    public QrDecorateView(Context context) {
        this(context, null);
    }

    public QrDecorateView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public QrDecorateView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public QrDecorateView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        final float strokeWidth = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                CORNER_STROKE_WIDTH,
                getResources().getDisplayMetrics()
        );
        mPaint = new Paint();
        mPaint.setStrokeWidth(strokeWidth);
        mFocused = false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        calculateFramePos();
        final float cornerLineLength = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                CORNER_LINE_LENGTH,
                getResources().getDisplayMetrics()
        );
        mPaint.setColor(mFocused ? Color.GREEN : Color.WHITE);
        drawCorner(mFrame, cornerLineLength, canvas);
        super.onDraw(canvas);
    }

    private void drawCorner(RectF frame, float lineLength, Canvas canvas) {
        final float strokeWidth = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                CORNER_STROKE_WIDTH,
                getResources().getDisplayMetrics()
        );
        // Draw top-left corner.
        canvas.drawLine(
                frame.left - strokeWidth / 2,
                frame.top,
                frame.left + lineLength,
                frame.top,
                mPaint);
        canvas.drawLine(frame.left, frame.top, frame.left, frame.top + lineLength, mPaint);
        // Draw top-right corner.
        canvas.drawLine(
                frame.right + strokeWidth / 2,
                frame.top,
                frame.right - lineLength,
                frame.top,
                mPaint);
        canvas.drawLine(frame.right, frame.top, frame.right, frame.top + lineLength, mPaint);
        // Draw bottom-left corner.
        canvas.drawLine(
                frame.left - strokeWidth / 2,
                frame.bottom,
                frame.left + lineLength,
                frame.bottom,
                mPaint);
        canvas.drawLine(frame.left, frame.bottom, frame.left, frame.bottom - lineLength, mPaint);
        // Draw bottom-right corner.
        canvas.drawLine(
                frame.right + strokeWidth / 2,
                frame.bottom,
                frame.right - lineLength,
                frame.bottom,
                mPaint);
        canvas.drawLine(frame.right, frame.bottom, frame.right, frame.bottom - lineLength, mPaint);
    }

    private void calculateFramePos() {
        final int centralX = getWidth() / 2;
        final int centralY = getHeight() / 2;
        final float halfFrameWidth = getWidth() / 3;
        mFrame = new RectF(
                centralX - halfFrameWidth,
                centralY - halfFrameWidth,
                centralX + halfFrameWidth,
                centralY + halfFrameWidth);
    }

    // Draws green lines if focued. Otherwise, draws white lines.
    public void setFocused(boolean focused) {
        mFocused = focused;
        invalidate();
    }
}
