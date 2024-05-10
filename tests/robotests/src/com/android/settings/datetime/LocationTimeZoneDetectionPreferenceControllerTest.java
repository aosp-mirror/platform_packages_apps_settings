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

import static android.app.time.Capabilities.CAPABILITY_NOT_APPLICABLE;
import static android.app.time.Capabilities.CAPABILITY_NOT_SUPPORTED;
import static android.app.time.Capabilities.CAPABILITY_POSSESSED;
import static android.app.time.DetectorStatusTypes.DETECTION_ALGORITHM_STATUS_NOT_SUPPORTED;
import static android.app.time.DetectorStatusTypes.DETECTION_ALGORITHM_STATUS_RUNNING;
import static android.app.time.DetectorStatusTypes.DETECTOR_STATUS_RUNNING;
import static android.app.time.LocationTimeZoneAlgorithmStatus.PROVIDER_STATUS_NOT_PRESENT;
import static android.app.time.LocationTimeZoneAlgorithmStatus.PROVIDER_STATUS_NOT_READY;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.time.Capabilities.CapabilityState;
import android.app.time.LocationTimeZoneAlgorithmStatus;
import android.app.time.TelephonyTimeZoneAlgorithmStatus;
import android.app.time.TimeManager;
import android.app.time.TimeZoneCapabilities;
import android.app.time.TimeZoneCapabilitiesAndConfig;
import android.app.time.TimeZoneConfiguration;
import android.app.time.TimeZoneDetectorStatus;
import android.content.Context;
import android.os.UserHandle;

import com.android.settings.R;
import com.android.settings.core.InstrumentedPreferenceFragment;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowFragment.class,
})
public class LocationTimeZoneDetectionPreferenceControllerTest {
    @Mock
    private TimeManager mTimeManager;
    private Context mContext;
    private LocationTimeZoneDetectionPreferenceController mController;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private InstrumentedPreferenceFragment mFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getSystemService(TimeManager.class)).thenReturn(mTimeManager);
        mController = new LocationTimeZoneDetectionPreferenceController(mContext);
        mController.setFragment(mFragment);
    }

    @Test
    public void setChecked_withTrue_shouldUpdateSetting_whenLocationIsEnabled() {
        boolean useLocationEnabled = true;
        TimeZoneCapabilitiesAndConfig capabilitiesAndConfig =
                createTimeZoneCapabilitiesAndConfig(useLocationEnabled, CAPABILITY_POSSESSED);
        when(mTimeManager.getTimeZoneCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);

        // Simulate the UI being clicked.
        mController.setChecked(true);

        // Verify the TimeManager was updated with the UI value.
        TimeZoneConfiguration expectedConfiguration = new TimeZoneConfiguration.Builder()
                .setGeoDetectionEnabled(true)
                .build();
        verify(mTimeManager).updateTimeZoneConfiguration(expectedConfiguration);
    }

    @Test
    public void isNotSliceable() {
        assertThat(mController.isSliceable()).isFalse();
    }

    @Test
    public void setChecked_withTrue_shouldDoNothing_whenLocationIsDisabled() {
        boolean useLocationEnabled = false;
        TimeZoneCapabilitiesAndConfig capabilitiesAndConfig =
                createTimeZoneCapabilitiesAndConfig(useLocationEnabled, CAPABILITY_POSSESSED);
        when(mTimeManager.getTimeZoneCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);

        // Simulate the UI being clicked.
        mController.setChecked(true);

        // Verify the TimeManager was not updated.
        verify(mTimeManager, never()).updateTimeZoneConfiguration(any());
    }

    @Test
    public void setChecked_withFalse_shouldUpdateSetting() {
        boolean useLocationEnabled = false;
        TimeZoneCapabilitiesAndConfig capabilitiesAndConfig =
                createTimeZoneCapabilitiesAndConfig(useLocationEnabled, CAPABILITY_POSSESSED);
        when(mTimeManager.getTimeZoneCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);

        // Simulate the UI being clicked.
        mController.setChecked(false);

        // Verify the TimeManager was updated with the UI value.
        TimeZoneConfiguration expectedConfiguration = new TimeZoneConfiguration.Builder()
                .setGeoDetectionEnabled(false)
                .build();
        verify(mTimeManager).updateTimeZoneConfiguration(expectedConfiguration);
    }

    @Test
    public void testLocationTimeZoneDetection_supported_shouldBeShown() {
        boolean useLocationEnabled = false;
        TimeZoneCapabilitiesAndConfig capabilitiesAndConfig =
                createTimeZoneCapabilitiesAndConfig(useLocationEnabled, CAPABILITY_POSSESSED);
        when(mTimeManager.getTimeZoneCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void testLocationTimeZoneDetection_unsupported_shouldNotBeShown() {
        boolean useLocationEnabled = false;
        TimeZoneCapabilitiesAndConfig capabilitiesAndConfig = createTimeZoneCapabilitiesAndConfig(
                useLocationEnabled, CAPABILITY_NOT_SUPPORTED);
        when(mTimeManager.getTimeZoneCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);

        assertThat(mController.isAvailable()).isFalse();
    }

    /**
     * Tests that the summary is set in just one of many cases. Exhaustive testing would be brittle.
     */
    @Test
    public void testLocationTimeZoneDetection_summary_geoDetectionEnabled() {
        boolean useLocationEnabled = false;
        TimeZoneCapabilitiesAndConfig capabilitiesAndConfig =
                createTimeZoneCapabilitiesAndConfig(useLocationEnabled, CAPABILITY_POSSESSED);

        when(mTimeManager.getTimeZoneCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);
        assertThat(mController.getSummary()).isEqualTo(
                mContext.getString(R.string.location_time_zone_detection_auto_is_on));
    }

    @Test
    public void testLocationTimeZoneDetection_toggleIsOn_whenGeoDetectionEnabledAnsMlsIsOff() {
        boolean useLocationEnabled = false;
        TimeZoneCapabilitiesAndConfig capabilitiesAndConfig = createTimeZoneCapabilitiesAndConfig(
                useLocationEnabled, CAPABILITY_NOT_APPLICABLE);

        when(mTimeManager.getTimeZoneCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);

        assertThat(mController.isChecked()).isTrue();
        assertThat(mController.getSummary()).isEqualTo(
                mContext.getString(R.string.location_app_permission_summary_location_off));
    }

    private static TimeZoneCapabilitiesAndConfig createTimeZoneCapabilitiesAndConfig(
            boolean useLocationEnabled,
            @CapabilityState int configureGeoDetectionEnabledCapability) {

        // Create a status that matches the user's capability state.
        LocationTimeZoneAlgorithmStatus locationAlgorithmStatus;
        switch (configureGeoDetectionEnabledCapability) {
            case CAPABILITY_NOT_SUPPORTED:
                locationAlgorithmStatus = new LocationTimeZoneAlgorithmStatus(
                        DETECTION_ALGORITHM_STATUS_NOT_SUPPORTED,
                        PROVIDER_STATUS_NOT_PRESENT, null, PROVIDER_STATUS_NOT_PRESENT, null);
                break;
            case CAPABILITY_NOT_APPLICABLE:
            case CAPABILITY_POSSESSED:
                locationAlgorithmStatus = new LocationTimeZoneAlgorithmStatus(
                        DETECTION_ALGORITHM_STATUS_RUNNING,
                        PROVIDER_STATUS_NOT_READY, null, PROVIDER_STATUS_NOT_READY, null);
                break;
            default:
                throw new AssertionError(
                        "Unsupported capability state: " + configureGeoDetectionEnabledCapability);
        }
        TimeZoneDetectorStatus status = new TimeZoneDetectorStatus(DETECTOR_STATUS_RUNNING,
                new TelephonyTimeZoneAlgorithmStatus(DETECTION_ALGORITHM_STATUS_RUNNING),
                locationAlgorithmStatus);

        UserHandle arbitraryUserHandle = UserHandle.of(123);
        TimeZoneCapabilities capabilities = new TimeZoneCapabilities.Builder(arbitraryUserHandle)
                .setConfigureAutoDetectionEnabledCapability(CAPABILITY_POSSESSED)
                .setUseLocationEnabled(useLocationEnabled)
                .setConfigureGeoDetectionEnabledCapability(configureGeoDetectionEnabledCapability)
                .setSetManualTimeZoneCapability(CAPABILITY_NOT_APPLICABLE)
                .build();

        TimeZoneConfiguration configuration = new TimeZoneConfiguration.Builder()
                .setAutoDetectionEnabled(true)
                .setGeoDetectionEnabled(true)
                .build();

        return new TimeZoneCapabilitiesAndConfig(status, capabilities, configuration);
    }
}
