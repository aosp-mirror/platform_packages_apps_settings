/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.applications.specialaccess;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;

import android.content.Context;
import android.nfc.NfcAdapter;

import androidx.preference.Preference;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
@Ignore
public class PaymentSettingsEnablerTest {
    private Context mContext;
    private Preference mPreference;
    private PaymentSettingsEnabler mEnabler;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mPreference = new Preference(mContext);
        mEnabler = spy(new PaymentSettingsEnabler(mContext, mPreference));
    }

    @Test
    public void handleNfcStateChanged_stateOff_shouldChangeSummaryAndDisablePreference() {
        mEnabler.handleNfcStateChanged(NfcAdapter.STATE_OFF);

        assertThat(mPreference.getSummary().toString()).contains(
                mContext.getString(R.string.nfc_and_payment_settings_payment_off_nfc_off_summary));
        assertThat(mPreference.isEnabled()).isFalse();
    }

    @Test
    public void handleNfcStateChanged_stateOn_shouldClearSummaryAndEnablePreference() {
        mEnabler.handleNfcStateChanged(NfcAdapter.STATE_ON);

        assertThat(mPreference.getSummary()).isNull();
        assertThat(mPreference.isEnabled()).isTrue();
    }
}
