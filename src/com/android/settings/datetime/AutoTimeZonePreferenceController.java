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

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
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

        // The preference has three states: visible, not visible, and visible but disabled.
        // This method handles the "is visible?" check.
        switch (capability) {
            case CAPABILITY_NOT_SUPPORTED:
                return false;
            case CAPABILITY_POSSESSED:
                return true;
            case CAPABILITY_NOT_ALLOWED:
                // This case is expected for enterprise restrictions, where the toggle should be
                // present but disabled. Disabling is handled declaratively via the
                // settings:userRestriction attribute in .xml. The client-side logic is expected to
                // concur with the capabilities logic in the system server.
                return true;
            case CAPABILITY_NOT_APPLICABLE:
                // CAPABILITY_NOT_APPLICABLE is not currently expected, so this is return value is
                // arbitrary.
                return true;
            default:
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

    @Override
    public CharSequence getSummary() {
        // If auto time zone cannot enable telephony fallback and is capable of location, then auto
        // time zone must use location.
        if (LocationProviderStatusPreferenceController.hasLocationTimeZoneNoTelephonyFallback(
                mTimeManager.getTimeZoneCapabilitiesAndConfig().getDetectorStatus())) {
            return mContext.getResources().getString(R.string.auto_zone_requires_location_summary);
        }
        // If the user has a dedicated toggle to control location use, the summary can
        // be empty because the use of location is explicit.
        return "";
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        refreshSummary(screen.findPreference(getPreferenceKey()));
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
