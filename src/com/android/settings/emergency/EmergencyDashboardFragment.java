/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.emergency;

import android.app.settings.SettingsEnums;
import android.content.Context;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.notification.EmergencyBroadcastPreferenceController;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.List;


/**
 * {@link DashboardFragment} that hosts emergency/safety related settings.
 */
@SearchIndexable
public class EmergencyDashboardFragment extends DashboardFragment {

    private static final String TAG = "EmergencyDashboard";
    private static final String WEA_PREF_KEY = "app_and_notif_cell_broadcast_settings";

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.emergency_settings;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.EMERGENCY_SETTINGS;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context);
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new EmergencyBroadcastPreferenceController(context, WEA_PREF_KEY));
        return controllers;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.emergency_settings) {
                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return context.getResources().getBoolean(R.bool.config_show_emergency_settings);
                }
            };
}
