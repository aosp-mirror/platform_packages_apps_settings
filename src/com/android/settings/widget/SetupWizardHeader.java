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
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/*
 * Copied from com.google.android.setupwizard.util.SetupWizardHeader
 */
public class SetupWizardHeader extends FrameLayout {

    // Size the baseline grid in pixels
    private float mBaselineGridSize;
    private Drawable mBackground;
    private Drawable mForeground;
    private int mForegroundHeight;
    private float mScale = 1.0f;

    public SetupWizardHeader(Context context) {
        super(context);
        init();
    }

    public SetupWizardHeader(Context context, AttributeSet attrs) {
        super(context, attrs);
        initFromAttributes(context, attrs);
    }

    public SetupWizardHeader(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initFromAttributes(context, attrs);
    }

    public SetupWizardHeader(Context context, AttributeSet attrs, int defStyleAttr,
                             int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initFromAttributes(context, attrs);
    }

    private void initFromAttributes(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs,
                new int[] { android.R.attr.foreground });
        setForeground(a.getDrawable(0));
        init();
    }

    protected void init() {
        // Number of pixels of the 8dp baseline grid as defined in material design specs
        mBaselineGridSize = getResources().getDisplayMetrics().density * 8;
        setWillNotDraw(false);
    }

    /**
     * The background will be drawn to fill up the rest of the view. It will also be scaled by the
     * same amount as the foreground so their textures look the same.
     */
    @Override
    public void setBackground(Drawable background) {
        mBackground = background;
    }

    /**
     * Sets the drawable used as the illustration. THe drawable is expected to have intrinsic
     * width and height defined and will be scaled to fit the width of the view.
     */
    public void setForeground(Drawable foreground) {
        mForeground = foreground;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int illustrationHeight = MeasureSpec.getSize(widthMeasureSpec) / 2;
        illustrationHeight -= illustrationHeight % mBaselineGridSize;
        setPadding(0, illustrationHeight, 0, 0);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        final int layoutWidth = right - left;
        final int layoutHeight = bottom - top;
        if (mForeground != null) {
            // Scale the foreground to fill the width of the view
            mScale = layoutWidth / (float) mForeground.getIntrinsicWidth();
            mForegroundHeight = (int) (mForeground.getIntrinsicHeight() * mScale);
            mForeground.setBounds(0, 0, layoutWidth, mForegroundHeight);
        }
        if (mBackground != null) {
            // Scale the bounds by mScale to compensate for the scale done to the canvas before
            // drawing.
            mBackground.setBounds(0, 0, (int) (layoutWidth / mScale),
                    (int) ((layoutHeight - mForegroundHeight) / mScale));
        }
        super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (mBackground != null) {
            // Draw the background filling parts not covered by the illustration
            int saveCount = canvas.save();
            canvas.translate(0, mForegroundHeight);
            // Scale the background so its size matches the foreground
            canvas.scale(mScale, mScale, 0, 0);
            mBackground.draw(canvas);
            canvas.restoreToCount(saveCount);
        }
        if (mForeground != null) {
            // Draw the illustration
            mForeground.draw(canvas);
        }
        super.onDraw(canvas);
    }
}

