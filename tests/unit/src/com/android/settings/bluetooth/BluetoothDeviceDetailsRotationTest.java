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

import static org.mockito.Mockito.when;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;

import com.android.settings.SettingsActivity;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class BluetoothDeviceDetailsRotationTest {
    private Context mContext;
    private UiDevice mUiDevice;
    private Instrumentation mInstrumentation;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private CachedBluetoothDevice mCachedDevice;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private LocalBluetoothManager mBluetoothManager;

    private String mDeviceAddress;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mContext = InstrumentationRegistry.getTargetContext();
        mUiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        mInstrumentation = InstrumentationRegistry.getInstrumentation();

        mDeviceAddress = "AA:BB:CC:DD:EE:FF";
        when(mCachedDevice.getAddress()).thenReturn(mDeviceAddress);
        when(mCachedDevice.getName()).thenReturn("Mock Device");

        BluetoothDeviceDetailsFragment.sTestDataFactory =
                new BluetoothDeviceDetailsFragment.TestDataFactory() {
            @Override
            public CachedBluetoothDevice getDevice(String deviceAddress) {
                return mCachedDevice;
            }

            @Override
            public LocalBluetoothManager getManager(Context context) {
                return mBluetoothManager;
            }
        };
    }

    @Test
    public void rotation() {
        Intent intent = new Intent("android.settings.BLUETOOTH_SETTINGS");
        SettingsActivity activity = (SettingsActivity) mInstrumentation.startActivitySync(intent);
        Bundle args = new Bundle(1);
        args.putString(BluetoothDeviceDetailsFragment.KEY_DEVICE_ADDRESS, mDeviceAddress);
        activity.startPreferencePanel(null, BluetoothDeviceDetailsFragment.class.getName(), args,
               0, null, null, 0);
        try {
            mUiDevice.setOrientationLeft();
            mUiDevice.setOrientationNatural();
            mUiDevice.setOrientationRight();
            mUiDevice.setOrientationNatural();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }
}
