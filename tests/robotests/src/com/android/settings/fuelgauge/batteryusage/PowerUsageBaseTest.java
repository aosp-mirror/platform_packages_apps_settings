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

import static com.android.settings.fuelgauge.batteryusage.PowerUsageBase.KEY_INCLUDE_HISTORY;
import static com.android.settings.fuelgauge.batteryusage.PowerUsageBase.KEY_REFRESH_TYPE;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.os.BatteryUsageStats;
import android.os.Bundle;

import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

import com.android.settings.fuelgauge.BatteryBroadcastReceiver;
import com.android.settings.testutils.shadow.ShadowDashboardFragment;
import com.android.settingslib.core.AbstractPreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowDashboardFragment.class)
public class PowerUsageBaseTest {

    @Mock
    private LoaderManager mLoaderManager;
    @Mock
    private Loader<BatteryUsageStats> mBatteryUsageStatsLoader;
    private TestFragment mFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mFragment = spy(new TestFragment(mLoaderManager));
    }

    @Test
    public void testOnCreate_batteryStatsLoaderNotInvoked() {
        mFragment.onCreate(null);

        verify(mLoaderManager, never()).initLoader(anyInt(), any(Bundle.class), any());
    }

    @Test
    public void restartBatteryInfoLoader() {
        final Bundle bundle = new Bundle();
        bundle.putInt(KEY_REFRESH_TYPE, BatteryBroadcastReceiver.BatteryUpdateType.BATTERY_STATUS);
        bundle.putBoolean(KEY_INCLUDE_HISTORY, false);
        doReturn(mBatteryUsageStatsLoader).when(mLoaderManager).getLoader(
                PowerUsageBase.LoaderIndex.BATTERY_USAGE_STATS_LOADER);
        doReturn(false).when(mBatteryUsageStatsLoader).isReset();

        mFragment.restartBatteryStatsLoader(
                BatteryBroadcastReceiver.BatteryUpdateType.BATTERY_STATUS);

        verify(mLoaderManager)
                .restartLoader(eq(PowerUsageBase.LoaderIndex.BATTERY_USAGE_STATS_LOADER),
                        refEq(bundle), any());
    }

    @Test
    public void restartBatteryInfoLoader_loaderReset_initLoader() {
        final Bundle bundle = new Bundle();
        bundle.putInt(KEY_REFRESH_TYPE, BatteryBroadcastReceiver.BatteryUpdateType.BATTERY_STATUS);
        bundle.putBoolean(KEY_INCLUDE_HISTORY, false);
        doReturn(mBatteryUsageStatsLoader).when(mLoaderManager).getLoader(
                PowerUsageBase.LoaderIndex.BATTERY_USAGE_STATS_LOADER);
        doReturn(true).when(mBatteryUsageStatsLoader).isReset();

        mFragment.restartBatteryStatsLoader(
                BatteryBroadcastReceiver.BatteryUpdateType.BATTERY_STATUS);

        verify(mLoaderManager)
                .initLoader(eq(PowerUsageBase.LoaderIndex.BATTERY_USAGE_STATS_LOADER),
                        refEq(bundle), any());
    }

    @Test
    public void restartBatteryInfoLoader_nullLoader_initLoader() {
        final Bundle bundle = new Bundle();
        bundle.putInt(KEY_REFRESH_TYPE, BatteryBroadcastReceiver.BatteryUpdateType.BATTERY_STATUS);
        bundle.putBoolean(KEY_INCLUDE_HISTORY, false);
        doReturn(null).when(mLoaderManager).getLoader(
                PowerUsageBase.LoaderIndex.BATTERY_USAGE_STATS_LOADER);

        mFragment.restartBatteryStatsLoader(
                BatteryBroadcastReceiver.BatteryUpdateType.BATTERY_STATUS);

        verify(mLoaderManager).initLoader(eq(PowerUsageBase.LoaderIndex.BATTERY_USAGE_STATS_LOADER),
                refEq(bundle), any());
    }

    private static class TestFragment extends PowerUsageBase {

        private LoaderManager mLoaderManager;

        TestFragment(LoaderManager loaderManager) {
            mLoaderManager = loaderManager;
        }

        @Override
        public int getMetricsCategory() {
            return 0;
        }

        @Override
        protected boolean isBatteryHistoryNeeded() {
            return false;
        }

        @Override
        protected void refreshUi(int refreshType) {
            // Do nothing
        }

        @Override
        protected String getLogTag() {
            return null;
        }

        @Override
        protected int getPreferenceScreenResId() {
            return 0;
        }

        @Override
        protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
            return null;
        }

        @Override
        protected LoaderManager getLoaderManagerForCurrentFragment() {
            return mLoaderManager;
        }
    }
}
