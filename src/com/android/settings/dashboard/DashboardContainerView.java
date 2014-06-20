/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings.dashboard;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import com.android.settings.R;

public class DashboardContainerView extends ViewGroup {

    private float mCellGapX;
    private float mCellGapY;

    private int mNumRows;
    private int mNumColumns;

    public DashboardContainerView(Context context, AttributeSet attrs) {
        super(context, attrs);

        final Resources res = context.getResources();
        mCellGapX = res.getDimension(R.dimen.dashboard_cell_gap_x);
        mCellGapY = res.getDimension(R.dimen.dashboard_cell_gap_y);
        mNumColumns = res.getInteger(R.integer.dashboard_num_columns);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int width = MeasureSpec.getSize(widthMeasureSpec);
        final int availableWidth = (int) (width - getPaddingLeft() - getPaddingRight() -
                (mNumColumns - 1) * mCellGapX);
        float cellWidth = (float) Math.ceil(((float) availableWidth) / mNumColumns);
        final int N = getChildCount();

        int cellHeight = 0;
        int cursor = 0;

        for (int i = 0; i < N; ++i) {
            DashboardTileView v = (DashboardTileView) getChildAt(i);
            if (v.getVisibility() == View.GONE) {
                continue;
            }

            ViewGroup.LayoutParams lp = v.getLayoutParams();
            int colSpan = v.getColumnSpan();
            lp.width = (int) ((colSpan * cellWidth) + (colSpan - 1) * mCellGapX);

            // Measure the child
            int newWidthSpec = getChildMeasureSpec(widthMeasureSpec, 0, lp.width);
            int newHeightSpec = getChildMeasureSpec(heightMeasureSpec, 0, lp.height);
            v.measure(newWidthSpec, newHeightSpec);

            // Save the cell height
            if (cellHeight <= 0) {
                cellHeight = v.getMeasuredHeight();
            }

            lp.height = cellHeight;

            cursor += colSpan;
        }

        mNumRows = (int) Math.ceil((float) cursor / mNumColumns);
        final int newHeight = (int) ((mNumRows * cellHeight) + ((mNumRows - 1) * mCellGapY)) +
                getPaddingTop() + getPaddingBottom();

        setMeasuredDimension(width, newHeight);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int N = getChildCount();
        final boolean isLayoutRtl = isLayoutRtl();
        final int width = getWidth();

        int x = getPaddingStart();
        int y = getPaddingTop();
        int cursor = 0;

        for (int i = 0; i < N; ++i) {
            final DashboardTileView child = (DashboardTileView) getChildAt(i);
            final ViewGroup.LayoutParams lp = child.getLayoutParams();
            if (child.getVisibility() == GONE) {
                continue;
            }

            final int col = cursor % mNumColumns;
            final int colSpan = child.getColumnSpan();

            final int childWidth = lp.width;
            final int childHeight = lp.height;

            int row = cursor / mNumColumns;

            if (row == mNumRows - 1) {
                child.setDividerVisibility(false);
            }

            // Push the item to the next row if it can't fit on this one
            if ((col + colSpan) > mNumColumns) {
                x = getPaddingStart();
                y += childHeight + mCellGapY;
                row++;
            }

            final int childLeft = (isLayoutRtl) ? width - x - childWidth : x;
            final int childRight = childLeft + childWidth;

            final int childTop = y;
            final int childBottom = childTop + childHeight;

            // Layout the container
            child.layout(childLeft, childTop, childRight, childBottom);

            // Offset the position by the cell gap or reset the position and cursor when we
            // reach the end of the row
            cursor += child.getColumnSpan();
            if (cursor < (((row + 1) * mNumColumns))) {
                x += childWidth + mCellGapX;
            } else {
                x = getPaddingStart();
                y += childHeight + mCellGapY;
            }
        }
    }
}
