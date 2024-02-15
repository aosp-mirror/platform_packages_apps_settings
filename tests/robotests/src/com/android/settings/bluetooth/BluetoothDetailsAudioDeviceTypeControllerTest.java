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

package com.android.settings.bluetooth;

import static android.bluetooth.BluetoothDevice.DEVICE_TYPE_LE;
import static android.media.AudioManager.AUDIO_DEVICE_CATEGORY_SPEAKER;
import static android.media.audio.Flags.automaticBtDeviceType;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothDevice;
import android.media.AudioManager;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceCategory;

import com.android.settingslib.bluetooth.LeAudioProfile;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class BluetoothDetailsAudioDeviceTypeControllerTest extends
        BluetoothDetailsControllerTestBase {

    private static final String MAC_ADDRESS = "04:52:C7:0B:D8:3C";
    private static final String KEY_BT_AUDIO_DEVICE_TYPE = "bluetooth_audio_device_type";

    @Mock
    private AudioManager mAudioManager;
    @Mock
    private Lifecycle mAudioDeviceTypeLifecycle;
    @Mock
    private PreferenceCategory mProfilesContainer;
    @Mock
    private BluetoothDevice mBluetoothDevice;
    @Mock
    private LocalBluetoothManager mManager;
    @Mock
    private LocalBluetoothProfileManager mProfileManager;
    @Mock
    private LeAudioProfile mLeAudioProfile;
    private BluetoothDetailsAudioDeviceTypeController mController;
    private ListPreference mAudioDeviceTypePref;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getSystemService(AudioManager.class)).thenReturn(mAudioManager);
        when(mCachedDevice.getAddress()).thenReturn(MAC_ADDRESS);
        when(mCachedDevice.getDevice()).thenReturn(mBluetoothDevice);
        when(mBluetoothDevice.getAnonymizedAddress()).thenReturn(MAC_ADDRESS);
        when(mBluetoothDevice.getType()).thenReturn(DEVICE_TYPE_LE);
        when(mManager.getProfileManager()).thenReturn(mProfileManager);
        when(mProfileManager.getLeAudioProfile()).thenReturn(mLeAudioProfile);
        when(mLeAudioProfile.isEnabled(mCachedDevice.getDevice())).thenReturn(true);

        mController = new BluetoothDetailsAudioDeviceTypeController(mContext, mFragment, mManager,
                mCachedDevice, mAudioDeviceTypeLifecycle);
        mController.mProfilesContainer = mProfilesContainer;

        mController.createAudioDeviceTypePreference(mContext);
        mAudioDeviceTypePref = mController.getAudioDeviceTypePreference();

        when(mProfilesContainer.findPreference(KEY_BT_AUDIO_DEVICE_TYPE)).thenReturn(
                mAudioDeviceTypePref);
    }

    @Test
    public void createAudioDeviceTypePreference_btDeviceIsCategorized_checkSelection() {
        int deviceType = AUDIO_DEVICE_CATEGORY_SPEAKER;
        if (automaticBtDeviceType()) {
            when(mAudioManager.getBluetoothAudioDeviceCategory(MAC_ADDRESS)).thenReturn(deviceType);
        } else {
            when(mAudioManager.getBluetoothAudioDeviceCategory_legacy(MAC_ADDRESS, /*isBle=*/
                    true)).thenReturn(deviceType);
        }

        mController.createAudioDeviceTypePreference(mContext);
        mAudioDeviceTypePref = mController.getAudioDeviceTypePreference();

        assertThat(mAudioDeviceTypePref.getValue()).isEqualTo(Integer.toString(deviceType));
    }

    @Test
    public void selectDeviceTypeSpeaker_invokeSetBluetoothAudioDeviceType() {
        int deviceType = AUDIO_DEVICE_CATEGORY_SPEAKER;
        mAudioDeviceTypePref.setValue(Integer.toString(deviceType));

        mController.onPreferenceChange(mAudioDeviceTypePref, Integer.toString(deviceType));

        if (automaticBtDeviceType()) {
            verify(mAudioManager).setBluetoothAudioDeviceCategory(eq(MAC_ADDRESS),
                    eq(AUDIO_DEVICE_CATEGORY_SPEAKER));
        } else {
            verify(mAudioManager).setBluetoothAudioDeviceCategory_legacy(eq(MAC_ADDRESS), eq(true),
                    eq(AUDIO_DEVICE_CATEGORY_SPEAKER));
        }
    }
}
