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

package com.android.settings.nearby;

import android.app.settings.SettingsEnums;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

/**
 * Fragment with the top level fast pair settings.
 */
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class FastPairSettingsFragment extends SettingsPreferenceFragment {

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.CONNECTION_DEVICE_ADVANCED_FAST_PAIR;
    }

    @Override
    public int getHelpResource() {
        return 0;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.fast_pair_settings;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.fast_pair_settings);

}
