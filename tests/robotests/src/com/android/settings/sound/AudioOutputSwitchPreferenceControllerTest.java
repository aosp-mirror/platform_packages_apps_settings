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
import static com.android.settings.core.BasePreferenceController.DISABLED_UNSUPPORTED;

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
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
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
    private A2dpProfile mA2dpProfile;

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
        when(mLocalBluetoothProfileManager.getA2dpProfile()).thenReturn(mA2dpProfile);

        mBluetoothManager = new BluetoothManager(mContext);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(TEST_DEVICE_ADDRESS_1);
        mShadowBluetoothDevice = Shadows.shadowOf(mBluetoothDevice);
        mShadowBluetoothDevice.setName(TEST_DEVICE_NAME_1);
        when(mCachedBluetoothDevice.getDevice()).thenReturn(mBluetoothDevice);

        mController = new AudioSwitchPreferenceControllerTestable(mContext, TEST_KEY);
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
    public void getAvailabilityStatus_byDefault_isAvailable() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_whenNotVisible_isDisable() {
        FeatureFlagUtils.setEnabled(mContext, FeatureFlags.AUDIO_SWITCHER_SETTINGS, false);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(DISABLED_UNSUPPORTED);
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
        List<BluetoothDevice> connectedDevices = new ArrayList<>(2);
        connectedDevices.add(mBluetoothDevice);
        connectedDevices.add(secondBluetoothDevice);
        mController.mConnectedDevices = connectedDevices;

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