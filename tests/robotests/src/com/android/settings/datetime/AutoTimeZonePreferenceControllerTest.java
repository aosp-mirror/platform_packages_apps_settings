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

import static android.app.time.DetectorStatusTypes.DETECTION_ALGORITHM_STATUS_NOT_SUPPORTED;
import static android.app.time.DetectorStatusTypes.DETECTION_ALGORITHM_STATUS_RUNNING;
import static android.app.time.DetectorStatusTypes.DETECTOR_STATUS_RUNNING;
import static android.app.time.LocationTimeZoneAlgorithmStatus.PROVIDER_STATUS_NOT_PRESENT;
import static android.app.time.LocationTimeZoneAlgorithmStatus.PROVIDER_STATUS_NOT_READY;

import static com.android.settings.core.BasePreferenceController.DISABLED_DEPENDENT_SETTING;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
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
import android.os.UserHandle;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.flags.Flags;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class AutoTimeZonePreferenceControllerTest {

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Mock
    private UpdateTimeAndDateCallback mCallback;
    private Context mContext;
    private Preference mPreference;
    @Mock
    private TimeManager mTimeManager;

    private AutoTimeZonePreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);

        mPreference = new Preference(mContext);

        when(mContext.getSystemService(TimeManager.class)).thenReturn(mTimeManager);

        mController = new AutoTimeZonePreferenceController(mContext, "test_key");
        mController.setFromSUW(false);
    }

    @Test
    public void isFromSUW_notAvailable() {
        TimeZoneCapabilitiesAndConfig capabilitiesAndConfig = createCapabilitiesAndConfig(
                /* autoSupported= */true, /* autoEnabled= */false);
        when(mTimeManager.getTimeZoneCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);

        mController.setFromSUW(true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(DISABLED_DEPENDENT_SETTING);
    }

    @Test
    public void notFromSUW_isAvailable() {
        TimeZoneCapabilitiesAndConfig capabilitiesAndConfig = createCapabilitiesAndConfig(
                /* autoSupported= */true, /* autoEnabled= */false);
        when(mTimeManager.getTimeZoneCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);

        mController.setFromSUW(false);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void autoTimeZoneNotSupported_notAvailable() {
        TimeZoneCapabilitiesAndConfig capabilitiesAndConfig = createCapabilitiesAndConfig(
                /* autoSupported= */false, /* autoEnabled= */false);
        when(mTimeManager.getTimeZoneCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(DISABLED_DEPENDENT_SETTING);
    }

    @Test
    public void isFromSUW_notEnable() {
        TimeZoneCapabilitiesAndConfig capabilitiesAndConfig = createCapabilitiesAndConfig(
                /* autoSupported= */false, /* autoEnabled= */false);
        when(mTimeManager.getTimeZoneCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);

        mController.setFromSUW(true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(DISABLED_DEPENDENT_SETTING);
    }

    @Test
    public void isFromSUW_isEnable() {
        TimeZoneCapabilitiesAndConfig capabilitiesAndConfig = createCapabilitiesAndConfig(
                /* autoSupported= */false, /* autoEnabled= */true);
        when(mTimeManager.getTimeZoneCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);

        mController.setFromSUW(true);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void autoTimeZoneNotSupported_notEnable() {
        TimeZoneCapabilitiesAndConfig capabilitiesAndConfig = createCapabilitiesAndConfig(
                /* autoSupported= */false, /* autoEnabled= */false);
        when(mTimeManager.getTimeZoneCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);

        assertThat(mController.isEnabled()).isFalse();
    }

    @Test
    public void isEnabled_autoEnabledIsFalse_shouldReadFromTimeManagerConfig() {
        // Disabled
        TimeZoneCapabilitiesAndConfig capabilitiesAndConfig = createCapabilitiesAndConfig(
                /* autoSupported= */true, /* autoEnabled= */false);
        when(mTimeManager.getTimeZoneCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);

        assertThat(mController.isEnabled()).isFalse();
    }

    @Test
    public void isEnabled_autoEnabledIsTrue_shouldReadFromTimeManagerConfig() {
        // Enabled
        TimeZoneCapabilitiesAndConfig capabilitiesAndConfig = createCapabilitiesAndConfig(
                /* autoSupported= */true, /* autoEnabled= */true);
        when(mTimeManager.getTimeZoneCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);

        assertThat(mController.isEnabled()).isTrue();
    }

    @Test
    public void updatePreferenceChange_prefIsChecked_shouldUpdatePreferenceAndNotifyCallback() {
        TimeZoneCapabilitiesAndConfig capabilitiesAndConfig = createCapabilitiesAndConfig(
                /* autoSupported= */true, /* autoEnabled= */false);
        when(mTimeManager.getTimeZoneCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);
        when(mTimeManager.updateTimeZoneConfiguration(Mockito.any())).thenReturn(true);

        mController.setTimeAndDateCallback(mCallback);

        assertThat(mController.onPreferenceChange(mPreference, true)).isTrue();
        verify(mCallback).updateTimeAndDateDisplay(mContext);

        // Check the service was asked to change the configuration correctly.
        TimeZoneConfiguration timeZoneConfiguration = new TimeZoneConfiguration.Builder()
                .setAutoDetectionEnabled(true)
                .build();
        verify(mTimeManager).updateTimeZoneConfiguration(timeZoneConfiguration);

        // Update the mTimeManager mock so that it now returns the expected updated config.
        TimeZoneCapabilitiesAndConfig capabilitiesAndConfigAfterUpdate =
                createCapabilitiesAndConfig(/* autoSupported= */true, /* autoEnabled= */true);
        when(mTimeManager.getTimeZoneCapabilitiesAndConfig())
                .thenReturn(capabilitiesAndConfigAfterUpdate);

        assertThat(mController.isEnabled()).isTrue();
    }

    @Test
    public void updatePreferenceChange_prefIsUnchecked_shouldUpdatePreferenceAndNotifyCallback() {
        TimeZoneCapabilitiesAndConfig capabilitiesAndConfig = createCapabilitiesAndConfig(
                /* autoSupported= */true, /* autoEnabled= */true);
        when(mTimeManager.getTimeZoneCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);
        when(mTimeManager.updateTimeZoneConfiguration(Mockito.any())).thenReturn(true);

        mController.setTimeAndDateCallback(mCallback);

        assertThat(mController.onPreferenceChange(mPreference, false)).isTrue();
        verify(mCallback).updateTimeAndDateDisplay(mContext);

        // Check the service was asked to change the configuration correctly.
        TimeZoneConfiguration timeZoneConfiguration = new TimeZoneConfiguration.Builder()
                .setAutoDetectionEnabled(false)
                .build();
        verify(mTimeManager).updateTimeZoneConfiguration(timeZoneConfiguration);

        // Update the mTimeManager mock so that it now returns the expected updated config.
        TimeZoneCapabilitiesAndConfig capabilitiesAndConfigAfterUpdate =
                createCapabilitiesAndConfig(/* autoSupported= */true, /* autoEnabled= */false);
        when(mTimeManager.getTimeZoneCapabilitiesAndConfig())
                .thenReturn(capabilitiesAndConfigAfterUpdate);

        assertThat(mController.isEnabled()).isFalse();
    }

    @Test
    public void getSummary() {
        mController.setTimeAndDateCallback(mCallback);

        TimeZoneCapabilitiesAndConfig capabilitiesAndConfig = createCapabilitiesAndConfig(
                /* autoSupported= */true, /* autoEnabled= */true, /* telephonySupported= */
                true);
        when(mTimeManager.getTimeZoneCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);
        when(mTimeManager.updateTimeZoneConfiguration(Mockito.any())).thenReturn(true);

        assertThat(mController.getSummary().toString()).isEqualTo(
                mContext.getString(R.string.zone_auto_title_summary));

        capabilitiesAndConfig = createCapabilitiesAndConfig(
                /* autoSupported= */true, /* autoEnabled= */true, /* telephonySupported= */
                false);
        when(mTimeManager.getTimeZoneCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);
        when(mTimeManager.updateTimeZoneConfiguration(Mockito.any())).thenReturn(true);

        assertThat(mController.getSummary().toString()).isEqualTo(
                mContext.getString(R.string.auto_zone_requires_location_summary));
    }

    @Test
    @EnableFlags({Flags.FLAG_REVAMP_TOGGLES})
    public void toggleOff_revampFlagOn_shouldToggleOffUseLocation() {
        TimeZoneCapabilitiesAndConfig capabilitiesAndConfig = createCapabilitiesAndConfig(
                /* autoSupported= */ true,
                /* autoEnabled= */ true,
                /* telephonySupported= */ true,
                /* locationSupported= */ true);
        when(mTimeManager.getTimeZoneCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);

        mController.setChecked(false);

        TimeZoneConfiguration configuration = new TimeZoneConfiguration.Builder()
                .setAutoDetectionEnabled(false)
                .setGeoDetectionEnabled(false)
                .build();

        verify(mTimeManager).updateTimeZoneConfiguration(configuration);
    }

    @Test
    @DisableFlags({Flags.FLAG_REVAMP_TOGGLES})
    public void toggleOff_revampFlagOff_shouldToggleOffUseLocation() {
        TimeZoneCapabilitiesAndConfig capabilitiesAndConfig = createCapabilitiesAndConfig(
                /* autoSupported= */ true,
                /* autoEnabled= */ true,
                /* telephonySupported= */ true,
                /* locationSupported= */ true);
        when(mTimeManager.getTimeZoneCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);

        mController.setChecked(false);

        TimeZoneConfiguration configuration = new TimeZoneConfiguration.Builder()
                .setAutoDetectionEnabled(false)
                .build();

        verify(mTimeManager).updateTimeZoneConfiguration(configuration);
    }

    private static TimeZoneCapabilitiesAndConfig createCapabilitiesAndConfig(
            boolean autoSupported, boolean autoEnabled, boolean telephonySupported) {
        return createCapabilitiesAndConfig(autoSupported, autoEnabled, telephonySupported, false);
    }

    private static TimeZoneCapabilitiesAndConfig createCapabilitiesAndConfig(
            boolean autoSupported, boolean autoEnabled, boolean telephonySupported,
            boolean locationSupported) {
        TimeZoneDetectorStatus status = new TimeZoneDetectorStatus(DETECTOR_STATUS_RUNNING,
                new TelephonyTimeZoneAlgorithmStatus(
                        telephonySupported ? DETECTION_ALGORITHM_STATUS_RUNNING
                                : DETECTION_ALGORITHM_STATUS_NOT_SUPPORTED),
                new LocationTimeZoneAlgorithmStatus(DETECTION_ALGORITHM_STATUS_RUNNING,
                        PROVIDER_STATUS_NOT_READY, null,
                        PROVIDER_STATUS_NOT_PRESENT, null));
        int configureAutoDetectionEnabledCapability =
                autoSupported ? Capabilities.CAPABILITY_POSSESSED
                        : Capabilities.CAPABILITY_NOT_SUPPORTED;
        TimeZoneCapabilities capabilities = new TimeZoneCapabilities.Builder(UserHandle.SYSTEM)
                .setConfigureAutoDetectionEnabledCapability(configureAutoDetectionEnabledCapability)
                .setUseLocationEnabled(true)
                .setConfigureGeoDetectionEnabledCapability(
                        locationSupported ? Capabilities.CAPABILITY_POSSESSED
                                : Capabilities.CAPABILITY_NOT_SUPPORTED)
                .setSetManualTimeZoneCapability(Capabilities.CAPABILITY_POSSESSED)
                .build();
        TimeZoneConfiguration config = new TimeZoneConfiguration.Builder()
                .setAutoDetectionEnabled(autoEnabled)
                .setGeoDetectionEnabled(locationSupported)
                .build();
        return new TimeZoneCapabilitiesAndConfig(status, capabilities, config);
    }

    private static TimeZoneCapabilitiesAndConfig createCapabilitiesAndConfig(
            boolean autoSupported, boolean autoEnabled) {
        return createCapabilitiesAndConfig(autoSupported, autoEnabled, false);
    }
}
