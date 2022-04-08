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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class WifiPrivacyPreferenceControllerTest {

    private static final int PRIVACY_RANDOMIZED = WifiConfiguration.RANDOMIZATION_PERSISTENT;
    private static final int PRIVACY_TRUSTED = WifiConfiguration.RANDOMIZATION_NONE;

    @Mock
    private WifiConfiguration mWifiConfiguration;

    private WifiPrivacyPreferenceController mPreferenceController;
    private Context mContext;
    private DropDownPreference mDropDownPreference;
    private String[] perferenceString;

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

        perferenceString = mContext.getResources().getStringArray(R.array.wifi_privacy_entries);
    }

    @Test
    public void testUpdateState_wifiPrivacy_setCorrectValue() {
        doReturn(PRIVACY_TRUSTED).when(mPreferenceController).getRandomizationValue();

        mPreferenceController.updateState(mDropDownPreference);

        int prefValue = mPreferenceController.translateMacRandomizedValueToPrefValue(
                PRIVACY_TRUSTED);
        assertThat(mDropDownPreference.getEntry()).isEqualTo(perferenceString[prefValue]);
    }

    @Test
    public void testUpdateState_wifiNotMetered_setCorrectValue() {
        doReturn(PRIVACY_RANDOMIZED).when(mPreferenceController).getRandomizationValue();

        mPreferenceController.updateState(mDropDownPreference);

        int prefValue = mPreferenceController.translateMacRandomizedValueToPrefValue(
                PRIVACY_RANDOMIZED);
        assertThat(mDropDownPreference.getEntry()).isEqualTo(perferenceString[prefValue]);
    }

    @Test
    public void testController_resilientToNullConfig() {
        mPreferenceController = spy(new WifiPrivacyPreferenceController(mContext));

        mPreferenceController.getRandomizationValue();
        mPreferenceController.onPreferenceChange(mDropDownPreference, "1");
    }

    @Test
    public void testUpdateState_isNotEphemeralNetwork_shouldBeSelectable() {
        mPreferenceController.setIsEphemeral(false);
        mPreferenceController.updateState(mDropDownPreference);

        assertThat(mDropDownPreference.isSelectable()).isTrue();
    }

    @Test
    public void testUpdateState_isEphemeralNetwork_shouldNotSelectable() {
        mPreferenceController.setIsEphemeral(true);
        mPreferenceController.updateState(mDropDownPreference);

        assertThat(mDropDownPreference.isSelectable()).isFalse();
    }

    @Test
    public void testUpdateState_isNotPasspointNetwork_shouldBeSelectable() {
        mPreferenceController.setIsPasspoint(false);
        mPreferenceController.updateState(mDropDownPreference);

        assertThat(mDropDownPreference.isSelectable()).isTrue();
    }

    @Test
    public void testUpdateState_isPasspointNetwork_shouldNotSelectable() {
        mPreferenceController.setIsPasspoint(true);
        mPreferenceController.updateState(mDropDownPreference);

        assertThat(mDropDownPreference.isSelectable()).isFalse();
    }
}
