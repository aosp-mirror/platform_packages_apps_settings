/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.settings.location;

import static android.app.time.TimeZoneCapabilities.CAPABILITY_NOT_APPLICABLE;
import static android.app.time.TimeZoneCapabilities.CAPABILITY_POSSESSED;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.time.TimeManager;
import android.app.time.TimeZoneCapabilities;
import android.app.time.TimeZoneCapabilitiesAndConfig;
import android.app.time.TimeZoneConfiguration;
import android.content.Context;
import android.os.UserHandle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class TimeZoneDetectionTogglePreferenceControllerTest {

    private static final String PREF_KEY = "test_key";

    @Mock
    private Context mContext;

    @Mock
    private TimeManager mTimeManager;
    private TimeZoneDetectionTogglePreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getSystemService(TimeManager.class)).thenReturn(mTimeManager);

        mController = new TimeZoneDetectionTogglePreferenceController(mContext, PREF_KEY);
    }

    @Test
    public void isAvailable_shouldReturnTrue() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isChecked_whenEnabled_shouldReturnTrue() {
        when(mTimeManager.getTimeZoneCapabilitiesAndConfig())
                .thenReturn(createTimeZoneCapabilitiesAndConfig(true));

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void isChecked_whenDisabled_shouldReturnFalse() {
        when(mTimeManager.getTimeZoneCapabilitiesAndConfig())
                .thenReturn(createTimeZoneCapabilitiesAndConfig(false));

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void setChecked_withTrue_shouldUpdateSetting() {
        // Simulate the UI being clicked.
        mController.setChecked(true);

        // Verify the TimeManager was updated with the UI value.
        TimeZoneConfiguration expectedConfiguration = new TimeZoneConfiguration.Builder()
                .setGeoDetectionEnabled(true)
                .build();
        verify(mTimeManager).updateTimeZoneConfiguration(expectedConfiguration);
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

    private static TimeZoneCapabilitiesAndConfig createTimeZoneCapabilitiesAndConfig(
            boolean geoDetectionEnabled) {
        UserHandle arbitraryUserHandle = UserHandle.of(123);
        TimeZoneCapabilities capabilities = new TimeZoneCapabilities.Builder(arbitraryUserHandle)
                .setConfigureAutoDetectionEnabledCapability(CAPABILITY_POSSESSED)
                .setConfigureGeoDetectionEnabledCapability(CAPABILITY_POSSESSED)
                .setSuggestManualTimeZoneCapability(CAPABILITY_NOT_APPLICABLE)
                .build();
        TimeZoneConfiguration configuration = new TimeZoneConfiguration.Builder()
                .setAutoDetectionEnabled(true)
                .setGeoDetectionEnabled(geoDetectionEnabled)
                .build();
        return new TimeZoneCapabilitiesAndConfig(capabilities, configuration);
    }
}
