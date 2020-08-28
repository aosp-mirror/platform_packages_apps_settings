/*
 * Copyright (C) 2018 The Android Open Source Project
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

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static com.google.common.truth.Truth.assertThat;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.test.uiautomator.UiDevice;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class RestrictAppTest {
    private static final String WM_DISMISS_KEYGUARD_COMMAND = "wm dismiss-keyguard";
    private static final String BATTERY_INTENT = "android.intent.action.POWER_USAGE_SUMMARY";
    private static final String PACKAGE_SETTINGS = "com.android.settings";
    private static final String PACKAGE_SYSTEM_UI = "com.android.systemui";
    private static final int ANOMALY_TYPE =
            StatsManagerConfig.AnomalyType.EXCESSIVE_WAKELOCK_ALL_SCREEN_OFF;

    private BatteryDatabaseManager mBatteryDatabaseManager;
    private PackageManager mPackageManager;
    private UiDevice mUiDevice;

    @Before
    public void setUp() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();

        mUiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        mUiDevice.wakeUp();
        mUiDevice.executeShellCommand(WM_DISMISS_KEYGUARD_COMMAND);

        mPackageManager = context.getPackageManager();
        mBatteryDatabaseManager = BatteryDatabaseManager.getInstance(context);
        mBatteryDatabaseManager.deleteAllAnomaliesBeforeTimeStamp(System.currentTimeMillis() +
                TimeUnit.DAYS.toMillis(1));
    }

    @Test
    public void batterySettings_hasOneAnomaly_showAnomaly() throws
            PackageManager.NameNotFoundException {
        mBatteryDatabaseManager.insertAnomaly(mPackageManager.getPackageUid(PACKAGE_SETTINGS, 0),
                PACKAGE_SETTINGS, ANOMALY_TYPE,
                AnomalyDatabaseHelper.State.NEW, System.currentTimeMillis());

        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.startActivitySync(createBatteryIntent());
        onView(withText("Restrict 1 app")).check(matches(isDisplayed()));
    }

    @Test
    public void batterySettings_hasTwoAnomalies_showAnomalies() throws
            PackageManager.NameNotFoundException {
        mBatteryDatabaseManager.insertAnomaly(mPackageManager.getPackageUid(PACKAGE_SETTINGS, 0),
                PACKAGE_SETTINGS, ANOMALY_TYPE,
                AnomalyDatabaseHelper.State.NEW, System.currentTimeMillis());
        mBatteryDatabaseManager.insertAnomaly(mPackageManager.getPackageUid(PACKAGE_SYSTEM_UI, 0),
                PACKAGE_SYSTEM_UI, ANOMALY_TYPE,
                AnomalyDatabaseHelper.State.NEW, System.currentTimeMillis());

        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.startActivitySync(createBatteryIntent());
        onView(withText("Restrict 2 apps")).check(matches(isDisplayed()));
    }

    @Test
    public void insertDuplicateAnomalies_onlyInsertOnce() throws
            PackageManager.NameNotFoundException {
        final int uid = mPackageManager.getPackageUid(PACKAGE_SETTINGS, 0);
        final long now = System.currentTimeMillis();

        // Insert same anomaly twice, it fails at the second time.
        assertThat(mBatteryDatabaseManager.insertAnomaly(uid, PACKAGE_SETTINGS, ANOMALY_TYPE,
                AnomalyDatabaseHelper.State.NEW, now)).isTrue();
        assertThat(mBatteryDatabaseManager.insertAnomaly(uid, PACKAGE_SETTINGS, ANOMALY_TYPE,
                AnomalyDatabaseHelper.State.NEW, now)).isFalse();

        // In database, only contains one row
        List<AppInfo> newAppInfos = mBatteryDatabaseManager.queryAllAnomalies(0,
                AnomalyDatabaseHelper.State.NEW);
        assertThat(newAppInfos).containsExactly(new AppInfo.Builder()
                .setUid(uid)
                .setPackageName(PACKAGE_SETTINGS)
                .addAnomalyType(ANOMALY_TYPE)
                .build());
    }

    private Intent createBatteryIntent() {
        final Intent intent = new Intent(BATTERY_INTENT);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        return intent;
    }
}
