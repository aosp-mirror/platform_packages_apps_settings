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
package com.android.settings.datetime;

import android.app.time.DetectorStatusTypes;
import android.app.time.LocationTimeZoneAlgorithmStatus;
import android.app.time.TelephonyTimeZoneAlgorithmStatus;
import android.app.time.TimeManager;
import android.app.time.TimeZoneDetectorStatus;
import android.content.Context;
import android.location.LocationManager;
import android.service.timezone.TimeZoneProviderStatus;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.location.LocationSettings;
import com.android.settingslib.widget.BannerMessagePreference;

import java.util.concurrent.Executor;

/**
 * The controller for the "location time zone detection" entry in the Location settings
 * screen.
 */
public class LocationProviderStatusPreferenceController
        extends BasePreferenceController implements TimeManager.TimeZoneDetectorListener {
    private final TimeManager mTimeManager;
    private final LocationManager mLocationManager;

    private BannerMessagePreference mPreference = null;

    public LocationProviderStatusPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mTimeManager = context.getSystemService(TimeManager.class);
        mLocationManager = context.getSystemService(LocationManager.class);

        Executor mainExecutor = context.getMainExecutor();
        mTimeManager.addTimeZoneDetectorListener(mainExecutor, this);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        assert mPreference != null;
        mPreference
                .setPositiveButtonText(
                        R.string.location_time_zone_provider_fix_dialog_ok_button)
                .setPositiveButtonOnClickListener(v -> launchLocationSettings());
    }

    @Override
    public int getAvailabilityStatus() {
        // Checks that the summary is non-empty as most status strings are optional. If a status
        // string is empty, we ignore the status.
        if (!TextUtils.isEmpty(getSummary())) {
            return AVAILABLE_UNSEARCHABLE;
        }
        return CONDITIONALLY_UNAVAILABLE;
    }

    private void launchLocationSettings() {
        new SubSettingLauncher(mContext)
                .setDestination(LocationSettings.class.getName())
                .setSourceMetricsCategory(getMetricsCategory())
                .launch();
    }

    // Android has up to two location time zone providers (LTZPs) which can
    // (optionally) report their status along several dimensions. Typically there is
    // only one LTZP on a device, the primary. The UI here only reports status for one
    // LTZP. This UI logic prioritizes the primary if there is a "bad" status for both.
    @Nullable
    private TimeZoneProviderStatus getLtzpStatus() {
        LocationTimeZoneAlgorithmStatus status =
                mTimeManager.getTimeZoneCapabilitiesAndConfig().getDetectorStatus()
                        .getLocationTimeZoneAlgorithmStatus();
        TimeZoneProviderStatus primary = status.getPrimaryProviderReportedStatus();
        TimeZoneProviderStatus secondary = status.getSecondaryProviderReportedStatus();
        if (primary == null && secondary == null) {
            return null;
        }

        if (primary == null) {
            return secondary;
        } else if (secondary == null) {
            return primary;
        }

        if (status.getPrimaryProviderStatus()
                != LocationTimeZoneAlgorithmStatus.PROVIDER_STATUS_IS_CERTAIN) {
            return secondary;
        }

        return primary;
    }

    @Override
    public void onChange() {
        if (mPreference != null) {
            mPreference.setVisible(getAvailabilityStatus() == AVAILABLE_UNSEARCHABLE);
            refreshSummary(mPreference);
        }
    }

    @Override
    public CharSequence getSummary() {
        boolean locationEnabled = mLocationManager.isLocationEnabled();
        final TimeZoneDetectorStatus detectorStatus =
                mTimeManager.getTimeZoneCapabilitiesAndConfig().getDetectorStatus();

        if (!locationEnabled && hasLocationTimeZoneNoTelephonyFallback(detectorStatus)) {
            return mContext.getResources().getString(
                    R.string.location_time_zone_detection_status_summary_blocked_by_settings);
        }

        TimeZoneProviderStatus ltzpStatus = getLtzpStatus();
        if (ltzpStatus == null) {
            return "";
        }

        int status = ltzpStatus.getLocationDetectionDependencyStatus();

        if (status == TimeZoneProviderStatus.DEPENDENCY_STATUS_BLOCKED_BY_ENVIRONMENT) {
            return mContext.getResources().getString(
                    R.string.location_time_zone_detection_status_summary_blocked_by_environment);
        }
        if (status == TimeZoneProviderStatus.DEPENDENCY_STATUS_DEGRADED_BY_SETTINGS) {
            return mContext.getResources().getString(
                    R.string.location_time_zone_detection_status_summary_degraded_by_settings);
        }
        if (status == TimeZoneProviderStatus.DEPENDENCY_STATUS_TEMPORARILY_UNAVAILABLE) {
            return mContext.getResources().getString(
                    R.string.location_time_zone_detection_status_summary_temporarily_unavailable);
        }
        if (status == TimeZoneProviderStatus.DEPENDENCY_STATUS_BLOCKED_BY_SETTINGS) {
            return mContext.getResources().getString(
                    R.string.location_time_zone_detection_status_summary_blocked_by_settings);
        }

        return "";
    }

    /** package */
    static boolean hasLocationTimeZoneNoTelephonyFallback(TimeZoneDetectorStatus detectorStatus) {
        final LocationTimeZoneAlgorithmStatus locationStatus =
                detectorStatus.getLocationTimeZoneAlgorithmStatus();
        final TelephonyTimeZoneAlgorithmStatus telephonyStatus =
                detectorStatus.getTelephonyTimeZoneAlgorithmStatus();
        return telephonyStatus.getAlgorithmStatus()
                == DetectorStatusTypes.DETECTION_ALGORITHM_STATUS_NOT_SUPPORTED
                && locationStatus.getStatus()
                != DetectorStatusTypes.DETECTION_ALGORITHM_STATUS_NOT_SUPPORTED;
    }
}
