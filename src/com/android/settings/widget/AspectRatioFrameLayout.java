/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;

/**
 * A {@link FrameLayout} with customizable aspect ratio.
 * This is used to avoid dynamically calculating the height for the frame. Default aspect
 * ratio will be 1 if none is set in layout attribute.
 */
public final class AspectRatioFrameLayout extends FrameLayout {

    private static final float ASPECT_RATIO_CHANGE_THREASHOLD = 0.01f;

    @VisibleForTesting
    float mAspectRatio = 1.0f;

    public AspectRatioFrameLayout(Context context) {
        this(context, null);
    }

    public AspectRatioFrameLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AspectRatioFrameLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        if (attrs != null) {
            TypedArray array =
                    context.obtainStyledAttributes(attrs, R.styleable.AspectRatioFrameLayout);
            mAspectRatio = array.getFloat(
                    R.styleable.AspectRatioFrameLayout_aspectRatio, 1.0f);
            array.recycle();
        }
    }

    public void setAspectRatio(float aspectRadio) {
        mAspectRatio = aspectRadio;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        if (width == 0 || height == 0) {
            return;
        }
        final float viewAspectRatio = (float) width / height;
        final float aspectRatioDiff = mAspectRatio - viewAspectRatio;
        if (Math.abs(aspectRatioDiff) <= ASPECT_RATIO_CHANGE_THREASHOLD) {
            // Close enough, skip.
            return;
        }

        width = (int) (height * mAspectRatio);

        super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
    }

}
