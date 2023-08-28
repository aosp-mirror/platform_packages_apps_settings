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

import static org.mockito.Mockito.spy;

import android.content.Context;

import com.android.settings.testutils.BatteryTestUtils;
import com.android.settings.testutils.shadow.ShadowDashboardFragment;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowDashboardFragment.class)
public final class PowerUsageAdvancedTest {

    private Context mContext;

    @Before
    public void setUp() {
        mContext = spy(RuntimeEnvironment.application);
    }

    @Test
    public void getHighestScoreAnomalyEvent_withEmptyOrNullList_getNull() {
        assertThat(PowerUsageAdvanced.getHighestScoreAnomalyEvent(mContext, null)).isNull();
        assertThat(PowerUsageAdvanced.getHighestScoreAnomalyEvent(
                mContext, BatteryTestUtils.createEmptyPowerAnomalyEventList())).isNull();
    }

    @Test
    public void getHighestScoreAnomalyEvent_withoutDismissed_getHighestScoreEvent() {
        final PowerAnomalyEventList powerAnomalyEventList =
                BatteryTestUtils.createNonEmptyPowerAnomalyEventList();

        final PowerAnomalyEvent highestScoreEvent =
                PowerUsageAdvanced.getHighestScoreAnomalyEvent(mContext, powerAnomalyEventList);

        assertThat(highestScoreEvent)
                .isEqualTo(BatteryTestUtils.createAdaptiveBrightnessAnomalyEvent());
    }

    @Test
    public void getHighestScoreAnomalyEvent_withBrightnessDismissed_getScreenTimeout() {
        final PowerAnomalyEventList powerAnomalyEventList =
                BatteryTestUtils.createNonEmptyPowerAnomalyEventList();
        DatabaseUtils.removeDismissedPowerAnomalyKeys(mContext);
        DatabaseUtils.setDismissedPowerAnomalyKeys(mContext, PowerAnomalyKey.KEY_BRIGHTNESS.name());

        final PowerAnomalyEvent highestScoreEvent =
                PowerUsageAdvanced.getHighestScoreAnomalyEvent(mContext, powerAnomalyEventList);

        assertThat(highestScoreEvent)
                .isEqualTo(BatteryTestUtils.createScreenTimeoutAnomalyEvent());
    }

    @Test
    public void getHighestScoreAnomalyEvent_withAllDismissed_getNull() {
        final PowerAnomalyEventList powerAnomalyEventList =
                BatteryTestUtils.createNonEmptyPowerAnomalyEventList();
        DatabaseUtils.removeDismissedPowerAnomalyKeys(mContext);
        for (PowerAnomalyKey key : PowerAnomalyKey.values()) {
            DatabaseUtils.setDismissedPowerAnomalyKeys(mContext, key.name());
        }

        final PowerAnomalyEvent highestScoreEvent =
                PowerUsageAdvanced.getHighestScoreAnomalyEvent(mContext, powerAnomalyEventList);

        assertThat(highestScoreEvent).isEqualTo(null);
    }
}
