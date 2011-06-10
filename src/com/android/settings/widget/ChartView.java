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

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static com.google.common.base.Preconditions.checkNotNull;

import android.content.Context;
import android.graphics.Rect;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

/**
 * Container for two-dimensional chart, drawn with a combination of
 * {@link ChartGridView}, {@link ChartNetworkSeriesView} and {@link ChartSweepView}
 * children. The entire chart uses {@link ChartAxis} to map between raw values
 * and screen coordinates.
 */
public class ChartView extends FrameLayout {
    private static final String TAG = "ChartView";

    // TODO: extend something that supports two-dimensional scrolling

    final ChartAxis mHoriz;
    final ChartAxis mVert;

    private Rect mContent = new Rect();

    public ChartView(Context context, ChartAxis horiz, ChartAxis vert) {
        super(context);

        mHoriz = checkNotNull(horiz, "missing horiz");
        mVert = checkNotNull(vert, "missing vert");

        setClipToPadding(false);
        setClipChildren(false);
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
                // sweep is always placed along specific dimension
                final ChartSweepView sweep = (ChartSweepView) child;
                final ChartAxis axis = sweep.getAxis();
                final float point = sweep.getPoint();

                if (axis == mHoriz) {
                    parentRect.left = parentRect.right = (int) point + getPaddingLeft();
                    parentRect.bottom += child.getMeasuredWidth();
                    Gravity.apply(params.gravity, child.getMeasuredWidth(), parentRect.height(),
                            parentRect, childRect);

                } else if (axis == mVert) {
                    parentRect.top = parentRect.bottom = (int) point + getPaddingTop();
                    parentRect.right += child.getMeasuredHeight();
                    Gravity.apply(params.gravity, parentRect.width(), child.getMeasuredHeight(),
                            parentRect, childRect);

                } else {
                    throw new IllegalStateException("unexpected axis");
                }
            }

            child.layout(childRect.left, childRect.top, childRect.right, childRect.bottom);
        }
    }

    public static LayoutParams buildChartParams() {
        final LayoutParams params = new LayoutParams(MATCH_PARENT, MATCH_PARENT);
        params.gravity = Gravity.LEFT | Gravity.BOTTOM;
        return params;
    }

    public static LayoutParams buildSweepParams() {
        final LayoutParams params = new LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        params.gravity = Gravity.CENTER;
        return params;
    }

}
