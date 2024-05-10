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

import static android.hardware.usb.UsbPortStatus.DATA_ROLE_DEVICE;
import static android.hardware.usb.UsbPortStatus.DATA_ROLE_NONE;
import static android.hardware.usb.UsbPortStatus.POWER_ROLE_NONE;
import static android.hardware.usb.UsbPortStatus.POWER_ROLE_SINK;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.hardware.usb.UsbManager;
import android.os.UserHandle;

import com.android.settings.R;
import com.android.settings.connecteddevice.DevicePreferenceCallback;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.testutils.DrawableTestHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class ConnectedUsbDeviceUpdaterTest {

    private Context mContext;
    private ConnectedUsbDeviceUpdater mDeviceUpdater;

    @Mock
    private DashboardFragment mFragment;
    @Mock
    private UsbConnectionBroadcastReceiver mUsbReceiver;
    @Mock
    private DevicePreferenceCallback mDevicePreferenceCallback;
    @Mock
    private UsbBackend mUsbBackend;
    @Mock
    private DevicePolicyManager mDevicePolicyManager;


    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        when(mFragment.getContext()).thenReturn(mContext);
        mDeviceUpdater =
                new ConnectedUsbDeviceUpdater(mContext, mFragment, mDevicePreferenceCallback,
                        mUsbBackend);
        mDeviceUpdater.mUsbReceiver = mUsbReceiver;
        when(mContext.getSystemService(DevicePolicyManager.class)).thenReturn(mDevicePolicyManager);
        doReturn(mContext).when(mContext).createPackageContextAsUser(
                any(String.class), anyInt(), any(UserHandle.class));
    }

    @Test
    public void initUsbPreference_preferenceInit() {
        when(mDevicePolicyManager.isUsbDataSignalingEnabled()).thenReturn(true);

        mDeviceUpdater.initUsbPreference(mContext);

        assertThat(mDeviceUpdater.mUsbPreference.getTitle()).isEqualTo("USB");
        DrawableTestHelper.assertDrawableResId(
                mDeviceUpdater.mUsbPreference.getIcon(), R.drawable.ic_usb);
        assertThat(mDeviceUpdater.mUsbPreference.isSelectable()).isTrue();
    }

    @Test
    public void initUsbPreference_usbConnected_preferenceAdded() {
        mDeviceUpdater.initUsbPreference(mContext);
        mDeviceUpdater.mUsbConnectionListener.onUsbConnectionChanged(true /* connected */,
                UsbManager.FUNCTION_NONE, POWER_ROLE_SINK, DATA_ROLE_DEVICE,
                /* isUsbConfigured= */ true);

        verify(mDevicePreferenceCallback).onDeviceAdded(mDeviceUpdater.mUsbPreference);
    }

    @Test
    public void initUsbPreference_usbDisconnected_preferenceRemoved() {
        mDeviceUpdater.initUsbPreference(mContext);
        mDeviceUpdater.mUsbConnectionListener.onUsbConnectionChanged(false /* connected */,
                UsbManager.FUNCTION_NONE, POWER_ROLE_NONE, DATA_ROLE_NONE,
                /* isUsbConfigured= */ true);

        verify(mDevicePreferenceCallback).onDeviceRemoved(mDeviceUpdater.mUsbPreference);
    }
}
