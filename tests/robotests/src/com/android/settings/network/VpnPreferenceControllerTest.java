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

package com.android.settings.network;


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.IConnectivityManager;
import android.net.NetworkRequest;
import android.os.IBinder;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowServiceManager;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class VpnPreferenceControllerTest {

    @Mock
    private Context mContext;
    @Mock
    private ConnectivityManager mConnectivityManager;
    @Mock
    private IBinder mBinder;
    @Mock
    private IConnectivityManager mConnectivityManagerService;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private Preference mPreference;
    private VpnPreferenceController mController;
    private Lifecycle mLifecycle;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getSystemService(Context.CONNECTIVITY_SERVICE))
                .thenReturn(mConnectivityManager);
        when(mBinder.queryLocalInterface("android.net.IConnectivityManager"))
                .thenReturn(mConnectivityManagerService);
        ShadowServiceManager.addService(Context.CONNECTIVITY_SERVICE, mBinder);
        when(mScreen.findPreference(anyString())).thenReturn(mPreference);

        mController = spy(new VpnPreferenceController(mContext));
        mLifecycle = new Lifecycle();
        mLifecycle.addObserver(mController);
    }

    @Test
    public void displayPreference_available_shouldSetDependency() {

        doReturn(true).when(mController).isAvailable();
        mController.displayPreference(mScreen);

        verify(mPreference).setDependency(AirplaneModePreferenceController.KEY_TOGGLE_AIRPLANE);
    }

    @Test
    public void goThroughLifecycle_shouldRegisterUnregisterListener() {
        doReturn(true).when(mController).isAvailable();

        mLifecycle.onResume();
        verify(mConnectivityManager).registerNetworkCallback(
                any(NetworkRequest.class), any(ConnectivityManager.NetworkCallback.class));

        mLifecycle.onPause();
        verify(mConnectivityManager).unregisterNetworkCallback(
                any(ConnectivityManager.NetworkCallback.class));
    }

}
