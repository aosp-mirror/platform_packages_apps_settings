/*
 * Copyright (C) 2022 The Android Open Source Project
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
package com.android.settings.fuelgauge.batteryusage;

import static com.android.settings.Utils.formatPercentage;
import static com.android.settings.fuelgauge.batteryusage.BatteryChartViewModel.AxisLabelPosition.BETWEEN_TRAPEZOIDS;
import static com.android.settingslib.fuelgauge.BatteryStatus.BATTERY_LEVEL_UNKNOWN;

import static java.lang.Math.abs;
import static java.lang.Math.round;
import static java.util.Objects.requireNonNull;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.widget.AppCompatImageView;

import com.android.settings.R;
import com.android.settingslib.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** A widget component to draw chart graph. */
public class BatteryChartView extends AppCompatImageView implements View.OnClickListener {
    private static final String TAG = "BatteryChartView";

    private static final int DIVIDER_COLOR = Color.parseColor("#CDCCC5");
    private static final int HORIZONTAL_DIVIDER_COUNT = 5;

    /** A callback listener for selected group index is updated. */
    public interface OnSelectListener {
        /** The callback function for selected group index is updated. */
        void onSelect(int trapezoidIndex);
    }

    private final String[] mPercentages = getPercentages();
    private final Rect mIndent = new Rect();
    private final Rect[] mPercentageBounds = new Rect[] {new Rect(), new Rect(), new Rect()};
    private final List<Rect> mAxisLabelsBounds = new ArrayList<>();
    private final Set<Integer> mLabelDrawnIndexes = new ArraySet<>();
    private final int mLayoutDirection =
            getContext().getResources().getConfiguration().getLayoutDirection();

    private BatteryChartViewModel mViewModel;
    private int mHoveredIndex = BatteryChartViewModel.SELECTED_INDEX_INVALID;
    private int mDividerWidth;
    private int mDividerHeight;
    private float mTrapezoidVOffset;
    private float mTrapezoidHOffset;
    private int mTrapezoidColor;
    private int mTrapezoidSolidColor;
    private int mTrapezoidHoverColor;
    private int mDefaultTextColor;
    private int mTextPadding;
    private int mTransomIconSize;
    private int mTransomTop;
    private int mTransomViewHeight;
    private int mTransomLineDefaultColor;
    private int mTransomLineSelectedColor;
    private float mTransomPadding;
    private Drawable mTransomIcon;
    private Paint mTransomLinePaint;
    private Paint mTransomSelectedSlotPaint;
    private Paint mDividerPaint;
    private Paint mTrapezoidPaint;
    private Paint mTextPaint;
    private AccessibilityNodeProvider mAccessibilityNodeProvider;
    private BatteryChartView.OnSelectListener mOnSelectListener;

    @VisibleForTesting TrapezoidSlot[] mTrapezoidSlots;
    // Records the location to calculate selected index.
    @VisibleForTesting float mTouchUpEventX = Float.MIN_VALUE;

    public BatteryChartView(Context context) {
        super(context, null);
    }

    public BatteryChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initializeColors(context);
        // Registers the click event listener.
        setOnClickListener(this);
        setClickable(false);
        requestLayout();
    }

    /** Sets the data model of this view. */
    public void setViewModel(BatteryChartViewModel viewModel) {
        if (viewModel == null) {
            mViewModel = null;
            invalidate();
            return;
        }

        Log.d(
                TAG,
                String.format(
                        "setViewModel(): size: %d, selectedIndex: %d, getHighlightSlotIndex: %d",
                        viewModel.size(),
                        viewModel.selectedIndex(),
                        viewModel.getHighlightSlotIndex()));
        mViewModel = viewModel;
        initializeAxisLabelsBounds();
        initializeTrapezoidSlots(viewModel.size() - 1);
        setClickable(hasAnyValidTrapezoid(viewModel));
        requestLayout();
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
            mDefaultTextColor = mTextPaint.getColor();
        } else {
            mTextPaint = null;
        }
        requestLayout();
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        // Measures text bounds and updates indent configuration.
        if (mTextPaint != null) {
            mTextPaint.setTextAlign(Paint.Align.LEFT);
            for (int index = 0; index < mPercentages.length; index++) {
                mTextPaint.getTextBounds(
                        mPercentages[index],
                        0,
                        mPercentages[index].length(),
                        mPercentageBounds[index]);
            }
            // Updates the indent configurations.
            mIndent.top = mPercentageBounds[0].height() + mTransomViewHeight;
            final int textWidth = mPercentageBounds[0].width() + mTextPadding;
            if (isRTL()) {
                mIndent.left = textWidth;
            } else {
                mIndent.right = textWidth;
            }

            if (mViewModel != null) {
                int maxTop = 0;
                for (int index = 0; index < mViewModel.size(); index++) {
                    final String text = mViewModel.getText(index);
                    mTextPaint.getTextBounds(text, 0, text.length(), mAxisLabelsBounds.get(index));
                    maxTop = Math.max(maxTop, -mAxisLabelsBounds.get(index).top);
                }
                mIndent.bottom = maxTop + round(mTextPadding * 2f);
            }
            Log.d(TAG, "setIndent:" + mPercentageBounds[0]);
        } else {
            mIndent.set(0, 0, 0, 0);
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        // Before mLevels initialized, the count of trapezoids is unknown. Only draws the
        // horizontal percentages and dividers.
        drawHorizontalDividers(canvas);
        if (mViewModel == null) {
            return;
        }
        drawVerticalDividers(canvas);
        drawTrapezoids(canvas);
        drawTransomLine(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Caches the location to calculate selected trapezoid index.
        final int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_UP:
                mTouchUpEventX = event.getX();
                break;
            case MotionEvent.ACTION_CANCEL:
                mTouchUpEventX = Float.MIN_VALUE; // reset
                break;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onHoverEvent(MotionEvent event) {
        final int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_HOVER_ENTER:
            case MotionEvent.ACTION_HOVER_MOVE:
                final int trapezoidIndex = getTrapezoidIndex(event.getX());
                if (mHoveredIndex != trapezoidIndex) {
                    mHoveredIndex = trapezoidIndex;
                    invalidate();
                    sendAccessibilityEventForHover(AccessibilityEvent.TYPE_VIEW_HOVER_ENTER);
                }
                // Ignore the super.onHoverEvent() because the hovered trapezoid has already been
                // sent here.
                return true;
            case MotionEvent.ACTION_HOVER_EXIT:
                if (mHoveredIndex != BatteryChartViewModel.SELECTED_INDEX_INVALID) {
                    sendAccessibilityEventForHover(AccessibilityEvent.TYPE_VIEW_HOVER_EXIT);
                    mHoveredIndex = BatteryChartViewModel.SELECTED_INDEX_INVALID; // reset
                    invalidate();
                }
                // Ignore the super.onHoverEvent() because the hovered trapezoid has already been
                // sent here.
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    @Override
    public void onHoverChanged(boolean hovered) {
        super.onHoverChanged(hovered);
        if (!hovered) {
            mHoveredIndex = BatteryChartViewModel.SELECTED_INDEX_INVALID; // reset
            invalidate();
        }
    }

    @Override
    public void onClick(View view) {
        if (mTouchUpEventX == Float.MIN_VALUE) {
            Log.w(TAG, "invalid motion event for onClick() callback");
            return;
        }
        onTrapezoidClicked(view, getTrapezoidIndex(mTouchUpEventX));
    }

    @Override
    public AccessibilityNodeProvider getAccessibilityNodeProvider() {
        if (mViewModel == null) {
            return super.getAccessibilityNodeProvider();
        }
        if (mAccessibilityNodeProvider == null) {
            mAccessibilityNodeProvider = new BatteryChartAccessibilityNodeProvider();
        }
        return mAccessibilityNodeProvider;
    }

    private void onTrapezoidClicked(View view, int index) {
        // Ignores the click event if the level is zero.
        if (!isValidToDraw(mViewModel, index)) {
            return;
        }
        if (mOnSelectListener != null) {
            // Selects all if users click the same trapezoid item two times.
            mOnSelectListener.onSelect(
                    index == mViewModel.selectedIndex()
                            ? BatteryChartViewModel.SELECTED_INDEX_ALL
                            : index);
        }
        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);
    }

    private boolean sendAccessibilityEvent(int virtualDescendantId, int eventType) {
        ViewParent parent = getParent();
        if (parent == null || !AccessibilityManager.getInstance(mContext).isEnabled()) {
            return false;
        }
        AccessibilityEvent accessibilityEvent = new AccessibilityEvent(eventType);
        accessibilityEvent.setSource(this, virtualDescendantId);
        accessibilityEvent.setEnabled(true);
        accessibilityEvent.setClassName(getAccessibilityClassName());
        accessibilityEvent.setPackageName(getContext().getPackageName());
        return parent.requestSendAccessibilityEvent(this, accessibilityEvent);
    }

    private void sendAccessibilityEventForHover(int eventType) {
        if (isTrapezoidIndexValid(mViewModel, mHoveredIndex)) {
            sendAccessibilityEvent(mHoveredIndex, eventType);
        }
    }

    private void initializeTrapezoidSlots(int count) {
        mTrapezoidSlots = new TrapezoidSlot[count];
        for (int index = 0; index < mTrapezoidSlots.length; index++) {
            mTrapezoidSlots[index] = new TrapezoidSlot();
        }
    }

    private void initializeColors(Context context) {
        setBackgroundColor(Color.TRANSPARENT);
        mTrapezoidSolidColor = Utils.getColorAccentDefaultColor(context);
        mTrapezoidColor = Utils.getDisabled(context, mTrapezoidSolidColor);
        mTrapezoidHoverColor =
                Utils.getColorAttrDefaultColor(
                        context, com.android.internal.R.attr.materialColorSecondaryContainer);
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
        // Initializes the padding top for drawing text information.
        mTransomViewHeight =
                resources.getDimensionPixelSize(R.dimen.chartview_transom_layout_height);
    }

    private void initializeTransomPaint() {
        if (mTransomLinePaint != null
                && mTransomSelectedSlotPaint != null
                && mTransomIcon != null) {
            return;
        }
        // Initializes the transom line paint.
        final Resources resources = getContext().getResources();
        final int transomLineWidth =
                resources.getDimensionPixelSize(R.dimen.chartview_transom_width);
        final int transomRadius = resources.getDimensionPixelSize(R.dimen.chartview_transom_radius);
        mTransomPadding = transomRadius * .5f;
        mTransomTop = resources.getDimensionPixelSize(R.dimen.chartview_transom_padding_top);
        mTransomLineDefaultColor = Utils.getDisabled(mContext, DIVIDER_COLOR);
        mTransomLineSelectedColor =
                resources.getColor(R.color.color_battery_anomaly_app_warning_selector);
        final int slotHighlightColor = Utils.getDisabled(mContext, mTransomLineSelectedColor);
        mTransomIconSize = resources.getDimensionPixelSize(R.dimen.chartview_transom_icon_size);
        mTransomLinePaint = new Paint();
        mTransomLinePaint.setAntiAlias(true);
        mTransomLinePaint.setStyle(Paint.Style.STROKE);
        mTransomLinePaint.setStrokeWidth(transomLineWidth);
        mTransomLinePaint.setStrokeCap(Paint.Cap.ROUND);
        mTransomLinePaint.setPathEffect(new CornerPathEffect(transomRadius));
        mTransomSelectedSlotPaint = new Paint();
        mTransomSelectedSlotPaint.setAntiAlias(true);
        mTransomSelectedSlotPaint.setColor(slotHighlightColor);
        mTransomSelectedSlotPaint.setStyle(Paint.Style.FILL);
        // Get the companion icon beside transom line
        mTransomIcon = getResources().getDrawable(R.drawable.ic_battery_tips_warning_icon);
    }

    private void drawHorizontalDividers(Canvas canvas) {
        final int width = getWidth() - abs(mIndent.width());
        final int height = getHeight() - mIndent.top - mIndent.bottom;
        final float topOffsetY = mIndent.top + mDividerWidth * .5f;
        final float bottomOffsetY = mIndent.top + (height - mDividerHeight - mDividerWidth * .5f);
        final float availableSpace = bottomOffsetY - topOffsetY;

        mDividerPaint.setColor(DIVIDER_COLOR);
        final float dividerOffsetUnit = availableSpace / (float) (HORIZONTAL_DIVIDER_COUNT - 1);

        // Draws 5 divider lines.
        for (int index = 0; index < HORIZONTAL_DIVIDER_COUNT; index++) {
            float offsetY = topOffsetY + dividerOffsetUnit * index;
            canvas.drawLine(mIndent.left, offsetY, mIndent.left + width, offsetY, mDividerPaint);

            //  Draws percentage text only for 100% / 50% / 0%
            if (index % 2 == 0) {
                drawPercentage(canvas, /* index= */ (index + 1) / 2, offsetY);
            }
        }
    }

    private void drawPercentage(Canvas canvas, int index, float offsetY) {
        if (mTextPaint != null) {
            mTextPaint.setTextAlign(isRTL() ? Paint.Align.RIGHT : Paint.Align.LEFT);
            mTextPaint.setColor(mDefaultTextColor);
            canvas.drawText(
                    mPercentages[index],
                    isRTL()
                            ? mIndent.left - mTextPadding
                            : getWidth() - mIndent.width() + mTextPadding,
                    offsetY + mPercentageBounds[index].height() * .5f,
                    mTextPaint);
        }
    }

    private void drawVerticalDividers(Canvas canvas) {
        final int width = getWidth() - abs(mIndent.width());
        final int dividerCount = mTrapezoidSlots.length + 1;
        final float dividerSpace = dividerCount * mDividerWidth;
        final float unitWidth = (width - dividerSpace) / (float) mTrapezoidSlots.length;
        final float bottomY = getHeight() - mIndent.bottom;
        final float startY = bottomY - mDividerHeight;
        final float trapezoidSlotOffset = mTrapezoidHOffset + mDividerWidth * .5f;
        // Draws the axis label slot information.
        if (mViewModel != null) {
            final float baselineY = getHeight() - mTextPadding;
            Rect[] axisLabelDisplayAreas;
            switch (mViewModel.axisLabelPosition()) {
                case CENTER_OF_TRAPEZOIDS:
                    axisLabelDisplayAreas =
                            getAxisLabelDisplayAreas(
                                    /* size= */ mViewModel.size() - 1,
                                    /* baselineX= */ mIndent.left + mDividerWidth + unitWidth * .5f,
                                    /* offsetX= */ mDividerWidth + unitWidth,
                                    baselineY,
                                    /* shiftFirstAndLast= */ false);
                    break;
                case BETWEEN_TRAPEZOIDS:
                default:
                    axisLabelDisplayAreas =
                            getAxisLabelDisplayAreas(
                                    /* size= */ mViewModel.size(),
                                    /* baselineX= */ mIndent.left + mDividerWidth * .5f,
                                    /* offsetX= */ mDividerWidth + unitWidth,
                                    baselineY,
                                    /* shiftFirstAndLast= */ true);
                    break;
            }
            drawAxisLabels(canvas, axisLabelDisplayAreas, baselineY);
        }
        // Draws each vertical dividers.
        float startX = mDividerWidth * .5f + mIndent.left;
        for (int index = 0; index < dividerCount; index++) {
            float dividerY = bottomY;
            if (mViewModel.axisLabelPosition() == BETWEEN_TRAPEZOIDS
                    && mLabelDrawnIndexes.contains(index)) {
                mDividerPaint.setColor(mTrapezoidSolidColor);
                dividerY += mDividerHeight / 4f;
            } else {
                mDividerPaint.setColor(DIVIDER_COLOR);
            }
            canvas.drawLine(startX, startY, startX, dividerY, mDividerPaint);
            final float nextX = startX + mDividerWidth + unitWidth;
            // Updates the trapezoid slots for drawing.
            if (index < mTrapezoidSlots.length) {
                final int trapezoidIndex = isRTL() ? mTrapezoidSlots.length - index - 1 : index;
                mTrapezoidSlots[trapezoidIndex].mLeft = round(startX + trapezoidSlotOffset);
                mTrapezoidSlots[trapezoidIndex].mRight = round(nextX - trapezoidSlotOffset);
            }
            startX = nextX;
        }
    }

    /** Gets all the axis label texts displaying area positions if they are shown. */
    private Rect[] getAxisLabelDisplayAreas(
            final int size,
            final float baselineX,
            final float offsetX,
            final float baselineY,
            final boolean shiftFirstAndLast) {
        final Rect[] result = new Rect[size];
        for (int index = 0; index < result.length; index++) {
            final float width = mAxisLabelsBounds.get(index).width();
            float middle = baselineX + index * offsetX;
            if (shiftFirstAndLast) {
                if (index == 0) {
                    middle += width * .5f;
                }
                if (index == size - 1) {
                    middle -= width * .5f;
                }
            }
            final float left = middle - width * .5f;
            final float right = left + width;
            final float top = baselineY + mAxisLabelsBounds.get(index).top;
            final float bottom = top + mAxisLabelsBounds.get(index).height();
            result[index] = new Rect(round(left), round(top), round(right), round(bottom));
        }
        return result;
    }

    private void drawAxisLabels(Canvas canvas, final Rect[] displayAreas, final float baselineY) {
        final int lastIndex = displayAreas.length - 1;
        mLabelDrawnIndexes.clear();
        // Suppose first and last labels are always able to draw.
        drawAxisLabelText(canvas, 0, displayAreas[0], baselineY);
        mLabelDrawnIndexes.add(0);
        drawAxisLabelText(canvas, lastIndex, displayAreas[lastIndex], baselineY);
        mLabelDrawnIndexes.add(lastIndex);
        drawAxisLabelsBetweenStartIndexAndEndIndex(canvas, displayAreas, 0, lastIndex, baselineY);
    }

    /**
     * Recursively draws axis labels between the start index and the end index. If the inner number
     * can be exactly divided into 2 parts, check and draw the middle index label and then
     * recursively draw the 2 parts. Otherwise, divide into 3 parts. Check and draw the middle two
     * labels and then recursively draw the 3 parts. If there are any overlaps, skip drawing and go
     * back to the uplevel of the recursion.
     */
    private void drawAxisLabelsBetweenStartIndexAndEndIndex(
            Canvas canvas,
            final Rect[] displayAreas,
            final int startIndex,
            final int endIndex,
            final float baselineY) {
        if (endIndex - startIndex <= 1) {
            return;
        }
        if ((endIndex - startIndex) % 2 == 0) {
            int middleIndex = (startIndex + endIndex) / 2;
            if (hasOverlap(displayAreas, startIndex, middleIndex)
                    || hasOverlap(displayAreas, middleIndex, endIndex)) {
                return;
            }
            drawAxisLabelText(canvas, middleIndex, displayAreas[middleIndex], baselineY);
            mLabelDrawnIndexes.add(middleIndex);
            drawAxisLabelsBetweenStartIndexAndEndIndex(
                    canvas, displayAreas, startIndex, middleIndex, baselineY);
            drawAxisLabelsBetweenStartIndexAndEndIndex(
                    canvas, displayAreas, middleIndex, endIndex, baselineY);
        } else {
            int middleIndex1 = startIndex + round((endIndex - startIndex) / 3f);
            int middleIndex2 = startIndex + round((endIndex - startIndex) * 2 / 3f);
            if (hasOverlap(displayAreas, startIndex, middleIndex1)
                    || hasOverlap(displayAreas, middleIndex1, middleIndex2)
                    || hasOverlap(displayAreas, middleIndex2, endIndex)) {
                return;
            }
            drawAxisLabelText(canvas, middleIndex1, displayAreas[middleIndex1], baselineY);
            mLabelDrawnIndexes.add(middleIndex1);
            drawAxisLabelText(canvas, middleIndex2, displayAreas[middleIndex2], baselineY);
            mLabelDrawnIndexes.add(middleIndex2);
            drawAxisLabelsBetweenStartIndexAndEndIndex(
                    canvas, displayAreas, startIndex, middleIndex1, baselineY);
            drawAxisLabelsBetweenStartIndexAndEndIndex(
                    canvas, displayAreas, middleIndex1, middleIndex2, baselineY);
            drawAxisLabelsBetweenStartIndexAndEndIndex(
                    canvas, displayAreas, middleIndex2, endIndex, baselineY);
        }
    }

    private boolean hasOverlap(
            final Rect[] displayAreas, final int leftIndex, final int rightIndex) {
        return displayAreas[leftIndex].right + mTextPadding * 2.3f > displayAreas[rightIndex].left;
    }

    private boolean isRTL() {
        return mLayoutDirection == View.LAYOUT_DIRECTION_RTL;
    }

    private void drawAxisLabelText(
            Canvas canvas, int index, final Rect displayArea, final float baselineY) {
        mTextPaint.setColor(mTrapezoidSolidColor);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        // Reverse the sort of axis labels for RTL
        if (isRTL()) {
            index =
                    mViewModel.axisLabelPosition() == BETWEEN_TRAPEZOIDS
                            ? mViewModel.size() - index - 1 // for hourly
                            : mViewModel.size() - index - 2; // for daily
        }
        canvas.drawText(mViewModel.getText(index), displayArea.centerX(), baselineY, mTextPaint);
        mLabelDrawnIndexes.add(index);
    }

    private void drawTrapezoids(Canvas canvas) {
        // Ignores invalid trapezoid data.
        if (mViewModel == null) {
            return;
        }
        final float trapezoidBottom =
                getHeight() - mIndent.bottom - mDividerHeight - mDividerWidth - mTrapezoidVOffset;
        final float availableSpace =
                trapezoidBottom - mDividerWidth * .5f - mIndent.top - mTrapezoidVOffset;
        final float unitHeight = availableSpace / 100f;
        // Draws all trapezoid shapes into the canvas.
        final Path trapezoidPath = new Path();
        Path trapezoidCurvePath = null;
        for (int index = 0; index < mTrapezoidSlots.length; index++) {
            // Not draws the trapezoid for corner or not initialization cases.
            if (!isValidToDraw(mViewModel, index)) {
                continue;
            }
            // Configures the trapezoid paint color.
            final int trapezoidColor =
                    (mViewModel.selectedIndex() == index
                                    || mViewModel.selectedIndex()
                                            == BatteryChartViewModel.SELECTED_INDEX_ALL)
                            ? mTrapezoidSolidColor
                            : mTrapezoidColor;
            final boolean isHoverState =
                    mHoveredIndex == index && isValidToDraw(mViewModel, mHoveredIndex);
            mTrapezoidPaint.setColor(isHoverState ? mTrapezoidHoverColor : trapezoidColor);

            float leftTop =
                    round(
                            trapezoidBottom
                                    - requireNonNull(mViewModel.getLevel(index)) * unitHeight);
            float rightTop =
                    round(
                            trapezoidBottom
                                    - requireNonNull(mViewModel.getLevel(index + 1)) * unitHeight);
            // Mirror the shape of the trapezoid for RTL
            if (isRTL()) {
                float temp = leftTop;
                leftTop = rightTop;
                rightTop = temp;
            }
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
        }
    }

    private boolean isHighlightSlotValid() {
        return mViewModel != null
                && mViewModel.getHighlightSlotIndex()
                        != BatteryChartViewModel.SELECTED_INDEX_INVALID;
    }

    private void drawTransomLine(Canvas canvas) {
        if (!isHighlightSlotValid()) {
            return;
        }
        initializeTransomPaint();
        // Draw the whole transom line and a warning icon
        mTransomLinePaint.setColor(mTransomLineDefaultColor);
        final int width = getWidth() - abs(mIndent.width());
        final float transomOffset = mTrapezoidHOffset + mDividerWidth * .5f + mTransomPadding;
        final float trapezoidBottom =
                getHeight() - mIndent.bottom - mDividerHeight - mDividerWidth - mTrapezoidVOffset;
        canvas.drawLine(
                mIndent.left + transomOffset,
                mTransomTop,
                mIndent.left + width - transomOffset,
                mTransomTop,
                mTransomLinePaint);
        drawTransomIcon(canvas);
        // Draw selected segment of transom line and a highlight slot
        mTransomLinePaint.setColor(mTransomLineSelectedColor);
        final int index = mViewModel.getHighlightSlotIndex();
        final float startX = mTrapezoidSlots[index].mLeft;
        final float endX = mTrapezoidSlots[index].mRight;
        canvas.drawLine(
                startX + mTransomPadding,
                mTransomTop,
                endX - mTransomPadding,
                mTransomTop,
                mTransomLinePaint);
        canvas.drawRect(startX, mTransomTop, endX, trapezoidBottom, mTransomSelectedSlotPaint);
    }

    private void drawTransomIcon(Canvas canvas) {
        if (mTransomIcon == null) {
            return;
        }
        final int left =
                isRTL()
                        ? mIndent.left - mTextPadding - mTransomIconSize
                        : getWidth() - abs(mIndent.width()) + mTextPadding;
        mTransomIcon.setBounds(
                left,
                mTransomTop - mTransomIconSize / 2,
                left + mTransomIconSize,
                mTransomTop + mTransomIconSize / 2);
        mTransomIcon.draw(canvas);
    }

    // Searches the corresponding trapezoid index from x location.
    private int getTrapezoidIndex(float x) {
        if (mTrapezoidSlots == null) {
            return BatteryChartViewModel.SELECTED_INDEX_INVALID;
        }
        for (int index = 0; index < mTrapezoidSlots.length; index++) {
            final TrapezoidSlot slot = mTrapezoidSlots[index];
            if (x >= slot.mLeft - mTrapezoidHOffset && x <= slot.mRight + mTrapezoidHOffset) {
                return index;
            }
        }
        return BatteryChartViewModel.SELECTED_INDEX_INVALID;
    }

    private void initializeAxisLabelsBounds() {
        mAxisLabelsBounds.clear();
        for (int i = 0; i < mViewModel.size(); i++) {
            mAxisLabelsBounds.add(new Rect());
        }
    }

    private static boolean isTrapezoidValid(
            @NonNull BatteryChartViewModel viewModel, int trapezoidIndex) {
        return viewModel.getLevel(trapezoidIndex) != BATTERY_LEVEL_UNKNOWN
                && viewModel.getLevel(trapezoidIndex + 1) != BATTERY_LEVEL_UNKNOWN;
    }

    private static boolean isTrapezoidIndexValid(
            @NonNull BatteryChartViewModel viewModel, int trapezoidIndex) {
        return viewModel != null && trapezoidIndex >= 0 && trapezoidIndex < viewModel.size() - 1;
    }

    private static boolean isValidToDraw(BatteryChartViewModel viewModel, int trapezoidIndex) {
        return isTrapezoidIndexValid(viewModel, trapezoidIndex)
                && isTrapezoidValid(viewModel, trapezoidIndex);
    }

    private static boolean hasAnyValidTrapezoid(@NonNull BatteryChartViewModel viewModel) {
        // Sets the chart is clickable if there is at least one valid item in it.
        for (int trapezoidIndex = 0; trapezoidIndex < viewModel.size() - 1; trapezoidIndex++) {
            if (isTrapezoidValid(viewModel, trapezoidIndex)) {
                return true;
            }
        }
        return false;
    }

    private static String[] getPercentages() {
        return new String[] {
            formatPercentage(/* percentage= */ 100, /* round= */ true),
            formatPercentage(/* percentage= */ 50, /* round= */ true),
            formatPercentage(/* percentage= */ 0, /* round= */ true)
        };
    }

    private class BatteryChartAccessibilityNodeProvider extends AccessibilityNodeProvider {
        @Override
        public AccessibilityNodeInfo createAccessibilityNodeInfo(int virtualViewId) {
            if (virtualViewId == AccessibilityNodeProvider.HOST_VIEW_ID) {
                final AccessibilityNodeInfo hostInfo =
                        new AccessibilityNodeInfo(BatteryChartView.this);
                for (int index = 0; index < mViewModel.size() - 1; index++) {
                    hostInfo.addChild(BatteryChartView.this, index);
                }
                return hostInfo;
            }
            final int index = virtualViewId;
            if (!isTrapezoidIndexValid(mViewModel, index)) {
                Log.w(TAG, "Invalid virtual view id:" + index);
                return null;
            }
            final AccessibilityNodeInfo childInfo =
                    new AccessibilityNodeInfo(BatteryChartView.this, index);
            onInitializeAccessibilityNodeInfo(childInfo);
            childInfo.setClickable(isValidToDraw(mViewModel, index));
            childInfo.setText(mViewModel.getFullText(index));
            childInfo.setContentDescription(mViewModel.getFullText(index));

            final Rect bounds = new Rect();
            getBoundsOnScreen(bounds, true);
            final int hostLeft = bounds.left;
            bounds.left = round(hostLeft + mTrapezoidSlots[index].mLeft);
            bounds.right = round(hostLeft + mTrapezoidSlots[index].mRight);
            childInfo.setBoundsInScreen(bounds);
            return childInfo;
        }

        @Override
        public boolean performAction(int virtualViewId, int action, @Nullable Bundle arguments) {
            if (virtualViewId == AccessibilityNodeProvider.HOST_VIEW_ID) {
                return performAccessibilityAction(action, arguments);
            }
            switch (action) {
                case AccessibilityNodeInfo.ACTION_CLICK:
                    onTrapezoidClicked(BatteryChartView.this, virtualViewId);
                    return true;

                case AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS:
                    return sendAccessibilityEvent(
                            virtualViewId, AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED);

                case AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS:
                    return sendAccessibilityEvent(
                            virtualViewId,
                            AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED);

                default:
                    return performAccessibilityAction(action, arguments);
            }
        }
    }

    // A container class for each trapezoid left and right location.
    @VisibleForTesting
    static final class TrapezoidSlot {
        public float mLeft;
        public float mRight;

        @Override
        public String toString() {
            return String.format(Locale.US, "TrapezoidSlot[%f,%f]", mLeft, mRight);
        }
    }
}
