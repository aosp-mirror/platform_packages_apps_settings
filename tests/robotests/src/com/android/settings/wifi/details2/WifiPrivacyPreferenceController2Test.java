/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.wifi.details2;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.preference.DropDownPreference;

import com.android.settings.R;
import com.android.wifitrackerlib.WifiEntry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class WifiPrivacyPreferenceController2Test {

    private static final int PRIVACY_RANDOMIZED = WifiEntry.PRIVACY_RANDOMIZED_MAC;
    private static final int PRIVACY_TRUSTED = WifiEntry.PRIVACY_DEVICE_MAC;

    @Mock private WifiEntry mMockWifiEntry;

    private WifiPrivacyPreferenceController2 mPreferenceController;
    private Context mContext;
    private DropDownPreference mDropDownPreference;
    private String[] mPerferenceStrings;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;

        mMockWifiEntry = mock(WifiEntry.class);
        WifiPrivacyPreferenceController2 preferenceController =
                new WifiPrivacyPreferenceController2(mContext);
        preferenceController.setWifiEntry(mMockWifiEntry);
        mPreferenceController = spy(preferenceController);
        mDropDownPreference = new DropDownPreference(mContext);
        mDropDownPreference.setEntries(R.array.wifi_privacy_entries);
        mDropDownPreference.setEntryValues(R.array.wifi_privacy_values);

        mPerferenceStrings = mContext.getResources().getStringArray(R.array.wifi_privacy_entries);
    }

    @Test
    public void testUpdateState_wifiPrivacy_setCorrectValue() {
        doReturn(PRIVACY_TRUSTED).when(mPreferenceController).getRandomizationValue();

        mPreferenceController.updateState(mDropDownPreference);

        int prefValue = mPreferenceController.translateMacRandomizedValueToPrefValue(
                PRIVACY_TRUSTED);
        assertThat(mDropDownPreference.getEntry()).isEqualTo(mPerferenceStrings[prefValue]);
    }

    @Test
    public void testUpdateState_wifiNotMetered_setCorrectValue() {
        doReturn(PRIVACY_RANDOMIZED).when(mPreferenceController).getRandomizationValue();

        mPreferenceController.updateState(mDropDownPreference);

        int prefValue = mPreferenceController.translateMacRandomizedValueToPrefValue(
                PRIVACY_RANDOMIZED);
        assertThat(mDropDownPreference.getEntry()).isEqualTo(mPerferenceStrings[prefValue]);
    }

    @Test
    public void testUpdateState_canSetPrivacy_shouldBeSelectable() {
        when(mMockWifiEntry.canSetPrivacy()).thenReturn(true);

        mPreferenceController.updateState(mDropDownPreference);

        assertThat(mDropDownPreference.isSelectable()).isTrue();
    }

    @Test
    public void testUpdateState_canNotSetPrivacy_shouldNotSelectable() {
        when(mMockWifiEntry.canSetPrivacy()).thenReturn(false);

        mPreferenceController.updateState(mDropDownPreference);

        assertThat(mDropDownPreference.isSelectable()).isFalse();
    }
}
