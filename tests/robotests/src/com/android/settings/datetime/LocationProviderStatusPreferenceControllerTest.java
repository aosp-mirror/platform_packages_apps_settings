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

import static android.app.time.DetectorStatusTypes.DETECTION_ALGORITHM_STATUS_RUNNING;
import static android.app.time.DetectorStatusTypes.DETECTOR_STATUS_RUNNING;
import static android.app.time.LocationTimeZoneAlgorithmStatus.PROVIDER_STATUS_IS_CERTAIN;
import static android.app.time.LocationTimeZoneAlgorithmStatus.PROVIDER_STATUS_IS_UNCERTAIN;
import static android.service.timezone.TimeZoneProviderStatus.DEPENDENCY_STATUS_BLOCKED_BY_SETTINGS;
import static android.service.timezone.TimeZoneProviderStatus.DEPENDENCY_STATUS_OK;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.time.Capabilities;
import android.app.time.LocationTimeZoneAlgorithmStatus;
import android.app.time.TelephonyTimeZoneAlgorithmStatus;
import android.app.time.TimeManager;
import android.app.time.TimeZoneCapabilities;
import android.app.time.TimeZoneCapabilitiesAndConfig;
import android.app.time.TimeZoneConfiguration;
import android.app.time.TimeZoneDetectorStatus;
import android.content.Context;
import android.location.LocationManager;
import android.os.UserHandle;
import android.service.timezone.TimeZoneProviderStatus;

import androidx.annotation.Nullable;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class LocationProviderStatusPreferenceControllerTest {

    private Context mContext;
    @Mock
    private TimeManager mTimeManager;
    @Mock
    private LocationManager mLocationManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);

        when(mContext.getSystemService(TimeManager.class)).thenReturn(mTimeManager);
        when(mContext.getSystemService(LocationManager.class)).thenReturn(mLocationManager);
        when(mLocationManager.isLocationEnabled()).thenReturn(true);
        when(mContext.getString(
                R.string.location_time_zone_detection_status_summary_blocked_by_settings))
                .thenReturn("BBS");
    }

    @Test
    public void testCapabilityStatus() {
        TimeZoneCapabilitiesAndConfig capabilitiesAndConfig = createCapabilitiesAndConfig(false,
                DEPENDENCY_STATUS_OK, DEPENDENCY_STATUS_OK);
        when(mTimeManager.getTimeZoneCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);
        LocationProviderStatusPreferenceController controller =
                new LocationProviderStatusPreferenceController(mContext, "LPSPC");

        assertThat(controller.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.CONDITIONALLY_UNAVAILABLE);

        capabilitiesAndConfig = createCapabilitiesAndConfig(true, DEPENDENCY_STATUS_OK,
                DEPENDENCY_STATUS_OK);
        when(mTimeManager.getTimeZoneCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);

        assertThat(controller.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.CONDITIONALLY_UNAVAILABLE);

        capabilitiesAndConfig = createCapabilitiesAndConfig(false,
                DEPENDENCY_STATUS_BLOCKED_BY_SETTINGS, DEPENDENCY_STATUS_OK);
        when(mTimeManager.getTimeZoneCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);

        assertThat(controller.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.AVAILABLE_UNSEARCHABLE);

        capabilitiesAndConfig = createCapabilitiesAndConfig(true,
                DEPENDENCY_STATUS_BLOCKED_BY_SETTINGS, DEPENDENCY_STATUS_OK);
        when(mTimeManager.getTimeZoneCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);

        assertThat(controller.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.AVAILABLE_UNSEARCHABLE);
    }

    @Test
    public void testProviderStatus_primaryCertain() {
        TimeZoneCapabilitiesAndConfig capabilitiesAndConfig = createCapabilitiesAndConfig(false,
                DEPENDENCY_STATUS_OK, DEPENDENCY_STATUS_OK);
        when(mTimeManager.getTimeZoneCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);
        LocationProviderStatusPreferenceController controller =
                new LocationProviderStatusPreferenceController(mContext, "LPSPC");

        assertThat(controller.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.CONDITIONALLY_UNAVAILABLE);

        capabilitiesAndConfig = createCapabilitiesAndConfig(false,
                DEPENDENCY_STATUS_BLOCKED_BY_SETTINGS, DEPENDENCY_STATUS_OK);
        when(mTimeManager.getTimeZoneCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);

        assertThat(controller.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.AVAILABLE_UNSEARCHABLE);

        capabilitiesAndConfig = createCapabilitiesAndConfig(false, DEPENDENCY_STATUS_OK,
                DEPENDENCY_STATUS_BLOCKED_BY_SETTINGS);
        when(mTimeManager.getTimeZoneCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);

        assertThat(controller.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.CONDITIONALLY_UNAVAILABLE);

        capabilitiesAndConfig = createCapabilitiesAndConfig(false, DEPENDENCY_STATUS_OK,
                DEPENDENCY_STATUS_BLOCKED_BY_SETTINGS);
        when(mTimeManager.getTimeZoneCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);

        assertThat(controller.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void testProviderStatus_primaryUncertain() {
        TimeZoneCapabilitiesAndConfig capabilitiesAndConfig = createCapabilitiesAndConfig(false,
                DEPENDENCY_STATUS_OK, DEPENDENCY_STATUS_OK, PROVIDER_STATUS_IS_CERTAIN);
        when(mTimeManager.getTimeZoneCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);
        LocationProviderStatusPreferenceController controller =
                new LocationProviderStatusPreferenceController(mContext, "LPSPC");

        assertThat(controller.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.CONDITIONALLY_UNAVAILABLE);

        capabilitiesAndConfig = createCapabilitiesAndConfig(false,
                DEPENDENCY_STATUS_BLOCKED_BY_SETTINGS, DEPENDENCY_STATUS_OK,
                PROVIDER_STATUS_IS_CERTAIN);
        when(mTimeManager.getTimeZoneCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);

        assertThat(controller.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.AVAILABLE_UNSEARCHABLE);

        capabilitiesAndConfig = createCapabilitiesAndConfig(false, DEPENDENCY_STATUS_OK,
                DEPENDENCY_STATUS_BLOCKED_BY_SETTINGS, PROVIDER_STATUS_IS_UNCERTAIN);
        when(mTimeManager.getTimeZoneCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);

        assertThat(controller.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.AVAILABLE_UNSEARCHABLE);

        capabilitiesAndConfig = createCapabilitiesAndConfig(false, DEPENDENCY_STATUS_OK,
                DEPENDENCY_STATUS_OK, PROVIDER_STATUS_IS_UNCERTAIN);
        when(mTimeManager.getTimeZoneCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);

        assertThat(controller.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void testProviderStatus_nullProviders() {
        TimeZoneCapabilitiesAndConfig capabilitiesAndConfig = createCapabilitiesAndConfig(false,
                null, null);
        when(mTimeManager.getTimeZoneCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);
        LocationProviderStatusPreferenceController controller =
                new LocationProviderStatusPreferenceController(mContext, "LPSPC");

        assertThat(controller.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.CONDITIONALLY_UNAVAILABLE);

        capabilitiesAndConfig = createCapabilitiesAndConfig(false,
                DEPENDENCY_STATUS_BLOCKED_BY_SETTINGS, null);
        when(mTimeManager.getTimeZoneCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);

        assertThat(controller.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.AVAILABLE_UNSEARCHABLE);

        capabilitiesAndConfig = createCapabilitiesAndConfig(false, DEPENDENCY_STATUS_OK, null);
        when(mTimeManager.getTimeZoneCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);

        assertThat(controller.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.CONDITIONALLY_UNAVAILABLE);

        capabilitiesAndConfig = createCapabilitiesAndConfig(false, null,
                DEPENDENCY_STATUS_BLOCKED_BY_SETTINGS);
        when(mTimeManager.getTimeZoneCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);

        assertThat(controller.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.AVAILABLE_UNSEARCHABLE);
    }

    private TimeZoneCapabilitiesAndConfig createCapabilitiesAndConfig(boolean capabilitySupported,
            @Nullable Integer primary, @Nullable Integer secondary) {
        return createCapabilitiesAndConfig(capabilitySupported, primary, secondary,
                PROVIDER_STATUS_IS_CERTAIN);
    }

    private TimeZoneCapabilitiesAndConfig createCapabilitiesAndConfig(boolean capabilitySupported,
            @Nullable Integer primary, @Nullable Integer secondary, int primaryProviderStatus) {
        TimeZoneDetectorStatus status = new TimeZoneDetectorStatus(DETECTOR_STATUS_RUNNING,
                new TelephonyTimeZoneAlgorithmStatus(DETECTION_ALGORITHM_STATUS_RUNNING),
                new LocationTimeZoneAlgorithmStatus(DETECTION_ALGORITHM_STATUS_RUNNING,
                        primaryProviderStatus, primary != null
                        ? new TimeZoneProviderStatus.Builder().setLocationDetectionDependencyStatus(
                        primary).build() : null, PROVIDER_STATUS_IS_CERTAIN, secondary != null
                        ? new TimeZoneProviderStatus.Builder().setLocationDetectionDependencyStatus(
                        secondary).build() : null));

        TimeZoneCapabilities capabilities = new TimeZoneCapabilities.Builder(
                UserHandle.SYSTEM).setConfigureAutoDetectionEnabledCapability(
                Capabilities.CAPABILITY_POSSESSED).setConfigureGeoDetectionEnabledCapability(
                capabilitySupported ? Capabilities.CAPABILITY_POSSESSED
                        : Capabilities.CAPABILITY_NOT_SUPPORTED).setSetManualTimeZoneCapability(
                Capabilities.CAPABILITY_POSSESSED).build();

        return new TimeZoneCapabilitiesAndConfig(status, capabilities,
                new TimeZoneConfiguration.Builder().build());
    }
}
