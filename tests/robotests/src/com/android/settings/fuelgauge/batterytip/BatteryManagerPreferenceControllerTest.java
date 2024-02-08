/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.android.settings.fuelgauge.batterytip;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.AppOpsManager;
import android.content.Context;
import android.provider.Settings;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.fuelgauge.PowerUsageFeatureProvider;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.SettingsShadowResources;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = SettingsShadowResources.class)
public class BatteryManagerPreferenceControllerTest {
    private static final int ON = 1;
    private static final int OFF = 0;

    @Mock
    private AppOpsManager mAppOpsManager;

    private Context mContext;
    private Preference mPreference;
    private FakeFeatureFactory mFeatureFactory;
    private PowerUsageFeatureProvider mPowerUsageFeatureProvider;
    private BatteryManagerPreferenceController mController;


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getSystemService(AppOpsManager.class)).thenReturn(mAppOpsManager);
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mPreference = new Preference(mContext);
        mController = new BatteryManagerPreferenceController(mContext);
        mPowerUsageFeatureProvider = mFeatureFactory.powerUsageFeatureProvider;
    }

    @Test
    public void updateState_smartBatteryWithRestrictApps_showSummary() {
        mController.updateSummary(mPreference, 2);

        assertThat(mPreference.getSummary()).isEqualTo("2 apps restricted");
    }

    @Test
    public void updateState_smartBatteryWithoutRestriction_showSummary() {
        when(mPowerUsageFeatureProvider.isSmartBatterySupported()).thenReturn(true);
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.ADAPTIVE_BATTERY_MANAGEMENT_ENABLED, ON);

        mController.updateState(mPreference);

        assertThat(mPreference.getSummary()).isEqualTo("Detecting when apps drain battery");
    }

    @Test
    public void getAvailabilityStatus_supportBatteryManager_showPrefPage() {
        SettingsShadowResources.overrideResource(
                R.bool.config_battery_manager_consider_ac, true);
        when(mPowerUsageFeatureProvider.isBatteryManagerSupported()).thenReturn(true);
        when(mPowerUsageFeatureProvider.isAdaptiveChargingSupported()).thenReturn(true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BatteryManagerPreferenceController.AVAILABLE_UNSEARCHABLE);
    }

    @Test
    public void getAvailabilityStatus_notSupportBatteryManager_notShowPrefPage() {
        when(mPowerUsageFeatureProvider.isBatteryManagerSupported()).thenReturn(false);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BatteryManagerPreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_supportBatteryManagerWithoutAC_notShowPrefPage() {
        SettingsShadowResources.overrideResource(
                R.bool.config_battery_manager_consider_ac, true);
        when(mPowerUsageFeatureProvider.isBatteryManagerSupported()).thenReturn(true);
        when(mPowerUsageFeatureProvider.isAdaptiveChargingSupported()).thenReturn(false);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BatteryManagerPreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_ignoreBatteryManagerWithoutAC_showPrefPage() {
        SettingsShadowResources.overrideResource(
                R.bool.config_battery_manager_consider_ac, false);
        when(mPowerUsageFeatureProvider.isBatteryManagerSupported()).thenReturn(true);
        when(mPowerUsageFeatureProvider.isAdaptiveChargingSupported()).thenReturn(false);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BatteryManagerPreferenceController.AVAILABLE_UNSEARCHABLE);
    }
}
