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

import com.android.settings.Utils;
import com.android.settings.fuelgauge.batterytip.AppInfo;

import java.util.function.Predicate;

/**
 * {@link Predicate} for {@link AppInfo} to check whether it has label
 */
public class AppLabelPredicate implements Predicate<AppInfo> {
    private Context mContext;
    private AppOpsManager mAppOpsManager;

    public AppLabelPredicate(Context context) {
        mContext = context;
        mAppOpsManager = context.getSystemService(AppOpsManager.class);
    }

    @Override
    public boolean test(AppInfo appInfo) {
        // Return true if app doesn't have label
        return Utils.getApplicationLabel(mContext, appInfo.packageName) == null;
    }
}
