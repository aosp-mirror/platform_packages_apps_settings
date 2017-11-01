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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class WifiInfoPreferenceControllerTest {

    @Mock
    private Context mContext;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private WifiManager mWifiManager;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private Preference mIpPreference;
    @Mock
    private Preference mMacPreference;

    private Lifecycle mLifecycle;
    private WifiInfoPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mLifecycle = new Lifecycle();
        when(mContext.getSystemService(WifiManager.class))
                .thenReturn(mWifiManager);
        when(mScreen.findPreference(anyString()))
                .thenReturn(mMacPreference)
                .thenReturn(mIpPreference);
        mController = new WifiInfoPreferenceController(mContext, mLifecycle, mWifiManager);
    }

    @Test
    public void testIsAvailable_shouldAlwaysReturnTrue() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void getPreferenceKey_shouldReturnNull() {
        assertThat(mController.getPreferenceKey()).isNull();
    }

    @Test
    public void runThroughLifecycle_shouldInstallListenerOnResume() {
        mLifecycle.onResume();
        verify(mContext).registerReceiver(any(BroadcastReceiver.class), any(IntentFilter.class));

        mLifecycle.onPause();
        verify(mContext).unregisterReceiver(any(BroadcastReceiver.class));
    }

    @Test
    public void onResume_shouldUpdateWifiInfo() {
        when(mWifiManager.getCurrentNetwork()).thenReturn(null);

        mController.displayPreference(mScreen);
        mController.onResume();

        verify(mMacPreference).setSummary(any());
        verify(mIpPreference).setSummary(any());
    }
}
