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

import static android.service.timezone.TimeZoneProviderStatus.DEPENDENCY_STATUS_OK;

import android.app.time.DetectorStatusTypes;
import android.app.time.LocationTimeZoneAlgorithmStatus;
import android.app.time.TelephonyTimeZoneAlgorithmStatus;
import android.app.time.TimeManager;
import android.app.time.TimeZoneCapabilities;
import android.app.time.TimeZoneCapabilitiesAndConfig;
import android.app.time.TimeZoneDetectorStatus;
import android.content.Context;
import android.service.timezone.TimeZoneProviderStatus;
import android.service.timezone.TimeZoneProviderStatus.DependencyStatus;
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

    private BannerMessagePreference mPreference = null;

    public LocationProviderStatusPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mTimeManager = context.getSystemService(TimeManager.class);

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
    private TimeZoneProviderStatus getLtzpStatusToReport() {
        LocationTimeZoneAlgorithmStatus status =
                mTimeManager.getTimeZoneCapabilitiesAndConfig().getDetectorStatus()
                        .getLocationTimeZoneAlgorithmStatus();
        @Nullable TimeZoneProviderStatus primary = status.getPrimaryProviderReportedStatus();
        @Nullable TimeZoneProviderStatus secondary = status.getSecondaryProviderReportedStatus();
        if (primary != null && secondary != null) {
            return pickWorstLtzpStatus(primary, secondary);
        } else if (primary != null) {
            return primary;
        } else {
            return secondary;
        }
    }

    private static TimeZoneProviderStatus pickWorstLtzpStatus(
            TimeZoneProviderStatus primary, TimeZoneProviderStatus secondary) {
        int primaryScore = scoreLtzpStatus(primary);
        int secondaryScore = scoreLtzpStatus(secondary);
        return primaryScore >= secondaryScore ? primary : secondary;
    }

    private static int scoreLtzpStatus(TimeZoneProviderStatus providerStatus) {
        @DependencyStatus int locationStatus =
                providerStatus.getLocationDetectionDependencyStatus();
        if (locationStatus <= DEPENDENCY_STATUS_OK) {
            return 0;
        }
        // The enum values currently correspond well to severity.
        return providerStatus.getLocationDetectionDependencyStatus();
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
        final TimeZoneCapabilitiesAndConfig timeZoneCapabilitiesAndConfig =
                mTimeManager.getTimeZoneCapabilitiesAndConfig();
        final TimeZoneDetectorStatus detectorStatus =
                timeZoneCapabilitiesAndConfig.getDetectorStatus();
        final TimeZoneCapabilities timeZoneCapabilities =
                timeZoneCapabilitiesAndConfig.getCapabilities();

        if (!timeZoneCapabilities.isUseLocationEnabled()
                && hasLocationTimeZoneNoTelephonyFallback(detectorStatus)) {
            return mContext.getString(
                    R.string.location_time_zone_detection_status_summary_blocked_by_settings);
        }

        TimeZoneProviderStatus ltzpStatus = getLtzpStatusToReport();
        if (ltzpStatus == null) {
            return "";
        }

        @DependencyStatus int locationStatus = ltzpStatus.getLocationDetectionDependencyStatus();

        if (locationStatus == TimeZoneProviderStatus.DEPENDENCY_STATUS_BLOCKED_BY_SETTINGS) {
            return mContext.getString(
                    R.string.location_time_zone_detection_status_summary_blocked_by_settings);
        }
        if (locationStatus == TimeZoneProviderStatus.DEPENDENCY_STATUS_DEGRADED_BY_SETTINGS) {
            return mContext.getString(
                    R.string.location_time_zone_detection_status_summary_degraded_by_settings);
        }
        if (locationStatus == TimeZoneProviderStatus.DEPENDENCY_STATUS_BLOCKED_BY_ENVIRONMENT) {
            return mContext.getString(
                    R.string.location_time_zone_detection_status_summary_blocked_by_environment);
        }
        if (locationStatus == TimeZoneProviderStatus.DEPENDENCY_STATUS_TEMPORARILY_UNAVAILABLE) {
            return mContext.getString(
                    R.string.location_time_zone_detection_status_summary_temporarily_unavailable);
        }

        // LTZP-reported network connectivity and time zone resolution statuses are currently
        // ignored. Partners can tweak this logic if they also want to report these to users.

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
