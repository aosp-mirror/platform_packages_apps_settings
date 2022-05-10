/*
 * Copyright (C) 2021 The Android Open Source Project
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

import static com.android.settings.bluetooth.BluetoothPairingService.ACTION_DISMISS_PAIRING;
import static com.android.settings.bluetooth.BluetoothPairingService.ACTION_PAIRING_DIALOG;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.app.NotificationManager;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.util.DisplayMetrics;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
public class BluetoothPairingServiceTest {

    private final String mFakeTicker = "fake_ticker";

    @Mock
    private NotificationManager mNm;
    @Mock
    private BluetoothDevice mDevice;
    @Mock
    private Context mContext;
    @Mock
    private Resources mResources;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private DisplayMetrics mDisplayMetrics;

    private BluetoothPairingService mBluetoothPairingService;
    private Application mApplication;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mBluetoothPairingService = new BluetoothPairingService();
        mBluetoothPairingService.mNm = mNm;
        mApplication = RuntimeEnvironment.application;

        ReflectionHelpers.setField(mBluetoothPairingService, "mBase", mContext);
        when(mContext.getResources()).thenReturn(mResources);
        when(mResources.getString(R.string.bluetooth_notif_ticker)).thenReturn(mFakeTicker);
        when(mContext.getApplicationInfo()).thenReturn(new ApplicationInfo());
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mResources.getDisplayMetrics()).thenReturn(mDisplayMetrics);
        mDisplayMetrics.density = 1.5f;
    }

    @Test
    public void receivePairingRequestAction_notificationShown() {
        Intent intent = new Intent();
        intent.setAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, mDevice);
        intent.putExtra(BluetoothDevice.EXTRA_NAME, "fake_name");
        when(mDevice.getBondState()).thenReturn(BluetoothDevice.BOND_BONDING);

        mBluetoothPairingService.onStartCommand(intent, /* flags */ 0, /* startId */ 0);

        verify(mNm).notify(eq(mBluetoothPairingService.NOTIFICATION_ID), any());
    }

    @Test
    public void receiveDismissPairingAction_cancelPairing() {
        Intent intent = new Intent();
        intent.setAction(ACTION_DISMISS_PAIRING);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, mDevice);
        intent.putExtra(BluetoothDevice.EXTRA_NAME, "fake_name");
        when(mDevice.getBondState()).thenReturn(BluetoothDevice.BOND_BONDING);

        mBluetoothPairingService.onStartCommand(intent, /* flags */ 0, /* startId */ 0);

        verify(mDevice).cancelBondProcess();
        verify(mNm).cancel(mBluetoothPairingService.NOTIFICATION_ID);
    }

    @Test
    public void receivePairingDialogAction_startActivity() {
        Intent intent = new Intent();
        intent.setAction(ACTION_PAIRING_DIALOG);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, mDevice);
        intent.putExtra(BluetoothDevice.EXTRA_NAME, "fake_name");
        when(mDevice.getBondState()).thenReturn(BluetoothDevice.BOND_BONDING);

        mBluetoothPairingService.onStartCommand(intent, /* flags */ 0, /* startId */ 0);

        verify(mContext).startActivity(any());
    }
}
