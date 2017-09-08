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

import static android.provider.Settings.Global.NETWORK_RECOMMENDATIONS_ENABLED;
import static android.provider.Settings.Global.WIFI_SCAN_ALWAYS_AVAILABLE;
import static android.provider.Settings.Global.WIFI_WAKEUP_AVAILABLE;
import static android.provider.Settings.Global.WIFI_WAKEUP_ENABLED;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;

import com.android.settings.R;
import com.android.settings.network.NetworkScoreManagerWrapper;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(
        manifest = TestConfig.MANIFEST_PATH,
        sdk = TestConfig.SDK_VERSION,
        shadows = { SettingsShadowResources.class })
public class WifiWakeupPreferenceControllerTest {

    private static final String TEST_SCORER_PACKAGE_NAME = "Test Scorer";

    private Context mContext;
    private WifiWakeupPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new WifiWakeupPreferenceController(
                mContext, mock(Lifecycle.class));
        Settings.System.putInt(mContext.getContentResolver(), WIFI_SCAN_ALWAYS_AVAILABLE, 1);
        Settings.System.putInt(mContext.getContentResolver(), NETWORK_RECOMMENDATIONS_ENABLED, 1);
        SettingsShadowResources.overrideResource(
                com.android.internal.R.integer.config_wifi_wakeup_available, 0);
    }

    @After
    public void tearDown() {
        SettingsShadowResources.reset();
    }

    @Test
    public void testIsAvailable_returnsFalseWhenSettingIsNotAvailable() {
        Settings.System.putInt(mContext.getContentResolver(), WIFI_WAKEUP_AVAILABLE, 0);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void testIsAvailable_returnsTrueWhenSettingIsAvailable() {
        Settings.System.putInt(mContext.getContentResolver(), WIFI_WAKEUP_AVAILABLE, 1);
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void handlePreferenceTreeClick_nonMatchingKey_shouldDoNothing() {
        final SwitchPreference pref = new SwitchPreference(mContext);

        assertThat(mController.handlePreferenceTreeClick(pref)).isFalse();
    }

    @Test
    public void handlePreferenceTreeClick_nonMatchingType_shouldDoNothing() {
        final Preference pref = new Preference(mContext);
        pref.setKey(mController.getPreferenceKey());

        assertThat(mController.handlePreferenceTreeClick(pref)).isFalse();
    }

    @Test
    public void handlePreferenceTreeClick_matchingKeyAndType_shouldUpdateSetting() {
        final SwitchPreference pref = new SwitchPreference(mContext);
        pref.setChecked(true);
        pref.setKey(mController.getPreferenceKey());

        assertThat(mController.handlePreferenceTreeClick(pref)).isTrue();
        assertThat(Settings.Global.getInt(mContext.getContentResolver(), WIFI_WAKEUP_ENABLED, 0))
                .isEqualTo(1);
    }

    @Test
    public void updateState_preferenceSetCheckedAndSetEnabledWhenWakeupSettingEnabled() {
        final SwitchPreference preference = mock(SwitchPreference.class);
        Settings.System.putInt(mContext.getContentResolver(), WIFI_WAKEUP_ENABLED, 1);

        mController.updateState(preference);

        verify(preference).setChecked(true);
        verify(preference).setEnabled(true);
        verify(preference).setSummary(R.string.wifi_wakeup_summary);
    }

    @Test
    public void updateState_preferenceSetUncheckedAndSetEnabledWhenWakeupSettingDisabled() {
        final SwitchPreference preference = mock(SwitchPreference.class);
        Settings.System.putInt(mContext.getContentResolver(), WIFI_WAKEUP_ENABLED, 0);

        mController.updateState(preference);

        verify(preference).setChecked(false);
        verify(preference).setEnabled(true);
        verify(preference).setSummary(R.string.wifi_wakeup_summary);
    }

    @Test
    public void updateState_preferenceSetUncheckedAndSetDisabledWhenWifiScanningDisabled() {
        final SwitchPreference preference = mock(SwitchPreference.class);
        Settings.System.putInt(mContext.getContentResolver(), WIFI_WAKEUP_ENABLED, 1);
        Settings.System.putInt(mContext.getContentResolver(), WIFI_SCAN_ALWAYS_AVAILABLE, 0);

        mController.updateState(preference);

        verify(preference).setChecked(true);
        verify(preference).setEnabled(false);
        verify(preference).setSummary(R.string.wifi_wakeup_summary_scanning_disabled);
    }

    @Test
    public void updateState_preferenceSetUncheckedAndSetDisabledWhenScoringDisabled() {
        final SwitchPreference preference = mock(SwitchPreference.class);
        Settings.System.putInt(mContext.getContentResolver(), WIFI_WAKEUP_ENABLED, 1);
        Settings.System.putInt(mContext.getContentResolver(), NETWORK_RECOMMENDATIONS_ENABLED, 0);

        mController.updateState(preference);

        verify(preference).setChecked(true);
        verify(preference).setEnabled(false);
        verify(preference).setSummary(R.string.wifi_wakeup_summary_scoring_disabled);
    }
}
