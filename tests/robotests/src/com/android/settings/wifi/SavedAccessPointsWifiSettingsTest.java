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

package com.android.settings.wifi;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.ActionListener;
import android.os.Handler;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settingslib.wifi.AccessPoint;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.util.ReflectionHelpers;

@RunWith(SettingsRobolectricTestRunner.class)
public class SavedAccessPointsWifiSettingsTest {

    @Mock
    private WifiManager mockWifiManager;
    @Mock
    private WifiDialog mockWifiDialog;
    @Mock
    private WifiConfigController mockConfigController;
    @Mock
    private WifiConfiguration mockWifiConfiguration;
    @Mock
    private AccessPoint mockAccessPoint;
    @Mock
    private Handler mHandler;

    private SavedAccessPointsWifiSettings mSettings;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mSettings = new SavedAccessPointsWifiSettings();
        ReflectionHelpers.setField(mSettings, "mHandler", mHandler);
        ReflectionHelpers.setField(mSettings, "mWifiManager", mockWifiManager);

        when(mockWifiDialog.getController()).thenReturn(mockConfigController);
        when(mockConfigController.getConfig()).thenReturn(mockWifiConfiguration);
    }

    @Test
    public void onForget_isPasspointConfig_shouldSendMessageToHandler() {
        final AccessPoint accessPoint = mock(AccessPoint.class);
        when(accessPoint.isPasspointConfig()).thenReturn(true);
        ReflectionHelpers.setField(mSettings, "mSelectedAccessPoint", accessPoint);

        mSettings.onForget(null);

        verify(mHandler).sendEmptyMessage(SavedAccessPointsWifiSettings.MSG_UPDATE_PREFERENCES);
    }

    @Test
    public void onForget_onSuccess_shouldSendMessageToHandler() {
        mSettings.mForgetListener.onSuccess();

        verify(mHandler).sendEmptyMessage(SavedAccessPointsWifiSettings.MSG_UPDATE_PREFERENCES);
    }

    @Test
    public void onForget_onFailure_shouldSendMessageToHandler() {
        mSettings.mForgetListener.onFailure(0);

        verify(mHandler).sendEmptyMessage(SavedAccessPointsWifiSettings.MSG_UPDATE_PREFERENCES);
    }

    @Test
    public void onSubmit_shouldInvokeSaveApi() {
        mSettings.onSubmit(mockWifiDialog);
        verify(mockWifiManager).save(eq(mockWifiConfiguration), any(ActionListener.class));
    }

    @Test
    public void onForget_shouldInvokeForgetApi() {
        ReflectionHelpers.setField(mSettings, "mSelectedAccessPoint", mockAccessPoint);
        when(mockAccessPoint.getConfig()).thenReturn(mockWifiConfiguration);
        mSettings.onForget(mockWifiDialog);
        verify(mockWifiManager)
                .forget(eq(mockWifiConfiguration.networkId), any(ActionListener.class));
    }
}
