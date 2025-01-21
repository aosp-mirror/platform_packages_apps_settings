/*
 * Copyright 2025 The Android Open Source Project
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

import static android.app.settings.SettingsEnums.ACTION_SLOW_KEYS_CUSTOM_VALUE_CHANGE;

import android.hardware.input.InputSettings;
import android.os.Bundle;

import com.android.settings.R;

public class KeyboardAccessibilitySlowKeysDialogFragment extends
        KeyboardAccessibilityKeysDialogFragment {

    static KeyboardAccessibilitySlowKeysDialogFragment getInstance() {
        final KeyboardAccessibilitySlowKeysDialogFragment result =
                new KeyboardAccessibilitySlowKeysDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(EXTRA_TITLE_RES, R.string.slow_keys);
        bundle.putInt(EXTRA_SUBTITLE_RES, R.string.slow_keys_summary);
        bundle.putInt(EXTRA_SEEKBAR_CONTENT_DESCRIPTION,
                R.string.input_setting_slow_keys_seekbar_desc);
        result.setArguments(bundle);
        return result;
    }

    @Override
    protected void updateInputSettingKeysValue(int thresholdTimeMillis) {
        InputSettings.setAccessibilitySlowKeysThreshold(getContext(), thresholdTimeMillis);
    }

    @Override
    protected void onCustomValueUpdated(int thresholdTimeMillis) {
        mMetricsFeatureProvider.action(getContext(),
                ACTION_SLOW_KEYS_CUSTOM_VALUE_CHANGE, thresholdTimeMillis);
    }

    @Override
    protected int getInputSettingKeysValue() {
        return InputSettings.getAccessibilitySlowKeysThreshold(getContext());
    }
}
