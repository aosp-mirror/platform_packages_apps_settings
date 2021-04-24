/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.android.settings.fuelgauge;

import static java.lang.Math.round;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import androidx.appcompat.widget.AppCompatImageView;

import com.android.settings.R;
import com.android.settingslib.Utils;

import java.util.Locale;

/** A widget component to draw chart graph. */
public class BatteryChartView extends AppCompatImageView implements View.OnClickListener {
    private static final String TAG = "BatteryChartView";
    private static final int DEFAULT_TRAPEZOID_COUNT = 12;
    /** Selects all trapezoid shapes. */
    public static final int SELECTED_INDEX_ALL = -1;
    public static final int SELECTED_INDEX_INVALID = -2;

    /** A callback listener for selected group index is updated. */
    public interface OnSelectListener {
        void onSelect(int trapezoidIndex);
    }

    private int mDividerWidth;
    private int mDividerHeight;
    private int mTrapezoidCount;
    private int mSelectedIndex;
    private float mTrapezoidVOffset;
    private float mTrapezoidHOffset;
    // Colors for drawing the trapezoid shape and dividers.
    private int mTrapezoidColor;
    private int mTrapezoidSolidColor;
    private final int mDividerColor = Color.parseColor("#CDCCC5");

    private int[] mLevels;
    private Paint mDividerPaint;
    private Paint mTrapezoidPaint;
    private TrapezoidSlot[] mTrapezoidSlot;
    // Records the location to calculate selected index.
    private MotionEvent mTouchUpEvent;
    private BatteryChartView.OnSelectListener mOnSelectListener;

    public BatteryChartView(Context context) {
        super(context, null);
    }

    public BatteryChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initializeColors(context);
        // Registers the click event listener.
        setOnClickListener(this);
        setClickable(false);
        setSelectedIndex(SELECTED_INDEX_ALL);
        setTrapezoidCount(DEFAULT_TRAPEZOID_COUNT);
    }

    /** Sets the total trapezoid count for drawing. */
    public void setTrapezoidCount(int trapezoidCount) {
        Log.i(TAG, "trapezoidCount:" + trapezoidCount);
        mTrapezoidCount = trapezoidCount;
        mTrapezoidSlot = new TrapezoidSlot[trapezoidCount];
        // Allocates the trapezoid slot array.
        for (int index = 0; index < trapezoidCount; index++) {
            mTrapezoidSlot[index] = new TrapezoidSlot();
        }
        invalidate();
    }

    /** Sets all levels value to draw the trapezoid shape */
    public void setLevels(int[] levels) {
        // We should provide trapezoid count + 1 data to draw all trapezoids.
        mLevels = levels.length == mTrapezoidCount + 1 ? levels : null;
        setClickable(false);
        invalidate();
        if (mLevels == null) {
            return;
        }
        // Sets the chart is clickable if there is at least one valid item in it.
        for (int index = 0; index < mLevels.length - 1; index++) {
            if (mLevels[index] != 0 && mLevels[index + 1] != 0) {
                setClickable(true);
                break;
            }
        }
    }

    /** Sets the selected group index to draw highlight effect. */
    public void setSelectedIndex(int index) {
        if (mSelectedIndex != index) {
            mSelectedIndex = index;
            invalidate();
            // Callbacks to the listener if we have.
            if (mOnSelectListener != null) {
                mOnSelectListener.onSelect(mSelectedIndex);
            }
        }
    }

    /** Sets the callback to monitor the selected group index. */
    public void setOnSelectListener(BatteryChartView.OnSelectListener listener) {
        mOnSelectListener = listener;
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        drawHorizontalDividers(canvas);
        drawVerticalDividers(canvas);
        drawTrapezoids(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Caches the location to calculate selected trapezoid index.
        final int action = event.getAction();
        if (action == MotionEvent.ACTION_UP) {
            mTouchUpEvent = MotionEvent.obtain(event);
        } else if (action == MotionEvent.ACTION_CANCEL) {
            mTouchUpEvent = null; // reset
        }
        return super.onTouchEvent(event);
    }

    @Override
    public void onClick(View view) {
        if (mTouchUpEvent == null) {
            Log.w(TAG, "invalid motion event for onClick() callback");
            return;
        }
        final int trapezoidIndex = getTrapezoidIndex(mTouchUpEvent.getX());
        // Ignores the click event if the level is zero.
        if (trapezoidIndex == SELECTED_INDEX_INVALID
                || (trapezoidIndex >= 0 && mLevels[trapezoidIndex] == 0)) {
            return;
        }
        // Selects all if users click the same trapezoid item two times.
        if (trapezoidIndex == mSelectedIndex) {
            setSelectedIndex(SELECTED_INDEX_ALL);
        } else {
            setSelectedIndex(trapezoidIndex);
        }
        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);
    }

    private void initializeColors(Context context) {
        setBackgroundColor(Color.TRANSPARENT);
        mTrapezoidSolidColor = Utils.getColorAccentDefaultColor(context);
        mTrapezoidColor = Utils.getDisabled(context, mTrapezoidSolidColor);
        // Initializes the divider line paint.
        final Resources resources = getContext().getResources();
        mDividerWidth = resources.getDimensionPixelSize(R.dimen.chartview_divider_width);
        mDividerHeight = resources.getDimensionPixelSize(R.dimen.chartview_divider_height);
        mDividerPaint = new Paint();
        mDividerPaint.setAntiAlias(true);
        mDividerPaint.setColor(mDividerColor);
        mDividerPaint.setStyle(Paint.Style.STROKE);
        mDividerPaint.setStrokeWidth(mDividerWidth);
        Log.i(TAG, "mDividerWidth:" + mDividerWidth);
        Log.i(TAG, "mDividerHeight:" + mDividerHeight);
        // Initializes the trapezoid paint.
        mTrapezoidHOffset = resources.getDimension(R.dimen.chartview_trapezoid_margin_start);
        mTrapezoidVOffset = resources.getDimension(R.dimen.chartview_trapezoid_margin_bottom);
        mTrapezoidPaint = new Paint();
        mTrapezoidPaint.setAntiAlias(true);
        mTrapezoidPaint.setColor(mTrapezoidSolidColor);
        mTrapezoidPaint.setStyle(Paint.Style.FILL);
        mTrapezoidPaint.setPathEffect(
            new CornerPathEffect(
                resources.getDimensionPixelSize(R.dimen.chartview_trapezoid_radius)));
    }

    private void drawHorizontalDividers(Canvas canvas) {
        // Draws the top divider line for 100% curve.
        float offsetY = mDividerWidth * 0.5f;
        canvas.drawLine(0, offsetY, getWidth(), offsetY, mDividerPaint);
        // Draws the center divider line for 50% curve.
        final float availableSpace =
                getHeight() - mDividerWidth * 2 - mTrapezoidVOffset - mDividerHeight;
        offsetY = mDividerWidth + availableSpace * 0.5f;
        canvas.drawLine(0, offsetY, getWidth(), offsetY, mDividerPaint);
        // Draws the center divider line for 0% curve.
        offsetY = getHeight() - mDividerHeight - mDividerWidth * 0.5f;
        canvas.drawLine(0, offsetY, getWidth(), offsetY, mDividerPaint);
    }

    private void drawVerticalDividers(Canvas canvas) {
        final int dividerCount = mTrapezoidCount + 1;
        final float dividerSpace = dividerCount * mDividerWidth;
        final float unitWidth = (getWidth() - dividerSpace) / (float) mTrapezoidCount;
        final float startY = getHeight() - mDividerHeight;
        final float trapezoidSlotOffset = mTrapezoidHOffset + mDividerWidth * 0.5f;
        // Draws each vertical dividers.
        float startX = mDividerWidth * 0.5f;
        for (int index = 0; index < dividerCount; index++) {
            canvas.drawLine(startX, startY, startX, getHeight(), mDividerPaint);
            final float nextX = startX + mDividerWidth + unitWidth;
            // Updates the trapezoid slots for drawing.
            if (index < mTrapezoidSlot.length) {
                mTrapezoidSlot[index].mLeft = round(startX + trapezoidSlotOffset);
                mTrapezoidSlot[index].mRight = round(nextX - trapezoidSlotOffset);
            }
            startX = nextX;
        }
    }

    private void drawTrapezoids(Canvas canvas) {
        // Ignores invalid trapezoid data.
        if (mLevels == null) {
            return;
        }
        final float trapezoidBottom =
            getHeight() - mDividerHeight - mDividerWidth - mTrapezoidVOffset;
        final float availableSpace = trapezoidBottom - mDividerWidth;
        final float unitHeight = availableSpace / 100f;
        // Draws all trapezoid shapes into the canvas.
        final Path trapezoidPath = new Path();
        for (int index = 0; index < mTrapezoidCount; index++) {
            // Not draws the trapezoid for corner or not initialization cases.
            if (mLevels[index] == 0 || mLevels[index + 1] == 0) {
                continue;
            }
            // Configures the trapezoid paint color.
            mTrapezoidPaint.setColor(
                mSelectedIndex == index || mSelectedIndex == SELECTED_INDEX_ALL
                ? mTrapezoidSolidColor
                : mTrapezoidColor);
            final float leftTop = round(trapezoidBottom - mLevels[index] * unitHeight);
            final float rightTop = round(trapezoidBottom - mLevels[index + 1] * unitHeight);
            trapezoidPath.reset();
            trapezoidPath.moveTo(mTrapezoidSlot[index].mLeft, trapezoidBottom);
            trapezoidPath.lineTo(mTrapezoidSlot[index].mLeft, leftTop);
            trapezoidPath.lineTo(mTrapezoidSlot[index].mRight, rightTop);
            trapezoidPath.lineTo(mTrapezoidSlot[index].mRight, trapezoidBottom);
            // A tricky way to make the trapezoid shape drawing the rounded corner.
            trapezoidPath.lineTo(mTrapezoidSlot[index].mLeft, trapezoidBottom);
            trapezoidPath.lineTo(mTrapezoidSlot[index].mLeft, leftTop);
            // Draws the trapezoid shape into canvas.
            canvas.drawPath(trapezoidPath, mTrapezoidPaint);
        }
    }

    // Searches the corresponding trapezoid index from x location.
    private int getTrapezoidIndex(float x) {
        for (int index = 0; index < mTrapezoidSlot.length; index++) {
            final TrapezoidSlot slot = mTrapezoidSlot[index];
            if (x >= slot.mLeft - mTrapezoidHOffset
                    && x <= slot.mRight + mTrapezoidHOffset) {
                return index;
            }
        }
        return SELECTED_INDEX_INVALID;
    }

    // A container class for each trapezoid left and right location.
    private static final class TrapezoidSlot {
        public float mLeft;
        public float mRight;

        @Override
        public String toString() {
            return String.format(Locale.US, "TrapezoidSlot[%f,%f]", mLeft, mRight);
        }
    }
}
