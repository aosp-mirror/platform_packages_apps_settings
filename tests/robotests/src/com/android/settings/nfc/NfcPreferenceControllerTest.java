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
import static org.mockito.Mockito.when;

import android.content.Context;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.os.UserManager;
import android.provider.Settings;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.List;

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

        mNfcController = new NfcPreferenceController(mContext,
                NfcPreferenceController.KEY_TOGGLE_NFC);
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
    public void getAvailabilityStatus_hasNfc_shouldReturnAvailable() {
        when(mNfcAdapter.isEnabled()).thenReturn(true);
        assertThat(mNfcController.getAvailabilityStatus())
                .isEqualTo(NfcPreferenceController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_noNfcAdapter_shouldReturnDisabledUnsupported() {
        ReflectionHelpers.setField(mNfcController, "mNfcAdapter", null);
        assertThat(mNfcController.getAvailabilityStatus())
                .isEqualTo(NfcPreferenceController.UNSUPPORTED_ON_DEVICE);
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

    @Test
    public void updateNonIndexableKeys_available_shouldNotUpdate() {
        when(mNfcAdapter.isEnabled()).thenReturn(true);
        final List<String> keys = new ArrayList<>();

        mNfcController.updateNonIndexableKeys(keys);

        assertThat(keys).isEmpty();
    }

    @Test
    public void updateNonIndexableKeys_notAvailable_shouldUpdate() {
        ReflectionHelpers.setField(mNfcController, "mNfcAdapter", null);
        final List<String> keys = new ArrayList<>();

        mNfcController.updateNonIndexableKeys(keys);

        assertThat(keys).hasSize(1);
    }
    @Test
    public void setChecked_True_nfcShouldEnable() {
        mNfcController.setChecked(true);
        mNfcController.onResume();

        verify(mNfcAdapter).enable();
    }

    @Test
    public void setChecked_False_nfcShouldDisable() {
        mNfcController.setChecked(false);
        mNfcController.onResume();

        verify(mNfcAdapter).disable();
    }

    @Test
    public void hasAsyncUpdate_shouldReturnTrue() {
        assertThat(mNfcController.hasAsyncUpdate()).isTrue();
    }

    @Test
    public void isToggleableInAirplaneMode_containNfc_shouldReturnTrue() {
        Settings.Global.putString(mContext.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_TOGGLEABLE_RADIOS,
                Settings.Global.RADIO_NFC);
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 1);

        assertThat(NfcPreferenceController.isToggleableInAirplaneMode(mContext)).isTrue();
    }

    @Test
    public void isToggleableInAirplaneMode_withoutNfc_shouldReturnFalse() {
        Settings.Global.putString(mContext.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_TOGGLEABLE_RADIOS,
                "null");
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 1);

        assertThat(NfcPreferenceController.isToggleableInAirplaneMode(mContext)).isFalse();
    }
}
