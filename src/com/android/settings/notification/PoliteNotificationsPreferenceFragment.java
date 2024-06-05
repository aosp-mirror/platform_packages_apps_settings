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

package com.android.settings.notification;

import android.app.settings.SettingsEnums;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

/**
 * Fragment for polite notifications.
 */
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class PoliteNotificationsPreferenceFragment extends DashboardFragment {

    private static final String POLITE_NOTIF_PREFERENCE_KEY = "polite_notifications_pref_dlg";

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_POLITE_NOTIFICATIONS;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.polite_notifications_settings;
    }
    @Override
    protected String getLogTag() {
        return "PoliteNotificationsPreferenceFragment";
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.polite_notifications_settings);
}
