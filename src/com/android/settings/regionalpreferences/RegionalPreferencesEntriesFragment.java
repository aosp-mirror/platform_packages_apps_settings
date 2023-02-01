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

package com.android.settings.regionalpreferences;

import android.app.settings.SettingsEnums;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

/** Provides entries of each regional preferences */
@SearchIndexable
public class RegionalPreferencesEntriesFragment extends DashboardFragment {
    private static final String TAG = RegionalPreferencesEntriesFragment.class.getSimpleName();

    static final String ARG_KEY_REGIONAL_PREFERENCE = "arg_key_regional_preference";

    @Override
    public void onStart() {
        super.onStart();
        getActivity().setTitle(R.string.regional_preferences_title);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.REGIONAL_PREFERENCE;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.regional_preference_main_page;
    }

    /**
     * Get the tag string for logging.
     */
    @Override
    protected String getLogTag() {
        return TAG;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.regional_preference_main_page);
}
