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
import com.android.settings.search.IndexDatabaseHelper;
import com.android.settings.slices.SlicesDatabaseHelper;

import java.lang.reflect.Field;

public class DatabaseTestUtils {

    public static void clearDb(Context context) {
        clearSearchDb(context);
        clearSlicesDb(context);
        clearAnomalyDb(context);
        clearAnomalyDbManager();
    }

    private static void clearSlicesDb(Context context) {
        SlicesDatabaseHelper helper = SlicesDatabaseHelper.getInstance(context);
        helper.close();

        Field instance;
        Class clazz = SlicesDatabaseHelper.class;
        try {
            instance = clazz.getDeclaredField("sSingleton");
            instance.setAccessible(true);
            instance.set(null, null);
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    private static void clearAnomalyDb(Context context) {
        AnomalyDatabaseHelper helper = AnomalyDatabaseHelper.getInstance(context);
        helper.close();

        Field instance;
        Class clazz = AnomalyDatabaseHelper.class;
        try {
            instance = clazz.getDeclaredField("sSingleton");
            instance.setAccessible(true);
            instance.set(null, null);
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    private static void clearSearchDb(Context context) {
        IndexDatabaseHelper helper = IndexDatabaseHelper.getInstance(context);
        helper.close();

        Field instance;
        Class clazz = IndexDatabaseHelper.class;
        try {
            instance = clazz.getDeclaredField("sSingleton");
            instance.setAccessible(true);
            instance.set(null, null);
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    private static void clearAnomalyDbManager() {
        Field instance;
        Class clazz = BatteryDatabaseManager.class;
        try {
            instance = clazz.getDeclaredField("sSingleton");
            instance.setAccessible(true);
            instance.set(null, null);
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }
}
