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

package com.android.settings.wifi.details;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.net.wifi.WifiConfiguration;

import androidx.preference.DropDownPreference;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RuntimeEnvironment;

@RunWith(SettingsRobolectricTestRunner.class)
public class WifiPrivacyPreferenceControllerTest {

    private static final int PRIVACY_RANDOMIZED = 0;
    private static final int PRIVACY_TRUSTED = 1;

    @Mock
    private WifiConfiguration mWifiConfiguration;

    private WifiPrivacyPreferenceController mPreferenceController;
    private Context mContext;
    private DropDownPreference mDropDownPreference;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;

        WifiPrivacyPreferenceController preferenceController = new WifiPrivacyPreferenceController(
                mContext);
        preferenceController.setWifiConfiguration(mWifiConfiguration);
        mPreferenceController = spy(preferenceController);
        mDropDownPreference = new DropDownPreference(mContext);
        mDropDownPreference.setEntries(R.array.wifi_privacy_entries);
        mDropDownPreference.setEntryValues(R.array.wifi_privacy_values);
    }

    @Test
    public void testUpdateState_wifiPrivacy_setCorrectValue() {
        doReturn(PRIVACY_TRUSTED).when(mPreferenceController).getRandomizationValue();

        mPreferenceController.updateState(mDropDownPreference);

        assertThat(mDropDownPreference.getEntry()).isEqualTo("Trusted");
    }

    @Test
    public void testUpdateState_wifiNotMetered_setCorrectValue() {
        doReturn(PRIVACY_RANDOMIZED).when(mPreferenceController).getRandomizationValue();

        mPreferenceController.updateState(mDropDownPreference);

        assertThat(mDropDownPreference.getEntry()).isEqualTo("Default (use randomized MAC)");
    }

    @Test
    public void testController_resilientToNullConfig() {
        mPreferenceController = spy(new WifiPrivacyPreferenceController(mContext));

        mPreferenceController.getRandomizationValue();
        mPreferenceController.onPreferenceChange(mDropDownPreference, new String("1"));
    }
}
