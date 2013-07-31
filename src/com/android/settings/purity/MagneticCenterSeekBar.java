/*
 * Copyright (C) 2013 The CyanogenMod Project
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

package com.android.settings.purity;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.SeekBar;

public class MagneticCenterSeekBar extends SeekBar {
    private static final float CENTER_SNAP_IN_THRESHOLD = 0.03f;
    private static final float CENTER_SNAP_OUT_THRESHOLD = 0.03f;

    public MagneticCenterSeekBar(Context context) {
        super(context);
    }

    public MagneticCenterSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MagneticCenterSeekBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected int updateTouchProgress(int lastProgress, int newProgress) {
        int centerProgress = getMax() / 2;
        int inThreshold = (int) (CENTER_SNAP_IN_THRESHOLD * getMax());
        int outThreshold = (int) (CENTER_SNAP_OUT_THRESHOLD * getMax());
        boolean resetToCenter = false;

        if (newProgress > lastProgress) {
            resetToCenter =
                    newProgress < centerProgress && newProgress > centerProgress - inThreshold
                    || newProgress > centerProgress && newProgress < centerProgress + outThreshold;
        } else if (newProgress < lastProgress) {
            resetToCenter =
                    newProgress > centerProgress && newProgress < centerProgress + inThreshold
                    || newProgress < centerProgress && newProgress > centerProgress + outThreshold;
        }

        if (resetToCenter) {
            return centerProgress;
        }

        return newProgress;
    }
}
