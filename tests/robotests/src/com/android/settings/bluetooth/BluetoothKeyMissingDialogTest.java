/*
 * Copyright (C) 2024 The Android Open Source Project
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

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.robolectric.shadows.ShadowLooper.shadowMainLooper;

import android.bluetooth.BluetoothDevice;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowAlertDialogCompat.class)
public class BluetoothKeyMissingDialogTest {
    @Mock private BluetoothDevice mBluetoothDevice;

    private BluetoothKeyMissingDialogFragment mFragment = null;
    private FragmentActivity mActivity = null;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mActivity = Robolectric.setupActivity(FragmentActivity.class);
        mFragment = new BluetoothKeyMissingDialogFragment(mBluetoothDevice);
        mActivity
                .getSupportFragmentManager()
                .beginTransaction()
                .add(mFragment, null)
                .commit();
        shadowMainLooper().idle();
    }

    @Test
    public void clickForgetDevice_removeBond() {
        mFragment.onClick(mFragment.getDialog(), AlertDialog.BUTTON_POSITIVE);

        verify(mBluetoothDevice).removeBond();
        assertThat(mActivity.isFinishing()).isTrue();
    }

    @Test
    public void clickCancel_notRemoveBond() {
        mFragment.onClick(mFragment.getDialog(), AlertDialog.BUTTON_NEGATIVE);

        verify(mBluetoothDevice, never()).removeBond();
        assertThat(mActivity.isFinishing()).isTrue();
    }
}
