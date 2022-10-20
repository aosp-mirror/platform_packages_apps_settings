/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.fuelgauge.batteryusage;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class BatteryUsageBroadcastReceiverTest {

    private Context mContext;
    private BatteryUsageBroadcastReceiver mBatteryUsageBroadcastReceiver;
    @Mock
    private PackageManager mPackageManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mBatteryUsageBroadcastReceiver = new BatteryUsageBroadcastReceiver();
        doReturn(mPackageManager).when(mContext).getPackageManager();
    }

    @Test
    public void onReceive_fetchUsageDataIntent_startService() {
        setProviderSetting(PackageManager.COMPONENT_ENABLED_STATE_ENABLED);
        mBatteryUsageBroadcastReceiver.onReceive(mContext,
                new Intent(BatteryUsageBroadcastReceiver.ACTION_FETCH_BATTERY_USAGE_DATA));

        assertThat(mBatteryUsageBroadcastReceiver.mFetchBatteryUsageData).isTrue();
    }

    @Test
    public void onReceive_invalidIntent_notStartService() {
        setProviderSetting(PackageManager.COMPONENT_ENABLED_STATE_ENABLED);
        mBatteryUsageBroadcastReceiver.onReceive(mContext, new Intent("invalid intent"));

        assertThat(mBatteryUsageBroadcastReceiver.mFetchBatteryUsageData).isFalse();
    }

    @Test
    public void onReceive_clearCacheIntentInDebugMode_clearBatteryCacheData() {
        BatteryUsageBroadcastReceiver.sIsDebugMode = true;
        // Insert testing data first.
        BatteryDiffEntry.sValidForRestriction.put(
                /*packageName*/ "com.android.testing_package", Boolean.valueOf(true));
        assertThat(BatteryDiffEntry.sValidForRestriction).isNotEmpty();

        mBatteryUsageBroadcastReceiver.onReceive(mContext,
                new Intent(BatteryUsageBroadcastReceiver.ACTION_CLEAR_BATTERY_CACHE_DATA));

        assertThat(BatteryDiffEntry.sValidForRestriction).isEmpty();
    }

    @Test
    public void onReceive_clearCacheIntentInNotDebugMode_notClearBatteryCacheData() {
        BatteryUsageBroadcastReceiver.sIsDebugMode = false;
        // Insert testing data first.
        BatteryDiffEntry.sValidForRestriction.put(
                /*packageName*/ "com.android.testing_package", Boolean.valueOf(true));
        assertThat(BatteryDiffEntry.sValidForRestriction).isNotEmpty();

        mBatteryUsageBroadcastReceiver.onReceive(mContext,
                new Intent(BatteryUsageBroadcastReceiver.ACTION_CLEAR_BATTERY_CACHE_DATA));

        assertThat(BatteryDiffEntry.sValidForRestriction).isNotEmpty();
    }

    private void setProviderSetting(int value) {
        when(mPackageManager.getComponentEnabledSetting(
                new ComponentName(
                        DatabaseUtils.SETTINGS_PACKAGE_PATH,
                        DatabaseUtils.BATTERY_PROVIDER_CLASS_PATH)))
                .thenReturn(value);
    }
}
