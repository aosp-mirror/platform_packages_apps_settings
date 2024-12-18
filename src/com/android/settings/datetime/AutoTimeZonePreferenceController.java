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
import android.util.Log;

import androidx.preference.Preference;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

public class AutoTimeZonePreferenceController extends TogglePreferenceController {

    private static final String TAG = "AutoTimeZonePreferenceController";

    private boolean mIsFromSUW;
    private UpdateTimeAndDateCallback mCallback;
    private final TimeManager mTimeManager;

    public AutoTimeZonePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mTimeManager = context.getSystemService(TimeManager.class);
        // This is a no-op implementation of UpdateTimeAndDateCallback to avoid a NPE when
        // setTimeAndDateCallback() isn't called, e.g. for slices and other cases where the
        // controller is instantiated outside of the context of the real Date & Time settings
        // screen.
        mCallback = (c) -> {};
    }

    /**
     * Set the Time and Date callback
     */
    public AutoTimeZonePreferenceController setTimeAndDateCallback(
            UpdateTimeAndDateCallback callback) {
        mCallback = callback;
        return this;
    }

    /**
     * Set if current fragment is launched via SUW
     */
    public AutoTimeZonePreferenceController setFromSUW(boolean isFromSUW) {
        mIsFromSUW = isFromSUW;
        return this;
    }

    @Override
    public int getAvailabilityStatus() {
        if (mIsFromSUW) {
            return DISABLED_DEPENDENT_SETTING;
        }

        TimeZoneCapabilities timeZoneCapabilities =
                getTimeZoneCapabilitiesAndConfig().getCapabilities();
        int capability = timeZoneCapabilities.getConfigureAutoDetectionEnabledCapability();

        // The preference has three states: visible, not visible, and visible but disabled.
        // This method handles the "is visible?" check.
        switch (capability) {
            case CAPABILITY_NOT_SUPPORTED:
                return DISABLED_DEPENDENT_SETTING;
            case CAPABILITY_POSSESSED:
            case CAPABILITY_NOT_ALLOWED:
                // This case is expected for enterprise restrictions, where the toggle should be
                // present but disabled. Disabling is handled declaratively via the
                // settings:userRestriction attribute in .xml. The client-side logic is expected to
                // concur with the capabilities logic in the system server.
            case CAPABILITY_NOT_APPLICABLE:
                // CAPABILITY_NOT_APPLICABLE is not currently expected, so this is return value is
                // arbitrary.
                return AVAILABLE;
            default:
                Log.e(TAG, "Unknown capability=" + capability);
                return UNSUPPORTED_ON_DEVICE;
        }
    }

    @Override
    public boolean isChecked() {
        return isEnabled();
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        TimeZoneConfiguration.Builder configuration = new TimeZoneConfiguration.Builder()
                .setAutoDetectionEnabled(isChecked);

        // "Use location for time zone" is only used if "Automatic time zone" is enabled. If
        // the user toggles off automatic time zone, set the toggle off and disable the toggle.
        int geoDetectionCapability = mTimeManager
                .getTimeZoneCapabilitiesAndConfig()
                .getCapabilities()
                .getConfigureGeoDetectionEnabledCapability();

        if (!isChecked
                && (geoDetectionCapability == CAPABILITY_NOT_APPLICABLE
                || geoDetectionCapability == CAPABILITY_POSSESSED)) {
            configuration.setGeoDetectionEnabled(false);
        }

        boolean result = mTimeManager.updateTimeZoneConfiguration(configuration.build());

        mCallback.updateTimeAndDateDisplay(mContext);
        return result;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_system;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        refreshSummary(preference);
    }

    @Override
    public CharSequence getSummary() {
        // If auto time zone cannot enable telephony fallback and is capable of location, then auto
        // time zone must use location.
        if (LocationProviderStatusPreferenceController.hasLocationTimeZoneNoTelephonyFallback(
                mTimeManager.getTimeZoneCapabilitiesAndConfig().getDetectorStatus())) {
            return mContext.getString(R.string.auto_zone_requires_location_summary);
        }

        // If the user has a dedicated toggle to control location use, explain what it does.
        return mContext.getString(R.string.zone_auto_title_summary);
    }

    @VisibleForTesting
    boolean isEnabled() {
        return mTimeManager
                .getTimeZoneCapabilitiesAndConfig()
                .getConfiguration()
                .isAutoDetectionEnabled();
    }

    private TimeZoneCapabilitiesAndConfig getTimeZoneCapabilitiesAndConfig() {
        return mTimeManager.getTimeZoneCapabilitiesAndConfig();
    }
}
