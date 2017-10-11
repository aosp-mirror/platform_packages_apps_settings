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

import android.content.Context;
import android.provider.Settings.Secure;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class LocationPreferenceControllerTest {
    @Mock
    private Preference mPreference;
    @Mock
    private PreferenceScreen mScreen;

    private LocationPreferenceController mController;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mController = new LocationPreferenceController(mContext);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
    }

    @Test
    public void isAvailable_shouldReturnTrue() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void updateState_shouldSetSummary() {
        mController.updateState(mPreference);

        verify(mPreference).setSummary(anyString());
    }

    @Test
    public void updateSummary_shouldSetSummary() {
        mController.displayPreference(mScreen);
        mController.updateSummary();

        verify(mPreference).setSummary(anyString());
    }

    @Test
    public void getLocationSummary_locationOff_shouldSetSummaryOff() {
        Secure.putInt(mContext.getContentResolver(),
            Secure.LOCATION_MODE, Secure.LOCATION_MODE_OFF);

        assertThat(mController.getLocationSummary(mContext)).isEqualTo(
            mContext.getString(R.string.location_off_summary));
    }

    @Test
    public void getLocationSummary_sensorsOnly_shouldSetSummarySensorsOnly() {
        Secure.putInt(mContext.getContentResolver(),
            Secure.LOCATION_MODE, Secure.LOCATION_MODE_SENSORS_ONLY);

        assertThat(mController.getLocationSummary(mContext)).isEqualTo(
            mContext.getString(R.string.location_on_summary,
                mContext.getString(R.string.location_mode_sensors_only_title)));
    }

    @Test
    public void getLocationSummary_highAccuracy_shouldSetSummarHighAccuracy() {
        Secure.putInt(mContext.getContentResolver(),
            Secure.LOCATION_MODE, Secure.LOCATION_MODE_HIGH_ACCURACY);

        assertThat(mController.getLocationSummary(mContext)).isEqualTo(
            mContext.getString(R.string.location_on_summary,
                mContext.getString(R.string.location_mode_high_accuracy_title)));
    }

    @Test
    public void getLocationSummary_batterySaving_shouldSetSummaryBatterySaving() {
        Secure.putInt(mContext.getContentResolver(),
            Secure.LOCATION_MODE, Secure.LOCATION_MODE_BATTERY_SAVING);

        assertThat(mController.getLocationSummary(mContext)).isEqualTo(
            mContext.getString(R.string.location_on_summary,
                mContext.getString(R.string.location_mode_battery_saving_title)));
    }

    @Test
    public void getLocationString_shouldCorrectString() {
        assertThat(mController.getLocationString(Secure.LOCATION_MODE_OFF)).isEqualTo(
            R.string.location_mode_location_off_title);
        assertThat(mController.getLocationString(Secure.LOCATION_MODE_SENSORS_ONLY)).isEqualTo(
            R.string.location_mode_sensors_only_title);
        assertThat(mController.getLocationString(Secure.LOCATION_MODE_BATTERY_SAVING)).isEqualTo(
            R.string.location_mode_battery_saving_title);
        assertThat(mController.getLocationString(Secure.LOCATION_MODE_HIGH_ACCURACY)).isEqualTo(
            R.string.location_mode_high_accuracy_title);
    }

}
