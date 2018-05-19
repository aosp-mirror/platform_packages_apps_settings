/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.display;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;

import com.android.internal.app.ColorDisplayController;
import com.android.settings.core.SliderPreferenceController;
import com.android.settings.widget.SeekBarPreference;

public class NightDisplayIntensityPreferenceController extends SliderPreferenceController {

    private ColorDisplayController mController;

    public NightDisplayIntensityPreferenceController(Context context, String key) {
        super(context, key);
        mController = new ColorDisplayController(context);
    }

    @Override
    public int getAvailabilityStatus() {
        if (!ColorDisplayController.isAvailable(mContext)) {
            return UNSUPPORTED_ON_DEVICE;
        } else if (!mController.isActivated()) {
            return DISABLED_DEPENDENT_SETTING;
        }
        return AVAILABLE;
    }

    @Override
    public boolean isSliceable() {
        return TextUtils.equals(getPreferenceKey(), "night_display_temperature");
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final SeekBarPreference preference = (SeekBarPreference) screen.findPreference(
            getPreferenceKey());
        preference.setContinuousUpdates(true);
        preference.setMax(getMaxSteps());
    }

    @Override
    public final void updateState(Preference preference) {
        super.updateState(preference);
        preference.setEnabled(mController.isActivated());
    }

    @Override
    public int getSliderPosition() {
        return convertTemperature(mController.getColorTemperature());
    }

    @Override
    public boolean setSliderPosition(int position) {
        return mController.setColorTemperature(convertTemperature(position));
    }

    @Override
    public int getMaxSteps() {
        return convertTemperature(mController.getMinimumColorTemperature());
    }

    /**
     * Inverts and range-adjusts a raw value from the SeekBar (i.e. [0, maxTemp-minTemp]), or
     * converts an inverted and range-adjusted value to the raw SeekBar value, depending on the
     * adjustment status of the input.
     */
    private int convertTemperature(int temperature) {
        return mController.getMaximumColorTemperature() - temperature;
    }
}