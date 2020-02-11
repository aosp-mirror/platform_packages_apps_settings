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

import static android.media.AudioSystem.DEVICE_OUT_BLUETOOTH_SCO_HEADSET;
import static android.media.AudioSystem.STREAM_MUSIC;

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
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.util.FeatureFlagUtils;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.settings.bluetooth.Utils;
import com.android.settings.core.FeatureFlags;
import com.android.settings.testutils.shadow.ShadowAudioManager;
import com.android.settings.testutils.shadow.ShadowBluetoothUtils;
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
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowBluetoothDevice;
import org.robolectric.shadows.ShadowPackageManager;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowAudioManager.class,
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
    private AudioManager mAudioManager;
    private ShadowAudioManager mShadowAudioManager;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mBluetoothDevice;
    private BluetoothDevice mLeftBluetoothHapDevice;
    private BluetoothDevice mRightBluetoothHapDevice;
    private LocalBluetoothManager mLocalBluetoothManager;
    private AudioSwitchPreferenceController mController;
    private List<BluetoothDevice> mProfileConnectedDevices;
    private List<BluetoothDevice> mHearingAidActiveDevices;
    private List<BluetoothDevice> mEmptyDevices;
    private ShadowPackageManager mPackageManager;

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
        when(mLocalBluetoothProfileManager.getA2dpProfile()).thenReturn(mA2dpProfile);
        when(mLocalBluetoothProfileManager.getHearingAidProfile()).thenReturn(mHearingAidProfile);
        when(mLocalBluetoothProfileManager.getHeadsetProfile()).thenReturn(mHeadsetProfile);
        mPackageManager = Shadow.extract(mContext.getPackageManager());
        mPackageManager.setSystemFeature(PackageManager.FEATURE_BLUETOOTH, true);

        mBluetoothManager = new BluetoothManager(mContext);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        mBluetoothDevice = spy(mBluetoothAdapter.getRemoteDevice(TEST_DEVICE_ADDRESS_1));
        when(mBluetoothDevice.getName()).thenReturn(TEST_DEVICE_NAME_1);
        when(mBluetoothDevice.isConnected()).thenReturn(true);

        mLeftBluetoothHapDevice = spy(mBluetoothAdapter.getRemoteDevice(TEST_DEVICE_ADDRESS_2));
        when(mLeftBluetoothHapDevice.isConnected()).thenReturn(true);
        mRightBluetoothHapDevice = spy(mBluetoothAdapter.getRemoteDevice(TEST_DEVICE_ADDRESS_3));
        when(mRightBluetoothHapDevice.isConnected()).thenReturn(true);

        mController = new AudioSwitchPreferenceControllerTestable(mContext, TEST_KEY);
        mScreen = spy(new PreferenceScreen(mContext, null));
        mPreference = new ListPreference(mContext);
        mProfileConnectedDevices = new ArrayList<>();
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
        ShadowBluetoothUtils.reset();
    }

    @Test
    public void constructor_notSupportBluetooth_shouldReturnBeforeUsingLocalBluetoothManager() {
        ShadowBluetoothUtils.reset();
        mLocalBluetoothManager = Utils.getLocalBtManager(mContext);

        AudioSwitchPreferenceController controller = new AudioSwitchPreferenceControllerTestable(
                mContext, TEST_KEY);
        controller.onStart();
        controller.onStop();

        assertThat(mLocalBluetoothManager).isNull();
    }

    @Test
    public void getAvailabilityStatus_disableFlagNoBluetoothFeature_returnUnavailable() {
        FeatureFlagUtils.setEnabled(mContext, FeatureFlags.AUDIO_SWITCHER_SETTINGS, false);
        mPackageManager.setSystemFeature(PackageManager.FEATURE_BLUETOOTH, false);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_disableFlagWithBluetoothFeature_returnUnavailable() {
        FeatureFlagUtils.setEnabled(mContext, FeatureFlags.AUDIO_SWITCHER_SETTINGS, false);
        mPackageManager.setSystemFeature(PackageManager.FEATURE_BLUETOOTH, true);


        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_enableFlagWithBluetoothFeature_returnAvailable() {
        FeatureFlagUtils.setEnabled(mContext, FeatureFlags.AUDIO_SWITCHER_SETTINGS, true);
        mPackageManager.setSystemFeature(PackageManager.FEATURE_BLUETOOTH, true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_enableFlagNoBluetoothFeature_returnUnavailable() {
        FeatureFlagUtils.setEnabled(mContext, FeatureFlags.AUDIO_SWITCHER_SETTINGS, true);
        mPackageManager.setSystemFeature(PackageManager.FEATURE_BLUETOOTH, false);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void onStart_shouldRegisterCallbackAndRegisterReceiver() {
        mController.onStart();

        verify(mLocalBluetoothManager.getEventManager()).registerCallback(
                any(BluetoothCallback.class));
        verify(mContext).registerReceiver(any(BroadcastReceiver.class), any(IntentFilter.class));
        verify(mLocalBluetoothManager).setForegroundActivity(mContext);
    }

    @Test
    public void onStop_shouldUnregisterCallbackAndUnregisterReceiver() {
        mController.onStart();
        mController.onStop();

        verify(mLocalBluetoothManager.getEventManager()).unregisterCallback(
                any(BluetoothCallback.class));
        verify(mContext).unregisterReceiver(any(BroadcastReceiver.class));
        verify(mLocalBluetoothManager).setForegroundActivity(null);
    }

    /**
     * Audio stream output to bluetooth sco headset which is the subset of all sco device.
     * isStreamFromOutputDevice should return true.
     */
    @Test
    public void isStreamFromOutputDevice_outputDeviceIsBtScoHeadset_shouldReturnTrue() {
        mShadowAudioManager.setOutputDevice(DEVICE_OUT_BLUETOOTH_SCO_HEADSET);

        assertThat(mController.isStreamFromOutputDevice(
                STREAM_MUSIC, DEVICE_OUT_BLUETOOTH_SCO_HEADSET)).isTrue();
    }

    /**
     * Left side of HAP device is active.
     * findActiveHearingAidDevice should return hearing aid device active device.
     */
    @Test
    public void findActiveHearingAidDevice_leftActiveDevice_returnLeftDeviceAsActiveHapDevice() {
        mController.mConnectedDevices.clear();
        mController.mConnectedDevices.add(mBluetoothDevice);
        mController.mConnectedDevices.add(mLeftBluetoothHapDevice);
        mHearingAidActiveDevices.clear();
        mHearingAidActiveDevices.add(mLeftBluetoothHapDevice);
        mHearingAidActiveDevices.add(null);
        when(mHeadsetProfile.getActiveDevice()).thenReturn(mBluetoothDevice);
        when(mHearingAidProfile.getActiveDevices()).thenReturn(mHearingAidActiveDevices);

        assertThat(mController.findActiveHearingAidDevice()).isEqualTo(mLeftBluetoothHapDevice);
    }

    /**
     * Right side of HAP device is active.
     * findActiveHearingAidDevice should return hearing aid device active device.
     */
    @Test
    public void findActiveHearingAidDevice_rightActiveDevice_returnRightDeviceAsActiveHapDevice() {
        mController.mConnectedDevices.clear();
        mController.mConnectedDevices.add(mBluetoothDevice);
        mController.mConnectedDevices.add(mRightBluetoothHapDevice);
        mHearingAidActiveDevices.clear();
        mHearingAidActiveDevices.add(null);
        mHearingAidActiveDevices.add(mRightBluetoothHapDevice);
        when(mHeadsetProfile.getActiveDevice()).thenReturn(mBluetoothDevice);
        when(mHearingAidProfile.getActiveDevices()).thenReturn(mHearingAidActiveDevices);

        assertThat(mController.findActiveHearingAidDevice()).isEqualTo(mRightBluetoothHapDevice);
    }

    /**
     * Both are active device.
     * findActiveHearingAidDevice only return the active device in mConnectedDevices.
     */
    @Test
    public void findActiveHearingAidDevice_twoActiveDevice_returnActiveDeviceInConnectedDevices() {
        mController.mConnectedDevices.clear();
        mController.mConnectedDevices.add(mBluetoothDevice);
        mController.mConnectedDevices.add(mRightBluetoothHapDevice);
        mHearingAidActiveDevices.clear();
        mHearingAidActiveDevices.add(mLeftBluetoothHapDevice);
        mHearingAidActiveDevices.add(mRightBluetoothHapDevice);
        when(mHeadsetProfile.getActiveDevice()).thenReturn(mBluetoothDevice);
        when(mHearingAidProfile.getActiveDevices()).thenReturn(mHearingAidActiveDevices);

        assertThat(mController.findActiveHearingAidDevice()).isEqualTo(mRightBluetoothHapDevice);
    }

    /**
     * None of them are active.
     * findActiveHearingAidDevice should return null.
     */
    @Test
    public void findActiveHearingAidDevice_noActiveDevice_returnNull() {
        mController.mConnectedDevices.clear();
        mController.mConnectedDevices.add(mBluetoothDevice);
        mController.mConnectedDevices.add(mLeftBluetoothHapDevice);
        mHearingAidActiveDevices.clear();
        when(mHeadsetProfile.getActiveDevice()).thenReturn(mBluetoothDevice);
        when(mHearingAidProfile.getActiveDevices()).thenReturn(mHearingAidActiveDevices);

        assertThat(mController.findActiveHearingAidDevice()).isNull();
    }

    /**
     * Two hearing aid devices with different HisyncId
     * getConnectedHearingAidDevices should add both device to list.
     */
    @Test
    public void getConnectedHearingAidDevices_deviceHisyncIdIsDifferent_shouldAddBothToList() {
        mEmptyDevices.clear();
        mProfileConnectedDevices.clear();
        mProfileConnectedDevices.add(mLeftBluetoothHapDevice);
        mProfileConnectedDevices.add(mRightBluetoothHapDevice);
        when(mHearingAidProfile.getConnectedDevices()).thenReturn(mProfileConnectedDevices);
        when(mHearingAidProfile.getHiSyncId(mLeftBluetoothHapDevice)).thenReturn(HISYNCID1);
        when(mHearingAidProfile.getHiSyncId(mRightBluetoothHapDevice)).thenReturn(HISYNCID2);

        mEmptyDevices.addAll(mController.getConnectedHearingAidDevices());

        assertThat(mEmptyDevices).containsExactly(mLeftBluetoothHapDevice,
                mRightBluetoothHapDevice);
    }

    /**
     * Two hearing aid devices with same HisyncId
     * getConnectedHearingAidDevices should only add first device to list.
     */
    @Test
    public void getConnectedHearingAidDevices_deviceHisyncIdIsSame_shouldAddOneToList() {
        mEmptyDevices.clear();
        mProfileConnectedDevices.clear();
        mProfileConnectedDevices.add(mLeftBluetoothHapDevice);
        mProfileConnectedDevices.add(mRightBluetoothHapDevice);
        when(mHearingAidProfile.getConnectedDevices()).thenReturn(mProfileConnectedDevices);
        when(mHearingAidProfile.getHiSyncId(mLeftBluetoothHapDevice)).thenReturn(HISYNCID1);
        when(mHearingAidProfile.getHiSyncId(mRightBluetoothHapDevice)).thenReturn(HISYNCID1);

        mEmptyDevices.addAll(mController.getConnectedHearingAidDevices());

        assertThat(mEmptyDevices).containsExactly(mLeftBluetoothHapDevice);
    }

    /**
     * One hands free profile device is connected.
     * getConnectedA2dpDevices should add this device to list.
     */
    @Test
    public void getConnectedHfpDevices_oneConnectedHfpDevice_shouldAddDeviceToList() {
        mEmptyDevices.clear();
        mProfileConnectedDevices.clear();
        mProfileConnectedDevices.add(mBluetoothDevice);
        when(mHeadsetProfile.getConnectedDevices()).thenReturn(mProfileConnectedDevices);

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
        mProfileConnectedDevices.clear();
        mProfileConnectedDevices.add(mBluetoothDevice);
        mProfileConnectedDevices.add(mLeftBluetoothHapDevice);
        when(mHeadsetProfile.getConnectedDevices()).thenReturn(mProfileConnectedDevices);

        mEmptyDevices.addAll(mController.getConnectedHfpDevices());

        assertThat(mEmptyDevices).containsExactly(mBluetoothDevice, mLeftBluetoothHapDevice);
    }

    private class AudioSwitchPreferenceControllerTestable extends
            AudioSwitchPreferenceController {
        AudioSwitchPreferenceControllerTestable(Context context, String key) {
            super(context, key);
        }

        @Override
        public BluetoothDevice findActiveDevice() {
            return null;
        }

        @Override
        public String getPreferenceKey() {
            return TEST_KEY;
        }
    }
}
