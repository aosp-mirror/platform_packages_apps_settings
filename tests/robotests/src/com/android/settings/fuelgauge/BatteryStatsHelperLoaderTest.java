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
package com.android.settings.fuelgauge;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.os.BatteryStats;
import android.os.Bundle;
import android.os.UserManager;

import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class BatteryStatsHelperLoaderTest {
    @Mock
    private BatteryStatsHelper mBatteryStatsHelper;
    @Mock
    private Bundle mBundle;
    @Mock
    private Context mContext;
    @Mock
    private UserManager mUserManager;
    private BatteryStatsHelperLoader mLoader;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mLoader = new BatteryStatsHelperLoader(mContext, mBundle);
        mLoader.mUserManager = mUserManager;
    }

    @Test
    public void testInitBatteryStatsHelper_init() {
        mLoader.initBatteryStatsHelper(mBatteryStatsHelper);

        verify(mBatteryStatsHelper).create(mBundle);
        verify(mBatteryStatsHelper).refreshStats(BatteryStats.STATS_SINCE_CHARGED,
                mUserManager.getUserProfiles());
    }
}
