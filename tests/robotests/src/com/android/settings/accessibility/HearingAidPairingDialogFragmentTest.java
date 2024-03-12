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

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.bluetooth.BluetoothPairingDetail;
import com.android.settings.bluetooth.HearingAidPairingDialogFragment;
import com.android.settings.bluetooth.Utils;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settings.testutils.shadow.ShadowBluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.HearingAidInfo;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

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
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadow.api.Shadow;

/** Tests for {@link HearingAidPairingDialogFragment}. */
@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowAlertDialogCompat.class,
        com.android.settings.testutils.shadow.ShadowBluetoothAdapter.class,
        com.android.settings.testutils.shadow.ShadowBluetoothUtils.class,
        com.android.settings.testutils.shadow.ShadowFragment.class,
})
public class HearingAidPairingDialogFragmentTest {

    @Rule
    public final MockitoRule mockito = MockitoJUnit.rule();

    private static final String TEST_DEVICE_ADDRESS = "00:A1:A1:A1:A1:A1";
    private static final int TEST_LAUNCH_PAGE = SettingsEnums.SETTINGS_CONNECTED_DEVICE_CATEGORY;

    private final Context mContext = ApplicationProvider.getApplicationContext();
    @Mock
    private CachedBluetoothDevice mCachedBluetoothDevice;
    @Mock
    private CachedBluetoothDevice mCachedSubBluetoothDevice;
    @Mock
    private LocalBluetoothManager mLocalBluetoothManager;
    @Mock
    private CachedBluetoothDeviceManager mCachedDeviceManager;
    private BluetoothAdapter mBluetoothAdapter;
    private FragmentActivity mActivity;
    private HearingAidPairingDialogFragment mFragment;
    private ShadowBluetoothAdapter mShadowBluetoothAdapter;
    private BluetoothDevice mBluetoothDevice;
    private FragmentManager mFragmentManager;

    @Before
    public void setUp() {
        setupEnvironment();
        setupDialog(TEST_LAUNCH_PAGE);
    }

    @Test
    public void newInstance_deviceSideRight_argumentSideRight() {
        when(mCachedBluetoothDevice.getDeviceSide()).thenReturn(
                HearingAidInfo.DeviceSide.SIDE_RIGHT);
        final AlertDialog dialog = (AlertDialog) mFragment.onCreateDialog(Bundle.EMPTY);
        dialog.show();

        final String pairLeftString = mContext.getText(
                R.string.bluetooth_pair_other_ear_dialog_left_ear_positive_button).toString();
        assertThat(dialog.getButton(
                DialogInterface.BUTTON_POSITIVE).getText().toString()).isEqualTo(pairLeftString);
    }

    @Test
    public void dialogPositiveButtonClick_intentToBluetoothPairingPage() {
        setupDialog(SettingsEnums.SETTINGS_CONNECTED_DEVICE_CATEGORY);
        final AlertDialog dialog = (AlertDialog) mFragment.onCreateDialog(Bundle.EMPTY);
        dialog.show();

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();

        final Intent intent = shadowOf(mActivity).getNextStartedActivity();
        assertThat(intent.getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT))
                .isEqualTo(BluetoothPairingDetail.class.getName());
    }

    @Test
    public void dialogPositiveButtonClick_intentToA11yPairingPage() {
        setupDialog(SettingsEnums.ACCESSIBILITY);
        final AlertDialog dialog = (AlertDialog) mFragment.onCreateDialog(Bundle.EMPTY);
        dialog.show();

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();

        final Intent intent = shadowOf(mActivity).getNextStartedActivity();
        assertThat(intent.getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT))
                .isEqualTo(HearingDevicePairingDetail.class.getName());
    }

    @Test
    public void dialogNegativeButtonClick_dismissDialog() {
        final AlertDialog dialog = (AlertDialog) mFragment.onCreateDialog(Bundle.EMPTY);
        dialog.show();

        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).performClick();

        assertThat(dialog.isShowing()).isFalse();
    }

    @Test
    public void getMetricsCategory_returnsCorrectCategory() {
        assertThat(mFragment.getMetricsCategory()).isEqualTo(
                SettingsEnums.DIALOG_ACCESSIBILITY_HEARING_AID_PAIR_ANOTHER);
    }

    @Test
    public void onDeviceAttributesChanged_subAshaHearingAidDeviceConnected_dialogDismiss() {
        when(mCachedSubBluetoothDevice.isConnectedAshaHearingAidDevice()).thenReturn(true);
        when(mCachedBluetoothDevice.getSubDevice()).thenReturn(mCachedSubBluetoothDevice);

        mFragment.onDeviceAttributesChanged();

        verify(mFragment).dismiss();
    }

    private void setupDialog(int launchPage) {
        mFragment = spy(
                HearingAidPairingDialogFragment.newInstance(TEST_DEVICE_ADDRESS, launchPage));
        mActivity = Robolectric.setupActivity(FragmentActivity.class);
        mFragmentManager = mActivity.getSupportFragmentManager();
        when(mFragment.getActivity()).thenReturn(mActivity);
        doReturn(mFragmentManager).when(mFragment).getParentFragmentManager();
        mFragment.onAttach(mContext);
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
