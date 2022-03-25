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

package com.android.settings.deviceinfo;

import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.ProgressBar;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settings.deviceinfo.storage.StorageUtils;

public class StorageItemPreference extends Preference {
    public int userHandle;

    private static final int UNINITIALIZED = -1;
    private static final int ANIMATE_DURATION_IN_MILLIS = 1000;

    private ProgressBar mProgressBar;
    private static final int PROGRESS_MAX = 100;
    private int mProgressPercent = UNINITIALIZED;
    private long mStorageSize;

    public StorageItemPreference(Context context) {
        this(context, null);
    }

    public StorageItemPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.storage_item);
    }

    public void setStorageSize(long size, long total) {
        setStorageSize(size, total, false /* animate */);
    }

    /**
     * Set the storage size info with/without animation
     */
    public void setStorageSize(long size, long total, boolean animate) {
        if (animate) {
            TypeEvaluator<Long> longEvaluator =
                    (fraction, startValue, endValue) -> {
                        // Directly returns end value if fraction is 1.0 and the end value is 0.
                        if (fraction >= 1.0f && endValue == 0) {
                            return endValue;
                        }
                        return startValue + (long) (fraction * (endValue - startValue));
                    };
            ValueAnimator valueAnimator = ValueAnimator.ofObject(longEvaluator, mStorageSize, size);
            valueAnimator.setDuration(ANIMATE_DURATION_IN_MILLIS);
            valueAnimator.addUpdateListener(
                    animation -> {
                        updateProgressBarAndSizeInfo((long) animation.getAnimatedValue(), total);
                    });
            valueAnimator.start();
        } else {
            updateProgressBarAndSizeInfo(size, total);
        }
        mStorageSize = size;
    }

    public long getStorageSize() {
        return mStorageSize;
    }

    protected void updateProgressBar() {
        if (mProgressBar == null || mProgressPercent == UNINITIALIZED) {
            return;
        }

        mProgressBar.setMax(PROGRESS_MAX);
        mProgressBar.setProgress(mProgressPercent);
    }

    private void updateProgressBarAndSizeInfo(long size, long total) {
        setSummary(StorageUtils.getStorageSizeLabel(getContext(), size));
        mProgressPercent = total == 0 ? 0 : (int) (size * PROGRESS_MAX / total);
        updateProgressBar();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        mProgressBar = (ProgressBar) view.findViewById(android.R.id.progress);
        updateProgressBar();
        super.onBindViewHolder(view);
    }
}
