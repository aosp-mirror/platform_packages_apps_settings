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

package com.android.settings.connecteddevice.usb;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbPort;
import android.hardware.usb.UsbPortStatus;
import android.net.ConnectivityManager;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.wrapper.UserManagerWrapper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(SettingsRobolectricTestRunner.class)
public class UsbBackendTest {

    @Mock(answer = RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private UsbManager mUsbManager;
    @Mock
    private UserManagerWrapper mUserManagerWrapper;
    @Mock
    private ConnectivityManager mConnectivityManager;
    @Mock
    private UsbPort mUsbPort;
    @Mock
    private UsbPortStatus mUsbPortStatus;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_MIDI))
                .thenReturn(true);
        when((Object) mContext.getSystemService(UsbManager.class)).thenReturn(mUsbManager);
        when(mContext.getSystemService(Context.CONNECTIVITY_SERVICE))
                .thenReturn(mConnectivityManager);
        when(mUsbManager.getPorts()).thenReturn(new UsbPort[] {mUsbPort});
        when(mUsbPortStatus.isConnected()).thenReturn(true);
        when(mUsbManager.getPortStatus(mUsbPort)).thenReturn(mUsbPortStatus);
    }

    @Test
    public void setDataRole_allRolesSupported_shouldSetDataRole() {
        final UsbBackend usbBackend = new UsbBackend(mContext, mUserManagerWrapper);

        when(mUsbPortStatus
                .isRoleCombinationSupported(UsbPort.POWER_ROLE_SINK, UsbPort.DATA_ROLE_DEVICE))
                .thenReturn(true);
        when(mUsbPortStatus
                .isRoleCombinationSupported(UsbPort.POWER_ROLE_SINK, UsbPort.DATA_ROLE_HOST))
                .thenReturn(true);
        when(mUsbPortStatus
                .isRoleCombinationSupported(UsbPort.POWER_ROLE_SOURCE, UsbPort.DATA_ROLE_DEVICE))
                .thenReturn(true);
        when(mUsbPortStatus
                .isRoleCombinationSupported(UsbPort.POWER_ROLE_SOURCE, UsbPort.DATA_ROLE_HOST))
                .thenReturn(true);
        when(mUsbPortStatus.getCurrentPowerRole()).thenReturn(UsbPort.POWER_ROLE_SINK);

        usbBackend.setDataRole(UsbPort.DATA_ROLE_HOST);

        verify(mUsbManager).setPortRoles(mUsbPort, UsbPort.POWER_ROLE_SINK, UsbPort.DATA_ROLE_HOST);
    }

    @Test
    public void setDataRole_notAllRolesSupported_shouldSetDataAndPowerRole() {
        final UsbBackend usbBackend = new UsbBackend(mContext, mUserManagerWrapper);

        when(mUsbPortStatus
                .isRoleCombinationSupported(UsbPort.POWER_ROLE_SINK, UsbPort.DATA_ROLE_DEVICE))
                .thenReturn(true);
        when(mUsbPortStatus
                .isRoleCombinationSupported(UsbPort.POWER_ROLE_SOURCE, UsbPort.DATA_ROLE_HOST))
                .thenReturn(true);
        when(mUsbPortStatus.getCurrentPowerRole()).thenReturn(UsbPort.POWER_ROLE_SINK);

        usbBackend.setDataRole(UsbPort.DATA_ROLE_HOST);

        verify(mUsbManager)
                .setPortRoles(mUsbPort, UsbPort.POWER_ROLE_SOURCE, UsbPort.DATA_ROLE_HOST);
    }

    @Test
    public void setPowerRole_allRolesSupported_shouldSetPowerRole() {
        final UsbBackend usbBackend = new UsbBackend(mContext, mUserManagerWrapper);

        when(mUsbPortStatus
                .isRoleCombinationSupported(UsbPort.POWER_ROLE_SINK, UsbPort.DATA_ROLE_DEVICE))
                .thenReturn(true);
        when(mUsbPortStatus
                .isRoleCombinationSupported(UsbPort.POWER_ROLE_SINK, UsbPort.DATA_ROLE_HOST))
                .thenReturn(true);
        when(mUsbPortStatus
                .isRoleCombinationSupported(UsbPort.POWER_ROLE_SOURCE, UsbPort.DATA_ROLE_DEVICE))
                .thenReturn(true);
        when(mUsbPortStatus
                .isRoleCombinationSupported(UsbPort.POWER_ROLE_SOURCE, UsbPort.DATA_ROLE_HOST))
                .thenReturn(true);
        when(mUsbPortStatus.getCurrentDataRole()).thenReturn(UsbPort.DATA_ROLE_DEVICE);

        usbBackend.setPowerRole(UsbPort.POWER_ROLE_SOURCE);

        verify(mUsbManager)
                .setPortRoles(mUsbPort, UsbPort.POWER_ROLE_SOURCE, UsbPort.DATA_ROLE_DEVICE);
    }

    @Test
    public void setPowerRole_notAllRolesSupported_shouldSetDataAndPowerRole() {
        final UsbBackend usbBackend = new UsbBackend(mContext, mUserManagerWrapper);

        when(mUsbPortStatus
                .isRoleCombinationSupported(UsbPort.POWER_ROLE_SINK, UsbPort.DATA_ROLE_DEVICE))
                .thenReturn(true);
        when(mUsbPortStatus
                .isRoleCombinationSupported(UsbPort.POWER_ROLE_SOURCE, UsbPort.DATA_ROLE_HOST))
                .thenReturn(true);
        when(mUsbPortStatus.getCurrentDataRole()).thenReturn(UsbPort.DATA_ROLE_DEVICE);

        usbBackend.setPowerRole(UsbPort.POWER_ROLE_SOURCE);

        verify(mUsbManager)
                .setPortRoles(mUsbPort, UsbPort.POWER_ROLE_SOURCE, UsbPort.DATA_ROLE_HOST);
    }

    @Test
    public void areFunctionsSupported_fileTransferDisallowed_shouldReturnFalse() {
        when(mUserManagerWrapper.isUsbFileTransferRestricted()).thenReturn(true);
        when(mUserManagerWrapper.isUsbFileTransferRestrictedBySystem()).thenReturn(true);

        final UsbBackend usbBackend = new UsbBackend(mContext, mUserManagerWrapper);

        assertThat(usbBackend.areFunctionsSupported(UsbManager.FUNCTION_MTP)).isFalse();
    }

    @Test
    public void areFunctionsSupported_fileTransferAllowed_shouldReturnTrue() {
        when(mUserManagerWrapper.isUsbFileTransferRestricted()).thenReturn(false);
        when(mUserManagerWrapper.isUsbFileTransferRestrictedBySystem()).thenReturn(false);

        final UsbBackend usbBackend = new UsbBackend(mContext, mUserManagerWrapper);

        assertThat(usbBackend.areFunctionsSupported(UsbManager.FUNCTION_MTP)).isTrue();
    }
}
