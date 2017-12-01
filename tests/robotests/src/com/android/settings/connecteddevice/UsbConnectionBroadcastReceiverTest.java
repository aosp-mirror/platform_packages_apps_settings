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
package com.android.settings.connecteddevice;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbManager;

import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class UsbConnectionBroadcastReceiverTest {
    private Context mContext;
    private UsbConnectionBroadcastReceiver mReceiver;
    private ShadowApplication mShadowApplication;

    @Mock
    private UsbConnectionBroadcastReceiver.UsbConnectionListener mListener;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mShadowApplication = ShadowApplication.getInstance();
        mContext = RuntimeEnvironment.application;
        mReceiver = new UsbConnectionBroadcastReceiver(mContext, mListener);
    }

    @Test
    public void testOnReceive_usbConnected_invokeCallback() {
        final Intent intent = new Intent();
        intent.putExtra(UsbManager.USB_CONNECTED, true);

        mReceiver.onReceive(mContext, intent);

        verify(mListener).onUsbConnectionChanged(true);
    }

    @Test
    public void testOnReceive_usbDisconnected_invokeCallback() {
        final Intent intent = new Intent();
        intent.putExtra(UsbManager.USB_CONNECTED, false);

        mReceiver.onReceive(mContext, intent);

        verify(mListener).onUsbConnectionChanged(false);
    }

    @Test
    public void testRegister_invokeMethodTwice_registerOnce() {
        mReceiver.register();
        mReceiver.register();

        final List<BroadcastReceiver> receivers = mShadowApplication.getReceiversForIntent(
                new Intent(UsbManager.ACTION_USB_STATE));
        assertHasOneUsbConnectionBroadcastReceiver(receivers);
    }

    @Test
    public void testUnregister_invokeMethodTwice_unregisterOnce() {
        mReceiver.register();
        mReceiver.unregister();
        mReceiver.unregister();

        final List<BroadcastReceiver> receivers = mShadowApplication.getReceiversForIntent(
                new Intent(UsbManager.ACTION_USB_STATE));
        assertHasNoUsbConnectionBroadcastReceiver(receivers);
    }

    private void assertHasOneUsbConnectionBroadcastReceiver(List<BroadcastReceiver> receivers) {
        boolean hasReceiver = false;
        for (final BroadcastReceiver receiver : receivers) {
            if (receiver instanceof UsbConnectionBroadcastReceiver) {
                // If hasReceiver is true, then we're at the second copy of it so fail.
                assertWithMessage(
                        "Only one instance of UsbConnectionBroadcastReceiver should be "
                                + "registered").that(
                        hasReceiver).isFalse();
                hasReceiver = true;
            }
        }
        assertThat(hasReceiver).isTrue();
    }

    private void assertHasNoUsbConnectionBroadcastReceiver(List<BroadcastReceiver> receivers) {
        for (final BroadcastReceiver receiver : receivers) {
            assertThat(receiver instanceof UsbConnectionBroadcastReceiver).isFalse();
        }
    }
}