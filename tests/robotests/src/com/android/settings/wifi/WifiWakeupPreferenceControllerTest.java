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

import static android.provider.Settings.Global.WIFI_SCAN_ALWAYS_AVAILABLE;
import static android.provider.Settings.Global.WIFI_WAKEUP_ENABLED;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;

import android.content.Context;
import android.location.LocationManager;
import android.provider.Settings;

import android.text.TextUtils;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;

import com.android.settingslib.core.lifecycle.Lifecycle;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class WifiWakeupPreferenceControllerTest {

    private Context mContext;
    private WifiWakeupPreferenceController mController;
    @Mock
    private DashboardFragment mFragment;
    @Mock
    private LocationManager mLocationManager;
    @Mock
    private SwitchPreference mPreference;
    @Mock
    private Lifecycle mLifecycle;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new WifiWakeupPreferenceController(mContext, mFragment, mLifecycle);
        mController.mLocationManager = mLocationManager;
        mController.mPreference = mPreference;

        Settings.Global.putInt(mContext.getContentResolver(), WIFI_SCAN_ALWAYS_AVAILABLE, 1);
        doReturn(true).when(mLocationManager).isLocationEnabled();
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
    public void handlePreferenceTreeClick_wifiWakeupEnableScanningDisable_wifiWakeupEnable() {
        Settings.Global.putInt(mContext.getContentResolver(), WIFI_WAKEUP_ENABLED, 1);
        Settings.Global.putInt(mContext.getContentResolver(), WIFI_SCAN_ALWAYS_AVAILABLE, 0);
        doReturn(true).when(mLocationManager).isLocationEnabled();

        mController.handlePreferenceTreeClick(mPreference);
        final boolean isWifiWakeupEnabled = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.WIFI_WAKEUP_ENABLED, 0) == 1;

        assertThat(isWifiWakeupEnabled).isTrue();
    }

    @Test
    public void updateState_preferenceSetCheckedWhenWakeupSettingEnabled() {
        final SwitchPreference preference = new SwitchPreference(mContext);
        Settings.Global.putInt(mContext.getContentResolver(), WIFI_WAKEUP_ENABLED, 1);
        Settings.Global.putInt(mContext.getContentResolver(), WIFI_SCAN_ALWAYS_AVAILABLE, 1);

        mController.updateState(preference);

        assertThat(preference.isChecked()).isTrue();
        assertThat(preference.getSummary())
            .isEqualTo(mContext.getString(R.string.wifi_wakeup_summary));
    }

    @Test
    public void updateState_preferenceSetUncheckedWhenWakeupSettingDisabled() {
        final SwitchPreference preference = new SwitchPreference(mContext);
        Settings.Global.putInt(mContext.getContentResolver(), WIFI_WAKEUP_ENABLED, 0);

        mController.updateState(preference);

        assertThat(preference.isChecked()).isFalse();
        assertThat(preference.getSummary())
            .isEqualTo(mContext.getString(R.string.wifi_wakeup_summary));
    }

    @Test
    public void updateState_preferenceSetUncheckedWhenWifiScanningDisabled() {
        final SwitchPreference preference = new SwitchPreference(mContext);
        Settings.Global.putInt(mContext.getContentResolver(), WIFI_WAKEUP_ENABLED, 1);
        Settings.Global.putInt(mContext.getContentResolver(), WIFI_SCAN_ALWAYS_AVAILABLE, 0);

        mController.updateState(preference);

        assertThat(preference.isChecked()).isFalse();
    }

    @Test
    public void updateState_preferenceSetUncheckedWhenWakeupSettingEnabledNoLocation() {
        final SwitchPreference preference = new SwitchPreference(mContext);
        Settings.Global.putInt(mContext.getContentResolver(), WIFI_WAKEUP_ENABLED, 1);
        doReturn(false).when(mLocationManager).isLocationEnabled();

        mController.updateState(preference);

        assertThat(preference.isChecked()).isFalse();
        assertThat(TextUtils.equals(preference.getSummary(), mController.getNoLocationSummary()))
            .isTrue();
    }

    @Test
    public void updateState_preferenceSetUncheckedWhenWakeupSettingDisabledLocationEnabled() {
        final SwitchPreference preference = new SwitchPreference(mContext);
        Settings.Global.putInt(mContext.getContentResolver(), WIFI_WAKEUP_ENABLED, 0);
        doReturn(false).when(mLocationManager).isLocationEnabled();

        mController.updateState(preference);

        assertThat(preference.isChecked()).isFalse();
        assertThat(TextUtils.equals(preference.getSummary(), mController.getNoLocationSummary()))
            .isTrue();
    }

    @Test
    public void updateState_preferenceSetUncheckedWhenWifiScanningDisabledLocationEnabled() {
        final SwitchPreference preference = new SwitchPreference(mContext);
        Settings.Global.putInt(mContext.getContentResolver(), WIFI_WAKEUP_ENABLED, 1);
        Settings.Global.putInt(mContext.getContentResolver(), WIFI_SCAN_ALWAYS_AVAILABLE, 0);
        doReturn(false).when(mLocationManager).isLocationEnabled();

        mController.updateState(preference);

        assertThat(preference.isChecked()).isFalse();
        assertThat(TextUtils.equals(preference.getSummary(), mController.getNoLocationSummary()))
            .isTrue();
    }
}
