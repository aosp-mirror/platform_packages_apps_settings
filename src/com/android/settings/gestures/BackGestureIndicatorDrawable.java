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

import android.animation.TimeAnimator;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;

/** A drawable to animate the inset back gesture in both edges of the screen */
public class BackGestureIndicatorDrawable extends Drawable {

    private static final String TAG = "BackGestureIndicatorDrawable";

    private static final int MSG_SET_INDICATOR_WIDTH = 1;
    private static final int MSG_HIDE_INDICATOR = 3;

    private static final long ANIMATION_DURATION_MS = 200L;
    private static final long HIDE_DELAY_MS = 700L;

    private static final int ALPHA_MAX = 64;

    private Context mContext;

    private Paint mPaint = new Paint();
    private boolean mReversed;

    private float mFinalWidth;
    private float mCurrentWidth;
    private float mWidthChangePerMs;

    private TimeAnimator mTimeAnimator = new TimeAnimator();

    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case MSG_SET_INDICATOR_WIDTH:
                    mTimeAnimator.end();
                    mFinalWidth = msg.arg1;
                    mWidthChangePerMs = Math.abs(mCurrentWidth - mFinalWidth)
                            / ANIMATION_DURATION_MS;
                    mTimeAnimator.start();
                    break;
                case MSG_HIDE_INDICATOR:
                    mCurrentWidth = mFinalWidth;
                    removeMessages(MSG_SET_INDICATOR_WIDTH);
                    sendMessageDelayed(obtainMessage(MSG_SET_INDICATOR_WIDTH, 0, 0), HIDE_DELAY_MS);
                    invalidateSelf();
                    break;
                default:
                    break;
            }
        }
    };

    /**
     * Creates an indicator drawable that responds to back gesture inset size change
     * @param reversed If false, indicator will expand right. If true, indicator will expand left
     */
    public BackGestureIndicatorDrawable(Context context, boolean reversed) {
        mContext = context;
        mReversed = reversed;

        // Restart the timer whenever a change is detected, so we can shrink/fade the indicators
        mTimeAnimator.setTimeListener((TimeAnimator animation, long totalTime, long deltaTime) -> {
            updateCurrentWidth(totalTime, deltaTime);
            invalidateSelf();
        });
    }

    private void updateCurrentWidth(long totalTime, long deltaTime) {
        synchronized (mTimeAnimator) {
            float step = deltaTime * mWidthChangePerMs;
            if (totalTime >= ANIMATION_DURATION_MS
                    || step >= Math.abs(mFinalWidth - mCurrentWidth)) {
                mCurrentWidth = mFinalWidth;
                mTimeAnimator.end();
            } else {
                float direction = mCurrentWidth < mFinalWidth ? 1 : -1;
                mCurrentWidth += direction * step;
            }
        }
    }

    @Override
    public void draw(@NonNull Canvas canvas) {

        mPaint.setAntiAlias(true);
        mPaint.setColor(mContext.getResources().getColor(R.color.back_gesture_indicator));
        mPaint.setAlpha(ALPHA_MAX);

        final int top = 0;
        final int bottom = canvas.getHeight();
        final int width = (int) mCurrentWidth;

        Rect rect = new Rect(0, top, width, bottom);
        if (mReversed) {
            rect.offset(canvas.getWidth() - width, 0);
        }

        canvas.drawRect(rect, mPaint);
    }

    @Override
    public void setAlpha(@IntRange(from = 0, to = 255) int alpha) {

    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {

    }

    @Override
    public int getOpacity() {
        return 0;
    }

    /**
     * Sets the visible width of the indicator in pixels.
     */
    public void setWidth(int width) {
        if (width == 0) {
            mHandler.sendEmptyMessage(MSG_HIDE_INDICATOR);
        } else {
            mHandler.sendMessage(mHandler.obtainMessage(MSG_SET_INDICATOR_WIDTH, width, 0));
        }
    }

    @VisibleForTesting
    public int getWidth() {
        return (int) mFinalWidth;
    }
}
