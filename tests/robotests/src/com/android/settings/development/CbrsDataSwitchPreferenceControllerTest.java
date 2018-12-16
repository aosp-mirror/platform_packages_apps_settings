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

package com.android.settings.development;

import static org.mockito.Mockito.when;
import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.telephony.TelephonyManager;

import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class CbrsDataSwitchPreferenceControllerTest {

    @Mock
    private PreferenceScreen mPreferenceScreen;
    private Context mContext;
    private TelephonyManager mTelephonyManager;
    private SwitchPreference mPreference;
    private CbrsDataSwitchPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new CbrsDataSwitchPreferenceController(mContext);
        mPreference = new SwitchPreference(mContext);
        when(mPreferenceScreen.findPreference(mController.getPreferenceKey()))
            .thenReturn(mPreference);
        mTelephonyManager = mContext.getSystemService(TelephonyManager.class);
        mController.displayPreference(mPreferenceScreen);
    }

    @Test
    public void onPreferenceChanged_settingEnabled_shouldEnableANAS() {
        mController.onPreferenceChange(mPreference, true);

        assertThat(mTelephonyManager.isAlternativeNetworkEnabled()).isTrue();
    }

    @Test
    public void onPreferenceChanged_settingDisabled_shouldDisableANAS() {
        mController.onPreferenceChange(mPreference, false);

        assertThat(mTelephonyManager.isAlternativeNetworkEnabled()).isFalse();
    }

    @Test
    public void updateState_settingEnabled_shouldEnablePreference() {
        mTelephonyManager.setAlternativeNetworkState(true);
        mController.updateState(mPreference);

        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void updateState_settingDisabled_shouldDisablePreference() {
        mTelephonyManager.setAlternativeNetworkState(false);
        mController.updateState(mPreference);

        assertThat(mPreference.isChecked()).isFalse();
    }
}
