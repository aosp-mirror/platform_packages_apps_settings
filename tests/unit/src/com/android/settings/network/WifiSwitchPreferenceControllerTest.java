/*
 * Copyright (C) 2020 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Looper;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class WifiSwitchPreferenceControllerTest {

    private PreferenceScreen mScreen;
    private RestrictedSwitchPreference mPreference;
    private WifiSwitchPreferenceController mController;

    @Mock
    private WifiManager mWifiManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        final Context context = spy(ApplicationProvider.getApplicationContext());
        doReturn(mWifiManager).when(context).getSystemService(Context.WIFI_SERVICE);

        mController = new WifiSwitchPreferenceController(context, mock(Lifecycle.class));
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        final PreferenceManager preferenceManager = new PreferenceManager(context);
        mScreen = preferenceManager.createPreferenceScreen(context);
        mPreference = new RestrictedSwitchPreference(context);
        mPreference.setKey(WifiSwitchPreferenceController.KEY);
        mScreen.addPreference(mPreference);
    }

    @Test
    public void isAvailable_returnTrue() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isChecked_wifiStateDisabled_returnFalse() {
        doReturn(WifiManager.WIFI_STATE_DISABLED).when(mWifiManager).getWifiState();

        mController.displayPreference(mScreen);
        mController.onStart();

        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void isChecked_wifiStateEnabled_returnTrue() {
        doReturn(WifiManager.WIFI_STATE_ENABLED).when(mWifiManager).getWifiState();

        mController.displayPreference(mScreen);
        mController.onStart();

        assertThat(mPreference.isChecked()).isTrue();
    }
}
