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

import android.content.Context;

import com.android.settings.fuelgauge.PowerUsageFeatureProvider;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.utils.AsyncLoaderCompat;

import java.util.Map;

/** Loader that can be used to load battery history information. */
public class BatteryHistoryLoader
        extends AsyncLoaderCompat<Map<Long, Map<String, BatteryHistEntry>>> {
    private static final String TAG = "BatteryHistoryLoader";

    private final Context mContext;

    public BatteryHistoryLoader(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    protected void onDiscardResult(Map<Long, Map<String, BatteryHistEntry>> result) {
    }

    @Override
    public Map<Long, Map<String, BatteryHistEntry>> loadInBackground() {
        final PowerUsageFeatureProvider powerUsageFeatureProvider =
                FeatureFactory.getFactory(mContext).getPowerUsageFeatureProvider(mContext);
        return powerUsageFeatureProvider.getBatteryHistorySinceLastFullCharge(mContext);
    }
}
