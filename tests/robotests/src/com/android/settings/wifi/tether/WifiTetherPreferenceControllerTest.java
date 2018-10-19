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

import static androidx.lifecycle.Lifecycle.Event.ON_START;
import static androidx.lifecycle.Lifecycle.Event.ON_STOP;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.lifecycle.LifecycleOwner;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import androidx.preference.PreferenceScreen;

import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.widget.MasterSwitchPreference;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.util.ReflectionHelpers;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(shadows = {
    WifiTetherPreferenceControllerTest.ShadowWifiTetherSettings.class,
    WifiTetherPreferenceControllerTest.ShadowWifiTetherSwitchBarController.class,
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
    @Mock
    private WifiConfiguration mWifiConfiguration;

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
        when(mWifiManager.getWifiApConfiguration()).thenReturn(mWifiConfiguration);
        mWifiConfiguration.SSID = SSID;

        when(mConnectivityManager.getTetherableWifiRegexs()).thenReturn(new String[]{"1", "2"});
        mController = new WifiTetherPreferenceController(mContext, mLifecycle,
                false /* initSoftApManager */);
        mController.displayPreference(mScreen);
    }

    @After
    public void tearDown() {
        ShadowWifiTetherSwitchBarController.reset();
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
    public void testReceiver_turnOnAirplaneMode_clearPreferenceSummary() {
        final ContentResolver cr = mock(ContentResolver.class);
        when(mContext.getContentResolver()).thenReturn(cr);
        Settings.Global.putInt(cr, Settings.Global.AIRPLANE_MODE_ON, 1);
        mController.displayPreference(mScreen);
        final BroadcastReceiver receiver = ReflectionHelpers.getField(mController, "mReceiver");
        final Intent broadcast = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);

        receiver.onReceive(RuntimeEnvironment.application, broadcast);

        assertThat(mPreference.getSummary().toString()).isEqualTo(
                "Unavailable because airplane mode is turned on");
    }

    @Test
    public void testReceiver_turnOffAirplaneMode_displayOffSummary() {
        final ContentResolver cr = mock(ContentResolver.class);
        when(mContext.getContentResolver()).thenReturn(cr);
        Settings.Global.putInt(cr, Settings.Global.AIRPLANE_MODE_ON, 0);
        mController.displayPreference(mScreen);
        final BroadcastReceiver receiver = ReflectionHelpers.getField(mController, "mReceiver");
        final Intent broadcast = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);

        receiver.onReceive(RuntimeEnvironment.application, broadcast);

        assertThat(mPreference.getSummary().toString()).isEqualTo(
                "Not sharing internet or content with other devices");
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
        public static boolean isTetherSettingPageEnabled() {
            return true;
        }
    }

    @Implements(WifiTetherSoftApManager.class)
    public static final class ShadowWifiTetherSoftApManager {
        @Implementation
        public void registerSoftApCallback() {
            // do nothing
        }

        @Implementation
        public void unRegisterSoftApCallback() {
            // do nothing
        }
    }

    @Implements(WifiTetherSwitchBarController.class)
    public static final class ShadowWifiTetherSwitchBarController {

        private static boolean onStartCalled;
        private static boolean onStopCalled;

        public static void reset() {
            onStartCalled = false;
            onStopCalled = false;
        }

        @Implementation
        public void onStart() {
            onStartCalled = true;
        }

        @Implementation
        public void onStop() {
            onStopCalled = true;
        }
    }
}
