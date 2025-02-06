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

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.hardware.input.InputSettings;

import androidx.annotation.NonNull;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

public class MouseScrollingAccelerationPreferenceController extends TogglePreferenceController {

    private final MetricsFeatureProvider mMetricsFeatureProvider;

    public MouseScrollingAccelerationPreferenceController(
            @NonNull Context context, @NonNull String key) {
        super(context, key);
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
    }

    @Override
    public boolean isChecked() {
        return !InputSettings.isMouseScrollingAccelerationEnabled(mContext);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        InputSettings.setMouseScrollingAcceleration(mContext, !isChecked);
        mMetricsFeatureProvider.action(mContext,
                isChecked ? SettingsEnums.ACTION_MOUSE_SCROLLING_ACCELERATION_DISABLED :
                            SettingsEnums.ACTION_MOUSE_SCROLLING_ACCELERATION_ENABLED);
        return true;
    }

    @Override
    public int getAvailabilityStatus() {
        if (!InputSettings.isMouseScrollingAccelerationFeatureFlagEnabled()) {
            return UNSUPPORTED_ON_DEVICE;
        }
        return AVAILABLE;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_system;
    }
}
