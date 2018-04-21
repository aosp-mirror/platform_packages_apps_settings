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


import static android.media.AudioSystem.DEVICE_OUT_USB_HEADSET;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
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
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.ShadowAudioManager;
import com.android.settings.testutils.shadow.ShadowBluetoothUtils;
import com.android.settings.testutils.shadow.ShadowMediaRouter;
import com.android.settingslib.bluetooth.BluetoothEventManager;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.HeadsetProfile;
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
public class HandsFreeProfileOutputPreferenceControllerTest {
    private static final String TEST_KEY = "Test_Key";
    private static final String TEST_DEVICE_NAME_1 = "Test_HAP_BT_Device_NAME_1";
    private static final String TEST_DEVICE_NAME_2 = "Test_HAP_BT_Device_NAME_2";
    private static final String TEST_DEVICE_ADDRESS_1 = "00:07:80:78:A4:69";
    private static final String TEST_DEVICE_ADDRESS_2 = "00:00:00:00:00:00";

    @Mock
    private LocalBluetoothManager mLocalManager;
    @Mock
    private BluetoothEventManager mBluetoothEventManager;
    @Mock
    private CachedBluetoothDevice mCachedBluetoothDevice;
    @Mock
    private LocalBluetoothProfileManager mLocalBluetoothProfileManager;
    @Mock
    private HeadsetProfile mHeadsetProfile;

    private Context mContext;
    private PreferenceScreen mScreen;
    private ListPreference mPreference;
    private ShadowAudioManager mShadowAudioManager;
    private ShadowMediaRouter mShadowMediaRouter;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mBluetoothDevice;
    private ShadowBluetoothDevice mShadowBluetoothDevice;
    private LocalBluetoothManager mLocalBluetoothManager;
    private AudioSwitchPreferenceController mController;
    private List<BluetoothDevice> mConnectedDevices;

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
        when(mLocalBluetoothProfileManager.getHeadsetProfile()).thenReturn(mHeadsetProfile);

        mBluetoothManager = new BluetoothManager(mContext);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(TEST_DEVICE_ADDRESS_1);
        mShadowBluetoothDevice = Shadows.shadowOf(mBluetoothDevice);
        mShadowBluetoothDevice.setName(TEST_DEVICE_NAME_1);
        when(mCachedBluetoothDevice.getDevice()).thenReturn(mBluetoothDevice);

        mController = new HandsFreeProfileOutputPreferenceController(mContext, TEST_KEY);
        mScreen = spy(new PreferenceScreen(mContext, null));
        mPreference = new ListPreference(mContext);
        mConnectedDevices = new ArrayList<>(1);
        mConnectedDevices.add(mBluetoothDevice);

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
    public void setActiveBluetoothDevice_duringACalling_shouldSetBtDeviceActive() {
        mShadowAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);

        mController.setActiveBluetoothDevice(mBluetoothDevice);

        verify(mHeadsetProfile).setActiveDevice(mBluetoothDevice);
    }

    @Test
    public void updateState_shouldSetSummary() {
        mController.updateState(mPreference);

        assertThat(mPreference.getSummary()).isEqualTo(
                mContext.getText(R.string.media_output_default_summary));
    }

    /**
     * One Headset Bluetooth device is available and activated
     * Preference should be enabled
     * Preference summary should be activate device name
     */
    @Test
    public void updateState_oneHeadsetsAvailableAndActivated_shouldSetDeviceName() {
        mShadowAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        when(mHeadsetProfile.getConnectedDevices()).thenReturn(mConnectedDevices);
        when(mHeadsetProfile.getActiveDevice()).thenReturn(mBluetoothDevice);

        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isTrue();
        assertThat(mPreference.getSummary()).isEqualTo(mBluetoothDevice.getName());
    }

    /**
     * More than one Headset Bluetooth devices are available, and second device is active.
     * Preference should be enabled
     * Preference summary should be activate device name
     */
    @Test
    public void updateState_moreThanOneHapBtDevicesAreAvailable_shouldSetActivatedDeviceName() {
        ShadowBluetoothDevice shadowBluetoothDevice;
        mShadowAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        BluetoothDevice secondBluetoothDevice;
        secondBluetoothDevice = mBluetoothAdapter.getRemoteDevice(TEST_DEVICE_ADDRESS_2);
        shadowBluetoothDevice = Shadows.shadowOf(secondBluetoothDevice);
        shadowBluetoothDevice.setName(TEST_DEVICE_NAME_2);
        List<BluetoothDevice> connectedDevices = new ArrayList<>(2);
        connectedDevices.add(mBluetoothDevice);
        connectedDevices.add(secondBluetoothDevice);

        when(mHeadsetProfile.getConnectedDevices()).thenReturn(connectedDevices);
        when(mHeadsetProfile.getActiveDevice()).thenReturn(secondBluetoothDevice);

        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isTrue();
        assertThat(mPreference.getSummary()).isEqualTo(secondBluetoothDevice.getName());
    }

    /**
     * Hands Free Profile Bluetooth device(s) are available, but wired headset is plugged in
     * and activated.
     * Preference should be enabled
     * Preference summary should be "This device"
     */
    @Test
    public void updateState_withAvailableDevicesWiredHeadsetActivated_shouldSetDefaultSummary() {
        mShadowAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        mShadowAudioManager.setStream(DEVICE_OUT_USB_HEADSET);
        when(mHeadsetProfile.getConnectedDevices()).thenReturn(mConnectedDevices);
        when(mHeadsetProfile.getActiveDevice()).thenReturn(
                mBluetoothDevice); // BT device is still activated in this case

        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isTrue();
        assertThat(mPreference.getSummary()).isEqualTo(
                mContext.getText(R.string.media_output_default_summary));
    }

    /**
     * No available Headset BT devices
     * Preference should be disabled
     * Preference summary should be "This device"
     */
    @Test
    public void updateState_noAvailableHeadsetBtDevices_shouldSetDefaultSummary() {
        mShadowAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        List<BluetoothDevice> emptyDeviceList = new ArrayList<>();
        when(mHeadsetProfile.getConnectedDevices()).thenReturn(emptyDeviceList);

        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isFalse();
        assertThat(mPreference.getSummary()).isEqualTo(
                mContext.getText(R.string.media_output_default_summary));
    }
}
