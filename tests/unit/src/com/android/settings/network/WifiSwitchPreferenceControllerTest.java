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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Looper;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.wifi.WifiEnabler;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(AndroidJUnit4.class)
public class WifiSwitchPreferenceControllerTest {

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Spy
    Context mContext = ApplicationProvider.getApplicationContext();
    @Mock
    WifiManager mWifiManager;
    @Mock
    WifiEnabler mWifiEnabler;

    private PreferenceScreen mScreen;
    private RestrictedSwitchPreference mPreference;
    private WifiSwitchPreferenceController mController;

    @Before
    public void setUp() {
        when(mContext.getSystemService(WifiManager.class)).thenReturn(mWifiManager);
        when(mWifiManager.isWifiEnabled()).thenReturn(true);

        mController = new WifiSwitchPreferenceController(mContext, mock(Lifecycle.class));
        mController.mIsChangeWifiStateAllowed = true;
        mController.mWifiEnabler = mWifiEnabler;
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        final PreferenceManager preferenceManager = new PreferenceManager(mContext);
        mScreen = preferenceManager.createPreferenceScreen(mContext);
        mPreference = new RestrictedSwitchPreference(mContext);
        mPreference.setKey(WifiSwitchPreferenceController.KEY);
        mScreen.addPreference(mPreference);
    }

    @Test
    public void isAvailable_returnTrue() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void displayPreference_wifiNotEnabled_preferenceNotChecked() {
        when(mWifiManager.isWifiEnabled()).thenReturn(false);

        mController.displayPreference(mScreen);

        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void displayPreference_wifiIsEnabled_preferenceIsChecked() {
        when(mWifiManager.isWifiEnabled()).thenReturn(true);

        mController.displayPreference(mScreen);

        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void displayPreference_disallowChangeWifiState_preferenceNotEnabled() {
        mController.mIsChangeWifiStateAllowed = false;

        mController.displayPreference(mScreen);

        assertThat(mPreference.isEnabled()).isFalse();
    }

    @Test
    public void displayPreference_allowChangeWifiState_preferenceIsEnabled() {
        mController.mIsChangeWifiStateAllowed = true;

        mController.displayPreference(mScreen);

        assertThat(mPreference.isEnabled()).isTrue();
    }

    @Test
    public void onStart_disallowChangeWifiState_wifiEnablerNotCreated() {
        mController.mIsChangeWifiStateAllowed = false;
        mController.displayPreference(mScreen);
        mController.mWifiEnabler = null;

        mController.onStart();

        assertThat(mController.mWifiEnabler).isNull();
    }

    @Test
    public void onStart_allowChangeWifiState_createWifiEnabler() {
        mController.mIsChangeWifiStateAllowed = true;
        mController.displayPreference(mScreen);
        mController.mWifiEnabler = null;

        mController.onStart();

        assertThat(mController.mWifiEnabler).isNotNull();
    }

    @Test
    public void onStop_wifiEnablerIsCreated_teardownWifiEnabler() {
        mController.mWifiEnabler = mWifiEnabler;

        mController.onStop();

        verify(mWifiEnabler).teardownSwitchController();
    }

    @Test
    public void onResume_wifiEnablerIsCreated_wifiEnablerResume() {
        mController.mWifiEnabler = mWifiEnabler;

        mController.onResume();

        verify(mWifiEnabler).resume(mContext);
    }

    @Test
    public void onPause_wifiEnablerIsCreated_wifiEnablerPause() {
        mController.mWifiEnabler = mWifiEnabler;

        mController.onPause();

        verify(mWifiEnabler).pause();
    }
}
