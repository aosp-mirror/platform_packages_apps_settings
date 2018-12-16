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

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowDialog;

@RunWith(RobolectricTestRunner.class)
public class ForgetDeviceDialogFragmentTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private CachedBluetoothDevice mCachedDevice;

    private ForgetDeviceDialogFragment mFragment;
    private FragmentActivity mActivity;
    private AlertDialog mDialog;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest();
        String deviceAddress = "55:66:77:88:99:AA";
        when(mCachedDevice.getAddress()).thenReturn(deviceAddress);
        mFragment = spy(ForgetDeviceDialogFragment.newInstance(deviceAddress));
        doReturn(mCachedDevice).when(mFragment).getDevice(any());
        mActivity = Robolectric.setupActivity(FragmentActivity.class);
        mActivity.getSupportFragmentManager().beginTransaction().add(mFragment, null).commit();
        mDialog = (AlertDialog) ShadowDialog.getLatestDialog();
    }

    @Test
    public void cancelDialog() {
        mDialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
        verify(mCachedDevice, never()).unpair();
        assertThat(mActivity.isFinishing()).isFalse();
    }

    @Test
    public void confirmDialog() {
        mDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        verify(mCachedDevice).unpair();
        assertThat(mActivity.isFinishing()).isTrue();
    }
}
