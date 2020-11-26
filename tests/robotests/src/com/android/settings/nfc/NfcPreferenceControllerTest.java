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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.os.UserManager;
import android.provider.Settings;

import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.settings.nfc.NfcPreferenceController.NfcSliceWorker;
import com.android.settings.nfc.NfcPreferenceController.NfcSliceWorker.NfcUpdateReceiver;
import com.android.settings.testutils.shadow.ShadowNfcAdapter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowNfcAdapter.class)
public class NfcPreferenceControllerTest {

    @Mock
    NfcManager mManager;
    @Mock
    private UserManager mUserManager;
    @Mock
    private PreferenceScreen mScreen;

    private Context mContext;
    private SwitchPreference mNfcPreference;
    private NfcPreferenceController mNfcController;
    private ShadowNfcAdapter mShadowNfcAdapter;
    private NfcAdapter mNfcAdapter;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mShadowNfcAdapter = Shadow.extract(NfcAdapter.getDefaultAdapter(mContext));
        mNfcAdapter = NfcAdapter.getDefaultAdapter(mContext);

        when(mContext.getApplicationContext()).thenReturn(mContext);
        when(mContext.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
        when(mContext.getSystemService(Context.NFC_SERVICE)).thenReturn(mManager);

        mNfcController = new NfcPreferenceController(mContext,
                NfcPreferenceController.KEY_TOGGLE_NFC);
        mNfcPreference = new SwitchPreference(RuntimeEnvironment.application);

        when(mScreen.findPreference(mNfcController.getPreferenceKey())).thenReturn(mNfcPreference);
    }

    @Test
    public void getAvailabilityStatus_hasNfc_shouldReturnAvailable() {
        mShadowNfcAdapter.setEnabled(true);
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
        mNfcController.displayPreference(mScreen);
        mShadowNfcAdapter.setAdapterState(NfcAdapter.STATE_ON);
        mNfcController.onResume();
        assertThat(mNfcPreference.isEnabled()).isTrue();

        mShadowNfcAdapter.setAdapterState(NfcAdapter.STATE_OFF);
        mNfcController.onResume();
        assertThat(mNfcPreference.isEnabled()).isTrue();
    }

    @Test
    public void isNfcEnable_nfcStateTurning_shouldReturnFalse() {
        mNfcController.displayPreference(mScreen);
        mShadowNfcAdapter.setAdapterState(NfcAdapter.STATE_TURNING_ON);
        mNfcController.onResume();
        assertThat(mNfcPreference.isEnabled()).isFalse();

        mShadowNfcAdapter.setAdapterState(NfcAdapter.STATE_TURNING_OFF);
        mNfcController.onResume();
        assertThat(mNfcPreference.isEnabled()).isFalse();
    }

    @Test
    public void isNfcChecked_nfcStateOn_shouldReturnTrue() {
        mNfcController.displayPreference(mScreen);
        mShadowNfcAdapter.setAdapterState(NfcAdapter.STATE_ON);
        mNfcController.onResume();
        assertThat(mNfcPreference.isChecked()).isTrue();

        mShadowNfcAdapter.setAdapterState(NfcAdapter.STATE_TURNING_ON);
        mNfcController.onResume();
        assertThat(mNfcPreference.isChecked()).isTrue();
    }

    @Test
    public void isNfcChecked_nfcStateOff_shouldReturnFalse() {
        mShadowNfcAdapter.setAdapterState(NfcAdapter.STATE_OFF);
        mNfcController.onResume();
        assertThat(mNfcPreference.isChecked()).isFalse();

        mShadowNfcAdapter.setAdapterState(NfcAdapter.STATE_TURNING_OFF);
        mNfcController.onResume();
        assertThat(mNfcPreference.isChecked()).isFalse();
    }

    @Test
    public void updateNonIndexableKeys_available_shouldNotUpdate() {
        mShadowNfcAdapter.setEnabled(true);
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

        assertThat(mNfcAdapter.isEnabled()).isTrue();
    }

    @Test
    public void setChecked_False_nfcShouldDisable() {
        mNfcController.setChecked(false);
        mNfcController.onResume();

        assertThat(mNfcAdapter.isEnabled()).isFalse();
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

    @Test
    public void shouldTurnOffNFCInAirplaneMode_airplaneModeRadiosContainsNfc_shouldReturnTrue() {
        Settings.Global.putString(mContext.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_RADIOS, Settings.Global.RADIO_NFC);

        assertThat(NfcPreferenceController.shouldTurnOffNFCInAirplaneMode(mContext)).isTrue();
    }

    @Test
    public void shouldTurnOffNFCInAirplaneMode_airplaneModeRadiosWithoutNfc_shouldReturnFalse() {
        Settings.Global.putString(mContext.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_RADIOS, "");

        assertThat(NfcPreferenceController.shouldTurnOffNFCInAirplaneMode(mContext)).isFalse();
    }

    @Test
    public void ncfSliceWorker_nfcBroadcast_noExtra_sliceDoesntUpdate() {
        final NfcSliceWorker worker = spy(
                new NfcSliceWorker(mContext, mNfcController.getSliceUri()));
        final NfcUpdateReceiver receiver = worker.new NfcUpdateReceiver(worker);
        final Intent triggerIntent = new Intent(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED);

        receiver.onReceive(mContext, triggerIntent);

        verify(worker, times(0)).updateSlice();
    }

    @Test
    public void ncfSliceWorker_nfcBroadcast_turningOn_sliceDoesntUpdate() {
        final NfcSliceWorker worker = spy(
                new NfcSliceWorker(mContext, mNfcController.getSliceUri()));
        final NfcUpdateReceiver receiver = worker.new NfcUpdateReceiver(worker);
        final Intent triggerIntent = new Intent(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED);
        triggerIntent.putExtra(NfcAdapter.EXTRA_ADAPTER_STATE, NfcAdapter.STATE_TURNING_ON);

        receiver.onReceive(mContext, triggerIntent);

        verify(worker, times(0)).updateSlice();
    }

    @Test
    public void ncfSliceWorker_nfcBroadcast_turningOff_sliceDoesntUpdate() {
        final NfcSliceWorker worker = spy(
                new NfcSliceWorker(mContext, mNfcController.getSliceUri()));
        final NfcUpdateReceiver receiver = worker.new NfcUpdateReceiver(worker);
        final Intent triggerIntent = new Intent(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED);
        triggerIntent.putExtra(NfcAdapter.EXTRA_ADAPTER_STATE, NfcAdapter.STATE_TURNING_OFF);

        receiver.onReceive(mContext, triggerIntent);

        verify(worker, times(0)).updateSlice();
    }

    @Test
    public void ncfSliceWorker_nfcBroadcast_nfcOn_sliceUpdates() {
        final NfcSliceWorker worker = spy(
                new NfcSliceWorker(mContext, mNfcController.getSliceUri()));
        final NfcUpdateReceiver receiver = worker.new NfcUpdateReceiver(worker);
        final Intent triggerIntent = new Intent(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED);
        triggerIntent.putExtra(NfcAdapter.EXTRA_ADAPTER_STATE, NfcAdapter.STATE_ON);

        receiver.onReceive(mContext, triggerIntent);

        verify(worker).updateSlice();
    }

    @Test
    public void ncfSliceWorker_nfcBroadcast_nfcOff_sliceUpdates() {
        final NfcSliceWorker worker = spy(
                new NfcSliceWorker(mContext, mNfcController.getSliceUri()));
        final NfcUpdateReceiver receiver = worker.new NfcUpdateReceiver(worker);
        final Intent triggerIntent = new Intent(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED);
        triggerIntent.putExtra(NfcAdapter.EXTRA_ADAPTER_STATE, NfcAdapter.STATE_OFF);

        receiver.onReceive(mContext, triggerIntent);

        verify(worker).updateSlice();
    }

    @Test
    public void isPublicSlice_returnsTrue() {
        assertThat(mNfcController.isPublicSlice()).isTrue();
    }
}
