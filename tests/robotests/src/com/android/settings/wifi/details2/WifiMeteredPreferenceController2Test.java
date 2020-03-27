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
import static org.mockito.Mockito.spy;

import android.content.Context;

import androidx.preference.DropDownPreference;

import com.android.settings.R;
import com.android.wifitrackerlib.WifiEntry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class WifiMeteredPreferenceController2Test {

    private static final int METERED_OVERRIDE_NONE = 0;
    private static final int METERED_OVERRIDE_METERED = 1;
    private static final int METERED_OVERRIDE_NOT_METERED = 2;

    @Mock
    private WifiEntry mWifiEntry;

    private WifiMeteredPreferenceController2 mPreferenceController;
    private Context mContext;
    private DropDownPreference mDropDownPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;

        mPreferenceController = spy(
                new WifiMeteredPreferenceController2(mContext, mWifiEntry));
        mDropDownPreference = new DropDownPreference(mContext);
        mDropDownPreference.setEntries(R.array.wifi_metered_entries);
        mDropDownPreference.setEntryValues(R.array.wifi_metered_values);
    }

    @Test
    public void testUpdateState_wifiMetered_setCorrectValue() {
        doReturn(METERED_OVERRIDE_METERED).when(mPreferenceController).getMeteredOverride();

        mPreferenceController.updateState(mDropDownPreference);

        assertThat(mDropDownPreference.getEntry()).isEqualTo("Treat as metered");
    }

    @Test
    public void testUpdateState_wifiNotMetered_setCorrectValue() {
        doReturn(METERED_OVERRIDE_NOT_METERED).when(mPreferenceController).getMeteredOverride();

        mPreferenceController.updateState(mDropDownPreference);

        assertThat(mDropDownPreference.getEntry()).isEqualTo("Treat as unmetered");
    }

    @Test
    public void testUpdateState_wifiAuto_setCorrectValue() {
        doReturn(METERED_OVERRIDE_NONE).when(mPreferenceController).getMeteredOverride();

        mPreferenceController.updateState(mDropDownPreference);

        assertThat(mDropDownPreference.getEntry()).isEqualTo("Detect automatically");
    }
}
