/*
 * Copyright (C) 2024 The Android Open Source Project
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

import static com.android.settings.bluetooth.BluetoothDetailsHearingDeviceController.KEY_HEARING_DEVICE_GROUP;
import static com.android.settings.bluetooth.BluetoothDetailsHearingDeviceInputRoutingController.KEY_HEARING_DEVICE_INPUT_ROUTING;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothDevice;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.bluetooth.HearingDeviceInputRoutingPreference.InputRoutingValue;
import com.android.settingslib.bluetooth.HapClientProfile;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

import java.util.Collections;
import java.util.List;

/** Tests for {@link BluetoothDetailsHearingDeviceInputRoutingController}. */

@RunWith(RobolectricTestRunner.class)
public class BluetoothDetailsHearingDeviceInputRoutingControllerTest extends
        BluetoothDetailsControllerTestBase {
    @Rule
    public final MockitoRule mockito = MockitoJUnit.rule();

    private static final String TEST_ADDRESS = "55:66:77:88:99:AA";

    @Mock
    private BluetoothDevice mBluetoothDevice;
    @Mock
    private HapClientProfile mHapClientProfile;
    @Spy
    private AudioManager mAudioManager;

    private BluetoothDetailsHearingDeviceInputRoutingController mController;

    @Override
    public void setUp() {
        super.setUp();

        mContext = spy(ApplicationProvider.getApplicationContext());
        mAudioManager = spy(mContext.getSystemService(AudioManager.class));
        when(mContext.getSystemService(AudioManager.class)).thenReturn(mAudioManager);
        setupDevice(makeDefaultDeviceConfig());
        when(mCachedDevice.getDevice()).thenReturn(mBluetoothDevice);
        PreferenceCategory deviceControls = new PreferenceCategory(mContext);
        deviceControls.setKey(KEY_HEARING_DEVICE_GROUP);
        mScreen.addPreference(deviceControls);
        mController = new BluetoothDetailsHearingDeviceInputRoutingController(mContext,
                mFragment, mCachedDevice, mLifecycle);
    }

    @Test
    public void init_getExpectedPreference() {
        mController.init(mScreen);

        Preference pref = mScreen.findPreference(KEY_HEARING_DEVICE_INPUT_ROUTING);
        assertThat(pref.getKey()).isEqualTo(KEY_HEARING_DEVICE_INPUT_ROUTING);
    }

    @Test
    public void init_setPreferredMicrophoneTrue_expectedSummary() {
        when(mBluetoothDevice.isMicrophonePreferredForCalls()).thenReturn(true);

        mController.init(mScreen);

        Preference pref = mScreen.findPreference(KEY_HEARING_DEVICE_INPUT_ROUTING);
        assertThat(pref.getSummary().toString()).isEqualTo(mContext.getString(
                R.string.bluetooth_hearing_device_input_routing_hearing_device_option));
    }

    @Test
    public void init_setPreferredMicrophoneFalse_expectedSummary() {
        when(mBluetoothDevice.isMicrophonePreferredForCalls()).thenReturn(false);
        mController.init(mScreen);

        Preference pref = mScreen.findPreference(KEY_HEARING_DEVICE_INPUT_ROUTING);
        assertThat(pref.getSummary().toString()).isEqualTo(mContext.getString(
                R.string.bluetooth_hearing_device_input_routing_builtin_option));
    }

    @Test
    public void onInputRoutingUpdated_hearingDevice_setMicrophonePreferredForCallsTrue() {
        mController.init(mScreen);

        mController.onInputRoutingUpdated(InputRoutingValue.HEARING_DEVICE);

        verify(mBluetoothDevice).setMicrophonePreferredForCalls(true);
    }

    @Test
    public void onInputRoutingUpdated_builtin_setMicrophonePreferredForCallsFalse() {
        mController.init(mScreen);

        mController.onInputRoutingUpdated(InputRoutingValue.BUILTIN_MIC);

        verify(mBluetoothDevice).setMicrophonePreferredForCalls(false);
    }

    @Test
    public void isAvailable_validInput_supportHapProfile_returnTrue() {
        when(mCachedDevice.getAddress()).thenReturn(TEST_ADDRESS);
        AudioDeviceInfo[] mockInfo = new AudioDeviceInfo[] {mockTestAddressInfo(TEST_ADDRESS)};
        when(mAudioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)).thenReturn(mockInfo);
        when(mCachedDevice.getProfiles()).thenReturn(List.of(mHapClientProfile));

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_notSupportHapProfile_returnFalse() {
        when(mCachedDevice.getAddress()).thenReturn(TEST_ADDRESS);
        AudioDeviceInfo[] mockInfo = new AudioDeviceInfo[] {mockTestAddressInfo(TEST_ADDRESS)};
        when(mAudioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)).thenReturn(mockInfo);
        when(mCachedDevice.getProfiles()).thenReturn(Collections.emptyList());

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_notValidInputDevice_returnFalse() {
        when(mCachedDevice.getAddress()).thenReturn(TEST_ADDRESS);
        when(mAudioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)).thenReturn(
                new AudioDeviceInfo[] {});
        when(mCachedDevice.getProfiles()).thenReturn(List.of(mHapClientProfile));

        assertThat(mController.isAvailable()).isFalse();
    }

    private AudioDeviceInfo mockTestAddressInfo(String address) {
        final AudioDeviceInfo info = mock(AudioDeviceInfo.class);
        when(info.getType()).thenReturn(AudioDeviceInfo.TYPE_BLE_HEADSET);
        when(info.getAddress()).thenReturn(address);
        return info;
    }
}
