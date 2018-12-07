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
import static android.media.AudioSystem.DEVICE_OUT_HEARING_AID;
import static android.media.AudioSystem.DEVICE_OUT_REMOTE_SUBMIX;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.media.AudioManager;

import androidx.preference.ListPreference;
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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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
    private final static long HISYNCID1 = 10;
    private final static long HISYNCID2 = 11;

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
    private ListPreference mPreference;
    private AudioManager mAudioManager;
    private ShadowAudioManager mShadowAudioManager;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mBluetoothDevice;
    private BluetoothDevice mSecondBluetoothDevice;
    private BluetoothDevice mLeftBluetoothHapDevice;
    private BluetoothDevice mRightBluetoothHapDevice;
    private LocalBluetoothManager mLocalBluetoothManager;
    private AudioSwitchPreferenceController mController;
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
        mPreference = new ListPreference(mContext);
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
     * In normal mode, bluetooth device with HisyncId.
     * HearingAidProfile should set active device to this device.
     */
    @Test
    public void setActiveBluetoothDevice_btDeviceWithHisyncId_shouldSetBtDeviceActive() {
        mAudioManager.setMode(AudioManager.MODE_NORMAL);
        when(mHearingAidProfile.getHiSyncId(mLeftBluetoothHapDevice)).thenReturn(HISYNCID1);

        mController.setActiveBluetoothDevice(mLeftBluetoothHapDevice);

        verify(mHearingAidProfile).setActiveDevice(mLeftBluetoothHapDevice);
        verify(mA2dpProfile, never()).setActiveDevice(mLeftBluetoothHapDevice);
    }

    /**
     * In normal mode, bluetooth device without HisyncId.
     * A2dpProfile should set active device to this device.
     */
    @Test
    public void setActiveBluetoothDevice_btDeviceWithoutHisyncId_shouldSetBtDeviceActive() {
        mAudioManager.setMode(AudioManager.MODE_NORMAL);

        mController.setActiveBluetoothDevice(mBluetoothDevice);

        verify(mA2dpProfile).setActiveDevice(mBluetoothDevice);
        verify(mHearingAidProfile, never()).setActiveDevice(mBluetoothDevice);
    }

    /**
     * In normal mode, set active device to "this device".
     * A2dpProfile should set to null.
     * HearingAidProfile should set to null.
     */
    @Test
    public void setActiveBluetoothDevice_setNull_shouldSetNullToBothProfiles() {
        mAudioManager.setMode(AudioManager.MODE_NORMAL);

        mController.setActiveBluetoothDevice(null);

        verify(mA2dpProfile).setActiveDevice(null);
        verify(mHearingAidProfile).setActiveDevice(null);
    }

    /**
     * During a call
     * A2dpProfile should not set active device.
     */
    @Test
    public void setActiveBluetoothDevice_duringACall_shouldNotSetActiveDeviceToA2dpProfile() {
        mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);

        mController.setActiveBluetoothDevice(mBluetoothDevice);

        verify(mA2dpProfile, times(0)).setActiveDevice(any(BluetoothDevice.class));
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

    /**
     * No available A2dp BT devices:
     * Preference should be invisible
     * Preference summary should be "This device"
     */
    @Test
    public void updateState_noAvailableA2dpBtDevices_shouldDisableAndSetDefaultSummary() {
        mAudioManager.setMode(AudioManager.MODE_NORMAL);
        List<BluetoothDevice> emptyDeviceList = new ArrayList<>();
        when(mA2dpProfile.getConnectedDevices()).thenReturn(emptyDeviceList);

        mController.updateState(mPreference);

        assertThat(mPreference.isVisible()).isFalse();
        String defaultString = mContext.getString(R.string.media_output_default_summary);
        assertThat(mPreference.getSummary()).isEqualTo(defaultString);
    }

    /**
     * Media stream is captured by something else (cast device):
     * Preference should be invisible
     * Preference summary should be "unavailable"
     */
    @Test
    public void updateState_mediaStreamIsCapturedByCast_shouldDisableAndSetDefaultSummary() {
        mShadowAudioManager.setOutputDevice(DEVICE_OUT_REMOTE_SUBMIX);

        mController.updateState(mPreference);

        assertThat(mPreference.isVisible()).isFalse();
        String defaultString = mContext.getString(R.string.media_output_summary_unavailable);
        assertThat(mPreference.getSummary()).isEqualTo(defaultString);
    }

    /**
     * One A2DP Bluetooth device is available and active.
     * Preference should be visible
     * Preference summary should be the activated device name
     */
    @Test
    public void updateState_oneA2dpBtDeviceAreAvailable_shouldSetActivatedDeviceName() {
        mAudioManager.setMode(AudioManager.MODE_NORMAL);
        mShadowAudioManager.setOutputDevice(DEVICE_OUT_BLUETOOTH_A2DP);
        mProfileConnectedDevices.clear();
        mProfileConnectedDevices.add(mBluetoothDevice);
        when(mA2dpProfile.getConnectedDevices()).thenReturn(mProfileConnectedDevices);
        when(mA2dpProfile.getActiveDevice()).thenReturn(mBluetoothDevice);

        mController.updateState(mPreference);

        assertThat(mPreference.isVisible()).isTrue();
        assertThat(mPreference.getSummary()).isEqualTo(TEST_DEVICE_NAME_1);
    }

    /**
     * More than one A2DP Bluetooth devices are available, and second device is active.
     * Preference should be visible
     * Preference summary should be the activated device name
     */
    @Test
    public void updateState_moreThanOneA2DpBtDevicesAreAvailable_shouldSetActivatedDeviceName() {
        mAudioManager.setMode(AudioManager.MODE_NORMAL);
        mShadowAudioManager.setOutputDevice(DEVICE_OUT_BLUETOOTH_A2DP);
        mProfileConnectedDevices.clear();
        mProfileConnectedDevices.add(mBluetoothDevice);
        mProfileConnectedDevices.add(mSecondBluetoothDevice);
        when(mA2dpProfile.getConnectedDevices()).thenReturn(mProfileConnectedDevices);
        when(mA2dpProfile.getActiveDevice()).thenReturn(mSecondBluetoothDevice);

        mController.updateState(mPreference);

        assertThat(mPreference.isVisible()).isTrue();
        assertThat(mPreference.getSummary()).isEqualTo(TEST_DEVICE_NAME_2);
    }

    /**
     * A2DP Bluetooth device(s) are available, but wired headset is plugged in and activated
     * Preference should be visible
     * Preference summary should be "This device"
     */
    @Test
    public void updateState_a2dpDevicesAvailableWiredHeadsetIsActivated_shouldSetDefaultSummary() {
        mAudioManager.setMode(AudioManager.MODE_NORMAL);
        mProfileConnectedDevices.clear();
        mProfileConnectedDevices.add(mBluetoothDevice);
        when(mLocalBluetoothProfileManager.getHearingAidProfile()).thenReturn(null);
        when(mA2dpProfile.getConnectedDevices()).thenReturn(mProfileConnectedDevices);
        when(mA2dpProfile.getActiveDevice()).thenReturn(null);

        mController.updateState(mPreference);

        assertThat(mPreference.isVisible()).isTrue();
        assertThat(mPreference.getSummary()).isEqualTo(
                mContext.getString(R.string.media_output_default_summary));
    }


    /**
     * A2DP Bluetooth device(s) are available, but current device speaker is activated
     * Preference should be visible
     * Preference summary should be "This device"
     */
    @Test
    public void updateState_a2dpDevicesAvailableCurrentDeviceActivated_shouldSetDefaultSummary() {
        mAudioManager.setMode(AudioManager.MODE_NORMAL);
        mProfileConnectedDevices.clear();
        mProfileConnectedDevices.add(mBluetoothDevice);
        when(mA2dpProfile.getConnectedDevices()).thenReturn(mProfileConnectedDevices);
        when(mA2dpProfile.getActiveDevice()).thenReturn(null);

        mController.updateState(mPreference);

        assertThat(mPreference.isVisible()).isTrue();
        assertThat(mPreference.getSummary()).isEqualTo(
                mContext.getString(R.string.media_output_default_summary));
    }

    /**
     * One hearing aid profile Bluetooth device is available and active.
     * Preference should be visible
     * Preference summary should be the activated device name
     */
    @Test
    public void updateState_oneHapBtDeviceAreAvailable_shouldSetActivatedDeviceName() {
        mAudioManager.setMode(AudioManager.MODE_NORMAL);
        mShadowAudioManager.setOutputDevice(DEVICE_OUT_HEARING_AID);
        mProfileConnectedDevices.clear();
        mProfileConnectedDevices.add(mLeftBluetoothHapDevice);
        mHearingAidActiveDevices.clear();
        mHearingAidActiveDevices.add(mLeftBluetoothHapDevice);
        when(mHearingAidProfile.getConnectedDevices()).thenReturn(mProfileConnectedDevices);
        when(mHearingAidProfile.getActiveDevices()).thenReturn(mHearingAidActiveDevices);
        when(mHearingAidProfile.getHiSyncId(mLeftBluetoothHapDevice)).thenReturn(HISYNCID1);

        mController.updateState(mPreference);

        assertThat(mPreference.isVisible()).isTrue();
        assertThat(mPreference.getSummary()).isEqualTo(mLeftBluetoothHapDevice.getName());
    }

    /**
     * More than one hearing aid profile Bluetooth devices are available, and second
     * device is active.
     * Preference should be visible
     * Preference summary should be the activated device name
     */
    @Test
    public void updateState_moreThanOneHapBtDevicesAreAvailable_shouldSetActivatedDeviceName() {
        mAudioManager.setMode(AudioManager.MODE_NORMAL);
        mShadowAudioManager.setOutputDevice(DEVICE_OUT_HEARING_AID);
        mProfileConnectedDevices.clear();
        mProfileConnectedDevices.add(mLeftBluetoothHapDevice);
        mProfileConnectedDevices.add(mRightBluetoothHapDevice);
        mHearingAidActiveDevices.clear();
        mHearingAidActiveDevices.add(mRightBluetoothHapDevice);
        when(mHearingAidProfile.getConnectedDevices()).thenReturn(mProfileConnectedDevices);
        when(mHearingAidProfile.getActiveDevices()).thenReturn(mHearingAidActiveDevices);
        when(mHearingAidProfile.getHiSyncId(mLeftBluetoothHapDevice)).thenReturn(HISYNCID1);
        when(mHearingAidProfile.getHiSyncId(mRightBluetoothHapDevice)).thenReturn(HISYNCID2);

        mController.updateState(mPreference);

        assertThat(mPreference.isVisible()).isTrue();
        assertThat(mPreference.getSummary()).isEqualTo(mRightBluetoothHapDevice.getName());
    }

    /**
     * Both hearing aid profile and A2dp Bluetooth devices are available, and two hearing aid
     * profile devices with same HisyncId are active. Both of HAP device are active,
     * "left" side HAP device is added first.
     * Preference should be visible
     * Preference summary should be the activated device name
     * ConnectedDevice should not contain second HAP device with same HisyncId
     */
    @Test
    public void updateState_hapBtDeviceWithSameId_shouldSetActivatedDeviceName() {
        mAudioManager.setMode(AudioManager.MODE_NORMAL);
        mShadowAudioManager.setOutputDevice(DEVICE_OUT_HEARING_AID);
        mProfileConnectedDevices.clear();
        mProfileConnectedDevices.add(mBluetoothDevice);
        //with same HisyncId, first one will remain in UI.
        mProfileConnectedDevices.add(mLeftBluetoothHapDevice);
        mProfileConnectedDevices.add(mRightBluetoothHapDevice);
        mHearingAidActiveDevices.clear();
        mHearingAidActiveDevices.add(mLeftBluetoothHapDevice);
        mHearingAidActiveDevices.add(mRightBluetoothHapDevice);
        when(mHearingAidProfile.getConnectedDevices()).thenReturn(mProfileConnectedDevices);
        when(mHearingAidProfile.getActiveDevices()).thenReturn(mHearingAidActiveDevices);
        when(mHearingAidProfile.getHiSyncId(mLeftBluetoothHapDevice)).thenReturn(HISYNCID1);
        when(mHearingAidProfile.getHiSyncId(mRightBluetoothHapDevice)).thenReturn(HISYNCID1);

        mController.updateState(mPreference);

        assertThat(mPreference.isVisible()).isTrue();
        assertThat(mPreference.getSummary()).isEqualTo(mLeftBluetoothHapDevice.getName());
        assertThat(mController.mConnectedDevices.contains(mLeftBluetoothHapDevice)).isTrue();
        assertThat(mController.mConnectedDevices.contains(mRightBluetoothHapDevice)).isFalse();
    }

    /**
     * Both hearing aid profile and A2dp Bluetooth devices are available, and two hearing aid
     * profile devices with same HisyncId. Both of HAP device are active,
     * "right" side HAP device is added first.
     * Preference should be visible
     * Preference summary should be the activated device name
     * ConnectedDevice should not contain second HAP device with same HisyncId
     */
    @Test
    public void updateState_hapBtDeviceWithSameIdButDifferentOrder_shouldSetActivatedDeviceName() {
        mAudioManager.setMode(AudioManager.MODE_NORMAL);
        mShadowAudioManager.setOutputDevice(DEVICE_OUT_HEARING_AID);
        mProfileConnectedDevices.clear();
        mProfileConnectedDevices.add(mBluetoothDevice);
        //with same HisyncId, first one will remain in UI.
        mProfileConnectedDevices.add(mRightBluetoothHapDevice);
        mProfileConnectedDevices.add(mLeftBluetoothHapDevice);
        mHearingAidActiveDevices.clear();
        mHearingAidActiveDevices.add(mLeftBluetoothHapDevice);
        mHearingAidActiveDevices.add(mRightBluetoothHapDevice);
        when(mHearingAidProfile.getConnectedDevices()).thenReturn(mProfileConnectedDevices);
        when(mHearingAidProfile.getActiveDevices()).thenReturn(mHearingAidActiveDevices);
        when(mHearingAidProfile.getHiSyncId(mLeftBluetoothHapDevice)).thenReturn(HISYNCID1);
        when(mHearingAidProfile.getHiSyncId(mRightBluetoothHapDevice)).thenReturn(HISYNCID1);

        mController.updateState(mPreference);

        assertThat(mPreference.isVisible()).isTrue();
        assertThat(mPreference.getSummary()).isEqualTo(mRightBluetoothHapDevice.getName());
        assertThat(mController.mConnectedDevices.contains(mRightBluetoothHapDevice)).isTrue();
        assertThat(mController.mConnectedDevices.contains(mLeftBluetoothHapDevice)).isFalse();
    }

    /**
     * Both hearing aid profile and A2dp Bluetooth devices are available, and two hearing aid
     * profile devices with different HisyncId. One of HAP device is active.
     * Preference should be visible
     * Preference summary should be the activated device name
     * ConnectedDevice should contain both HAP device with different HisyncId
     */
    @Test
    public void updateState_hapBtDeviceWithDifferentId_shouldSetActivatedDeviceName() {
        mAudioManager.setMode(AudioManager.MODE_NORMAL);
        mShadowAudioManager.setOutputDevice(DEVICE_OUT_HEARING_AID);
        mProfileConnectedDevices.clear();
        mProfileConnectedDevices.add(mBluetoothDevice);
        mProfileConnectedDevices.add(mLeftBluetoothHapDevice);
        mProfileConnectedDevices.add(mRightBluetoothHapDevice);
        mHearingAidActiveDevices.clear();
        mHearingAidActiveDevices.add(null);
        mHearingAidActiveDevices.add(mRightBluetoothHapDevice);
        when(mHearingAidProfile.getConnectedDevices()).thenReturn(mProfileConnectedDevices);
        when(mHearingAidProfile.getActiveDevices()).thenReturn(mHearingAidActiveDevices);
        when(mHearingAidProfile.getHiSyncId(mLeftBluetoothHapDevice)).thenReturn(HISYNCID1);
        when(mHearingAidProfile.getHiSyncId(mRightBluetoothHapDevice)).thenReturn(HISYNCID2);

        mController.updateState(mPreference);

        assertThat(mPreference.isVisible()).isTrue();
        assertThat(mPreference.getSummary()).isEqualTo(mRightBluetoothHapDevice.getName());
        assertThat(mController.mConnectedDevices).containsExactly(mBluetoothDevice,
                mLeftBluetoothHapDevice, mRightBluetoothHapDevice);
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
