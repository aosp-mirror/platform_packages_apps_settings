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

package com.android.settings.nfc;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

@RunWith(SettingsRobolectricTestRunner.class)
public class NfcForegroundPreferenceTest {
    @Mock
    private PaymentBackend mPaymentBackend;

    private Context mContext;
    private PreferenceScreen mScreen;
    private NfcForegroundPreference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mScreen = spy(new PreferenceScreen(mContext, null));
        when(mScreen.getPreferenceManager()).thenReturn(mock(PreferenceManager.class));
        when(mPaymentBackend.isForegroundMode()).thenReturn(false);
        mPreference = new NfcForegroundPreference(mContext, mPaymentBackend);
        mScreen.addPreference(mPreference);
    }

    @Test
    public void testTogglingMode() {
        String nfc_payment_favor_default = mContext.getString(R.string.nfc_payment_favor_default);
        String nfc_payment_favor_open = mContext.getString(R.string.nfc_payment_favor_open);

        assertThat(mPreference.getEntry()).isEqualTo(nfc_payment_favor_default);
        assertThat(mPreference.getSummary()).isEqualTo(nfc_payment_favor_default);

        mPreference.setValueIndex(0);
        mPreference.callChangeListener(mPreference.getEntryValues()[0]);
        verify(mPaymentBackend).setForegroundMode(true);
        assertThat(mPreference.getEntry()).isEqualTo(nfc_payment_favor_open);
        assertThat(mPreference.getSummary()).isEqualTo(nfc_payment_favor_open);

        mPreference.setValueIndex(1);
        mPreference.callChangeListener(mPreference.getEntryValues()[1]);
        verify(mPaymentBackend).setForegroundMode(false);
        assertThat(mPreference.getEntry()).isEqualTo(nfc_payment_favor_default);
        assertThat(mPreference.getSummary()).isEqualTo(nfc_payment_favor_default);
    }
}
