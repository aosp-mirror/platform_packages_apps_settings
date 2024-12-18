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
import static org.robolectric.Shadows.shadowOf;

import android.app.AlarmManager;
import android.app.Application;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.UserManager;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.fuelgauge.batteryusage.db.BatteryEventDao;
import com.android.settings.fuelgauge.batteryusage.db.BatteryStateDao;
import com.android.settings.fuelgauge.batteryusage.db.BatteryStateDatabase;
import com.android.settings.testutils.BatteryTestUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowAlarmManager;

import java.time.Clock;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/** Tests of {@link BootBroadcastReceiver}. */
@RunWith(RobolectricTestRunner.class)
public final class BootBroadcastReceiverTest {
    private Context mContext;
    private BatteryStateDao mBatteryStateDao;
    private BatteryEventDao mBatteryEventDao;
    private BootBroadcastReceiver mReceiver;
    private ShadowAlarmManager mShadowAlarmManager;
    private PeriodicJobManager mPeriodicJobManager;

    @Mock
    private UserManager mUserManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getSystemService(UserManager.class)).thenReturn(mUserManager);
        mPeriodicJobManager = PeriodicJobManager.getInstance(mContext);
        mShadowAlarmManager = shadowOf(mContext.getSystemService(AlarmManager.class));
        mReceiver = new BootBroadcastReceiver();

        // Inserts fake data into database for testing.
        final BatteryStateDatabase database = BatteryTestUtils.setUpBatteryStateDatabase(mContext);
        mBatteryStateDao = database.batteryStateDao();
        mBatteryStateDao.clearAll();
        mBatteryEventDao = database.batteryEventDao();
        mBatteryEventDao.clearAll();
        clearSharedPreferences();
    }

    @After
    public void tearDown() {
        clearSharedPreferences();
        mPeriodicJobManager.reset();
    }

    @Test
    public void onReceive_withWorkProfile_notRefreshesJob() {
        doReturn(true).when(mUserManager).isManagedProfile();
        mReceiver.onReceive(mContext, new Intent(Intent.ACTION_BOOT_COMPLETED));

        assertThat(mShadowAlarmManager.peekNextScheduledAlarm()).isNull();
    }

    @Test
    public void onReceive_withPrivateProfile_notRefreshesJob() {
        doReturn(true).when(mUserManager).isPrivateProfile();
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

    @Test
    public void onReceive_withTimeChangedIntentSetEarlierTime_refreshesJob()
            throws InterruptedException {
        insertDataToTable(Clock.systemUTC().millis() + 60000);

        mReceiver.onReceive(mContext, new Intent(Intent.ACTION_TIME_CHANGED));

        TimeUnit.MILLISECONDS.sleep(1000);
        assertThat(mBatteryStateDao.getAllAfter(0)).isEmpty();
        assertThat(mBatteryEventDao.getAllAfterForLog(0)).isEmpty();
        assertThat(mShadowAlarmManager.peekNextScheduledAlarm()).isNotNull();
    }

    @Test
    public void onReceive_withTimeChangedIntentSetLaterTime_clearNoDataAndRefreshesJob()
            throws InterruptedException {
        insertDataToTable(Clock.systemUTC().millis() - 60000);

        mReceiver.onReceive(mContext, new Intent(Intent.ACTION_TIME_CHANGED));

        TimeUnit.MILLISECONDS.sleep(1000);
        assertThat(mBatteryStateDao.getAllAfter(0)).hasSize(1);
        assertThat(mBatteryEventDao.getAllAfterForLog(0)).hasSize(1);
        assertThat(mShadowAlarmManager.peekNextScheduledAlarm()).isNotNull();
    }

    @Test
    public void onReceive_withTimeFormatChangedIntent_skipRefreshJob() throws InterruptedException {
        insertDataToTable(Clock.systemUTC().millis() + 60000);

        mReceiver.onReceive(
                mContext,
                new Intent(Intent.EXTRA_INTENT)
                        .putExtra(
                                Intent.EXTRA_TIME_PREF_24_HOUR_FORMAT,
                                Intent.EXTRA_TIME_PREF_VALUE_USE_12_HOUR));

        TimeUnit.MILLISECONDS.sleep(1000);
        assertThat(mBatteryStateDao.getAllAfter(0)).hasSize(1);
        assertThat(mBatteryEventDao.getAllAfterForLog(0)).hasSize(1);
        assertThat(mShadowAlarmManager.peekNextScheduledAlarm()).isNull();
    }

    @Test
    public void onReceive_withTimeZoneChangedIntent_clearCacheDataAndRefreshesJob()
            throws InterruptedException {
        insertDataToTable(Clock.systemUTC().millis());

        assertThat(mBatteryStateDao.getAllAfter(0)).hasSize(1);

        mReceiver.onReceive(mContext, new Intent(Intent.ACTION_TIMEZONE_CHANGED));

        TimeUnit.MILLISECONDS.sleep(1000);
        // Only clear cache data.
        assertThat(mBatteryStateDao.getAllAfter(0)).hasSize(1);
        assertThat(mBatteryEventDao.getAllAfterForLog(0)).isEmpty();
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

    private void insertDataToTable(long recordTimeMs) {
        BatteryTestUtils.insertDataToBatteryStateTable(
                mContext, recordTimeMs, "com.android.systemui");
        BatteryTestUtils.insertDataToBatteryEventTable(
                mContext, recordTimeMs, BatteryEventType.EVEN_HOUR.getNumber(), 50);
        assertThat(mBatteryStateDao.getAllAfter(0)).hasSize(1);
        assertThat(mBatteryEventDao.getAllAfterForLog(0)).hasSize(1);
    }
}
