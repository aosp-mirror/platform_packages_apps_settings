/*
 * Copyright (C) 2018 The Android Open Source Project
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

import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class BatteryManagerPreferenceControllerTest {
    private static final int ON = 1;
    private static final int OFF = 0;

    @Mock
    private AppOpsManager mAppOpsManager;


    private Context mContext;
    private Preference mPreference;
    private FakeFeatureFactory mFeatureFactory;
    private BatteryManagerPreferenceController mController;


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getSystemService(AppOpsManager.class)).thenReturn(mAppOpsManager);
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mPreference = new Preference(mContext);
        mController = new BatteryManagerPreferenceController(mContext);
    }

    @Test
    public void updateState_smartBatteryOnWithRestrictApps_showSummary() {
        mController.updateSummary(mPreference, true /* smartBatteryOn */, 2);

        assertThat(mPreference.getSummary()).isEqualTo("2 apps restricted");
    }

    @Test
    public void updateState_smartBatteryOnWithoutRestriction_showSummary() {
        when(mFeatureFactory.powerUsageFeatureProvider.isSmartBatterySupported()).thenReturn(true);
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.ADAPTIVE_BATTERY_MANAGEMENT_ENABLED, ON);

        mController.updateState(mPreference);

        assertThat(mPreference.getSummary()).isEqualTo("On / Detecting when apps drain battery");
    }

    @Test
    public void updateState_smartBatteryOff_showSummary() {
        when(mFeatureFactory.powerUsageFeatureProvider.isSmartBatterySupported()).thenReturn(true);
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.ADAPTIVE_BATTERY_MANAGEMENT_ENABLED, OFF);

        mController.updateState(mPreference);

        assertThat(mPreference.getSummary()).isEqualTo("Off");
    }
}
