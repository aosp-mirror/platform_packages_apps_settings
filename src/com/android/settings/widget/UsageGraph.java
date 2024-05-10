/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.annotation.Nullable;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.CornerPathEffect;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.fuelgauge.BatteryUtils;

public class UsageGraph extends View {

    private static final int PATH_DELIM = -1;
    public static final String LOG_TAG = "UsageGraph";

    private final Paint mLinePaint;
    private final Paint mFillPaint;
    private final Paint mDottedPaint;

    private final Drawable mDivider;
    private final Drawable mTintedDivider;
    private final int mDividerSize;

    private final Path mPath = new Path();

    // Paths in coordinates they are passed in.
    private final SparseIntArray mPaths = new SparseIntArray();
    // Paths in local coordinates for drawing.
    private final SparseIntArray mLocalPaths = new SparseIntArray();

    // Paths for projection in coordinates they are passed in.
    private final SparseIntArray mProjectedPaths = new SparseIntArray();
    // Paths for projection in local coordinates for drawing.
    private final SparseIntArray mLocalProjectedPaths = new SparseIntArray();

    private final int mCornerRadius;
    private int mAccentColor;

    private float mMaxX = 100;
    private float mMaxY = 100;

    private float mMiddleDividerLoc = .5f;
    private int mMiddleDividerTint = -1;
    private int mTopDividerTint = -1;

    public UsageGraph(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        final Resources resources = context.getResources();

        mLinePaint = new Paint();
        mLinePaint.setStyle(Style.STROKE);
        mLinePaint.setStrokeCap(Cap.ROUND);
        mLinePaint.setStrokeJoin(Join.ROUND);
        mLinePaint.setAntiAlias(true);
        mCornerRadius = resources.getDimensionPixelSize(
                com.android.settingslib.R.dimen.usage_graph_line_corner_radius);
        mLinePaint.setPathEffect(new CornerPathEffect(mCornerRadius));
        mLinePaint.setStrokeWidth(resources.getDimensionPixelSize(
                com.android.settingslib.R.dimen.usage_graph_line_width));

        mFillPaint = new Paint(mLinePaint);
        mFillPaint.setStyle(Style.FILL);

        mDottedPaint = new Paint(mLinePaint);
        mDottedPaint.setStyle(Style.STROKE);
        float dots = resources.getDimensionPixelSize(
                com.android.settingslib.R.dimen.usage_graph_dot_size);
        float interval = resources.getDimensionPixelSize(
                com.android.settingslib.R.dimen.usage_graph_dot_interval);
        mDottedPaint.setStrokeWidth(dots * 3);
        mDottedPaint.setPathEffect(new DashPathEffect(new float[] {dots, interval}, 0));
        mDottedPaint.setColor(context.getColor(R.color.usage_graph_dots));

        TypedValue v = new TypedValue();
        context.getTheme().resolveAttribute(com.android.internal.R.attr.listDivider, v, true);
        mDivider = context.getDrawable(v.resourceId);
        mTintedDivider = context.getDrawable(v.resourceId);
        mDividerSize = resources.getDimensionPixelSize(
                com.android.settingslib.R.dimen.usage_graph_divider_size);
    }

    void clearPaths() {
        mPaths.clear();
        mLocalPaths.clear();
        mProjectedPaths.clear();
        mLocalProjectedPaths.clear();
    }

    void setMax(int maxX, int maxY) {
        final long startTime = System.currentTimeMillis();
        mMaxX = maxX;
        mMaxY = maxY;
        calculateLocalPaths();
        postInvalidate();
        BatteryUtils.logRuntime(LOG_TAG, "setMax", startTime);
    }

    void setDividerLoc(int height) {
        mMiddleDividerLoc = 1 - height / mMaxY;
    }

    void setDividerColors(int middleColor, int topColor) {
        mMiddleDividerTint = middleColor;
        mTopDividerTint = topColor;
    }

    public void addPath(SparseIntArray points) {
        addPathAndUpdate(points, mPaths, mLocalPaths);
    }

    public void addProjectedPath(SparseIntArray points) {
        addPathAndUpdate(points, mProjectedPaths, mLocalProjectedPaths);
    }

    private void addPathAndUpdate(
            SparseIntArray points, SparseIntArray paths, SparseIntArray localPaths) {
        final long startTime = System.currentTimeMillis();
        for (int i = 0, size = points.size(); i < size; i++) {
            paths.put(points.keyAt(i), points.valueAt(i));
        }
        // Add a delimiting value immediately after the last point.
        paths.put(points.keyAt(points.size() - 1) + 1, PATH_DELIM);
        calculateLocalPaths(paths, localPaths);
        postInvalidate();
        BatteryUtils.logRuntime(LOG_TAG, "addPathAndUpdate", startTime);
    }

    void setAccentColor(int color) {
        mAccentColor = color;
        mLinePaint.setColor(mAccentColor);
        updateGradient();
        postInvalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        final long startTime = System.currentTimeMillis();
        super.onSizeChanged(w, h, oldw, oldh);
        updateGradient();
        calculateLocalPaths();
        BatteryUtils.logRuntime(LOG_TAG, "onSizeChanged", startTime);
    }

    private void calculateLocalPaths() {
        calculateLocalPaths(mPaths, mLocalPaths);
        calculateLocalPaths(mProjectedPaths, mLocalProjectedPaths);
    }

    @VisibleForTesting
    void calculateLocalPaths(SparseIntArray paths, SparseIntArray localPaths) {
        final long startTime = System.currentTimeMillis();
        if (getWidth() == 0) {
            return;
        }
        localPaths.clear();
        // Store the local coordinates of the most recent point.
        int lx = 0;
        int ly = PATH_DELIM;
        boolean skippedLastPoint = false;
        for (int i = 0; i < paths.size(); i++) {
            int x = paths.keyAt(i);
            int y = paths.valueAt(i);
            if (y == PATH_DELIM) {
                if (i == 1) {
                    localPaths.put(getX(x+1) - 1, getY(0));
                    continue;
                }
                if (i == paths.size() - 1 && skippedLastPoint) {
                    // Add back skipped point to complete the path.
                    localPaths.put(lx, ly);
                }
                skippedLastPoint = false;
                localPaths.put(lx + 1, PATH_DELIM);
            } else {
                lx = getX(x);
                ly = getY(y);
                // Skip this point if it is not far enough from the last one added.
                if (localPaths.size() > 0) {
                    int lastX = localPaths.keyAt(localPaths.size() - 1);
                    int lastY = localPaths.valueAt(localPaths.size() - 1);
                    if (lastY != PATH_DELIM && !hasDiff(lastX, lx) && !hasDiff(lastY, ly)) {
                        skippedLastPoint = true;
                        continue;
                    }
                }
                skippedLastPoint = false;
                localPaths.put(lx, ly);
            }
        }
        BatteryUtils.logRuntime(LOG_TAG, "calculateLocalPaths", startTime);
    }

    private boolean hasDiff(int x1, int x2) {
        return Math.abs(x2 - x1) >= mCornerRadius;
    }

    private int getX(float x) {
        return (int) (x / mMaxX * getWidth());
    }

    private int getY(float y) {
        return (int) (getHeight() * (1 - (y / mMaxY)));
    }

    private void updateGradient() {
        mFillPaint.setShader(
                new LinearGradient(
                        0, 0, 0, getHeight(), getColor(mAccentColor, .2f), 0, TileMode.CLAMP));
    }

    private int getColor(int color, float alphaScale) {
        return (color & (((int) (0xff * alphaScale) << 24) | 0xffffff));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        final long startTime = System.currentTimeMillis();
        // Draw lines across the top, middle, and bottom.
        if (mMiddleDividerLoc != 0) {
            drawDivider(0, canvas, mTopDividerTint);
        }
        drawDivider(
                (int) ((canvas.getHeight() - mDividerSize) * mMiddleDividerLoc),
                canvas,
                mMiddleDividerTint);
        drawDivider(canvas.getHeight() - mDividerSize, canvas, -1);

        if (mLocalPaths.size() == 0 && mLocalProjectedPaths.size() == 0) {
            return;
        }

        canvas.save();
        if (getLayoutDirection() == LAYOUT_DIRECTION_RTL) {
            // Flip the canvas along the y-axis of the center of itself before drawing paths.
            canvas.scale(-1, 1, canvas.getWidth() * 0.5f, 0);
        }
        drawLinePath(canvas, mLocalProjectedPaths, mDottedPaint);
        drawFilledPath(canvas, mLocalPaths, mFillPaint);
        drawLinePath(canvas, mLocalPaths, mLinePaint);
        canvas.restore();
        BatteryUtils.logRuntime(LOG_TAG, "onDraw", startTime);
    }

    private void drawLinePath(Canvas canvas, SparseIntArray localPaths, Paint paint) {
        if (localPaths.size() == 0) {
            return;
        }
        mPath.reset();
        mPath.moveTo(localPaths.keyAt(0), localPaths.valueAt(0));
        for (int i = 1; i < localPaths.size(); i++) {
            int x = localPaths.keyAt(i);
            int y = localPaths.valueAt(i);
            if (y == PATH_DELIM) {
                if (++i < localPaths.size()) {
                    mPath.moveTo(localPaths.keyAt(i), localPaths.valueAt(i));
                }
            } else {
                mPath.lineTo(x, y);
            }
        }
        canvas.drawPath(mPath, paint);
    }

    @VisibleForTesting
    void drawFilledPath(Canvas canvas, SparseIntArray localPaths, Paint paint) {
        if (localPaths.size() == 0) {
            return;
        }
        mPath.reset();
        float lastStartX = localPaths.keyAt(0);
        mPath.moveTo(localPaths.keyAt(0), localPaths.valueAt(0));
        for (int i = 1; i < localPaths.size(); i++) {
            int x = localPaths.keyAt(i);
            int y = localPaths.valueAt(i);
            if (y == PATH_DELIM) {
                mPath.lineTo(localPaths.keyAt(i - 1), getHeight());
                mPath.lineTo(lastStartX, getHeight());
                mPath.close();
                if (++i < localPaths.size()) {
                    lastStartX = localPaths.keyAt(i);
                    mPath.moveTo(localPaths.keyAt(i), localPaths.valueAt(i));
                }
            } else {
                mPath.lineTo(x, y);
            }
        }
        canvas.drawPath(mPath, paint);
    }

    private void drawDivider(int y, Canvas canvas, int tintColor) {
        Drawable d = mDivider;
        if (tintColor != -1) {
            mTintedDivider.setTint(tintColor);
            d = mTintedDivider;
        }
        d.setBounds(0, y, canvas.getWidth(), y + mDividerSize);
        d.draw(canvas);
    }
}
