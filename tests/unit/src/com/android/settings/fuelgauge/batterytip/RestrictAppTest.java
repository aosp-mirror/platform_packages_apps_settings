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

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class RestrictAppTest {
    private static final String BATTERY_INTENT = "android.intent.action.POWER_USAGE_SUMMARY";
    private static final String PACKAGE_SETTINGS = "com.android.settings";
    private static final String PACKAGE_SYSTEM_UI = "com.android.systemui";

    private BatteryDatabaseManager mBatteryDatabaseManager;
    private PackageManager mPackageManager;

    @Before
    public void setUp() {
        final Context context = InstrumentationRegistry.getTargetContext();
        mPackageManager = context.getPackageManager();
        mBatteryDatabaseManager = BatteryDatabaseManager.getInstance(context);
        mBatteryDatabaseManager.deleteAllAnomaliesBeforeTimeStamp(System.currentTimeMillis() +
                TimeUnit.DAYS.toMillis(1));
    }

    @Test
    public void testBatterySettings_hasOneAnomaly_showAnomaly() throws
            PackageManager.NameNotFoundException {
        mBatteryDatabaseManager.insertAnomaly(mPackageManager.getPackageUid(PACKAGE_SETTINGS, 0),
                PACKAGE_SETTINGS, 1,
                AnomalyDatabaseHelper.State.NEW, System.currentTimeMillis());

        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.startActivitySync(new Intent(BATTERY_INTENT));
        onView(withText("Restrict 1 app")).check(matches(isDisplayed()));
    }

    @Test
    public void testBatterySettings_hasTwoAnomalies_showAnomalies() throws
            PackageManager.NameNotFoundException {
        mBatteryDatabaseManager.insertAnomaly(mPackageManager.getPackageUid(PACKAGE_SETTINGS, 0),
                PACKAGE_SETTINGS, 1,
                AnomalyDatabaseHelper.State.NEW, System.currentTimeMillis());
        mBatteryDatabaseManager.insertAnomaly(mPackageManager.getPackageUid(PACKAGE_SYSTEM_UI, 0),
                PACKAGE_SYSTEM_UI, 1,
                AnomalyDatabaseHelper.State.NEW, System.currentTimeMillis());

        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.startActivitySync(new Intent(BATTERY_INTENT));
        onView(withText("Restrict 2 apps")).check(matches(isDisplayed()));
    }
}
