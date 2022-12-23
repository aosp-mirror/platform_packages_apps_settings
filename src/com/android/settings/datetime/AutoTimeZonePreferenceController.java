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

import android.app.time.TimeManager;
import android.app.time.TimeZoneCapabilities;
import android.app.time.TimeZoneCapabilitiesAndConfig;
import android.app.time.TimeZoneConfiguration;
import android.content.Context;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

public class AutoTimeZonePreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    private static final String KEY_AUTO_TIME_ZONE = "auto_zone";

    private final boolean mIsFromSUW;
    private final UpdateTimeAndDateCallback mCallback;
    private final TimeManager mTimeManager;

    public AutoTimeZonePreferenceController(Context context, UpdateTimeAndDateCallback callback,
            boolean isFromSUW) {
        super(context);
        mTimeManager = context.getSystemService(TimeManager.class);
        mCallback = callback;
        mIsFromSUW = isFromSUW;
    }

    @Override
    public boolean isAvailable() {
        if (mIsFromSUW) {
            return false;
        }

        TimeZoneCapabilities timeZoneCapabilities =
                getTimeZoneCapabilitiesAndConfig().getCapabilities();
        int capability = timeZoneCapabilities.getConfigureAutoDetectionEnabledCapability();

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
    public String getPreferenceKey() {
        return KEY_AUTO_TIME_ZONE;
    }

    @Override
    public void updateState(Preference preference) {
        if (!(preference instanceof SwitchPreference)) {
            return;
        }
        ((SwitchPreference) preference).setChecked(isEnabled());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean autoZoneEnabled = (Boolean) newValue;
        TimeZoneConfiguration configuration = new TimeZoneConfiguration.Builder()
                .setAutoDetectionEnabled(autoZoneEnabled)
                .build();
        boolean result = mTimeManager.updateTimeZoneConfiguration(configuration);

        mCallback.updateTimeAndDateDisplay(mContext);
        return result;
    }

    @VisibleForTesting
    boolean isEnabled() {
        TimeZoneConfiguration config = getTimeZoneCapabilitiesAndConfig().getConfiguration();
        return config.isAutoDetectionEnabled();
    }

    private TimeZoneCapabilitiesAndConfig getTimeZoneCapabilitiesAndConfig() {
        return mTimeManager.getTimeZoneCapabilitiesAndConfig();
    }
}
