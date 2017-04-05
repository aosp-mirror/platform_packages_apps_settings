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

import android.os.BatteryStats;

import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static android.os.BatteryStats.Uid.PROCESS_STATE_BACKGROUND;
import static android.os.BatteryStats.Uid.PROCESS_STATE_FOREGROUND;
import static android.os.BatteryStats.Uid.PROCESS_STATE_FOREGROUND_SERVICE;
import static android.os.BatteryStats.Uid.PROCESS_STATE_TOP;
import static android.os.BatteryStats.Uid.PROCESS_STATE_TOP_SLEEPING;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Matchers.eq;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class BatteryUtilsTest {
    // unit that used to converted ms to us
    private static final long UNIT = 1000;
    private static final long TIME_STATE_TOP = 1500 * UNIT;
    private static final long TIME_STATE_FOREGROUND_SERVICE = 2000 * UNIT;
    private static final long TIME_STATE_TOP_SLEEPING = 2500 * UNIT;
    private static final long TIME_STATE_FOREGROUND = 3000 * UNIT;
    private static final long TIME_STATE_BACKGROUND = 6000 * UNIT;

    private static final long TIME_EXPECTED_FOREGROUND = 9000;
    private static final long TIME_EXPECTED_BACKGROUND = 6000;
    private static final long TIME_EXPECTED_ALL = 15000;

    @Mock
    private BatteryStats.Uid mUid;
    private BatteryUtils mBatteryUtils;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        doReturn(TIME_STATE_TOP).when(mUid).getProcessStateTime(eq(PROCESS_STATE_TOP), anyLong(),
                anyInt());
        doReturn(TIME_STATE_FOREGROUND_SERVICE).when(mUid).getProcessStateTime(
                eq(PROCESS_STATE_FOREGROUND_SERVICE), anyLong(), anyInt());
        doReturn(TIME_STATE_TOP_SLEEPING).when(mUid).getProcessStateTime(
                eq(PROCESS_STATE_TOP_SLEEPING), anyLong(), anyInt());
        doReturn(TIME_STATE_FOREGROUND).when(mUid).getProcessStateTime(eq(PROCESS_STATE_FOREGROUND),
                anyLong(), anyInt());
        doReturn(TIME_STATE_BACKGROUND).when(mUid).getProcessStateTime(eq(PROCESS_STATE_BACKGROUND),
                anyLong(), anyInt());

        mBatteryUtils = BatteryUtils.getInstance(RuntimeEnvironment.application);
    }

    @Test
    public void testGetProcessTimeMs_typeForeground_timeCorrect() {
        final long time = mBatteryUtils.getProcessTimeMs(BatteryUtils.StatusType.FOREGROUND, mUid,
                BatteryStats.STATS_SINCE_CHARGED);

        assertThat(time).isEqualTo(TIME_EXPECTED_FOREGROUND);
    }

    @Test
    public void testGetProcessTimeMs_typeBackground_timeCorrect() {
        final long time = mBatteryUtils.getProcessTimeMs(BatteryUtils.StatusType.BACKGROUND, mUid,
                BatteryStats.STATS_SINCE_CHARGED);

        assertThat(time).isEqualTo(TIME_EXPECTED_BACKGROUND);
    }

    @Test
    public void testGetProcessTimeMs_typeAll_timeCorrect() {
        final long time = mBatteryUtils.getProcessTimeMs(BatteryUtils.StatusType.ALL, mUid,
                BatteryStats.STATS_SINCE_CHARGED);

        assertThat(time).isEqualTo(TIME_EXPECTED_ALL);
    }

    @Test
    public void testGetProcessTimeMs_uidNull_returnZero() {
        final long time = mBatteryUtils.getProcessTimeMs(BatteryUtils.StatusType.ALL, null,
                BatteryStats.STATS_SINCE_CHARGED);

        assertThat(time).isEqualTo(0);
    }
}
