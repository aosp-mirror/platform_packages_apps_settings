/*
 * Copyright (C) 2023 The Android Open Source Project
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

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.hardware.input.InputSettings;

import androidx.preference.PreferenceScreen;

import com.android.settings.core.SliderPreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.widget.SeekBarPreference;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

public class TrackpadPointerSpeedPreferenceController extends SliderPreferenceController {

    private SeekBarPreference mPreference;
    private MetricsFeatureProvider mMetricsFeatureProvider;

    public TrackpadPointerSpeedPreferenceController(Context context, String key) {
        super(context, key);
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        mPreference.setMax(getMax());
        mPreference.setMin(getMin());
        mPreference.setProgress(getSliderPosition());
        updateState(mPreference);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean setSliderPosition(int position) {
        if (position < getMin() || position > getMax()) {
            return false;
        }
        InputSettings.setTouchpadPointerSpeed(mContext, position);
        mMetricsFeatureProvider.action(
                mContext, SettingsEnums.ACTION_GESTURE_POINTER_SPEED_CHANGED, position);
        return true;
    }

    @Override
    public int getSliderPosition() {
        return InputSettings.getTouchpadPointerSpeed(mContext);
    }

    @Override
    public int getMin() {
        return InputSettings.MIN_POINTER_SPEED;
    }

    @Override
    public int getMax() {
        return InputSettings.MAX_POINTER_SPEED;
    }
}
