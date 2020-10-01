/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.gestures;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PixelFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.android.settings.R;

/**
 * A linear layout containing the left and right location indicators.
 */
public class BackGestureIndicatorView extends LinearLayout {
    private ViewGroup mLayout;
    private ImageView mLeftIndicator;
    private ImageView mRightIndicator;
    private BackGestureIndicatorDrawable mLeftDrawable;
    private BackGestureIndicatorDrawable mRightDrawable;

    public BackGestureIndicatorView(Context context) {
        super(context);

        LayoutInflater factory = LayoutInflater.from(context);
        mLayout = (ViewGroup) factory.inflate(R.layout.back_gesture_indicator_container,
                this, false);

        if (mLayout == null) {
            return;
        }

        addView(mLayout);

        mLeftDrawable = new BackGestureIndicatorDrawable(context, false);
        mRightDrawable = new BackGestureIndicatorDrawable(context, true);

        mLeftIndicator = mLayout.findViewById(R.id.indicator_left);
        mRightIndicator = mLayout.findViewById(R.id.indicator_right);

        mLeftIndicator.setImageDrawable(mLeftDrawable);
        mRightIndicator.setImageDrawable(mRightDrawable);

        int visibility = getSystemUiVisibility()
                | View.SYSTEM_UI_FLAG_IMMERSIVE
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN;

        TypedArray a = context.obtainStyledAttributes(new int[] {
                android.R.attr.windowLightNavigationBar,
                android.R.attr.windowLightStatusBar});
        if (a.getBoolean(0, false)) {
            visibility |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        if (a.getBoolean(1, false)) {
            visibility |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        }
        a.recycle();

        setSystemUiVisibility(visibility);
    }

    public void setIndicatorWidth(int width, boolean leftIndicator) {
        BackGestureIndicatorDrawable indicator = leftIndicator ? mLeftDrawable : mRightDrawable;
        indicator.setWidth(width);
    }

    public WindowManager.LayoutParams getLayoutParams(
            WindowManager.LayoutParams parentWindowAttributes) {
        int copiedFlags = (parentWindowAttributes.flags
                & WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        final WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | copiedFlags,
                PixelFormat.TRANSLUCENT);

        lp.setTitle("BackGestureIndicatorView");
        lp.token = getContext().getActivityToken();
        return lp;
    }
}
