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

import static android.media.AudioSystem.DEVICE_OUT_BLE_HEADSET;
import static android.media.AudioSystem.DEVICE_OUT_BLUETOOTH_SCO;
import static android.media.AudioSystem.DEVICE_OUT_HEARING_AID;

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
import com.android.settingslib.bluetooth.BluetoothEventManager;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.HeadsetProfile;
import com.android.settingslib.bluetooth.HearingAidProfile;
import com.android.settingslib.bluetooth.LeAudioProfile;
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
import java.util.Collection;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowAudioManager.class,
        ShadowBluetoothUtils.class,
        ShadowBluetoothDevice.class}
)
public class HandsFreeProfileOutputPreferenceControllerTest {
    private static final String TEST_KEY = "Test_Key";
    private static final String TEST_DEVICE_NAME_1 = "Test_HFP_BT_Device_NAME_1";
    private static final String TEST_DEVICE_NAME_2 = "Test_HFP_BT_Device_NAME_2";
    private static final String TEST_HAP_DEVICE_NAME_1 = "Test_HAP_BT_Device_NAME_1";
    private static final String TEST_HAP_DEVICE_NAME_2 = "Test_HAP_BT_Device_NAME_2";
    private static final String TEST_LE_AUDIO_DEVICE_NAME_1 = "Test_LE_AUDIO_Device_NAME_1";
    private static final String TEST_DEVICE_ADDRESS_1 = "00:A1:A1:A1:A1:A1";
    private static final String TEST_DEVICE_ADDRESS_2 = "00:B2:B2:B2:B2:B2";
    private static final String TEST_DEVICE_ADDRESS_3 = "00:C3:C3:C3:C3:C3";
    private static final String TEST_DEVICE_ADDRESS_4 = "00:D4:D4:D4:D4:D4";
    private static final String TEST_DEVICE_ADDRESS_5 = "00:E5:E5:E5:E5:E5";
    private final static long HISYNCID1 = 10;
    private final static long HISYNCID2 = 11;

    @Mock
    private LocalBluetoothManager mLocalManager;
    @Mock
    private BluetoothEventManager mBluetoothEventManager;
    @Mock
    private LocalBluetoothProfileManager mLocalBluetoothProfileManager;
    @Mock
    private HeadsetProfile mHeadsetProfile;
    @Mock
    private HearingAidProfile mHearingAidProfile;
    @Mock
    private LeAudioProfile mLeAudioProfile;
    @Mock
    private AudioSwitchPreferenceController.AudioSwitchCallback mAudioSwitchPreferenceCallback;
    @Mock
    private CachedBluetoothDeviceManager mCachedDeviceManager;
    @Mock
    private CachedBluetoothDevice mCachedBluetoothDeviceL;
    @Mock
    private CachedBluetoothDevice mCachedBluetoothDeviceR;

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
    private HandsFreeProfileOutputPreferenceController mController;
    private List<BluetoothDevice> mProfileConnectedDevices;
    private List<BluetoothDevice> mHearingAidActiveDevices;
    private List<BluetoothDevice> mLeAudioActiveDevices;
    private Collection<CachedBluetoothDevice> mCachedDevices;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);

        mAudioManager = mContext.getSystemService(AudioManager.class);
        mShadowAudioManager = ShadowAudioManager.getShadow();

        ShadowBluetoothUtils.sLocalBluetoothManager = mLocalManager;
        mLocalBluetoothManager = Utils.getLocalBtManager(mContext);

        when(mLocalBluetoothManager.getEventManager()).thenReturn(mBluetoothEventManager);
        when(mLocalBluetoothManager.getProfileManager()).thenReturn(mLocalBluetoothProfileManager);
        when(mLocalBluetoothManager.getCachedDeviceManager()).thenReturn(mCachedDeviceManager);
        when(mLocalBluetoothProfileManager.getHeadsetProfile()).thenReturn(mHeadsetProfile);
        when(mLocalBluetoothProfileManager.getHearingAidProfile()).thenReturn(mHearingAidProfile);
        when(mLocalBluetoothProfileManager.getLeAudioProfile()).thenReturn(mLeAudioProfile);

        mBluetoothManager = mContext.getSystemService(BluetoothManager.class);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        mCachedDevices = new ArrayList<>();
        mCachedDevices.add(mCachedBluetoothDeviceL);
        mCachedDevices.add(mCachedBluetoothDeviceR);
        when(mCachedDeviceManager.getCachedDevicesCopy()).thenReturn(mCachedDevices);

        mBluetoothDevice = spy(mBluetoothAdapter.getRemoteDevice(TEST_DEVICE_ADDRESS_1));
        when(mBluetoothDevice.getName()).thenReturn(TEST_DEVICE_NAME_1);
        when(mBluetoothDevice.getAlias()).thenReturn(TEST_DEVICE_NAME_1);
        when(mBluetoothDevice.isConnected()).thenReturn(true);

        mSecondBluetoothDevice = spy(mBluetoothAdapter.getRemoteDevice(TEST_DEVICE_ADDRESS_2));
        when(mSecondBluetoothDevice.getName()).thenReturn(TEST_DEVICE_NAME_2);
        when(mSecondBluetoothDevice.getAlias()).thenReturn(TEST_DEVICE_NAME_2);
        when(mSecondBluetoothDevice.isConnected()).thenReturn(true);

        mLeftBluetoothHapDevice = spy(mBluetoothAdapter.getRemoteDevice(TEST_DEVICE_ADDRESS_3));
        when(mLeftBluetoothHapDevice.getName()).thenReturn(TEST_HAP_DEVICE_NAME_1);
        when(mLeftBluetoothHapDevice.getAlias()).thenReturn(TEST_HAP_DEVICE_NAME_1);
        when(mLeftBluetoothHapDevice.isConnected()).thenReturn(true);

        mRightBluetoothHapDevice = spy(mBluetoothAdapter.getRemoteDevice(TEST_DEVICE_ADDRESS_4));
        when(mRightBluetoothHapDevice.getName()).thenReturn(TEST_HAP_DEVICE_NAME_2);
        when(mRightBluetoothHapDevice.getAlias()).thenReturn(TEST_HAP_DEVICE_NAME_2);
        when(mRightBluetoothHapDevice.isConnected()).thenReturn(true);

        mController = new HandsFreeProfileOutputPreferenceController(mContext, TEST_KEY);
        mScreen = spy(new PreferenceScreen(mContext, null));
        mPreference = new ListPreference(mContext);
        mProfileConnectedDevices = new ArrayList<>();
        mHearingAidActiveDevices = new ArrayList<>(2);
        mLeAudioActiveDevices = new ArrayList<>();

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
     * During a call, bluetooth device with HisyncId.
     * HearingAidProfile should set active device to this device.
     */
    @Test
    public void setActiveBluetoothDevice_btDeviceWithHisyncId_shouldSetBtDeviceActive() {
        mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        when(mHearingAidProfile.getHiSyncId(mLeftBluetoothHapDevice)).thenReturn(HISYNCID1);

        mController.setActiveBluetoothDevice(mLeftBluetoothHapDevice);

        verify(mHearingAidProfile).setActiveDevice(mLeftBluetoothHapDevice);
        verify(mHeadsetProfile, never()).setActiveDevice(mLeftBluetoothHapDevice);
    }

    /**
     * During a call, Bluetooth device without HisyncId.
     * HeadsetProfile should set active device to this device.
     */
    @Test
    public void setActiveBluetoothDevice_btDeviceWithoutHisyncId_shouldSetBtDeviceActive() {
        mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);

        mController.setActiveBluetoothDevice(mBluetoothDevice);

        verify(mHeadsetProfile).setActiveDevice(mBluetoothDevice);
        verify(mHearingAidProfile, never()).setActiveDevice(mBluetoothDevice);
    }

    /**
     * During a call, set active device to "this device".
     * HeadsetProfile should set to null.
     * HearingAidProfile should set to null.
     */
    @Test
    public void setActiveBluetoothDevice_setNull_shouldSetNullToBothProfiles() {
        mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);

        mController.setActiveBluetoothDevice(null);

        verify(mHeadsetProfile).setActiveDevice(null);
        verify(mHearingAidProfile).setActiveDevice(null);
    }

    /**
     * In normal mode
     * HeadsetProfile should not set active device.
     */
    @Test
    public void setActiveBluetoothDevice_inNormalMode_shouldNotSetActiveDeviceToHeadsetProfile() {
        mAudioManager.setMode(AudioManager.MODE_NORMAL);

        mController.setActiveBluetoothDevice(mBluetoothDevice);

        verify(mHeadsetProfile, times(0)).setActiveDevice(any(BluetoothDevice.class));
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
     * One Hands Free Profile Bluetooth device is available and activated
     * Preference should be visible
     * Preference summary should be the activated device name
     */
    @Test
    public void updateState_oneHeadsetsAvailableAndActivated_shouldSetDeviceName() {
        mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        mShadowAudioManager.setOutputDevice(DEVICE_OUT_BLUETOOTH_SCO);
        mProfileConnectedDevices.clear();
        mProfileConnectedDevices.add(mBluetoothDevice);
        when(mHeadsetProfile.getConnectedDevices()).thenReturn(mProfileConnectedDevices);
        when(mHeadsetProfile.getActiveDevice()).thenReturn(mBluetoothDevice);

        mController.updateState(mPreference);

        assertThat(mPreference.isVisible()).isTrue();
        assertThat(mPreference.getSummary()).isEqualTo(TEST_DEVICE_NAME_1);
    }

    /**
     * More than one Hands Free Profile Bluetooth devices are available, and second
     * device is active.
     * Preference should be visible
     * Preference summary should be the activated device name
     */
    @Test
    public void updateState_moreThanOneHfpBtDevicesAreAvailable_shouldSetActivatedDeviceName() {
        mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        mShadowAudioManager.setOutputDevice(DEVICE_OUT_BLUETOOTH_SCO);
        List<BluetoothDevice> connectedDevices = new ArrayList<>(2);
        connectedDevices.add(mBluetoothDevice);
        connectedDevices.add(mSecondBluetoothDevice);
        when(mHeadsetProfile.getConnectedDevices()).thenReturn(connectedDevices);
        when(mHeadsetProfile.getActiveDevice()).thenReturn(mSecondBluetoothDevice);

        mController.updateState(mPreference);

        assertThat(mPreference.isVisible()).isTrue();
        assertThat(mPreference.getSummary()).isEqualTo(TEST_DEVICE_NAME_2);
    }

    /**
     * Hands Free Profile Bluetooth device(s) are available, but wired headset is plugged in
     * and activated.
     * Preference should be visible
     * Preference summary should be "This device"
     */
    @Test
    public void updateState_withAvailableDevicesWiredHeadsetActivated_shouldSetDefaultSummary() {
        mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        mProfileConnectedDevices.clear();
        mProfileConnectedDevices.add(mBluetoothDevice);
        when(mHeadsetProfile.getConnectedDevices()).thenReturn(mProfileConnectedDevices);
        when(mHeadsetProfile.getActiveDevice()).thenReturn(null);

        mController.updateState(mPreference);

        assertThat(mPreference.isVisible()).isTrue();
        assertThat(mPreference.getSummary()).isEqualTo(
                mContext.getText(R.string.media_output_default_summary));
    }

    /**
     * No available Headset BT devices
     * Preference should be invisible
     * Preference summary should be "This device"
     */
    @Test
    public void updateState_noAvailableHeadsetBtDevices_shouldSetDefaultSummary() {
        mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        List<BluetoothDevice> emptyDeviceList = new ArrayList<>();
        when(mHeadsetProfile.getConnectedDevices()).thenReturn(emptyDeviceList);

        mController.updateState(mPreference);

        assertThat(mPreference.isVisible()).isFalse();
        assertThat(mPreference.getSummary()).isEqualTo(
                mContext.getText(R.string.media_output_default_summary));
    }

    /**
     * One hearing aid profile Bluetooth device is available and active.
     * Preference should be visible
     * Preference summary should be the activated device name
     */
    @Test
    public void updateState_oneHapBtDeviceAreAvailable_shouldSetActivatedDeviceName() {
        mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
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
        mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
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
     * Both hearing aid profile and hands free profile Bluetooth devices are available, and
     * two hearing aid profile devices with same HisyncId. Both of HAP device are active,
     * "left" side HAP device is added first.
     * Preference should be visible
     * Preference summary should be the activated device name
     * ConnectedDevice should not contain second HAP device with same HisyncId
     */
    @Test
    public void updateState_hapBtDeviceWithSameId_shouldSetActivatedDeviceName() {
        mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        mShadowAudioManager.setOutputDevice(DEVICE_OUT_HEARING_AID);
        mProfileConnectedDevices.clear();
        mProfileConnectedDevices.add(mBluetoothDevice);
        //with same HisyncId, only the first one will remain in UI.
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
     * Both hearing aid profile and hands free profile Bluetooth devices are available, and
     * two hearing aid profile devices with same HisyncId. Both of HAP device are active,
     * "right" side HAP device is added first.
     * Preference should be visible
     * Preference summary should be the activated device name
     * ConnectedDevice should not contain second HAP device with same HisyncId
     */
    @Test
    public void updateState_hapBtDeviceWithSameIdButDifferentOrder_shouldSetActivatedDeviceName() {
        mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        mShadowAudioManager.setOutputDevice(DEVICE_OUT_HEARING_AID);
        mProfileConnectedDevices.clear();
        mProfileConnectedDevices.add(mBluetoothDevice);
        //with same HisyncId, only the first one will remain in UI.
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
        assertThat(mController.mConnectedDevices.contains(mRightBluetoothHapDevice)).isTrue();
        assertThat(mController.mConnectedDevices.contains(mLeftBluetoothHapDevice)).isFalse();
        assertThat(mPreference.getSummary()).isEqualTo(mRightBluetoothHapDevice.getName());
    }

    /**
     * Both hearing aid profile and hands free profile  Bluetooth devices are available, and
     * two hearing aid profile devices with different HisyncId. One of HAP device is active.
     * Preference should be visible
     * Preference summary should be the activated device name
     * ConnectedDevice should contain both HAP device with different HisyncId
     */
    @Test
    public void updateState_hapBtDeviceWithDifferentId_shouldSetActivatedDeviceName() {
        mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
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
    public void updateState_leAudioDeviceActive_shouldSetActivatedDeviceName() {
        mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        mShadowAudioManager.setOutputDevice(DEVICE_OUT_BLE_HEADSET);
        when(mCachedBluetoothDeviceL.getDevice()).thenReturn(mBluetoothDevice);
        when(mBluetoothDevice.getName()).thenReturn(TEST_LE_AUDIO_DEVICE_NAME_1);
        when(mBluetoothDevice.getAlias()).thenReturn(TEST_LE_AUDIO_DEVICE_NAME_1);
        mProfileConnectedDevices.clear();
        mProfileConnectedDevices.add(mBluetoothDevice);
        mLeAudioActiveDevices.clear();
        mLeAudioActiveDevices.add(mBluetoothDevice);
        when(mLeAudioProfile.getConnectedDevices()).thenReturn(mProfileConnectedDevices);
        when(mLeAudioProfile.getActiveDevices()).thenReturn(mLeAudioActiveDevices);

        mController.updateState(mPreference);

        assertThat(mPreference.isVisible()).isTrue();
        assertThat(mPreference.getSummary()).isEqualTo(mBluetoothDevice.getName());
    }

    @Test
    public void findActiveDevice_onlyHeadsetDeviceActive_returnHeadsetDevice() {
        when(mLocalBluetoothProfileManager.getHearingAidProfile()).thenReturn(null);
        when(mHeadsetProfile.getActiveDevice()).thenReturn(mBluetoothDevice);

        assertThat(mController.findActiveDevice()).isEqualTo(mBluetoothDevice);
    }

    @Test
    public void findActiveDevice_allDevicesNotActive_returnNull() {
        when(mLocalBluetoothProfileManager.getHearingAidProfile()).thenReturn(null);
        when(mHeadsetProfile.getActiveDevice()).thenReturn(null);

        assertThat(mController.findActiveDevice()).isNull();
    }

    @Test
    public void findActiveDevice_allProfilesWithActiveDevice_returnHADevice() {
        BluetoothDevice btLeDevice = spy(mBluetoothAdapter.getRemoteDevice(TEST_DEVICE_ADDRESS_5));
        when(btLeDevice.getName()).thenReturn(TEST_LE_AUDIO_DEVICE_NAME_1);
        mController.mConnectedDevices.clear();
        mController.mConnectedDevices.add(mBluetoothDevice);
        mController.mConnectedDevices.add(mLeftBluetoothHapDevice);
        mController.mConnectedDevices.add(btLeDevice);
        mLeAudioActiveDevices.clear();
        mLeAudioActiveDevices.add(btLeDevice);
        mHearingAidActiveDevices.clear();
        mHearingAidActiveDevices.add(mLeftBluetoothHapDevice);
        when(mHearingAidProfile.getActiveDevices()).thenReturn(mHearingAidActiveDevices);
        when(mHearingAidProfile.getHiSyncId(mLeftBluetoothHapDevice)).thenReturn(HISYNCID1);
        when(mHeadsetProfile.getActiveDevice()).thenReturn(mBluetoothDevice);
        when(mLeAudioProfile.getActiveDevices()).thenReturn(mLeAudioActiveDevices);

        assertThat(mController.findActiveDevice()).isEqualTo(mLeftBluetoothHapDevice);
    }

    @Test
    public void findActiveDevice_headsetDeviceAndLeAudioDeviceActive_returnLeAudioDevice() {
        BluetoothDevice btLeDevice = spy(mBluetoothAdapter.getRemoteDevice(TEST_DEVICE_ADDRESS_5));
        when(btLeDevice.getName()).thenReturn(TEST_LE_AUDIO_DEVICE_NAME_1);
        mLeAudioActiveDevices.clear();
        mLeAudioActiveDevices.add(btLeDevice);
        mHearingAidActiveDevices.clear();
        when(mHearingAidProfile.getActiveDevices()).thenReturn(mHearingAidActiveDevices);
        when(mHeadsetProfile.getActiveDevice()).thenReturn(mBluetoothDevice);
        when(mLeAudioProfile.getActiveDevices()).thenReturn(mLeAudioActiveDevices);

        assertThat(mController.findActiveDevice()).isEqualTo(btLeDevice);
    }

    @Test
    public void findActiveDevice_onlyLeAudioDeviceActive_returnLeAudioDevice() {
        BluetoothDevice btLeDevice = spy(mBluetoothAdapter.getRemoteDevice(TEST_DEVICE_ADDRESS_5));
        when(btLeDevice.getName()).thenReturn(TEST_LE_AUDIO_DEVICE_NAME_1);
        mLeAudioActiveDevices.clear();
        mLeAudioActiveDevices.add(btLeDevice);
        mHearingAidActiveDevices.clear();
        when(mHearingAidProfile.getActiveDevices()).thenReturn(mHearingAidActiveDevices);
        when(mHeadsetProfile.getActiveDevice()).thenReturn(null);
        when(mLeAudioProfile.getActiveDevices()).thenReturn(mLeAudioActiveDevices);

        assertThat(mController.findActiveDevice()).isEqualTo(btLeDevice);
    }


    /**
     * One Bluetooth devices are available, and select the device.
     * Preference summary should be device name.
     */
    @Test
    public void onPreferenceChange_toBtDevice_shouldSetBtDeviceName() {
        mController.mConnectedDevices.clear();
        mController.mConnectedDevices.add(mBluetoothDevice);

        mController.onPreferenceChange(mPreference, TEST_DEVICE_ADDRESS_1);

        assertThat(mPreference.getSummary()).isEqualTo(TEST_DEVICE_NAME_1);
    }

    /**
     * More than one Bluetooth devices are available, and select second device.
     * Preference summary should be second device name.
     */
    @Test
    public void onPreferenceChange_toBtDevices_shouldSetSecondBtDeviceName() {
        mController.mConnectedDevices.clear();
        mController.mConnectedDevices.add(mBluetoothDevice);
        mController.mConnectedDevices.add(mSecondBluetoothDevice);

        mController.onPreferenceChange(mPreference, TEST_DEVICE_ADDRESS_2);

        assertThat(mPreference.getSummary()).isEqualTo(TEST_DEVICE_NAME_2);
    }

    /**
     * mConnectedDevices is empty.
     * onPreferenceChange should return false.
     */
    @Test
    public void onPreferenceChange_connectedDeviceIsNull_shouldReturnFalse() {
        mController.mConnectedDevices.clear();

        assertThat(mController.onPreferenceChange(mPreference, TEST_DEVICE_ADDRESS_1)).isFalse();
    }

    @Test
    public void onPreferenceChange_toThisDevice_shouldSetDefaultSummary() {
        mController.mConnectedDevices.clear();
        mController.mConnectedDevices.add(mBluetoothDevice);

        mController.onPreferenceChange(mPreference,
                mContext.getText(R.string.media_output_default_summary));

        assertThat(mPreference.getSummary()).isEqualTo(
                mContext.getText(R.string.media_output_default_summary));
    }
}
