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

package com.android.settings.sound;

import static android.media.AudioSystem.DEVICE_OUT_BLUETOOTH_A2DP;
import static android.media.AudioSystem.DEVICE_OUT_EARPIECE;
import static android.media.AudioSystem.DEVICE_OUT_HEARING_AID;
import static android.media.AudioSystem.DEVICE_OUT_REMOTE_SUBMIX;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.bluetooth.Utils;
import com.android.settings.testutils.shadow.ShadowAudioManager;
import com.android.settings.testutils.shadow.ShadowBluetoothUtils;
import com.android.settingslib.bluetooth.A2dpProfile;
import com.android.settingslib.bluetooth.BluetoothEventManager;
import com.android.settingslib.bluetooth.HearingAidProfile;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.media.MediaOutputSliceConstants;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowBluetoothDevice;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowAudioManager.class,
        ShadowBluetoothUtils.class,
        ShadowBluetoothDevice.class}
)
public class MediaOutputPreferenceControllerTest {
    private static final String TEST_KEY = "Test_Key";
    private static final String TEST_DEVICE_NAME_1 = "Test_A2DP_BT_Device_NAME_1";
    private static final String TEST_DEVICE_NAME_2 = "Test_A2DP_BT_Device_NAME_2";
    private static final String TEST_HAP_DEVICE_NAME_1 = "Test_HAP_BT_Device_NAME_1";
    private static final String TEST_HAP_DEVICE_NAME_2 = "Test_HAP_BT_Device_NAME_2";
    private static final String TEST_DEVICE_ADDRESS_1 = "00:A1:A1:A1:A1:A1";
    private static final String TEST_DEVICE_ADDRESS_2 = "00:B2:B2:B2:B2:B2";
    private static final String TEST_DEVICE_ADDRESS_3 = "00:C3:C3:C3:C3:C3";
    private static final String TEST_DEVICE_ADDRESS_4 = "00:D4:D4:D4:D4:D4";

    @Mock
    private LocalBluetoothManager mLocalManager;
    @Mock
    private BluetoothEventManager mBluetoothEventManager;
    @Mock
    private LocalBluetoothProfileManager mLocalBluetoothProfileManager;
    @Mock
    private A2dpProfile mA2dpProfile;
    @Mock
    private HearingAidProfile mHearingAidProfile;
    @Mock
    private AudioSwitchPreferenceController.AudioSwitchCallback mAudioSwitchPreferenceCallback;

    private Context mContext;
    private PreferenceScreen mScreen;
    private Preference mPreference;
    private AudioManager mAudioManager;
    private ShadowAudioManager mShadowAudioManager;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mBluetoothDevice;
    private BluetoothDevice mSecondBluetoothDevice;
    private BluetoothDevice mLeftBluetoothHapDevice;
    private BluetoothDevice mRightBluetoothHapDevice;
    private LocalBluetoothManager mLocalBluetoothManager;
    private MediaOutputPreferenceController mController;
    private List<BluetoothDevice> mProfileConnectedDevices;
    private List<BluetoothDevice> mHearingAidActiveDevices;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);

        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mShadowAudioManager = ShadowAudioManager.getShadow();

        ShadowBluetoothUtils.sLocalBluetoothManager = mLocalManager;
        mLocalBluetoothManager = Utils.getLocalBtManager(mContext);

        when(mLocalBluetoothManager.getEventManager()).thenReturn(mBluetoothEventManager);
        when(mLocalBluetoothManager.getProfileManager()).thenReturn(mLocalBluetoothProfileManager);
        when(mLocalBluetoothProfileManager.getA2dpProfile()).thenReturn(mA2dpProfile);
        when(mLocalBluetoothProfileManager.getHearingAidProfile()).thenReturn(mHearingAidProfile);

        mBluetoothManager = new BluetoothManager(mContext);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        mBluetoothDevice = spy(mBluetoothAdapter.getRemoteDevice(TEST_DEVICE_ADDRESS_1));
        when(mBluetoothDevice.getName()).thenReturn(TEST_DEVICE_NAME_1);
        when(mBluetoothDevice.isConnected()).thenReturn(true);

        mSecondBluetoothDevice = spy(mBluetoothAdapter.getRemoteDevice(TEST_DEVICE_ADDRESS_2));
        when(mSecondBluetoothDevice.getName()).thenReturn(TEST_DEVICE_NAME_2);
        when(mSecondBluetoothDevice.isConnected()).thenReturn(true);

        mLeftBluetoothHapDevice = spy(mBluetoothAdapter.getRemoteDevice(TEST_DEVICE_ADDRESS_3));
        when(mLeftBluetoothHapDevice.getName()).thenReturn(TEST_HAP_DEVICE_NAME_1);
        when(mLeftBluetoothHapDevice.isConnected()).thenReturn(true);

        mRightBluetoothHapDevice = spy(mBluetoothAdapter.getRemoteDevice(TEST_DEVICE_ADDRESS_4));
        when(mRightBluetoothHapDevice.getName()).thenReturn(TEST_HAP_DEVICE_NAME_2);
        when(mRightBluetoothHapDevice.isConnected()).thenReturn(true);

        mController = new MediaOutputPreferenceController(mContext, TEST_KEY);
        mScreen = spy(new PreferenceScreen(mContext, null));
        mPreference = new Preference(mContext);
        mProfileConnectedDevices = new ArrayList<>();
        mHearingAidActiveDevices = new ArrayList<>(2);

        when(mScreen.getPreferenceManager()).thenReturn(mock(PreferenceManager.class));
        when(mScreen.getContext()).thenReturn(mContext);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        mScreen.addPreference(mPreference);
        mController.displayPreference(mScreen);
        mController.setCallback(mAudioSwitchPreferenceCallback);
    }

    @After
    public void tearDown() {
        ShadowBluetoothUtils.reset();
    }


    /**
     * A2DP Bluetooth device(s) are not connected nor previously connected
     * Preference should be invisible
     */
    @Test
    public void updateState_withoutConnectedBtDevice_preferenceInvisible() {
        mShadowAudioManager.setOutputDevice(DEVICE_OUT_EARPIECE);
        mAudioManager.setMode(AudioManager.MODE_NORMAL);
        mProfileConnectedDevices.clear();
        when(mA2dpProfile.getConnectedDevices()).thenReturn(mProfileConnectedDevices);
        mPreference.setVisible(true);

        assertThat(mPreference.isVisible()).isTrue();
        mController.updateState(mPreference);
        assertThat(mPreference.isVisible()).isFalse();
    }

    /**
     * A2DP Bluetooth device(s) are connected, no matter active or inactive
     * Preference should be visible
     */
    @Test
    public void updateState_withConnectedBtDevice_preferenceVisible() {
        mShadowAudioManager.setOutputDevice(DEVICE_OUT_BLUETOOTH_A2DP);
        mAudioManager.setMode(AudioManager.MODE_NORMAL);
        mProfileConnectedDevices.clear();
        mProfileConnectedDevices.add(mBluetoothDevice);
        when(mA2dpProfile.getConnectedDevices()).thenReturn(mProfileConnectedDevices);
        assertThat(mPreference.isVisible()).isFalse();

        // Without Active Bluetooth Device
        mController.updateState(mPreference);
        assertThat(mPreference.isVisible()).isTrue();

        // With Active Bluetooth Device
        when(mA2dpProfile.getActiveDevice()).thenReturn(mBluetoothDevice);
        mController.updateState(mPreference);
        assertThat(mPreference.isVisible()).isTrue();
    }

    /**
     * A2DP Bluetooth device(s) are connected, but no device is set as activated
     * Preference summary should be "This device"
     */
    @Test
    public void updateState_withConnectedBtDevice_withoutActiveBtDevice_setDefaultSummary() {
        mShadowAudioManager.setOutputDevice(DEVICE_OUT_EARPIECE);
        mAudioManager.setMode(AudioManager.MODE_NORMAL);
        mProfileConnectedDevices.clear();
        mProfileConnectedDevices.add(mBluetoothDevice);
        mProfileConnectedDevices.add(mSecondBluetoothDevice);
        when(mA2dpProfile.getConnectedDevices()).thenReturn(mProfileConnectedDevices);
        when(mA2dpProfile.getActiveDevice()).thenReturn(null);

        assertThat(mPreference.getSummary()).isNull();
        mController.updateState(mPreference);
        assertThat(mPreference.getSummary()).isEqualTo(
                mContext.getText(R.string.media_output_default_summary));
    }

    /**
     * A2DP Bluetooth device(s) are connected and active
     * Preference summary should be device's name
     */
    @Test
    public void updateState_withActiveBtDevice_setActivatedDeviceName() {
        mShadowAudioManager.setOutputDevice(DEVICE_OUT_BLUETOOTH_A2DP);
        mAudioManager.setMode(AudioManager.MODE_NORMAL);
        mProfileConnectedDevices.clear();
        mProfileConnectedDevices.add(mBluetoothDevice);
        mProfileConnectedDevices.add(mSecondBluetoothDevice);
        when(mA2dpProfile.getConnectedDevices()).thenReturn(mProfileConnectedDevices);
        when(mA2dpProfile.getActiveDevice()).thenReturn(mBluetoothDevice);

        assertThat(mPreference.getSummary()).isNull();
        mController.updateState(mPreference);
        assertThat(mPreference.getSummary()).isEqualTo(TEST_DEVICE_NAME_1);
    }


    /**
     * Hearing Aid device(s) are connected, no matter active or inactive
     * Preference should be visible
     */
    @Test
    public void updateState_withConnectedHADevice_preferenceVisible() {
        mShadowAudioManager.setOutputDevice(DEVICE_OUT_HEARING_AID);
        mAudioManager.setMode(AudioManager.MODE_NORMAL);
        mHearingAidActiveDevices.clear();
        mHearingAidActiveDevices.add(mLeftBluetoothHapDevice);
        when(mHearingAidProfile.getConnectedDevices()).thenReturn(mHearingAidActiveDevices);
        assertThat(mPreference.isVisible()).isFalse();

        // Without Active Hearing Aid Device
        mController.updateState(mPreference);
        assertThat(mPreference.isVisible()).isTrue();

        // With Active Hearing Aid Device
        when(mHearingAidProfile.getActiveDevices()).thenReturn(mHearingAidActiveDevices);
        mController.updateState(mPreference);
        assertThat(mPreference.isVisible()).isTrue();
    }

    /**
     * Hearing Aid device(s) are connected and active
     * Preference summary should be device's name
     */
    @Test
    public void updateState_withActiveHADevice_setActivatedDeviceName() {
        mShadowAudioManager.setOutputDevice(DEVICE_OUT_HEARING_AID);
        mAudioManager.setMode(AudioManager.MODE_NORMAL);
        mHearingAidActiveDevices.clear();
        mHearingAidActiveDevices.add(mLeftBluetoothHapDevice);
        when(mHearingAidProfile.getConnectedDevices()).thenReturn(mHearingAidActiveDevices);
        when(mHearingAidProfile.getActiveDevices()).thenReturn(mHearingAidActiveDevices);

        assertThat(mPreference.getSummary()).isNull();
        mController.updateState(mPreference);
        assertThat(mPreference.getSummary()).isEqualTo(TEST_HAP_DEVICE_NAME_1);

    }

    @Test
    public void click_launch_outputSwitcherSlice() {
        final ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        mController.handlePreferenceTreeClick(mPreference);
        verify(mContext, never()).startActivity(intentCaptor.capture());

        mPreference.setKey(TEST_KEY);
        mController.handlePreferenceTreeClick(mPreference);
        verify(mContext).startActivity(intentCaptor.capture());
        assertThat(intentCaptor.getValue().getAction())
                .isEqualTo(MediaOutputSliceConstants.ACTION_MEDIA_OUTPUT);
    }

    /**
     * Default status
     * Preference should be invisible
     * Summary should be default summary
     */
    @Test
    public void updateState_shouldSetSummary() {
        mController.updateState(mPreference);

        assertThat(mPreference.isVisible()).isFalse();
        assertThat(mPreference.getSummary()).isEqualTo(
                mContext.getText(R.string.media_output_default_summary));
    }

    /**
     * During a call
     * Preference should be invisible
     * Default string should be "Unavailable during calls"
     */
    @Test
    public void updateState_duringACall_shouldSetDefaultSummary() {
        mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);

        mController.updateState(mPreference);

        assertThat(mPreference.isVisible()).isFalse();
        assertThat(mPreference.getSummary()).isEqualTo(
                mContext.getText(R.string.media_out_summary_ongoing_call_state));
    }

    @Test
    public void findActiveDevice_onlyA2dpDeviceActive_returnA2dpDevice() {
        when(mLocalBluetoothProfileManager.getHearingAidProfile()).thenReturn(null);
        when(mA2dpProfile.getActiveDevice()).thenReturn(mBluetoothDevice);

        assertThat(mController.findActiveDevice()).isEqualTo(mBluetoothDevice);
    }

    @Test
    public void findActiveDevice_allDevicesNotActive_returnNull() {
        when(mLocalBluetoothProfileManager.getHearingAidProfile()).thenReturn(null);
        when(mA2dpProfile.getActiveDevice()).thenReturn(null);

        assertThat(mController.findActiveDevice()).isNull();
    }
}
