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


import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.PreferenceScreen;
import android.util.FeatureFlagUtils;

import com.android.settings.R;
import com.android.settings.core.FeatureFlags;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.ShadowAudioManager;
import com.android.settings.testutils.shadow.ShadowBluetoothUtils;
import com.android.settings.testutils.shadow.ShadowMediaRouter;
import com.android.settingslib.bluetooth.A2dpProfile;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.BluetoothEventManager;
import com.android.settingslib.bluetooth.HeadsetProfile;
import com.android.settingslib.bluetooth.HearingAidProfile;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowBluetoothDevice;

import java.util.ArrayList;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(shadows = {
        ShadowAudioManager.class,
        ShadowMediaRouter.class,
        ShadowBluetoothUtils.class,
        ShadowBluetoothDevice.class}
)
public class AudioOutputSwitchPreferenceControllerTest {
    private static final String TEST_KEY = "Test_Key";
    private static final String TEST_DEVICE_NAME_1 = "Test_A2DP_BT_Device_NAME_1";
    private static final String TEST_DEVICE_NAME_2 = "Test_A2DP_BT_Device_NAME_2";
    private static final String TEST_DEVICE_ADDRESS_1 = "00:A1:A1:A1:A1:A1";
    private static final String TEST_DEVICE_ADDRESS_2 = "00:B2:B2:B2:B2:B2";
    private static final String TEST_DEVICE_ADDRESS_3 = "00:C3:C3:C3:C3:C3";
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
    private HeadsetProfile mHeadsetProfile;
    @Mock
    private HearingAidProfile mHearingAidProfile;

    private Context mContext;
    private PreferenceScreen mScreen;
    private ListPreference mPreference;
    private ShadowAudioManager mShadowAudioManager;
    private ShadowMediaRouter mShadowMediaRouter;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mBluetoothDevice;
    private BluetoothDevice mBluetoothHapDevice;
    private BluetoothDevice mSecondBluetoothHapDevice;
    private LocalBluetoothManager mLocalBluetoothManager;
    private AudioSwitchPreferenceController mController;
    private List<BluetoothDevice> mConnectedDevices;
    private List<BluetoothDevice> mHearingAidActiveDevices;
    private List<BluetoothDevice> mEmptyDevices;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);

        mShadowAudioManager = ShadowAudioManager.getShadow();
        mShadowMediaRouter = ShadowMediaRouter.getShadow();

        ShadowBluetoothUtils.sLocalBluetoothManager = mLocalManager;
        mLocalBluetoothManager = ShadowBluetoothUtils.getLocalBtManager(mContext);

        when(mLocalBluetoothManager.getEventManager()).thenReturn(mBluetoothEventManager);
        when(mLocalBluetoothManager.getProfileManager()).thenReturn(mLocalBluetoothProfileManager);
        when(mLocalBluetoothProfileManager.getA2dpProfile()).thenReturn(mA2dpProfile);
        when(mLocalBluetoothProfileManager.getHearingAidProfile()).thenReturn(mHearingAidProfile);
        when(mLocalBluetoothProfileManager.getHeadsetProfile()).thenReturn(mHeadsetProfile);

        mBluetoothManager = new BluetoothManager(mContext);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        mBluetoothDevice = spy(mBluetoothAdapter.getRemoteDevice(TEST_DEVICE_ADDRESS_1));
        when(mBluetoothDevice.getName()).thenReturn(TEST_DEVICE_NAME_1);
        when(mBluetoothDevice.isConnected()).thenReturn(true);

        mBluetoothHapDevice = spy(mBluetoothAdapter.getRemoteDevice(TEST_DEVICE_ADDRESS_2));
        when(mBluetoothHapDevice.isConnected()).thenReturn(true);
        mSecondBluetoothHapDevice = spy(mBluetoothAdapter.getRemoteDevice(TEST_DEVICE_ADDRESS_3));
        when(mSecondBluetoothHapDevice.isConnected()).thenReturn(true);

        mController = new AudioSwitchPreferenceControllerTestable(mContext, TEST_KEY);
        mScreen = spy(new PreferenceScreen(mContext, null));
        mPreference = new ListPreference(mContext);
        mConnectedDevices = new ArrayList<>(2);
        mHearingAidActiveDevices = new ArrayList<>(2);
        mEmptyDevices = new ArrayList<>(2);

        when(mScreen.getPreferenceManager()).thenReturn(mock(PreferenceManager.class));
        when(mScreen.getContext()).thenReturn(mContext);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        mScreen.addPreference(mPreference);
        mController.displayPreference(mScreen);
    }

    @After
    public void tearDown() {
        mShadowAudioManager.reset();
        mShadowMediaRouter.reset();
        ShadowBluetoothUtils.reset();
    }

    @Test
    public void getAvailabilityStatus_byDefault_isAvailable() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_whenNotVisible_isDisable() {
        FeatureFlagUtils.setEnabled(mContext, FeatureFlags.AUDIO_SWITCHER_SETTINGS, false);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void onStart_shouldRegisterCallbackAndRegisterReceiver() {
        mController.onStart();

        verify(mLocalBluetoothManager.getEventManager()).registerCallback(
                any(BluetoothCallback.class));
        verify(mContext).registerReceiver(any(BroadcastReceiver.class), any(IntentFilter.class));
    }

    @Test
    public void onStop_shouldUnregisterCallbackAndUnregisterReceiver() {
        mController.onStart();
        mController.onStop();

        verify(mLocalBluetoothManager.getEventManager()).unregisterCallback(
                any(BluetoothCallback.class));
        verify(mContext).unregisterReceiver(any(BroadcastReceiver.class));
    }

    @Test
    public void onPreferenceChange_toThisDevice_shouldSetDefaultSummary() {
        mConnectedDevices.clear();
        mConnectedDevices.add(mBluetoothDevice);
        mController.mConnectedDevices = mConnectedDevices;

        mController.onPreferenceChange(mPreference,
                mContext.getText(R.string.media_output_default_summary));

        assertThat(mPreference.getSummary()).isEqualTo(
                mContext.getText(R.string.media_output_default_summary));
    }

    /**
     * One Bluetooth devices are available, and select the device.
     * Preference summary should be device name.
     */
    @Test
    public void onPreferenceChange_toBtDevice_shouldSetBtDeviceName() {
        mConnectedDevices.clear();
        mConnectedDevices.add(mBluetoothDevice);
        mController.mConnectedDevices = mConnectedDevices;

        mController.onPreferenceChange(mPreference, TEST_DEVICE_ADDRESS_1);

        assertThat(mPreference.getSummary()).isEqualTo(mBluetoothDevice.getName());
    }

    /**
     * More than one Bluetooth devices are available, and select second device.
     * Preference summary should be second device name.
     */
    @Test
    public void onPreferenceChange_toBtDevices_shouldSetSecondBtDeviceName() {
        ShadowBluetoothDevice shadowBluetoothDevice;
        BluetoothDevice secondBluetoothDevice;
        secondBluetoothDevice = mBluetoothAdapter.getRemoteDevice(TEST_DEVICE_ADDRESS_2);
        shadowBluetoothDevice = Shadows.shadowOf(secondBluetoothDevice);
        shadowBluetoothDevice.setName(TEST_DEVICE_NAME_2);
        mConnectedDevices.clear();
        mConnectedDevices.add(mBluetoothDevice);
        mConnectedDevices.add(secondBluetoothDevice);
        mController.mConnectedDevices = mConnectedDevices;

        mController.onPreferenceChange(mPreference, TEST_DEVICE_ADDRESS_2);

        assertThat(mPreference.getSummary()).isEqualTo(secondBluetoothDevice.getName());
    }

    /**
     * mConnectedDevices is Null.
     * onPreferenceChange should return false.
     */
    @Test
    public void onPreferenceChange_connectedDeviceIsNull_shouldReturnFalse() {
        mController.mConnectedDevices = null;

        assertThat(mController.onPreferenceChange(mPreference, TEST_DEVICE_ADDRESS_1)).isFalse();
    }

    /**
     * Two hearing aid devices with different HisyncId
     * getConnectedHearingAidDevices should add both device to list.
     */
    @Test
    public void getConnectedHearingAidDevices_deviceHisyncIdIsDifferent_shouldAddBothToList() {
        mEmptyDevices.clear();
        mConnectedDevices.clear();
        mConnectedDevices.add(mBluetoothHapDevice);
        mConnectedDevices.add(mSecondBluetoothHapDevice);
        when(mHearingAidProfile.getConnectedDevices()).thenReturn(mConnectedDevices);
        when(mHearingAidProfile.getHiSyncId(mBluetoothHapDevice)).thenReturn(HISYNCID1);
        when(mHearingAidProfile.getHiSyncId(mSecondBluetoothHapDevice)).thenReturn(
                HISYNCID2);

        mEmptyDevices.addAll(mController.getConnectedHearingAidDevices());

        assertThat(mEmptyDevices).containsExactly(mBluetoothHapDevice, mSecondBluetoothHapDevice);
    }

    /**
     * Two hearing aid devices with same HisyncId
     * getConnectedHearingAidDevices should only add first device to list.
     */
    @Test
    public void getConnectedHearingAidDevices_deviceHisyncIdIsSame_shouldAddOneToList() {
        mEmptyDevices.clear();
        mConnectedDevices.clear();
        mConnectedDevices.add(mBluetoothHapDevice);
        mConnectedDevices.add(mSecondBluetoothHapDevice);
        when(mHearingAidProfile.getConnectedDevices()).thenReturn(mConnectedDevices);
        when(mHearingAidProfile.getHiSyncId(mBluetoothHapDevice)).thenReturn(HISYNCID1);
        when(mHearingAidProfile.getHiSyncId(mSecondBluetoothHapDevice)).thenReturn(
                HISYNCID1);

        mEmptyDevices.addAll(mController.getConnectedHearingAidDevices());

        assertThat(mEmptyDevices).containsExactly(mBluetoothHapDevice);
    }

    /**
     * One A2dp device is connected.
     * getConnectedA2dpDevices should add this device to list.
     */
    @Test
    public void getConnectedA2dpDevices_oneConnectedA2dpDevice_shouldAddDeviceToList() {
        mEmptyDevices.clear();
        mConnectedDevices.clear();
        mConnectedDevices.add(mBluetoothDevice);
        when(mA2dpProfile.getConnectedDevices()).thenReturn(mConnectedDevices);

        mEmptyDevices.addAll(mController.getConnectedA2dpDevices());

        assertThat(mEmptyDevices).containsExactly(mBluetoothDevice);
    }

    /**
     * More than one A2dp devices are connected.
     * getConnectedA2dpDevices should add all devices to list.
     */
    @Test
    public void getConnectedA2dpDevices_moreThanOneConnectedA2dpDevice_shouldAddDeviceToList() {
        mEmptyDevices.clear();
        mConnectedDevices.clear();
        mConnectedDevices.add(mBluetoothDevice);
        mConnectedDevices.add(mBluetoothHapDevice);
        when(mA2dpProfile.getConnectedDevices()).thenReturn(mConnectedDevices);

        mEmptyDevices.addAll(mController.getConnectedA2dpDevices());

        assertThat(mEmptyDevices).containsExactly(mBluetoothDevice, mBluetoothHapDevice);
    }

    /**
     * One hands free profile device is connected.
     * getConnectedA2dpDevices should add this device to list.
     */
    @Test
    public void getConnectedHfpDevices_oneConnectedHfpDevice_shouldAddDeviceToList() {
        mEmptyDevices.clear();
        mConnectedDevices.clear();
        mConnectedDevices.add(mBluetoothDevice);
        when(mHeadsetProfile.getConnectedDevices()).thenReturn(mConnectedDevices);

        mEmptyDevices.addAll(mController.getConnectedHfpDevices());

        assertThat(mEmptyDevices).containsExactly(mBluetoothDevice);
    }

    /**
     * More than one hands free profile devices are connected.
     * getConnectedA2dpDevices should add all devices to list.
     */
    @Test
    public void getConnectedHfpDevices_moreThanOneConnectedHfpDevice_shouldAddDeviceToList() {
        mEmptyDevices.clear();
        mConnectedDevices.clear();
        mConnectedDevices.add(mBluetoothDevice);
        mConnectedDevices.add(mBluetoothHapDevice);
        when(mHeadsetProfile.getConnectedDevices()).thenReturn(mConnectedDevices);

        mEmptyDevices.addAll(mController.getConnectedHfpDevices());

        assertThat(mEmptyDevices).containsExactly(mBluetoothDevice, mBluetoothHapDevice);
    }

    private class AudioSwitchPreferenceControllerTestable extends
            AudioSwitchPreferenceController {
        AudioSwitchPreferenceControllerTestable(Context context, String key) {
            super(context, key);
        }

        @Override
        public void setActiveBluetoothDevice(BluetoothDevice device) {
        }

        @Override
        public String getPreferenceKey() {
            return TEST_KEY;
        }
    }
}