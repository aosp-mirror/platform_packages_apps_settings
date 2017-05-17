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

package com.android.settings.fuelgauge.anomaly;

import android.content.Context;
import android.support.annotation.VisibleForTesting;

import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.fuelgauge.PowerUsageFeatureProvider;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.utils.AsyncLoader;

import java.util.ArrayList;
import java.util.List;

/**
 * Loader to compute which apps are anomaly and return a anomaly list. It will return
 * an empty list if there is no anomaly.
 */
public class AnomalyLoader extends AsyncLoader<List<Anomaly>> {
    private BatteryStatsHelper mBatteryStatsHelper;
    private PowerUsageFeatureProvider mPowerUsageFeatureProvider;
    @VisibleForTesting
    AnomalyUtils mAnomalyUtils;

    public AnomalyLoader(Context context, BatteryStatsHelper batteryStatsHelper) {
        super(context);
        mBatteryStatsHelper = batteryStatsHelper;
        mPowerUsageFeatureProvider = FeatureFactory.getFactory(
                context).getPowerUsageFeatureProvider(context);
        mAnomalyUtils = AnomalyUtils.getInstance(context);
    }

    @Override
    protected void onDiscardResult(List<Anomaly> result) {
    }

    @Override
    public List<Anomaly> loadInBackground() {
        final List<Anomaly> anomalies = new ArrayList<>();
        for (@Anomaly.AnomalyType int type : Anomaly.ANOMALY_TYPE_LIST) {
            if (mPowerUsageFeatureProvider.isAnomalyDetectorEnabled(type)) {
                anomalies.addAll(mAnomalyUtils.getAnomalyDetector(type).detectAnomalies(
                        mBatteryStatsHelper));
            }
        }

        return anomalies;
    }

}
