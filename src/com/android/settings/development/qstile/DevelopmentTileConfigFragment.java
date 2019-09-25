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

package com.android.settings.development.qstile;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.provider.SearchIndexableResource;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.Indexable;
import com.android.settingslib.development.DevelopmentSettingsEnabler;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.List;

@SearchIndexable
public class DevelopmentTileConfigFragment extends DashboardFragment {
    private static final String TAG = "DevelopmentTileConfig";

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.development_tile_settings;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DEVELOPMENT_QS_TILE_CONFIG;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.development_tile_settings) {

                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return DevelopmentSettingsEnabler.isDevelopmentSettingsEnabled(context);
                }
            };
}
