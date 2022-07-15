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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import android.app.time.Capabilities;
import android.app.time.TimeManager;
import android.app.time.TimeZoneCapabilities;
import android.app.time.TimeZoneCapabilitiesAndConfig;
import android.app.time.TimeZoneConfiguration;
import android.content.Context;
import android.location.LocationManager;
import android.os.UserHandle;

import com.android.settings.R;
import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class LocationTimeZoneDetectionPreferenceControllerTest {
    @Mock
    private TimeManager mTimeManager;
    @Mock
    private LocationManager mLocationManager;
    private Context mContext;
    private LocationTimeZoneDetectionPreferenceController mController;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private InstrumentedPreferenceFragment mFragment;
    @Mock
    private Lifecycle mLifecycle;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getSystemService(TimeManager.class)).thenReturn(mTimeManager);
        when(mContext.getSystemService(LocationManager.class)).thenReturn(mLocationManager);
        mController = new LocationTimeZoneDetectionPreferenceController(mContext);
        mController.setFragment(mFragment);
    }

    @Test
    public void setChecked_withTrue_shouldUpdateSetting_whenLocationIsEnabled() {
        when(mLocationManager.isLocationEnabled()).thenReturn(true);

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
        when(mLocationManager.isLocationEnabled()).thenReturn(false);

        // Simulate the UI being clicked.
        mController.setChecked(true);

        // Verify the TimeManager was not called.
        verifyNoInteractions(mTimeManager);
    }

    @Test
    public void setChecked_withFalse_shouldUpdateSetting() {
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
        TimeZoneCapabilities capabilities =
                createTimeZoneCapabilities(CAPABILITY_POSSESSED);
        TimeZoneConfiguration configuration = createTimeZoneConfig(/* geoDetectionEnabled= */ true);
        TimeZoneCapabilitiesAndConfig capabilitiesAndConfig =
                new TimeZoneCapabilitiesAndConfig(capabilities, configuration);
        when(mTimeManager.getTimeZoneCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void testLocationTimeZoneDetection_unsupported_shouldNotBeShown() {
        TimeZoneCapabilities capabilities =
                createTimeZoneCapabilities(CAPABILITY_NOT_SUPPORTED);
        TimeZoneConfiguration configuration = createTimeZoneConfig(/* geoDetectionEnabled= */ true);
        TimeZoneCapabilitiesAndConfig capabilitiesAndConfig =
                new TimeZoneCapabilitiesAndConfig(capabilities, configuration);
        when(mTimeManager.getTimeZoneCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);

        assertThat(mController.isAvailable()).isFalse();
    }

    /**
     * Tests that the summary is set in just one of many cases. Exhaustive testing would be brittle.
     */
    @Test
    public void testLocationTimeZoneDetection_summary_geoDetectionEnabled() {
        TimeZoneCapabilities capabilities =
                createTimeZoneCapabilities(CAPABILITY_POSSESSED);
        TimeZoneConfiguration configuration = createTimeZoneConfig(/* geoDetectionEnabled= */ true);
        TimeZoneCapabilitiesAndConfig capabilitiesAndConfig =
                new TimeZoneCapabilitiesAndConfig(capabilities, configuration);

        when(mTimeManager.getTimeZoneCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);
        assertThat(mController.getSummary().toString()).isEmpty();
    }

    @Test
    public void testLocationTimeZoneDetection_toggleIsOn_whenGeoDetectionEnabledAnsMlsIsOff() {
        TimeZoneCapabilities capabilities =
                createTimeZoneCapabilities(CAPABILITY_NOT_APPLICABLE);
        TimeZoneConfiguration configuration = createTimeZoneConfig(/* geoDetectionEnabled= */ true);
        TimeZoneCapabilitiesAndConfig capabilitiesAndConfig =
                new TimeZoneCapabilitiesAndConfig(capabilities, configuration);

        when(mTimeManager.getTimeZoneCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);
        when(mLocationManager.isLocationEnabled()).thenReturn(false);

        assertThat(mController.isChecked()).isTrue();
        assertThat(mController.getSummary()).isEqualTo(
                mContext.getString(R.string.location_app_permission_summary_location_off));
    }

    private static TimeZoneCapabilities createTimeZoneCapabilities(
            @Capabilities.CapabilityState int geoDetectionCapability) {
        UserHandle arbitraryUserHandle = UserHandle.of(123);
        return new TimeZoneCapabilities.Builder(arbitraryUserHandle)
                .setConfigureAutoDetectionEnabledCapability(CAPABILITY_POSSESSED)
                .setConfigureGeoDetectionEnabledCapability(geoDetectionCapability)
                .setSuggestManualTimeZoneCapability(CAPABILITY_NOT_APPLICABLE)
                .build();
    }

    private static TimeZoneConfiguration createTimeZoneConfig(boolean geoDetectionEnabled) {
        return new TimeZoneConfiguration.Builder()
                .setAutoDetectionEnabled(true)
                .setGeoDetectionEnabled(geoDetectionEnabled)
                .build();
    }
}
