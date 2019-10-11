/*
 * Copyright (C) 2017 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import com.android.settings.R;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.shadows.androidx.fragment.FragmentController;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowAlertDialogCompat.class})
public class ForgetDeviceDialogFragmentTest {

    private static final String DEVICE_NAME = "Nightshade";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private CachedBluetoothDevice mCachedDevice;
    @Mock
    private BluetoothDevice mBluetoothDevice;

    private ForgetDeviceDialogFragment mFragment;
    private FragmentActivity mActivity;
    private AlertDialog mDialog;
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        FakeFeatureFactory.setupForTest();
        String deviceAddress = "55:66:77:88:99:AA";
        when(mCachedDevice.getAddress()).thenReturn(deviceAddress);
        when(mCachedDevice.getDevice()).thenReturn(mBluetoothDevice);
        when(mCachedDevice.getName()).thenReturn(DEVICE_NAME);
        mFragment = spy(ForgetDeviceDialogFragment.newInstance(deviceAddress));
        doReturn(mCachedDevice).when(mFragment).getDevice(any());
        mActivity = Robolectric.setupActivity(FragmentActivity.class);
    }

    @Test
    public void cancelDialog() {
        initDialog();

        mDialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
        verify(mCachedDevice, never()).unpair();
        assertThat(mActivity.isFinishing()).isFalse();
    }

    @Test
    public void confirmDialog() {
        initDialog();

        mDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        verify(mCachedDevice).unpair();
        assertThat(mActivity.isFinishing()).isTrue();
    }

    @Test
    public void createDialog_untetheredDevice_showUntetheredMessage() {
        when(mBluetoothDevice.getMetadata(BluetoothDevice.METADATA_IS_UNTETHERED_HEADSET))
                .thenReturn("true".getBytes());

        FragmentController.setupFragment(mFragment, FragmentActivity.class,
                0 /* containerViewId */, null /* bundle */);
        final AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        ShadowAlertDialogCompat shadowDialog = ShadowAlertDialogCompat.shadowOf(dialog);

        assertThat(shadowDialog.getMessage()).isEqualTo(
                mContext.getString(R.string.bluetooth_untethered_unpair_dialog_body, DEVICE_NAME));
    }

    @Test
    public void createDialog_normalDevice_showNormalMessage() {
        when(mBluetoothDevice.getMetadata(BluetoothDevice.METADATA_IS_UNTETHERED_HEADSET))
                .thenReturn("false".getBytes());

        FragmentController.setupFragment(mFragment, FragmentActivity.class,
                0 /* containerViewId */, null /* bundle */);
        final AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        ShadowAlertDialogCompat shadowDialog = ShadowAlertDialogCompat.shadowOf(dialog);

        assertThat(shadowDialog.getMessage()).isEqualTo(
                mContext.getString(R.string.bluetooth_unpair_dialog_body, DEVICE_NAME));
    }

    private void initDialog() {
        mActivity.getSupportFragmentManager().beginTransaction().add(mFragment, null).commit();
        mDialog = (AlertDialog) ShadowDialog.getLatestDialog();
    }
}
