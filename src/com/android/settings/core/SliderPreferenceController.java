/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.core;

import android.content.Context;

import androidx.preference.Preference;

import com.android.settings.slices.SliceData;

public abstract class SliderPreferenceController extends BasePreferenceController implements
        Preference.OnPreferenceChangeListener {

    public SliderPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return setSliderPosition((int) newValue);
    }

    @Override
    public void updateState(Preference preference) {
        if (preference instanceof com.android.settings.widget.SeekBarPreference) {
            ((com.android.settings.widget.SeekBarPreference) preference)
                .setProgress(getSliderPosition());
        } else if (preference instanceof androidx.preference.SeekBarPreference) {
            ((androidx.preference.SeekBarPreference) preference)
                .setValue(getSliderPosition());
        }
    }

    /**
     * @return the value of the Slider's position based on the range: [min, max].
     */
    public abstract int getSliderPosition();

    /**
     * Set the slider to a new value.
     *
     * @param position of the slider.
     * @return {@code true} if the position is successfully set.
     */
    public abstract boolean setSliderPosition(int position);

    /**
     * @return the maximum value supported by the slider.
     */
    public abstract int getMax();

    /**
     * @return the minimum value supported by the slider.
     */
    public abstract int getMin();

    @Override
    public int getSliceType() {
        return SliceData.SliceType.SLIDER;
    }
}
