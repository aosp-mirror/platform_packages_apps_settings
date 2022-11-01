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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.time.Capabilities;
import android.app.time.TimeManager;
import android.app.time.TimeZoneCapabilities;
import android.app.time.TimeZoneCapabilitiesAndConfig;
import android.app.time.TimeZoneConfiguration;
import android.content.Context;
import android.os.UserHandle;

import com.android.settingslib.RestrictedPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class TimeZonePreferenceControllerTest {

    @Mock
    private TimeManager mTimeManager;
    private Context mContext;
    private TimeZonePreferenceController mController;
    private RestrictedPreference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        doReturn(mTimeManager).when(mContext).getSystemService(TimeManager.class);

        mPreference = new RestrictedPreference(mContext);

        mController = spy(new TimeZonePreferenceController(mContext));
        doReturn("test timezone").when(mController).getTimeZoneOffsetAndName();
    }

    @Test
    public void isAlwaysAvailable() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void updateState_suggestManualNotAllowed_shouldDisablePref() {
        // Make sure not disabled by admin.
        mPreference.setDisabledByAdmin(null);

        TimeZoneCapabilitiesAndConfig capabilitiesAndConfig = createCapabilitiesAndConfig(
            /* suggestManualAllowed= */false);
        when(mTimeManager.getTimeZoneCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);

        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isFalse();
    }

    @Test
    public void updateState_suggestManualAllowed_shouldEnablePref() {
        // Make sure not disabled by admin.
        mPreference.setDisabledByAdmin(null);

        TimeZoneCapabilitiesAndConfig capabilitiesAndConfig = createCapabilitiesAndConfig(
            /* suggestManualAllowed= */true);
        when(mTimeManager.getTimeZoneCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);

        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isTrue();
    }

    private static TimeZoneCapabilitiesAndConfig createCapabilitiesAndConfig(
            boolean suggestManualAllowed) {
        int suggestManualCapability = suggestManualAllowed ? Capabilities.CAPABILITY_POSSESSED
                : Capabilities.CAPABILITY_NOT_SUPPORTED;
        TimeZoneCapabilities capabilities = new TimeZoneCapabilities.Builder(UserHandle.SYSTEM)
                .setConfigureAutoDetectionEnabledCapability(Capabilities.CAPABILITY_POSSESSED)
                .setConfigureGeoDetectionEnabledCapability(Capabilities.CAPABILITY_NOT_SUPPORTED)
                .setSuggestManualTimeZoneCapability(suggestManualCapability)
                .build();
        TimeZoneConfiguration config = new TimeZoneConfiguration.Builder()
                .setAutoDetectionEnabled(!suggestManualAllowed)
                .setGeoDetectionEnabled(false)
                .build();
        return new TimeZoneCapabilitiesAndConfig(capabilities, config);
    }
}
