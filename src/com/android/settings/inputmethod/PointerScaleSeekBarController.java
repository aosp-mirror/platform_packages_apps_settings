/*
 * Copyright 2024 The Android Open Source Project
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

package com.android.settings.inputmethod;

import static android.view.PointerIcon.DEFAULT_POINTER_SCALE;

import android.content.Context;
import android.content.res.Resources;
import android.os.UserHandle;
import android.provider.Settings;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.widget.LabeledSeekBarPreference;

public class PointerScaleSeekBarController extends BasePreferenceController {

    private final int mProgressMin;
    private final int mProgressMax;
    private final float mScaleMin;
    private final float mScaleMax;

    public PointerScaleSeekBarController(@NonNull Context context, @NonNull String key) {
        super(context, key);

        Resources res =  context.getResources();
        mProgressMin = res.getInteger(R.integer.pointer_scale_seek_bar_start);
        mProgressMax = res.getInteger(R.integer.pointer_scale_seek_bar_end);
        mScaleMin = res.getFloat(R.dimen.pointer_scale_size_start);
        mScaleMax = res.getFloat(R.dimen.pointer_scale_size_end);
    }

    @AvailabilityStatus
    public int getAvailabilityStatus() {
        return android.view.flags.Flags.enableVectorCursorA11ySettings() ? AVAILABLE
                : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);

        LabeledSeekBarPreference seekBarPreference = screen.findPreference(getPreferenceKey());
        seekBarPreference.setMax(mProgressMax);
        seekBarPreference.setContinuousUpdates(/* continuousUpdates= */ true);
        seekBarPreference.setProgress(scaleToProgress(
                Settings.System.getFloatForUser(mContext.getContentResolver(),
                        Settings.System.POINTER_SCALE, DEFAULT_POINTER_SCALE,
                        UserHandle.USER_CURRENT)));
        seekBarPreference.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(@NonNull SeekBar seekBar, int progress,
                    boolean fromUser) {
                Settings.System.putFloatForUser(mContext.getContentResolver(),
                        Settings.System.POINTER_SCALE, progressToScale(progress),
                        UserHandle.USER_CURRENT);
            }

            @Override
            public void onStartTrackingTouch(@NonNull SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(@NonNull SeekBar seekBar) {}
        });
    }

    private float progressToScale(int progress) {
        return (((progress - mProgressMin) * (mScaleMax - mScaleMin)) / (mProgressMax
                - mProgressMin)) + mScaleMin;
    }

    private int scaleToProgress(float scale) {
        return (int) (
                (((scale - mScaleMin) * (mProgressMax - mProgressMin)) / (mScaleMax - mScaleMin))
                        + mProgressMin);
    }
}
