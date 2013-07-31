/*
 * Copyright (C) 2013 The CyanogenMod Project
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
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Shader;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Spline;
import android.view.SurfaceView;
import android.view.View;

import com.android.settings.R;

public class CubicSplinePreviewView extends SurfaceView {
    private static final String TAG = "CubicSplinePreviewView";
    private static final boolean DEBUG = false;

    private float[] mXPoints;
    private float[] mYPoints;
    private Spline mSpline;

    private static final int POINTS = 100;

    private final Paint mFgPaint, mGridLinePaint;
    private final Paint mXTextPaint, mYTextPaint;
    private final Paint mPointPaint;
    private final int mBgColor;
    private final float mMarkerRadius;
    private final int mMargin;

    public CubicSplinePreviewView(Context context) {
        this(context, null);
    }

    public CubicSplinePreviewView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CubicSplinePreviewView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        final TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.CubicSplinePreviewView, defStyle, 0);

        int fgColor = a.getColor(R.styleable.CubicSplinePreviewView_foregroundColor, Color.WHITE);
        int markerColor = a.getColor(R.styleable.CubicSplinePreviewView_markerColor, 0x22ffffff);
        int gridColor = a.getColor(R.styleable.CubicSplinePreviewView_gridColor, Color.WHITE);
        mBgColor = a.getColor(R.styleable.CubicSplinePreviewView_backgroundColor, Color.BLACK);

        float textSize = a.getDimensionPixelSize(R.styleable.CubicSplinePreviewView_textSize, 0);
        float strokeWidth = a.getDimensionPixelSize(R.styleable.CubicSplinePreviewView_strokeWidth, 0);
        mMarkerRadius = a.getDimensionPixelSize(R.styleable.CubicSplinePreviewView_markerSize, 1);
        mMargin = a.getDimensionPixelSize(R.styleable.CubicSplinePreviewView_margin, 0);

        a.recycle();

        mFgPaint = new Paint();
        mFgPaint.setColor(fgColor);
        mFgPaint.setStyle(Style.STROKE);
        mFgPaint.setStrokeWidth(strokeWidth);
        mFgPaint.setTextSize(textSize);
        mFgPaint.setAntiAlias(true);

        mGridLinePaint = new Paint();
        mGridLinePaint.setColor(gridColor);
        mGridLinePaint.setStyle(Style.STROKE);

        mXTextPaint = new Paint(mFgPaint);
        mXTextPaint.setTextAlign(Align.CENTER);
        mXTextPaint.setStrokeWidth(0);
        mXTextPaint.setShadowLayer(2, 0, 0, 0xff000000);

        mYTextPaint = new Paint(mXTextPaint);
        mYTextPaint.setTextAlign(Align.LEFT);

        mPointPaint = new Paint();
        mPointPaint.setStyle(Style.FILL);
        mPointPaint.setColor(markerColor);
        mPointPaint.setAntiAlias(true);

        setWillNotDraw(false);
    }

    /**
     * Sets the spline control points.
     *
     * @param xPoints  Array of X coordinates of control points
     * @param yPoints  Array of Y coordinates of control points
     *
     * There are some assumptions made about those arrays:
     * - xPoints and yPoints must be of equal length
     * - The value range of yPoints is 0..1
     */
    public void setSpline(float[] xPoints, float[] yPoints) {
        mXPoints = xPoints;
        mYPoints = yPoints;
        if (DEBUG) {
            for (int i = 0; i < xPoints.length; i++) {
                Log.d(TAG, "Spline data[" + i + "]: x = " + xPoints[i] + " y = " + yPoints[i]);
            }
        }
        mSpline = Spline.createMonotoneCubicSpline(xPoints, yPoints);
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        /* clear canvas */
        canvas.drawRGB(Color.red(mBgColor), Color.green(mBgColor), Color.blue(mBgColor));

        if (mSpline == null) {
            return;
        }

        Path curve = new Path();

        int width = getWidth() - 2 * mMargin;
        int height = getHeight() - 2 * mMargin;
        double dist = (double) width / (POINTS - 1);

        for (int i = 0; i < POINTS; i++) {
            double xPixel = dist * i;
            float x = (float) reverseProjectX(xPixel / width);
            float y = mSpline.interpolate(x);
            float yPixel = (float) ((1.0 - projectY(y)) * height);

            xPixel += mMargin;
            yPixel += mMargin;

            if (DEBUG) {
                Log.d(TAG, "point[" + i + "]: X = (" + x + "," + xPixel + "), Y = (" + y + "," + yPixel + ")");
            }

            if (i == 0) {
                curve.moveTo((float) xPixel, yPixel);
            } else {
                curve.lineTo((float) xPixel, yPixel);
            }
        }

        canvas.drawPath(curve, mFgPaint);

        /* draw vertical lines */
        float minX = getMinX();
        float maxX = getMaxX();
        float minY = getMinY();
        float maxY = getMaxY();

        for (float xPos = minX; xPos <= maxX; ) {
            float x = (float) (projectX(xPos) * width + mMargin);
            canvas.drawLine(x, mMargin, x, mMargin + height, mGridLinePaint);
            if (xPos < 10) {
                xPos += 1;
            } else if (xPos < 100) {
                xPos += 10;
            } else if (xPos < 1000) {
                xPos += 100;
            } else if (xPos < 10000) {
                xPos += 1000;
            } else {
                xPos += 10000;
            }
        }

        /* draw horizontal lines */
        canvas.drawLine(mMargin, mMargin + height, mMargin + width, mMargin + height, mGridLinePaint);
        float yDist = (maxY - minY) / 10;
        for (int i = 1; i <= 10; i++) {
            float y = (float) ((1.0 - projectY(yDist * i + minY)) * height + mMargin);
            canvas.drawLine(mMargin, y, mMargin + width, y, mGridLinePaint);
            canvas.drawText(String.format("%.0f%%", yDist * i * 100), mMargin + 1,
                    y + mYTextPaint.getTextSize(), mYTextPaint);
        }

        for (int i = 0; i < mXPoints.length; i ++) {
            /* take special care of the first control point that's likely 0 */
            float x = (i == 0) ? getMinX() : mXPoints[i];
            float y = (x != mXPoints[i]) ? mSpline.interpolate(x) : mYPoints[i];
            float xPixel = (float) (projectX(x) * width + mMargin);
            float yPixel = (float) ((1.0 - projectY(y)) * height + mMargin);

            if (DEBUG) {
                Log.d(TAG, "Print control point " + x + " at (" + xPixel + "," + (height - 2) + ")");
            }

            if (i == 0) {
                mXTextPaint.setTextAlign(Align.LEFT);
            } else if (i == (mXPoints.length - 1)) {
                mXTextPaint.setTextAlign(Align.RIGHT);
            } else {
                mXTextPaint.setTextAlign(Align.CENTER);
            }
            canvas.drawCircle(xPixel, yPixel, mMarkerRadius, mPointPaint);
            canvas.drawText(String.format("%.0f", mXPoints[i]), xPixel, mMargin + height - 2, mXTextPaint);
        }
    }

    private double projectX(double value) {
        double pos = Math.log(value);
        double minPos = Math.log(getMinX());
        double maxPos = Math.log(getMaxX());
        return (pos - minPos) / (maxPos - minPos);
    }

    private double reverseProjectX(double pos) {
        double minPos = Math.log(getMinX());
        double maxPos = Math.log(getMaxX());
        return Math.exp(pos * (maxPos - minPos) + minPos);
    }

    private double projectY(double value) {
        double min = getMinY();
        double max = getMaxY();
        return (value - min) / (max - min);
    }

    private float getMinX() {
        return Math.max(mXPoints[0], 1);
    }

    private float getMaxX() {
        return mXPoints[mXPoints.length - 1];
    }

    private float getMinY() {
        return Math.min(mYPoints[0], 0);
    }

    private float getMaxY() {
        return Math.max(mYPoints[mYPoints.length - 1], 1);
    }
}
