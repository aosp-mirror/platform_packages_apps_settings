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

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.ContentResolver;
import android.content.Context;
import android.nfc.NfcAdapter;
import android.provider.Settings;

import androidx.preference.SwitchPreference;

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
    private SwitchPreference mNfcPreference;

    private Context mContext;
    private NfcEnabler mNfcEnabler;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mNfcEnabler = spy(new NfcEnabler(mContext, mNfcPreference));
    }

    @Test
    public void isToggleable_AirplaneModeOff_shouldReturnTrue() {
        final ContentResolver contentResolver = mContext.getContentResolver();
        Settings.Global.putInt(contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0);
        Settings.Global.putString(contentResolver,
            Settings.Global.AIRPLANE_MODE_RADIOS, Settings.Global.RADIO_NFC);
        Settings.Global.putString(contentResolver,
            Settings.Global.AIRPLANE_MODE_TOGGLEABLE_RADIOS, Settings.Global.RADIO_NFC);

        assertThat(mNfcEnabler.isToggleable()).isTrue();
    }

    @Test
    public void isToggleable_AirplaneModeOnNfcNotInAirplaneModeRadio_shouldReturnTrue() {
        final ContentResolver contentResolver = mContext.getContentResolver();
        Settings.Global.putInt(contentResolver, Settings.Global.AIRPLANE_MODE_ON, 1);
        Settings.Global.putString(contentResolver, Settings.Global.AIRPLANE_MODE_RADIOS, "");

        assertThat(mNfcEnabler.isToggleable()).isTrue();
    }

    @Test
    public void isToggleable_AirplaneModeOnNfcToggleable_shouldReturnTrue() {
        final ContentResolver contentResolver = mContext.getContentResolver();
        Settings.Global.putInt(contentResolver, Settings.Global.AIRPLANE_MODE_ON, 1);
        Settings.Global.putString(contentResolver,
            Settings.Global.AIRPLANE_MODE_RADIOS, Settings.Global.RADIO_NFC);
        Settings.Global.putString(contentResolver,
            Settings.Global.AIRPLANE_MODE_TOGGLEABLE_RADIOS, Settings.Global.RADIO_NFC);

        assertThat(mNfcEnabler.isToggleable()).isTrue();
    }

    @Test
    public void isToggleable_AirplaneModeOnNfcNotToggleable_shouldReturnFalse() {
        final ContentResolver contentResolver = mContext.getContentResolver();
        Settings.Global.putInt(contentResolver, Settings.Global.AIRPLANE_MODE_ON, 1);
        Settings.Global.putString(contentResolver,
            Settings.Global.AIRPLANE_MODE_RADIOS, Settings.Global.RADIO_NFC);
        Settings.Global.putString(contentResolver,
                Settings.Global.AIRPLANE_MODE_TOGGLEABLE_RADIOS, "");

        assertThat(mNfcEnabler.isToggleable()).isFalse();
    }

    @Test
    public void handleNfcStateChanged_stateOff_shouldCheckIfPreferenceEnableState() {
        mNfcEnabler.handleNfcStateChanged(NfcAdapter.STATE_OFF);

        verify(mNfcEnabler).isToggleable();
    }
}
