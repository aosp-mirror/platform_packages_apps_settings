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
import android.app.Application;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.fuelgauge.batteryusage.db.BatteryStateDao;
import com.android.settings.fuelgauge.batteryusage.db.BatteryStateDatabase;
import com.android.settings.testutils.BatteryTestUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowAlarmManager;

import java.time.Clock;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Tests of {@link BootBroadcastReceiver}. */
@RunWith(RobolectricTestRunner.class)
public final class BootBroadcastReceiverTest {
    private Context mContext;
    private BatteryStateDao mDao;
    private BootBroadcastReceiver mReceiver;
    private ShadowAlarmManager mShadowAlarmManager;
    private PeriodicJobManager mPeriodicJobManager;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mPeriodicJobManager = PeriodicJobManager.getInstance(mContext);
        mShadowAlarmManager = shadowOf(mContext.getSystemService(AlarmManager.class));
        mReceiver = new BootBroadcastReceiver();

        // Inserts fake data into database for testing.
        final BatteryStateDatabase database = BatteryTestUtils.setUpBatteryStateDatabase(mContext);
        BatteryTestUtils.insertDataToBatteryStateTable(
                mContext, Clock.systemUTC().millis(), "com.android.systemui");
        mDao = database.batteryStateDao();
        clearSharedPreferences();
    }

    @After
    public void tearDown() {
        clearSharedPreferences();
        mPeriodicJobManager.reset();
    }

    @Test
    public void onReceive_withWorkProfile_notRefreshesJob() {
        BatteryTestUtils.setWorkProfile(mContext);
        mReceiver.onReceive(mContext, new Intent(Intent.ACTION_BOOT_COMPLETED));

        assertThat(mShadowAlarmManager.peekNextScheduledAlarm()).isNull();
    }

    @Test
    public void onReceive_withBootCompletedIntent_refreshesJob() {
        final SharedPreferences sharedPreferences = DatabaseUtils.getSharedPreferences(mContext);
        sharedPreferences
                .edit()
                .putInt(
                        DatabaseUtils.KEY_LAST_USAGE_SOURCE,
                        UsageStatsManager.USAGE_SOURCE_CURRENT_ACTIVITY)
                .apply();

        mReceiver.onReceive(mContext, new Intent(Intent.ACTION_BOOT_COMPLETED));

        assertThat(mShadowAlarmManager.peekNextScheduledAlarm()).isNotNull();
        assertThat(
                        DatabaseUtils.getSharedPreferences(mContext)
                                .contains(DatabaseUtils.KEY_LAST_USAGE_SOURCE))
                .isFalse();
    }

    @Test
    public void onReceive_withSetupWizardIntent_refreshesJob() {
        mReceiver.onReceive(
                mContext, new Intent(BootBroadcastReceiver.ACTION_SETUP_WIZARD_FINISHED));
        assertThat(mShadowAlarmManager.peekNextScheduledAlarm()).isNotNull();
    }

    @Test
    public void onReceive_withRecheckIntent_refreshesJob() {
        mReceiver.onReceive(
                mContext, new Intent(BootBroadcastReceiver.ACTION_PERIODIC_JOB_RECHECK));
        assertThat(mShadowAlarmManager.peekNextScheduledAlarm()).isNotNull();
    }

    @Test
    public void onReceive_unexpectedIntent_notRefreshesJob() {
        mReceiver.onReceive(mContext, new Intent("invalid intent action"));
        assertThat(mShadowAlarmManager.peekNextScheduledAlarm()).isNull();
    }

    @Test
    public void onReceive_nullIntent_notRefreshesJob() {
        mReceiver.onReceive(mContext, /* intent= */ null);
        assertThat(mShadowAlarmManager.peekNextScheduledAlarm()).isNull();
    }

    @Ignore("b/314921894")
    @Test
    public void onReceive_withTimeChangedIntent_clearsAllDataAndRefreshesJob()
            throws InterruptedException {
        mReceiver.onReceive(mContext, new Intent(Intent.ACTION_TIME_CHANGED));

        TimeUnit.MILLISECONDS.sleep(100);
        assertThat(mDao.getAllAfter(0)).isEmpty();
        assertThat(mShadowAlarmManager.peekNextScheduledAlarm()).isNotNull();
    }

    @Test
    public void invokeJobRecheck_broadcastsIntent() {
        BootBroadcastReceiver.invokeJobRecheck(mContext);

        final List<Intent> intents = Shadows.shadowOf((Application) mContext).getBroadcastIntents();
        assertThat(intents).hasSize(1);
        assertThat(intents.get(0).getAction())
                .isEqualTo(BootBroadcastReceiver.ACTION_PERIODIC_JOB_RECHECK);
    }

    private void clearSharedPreferences() {
        DatabaseUtils.getSharedPreferences(mContext).edit().clear().apply();
    }
}
