/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.android.settings.fuelgauge;

import static com.android.settings.fuelgauge.BatteryBroadcastReceiver.BatteryUpdateType;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.BatteryManager;
import android.os.Bundle;
import android.provider.SearchIndexableResource;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class PowerUsageAdvanced extends PowerUsageBase {
    private static final String TAG = "AdvancedBatteryUsage";
    private static final String KEY_BATTERY_GRAPH = "battery_graph";
    private static final String KEY_APP_LIST = "app_list";

    @VisibleForTesting
    BatteryHistoryPreference mHistPref;
    private PowerUsageFeatureProvider mPowerUsageFeatureProvider;
    private BatteryAppListPreferenceController mBatteryAppListPreferenceController;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        final Context context = getContext();

        mHistPref = (BatteryHistoryPreference) findPreference(KEY_BATTERY_GRAPH);
        mPowerUsageFeatureProvider = FeatureFactory.getFactory(context)
                .getPowerUsageFeatureProvider(context);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (getActivity().isChangingConfigurations()) {
            BatteryEntry.clearUidCache();
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.FUELGAUGE_BATTERY_HISTORY_DETAIL;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.power_usage_advanced;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();

        mBatteryAppListPreferenceController = new BatteryAppListPreferenceController(context,
                KEY_APP_LIST, getSettingsLifecycle(), (SettingsActivity) getActivity(), this);
        controllers.add(mBatteryAppListPreferenceController);

        return controllers;
    }

    @Override
    protected boolean isBatteryHistoryNeeded() {
        return true;
    }

    @Override
    protected void refreshUi(@BatteryUpdateType int refreshType) {
        final Context context = getContext();
        if (context == null) {
            return;
        }
        updatePreference(mHistPref);
        mBatteryAppListPreferenceController.refreshAppListGroup(mBatteryUsageStats, true);
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.power_usage_advanced;
                    return Arrays.asList(sir);
                }

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(
                        Context context) {
                    final List<AbstractPreferenceController> controllers = new ArrayList<>();
                    controllers.add(new BatteryAppListPreferenceController(context,
                            KEY_APP_LIST, null /* lifecycle */, null /* activity */,
                            null /* fragment */));
                    return controllers;
                }
            };

}
