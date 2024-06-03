/*
 * Copyright (C) 2024 The Android Open Source Project
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

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;

import androidx.preference.PreferenceScreen;

import com.android.server.accessibility.Flags;
import com.android.settings.core.SliderPreferenceController;
import com.android.settings.widget.SeekBarPreference;

/**
 * The controller of the seekbar preference for the saturation level of color correction.
 */
public class DaltonizerSaturationSeekbarPreferenceController extends SliderPreferenceController {

    private static final int DEFAULT_SATURATION_LEVEL = 7;
    private static final int SATURATION_MAX = 10;
    private static final int SATURATION_MIN = 0;

    private int mSliderPosition;
    private final ContentResolver mContentResolver;

    public DaltonizerSaturationSeekbarPreferenceController(Context context,
            String preferenceKey) {
        super(context, preferenceKey);
        mContentResolver = context.getContentResolver();
        mSliderPosition = Settings.Secure.getInt(
                mContentResolver,
                Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_SATURATION_LEVEL,
                DEFAULT_SATURATION_LEVEL);
        setSliderPosition(mSliderPosition);
        // TODO: Observer color correction on/off and enable/disable based on secure settings.
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        SeekBarPreference preference = screen.findPreference(getPreferenceKey());
        preference.setMax(getMax());
        preference.setMin(getMin());
        preference.setProgress(mSliderPosition);
        preference.setContinuousUpdates(true);
    }

    @Override
    public int getAvailabilityStatus() {
        if (Flags.enableColorCorrectionSaturation()) {
            return AVAILABLE;
        }
        return CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public int getSliderPosition() {
        return mSliderPosition;
    }

    @Override
    public boolean setSliderPosition(int position) {
        if (position < getMin() || position > getMax()) {
            return false;
        }
        mSliderPosition = position;
        Settings.Secure.putInt(
                mContentResolver,
                Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_SATURATION_LEVEL,
                mSliderPosition);
        return true;
    }

    @Override
    public int getMax() {
        return SATURATION_MAX;
    }

    @Override
    public int getMin() {
        return SATURATION_MIN;
    }
}
