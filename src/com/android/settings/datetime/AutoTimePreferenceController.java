/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.datetime;

import static android.app.time.Capabilities.CAPABILITY_NOT_ALLOWED;
import static android.app.time.Capabilities.CAPABILITY_NOT_APPLICABLE;
import static android.app.time.Capabilities.CAPABILITY_NOT_SUPPORTED;
import static android.app.time.Capabilities.CAPABILITY_POSSESSED;

import android.app.time.TimeCapabilities;
import android.app.time.TimeCapabilitiesAndConfig;
import android.app.time.TimeConfiguration;
import android.app.time.TimeManager;
import android.content.Context;

import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

public class AutoTimePreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    private static final String KEY_AUTO_TIME = "auto_time";
    private final UpdateTimeAndDateCallback mCallback;
    private final TimeManager mTimeManager;

    public AutoTimePreferenceController(Context context, UpdateTimeAndDateCallback callback) {
        super(context);
        mTimeManager = context.getSystemService(TimeManager.class);
        mCallback = callback;
    }

    @Override
    public boolean isAvailable() {
        TimeCapabilities timeCapabilities =
                getTimeCapabilitiesAndConfig().getCapabilities();
        int capability = timeCapabilities.getConfigureAutoDetectionEnabledCapability();

        // The preference only has two states: present and not present. The preference is never
        // present but disabled.
        if (capability == CAPABILITY_NOT_SUPPORTED
                || capability == CAPABILITY_NOT_ALLOWED
                || capability == CAPABILITY_NOT_APPLICABLE) {
            return false;
        } else if (capability == CAPABILITY_POSSESSED) {
            return true;
        } else {
            throw new IllegalStateException("Unknown capability=" + capability);
        }
    }

    @Override
    public void updateState(Preference preference) {
        if (!(preference instanceof SwitchPreference)) {
            return;
        }
        ((SwitchPreference) preference).setChecked(isEnabled());
    }

    @Override
    public String getPreferenceKey() {
        return KEY_AUTO_TIME;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean autoTimeEnabled = (Boolean) newValue;
        TimeConfiguration configuration = new TimeConfiguration.Builder()
                .setAutoDetectionEnabled(autoTimeEnabled)
                .build();
        boolean result = mTimeManager.updateTimeConfiguration(configuration);

        mCallback.updateTimeAndDateDisplay(mContext);
        return result;
    }

    /** Returns whether the preference should be "checked", i.e. set to the "on" position. */
    @VisibleForTesting
    public boolean isEnabled() {
        TimeConfiguration config = getTimeCapabilitiesAndConfig().getConfiguration();
        return config.isAutoDetectionEnabled();
    }

    private TimeCapabilitiesAndConfig getTimeCapabilitiesAndConfig() {
        return mTimeManager.getTimeCapabilitiesAndConfig();
    }
}
