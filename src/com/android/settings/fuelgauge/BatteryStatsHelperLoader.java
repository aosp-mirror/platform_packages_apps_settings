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
import android.os.UserManager;

import androidx.annotation.VisibleForTesting;

import com.android.internal.os.BatteryStatsHelper;
import com.android.settingslib.utils.AsyncLoaderCompat;

/**
 * Loader to get new {@link BatteryStatsHelper} in the background
 */
public class BatteryStatsHelperLoader extends AsyncLoaderCompat<BatteryStatsHelper> {
    @VisibleForTesting
    UserManager mUserManager;
    @VisibleForTesting
    BatteryUtils mBatteryUtils;

    public BatteryStatsHelperLoader(Context context) {
        super(context);
        mUserManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        mBatteryUtils = BatteryUtils.getInstance(context);
    }

    @Override
    public BatteryStatsHelper loadInBackground() {
        Context context = getContext();
        final BatteryStatsHelper statsHelper = new BatteryStatsHelper(context,
                true /* collectBatteryBroadcast */);
        mBatteryUtils.initBatteryStatsHelper(statsHelper, null /* bundle */, mUserManager);

        return statsHelper;
    }

    @Override
    protected void onDiscardResult(BatteryStatsHelper result) {

    }

}
