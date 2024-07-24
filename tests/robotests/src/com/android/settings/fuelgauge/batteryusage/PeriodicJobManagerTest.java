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

import static org.robolectric.Shadows.shadowOf;

import android.app.AlarmManager;
import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.testutils.FakeClock;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowAlarmManager;

import java.time.Duration;

/** Tests of {@link PeriodicJobManager}. */
@RunWith(RobolectricTestRunner.class)
public final class PeriodicJobManagerTest {
    private Context mContext;
    private ShadowAlarmManager mShadowAlarmManager;
    private PeriodicJobManager mPeriodicJobManager;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mPeriodicJobManager = PeriodicJobManager.getInstance(mContext);
        mShadowAlarmManager = shadowOf(mContext.getSystemService(AlarmManager.class));
    }

    @After
    public void tearDown() {
        mPeriodicJobManager.reset();
    }

    @Test
    public void refreshJob_refreshesAlarmJob() {
        mPeriodicJobManager.refreshJob(/* fromBoot= */ false);

        final ShadowAlarmManager.ScheduledAlarm alarm =
                mShadowAlarmManager.peekNextScheduledAlarm();
        // Verifies the alarm manager type.
        assertThat(alarm.type).isEqualTo(AlarmManager.RTC_WAKEUP);
        // Verifies there is pending intent in the alarm.
        assertThat(alarm.operation).isNotNull();
    }

    @Test
    public void getTriggerAtMillis_withoutOffset_returnsExpectedResult() {
        long timeSlotUnit = PeriodicJobManager.DATA_FETCH_INTERVAL_MINUTE;
        // Sets the current time.
        Duration currentTimeDuration = Duration.ofMinutes(timeSlotUnit * 2);
        FakeClock fakeClock = new FakeClock();
        fakeClock.setCurrentTime(currentTimeDuration);

        assertThat(
                        PeriodicJobManager.getTriggerAtMillis(
                                mContext, fakeClock, /* fromBoot= */ false))
                .isEqualTo(currentTimeDuration.plusMinutes(timeSlotUnit).toMillis());
    }

    @Test
    public void getTriggerAtMillis_withOffset_returnsExpectedResult() {
        long timeSlotUnit = PeriodicJobManager.DATA_FETCH_INTERVAL_MINUTE;
        // Sets the current time.
        Duration currentTimeDuration = Duration.ofMinutes(timeSlotUnit * 2);
        FakeClock fakeClock = new FakeClock();
        fakeClock.setCurrentTime(currentTimeDuration.plusMinutes(1L).plusMillis(51L));

        assertThat(PeriodicJobManager.getTriggerAtMillis(mContext, fakeClock, /* fromBoot= */ true))
                .isEqualTo(currentTimeDuration.plusMinutes(timeSlotUnit).toMillis());
    }
}
