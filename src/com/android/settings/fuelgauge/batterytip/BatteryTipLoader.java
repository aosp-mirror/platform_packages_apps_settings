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

package com.android.settings.fuelgauge.batterytip;

import android.content.Context;

import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.fuelgauge.batterytip.tips.BatteryTip;
import com.android.settings.fuelgauge.batterytip.tips.SummaryTip;
import com.android.settingslib.utils.AsyncLoader;

import java.util.ArrayList;
import java.util.List;

/**
 * Loader to compute and return a battery tip list. It will always return a full length list even
 * though some tips may have state {@code BaseBatteryTip.StateType.INVISIBLE}.
 */
public class BatteryTipLoader extends AsyncLoader<List<BatteryTip>> {
    private static final String TAG = "BatteryTipLoader";

    private static final boolean USE_FAKE_DATA = false;

    private BatteryStatsHelper mBatteryStatsHelper;

    public BatteryTipLoader(Context context, BatteryStatsHelper batteryStatsHelper) {
        super(context);
        mBatteryStatsHelper = batteryStatsHelper;
    }

    @Override
    public List<BatteryTip> loadInBackground() {
        List<BatteryTip> tips = new ArrayList<>();

        //TODO(b/70570352): add battery tip detectors
        tips.add(new SummaryTip(BatteryTip.StateType.NEW));
        return tips;
    }

    @Override
    protected void onDiscardResult(List<BatteryTip> result) {
    }

}
