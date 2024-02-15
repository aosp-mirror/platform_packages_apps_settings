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

package com.android.settings.fuelgauge.batterytip;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.content.Intent;
import android.os.BatteryUsageStats;
import android.os.PowerManager;

import com.android.settings.fuelgauge.BatteryInfo;
import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.fuelgauge.batterytip.tips.AppLabelPredicate;
import com.android.settings.fuelgauge.batterytip.tips.AppRestrictionPredicate;
import com.android.settings.fuelgauge.batterytip.tips.BatteryTip;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class BatteryTipLoaderTest {

    private static final int[] TIP_ORDER = {
        BatteryTip.TipType.LOW_BATTERY,
        BatteryTip.TipType.BATTERY_DEFENDER,
        BatteryTip.TipType.INCOMPATIBLE_CHARGER,
        BatteryTip.TipType.HIGH_DEVICE_USAGE
    };

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private BatteryUsageStats mBatteryUsageStats;

    @Mock private PowerManager mPowerManager;
    @Mock private Intent mIntent;
    @Mock private BatteryUtils mBatteryUtils;
    @Mock private BatteryInfo mBatteryInfo;
    private Context mContext;
    private BatteryTipLoader mBatteryTipLoader;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        doReturn(mContext).when(mContext).getApplicationContext();
        doReturn(mPowerManager).when(mContext).getSystemService(Context.POWER_SERVICE);
        doReturn(mIntent).when(mContext).registerReceiver(any(), any());
        doReturn(mBatteryInfo).when(mBatteryUtils).getBatteryInfo(any());
        mBatteryTipLoader = new BatteryTipLoader(mContext, mBatteryUsageStats);
        mBatteryTipLoader.mBatteryUtils = mBatteryUtils;
    }

    @After
    public void tearDown() {
        ReflectionHelpers.setStaticField(AppLabelPredicate.class, "sInstance", null);
        ReflectionHelpers.setStaticField(AppRestrictionPredicate.class, "sInstance", null);
    }

    @Test
    public void testLoadBackground_containsAllTipsWithOrder() {
        final List<BatteryTip> batteryTips = mBatteryTipLoader.loadInBackground();

        assertThat(batteryTips.size()).isEqualTo(TIP_ORDER.length);
        for (int i = 0, size = batteryTips.size(); i < size; i++) {
            assertThat(batteryTips.get(i).getType()).isEqualTo(TIP_ORDER[i]);
        }
    }
}
