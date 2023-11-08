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

package com.android.settings.security;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.os.UserManager;

import androidx.annotation.VisibleForTesting;
import androidx.preference.SwitchPreference;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

@SearchIndexable
public class ContentProtectionPreferenceFragment extends DashboardFragment {
    private static final String TAG = "ContentProtectionPreferenceFragment";

    @VisibleForTesting
    static final String KEY_WORK_PROFILE_SWITCH =
            "content_protection_preference_user_consent_work_profile_switch";

    // Required by @SearchIndexable to make the fragment and preferences to be indexed.
    // Do not rename.
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.layout.content_protection_preference_fragment);

    private SwitchPreference mWorkProfileSwitch;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mWorkProfileSwitch = getPreferenceScreen().findPreference(KEY_WORK_PROFILE_SWITCH);
        // If any work profile on the device, display the disable toggle unchecked
        if (Utils.getManagedProfile(getContext().getSystemService(UserManager.class)) != null) {
            mWorkProfileSwitch.setVisible(true);
            mWorkProfileSwitch.setEnabled(false);
            mWorkProfileSwitch.setChecked(false);
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.CONTENT_PROTECTION_PREFERENCE;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.layout.content_protection_preference_fragment;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }
}
