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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.text.TextUtils;

import androidx.preference.SwitchPreference;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;

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
    private WifiManager mWifiManager;
    @Mock
    private SwitchPreference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new WifiWakeupPreferenceController(mContext);
        mController.setFragment(mFragment);
        mController.mLocationManager = mLocationManager;
        mController.mPreference = mPreference;
        mController.mWifiManager = mWifiManager;

        when(mWifiManager.isScanAlwaysAvailable()).thenReturn(true);
        doReturn(true).when(mLocationManager).isLocationEnabled();
    }

    @Test
    public void setChecked_scanEnableLocationEnable_wifiWakeupEnable() {
        when(mWifiManager.isAutoWakeupEnabled()).thenReturn(false);
        when(mWifiManager.isScanAlwaysAvailable()).thenReturn(true);
        doReturn(true).when(mLocationManager).isLocationEnabled();

        mController.setChecked(true);

        verify(mWifiManager).setAutoWakeupEnabled(true);
    }

    @Test
    public void updateState_wifiWakeupEnableScanningDisable_wifiWakeupDisabled() {
        final SwitchPreference preference = new SwitchPreference(mContext);
        when(mWifiManager.isAutoWakeupEnabled()).thenReturn(true);
        when(mWifiManager.isScanAlwaysAvailable()).thenReturn(false);
        doReturn(true).when(mLocationManager).isLocationEnabled();

        mController.updateState(preference);

        assertThat(preference.isChecked()).isFalse();
        assertThat(preference.getSummary())
            .isEqualTo(mContext.getString(R.string.wifi_wakeup_summary));
    }

    @Test
    public void updateState_preferenceSetCheckedWhenWakeupSettingEnabled() {
        final SwitchPreference preference = new SwitchPreference(mContext);
        when(mWifiManager.isAutoWakeupEnabled()).thenReturn(true);
        when(mWifiManager.isScanAlwaysAvailable()).thenReturn(true);
        doReturn(true).when(mLocationManager).isLocationEnabled();

        mController.updateState(preference);

        assertThat(preference.isChecked()).isTrue();
        assertThat(preference.getSummary())
            .isEqualTo(mContext.getString(R.string.wifi_wakeup_summary));
    }

    @Test
    public void updateState_preferenceSetUncheckedWhenWakeupSettingDisabled() {
        final SwitchPreference preference = new SwitchPreference(mContext);
        when(mWifiManager.isAutoWakeupEnabled()).thenReturn(false);

        mController.updateState(preference);

        assertThat(preference.isChecked()).isFalse();
        assertThat(preference.getSummary())
            .isEqualTo(mContext.getString(R.string.wifi_wakeup_summary));
    }

    @Test
    public void updateState_preferenceSetUncheckedWhenWifiScanningDisabled() {
        final SwitchPreference preference = new SwitchPreference(mContext);
        when(mWifiManager.isAutoWakeupEnabled()).thenReturn(true);
        when(mWifiManager.isScanAlwaysAvailable()).thenReturn(false);

        mController.updateState(preference);

        assertThat(preference.isChecked()).isFalse();
    }

    @Test
    public void updateState_preferenceSetUncheckedWhenWakeupSettingEnabledNoLocation() {
        final SwitchPreference preference = new SwitchPreference(mContext);
        when(mWifiManager.isAutoWakeupEnabled()).thenReturn(true);
        doReturn(false).when(mLocationManager).isLocationEnabled();

        mController.updateState(preference);

        assertThat(preference.isChecked()).isFalse();
        assertThat(TextUtils.equals(preference.getSummary(), mController.getNoLocationSummary()))
            .isTrue();
    }

    @Test
    public void updateState_preferenceSetUncheckedWhenWakeupSettingDisabledLocationEnabled() {
        final SwitchPreference preference = new SwitchPreference(mContext);
        when(mWifiManager.isAutoWakeupEnabled()).thenReturn(false);
        doReturn(false).when(mLocationManager).isLocationEnabled();

        mController.updateState(preference);

        assertThat(preference.isChecked()).isFalse();
        assertThat(TextUtils.equals(preference.getSummary(), mController.getNoLocationSummary()))
            .isTrue();
    }

    @Test
    public void updateState_preferenceSetUncheckedWhenWifiScanningDisabledLocationEnabled() {
        final SwitchPreference preference = new SwitchPreference(mContext);
        when(mWifiManager.isAutoWakeupEnabled()).thenReturn(true);
        when(mWifiManager.isScanAlwaysAvailable()).thenReturn(false);
        doReturn(false).when(mLocationManager).isLocationEnabled();

        mController.updateState(preference);

        assertThat(preference.isChecked()).isFalse();
        assertThat(TextUtils.equals(preference.getSummary(), mController.getNoLocationSummary()))
            .isTrue();
    }
}
