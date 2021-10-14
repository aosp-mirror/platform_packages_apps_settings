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

import static com.android.settings.Utils.formatPercentage;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Handler;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.Utils;

import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** A widget component to draw chart graph. */
public class BatteryChartView extends AppCompatImageView implements View.OnClickListener,
        AccessibilityManager.AccessibilityStateChangeListener {
    private static final String TAG = "BatteryChartView";
    private static final List<String> ACCESSIBILITY_SERVICE_NAMES =
        Arrays.asList("SwitchAccessService", "TalkBackService", "JustSpeakService");

    private static final int DEFAULT_TRAPEZOID_COUNT = 12;
    private static final int DEFAULT_TIMESTAMP_COUNT = 5;
    private static final int DIVIDER_COLOR = Color.parseColor("#CDCCC5");
    private static final long UPDATE_STATE_DELAYED_TIME = 500L;

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
    private float mTrapezoidVOffset;
    private float mTrapezoidHOffset;
    private boolean mIsSlotsClickabled;
    private String[] mPercentages = getPercentages();

    @VisibleForTesting int mSelectedIndex;
    @VisibleForTesting String[] mTimestamps;

    // Colors for drawing the trapezoid shape and dividers.
    private int mTrapezoidColor;
    private int mTrapezoidSolidColor;
    // For drawing the percentage information.
    private int mTextPadding;
    private final Rect mIndent = new Rect();
    private final Rect[] mPercentageBounds =
        new Rect[] {new Rect(), new Rect(), new Rect()};
    // For drawing the timestamp information.
    private final Rect[] mTimestampsBounds =
        new Rect[] {new Rect(), new Rect(), new Rect(), new Rect(), new Rect()};

    @VisibleForTesting
    Handler mHandler = new Handler();
    @VisibleForTesting
    final Runnable mUpdateClickableStateRun = () -> updateClickableState();

    private int[] mLevels;
    private Paint mTextPaint;
    private Paint mDividerPaint;
    private Paint mTrapezoidPaint;

    @VisibleForTesting
    Paint mTrapezoidCurvePaint = null;
    private TrapezoidSlot[] mTrapezoidSlots;
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
        setSelectedIndex(SELECTED_INDEX_ALL);
        setTrapezoidCount(DEFAULT_TRAPEZOID_COUNT);
        setClickable(false);
        setLatestTimestamp(0);
    }

    /** Sets the total trapezoid count for drawing. */
    public void setTrapezoidCount(int trapezoidCount) {
        Log.i(TAG, "trapezoidCount:" + trapezoidCount);
        mTrapezoidCount = trapezoidCount;
        mTrapezoidSlots = new TrapezoidSlot[trapezoidCount];
        // Allocates the trapezoid slot array.
        for (int index = 0; index < trapezoidCount; index++) {
            mTrapezoidSlots[index] = new TrapezoidSlot();
        }
        invalidate();
    }

    /** Sets all levels value to draw the trapezoid shape */
    public void setLevels(int[] levels) {
        Log.d(TAG, "setLevels() " + (levels == null ? "null" : levels.length));
        if (levels == null) {
            mLevels = null;
            return;
        }
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

    /** Sets the companion {@link TextView} for percentage information. */
    public void setCompanionTextView(TextView textView) {
        if (textView != null) {
            // Pre-draws the view first to load style atttributions into paint.
            textView.draw(new Canvas());
            mTextPaint = textView.getPaint();
        } else {
            mTextPaint = null;
        }
        setVisibility(View.VISIBLE);
        requestLayout();
    }

    /** Sets the latest timestamp for drawing into x-axis information. */
    public void setLatestTimestamp(long latestTimestamp) {
        if (latestTimestamp == 0) {
            latestTimestamp = Clock.systemUTC().millis();
        }
        if (mTimestamps == null) {
            mTimestamps = new String[DEFAULT_TIMESTAMP_COUNT];
        }
        final long timeSlotOffset = DateUtils.HOUR_IN_MILLIS * 6;
        final boolean is24HourFormat = DateFormat.is24HourFormat(getContext());
        for (int index = 0; index < DEFAULT_TIMESTAMP_COUNT; index++) {
            mTimestamps[index] =
                ConvertUtils.utcToLocalTimeHour(
                    getContext(),
                    latestTimestamp - (4 - index) * timeSlotOffset,
                    is24HourFormat);
        }
        requestLayout();
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        // Measures text bounds and updates indent configuration.
        if (mTextPaint != null) {
            for (int index = 0; index < mPercentages.length; index++) {
                mTextPaint.getTextBounds(
                    mPercentages[index], 0, mPercentages[index].length(),
                    mPercentageBounds[index]);
            }
            // Updates the indent configurations.
            mIndent.top = mPercentageBounds[0].height();
            mIndent.right = mPercentageBounds[0].width() + mTextPadding;

            if (mTimestamps != null) {
                int maxHeight = 0;
                for (int index = 0; index < DEFAULT_TIMESTAMP_COUNT; index++) {
                    mTextPaint.getTextBounds(
                        mTimestamps[index], 0, mTimestamps[index].length(),
                        mTimestampsBounds[index]);
                    maxHeight = Math.max(maxHeight, mTimestampsBounds[index].height());
                }
                mIndent.bottom = maxHeight + round(mTextPadding * 1.5f);
            }
            Log.d(TAG, "setIndent:" + mPercentageBounds[0]);
        } else {
            mIndent.set(0, 0, 0, 0);
        }
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
                || !isValidToDraw(trapezoidIndex)) {
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

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateClickableState();
        mContext.getSystemService(AccessibilityManager.class)
            .addAccessibilityStateChangeListener(/*listener=*/ this);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mContext.getSystemService(AccessibilityManager.class)
            .removeAccessibilityStateChangeListener(/*listener=*/ this);
        mHandler.removeCallbacks(mUpdateClickableStateRun);
    }

    @Override
    public void onAccessibilityStateChanged(boolean enabled) {
        Log.d(TAG, "onAccessibilityStateChanged:" + enabled);
        mHandler.removeCallbacks(mUpdateClickableStateRun);
        // We should delay it a while since accessibility manager will spend
        // some times to bind with new enabled accessibility services.
        mHandler.postDelayed(
            mUpdateClickableStateRun, UPDATE_STATE_DELAYED_TIME);
    }

    private void updateClickableState() {
        final Context context = mContext;
        mIsSlotsClickabled =
            FeatureFactory.getFactory(context)
                    .getPowerUsageFeatureProvider(context)
                    .isChartGraphSlotsEnabled(context)
            && !isAccessibilityEnabled(context);
        Log.d(TAG, "isChartGraphSlotsEnabled:" + mIsSlotsClickabled);
        setClickable(isClickable());
        // Initializes the trapezoid curve paint for non-clickable case.
        if (!mIsSlotsClickabled && mTrapezoidCurvePaint == null) {
            mTrapezoidCurvePaint = new Paint();
            mTrapezoidCurvePaint.setAntiAlias(true);
            mTrapezoidCurvePaint.setColor(mTrapezoidSolidColor);
            mTrapezoidCurvePaint.setStyle(Paint.Style.STROKE);
            mTrapezoidCurvePaint.setStrokeWidth(mDividerWidth * 2);
        } else if (mIsSlotsClickabled) {
            mTrapezoidCurvePaint = null;
            // Sets levels again to force update the click state.
            setLevels(mLevels);
        }
        invalidate();
    }

    @Override
    public void setClickable(boolean clickable) {
        super.setClickable(mIsSlotsClickabled && clickable);
    }

    @VisibleForTesting
    void setClickableForce(boolean clickable) {
        super.setClickable(clickable);
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
        mDividerPaint.setColor(DIVIDER_COLOR);
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
        // Initializes for drawing text information.
        mTextPadding = resources.getDimensionPixelSize(R.dimen.chartview_text_padding);
    }

    private void drawHorizontalDividers(Canvas canvas) {
        final int width = getWidth() - mIndent.right;
        final int height = getHeight() - mIndent.top - mIndent.bottom;
        // Draws the top divider line for 100% curve.
        float offsetY = mIndent.top + mDividerWidth * .5f;
        canvas.drawLine(0, offsetY, width, offsetY, mDividerPaint);
        drawPercentage(canvas, /*index=*/ 0, offsetY);

        // Draws the center divider line for 50% curve.
        final float availableSpace =
            height - mDividerWidth * 2 - mTrapezoidVOffset - mDividerHeight;
        offsetY = mIndent.top + mDividerWidth + availableSpace * .5f;
        canvas.drawLine(0, offsetY, width, offsetY, mDividerPaint);
        drawPercentage(canvas, /*index=*/ 1, offsetY);

        // Draws the bottom divider line for 0% curve.
        offsetY = mIndent.top + (height - mDividerHeight - mDividerWidth * .5f);
        canvas.drawLine(0, offsetY, width, offsetY, mDividerPaint);
        drawPercentage(canvas, /*index=*/ 2, offsetY);
    }

    private void drawPercentage(Canvas canvas, int index, float offsetY) {
        if (mTextPaint != null) {
            canvas.drawText(
                mPercentages[index],
                getWidth() - mPercentageBounds[index].width() - mPercentageBounds[index].left,
                offsetY + mPercentageBounds[index].height() *.5f,
                mTextPaint);
        }
    }

    private void drawVerticalDividers(Canvas canvas) {
        final int width = getWidth() - mIndent.right;
        final int dividerCount = mTrapezoidCount + 1;
        final float dividerSpace = dividerCount * mDividerWidth;
        final float unitWidth = (width - dividerSpace) / (float) mTrapezoidCount;
        final float bottomY = getHeight() - mIndent.bottom;
        final float startY = bottomY - mDividerHeight;
        final float trapezoidSlotOffset = mTrapezoidHOffset + mDividerWidth * .5f;
        // Draws each vertical dividers.
        float startX = mDividerWidth * .5f;
        for (int index = 0; index < dividerCount; index++) {
            canvas.drawLine(startX, startY, startX, bottomY, mDividerPaint);
            final float nextX = startX + mDividerWidth + unitWidth;
            // Updates the trapezoid slots for drawing.
            if (index < mTrapezoidSlots.length) {
                mTrapezoidSlots[index].mLeft = round(startX + trapezoidSlotOffset);
                mTrapezoidSlots[index].mRight = round(nextX - trapezoidSlotOffset);
            }
            startX = nextX;
        }
        // Draws the timestamp slot information.
        if (mTimestamps != null) {
            final float[] xOffsets = new float[DEFAULT_TIMESTAMP_COUNT];
            final float baselineX = mDividerWidth * .5f;
            final float offsetX = mDividerWidth + unitWidth;
            for (int index = 0; index < DEFAULT_TIMESTAMP_COUNT; index++) {
                xOffsets[index] = baselineX + index * offsetX * 3;
            }
            drawTimestamp(canvas, xOffsets);
        }
    }

    private void drawTimestamp(Canvas canvas, float[] xOffsets) {
        // Draws the 1st timestamp info.
        canvas.drawText(
            mTimestamps[0],
            xOffsets[0] - mTimestampsBounds[0].left,
            getTimestampY(0), mTextPaint);
        // Draws the last timestamp info.
        canvas.drawText(
            mTimestamps[4],
            xOffsets[4] - mTimestampsBounds[4].width() - mTimestampsBounds[4].left,
            getTimestampY(4), mTextPaint);
        // Draws the rest of timestamp info since it is located in the center.
        for (int index = 1; index <= 3; index++) {
            canvas.drawText(
                mTimestamps[index],
                xOffsets[index] -
                    (mTimestampsBounds[index].width() - mTimestampsBounds[index].left) * .5f,
                getTimestampY(index), mTextPaint);

        }
    }

    private int getTimestampY(int index) {
        return getHeight() - mTimestampsBounds[index].height()
            + (mTimestampsBounds[index].height() + mTimestampsBounds[index].top)
            + round(mTextPadding * 1.5f);
    }

    private void drawTrapezoids(Canvas canvas) {
        // Ignores invalid trapezoid data.
        if (mLevels == null) {
            return;
        }
        final float trapezoidBottom =
            getHeight() - mIndent.bottom - mDividerHeight - mDividerWidth
                - mTrapezoidVOffset;
        final float availableSpace = trapezoidBottom - mDividerWidth * .5f - mIndent.top;
        final float unitHeight = availableSpace / 100f;
        // Draws all trapezoid shapes into the canvas.
        final Path trapezoidPath = new Path();
        Path trapezoidCurvePath = null;
        for (int index = 0; index < mTrapezoidCount; index++) {
            // Not draws the trapezoid for corner or not initialization cases.
            if (!isValidToDraw(index)) {
                if (mTrapezoidCurvePaint != null && trapezoidCurvePath != null) {
                    canvas.drawPath(trapezoidCurvePath, mTrapezoidCurvePaint);
                    trapezoidCurvePath = null;
                }
                continue;
            }
            // Configures the trapezoid paint color.
            final int trapezoidColor =
                !mIsSlotsClickabled
                    ? mTrapezoidColor
                    : mSelectedIndex == index || mSelectedIndex == SELECTED_INDEX_ALL
                        ? mTrapezoidSolidColor : mTrapezoidColor;
            mTrapezoidPaint.setColor(trapezoidColor);

            final float leftTop = round(trapezoidBottom - mLevels[index] * unitHeight);
            final float rightTop = round(trapezoidBottom - mLevels[index + 1] * unitHeight);
            trapezoidPath.reset();
            trapezoidPath.moveTo(mTrapezoidSlots[index].mLeft, trapezoidBottom);
            trapezoidPath.lineTo(mTrapezoidSlots[index].mLeft, leftTop);
            trapezoidPath.lineTo(mTrapezoidSlots[index].mRight, rightTop);
            trapezoidPath.lineTo(mTrapezoidSlots[index].mRight, trapezoidBottom);
            // A tricky way to make the trapezoid shape drawing the rounded corner.
            trapezoidPath.lineTo(mTrapezoidSlots[index].mLeft, trapezoidBottom);
            trapezoidPath.lineTo(mTrapezoidSlots[index].mLeft, leftTop);
            // Draws the trapezoid shape into canvas.
            canvas.drawPath(trapezoidPath, mTrapezoidPaint);

            // Generates path for non-clickable trapezoid curve.
            if (mTrapezoidCurvePaint != null) {
                if (trapezoidCurvePath == null) {
                    trapezoidCurvePath= new Path();
                    trapezoidCurvePath.moveTo(mTrapezoidSlots[index].mLeft, leftTop);
                } else {
                    trapezoidCurvePath.lineTo(mTrapezoidSlots[index].mLeft, leftTop);
                }
                trapezoidCurvePath.lineTo(mTrapezoidSlots[index].mRight, rightTop);
            }
        }
        // Draws the trapezoid curve for non-clickable case.
        if (mTrapezoidCurvePaint != null && trapezoidCurvePath != null) {
            canvas.drawPath(trapezoidCurvePath, mTrapezoidCurvePaint);
            trapezoidCurvePath = null;
        }
    }

    // Searches the corresponding trapezoid index from x location.
    private int getTrapezoidIndex(float x) {
        for (int index = 0; index < mTrapezoidSlots.length; index++) {
            final TrapezoidSlot slot = mTrapezoidSlots[index];
            if (x >= slot.mLeft - mTrapezoidHOffset
                    && x <= slot.mRight + mTrapezoidHOffset) {
                return index;
            }
        }
        return SELECTED_INDEX_INVALID;
    }

    private boolean isValidToDraw(int trapezoidIndex) {
        return mLevels != null
                && trapezoidIndex >= 0
                && trapezoidIndex < mLevels.length - 1
                && mLevels[trapezoidIndex] != 0
                && mLevels[trapezoidIndex + 1] != 0;
    }

    private static String[] getPercentages() {
        return new String[] {
            formatPercentage(/*percentage=*/ 100, /*round=*/ true),
            formatPercentage(/*percentage=*/ 50, /*round=*/ true),
            formatPercentage(/*percentage=*/ 0, /*round=*/ true)};
    }

    @VisibleForTesting
    static boolean isAccessibilityEnabled(Context context) {
        final AccessibilityManager accessibilityManager =
            context.getSystemService(AccessibilityManager.class);
        if (!accessibilityManager.isEnabled()) {
            return false;
        }
        final List<AccessibilityServiceInfo> serviceInfoList =
            accessibilityManager.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_SPOKEN
                    | AccessibilityServiceInfo.FEEDBACK_GENERIC);
        for (AccessibilityServiceInfo info : serviceInfoList) {
            for (String serviceName : ACCESSIBILITY_SERVICE_NAMES) {
                final String serviceId = info.getId();
                if (serviceId != null && serviceId.contains(serviceName)) {
                    Log.d(TAG, "acccessibilityEnabled:" + serviceId);
                    return true;
                }
            }
        }
        return false;
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
