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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.widget.MasterSwitchPreference;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowSettings;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION,
        shadows = {
                WifiTetherPreferenceControllerTest.ShadowWifiTetherSettings.class,
                WifiTetherPreferenceControllerTest.ShadowWifiTetherSwitchBarController.class,
        })
public class WifiTetherPreferenceControllerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mFeatureFactoryContext;
    @Mock
    private Context mContext;
    @Mock
    private ConnectivityManager mConnectivityManager;
    @Mock
    private WifiManager mWifiManager;
    @Mock
    private PreferenceScreen mScreen;

    private WifiTetherPreferenceController mController;
    private Lifecycle mLifecycle;
    private MasterSwitchPreference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mLifecycle = new Lifecycle();
        FakeFeatureFactory.setupForTest(mFeatureFactoryContext);
        mPreference = new MasterSwitchPreference(RuntimeEnvironment.application);
        when(mContext.getSystemService(Context.CONNECTIVITY_SERVICE))
                .thenReturn(mConnectivityManager);
        when(mContext.getSystemService(Context.WIFI_SERVICE)).thenReturn(mWifiManager);
        when(mScreen.findPreference(anyString())).thenReturn(mPreference);

        when(mConnectivityManager.getTetherableWifiRegexs()).thenReturn(new String[]{"1", "2"});
        mController = new WifiTetherPreferenceController(mContext, mLifecycle);
    }

    @After
    public void tearDown() {
        ShadowWifiTetherSwitchBarController.reset();
    }

    @Test
    public void isAvailable_noTetherRegex_shouldReturnFalse() {
        when(mConnectivityManager.getTetherableWifiRegexs()).thenReturn(new String[]{});
        mController = new WifiTetherPreferenceController(mContext, mLifecycle);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_hasTetherRegex_shouldReturnTrue() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void startAndStop_shouldRegisterUnregisterReceiver() {
        final BroadcastReceiver receiver = ReflectionHelpers.getField(mController, "mReceiver");

        mController.displayPreference(mScreen);
        mLifecycle.onStart();
        mLifecycle.onStop();

        assertThat(ShadowWifiTetherSwitchBarController.onStartCalled).isTrue();
        assertThat(ShadowWifiTetherSwitchBarController.onStopCalled).isTrue();
        verify(mContext).registerReceiver(eq(receiver), any(IntentFilter.class));
        verify(mContext).unregisterReceiver(receiver);
    }

    @Test
    public void start_wifiApOff_shouldSetInitialStateToOff() {
        when(mWifiManager.getWifiApState()).thenReturn(WifiManager.WIFI_AP_STATE_DISABLED);
        final BroadcastReceiver receiver = ReflectionHelpers.getField(mController, "mReceiver");
        final MasterSwitchPreference pref = mock(MasterSwitchPreference.class);
        when(mScreen.findPreference(anyString())).thenReturn(pref);

        mController.displayPreference(mScreen);
        mLifecycle.onStart();

        assertThat(ShadowWifiTetherSwitchBarController.onStartCalled).isTrue();
        verify(mContext).registerReceiver(eq(receiver), any(IntentFilter.class));
        verify(pref).setChecked(false);
    }

    @Test
    public void start_wifiApOn_shouldSetInitialStateToOn() {
        when(mWifiManager.getWifiApState()).thenReturn(WifiManager.WIFI_AP_STATE_ENABLED);
        final BroadcastReceiver receiver = ReflectionHelpers.getField(mController, "mReceiver");
        final MasterSwitchPreference pref = mock(MasterSwitchPreference.class);
        when(mScreen.findPreference(anyString())).thenReturn(pref);

        mController.displayPreference(mScreen);
        mLifecycle.onStart();

        assertThat(ShadowWifiTetherSwitchBarController.onStartCalled).isTrue();
        verify(mContext).registerReceiver(eq(receiver), any(IntentFilter.class));
        verify(pref).setChecked(true);
    }

    @Test
    public void testReceiver_apStateChangedToDisabled_shouldUpdatePreferenceSummary() {
        mController.displayPreference(mScreen);
        receiveApStateChangedBroadcast(WifiManager.WIFI_AP_STATE_DISABLED);
        assertThat(mPreference.getSummary().toString()).isEqualTo(
                RuntimeEnvironment.application.getString(R.string.wifi_hotspot_off_subtext));
    }

    @Test
    public void testReceiver_apStateChangedToDisabling_shouldUpdatePreferenceSummary() {
        mController.displayPreference(mScreen);
        receiveApStateChangedBroadcast(WifiManager.WIFI_AP_STATE_DISABLING);
        assertThat(mPreference.getSummary().toString()).isEqualTo(
                RuntimeEnvironment.application.getString(R.string.wifi_tether_stopping));
    }

    @Test
    public void testReceiver_apStateChangedToEnabling_shouldUpdatePreferenceSummary() {
        mController.displayPreference(mScreen);
        receiveApStateChangedBroadcast(WifiManager.WIFI_AP_STATE_ENABLING);
        assertThat(mPreference.getSummary().toString()).isEqualTo(
                RuntimeEnvironment.application.getString(R.string.wifi_tether_starting));
    }

    @Test
    public void testReceiver_apStateChangedToEnabled_shouldNotUpdatePreferenceSummary() {
        mController.displayPreference(mScreen);
        receiveApStateChangedBroadcast(WifiManager.WIFI_AP_STATE_DISABLED);
        assertThat(mPreference.getSummary().toString()).isEqualTo(
                RuntimeEnvironment.application.getString(R.string.wifi_hotspot_off_subtext));

        // When turning on the hotspot, we receive STATE_ENABLING followed by STATE_ENABLED. The
        // first should change the status to wifi_tether_starting, and the second should not change
        // this.
        receiveApStateChangedBroadcast(WifiManager.WIFI_AP_STATE_ENABLING);
        assertThat(mPreference.getSummary().toString()).isEqualTo(
                RuntimeEnvironment.application.getString(R.string.wifi_tether_starting));
        receiveApStateChangedBroadcast(WifiManager.WIFI_AP_STATE_ENABLED);
        assertThat(mPreference.getSummary().toString()).isEqualTo(
                RuntimeEnvironment.application.getString(R.string.wifi_tether_starting));
    }

    @Test
    public void testReceiver_goingToAirplaneMode_shouldClearPreferenceSummary() {
        final ContentResolver cr = mock(ContentResolver.class);
        when(mContext.getContentResolver()).thenReturn(cr);
        ShadowSettings.ShadowGlobal.putInt(cr, Settings.Global.AIRPLANE_MODE_ON, 1);
        mController.displayPreference(mScreen);
        final BroadcastReceiver receiver = ReflectionHelpers.getField(mController, "mReceiver");
        final Intent broadcast = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);

        receiver.onReceive(RuntimeEnvironment.application, broadcast);

        assertThat(mPreference.getSummary().toString()).isEqualTo(
                RuntimeEnvironment.application.getString(R.string.summary_placeholder));
    }

    @Test
    public void testReceiver_tetherEnabled_shouldUpdatePreferenceSummary() {
        mController.displayPreference(mScreen);
        final BroadcastReceiver receiver = ReflectionHelpers.getField(mController, "mReceiver");
        final Intent broadcast = new Intent(ConnectivityManager.ACTION_TETHER_STATE_CHANGED);
        final ArrayList<String> activeTethers = new ArrayList<>();
        activeTethers.add("1");
        broadcast.putStringArrayListExtra(ConnectivityManager.EXTRA_ACTIVE_TETHER, activeTethers);
        broadcast.putStringArrayListExtra(ConnectivityManager.EXTRA_ERRORED_TETHER,
                new ArrayList<>());
        final WifiConfiguration configuration = new WifiConfiguration();
        configuration.SSID = "test-ap";
        when(mWifiManager.getWifiApConfiguration()).thenReturn(configuration);

        receiver.onReceive(RuntimeEnvironment.application, broadcast);

        verify(mContext).getString(eq(R.string.wifi_tether_enabled_subtext), any());
    }

    @Implements(WifiTetherSettings.class)
    public static final class ShadowWifiTetherSettings {

        @Implementation
        public static boolean isTetherSettingPageEnabled() {
            return true;
        }
    }

    @Implements(WifiTetherSwitchBarController.class)
    public static final class ShadowWifiTetherSwitchBarController {

        public static boolean onStartCalled;
        public static boolean onStopCalled;

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

    /**
     * Helper to cause the controller to receive a WIFI_AP_STATE_CHANGED_ACTION with a specific
     * state.
     * @param state - the state, as specified by one of the WifiManager.WIFI_AP_STATE_* values
     */
    private void receiveApStateChangedBroadcast(int state) {
        final BroadcastReceiver receiver = ReflectionHelpers.getField(mController, "mReceiver");
        final Intent broadcast = new Intent(WifiManager.WIFI_AP_STATE_CHANGED_ACTION);
        broadcast.putExtra(WifiManager.EXTRA_WIFI_AP_STATE, state);
        receiver.onReceive(RuntimeEnvironment.application, broadcast);
    }
}
