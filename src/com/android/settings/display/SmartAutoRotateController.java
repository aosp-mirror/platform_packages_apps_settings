/*
 * Copyright (C) 2021 The Android Open Source Project
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

import static android.provider.Settings.Secure.CAMERA_AUTOROTATE;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.provider.Settings;

import androidx.preference.Preference;

import com.android.internal.view.RotationPolicy;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

/**
 * SmartAutoRotateController controls whether auto rotation is enabled
 */
public class SmartAutoRotateController extends TogglePreferenceController implements
        Preference.OnPreferenceChangeListener {

    private final MetricsFeatureProvider mMetricsFeatureProvider;

    public SmartAutoRotateController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
    }

    @Override
    public int getAvailabilityStatus() {
        return !RotationPolicy.isRotationLocked(mContext)
                ? AVAILABLE : DISABLED_DEPENDENT_SETTING;
    }

    @Override
    public boolean isChecked() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                CAMERA_AUTOROTATE, 0) == 1;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        mMetricsFeatureProvider.action(mContext, SettingsEnums.ACTION_CAMERA_ROTATE_TOGGLE,
                isChecked);
        Settings.Secure.putInt(mContext.getContentResolver(),
                CAMERA_AUTOROTATE,
                isChecked ? 1 : 0);
        return true;
    }
}
