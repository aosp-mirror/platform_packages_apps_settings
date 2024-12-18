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

package com.android.settings.development;

import android.app.settings.SettingsEnums;
import android.content.Context;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

@SearchIndexable
public class DevelopmentMemtagPage extends DashboardFragment implements DeveloperOptionAwareMixin {
    private static final String TAG = "DevelopmentMemtagPage";

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_DEVELOPMENT_MEMTAG_CATEGORY;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        use(RebootWithMtePreferenceController.class).setFragment(this);
        use(DevelopmentMemtagPreferenceController.class).setFragment(this);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.development_memtag_page;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.development_memtag_page);
}
