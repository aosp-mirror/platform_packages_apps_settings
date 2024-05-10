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

package com.android.settings.fuelgauge.batterytip.tips;

import android.app.AppOpsManager;
import android.content.Context;

import com.android.settings.fuelgauge.batterytip.AppInfo;

import java.util.function.Predicate;

/** {@link Predicate} for {@link AppInfo} to check whether it is restricted. */
public class AppRestrictionPredicate implements Predicate<AppInfo> {

    private static AppRestrictionPredicate sInstance;
    private AppOpsManager mAppOpsManager;

    public static AppRestrictionPredicate getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new AppRestrictionPredicate(context.getApplicationContext());
        }

        return sInstance;
    }

    private AppRestrictionPredicate(Context context) {
        mAppOpsManager = context.getSystemService(AppOpsManager.class);
    }

    @Override
    public boolean test(AppInfo appInfo) {
        // Return true if app already been restricted
        return mAppOpsManager.checkOpNoThrow(
                        AppOpsManager.OP_RUN_ANY_IN_BACKGROUND, appInfo.uid, appInfo.packageName)
                == AppOpsManager.MODE_IGNORED;
    }
}
