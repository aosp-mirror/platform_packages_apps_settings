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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.SystemClock;
import android.text.format.DateUtils;

import com.android.settings.testutils.FakeFeatureFactory;

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
    private FakeFeatureFactory mFakeFeatureFactory;

    @Mock
    private PackageManager mPackageManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mFakeFeatureFactory = FakeFeatureFactory.setupForTest();
        mBatteryUsageBroadcastReceiver = new BatteryUsageBroadcastReceiver();
        doReturn(mPackageManager).when(mContext).getPackageManager();
    }

    @Test
    public void onReceive_invalidIntent_notStartService() {
        mBatteryUsageBroadcastReceiver.onReceive(mContext, new Intent("invalid intent"));

        assertThat(mBatteryUsageBroadcastReceiver.mFetchBatteryUsageData).isFalse();
    }

    @Test
    public void onReceive_actionBatteryLevelChanged_notFetchUsageData_notFullCharged() {
        when(mFakeFeatureFactory.powerUsageFeatureProvider.getFullChargeIntentAction())
                .thenReturn(Intent.ACTION_BATTERY_LEVEL_CHANGED);
        doReturn(getBatteryIntent(/*level=*/ 20, BatteryManager.BATTERY_STATUS_UNKNOWN))
                .when(mContext).registerReceiver(any(), any());

        mBatteryUsageBroadcastReceiver.onReceive(mContext,
                new Intent(Intent.ACTION_BATTERY_LEVEL_CHANGED));

        assertThat(mBatteryUsageBroadcastReceiver.mFetchBatteryUsageData).isFalse();
    }

    @Test
    public void onReceive_actionBatteryLevelChanged_notFetchUsageData_nearBooting() {
        when(mFakeFeatureFactory.powerUsageFeatureProvider.getFullChargeIntentAction())
                .thenReturn(Intent.ACTION_BATTERY_LEVEL_CHANGED);
        // Make sure isCharged returns true.
        doReturn(getBatteryIntent(/*level=*/ 100, BatteryManager.BATTERY_STATUS_FULL))
                .when(mContext).registerReceiver(any(), any());
        // Make sure broadcast will be sent with delay.
        BatteryUsageBroadcastReceiver.sBroadcastDelayFromBoot =
                SystemClock.elapsedRealtime() + 5 * DateUtils.MINUTE_IN_MILLIS;

        mBatteryUsageBroadcastReceiver.onReceive(mContext,
                new Intent(Intent.ACTION_BATTERY_LEVEL_CHANGED));

        assertThat(mBatteryUsageBroadcastReceiver.mFetchBatteryUsageData).isFalse();
    }

    @Test
    public void onReceive_actionBatteryLevelChanged_notFetchUsageData_wrongAction() {
        when(mFakeFeatureFactory.powerUsageFeatureProvider.getFullChargeIntentAction())
                .thenReturn(Intent.ACTION_POWER_DISCONNECTED);
        // Make sure isCharged returns true.
        doReturn(getBatteryIntent(/*level=*/ 100, BatteryManager.BATTERY_STATUS_UNKNOWN))
                .when(mContext).registerReceiver(any(), any());
        BatteryUsageBroadcastReceiver.sBroadcastDelayFromBoot =
                SystemClock.elapsedRealtime() - 5 * DateUtils.MINUTE_IN_MILLIS;

        mBatteryUsageBroadcastReceiver.onReceive(mContext,
                new Intent(Intent.ACTION_BATTERY_LEVEL_CHANGED));

        assertThat(mBatteryUsageBroadcastReceiver.mFetchBatteryUsageData).isFalse();
    }

    @Test
    public void onReceive_actionBatteryLevelChanged_fetchUsageData() {
        when(mFakeFeatureFactory.powerUsageFeatureProvider.getFullChargeIntentAction())
                .thenReturn(Intent.ACTION_BATTERY_LEVEL_CHANGED);
        // Make sure isCharged returns true.
        doReturn(getBatteryIntent(/*level=*/ 100, BatteryManager.BATTERY_STATUS_UNKNOWN))
                .when(mContext).registerReceiver(any(), any());
        BatteryUsageBroadcastReceiver.sBroadcastDelayFromBoot =
                SystemClock.elapsedRealtime() - 5 * DateUtils.MINUTE_IN_MILLIS;

        mBatteryUsageBroadcastReceiver.onReceive(mContext,
                new Intent(Intent.ACTION_BATTERY_LEVEL_CHANGED));

        assertThat(mBatteryUsageBroadcastReceiver.mFetchBatteryUsageData).isTrue();
    }

    @Test
    public void onReceive_actionBatteryUnplugging_notFetchUsageData_notFullCharged() {
        when(mFakeFeatureFactory.powerUsageFeatureProvider.getFullChargeIntentAction())
                .thenReturn(Intent.ACTION_POWER_DISCONNECTED);
        doReturn(getBatteryIntent(/*level=*/ 20, BatteryManager.BATTERY_STATUS_UNKNOWN))
                .when(mContext).registerReceiver(any(), any());

        mBatteryUsageBroadcastReceiver.onReceive(mContext,
                new Intent(BatteryUsageBroadcastReceiver.ACTION_BATTERY_UNPLUGGING));

        assertThat(mBatteryUsageBroadcastReceiver.mFetchBatteryUsageData).isFalse();
    }

    @Test
    public void onReceive_actionBatteryUnplugging_notFetchUsageData_nearBooting() {
        when(mFakeFeatureFactory.powerUsageFeatureProvider.getFullChargeIntentAction())
                .thenReturn(Intent.ACTION_POWER_DISCONNECTED);
        // Make sure isCharged returns true.
        doReturn(getBatteryIntent(/*level=*/ 100, BatteryManager.BATTERY_STATUS_FULL))
                .when(mContext).registerReceiver(any(), any());
        // Make sure broadcast will be sent with delay.
        BatteryUsageBroadcastReceiver.sBroadcastDelayFromBoot =
                SystemClock.elapsedRealtime() + 5 * DateUtils.MINUTE_IN_MILLIS;

        mBatteryUsageBroadcastReceiver.onReceive(mContext,
                new Intent(BatteryUsageBroadcastReceiver.ACTION_BATTERY_UNPLUGGING));

        assertThat(mBatteryUsageBroadcastReceiver.mFetchBatteryUsageData).isFalse();
    }

    @Test
    public void onReceive_actionBatteryUnplugging_notFetchUsageData_wrongAction() {
        when(mFakeFeatureFactory.powerUsageFeatureProvider.getFullChargeIntentAction())
                .thenReturn(Intent.ACTION_BATTERY_LEVEL_CHANGED);
        // Make sure isCharged returns true.
        doReturn(getBatteryIntent(/*level=*/ 100, BatteryManager.BATTERY_STATUS_UNKNOWN))
                .when(mContext).registerReceiver(any(), any());
        BatteryUsageBroadcastReceiver.sBroadcastDelayFromBoot =
                SystemClock.elapsedRealtime() - 5 * DateUtils.MINUTE_IN_MILLIS;

        mBatteryUsageBroadcastReceiver.onReceive(mContext,
                new Intent(BatteryUsageBroadcastReceiver.ACTION_BATTERY_UNPLUGGING));

        assertThat(mBatteryUsageBroadcastReceiver.mFetchBatteryUsageData).isFalse();
    }

    @Test
    public void onReceive_actionBatteryUnplugging_fetchUsageData() {
        when(mFakeFeatureFactory.powerUsageFeatureProvider.getFullChargeIntentAction())
                .thenReturn(Intent.ACTION_POWER_DISCONNECTED);
        // Make sure isCharged returns true.
        doReturn(getBatteryIntent(/*level=*/ 100, BatteryManager.BATTERY_STATUS_UNKNOWN))
                .when(mContext).registerReceiver(any(), any());
        BatteryUsageBroadcastReceiver.sBroadcastDelayFromBoot =
                SystemClock.elapsedRealtime() - 5 * DateUtils.MINUTE_IN_MILLIS;

        mBatteryUsageBroadcastReceiver.onReceive(mContext,
                new Intent(BatteryUsageBroadcastReceiver.ACTION_BATTERY_UNPLUGGING));

        assertThat(mBatteryUsageBroadcastReceiver.mFetchBatteryUsageData).isTrue();
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

    private static Intent getBatteryIntent(int level, int status) {
        final Intent intent = new Intent(Intent.ACTION_BATTERY_CHANGED);
        intent.putExtra(BatteryManager.EXTRA_LEVEL, level);
        intent.putExtra(BatteryManager.EXTRA_SCALE, 100);
        intent.putExtra(BatteryManager.EXTRA_STATUS, status);
        return intent;
    }
}
