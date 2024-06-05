/*
 * Copyright (C) 2022 The Android Open Source Project
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

import static android.util.FeatureFlagUtils.SETTINGS_ENABLE_MONITOR_PHANTOM_PROCS;

import android.content.Context;
import android.util.FeatureFlagUtils;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

/**
 * PreferenceController for MockModem
 */
public class PhantomProcessPreferenceController extends
        DeveloperOptionsPreferenceController implements Preference.OnPreferenceChangeListener,
        PreferenceControllerMixin {

    private static final String TAG = "PhantomProcessPreferenceController";
    private static final String DISABLE_PHANTOM_PROCESS_MONITOR_KEY =
            "disable_phantom_process_monitor";

    public PhantomProcessPreferenceController(Context context) {
        super(context);
    }

    @Override
    public String getPreferenceKey() {
        return DISABLE_PHANTOM_PROCESS_MONITOR_KEY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean isEnabled = (Boolean) newValue; // true means we're disabling this flag.
        try {
            FeatureFlagUtils.setEnabled(mContext,
                    SETTINGS_ENABLE_MONITOR_PHANTOM_PROCS,
                    !isEnabled);
        } catch (RuntimeException e) {
            Log.e(TAG, "Fail to set feature flag: " + e.getMessage());
        }
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        try {
            final boolean isEnabled = !FeatureFlagUtils.isEnabled(mContext,
                    SETTINGS_ENABLE_MONITOR_PHANTOM_PROCS);
            ((TwoStatePreference) mPreference).setChecked(isEnabled);
        } catch (RuntimeException e) {
            Log.e(TAG, "Fail to get feature flag: " + e.getMessage());
        }
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        try {
            FeatureFlagUtils.setEnabled(mContext,
                    SETTINGS_ENABLE_MONITOR_PHANTOM_PROCS,
                    true /* Enable the monitoring */);
            ((TwoStatePreference) mPreference).setChecked(false);
        } catch (RuntimeException e) {
            Log.e(TAG, "Fail to set feature flag: " + e.getMessage());
        }
    }
}
