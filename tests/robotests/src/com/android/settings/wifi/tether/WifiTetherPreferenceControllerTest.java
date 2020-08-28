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

package com.android.settings.wifi.tether;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.SoftApConfiguration;
import android.net.wifi.WifiManager;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceScreen;

import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.widget.MasterSwitchPreference;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
    WifiTetherPreferenceControllerTest.ShadowWifiTetherSettings.class,
    WifiTetherPreferenceControllerTest.ShadowWifiTetherSoftApManager.class
})
public class WifiTetherPreferenceControllerTest {

    private static final String SSID = "Pixel";

    private Context mContext;
    @Mock
    private ConnectivityManager mConnectivityManager;
    @Mock
    private WifiManager mWifiManager;
    @Mock
    private PreferenceScreen mScreen;
    private SoftApConfiguration mSoftApConfiguration;

    private WifiTetherPreferenceController mController;
    private Lifecycle mLifecycle;
    private LifecycleOwner mLifecycleOwner;
    private MasterSwitchPreference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        FakeFeatureFactory.setupForTest();
        mPreference = new MasterSwitchPreference(RuntimeEnvironment.application);
        when(mContext.getSystemService(Context.CONNECTIVITY_SERVICE))
                .thenReturn(mConnectivityManager);
        when(mContext.getSystemService(Context.WIFI_SERVICE)).thenReturn(mWifiManager);
        when(mScreen.findPreference(anyString())).thenReturn(mPreference);
        mSoftApConfiguration = new SoftApConfiguration.Builder().setSsid(SSID).build();
        when(mWifiManager.getSoftApConfiguration()).thenReturn(mSoftApConfiguration);

        when(mConnectivityManager.getTetherableWifiRegexs()).thenReturn(new String[]{"1", "2"});
        mController = new WifiTetherPreferenceController(mContext, mLifecycle,
                false /* initSoftApManager */);
        mController.displayPreference(mScreen);
    }

    @Test
    public void isAvailable_noTetherRegex_shouldReturnFalse() {
        when(mConnectivityManager.getTetherableWifiRegexs()).thenReturn(new String[]{});
        mController = new WifiTetherPreferenceController(mContext, mLifecycle,
                false /* initSoftApManager */);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_hasTetherRegex_shouldReturnTrue() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void testHandleWifiApStateChanged_stateEnabling_showEnablingSummary() {
        mController.handleWifiApStateChanged(WifiManager.WIFI_AP_STATE_ENABLING, 0 /* reason */);

        assertThat(mPreference.getSummary()).isEqualTo("Turning hotspot on\u2026");
    }

    @Test
    public void testHandleWifiApStateChanged_stateEnabled_showEnabledSummary() {
        mController.handleWifiApStateChanged(WifiManager.WIFI_AP_STATE_ENABLED, 0 /* reason */);

        assertThat(mPreference.getSummary()).isEqualTo("Pixel is active");
    }

    @Test
    public void testHandleWifiApStateChanged_stateDisabling_showDisablingSummary() {
        mController.handleWifiApStateChanged(WifiManager.WIFI_AP_STATE_DISABLING, 0 /* reason */);

        assertThat(mPreference.getSummary()).isEqualTo("Turning off hotspot\u2026");
    }

    @Test
    public void testHandleWifiApStateChanged_stateDisabled_showDisabledSummary() {
        mController.handleWifiApStateChanged(WifiManager.WIFI_AP_STATE_DISABLED, 0 /* reason */);

        assertThat(mPreference.getSummary()).isEqualTo(
                "Not sharing internet or content with other devices");
    }

    @Implements(WifiTetherSettings.class)
    public static final class ShadowWifiTetherSettings {

        @Implementation
        protected static boolean isTetherSettingPageEnabled() {
            return true;
        }
    }

    @Implements(WifiTetherSoftApManager.class)
    public static final class ShadowWifiTetherSoftApManager {
        @Implementation
        protected void registerSoftApCallback() {
            // do nothing
        }

        @Implementation
        protected void unRegisterSoftApCallback() {
            // do nothing
        }
    }
}
