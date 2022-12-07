/*
 * Copyright (C) 2022 The Android Open Source Project
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
 * See the License for the specific language governing `permissions and
 * limitations under the License.
 */

package com.android.settings.network;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.UserManager;
import android.telephony.TelephonyManager;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowToast;

@RunWith(RobolectricTestRunner.class)
public class BluetoothWiFiResetPreferenceControllerTest {

    private static final String PREFERENCE_KEY = "network_reset_bluetooth_wifi_pref";

    @Mock
    private UserManager mUserManager;
    @Mock
    private Resources mResources;
    @Mock
    private ConnectivityManager mConnectivityManager;
    @Mock
    private TelephonyManager mTelephonyManager;

    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getResources()).thenReturn(mResources);

        mockService(Context.CONNECTIVITY_SERVICE, ConnectivityManager.class,
                mConnectivityManager);
        mockService(Context.TELEPHONY_SERVICE, TelephonyManager.class, mTelephonyManager);
    }

    @Test
    public void getAvailabilityStatus_returnAvailable_asOwnerUser() {
        mockService(Context.USER_SERVICE, UserManager.class, mUserManager);
        doReturn(true).when(mUserManager).isAdminUser();

        BluetoothWiFiResetPreferenceController target =
                new BluetoothWiFiResetPreferenceController(mContext, PREFERENCE_KEY);

        assertThat(target.getAvailabilityStatus()).isEqualTo(
                BluetoothWiFiResetPreferenceController.AVAILABLE);
    }

    @Test
    public void resetOperation_notResetConnectivity_onDeviceWithSimVisible() {
        mockService(Context.CONNECTIVITY_SERVICE, ConnectivityManager.class,
                mConnectivityManager);
        when(mResources.getBoolean(R.bool.config_show_sim_info)).thenReturn(true);

        BluetoothWiFiResetPreferenceController target =
                new BluetoothWiFiResetPreferenceController(mContext, PREFERENCE_KEY);

        try {
            target.resetOperation().run();
        } catch (Exception exception) {}
        verify(mConnectivityManager, never()).factoryReset();
    }

    @Test
    public void endOfReset_toastMessage_whenSuccess() {
        String testText = "reset_bluetooth_wifi_complete_toast";
        when(mResources.getString(R.string.reset_bluetooth_wifi_complete_toast)).thenReturn(testText);
        BluetoothWiFiResetPreferenceController target =
                new BluetoothWiFiResetPreferenceController(mContext, PREFERENCE_KEY);

        target.endOfReset(null);

        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(testText);
    }

    private <T> void mockService(String serviceName, Class<T> serviceClass, T service) {
        when(mContext.getSystemServiceName(serviceClass)).thenReturn(serviceName);
        when(mContext.getSystemService(serviceName)).thenReturn(service);
    }
}
