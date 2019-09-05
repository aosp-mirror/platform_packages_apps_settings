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
 * limitations under the License
 */
package com.android.settings.connecteddevice.usb;

import static android.hardware.usb.UsbPortStatus.CONTAMINANT_DETECTION_NOT_SUPPORTED;
import static android.hardware.usb.UsbPortStatus.CONTAMINANT_PROTECTION_NONE;
import static android.hardware.usb.UsbPortStatus.DATA_ROLE_DEVICE;
import static android.hardware.usb.UsbPortStatus.DATA_ROLE_NONE;
import static android.hardware.usb.UsbPortStatus.POWER_ROLE_NONE;
import static android.hardware.usb.UsbPortStatus.POWER_ROLE_SINK;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbPortStatus;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowApplication.Wrapper;

@RunWith(RobolectricTestRunner.class)
public class UsbConnectionBroadcastReceiverTest {

    private Context mContext;
    private UsbConnectionBroadcastReceiver mReceiver;

    @Mock
    private UsbConnectionBroadcastReceiver.UsbConnectionListener mListener;
    @Mock
    private UsbBackend mUsbBackend;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mReceiver = new UsbConnectionBroadcastReceiver(mContext, mListener, mUsbBackend);
    }

    @Test
    public void onReceive_usbConnected_invokeCallback() {
        final Intent intent = new Intent();
        intent.setAction(UsbManager.ACTION_USB_STATE);
        intent.putExtra(UsbManager.USB_CONNECTED, true);

        mReceiver.onReceive(mContext, intent);

        verify(mListener).onUsbConnectionChanged(true /* connected */, UsbManager.FUNCTION_NONE,
                POWER_ROLE_NONE, DATA_ROLE_NONE);
    }

    @Test
    public void onReceive_usbDisconnected_invokeCallback() {
        final Intent intent = new Intent();
        intent.setAction(UsbManager.ACTION_USB_STATE);
        intent.putExtra(UsbManager.USB_CONNECTED, false);

        mReceiver.onReceive(mContext, intent);

        verify(mListener).onUsbConnectionChanged(false /* connected */, UsbManager.FUNCTION_NONE,
                POWER_ROLE_NONE, DATA_ROLE_NONE);
    }

    @Test
    public void onReceive_usbConnectedMtpEnabled_invokeCallback() {
        final Intent intent = new Intent();
        intent.setAction(UsbManager.ACTION_USB_STATE);
        intent.putExtra(UsbManager.USB_CONNECTED, true);
        intent.putExtra(UsbManager.USB_FUNCTION_MTP, true);
        intent.putExtra(UsbManager.USB_DATA_UNLOCKED, true);

        mReceiver.onReceive(mContext, intent);

        verify(mListener).onUsbConnectionChanged(true /* connected */, UsbManager.FUNCTION_MTP,
                POWER_ROLE_NONE, DATA_ROLE_NONE);
    }

    @Test
    public void onReceive_usbPortStatus_invokeCallback() {
        final Intent intent = new Intent();
        intent.setAction(UsbManager.ACTION_USB_PORT_CHANGED);
        final UsbPortStatus status = new UsbPortStatus(0, POWER_ROLE_SINK,
                DATA_ROLE_DEVICE, 0, CONTAMINANT_PROTECTION_NONE,
                CONTAMINANT_DETECTION_NOT_SUPPORTED);
        intent.putExtra(UsbManager.EXTRA_PORT_STATUS, status);

        mReceiver.onReceive(mContext, intent);

        verify(mListener).onUsbConnectionChanged(false /* connected */, UsbManager.FUNCTION_NONE,
                POWER_ROLE_SINK, DATA_ROLE_DEVICE);
    }

    @Test
    public void register_invokeMethodTwice_registerOnce() {
        mReceiver.register();
        mReceiver.register();

        assertThat(countUsbConnectionBroadcastReceivers()).isEqualTo(1);
    }

    @Test
    public void unregister_invokeMethodTwice_unregisterOnce() {
        mReceiver.register();
        mReceiver.unregister();
        mReceiver.unregister();

        assertThat(countUsbConnectionBroadcastReceivers()).isEqualTo(0);
    }

    private int countUsbConnectionBroadcastReceivers() {
        int count = 0;
        for (Wrapper wrapper : ShadowApplication.getInstance().getRegisteredReceivers()) {
            if (wrapper.getBroadcastReceiver() instanceof UsbConnectionBroadcastReceiver) {
                count++;
            }
        }
        return count;
    }
}
