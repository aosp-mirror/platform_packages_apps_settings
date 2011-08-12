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
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader.TileMode;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.google.common.collect.Lists;

import java.util.ArrayList;

/**
 * Pie chart with multiple items.
 */
public class PieChartView extends View {
    public static final String TAG = "PieChartView";
    public static final boolean LOGD = true;

    private ArrayList<Slice> mSlices = Lists.newArrayList();

    private int mOriginAngle;

    private Paint mPaintPrimary = new Paint();
    private Paint mPaintShadow = new Paint();

    private Path mPathSide = new Path();
    private Path mPathSideShadow = new Path();

    private Path mPathShadow = new Path();

    private int mSideWidth;

    public class Slice {
        public long value;

        public Path pathPrimary = new Path();
        public Path pathShadow = new Path();

        public Paint paintPrimary;

        public Slice(long value, int color) {
            this.value = value;
            this.paintPrimary = buildFillPaint(color, getResources());
        }
    }

    public PieChartView(Context context) {
        this(context, null);
    }

    public PieChartView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PieChartView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mPaintPrimary = buildFillPaint(Color.parseColor("#666666"), getResources());

        mPaintShadow.setColor(Color.BLACK);
        mPaintShadow.setStyle(Style.STROKE);
        mPaintShadow.setStrokeWidth(3f * getResources().getDisplayMetrics().density);
        mPaintShadow.setAntiAlias(true);

        mSideWidth = (int) (20 * getResources().getDisplayMetrics().density);

        setWillNotDraw(false);
    }

    private static Paint buildFillPaint(int color, Resources res) {
        final Paint paint = new Paint();

        paint.setColor(color);
        paint.setStyle(Style.FILL_AND_STROKE);
        paint.setAntiAlias(true);

        final int width = (int) (280 * res.getDisplayMetrics().density);
        paint.setShader(new RadialGradient(0, 0, width, color, darken(color), TileMode.MIRROR));

        return paint;
    }

    public void setOriginAngle(int originAngle) {
        mOriginAngle = originAngle;
    }

    public void addSlice(long value, int color) {
        mSlices.add(new Slice(value, color));
    }

    public void removeAllSlices() {
        mSlices.clear();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        generatePath();
    }

    public void generatePath() {
        if (LOGD) Log.d(TAG, "generatePath()");

        long total = 0;
        for (Slice slice : mSlices) {
            slice.pathPrimary.reset();
            slice.pathShadow.reset();
            total += slice.value;
        }

        mPathSide.reset();
        mPathSideShadow.reset();
        mPathShadow.reset();

        // bail when not enough stats to render
        if (total == 0) {
            invalidate();
            return;
        }

        final int width = getWidth();
        final int height = getHeight();

        final RectF rect = new RectF(0, 0, width, height);

        mPathSide.addOval(rect, Direction.CW);
        mPathSideShadow.addOval(rect, Direction.CW);
        mPathShadow.addOval(rect, Direction.CW);

        int startAngle = mOriginAngle;
        for (Slice slice : mSlices) {
            final int sweepAngle = (int) (slice.value * 360 / total);

            slice.pathPrimary.moveTo(rect.centerX(), rect.centerY());
            slice.pathPrimary.arcTo(rect, startAngle, sweepAngle);
            slice.pathPrimary.lineTo(rect.centerX(), rect.centerY());

            slice.pathShadow.moveTo(rect.centerX(), rect.centerY());
            slice.pathShadow.arcTo(rect, startAngle, 0);
            slice.pathShadow.moveTo(rect.centerX(), rect.centerY());
            slice.pathShadow.arcTo(rect, startAngle + sweepAngle, 0);

            startAngle += sweepAngle;
        }

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {

        canvas.translate(getWidth() * 0.25f, getHeight() * -0.05f);
        canvas.rotate(-40, getWidth() * 0.5f, getHeight());
        canvas.scale(0.7f, 1.0f, getWidth(), getHeight());

        canvas.save();
        canvas.translate(-mSideWidth, 0);
        canvas.drawPath(mPathSide, mPaintPrimary);
        canvas.drawPath(mPathSideShadow, mPaintShadow);
        canvas.restore();

        for (Slice slice : mSlices) {
            canvas.drawPath(slice.pathPrimary, slice.paintPrimary);
            canvas.drawPath(slice.pathShadow, mPaintShadow);
        }
        canvas.drawPath(mPathShadow, mPaintShadow);
    }

    public static int darken(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] /= 2;
        hsv[1] /= 2;
        return Color.HSVToColor(hsv);
    }

}
