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

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.nfc.NfcAdapter;

import com.android.settingslib.widget.MainSwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class NfcEnablerTest {

    @Mock
    private MainSwitchPreference mNfcPreference;

    private Context mContext;
    private NfcEnabler mNfcEnabler;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mNfcEnabler = spy(new NfcEnabler(mContext, mNfcPreference));
    }

    @Test
    public void handleNfcStateChanged_stateOff_shouldCheckIfPreferenceEnableState() {
        mNfcEnabler.handleNfcStateChanged(NfcAdapter.STATE_OFF);
        verify(mNfcPreference).updateStatus(false);
        verify(mNfcPreference).setEnabled(true);

        mNfcEnabler.handleNfcStateChanged(NfcAdapter.STATE_ON);
        verify(mNfcPreference).updateStatus(true);
        verify(mNfcPreference, times(2)).setEnabled(true);
    }
}
