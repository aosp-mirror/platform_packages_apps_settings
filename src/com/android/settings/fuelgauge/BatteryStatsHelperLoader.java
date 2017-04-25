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

package com.android.settings.fuelgauge;

import android.content.Context;
import android.os.BatteryStats;
import android.os.Bundle;
import android.os.UserManager;
import android.support.annotation.VisibleForTesting;

import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.utils.AsyncLoader;

/**
 * Loader to get new {@link BatteryStatsHelper} in the background
 */
public class BatteryStatsHelperLoader extends AsyncLoader<BatteryStatsHelper> {
    @VisibleForTesting
    UserManager mUserManager;
    private Bundle mBundle;

    public BatteryStatsHelperLoader(Context context, Bundle bundle) {
        super(context);
        mBundle = bundle;
        mUserManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
    }

    @Override
    public BatteryStatsHelper loadInBackground() {
        final BatteryStatsHelper statsHelper = new BatteryStatsHelper(getContext(), true);

        initBatteryStatsHelper(statsHelper);
        return statsHelper;
    }

    @Override
    protected void onDiscardResult(BatteryStatsHelper result) {

    }

    @VisibleForTesting
    void initBatteryStatsHelper(BatteryStatsHelper statsHelper) {
        statsHelper.create(mBundle);
        statsHelper.refreshStats(BatteryStats.STATS_SINCE_CHARGED, mUserManager.getUserProfiles());
    }
}
