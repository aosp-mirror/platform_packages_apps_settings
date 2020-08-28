/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.applications.managedomainurls;

import static com.android.settingslib.search.SearchIndexable.MOBILE;

import android.app.settings.SettingsEnums;
import android.content.Context;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

/**
 * Activity to manage how Android handles URL resolution. Includes both per-app
 * handling as well as system handling for Web Actions.
 */
@SearchIndexable(forTarget = MOBILE)
public class ManageDomainUrls extends DashboardFragment {

    private static final String TAG = "ManageDomainUrls";

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        use(DomainAppPreferenceController.class).setFragment(this);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.manage_domain_url_settings;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.MANAGE_DOMAIN_URLS;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.manage_domain_url_settings);
}
