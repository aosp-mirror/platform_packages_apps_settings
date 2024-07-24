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

import androidx.annotation.VisibleForTesting;

import com.android.settingslib.utils.AsyncLoaderCompat;

/**
 * Loader that can be used by classes to load BatteryInfo in a background thread. This loader will
 * automatically grab enhanced battery estimates if available or fall back to the system estimate
 * when not available.
 */
public class BatteryInfoLoader extends AsyncLoaderCompat<BatteryInfo> {
    private static final String LOG_TAG = "BatteryInfoLoader";

    @VisibleForTesting BatteryUtils mBatteryUtils;

    public BatteryInfoLoader(Context context) {
        super(context);
        mBatteryUtils = BatteryUtils.getInstance(context);
    }

    @Override
    protected void onDiscardResult(BatteryInfo result) {}

    @Override
    public BatteryInfo loadInBackground() {
        return mBatteryUtils.getBatteryInfo(LOG_TAG);
    }
}
