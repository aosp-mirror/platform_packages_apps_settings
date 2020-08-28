/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.display;

import static com.android.settings.homepage.contextualcards.slices.ContextualAdaptiveSleepSlice.PREF;
import static com.android.settings.homepage.contextualcards.slices.ContextualAdaptiveSleepSlice.PREF_KEY_INTERACTED;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class AdaptiveSleepSettings extends DashboardFragment {

    private static final String TAG = "AdaptiveSleepSettings";
    private Context mContext;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mContext = getContext();
        Preference permissionPreference = findPreference(
                AdaptiveSleepPermissionPreferenceController.PREF_NAME);
        if (permissionPreference != null) {
            permissionPreference.setVisible(false);
        }

        mContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(PREF_KEY_INTERACTED, true)
                .apply();
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.adaptive_sleep_detail;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_ADAPTIVE_SLEEP;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_adaptive_sleep;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.adaptive_sleep_detail);
}
