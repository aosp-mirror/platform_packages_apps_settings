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

package com.android.settings.accessibility;

import static android.view.HapticFeedbackConstants.CLOCK_TICK;

import static com.android.settings.Utils.isNightMode;

import android.annotation.StringRes;
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

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.Utils;

/**
 * A custom seekbar for the balance setting.
 *
 * Adds a center line indicator between left and right, which snaps to if close.
 * Updates Settings.System for balance on progress changed.
 */
public class BalanceSeekBar extends SeekBar {
    private final Context mContext;
    private final Object mListenerLock = new Object();
    private OnSeekBarChangeListener mOnSeekBarChangeListener;
    private int mLastProgress = -1;
    private final OnSeekBarChangeListener mProxySeekBarListener = new OnSeekBarChangeListener() {
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            synchronized (mListenerLock) {
                if (mOnSeekBarChangeListener != null) {
                    mOnSeekBarChangeListener.onStopTrackingTouch(seekBar);
                }
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            synchronized (mListenerLock) {
                if (mOnSeekBarChangeListener != null) {
                    mOnSeekBarChangeListener.onStartTrackingTouch(seekBar);
                }
            }
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                // Snap to centre when within the specified threshold
                if (progress != mCenter
                        && progress > mCenter - mSnapThreshold
                        && progress < mCenter + mSnapThreshold) {
                    progress = mCenter;
                    seekBar.setProgress(progress); // direct update (fromUser becomes false)
                }
                if (progress != mLastProgress) {
                    if (progress == mCenter || progress == getMin() || progress == getMax()) {
                        seekBar.performHapticFeedback(CLOCK_TICK);
                    }
                    mLastProgress = progress;
                }
                final float balance = (progress - mCenter) * 0.01f;
                Settings.System.putFloatForUser(mContext.getContentResolver(),
                        Settings.System.MASTER_BALANCE, balance, UserHandle.USER_CURRENT);
            }
            final int max = getMax();
            if (Flags.audioBalanceStateDescription() && max > 0) {
                seekBar.setStateDescription(createStateDescription(mContext,
                        R.string.audio_seek_bar_state_left_first,
                        R.string.audio_seek_bar_state_right_first,
                        progress,
                        max));
            }
            // If fromUser is false, the call is a set from the framework on creation or on
            // internal update. The progress may be zero, ignore (don't change system settings).

            // after adjusting the seekbar, notify downstream listener.
            // note that progress may have been adjusted in the code above to mCenter.
            synchronized (mListenerLock) {
                if (mOnSeekBarChangeListener != null) {
                    mOnSeekBarChangeListener.onProgressChanged(seekBar, progress, fromUser);
                }
            }
        }
    };

    // Percentage of max to be used as a snap to threshold
    @VisibleForTesting
    static final float SNAP_TO_PERCENTAGE = 0.03f;
    private final Paint mCenterMarkerPaint;
    private final Rect mCenterMarkerRect;
    // changed in setMax()
    private float mSnapThreshold;
    private int mCenter;

    public BalanceSeekBar(Context context, AttributeSet attrs) {
        this(context, attrs, com.android.internal.R.attr.seekBarStyle);
    }

    public BalanceSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0 /* defStyleRes */);
    }

    public BalanceSeekBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mContext = context;
        Resources res = getResources();
        mCenterMarkerRect = new Rect(0 /* left */, 0 /* top */,
                res.getDimensionPixelSize(R.dimen.balance_seekbar_center_marker_width),
                res.getDimensionPixelSize(R.dimen.balance_seekbar_center_marker_height));
        mCenterMarkerPaint = new Paint();
        // TODO use a more suitable colour?
        mCenterMarkerPaint.setColor(isNightMode(context) ? Color.WHITE : Color.BLACK);
        mCenterMarkerPaint.setStyle(Paint.Style.FILL);
        // Remove the progress colour
        setProgressTintList(ColorStateList.valueOf(Color.TRANSPARENT));

        super.setOnSeekBarChangeListener(mProxySeekBarListener);
    }

    @Override
    public void setOnSeekBarChangeListener(OnSeekBarChangeListener listener) {
        synchronized (mListenerLock) {
            mOnSeekBarChangeListener = listener;
        }
    }

    // Note: the superclass AbsSeekBar.setMax is synchronized.
    @Override
    public synchronized void setMax(int max) {
        super.setMax(max);
        // update snap to threshold
        mCenter = max / 2;
        mSnapThreshold = max * SNAP_TO_PERCENTAGE;
    }

    // Note: the superclass AbsSeekBar.onDraw is synchronized.
    @Override
    protected synchronized void onDraw(Canvas canvas) {
        // Draw a vertical line at 50% that represents centred balance
        int seekBarCenter = (canvas.getHeight() - getPaddingBottom()) / 2;
        canvas.save();
        canvas.translate((canvas.getWidth() - mCenterMarkerRect.right - getPaddingEnd()) / 2,
                seekBarCenter - (mCenterMarkerRect.bottom / 2));
        canvas.drawRect(mCenterMarkerRect, mCenterMarkerPaint);
        canvas.restore();
        super.onDraw(canvas);
    }

    private static CharSequence createStateDescription(Context context,
            @StringRes int resIdLeftFirst, @StringRes int resIdRightFirst,
            int progress, float max) {
        final boolean isLayoutRtl = context.getResources().getConfiguration().getLayoutDirection()
                == LAYOUT_DIRECTION_RTL;
        final int rightPercent = (int) (100 * (progress / max));
        final int leftPercent = 100 - rightPercent;
        final String rightPercentString = Utils.formatPercentage(rightPercent);
        final String leftPercentString = Utils.formatPercentage(leftPercent);
        if (rightPercent > leftPercent || (rightPercent == leftPercent && isLayoutRtl)) {
            return context.getString(resIdRightFirst, rightPercentString, leftPercentString);
        } else {
            return context.getString(resIdLeftFirst, leftPercentString, rightPercentString);
        }
    }
}

