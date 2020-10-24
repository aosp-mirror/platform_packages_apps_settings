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

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.time.TimeManager;
import android.app.time.TimeZoneCapabilities;
import android.app.time.TimeZoneCapabilitiesAndConfig;
import android.app.time.TimeZoneConfiguration;
import android.content.ContentResolver;
import android.content.Context;
import android.os.UserHandle;

import androidx.preference.SwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class TimeZoneDetectionPreferenceControllerTest {

    @Mock
    private SwitchPreference mPreference;
    @Mock
    private TimeManager mTimeManager;

    private Context mContext;
    private ContentResolver mContentResolver;
    private TimeZoneDetectionPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContentResolver = RuntimeEnvironment.application.getContentResolver();
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getSystemService(TimeManager.class)).thenReturn(mTimeManager);

        mController = new TimeZoneDetectionPreferenceController(mContext);
        when(mPreference.getKey()).thenReturn(mController.getPreferenceKey());
    }

    @Test
    public void updateState_locationDetectionEnabled_shouldCheckPreference() {
        TimeZoneCapabilitiesAndConfig capabilitiesAndConfig =
                createTimeZoneCapabilitiesAndConfig(/* geoDetectionEnabled= */ true);
        when(mTimeManager.getTimeZoneCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);

        mController.updateState(mPreference);

        verify(mPreference).setChecked(true);
    }

    @Test
    public void updateState_locationDetectionDisabled_shouldUncheckPreference() {
        TimeZoneCapabilitiesAndConfig capabilitiesAndConfig =
                createTimeZoneCapabilitiesAndConfig(/* geoDetectionEnabled= */ false);
        when(mTimeManager.getTimeZoneCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);

        mController.updateState(mPreference);

        verify(mPreference).setChecked(false);
    }

    @Test
    public void handlePreferenceTreeClick_unchecked_shouldDisableGeoDetection() {
        // getTimeZoneCapabilitiesAndConfig() is called after updateTimeZoneConfiguration() to
        // obtain the new state.
        boolean postUpdateResponseValue = false;
        TimeZoneCapabilitiesAndConfig capabilitiesAndConfig = createTimeZoneCapabilitiesAndConfig(
                /* geoDetectionEnabled= */postUpdateResponseValue);
        when(mTimeManager.getTimeZoneCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);

        // Simulate the UI being clicked.
        boolean preferenceCheckedState = false;
        when(mPreference.isChecked()).thenReturn(preferenceCheckedState);
        mController.handlePreferenceTreeClick(mPreference);

        // Verify the TimeManager was updated with the UI value.
        TimeZoneConfiguration expectedConfiguration = new TimeZoneConfiguration.Builder()
                .setGeoDetectionEnabled(preferenceCheckedState)
                .build();
        verify(mTimeManager).updateTimeZoneConfiguration(expectedConfiguration);

        // Confirm the UI state was reset using the getTimeZoneCapabilitiesAndConfig() response.
        verify(mPreference).setChecked(postUpdateResponseValue);
    }

    @Test
    public void handlePreferenceTreeClick_checked_shouldEnableGeoDetection() {
        // getTimeZoneCapabilitiesAndConfig() is called after updateTimeZoneConfiguration() to
        // obtain the new state.
        boolean postUpdateResponseValue = true;
        TimeZoneCapabilitiesAndConfig capabilitiesAndConfig = createTimeZoneCapabilitiesAndConfig(
                /* geoDetectionEnabled= */ postUpdateResponseValue);
        when(mTimeManager.getTimeZoneCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);

        // Simulate the UI being clicked.
        boolean preferenceCheckedState = true;
        when(mPreference.isChecked()).thenReturn(preferenceCheckedState);
        mController.handlePreferenceTreeClick(mPreference);

        // Verify the TimeManager was updated with the UI value.
        TimeZoneConfiguration expectedConfiguration = new TimeZoneConfiguration.Builder()
                .setGeoDetectionEnabled(preferenceCheckedState)
                .build();
        verify(mTimeManager).updateTimeZoneConfiguration(expectedConfiguration);

        // Confirm the UI state was reset using the getTimeZoneCapabilitiesAndConfig() response.
        verify(mPreference).setChecked(postUpdateResponseValue);
    }

    @Test
    public void handlePreferenceTreeClick_checked_shouldEnableGeoDetection_updateRefused() {
        // getTimeZoneCapabilitiesAndConfig() is called after updateTimeZoneConfiguration() to
        // obtain the new state.
        boolean postUpdateResponseValue = false;
        TimeZoneCapabilitiesAndConfig capabilitiesAndConfig = createTimeZoneCapabilitiesAndConfig(
                /* geoDetectionEnabled= */ postUpdateResponseValue);
        when(mTimeManager.getTimeZoneCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);

        // Simulate the UI being clicked.
        boolean preferenceCheckedState = true;
        when(mPreference.isChecked()).thenReturn(preferenceCheckedState);
        mController.handlePreferenceTreeClick(mPreference);

        // Verify the TimeManager was updated with the UI value.
        TimeZoneConfiguration expectedConfiguration = new TimeZoneConfiguration.Builder()
                .setGeoDetectionEnabled(preferenceCheckedState)
                .build();
        verify(mTimeManager).updateTimeZoneConfiguration(expectedConfiguration);

        // Confirm the UI state was reset using the getTimeZoneCapabilitiesAndConfig() response.
        verify(mPreference).setChecked(postUpdateResponseValue);
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
