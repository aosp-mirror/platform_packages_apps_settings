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
import static org.mockito.Mockito.when;

import android.content.Context;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.os.UserManager;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

@RunWith(SettingsRobolectricTestRunner.class)
public class NfcPreferenceControllerTest {

    Context mContext;
    @Mock
    private NfcAdapter mNfcAdapter;
    @Mock
    NfcManager mManager;
    @Mock
    private UserManager mUserManager;
    @Mock
    private PreferenceScreen mScreen;

    private SwitchPreference mNfcPreference;
    private NfcPreferenceController mNfcController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);

        when(mContext.getApplicationContext()).thenReturn(mContext);
        when(mContext.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
        when(mContext.getSystemService(Context.NFC_SERVICE)).thenReturn(mManager);
        when(NfcAdapter.getDefaultAdapter(mContext)).thenReturn(mNfcAdapter);

        mNfcController = new NfcPreferenceController(mContext);
        mNfcPreference = new SwitchPreference(RuntimeEnvironment.application);
        when(mScreen.findPreference(mNfcController.getPreferenceKey())).thenReturn(mNfcPreference);

        Settings.Global.putString(mContext.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_TOGGLEABLE_RADIOS,
                Settings.Global.RADIO_NFC);

        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON,
                0);
        mNfcController.displayPreference(mScreen);
    }

    @Test
    public void isAvailable_hasNfc_shouldReturnTrue() {
        when(mNfcAdapter.isEnabled()).thenReturn(true);
        assertThat(mNfcController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_noNfcAdapter_shouldReturnFalse() {
        ReflectionHelpers.setField(mNfcController, "mNfcAdapter", null);
        assertThat(mNfcController.isAvailable()).isFalse();
    }

    @Test
    public void isNfcEnable_nfcStateNotTurning_shouldReturnTrue() {
        when(mNfcAdapter.getAdapterState()).thenReturn(NfcAdapter.STATE_ON);
        mNfcController.onResume();
        assertThat(mNfcPreference.isEnabled()).isTrue();

        when(mNfcAdapter.getAdapterState()).thenReturn(NfcAdapter.STATE_OFF);
        mNfcController.onResume();
        assertThat(mNfcPreference.isEnabled()).isTrue();
    }

    @Test
    public void isNfcEnable_nfcStateTurning_shouldReturnFalse() {
        when(mNfcAdapter.getAdapterState()).thenReturn(NfcAdapter.STATE_TURNING_ON);
        mNfcController.onResume();
        assertThat(mNfcPreference.isEnabled()).isFalse();

        when(mNfcAdapter.getAdapterState()).thenReturn(NfcAdapter.STATE_TURNING_OFF);
        mNfcController.onResume();
        assertThat(mNfcPreference.isEnabled()).isFalse();
    }

    @Test
    public void isNfcChecked_nfcStateOn_shouldReturnTrue() {
        when(mNfcAdapter.getAdapterState()).thenReturn(NfcAdapter.STATE_ON);
        mNfcController.onResume();
        assertThat(mNfcPreference.isChecked()).isTrue();

        when(mNfcAdapter.getAdapterState()).thenReturn(NfcAdapter.STATE_TURNING_ON);
        mNfcController.onResume();
        assertThat(mNfcPreference.isChecked()).isTrue();
    }

    @Test
    public void isNfcChecked_nfcStateOff_shouldReturnFalse() {
        when(mNfcAdapter.getAdapterState()).thenReturn(NfcAdapter.STATE_OFF);
        mNfcController.onResume();
        assertThat(mNfcPreference.isChecked()).isFalse();

        when(mNfcAdapter.getAdapterState()).thenReturn(NfcAdapter.STATE_TURNING_OFF);
        mNfcController.onResume();
        assertThat(mNfcPreference.isChecked()).isFalse();
    }
}
