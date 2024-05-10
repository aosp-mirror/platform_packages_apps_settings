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

import static com.android.settings.core.BasePreferenceController.DISABLED_DEPENDENT_SETTING;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.time.Capabilities;
import android.app.time.TimeCapabilities;
import android.app.time.TimeCapabilitiesAndConfig;
import android.app.time.TimeConfiguration;
import android.app.time.TimeManager;
import android.content.Context;
import android.os.UserHandle;

import androidx.preference.Preference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class AutoTimePreferenceControllerTest {

    @Mock
    private UpdateTimeAndDateCallback mCallback;
    private Context mContext;
    private AutoTimePreferenceController mController;
    private Preference mPreference;
    @Mock
    private TimeManager mTimeManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mPreference = new Preference(mContext);
        when(mContext.getSystemService(TimeManager.class)).thenReturn(mTimeManager);

        mController = new AutoTimePreferenceController(mContext, "test_key");
        mController.setDateAndTimeCallback(mCallback);
    }

    @Test
    public void autoTimeNotSupported_notAvailable() {
        TimeCapabilitiesAndConfig capabilitiesAndConfig = createCapabilitiesAndConfig(
                /* autoSupported= */false, /* autoEnabled= */false);
        when(mTimeManager.getTimeCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(DISABLED_DEPENDENT_SETTING);
    }

    @Test
    public void autoTimeNotSupported_notEnable() {
        TimeCapabilitiesAndConfig capabilitiesAndConfig = createCapabilitiesAndConfig(
                /* autoSupported= */false, /* autoEnabled= */false);
        when(mTimeManager.getTimeCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);

        assertThat(mController.isEnabled()).isFalse();
    }

    @Test
    public void isEnabled_autoEnabledIsFalse_shouldReadFromTimeManagerConfig() {
        // Disabled
        TimeCapabilitiesAndConfig capabilitiesAndConfig = createCapabilitiesAndConfig(
                /* autoSupported= */true, /* autoEnabled= */false);
        when(mTimeManager.getTimeCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);

        assertThat(mController.isEnabled()).isFalse();
    }

    @Test
    public void isEnabled_autoEnabledIsTrue_shouldReadFromTimeManagerConfig() {
        // Enabled
        TimeCapabilitiesAndConfig capabilitiesAndConfig = createCapabilitiesAndConfig(
                /* autoSupported= */true, /* autoEnabled= */true);
        when(mTimeManager.getTimeCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);

        assertThat(mController.isEnabled()).isTrue();
    }

    @Test
    public void updatePreferenceChange_prefIsChecked_shouldUpdatePreferenceAndNotifyCallback() {
        TimeCapabilitiesAndConfig capabilitiesAndConfig = createCapabilitiesAndConfig(
                /* autoSupported= */true, /* autoEnabled= */false);
        when(mTimeManager.getTimeCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);
        when(mTimeManager.updateTimeConfiguration(Mockito.any())).thenReturn(true);

        assertThat(mController.onPreferenceChange(mPreference, true)).isTrue();
        verify(mCallback).updateTimeAndDateDisplay(mContext);

        // Check the service was asked to change the configuration correctly.
        TimeConfiguration timeConfiguration = new TimeConfiguration.Builder()
                .setAutoDetectionEnabled(true)
                .build();
        verify(mTimeManager).updateTimeConfiguration(timeConfiguration);

        // Update the mTimeManager mock so that it now returns the expected updated config.
        TimeCapabilitiesAndConfig capabilitiesAndConfigAfterUpdate = createCapabilitiesAndConfig(
                /* autoSupported= */true, /* autoEnabled= */true);
        when(mTimeManager.getTimeCapabilitiesAndConfig()).thenReturn(
                capabilitiesAndConfigAfterUpdate);

        assertThat(mController.isEnabled()).isTrue();
    }

    @Test
    public void updatePreferenceChange_prefIsUnchecked_shouldUpdatePreferenceAndNotifyCallback() {
        TimeCapabilitiesAndConfig capabilitiesAndConfig = createCapabilitiesAndConfig(
                /* autoSupported= */true, /* autoEnabled= */true);
        when(mTimeManager.getTimeCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);
        when(mTimeManager.updateTimeConfiguration(Mockito.any())).thenReturn(true);

        assertThat(mController.onPreferenceChange(mPreference, false)).isTrue();
        verify(mCallback).updateTimeAndDateDisplay(mContext);

        // Check the service was asked to change the configuration correctly.
        TimeConfiguration timeConfiguration = new TimeConfiguration.Builder()
                .setAutoDetectionEnabled(false)
                .build();
        verify(mTimeManager).updateTimeConfiguration(timeConfiguration);

        // Update the mTimeManager mock so that it now returns the expected updated config.
        TimeCapabilitiesAndConfig capabilitiesAndConfigAfterUpdate =
                createCapabilitiesAndConfig(/* autoSupported= */true, /* autoEnabled= */false);
        when(mTimeManager.getTimeCapabilitiesAndConfig()).thenReturn(
                capabilitiesAndConfigAfterUpdate);

        assertThat(mController.isEnabled()).isFalse();
    }

    private static TimeCapabilitiesAndConfig createCapabilitiesAndConfig(boolean autoSupported,
            boolean autoEnabled) {
        int configureAutoDetectionEnabledCapability =
                autoSupported ? Capabilities.CAPABILITY_POSSESSED
                        : Capabilities.CAPABILITY_NOT_SUPPORTED;
        TimeCapabilities capabilities = new TimeCapabilities.Builder(UserHandle.SYSTEM)
                .setConfigureAutoDetectionEnabledCapability(configureAutoDetectionEnabledCapability)
                .setSetManualTimeCapability(Capabilities.CAPABILITY_POSSESSED)
                .build();
        TimeConfiguration config = new TimeConfiguration.Builder()
                .setAutoDetectionEnabled(autoEnabled)
                .build();
        return new TimeCapabilitiesAndConfig(capabilities, config);
    }
}
