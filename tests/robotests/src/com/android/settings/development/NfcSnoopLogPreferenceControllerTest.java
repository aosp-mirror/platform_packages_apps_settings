/*
 * Copyright (C) 2022 The Android Open Source Project
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

import static com.android.settings.development.NfcSnoopLogPreferenceController
        .NFCSNOOP_MODE_FILTERED;
import static com.android.settings.development.NfcSnoopLogPreferenceController
        .NFCSNOOP_MODE_FULL;
import static com.android.settings.development.NfcSnoopLogPreferenceController
        .NFC_NFCSNOOP_LOG_MODE_PROPERTY;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.SystemProperties;

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
public class NfcSnoopLogPreferenceControllerTest {
    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private DevelopmentSettingsDashboardFragment mFragment;

    private Context mContext;
    private SwitchPreference mPreference;
    private NfcSnoopLogPreferenceController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mPreference = new SwitchPreference(mContext);
        mController = spy(new NfcSnoopLogPreferenceController(mContext, mFragment));
        when(mPreferenceScreen.findPreference(mController.getPreferenceKey()))
            .thenReturn(mPreference);
        mController.displayPreference(mPreferenceScreen);
    }

    @Test
    public void onNfcRebootDialogConfirmed_nfcSnoopLogDisabled_shouldChangeProperty() {
        SystemProperties.set(NFC_NFCSNOOP_LOG_MODE_PROPERTY, NFCSNOOP_MODE_FILTERED);
        mController.mChanged = true;

        mController.onNfcRebootDialogConfirmed();

        final String currentValue = SystemProperties.get(NFC_NFCSNOOP_LOG_MODE_PROPERTY);
        assertThat(currentValue.equals(NFCSNOOP_MODE_FULL)).isTrue();
    }

    @Test
    public void onNfcRebootDialogConfirmed_nfcSnoopLogEnabled_shouldChangeProperty() {
        SystemProperties.set(NFC_NFCSNOOP_LOG_MODE_PROPERTY, NFCSNOOP_MODE_FULL);
        mController.mChanged = true;

        mController.onNfcRebootDialogConfirmed();

        final String currentValue = SystemProperties.get(NFC_NFCSNOOP_LOG_MODE_PROPERTY);
        assertThat(currentValue.equals(NFCSNOOP_MODE_FILTERED)).isTrue();
    }

    @Test
    public void onNfcRebootDialogCanceled_shouldNotChangeProperty() {
        SystemProperties.set(NFC_NFCSNOOP_LOG_MODE_PROPERTY, NFCSNOOP_MODE_FILTERED);
        mController.mChanged = true;

        mController.onNfcRebootDialogCanceled();

        final String currentValue = SystemProperties.get(NFC_NFCSNOOP_LOG_MODE_PROPERTY);
        assertThat(currentValue.equals(NFCSNOOP_MODE_FILTERED)).isTrue();
    }
}
