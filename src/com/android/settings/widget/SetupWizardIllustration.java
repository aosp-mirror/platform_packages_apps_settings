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
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;

import com.android.settings.R;

/**
 * Class to draw the illustration of setup wizard. The aspectRatio attribute determines the aspect
 * ratio of the top padding, which is leaving space for the illustration. Draws an illustration
 * (foreground) to fit the width of the view and fills the rest with the background.
 *
 * Copied from com.google.android.setupwizard.util.SetupWizardIllustration
 */
public class SetupWizardIllustration extends FrameLayout {

    private static final String TAG = "SetupWizardIllustration";

    // Size of the baseline grid in pixels
    private float mBaselineGridSize;
    private Drawable mBackground;
    private Drawable mForeground;
    private final Rect mViewBounds = new Rect();
    private final Rect mForegroundBounds = new Rect();
    private float mScale = 1.0f;
    private float mAspectRatio = 0.0f;

    public SetupWizardIllustration(Context context) {
        this(context, null);
    }

    public SetupWizardIllustration(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SetupWizardIllustration(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public SetupWizardIllustration(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs,
                    R.styleable.SetupWizardIllustration, 0, 0);
            mAspectRatio = a.getFloat(R.styleable.SetupWizardIllustration_aspectRatio, 0.0f);
            a.recycle();
        }
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
    @Override
    public void setForeground(Drawable foreground) {
        mForeground = foreground;
    }

    @Override
    public void onResolveDrawables(int layoutDirection) {
        mBackground.setLayoutDirection(layoutDirection);
        mForeground.setLayoutDirection(layoutDirection);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mAspectRatio != 0.0f) {
            int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
            int illustrationHeight = (int) (parentWidth / mAspectRatio);
            illustrationHeight -= illustrationHeight % mBaselineGridSize;
            setPaddingRelative(0, illustrationHeight, 0, 0);
        }
        setOutlineProvider(ViewOutlineProvider.BOUNDS);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        final int layoutWidth = right - left;
        final int layoutHeight = bottom - top;
        if (mForeground != null) {
            int intrinsicWidth = mForeground.getIntrinsicWidth();
            int intrinsicHeight = mForeground.getIntrinsicHeight();
            final int layoutDirection = getLayoutDirection();

            mViewBounds.set(0, 0, layoutWidth, layoutHeight);
            if (mAspectRatio != 0f) {
                mScale = layoutWidth / (float) intrinsicWidth;
                intrinsicWidth = layoutWidth;
                intrinsicHeight = (int) (intrinsicHeight * mScale);
            }
            Gravity.apply(Gravity.FILL_HORIZONTAL | Gravity.TOP, intrinsicWidth, intrinsicHeight,
                    mViewBounds, mForegroundBounds, layoutDirection);
            mForeground.setBounds(mForegroundBounds);
        }
        if (mBackground != null) {
            // Scale the bounds by mScale to compensate for the scale done to the canvas before
            // drawing.
            mBackground.setBounds(0, 0, (int) Math.ceil(layoutWidth / mScale),
                    (int) Math.ceil((layoutHeight - mForegroundBounds.height()) / mScale));
        }
        super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (mBackground != null) {
            canvas.save();
            // Draw the background filling parts not covered by the illustration
            canvas.translate(0, mForegroundBounds.height());
            // Scale the background so its size matches the foreground
            canvas.scale(mScale, mScale, 0, 0);
            mBackground.draw(canvas);
            canvas.restore();
        }
        if (mForeground != null) {
            canvas.save();
            // Draw the illustration
            mForeground.draw(canvas);
            canvas.restore();
        }
        super.onDraw(canvas);
    }
}
