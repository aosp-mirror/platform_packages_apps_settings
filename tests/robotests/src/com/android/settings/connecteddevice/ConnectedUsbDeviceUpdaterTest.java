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

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import android.content.Context;

import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.deviceinfo.UsbBackend;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class ConnectedUsbDeviceUpdaterTest {
    private Context mContext;
    private ConnectedUsbDeviceUpdater mDeviceUpdater;

    @Mock
    private UsbConnectionBroadcastReceiver mUsbReceiver;
    @Mock
    private DevicePreferenceCallback mDevicePreferenceCallback;
    @Mock
    private UsbBackend mUsbBackend;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mDeviceUpdater = new ConnectedUsbDeviceUpdater(mContext, mDevicePreferenceCallback,
                mUsbBackend);
        mDeviceUpdater.mUsbReceiver = mUsbReceiver;
    }

    @Test
    public void testInitUsbPreference_preferenceInit() {
        mDeviceUpdater.initUsbPreference(mContext);

        assertThat(mDeviceUpdater.mUsbPreference.getTitle()).isEqualTo("USB");
        assertThat(mDeviceUpdater.mUsbPreference.getIcon()).isEqualTo(mContext.getDrawable(
                R.drawable.ic_usb));
        assertThat(mDeviceUpdater.mUsbPreference.isSelectable()).isFalse();
    }

    @Test
    public void testInitUsbPreference_usbConnected_preferenceAdded() {
        doReturn(true).when(mUsbReceiver).isConnected();

        mDeviceUpdater.initUsbPreference(mContext);

        verify(mDevicePreferenceCallback).onDeviceAdded(mDeviceUpdater.mUsbPreference);
    }

    @Test
    public void testInitUsbPreference_usbDisconnected_preferenceRemoved() {
        doReturn(false).when(mUsbReceiver).isConnected();

        mDeviceUpdater.initUsbPreference(mContext);

        verify(mDevicePreferenceCallback).onDeviceRemoved(mDeviceUpdater.mUsbPreference);
    }

}