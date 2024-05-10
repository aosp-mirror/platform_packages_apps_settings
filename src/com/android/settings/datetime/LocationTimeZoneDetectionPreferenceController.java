/*
 * Copyright (C) 2020 The Android Open Source Project
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

import com.android.settings.R;
import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settings.core.TogglePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

import java.util.concurrent.Executor;

/**
 * The controller for the "location time zone detection" entry in the Location settings
 * screen.
 */
public class LocationTimeZoneDetectionPreferenceController
        extends TogglePreferenceController
        implements LifecycleObserver, OnStart, OnStop, TimeManager.TimeZoneDetectorListener {

    private static final String TAG = "location_time_zone_detection";

    private final TimeManager mTimeManager;
    private TimeZoneCapabilitiesAndConfig mTimeZoneCapabilitiesAndConfig;
    private InstrumentedPreferenceFragment mFragment;
    private Preference mPreference;

    public LocationTimeZoneDetectionPreferenceController(Context context) {
        super(context, TAG);
        mTimeManager = context.getSystemService(TimeManager.class);
    }

    void setFragment(InstrumentedPreferenceFragment fragment) {
        mFragment = fragment;
    }

    @Override
    public boolean isChecked() {
        TimeZoneCapabilitiesAndConfig capabilitiesAndConfig =
                getTimeZoneCapabilitiesAndConfig(/*forceRefresh=*/false);
        TimeZoneConfiguration configuration = capabilitiesAndConfig.getConfiguration();
        return configuration.isGeoDetectionEnabled();
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        TimeZoneCapabilitiesAndConfig timeZoneCapabilitiesAndConfig =
                getTimeZoneCapabilitiesAndConfig(/*forceRefresh=*/false);
        boolean isLocationEnabled =
                timeZoneCapabilitiesAndConfig.getCapabilities().isUseLocationEnabled();
        if (isChecked && !isLocationEnabled) {
            new LocationToggleDisabledDialogFragment()
                    .show(mFragment.getFragmentManager(), TAG);
            // Toggle status is not updated.
            return false;
        } else {
            TimeZoneConfiguration configuration = new TimeZoneConfiguration.Builder()
                    .setGeoDetectionEnabled(isChecked)
                    .build();
            return mTimeManager.updateTimeZoneConfiguration(configuration);
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void onStart() {
        // Register for updates to the user's time zone capabilities or configuration which could
        // require UI changes.
        Executor mainExecutor = mContext.getMainExecutor();
        mTimeManager.addTimeZoneDetectorListener(mainExecutor, this);
        // Setup the initial state of the summary.
        refreshUi();
    }

    @Override
    public void onStop() {
        mTimeManager.removeTimeZoneDetectorListener(this);
    }

    @Override
    public boolean isSliceable() {
        // Prevent use in a slice, which would enable search to display a toggle in the search
        // results: LocationToggleDisabledDialogFragment has to be shown under some circumstances
        // which doesn't work when embedded in search. b/185906072
        return false;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        // not needed since it's not sliceable
        return NO_RES;
    }

    @Override
    public int getAvailabilityStatus() {
        TimeZoneCapabilities timeZoneCapabilities =
                getTimeZoneCapabilitiesAndConfig(/* forceRefresh= */ false).getCapabilities();
        int capability = timeZoneCapabilities.getConfigureGeoDetectionEnabledCapability();

        // The preference only has two states: present and not present. The preference is never
        // present but disabled.
        if (capability == CAPABILITY_NOT_SUPPORTED || capability == CAPABILITY_NOT_ALLOWED) {
            return UNSUPPORTED_ON_DEVICE;
        } else if (capability == CAPABILITY_NOT_APPLICABLE || capability == CAPABILITY_POSSESSED) {
            return AVAILABLE;
        } else {
            throw new IllegalStateException("Unknown capability=" + capability);
        }
    }

    @Override
    public CharSequence getSummary() {
        TimeZoneCapabilitiesAndConfig timeZoneCapabilitiesAndConfig =
                getTimeZoneCapabilitiesAndConfig(/* forceRefresh= */ false);
        TimeZoneCapabilities capabilities = timeZoneCapabilitiesAndConfig.getCapabilities();
        int configureGeoDetectionEnabledCapability =
                capabilities.getConfigureGeoDetectionEnabledCapability();
        TimeZoneConfiguration configuration = timeZoneCapabilitiesAndConfig.getConfiguration();

        int summaryResId;
        if (configureGeoDetectionEnabledCapability == CAPABILITY_NOT_SUPPORTED) {
            // The preference should not be visible, but text is referenced in case this changes.
            summaryResId = R.string.location_time_zone_detection_not_supported;
        } else if (configureGeoDetectionEnabledCapability == CAPABILITY_NOT_ALLOWED) {
            // The preference should not be visible, but text is referenced in case this changes.
            summaryResId = R.string.location_time_zone_detection_not_allowed;
        } else if (configureGeoDetectionEnabledCapability == CAPABILITY_NOT_APPLICABLE) {
            boolean isLocationEnabled =
                    timeZoneCapabilitiesAndConfig.getCapabilities().isUseLocationEnabled();
            // The TimeZoneCapabilities cannot provide implementation-specific information about why
            // the user doesn't have the capability, but the user's "location enabled" being off and
            // the global automatic detection setting will always be considered overriding reasons
            // why location time zone detection cannot be used.
            if (!isLocationEnabled) {
                summaryResId = R.string.location_app_permission_summary_location_off;
            } else if (!configuration.isAutoDetectionEnabled()) {
                summaryResId = R.string.location_time_zone_detection_auto_is_off;
            } else {
                // This is in case there are other reasons in future why location time zone
                // detection is not applicable.
                summaryResId = R.string.location_time_zone_detection_not_applicable;
            }
        } else if (configureGeoDetectionEnabledCapability == CAPABILITY_POSSESSED) {
            // If capability is possessed, toggle status already tells all the information needed.
            // Returning null will make previous text stick on toggling.
            // See AbstractPreferenceController#refreshSummary.
            summaryResId = R.string.location_time_zone_detection_auto_is_on;
        } else {
            // This is unexpected: getAvailabilityStatus() should ensure that the UI element isn't
            // even shown for known cases, or the capability is unknown.
            throw new IllegalStateException("Unexpected configureGeoDetectionEnabledCapability="
                    + configureGeoDetectionEnabledCapability);
        }
        return mContext.getString(summaryResId);
    }

    /**
     * Implementation of {@link TimeManager.TimeZoneDetectorListener#onChange()}. Called by the
     * system server after a change that affects {@link TimeZoneCapabilitiesAndConfig}.
     */
    @Override
    public void onChange() {
        refreshUi();
    }

    private void refreshUi() {
        // Force a refresh of cached user capabilities and config before refreshing the summary.
        getTimeZoneCapabilitiesAndConfig(/* forceRefresh= */ true);
        refreshSummary(mPreference);
    }

    /**
     * Returns the current user capabilities and configuration. {@code forceRefresh} can be {@code
     * true} to discard any cached copy.
     */
    private TimeZoneCapabilitiesAndConfig getTimeZoneCapabilitiesAndConfig(boolean forceRefresh) {
        if (forceRefresh || mTimeZoneCapabilitiesAndConfig == null) {
            mTimeZoneCapabilitiesAndConfig = mTimeManager.getTimeZoneCapabilitiesAndConfig();
        }
        return mTimeZoneCapabilitiesAndConfig;
    }
}
