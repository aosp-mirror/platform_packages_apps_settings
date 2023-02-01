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

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.HearingAidProfile;

import org.junit.After;
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

/** Tests for {@link HearingAidUtils}. */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowAlertDialogCompat.class)
public class HearingAidUtilsTest {

    @Rule
    public final MockitoRule mockito = MockitoJUnit.rule();

    @Mock
    private CachedBluetoothDevice mCachedBluetoothDevice;
    @Mock
    private CachedBluetoothDevice mSubCachedBluetoothDevice;

    private FragmentManager mFragmentManager;

    @Before
    public void setUp() {
        final FragmentActivity mActivity = Robolectric.setupActivity(FragmentActivity.class);
        mFragmentManager = mActivity.getSupportFragmentManager();
    }

    @After
    public void tearDown() {
        ShadowAlertDialogCompat.reset();
    }
    @Test
    public void launchHearingAidPairingDialog_deviceNotConnectedHearingAid_noDialog() {
        when(mCachedBluetoothDevice.isConnectedHearingAidDevice()).thenReturn(false);

        HearingAidUtils.launchHearingAidPairingDialog(mFragmentManager, mCachedBluetoothDevice);

        final AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNull();
    }

    @Test
    public void launchHearingAidPairingDialog_deviceIsModeMonaural_noDialog() {
        when(mCachedBluetoothDevice.isConnectedHearingAidDevice()).thenReturn(true);
        when(mCachedBluetoothDevice.getDeviceMode()).thenReturn(
                HearingAidProfile.DeviceMode.MODE_MONAURAL);

        HearingAidUtils.launchHearingAidPairingDialog(mFragmentManager, mCachedBluetoothDevice);

        final AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNull();
    }

    @Test
    public void launchHearingAidPairingDialog_deviceHasSubDevice_noDialog() {
        when(mCachedBluetoothDevice.isConnectedHearingAidDevice()).thenReturn(true);
        when(mCachedBluetoothDevice.getDeviceMode()).thenReturn(
                HearingAidProfile.DeviceMode.MODE_BINAURAL);
        when(mCachedBluetoothDevice.getSubDevice()).thenReturn(mSubCachedBluetoothDevice);

        HearingAidUtils.launchHearingAidPairingDialog(mFragmentManager, mCachedBluetoothDevice);

        final AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNull();
    }

    @Test
    public void launchHearingAidPairingDialog_deviceIsInvalidSide_noDialog() {
        when(mCachedBluetoothDevice.isConnectedHearingAidDevice()).thenReturn(true);
        when(mCachedBluetoothDevice.getDeviceMode()).thenReturn(
                HearingAidProfile.DeviceMode.MODE_BINAURAL);
        when(mCachedBluetoothDevice.getDeviceSide()).thenReturn(
                HearingAidProfile.DeviceSide.SIDE_INVALID);

        HearingAidUtils.launchHearingAidPairingDialog(mFragmentManager, mCachedBluetoothDevice);

        final AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNull();
    }

    @Test
    public void launchHearingAidPairingDialog_dialogShown() {
        when(mCachedBluetoothDevice.isConnectedHearingAidDevice()).thenReturn(true);
        when(mCachedBluetoothDevice.getDeviceMode()).thenReturn(
                HearingAidProfile.DeviceMode.MODE_BINAURAL);
        when(mCachedBluetoothDevice.getDeviceSide()).thenReturn(
                HearingAidProfile.DeviceSide.SIDE_LEFT);

        HearingAidUtils.launchHearingAidPairingDialog(mFragmentManager, mCachedBluetoothDevice);

        final AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog.isShowing()).isTrue();
    }
}
