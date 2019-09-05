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

package com.android.settings.testutils;

import android.content.Context;

import com.android.settings.fuelgauge.batterytip.AnomalyDatabaseHelper;
import com.android.settings.fuelgauge.batterytip.BatteryDatabaseManager;
import com.android.settings.slices.SlicesDatabaseHelper;

import org.robolectric.util.ReflectionHelpers;

public class DatabaseTestUtils {

    public static void clearDb(Context context) {
        clearSlicesDb(context);
        clearAnomalyDb(context);
        clearAnomalyDbManager();
    }

    private static void clearSlicesDb(Context context) {
        SlicesDatabaseHelper helper = SlicesDatabaseHelper.getInstance(context);
        helper.close();

        ReflectionHelpers.setStaticField(SlicesDatabaseHelper.class, "sSingleton", null);
    }

    private static void clearAnomalyDb(Context context) {
        AnomalyDatabaseHelper helper = AnomalyDatabaseHelper.getInstance(context);
        helper.close();

        ReflectionHelpers.setStaticField(AnomalyDatabaseHelper.class, "sSingleton", null);
    }

    private static void clearAnomalyDbManager() {
        ReflectionHelpers.setStaticField(BatteryDatabaseManager.class, "sSingleton", null);
    }
}
