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

package com.android.settings.development;

import android.content.Context;
import android.hardware.input.InputSettings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

/** PreferenceController that controls the "Show touchpad input" developer option. */
public class TouchpadVisualizerPreferenceController extends
        DeveloperOptionsPreferenceController implements
        Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    private static final String TOUCHPAD_VISUALIZER_KEY = "touchpad_visualizer";

    public TouchpadVisualizerPreferenceController(@NonNull Context context) {
        super(context);
    }

    @Override
    public @NonNull String getPreferenceKey() {
        return TOUCHPAD_VISUALIZER_KEY;
    }

    @Override
    public boolean isAvailable(){
        return InputSettings.isTouchpadVisualizerFeatureFlagEnabled();
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, @Nullable Object newValue) {
        final boolean isEnabled = newValue != null ? (Boolean) newValue : false;
        InputSettings.setTouchpadVisualizer(mContext, isEnabled);

        return true;
    }

    @Override
    public void updateState(@NonNull Preference preference) {
        boolean touchpadVisualizerEnabled = InputSettings.useTouchpadVisualizer(mContext);
        ((SwitchPreference) mPreference).setChecked(touchpadVisualizerEnabled);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        InputSettings.setTouchpadVisualizer(mContext, false);

        ((SwitchPreference) mPreference).setChecked(false);
    }
}