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

package com.android.settings.development;

import static com.android.settings.development.BluetoothAvrcpVersionPreferenceController
        .BLUETOOTH_AVRCP_VERSION_PROPERTY;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.os.SystemProperties;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class BluetoothAvrcpVersionPreferenceControllerTest {

    @Mock
    private ListPreference mPreference;
    @Mock
    private PreferenceScreen mPreferenceScreen;

    private Context mContext;
    private BluetoothAvrcpVersionPreferenceController mController;

    /**
     * 0: AVRCP 1.4 (Default)
     * 1: AVRCP 1.3
     * 2: AVRCP 1.5
     * 3: AVRCP 1.6
     */
    private String[] mListValues;
    private String[] mListSummaries;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        final Resources resources = mContext.getResources();
        mListValues = resources.getStringArray(R.array.bluetooth_avrcp_version_values);
        mListSummaries = resources.getStringArray(R.array.bluetooth_avrcp_versions);
        mController = new BluetoothAvrcpVersionPreferenceController(mContext);
        when(mPreferenceScreen.findPreference(mController.getPreferenceKey()))
            .thenReturn(mPreference);
        mController.displayPreference(mPreferenceScreen);
    }

    @Test
    public void onPreferenceChange_setAvrcp13_shouldEnableAvrcp13() {
        mController.onPreferenceChange(mPreference, mListValues[1]);

        final String currentValue = SystemProperties.get(BLUETOOTH_AVRCP_VERSION_PROPERTY);

        assertThat(currentValue).isEqualTo(mListValues[1]);
    }

    @Test
    public void onPreferenceChange_setAvrcp15_shouldEnableAvrcp15() {
        mController.onPreferenceChange(mPreference, mListValues[2]);

        final String currentValue = SystemProperties.get(BLUETOOTH_AVRCP_VERSION_PROPERTY);

        assertThat(currentValue).isEqualTo(mListValues[2]);
    }

    @Test
    public void updateState_setAvrcp13_shouldSetPreferenceToAvrcp13() {
        SystemProperties.set(BLUETOOTH_AVRCP_VERSION_PROPERTY, mListValues[1]);

        mController.updateState(mPreference);

        verify(mPreference).setValue(mListValues[1]);
        verify(mPreference).setSummary(mListSummaries[1]);
    }

    @Test
    public void updateState_setAvrcp15_shouldSetPreferenceToAvrcp15() {
        SystemProperties.set(BLUETOOTH_AVRCP_VERSION_PROPERTY, mListValues[2]);

        mController.updateState(mPreference);

        verify(mPreference).setValue(mListValues[2]);
        verify(mPreference).setSummary(mListSummaries[2]);
    }

    @Test
    public void updateState_noValueSet_shouldSetDefaultToAvrcp14() {
        mController.updateState(mPreference);

        verify(mPreference).setValue(mListValues[0]);
        verify(mPreference).setSummary(mListSummaries[0]);
    }
}
