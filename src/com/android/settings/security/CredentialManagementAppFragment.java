/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.security;

import android.app.settings.SettingsEnums;
import android.content.Context;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

/**
 * Settings fragment containing the credential management app. The credential management app has
 * the ability to manage the user's credentials on unmanaged devices.
 */
@SearchIndexable
public class CredentialManagementAppFragment extends DashboardFragment {

    private static final String TAG = "CredentialManagementApp";

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.credential_management_app_fragment;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.CREDENTIAL_MANAGEMENT_APP;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        use(CredentialManagementAppButtonsController.class).setParentFragment(this);
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.credential_management_app_fragment);
}
