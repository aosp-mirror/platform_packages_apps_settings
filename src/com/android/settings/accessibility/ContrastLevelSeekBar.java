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

package com.android.settings.accessibility;

import static android.view.HapticFeedbackConstants.CLOCK_TICK;

import static com.android.settings.Utils.isNightMode;
import static com.android.settings.accessibility.ContrastLevelSeekBarPreference.CONTRAST_SLIDER_TICKS;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.widget.SeekBar;

import com.android.settings.R;

/**
 * A custom seekbar for the contrast level setting.
 *
 * Adds a center line indicator between left and right, which snaps to if close.
 * Updates the Settings.Secure.CONTRAST_LEVEL setting on progress changed.
 *
 * TODO(b/266071578): remove this class and replace this with the final UI
 */
public class ContrastLevelSeekBar extends SeekBar {

    private final Context mContext;
    private int mLastProgress = -1;

    private final Paint mMarkerPaint;
    private final Rect mMarkerRect;

    private final OnSeekBarChangeListener mProxySeekBarListener = new OnSeekBarChangeListener() {
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) { }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) { }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (!fromUser || progress == mLastProgress) return;
            seekBar.performHapticFeedback(CLOCK_TICK);
            mLastProgress = progress;

            // rescale progress from [0, 1, 2] to [0, 0.5, 1]
            final float contrastLevel = (float) progress / CONTRAST_SLIDER_TICKS;

            Settings.Secure.putFloatForUser(mContext.getContentResolver(),
                    Settings.Secure.CONTRAST_LEVEL, contrastLevel, UserHandle.USER_CURRENT);
        }
    };

    public ContrastLevelSeekBar(Context context, AttributeSet attrs) {
        this(context, attrs, com.android.internal.R.attr.seekBarStyle);
    }

    public ContrastLevelSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0 /* defStyleRes */);
    }

    public ContrastLevelSeekBar(
            Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mContext = context;
        Resources res = getResources();
        mMarkerRect = new Rect(0 /* left */, 0 /* top */,
                res.getDimensionPixelSize(R.dimen.contrast_level_seekbar_center_marker_width),
                res.getDimensionPixelSize(R.dimen.contrast_level_seekbar_center_marker_height));
        mMarkerPaint = new Paint();

        // the might be a better colour for the markers, but this slider is temporary anyway
        mMarkerPaint.setColor(isNightMode(context) ? Color.WHITE : Color.BLACK);
        mMarkerPaint.setStyle(Paint.Style.FILL);
        // Remove the progress colour
        setProgressTintList(ColorStateList.valueOf(Color.TRANSPARENT));
        super.setOnSeekBarChangeListener(mProxySeekBarListener);
    }

    @Override
    public void setOnSeekBarChangeListener(OnSeekBarChangeListener listener) { }

    // Note: the superclass AbsSeekBar.onDraw is synchronized.
    @Override
    protected synchronized void onDraw(Canvas canvas) {

        // Draw a marker at the center of the seekbar
        int seekBarCenter = (getHeight() - getPaddingBottom()) / 2;
        float sliderWidth = getWidth() - mMarkerRect.right - getPaddingEnd();
        canvas.save();
        canvas.translate(sliderWidth / 2f,
                seekBarCenter - (mMarkerRect.bottom / 2f));
        canvas.drawRect(mMarkerRect, mMarkerPaint);
        canvas.restore();
        super.onDraw(canvas);
    }
}
