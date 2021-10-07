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

import static androidx.lifecycle.Lifecycle.Event.ON_PAUSE;
import static androidx.lifecycle.Lifecycle.Event.ON_RESUME;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkRequest;
import android.net.VpnManager;
import android.os.Looper;
import android.os.UserHandle;
import android.provider.Settings;
import android.provider.SettingsSlicesContract;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.internal.net.VpnConfig;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class VpnPreferenceControllerTest {
    private static final String VPN_PREFERENCE_KEY = "vpn_settings";

    private Context mContext;
    @Mock
    private ConnectivityManager mConnectivityManager;
    @Mock
    private VpnManager mVpnManager;
    private PreferenceScreen mScreen;
    @Mock
    private Preference mPreference;
    private VpnPreferenceController mController;
    private Lifecycle mLifecycle;
    private LifecycleOwner mLifecycleOwner;

    @Before
    @UiThreadTest
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getSystemService(Context.CONNECTIVITY_SERVICE))
                .thenReturn(mConnectivityManager);
        when(mContext.getSystemService(VpnManager.class)).thenReturn(mVpnManager);

        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        PreferenceManager preferenceManager = new PreferenceManager(mContext);
        mScreen = preferenceManager.createPreferenceScreen(mContext);
        when(mPreference.getKey()).thenReturn(VPN_PREFERENCE_KEY);
        mScreen.addPreference(mPreference);

        mController = spy(new VpnPreferenceController(mContext));
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        mLifecycle.addObserver(mController);
    }

    @Test
    public void displayPreference_available_shouldSetDependency() {
        Settings.Global.putString(mContext.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_TOGGLEABLE_RADIOS, "");
        VpnPreferenceController controller = spy(new VpnPreferenceController(mContext));
        doReturn(true).when(controller).isAvailable();

        controller.displayPreference(mScreen);

        verify(mPreference).setDependency(SettingsSlicesContract.KEY_AIRPLANE_MODE);
    }

    @Test
    // TODO(b/176821216) re-enable this test once VpnPreferenceController is edited to notify
    // the preference of legacy VPNs
    @Ignore
    public void goThroughLifecycle_shouldRegisterUnregisterListener() {
        doReturn(true).when(mController).isAvailable();

        mLifecycle.handleLifecycleEvent(ON_RESUME);
        verify(mConnectivityManager).registerNetworkCallback(
                any(NetworkRequest.class), any(ConnectivityManager.NetworkCallback.class));

        mLifecycle.handleLifecycleEvent(ON_PAUSE);
        verify(mConnectivityManager).unregisterNetworkCallback(
                any(ConnectivityManager.NetworkCallback.class));
    }

    @Test
    public void getNameForVpnConfig_legacyVPNConfig_shouldSetSummaryToConnected() {
        final VpnConfig config = new VpnConfig();
        config.legacy = true;
        final VpnPreferenceController controller =
                new VpnPreferenceController(ApplicationProvider.getApplicationContext());

        final String summary = controller.getNameForVpnConfig(config, UserHandle.CURRENT);

        assertThat(summary).isEqualTo("Connected");
    }
}
