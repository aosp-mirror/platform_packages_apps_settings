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

package com.android.settings.development.snooplogger;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.provider.SearchIndexableResource;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.development.DeveloperOptionAwareMixin;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.development.DevelopmentSettingsEnabler;
import com.android.settingslib.search.SearchIndexable;

import java.util.List;

/**
 * Bluetooth Snoop Logger Filters Dashboard
 */
@SearchIndexable
public class SnoopLoggerFiltersDashboard extends DashboardFragment implements
        DeveloperOptionAwareMixin {

    private static final String TAG = "SnoopLoggerFiltersDashboard";

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_SNOOP_LOGGER_DASHBOARD;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.snoop_logger_filters_settings;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public int getHelpResource() {
        return 0;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.snoop_logger_filters_settings;
                    return List.of(sir);
                }

                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return DevelopmentSettingsEnabler.isDevelopmentSettingsEnabled(context);
                }
            };
}
