/**
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.List;

/** Main fragment to display first day of week. */
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class FirstDayOfWeekItemFragment extends DashboardFragment {

    private static final String LOG_TAG = "FirstDayOfWeekItemFragment";
    private static final String KEY_PREFERENCE_CATEGORY_FIRST_DAY_OF_WEEK_ITEM =
            "first_day_of_week_item_category";

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.regional_preferences_first_day_of_week;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.FIRST_DAY_OF_WEEK_PREFERENCE;
    }

    @Override
    protected String getLogTag() {
        return LOG_TAG;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new FirstDayOfWeekItemCategoryController(context,
                KEY_PREFERENCE_CATEGORY_FIRST_DAY_OF_WEEK_ITEM));
        return controllers;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.regional_preferences_first_day_of_week);
}
