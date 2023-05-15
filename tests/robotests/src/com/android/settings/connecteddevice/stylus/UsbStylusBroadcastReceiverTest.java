/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.connecteddevice.stylus;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class UsbStylusBroadcastReceiverTest {
    private Context mContext;
    private UsbStylusBroadcastReceiver mReceiver;
    private FakeFeatureFactory mFeatureFactory;
    @Mock
    private UsbStylusBroadcastReceiver.UsbStylusConnectionListener mListener;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mReceiver = new UsbStylusBroadcastReceiver(mContext, mListener);
        mFeatureFactory = FakeFeatureFactory.setupForTest();
    }

    @Test
    public void onReceive_usbDeviceAttachedStylus_invokeCallback() {
        when(mFeatureFactory.mStylusFeatureProvider.isUsbFirmwareUpdateEnabled(any()))
                .thenReturn(true);
        final UsbDevice usbDevice = mock(UsbDevice.class);
        final Intent intent = new Intent();
        intent.setAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        intent.putExtra(UsbManager.EXTRA_DEVICE, usbDevice);

        mReceiver.onReceive(mContext, intent);

        verify(mListener).onUsbStylusConnectionChanged(usbDevice, true);
    }

    @Test
    public void onReceive_usbDeviceDetachedStylus_invokeCallback() {
        when(mFeatureFactory.mStylusFeatureProvider.isUsbFirmwareUpdateEnabled(any()))
                .thenReturn(true);
        final UsbDevice usbDevice = mock(UsbDevice.class);
        final Intent intent = new Intent();
        intent.setAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        intent.putExtra(UsbManager.EXTRA_DEVICE, usbDevice);

        mReceiver.onReceive(mContext, intent);

        verify(mListener).onUsbStylusConnectionChanged(usbDevice, false);
    }

    @Test
    public void onReceive_usbDeviceAttachedNotStylus_doesNotInvokeCallback() {
        when(mFeatureFactory.mStylusFeatureProvider.isUsbFirmwareUpdateEnabled(any()))
                .thenReturn(false);
        final UsbDevice usbDevice = mock(UsbDevice.class);
        final Intent intent = new Intent();
        intent.setAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        intent.putExtra(UsbManager.EXTRA_DEVICE, usbDevice);

        mReceiver.onReceive(mContext, intent);

        verifyNoMoreInteractions(mListener);
    }
}
