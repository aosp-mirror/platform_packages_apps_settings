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

package com.android.settings.accessibility;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;
import static org.robolectric.shadows.ShadowLooper.shadowMainLooper;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.bluetooth.Utils;
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settings.testutils.shadow.ShadowBluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.CsipSetCoordinatorProfile;
import com.android.settingslib.bluetooth.HearingAidInfo;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfile;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;

import java.util.ArrayList;
import java.util.List;

/** Tests for {@link HearingAidUtils}. */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowAlertDialogCompat.class,
        com.android.settings.testutils.shadow.ShadowBluetoothAdapter.class,
        com.android.settings.testutils.shadow.ShadowBluetoothUtils.class,
})
public class HearingAidUtilsTest {

    @Rule
    public final MockitoRule mockito = MockitoJUnit.rule();
    private final Context mContext = ApplicationProvider.getApplicationContext();

    private static final String TEST_DEVICE_ADDRESS = "00:A1:A1:A1:A1:A1";
    private static final int TEST_LAUNCH_PAGE = 1;

    @Mock
    private CachedBluetoothDevice mCachedBluetoothDevice;
    @Mock
    private CachedBluetoothDevice mSubCachedBluetoothDevice;
    @Mock
    private LocalBluetoothManager mLocalBluetoothManager;
    @Mock
    private CachedBluetoothDeviceManager mCachedDeviceManager;
    @Mock
    private CsipSetCoordinatorProfile mCsipSetCoordinatorProfile;
    private BluetoothDevice mBluetoothDevice;
    private BluetoothAdapter mBluetoothAdapter;
    private ShadowBluetoothAdapter mShadowBluetoothAdapter;
    private FragmentManager mFragmentManager;

    @Before
    public void setUp() {
        setupEnvironment();
        final FragmentActivity mActivity = Robolectric.setupActivity(FragmentActivity.class);
        shadowMainLooper().idle();
        mFragmentManager = mActivity.getSupportFragmentManager();
        ShadowAlertDialogCompat.reset();
        when(mCachedBluetoothDevice.getAddress()).thenReturn(TEST_DEVICE_ADDRESS);
    }

    @Test
    public void launchHearingAidPairingDialog_deviceIsNotConnectedAshaHearingAid_noDialog() {
        when(mCachedBluetoothDevice.isConnectedAshaHearingAidDevice()).thenReturn(false);

        HearingAidUtils.launchHearingAidPairingDialog(mFragmentManager, mCachedBluetoothDevice,
                TEST_LAUNCH_PAGE);

        shadowMainLooper().idle();
        final AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNull();
    }

    @Test
    public void launchHearingAidPairingDialog_deviceIsMonauralMode_noDialog() {
        when(mCachedBluetoothDevice.isConnectedAshaHearingAidDevice()).thenReturn(true);
        when(mCachedBluetoothDevice.getDeviceMode()).thenReturn(
                HearingAidInfo.DeviceMode.MODE_MONAURAL);

        HearingAidUtils.launchHearingAidPairingDialog(mFragmentManager, mCachedBluetoothDevice,
                TEST_LAUNCH_PAGE);

        shadowMainLooper().idle();
        final AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNull();
    }

    @Test
    public void launchHearingAidPairingDialog_deviceHasSubDevice_noDialog() {
        when(mCachedBluetoothDevice.isConnectedAshaHearingAidDevice()).thenReturn(true);
        when(mCachedBluetoothDevice.getDeviceMode()).thenReturn(
                HearingAidInfo.DeviceMode.MODE_BINAURAL);
        when(mCachedBluetoothDevice.getSubDevice()).thenReturn(mSubCachedBluetoothDevice);

        HearingAidUtils.launchHearingAidPairingDialog(mFragmentManager, mCachedBluetoothDevice,
                TEST_LAUNCH_PAGE);

        shadowMainLooper().idle();
        final AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNull();
    }

    @Test
    public void launchHearingAidPairingDialog_deviceIsInvalidSide_noDialog() {
        when(mCachedBluetoothDevice.isConnectedAshaHearingAidDevice()).thenReturn(true);
        when(mCachedBluetoothDevice.getDeviceMode()).thenReturn(
                HearingAidInfo.DeviceMode.MODE_BINAURAL);
        when(mCachedBluetoothDevice.getDeviceSide()).thenReturn(
                HearingAidInfo.DeviceSide.SIDE_INVALID);

        HearingAidUtils.launchHearingAidPairingDialog(mFragmentManager, mCachedBluetoothDevice,
                TEST_LAUNCH_PAGE);

        shadowMainLooper().idle();
        final AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNull();
    }

    @Test
    public void launchHearingAidPairingDialog_deviceSupportsCsip_noDialog() {
        when(mCachedBluetoothDevice.isConnectedAshaHearingAidDevice()).thenReturn(true);
        when(mCachedBluetoothDevice.getDeviceMode()).thenReturn(
                HearingAidInfo.DeviceMode.MODE_BINAURAL);
        when(mCachedBluetoothDevice.getDeviceSide()).thenReturn(
                HearingAidInfo.DeviceSide.SIDE_LEFT);
        makeDeviceSupportCsip();

        HearingAidUtils.launchHearingAidPairingDialog(mFragmentManager, mCachedBluetoothDevice,
                TEST_LAUNCH_PAGE);

        shadowMainLooper().idle();
        final AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNull();
    }

    @Test
    public void launchHearingAidPairingDialog_dialogShown() {
        when(mCachedBluetoothDevice.isConnectedAshaHearingAidDevice()).thenReturn(true);
        when(mCachedBluetoothDevice.getDeviceMode()).thenReturn(
                HearingAidInfo.DeviceMode.MODE_BINAURAL);
        when(mCachedBluetoothDevice.getDeviceSide()).thenReturn(
                HearingAidInfo.DeviceSide.SIDE_LEFT);

        HearingAidUtils.launchHearingAidPairingDialog(mFragmentManager, mCachedBluetoothDevice,
                TEST_LAUNCH_PAGE);

        shadowMainLooper().idle();
        final AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog.isShowing()).isTrue();
    }

    private void makeDeviceSupportCsip() {
        List<LocalBluetoothProfile> uuids = new ArrayList<>();
        uuids.add(mCsipSetCoordinatorProfile);
        when(mCachedBluetoothDevice.getProfiles()).thenReturn(uuids);
    }

    private void setupEnvironment() {
        ShadowBluetoothUtils.sLocalBluetoothManager = mLocalBluetoothManager;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mLocalBluetoothManager = Utils.getLocalBtManager(mContext);
        mShadowBluetoothAdapter = Shadow.extract(mBluetoothAdapter);
        mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(TEST_DEVICE_ADDRESS);
        mShadowBluetoothAdapter.addSupportedProfiles(BluetoothProfile.HEARING_AID);
        when(mLocalBluetoothManager.getCachedDeviceManager()).thenReturn(mCachedDeviceManager);
        when(mCachedDeviceManager.findDevice(mBluetoothDevice)).thenReturn(mCachedBluetoothDevice);
        when(mCachedBluetoothDevice.getAddress()).thenReturn(TEST_DEVICE_ADDRESS);
    }
}
