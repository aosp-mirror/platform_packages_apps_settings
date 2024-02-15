/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.deviceinfo.batteryinfo;

import android.app.settings.SettingsEnums;
import android.content.Context;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

/** A fragment that shows battery hardware information. */
@SearchIndexable
public class BatteryInfoFragment extends DashboardFragment {

    public static final String TAG = "BatteryInfo";

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_BATTERY_INFORMATION;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.battery_info;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.battery_info) {
                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return FeatureFactory.getFeatureFactory()
                            .getBatterySettingsFeatureProvider()
                            .isBatteryInfoEnabled(context);
                }
            };
}
