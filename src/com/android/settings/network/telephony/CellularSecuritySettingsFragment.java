/*
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
package com.android.settings.network.telephony;

import android.app.settings.SettingsEnums;
import android.os.Bundle;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

/**
 * Cellular Security features (insecure network notifications, network security controls, etc)
 */
@SearchIndexable
public class CellularSecuritySettingsFragment extends DashboardFragment {

    private static final String TAG = "CellularSecuritySettingsFragment";

    public static final String KEY_CELLULAR_SECURITY_PREFERENCE = "cellular_security";

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.CELLULAR_SECURITY_SETTINGS;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.cellular_security;
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String rootKey) {
        super.onCreatePreferences(bundle, rootKey);
        setPreferencesFromResource(R.xml.cellular_security, rootKey);
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.cellular_security);
}
