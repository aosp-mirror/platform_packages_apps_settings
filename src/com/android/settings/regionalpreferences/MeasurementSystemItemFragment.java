/**
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.regionalpreferences;

import android.app.settings.SettingsEnums;
import android.content.Context;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.flags.Flags;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.List;

/** Main fragment to display measurement system. */
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class MeasurementSystemItemFragment extends DashboardFragment {

    private static final String LOG_TAG = "MeasurementSystemItemFragment";
    private static final String KEY_PREFERENCE_CATEGORY_MEASUREMENT_SYSTEM_ITEM =
            "measurement_system_item_category";
    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.regional_preferences_measurement_system;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.MEASUREMENT_SYSTEM_PREFERENCE;
    }

    @Override
    protected String getLogTag() {
        return LOG_TAG;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new MeasurementSystemItemCategoryController(context,
                KEY_PREFERENCE_CATEGORY_MEASUREMENT_SYSTEM_ITEM));
        return controllers;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.regional_preferences_measurement_system) {
                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    if (!Flags.regionalPreferencesApiEnabled()) {
                        return false;
                    }
                    return true;
                }
            };
}
