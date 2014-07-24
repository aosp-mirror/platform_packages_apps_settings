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


package com.android.settings.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.widget.ListView;

/**
 * This class provides sticky header functionality in a list view, to use with
 * SetupWizardIllustration. To use this, add a header tagged with "sticky", or a header tagged with
 * "stickyContainer" and one of its child tagged as "sticky". The sticky container will be drawn
 * when the sticky element hits the top of the view.
 *
 * There are a few things to note:
 * 1. The two supported scenarios are StickyHeaderListView -> Header (stickyContainer) -> sticky,
 *    and StickyHeaderListView -> Header (sticky). The arrow (->) represents parent/child
 *    relationship and must be immediate child.
 * 2. The view does not work well with padding. b/16190933
 * 3. If fitsSystemWindows is true, then this will offset the sticking position by the height of
 *    the system decorations at the top of the screen.
 *
 * @see SetupWizardIllustration
 * @see com.google.android.setupwizard.util.StickyHeaderScrollView
 *
 * Copied from com.google.android.setupwizard.util.StickyHeaderListView
 */
public class StickyHeaderListView extends ListView {

    private View mSticky;
    private View mStickyContainer;
    private boolean mDrawScrollBar;
    private int mStatusBarInset = 0;
    private RectF mStickyRect = new RectF();

    public StickyHeaderListView(Context context) {
        super(context);
    }

    public StickyHeaderListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public StickyHeaderListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public StickyHeaderListView(Context context, AttributeSet attrs, int defStyleAttr,
                                int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (mSticky == null) {
            updateStickyView();
        }
    }

    public void updateStickyView() {
        mSticky = findViewWithTag("sticky");
        mStickyContainer = findViewWithTag("stickyContainer");
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (mStickyRect.contains(ev.getX(), ev.getY())) {
            ev.offsetLocation(-mStickyRect.left, -mStickyRect.top);
            return mStickyContainer.dispatchTouchEvent(ev);
        } else {
            return super.dispatchTouchEvent(ev);
        }
    }

    @Override
    public void draw(Canvas canvas) {
        mDrawScrollBar = false;
        super.draw(canvas);
        if (mSticky != null) {
            final int saveCount = canvas.save();
            // The view to draw when sticking to the top
            final View drawTarget = mStickyContainer != null ? mStickyContainer : mSticky;
            // The offset to draw the view at when sticky
            final int drawOffset = mStickyContainer != null ? mSticky.getTop() : 0;
            // Position of the draw target, relative to the outside of the scrollView
            final int drawTop = drawTarget.getTop();
            if (drawTop + drawOffset < mStatusBarInset || !drawTarget.isShown()) {
                // ListView does not translate the canvas, so we can simply draw at the top
                canvas.translate(0, -drawOffset + mStatusBarInset);
                canvas.clipRect(0, 0, drawTarget.getWidth(), drawTarget.getHeight());
                drawTarget.draw(canvas);
                mStickyRect.set(0, -drawOffset + mStatusBarInset, drawTarget.getWidth(),
                        drawTarget.getHeight() - drawOffset + mStatusBarInset);
            } else {
                mStickyRect.setEmpty();
            }
            canvas.restoreToCount(saveCount);
        }
        // Draw the scrollbars last so they are on top of the header
        mDrawScrollBar = true;
        onDrawScrollBars(canvas);
    }

    @Override
    protected boolean isVerticalScrollBarHidden() {
        return super.isVerticalScrollBarHidden() || !mDrawScrollBar;
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        if (getFitsSystemWindows()) {
            mStatusBarInset = insets.getSystemWindowInsetTop();
            insets.consumeSystemWindowInsets(false, true, false, false);
        }
        return insets;
    }
}
