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
import static org.robolectric.Shadows.shadowOf;

import android.app.AlarmManager;
import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowAlarmManager;

import java.time.Duration;
import java.util.Calendar;
import java.util.TimeZone;

/** Tests of {@link PeriodicJobManager}. */
@RunWith(RobolectricTestRunner.class)
public final class PeriodicJobManagerTest {
    private Context mContext;
    private ShadowAlarmManager mShadowAlarmManager;
    private PeriodicJobManager mPeriodicJobManager;
    private FakeFeatureFactory mFeatureFactory;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mPeriodicJobManager = PeriodicJobManager.getInstance(mContext);
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        doReturn(false).when(mFeatureFactory.powerUsageFeatureProvider).delayHourlyJobWhenBooting();
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
    public void getTriggerAtMillis_halfFullHourTimeZoneWithoutOffset_returnsExpectedResult() {
        final int minutesOffset = 0;
        final long currentTimestamp =
                setTimeZoneAndGenerateTestTimestamp(/* isFullHourTimeZone= */ false, minutesOffset);
        final long expectedTimestamp =
                currentTimestamp + Duration.ofMinutes(60 - minutesOffset).toMillis();

        assertThat(
                        PeriodicJobManager.getTriggerAtMillis(
                                /* currentTimeMillis= */ currentTimestamp, /* fromBoot= */ false))
                .isEqualTo(expectedTimestamp);
    }

    @Test
    public void getTriggerAtMillis_halfFullHourTimeZoneWithOffset_returnsExpectedResult() {
        final int minutesOffset = 21;
        final long currentTimestamp =
                setTimeZoneAndGenerateTestTimestamp(/* isFullHourTimeZone= */ false, minutesOffset);
        final long expectedTimestamp =
                currentTimestamp + Duration.ofMinutes(60 - minutesOffset).toMillis();

        assertThat(
                        PeriodicJobManager.getTriggerAtMillis(
                                /* currentTimeMillis= */ currentTimestamp, /* fromBoot= */ false))
                .isEqualTo(expectedTimestamp);
    }

    @Test
    public void getTriggerAtMillis_halfFullHourTimeZoneWithBroadcastDelay_returnsExpectedResult() {
        doReturn(true).when(mFeatureFactory.powerUsageFeatureProvider).delayHourlyJobWhenBooting();

        final int minutesOffset = 21;
        final long currentTimestamp =
                setTimeZoneAndGenerateTestTimestamp(/* isFullHourTimeZone= */ false, minutesOffset);
        final long expectedTimestamp =
                currentTimestamp + Duration.ofMinutes(60 * 2 - minutesOffset).toMillis();

        assertThat(
                        PeriodicJobManager.getTriggerAtMillis(
                                /* currentTimeMillis= */ currentTimestamp, /* fromBoot= */ true))
                .isEqualTo(expectedTimestamp);
    }

    @Test
    public void getTriggerAtMillis_fullHourTimeZoneWithoutOffset_returnsExpectedResult() {
        final int minutesOffset = 0;
        final long currentTimestamp =
                setTimeZoneAndGenerateTestTimestamp(/* isFullHourTimeZone= */ true, minutesOffset);
        final long expectedTimestamp =
                currentTimestamp + Duration.ofMinutes(60 - minutesOffset).toMillis();

        assertThat(
                        PeriodicJobManager.getTriggerAtMillis(
                                /* currentTimeMillis= */ currentTimestamp, /* fromBoot= */ false))
                .isEqualTo(expectedTimestamp);
    }

    @Test
    public void getTriggerAtMillis_fullHourTimeZoneWithOffset_returnsExpectedResult() {
        final int minutesOffset = 21;
        final long currentTimestamp =
                setTimeZoneAndGenerateTestTimestamp(/* isFullHourTimeZone= */ true, minutesOffset);
        final long expectedTimestamp =
                currentTimestamp + Duration.ofMinutes(60 - minutesOffset).toMillis();

        assertThat(
                        PeriodicJobManager.getTriggerAtMillis(
                                /* currentTimeMillis= */ currentTimestamp, /* fromBoot= */ false))
                .isEqualTo(expectedTimestamp);
    }

    @Test
    public void getTriggerAtMillis_fullHourTimeZoneWithBroadcastDelay_returnsExpectedResult() {
        doReturn(true).when(mFeatureFactory.powerUsageFeatureProvider).delayHourlyJobWhenBooting();

        final int minutesOffset = 21;
        final long currentTimestamp =
                setTimeZoneAndGenerateTestTimestamp(/* isFullHourTimeZone= */ true, minutesOffset);
        final long expectedTimestamp =
                currentTimestamp + Duration.ofMinutes(60 * 2 - minutesOffset).toMillis();

        assertThat(
                        PeriodicJobManager.getTriggerAtMillis(
                                /* currentTimeMillis= */ currentTimestamp, /* fromBoot= */ true))
                .isEqualTo(expectedTimestamp);
    }

    private static long setTimeZoneAndGenerateTestTimestamp(
            final boolean isFullHourTimeZone, final int minutesOffset) {
        final TimeZone timeZone =
                TimeZone.getTimeZone(isFullHourTimeZone ? "UTC" : /* GMT+05:30 */ "Asia/Kalkata");
        TimeZone.setDefault(timeZone);
        Calendar calendar = (Calendar) Calendar.getInstance().clone();
        calendar.set(Calendar.MINUTE, minutesOffset);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }
}
