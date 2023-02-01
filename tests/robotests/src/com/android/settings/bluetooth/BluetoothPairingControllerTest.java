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
package com.android.settings.bluetooth;

import static android.bluetooth.BluetoothDevice.PAIRING_VARIANT_CONSENT;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Parcel;

import com.android.settings.core.SettingsUIDeviceConfig;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settings.testutils.shadow.ShadowBluetoothUtils;
import com.android.settings.testutils.shadow.ShadowDeviceConfig;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfile;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowBluetoothAdapter.class, ShadowBluetoothUtils.class,
        ShadowDeviceConfig.class})
public class BluetoothPairingControllerTest {
    private final BluetoothClass mBluetoothClass =
            createBtClass(BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE);

    @Mock
    private CachedBluetoothDeviceManager mCachedDeviceManager;
    @Mock
    private LocalBluetoothManager mLocalBluetoothManager;
    @Mock
    private LocalBluetoothProfileManager mLocalBluetoothProfileManager;
    @Mock
    private BluetoothDevice mBluetoothDevice;
    @Mock
    private CachedBluetoothDevice mCachedDevice;
    @Mock
    private LocalBluetoothProfile mLocalBluetoothProfile;
    @Mock
    private LocalBluetoothProfile mPbapLocalBluetoothProfile;

    private Context mContext;
    private BluetoothPairingController mBluetoothPairingController;
    private ShadowBluetoothAdapter mShadowBluetoothAdapter;

    private BluetoothClass createBtClass(int deviceClass) {
        Parcel p = Parcel.obtain();
        p.writeInt(deviceClass);
        p.setDataPosition(0); // reset position of parcel before passing to constructor

        BluetoothClass bluetoothClass = BluetoothClass.CREATOR.createFromParcel(p);
        p.recycle();
        return bluetoothClass;
    }

    private BluetoothPairingController createBluetoothPairingController() {
        final Intent intent = new Intent();
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, mBluetoothDevice);
        return new BluetoothPairingController(intent, mContext);
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mShadowBluetoothAdapter = Shadow.extract(BluetoothAdapter.getDefaultAdapter());
        mShadowBluetoothAdapter.setEnabled(true);
        ShadowBluetoothUtils.sLocalBluetoothManager = mLocalBluetoothManager;
        mLocalBluetoothManager = Utils.getLocalBtManager(mContext);
        when(mLocalBluetoothManager.getCachedDeviceManager()).thenReturn(mCachedDeviceManager);
        when(mLocalBluetoothManager.getProfileManager()).thenReturn(mLocalBluetoothProfileManager);
        when(mCachedDeviceManager.findDevice(mBluetoothDevice)).thenReturn(mCachedDevice);
        List<LocalBluetoothProfile> localBluetoothProfiles = new ArrayList<>();
        mockIsLeAudio(false);
        localBluetoothProfiles.add(mLocalBluetoothProfile);
        when(mCachedDevice.getProfiles()).thenReturn(localBluetoothProfiles);
        when(mCachedDevice.getDevice()).thenReturn(mBluetoothDevice);

        mBluetoothPairingController = createBluetoothPairingController();
        mBluetoothPairingController.mockPbapClientProfile(mPbapLocalBluetoothProfile);
    }

    @Test
    public void onDialogPositiveClick_confirmationDialog_setPBAP() {
        mBluetoothPairingController.mType = PAIRING_VARIANT_CONSENT;
        mBluetoothPairingController.onCheckedChanged(null, true);

        mBluetoothPairingController.onDialogPositiveClick(null);

        verify(mBluetoothDevice).setPhonebookAccessPermission(BluetoothDevice.ACCESS_ALLOWED);
    }

    @Test
    public void onSetContactSharingState_permissionAllowed_setPBAPAllowed() {
        when(mBluetoothDevice.getPhonebookAccessPermission()).thenReturn(
                BluetoothDevice.ACCESS_ALLOWED);
        mBluetoothPairingController.setContactSharingState();
        mBluetoothPairingController.onDialogPositiveClick(null);

        verify(mBluetoothDevice).setPhonebookAccessPermission(BluetoothDevice.ACCESS_ALLOWED);
    }

    @Test
    public void onSetContactSharingState_permissionUnknown_audioVideoHandsfree_setPBAPAllowed() {
        when(mBluetoothDevice.getPhonebookAccessPermission()).thenReturn(
                BluetoothDevice.ACCESS_UNKNOWN);
        when(mBluetoothDevice.getBluetoothClass()).thenReturn(mBluetoothClass);
        mBluetoothPairingController.setContactSharingState();
        mBluetoothPairingController.onDialogPositiveClick(null);

        verify(mBluetoothDevice).setPhonebookAccessPermission(BluetoothDevice.ACCESS_ALLOWED);
    }

    @Test
    public void onSetContactSharingState_permissionRejected_setPBAPRejected() {
        when(mBluetoothDevice.getPhonebookAccessPermission()).thenReturn(
                BluetoothDevice.ACCESS_REJECTED);
        when(mBluetoothDevice.getBluetoothClass()).thenReturn(mBluetoothClass);
        mBluetoothPairingController.setContactSharingState();
        mBluetoothPairingController.onDialogPositiveClick(null);

        verify(mBluetoothDevice).setPhonebookAccessPermission(BluetoothDevice.ACCESS_REJECTED);
    }

    @Test
    public void isLeAudio_noLeProfile_returnsFalse() {
        mockIsLeAudio(false);

        mBluetoothPairingController = createBluetoothPairingController();

        assertThat(mBluetoothPairingController.isLeAudio()).isFalse();
    }

    @Test
    public void isLeAudio_isLeProfile_returnsTrue() {
        mockIsLeAudio(true);

        mBluetoothPairingController = createBluetoothPairingController();

        assertThat(mBluetoothPairingController.isLeAudio()).isTrue();
    }

    @Test
    public void isLeContactSharingEnabled_configIsFalse_returnsFalse() {
        mockIsLeContactSharingEnabled(false);

        mBluetoothPairingController = createBluetoothPairingController();

        assertThat(mBluetoothPairingController.isLeContactSharingEnabled()).isFalse();
    }

    @Test
    public void isLeContactSharingEnabled_configIsTrue_returnsTrue() {
        mockIsLeContactSharingEnabled(true);

        mBluetoothPairingController = createBluetoothPairingController();

        assertThat(mBluetoothPairingController.isLeContactSharingEnabled()).isTrue();
    }

    @Test
    public void isContactSharingVisible_profileIsNotReady_returnsTrue() {
        // isProfileReady=false, isLeAudio=false, isLeContactSharingEnabled=true
        mockIsProfileReady(false);
        mockIsLeAudio(false);
        mockIsLeContactSharingEnabled(true);

        mBluetoothPairingController = createBluetoothPairingController();
        mBluetoothPairingController.mockPbapClientProfile(mPbapLocalBluetoothProfile);

        assertThat(mBluetoothPairingController.isContactSharingVisible()).isTrue();
    }

    @Test
    public void isContactSharingVisible_profileIsReady_returnsFalse() {
        // isProfileReady=true, isLeAudio=false, isLeContactSharingEnabled=true
        mockIsProfileReady(true);
        mockIsLeAudio(false);
        mockIsLeContactSharingEnabled(true);

        mBluetoothPairingController = createBluetoothPairingController();
        mBluetoothPairingController.mockPbapClientProfile(mPbapLocalBluetoothProfile);

        assertThat(mBluetoothPairingController.isContactSharingVisible()).isFalse();
    }

    @Test
    public void isContactSharingVisible_DeviceIsLeAudioAndProfileIsReady_returnsFalse() {
        // isProfileReady=true, isLeAudio=true, isLeContactSharingEnabled=true
        mockIsProfileReady(true);
        mockIsLeAudio(true);
        mockIsLeContactSharingEnabled(true);

        mBluetoothPairingController = createBluetoothPairingController();
        mBluetoothPairingController.mockPbapClientProfile(mPbapLocalBluetoothProfile);

        assertThat(mBluetoothPairingController.isContactSharingVisible()).isFalse();
    }

    @Test
    public void isContactSharingVisible_DeviceIsLeAudioAndProfileIsNotReady_returnsTrue() {
        // isProfileReady=false, isLeAudio=true, isLeContactSharingEnabled=true
        mockIsProfileReady(false);
        mockIsLeAudio(true);
        mockIsLeContactSharingEnabled(true);

        mBluetoothPairingController = createBluetoothPairingController();
        mBluetoothPairingController.mockPbapClientProfile(mPbapLocalBluetoothProfile);

        assertThat(mBluetoothPairingController.isContactSharingVisible()).isTrue();
    }

    @Test
    public void isContactSharingVisible_DeviceIsLeAndContactSharingIsNotEnabled_returnsFalse() {
        // isProfileReady=false, isLeAudio=true, isLeContactSharingEnabled=false
        mockIsProfileReady(false);
        mockIsLeAudio(true);
        mockIsLeContactSharingEnabled(false);

        mBluetoothPairingController = createBluetoothPairingController();
        mBluetoothPairingController.mockPbapClientProfile(mPbapLocalBluetoothProfile);

        assertThat(mBluetoothPairingController.isContactSharingVisible()).isFalse();
    }

    private void mockIsProfileReady(boolean mockValue) {
        when(mPbapLocalBluetoothProfile.isProfileReady()).thenReturn(mockValue);
    }

    private void mockIsLeAudio(boolean mockValue) {
        int profileId = BluetoothProfile.HEADSET;
        if (mockValue) {
            profileId = BluetoothProfile.LE_AUDIO;
        }
        when(mLocalBluetoothProfile.getProfileId()).thenReturn(profileId);
    }

    private void mockIsLeContactSharingEnabled(boolean mockValue) {
        android.provider.DeviceConfig.setProperty(
                android.provider.DeviceConfig.NAMESPACE_SETTINGS_UI,
                SettingsUIDeviceConfig.BT_LE_AUDIO_CONTACT_SHARING_ENABLED,
                /* value= */ mockValue ? "true" : "false", true);
    }
}
