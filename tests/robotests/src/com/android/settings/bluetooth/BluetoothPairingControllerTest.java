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

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settings.testutils.shadow.ShadowBluetoothPan;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(shadows = {ShadowBluetoothPan.class, ShadowBluetoothAdapter.class})
public class BluetoothPairingControllerTest {
    @Mock
    private BluetoothDevice mBluetoothDevice;
    private Context mContext;
    private BluetoothPairingController mBluetoothPairingController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        final Intent intent = new Intent();
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, mBluetoothDevice);
        mBluetoothPairingController = spy(new BluetoothPairingController(intent, mContext));
    }

    @Test
    public void onDialogPositiveClick_confirmationDialog_setPBAP() {
        mBluetoothPairingController.mType = PAIRING_VARIANT_CONSENT;
        mBluetoothPairingController.onCheckedChanged(null, true);

        mBluetoothPairingController.onDialogPositiveClick(null);

        verify(mBluetoothDevice).setPhonebookAccessPermission(BluetoothDevice.ACCESS_ALLOWED);
    }
}
