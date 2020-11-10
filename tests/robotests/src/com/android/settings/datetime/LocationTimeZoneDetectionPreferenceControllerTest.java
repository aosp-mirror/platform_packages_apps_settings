/*
 * Copyright (C) 2018 The Android Open Source Project
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

import static android.app.time.TimeZoneCapabilities.CAPABILITY_NOT_APPLICABLE;
import static android.app.time.TimeZoneCapabilities.CAPABILITY_NOT_SUPPORTED;
import static android.app.time.TimeZoneCapabilities.CAPABILITY_POSSESSED;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.time.TimeManager;
import android.app.time.TimeZoneCapabilities;
import android.app.time.TimeZoneCapabilitiesAndConfig;
import android.app.time.TimeZoneConfiguration;
import android.content.Context;
import android.location.LocationManager;
import android.os.UserHandle;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getSystemService(TimeManager.class)).thenReturn(mTimeManager);
        when(mContext.getSystemService(LocationManager.class)).thenReturn(mLocationManager);
        mController = new LocationTimeZoneDetectionPreferenceController(mContext, "key");
    }

    @Test
    public void testLocationTimeZoneDetection_supported_shouldBeShown() {
        TimeZoneCapabilities capabilities =
                createTimeZoneCapabilities(/* geoDetectionSupported= */ true);
        TimeZoneConfiguration configuration = createTimeZoneConfig(/* geoDetectionEnabled= */ true);
        TimeZoneCapabilitiesAndConfig capabilitiesAndConfig =
                new TimeZoneCapabilitiesAndConfig(capabilities, configuration);
        when(mTimeManager.getTimeZoneCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void testLocationTimeZoneDetection_unsupported_shouldNotBeShown() {
        TimeZoneCapabilities capabilities =
                createTimeZoneCapabilities(/* geoDetectionSupported= */ false);
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
                createTimeZoneCapabilities(/* geoDetectionSupported= */ true);
        TimeZoneConfiguration configuration = createTimeZoneConfig(/* geoDetectionEnabled= */ true);
        TimeZoneCapabilitiesAndConfig capabilitiesAndConfig =
                new TimeZoneCapabilitiesAndConfig(capabilities, configuration);

        when(mTimeManager.getTimeZoneCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);
        assertThat(mController.getSummary()).isEqualTo(
                mContext.getString(R.string.location_time_zone_detection_on));
    }

    private static TimeZoneCapabilities createTimeZoneCapabilities(boolean geoDetectionSupported) {
        UserHandle arbitraryUserHandle = UserHandle.of(123);
        int geoDetectionCapability =
                geoDetectionSupported ? CAPABILITY_POSSESSED : CAPABILITY_NOT_SUPPORTED;
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
